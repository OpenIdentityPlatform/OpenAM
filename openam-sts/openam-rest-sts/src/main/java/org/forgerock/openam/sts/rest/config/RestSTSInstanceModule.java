/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.rest.config;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.google.inject.name.Names;
import com.iplanet.am.util.SystemProperties;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.HttpURLConnectionFactory;
import org.forgerock.openam.sts.HttpURLConnectionWrapperFactory;
import org.forgerock.openam.sts.OpenAMHttpURLConnectionFactory;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.TokenCancellationException;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.TokenTypeId;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.XMLUtilities;
import org.forgerock.openam.sts.XMLUtilitiesImpl;
import org.forgerock.openam.sts.config.user.AuthTargetMapping;
import org.forgerock.openam.sts.config.user.CustomTokenOperation;
import org.forgerock.openam.sts.rest.RestSTS;
import org.forgerock.openam.sts.rest.RestSTSImpl;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;
import org.forgerock.openam.sts.rest.config.user.TokenTransformConfig;
import org.forgerock.openam.sts.rest.operation.cancel.IssuedTokenCancelOperation;
import org.forgerock.openam.sts.rest.operation.cancel.IssuedTokenCancelOperationImpl;
import org.forgerock.openam.sts.rest.operation.cancel.IssuedTokenCancellerFactory;
import org.forgerock.openam.sts.rest.operation.cancel.IssuedTokenCancellerFactoryImpl;
import org.forgerock.openam.sts.rest.operation.validate.IssuedTokenValidateOperation;
import org.forgerock.openam.sts.rest.operation.validate.IssuedTokenValidateOperationImpl;
import org.forgerock.openam.sts.rest.operation.TokenRequestMarshaller;
import org.forgerock.openam.sts.rest.operation.TokenRequestMarshallerImpl;
import org.forgerock.openam.sts.rest.operation.translate.TokenTransformFactory;
import org.forgerock.openam.sts.rest.operation.translate.TokenTransformFactoryImpl;
import org.forgerock.openam.sts.rest.operation.translate.TokenTranslateOperation;
import org.forgerock.openam.sts.rest.operation.translate.TokenTranslateOperationImpl;
import org.forgerock.openam.sts.rest.operation.validate.IssuedTokenValidatorFactory;
import org.forgerock.openam.sts.rest.operation.validate.IssuedTokenValidatorFactoryImpl;
import org.forgerock.openam.sts.rest.token.provider.oidc.DefaultOpenIdConnectTokenAuthMethodReferencesMapper;
import org.forgerock.openam.sts.rest.token.provider.oidc.DefaultOpenIdConnectTokenAuthnContextMapper;
import org.forgerock.openam.sts.rest.token.provider.oidc.OpenIdConnectTokenAuthMethodReferencesMapper;
import org.forgerock.openam.sts.rest.token.provider.oidc.OpenIdConnectTokenAuthnContextMapper;
import org.forgerock.openam.sts.rest.token.provider.saml.Saml2JsonTokenAuthnContextMapperImpl;
import org.forgerock.openam.sts.rest.token.provider.saml.Saml2JsonTokenAuthnContextMapper;
import org.forgerock.openam.sts.rest.token.validator.disp.RestUsernameTokenAuthenticationRequestDispatcher;
import org.forgerock.openam.sts.token.AMTokenParser;
import org.forgerock.openam.sts.token.AMTokenParserImpl;
import org.forgerock.openam.sts.token.CTSTokenIdGenerator;
import org.forgerock.openam.sts.token.CTSTokenIdGeneratorImpl;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCacheImpl;
import org.forgerock.openam.sts.token.UrlConstituentCatenator;
import org.forgerock.openam.sts.token.UrlConstituentCatenatorImpl;
import org.forgerock.openam.sts.token.model.OpenIdConnectIdToken;
import org.forgerock.openam.sts.token.model.RestUsernameToken;
import org.forgerock.openam.sts.token.provider.TokenServiceConsumer;
import org.forgerock.openam.sts.token.provider.TokenServiceConsumerImpl;
import org.forgerock.openam.sts.token.validator.PrincipalFromSession;
import org.forgerock.openam.sts.token.validator.PrincipalFromSessionImpl;
import org.forgerock.openam.sts.token.validator.AuthenticationHandler;
import org.forgerock.openam.sts.token.validator.disp.CertificateAuthenticationRequestDispatcher;
import org.forgerock.openam.sts.rest.token.validator.disp.OpenIdConnectAuthenticationRequestDispatcher;
import org.forgerock.openam.sts.token.validator.disp.TokenAuthenticationRequestDispatcher;
import org.forgerock.openam.sts.token.validator.AuthenticationHandlerImpl;
import org.forgerock.openam.sts.token.validator.url.AuthenticationUrlProviderImpl;
import org.forgerock.openam.sts.token.validator.url.AuthenticationUrlProvider;

import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

import org.forgerock.openam.sts.user.invocation.RestSTSTokenCancellationInvocationState;
import org.forgerock.openam.sts.user.invocation.RestSTSTokenValidationInvocationState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class defines all of the bindings for an instance of the REST-STS. The RestSTSInstanceConfig instance
 * passed to its ctor defines all of the state necessary to bind the elements necessary for a full REST-STS object
 * graph.
 *
 * Note that the top-level entry point for REST-STS invocations is RestSTS/RestSTSImpl. These invocations are then
 * delegated to the top-level operations (TokenTranslateOperation, IssuedTokenValidateOperation, and IssuedTokenCancelOperation),
 * which are effectively singletons - i.e they will be initialized once. This also means that their dependencies are also
 * singletons. The point is to create an effectively-immutable object graph for threading performance.
 */
public class RestSTSInstanceModule extends AbstractModule {
    private final RestSTSInstanceConfig stsInstanceConfig;

    public RestSTSInstanceModule(RestSTSInstanceConfig stsInstanceConfig) {
        this.stsInstanceConfig = stsInstanceConfig;
    }

    public void configure() {
        bind(ThreadLocalAMTokenCache.class).to(ThreadLocalAMTokenCacheImpl.class).in(Scopes.SINGLETON);


        bind(AuthenticationUrlProvider.class)
                .to(AuthenticationUrlProviderImpl.class);

        bind(new TypeLiteral<TokenAuthenticationRequestDispatcher<RestUsernameToken>>(){})
                .to(RestUsernameTokenAuthenticationRequestDispatcher.class);

        bind(new TypeLiteral<AuthenticationHandler<RestUsernameToken>>(){})
                .to(new TypeLiteral<AuthenticationHandlerImpl<RestUsernameToken>>() {});

        bind(new TypeLiteral<TokenAuthenticationRequestDispatcher<OpenIdConnectIdToken>>(){})
                .to(OpenIdConnectAuthenticationRequestDispatcher.class);

        bind(new TypeLiteral<AuthenticationHandler<OpenIdConnectIdToken>>(){})
                .to(new TypeLiteral<AuthenticationHandlerImpl<OpenIdConnectIdToken>>() {});

        bind(new TypeLiteral<TokenAuthenticationRequestDispatcher<X509Certificate[]>>(){})
                .to(CertificateAuthenticationRequestDispatcher.class);

        bind(new TypeLiteral<AuthenticationHandler<X509Certificate[]>>(){})
                .to(new TypeLiteral<AuthenticationHandlerImpl<X509Certificate[]>>() {});

        bind(TokenRequestMarshaller.class).to(TokenRequestMarshallerImpl.class);
        bind(TokenTransformFactory.class).to(TokenTransformFactoryImpl.class);
        bind(IssuedTokenValidatorFactory.class).to(IssuedTokenValidatorFactoryImpl.class);
        bind(IssuedTokenCancellerFactory.class).to(IssuedTokenCancellerFactoryImpl.class);
        bind(TokenTranslateOperation.class).to(TokenTranslateOperationImpl.class);
        bind(AMTokenParser.class).to(AMTokenParserImpl.class);
        bind(PrincipalFromSession.class).to(PrincipalFromSessionImpl.class);

        bind(RestSTS.class).to(RestSTSImpl.class).in(Scopes.SINGLETON);
        bind(UrlConstituentCatenator.class).to(UrlConstituentCatenatorImpl.class);

        bind(TokenServiceConsumer.class).to(TokenServiceConsumerImpl.class);

        /*
        Bind the class responsible for producing HttpURLConnectionWrapper instances, and the HttpURLConnectionFactory
        it consumes.
         */
        bind(HttpURLConnectionFactory.class).to(OpenAMHttpURLConnectionFactory.class).in(Scopes.SINGLETON);
        bind(HttpURLConnectionWrapperFactory.class).in(Scopes.SINGLETON);

        /*
        Bind the class which will allow for the generation of token ids, given a to-be-validated/canceled token.
        Necessary to consume the TokenService's validation/cancellation functionality
         */
        bind(CTSTokenIdGenerator.class).to(CTSTokenIdGeneratorImpl.class).in(Scopes.SINGLETON);

        /*
        Necessary for the CTSTokenIdGenerator, to generate a CTS token id when processing a SAML2 assertion
         */
        bind(XMLUtilities.class).to(XMLUtilitiesImpl.class).in(Scopes.SINGLETON);
    }

    /*
    Bindings below required by the STSAuthenticationUriProviderImpl - necessary to construct the URI for the REST authn call.
     */
    @Provides
    @Named (AMSTSConstants.REALM)
    String realm() {
        return stsInstanceConfig.getDeploymentConfig().getRealm();
    }

    @Provides
    @Singleton
    @Named (AMSTSConstants.AM_DEPLOYMENT_URL)
    String amDeploymentUrl() {
        /*
        This information used to be provided from the STSInstanceConfig, but this value needs to correspond to the local
        deployment so that, in site deployments, the Rest AuthN and the TGS of the local OpenAM instance is consumed, instead
        of the OpenAM url set when the rest-sts-instance was published.
         */
        return SystemProperties.getServerInstanceName();
    }

    /*
    Reference the RestSTSInjectorHolder, and the bindings made in the RestSTSModule, to obtain the
    OpenAM uri information which is global to all STS instances.
    Singleton so it is only called once.
     */
    @Provides
    @Singleton
    @Named (AMSTSConstants.REST_AUTHN_URI_ELEMENT)
    String restAuthnUriElement() {
        return RestSTSInjectorHolder.getInstance(Key.get(String.class,
                Names.named(AMSTSConstants.REST_AUTHN_URI_ELEMENT)));
    }

    /*
    Reference the RestSTSInjectorHolder, and the bindings made in the RestSTSModule, to obtain the
    OpenAM uri information which is global to all STS instances.
    Singleton so it is only called once.
     */
    @Provides
    @Singleton
    @Named (AMSTSConstants.REST_LOGOUT_URI_ELEMENT)
    String restLogoutUriElement() {
        return RestSTSInjectorHolder.getInstance(Key.get(String.class,
                Names.named(AMSTSConstants.REST_LOGOUT_URI_ELEMENT)));
    }

    /*
    Reference the RestSTSInjectorHolder, and the bindings made in the RestSTSModule, to obtain the
    OpenAM uri information which is global to all STS instances.
    Singleton so it is only called once.
     */
    @Provides
    @Singleton
    @Named (AMSTSConstants.REST_ID_FROM_SESSION_URI_ELEMENT)
    String restAMTokenValidationUriElement() {
        return RestSTSInjectorHolder.getInstance(Key.get(String.class,
                Names.named(AMSTSConstants.REST_ID_FROM_SESSION_URI_ELEMENT)));
    }

    /*
    Reference the RestSTSInjectorHolder, and the bindings made in the RestSTSModule, to obtain the
    OpenAM uri information which is global to all STS instances.
    Singleton so it is only called once.
     */
    @Provides
    @Singleton
    @Named(AMSTSConstants.REST_TOKEN_GENERATION_SERVICE_URI_ELEMENT)
    String tokenGenerationServiceUriElement() {
        return RestSTSInjectorHolder.getInstance(Key.get(String.class,
                Names.named(AMSTSConstants.REST_TOKEN_GENERATION_SERVICE_URI_ELEMENT)));
    }

    /*
    Reference the RestSTSInjectorHolder, and the bindings made in the RestSTSModule, to obtain the
    OpenAM uri information which is global to all STS instances.
    Singleton so it is only called once.
     */
    @Provides
    @Singleton
    @Named(AMSTSConstants.AM_SESSION_COOKIE_NAME)
    String getAMSessionCookieName() {
        return RestSTSInjectorHolder.getInstance(Key.get(String.class,
                Names.named(AMSTSConstants.AM_SESSION_COOKIE_NAME)));
    }

    /*
    Reference the RestSTSInjectorHolder, and the bindings made in the RestSTSModule, to obtain the
    OpenAM uri information which is global to all STS instances.
    Singleton so it is only called once.
     */
    @Provides
    @Singleton
    @Named(AMSTSConstants.AM_REST_AUTHN_JSON_ROOT)
    String getJsonRoot() {
        return RestSTSInjectorHolder.getInstance(Key.get(String.class,
                Names.named(AMSTSConstants.AM_REST_AUTHN_JSON_ROOT)));
    }


    @Provides
    AuthTargetMapping authTargetMapping() {
        return stsInstanceConfig.getDeploymentConfig().getAuthTargetMapping();
    }

    @Provides
    @Named(AMSTSConstants.REST_SUPPORTED_TOKEN_TRANSFORMS)
    Set<TokenTransformConfig> getSupportedTokenTransforms() {
        return stsInstanceConfig.getSupportedTokenTransforms();
    }

    /*
    This method is used to identify the rest sts instance. This identification is necessary when consuming
    the TokenGenerationService, as it is used to look-up the sts-instance-specific configuration state
    (crypto and SAML2 configurations) when issuing tokens for this sts instance. Note that this identifier
    does not have to be unique across rest and soap sts instances, as each will be represented by a different
    service-definition xml file and thus will be stored in a different DN by the SMS. The rest-sts will be identified
    by a combination of the realm, and the uri element within this realm. The uriElement defines the final endpoint, and
    it will always be deployed at a url which includes the realm.
    The value returned from RestSTSInstanceConfig#getDeploymentSubPath() will:
    1. determine the sub-path added to the crest router which will determine the url at which the sts instance is exposed
    2. be the most discriminating DN element identifying the config state corresponding to the STS instance in the SMS/LDAP
    3. Because of #2, the same deployment sub-path will be used to identify the rest sts instance when this instance consumes
    the TokenGenerationService, thereby allowing the TGS to look-up the instance-specific state necessary to produce
    instance-specific tokens.
     */
    @Provides
    @Named(AMSTSConstants.STS_INSTANCE_ID)
    String getSTSInstanceId() {
        return stsInstanceConfig.getDeploymentSubPath();
    }

    @Provides
    @Inject
    OpenIdConnectTokenAuthnContextMapper getOpenIdConnectTokenAuthnContextMapper(Logger logger) {
        if (stsInstanceConfig.getOpenIdConnectTokenConfig() != null) {
            String customMapperClassName = stsInstanceConfig.getOpenIdConnectTokenConfig().getCustomAuthnContextMapperClass();
            if (customMapperClassName != null) {
                try {
                    return Class.forName(customMapperClassName).asSubclass(OpenIdConnectTokenAuthnContextMapper.class).newInstance();
                } catch (Exception e) {
                    logger.error("Exception caught instantiating custom OpenIdConnectTokenAuthnContextMapper class: " + e + ". Default" +
                            " implementation will be returned. This means that acr claims will not be included in issued OIDC tokens.");
                }
            }
        }
        /*
        Slight semantic impurity: note that I am returning a default mapper, even though no config for the corresponding
        token type is present. I could return null, and annotate the dependency with @Nullable so Guice will inject null.
        The RestSTSInstanceConfig ctor will reject a construction which defines an OPENIDCONNECT output token, without
        a corresponding OpenIdConnectTokenConfig. However, it is possible that an sts instance will be published programmatically
        without the aid of the RestSTSInstanceConfig class. In this case, if null were returned here, the RestOpenIdConnectTokenProvider
        would NPE when obtaining the mapping. Thus the default mapper is a better choice. Token creation will be rejected
        at the token service if the invoking sts has no config corresponding to the desired token type.
         */
        return new DefaultOpenIdConnectTokenAuthnContextMapper();
    }

    @Provides
    @Inject
    OpenIdConnectTokenAuthMethodReferencesMapper getOpenIdConnectTokenAuthMethodReferencesMapper(Logger logger) {
        if (stsInstanceConfig.getOpenIdConnectTokenConfig() != null) {
            String customMapperClassName = stsInstanceConfig.getOpenIdConnectTokenConfig().getCustomAuthnMethodReferencesMapperClass();
            if (customMapperClassName != null) {
                try {
                    return Class.forName(customMapperClassName).asSubclass(OpenIdConnectTokenAuthMethodReferencesMapper.class).newInstance();
                } catch (Exception e) {
                    logger.error("Exception caught instantiating custom OpenIdConnectTokenAuthMethodReferencesMapper class: " + e + ". Default" +
                            " implementation will be returned. This means that amr claims will not be included in issued OIDC tokens.");
                }
            }
        }
        /*
        Slight semantic impurity: note that I am returning a default mapper, even though no config for the corresponding
        token type is present. I could return null, and annotate the dependency with @Nullable so Guice will inject null.
        The RestSTSInstanceConfig ctor will reject a construction which defines an OPENIDCONNECT output token, without
        a corresponding OpenIdConnectTokenConfig. However, it is possible that an sts instance will be published programmatically
        without the aid of the RestSTSInstanceConfig class. In this case, if null were returned here, the RestOpenIdConnectTokenProvider
        would NPE when obtaining the mapping. Thus the default mapper is a better choice. Token creation will be rejected
        at the token service if the invoking sts has no config corresponding to the desired token type.
         */
        return new DefaultOpenIdConnectTokenAuthMethodReferencesMapper();
    }

    /*
    Allows for a custom Saml2JsonTokenAuthnContextMapper to be plugged-in. This Saml2JsonTokenAuthnContextMapper provides a
    SAML2 AuthnContext class ref value given an input token and input token type.
     */
    @Provides
    @Inject
    Saml2JsonTokenAuthnContextMapper getSaml2AuthnContextMapper(Logger logger) {
        if (stsInstanceConfig.getSaml2Config() != null) {
            String customMapperClassName = stsInstanceConfig.getSaml2Config().getCustomAuthNContextMapperClassName();
            if (customMapperClassName != null) {
                try {
                    return Class.forName(customMapperClassName).asSubclass(Saml2JsonTokenAuthnContextMapper.class).newInstance();
                } catch (Exception e) {
                    logger.error("Exception caught instantiating custom Saml2JsonTokenAuthnContextMapper class " + customMapperClassName
                            + "; Returning default Saml2JsonTokenAuthnContextMapperImpl. The exception: " + e);
                }
            }
        }
        /*
        Slight semantic impurity: note that I am returning a default mapper, even though no config for the corresponding
        token type is present. I could return null, and annotate the dependency with @Nullable so Guice will inject null.
        The RestSTSInstanceConfig ctor will reject a construction which defines an SAML2 output token, without
        a corresponding SAML2Config. However, it is possible that an sts instance will be published programmatically
        without the aid of the RestSTSInstanceConfig class. In this case, if null were returned here, the RestOpenIdConnectTokenProvider
        would NPE when obtaining the mapping. Thus the default mapper is a better choice. Token creation will be rejected
        at the token service if the invoking sts has no config corresponding to the desired token type.
         */
        return new Saml2JsonTokenAuthnContextMapperImpl(logger);

    }

    @Provides
    Logger getSlf4jLogger() {
        return LoggerFactory.getLogger(AMSTSConstants.REST_STS_DEBUG_ID);
    }


    /**
     * If a rest-sts instance is configured to support a token transformation with an x509 token as an input token type, the
     * instance must be invoked via a two-way TLS exchange (i.e. where the client presents their certificate). If OpenAM
     * is deployed behind a tls-offloading engine, the client certificate won't be set as a HttpServetRequest attribute
     * referenced by the javax.servlet.request.X509Certificate key, but rather the rest sts instance must be configured
     * with the name of the http header where the tls-offloading engine will store the client certificate prior to invoking
     * OpenAM.
     *
     * This value will be injected into the TokenRequestMarshallerImpl so that it may obtain the X509 token specified
     * as the input token type. Note that this value must be injected, and because guice will not inject null references
     * without the @Nullable annotation, this method will return the empty string if this value is not set for the
     * published rest sts (it will only be set if x509 token transformations are desired, and if OpenAM is consumed in
     * a tls-offloaded deployment).
     */
    @Provides
    @Named(AMSTSConstants.OFFLOADED_TWO_WAY_TLS_HEADER_KEY)
    String getOffloadedTwoWayTLSHeaderKey() {
        String headerKey = stsInstanceConfig.getDeploymentConfig().getOffloadedTwoWayTlsHeaderKey();
        if (headerKey == null) {
            return "";
        }
        return headerKey;
    }

    @Provides
    @Named(AMSTSConstants.REST_CUSTOM_TOKEN_VALIDATORS)
    Set<CustomTokenOperation> getCustomTokenValidators() {
        return stsInstanceConfig.getCustomTokenValidators();
    }

    @Provides
    @Named(AMSTSConstants.REST_CUSTOM_TOKEN_PROVIDERS)
    Set<CustomTokenOperation> getCustomTokenProviders() {
        return stsInstanceConfig.getCustomTokenProviders();
    }

    @Provides
    @Named(AMSTSConstants.REST_CUSTOM_TOKEN_TRANSLATIONS)
    Set<TokenTransformConfig> getCustomTokenTransforms() {
        return stsInstanceConfig.getCustomTokenTransforms();
    }

    /*
    Needed by the IssuedTokenValidateOperationImpl - RestIssuedTokenValidator instances will be created for all of the rest-sts issued
    token types.
     */
    @Provides
    @Named(AMSTSConstants.ISSUED_TOKEN_TYPES)
    @Inject
    Set<TokenTypeId> getIssuedTokenTypes(@Named(AMSTSConstants.REST_SUPPORTED_TOKEN_TRANSFORMS) Set<TokenTransformConfig> tokenTransforms) {
        Set<TokenTypeId> issuedTokenTypes = new HashSet<>();
        for (TokenTransformConfig tokenTransformConfig : tokenTransforms) {
            if (!issuedTokenTypes.contains(tokenTransformConfig.getOutputTokenType())) {
                issuedTokenTypes.add(tokenTransformConfig.getOutputTokenType());
            }
        }
        return issuedTokenTypes;
    }

    /**
     * If a rest-sts instance is configured to support a token transformation with a x509 Certificate as an input token
     * type, and the OpenAM instance is deployed in a TLS-offloaded-environment, the TLS-offload-engines will be able
     * to provide the client's certificate, validated by a two-way-TLS handshake, to the rest-sts instance via a
     * header value only if the ip address of the host of the TLS-offload-engine is specified in the list below.
     * @return the set of Strings corresponding to the ip addresses of the deployment's TLS-offload-engines
     */
    @Provides
    @Named(AMSTSConstants.TLS_OFFLOAD_ENGINE_HOSTS)
    Set<String> getTlsOffloadEngineHostIpAddrs() {
        return stsInstanceConfig.getDeploymentConfig().getTlsOffloadEngineHostIpAddrs();
    }

    /*
    Required by the getIssuedTokenValidateOperation method below - a functional IssuedTokenValidateOperation instance will
    be bound only if issued tokens are persisted in the CTS, as this is a pre-requisite for token validation
     */
    @Provides
    @Named(AMSTSConstants.ISSUED_TOKENS_PERSISTED_IN_CTS)
    boolean issuedTokensPersistedInCTS() {
        return stsInstanceConfig.persistIssuedTokensInCTS();
    }

    /*
    Provides the IssuedTokenValidateOperation which is responsible for validating tokens issued by this sts instance.
    Will only return a functional IssuedTokenValidateOperation if the sts instance is configured to persist issued
    tokens in the CTS.
     */
    @Provides
    @Inject
    IssuedTokenValidateOperation getIssuedTokenValidateOperation(
                                @Named(AMSTSConstants.ISSUED_TOKENS_PERSISTED_IN_CTS) boolean issuedTokensPersistedInCTS,
                                IssuedTokenValidatorFactory issuedTokenValidatorFactory,
                                TokenRequestMarshaller tokenRequestMarshaller,
                                @Named(AMSTSConstants.ISSUED_TOKEN_TYPES) Set<TokenTypeId> validatedTokenTypes) throws STSInitializationException {
        if (issuedTokensPersistedInCTS) {
            return new IssuedTokenValidateOperationImpl(issuedTokenValidatorFactory, tokenRequestMarshaller, validatedTokenTypes);
        } else {
            return new IssuedTokenValidateOperation() {
                @Override
                public JsonValue validateToken(RestSTSTokenValidationInvocationState invocationState) throws TokenMarshalException, TokenValidationException {
                    throw new TokenMarshalException(ResourceException.CONFLICT, "This rest-sts instance is not configured " +
                            "to persist tokens in the CoreTokenStore, which is a pre-requisite for token validation. " +
                            "Update the rest-sts instance to persist issued tokens in the CTS, and functional token validation " +
                            "will be configured for token types issued by this sts instance.");
                }
            };
        }
    }

    /*
    Provides the IssuedTokenCancelOperation which is responsible for cancelling tokens issued by this sts instance.
    Will only return a functional IssuedTokenCancelOperation if the sts instance is configured to persist issued
    tokens in the CTS.
     */
    @Provides
    @Inject
    IssuedTokenCancelOperation getIssuedTokenCancelOperation(
            @Named(AMSTSConstants.ISSUED_TOKENS_PERSISTED_IN_CTS) boolean issuedTokensPersistedInCTS,
            IssuedTokenCancellerFactory issuedTokenCancellerFactory,
            TokenRequestMarshaller tokenRequestMarshaller,
            @Named(AMSTSConstants.ISSUED_TOKEN_TYPES) Set<TokenTypeId> cancelledTokenTypes) throws STSInitializationException {
        if (issuedTokensPersistedInCTS) {
            return new IssuedTokenCancelOperationImpl(issuedTokenCancellerFactory, tokenRequestMarshaller, cancelledTokenTypes);
        } else {
            return new IssuedTokenCancelOperation() {
                @Override
                public JsonValue cancelToken(RestSTSTokenCancellationInvocationState invocationState) throws TokenMarshalException, TokenCancellationException {
                    throw new TokenMarshalException(ResourceException.CONFLICT, "This rest-sts instance is not configured " +
                            "to persist tokens in the CoreTokenStore, which is a pre-requisite for token cancellation. " +
                            "Update the rest-sts instance to persist issued tokens in the CTS, and functional token cancellation " +
                            "will be configured for token types issued by this sts instance.");

                }
            };
        }
    }
    /**
     * The value corresponding to the Accept-API-Version header specifying the version of CREST services to consume. Note
     * that the rest-sts run-time consumes the rest authN (classes in the wss/disp package), the token generation
     * service (TokenServiceConsumerImpl), the service to obtain a principal from a session (PrincipalFromSessionImpl),
     * and the session invalidation service (AMSessionInvalidatorImpl).
     * All of these will specify the version returned below. If different versions need to be consumed, different strings
     * can be @Named and provided for the various clients.
     */

    @Provides
    @Singleton
    @Named(AMSTSConstants.CREST_VERSION_SESSION_SERVICE)
    String getSessionServiceVersion() {
        return RestSTSInjectorHolder.getInstance(Key.get(String.class,
                Names.named(AMSTSConstants.CREST_VERSION_SESSION_SERVICE)));
    }

    @Provides
    @Singleton
    @Named(AMSTSConstants.CREST_VERSION_AUTHN_SERVICE)
    String getAuthNServiceVersion() {
        return RestSTSInjectorHolder.getInstance(Key.get(String.class,
                Names.named(AMSTSConstants.CREST_VERSION_AUTHN_SERVICE)));
    }

    @Provides
    @Singleton
    @Named(AMSTSConstants.CREST_VERSION_TOKEN_GEN_SERVICE)
    String getTokenGenServiceVersion() {
        return RestSTSInjectorHolder.getInstance(Key.get(String.class,
                Names.named(AMSTSConstants.CREST_VERSION_TOKEN_GEN_SERVICE)));
    }

    @Provides
    @Singleton
    @Named(AMSTSConstants.CREST_VERSION_USERS_SERVICE)
    String getUsersServiceVersion() {
        return RestSTSInjectorHolder.getInstance(Key.get(String.class,
                Names.named(AMSTSConstants.CREST_VERSION_USERS_SERVICE)));
    }

    /*
    Required by the TokenServiceConsumerImpl, to specify the sts type which will allow the token generation
    service to look-up the appropriate sts instance state.
     */
    @Provides
    @Singleton
    AMSTSConstants.STSType getSTSType() {
        return AMSTSConstants.STSType.REST;
    }
}


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
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
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
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.cache.DefaultInMemoryTokenStore;

import org.apache.cxf.ws.security.tokenstore.TokenStore;

import org.apache.ws.security.message.token.UsernameToken;

import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.HttpURLConnectionWrapperFactory;
import org.forgerock.openam.sts.config.user.AuthTargetMapping;
import org.forgerock.openam.sts.JsonMarshaller;
import org.forgerock.openam.sts.XMLUtilities;
import org.forgerock.openam.sts.XMLUtilitiesImpl;
import org.forgerock.openam.sts.XmlMarshaller;
import org.forgerock.openam.sts.rest.RestSTS;
import org.forgerock.openam.sts.rest.RestSTSImpl;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;
import org.forgerock.openam.sts.rest.config.user.TokenTransformConfig;
import org.forgerock.openam.sts.rest.marshal.*;
import org.forgerock.openam.sts.rest.operation.TokenTransformFactory;
import org.forgerock.openam.sts.rest.operation.TokenTransformFactoryImpl;
import org.forgerock.openam.sts.rest.operation.TokenTranslateOperation;
import org.forgerock.openam.sts.rest.operation.TokenTranslateOperationImpl;
import org.forgerock.openam.sts.token.AMTokenParser;
import org.forgerock.openam.sts.token.AMTokenParserImpl;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCacheImpl;
import org.forgerock.openam.sts.token.UrlConstituentCatenator;
import org.forgerock.openam.sts.token.UrlConstituentCatenatorImpl;
import org.forgerock.openam.sts.token.model.OpenAMSessionToken;
import org.forgerock.openam.sts.token.model.OpenAMSessionTokenMarshaller;
import org.forgerock.openam.sts.token.model.OpenIdConnectIdToken;
import org.forgerock.openam.sts.token.model.OpenIdConnectIdTokenMarshaller;
import org.forgerock.openam.sts.token.provider.AuthnContextMapper;
import org.forgerock.openam.sts.token.provider.AuthnContextMapperImpl;
import org.forgerock.openam.sts.token.provider.TokenGenerationServiceConsumer;
import org.forgerock.openam.sts.token.provider.TokenGenerationServiceConsumerImpl;
import org.forgerock.openam.sts.token.validator.PrincipalFromSession;
import org.forgerock.openam.sts.token.validator.PrincipalFromSessionImpl;
import org.forgerock.openam.sts.token.validator.wss.AuthenticationHandler;
import org.forgerock.openam.sts.token.validator.wss.disp.CertificateAuthenticationRequestDispatcher;
import org.forgerock.openam.sts.token.validator.wss.disp.OpenIdConnectAuthenticationRequestDispatcher;
import org.forgerock.openam.sts.token.validator.wss.disp.TokenAuthenticationRequestDispatcher;
import org.forgerock.openam.sts.token.validator.wss.disp.UsernameTokenAuthenticationRequestDispatcher;
import org.forgerock.openam.sts.token.validator.wss.AuthenticationHandlerImpl;
import org.forgerock.openam.sts.token.validator.wss.UsernameTokenValidator;
import org.forgerock.openam.sts.token.validator.wss.url.AuthenticationUrlProviderImpl;
import org.forgerock.openam.sts.token.validator.wss.url.AuthenticationUrlProvider;

import java.security.cert.X509Certificate;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class defines all of the bindings for an instance of the REST-STS. The RestSTSInstanceConfig instance
 * passed to its ctor defines all of the state necessary to bind the elements necessary for a full REST-STS object
 * graph.
 */
public class RestSTSInstanceModule extends AbstractModule {

    private final RestSTSInstanceConfig stsInstanceConfig;
    private final Logger logger;


    public RestSTSInstanceModule(RestSTSInstanceConfig stsInstanceConfig) {
        this.stsInstanceConfig = stsInstanceConfig;
        logger = LoggerFactory.getLogger(AMSTSConstants.REST_STS_DEBUG_ID);
    }

    public void configure() {
        /*
        we want only one instance of the TokenStore shared among all token operations
        Perhaps this should be a provider - i.e. to leverage the ctor that takes a bus instance? TODO:
        Do I even want to bind a TokenStore - it seems there only to support SecurityContextTokens, and I don't
        set it in the TokenValidatorParameters or the TokenProviderParameters. This binding is candidate for removal.
         */
        bind(TokenStore.class).to(DefaultInMemoryTokenStore.class).in(Scopes.SINGLETON);

//        bind(AMTokenCache.class).to(AMTokenCacheImpl.class).in(Scopes.SINGLETON);
        bind(ThreadLocalAMTokenCache.class).to(ThreadLocalAMTokenCacheImpl.class).in(Scopes.SINGLETON);


        bind(AuthenticationUrlProvider.class)
                .to(AuthenticationUrlProviderImpl.class);

        bind(new TypeLiteral<TokenAuthenticationRequestDispatcher<UsernameToken>>(){})
                .to(UsernameTokenAuthenticationRequestDispatcher.class);

        bind(new TypeLiteral<AuthenticationHandler<UsernameToken>>(){})
                .to(new TypeLiteral<AuthenticationHandlerImpl<UsernameToken>>() {});

        bind(new TypeLiteral<TokenAuthenticationRequestDispatcher<OpenIdConnectIdToken>>(){})
                .to(OpenIdConnectAuthenticationRequestDispatcher.class);

        bind(new TypeLiteral<AuthenticationHandler<OpenIdConnectIdToken>>(){})
                .to(new TypeLiteral<AuthenticationHandlerImpl<OpenIdConnectIdToken>>() {});

        bind(new TypeLiteral<TokenAuthenticationRequestDispatcher<X509Certificate[]>>(){})
                .to(CertificateAuthenticationRequestDispatcher.class);
        bind(new TypeLiteral<AuthenticationHandler<X509Certificate[]>>() {})
                .to(new TypeLiteral<AuthenticationHandlerImpl<X509Certificate[]>>() {});


        bind(WebServiceContextFactory.class).to(CrestWebServiceContextFactoryImpl.class);
        bind(TokenRequestMarshaller.class).to(TokenRequestMarshallerImpl.class);
        bind(TokenResponseMarshaller.class).to(TokenResponseMarshallerImpl.class);
        bind(TokenTransformFactory.class).to(TokenTransformFactoryImpl.class);
        bind(TokenTranslateOperation.class).to(TokenTranslateOperationImpl.class);
        bind(AMTokenParser.class).to(AMTokenParserImpl.class);
        bind(PrincipalFromSession.class).to(PrincipalFromSessionImpl.class);

        bind(RestSTS.class).to(RestSTSImpl.class);

        /*
        bind the class that can issue XML Element instances encapsulating an OpenAM session Id.
         */
        bind(new TypeLiteral<XmlMarshaller<OpenAMSessionToken>>(){}).to(OpenAMSessionTokenMarshaller.class);
        bind(new TypeLiteral<XmlMarshaller<OpenIdConnectIdToken>>(){}).to(OpenIdConnectIdTokenMarshaller.class);
        bind(new TypeLiteral<JsonMarshaller<OpenIdConnectIdToken>>(){}).to(OpenIdConnectIdTokenMarshaller.class);

        bind(UrlConstituentCatenator.class).to(UrlConstituentCatenatorImpl.class);

        bind(TokenGenerationServiceConsumer.class).to(TokenGenerationServiceConsumerImpl.class);
        bind(XMLUtilities.class).to(XMLUtilitiesImpl.class);

        //bind the class responsible for producing HttpURLConnectionWrapper instances
        bind(HttpURLConnectionWrapperFactory.class).in(Scopes.SINGLETON);
    }

    /**
     * This method will provide the instance of the STSPropertiesMBean necessary both for the STS proper, and for the
     * CXF interceptor-set which enforces the SecurityPolicy bindings.
     *
     * It should be a singleton because this same instance is shared by all of the token operation instances, as well as
     * by the CXF interceptor-set
     */
    @Provides
    @Singleton
    @Inject
    STSPropertiesMBean getSTSProperties(Logger logger) {
        StaticSTSProperties stsProperties = new StaticSTSProperties();
        stsProperties.setIssuer(stsInstanceConfig.getIssuerName());
        return stsProperties;
    }

    /*
    Provides the WSSUsernameTokenValidator provider to the TokenTransformFactoryImpl
     */
    @Provides
    @Inject
    UsernameTokenValidator getWssUsernameTokenValidator(
            AuthenticationHandler<UsernameToken> authenticationHandler,
            Logger logger) {
        return new UsernameTokenValidator(logger, authenticationHandler);

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
    @Named(AMSTSConstants.REST_SUPPORTED_TOKEN_TRANSLATIONS)
    Set<TokenTransformConfig> getSupportedTokenTranslations() {
        return stsInstanceConfig.getSupportedTokenTranslations();
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

    /*
    Allows for a custom AuthnContextMapper to be plugged-in. This AuthnContextMapper provides a
    SAML2 AuthnContext class ref value given an input token and input token type.
     */
    @Provides
    @Inject
    AuthnContextMapper getAuthnContextMapper(Logger logger) {
        String customMapperClassName = stsInstanceConfig.getSaml2Config().getCustomAuthNContextMapperClassName();
        if (customMapperClassName == null) {
            return new AuthnContextMapperImpl(logger);
        } else {
            try {
                return Class.forName(customMapperClassName).asSubclass(AuthnContextMapper.class).newInstance();
            } catch (Exception e) {
                logger.error("Exception caught implementing custom AuthnContextMapper class " + customMapperClassName
                        + "; Returning default AuthnContextMapperImpl. The exception: " + e);
                return new AuthnContextMapperImpl(logger);
            }
        }
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

    /**
     * If a rest-sts instance is configured to support a token transformation with a x509 Certificate as an input token
     * type, and the OpenAM instance is deployed in a TLS-offloaded-environment, the TLS-offload-engines will be able
     * to provide the client's certificate, validated by a two-way-TLS handshake, to the rest-sts instance via a
     * header value only if the ip address of the host of the TLS-offload-engine is specified in the list below.
     * @return
     */
    @Provides
    @Named(AMSTSConstants.TLS_OFFLOAD_ENGINE_HOSTS)
    Set<String> getTlsOffloadEngineHostIpAddrs() {
        return stsInstanceConfig.getDeploymentConfig().getTlsOffloadEngineHostIpAddrs();
    }

    /**
     * The value corresponding to the Accept-API-Version header specifying the version of CREST services to consume. Note
     * that the rest-sts run-time consumes the rest authN (classes in the wss/disp package), the token generation
     * service (TokenGenerationServiceConsumerImpl), the service to obtain a principal from a session (PrincipalFromSessionImpl),
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
}


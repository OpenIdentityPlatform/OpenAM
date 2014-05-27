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
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sun.identity.shared.configuration.SystemPropertiesManager;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.cache.DefaultInMemoryTokenStore;

import org.apache.cxf.ws.security.tokenstore.TokenStore;

import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.message.token.UsernameToken;

import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.AuthTargetMapping;
import org.forgerock.openam.sts.JsonMarshaller;
import org.forgerock.openam.sts.XMLUtilities;
import org.forgerock.openam.sts.XMLUtilitiesImpl;
import org.forgerock.openam.sts.XmlMarshaller;
import org.forgerock.openam.sts.publish.STSInstanceConfigPersister;
import org.forgerock.openam.sts.rest.RestSTS;
import org.forgerock.openam.sts.rest.RestSTSImpl;
import org.forgerock.openam.sts.STSCallbackHandler;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;
import org.forgerock.openam.sts.rest.config.user.TokenTransformConfig;
import org.forgerock.openam.sts.rest.marshal.*;
import org.forgerock.openam.sts.rest.operation.TokenTransformFactory;
import org.forgerock.openam.sts.rest.operation.TokenTransformFactoryImpl;
import org.forgerock.openam.sts.rest.operation.TokenTranslateOperation;
import org.forgerock.openam.sts.rest.operation.TokenTranslateOperationImpl;
import org.forgerock.openam.sts.rest.publish.RestSTSInstanceConfigPersister;
import org.forgerock.openam.sts.token.*;
import org.forgerock.openam.sts.token.model.OpenAMSessionToken;
import org.forgerock.openam.sts.token.model.OpenAMSessionTokenMarshaller;
import org.forgerock.openam.sts.token.model.OpenIdConnectIdToken;
import org.forgerock.openam.sts.token.model.OpenIdConnectIdTokenMarshaller;
import org.forgerock.openam.sts.token.provider.AMTokenProvider;
import org.forgerock.openam.sts.token.provider.AuthnContextMapper;
import org.forgerock.openam.sts.token.provider.AuthnContextMapperImpl;
import org.forgerock.openam.sts.token.provider.TokenGenerationServiceConsumer;
import org.forgerock.openam.sts.token.provider.TokenGenerationServiceConsumerImpl;
import org.forgerock.openam.sts.token.validator.PrincipalFromSession;
import org.forgerock.openam.sts.token.validator.PrincipalFromSessionImpl;
import org.forgerock.openam.sts.token.validator.wss.AuthenticationHandler;
import org.forgerock.openam.sts.token.validator.wss.disp.OpenIdConnectAuthenticationRequestDispatcher;
import org.forgerock.openam.sts.token.validator.wss.disp.TokenAuthenticationRequestDispatcher;
import org.forgerock.openam.sts.token.validator.wss.disp.UsernameTokenAuthenticationRequestDispatcher;
import org.forgerock.openam.sts.token.validator.wss.AuthenticationHandlerImpl;
import org.forgerock.openam.sts.token.validator.wss.UsernameTokenValidator;
import org.forgerock.openam.sts.token.validator.wss.uri.AuthenticationUriProviderImpl;
import org.forgerock.openam.sts.token.validator.wss.uri.AuthenticationUriProvider;

import java.io.UnsupportedEncodingException;
import java.util.Properties;
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
         */
        bind(TokenStore.class).to(DefaultInMemoryTokenStore.class).in(Scopes.SINGLETON);

//        bind(AMTokenCache.class).to(AMTokenCacheImpl.class).in(Scopes.SINGLETON);
        bind(ThreadLocalAMTokenCache.class).to(ThreadLocalAMTokenCacheImpl.class).in(Scopes.SINGLETON);


        bind(AuthenticationUriProvider.class)
                .to(AuthenticationUriProviderImpl.class);

        bind(new TypeLiteral<TokenAuthenticationRequestDispatcher<UsernameToken>>(){})
                .to(UsernameTokenAuthenticationRequestDispatcher.class);

        bind(new TypeLiteral<AuthenticationHandler<UsernameToken>>(){})
                .to(new TypeLiteral<AuthenticationHandlerImpl<UsernameToken>>() {});

        bind(new TypeLiteral<TokenAuthenticationRequestDispatcher<OpenIdConnectIdToken>>(){})
                .to(OpenIdConnectAuthenticationRequestDispatcher.class);

        bind(new TypeLiteral<AuthenticationHandler<OpenIdConnectIdToken>>(){})
                .to(new TypeLiteral<AuthenticationHandlerImpl<OpenIdConnectIdToken>>() {});

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
        Needed by the AMTokenProvider.
         */
        bind(new TypeLiteral<XmlMarshaller<OpenAMSessionToken>>(){}).to(OpenAMSessionTokenMarshaller.class);
        bind(new TypeLiteral<XmlMarshaller<OpenIdConnectIdToken>>(){}).to(OpenIdConnectIdTokenMarshaller.class);
        bind(new TypeLiteral<JsonMarshaller<OpenIdConnectIdToken>>(){}).to(OpenIdConnectIdTokenMarshaller.class);

        bind(new TypeLiteral<STSInstanceConfigPersister<RestSTSInstanceConfig>>() {
        }).to(RestSTSInstanceConfigPersister.class);
        bind(UrlConstituentCatenator.class).to(UrlConstituentCatenatorImpl.class);

        bind(TokenGenerationServiceConsumer.class).to(TokenGenerationServiceConsumerImpl.class);
        bind(XMLUtilities.class).to(XMLUtilitiesImpl.class);
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
        stsProperties.setCallbackHandler(new STSCallbackHandler(stsInstanceConfig.getKeystoreConfig(), logger));
        Crypto crypto = null;
        try {
            crypto = CryptoFactory.getInstance(getEncryptionProperties());
        } catch (WSSecurityException e) {
            String message = "Exception caught initializing the CryptoFactory: " + e;
            logger.error(message, e);
            throw new IllegalStateException(message);
        }
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureUsername(stsInstanceConfig.getKeystoreConfig().getSignatureKeyAlias());

        return stsProperties;
    }

    private Properties getEncryptionProperties() {
        Properties properties = new Properties();
        properties.put(
                "org.apache.ws.security.crypto.provider", "org.apache.ws.security.components.crypto.Merlin"
        );
        String keystorePassword  = null;
        try {
            keystorePassword = new String(stsInstanceConfig.getKeystoreConfig().getKeystorePassword(), AMSTSConstants.UTF_8_CHARSET_ID);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unsupported string encoding for keystore password: " + e);
        }
        properties.put("org.apache.ws.security.crypto.merlin.keystore.password", keystorePassword);
        properties.put("org.apache.ws.security.crypto.merlin.keystore.file", stsInstanceConfig.getKeystoreConfig().getKeystoreFileName());
        properties.put("org.apache.ws.security.crypto.merlin.keystore.type", "jks");
        return properties;
    }

    /*
    Provides the WSSUsernameTokenValidator provider to the TokenOperationProviderImpl
     */
    @Provides
    @Inject
    UsernameTokenValidator getWssUsernameTokenValidator(
            AuthenticationHandler<UsernameToken> authenticationHandler,
            Logger logger) {
        return new UsernameTokenValidator(logger, authenticationHandler);

    }

    /*
    Provides the AMTokenProvider Provider to issue AMTokens.
     */
    @Provides
    @Inject
    AMTokenProvider getAMTokenProviderProvider(/*AMTokenCache tokenCache,*/
                                               ThreadLocalAMTokenCache tokenCache,
                                               XmlMarshaller<OpenAMSessionToken> sessionTokenMarshaller,
                                               org.slf4j.Logger logger) {
        return new AMTokenProvider(tokenCache, sessionTokenMarshaller, logger);
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
    @Named (AMSTSConstants.AM_DEPLOYMENT_URL)
    String amDeploymentUrl() {
        return stsInstanceConfig.getAMDeploymentUrl();
    }

    @Provides
    @Named (AMSTSConstants.REST_AUTHN_URI_ELEMENT)
    String restAuthnUriElement() {
        return stsInstanceConfig.getAMRestAuthNUriElement();
    }

    @Provides
    @Named (AMSTSConstants.REST_LOGOUT_URI_ELEMENT)
    String restLogoutUriElement() {
        return stsInstanceConfig.getAMRestLogoutUriElement();
    }

    @Provides
    @Named (AMSTSConstants.REST_ID_FROM_SESSION_URI_ELEMENT)
    String restAMTokenValidationUriElement() {
        return stsInstanceConfig.getAMRestIdFromSessionUriElement();
    }

    @Provides
    @Named(AMSTSConstants.REST_TOKEN_GENERATION_SERVICE_URI_ELEMENT)
    String tokenGenerationServiceUriElement() {
        return stsInstanceConfig.getAmRestTokenGenerationServiceUriElement();
    }

    @Provides
    @Named(AMSTSConstants.AM_SESSION_COOKIE_NAME)
    String getAMSessionCookieName() {
        return stsInstanceConfig.getAMSessionCookieName();
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

    @Provides
    @Named(AMSTSConstants.AM_REST_AUTHN_JSON_ROOT)
    String getJsonRoot() {
        return stsInstanceConfig.getJsonRestBase();
    }

    @Provides
    @Named(AMSTSConstants.STS_INSTANCE_ID)
    String getSTSInstanceId() {
        return stsInstanceConfig.getDeploymentConfig().getUriElement();
    }

    /*
    Allows for a custom AuthnContextMapper to be plugged-in. This AuthnContextMapper provides a
    SAML2 AuthnContext class ref value given an input token and input token type.
     */
    @Provides
    @Inject
    AuthnContextMapper getAuthnContextMapper(Logger logger) {
        String customMapperClassName = SystemPropertiesManager.get(AMSTSConstants.CUSTOM_STS_AUTHN_CONTEXT_MAPPER_PROPERTY);
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
}


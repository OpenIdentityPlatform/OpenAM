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

package org.forgerock.openam.sts.soap.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import javax.inject.Named;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.inject.Provider;

import com.sun.identity.shared.configuration.SystemPropertiesManager;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.cache.DefaultInMemoryTokenStore;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.sts.provider.SecurityTokenServiceProvider;
import org.apache.cxf.ws.security.sts.provider.operation.IssueOperation;
import org.apache.cxf.ws.security.sts.provider.operation.RenewOperation;
import org.apache.cxf.ws.security.sts.provider.operation.ValidateOperation;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.message.token.UsernameToken;
import org.forgerock.openam.sts.JsonMarshaller;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.XMLUtilities;
import org.forgerock.openam.sts.XMLUtilitiesImpl;
import org.forgerock.openam.sts.XmlMarshaller;
import org.forgerock.openam.sts.soap.STSEndpoint;
import org.forgerock.openam.sts.soap.SoapSTSCallbackHandler;
import org.forgerock.openam.sts.soap.publish.STSInstancePublisher;
import org.forgerock.openam.sts.soap.publish.STSInstancePublisherImpl;
import org.forgerock.openam.sts.soap.token.config.TokenIssueOperationProvider;
import org.forgerock.openam.sts.soap.token.config.TokenOperationFactory;
import org.forgerock.openam.sts.soap.token.config.TokenOperationFactoryImpl;
import org.forgerock.openam.sts.soap.token.config.TokenRenewOperationProvider;
import org.forgerock.openam.sts.soap.token.config.TokenValidateOperationProvider;
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
import org.forgerock.openam.sts.token.validator.wss.disp.TokenAuthenticationRequestDispatcher;
import org.forgerock.openam.sts.token.validator.wss.disp.UsernameTokenAuthenticationRequestDispatcher;
import org.forgerock.openam.sts.AMSTSConstants;

import org.forgerock.openam.sts.config.user.AuthTargetMapping;
import org.forgerock.openam.sts.soap.config.user.SoapSTSInstanceConfig;
import org.forgerock.openam.sts.token.validator.wss.AuthenticationHandlerImpl;
import org.forgerock.openam.sts.token.validator.wss.UsernameTokenValidator;
import org.forgerock.openam.sts.token.validator.wss.url.AuthenticationUrlProviderImpl;
import org.forgerock.openam.sts.token.validator.wss.url.AuthenticationUrlProvider;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the base module for the STS operations. It will take an instance of KeystoreConfig (ultimately provided by
 * the UI elements configuring STS state) to implement the @Provides method to populate an instance of the
 * STSPropertiesMBean necessary for all of the token operations.
 *
 */
public class SoapSTSInstanceModule extends AbstractModule {

    private final SoapSTSInstanceConfig stsInstanceConfig;
    private final Logger logger;


    public SoapSTSInstanceModule(SoapSTSInstanceConfig stsInstanceConfig) {
        this.stsInstanceConfig = stsInstanceConfig;
        logger = LoggerFactory.getLogger(AMSTSConstants.SOAP_STS_DEBUG_ID);
    }

    public void configure() {
//        bind(AMTokenCache.class).to(AMTokenCacheImpl.class).in(Scopes.SINGLETON);
        bind(ThreadLocalAMTokenCache.class).to(ThreadLocalAMTokenCacheImpl.class).in(Scopes.SINGLETON);
        bind(AMTokenParser.class).to(AMTokenParserImpl.class);

        //Bind the class used to publish STS instances. Leverages instance-specific state, so can't be global.
        bind(STSInstancePublisher.class).to(STSInstancePublisherImpl.class);

        //we want only one instance of the TokenStore shared among all token operations
        bind(TokenStore.class).to(DefaultInMemoryTokenStore.class).in(Scopes.SINGLETON);

        bind(AuthenticationUrlProvider.class)
                .to(AuthenticationUrlProviderImpl.class);

        bind(new TypeLiteral<TokenAuthenticationRequestDispatcher<UsernameToken>>(){})
                .to(UsernameTokenAuthenticationRequestDispatcher.class);

        bind(new TypeLiteral<AuthenticationHandler<UsernameToken>>(){})
                .to(new TypeLiteral<AuthenticationHandlerImpl<UsernameToken>>() {});

        /*
        bind the class that can issue XML Element instances encapsulating an OpenAM session Id.
         */
        bind(new TypeLiteral<XmlMarshaller<OpenAMSessionToken>>(){}).to(OpenAMSessionTokenMarshaller.class);

        bind(new TypeLiteral<XmlMarshaller<OpenIdConnectIdToken>>(){}).to(OpenIdConnectIdTokenMarshaller.class);
        bind(new TypeLiteral<JsonMarshaller<OpenIdConnectIdToken>>(){}).to(OpenIdConnectIdTokenMarshaller.class);

        //binding all of the Providers of the various sorts of operations
        bind(TokenOperationFactory.class).to(TokenOperationFactoryImpl.class).in(Scopes.SINGLETON);
        bind(IssueOperation.class).toProvider(TokenIssueOperationProvider.class);
        bind(ValidateOperation.class).toProvider(TokenValidateOperationProvider.class);
        bind(RenewOperation.class).toProvider(TokenRenewOperationProvider.class);
        bind(PrincipalFromSession.class).to(PrincipalFromSessionImpl.class);

        //bind the class defining the core STS functionality - necessary for its dependencies to be injected
        bind(SecurityTokenServiceProvider.class).to(STSEndpoint.class);
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
        stsProperties.setCallbackHandler(new SoapSTSCallbackHandler(stsInstanceConfig.getKeystoreConfig(), logger));
        Crypto crypto;
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
    /*
        These properties configure the web-service deployment, and are primarily referenced by the ws-security interceptors
        deployed as part of CXF. These interceptors are responsible for enforcing the security-policy bindings protecting
        the STS. To this end, various crypto objects are required.
     */
    @Provides
    @Named(AMSTSConstants.STS_WEB_SERVICE_PROPERTIES)
    @Inject
    Map<String, Object> getProperties(Provider<UsernameTokenValidator> usernameTokenValidatorProvider, Logger logger) throws WSSecurityException {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(SecurityConstants.CALLBACK_HANDLER, new SoapSTSCallbackHandler(stsInstanceConfig.getKeystoreConfig(), logger));
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        properties.put(SecurityConstants.ENCRYPT_CRYPTO, crypto);
        properties.put(SecurityConstants.SIGNATURE_CRYPTO, crypto);

        properties.put(SecurityConstants.SIGNATURE_USERNAME, stsInstanceConfig.getKeystoreConfig().getSignatureKeyAlias());

        properties.put("faultStackTraceEnabled", "true");
        properties.put("exceptionMessageCauseEnabled", "true");

        /*
        Plug-in the validator for Username Tokens and Certificate Tokens. These classes will be called to enforce SecurityPolicy bindings which specify
          UsernameTokens and X509Certificates.
          Removing the AMCertificateTokenValidator, as the AM Cert auth won't satisfy authenticating a user with a cert.
          See https://docs.google.com/a/forgerock.com/document/d/1qo2T19ooyCqhEo9Zh1GJibVb2iBRzzBGFFsYF9FIBU8/edit?usp=sharing
          for full details.
          TODO: it should also be configurable, on an STS-by-STS basis, which token-validators are plugged-into the SecurityPolicy
          enforcement - perhaps this should be determined by the SecurityPolicy binding associated with the STS deployment.
         */
        properties.put(SecurityConstants.USERNAME_TOKEN_VALIDATOR, usernameTokenValidatorProvider.get());
//        properties.put(SecurityConstants.SIGNATURE_TOKEN_VALIDATOR, certificateTokenValidatorProvider.get());

        return properties;
    }

    private Properties getEncryptionProperties() {
        Properties properties = new Properties();
        properties.put(
                "org.apache.ws.security.crypto.provider", "org.apache.ws.security.components.crypto.Merlin"
        );
        String keystorePassword;
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
    The STSInstancePublisherImpl needs this dependency injected.
     */
    @Provides
    SoapSTSInstanceConfig getStsInstanceConfig() {
        return stsInstanceConfig;
    }

    /*
    Ultimately, the Token*OperationProvider instances must know the set of Token{Validators | Providers | Renewers} to
    create for the Token*OperationProvider. This information will ultimately come from the user, and will be injected
    into the Token*OperationProvider classes. The methods below provide the state needed by Guice, leveraging the
    user-provided configuration.
     */
    @Provides
    @Named(AMSTSConstants.TOKEN_ISSUE_OPERATION)
    Set<TokenType> issueTokenTypes() {
        return stsInstanceConfig.getIssueTokenTypes();
    }

    @Provides
    @Named(AMSTSConstants.TOKEN_VALIDATE_OPERATION_STATUS)
    Set<TokenType> validateTokenStatusTypes() {
        return stsInstanceConfig.getValidateTokenStatusTypes();
    }

    @Provides
    @Named(AMSTSConstants.TOKEN_RENEW_OPERATION)
    Set<TokenType> renewTokenTypes() {
        return stsInstanceConfig.getRenewTokenTypes();
    }

    /*
    Provides the valid input->output mappings for the token transformations that can be realized by the validate
    method.
     */
    @Provides
    Map<TokenType, TokenType> getValidateTransformTypes() {
        return stsInstanceConfig.getValidateTokenTransformTypes();
    }

    /*
    Provides the WSSUsernameTokenValidator provider to the TokenOperationProviderImpl. Also is the UsernameTokenValidator
    used to validate UNTs in the SecurityPolicy enforcement context. TODO: need to configure instances of this class
    with state which will allow it to invalidate the sessions created - or perhaps to authenticate without creating a
    session. In other words, if a sts instance is protected by a SecurityPolicy binding which includes a UNT ProtectionToken,
    and then seeks to validate the UNT, a session will be created twice, once for SecurityPolicy enforcement, and once for
    UNT verification, and caching the sessionId in the ThreadLocalTokenCache the second time will cause it to emit a warning
    message. I only need to cache any interim AMSession instance as part of a token transformation - i.e. if the principal
    associated with the token validation operation is necessary for the token issue operation. So my UsernameTokenValidator
    needs the contextual information necessary to make this distinction.
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
    @Named (AMSTSConstants.AM_DEPLOYMENT_URL)
    String amDeploymentUrl() {
        return stsInstanceConfig.getDeploymentConfig().getAMDeploymentUrl();
    }


    @Provides
    @Named (AMSTSConstants.REST_AUTHN_URI_ELEMENT)
    String restAuthnUriElement() {
        return "/authenticate";
    }

    @Provides
    AuthTargetMapping authTargetMapping() {
        return stsInstanceConfig.getDeploymentConfig().getAuthTargetMapping();
    }

    @Provides
    @Named(AMSTSConstants.REST_LOGOUT_URI_ELEMENT)
    String restLogoutUriElement() {
        return "/sessions/?_action=logout";
    }

    @Provides
    @Named(AMSTSConstants.REST_ID_FROM_SESSION_URI_ELEMENT)
    String restAMTokenValidationUriElement() {
        return "/users/?_action=idFromSession";
    }

    @Provides
    @Named(AMSTSConstants.REST_TOKEN_GENERATION_SERVICE_URI_ELEMENT)
    String tokenGenerationServiceUriElement() {
        return "/sts-tokengen/issue?_action=issue";
    }

    @Provides
    @Named(AMSTSConstants.AM_SESSION_COOKIE_NAME)
    String getAMSessionCookieName() {
        /*
        This cannot come from the SystemPropertiesManager, as this is not running in the context of OpenAM. Perhaps
        set in SystemProperties, or some other properties file? Right now, just hard-code. TODO
         */
        return "iPlanetDirectoryPro";
//        return SystemPropertiesManager.get(Constants.AM_COOKIE_NAME);
    }

    @Provides
    @Named(AMSTSConstants.AM_REST_AUTHN_JSON_ROOT)
    String getJsonRoot() {
        return "/json";
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
        return LoggerFactory.getLogger(AMSTSConstants.SOAP_STS_DEBUG_ID);
    }
}


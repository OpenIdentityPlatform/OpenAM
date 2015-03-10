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

package org.forgerock.openam.sts.soap.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import javax.inject.Named;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.inject.Provider;

import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.cache.DefaultInMemoryTokenStore;
import org.apache.cxf.sts.token.delegation.TokenDelegationHandler;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.sts.provider.SecurityTokenServiceProvider;
import org.apache.cxf.ws.security.sts.provider.operation.IssueOperation;
import org.apache.cxf.ws.security.sts.provider.operation.ValidateOperation;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.dom.message.token.UsernameToken;
import org.apache.wss4j.dom.validate.NoOpValidator;
import org.apache.wss4j.dom.validate.SignatureTrustValidator;
import org.forgerock.openam.sts.DefaultHttpURLConnectionFactory;
import org.forgerock.openam.sts.HttpURLConnectionFactory;
import org.forgerock.openam.sts.HttpURLConnectionWrapperFactory;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.XMLUtilities;
import org.forgerock.openam.sts.XMLUtilitiesImpl;
import org.forgerock.openam.sts.XmlMarshaller;
import org.forgerock.openam.sts.config.user.TokenTransformConfig;
import org.forgerock.openam.sts.soap.STSEndpoint;
import org.forgerock.openam.sts.soap.SoapSTSCallbackHandler;
import org.forgerock.openam.sts.soap.token.config.TokenIssueOperationProvider;
import org.forgerock.openam.sts.soap.token.config.TokenOperationFactory;
import org.forgerock.openam.sts.soap.token.config.TokenOperationFactoryImpl;
import org.forgerock.openam.sts.soap.token.config.TokenValidateOperationProvider;
import org.forgerock.openam.sts.soap.token.delegation.TokenDelegationHandlersProvider;
import org.forgerock.openam.sts.token.AMTokenParser;
import org.forgerock.openam.sts.token.AMTokenParserImpl;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCacheImpl;
import org.forgerock.openam.sts.token.UrlConstituentCatenator;
import org.forgerock.openam.sts.token.UrlConstituentCatenatorImpl;
import org.forgerock.openam.sts.token.model.OpenAMSessionToken;
import org.forgerock.openam.sts.token.model.OpenAMSessionTokenMarshaller;
import org.forgerock.openam.sts.soap.token.provider.XmlTokenAuthnContextMapper;
import org.forgerock.openam.sts.soap.token.provider.XmlTokenAuthnContextMapperImpl;
import org.forgerock.openam.sts.token.provider.AMSessionInvalidator;
import org.forgerock.openam.sts.token.provider.AMSessionInvalidatorImpl;
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
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Properties;

import org.slf4j.Logger;

/**
 * This is the base module for the STS operations. It will take an instance of KeystoreConfig (ultimately provided by
 * the UI elements configuring STS state) to implement the @Provides method to populate an instance of the
 * STSPropertiesMBean necessary for all of the token operations.
 *
 */
public class SoapSTSInstanceModule extends AbstractModule {
    private final SoapSTSInstanceConfig stsInstanceConfig;

    public SoapSTSInstanceModule(SoapSTSInstanceConfig stsInstanceConfig) {
        this.stsInstanceConfig = stsInstanceConfig;
    }

    public void configure() {
        bind(ThreadLocalAMTokenCache.class).to(ThreadLocalAMTokenCacheImpl.class).in(Scopes.SINGLETON);
        bind(AMTokenParser.class).to(AMTokenParserImpl.class);

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

//        bind(new TypeLiteral<XmlMarshaller<OpenIdConnectIdToken>>(){}).to(OpenIdConnectIdTokenMarshaller.class);
//        bind(new TypeLiteral<JsonMarshaller<OpenIdConnectIdToken>>(){}).to(OpenIdConnectIdTokenMarshaller.class);

        //binding all of the Providers of the various sorts of operations
        bind(TokenOperationFactory.class).to(TokenOperationFactoryImpl.class).in(Scopes.SINGLETON);
        bind(IssueOperation.class).toProvider(TokenIssueOperationProvider.class);
        bind(ValidateOperation.class).toProvider(TokenValidateOperationProvider.class);
        //TODO: don't bind renewal - probably won't renew tokens
//        bind(RenewOperation.class).toProvider(TokenRenewOperationProvider.class);
        bind(PrincipalFromSession.class).to(PrincipalFromSessionImpl.class);

        //bind the class defining the core STS functionality - necessary for its dependencies to be injected
        bind(SecurityTokenServiceProvider.class).to(STSEndpoint.class);
        bind(UrlConstituentCatenator.class).to(UrlConstituentCatenatorImpl.class);

        /*
        Bind the client class used to speak to the TokenGenerationService
         */
        bind(TokenGenerationServiceConsumer.class).to(TokenGenerationServiceConsumerImpl.class);

        /*
        Bind a XMLUtilities class which encapsulates the shared XMLUtils class, so that static methods are not called
        directly.
         */
        bind(XMLUtilities.class).to(XMLUtilitiesImpl.class);

        /*
        Bind the XmlMarhsaller for marshalling OpenAMSessionTokens to/from xml.
         */
        bind(new TypeLiteral<XmlMarshaller<OpenAMSessionToken>>(){}).to(OpenAMSessionTokenMarshaller.class);

        /*
        Bind the class responsible for producing HttpURLConnectionWrapper instances, and the HttpURLConnectionFactory it consumes
         */
        bind(HttpURLConnectionFactory.class).to(DefaultHttpURLConnectionFactory.class).in(Scopes.SINGLETON);
        bind(HttpURLConnectionWrapperFactory.class).in(Scopes.SINGLETON);

        /*
        Bind the Provider responsible for providing the List<TokenDelegationHandler> required by the IssueOperation.
         */
        bind(new TypeLiteral<List<TokenDelegationHandler>>(){}).toProvider(TokenDelegationHandlersProvider.class);

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
    Map<String, Object> getProperties(Provider<UsernameTokenValidator> usernameTokenValidatorProvider,
                                      Provider<SignatureTrustValidator> signatureTrustValidatorProvider,
                                      Logger logger) throws WSSecurityException {
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
        properties.put(SecurityConstants.SIGNATURE_TOKEN_VALIDATOR, signatureTrustValidatorProvider.get());

        return properties;
    }

    private Properties getEncryptionProperties() {
        Properties properties = new Properties();
        properties.put(
                "org.apache.wss4j.crypto.provider", "org.apache.wss4j.common.crypto.Merlin"
        );
        String keystorePassword;
        try {
            keystorePassword = new String(stsInstanceConfig.getKeystoreConfig().getKeystorePassword(), AMSTSConstants.UTF_8_CHARSET_ID);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unsupported string encoding for keystore password: " + e);
        }
        properties.put("org.apache.wss4j.crypto.merlin.keystore.password", keystorePassword);
        properties.put("org.apache.wss4j.crypto.merlin.keystore.file", stsInstanceConfig.getKeystoreConfig().getKeystoreFileName());
        properties.put("org.apache.wss4j.crypto.merlin.keystore.type", "jks");
        return properties;
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

    /*
    Provides the valid input->output mappings for the token transformations that can be realized by the validate
    method.
     */
    @Provides
    Set<TokenTransformConfig> getValidateTransformTypes() {
        return stsInstanceConfig.getValidateTokenTranslations();
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
    needs the contextual information necessary to make this distinction. But does it make sense to define a UNT-based token
    transformation protected by a UNT-based SecurityPolicy binding?
     */
    @Provides
    @Inject
    UsernameTokenValidator getWssUsernameTokenValidator(
            AuthenticationHandler<UsernameToken> authenticationHandler,
            Logger logger) {
        return new UsernameTokenValidator(logger, authenticationHandler);

    }

    /*
    TODO: when to plug-in the SoapCertificateTokenValidator, which integrates x509 token authN into OpenAM?
     */
    @Provides
    SignatureTrustValidator getSignatureTrustValidator() {
        return new SignatureTrustValidator();
    }

    @Provides
    AuthTargetMapping authTargetMapping() {
        return stsInstanceConfig.getDeploymentConfig().getAuthTargetMapping();
    }

    @Provides
    @Named (AMSTSConstants.REALM)
    String realm() {
        return stsInstanceConfig.getDeploymentConfig().getRealm();
    }

    @Provides
    @Named (AMSTSConstants.AM_DEPLOYMENT_URL)
    String amDeploymentUrl() {
        return stsInstanceConfig.getDeploymentConfig().getAmDeploymentUrl();
    }

    /*
    This method is used to identify the soap sts instance. This identification is necessary when consuming
    the TokenGenerationService, as it is used to look-up the sts-instance-specific configuration state
    (crypto and SAML2 configurations) when issuing tokens for this sts instance. Note that this identifier
    does not have to be unique across rest and soap sts instances, as each will be represented by a different
    service-definition xml file and thus will be stored in a different DN by the SMS. The soap-sts will be identified
    by a combination of the realm, and the uri element within this realm. The uriElement defines the final endpoint, and
    it will always be deployed at a url which includes the realm.
    The value returned from RestSTSInstanceConfig#getDeploymentSubPath() will:
    1. determine the sub-path in the SoapSTSInstancePublisherImpl at which the new sts instance will be exposed
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
    Allows for a custom XmlTokenAuthnContextMapper to be plugged-in. This XmlTokenAuthnContextMapper provides a
    SAML2 AuthnContext class ref value given an input token and input token type.
     */
    @Provides
    @Inject
    XmlTokenAuthnContextMapper getAuthnContextMapper(Logger logger) {
        String customMapperClassName = stsInstanceConfig.getSaml2Config().getCustomAuthNContextMapperClassName();
        if (customMapperClassName == null) {
            return new XmlTokenAuthnContextMapperImpl(logger);
        } else {
            try {
                return Class.forName(customMapperClassName).asSubclass(XmlTokenAuthnContextMapper.class).newInstance();
            } catch (Exception e) {
                logger.error("Exception caught implementing custom JsonTokenAuthnContextMapper class " + customMapperClassName
                        + "; Returning default XmlTokenAuthnContextMapperImpl. The exception: " + e);
                return new XmlTokenAuthnContextMapperImpl(logger);
            }
        }
    }

    @Provides
    @Inject
    AMSessionInvalidator getAMSessionInvalidator(@Named(AMSTSConstants.AM_DEPLOYMENT_URL) String amDeploymentUrl,
                                                 @Named(AMSTSConstants.AM_REST_AUTHN_JSON_ROOT) String jsonRestRoot,
                                                 @Named(AMSTSConstants.REST_LOGOUT_URI_ELEMENT) String restLogoutUriElement,
                                                 @Named(AMSTSConstants.AM_SESSION_COOKIE_NAME) String amSessionCookieName,
                                                 @Named (AMSTSConstants.REALM) String realm,
                                                 UrlConstituentCatenator urlConstituentCatenator,
                                                 @Named(AMSTSConstants.CREST_VERSION_SESSION_SERVICE) String crestVersionSessionService,
                                                 HttpURLConnectionWrapperFactory connectionWrapperFactory,
                                                 Logger logger) {
        try {
            return new AMSessionInvalidatorImpl(amDeploymentUrl, jsonRestRoot, realm, restLogoutUriElement,
                    amSessionCookieName, urlConstituentCatenator, crestVersionSessionService, connectionWrapperFactory, logger);
        } catch (MalformedURLException e) {
            //TODO: throwing providers?
            throw new RuntimeException("URL elements passed to the AMSessionInvalidator constitute a malformed url: " + e, e);
        }
    }

    /*
    Required by the TokenGenerationServiceConsumerImpl, to specify the sts type which will allow the token generation
    service to look-up the appropriate sts instance state.
    */
    @Provides
    @Singleton
    AMSTSConstants.STSType getSTSType() {
        return AMSTSConstants.STSType.SOAP;
    }

    /*
    Returns the set token types for which TokenValidators will be plugged-in to validate ActAs or OnBehalfOf tokens.
    Consumed by the TokenIssueOperationProvider.
     */
    @Provides
    @Named(AMSTSConstants.DELEGATED_TOKEN_VALIDATORS)
    Set<TokenType> getDelegatedTokenValidators() {
        if (stsInstanceConfig.delegationRelationshipsSupported()) {
            return stsInstanceConfig.getSoapDelegationConfig().getValidatedDelegatedTokenTypes();
        } else {
            return Collections.emptySet();
        }
    }

    /*
    Required by the TokenDelegationHandlersProvider.
     */
    @Provides
    SoapSTSInstanceConfig getStsInstanceConfig() {
        return stsInstanceConfig;
    }
}


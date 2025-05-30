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
 * Portions Copyrighted 2025 3A-Systems LLC.
 */

package org.forgerock.openam.sts.soap.token.config;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.inject.Provider;
import org.apache.cxf.sts.token.canceller.TokenCanceller;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.apache.wss4j.dom.message.token.UsernameToken;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.XMLUtilities;
import org.forgerock.openam.sts.soap.bootstrap.SoapSTSAccessTokenProvider;
import org.forgerock.openam.sts.soap.token.canceller.OpenIdConnectTokenCanceller;
import org.forgerock.openam.sts.soap.token.canceller.SAML2TokenCanceller;
import org.forgerock.openam.sts.soap.token.provider.oidc.SoapOpenIdConnectTokenAuthnContextMapper;
import org.forgerock.openam.sts.soap.token.provider.oidc.SoapOpenIdConnectTokenAuthnMethodsReferencesMapper;
import org.forgerock.openam.sts.soap.token.provider.oidc.SoapOpenIdConnectTokenProvider;
import org.forgerock.openam.sts.soap.token.provider.saml2.Saml2XmlTokenAuthnContextMapper;
import org.forgerock.openam.sts.soap.token.provider.saml2.SoapSamlTokenProvider;
import org.forgerock.openam.sts.soap.token.validator.SimpleOpenIdConnectTokenValidator;
import org.forgerock.openam.sts.soap.token.validator.SimpleSAML2TokenValidator;
import org.forgerock.openam.sts.token.CTSTokenIdGenerator;
import org.forgerock.openam.sts.token.provider.AMSessionInvalidator;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.provider.TokenServiceConsumer;
import org.forgerock.openam.sts.soap.token.validator.SoapAMTokenValidator;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.token.validator.PrincipalFromSession;
import org.forgerock.openam.sts.token.validator.ValidationInvocationContext;
import org.forgerock.openam.sts.token.validator.AuthenticationHandler;
import org.forgerock.openam.sts.soap.token.validator.wss.OpenAMWSSUsernameTokenValidator;
import org.slf4j.Logger;

/**
 * A Factory class which provides all of the lower-level Token{Validator|Provider|Renewer|Canceller} classes which are
 * plugged-into the top-level operation classes corresponding to the fundamental operations defined in WS-Trust.
 */
public class TokenOperationFactoryImpl implements TokenOperationFactory {
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final PrincipalFromSession principalFromSession;
    private final TokenServiceConsumer tokenServiceConsumer;
    private final String stsInstanceId;
    private final String realm;
    private final XMLUtilities xmlUtilities;
    private final Saml2XmlTokenAuthnContextMapper saml2XmlTokenAuthnContextMapper;
    private final Provider<AMSessionInvalidator> amSessionInvalidatorProvider;
    private final SoapSTSAccessTokenProvider soapSTSAccessTokenProvider;
    private final AuthenticationHandler<UsernameToken> usernameTokenAuthenticationHandler;
    private final SoapOpenIdConnectTokenAuthnContextMapper soapOpenIdConnectTokenAuthnContextMapper;
    private final SoapOpenIdConnectTokenAuthnMethodsReferencesMapper soapOpenIdConnectTokenAuthnMethodsReferencesMapper;
    private final CTSTokenIdGenerator ctsTokenIdGenerator;
    private final Logger logger;

    /**
     * This class is a factory for the various operands (token providers, token validators, and the sub-components they require)
     * which are necessary to compose the higher-level ws-trust defined operations (Issue, Validate, Renew, etc).
     */
    @Inject
    public TokenOperationFactoryImpl(
            ThreadLocalAMTokenCache threadLocalAMTokenCache,
            PrincipalFromSession principalFromSession,
            TokenServiceConsumer tokenServiceConsumer,
            @Named(AMSTSConstants.STS_INSTANCE_ID) String stsInstanceId,
            @Named (AMSTSConstants.REALM) String realm,
            XMLUtilities xmlUtilities,
            Saml2XmlTokenAuthnContextMapper saml2XmlTokenAuthnContextMapper,
            Provider<AMSessionInvalidator> amSessionInvalidatorProvider,
            SoapSTSAccessTokenProvider soapSTSAccessTokenProvider,
            AuthenticationHandler<UsernameToken> usernameTokenAuthenticationHandler,
            SoapOpenIdConnectTokenAuthnContextMapper soapOpenIdConnectTokenAuthnContextMapper,
            SoapOpenIdConnectTokenAuthnMethodsReferencesMapper soapOpenIdConnectTokenAuthnMethodsReferencesMapper,
            CTSTokenIdGenerator ctsTokenIdGenerator,
            Logger logger) {
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.principalFromSession = principalFromSession;
        this.tokenServiceConsumer = tokenServiceConsumer;
        this.stsInstanceId = stsInstanceId;
        this.realm = realm;
        this.xmlUtilities = xmlUtilities;
        this.saml2XmlTokenAuthnContextMapper = saml2XmlTokenAuthnContextMapper;
        this.amSessionInvalidatorProvider = amSessionInvalidatorProvider;
        this.soapSTSAccessTokenProvider = soapSTSAccessTokenProvider;
        this.usernameTokenAuthenticationHandler = usernameTokenAuthenticationHandler;
        this.soapOpenIdConnectTokenAuthnContextMapper = soapOpenIdConnectTokenAuthnContextMapper;
        this.soapOpenIdConnectTokenAuthnMethodsReferencesMapper = soapOpenIdConnectTokenAuthnMethodsReferencesMapper;
        this.ctsTokenIdGenerator = ctsTokenIdGenerator;
        this.logger = logger;
    }
    /**
     * Returns a TokenValidator instance that can validate the status of the specified TokenType. This method will be
     * consumed to obtain the TokenValidators necessary to enforce SecurityPolicy bindings, and delegated token relationships.
     * @param validatedTokenType The type of token to be validated
     * @return The TokenValidation implementation which can validate the specified TokenType
     * @throws STSInitializationException if the TokenValidator cannot be instantiated
     */
    @Override
    public TokenValidator getTokenValidator(TokenType validatedTokenType, ValidationInvocationContext validationInvocationContext,
                                            boolean invalidateAMSession) throws STSInitializationException {
        if (TokenType.OPENAM.equals(validatedTokenType)) {
            return buildAMTokenValidator(validationInvocationContext, invalidateAMSession);
        } else if (TokenType.USERNAME.equals(validatedTokenType)) {
            return buildUsernameTokenValidator(validationInvocationContext, invalidateAMSession);
        } else {
            throw new STSInitializationException(ResourceException.BAD_REQUEST, "In TokenOperationFactory, unknown " +
                    "TokenType provided to obtain TokenValidator: " + validatedTokenType);
        }
    }

    /**
     *
     * @param issuedTokenType the type of token to be created
     * @return a TokenProvider implementation which can create the specified token type
     * @throws STSInitializationException if an unsupported token type is specified
     */
    @Override
    public TokenProvider getTokenProvider(TokenType issuedTokenType) throws STSInitializationException{
        if (TokenType.SAML2.equals(issuedTokenType)) {
            return  SoapSamlTokenProvider.builder()
                    .tokenGenerationServiceConsumer(tokenServiceConsumer)
                    .amSessionInvalidator(amSessionInvalidatorProvider.get())
                    .threadLocalAMTokenCache(threadLocalAMTokenCache)
                    .stsInstanceId(stsInstanceId)
                    .realm(realm)
                    .xmlUtilities(xmlUtilities)
                    .authnContextMapper(saml2XmlTokenAuthnContextMapper)
                    .soapSTSAccessTokenProvider(soapSTSAccessTokenProvider)
                    .logger(logger)
                    .build();
        } else if (TokenType.OPENIDCONNECT.equals(issuedTokenType)) {
            return SoapOpenIdConnectTokenProvider.builder()
                    .tokenGenerationServiceConsumer(tokenServiceConsumer)
                    .amSessionInvalidator(amSessionInvalidatorProvider.get())
                    .threadLocalAMTokenCache(threadLocalAMTokenCache)
                    .stsInstanceId(stsInstanceId)
                    .realm(realm)
                    .xmlUtilities(xmlUtilities)
                    .soapSTSAccessTokenProvider(soapSTSAccessTokenProvider)
                    .authenticationContextReferenceMapper(soapOpenIdConnectTokenAuthnContextMapper)
                    .authenticationMethodsReferencesMapper(soapOpenIdConnectTokenAuthnMethodsReferencesMapper)
                    .logger(logger)
                    .build();
        }
        throw new STSInitializationException(ResourceException.BAD_REQUEST, "Unhandled outputTokenType specified in " +
                "getTokenProviderForTransformOperation. OutputTokenType: " + issuedTokenType);
    }

    @Override
    public TokenValidator getSimpleTokenValidator(TokenType validatedTokenType) throws STSInitializationException {
        if (TokenType.SAML2.equals(validatedTokenType)) {
            return new SimpleSAML2TokenValidator(tokenServiceConsumer, soapSTSAccessTokenProvider, ctsTokenIdGenerator, xmlUtilities);
        }  else if (TokenType.OPENIDCONNECT.equals(validatedTokenType)) {
            return new SimpleOpenIdConnectTokenValidator(tokenServiceConsumer, soapSTSAccessTokenProvider, ctsTokenIdGenerator);
        }
        throw new STSInitializationException(ResourceException.INTERNAL_ERROR,
                "Unexpected TokenType passed to TokenOperationFactoryImpl#getSimpleTokenValidator: " + validatedTokenType);
    }

    @Override
    public TokenCanceller getTokenCanceller(TokenType cancelledTokenType) throws STSInitializationException {
        if (TokenType.SAML2.equals(cancelledTokenType)) {
            return new SAML2TokenCanceller(tokenServiceConsumer, soapSTSAccessTokenProvider, ctsTokenIdGenerator, xmlUtilities);
        }  else if (TokenType.OPENIDCONNECT.equals(cancelledTokenType)) {
            return new OpenIdConnectTokenCanceller(tokenServiceConsumer, soapSTSAccessTokenProvider, ctsTokenIdGenerator);
        }
        throw new STSInitializationException(ResourceException.INTERNAL_ERROR,
                "Unexpected TokenType passed to TokenOperationFactoryImpl#getTokenCanceller: " + cancelledTokenType);

    }

    private SoapAMTokenValidator buildAMTokenValidator(ValidationInvocationContext validationInvocationContext, boolean invalidateAMSession) {
        return new SoapAMTokenValidator(threadLocalAMTokenCache, principalFromSession, validationInvocationContext,
                invalidateAMSession, logger);
    }

    private org.apache.cxf.sts.token.validator.UsernameTokenValidator buildUsernameTokenValidator(
            ValidationInvocationContext validationInvocationContext, boolean invalidateAMSession) {
        org.apache.cxf.sts.token.validator.UsernameTokenValidator validator =
                new org.apache.cxf.sts.token.validator.UsernameTokenValidator();
        validator.setValidator(new OpenAMWSSUsernameTokenValidator(usernameTokenAuthenticationHandler,
                threadLocalAMTokenCache, validationInvocationContext, invalidateAMSession, logger));
        return validator;
    }
}

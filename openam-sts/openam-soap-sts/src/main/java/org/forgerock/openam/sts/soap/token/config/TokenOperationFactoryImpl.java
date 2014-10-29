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
 * Copyright © 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.soap.token.config;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.inject.Provider;
import org.apache.cxf.sts.token.provider.SAMLTokenProvider;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.renewer.SAMLTokenRenewer;
import org.apache.cxf.sts.token.renewer.TokenRenewer;
import org.apache.cxf.sts.token.validator.SAMLTokenValidator;
import org.apache.cxf.sts.token.validator.UsernameTokenValidator;
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.validator.AMTokenValidator;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.token.validator.PrincipalFromSession;
import org.slf4j.Logger;

/**
 * A Factory class which provides all of the lower-level Token{Validator|Provider|Renewer|Canceller} classes which are
 * plugged-into the top-level operation classes corresponding to the fundamental operations defined in WS-Trust.
 */
public class TokenOperationFactoryImpl implements TokenOperationFactory {
    private final Provider<org.forgerock.openam.sts.token.validator.wss.UsernameTokenValidator> wssUsernameTokenValidatorProvider;
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final PrincipalFromSession principalFromSession;
    private final Logger logger;

    @Inject
    public TokenOperationFactoryImpl(
            Provider<org.forgerock.openam.sts.token.validator.wss.UsernameTokenValidator> wssUsernameTokenValidatorProvider,
            ThreadLocalAMTokenCache threadLocalAMTokenCache,
            PrincipalFromSession principalFromSession,
            Logger logger) {
        this.wssUsernameTokenValidatorProvider = wssUsernameTokenValidatorProvider;
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.principalFromSession = principalFromSession;
        this.logger = logger;
    }
    /**
     * Returns a TokenValidator instance that can validate the status of the specified TokenType.
     *
     */
    public TokenValidator getTokenStatusValidatorForType(TokenType tokenType) throws STSInitializationException {
        if (TokenType.SAML2.equals(tokenType)) {
            //TODO: do we want to distinguish SAML1 and SAML2? The SAMLTokenProvider does both, but...
            return new SAMLTokenValidator();
        } else if (TokenType.OPENAM.equals(tokenType)) {
            return buildAMTokenValidator();
        } else if (TokenType.USERNAME.equals(tokenType)) {
            /*
            Here I want to return the sts.token.validator.UsernameTokenValidator, but I want to set the wss validator
            (to which the actual validation is delegated) to an instance of my WSSUsernameTokenValidator. But this guy needs
            the AuthenticationHandler injected - so I will provide a WSS TokenValidator provider to this class? Try the
            specific class, and then refactor as necessary
             */
            logger.debug("Plugging in the UsernameTokenValidator.");
            UsernameTokenValidator validator = new UsernameTokenValidator();
            validator.setValidator(wssUsernameTokenValidatorProvider.get());
            return validator;
        } else {
            throw new STSInitializationException(ResourceException.BAD_REQUEST, "In TokenOperationFactory, unknown TokenType provided to obtain status TokenValidator: "
                    + tokenType);
        }
    }

    /**
     * Called to obtain the set of TokenValidate instances to validate the initial token in the context of token transformation -
     * the validate operation called with a TokenType other than STATUS.
     * TODO: revisit the necessity of this method.
     */
    @Override
    public TokenValidator getTokenValidatorForTransformOperation(TokenType inputTokenType, TokenType outputTokenType) {
        return null;
    }

    /**
     * Returns a TokenProvider instance in the context of the Validate 'token transformation' operation.
     * @param inputTokenType The input TokenType - the to-be-validated token type
     * @param outputTokenType The TokenType which the TokenProvider should issue - the token returned as part of the
     *                        Validate operation.
     * @return A TokenProvider which can issue the TokenType specified in the outputTokenType parameter.
     */
    @Override
    public TokenProvider getTokenProviderForTransformOperation(TokenType inputTokenType, TokenType outputTokenType)
            throws STSInitializationException{
        if (TokenType.SAML2.equals(outputTokenType)) {
            return new SAMLTokenProvider();
        }
        throw new STSInitializationException(ResourceException.BAD_REQUEST, "Unhandled outputTokenType specified in " +
                "getTokenProviderForTransformOperation. OutputTokenType: " + outputTokenType);
    }


    public TokenProvider getTokenProviderForType(TokenType tokenType) throws STSInitializationException {
        if (TokenType.SAML2.equals(tokenType)) {
            return new SAMLTokenProvider();
        } else {
            //we are only supporting issuing SAML tokens at this point.
            throw new STSInitializationException(ResourceException.BAD_REQUEST,
                    "In TokenOperationFactory, unknown TokenType provided to obtain TokenProvider: "
                    + tokenType);
        }
    }

    @Override
    public TokenValidator getTokenValidatorForRenewal(TokenType tokenType) throws STSInitializationException{
        if (TokenType.SAML2.equals(tokenType)) {
            return new SAMLTokenValidator();
        } else if (TokenType.OPENAM.equals(tokenType)) {
            return buildAMTokenValidator();
        } else {
            throw new STSInitializationException(ResourceException.BAD_REQUEST,
                    "In TokenOperationFactory, unknown TokenType provided to obtain TokenValidator: "
                    + tokenType);
        }
    }

    public TokenRenewer getTokenRenewerForType(TokenType tokenType) throws  STSInitializationException {
        if (TokenType.SAML2.equals(tokenType)) {
            SAMLTokenRenewer samlTokenRenewer =  new SAMLTokenRenewer();
            /*
            TODO: if the line below is commented-out, current state does not allow this to work - investigate!!
            What happens is the STSCallbackHandler gets called with a WSPasswordCallback instance but with usage identifier
            of SECRET_KEY (9). The identifier is some long string - e.g. _E56EE83BEB8E9F63C9136683677368353, which is
            obviously not a key alias we are providing. So I am not quite sure what is going on. Need to do some more digging
            before the line below can be commented-out. (And it probably should be configurable).
             */
            samlTokenRenewer.setVerifyProofOfPossession(false);
            return samlTokenRenewer;
        } else {
            throw new STSInitializationException(ResourceException.BAD_REQUEST,
                    "In TokenOperationFactory, unknown TokenType provided to obtain TokenRenewer: "
                    + tokenType);
        }
    }

    private AMTokenValidator buildAMTokenValidator() {
        return new AMTokenValidator(threadLocalAMTokenCache, principalFromSession, logger);
    }
}

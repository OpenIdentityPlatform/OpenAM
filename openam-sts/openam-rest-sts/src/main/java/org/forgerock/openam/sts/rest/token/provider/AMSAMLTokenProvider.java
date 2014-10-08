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

package org.forgerock.openam.sts.rest.token.provider;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.security.AdminTokenAction;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.AMSTSRuntimeException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.XMLUtilities;
import org.forgerock.openam.sts.service.invocation.ProofTokenState;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;

import org.forgerock.openam.sts.token.provider.AuthnContextMapper;
import org.forgerock.openam.sts.token.provider.TokenGenerationServiceConsumer;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.security.AccessController;
import java.util.Map;

/**
 * This encapsulates logic to both create a SAML token, and to invalidate the interim OpenAM session object
 * generated from the preceding TokenValidation operation if the TokenTransform has been configured to invalidate
 * the interim OpenAM sessions generated from token validation. Note that thus the AMSessionInvalidator can be null.
 *
 * Note that this class may be a candidate for being moved to the common module, but the manner in which it pulls
 * SubjectConfirmation method specific data out of the additionalProperties in the TokenProviderParameters might not
 * work in the SOAP STS - this is TBD.
 *
 * See the TokenTranslateOperationImpl#buildTokenProviderParameters for details on how the additional state necessary
 * for issuing assertions of the various SubjectConfirmationMethod types are constituted in the additionalProperties
 * Map<String, Object> encapsulated in the TokenProviderParameters.
 */
public class AMSAMLTokenProvider implements TokenProvider {
    private final TokenGenerationServiceConsumer tokenGenerationServiceConsumer;
    private final AMSessionInvalidator amSessionInvalidator;
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final String stsInstanceId;
    private final String realm;
    private final XMLUtilities xmlUtilities;
    private final AuthnContextMapper authnContextMapper;
    private final Logger logger;

    /*
    ctor not injected as this class created by TokenTransformFactoryImpl
     */
    public AMSAMLTokenProvider(TokenGenerationServiceConsumer tokenGenerationServiceConsumer,
                               AMSessionInvalidator amSessionInvalidator,
                               ThreadLocalAMTokenCache threadLocalAMTokenCache,
                               String stsInstanceId,
                               String realm,
                               XMLUtilities xmlUtilities,
                               AuthnContextMapper authnContextMapper,
                               Logger logger) {
        this.tokenGenerationServiceConsumer = tokenGenerationServiceConsumer;
        this.amSessionInvalidator = amSessionInvalidator;
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.stsInstanceId = stsInstanceId;
        this.realm = realm;
        this.xmlUtilities = xmlUtilities;
        this.authnContextMapper = authnContextMapper;
        this.logger = logger;
    }

    /*
    The String tokenType passed here is obtained from the tokenType field of the TokenRequirements set in the
    TokenProviderParameters. This value is set in the TokenTranslateOperationImpl by calling calling name() on the
    specified desired TokenType.
     */
    @Override
    public boolean canHandleToken(String tokenType) {
        return canHandleToken(tokenType, null);
    }

    @Override
    public boolean canHandleToken(String tokenType, String realm) {
        return TokenType.SAML2.name().equals(tokenType);
    }

    @Override
    public TokenProviderResponse createToken(TokenProviderParameters tokenParameters) {
        try {
            final TokenProviderResponse tokenProviderResponse = new TokenProviderResponse();
            final Map<String, Object> additionalProperties = tokenParameters.getAdditionalProperties();
            final Object subjectConfirmationObject = additionalProperties.get(AMSTSConstants.SAML2_SUBJECT_CONFIRMATION_KEY);
            if (!(subjectConfirmationObject instanceof SAML2SubjectConfirmation)) {
                throw new AMSTSRuntimeException(ResourceException.INTERNAL_ERROR,
                        "No entry in additionalProperties in TokenProviderParameters corresponding to key "
                                + AMSTSConstants.SAML2_SUBJECT_CONFIRMATION_KEY);
            }
            final SAML2SubjectConfirmation subjectConfirmation = (SAML2SubjectConfirmation) subjectConfirmationObject;
            final String authNContextClassRef = getAuthnContextClassRef(additionalProperties);
            String assertion;
            try {
                assertion = getAssertion(authNContextClassRef, subjectConfirmation, additionalProperties);
            } catch (TokenCreationException e) {
                throw new AMSTSRuntimeException(e.getCode(), e.getMessage(), e);
            }
            Document assertionDocument = xmlUtilities.stringToDocumentConversion(assertion);
            if (assertionDocument ==  null) {
                logger.error("Could not turn assertion string returned from TokenGenerationService into DOM Document. " +
                        "The assertion string: " + assertion);
                throw new AMSTSRuntimeException(ResourceException.INTERNAL_ERROR,
                        "Could not turn assertion string returned from TokenGenerationService into DOM Document.");
            }
            final Element assertionElement = assertionDocument.getDocumentElement();
            tokenProviderResponse.setToken(assertionElement);
            tokenProviderResponse.setTokenId(assertionElement.getAttributeNS(null, SAML2Constants.ID));
            return tokenProviderResponse;
        } finally {
            if (amSessionInvalidator != null) {
                try {
                    amSessionInvalidator.invalidateAMSession(threadLocalAMTokenCache.getAMToken());
                } catch (Exception e) {
                    String message = "Exception caught invalidating interim AMSession: " + e;
                    logger.warn(message, e);
                /*
                The fact that the interim OpenAM session was not invalidated should not prevent a token from being issued, so
                I will not throw a AMSTSRuntimeException
                 */
                }
            }
        }
    }

    private String getAuthnContextClassRef(Map<String, Object> additionalProperties) {
        final Object tokenTypeObject = additionalProperties.get(AMSTSConstants.VALIDATED_TOKEN_TYPE_KEY);
        if (!(tokenTypeObject instanceof TokenType)) {
            throw new AMSTSRuntimeException(ResourceException.INTERNAL_ERROR,
                    "No entry in additionalProperties in TokenProviderParameters corresponding to key "
                            + AMSTSConstants.VALIDATED_TOKEN_TYPE_KEY);
        }
        final TokenType validatedTokenType = (TokenType)tokenTypeObject;

        final Object tokenObject = additionalProperties.get(AMSTSConstants.INPUT_TOKEN_STATE_KEY);
        if (!(tokenObject instanceof JsonValue)) {
            throw new AMSTSRuntimeException(ResourceException.INTERNAL_ERROR,
                    "No entry in additionalProperties in TokenProviderParameters corresponding to key "
                            + AMSTSConstants.INPUT_TOKEN_STATE_KEY);
        }
        return authnContextMapper.getAuthnContext(validatedTokenType, (JsonValue)tokenObject);
    }

    /*
    Throw TokenCreationException as threadLocalAMTokenCache.getAMToken throws a TokenCreationException. Let caller above
    map that to an AMSTSRuntimeException.
     */
    private String getAssertion(String authnContextClassRef, SAML2SubjectConfirmation subjectConfirmation,
                                Map<String, Object> additionalProperties) throws TokenCreationException {
        switch (subjectConfirmation) {
            case BEARER:
                return tokenGenerationServiceConsumer.getSAML2BearerAssertion(threadLocalAMTokenCache.getAMToken(),
                        stsInstanceId, realm, authnContextClassRef, getAdminToken());
            case SENDER_VOUCHES:
                return tokenGenerationServiceConsumer.getSAML2SenderVouchesAssertion(threadLocalAMTokenCache.getAMToken(),
                        stsInstanceId, realm, authnContextClassRef, getAdminToken());
            case HOLDER_OF_KEY:
                Object proofTokenStateObject = additionalProperties.get(AMSTSConstants.PROOF_TOKEN_STATE_KEY);
                if (!(proofTokenStateObject instanceof ProofTokenState)) {
                    throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                            "No ProofTokenState entry in additionalProperties map in TokenProvideProperties for "
                                    + AMSTSConstants.PROOF_TOKEN_STATE_KEY);
                }
                return tokenGenerationServiceConsumer.getSAML2HolderOfKeyAssertion(threadLocalAMTokenCache.getAMToken(),
                        stsInstanceId, realm, authnContextClassRef, (ProofTokenState)proofTokenStateObject, getAdminToken());
        }
        throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                "Unexpected SAML2SubjectConfirmation in AMSAMLTokenProvider: " + subjectConfirmation);
    }

    private String getAdminToken() {
        return AccessController.doPrivileged(AdminTokenAction.getInstance()).getTokenID().toString();
    }
}

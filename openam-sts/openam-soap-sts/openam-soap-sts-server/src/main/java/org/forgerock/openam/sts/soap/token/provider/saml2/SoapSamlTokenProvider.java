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
* Copyright 2014-2015 ForgeRock AS.
*/

package org.forgerock.openam.sts.soap.token.provider.saml2;

import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.request.ReceivedKey;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.apache.ws.security.WSConstants;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSRuntimeException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.XMLUtilities;
import org.forgerock.openam.sts.soap.token.provider.SoapTokenProviderBase;
import org.forgerock.openam.sts.user.invocation.ProofTokenState;
import org.forgerock.openam.sts.soap.bootstrap.SoapSTSAccessTokenProvider;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.provider.AMSessionInvalidator;
import org.forgerock.openam.sts.token.provider.TokenGenerationServiceConsumer;
import org.forgerock.openam.sts.token.validator.ValidationInvocationContext;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.security.cert.X509Certificate;
import java.util.UUID;

/**
 * The TokenProvider responsible for issuing SAML2 assertions by consuming the TokenGenerationService.
 */
public class SoapSamlTokenProvider extends SoapTokenProviderBase {
    public static class SoapSamlTokenProviderBuilder {
        private TokenGenerationServiceConsumer tokenGenerationServiceConsumer;
        private AMSessionInvalidator amSessionInvalidator;
        private ThreadLocalAMTokenCache threadLocalAMTokenCache;
        private String stsInstanceId;
        private String realm;
        private XMLUtilities xmlUtilities;
        private Saml2XmlTokenAuthnContextMapper authnContextMapper;
        private SoapSTSAccessTokenProvider soapSTSAccessTokenProvider;
        private Logger logger;

        public SoapSamlTokenProviderBuilder tokenGenerationServiceConsumer(TokenGenerationServiceConsumer tokenGenerationServiceConsumer) {
            this.tokenGenerationServiceConsumer = tokenGenerationServiceConsumer;
            return this;
        }

        public SoapSamlTokenProviderBuilder amSessionInvalidator(AMSessionInvalidator amSessionInvalidator) {
            this.amSessionInvalidator = amSessionInvalidator;
            return this;
        }

        public SoapSamlTokenProviderBuilder threadLocalAMTokenCache(ThreadLocalAMTokenCache threadLocalAMTokenCache) {
            this.threadLocalAMTokenCache = threadLocalAMTokenCache;
            return this;
        }

        public SoapSamlTokenProviderBuilder stsInstanceId(String stsInstanceId) {
            this.stsInstanceId = stsInstanceId;
            return this;
        }

        public SoapSamlTokenProviderBuilder realm(String realm) {
            this.realm = realm;
            return this;
        }

        public SoapSamlTokenProviderBuilder xmlUtilities(XMLUtilities xmlUtilities) {
            this.xmlUtilities = xmlUtilities;
            return this;
        }

        public SoapSamlTokenProviderBuilder authnContextMapper(Saml2XmlTokenAuthnContextMapper authnContextMapper) {
            this.authnContextMapper = authnContextMapper;
            return this;
        }

        public SoapSamlTokenProviderBuilder soapSTSAccessTokenProvider(SoapSTSAccessTokenProvider soapSTSAccessTokenProvider) {
            this.soapSTSAccessTokenProvider = soapSTSAccessTokenProvider;
            return this;
        }

        public SoapSamlTokenProviderBuilder logger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public SoapSamlTokenProvider build() {
            return new SoapSamlTokenProvider(this);
        }
    }

    private final TokenGenerationServiceConsumer tokenGenerationServiceConsumer;
    private final AMSessionInvalidator amSessionInvalidator;
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final String stsInstanceId;
    private final String realm;
    private final Saml2XmlTokenAuthnContextMapper authnContextMapper;

    /*
    ctor not injected as this class created by TokenOperationFactoryImpl
     */
    private SoapSamlTokenProvider(SoapSamlTokenProviderBuilder builder) {
        super(builder.soapSTSAccessTokenProvider, builder.xmlUtilities, builder.logger);
        this.tokenGenerationServiceConsumer = builder.tokenGenerationServiceConsumer;
        this.amSessionInvalidator = builder.amSessionInvalidator;
        this.threadLocalAMTokenCache = builder.threadLocalAMTokenCache;
        this.stsInstanceId = builder.stsInstanceId;
        this.realm = builder.realm;
        this.authnContextMapper = builder.authnContextMapper;
    }

    public static SoapSamlTokenProviderBuilder builder() {
        return new SoapSamlTokenProviderBuilder();
    }

    /**
     * @see org.apache.cxf.sts.token.provider.TokenProvider
     * Note that I got rid of realm support as defined by the CXF-STS. See
     * @see org.apache.cxf.sts.token.provider.SAMLTokenProvider#canHandleToken for details on the realm support.
     */
    @Override
    public boolean canHandleToken(String tokenType) {
        return canHandleToken(tokenType, null);
    }

    /**
     * @see org.apache.cxf.sts.token.provider.TokenProvider
     * Note that I got rid of realm support as defined by the CXF-STS. See
     * @see org.apache.cxf.sts.token.provider.SAMLTokenProvider#canHandleToken for details on the realm support.
     */
    @Override
    public boolean canHandleToken(String tokenType, String realm) {
        return (WSConstants.WSS_SAML2_TOKEN_TYPE.equals(tokenType) || WSConstants.SAML2_NS.equals(tokenType));
    }

    /**
     * @see org.apache.cxf.sts.token.provider.TokenProvider
     */
    @Override
    public TokenProviderResponse createToken(TokenProviderParameters tokenProviderParameters) {
        try {
            final TokenProviderResponse tokenProviderResponse = new TokenProviderResponse();
            final SAML2SubjectConfirmation subjectConfirmation = determineSubjectConfirmation(tokenProviderParameters);
            final SoapTokenProviderBase.AuthenticationContextState authenticationContextState =
                    getAuthenticationContextState(tokenProviderParameters);

            final String authNContextClassRef = authnContextMapper.getAuthnContext(
                                                        authenticationContextState.getAuthenticatedTokenType(),
                                                        authenticationContextState.getAuthenticatedToken());
            ProofTokenState proofTokenState = null;
            if (SAML2SubjectConfirmation.HOLDER_OF_KEY.equals(subjectConfirmation)) {
                proofTokenState = getProofTokenState(tokenProviderParameters);
            }
            String assertion;
            try {
                assertion = getAssertion(authNContextClassRef, subjectConfirmation, proofTokenState);
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
            final String tokenId = assertionElement.getAttributeNS(null, "ID");
            /*
            The tokenId cannot be null or empty because a reference to the issued token is created using this id in the wss
            security header in the RequestSecurityTokenResponse. A null or empty id will generate a cryptic error in the cxf
            runtime. And if we are dealing with an encrypted assertion, there is no ID attribute, so in this case,
            a random uuid should be generated, as I believe the id serves only to refer to the token within the
            security header, and does not have to be connected to the token itself. An encrypted SAML2 assertion only
            contains some information on the encryption method, the symmetric key used for encryption, itself encrypted
            with the recipient's public key, and the encrypted assertion. So if no ID attribute is present, we are dealing
            with an encrypted assertion, and will generate a random UUID to serve as the key id.
            */
            if (StringUtils.isEmpty(tokenId)) {
                tokenProviderResponse.setTokenId(UUID.randomUUID().toString());
            } else {
                tokenProviderResponse.setTokenId(tokenId);
            }
            return tokenProviderResponse;
        } finally {
            try {
                amSessionInvalidator.invalidateAMSessions(threadLocalAMTokenCache.getToBeInvalidatedAMSessionIds());
            } catch (Exception e) {
                String message = "Exception caught invalidating interim AMSession in SoapSamlTokenProvider: " + e;
                logger.warn(message, e);
                /*
                The fact that the interim OpenAM session was not invalidated should not prevent a token from being issued, so
                I will not throw a AMSTSRuntimeException
                */
            }
        }
    }

    /**
     *
     * @param tokenProviderParameters The TokenProviderParameters corresponding the the RST invocation.
     * @return the SAM2SubjectConfirmation instance corresponding to teh KeyType specified in the RST invocation, considering
     * the OnBehalfOf/ActAs element, if present.
     * @throws AMSTSRuntimeException if an appropriate SubjectConfirmation value cannot be obtained.
     */
    private SAML2SubjectConfirmation determineSubjectConfirmation(TokenProviderParameters tokenProviderParameters) throws AMSTSRuntimeException {
        String keyType = tokenProviderParameters.getKeyRequirements().getKeyType();
        if (STSConstants.BEARER_KEY_KEYTYPE.equals(keyType)) {
            return SAML2SubjectConfirmation.BEARER;
        } else if (STSConstants.PUBLIC_KEY_KEYTYPE.equals(keyType)) {
            /*
            The OnBehalfOf element defined in WS-Trust is used to indicate that the STS is being asked to issue a
            token OnBehalfOf another party, which is idiom which matches the STS issuing a SAML2 SV assertion. It appears
            that the ActAs element also has the same semantics. See the following links for details:
            http://owulff.blogspot.com/2012/03/saml-sender-vouches-use-case.html
            http://coheigea.blogspot.com/2011/08/ws-trust-14-support-in-cxf.html
             */
            if ((tokenProviderParameters.getTokenRequirements().getOnBehalfOf() != null) ||
                    (tokenProviderParameters.getTokenRequirements().getActAs() != null)) {
                return SAML2SubjectConfirmation.SENDER_VOUCHES;
            } else {
                return SAML2SubjectConfirmation.HOLDER_OF_KEY;
            }
        } else if (STSConstants.SYMMETRIC_KEY_KEYTYPE.equals(keyType)) {
            /*
            The TokenGenerationService does not, as of now, support HoK assertions with KeyInfo state in the SubjectConfirmationData
            corresponding to symmetric keys.
             */
            throw new AMSTSRuntimeException(ResourceException.NOT_SUPPORTED, "Issuing SAML2 assertions with symmetric KeyInfo" +
                    "in the SubjectConfirmationData of HoK assertions is currently not supported.");
        } else {
            String message = "Unexpected keyType in SoapSamlTokenProvider#determineSubjectConfirmation: " + keyType;
            logger.error(message);
            throw new AMSTSRuntimeException(ResourceException.BAD_REQUEST, message);
        }
    }

    /**
     *
     * @param tokenProviderParameters The TokenProviderParameters corresponding to the RST invocation
     * @return The ProofTokenState necessary for HoK assertions.
     * @throws AMSTSRuntimeException if the ProofTokenState cannot be obtained from the request, or the X509Certificate
     * state cannot be successfully constructed.
     */
    private ProofTokenState getProofTokenState(TokenProviderParameters tokenProviderParameters) throws AMSTSRuntimeException {
        ReceivedKey receivedKey = tokenProviderParameters.getKeyRequirements().getReceivedKey();
        X509Certificate certificate = receivedKey.getX509Cert();
        if (certificate == null) {
            String exceptionMessage = "The ReceivedKey instance in the KeyRequirements has a null X509Cert. Thus the " +
                    "ProofTokenState necessary to consume the TokenGenerationService cannot be created.";
            logger.error(exceptionMessage + " PublicKey in the ReceivedToken: " + receivedKey.getPublicKey());
            throw new AMSTSRuntimeException(ResourceException.BAD_REQUEST, exceptionMessage);
        }
        try {
            return ProofTokenState.builder().x509Certificate(certificate).build();
        } catch (TokenMarshalException e) {
            String message = "In SoapSamlTokenProvider#getAssertion, could not marshal X509Cert in ReceivedKey " +
                    "into ProofTokenState: " + e;
            logger.error(message, e);
            throw new AMSTSRuntimeException(ResourceException.BAD_REQUEST, message);
        }
    }


    private String getAssertion(String authnContextClassRef, SAML2SubjectConfirmation subjectConfirmation,
                                ProofTokenState proofTokenState) throws TokenCreationException {
        /*
            Throw TokenCreationException as threadLocalAMTokenCache.getAMToken throws a TokenCreationException. Let caller above
            map that to an AMSTSRuntimeException.
        */
        String consumptionToken = null;
        try {
            consumptionToken = getTokenGenerationServiceConsumptionToken();
            switch (subjectConfirmation) {
                case BEARER:
                    return tokenGenerationServiceConsumer.getSAML2BearerAssertion(
                            threadLocalAMTokenCache.getSessionIdForContext(ValidationInvocationContext.SOAP_SECURITY_POLICY),
                            stsInstanceId, realm, authnContextClassRef, consumptionToken);
                case SENDER_VOUCHES:
                    return tokenGenerationServiceConsumer.getSAML2SenderVouchesAssertion(
                            threadLocalAMTokenCache.getSessionIdForContext(ValidationInvocationContext.SOAP_TOKEN_DELEGATION),
                            stsInstanceId, realm, authnContextClassRef, consumptionToken);
                case HOLDER_OF_KEY:
                    return tokenGenerationServiceConsumer.getSAML2HolderOfKeyAssertion(
                            threadLocalAMTokenCache.getSessionIdForContext(ValidationInvocationContext.SOAP_SECURITY_POLICY),
                            stsInstanceId, realm, authnContextClassRef, proofTokenState, consumptionToken);
            }
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Unexpected SAML2SubjectConfirmation in AMSAMLTokenProvider: " + subjectConfirmation);
        } finally {
            if (consumptionToken != null) {
                invalidateTokenGenerationServiceConsumptionToken(consumptionToken);
            }
        }
    }

}

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

package org.forgerock.openam.sts.soap.token.provider;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.request.ReceivedKey;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.apache.ws.security.message.token.UsernameToken;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSRuntimeException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.XMLUtilities;
import org.forgerock.openam.sts.XmlMarshaller;
import org.forgerock.openam.sts.service.invocation.ProofTokenState;
import org.forgerock.openam.sts.soap.bootstrap.SoapSTSAccessTokenProvider;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.model.OpenAMSessionToken;
import org.forgerock.openam.sts.token.provider.AMSessionInvalidator;
import org.forgerock.openam.sts.token.provider.TokenGenerationServiceConsumer;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.security.cert.X509Certificate;
import java.util.List;

/**
 * The TokenProvider responsible for issuing SAML2 assertions by consuming the TokenGenerationService.
 */
public class SoapSamlTokenProvider implements TokenProvider {
    public static class SoapSamlTokenProviderBuilder {
        private TokenGenerationServiceConsumer tokenGenerationServiceConsumer;
        private AMSessionInvalidator amSessionInvalidator;
        private ThreadLocalAMTokenCache threadLocalAMTokenCache;
        private String stsInstanceId;
        private String realm;
        private XMLUtilities xmlUtilities;
        private XmlTokenAuthnContextMapper authnContextMapper;
        private XmlMarshaller<OpenAMSessionToken> amSessionTokenXmlMarshaller;
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

        public SoapSamlTokenProviderBuilder authnContextMapper(XmlTokenAuthnContextMapper authnContextMapper) {
            this.authnContextMapper = authnContextMapper;
            return this;
        }

        public SoapSamlTokenProviderBuilder amSessionTokenXmlMarshaller(XmlMarshaller<OpenAMSessionToken> amSessionTokenXmlMarshaller) {
            this.amSessionTokenXmlMarshaller = amSessionTokenXmlMarshaller;
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
    private final XMLUtilities xmlUtilities;
    private final XmlTokenAuthnContextMapper authnContextMapper;
    private final XmlMarshaller<OpenAMSessionToken> amSessionTokenXmlMarshaller;
    private final SoapSTSAccessTokenProvider soapSTSAccessTokenProvider;
    private final Logger logger;

    /*
    ctor not injected as this class created by TokenOperationFactoryImpl
     */
    private SoapSamlTokenProvider(SoapSamlTokenProviderBuilder builder) {
        this.tokenGenerationServiceConsumer = builder.tokenGenerationServiceConsumer;
        this.amSessionInvalidator = builder.amSessionInvalidator;
        this.threadLocalAMTokenCache = builder.threadLocalAMTokenCache;
        this.stsInstanceId = builder.stsInstanceId;
        this.realm = builder.realm;
        this.xmlUtilities = builder.xmlUtilities;
        this.authnContextMapper = builder.authnContextMapper;
        this.amSessionTokenXmlMarshaller = builder.amSessionTokenXmlMarshaller;
        this.soapSTSAccessTokenProvider = builder.soapSTSAccessTokenProvider;
        this.logger = builder.logger;
    }

    public static SoapSamlTokenProviderBuilder builder() {
        return new SoapSamlTokenProviderBuilder();
    }

    /**
     * @see org.apache.cxf.sts.token.provider.TokenProvider
     * Note that I got rid of realm support as defined by the CXF-STS. See
     * org.apache.cxf.sts.token.provider.SAMLTokenProvider#canHandleToken for details on the realm support.
     */
    @Override
    public boolean canHandleToken(String tokenType) {
        return canHandleToken(tokenType, null);
    }

    /**
     * @see org.apache.cxf.sts.token.provider.TokenProvider
     * Note that I got rid of realm support as defined by the CXF-STS. See
     * org.apache.cxf.sts.token.provider.SAMLTokenProvider#canHandleToken for details on the realm support.
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
            final String authNContextClassRef = getAuthnContextClassRef(tokenProviderParameters);
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
            tokenProviderResponse.setTokenId(assertionElement.getAttributeNS(null, "ID"));
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

    /*
    The TokenGenerationService needs to the SAML2SubjectConfirmation as a parameter. This method will return the appropriate
    SubjectConfirmation value, depending upon the KeyType specified in the RST invocation, and also the OnBehalfOf value.
     */
    private SAML2SubjectConfirmation determineSubjectConfirmation(TokenProviderParameters tokenProviderParameters)  {
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

    /*
    Will return the ProofTokenState necessary for HoK assertions.
     */
    private ProofTokenState getProofTokenState(TokenProviderParameters tokenProviderParameters) {
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

    /*
    This method must return the AuthnContextClassRef, a set of values defined in SAML2, included in the assertion generated
    by the TokenGenerationService, which specify the authentication performed as part of issuing the assertion. Essentially
    this value tells the relying party how the assertion subject was authenticated.

    A SAML2 assertion will be generated under two circumstances:
    1. as part of token transformation defined in the validate operation
    2. as part of an issue operation

    For case #1, the type of the validated token must be determined - accessed via the TokenRequirements in the TokenProviderParameters
    For case #2, I imagine that it is possible to obtain the validated token from the Security-Policy enforcing interceptors
    traversed during the Issue operation invocation.

    Firstly, I have to determine what invocation I am dealing with - presumably this is possible by looking at the
    tokens in the TokenRequirements.
     */
    private String getAuthnContextClassRef(TokenProviderParameters tokenProviderParameters) {
        TokenRequirements tokenRequirements = tokenProviderParameters.getTokenRequirements();
        if (tokenRequirements.getRenewTarget() != null) {
            return getAuthnContextClassRefForReceivedToken(tokenRequirements.getRenewTarget());
        } else if (tokenRequirements.getValidateTarget() != null) {
            return getAuthnContextClassRefForReceivedToken(tokenRequirements.getValidateTarget());
        } else if (tokenRequirements.getCancelTarget() != null) {
            /*
            Should not enter this block, as Cancel operation was never bound in the STSEndpoint. Log an throw an exception
            if this occurs, as it is unexpected.
             */
            String message = "Unexpected state in SoapSamlTokenProvider: TokenProviderParameters has a non-null cancelTarget " +
                    "in the TokenRequirments. A cancel operation is not bound, so this state is unexpected!";
            logger.error(message);
            throw new AMSTSRuntimeException(ResourceException.INTERNAL_ERROR, message);
        } else {
            /*
            Here we must be dealing with an Issue operation, so I need to obtain the token validated by the SecurityPolicy
            bindings protecting the issue operation, or from the ActAs/OnBehalfOf token, if present. The ActAs/OnBehalfOf
            token has precedence, as it is the identity of this token for which an assertion will be generated.
             */
            if ((tokenProviderParameters.getTokenRequirements().getOnBehalfOf() != null) ||
                    (tokenProviderParameters.getTokenRequirements().getActAs() != null)) {
                return getAuthnContextClassRefFromDelegatedContext(tokenProviderParameters);
            } else {
                return getAuthnContextFromSecurityPolicyBindings(tokenProviderParameters);
            }
        }
    }

    private String getAuthnContextClassRefFromDelegatedContext(TokenProviderParameters tokenProviderParameters) {
        ReceivedToken delgatedToken;
        if (tokenProviderParameters.getTokenRequirements().getActAs() != null) {
            delgatedToken = tokenProviderParameters.getTokenRequirements().getActAs();
        } else if (tokenProviderParameters.getTokenRequirements().getOnBehalfOf() != null){
            delgatedToken = tokenProviderParameters.getTokenRequirements().getOnBehalfOf();
        } else {
            throw new AMSTSRuntimeException(ResourceException.INTERNAL_ERROR,
                    "Error in SoapSamlTokenProvider#getAuthnContextClassRefFromDelegatedContext - neither ActAs nor " +
                            "OnBehalfOf tokens found!");
        }
        final TokenType tokenType = parseTokenTypeFromDelegatedReceivedToken(delgatedToken);
        final Element tokenElement = getDelegatedReceivedTokenElement(delgatedToken);
        return authnContextMapper.getAuthnContext(tokenType, tokenElement);
    }

    /*
    This examination does not have to be exhaustive, as this method is only called in a delegated context, and can only return
    token types which we validate. Will return null if no TokenType could be determined.
    See org.apache.cxf.sts.request.RequestParser#parseTokenElements for details on how this token is parsed - but it looks
    like only an element will be set straight out of the RST payload.

    This may well be refactored when we migrate to the 2.x version of wss4j.
     */
    private TokenType parseTokenTypeFromDelegatedReceivedToken(ReceivedToken receivedToken) {
        if (receivedToken.isUsernameToken()) {
            return TokenType.USERNAME;
        } else if (receivedToken.isBinarySecurityToken()) {
            return TokenType.X509;
        } else if (receivedToken.isDOMElement()) {
            final Element tokenElement = (Element) receivedToken.getToken();
            String tokenString = xmlUtilities.documentToStringConversion(tokenElement);
            if ((tokenString != null)  && tokenString.contains(QNameConstants.USERNAME_TOKEN.getLocalPart())) {
                return TokenType.USERNAME;
            } else if ((tokenString != null)  && tokenString.contains("X509")) { //TODO: clean up - use globally-defined constant, or refactor with wss4j migration
                return TokenType.X509;
            } else {
                logger.error("Unexpected state in parseTokenTypeFromDelegatedReceivedToken: dealing with a token element, " +
                        "but it is neither UNT or X509. The element string: " + tokenString + "; Returning null for the TokenType.");
                return null;
            }
        } else {
            logger.error("Unexpected state in parseTokenTypeFromDelegatedReceivedToken: not dealing with a USERNAME or " +
                    "X509 token. The token object class: " + receivedToken.getClass().getCanonicalName() +
                    "; toString on the token object: " + receivedToken + "; Returning null for TokenType.");
            return null;
        }
    }

    /*
    See org.apache.cxf.sts.request.RequestParser#parseTokenElements for details on how ActAs/OnBehalfOf token elements
    are parsed - an element will be set straight out of the RST payload. This may change during the wss4j 2.x migration.

     */
    private Element getDelegatedReceivedTokenElement(ReceivedToken receivedToken) {
        if (receivedToken.isDOMElement()) {
            return (Element) receivedToken.getToken();
        } else {
            logger.error("Unexpected state in getDelegatedReceivedTokenElement: the ReceivedToken is not an Element. The token class: " +
                    receivedToken.getToken().getClass().getCanonicalName() + "; toString on the token: " + receivedToken.getToken() +
                    "; Returning a null token type.");
            return null;
        }
    }

    /*
     Approach to obtain the token state from the SecurityPolicy binding traversal yield detailed in question in forums here:
            http://cxf.547215.n5.nabble.com/Accessing-SecurityPolicy-SupportingToken-in-STS-TokenProvider-td5746242.html
     */
    private String getAuthnContextFromSecurityPolicyBindings(TokenProviderParameters tokenProviderParameters) {
        final List<WSHandlerResult> handlerResults =
                CastUtils.cast((List<?>)
                        tokenProviderParameters.getWebServiceContext().getMessageContext().get(WSHandlerConstants.RECV_RESULTS));
            /*
            Note that the code referenced in the forum link above
            (https://git-wip-us.apache.org/repos/asf?p=cxf.git;a=blob_plain;f=services/sts/sts-core/src/main/java/org/apache/cxf/sts/request/RequestParser.java;hb=HEAD)
            seems to be doing about the same thing (obtaining a token element), but does not do any sanity checking on the
            number of handlerResults or engineResults - it just processes and returns the first element encountered. I will
            do the same, but log a warning if additional elements encountered.
             */
        if (handlerResults != null && handlerResults.size() > 0) {
            final WSHandlerResult handlerResult = handlerResults.get(0);
            if (handlerResults.size() > 1) {
                logger.warn("WSHanderResults obtained from the MessageContext in SoapSamlTokenProvider#getAuthnContextClassRef > 1:"
                        + handlerResults.size() + "; The results: " + handlerResults);
            }
            final List<WSSecurityEngineResult> engineResults = handlerResult.getResults();

            for (WSSecurityEngineResult engineResult : engineResults) {
                final Element supportingTokenElement = parseSupportingTokenElementFromWSSecurityEngineResult(engineResult);
                if (supportingTokenElement != null) {
                    final TokenType tokenType = parseTokenTypeFromWSSecurityEngineResult(engineResult);
                    return authnContextMapper.getAuthnContext(tokenType, supportingTokenElement);
                }
            }
            throw new AMSTSRuntimeException(ResourceException.INTERNAL_ERROR, "No WSHandlerResult instances to " +
                    "inspect to obtain the input token validated by SecurityPolicy bindings in " +
                    "SoapSamlTokenProvider#getAuthnContextClassRef");
        } else {
            throw new AMSTSRuntimeException(ResourceException.INTERNAL_ERROR, "No WSSecurityEngineResult obtained " +
                    "from the WSHandlerResult as necessary to inspect to obtain the input token validated by " +
                    "SecurityPolicy bindings in SoapSamlTokenProvider#getAuthnContextClassRef");
        }

    }

    /*
    There are multiple WSSecurityEngineResult instances (5) generated for a successful invocation of e.g. a UNT
    over the asymmetric binding: two for the client's cert (one as a BinarySecurityToken representation of the client's
    cert, and one with more generic information about this cert), one for the server's cert, one for a timestamp, and
    one for the UNT ProtectionToken. The non-BinarySecurityToken representations of a x509 cert do not have a token-element
    entry, but e.g. the timestamp does. The question is what can be used to identify the WSSecurityEngine result corresponding
    to the actual identity-asserting SupportingToken asserted by the caller, rather than the various tokens which
    serve to protect this identity token.

    Note that this method will return null unless it can obtain an Element corresponding to the the SupportingToken asserting
    the client's identity.
     */
    private Element parseSupportingTokenElementFromWSSecurityEngineResult(WSSecurityEngineResult engineResult) {
        final Element tokenElement =
                (Element) engineResult.get(WSSecurityEngineResult.TAG_TOKEN_ELEMENT);
        if (tokenElement != null) {
            final Object validatedObject = engineResult.get(WSSecurityEngineResult.TAG_VALIDATED_TOKEN);
            if ((validatedObject != null) && (validatedObject instanceof Boolean)) {
                if ((Boolean)validatedObject) {
                    /*
                    The TAG_ACTION is used by the various validators to indicate the action to which this result corresponds.
                    Exclude Timestamp actions. See classes on org.apache.ws.security.processor class for details.
                     */
                    if (((Integer)engineResult.get(WSSecurityEngineResult.TAG_ACTION) & WSConstants.TS) != WSConstants.TS) {
                        return tokenElement;
                    }
                }
            }
        }
        return null;
    }

    /*
    TODO: if we are doing OpenAM-based transformations, what would this input token type look like?
     */
    private TokenType parseTokenTypeFromWSSecurityEngineResult(WSSecurityEngineResult engineResult) {
        if ((engineResult.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE) != null) ||
                (engineResult.get(WSSecurityEngineResult.TAG_X509_CERTIFICATES) != null)) {
            return TokenType.X509;
        } else if (engineResult.get(WSSecurityEngineResult.TAG_USERNAME_TOKEN) != null) {
            return TokenType.USERNAME;
        } else if (engineResult.get(WSSecurityEngineResult.TAG_SAML_ASSERTION) != null) {
            return TokenType.SAML2;
        } else {
            throw new AMSTSRuntimeException(ResourceException.BAD_REQUEST,
                    "Unknown token type validated by ws-SecurityPolicy interceptors or passed as transform input token type: " +
                            engineResult.get(WSSecurityEngineResult.TAG_TOKEN_ELEMENT)); //TODO: proper logging for this xml element?
        }
    }

    private String getAuthnContextClassRefForReceivedToken(ReceivedToken receivedToken) {
        /*
        The set of input tokens we support is currently limited to:
         1. UNTs
         2. OpenAM tokens - will be represented as a DOM Element

         When we support X509 Certificates, a new branch has to be added here. Not sure whether a X509Cert is represented
         as a JAXBElement or a DOM Element. See ReceivedToken for details.
         */
        if (receivedToken.isUsernameToken()) {
            if (receivedToken.getToken() instanceof UsernameToken) {
                return authnContextMapper.getAuthnContext(TokenType.USERNAME, ((UsernameToken)receivedToken.getToken()).getElement());
            } else {
                String message = "Unexpected type for a ReceivedToken which reports that it is a UsernameToken: the type: "
                        + receivedToken.getToken().getClass().getCanonicalName() + "; The actual token: " + receivedToken.getToken();
                logger.error(message);
                throw new AMSTSRuntimeException(ResourceException.NOT_SUPPORTED, message);
            }
        } else if (receivedToken.isDOMElement()) {
                /*
                Right now, this can only mean we are dealing with an OpenAMSession token. Attempt to marshal with
                the amSessionTokenXmlMarshaller, just to be sure.
                 */
            Element tokenElement = (Element)receivedToken.getToken();
            try {
                amSessionTokenXmlMarshaller.fromXml(tokenElement);
                return authnContextMapper.getAuthnContext(TokenType.OPENAM, tokenElement);
            } catch (TokenMarshalException e) {
                    /*
                    Note it seems that we could enter this branch because the CXF-STS does not distinguish token validators
                    for status validation, and token validators for the input in a transformation operation. Thus if a
                    deployed STS instance supports a set of token validation operations which is a superset of the input
                    set in token transformation operations, then this branch could be entered.
                    TODO: see of this contingency could be obviated by distinguishing the status validators from the
                    transformation validators.
                     */
                String message = "Unexpected state in SoapSamlTokenProivder#getAuthnContextClassRef: the ReceivedToken" +
                        " in the validateTarget is a DOM Element, but cannot be marshaled to an OpenAM Session token. " +
                        " This means that the validate operation is being invoked with an unsupported token type. " +
                        "The token element " + tokenElement;
                logger.error(message);
                throw new AMSTSRuntimeException(ResourceException.BAD_REQUEST, message);
            }
        } else {
            String message = "Unexpected validateTarget token in SoapSamlTokenProvider#getAuthnContextClassRef - " +
                    "the token is neither a DOM Element or a UNT. The token: " + receivedToken.getToken();
            logger.error(message);
            throw new AMSTSRuntimeException(ResourceException.NOT_SUPPORTED, message);
        }
    }

    /*
    Throw TokenCreationException as threadLocalAMTokenCache.getAMToken throws a TokenCreationException. Let caller above
    map that to an AMSTSRuntimeException.
     */
    private String getAssertion(String authnContextClassRef, SAML2SubjectConfirmation subjectConfirmation,
                                ProofTokenState proofTokenState) throws TokenCreationException {
        String consumptionToken = null;
        try {
            consumptionToken = getTokenGenerationServiceConsumptionToken();
            switch (subjectConfirmation) {
                case BEARER:
                    return tokenGenerationServiceConsumer.getSAML2BearerAssertion(threadLocalAMTokenCache.getAMToken(),
                            stsInstanceId, realm, authnContextClassRef, consumptionToken);
                case SENDER_VOUCHES:
                    return tokenGenerationServiceConsumer.getSAML2SenderVouchesAssertion(threadLocalAMTokenCache.getAMToken(),
                            stsInstanceId, realm, authnContextClassRef, consumptionToken);
                case HOLDER_OF_KEY:
                    return tokenGenerationServiceConsumer.getSAML2HolderOfKeyAssertion(threadLocalAMTokenCache.getAMToken(),
                            stsInstanceId, realm, authnContextClassRef, proofTokenState, consumptionToken);
            }
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Unexpected SAML2SubjectConfirmation in AMSAMLTokenProvider: " + subjectConfirmation);
        } finally {
            if (consumptionToken != null) {
                soapSTSAccessTokenProvider.invalidateAccessToken(consumptionToken);
            }
        }
    }

    private String getTokenGenerationServiceConsumptionToken() throws TokenCreationException {
        try {
            return soapSTSAccessTokenProvider.getAccessToken();
        } catch (ResourceException e) {
            throw new TokenCreationException(e.getCode(), e.getMessage(), e);
        }
    }
}

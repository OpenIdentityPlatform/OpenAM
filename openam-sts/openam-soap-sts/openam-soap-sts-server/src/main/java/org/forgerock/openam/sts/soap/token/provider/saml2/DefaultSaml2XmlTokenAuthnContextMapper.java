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

import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.handler.WSHandlerResult;
import org.apache.ws.security.message.token.BinarySecurity;
import org.apache.ws.security.saml.ext.builder.SAML2Constants;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenTypeId;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.List;

/**
 * @see Saml2XmlTokenAuthnContextMapper
 * Note that end-users can publish soap-sts instances which specify their own implementation of the Saml2XmlTokenAuthnContextMapper
 * implementations. This class is built to be readily sub-class-able by such custom implementations.
 */
public class DefaultSaml2XmlTokenAuthnContextMapper implements Saml2XmlTokenAuthnContextMapper {
    private final Logger logger;

    @Inject
    public DefaultSaml2XmlTokenAuthnContextMapper(Logger logger) {
        this.logger = logger;
    }

    @Override
    public String getAuthnContext(List<WSHandlerResult> securityPolicyBindingTraversalYield) {
        /*
            Note that the code referenced in the forum link above
            (https://git-wip-us.apache.org/repos/asf?p=cxf.git;a=blob_plain;f=services/sts/sts-core/src/main/java/org/apache/cxf/sts/request/RequestParser.java;hb=HEAD)
            seems to be doing about the same thing (obtaining a token element), but does not do any sanity checking on the
            number of handlerResults or engineResults - it just processes and returns the first element. Note that
            this seems a bit sloppy, but the AbstractTokenInterceptor subclasses, like org.apache.cxf.ws.security.wss4j.UsernameTokenInterceptor
            SamlTokenInterceptor, and KerberosTokenInterceptor insure that the results of processing the token go in the first
            element of the WSHandlerResult array, a pattern which the OpenAMSessionTokenServerInterceptor follows. Note that
            I get two WSHandlerResult instances when invoked via the am_transport binding, but the OpenAMSessionTokenServerInterceptor
            is only invoked once, and only creates a single WSHandlerResult instance - not sure where the other result gets generated, but
            it seems to occur as part of timestamp verification, and it pre-exists OpenAMSessionTokenInterceptor#processToken, and includes
            both timestamp verification results, and the OpenAM BST. So I will bump the log severity down to debug, as it does not
            seem to be an error.
         */
        if (securityPolicyBindingTraversalYield != null && securityPolicyBindingTraversalYield.size() > 0) {
            final WSHandlerResult handlerResult = securityPolicyBindingTraversalYield.get(0);
            if (securityPolicyBindingTraversalYield.size() > 1) {
                logger.debug("WSHanderResults obtained from the MessageContext in SoapTokenProviderBase#getAuthnContextClassRef > 1: actual size:"
                        + securityPolicyBindingTraversalYield.size() + "; The results: " + getWSHandlerResultsDebug(securityPolicyBindingTraversalYield));
            }
            final List<WSSecurityEngineResult> securityEngineResults = handlerResult.getResults();

            /*
            The List of WSSecurityEngineResult instances is extensive, and is a function of the type of SecurityPolicy binding.
            Ultimately, these instances are created by classes in wss4j's org.apache.ws.security.processor package. The set of
            SupportingTokens used to traverse the SecurityPolicy bindings in the set of supported .wsdl files is limited to
            UsernameTokens, OpenAM Session tokens, and x509 tokens. In both the symmetric and asymmetric bindings, there are
            several WSSecurityEngineResult instances which correspond to x509 tokens, as both of these bindings use PKI to
            realize confidentiality/integrity of the SupportingTokens/soap-messages. Each WSSecurityEngineResult is constructed
            with an action bitmap which specifies the nature of its processing. This action does not seem to differentiate x509
            tokens presented as a SupportingToken, and those presented to secure messages and/or presented tokens (in both cases,
            the action bitmap appears to be WSConstants.SIGN - see the org.apache.ws.security.processor.SignatureProcessor
            for details.). Thus the most reliable scheme will be to traverse all of the WSSecurityEngineResult instances, and look for the presence of a
            UsernameToken and a OpenAM Session token. If neither of these tokens are present, then we are dealing with a
            x509 SupportingToken.

            Listing of WSSecurityEngineResult instances corresponding to various bindings:

            For an UNT over the asymmetric binding, the List<WSSecurityEngineResult> has the following elements:
            0. action = 2 - WSConstants.SIGN - an x509 certificate, corresponding to the client - the signature of the request
            1. action = 1 - WSConstants.UT - the UNT - this is the SupportingToken
            2. action = 4 - WSConstants.ENCR - an x509 certificate corresponding to the server cert - public key used to encrypt the message, corresponding to the asymmetric binding
            3. action = 32 - WSConstants.TS - timestamp verification
            4. action = 4096 - WSConstants.BST - the client’s x509 certificate, used to verify the signature.

            For an UNT over the symmetric binding, the List<WSSecurityEngineResult> has the following elements:
            0. action = 2 - WSConstants.SIGN - no x509 certificate - signature of the message using the client-generated secret
            per the symmetric binding. validated-token = true
            1. action = 1 - WSConstants.UT - the UNT - this is the SupportingToken
            2. action = 4 - WSConstants.ENCR - no x509 certificate - encryption of the message using the client-generated secret
            - validated-token = false
            3. action = 2048 - WSConstants.DKT - the derived key - nonce generated by client
            4. action = 2048 - WSConstants.DKT - the derived key - key generated by client
            5. action = 4 - WSConstants.ENCR - presumably the encryption of the derived key - x509 cert referencing the server's cert
            6. action = 32 - WSConstants.TS - timestamp verification

            For x509 over the asymmetric binding, the List<WSSecurityEngineResult> has the following elements:
            0. action = 2 - WSConstants.SIGN - the client's cert, validated-token = true
            1. action = 32 - WSConstants.TS - timestamp verification
            2. action = 4096 - WSConstants.BST - the client's X509 certificate, used to verify the signature

            For x509 over the symmetric binding, the List<WSSecurityEngineResult> has the following elements(note in this
            binding, the client-generated secret is used to sign the message, but this signature is itself signed by the client, so that
            the server can be sure that the private key holder originated the message) :
            0. action = 2 - WSConstants.SIGN - the client's certificate - validated-token  = true
            1. action = 2 - WSConstants.SIGN - no x509 certificate - just a reference - presumably this is the signature of the
            message signature
            2. action = 2048 - WSConstants.DKT - no x509 certificate - key generated by client
            - validated-token = false
            3. action = 4 - WSConstants.ENC - encryption of the derived key
            4. action = 32 - WSConstants.TS - timestamp verification
            5. action = 4096 - WSConstants.BST - the client's cert so that the signature of the message signature can be verified by the server

            For UNT over the transport binding, the List<WSSecurityEngineResult> has the following elements):
            0. action = 1 - WSConstants.UT - the UNT
            1. action = 32 - WSConstants.TS - timestamp verification

            For AM over the transport binding, the List<WSSecurityEngineResult> has the following elements):
            0. action = 4096 - WSConstants.BST - the BinarySecurityToken encapsulating the OpenAM session token

            Given it does not seem possible to distinguish an x509 SupportingToken from a cert used for signature (though
            this makes sense - the only way possession of a cert is demonstrated is by signature generation/verification), there
            is really no way to identify a x509 'supporting token', other than by the failure to find any other explicit
            SupportingToken type. And it does appear that the first SIGN reference is the correct reference.

            Additional context - a list of Action bitmasks that don't ever seem to pertain to a SupportingToken:
            WSConstants.TS - corresponds to timestamp verification
            WSConstants.SC - signature confirmation - only the org.apache.ws.secruity.processor.SecurityConfirmationProcessor sets this
            value - this class does not seem to do much, and may not be engaged
            WSConstants.ENCR - encryption - encryption of the message in the asymmetric binding using the recipients public key/cert,
            or encryption of the messge using the client-generated secret in the symmetric binding - again, no supporting token here.
            WSConstants.DKT - derived key token - in the symmetric binding, relates to key/nonce generated by client
             */
            final WSSecurityEngineResult untSupportingTokenResult = getUsernameTokenResult(securityEngineResults);
            if (untSupportingTokenResult != null) {
                return peformSaml2AuthNContextClassReferenceMapping(TokenType.USERNAME, untSupportingTokenResult);
            }

            final WSSecurityEngineResult openAMSessionSupportingTokenResult = getOpenAMSessionTokenResult(securityEngineResults);
            if (openAMSessionSupportingTokenResult != null) {
                return peformSaml2AuthNContextClassReferenceMapping(TokenType.OPENAM, openAMSessionSupportingTokenResult);
            }

            final WSSecurityEngineResult x509SupportingTokenResult = getX509TokenResult(securityEngineResults);
            if (x509SupportingTokenResult != null) {
                return peformSaml2AuthNContextClassReferenceMapping(TokenType.X509, x509SupportingTokenResult);
            }

            logger.error("No recognizable WSHandlerResult instances found matching an expected input token validated by " +
                    "SecurityPolicy bindings in the DefaultSaml2XmlTokenAuthnContextMapper. Returning " +
                    SAML2Constants.AUTH_CONTEXT_CLASS_REF_UNSPECIFIED + " as the SAML2 authn context class reference.");
            return SAML2Constants.AUTH_CONTEXT_CLASS_REF_UNSPECIFIED;
        } else {
            logger.error("No WSSecurityEngineResult obtained from the WSHandlerResult as necessary to inspect to obtain " +
                    "the input token validated by SecurityPolicy bindings in theDefaultSaml2XmlTokenAuthnContextMapper. " +
                    "Returning " + SAML2Constants.AUTH_CONTEXT_CLASS_REF_UNSPECIFIED + "as the SAML2 authn context class reference.");
            return SAML2Constants.AUTH_CONTEXT_CLASS_REF_UNSPECIFIED;
        }
    }

    @Override
    public String getAuthnContextForDelegatedToken(List<WSHandlerResult> securityPolicyBindingTraversalYield, ReceivedToken delegatedToken) {
        final TokenTypeId tokenType = parseTokenTypeFromDelegatedReceivedToken(delegatedToken);
        if (tokenType != null) {
            return peformSaml2AuthNContextClassReferenceMappingForDelegatedToken(tokenType);
        }
        logger.error("Unexpected delegated token type. Returning " + SAML2Constants.AUTH_CONTEXT_CLASS_REF_UNSPECIFIED +
            " for the AuthnContext class ref.");
        return SAML2Constants.AUTH_CONTEXT_CLASS_REF_UNSPECIFIED;
    }

    private TokenTypeId parseTokenTypeFromDelegatedReceivedToken(ReceivedToken receivedToken) {
        /*
            This examination does not have to be exhaustive, as this method is only called in a delegated context, and can only return
            delegated token types which we validate. Will return null if no TokenType could be determined.
            See org.apache.cxf.sts.request.RequestParser#parseTokenElements for details on how this token is parsed - but it looks
            like only an element will be set straight out of the RST payload.
        */
        if (receivedToken.isUsernameToken()) {
            return TokenType.USERNAME;
        } else if (isReceivedTokenOpenAMSessionToken(receivedToken)) {
            return TokenType.OPENAM;
        } else {
            logger.error("Unexpected state in parseTokenTypeFromDelegatedReceivedToken: not dealing with a USERNAME or " +
                    "OPENAM token. The token object class: " + receivedToken.getClass().getCanonicalName() +
                    "; toString on the token object: " + receivedToken + "; Returning null for TokenType.");
            return null;
        }
    }

    private boolean isReceivedTokenOpenAMSessionToken(ReceivedToken receivedToken) {
        if (receivedToken.isBinarySecurityToken()) {
            final BinarySecurityTokenType binarySecurityTokenType = (BinarySecurityTokenType)receivedToken.getToken();
            return AMSTSConstants.AM_SESSION_TOKEN_ASSERTION_BST_VALUE_TYPE.equals(binarySecurityTokenType.getValueType());
        }
        return false;
    }

    /**
     *
     * @param securityEngineResults the results of SecurityPolicy binding enforcement generated by wss4j
     * @return the WSSecurityEngineResult corresponding to a Username SupportingToken - null if no such token was presented.
     */
    protected WSSecurityEngineResult getUsernameTokenResult(List<WSSecurityEngineResult> securityEngineResults) {
        for (WSSecurityEngineResult securityEngineResult : securityEngineResults) {
            if (isUsernameToken(securityEngineResult)) {
                return securityEngineResult;
            }
        }
        return null;
    }


    protected boolean isUsernameToken(WSSecurityEngineResult securityEngineResult) {
        return doesBitmapMatch(WSConstants.UT, (Integer)securityEngineResult.get(WSSecurityEngineResult.TAG_ACTION));
    }

    /**
     *
     * @param securityEngineResults the results of SecurityPolicy binding enforcement generated by wss4j
     * @return the WSSecurityEngineResult corresponding to a OpenAM Session SupportingToken - null if no such token was presented.
     */
    protected WSSecurityEngineResult getOpenAMSessionTokenResult(List<WSSecurityEngineResult> securityEngineResults) {
        for (WSSecurityEngineResult securityEngineResult : securityEngineResults) {
            if (isOpenAMSessionToken(securityEngineResult)) {
                return securityEngineResult;
            }
        }
        return null;
    }

    /*
    Determine if the SupportingToken is an OpenAM session token. See logic in OpenAMSessionTokenInterceptor#validateToken for
    insight into the logic in the block below.
     */
    protected boolean isOpenAMSessionToken(WSSecurityEngineResult securityEngineResult) {
        if (isActionTagForBinarySecurityToken((Integer) securityEngineResult.get(WSSecurityEngineResult.TAG_ACTION))) {
            BinarySecurity binarySecurity = (BinarySecurity) securityEngineResult.get(WSSecurityEngineResult.TAG_BINARY_SECURITY_TOKEN);
            return AMSTSConstants.AM_SESSION_TOKEN_ASSERTION_BST_VALUE_TYPE.equals(binarySecurity.getValueType());
        }
        return false;
    }

    protected boolean isActionTagForBinarySecurityToken(int actionTag) {
        return doesBitmapMatch(WSConstants.BST, actionTag);
    }

    protected WSSecurityEngineResult getX509TokenResult(List<WSSecurityEngineResult> securityEngineResults) {
        for (WSSecurityEngineResult securityEngineResult : securityEngineResults) {
            if (isActionTagForSignature((Integer)securityEngineResult.get(WSSecurityEngineResult.TAG_ACTION))) {
                return securityEngineResult;
            }
        }
        return null;
    }

    protected boolean isActionTagForSignature(int actionTag) {
        return doesBitmapMatch(WSConstants.SIGN, actionTag);
    }

    protected boolean doesBitmapMatch(int bitmap, int potentialMatch) {
        return (bitmap & potentialMatch) == bitmap;
    }

    /*
    Note that the WSSecurityEngineResult is not examined. It is included only to provide additional context to potential
    end-user-specified implementations of the Saml2XmlTokenAuthnContextMapper interface which might wish to subclass this
    class.
     */
    protected String peformSaml2AuthNContextClassReferenceMapping(TokenTypeId tokenTypeId, WSSecurityEngineResult securityEngineResult) {
        if (TokenType.OPENAM.getId().equals(tokenTypeId.getId())) {
            return SAML2Constants.AUTH_CONTEXT_CLASS_REF_PREVIOUS_SESSION;
        } else if (TokenType.USERNAME.getId().equals(tokenTypeId.getId())) {
            return SAML2Constants.AUTH_CONTEXT_CLASS_REF_PASSWORD_PROTECTED_TRANSPORT;
        } else if (TokenType.X509.getId().equals(tokenTypeId.getId())) {
            return SAML2Constants.AUTH_CONTEXT_CLASS_REF_X509;
        } else {
            logger.error("Unexpected TokenType passed to DefaultSaml2XmlTokenAuthnContextMapper: " + tokenTypeId + "; returning " +
                    SAML2Constants.AUTH_CONTEXT_CLASS_REF_UNSPECIFIED);
            return SAML2Constants.AUTH_CONTEXT_CLASS_REF_UNSPECIFIED;
        }
    }

    /*
    Performs the authNContext mapping for delegated tokens. Here there is no WSSecurityEngineResult, as the delegated token
    is not examined as part of SecurityPolicy binding enforcement.
     */
    protected String peformSaml2AuthNContextClassReferenceMappingForDelegatedToken(TokenTypeId tokenTypeId) {
        return peformSaml2AuthNContextClassReferenceMapping(tokenTypeId, null);
    }

    /*
    No toString on WSHandlerResult, so this method will be called to provide debugging information
    */
    protected String getWSHandlerResultsDebug(List<WSHandlerResult> handlerResults) {
        StringBuilder builder = new StringBuilder();
        for (WSHandlerResult result : handlerResults) {
            builder.append("WSHandlerResults for actor " ).append(result.getActor()).append('\n');
            List<WSSecurityEngineResult> securityEngineResults = result.getResults();
            if (securityEngineResults != null) {
                for (WSSecurityEngineResult securityEngineResult : securityEngineResults) {
                    builder.append('\t').append(securityEngineResult).append('\n');
                }
            } else {
                builder.append("Null WSSecurityEngineResult list.");
            }
        }
        return builder.toString();
    }
}

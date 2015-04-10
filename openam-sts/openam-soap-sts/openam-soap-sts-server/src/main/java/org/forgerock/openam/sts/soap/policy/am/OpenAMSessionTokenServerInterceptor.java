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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.soap.policy.am;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.security.DefaultSecurityContext;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.apache.ws.security.message.token.BinarySecurity;
import org.apache.cxf.ws.security.policy.model.Token;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.validator.PrincipalFromSession;
import org.forgerock.openam.sts.token.validator.ValidationInvocationContext;
import org.w3c.dom.Element;

import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * The custom AbstractTokenInterceptor deployed with published soap-sts instances. It is responsible for validating
 * the OpenAMSessionToken assertions, and communicating the the apache Neethi SecurityPolicy runtime that the relevant
 * assertions have been fulfilled.
 * Implementation modeled after org.apache.cxf.ws.security.wss4j.SamlTokenInterceptor.
 */
public class OpenAMSessionTokenServerInterceptor extends AbstractOpenAMSessionTokenInterceptor {
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final PrincipalFromSession principalFromSession;

    /*
    No @Inject - called from OpenAMSessionTokenServerInterceptorProvider.
     */
    OpenAMSessionTokenServerInterceptor(ThreadLocalAMTokenCache threadLocalAMTokenCache,
                                        PrincipalFromSession principalFromSession)  {
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.principalFromSession = principalFromSession;
    }

    /**
     * This method is called in-bound on the server-side - validate-request in JASPI terms. The method must validate the
     * OpenAM session id with OpenAM, and, if validation is successful, populate the wss4j results with state corresponding
     * to the token validation. It will also assert the relevant tokens, which means affirm that the assertions corresponding
     * to the OpenAMSessionToken have been successfully fulfilled.
     * @param message The message encapsulating the soap invocation.
     * @throws Fault if the OpenAM session in the BinarySecurityToken in invalid.
     */
    @Override
    protected void processToken(SoapMessage message) throws Fault {
        Header header = findSecurityHeader(message, false);
        if (header == null) {
            return;
        }
        Element el = (Element)header.getObject();
        Element child = DOMUtils.getFirstElement(el);
        while (child != null) {
            if (WSConstants.BINARY_TOKEN_LN.equals(child.getLocalName())
                    && WSConstants.WSSE_NS.equals(child.getNamespaceURI())
                    && AMSTSConstants.AM_SESSION_TOKEN_ASSERTION_BST_VALUE_TYPE.equals(child.getAttribute("ValueType"))) {
                try {
                    List<WSSecurityEngineResult> validationResults = validateToken(child);
                    if (validationResults != null) {
                        List<WSHandlerResult> results = CastUtils.cast((List<?>) message
                                .get(WSHandlerConstants.RECV_RESULTS));
                        if (results == null) {
                            results = new ArrayList<WSHandlerResult>();
                            message.put(WSHandlerConstants.RECV_RESULTS, results);
                        }
                        WSHandlerResult rResult = new WSHandlerResult(null, validationResults);
                        results.add(0, rResult);

                        assertTokens(message);

                        Principal principal =
                                (Principal)validationResults.get(0).get(WSSecurityEngineResult.TAG_PRINCIPAL);
                        message.put(WSS4JInInterceptor.PRINCIPAL_RESULT, principal);

                        SecurityContext sc = message.get(SecurityContext.class);
                        if (sc == null || sc.getUserPrincipal() == null) {
                            message.put(SecurityContext.class, new DefaultSecurityContext(principal, null));
                        }
                    }
                } catch (WSSecurityException ex) {
                    throw new Fault(ex);
                }
            }
            child = DOMUtils.getNextElement(child);
        }
    }

    /**
     * @param tokenElement the BinarySecurityToken representing the OpenAMSessionToken. The OpenAM session id is the text
     *                     content of this Element.
     * @return a List with a single WSSecurityEngineResult with information concerning the successful validation.
     * @throws WSSecurityException if the OpenAM session cannot be validated successfully.
     */
    private List<WSSecurityEngineResult> validateToken(Element tokenElement) throws WSSecurityException {
        final boolean bspComliant = true;
        final BinarySecurity bst = new BinarySecurity(tokenElement, bspComliant);
        bst.setValueType(AMSTSConstants.AM_SESSION_TOKEN_ASSERTION_BST_VALUE_TYPE);
        final X509Certificate[] certs = null;
        WSSecurityEngineResult result =  new WSSecurityEngineResult(WSConstants.BST, bst, certs);
        try {
            final String sessionId = tokenElement.getTextContent();
            final Principal principal = principalFromSession.getPrincipalFromSession(sessionId);
            //because we are dealing with an OpenAM session which was not created as part of TokenValidation, but
            //rather pre-existed this validation, it should not be invalidated.
            threadLocalAMTokenCache.cacheSessionIdForContext(ValidationInvocationContext.SOAP_SECURITY_POLICY, sessionId, false);
            result.put(WSSecurityEngineResult.TAG_VALIDATED_TOKEN, Boolean.TRUE);
            result.put(WSSecurityEngineResult.TAG_PRINCIPAL, principal);

        } catch (TokenValidationException e) {
            throw new WSSecurityException(e.getMessage(), e);
        }
        return Collections.singletonList(result);
    }

    /**
     * This method is called on the outbound client side, secure-request in JASPI terms. In the
     * OpenAMSessionTokenClientInterceptor, this method will add the OpenAMSessionAssertion state to the message, but in
     * the server-side interceptor, this method should never be called.
     * @param message the encapsulation of the soap request.
     */
    @Override
    protected void addToken(SoapMessage message) {
        throw new IllegalStateException("OpenAMSessionTokenSeverInterceptor#addToken called - this is unexpected!");
    }

    /**
     * Called to assert the relevant tokens. Asserting tokens means asserting that the corresponding policy has been
     * satisfied. This method is called outbound on the server-side, and inbound on the client side. It is also called from
     * processTokenAbove, following successful token validation. This method will assert that the OpenAMSessionAssertion
     * has been satisfied, and also the SupportingToken policy (the OpenAMSessionToken policy always defines a SupportingToken),
     * and, if TLS is being used in the invocation, that the TransportPolicy has also been satisfied, as the OpenAMSessionToken
     * SecurityPolicy binding is always deployed as part of an unprotected binding (i.e. a 'bare' OpenAMSessionToken), or
     * as part of the Transport binding. Note that a TransportToken is the token manifestation of a TransportPolicy binding,
     * so asserting the TransportToken will assert the TransportPolicy.
     * @param message The SoapMessage defining the invocation.
     * @return The OpenAMSessionAssertion corresponding to the OpenAMSessionToken SecurityPolicy element protecting
     * soap-sts instances.
     */
    @Override
    protected Token assertTokens(SoapMessage message) {
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        Collection<AssertionInfo> ais = aim.getAssertionInfo(AMSTSConstants.AM_SESSION_TOKEN_ASSERTION_QNAME);
        Token token = null;
        for (AssertionInfo ai : ais) {
            token = (Token)ai.getAssertion();
            ai.setAsserted(true);
        }
        ais = aim.getAssertionInfo(SP12Constants.SUPPORTING_TOKENS);
        for (AssertionInfo ai : ais) {
            ai.setAsserted(true);
        }
        if (isTLSInUse(message)) {
            /*
            if TLS is in use, then the tokens are signed by TLS. So instead of having the transport binding reference
            a SupportingToken element, a SignedSupportingToken could be specified.
             */
            ais = aim.getAssertionInfo(SP12Constants.SIGNED_SUPPORTING_TOKENS);
            for (AssertionInfo ai : ais) {
                ai.setAsserted(true);
            }
            /*
            this should be asserted by the cxf TransportBindingHandler or TransportBinding or TransportToken, but
            it is not, resulting in the following messages, logged as FINE:
            An exception was thrown when verifying that the effective policy for this request was satisfied.
            However, this exception will not result in a fault.  The exception raised is: org.apache.cxf.ws.policy.PolicyException:
            These policy alternatives can not be satisfied:{http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702}TransportToken
            Asserting the TRANSPORT_TOKEN makes this message go away. I know that the OpenAMSessionToken will be deployed in
            either a 'bare' SecurityPolicy binding, or under the Transport binding, so if TLS is in use, the TRANSPORT_TOKEN
            can be asserted.
            Note that this message is a bug - see
            http://cxf.547215.n5.nabble.com/Custom-SecurityPolicy-Assertions-and-the-Symmetric-binding-td5754879.html#a5755303
            for details. I will continue to assert the TRANSPORT_TOKEN to prevent these messages.
             */
            ais = aim.getAssertionInfo(SP12Constants.TRANSPORT_TOKEN);
            for (AssertionInfo ai : ais) {
                ai.setAsserted(true);
            }
        }
        return token;
    }
}

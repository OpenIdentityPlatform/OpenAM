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
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.model.Token;
import org.forgerock.openam.sts.AMSTSConstants;
import org.w3c.dom.Element;

import java.util.Collection;

/**
 * Implementation modeled after org.apache.cxf.ws.security.wss4j.SamlTokenInterceptor. It appears that the OpenAMSessionTokenAssertionBuilder
 * is called first, once the OpenAMSessionPolicy is encountered in the sts wsdl. So the addToken does not really need to
 * add the token, but rather assert the corresponding policy.
 */
public class OpenAMSessionTokenClientInterceptor extends AbstractOpenAMSessionTokenInterceptor {
    /**
     * This method will be called in-bound on the server side. It will not be called in the client context.
     * @param message defines the invocation and its result
     */
    @Override
    protected void processToken(SoapMessage message) {
        throw new IllegalStateException("OpenAMSessionTokenClientInterceptor#processToken should not be called!");
    }

    /**
     * This method is called on the outbound client side, secure-request in JASPI terms. This method will add the
     * OpenAMSessionAssertion state to the message.
     * @param message the encapsulation of the soap request.
     */
    @Override
    protected void addToken(SoapMessage message) {
        OpenAMSessionAssertion openAMSessionAssertion = (OpenAMSessionAssertion) assertTokens(message);
        Header header = findSecurityHeader(message, true);
        final Element element = (Element)header.getObject();
        final Element openAMSessionAssertionElement = openAMSessionAssertion.getTokenElement();
        element.appendChild(element.getOwnerDocument().importNode(openAMSessionAssertionElement, true));
    }

    /**
     * Called to assert the relevant tokens. Asserting tokens means asserting that the corresponding policy has been
     * satisfied. This method is called inbound on the client side. This method will assert that the OpenAMSessionAssertion
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
        /*
        On the server-side, isTLSinUse is used to determine a tls invocation. On the client side, pulling the
        "http.scheme" and comparing it to https seems to be the approved approach:
        @see org.apache.cxf.ws.security.policy.interceptors.HttpsTokenInterceptorProvider
         */
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
             */
            ais = aim.getAssertionInfo(SP12Constants.TRANSPORT_TOKEN);
            for (AssertionInfo ai : ais) {
                ai.setAsserted(true);
            }
        }
        return token;
    }
}

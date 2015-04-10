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
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.ws.security.wss4j.AbstractTokenInterceptor;

/**
 * This is the base class for the OpenAMSessionTokenClientInterceptor and OpenAMSessionTokenServerInterceptor classes.
 * It extends the AbstractTokenInterceptor, and over-rides the handleMessage method to insure that the OpenAMSessionToken
 * interceptors are invoked, even in the presence of a wsdl file with SecurityPolicy bindings. See
 * http://cxf.547215.n5.nabble.com/Custom-SecurityPolicy-Assertions-and-the-Symmetric-binding-td5754879.html for more
 * details.
 */
public abstract class AbstractOpenAMSessionTokenInterceptor extends AbstractTokenInterceptor {
    private static final String HTTPS = "https";
    private static final String HTTP_SCHEME = "http.scheme";

    /**
     * This method is over-ridden from the AbstractTokenInterceptor super-class. The super-class implementation contains
     * logic to prevent the invocation of the AbstractTokenInterceptor#addToken and #processToken methods when
     * the PolicyBasedWSS4JOutInterceptor or PolicyBasedWSS4JInInterceptor are deployed, as in these cases, the interceptors
     * set message state which prevents the AbstractTokenInterceptor from invoking the addToken and processToken methods
     * of subclasses. This is because the PolicyBasedWSS4J*Interceptors are deployed when SecurityPolicy bindings are
     * deployed, and the specific tokens (e.g. UNT, SAMLToken, KerberosToken) are handled by the higher-level
     * SecurityPolicy-related tokens and binding which subsume the specific token types - e.g. the classes in the
     * org.apache.cxf.ws.security.policy.model package, which include classes like the TransportToken, SignatureToken, etc.
     * Over-riding the handleMessage in an AbstractTokenInterceptor sub-class appears to be a viable path for token
     * types with  more restricted SecurityPolicy bindings, like the OpenAMSessionToken, which will support only the
     * Transport and 'bare' bindings. Though there are several examples - e.g. the org.apache.cxf.ws.security.wss4j.SamlTokenInterceptor,
     * which is used in the Transport, Symmetric, and Asymmetric bindings, and is a AbstractTokenInterceptor subclass.
     * Given the documentation state of cxf and wss4j, and the complexity of the code-base, this ambiguity cannot be
     * resolved without significant amounts of additional time. If the OpenAMSessionAssertion must be presented over the
     * Symmetric or Asymmetric binding, then this work will be undertaken.
     * @param message The message corresponding to the soap invocation.
     * @throws org.apache.cxf.interceptor.Fault if the assertTokens, addToken, or processToken methods throw an exception.
     */
    @Override
    public void handleMessage(SoapMessage message) throws Fault {
        boolean isReq = MessageUtils.isRequestor(message);
        boolean isOut = MessageUtils.isOutbound(message);
        /*
        This branch is entered on an inbound client request, or an outbound server response - validate-response and
        secure-response respectively in JASPI terms.
         */
        if (isReq != isOut) {
            assertTokens(message);
            return;
        }
        if (isReq) {
            //outbound-client - e.g. JASPI secure-request
            addToken(message);
        } else {
            //inbound-server - e.g. JASPI validate-request
            processToken(message);
        }
    }

    /**
     * This method is called by the OpenAMSessionTokenServerInterceptor and OpenAMSessionTokenClientInterceptor classes
     * when determining whether to assert the TransportToken. The AbstractTokenInterceptor#assertTokens is called in all
     * four JASPI phases. The isTLSInUse implemented in the AbstractTokenInterceptor does not work in the secure-request
     * or validate-response phases - it seems to work exclusively in the validate-request case. The OpenAMSessionToken interceptors
     * need to assert the TransportToken in all four cases to prevent FINE warning messages concerning an unasserted
     * TransportToken, hence this override, which works in all cases. This override will also consult the superclass method.
     * Note however, that there is no state in the outgoing message corresponding to "http.scheme" in the server egress
     * message, so the validate-response is not able to assert the TransportToken, and thus the FINE message warning of an
     * unasserted TransportToken will appear. This is really the domain of the TransportBindingHandler (see
     * http://cxf.547215.n5.nabble.com/Custom-SecurityPolicy-Assertions-and-the-Symmetric-binding-td5754879.html
     * for details), so we will have to live with this error message.
     * @see org.apache.cxf.ws.security.policy.interceptors.HttpsTokenInterceptorProvider for explanation of the approach
     * chosen for this method.
     * @param message the SoapMessage encapsulating the STS invocation
     * @return true if tls is being deployed
     */
    @Override
    public boolean isTLSInUse(SoapMessage message) {
        return super.isTLSInUse(message) || HTTPS.equals(message.get(HTTP_SCHEME));
    }
}

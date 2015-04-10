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

import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.forgerock.openam.sts.soap.OpenAMSessionTokenCallback;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.builders.AssertionBuilder;
import org.forgerock.openam.sts.AMSTSConstants;

import org.w3c.dom.Element;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.xml.namespace.QName;
import java.io.IOException;

/**
 * The AssertionBuilder registered with the CXF Bus running the CXF sts client so that OpenAMSessionAssertions can be
 * created to consume sts instances with SecurityPolicy bindings specifying OpenAMSessionAssertions.
 */
public class OpenAMSessionTokenClientAssertionBuilder implements AssertionBuilder<Element> {
    private final CallbackHandler callbackHandler;

    public OpenAMSessionTokenClientAssertionBuilder(CallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }

    /**
     *
     * @param element the xml representation of the OpenAMSessionToken policy obtained from
     * the sts wsdl.
     * @param assertionBuilderFactory an Apache Neethi constuct used to access the surrounding Policy and other AssertionBuilder
     *                                instances.
     * @return the OpenAMSessionAssertion encapsulating the OpenAM session id which will be included in a BinarySecurityToken
     * included in the security header of the RequestSecurityToken request.
     * @throws IllegalStateException If the OpenAM session id could not be obtained from the CallbackHandler
     */
    @Override
    public Assertion build(Element element, AssertionBuilderFactory assertionBuilderFactory) throws IllegalStateException {
        final Element nestedPolicyElement = PolicyConstants.findPolicyElement(element);
        if (nestedPolicyElement == null) {
            throw new IllegalStateException(AMSTSConstants.AM_SESSION_TOKEN_ASSERTION_QNAME
                    + " must have an inner wsp:Policy element");
        }
        Callback[] callbacks = new Callback[1];
        callbacks[0] = new OpenAMSessionTokenCallback();
        try {
            callbackHandler.handle(callbacks);
        } catch (IOException e) {
            throw new IllegalStateException("CallbackHandler registered with OpenAMSessionTokenClientAssertionBuilder " +
                    "cannot handle OpenAMSessionTokenCallback: " + e, e);
        } catch (UnsupportedCallbackException e) {
            throw new IllegalStateException("CallbackHandler registered with OpenAMSessionTokenClientAssertionBuilder " +
                    "cannot handle OpenAMSessionTokenCallback: " + e, e);
        }
        return new OpenAMSessionAssertion(SP12Constants.INSTANCE, SPConstants.IncludeTokenType.INCLUDE_TOKEN_ALWAYS_TO_RECIPIENT,
                nestedPolicyElement, ((OpenAMSessionTokenCallback)callbacks[0]).getSessionId());
    }

    /**
     *
     * @return the QNames for which this AssertionBuilder can build Assertion instances.
     */
    @Override
    public QName[] getKnownElements() {
        return new QName[] {AMSTSConstants.AM_SESSION_TOKEN_ASSERTION_QNAME};
    }
}

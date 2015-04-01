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
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.builders.AssertionBuilder;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.forgerock.openam.sts.AMSTSConstants;

import org.w3c.dom.Element;
import javax.xml.namespace.QName;

/**
 * Registered with the org.apache.cxf.ws.policy.AssertionBuilderRegistry, obtained via the cxf Bus in SoapSTSLifecycleImpl,
 * to obtain and create OpenAMSessionAssertion included in the soap-sts RequestSecurityToken invocation. If the
 * session id encapsulated in the OpenAMSessionAssertion can be successfully validated with OpenAM, then the
 * OpenAMSessionToken SecurityPolicy bindings protecting the soap-sts instance will be satisfied.
 */
public class OpenAMSessionTokenServerAssertionBuilder implements AssertionBuilder<Element> {
    /**
     * @see org.apache.neethi.builders.AssertionBuilder#build
     * Note that the Element parameter will be the xml representation of the OpenAMSessionToken included in the soap-sts
     * invocation by the OpenAMSessionTokenClientInterceptor.
     */
    @Override
    public Assertion build(Element element, AssertionBuilderFactory assertionBuilderFactory) throws IllegalArgumentException {
        final Element nestedPolicyElement = PolicyConstants.findPolicyElement(element);
        if (nestedPolicyElement == null) {
            throw new IllegalArgumentException(AMSTSConstants.AM_SESSION_TOKEN_ASSERTION_QNAME
                    + " must have an inner wsp:Policy element");
        }
        return new OpenAMSessionAssertion(SP12Constants.INSTANCE, SPConstants.IncludeTokenType.INCLUDE_TOKEN_ALWAYS_TO_RECIPIENT,
                nestedPolicyElement, element.getTextContent());
    }

    /**
     * @see org.apache.neethi.builders.AssertionBuilder#getKnownElements
     */
    @Override
    public QName[] getKnownElements() {
        return new QName[] {AMSTSConstants.AM_SESSION_TOKEN_ASSERTION_QNAME};
    }
}

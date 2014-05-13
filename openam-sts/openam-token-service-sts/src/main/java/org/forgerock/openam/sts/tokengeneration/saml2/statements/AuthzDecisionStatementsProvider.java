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
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.tokengeneration.saml2.statements;

import com.iplanet.sso.SSOToken;
import com.sun.identity.saml2.assertion.AuthzDecisionStatement;
import org.forgerock.openam.sts.config.user.SAML2Config;

import java.util.List;

/**
 * This interface defines the plug-in point for producing AuthzDecisionStatements. Note that there is no implementation
 * of the AuthzDecisionStatement in OpenAM, and no processing of these statements, other than calling toXMLString(bool, bool)
 * on them when toXMLString(bool, bool) is called on the encapsulating Assertion, and isMutable and makeImmutable.
 * The isMutable method should always return true prior to signature generation, and the makeImmutable is called after
 * the signature is generated to tell the object to reject any subsequent changes.
 *
 * Thus a 'bare-bones' implementation of this interface could return a List of implementations of the AuthzDecisionStatement
 * interface, where the implementation simply returns the xml string corresponding to the AuthzDecisionStatement in
 * toXMLString, and always return true from isMutable. Note that the toXMLString method of the AssertionImpl class should be
 * consulted to determine the proper formatting and character escaping in the String returned from toXMLString in the
 * AuthzDecisionStatement implementations.
 * @see com.sun.identity.saml2.assertion.AuthzDecisionStatement
 */
public interface AuthzDecisionStatementsProvider {
    /**
     * @param ssoToken The SSOToken corresponding to the asserted subject
     * @param config The SAML2Config state for the invoked STS instance.
     * @return The List of AuthzDecisionStatement instances to be included in the assertion. List must be non-null - return
     * Collections.emptyList() if no AuthzDecisionStatements are to be included in the assertion.
     */
    List<AuthzDecisionStatement> get(SSOToken ssoToken, SAML2Config config);
}

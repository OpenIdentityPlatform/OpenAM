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

import java.util.Collections;
import java.util.List;

/**
 * @see org.forgerock.openam.sts.tokengeneration.saml2.statements.AuthzDecisionStatementsProvider
 */
public class DefaultAuthzDecisionStatementsProvider implements AuthzDecisionStatementsProvider{
    public List<AuthzDecisionStatement> get(SSOToken ssoToken, SAML2Config config) {
        return Collections.emptyList();
    }
}

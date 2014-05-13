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

import com.sun.identity.saml2.assertion.AuthnStatement;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.config.user.SAML2Config;

import java.util.List;

/**
 * Defines the concern of providing the AuthnStatement list to be included in the generated SAML2 assertion. If no
 * custom interface implementation is specified in the SAML2Config, then the DefaultAuthenticationStatementsProvider will
 * be used.
 */
public interface AuthenticationStatementsProvider {
    /**
     * Invoked to obtain the List of AuthnStatement instances to be included in the generated SAML2 assertion.
     * @param saml2Config The STS-instance-specific SAML2 configurations
     * @param authnContextClassRef The AuthNContext class ref pulled out of the TokenGenerationServiceInvocationState.
     * @return The list of AuthnStatements
     * @throws TokenCreationException
     */
    List<AuthnStatement> get(SAML2Config saml2Config, String authnContextClassRef) throws TokenCreationException;
}

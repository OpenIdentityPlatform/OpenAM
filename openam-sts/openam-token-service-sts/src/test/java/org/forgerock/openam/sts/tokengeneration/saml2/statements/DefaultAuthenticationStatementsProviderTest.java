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
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertTrue;

public class DefaultAuthenticationStatementsProviderTest {
    private static final String AUTHN_CONTEXT = "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport";

    @Test
    public void testAuthNContext() throws TokenCreationException {
        AuthenticationStatementsProvider provider = new DefaultAuthenticationStatementsProvider();
        List<AuthnStatement> statements = provider.get(createSAML2Config(), AUTHN_CONTEXT);
        assertTrue(AUTHN_CONTEXT.equals(statements.get(0).getAuthnContext().getAuthnContextClassRef()));
    }

    private SAML2Config createSAML2Config() {
        Map<String, String> attributeMap = new HashMap<String, String>();
        attributeMap.put("email", "mail");

        return SAML2Config.builder()
                .attributeMap(attributeMap)
                .nameIdFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent")
                .spEntityId("http://host.com/sp/entity/id")
                .build();
    }
}

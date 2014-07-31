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
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.Attribute;
import com.sun.identity.saml2.assertion.AttributeStatement;
import com.sun.identity.saml2.common.SAML2Exception;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.config.user.SAML2Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encrypted attributes are currently not supported.
 * @see org.forgerock.openam.sts.tokengeneration.saml2.statements.AttributeStatementsProvider
 */
public class DefaultAttributeStatementsProvider implements AttributeStatementsProvider {
    /**
     * @see org.forgerock.openam.sts.tokengeneration.saml2.statements.AttributeStatementsProvider#get(com.iplanet.sso.SSOToken,
     * org.forgerock.openam.sts.config.user.SAML2Config, AttributeMapper)
     *
     */
    public List<AttributeStatement> get(SSOToken ssoToken, SAML2Config saml2Config, AttributeMapper mapper) throws TokenCreationException {
        AttributeStatement attributeStatement = AssertionFactory.getInstance().createAttributeStatement();
        try {
            List<Attribute> attributeList = mapper.getAttributes(ssoToken, saml2Config.getAttributeMap());
            if ((attributeList == null) || attributeList.isEmpty()) {
                return Collections.emptyList();
            } else {
                attributeStatement.setAttribute(attributeList);
            }
        } catch (SAML2Exception e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught setting attributes in DefaultAttributeStatementsProvider: " + e, e);
        }
        List<AttributeStatement> attributeStatements = new ArrayList<AttributeStatement>(1);
        attributeStatements.add(attributeStatement);
        return attributeStatements;
    }
}

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
import com.sun.identity.saml2.assertion.AttributeStatement;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.config.user.SAML2Config;

import java.util.List;

/**
 * Defines the concerns of generating the AttributeStatement list to be included in the SAML2 assertion. If no custom
 * class is specified in the SAML2Config, then the DefaultAttributeStatementsProvider will be used.
 */
public interface AttributeStatementsProvider {
    /**
     *
     * @param ssoToken The SSOToken corresponding to asserted subject
     * @param saml2Config The STS-instance-specific SAML2 configurations
     * @param attributeMapper The AttributeMapper implementation which will map attributes. If the AttributeMapper cannot map
     *                        any attributes, then an empty list should be returned.
     * @return The list of AttributeStatement instances containing the mapped attributes, or an empty list, if no attributes could
     * be mapped.
     * @throws TokenCreationException
     */
    List<AttributeStatement> get(SSOToken ssoToken, SAML2Config saml2Config, AttributeMapper attributeMapper) throws TokenCreationException;
}

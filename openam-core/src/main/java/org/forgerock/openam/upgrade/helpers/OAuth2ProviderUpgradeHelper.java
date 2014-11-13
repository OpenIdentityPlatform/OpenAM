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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.openam.upgrade.helpers;

import com.sun.identity.sm.AbstractUpgradeHelper;
import com.sun.identity.sm.AttributeSchemaImpl;
import org.forgerock.openam.upgrade.UpgradeException;

/**
 * This upgrade helper is used to add new default values to the OAuth2 Provider schema.
 *
 * @since 12.0.0
 */
public class OAuth2ProviderUpgradeHelper extends AbstractUpgradeHelper {

    private static final String SIGNING_ALGORITHMS = "forgerock-oauth2-provider-id-token-signing-algorithms-supported";

    public OAuth2ProviderUpgradeHelper() {
        attributes.add(SIGNING_ALGORITHMS);
    }

    @Override
    public AttributeSchemaImpl upgradeAttribute(AttributeSchemaImpl oldAttr, AttributeSchemaImpl newAttr)
            throws UpgradeException {

        if (SIGNING_ALGORITHMS.equals(newAttr.getName()) &&
                !oldAttr.getDefaultValues().equals(newAttr.getDefaultValues())) {
            return newAttr;
        }

        return null;
    }

}

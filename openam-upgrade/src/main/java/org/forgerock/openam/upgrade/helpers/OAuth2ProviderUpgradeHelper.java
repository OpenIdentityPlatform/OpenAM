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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.openam.upgrade.helpers;

import static org.forgerock.openam.oauth2.OAuth2Constants.OAuth2ProviderService.*;

import org.forgerock.openam.upgrade.UpgradeException;

import com.sun.identity.sm.AbstractUpgradeHelper;
import com.sun.identity.sm.AttributeSchemaImpl;

/**
 * This upgrade helper is used to add new default values to the OAuth2 Provider schema.
 *
 * @since 12.0.0
 */
public class OAuth2ProviderUpgradeHelper extends AbstractUpgradeHelper {

    /**
     * Default constructor
     */
    public OAuth2ProviderUpgradeHelper() {
        attributes.add(ID_TOKEN_SIGNING_ALGORITHMS);
        attributes.add(JKWS_URI);
        attributes.add(SUPPORTED_CLAIMS);
        attributes.add(OIDC_CLAIMS_EXTENSION_SCRIPT);
        attributes.add(SCOPE_PLUGIN_CLASS);

        attributes.add(AUTHZ_CODE_LIFETIME_NAME);
        attributes.add(REFRESH_TOKEN_LIFETIME_NAME);
        attributes.add(ACCESS_TOKEN_LIFETIME_NAME);
        attributes.add(JWT_TOKEN_LIFETIME_NAME);

        attributes.add(RESPONSE_TYPE_LIST);
    }

    @Override
    public AttributeSchemaImpl upgradeAttribute(AttributeSchemaImpl attributeToUpgrade,
            AttributeSchemaImpl attributeFromNewSchema) throws UpgradeException {

        boolean attributeNeedUpgrade = false;

        switch (attributeFromNewSchema.getName()) {
            case JKWS_URI:
                if (!attributeToUpgrade.getType().equals(attributeFromNewSchema.getType())) {
                    attributeNeedUpgrade = true;
                    attributeToUpgrade = attributeFromNewSchema;
                }
                break;
            case ID_TOKEN_SIGNING_ALGORITHMS:
            case SUPPORTED_CLAIMS:
            case OIDC_CLAIMS_EXTENSION_SCRIPT:
            case RESPONSE_TYPE_LIST:
            case SCOPE_PLUGIN_CLASS:
            case AUTHZ_CODE_LIFETIME_NAME:
            case REFRESH_TOKEN_LIFETIME_NAME:
            case ACCESS_TOKEN_LIFETIME_NAME:
            case JWT_TOKEN_LIFETIME_NAME:
                if (!attributeToUpgrade.getDefaultValues().equals(attributeFromNewSchema.getDefaultValues())) {
                    attributeNeedUpgrade = true;
                    attributeToUpgrade = updateDefaultValues(attributeToUpgrade, attributeFromNewSchema.getDefaultValues());
                }
                break;
        }

        if (attributeNeedUpgrade) {
            return attributeToUpgrade;
        } else {
            return null;
        }
    }
}

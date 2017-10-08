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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openam.upgrade.helpers;

import org.forgerock.openam.upgrade.UpgradeException;

import com.sun.identity.sm.AbstractUpgradeHelper;
import com.sun.identity.sm.AttributeSchemaImpl;

/**
 * This upgrade helper is used to add new default values to the Auth Authenticator OATH schema.
 *
 * @since 14.0.0
 */
public class AuthAuthenticatorOathHelper extends AbstractUpgradeHelper {
    private static final String ISSUER_NAME = "openam-auth-fr-oath-issuer-name";

    /**
     * Default Constructor
     */
    public AuthAuthenticatorOathHelper() {
        attributes.add(ISSUER_NAME);
    }

    @Override
    public AttributeSchemaImpl upgradeAttribute(AttributeSchemaImpl attributeToUpgrade,
            AttributeSchemaImpl attributeFromNewSchema) throws UpgradeException {

        boolean attributeNeedUpgrade = false;
        switch (attributeFromNewSchema.getName()) {
            case ISSUER_NAME:
                if (!attributeToUpgrade.getExampleValues().equals(attributeFromNewSchema.getExampleValues())) {
                    attributeToUpgrade = updateExampleValues(attributeToUpgrade, attributeFromNewSchema.getExampleValues());
                    attributeNeedUpgrade = true;
                }
                if (!attributeToUpgrade.getDefaultValues().equals(attributeFromNewSchema.getDefaultValues())) {
                    attributeToUpgrade = updateDefaultValues(attributeToUpgrade, attributeFromNewSchema.getDefaultValues());
                    attributeNeedUpgrade = true;
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

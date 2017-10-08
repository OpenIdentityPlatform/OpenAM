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
 * Upgrade Helper for iPlanetAMAuthOpenIdConnectService service to ensure that AttributeSchema modifications are applied
 * correctly during upgrades.
 */
public class OpenIdConnectAuthModuleServiceHelper extends AbstractUpgradeHelper {

    private static final String PRINCIPAL_MAPPER_ATTR = "openam-auth-openidconnect-principal-mapper-class";
    private static final String ACCOUNT_PROVIDER_ATTR = "openam-auth-openidconnect-account-provider-class";

    public OpenIdConnectAuthModuleServiceHelper() {
        attributes.add(PRINCIPAL_MAPPER_ATTR);
        attributes.add(ACCOUNT_PROVIDER_ATTR);
    }

    @Override
    public AttributeSchemaImpl upgradeAttribute(AttributeSchemaImpl oldAttr, AttributeSchemaImpl newAttr) throws UpgradeException {
        if (!newAttr.getI18NKey().equals(oldAttr.getI18NKey())) {
            return newAttr;
        }
        return null;
    }
}

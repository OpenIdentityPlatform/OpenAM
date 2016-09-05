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

import java.util.HashSet;
import java.util.Set;

import com.sun.identity.sm.AbstractUpgradeHelper;
import com.sun.identity.sm.AttributeSchemaImpl;
import org.forgerock.openam.upgrade.UpgradeException;

/**
 * This upgrade helper allows the modification of attributes for the Dashboard Service.
 *
 * @since 14.0.0
 */
public class DashboardServiceHelper extends AbstractUpgradeHelper {


    private static final String DASHBOARD_CLASS_NAME = "dashboardClassName";
    private static final String DASHBOARD_NAME = "dashboardName";
    private static final String DASHBOARD_DISPLAY_NAME = "dashboardDisplayName";
    private static final String DASHBOARD_ICON = "dashboardIcon";
    private static final String DASHBOARD_LOGIN = "dashboardLogin";
    private static final String ICF_IDENTIFIER = "ICFIdentifier";

    private final Set<String> attributesRequireUpdate = new HashSet<>();

    /**
     * Constructs an instance of the DashboardServiceHelper
     */
    public DashboardServiceHelper() {
        attributesRequireUpdate.add(DASHBOARD_CLASS_NAME);
        attributesRequireUpdate.add(DASHBOARD_NAME);
        attributesRequireUpdate.add(DASHBOARD_DISPLAY_NAME);
        attributesRequireUpdate.add(DASHBOARD_ICON);
        attributesRequireUpdate.add(DASHBOARD_LOGIN);
        attributesRequireUpdate.add(ICF_IDENTIFIER);
        attributes.addAll(attributesRequireUpdate);
    }

    @Override
    public AttributeSchemaImpl upgradeAttribute(AttributeSchemaImpl oldAttr,
            AttributeSchemaImpl newAttr) throws UpgradeException {
        if(attributesRequireUpdate.contains(newAttr.getName()) && !newAttr.getI18NKey().equals(oldAttr.getI18NKey())) {
            return newAttr;
        }
        return null;
    }
}

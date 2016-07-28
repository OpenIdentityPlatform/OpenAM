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

import com.sun.identity.sm.AbstractUpgradeHelper;
import com.sun.identity.sm.AttributeSchemaImpl;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;

import java.util.Collections;

/**
 * This upgrade helper allows the modification of attributes for the SAMLv2 config service.
 */
public class SAML2ConfigHelper extends AbstractUpgradeHelper {

    private static final String CACHE_CLEANUP_INTERVAL = "CacheCleanupInterval";
    private static final int MINIMUM_CACHE_CLEANUP_INTERVAL = 300;
    /**
     * The constructor for SAML2ConfigHelper is used to register the attributes which will be upgraded.
     */
    public SAML2ConfigHelper() {
        attributes.add(CACHE_CLEANUP_INTERVAL);
    }

    @Override
    public AttributeSchemaImpl upgradeAttribute(AttributeSchemaImpl oldAttr, AttributeSchemaImpl newAttr)
            throws UpgradeException {
        if (CACHE_CLEANUP_INTERVAL.equals(oldAttr.getName()) && StringUtils.isEmpty(oldAttr.getStartRange())) {
            String currentValue = CollectionUtils.getFirstItem(oldAttr.getDefaultValues());
            if (currentValue != null && Integer.parseInt(currentValue) < MINIMUM_CACHE_CLEANUP_INTERVAL) {
                currentValue = String.valueOf(MINIMUM_CACHE_CLEANUP_INTERVAL);
            }
            updateDefaultValues(newAttr, Collections.singleton(currentValue));
            return newAttr;
        }
        return null;
    }
}

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

import java.util.Set;

import static org.forgerock.openam.utils.CollectionUtils.asSet;

/**
 * Used to upgrade the iPlanetAMLoggingService.
 *
 * @since 12.0.0
 */
public class LoggingUpgradeHelper extends AbstractUpgradeHelper {

    private static final String SUN_AM_LOG_LEVEL_ATTR = "sun-am-log-level";
    private static final String NATURAL_LIST_ORDER = "natural";

    /**
     * Constructs a new LoggingUpgradeHelper instance, add configures the logging attributes that are to updated.
     */
    public LoggingUpgradeHelper() {
        attributes.add(SUN_AM_LOG_LEVEL_ATTR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AttributeSchemaImpl upgradeAttribute(AttributeSchemaImpl oldAttr, AttributeSchemaImpl newAttr)
            throws UpgradeException {

        if (SUN_AM_LOG_LEVEL_ATTR.equals(newAttr.getName()) && oldAttr.getDefaultValues().isEmpty()) {
            updateDefaultValues(newAttr, asSet(NATURAL_LIST_ORDER));
            return newAttr;
        }

        return null;
    }
}

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
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.AttributeSchemaImpl;
import java.util.Arrays;
import org.forgerock.openam.upgrade.UpgradeException;

/**
 * Used to upgrade the iPlanetAMLoggingService.
 *
 * @since 12.0.0
 */
public class LoggingUpgradeHelper extends AbstractUpgradeHelper {

    private static final String SUN_AM_LOG_LEVEL_ATTR = "sun-am-log-level";
    private static final String LOGGING_TYPE = "iplanet-am-logging-type";
    private static final String SYSLOG_LOGGING_TYPE = "Syslog";

    /**
     * Constructs a new LoggingUpgradeHelper instance, add configures the logging attributes that are to updated.
     */
    public LoggingUpgradeHelper() {
        attributes.add(SUN_AM_LOG_LEVEL_ATTR);
        attributes.add(LOGGING_TYPE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AttributeSchemaImpl upgradeAttribute(AttributeSchemaImpl oldAttr, AttributeSchemaImpl newAttr)
            throws UpgradeException {
        if (SUN_AM_LOG_LEVEL_ATTR.equals(newAttr.getName())
                && !AttributeSchema.ListOrder.INSERTION.equals(oldAttr.getListOrder())) {
            return newAttr;
        } else if (LOGGING_TYPE.equals(newAttr.getName())
                && !Arrays.asList(oldAttr.getChoiceValues()).contains(SYSLOG_LOGGING_TYPE)) {
            newAttr = updateDefaultValues(newAttr, oldAttr.getDefaultValues());
            return newAttr;
        }

        return null;
    }
}

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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.upgrade.helpers;

import static java.util.Collections.singleton;

import com.sun.identity.sm.AbstractUpgradeHelper;
import com.sun.identity.sm.AttributeSchemaImpl;
import org.forgerock.openam.upgrade.UpgradeException;

/**
 * For Upgrade from 12.0.x to 13.0.0 we want to disable the new Audit Logging framework. Current audit settings should
 * not be modified.
 *
 * @since 13.0.0
 */
public final class AuditUpgradeHelper extends AbstractUpgradeHelper {

    /**
     * The constructor for {@code AuditUpgradeHelper} is used to register the attributes which will be upgraded.
     */
    public AuditUpgradeHelper() {
        attributes.add("auditEnabled");
    }

    @Override
    public AttributeSchemaImpl upgradeAttribute(AttributeSchemaImpl newAttr) throws UpgradeException {
        return updateDefaultValues(newAttr, singleton("false"));
    }

    @Override
    public AttributeSchemaImpl upgradeAttribute(AttributeSchemaImpl oldAttr, AttributeSchemaImpl newAttr)
            throws UpgradeException {
        return null;
    }
}

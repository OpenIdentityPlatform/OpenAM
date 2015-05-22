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

import com.sun.identity.sm.AbstractUpgradeHelper;
import com.sun.identity.sm.AttributeSchemaImpl;
import org.forgerock.openam.upgrade.UpgradeException;

import java.util.HashSet;
import java.util.Set;

/**
 * This upgrade helper allows the modification of attributes for the Scripted Auth module.
 *
 * @since 13.0.0
 */
public class ScriptedAuthHelper extends AbstractUpgradeHelper {

    private final Set<String> attributeNames = new HashSet<>();

    /**
     * The constructor for ScriptedAuthHelper is used to register the attributes which will be upgraded.
     */
    public ScriptedAuthHelper() {
        attributeNames.add("iplanet-am-auth-scripted-client-script");
        attributeNames.add("iplanet-am-auth-scripted-server-script");
        attributes.addAll(attributeNames);
    }

    @Override
    public AttributeSchemaImpl upgradeAttribute(AttributeSchemaImpl oldAttr, AttributeSchemaImpl newAttr)
            throws UpgradeException {

        if (attributeNames.contains(newAttr.getName())) {
            return newAttr;
        }
        return null;
    }
}

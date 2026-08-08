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
 * Copyright 2026 3A Systems, LLC.
 */
package org.forgerock.openam.upgrade.helpers;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.forgerock.openam.upgrade.UpgradeException;

import com.sun.identity.sm.AbstractUpgradeHelper;
import com.sun.identity.sm.AttributeSchemaImpl;

/**
 * This upgrade helper keeps the choice values of the Scripting Service {@code defaultScriptContext} attribute in
 * sync with the service definition, so that script contexts introduced in later versions become available on
 * upgraded instances. The administrator's configured default is preserved.
 */
public class ScriptingServiceHelper extends AbstractUpgradeHelper {

    private static final String DEFAULT_SCRIPT_CONTEXT = "defaultScriptContext";

    /**
     * Default constructor
     */
    public ScriptingServiceHelper() {
        attributes.add(DEFAULT_SCRIPT_CONTEXT);
    }

    @Override
    public AttributeSchemaImpl upgradeAttribute(AttributeSchemaImpl attributeToUpgrade,
            AttributeSchemaImpl attributeFromNewSchema) throws UpgradeException {

        if (asSet(attributeToUpgrade.getChoiceValues()).equals(asSet(attributeFromNewSchema.getChoiceValues()))) {
            return null;
        }
        // For Global attributes the administrator's configured value is persisted as the schema default, so
        // carry it over instead of reverting to the default bundled in the service definition.
        Set<String> existingDefaults = attributeToUpgrade.getDefaultValues();
        if (existingDefaults.isEmpty()) {
            return attributeFromNewSchema;
        }
        return updateDefaultValues(attributeFromNewSchema, existingDefaults);
    }

    private static Set<String> asSet(String[] values) {
        return values == null ? Collections.<String>emptySet() : new HashSet<>(Arrays.asList(values));
    }
}

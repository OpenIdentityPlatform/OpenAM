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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.forgerock.openam.upgrade.UpgradeException;
import org.w3c.dom.Node;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.sm.AbstractUpgradeHelper;
import com.sun.identity.sm.AttributeSchemaImpl;

/**
 * This upgrade helper keeps the script context choice values declared by the Scripting Service schema — the
 * global {@code defaultScriptContext} attribute and the {@code context} attribute of the realm-level
 * {@code scriptConfiguration} sub-schema — in sync with the service definition, so that script contexts
 * introduced in later versions become available on upgraded instances. The administrator's configured default
 * is preserved as long as it is still a valid choice.
 */
public class ScriptingServiceHelper extends AbstractUpgradeHelper {

    private static final String DEFAULT_SCRIPT_CONTEXT = "defaultScriptContext";
    private static final String SCRIPT_CONTEXT = "context";
    private static final String CHOICE_VALUES = "ChoiceValues";
    private static final String CHOICE_VALUE = "ChoiceValue";

    /**
     * Default constructor
     */
    public ScriptingServiceHelper() {
        attributes.add(DEFAULT_SCRIPT_CONTEXT);
        attributes.add(SCRIPT_CONTEXT);
    }

    @Override
    public AttributeSchemaImpl upgradeAttribute(AttributeSchemaImpl attributeToUpgrade,
            AttributeSchemaImpl attributeFromNewSchema) throws UpgradeException {

        Set<String> newChoiceValues = getRawChoiceValues(attributeFromNewSchema);
        if (getRawChoiceValues(attributeToUpgrade).equals(newChoiceValues)) {
            return null;
        }
        // For Global attributes the administrator's configured value is persisted as the schema default, so
        // carry it over instead of reverting to the default bundled in the service definition — unless that
        // value is no longer a valid choice.
        Set<String> existingDefaults = attributeToUpgrade.getDefaultValues();
        if (existingDefaults.isEmpty() || !newChoiceValues.containsAll(existingDefaults)) {
            return attributeFromNewSchema;
        }
        return updateDefaultValues(attributeFromNewSchema, existingDefaults);
    }

    /**
     * Read the choice values from the raw attribute schema node. {@link AttributeSchemaImpl} only parses
     * {@code <ChoiceValues>} for choice-typed attributes, while the service definition also declares them on
     * the {@code type="single"} attribute {@code scriptConfiguration.context}.
     */
    private static Set<String> getRawChoiceValues(AttributeSchemaImpl attribute) {
        Set<String> values = new HashSet<>();
        Node choiceValuesNode = XMLUtils.getChildNode(attribute.getAttributeSchemaNode(), CHOICE_VALUES);
        if (choiceValuesNode != null) {
            for (Iterator it = XMLUtils.getChildNodes(choiceValuesNode, CHOICE_VALUE).iterator(); it.hasNext();) {
                values.add(XMLUtils.getValueOfValueNode((Node) it.next()));
            }
        }
        return values;
    }
}

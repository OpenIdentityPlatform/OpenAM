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

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;
import org.w3c.dom.Node;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.sm.AttributeSchemaImpl;

/**
 * Unit test to exercise the behaviour of {@link ScriptingServiceHelper}.
 */
public class ScriptingServiceHelperTest {

    private static final String OLD_CHOICE_VALUES =
            "<ChoiceValues>"
            + "<ChoiceValue i18nKey=\"script-type-01\">POLICY_CONDITION</ChoiceValue>"
            + "<ChoiceValue i18nKey=\"script-type-02\">AUTHENTICATION_SERVER_SIDE</ChoiceValue>"
            + "<ChoiceValue i18nKey=\"script-type-03\">AUTHENTICATION_CLIENT_SIDE</ChoiceValue>"
            + "<ChoiceValue i18nKey=\"script-type-04\">OIDC_CLAIMS</ChoiceValue>"
            + "</ChoiceValues>";
    private static final String NEW_CHOICE_VALUES = OLD_CHOICE_VALUES.replace("</ChoiceValues>",
            "<ChoiceValue i18nKey=\"script-type-05\">OAUTH2_ACCESS_TOKEN_MODIFICATION</ChoiceValue>"
            + "</ChoiceValues>");

    private final ScriptingServiceHelper helper = new ScriptingServiceHelper();

    @Test
    public void addsNewChoiceValuesPreservingConfiguredDefault() throws Exception {
        AttributeSchemaImpl upgraded = helper.upgradeAttribute(
                attributeSchema(OLD_CHOICE_VALUES, "OIDC_CLAIMS"),
                attributeSchema(NEW_CHOICE_VALUES, "POLICY_CONDITION"));

        assertThat(upgraded).isNotNull();
        assertThat(upgraded.getChoiceValues()).contains("OAUTH2_ACCESS_TOKEN_MODIFICATION");
        assertThat(upgraded.getDefaultValues()).containsExactly("OIDC_CLAIMS");
    }

    @Test
    public void returnsNullWhenChoiceValuesAreUnchanged() throws Exception {
        assertThat(helper.upgradeAttribute(
                attributeSchema(NEW_CHOICE_VALUES, "OIDC_CLAIMS"),
                attributeSchema(NEW_CHOICE_VALUES, "POLICY_CONDITION"))).isNull();
    }

    @Test
    public void usesBundledDefaultWhenNoDefaultIsConfigured() throws Exception {
        AttributeSchemaImpl upgraded = helper.upgradeAttribute(
                attributeSchema(OLD_CHOICE_VALUES, null),
                attributeSchema(NEW_CHOICE_VALUES, "POLICY_CONDITION"));

        assertThat(upgraded).isNotNull();
        assertThat(upgraded.getChoiceValues()).contains("OAUTH2_ACCESS_TOKEN_MODIFICATION");
        assertThat(upgraded.getDefaultValues()).containsExactly("POLICY_CONDITION");
    }

    private static AttributeSchemaImpl attributeSchema(String choiceValues, String defaultValue) {
        String xml = "<AttributeSchema name=\"defaultScriptContext\" type=\"single_choice\" syntax=\"string\">"
                + choiceValues
                + (defaultValue == null ? "" : "<DefaultValues><Value>" + defaultValue + "</Value></DefaultValues>")
                + "</AttributeSchema>";
        Node node = XMLUtils.toDOMDocument(xml, null).getDocumentElement();
        return new TestAttributeSchemaImpl(node);
    }

    /** Grants access to the protected {@link AttributeSchemaImpl} constructor. */
    private static final class TestAttributeSchemaImpl extends AttributeSchemaImpl {

        TestAttributeSchemaImpl(Node node) {
            super(node);
        }
    }
}

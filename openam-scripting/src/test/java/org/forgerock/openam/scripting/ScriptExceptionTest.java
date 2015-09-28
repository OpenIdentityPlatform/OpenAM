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
package org.forgerock.openam.scripting;

import static java.text.MessageFormat.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptErrorCode;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptErrorCode.*;

import org.testng.annotations.Test;

import java.util.Locale;

public class ScriptExceptionTest {
    final String context = "TEST_CONTEXT";
    final String language = "TEST_LANGUAGE";
    final String scriptName = "TestScript";
    final String realm = "TestRealm";
    final String uuid = "1234567890";

    @Test
    public void shouldMapScriptErrorCodeToMessage() {
        // given
        ScriptException se;

        // when
        se = new ScriptException(CONTEXT_NOT_RECOGNISED, context);
        //then
        assertThat(se.getMessage()).isEqualTo(format("Script type not recognised: {0}", context));

        // when
        se = new ScriptException(LANGUAGE_NOT_SUPPORTED, language);
        //then
        assertThat(se.getMessage()).isEqualTo(format("Scripting language not supported: {0}", language));

        // when
        se = new ScriptException(FIND_BY_NAME_FAILED, scriptName, realm);
        //then
        assertThat(se.getMessage()).isEqualTo(format("Failed to read script called {0} from realm {1}", scriptName,
                realm));

        // when
        se = new ScriptException(FIND_BY_UUID_FAILED, uuid, realm);
        //then
        assertThat(se.getMessage()).isEqualTo(format("Failed to read script with UUID {0} from realm " + "{1}", uuid,
                realm));

        // when
        se = new ScriptException(DELETE_FAILED, uuid, realm);
        //then
        assertThat(se.getMessage()).isEqualTo(format("Failed to delete script with UUID {0} from realm " + "{1}",
                uuid, realm));

        // when
        se = new ScriptException(RETRIEVE_FAILED, uuid, realm);
        //then
        assertThat(se.getMessage()).isEqualTo(format("Failed to retrieve script with UUID {0} from " + "realm {1}",
                uuid, realm));

        // when
        se = new ScriptException(RETRIEVE_ALL_FAILED, realm);
        //then
        assertThat(se.getMessage()).isEqualTo(format("Failed to retrieve scripts from realm {0}", realm));

        // when
        se = new ScriptException(SAVE_FAILED, uuid, realm);
        //then
        assertThat(se.getMessage()).isEqualTo(format("Failed to save script with UUID {0} in realm " + "{1}", uuid,
                realm));

        // when
        se = new ScriptException(MISSING_SCRIPT_UUID);
        //then
        assertThat(se.getMessage()).isEqualTo("Script UUID must be specified");

        // when
        se = new ScriptException(MISSING_SCRIPT_NAME);
        //then
        assertThat(se.getMessage()).isEqualTo("Script name must be specified");

        // when
        se = new ScriptException(MISSING_SCRIPT);
        //then
        assertThat(se.getMessage()).isEqualTo("A script must be specified");

        // when
        se = new ScriptException(MISSING_SCRIPTING_LANGUAGE);
        //then
        assertThat(se.getMessage()).isEqualTo("Scripting language must be specified");

        // when
        se = new ScriptException(MISSING_SCRIPT_CONTEXT);
        //then
        assertThat(se.getMessage()).isEqualTo("Script type must be specified");

        // when
        se = new ScriptException(SCRIPT_NAME_EXISTS, scriptName, realm);
        //then
        assertThat(se.getMessage()).isEqualTo(format("Script with name {0} already exist in realm {1}", scriptName,
                realm));

        // when
        se = new ScriptException(SCRIPT_UUID_EXISTS, uuid, realm);
        //then
        assertThat(se.getMessage()).isEqualTo(format("Script with UUID {0} already exist in realm {1}", uuid, realm));

        // when
        se = new ScriptException(SCRIPT_UUID_NOT_FOUND, uuid, realm);
        //then
        assertThat(se.getMessage()).isEqualTo(format("Script with UUID {0} could not be found in realm {1}", uuid,
                realm));

        // when
        se = new ScriptException(FILTER_BOOLEAN_LITERAL_FALSE);
        //then
        assertThat(se.getMessage()).isEqualTo("The 'boolean literal' filter with value of 'false' is not supported");

        // when
        se = new ScriptException(FILTER_EXTENDED_MATCH);
        //then
        assertThat(se.getMessage()).isEqualTo("The 'extended match' filter is not supported");

        // when
        se = new ScriptException(FILTER_GREATER_THAN);
        //then
        assertThat(se.getMessage()).isEqualTo("The 'greater than' filter is not supported");

        // when
        se = new ScriptException(FILTER_GREATER_THAN_OR_EQUAL);
        //then
        assertThat(se.getMessage()).isEqualTo("The 'greater than or equal' filter is not supported");

        // when
        se = new ScriptException(FILTER_LESS_THAN);
        //then
        assertThat(se.getMessage()).isEqualTo("The 'less than' filter is not supported");

        // when
        se = new ScriptException(FILTER_LESS_THAN_OR_EQUAL);
        //then
        assertThat(se.getMessage()).isEqualTo("The 'less than or equal' filter is not supported");

        // when
        se = new ScriptException(FILTER_NOT);
        //then
        assertThat(se.getMessage()).isEqualTo("The 'not' filter is not supported");

        // when
        se = new ScriptException(FILTER_PRESENT);
        //then
        assertThat(se.getMessage()).isEqualTo("The 'present' filter is not supported");
    }

    @Test
    public void shouldMapScriptErrorCodeToLocalisedMessage() {
        // given
        ScriptException se;
        Locale locale = new Locale("te");

        for (ScriptErrorCode errorCode : ScriptErrorCode.values()) {
            // when
            se = new ScriptException(errorCode);
            //then
            assertThat(se.getL10NMessage(locale)).isEqualTo(format(errorCode.name() + "-TRANSLATED"));
        }
    }
}

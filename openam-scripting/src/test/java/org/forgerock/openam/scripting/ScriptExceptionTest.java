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

import static org.forgerock.openam.scripting.ScriptConstants.ScriptErrorCode.*;
import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import java.text.MessageFormat;
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
        assertEquals(se.getMessage(),
                MessageFormat.format("Scripting context not recognised: {0}", context));

        // when
        se = new ScriptException(LANGUAGE_NOT_SUPPORTED, language);
        //then
        assertEquals(se.getMessage(),
                MessageFormat.format("Scripting language not supported: {0}", language));

        // when
        se = new ScriptException(FIND_BY_NAME_FAILED, scriptName, realm);
        //then
        assertEquals(se.getMessage(),
                MessageFormat.format("Failed to read script called {0} from realm {1}", scriptName, realm));

        // when
        se = new ScriptException(FIND_BY_UUID_FAILED, uuid, realm);
        //then
        assertEquals(se.getMessage(),
                MessageFormat.format("Failed to read script with UUID {0} from realm {1}", uuid, realm));

        // when
        se = new ScriptException(DELETE_FAILED, uuid, realm);
        //then
        assertEquals(se.getMessage(),
                MessageFormat.format("Failed to delete script with UUID {0} from realm {1}", uuid, realm));

        // when
        se = new ScriptException(RETRIEVE_FAILED, uuid, realm);
        //then
        assertEquals(se.getMessage(),
                MessageFormat.format("Failed to retrieve script with UUID {0} from realm {1}", uuid, realm));

        // when
        se = new ScriptException(RETRIEVE_ALL_FAILED, realm);
        //then
        assertEquals(se.getMessage(),
                MessageFormat.format("Failed to retrieve scripts from realm {0}", realm));

        // when
        se = new ScriptException(SAVE_FAILED, uuid, realm);
        //then
        assertEquals(se.getMessage(),
                MessageFormat.format("Failed to save script with UUID {0} in realm {1}", uuid, realm));

        // when
        se = new ScriptException(MISSING_SCRIPT_UUID);
        //then
        assertEquals(se.getMessage(), "Script UUID must be specified");

        // when
        se = new ScriptException(MISSING_SCRIPT_NAME);
        //then
        assertEquals(se.getMessage(), "Script name must be specified");

        // when
        se = new ScriptException(MISSING_SCRIPT);
        //then
        assertEquals(se.getMessage(), "A script must be specified");

        // when
        se = new ScriptException(MISSING_SCRIPTING_LANGUAGE);
        //then
        assertEquals(se.getMessage(), "Scripting language must be specified");

        // when
        se = new ScriptException(MISSING_SCRIPT_CONTEXT);
        //then
        assertEquals(se.getMessage(), "Script context must be specified");

        // when
        se = new ScriptException(SCRIPT_NAME_EXISTS, scriptName, realm);
        //then
        assertEquals(se.getMessage(),
                MessageFormat.format("Script with name {0} already exist in realm {1}", scriptName, realm));

        // when
        se = new ScriptException(SCRIPT_UUID_EXISTS, uuid, realm);
        //then
        assertEquals(se.getMessage(),
                MessageFormat.format("Script with UUID {0} already exist in realm {1}", uuid, realm));

        // when
        se = new ScriptException(SCRIPT_UUID_NOT_FOUND, uuid, realm);
        //then
        assertEquals(se.getMessage(),
                MessageFormat.format("Script with UUID {0} could not be found in realm {1}", uuid, realm));

        // when
        se = new ScriptException(FILTER_BOOLEAN_LITERAL_FALSE);
        //then
        assertEquals(se.getMessage(), "The 'boolean literal' filter with value of 'false' is not supported");

        // when
        se = new ScriptException(FILTER_EXTENDED_MATCH);
        //then
        assertEquals(se.getMessage(), "The 'extended match' filter is not supported");

        // when
        se = new ScriptException(FILTER_GREATER_THAN);
        //then
        assertEquals(se.getMessage(), "The 'greater than' filter is not supported");

        // when
        se = new ScriptException(FILTER_GREATER_THAN_OR_EQUAL);
        //then
        assertEquals(se.getMessage(), "The 'greater than or equal' filter is not supported");

        // when
        se = new ScriptException(FILTER_LESS_THAN);
        //then
        assertEquals(se.getMessage(), "The 'less than' filter is not supported");

        // when
        se = new ScriptException(FILTER_LESS_THAN_OR_EQUAL);
        //then
        assertEquals(se.getMessage(), "The 'less than or equal' filter is not supported");

        // when
        se = new ScriptException(FILTER_NOT);
        //then
        assertEquals(se.getMessage(), "The 'not' filter is not supported");

        // when
        se = new ScriptException(FILTER_PRESENT);
        //then
        assertEquals(se.getMessage(), "The 'present' filter is not supported");

        // when
        se = new ScriptException(SCRIPT_ENCODING_FAILED, "UTF-8");
        //then
        assertEquals(se.getMessage(), MessageFormat.format("Failed to encode script as {0}", "UTF-8"));
    }

    @Test
    public void shouldMapScriptErrorCodeToLocalisedMessage() {
        // given
        ScriptException se;
        Locale locale = new Locale("af");

        // when
        se = new ScriptException(CONTEXT_NOT_RECOGNISED, context);
        //then
        assertEquals(se.getL10NMessage(locale),
                MessageFormat.format("Skrip konteks kan nie erken word nie: {0}", context));

        // when
        se = new ScriptException(LANGUAGE_NOT_SUPPORTED, language);
        //then
        assertEquals(se.getL10NMessage(locale),
                MessageFormat.format("Skrip taal word nie geondersteun nie: {0}", language));

        // when
        se = new ScriptException(FIND_BY_NAME_FAILED, scriptName, realm);
        //then
        assertEquals(se.getL10NMessage(locale),
                MessageFormat.format("Lees van skrip genaamd {0} uit realm {1} het misluk", scriptName, realm));

        // when
        se = new ScriptException(FIND_BY_UUID_FAILED, uuid, realm);
        //then
        assertEquals(se.getL10NMessage(locale),
                MessageFormat.format("Lees van skrip met UUID {0} uit realm {1} het misluk", uuid, realm));

        // when
        se = new ScriptException(DELETE_FAILED, uuid, realm);
        //then
        assertEquals(se.getL10NMessage(locale),
                MessageFormat.format("Verwydering van skrip met UUID {0} uit realm {1} het misluk", uuid, realm));

        // when
        se = new ScriptException(RETRIEVE_FAILED, uuid, realm);
        //then
        assertEquals(se.getL10NMessage(locale),
                MessageFormat.format("Ontrek van skrip met UUID {0} uit realm {1} het misluk", uuid, realm));

        // when
        se = new ScriptException(RETRIEVE_ALL_FAILED, realm);
        //then
        assertEquals(se.getL10NMessage(locale),
                MessageFormat.format("Ontrek van skripte uit realm {0} het misluk", realm));

        // when
        se = new ScriptException(SAVE_FAILED, uuid, realm);
        //then
        assertEquals(se.getL10NMessage(locale),
                MessageFormat.format("Stooring van skrip met UUID {0} in realm {1} het misluk", uuid, realm));

        // when
        se = new ScriptException(MISSING_SCRIPT_UUID);
        //then
        assertEquals(se.getL10NMessage(locale), "Skrip UUID moet gespesifiseer word");

        // when
        se = new ScriptException(MISSING_SCRIPT_NAME);
        //then
        assertEquals(se.getL10NMessage(locale), "Skrip naam moet gespesifiseer word");

        // when
        se = new ScriptException(MISSING_SCRIPT);
        //then
        assertEquals(se.getL10NMessage(locale), "'n Skrip moet gespesifiseer word");

        // when
        se = new ScriptException(MISSING_SCRIPTING_LANGUAGE);
        //then
        assertEquals(se.getL10NMessage(locale), "Skrip taal moet gespesifiseer word");

        // when
        se = new ScriptException(MISSING_SCRIPT_CONTEXT);
        //then
        assertEquals(se.getL10NMessage(locale), "Skrip konteks moet gespesifiseer word");

        // when
        se = new ScriptException(SCRIPT_NAME_EXISTS, scriptName, realm);
        //then
        assertEquals(se.getL10NMessage(locale),
                MessageFormat.format("Skrip genaamd {0} bestaan reeds in realm {1}", scriptName, realm));

        // when
        se = new ScriptException(SCRIPT_UUID_EXISTS, uuid, realm);
        //then
        assertEquals(se.getL10NMessage(locale),
                MessageFormat.format("Skrip met UUID {0} bestaan reeds in realm {1}", uuid, realm));

        // when
        se = new ScriptException(SCRIPT_UUID_NOT_FOUND, uuid, realm);
        //then
        assertEquals(se.getL10NMessage(locale),
                MessageFormat.format("Skrip met UUID {0} kon nie gevind word in realm {1}", uuid, realm));

        // when
        se = new ScriptException(FILTER_BOOLEAN_LITERAL_FALSE);
        //then
        assertEquals(se.getL10NMessage(locale),
                "Die 'boolean literal' filter met waarde van 'false' word nie geondersteun nie");

        // when
        se = new ScriptException(FILTER_EXTENDED_MATCH);
        //then
        assertEquals(se.getL10NMessage(locale), "Die 'extended match' filter word nie geondersteun nie");

        // when
        se = new ScriptException(FILTER_GREATER_THAN);
        //then
        assertEquals(se.getL10NMessage(locale), "Die 'greater than' filter word nie geondersteun nie");

        // when
        se = new ScriptException(FILTER_GREATER_THAN_OR_EQUAL);
        //then
        assertEquals(se.getL10NMessage(locale), "Die 'greater than or equal' filter word nie geondersteun nie");

        // when
        se = new ScriptException(FILTER_LESS_THAN);
        //then
        assertEquals(se.getL10NMessage(locale), "Die 'less than' filter word nie geondersteun nie");

        // when
        se = new ScriptException(FILTER_LESS_THAN_OR_EQUAL);
        //then
        assertEquals(se.getL10NMessage(locale), "Die 'less than or equal' filter word nie geondersteun nie");

        // when
        se = new ScriptException(FILTER_NOT);
        //then
        assertEquals(se.getL10NMessage(locale), "Die 'not' filter word nie geondersteun nie");

        // when
        se = new ScriptException(FILTER_PRESENT);
        //then
        assertEquals(se.getL10NMessage(locale), "Die 'present' filter word nie geondersteun nie");

        // when
        se = new ScriptException(SCRIPT_ENCODING_FAILED, "UTF-8");
        //then
        assertEquals(se.getL10NMessage(locale), MessageFormat.format("Kodering van skrip as {0} het misluk", "UTF-8"));
    }
}

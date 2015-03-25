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

/**
 * Constants used for managing scripts.
 *
 * @since 13.0.0
 */
public final class ScriptConstants {

    private ScriptConstants() {
        throw new RuntimeException("Constructor for ScriptConstants is not supported.");
    }

    /**
     * Resource bundle used for error messages.
     */
    public static final String RESOURCE_BUNDLE = "scripting";

    public static final String SCRIPT_NAME = "name";
    public static final String JSON_UUID = "_id";
    public static final String SCRIPT_TEXT = "script";
    public static final String SCRIPT_LANGUAGE = "language";
    public static final String SCRIPT_CONTEXT = "context";
    public static final String SCRIPT_DESCRIPTION = "description";
    public static final String SCRIPT_CREATED_BY = "createdBy";
    public static final String SCRIPT_CREATION_DATE = "creationDate";
    public static final String SCRIPT_LAST_MODIFIED_BY = "lastModifiedBy";
    public static final String SCRIPT_LAST_MODIFIED_DATE = "lastModifiedDate";
    public static final String SERVICE_NAME = "ScriptingService";
    public static final String SCRIPT_CONFIGURATION = "scriptConfiguration";
    public static final String SCRIPT_CONFIGURATIONS = "scriptConfigurations";
    public static final String EMPTY = "";

    /**
     * The context in which a script will be used.
     */
    public static enum ScriptContext {
        AUTHENTICATION_SERVER_SIDE,
        AUTHENTICATION_CLIENT_SIDE,
        AUTHORIZATION_ENTITLEMENT_CONDITION
    }

    /**
     * Error messages are stored in the scripting.properties file to facilitate translation. Each entry in this
     * enum corresponds to a specific error message in the file keyed on the code.
     */
    public static enum ScriptErrorCode {
        CONTEXT_NOT_RECOGNISED("1"),
        LANGUAGE_NOT_SUPPORTED("2"),
        FIND_BY_NAME_FAILED("3"),
        FIND_BY_UUID_FAILED("4"),
        DELETE_FAILED("5"),
        RETRIEVE_FAILED("6"),
        RETRIEVE_ALL_FAILED("7"),
        SAVE_FAILED("8"),
        MISSING_SCRIPT_UUID("9"),
        MISSING_SCRIPT_NAME("10"),
        MISSING_SCRIPT("11"),
        MISSING_SCRIPTING_LANGUAGE("12"),
        MISSING_SCRIPT_CONTEXT("13"),
        SCRIPT_NAME_EXISTS("14"),
        SCRIPT_UUID_EXISTS("15"),
        SCRIPT_UUID_NOT_FOUND("16"),
        FILTER_BOOLEAN_LITERAL_FALSE("17"),
        FILTER_EXTENDED_MATCH("18"),
        FILTER_GREATER_THAN("19"),
        FILTER_GREATER_THAN_OR_EQUAL("20"),
        FILTER_LESS_THAN("21"),
        FILTER_LESS_THAN_OR_EQUAL("22"),
        FILTER_NOT("23"),
        FILTER_PRESENT("24"),
        SCRIPT_ENCODING_FAILED("25"),
        RESOURCE_FILTER_NOT_SUPPORTED("26");

        private final String code;

        private ScriptErrorCode(String code) {
            this.code = code;
        }

        /**
         * Get the code for this error message.
         * @return the error message code
         */
        public String getCode() {
            return code;
        }
    }

    /**
     * Retrieve the {@code SupportedScriptingLanguage} instance for the given language.
     * @param languageName The name of the required scripting language.
     * @return The {@code SupportedScriptingLanguage}.
     * @throws ScriptException If the given language is not supported.
     */
    public static SupportedScriptingLanguage getLanguageFromString(String languageName) throws ScriptException {
        for (SupportedScriptingLanguage ssl : SupportedScriptingLanguage.values()) {
            if (ssl.name().equalsIgnoreCase(languageName)) {
                return ssl;
            }
        }
        throw new ScriptException(ScriptErrorCode.LANGUAGE_NOT_SUPPORTED, languageName);
    }

    /**
     * Retrieve the {@code ScriptContext} instance for the given context.
     * @param context The name of the required scripting context.
     * @return The {@code ScriptContext}.
     * @throws ScriptException If the given context is not supported.
     */
    public static ScriptContext getContextFromString(String context) throws ScriptException {
        for (ScriptContext sc : ScriptContext.values()) {
            if (sc.name().equalsIgnoreCase(context)) {
                return sc;
            }
        }
        throw new ScriptException(ScriptErrorCode.CONTEXT_NOT_RECOGNISED, context);
    }
}

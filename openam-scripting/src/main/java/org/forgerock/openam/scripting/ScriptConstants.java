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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openam.scripting;

import static org.forgerock.openam.scripting.ScriptConstants.ScriptContext.*;

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
    public static final String EMPTY_SCRIPT_SELECTION = "[Empty]";

    public static final String SCRIPT_TIMEOUT = "serverTimeout";
    public static final String THREAD_POOL_CORE_SIZE = "coreThreads";
    public static final String THREAD_POOL_MAX_SIZE = "maxThreads";
    public static final String THREAD_POOL_QUEUE_SIZE = "queueSize";
    public static final String THREAD_POOL_IDLE_TIMEOUT = "idleTimeout";
    public static final String WHITE_LIST = "whiteList";
    public static final String BLACK_LIST = "blackList";
    public static final String USE_SECURITY_MANAGER = "useSecurityManager";
    public static final String ENGINE_CONFIGURATION = "EngineConfiguration";

    public static final int DEFAULT_CORE_THREADS = 10;
    public static final int DEFAULT_MAX_THREADS = 10;
    public static final int DEFAULT_QUEUE_SIZE = 10;
    public static final long DEFAULT_IDLE_TIMEOUT_SECONDS = 60l; // Seconds

    public static final String LOGGER_NAME = "Scripting";

    public static final String AUTHENTICATION_SERVER_SIDE_NAME = "AUTHENTICATION_SERVER_SIDE";
    public static final String POLICY_CONDITION_NAME = "POLICY_CONDITION";
    public static final String OIDC_CLAIMS_NAME = "OIDC_CLAIMS";

    /**
     * The context in which a script will be used.
     */
    public enum ScriptContext {
        AUTHENTICATION_SERVER_SIDE,
        AUTHENTICATION_CLIENT_SIDE,
        POLICY_CONDITION,
        OIDC_CLAIMS
    }

    /**
     * Predefined global script configuration IDs. The global script configurations are defined in the
     * scripting service and accessible in all realms.
     */
    public enum GlobalScript {
        AUTH_MODULE_SERVER_SIDE("Scripted Module - Server Side", "7e3d7067-d50f-4674-8c76-a3e13a810c33",
                AUTHENTICATION_SERVER_SIDE),
        AUTH_MODULE_CLIENT_SIDE("Scripted Module - Client Side", "c827d2b4-3608-4693-868e-bbcf86bd87c7",
                AUTHENTICATION_CLIENT_SIDE),
        DEVICE_ID_MATCH_SERVER_SIDE("Device Id (Match) - Server Side", "703dab1a-1921-4981-98dd-b8e5349d8548",
                AUTHENTICATION_SERVER_SIDE),
        DEVICE_ID_MATCH_CLIENT_SIDE("Device Id (Match) - Client Side", "157298c0-7d31-4059-a95b-eeb08473b7e5",
                AUTHENTICATION_CLIENT_SIDE),
        OIDC_CLAIMS_SCRIPT("OIDC Claims Script", "36863ffb-40ec-48b9-94b1-9a99f71cc3b5", OIDC_CLAIMS),
        POLICY_CONDITION_SCRIPT("Policy Condition", "9de3eb62-f131-4fac-a294-7bd170fd4acb", POLICY_CONDITION);

        private final String displayName;
        private final String id;
        private final ScriptContext context;

        GlobalScript(String displayName, String id, ScriptContext context) {
            this.displayName = displayName;
            this.id = id;
            this.context = context;
        }

        /**
         * Get the display name of the global script.
         * @return The display name of the script.
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Get the Id of the global script.
         * @return The Id of the global script.
         */
        public String getId() {
            return id;
        }

        /**
         * Get the {@link ScriptContext} of the global script.
         * @return The context of the global script.
         */
        public ScriptContext getContext() {
            return context;
        }
    }

    /**
     * Error messages are stored in the scripting.properties file to facilitate translation. Each entry in this
     * enum corresponds to a specific error message in the file keyed on the code.
     */
    public enum ScriptErrorCode {
        CONTEXT_NOT_RECOGNISED,
        LANGUAGE_NOT_SUPPORTED,
        FIND_BY_NAME_FAILED,
        FIND_BY_UUID_FAILED,
        DELETE_FAILED,
        RETRIEVE_FAILED,
        RETRIEVE_ALL_FAILED,
        SAVE_FAILED,
        MISSING_SCRIPT_UUID,
        MISSING_SCRIPT_NAME,
        MISSING_SCRIPT,
        MISSING_SCRIPTING_LANGUAGE,
        MISSING_SCRIPT_CONTEXT,
        SCRIPT_NAME_EXISTS,
        SCRIPT_UUID_EXISTS,
        SCRIPT_UUID_NOT_FOUND,
        FILTER_BOOLEAN_LITERAL_FALSE,
        FILTER_EXTENDED_MATCH,
        FILTER_GREATER_THAN,
        FILTER_GREATER_THAN_OR_EQUAL,
        FILTER_LESS_THAN,
        FILTER_LESS_THAN_OR_EQUAL,
        FILTER_NOT,
        FILTER_PRESENT,
        RESOURCE_FILTER_NOT_SUPPORTED,
        SCRIPT_DECODING_FAILED,
        DELETING_DEFAULT_SCRIPT,
        DELETING_SCRIPT_IN_USE_SINGULAR,
        DELETING_SCRIPT_IN_USE_PLURAL,
        INSUFFICIENT_PRIVILEGES
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

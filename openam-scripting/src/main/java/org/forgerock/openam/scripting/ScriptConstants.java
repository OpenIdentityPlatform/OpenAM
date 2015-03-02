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

    public static final String SCRIPT_NAME = "name";
    public static final String SCRIPT_UUID = "uuid";
    public static final String SCRIPT_TEXT = "script";
    public static final String SCRIPT_LANGUAGE = "language";
    public static final String SCRIPT_CONTEXT = "context";

    /**
     * The context in which a script will be used.
     */
    public static enum ScriptContext {
        AUTHENTICATION_SERVER_SIDE,
        AUTHENTICATION_CLIENT_SIDE,
        AUTHORIZATION_ENTITLEMENT_CONDITION
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
        throw new ScriptException("Scripting language not supported: " + languageName);
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
        throw new ScriptException("Scripting context not recognised: " + context);
    }
}

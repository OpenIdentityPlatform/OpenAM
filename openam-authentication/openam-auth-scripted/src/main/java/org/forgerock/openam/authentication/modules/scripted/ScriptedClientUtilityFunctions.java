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
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.openam.authentication.modules.scripted;

/**
 * Useful functions to be used with client side scripts, and methods for injection of such scripts, used with scripted
 * auth modules.
 */
public class ScriptedClientUtilityFunctions {

    /**
     * Creates an anonymous function which causes the script to run automatically when the client page containing the
     * function is rendered. This function takes as an argument the id of the form element which will hold the values
     * returned by the client script to the server.
     *
     * @param script The client side script to be ran.
     * @param outputParameterId The id of the form element.
     * @return The anonymous function, supplied with the element with the id.
     */
    public static String createClientSideScriptExecutorFunction(String script, String outputParameterId) {
        String clientSideScriptFunction = "(function(output){\n" +
                script +
                "\n})" +
                "(document.forms[0].elements['" + outputParameterId + "']);\n";
        return clientSideScriptFunction;
    }

    /**
     * Creates a piece of Javascript which will cause the login form to submit.
     *
     * @return The Javascript which causes the form submission.
     */
    public static String createAutoSubmissionLogic() {
        String autoSubmit = "" +
                "if(!(window.jQuery)) {\n" + // Crude detection to see if XUI is not present.
                "document.forms[0].submit();\n" +
                "} else {\n" +
                "$('input[type=submit]').trigger('click');\n" +
                "}";

        return autoSubmit;
    }

}
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

package org.forgerock.openam.scripting;

/**
 * Holds information about validation errors found in a script.
 *
 * @since 12.0.0
 */
public class ScriptError {

    private String scriptName;
    private String message;
    private int lineNumber;
    private int columnNumber;

    /**
     * Get the name of the script in which the error occurred.
     * @return the script name.
     */
    public String getScriptName() {
        return scriptName;
    }

    /**
     * Set the name of the script in which the error occurred.
     * @param scriptName the script name.
     */
    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    /**
     * Get the message that describes the error.
     * @return the error message.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set the message that describes the error.
     * @param message the error message.
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Get the line number on which the error occurred.
     * @return the line number on which the error occurred.
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Set the line number on which the error occurred.
     * @param lineNumber the line number on which the error occurred.
     */
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
     * Set the column number on which the error occurred.
     * @return the line number on which the error occurred.
     */
    public int getColumnNumber() {
        return columnNumber;
    }

    /**
     * Set the column number on which the error occurred.
     * @param columnNumber the line number on which the error occurred.
     */
    public void setColumnNumber(int columnNumber) {
        this.columnNumber = columnNumber;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("Error in '")
                .append(scriptName)
                .append("' on line ")
                .append(lineNumber)
                .append(", column ")
                .append(columnNumber)
                .append(": ")
                .append(message)
                .toString();
    }
}

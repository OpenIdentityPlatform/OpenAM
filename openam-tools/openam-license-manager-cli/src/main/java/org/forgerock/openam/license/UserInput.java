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
package org.forgerock.openam.license;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * Requests user input from an {@link InputStream}.
 */
public class UserInput {

    private final PrintStream outputStream;
    private final InputStream inputStream;

    public UserInput() {
        this.outputStream = System.out;
        this.inputStream = System.in;
    }

    public UserInput(PrintStream outputStream, InputStream inputStream) {
        this.outputStream = outputStream;
        this.inputStream = inputStream;
    }

    /**
     * Prompts user for input from System.in.
     *
     * @param prompt Prompt - text to display to the user
     * @return The user input.
     */
    public String getUserInput(String prompt) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        outputStream.print(prompt);
        return in.readLine();
    }

}

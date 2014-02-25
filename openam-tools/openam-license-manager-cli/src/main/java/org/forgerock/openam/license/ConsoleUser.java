/*
 * Copyright 2014 ForgeRock, AS.
 *
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
 */

package org.forgerock.openam.license;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ResourceBundle;

/**
 * Command-line interface (CLI) for the console user.
 *
 * @since 12.0.0
 */
public class ConsoleUser implements User {
    private static final String LICENSE_PROPERTIES = "LicensePresenter";

    private final PrintWriter out;
    private final BufferedReader in;
    private final ResourceBundle messages;

    /**
     * Constructs a CLI with the given input and output streams, and using the given resource bundle for
     * message localisation.
     *
     * @param out the output stream to write messages to.
     * @param in the input stream to read responses from.
     * @param messages the resource bundle to use for message localisation.
     */
    public ConsoleUser(Writer out, Reader in, ResourceBundle messages) {
        if (out == null) {
            throw new NullPointerException("output stream is null");
        }
        if (in == null) {
            throw new NullPointerException("input stream is null");
        }
        if (messages == null) {
            throw new NullPointerException("message bundle is null");
        }
        this.out = new PrintWriter(out);
        this.in = new BufferedReader(in);
        this.messages = messages;
    }

    /**
     * Constructs a CLI using the process standard input and output streams and the default license resource bundle for
     * message localisation.
     */
    public ConsoleUser() {
        this(new PrintWriter(System.out), new InputStreamReader(System.in), ResourceBundle.getBundle(LICENSE_PROPERTIES));
    }

    /**
     * {@inheritDoc}
     *
     * Displays messages on the console with a trailing new line.
     *
     * @param messageId the id of the localised message to display.
     */
    public void tell(String messageId) {
        show(messages.getString(messageId));
    }

    /**
     * {@inheritDoc}
     *
     * Displays messages directly on the console with a trailing new line.
     *
     * @param message the message to display verbatim.
     */
    public void show(String message) {
        out.println(message);
    }

    /**
     * {@inheritDoc}
     *
     * Displays the question on the console (without a trailing newline) and then reads a single line response. Will
     * repeat the question until the user enters non-empty text, or closes the console.
     *
     * @param question the id of the question to ask.
     * @return the response from the user, or null if they close the console.
     */
    public String ask(String question) {
        String response;
        try {
            do {
                out.print(messages.getString(question) + " ");
                out.flush();
                response = in.readLine();
            } while (response != null && response.trim().isEmpty());
        } catch (IOException ex) {
            response = null;
        }
        return response;
    }

    /**
     * {@inheritDoc} Returns the user name from the system {@code user.name} property.
     *
     * @return the user name.
     */
    public String getName() {
        return System.getProperty("user.name");
    }

    /**
     * {@inheritDoc}
     */
    public String getMessage(String messageId) {
        return messages.getString(messageId);
    }
}

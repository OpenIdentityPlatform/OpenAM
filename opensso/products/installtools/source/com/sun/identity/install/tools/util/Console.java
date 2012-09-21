/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: Console.java,v 1.3 2008/06/25 05:51:28 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.StringTokenizer;

public class Console {

    public static final String pause() {
        println();
        println(LocalizedMessage.get(MSG_PAUSE_PROMPT));
        String result = readLine();
        return result;
    }
    
    public static final void printlnRawText(String text) {
        System.out.println(text);
    }

    public static final void printRawText(String text) {
        System.out.print(text);
    }

    public static final void println() {
        writeIt(NEW_LINE);
    }

    public static final void print(String localizedMessage) {
        writeIt(localizedMessage);
    }

    public static final void println(LocalizedMessage message, Object[] args) {
        MessageFormat fmt = new MessageFormat(message.toString());
        writeIt(fmt.format(args) + NEW_LINE);
    }

    public static final void print(LocalizedMessage message, Object[] args) {
        MessageFormat fmt = new MessageFormat(message.toString());
        writeIt(fmt.format(args));
    }

    public static final void println(LocalizedMessage message) {
        writeIt(message + NEW_LINE);
    }

    public static final void println(String localizedMessage) {
        writeIt(localizedMessage + NEW_LINE);
    }

    public static final void print(LocalizedMessage prompt, 
            String defaultValue) {
        writeIt(prompt + " [" + defaultValue + "]: ");
    }

    public static final void print(LocalizedMessage message) {
        writeIt(message.toString());
    }

    public static final String readLine() {
        String result = null;
        try {
            result = getInputReader().readLine();
        } catch (Exception ex) {
            Debug.log("Failed to read input", ex);
            throw new RuntimeException("Failed to read input: "
                    + ex.getMessage());
        }
        return result;
    }

    private static void writeIt(String msg) {
        if (msg.trim().length() == 0) {
            System.out.print(msg);
            System.out.flush();
        } else {
            StringTokenizer stok = new StringTokenizer(msg, " ");
            int index = 0;
            String nextToken = null;
            while (stok.hasMoreElements()) {
                nextToken = stok.nextToken();
                index += nextToken.length();
                if (index > 65) {
                    System.out.print(NEW_LINE);
                    System.out.print(nextToken);
                    index = nextToken.length();
                } else {
                    if (index != nextToken.length()) {
                        System.out.print(SPACE);
                    }
                    System.out.print(nextToken);
                }
            }
            // Just add a space at end. Would help in cases where we 
            // display ":"
            if (nextToken.endsWith(":")) { // Last token is not NEW_LINE
                System.out.print(SPACE);
            }
            System.out.flush();
        }
    }

    private static BufferedReader getInputReader() {
        return inReader;
    }

    private static synchronized void initialize() {
        try {
            inReader = new BufferedReader(new InputStreamReader(System.in));
        } catch (Exception ex) {
            Debug.log("Failed to initialize console", ex);
            throw new RuntimeException("Failed to initialize console: "
                    + ex.getMessage());
        }
    }

    private static BufferedReader inReader = null;

    private static final String NEW_LINE = System.getProperty("line.separator",
            "\n");

    private static final String SPACE = " ";

    private static final String MSG_PAUSE_PROMPT = "console_pause_prompt";

    static {
        initialize();
    }

}

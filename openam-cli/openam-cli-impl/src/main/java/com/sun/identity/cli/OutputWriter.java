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
 * $Id: OutputWriter.java,v 1.2 2008/06/25 05:42:09 qcheng Exp $
 *
 */

package com.sun.identity.cli;


/**
 * Output writer gets the messages from CLI engine and writes them to 
 * <code>System.out</code> or <code>System.err</code>.
 */
public class OutputWriter implements IOutput {
    /**
     * Prints message.
     *
     * @param str Message string.
     */
    public void printMessage(String str) {
        System.out.print(str);
    }

    /**
     * Prints message with new line.
     *
     * @param str Message string.
     */
    public void printlnMessage(String str) {
        System.out.println(str);
    }

    /**
     * Prints error.
     *
     * @param str Error message string.
     */
    public void printError(String str) {
        System.err.print(str);
    }

    /**
     * Prints error with new line.
     *
     * @param str Error message string.
     */
    public void printlnError(String str) {
        System.err.println(str);
    }
}

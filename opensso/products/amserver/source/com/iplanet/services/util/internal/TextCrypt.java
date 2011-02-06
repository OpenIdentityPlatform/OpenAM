/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: TextCrypt.java,v 1.2 2008/06/25 05:41:42 qcheng Exp $
 *
 */

package com.iplanet.services.util.internal;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import com.iplanet.services.util.Crypt;

/**
 * This class is used to encrypt the string value. It is used during
 * installation.
 */
public class TextCrypt {

    /**
     * This method encrypts the passed string and then prints on console.
     * 
     * @param args
     *            arguments for this method. The string to be encrypted
     * 
     */
    public static void main(String args[]) {
        if (args.length != 0) {
            if (args.length == 1) {
                System.out.println(Crypt.encrypt(args[0]));
            } 
            else if ((args.length == 2) && ((args[0]).equalsIgnoreCase("-f")))
            {
                String passwd = null;
                passwd = (readFromPasswordFile(args[1])).trim();
                if (passwd != null) {
                    System.out.println(Crypt.encrypt(passwd));
                }
            }
        }
    }

    /**
     * This method reads the password from the file. If the read fails it prints
     * stacktrace of the exception.
     * 
     * @param passwordfile
     *            The file from which the password is read.
     * 
     * @return Returns the password read from the file.
     */
    public static String readFromPasswordFile(String passwordfile) {
        String line = null;
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(passwordfile));
            if (in.ready()) {
                line = in.readLine();
            }
            return line;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}

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
 * $Id: Hash.java,v 1.2 2008/06/25 05:53:02 qcheng Exp $
 *
 */

package com.sun.identity.shared.encode;

import com.sun.identity.shared.debug.Debug;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;

/**
 * The class <code>Hash</code> provides generic methods to hash data.
 */
public class Hash {

    /**
     * Generates a SHA1 digest of the string and returns BASE64 encoded digest.
     * 
     * @param string
     *            a string to be hashed return a BASE64 encoded hashed String or
     *            null if an error occurred
     */
    public static String hash(String string) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            sha1.update(string.getBytes("UTF-8"));
            return Base64.encode(sha1.digest());
        } catch (Exception ex) {
            Debug debug = Debug.getInstance("amSDK");
            if (debug.warningEnabled()) {
                debug.warning("Hash.hash:", ex);
            }
            return null;
        }
    }

    /**
     * Hash of the provided string or string read from a file specified with a
     * "-f" option
     */
    public static void main(String[] args) {
        if (args.length == 1) {
            // Hash the first argument and return
            System.out.println(hash(args[0]));
        } else if (args.length == 2 && args[0].equals("-f")) {
            String passwd = readFromPasswordFile(args[1]);
            if (passwd != null) {
                System.out.println(hash(passwd.trim()));
            }
        }
    }

    private static String readFromPasswordFile(String passwordfile) {
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

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
 * $Id: Hash.java,v 1.4 2008/06/25 05:41:41 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.iplanet.services.util;

import java.security.MessageDigest;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.services.util.internal.TextCrypt;
import com.sun.identity.shared.encode.Base64;

/**
 * The class <code>Hash</code> provides generic methods to hash data.
 *
 * @deprecated As of OpenSSO version 8.0
 *             {@link com.sun.identity.shared.encode.Hash}
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
            String passwd = TextCrypt.readFromPasswordFile(args[1]);
            if (passwd != null) {
                System.out.println(hash(passwd.trim()));
            }
        }
    }
}

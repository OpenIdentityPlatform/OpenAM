/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: FedletEncodeDecode.java,v 1.1 2009/11/12 17:30:31 exu Exp $
 *
 */

package com.sun.identity.fedlet;

import com.sun.identity.security.DecodeAction;
import com.sun.identity.security.EncodeAction;
import com.sun.identity.saml.xmlsig.PasswordDecoder;
import java.security.AccessController;


/**
 * Utility that a fedlet can use to encode/decode password.
 */
public class FedletEncodeDecode implements PasswordDecoder {

    /**
     * Encodes the password.
     * @param args password to be encoded.
     */
    public static void main(String[] args) {
        if ((args == null) || (args.length != 1)) {
            System.err.println("Invalid parameter.");
            System.err.println("Expected parameter : <text_tobe_encoded>");
            System.exit(-1);
        }

        try {
            System.out.println((String) AccessController.doPrivileged(
                new EncodeAction(args[0])));
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Returns decoded password.
     *
     * @param encodedPwd encoded password
     * @return decoded password, return null if couldn't
     *     decode the password.
     */
    public String  getDecodedPassword (String encodedPwd) {
        String password = (String) AccessController.doPrivileged(
            new DecodeAction(encodedPwd));
        return password;
    }
}

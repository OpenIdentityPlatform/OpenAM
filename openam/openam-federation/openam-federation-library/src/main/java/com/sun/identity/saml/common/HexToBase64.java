/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: HexToBase64.java,v 1.2 2008/06/25 05:47:33 qcheng Exp $
 *
 */

package com.sun.identity.saml.common;

import com.sun.identity.shared.encode.Base64;

/**
 * This class <code>HexToBase64</code> is used to convert Hex encoded
 * SAML source ID to Base64 encoded ID. 
 *
 * @supported.all.api
 */
public class HexToBase64 {

    /**
     * Returns Base64 encoded source ID based on the input Hex source ID.
     * <br>Usage: java com.sun.identity.saml.common.HexToBase64 &lt;Hex_encoded_id&gt;<br> 
     * This method will print out Base64 encoded source ID to the standard 
     * output.
     *
     * @param args Hex encoded source ID. 
     */
    public static void main(String args[]) throws Exception {

        if (args.length != 1) {
            System.out.println("Usage: java HexToBase64 <Hex_encoded_id>");
            return;
        }
        Base64 encoder = new Base64();
        String hexString = args[0];
	int read = hexString.length();
	byte[] byteArray = new byte[read/2];
	for (int i=0, j=0; i < read; i++, j++) {
            String part = hexString.substring(i,i+2);
            byteArray[j] =
                new Short(Integer.toString(Integer.parseInt(part,16))).
            byteValue();
            i++;
	}
	try {
            String encodedID = encoder.encode(byteArray).trim();
            System.out.println(encodedID);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}

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
 * $Id: Base64ToHex.java,v 1.2 2008/06/25 05:47:33 qcheng Exp $
 *
 */

package com.sun.identity.saml.common;

import com.sun.identity.shared.encode.Base64;

/**
 * This class <code>Base64ToHex</code> is used to convert Base64 encoded
 * SAML source ID to Hex encoded ID.
 * @supported.all.api
 */

public class Base64ToHex {

    /**
     * Returns Hex encoded source ID based on the input Base64 source ID.
     * <br>Usage: java com.sun.identity.saml.common.Base64ToHex &lt;Base64_encoded_id&gt;<br>
     * This method will print out Hex encoded source ID to the standard
     * output.
     *
     * @param args Hex encoded source ID.
     */

    public static void main(String args[]) throws Exception {

        if (args.length != 1 ) {
            System.out.println("Usage: java Base64ToHex <Base64_encoded_id>");
            return;
        } 

        String inputString = args[0];
        Base64 decoder = new Base64();
        byte[] byteArray = decoder.decode(inputString);
	int readBytes = byteArray.length;
	StringBuffer hexData = new StringBuffer();
	int onebyte;
	for (int i=0; i < readBytes; i++) {
 	  onebyte = ((0x000000ff & byteArray[i]) | 0xffffff00);
          hexData.append(Integer.toHexString(onebyte).substring(6));
	}
        System.out.println(hexData.toString());
    }
}

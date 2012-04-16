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
 * $Id: SAMLSiteID.java,v 1.2 2008/06/25 05:47:35 qcheng Exp $
 *
 */


package com.sun.identity.saml.common;

import java.util.Random; 
import com.sun.identity.shared.encode.Base64;

import java.security.MessageDigest;

/**
 * This class is used to generate SAML Site ID.
 * 
 * @supported.all.api
 */
public class SAMLSiteID {

    private static Random random = new Random();

    private SAMLSiteID() {
    }
   
    /**
     * Returns an ID String with length of
     * <code>SAMLConstants.ID_LENGTH</code>.
     *
     * @return ID String or null if it fails.
     */
    public static String generateID() {
        if (random == null) {
            return null;
        }
        byte bytes[] = new byte[SAMLConstants.ID_LENGTH];
        random.nextBytes(bytes);
        String encodedID = null; 
        try {
            encodedID = Base64.encode(bytes).trim();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return encodedID;
    }

    /**
     * Returns SAML site ID based on <code>siteURL</code>, this will return a
     * <code>Base64</code> encoded <code>SHA-1</code> digest. 
     *
     * @param siteURL site URL for example:
     *         <code>http://host.sun.com:58080</code>.
     * @return Base64 encoded site ID 
     */
    public static String generateSourceID(String siteURL) {
        if ((siteURL == null) || (siteURL.length() == 0)) {
            return null;
        }
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        char chars[] = siteURL.toCharArray();
        byte bytes[] = new byte[chars.length];
        for (int i = 0; i < chars.length; i++) {
            bytes[i] = (byte) chars[i];
        }
        md.update(bytes);
        byte byteResult[] = md.digest();
        String result = null;
        try {
            result = Base64.encode(byteResult);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return result;
    }

    /**
     * Obtains site ID based on the host name.
     * This method will print out site ID to the standard output.
     *
     * @param args  host name
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("usage : java SAMLSiteID <host_name>");
            return;
        }

        System.out.println(generateSourceID(args[0]));
    }
}

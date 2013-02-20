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
 * $Id: JCECrypt.java,v 1.1 2009/05/05 21:24:47 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.setup;

import com.iplanet.services.util.AMEncryption;
import com.iplanet.services.util.ConfigurableKey;
import com.iplanet.services.util.JCEEncryption;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;

/**
 * The class <code>Crypt</code> provides generic methods to encryt and decrypt
 * data. This class provides a pluggable architecture to encrypt and decrypt
 * data, using the <code>AMEncryption</code> interface class. A class that
 * implements <code>AMEncryption</code> must be specified via the system
 * property: <code>com.iplanet.services.security.encryptor</code>. If none is
 * provided, the default provided by iDSAME
 * <code>com.iplanet.services.util.JCEEncryption</code> will be used.
 * <p>
 * Additionally, it provides a method to check if the calling class has
 * permission to call these methods. To enable the additional security, the
 * property com.sun.identity.security.checkcaller must be set to true.
 */
public class JCECrypt {
    private static final String DEFAULT_ENCRYPTOR_CLASS = 
        "com.iplanet.services.util.JCEEncryption";
    private static final String DEFAULT_PWD = 
        "KmhUnWR1MYWDYW4xuqdF5nbm+CXIyOVt";

    private static AMEncryption encryptor;

    static {
        encryptor = createInstance(DEFAULT_PWD);
    }

    private static AMEncryption createInstance(String password) {
        AMEncryption instance;
        // Construct the encryptor class
        String encClass = DEFAULT_ENCRYPTOR_CLASS;
        
        try {
            instance = (AMEncryption) Class.forName(encClass).newInstance();
        } catch (Exception e) {
            Debug debug = Debug.getInstance("amSDK");
            debug.error(
                "JCECrypt.createInstance Unable to get class instance: " +
                encClass, e);
            instance = new JCEEncryption();
        }
        try {
            ((ConfigurableKey) instance).setPassword(password);
        } catch (Exception e) {
            Debug debug = Debug.getInstance("amSDK");
            if (debug != null) {
                debug.error(
                    "JCECrypt.createInstance: failed to set password-based key",
                    e);
            }
        }

        return instance;
    }

    private static String encode(String clearText, AMEncryption encr) {
        if (clearText == null || clearText.length() == 0) {
            return null;
        }

        // Encrypt the data
        byte[] encData = null;
        try {
            encData = encr.encrypt(clearText.getBytes("utf-8"));
        } catch (UnsupportedEncodingException uee) {
            Debug debug = Debug.getInstance("amSDK");
            debug.error("Crypt:: utf-8 encoding is not supported");
            encData = encryptor.encrypt(clearText.getBytes());
        }

        // BASE64 encode the data
        String str = null;
        // Perf Improvement : Removed the sync block and newed up the Encoder
        // object for every call. Its a trade off b/w CPU and mem usage.
        str = Base64.encode(encData).trim();

        // Serialize the data, i.e., remove \n and \r
        BufferedReader bufReader = new BufferedReader(new StringReader(str));
        StringBuilder strClean = new StringBuilder(str.length());
        String strTemp = null;
        try {
            while ((strTemp = bufReader.readLine()) != null) {
                strClean.append(strTemp);
            }
        } catch (IOException ioe) {
            Debug debug = Debug.getInstance("amSDK");
            debug.error("Crypt:: Error while base64 encoding", ioe);
        }
        return (strClean.toString());
    }

    public static String encode(String clearText) {
        return encode(clearText, encryptor);
    }

    private static String decode(String encoded, AMEncryption encr) {
        if (encoded == null || encoded.length() == 0) {
            return (null);
        }

        // BASE64 decode the data
        byte[] encData = null;
        // Perf Improvement : Removed the sync block and newed up the Decoder
        // object for every call. Its a trade off b/w CPU and mem usage.
        encData = Base64.decode(encoded.trim());

        // Decrypt the data
        byte[] rawData = encr.decrypt(encData);
        if (rawData == null) {
            return (null);
        }

        // Convert to String and return
        String answer = null;
        try {
            answer = new String(rawData, "utf-8");
        } catch (UnsupportedEncodingException uue) {
            Debug debug = Debug.getInstance("amSDK");
            debug.error("Crypt:: Unsupported encoding UTF-8", uue);
            answer = new String(rawData);
        }
        return (answer);
    }

    /**
     * Decode an encoded string
     * 
     * @param encoded
     *            The encoded string.
     * @return The decoded string.
     */
    public static String decode(String encoded) {
        return decode(encoded, encryptor);
    }
}

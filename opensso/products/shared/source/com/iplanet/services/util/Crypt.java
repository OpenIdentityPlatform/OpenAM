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
 * $Id: Crypt.java,v 1.4 2008/08/19 19:14:54 veiming Exp $
 *
 */

package com.iplanet.services.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.security.ISSecurityPermission;

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
public class Crypt {
    // Private static final varibales
    private static final String ENCRYPTOR_CLASS_PROPERTY = 
        "com.iplanet.security.encryptor";

    private static final String CHECK_CALLER_PROPERTY = 
        "com.sun.identity.security.checkcaller";

    private static final String DEFAULT_ENCRYPTOR_CLASS = 
        "com.iplanet.services.util.JCEEncryption";

    // The pwd can be changed through the config file.
    // But be super consious when you change it. You have to change the
    // encrypted versions of the admin passwords simulaneously.
    private static final String PROPERTY_PWD = "am.encryption.pwd";

    private static final String PROPERTY_PWD_LOCAL = 
        "com.sun.identity.client.encryptionKey";

    private static final String DEFAULT_PWD = 
        "KmhUnWR1MYWDYW4xuqdF5nbm+CXIyOVt";

    private static boolean checkCaller;

    public static SecurityManager securityManager;

    private static AMEncryption encryptor;

    private static AMEncryption localEncryptor;

    private static AMEncryption hardcodedKeyEncryptor;

    static {
        initialize();
    }

    public static synchronized void reinitialize() {
        initialize();
    }

    private static void initialize() {
        encryptor = createInstance(SystemPropertiesManager.get(PROPERTY_PWD,
                DEFAULT_PWD));
        localEncryptor = createInstance(SystemPropertiesManager.get(
                PROPERTY_PWD_LOCAL, SystemPropertiesManager.get(PROPERTY_PWD,
                        DEFAULT_PWD)));
        hardcodedKeyEncryptor = createInstance(DEFAULT_PWD);

        // check if caller needs to be validated
        String cCaller = SystemPropertiesManager.get(CHECK_CALLER_PROPERTY);
        if ((cCaller != null) && (cCaller.equalsIgnoreCase("true"))) {
            checkCaller = true;
            securityManager = System.getSecurityManager();
        }
    }

    private static AMEncryption createInstance(String password) {
        AMEncryption instance;
        // Construct the encryptor class
        String encClass = SystemPropertiesManager.get(ENCRYPTOR_CLASS_PROPERTY,
                DEFAULT_ENCRYPTOR_CLASS);
        
        try {
            instance = (AMEncryption) Class.forName(encClass).newInstance();
        } catch (Exception e) {
            Debug debug = Debug.getInstance("amSDK");
            debug.error("Crypt:: Unable to get class instance: " + encClass, e);
            instance = new JCEEncryption();
        }
        try {
            ((ConfigurableKey) instance).setPassword(password);
        } catch (Exception e) {
            Debug debug = Debug.getInstance("amSDK");
            if (debug != null) {
                debug.error("Crypt: failed to set password-based key", e);
            }
        }

        return instance;
    }

    /**
     * Check to see if security is enabled and Caller needs to be checked for
     * OpenSSO specific Java security permissions
     * 
     * @return boolean true if security check enabled, false otherwise
     */

    public static boolean checkCaller() {
        return checkCaller;
    }

    /**
     * This is a temporary kludge which always returns an instance of
     * AMEncryption using hardcoded key It is necessary for backward
     * compatibility with 2.0 Java agents This method is to be ONLY used by
     * Session module for session id generation.
     * 
     */
    public static AMEncryption getHardcodedKeyEncryptor() {
        return hardcodedKeyEncryptor;
    }

    /**
     * Checks security permission returns true if action is allowed, false
     * otherwise
     */

    private static boolean isAccessPermitted() {
        try {
            ISSecurityPermission isp = new ISSecurityPermission("access",
                    "adminpassword");
            if (securityManager != null) {
                securityManager.checkPermission(isp);
            }
            return true;
        } catch (SecurityException e) {
            Debug debug = Debug.getInstance("amSDK");
            debug.error(
                    "Security Alert: Unauthorized access to Encoding/Decoding"
                            + " password utility: Returning NULL", e);
        }
        return false;
    }

    /**
     * Return AMEncryption instance for deployment-specific secret key
     * 
     */
    public static AMEncryption getEncryptor() {
        return isAccessPermitted() ? encryptor : null;
    }

    /**
     * <p>
     * Encrypt a String.
     * </p>
     * 
     * @param clearText
     *            The string to be encoded.
     * @return The encoded string.
     */
    public static String encrypt(String clearText) {
        return encode(clearText);
    }

    /**
     * <p>
     * Encrypt a String using the client's encryption key
     * </p>
     * 
     * @param clearText
     *            The string to be encoded.
     * @return The encoded string.
     */
    public static String encryptLocal(String clearText) {
        return encode(clearText, localEncryptor);
    }

    /**
     * <p>
     * Decrypt a String.
     * </p>
     * 
     * @param encoded
     *            The string to be decoded.
     * @return The decoded string.
     */
    public static String decrypt(String encoded) {
        return decode(encoded);
    }

    /**
     * <p>
     * Decrypt a String using client's encryption key
     * </p>
     * 
     * @param encoded
     *            The string to be decoded.
     * @return The decoded string.
     */
    public static String decryptLocal(String encoded) {
        return decode(encoded, localEncryptor);
    }

    /**
     * <p>
     * Encode a String.
     * </p>
     * 
     * @param clearText
     *            The string to be encoded.
     * @param encr
     *            instance of AMEncryption to use
     * @return The encoded string.
     */
    public static String encode(String clearText, AMEncryption encr) {
        if (checkCaller()) {

            if (!isAccessPermitted())
                return null;
        }
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
        StringBuffer strClean = new StringBuffer(str.length());
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

    /**
     * <p>
     * Encode a String.
     * </p>
     * 
     * @param clearText
     *            The string to be encoded.
     * @return The encoded string.
     */
    public static String encode(String clearText) {
        return encode(clearText, encryptor);
    }

    /**
     * Decode an encoded string
     * 
     * @param encoded
     *            The encoded string.
     * @param encr
     *            instance of AMEncryption to use
     * @return The decoded string.
     */
    public static String decode(String encoded, AMEncryption encr) {

        if (checkCaller()) {
            try {
                ISSecurityPermission isp = new ISSecurityPermission("access",
                        "adminpassword");
                if (securityManager != null) {
                    securityManager.checkPermission(isp);
                }
            } catch (SecurityException e) {
                Debug debug = Debug.getInstance("amSDK");
                debug.error("Security Alert: Unauthorized access to " +
                       "Encoding/Decoding password utility: Returning NULL", e);
                return null;
            }

        }
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

    /**
     * Check to determine if the calling class has the privilege to execute
     * sensitive methods which returns passwords, decrypts data, etc. This
     * method uses the stack trace to determine the calling class.
     */
    protected static boolean isCallerValid() {
        if (!checkCaller) {
            return (true);
        }
        return (isCallerValid(CLASSNAME));
    }

    /**
     * Check to determine if the calling class has the privilege to execute
     * sensitive methods which returns passwords, decrypts data, etc. This
     * method uses the stack trace to determine the calling class.
     * 
     * @param obj
     *            The Java object that is performing this check
     */
    public static boolean isCallerValid(Object obj) {
        if (!checkCaller) {
            return (true);
        }
        if (obj == null) {
            return (isCallerValid(CLASSNAME));
        }
        return (isCallerValid(obj.getClass().getName()));
    }

    /**
     * Check to determine if the calling class has the privilege to execute
     * sensitive methods which returns passwords, decrypts data, etc. This
     * method uses the stack trace to determine the calling class.
     * 
     * @param className 
     *            fully qualified class name of Object calling this function
     */
    public static boolean isCallerValid(String className) {
        if (!checkCaller) {
            return (true);
        }
        String parentClass = getParentClass(className);

        // Check for Package name matches
        for (int i = 0; i < VALID_PACKAGES.length; i++) {
            if (parentClass.startsWith(VALID_PACKAGES[i])) {
                return (true);
            }
        }
        // Check for Class name matches
        for (int i = 0; i < VALID_CLASSES.length; i++) {
            if (parentClass.equals(VALID_CLASSES[i])) {
                return (true);
            }
        }
        return (false);
    }

    protected static String getParentClass(String callerClass) {
        String parentClass = null;
        try {
            throw (new Exception());
        } catch (Exception pe) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(os);
            pe.printStackTrace(ps);
            String stackTrace = os.toString();
            String index = stackTrace.substring(stackTrace
                    .lastIndexOf(callerClass)
                    + callerClass.length());
            stackTrace = index.substring(index.lastIndexOf(AT_NAME)
                    + AT_NAME.length());
            parentClass = stackTrace.substring(0, stackTrace.indexOf("("));
            parentClass = stackTrace.substring(0, parentClass.lastIndexOf("."));
        }
        return (parentClass);
    }

    private static final String[] VALID_PACKAGES = { "com.iplanet.services",
            "com.iplanet.am", "com.sun.identity.policy" };

    private static final String[] VALID_CLASSES = {
            "com.iplanet.services.util.Crypt", "TestCrypt" };

    private static final String CLASSNAME = "com.iplanet.services.util.Crypt";

    private static final String AT_NAME = "at ";
}

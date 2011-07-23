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
 * $Id: JSSEncryption.java,v 1.3 2009/01/23 22:16:26 beomsuk Exp $
 *
 */

package com.iplanet.services.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.security.Provider;
import java.security.Security;
import java.util.Enumeration;

import org.mozilla.jss.CryptoManager;
import org.mozilla.jss.crypto.CryptoToken;
import org.mozilla.jss.crypto.SymmetricKey;
import org.mozilla.jss.crypto.IVParameterSpec;
import org.mozilla.jss.crypto.PBEAlgorithm;
import org.mozilla.jss.crypto.KeyGenerator;
import org.mozilla.jss.crypto.PBEKeyGenParams;
import org.mozilla.jss.crypto.EncryptionAlgorithm;
import org.mozilla.jss.crypto.Cipher;
import org.mozilla.jss.util.Password;

import com.iplanet.am.util.JSSInit;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;

/**
 * This class uses JSS symmetric algorithm for string encryption/decryption. 
 * The encrypted string contains BASE64 Characters as specified
 * in RFC1521. 
 * The format of the encoded byte before BASE64 encoding is
 *    byte[0] = crypt version number. This version is 1.
 *    byte[1]=keyGenAlg
 *    bype[2]=EncrytionAlg
 *    byte[3-10]=IV for encryption/decryption
 *    The rest is the encoded bytes.
 *
 * subnote:
 * This is initially intended to replace SessionID
 * encryption/decryption (xor). And is pulled to the DAI space at the
 * last minutes. Since the requirement and restrictions are different,
 * It needs adjustment later.
 *
 * for furtue "enhancement" (adopted form the old Password.java):
 * 1. Use an array of pins to be randomly picked. Add the index as a
 *    prefix of the encrypted string.
 *
 * Aravindan's thought:
 * 1. From the password, generate the key multiple times, put the
 *    number of times as a prefix of the encrypted string.
 * 2. Random generated a pwd and build a key from it. Put the pwd
 *    encrypted somehow as a prefix of the encrypted string.
 * 3. put pwd in a class and embeded in a jar file, so that it's not
 *    in "plain text", somewhat. 
 *    The class can be replaced at installation time by taking a
 *    pwd from user and dynamically created and replaced in the jar.
 *    (mzhao: And it should be able to be replaced by customer
 *    periodically. However if it's used for password encryption. The
 *    encrypted version of pwds should be changed simultaneously.)
 *
 * mzhao thought:
 * 1. There is known problem in this framework that we need to store
 *    the password securely in some way. Hardcoding it is not considered
 *    secure, putting it in a file is not either.
 * 2. Client Auth can be used for web based SSO.
 *
 * Borrowed from CMS:
 * 1. A storage cert can be created to encrypt/decrypt the pwd.
 * 2. A password cache can be used to store all passwords, such as 
 *    puser, daiuser, amadmin password, and ssl password. A SSO
 *    password is used to encryt them. When server restarts, this SSO
 *    password must be asked.
 * 3. A watchdog may be needed to auto restart the server.
 *
 * @author mzhao
 * @version $Revision: 1.3 $, $Date: 2009/01/23 22:16:26 $
 **/
public class JSSEncryption implements AMEncryption, ConfigurableKey {

    private static final byte VERSION = 1;

    private static String DEFAULT_KEYGEN_ALG = "PBE_MD5_DES_CBC"; 
    private static String DEFAULT_ENCYPTION_ALG = "DES_CBC_PAD";
    private static Debug debug = Debug.getInstance("amJSS");
    static String method = "JSSEncryption.initialize";

    static {
        JSSInit.initialize();
        try {
            CryptoManager cm = CryptoManager.getInstance();

            /* if FIPS is enabled, configure only FIPS ciphersuites */
            if (cm.FIPSEnabled()) {
                DEFAULT_KEYGEN_ALG = "PBE_SHA1_DES3_CBC"; 
                DEFAULT_ENCYPTION_ALG = "DES3_CBC_PAD";
            }
        } catch (Exception e) {
            if (debug != null) {
                debug.error("Crypt: Initialize JSS ", e);
            }
        }                
    }

    private static final String[] KEYGEN_ALGS = {
        "PBE_SHA1_DES3_CBC",
        "PBE_MD2_DES_CBC",
        "PBE_MD5_DES_CBC", 
        "PBE_SHA1_DES_CBC", 
        "PBE_SHA1_RC2_128_CBC",
        "PBE_SHA1_RC2_40_CBC",
        "PBE_SHA1_RC4_128",
        "PBE_SHA1_RC4_40"};
    private static int NUM_KEYGEN_ALG = KEYGEN_ALGS.length; 

    private static final String[] ENCRYPTION_ALGS = {
        "DES3_CBC_PAD",
        "DES_CBC",
        "DES_CBC_PAD",
        "DES_ECB", 
        "DES3_CBC",
        "DES3_ECB",
        "RC2_CBC",
        "RC4"};
    private static int NUM_ENCRYPTION_ALG = ENCRYPTION_ALGS.length; 

    private SymmetricKey sKeys[] = null;
    private IVParameterSpec ivParamSpecs[] = null;
    private static CryptoToken mToken = null;

    static {
        try {
            mToken = findToken();
        } catch (CryptoManager.NotInitializedException ex) {
            try {
                JSSInit.initialize();
                mToken = findToken();
            } catch (Exception e) {
                if (debug != null) {
                    debug.error("Crypt: Initialize JSS ", e);
                }
            }                
        }
    }

    /**
     * Default constructor
     */
    JSSEncryption() {
    }
 
    private static CryptoToken findToken() 
        throws CryptoManager.NotInitializedException {
        // This crypto token has to support encryption algorithm 
        // and all the key generation algorithms in KEYGEN_ALGS.
        // CryptoManager returns "Internal Key Storage Token" at least.
        CryptoToken token = null;
        CryptoManager cm = CryptoManager.getInstance();
        Enumeration e = cm.getTokensSupportingAlgorithm(
                        getEncryptionAlg(DEFAULT_ENCYPTION_ALG));
                        
        while (e.hasMoreElements()) {
           CryptoToken tok = (CryptoToken) e.nextElement();

           boolean foundToken = true;
           for (int i = 0; i<NUM_KEYGEN_ALG; i++) {
              if (!tok.doesAlgorithm(getKeyGenAlg(KEYGEN_ALGS[i]))) {
                 foundToken = false;
                 break;
              }
           }
           if (foundToken) {
              return tok;
           }
        }
           
        return null;
    }
        
    /**
     * Sets password-based key to use
     */
    public void setPassword(String password) throws Exception {
        initSymmetricKeysAndInitializationVectors(password);
    }

    private void initSymmetricKeysAndInitializationVectors(String password) {
        sKeys = new SymmetricKey[NUM_KEYGEN_ALG];
        ivParamSpecs = new IVParameterSpec[NUM_KEYGEN_ALG];
        byte salt[] = {0x01, 0x01, 0x01, 0x01, 0x01,0x01, 0x01, 0x01};

        Password pass = new Password(password.toCharArray());
        for (int i=0; i<NUM_KEYGEN_ALG; i++) {
            try {
                PBEAlgorithm keyAlg = getKeyGenAlg(KEYGEN_ALGS[i]);
                KeyGenerator kg = mToken.getKeyGenerator(keyAlg);
                PBEKeyGenParams kgp = new PBEKeyGenParams(pass, salt, 5);
                kg.initialize(kgp);
                sKeys[i] = kg.generate();
                ivParamSpecs[i] = new IVParameterSpec(kg.generatePBE_IV());
                if (debug.messageEnabled()) {
                    debug.message("Created symKey successfully : " + 
                            KEYGEN_ALGS[i]);
                }
            } catch (Exception e) {
                debug.error("Failed creating symKey : " + KEYGEN_ALGS[i], e);
            }
        }
        pass.clear();
    }

    private SymmetricKey getSymmetricKey(int type) {
        if (type >= 0 && type < NUM_KEYGEN_ALG)
            return sKeys[type];
        else
            return null;
    }

    private IVParameterSpec getIVParameterSpec(int type) {
         if (type >= 0 && type < NUM_KEYGEN_ALG)
             return ivParamSpecs[type];
         else
             return null;
    }

    /**
     * <p>Encrypt a String.</p>
     * @param clearText The string to be encoded.
     * @return The encoded string.
     */
    public byte[] encrypt(byte[] clearText){
        return encode(clearText);
    }

    /**
     * <p>Decrypt a String.</p>
     * @param encoded The string to be decoded.
     * @return The decoded string.
     */
    public byte[] decrypt(byte[] encoded){
        return decode(encoded);
    }

    /**
     * <p>Encrypt a String.</p>
     * @param clearText The string to be encoded.
     * @return The encoded string.
     */
    private byte[] encode(byte[] clearText){
        if (clearText == null || clearText.length == 0)
            return null;

        try {
            byte type[] = new byte[2];

            String encAlgString = DEFAULT_ENCYPTION_ALG;
            EncryptionAlgorithm encAlg = getEncryptionAlg(encAlgString);
            int i = getEncryptionByte(encAlgString);
            type[1] = (byte)i;

            Cipher cipher = mToken.getCipherContext(encAlg);

            String keyA = DEFAULT_KEYGEN_ALG;
            i = getKeyGenByte(keyA);
            type[0] = (byte)i;
            SymmetricKey sk = getSymmetricKey(i);

            // bug in JSS: msg in stdout.
            //secureRandom.nextBytes(iv);
            IVParameterSpec ivSpec = getIVParameterSpec(i);
            byte iv[] = ivSpec.getIV();
            cipher.initEncrypt(sk, ivSpec);
            byte enc[] = cipher.doFinal(clearText);
            enc = addPrefix(type, iv, enc);
            return (enc);
        } catch (Throwable e) {
            if (debug != null) {
                debug.error("in encode string " + e);
            }
            return null;
        }
    }

    /** 
     * Decode an encoded string
     *
     * @param encoded The encoded string.
     * @return The decoded string.
     **/
    private byte[] decode(byte[] encoded) {
        if (encoded == null || encoded.length == 0) {
            return null;
        }
        try {
            byte share[] = encoded;
            if (share[0] != VERSION) {
                if (debug != null) {
                    debug.error(
                        "In decode string: unsupported version:"+share[0]);
                }
                return null;
            }
            // get the alg from the string
            byte type[] = getType(share);
            // get the encrypted data
            share = getRaw(share);

            if ((int)type[1] < 0 && (int)type[1] >= NUM_ENCRYPTION_ALG){
                if (debug != null) {
                    debug.error("In decode string: unsupported encryption bit:" 
                        + (int)type[1]);
                }
                return null;
            }

            EncryptionAlgorithm encAlg =
                getEncryptionAlg(ENCRYPTION_ALGS[(int)type[1]]);

            Cipher cipher = mToken.getCipherContext(encAlg);

            if ((int)type[0] < 0 && (int)type[0] >= NUM_KEYGEN_ALG){
                if (debug != null) {
                    debug.error(
                        "In decode string: unsupported keygen bit:" 
                        + (int)type[0]);
                }
                return null;
            }
            
            SymmetricKey sk = getSymmetricKey((int)type[0]);
            IVParameterSpec ivSpec = getIVParameterSpec((int)type[0]);
            cipher.initDecrypt(sk, ivSpec);
            byte dec[] = cipher.doFinal(share);
            if (dec == null) {
                debug.error("Failed to decode " + encoded);
                return null;
            }
            return (dec);
        } catch (Throwable e) {
            if (debug != null) {
                debug.error("in decoding string " + encoded, e);
            }
            return null;
        }
    }

    private static byte[] addPrefix(byte type[], byte iv[], byte share[]) {
        byte data[] = new byte[share.length + 11];
        data[0] = VERSION;
        data[1] = type[0];
        data[2] = type[1];
        for (int i = 0; i < 8; i++) {
            data[3+i] = iv[i];
        }
        for (int i = 0; i < share.length; i++) {
            data[11+i] = share[i];
        }
        return data;
    }

    private static byte[] getType(byte share[]) {
        byte type[] = new byte[2];
        type[0] = share[1];
        type[1] = share[2];
        return type;
    }

    private static byte[] getIV(byte share[]) {
        byte iv[] = new byte[8];
        for (int i = 0; i < 8; i++) {
            iv[i] = share[i+3];
        }
        return iv;
    }

    private static byte[] getRaw(byte share[]) {
        byte data[] = new byte[share.length-11];
        for (int i = 11; i <share.length; i++) {
            data[i-11] = share[i];
        }
        return data;
    }

    private static int getKeyGenByte(String algName) {
        for (int i = 0; i < NUM_KEYGEN_ALG; i++) {
            if (algName.equals(KEYGEN_ALGS[i])) {
                return i;
            }
        }
        if (debug != null) {
            debug.error("keyGen algorithm is not valid.");
        }
        // return the default
        return 0;
    }

    private static PBEAlgorithm getKeyGenAlg(String algName) {
        if (algName.equals("PBE_SHA1_DES3_CBC")) {
            return PBEAlgorithm.PBE_SHA1_DES3_CBC;
        } else if (algName.equals("PBE_MD2_DES_CBC")) {
            return PBEAlgorithm.PBE_MD2_DES_CBC;
        } else if (algName.equals("PBE_MD5_DES_CBC")) {
            return PBEAlgorithm.PBE_MD5_DES_CBC;
        } else if (algName.equals("PBE_SHA1_DES_CBC")) {
            return PBEAlgorithm.PBE_SHA1_DES_CBC ;
        } else if (algName.equals("PBE_SHA1_RC2_128_CBC")) {
            return PBEAlgorithm.PBE_SHA1_RC2_128_CBC;
        } else if (algName.equals("PBE_SHA1_RC2_40_CBC")) {
            return PBEAlgorithm.PBE_SHA1_RC2_40_CBC;
        } else if (algName.equals("PBE_SHA1_RC4_128")) {
            return PBEAlgorithm.PBE_SHA1_RC4_128;
        } else if (algName.equals("PBE_SHA1_RC4_40")) {
            return PBEAlgorithm.PBE_SHA1_RC4_40;
        } else {
            if (debug != null) {
                debug.message("keyGen algorithm is not valid.");
            }
            return PBEAlgorithm.PBE_SHA1_DES3_CBC;
        }
    }

    private static int getEncryptionByte(String algName) {
        for (int i = 0; i < NUM_ENCRYPTION_ALG; i++) {
            if (algName.equals(ENCRYPTION_ALGS[i])) {
                return i;
            }
        }
        if (debug != null) {
            debug.error("Encryption algorithm is not valid.");
        }
        // return the default
        return 0;
    }

    private static EncryptionAlgorithm getEncryptionAlg(String algName) {
        if (algName.equals("DES3_CBC_PAD")) {
            return EncryptionAlgorithm.DES3_CBC_PAD;
        } else if (algName.equals("DES3_CBC")) {
            return EncryptionAlgorithm.DES3_CBC;
        } else if (algName.equals("DES3_ECB")) {
            return EncryptionAlgorithm.DES3_ECB;
        } else if (algName.equals("DES_CBC")) {
            return EncryptionAlgorithm.DES_CBC;
        } else if (algName.equals("DES_CBC_PAD")) {
            return EncryptionAlgorithm.DES_CBC_PAD;
        } else if (algName.equals("DES_ECB")) {
            return EncryptionAlgorithm.DES_ECB;
        } else if (algName.equals("RC2_CBC")) {
            return EncryptionAlgorithm.RC2_CBC;
        } else if (algName.equals("RC4")) {
            return EncryptionAlgorithm.RC4;
        } else {
            if (debug != null) {
                debug.message("Encryption algorithm is not valid.");
            }
            return EncryptionAlgorithm.DES3_CBC_PAD;
        }  
    }
}

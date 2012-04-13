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
 * $Id: DataEncryptor.java,v 1.1 2009/02/26 23:58:41 exu Exp $
 *
 */

package com.sun.identity.security;

import java.util.Map;
import java.util.HashMap;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import javax.crypto.NoSuchPaddingException;
import java.security.Key;
import java.security.InvalidKeyException;
import java.io.UnsupportedEncodingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import com.sun.identity.shared.encode.Base64;


/**
 * This class <code>DataEncryptor</code> is used to encrypt the data
 * with symmetric and asymmetric keys.
 * @supported.all.api
 */
public class DataEncryptor {

    private static final String ENCRYPTED_DATA = "EncryptedData";
    
    private static final String ENCRYPTED_KEY = "EncryptedKey";
    private static final byte[] ___y = { 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
            0x01, 0x01 };
    private static final int ITERATION_COUNT = 5;
    private static PBEParameterSpec pbeParameterSpec = new PBEParameterSpec(
            ___y, ITERATION_COUNT);

    /**
     * Encrypts the given data with an asymmetric key. The asymmetric 
     * encryption uses symmetric secret key for data encryption and sends
     * the secret key to the recipient by encrypting the same with given
     * transport key (publick key). 
     * @param data the data to be encrypted.
     * @param encryptionAlgorithm the encryption algorithm to be used.
     *        The encryption algorithm must be one of the supported
     *        algorithm by the underlying JCE encryption provider.
     *        Examples of encryption algorithms are "DES", "AES" etc. 
     * @param encryptionStrength the encryption strength for a given
     *                           encryption algorithm.
     * @param encKey the encryption key to be used. For PKI, this
     *               key should be public key of the intended recipient.  
     * @return the encrypted data in Base64 encoded format.
     */
    public static String encryptWithAsymmetricKey(String data,
                   String encryptionAlgorithm,
                   int encryptionStrength,
                   Key encKey) throws Exception {
        try {
            KeyGenerator keygen = KeyGenerator.getInstance(encryptionAlgorithm);
            if(encryptionStrength != 0) {
               keygen.init(encryptionStrength);
            }
            SecretKey sKey = keygen.generateKey();
            Cipher cipher = Cipher.getInstance(encryptionAlgorithm); 
            cipher.init(Cipher.ENCRYPT_MODE, sKey);
            byte[] encData = cipher.doFinal(data.getBytes("UTF-8")); 
            cipher = Cipher.getInstance(encKey.getAlgorithm());
            cipher.init(Cipher.WRAP_MODE, encKey);
            byte[] keyWrap = cipher.wrap(sKey);
            byte[] encDataPad = wrapKeyWithEncryptedData(encData, keyWrap);
            return Base64.encode(encDataPad);
        } catch (NoSuchAlgorithmException nse) {
            throw new Exception(nse.getMessage());
        } catch (NoSuchPaddingException npe) {
            throw new Exception(npe.getMessage());
        } catch (InvalidKeyException ike) {
            throw new Exception(ike.getMessage());
        } catch (UnsupportedEncodingException uae) {
            throw new Exception(uae.getMessage());
        } 
    }

    /**
     * Decrypts the given data with asymmetric key.
     * @param data the data to be decrypted.
     * @param encAlgorithm the encryption algorithm was used for encrypted
     *                     data. 
     * @param encKey the private key for decrypting the data.
     * @return the decrypted data.
     */
    public static String decryptWithAsymmetricKey(
              String data, String encAlgorithm, Key encKey) throws Exception {

        try {
            byte[] tmp = Base64.decode(data);
            Map map = unwrapKeyWithEncodedData(tmp);
            byte[] encData = (byte[])map.get(ENCRYPTED_DATA);
            byte[] keyData = (byte[])map.get(ENCRYPTED_KEY);
            Cipher cipher = Cipher.getInstance(encKey.getAlgorithm());
            cipher.init(Cipher.UNWRAP_MODE, encKey); 
            Key secretKey = cipher.unwrap(keyData, encAlgorithm, 
                   Cipher.SECRET_KEY);
            cipher = Cipher.getInstance(encAlgorithm);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedData =  cipher.doFinal(encData);
            return Base64.encode(decryptedData);
        } catch (NoSuchAlgorithmException nse) {
            throw new Exception(nse.getMessage());
        } catch (InvalidKeyException ike) {
            throw new Exception(ike.getMessage());
        }
    }

    /**
     * Encrypts the given data with a symmetric key that was generated
     * using given shared secret. 
     * @param data the data to be encrypted.
     * @param encAlgorithm the encryption algorithm to be used.
     *        The encryption algorithm must be one of the supported
     *        algorithm by the underlying JCE encryption provider.
     *        For password based encryptions, the encryption algorithm
     *        PBEWithMD5AndDES is commonly used. 
     * @param secret the shared secret to be used for symmetric encryption.
     * @return the encrypted data in Base64 encoded format. 
     */
    public static String encryptWithSymmetricKey(String data, 
               String encAlgorithm, String secret) throws Exception {
        try {
            String algorithm = encAlgorithm;
            if(!algorithm.startsWith("PBEWith")) {
               algorithm = "PBEWithMD5And" + encAlgorithm;
            }
            SecretKeyFactory skFactory = 
                     SecretKeyFactory.getInstance(algorithm);
            PBEKeySpec pbeKeySpec = new PBEKeySpec(secret.toCharArray());
            SecretKey sKey = skFactory.generateSecret(pbeKeySpec);
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, sKey, pbeParameterSpec );
            byte[] encData = cipher.doFinal(data.getBytes("UTF-8")); 
            encData = addPrefix(encData); 
            return Base64.encode(encData);
        } catch (NoSuchAlgorithmException nse) {
            throw new Exception(nse.getMessage());
        } 
    }

    /**
     * Decrypts the given data with a symmetric key generated using shared
     * secret.
     * @param data the data to be decrypted with symmetric key.
     * @param encAlgorithm the encryption algorithm was used for
     *        encrypting the data.
     * @param secret the shared secret to be used for decrypting the data.
     * @return the decrypted data. 
     */
    public static String decryptWithSymmetricKey(String data, 
             String encAlgorithm, String secret) throws Exception {
        try {
            String algorithm = encAlgorithm;
            if(!algorithm.startsWith("PBEWith")) {
               algorithm = "PBEWithMD5And" + encAlgorithm;
            }
            SecretKeyFactory skFactory = 
                     SecretKeyFactory.getInstance(algorithm);
            PBEKeySpec pbeKeySpec = new PBEKeySpec(secret.toCharArray());
            SecretKey sKey = skFactory.generateSecret(pbeKeySpec);
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.DECRYPT_MODE, sKey, pbeParameterSpec);
            byte[] tmp = Base64.decode(data);
            byte[] encData = removePrefix(tmp);
            byte[] decData = cipher.doFinal(encData);
            return Base64.encode(decData);
         } catch (NoSuchAlgorithmException nse) {
            throw new Exception(nse.getMessage());
         }
    }

    private static byte[] addPrefix(byte[] encData) {
        int length = encData.length;
        byte[] result = new byte[9 + length]; 
        byte[] encrypted = new String("ENCRYPTED").getBytes();
        for (int i=0; i < 9; i++) {
             result[i] = encrypted[i];
        }
        for (int i=0; i <  length; i++) {
            result[9 + i] = encData[i];
        }
        return result;
    }

    private static byte[] removePrefix(byte[] data) {
        int length = data.length - 9;
        byte[] result = new byte[length];
        for (int i=0; i < length; i++) {
            result[i] = data[9 + i];
        }
        return result;
    }

    private static byte[] wrapKeyWithEncryptedData(byte[] data, 
                         byte[] key) {

        int dataLength = data.length;
        int keyLength = key.length;
        byte[] result = new byte[17 + data.length + key.length];
        byte[] encrypted = new String("ENCRYPTED").getBytes();
        for (int i=0; i < 9; i++) {
             result[i] = encrypted[i];
        }
        byte[] datasize = intToByteArray(dataLength);
        for (int i =0; i < 4; i++) {
            result[i+9] = datasize[i];
        }

        for (int i=0; i < dataLength; i++) {
             result[i+13] = data[i];
        }

        byte[] keysize = intToByteArray(keyLength);
        for (int i =0; i < 4; i++) {
            result[i+13+dataLength] = keysize[i];
        }

        for (int i=0; i < keyLength; i++) {
             result[i+17+dataLength] = key[i];
        }

        return result;
    }

    private static Map unwrapKeyWithEncodedData(byte[] decodeData) {
        Map map = new HashMap();
        
        byte[] dataLength = new byte[4]; 
        int j = 0;
        for (int i = 9; i < 13; i++) {
             dataLength[j] = decodeData[i];
             j++;
        }

        int encDataLength = byteArrayToInt(dataLength); 
        byte[] encData = new byte[encDataLength];
        j = 0;
        for (int i=13; i < encDataLength + 13; i++) {
             encData[j] = decodeData[i];
             j++;
        }

        map.put(ENCRYPTED_DATA, encData);
        byte[] keyLen = new byte[4];
        int startIndex = 13 + encDataLength;
        int endIndex = startIndex + 4;
        j = 0;
        for (int i=startIndex; i < endIndex; i++) {
             keyLen[j] = decodeData[i]; 
             j++;
        }

        int keyDataLength = byteArrayToInt(keyLen);
        startIndex = startIndex + 4; 
        endIndex = startIndex + keyDataLength; 
        byte[] keyData = new byte[keyDataLength];
        j = 0;
        for (int i=startIndex; i < endIndex; i++) {
             keyData[j] = decodeData[i]; 
             j++;
        }
        map.put(ENCRYPTED_KEY, keyData);
        return map;
    }


    private static final byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value
        };
    }

    private static final int byteArrayToInt(byte [] b) {
        return (b[0] << 24)
                + ((b[1] & 0xFF) << 16)
                + ((b[2] & 0xFF) << 8)
                + (b[3] & 0xFF);
    }



}

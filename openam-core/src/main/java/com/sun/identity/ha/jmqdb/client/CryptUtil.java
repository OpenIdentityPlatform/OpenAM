/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: CryptUtil.java,v 1.2 2008/06/25 05:43:28 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.ha.jmqdb.client;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.NoSuchPaddingException;

@Deprecated
public class CryptUtil {

    static final String DEFAULT_PBE_PWD =
        "KmhUnWR1MYWDYW4xuqdF5nbm+CXIyOVt";
    private static final String CRYPTO_DESCRIPTOR =
        "PBEWithMD5AndDES";   
    private static final String KEYGEN_ALGORITHM =
        "PBEWithMD5AndDES";
    private static final int    ITERATION_COUNT = 5;


    private static final byte[]     ___y = {
        0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01
    };

    static private SecretKey        pbeKey;
    static private boolean          _initialized = false;

    private static PBEParameterSpec pbeParameterSpec =
        new PBEParameterSpec(___y, ITERATION_COUNT);

    /**
     * Method declaration
     *
     * @param clearText
     */
    static public String encrypt(String pbePassword,
                                 String clearText) {
        setPassword(pbePassword);
        byte[] encData = pbeEncrypt(clearText.getBytes());

        // BASE64 encode the data
        String str = base64encode(encData).trim();

        // Serialize the data, i.e., remove \n and \r
        BufferedReader bufReader = 
            new BufferedReader(new StringReader(str));
        StringBuilder strClean = new StringBuilder(str.length());
        String strTemp = null;
        try {
            while ((strTemp = bufReader.readLine()) != null) {
                strClean.append(strTemp);
            }
        } catch (IOException ioe) {
            System.out.println("Crypt:: Error while base64 encoding");
        }
        
        return strClean.toString();
    }

    /**
     * Method declaration
     *
     * @param encText
     */
    static public String decrypt(String pbePassword,
                                 String encText) {
        setPassword(pbePassword);
        if (encText == null || encText.length() == 0){
            System.out.println("encText == null");
        }        

        // BASE64 decode the data
        byte[] encData = encData = base64decode(encText.trim());

        // Decrypt the data
        byte[] rawData = pbeDecrypt(encData);
        if (rawData == null) {
            System.out.println("rawData == null");
        }

        // Convert to String and return
        String answer = new String(rawData);

        return answer;
    }

    /**
     * Method declaration
     *
     * @param clearText
     */
    static private byte[] pbeEncrypt(byte[] clearText) {
	byte[] result = null;
        if (clearText == null || clearText.length == 0) {
            return null;
        }

        if (_initialized) {
            try {

                Cipher pbeCipher = null;
		try {
                    pbeCipher = Cipher.getInstance(CRYPTO_DESCRIPTOR);
		} catch (Exception ex) {
		    if (ex instanceof NoSuchAlgorithmException ||
			ex instanceof NoSuchPaddingException) 
		    {
			//Best effort try dynamically registring the SunJCE provider
                        System.out.println("JCEEncryption: Exception caught: ");
                        pbeCipher = Cipher.getInstance(CRYPTO_DESCRIPTOR);
		    } else {
			throw ex;
		    }
		}

		if (pbeCipher != null) {
                    pbeCipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParameterSpec);

                    result = pbeCipher.doFinal(clearText);
		} else  {
		    System.out.println("JCEEncryption: Failed to obtain Cipher");
		}
            } catch (Exception ex) {
                System.out.println("JCEEncryption:: failed to encrypt data");
            }
        } else {
            System.out.println("JCEEncryption:: not yet initialized");
        }

        return result;
    }

    /**
     * Method declaration
     *
     * @param cipherText
     */
    static private byte[] pbeDecrypt(byte[] cipherText) {
	byte[] result = null;
        if (_initialized) {
            try {
                byte share[] = cipherText;                
                Cipher pbeCipher = null;
		try {
                    pbeCipher = Cipher.getInstance(CRYPTO_DESCRIPTOR);
		} catch (Exception ex) {
		    if (ex instanceof NoSuchAlgorithmException ||
			ex instanceof NoSuchPaddingException) 
		    {
			//Best effort try dynamically registring the SunJCE provider
                        System.out.println("JCEEncryption: Exception caught: ");
                        pbeCipher = Cipher.getInstance(CRYPTO_DESCRIPTOR);
		    } else {
			throw ex;
		    }
		}

		if (pbeCipher != null) {
                    pbeCipher.init(Cipher.DECRYPT_MODE, 
                                   pbeKey, pbeParameterSpec);		
                    result = pbeCipher.doFinal(share);
                    
		} else {
		    System.out.println("JCEEncryption: Failed to obtain Cipher");
		}
            } catch (Exception ex) {
                System.out.println("JCEEncryption:: failed to decrypt data");
            }
        } else {
            System.out.println("JCEEncryption:: not yet initialized");
        }

        return result;
    }

    //****************************************************
    // utility methods for BASE64 encode/decode
    //****************************************************

    //
    // code characters for values 0..63
    //
    static private char[] alphabet =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="
    .toCharArray();

    //
    // lookup table for converting base64 characters to value in range 0..63
    //
    static private byte[] codes = new byte[256];
    static {
        for (int i=0; i<256; i++) codes[i] = -1;
        for (int i = 'A'; i <= 'Z'; i++) codes[i] = (byte)( i - 'A');
        for (int i = 'a'; i <= 'z'; i++) codes[i] = (byte)(26 + i - 'a');
        for (int i = '0'; i <= '9'; i++) codes[i] = (byte)(52 + i - '0');
        codes['+'] = 62;
        codes['/'] = 63;
    }

    /**
    * Returns a String of base64-encoded characters to represent the
    * passed data array.
    *
    * @param data the array of bytes to encode
    * @return String base64-coded characters
    */
    static public String base64encode(byte[] data){
        char[] out = new char[((data.length + 2) / 3) * 4];

        //
        // 3 bytes encode to 4 chars. Output is always an even
        // multiple of 4 characters.
        //
        for (int i=0, index=0; i<data.length; i+=3, index+=4) {
            boolean quad = false;
            boolean trip = false;

            int val = (0xFF & (int) data[i]);
            val <<= 8;
            if ((i+1) < data.length) {
                val |= (0xFF & (int) data[i+1]);
                trip = true;
            }
            val <<= 8;
            if ((i+2) < data.length) {
                val |= (0xFF & (int) data[i+2]);
                quad = true;
            }
            out[index+3] = alphabet[(quad? (val & 0x3F): 64)];
            val >>= 6;
            out[index+2] = alphabet[(trip? (val & 0x3F): 64)];
            val >>= 6;
            out[index+1] = alphabet[val & 0x3F];
            val >>= 6;
            out[index+0] = alphabet[val & 0x3F];
        }
        return new String(out);
    }

    /**
    * Returns an array of decoded bytes which were encoded in the passed
    * byte array.
    *
    * @param encdata the array of base64-encoded characters
    * @return byte[] the decoded data array
    */
    static public byte[] base64decode(String encdata){
        // convert to a char[]
        char[] data = encdata.toCharArray();

        if (data.length <= 0)
            throw new RuntimeException("Invalid encoded data!");
	    // check that length is a multiple of 4
	    if(data.length % 4 != 0)
	        throw new RuntimeException("Data is not Base64 encoded.");

        int len = ((data.length + 3) / 4) * 3;
        if (data[data.length-1] == '=') --len; 
        if (data[data.length-2] == '=') --len;
        byte[] out = new byte[len];

        int shift = 0; // # of excess bits stored in accum
        int accum = 0; // excess bits
        int index = 0;

        for (int ix=0; ix<data.length; ix++){
            int value = codes[ data[ix] & 0xFF ]; // ignore high byte of char
            if ( value >= 0 ) { // skip over non-code
                accum <<= 6; // bits shift up by 6 each time thru
                shift += 6; // loop, with new bits being put in
                accum |= value; // at the bottom.
                if ( shift >= 8 ) { // whenever there are 8 or more shifted in,
                    shift -= 8; // write them out (from the top, leaving any
                    out[index++] = // excess at the bottom for next iteration.
                    (byte) ((accum >> shift) & 0xff);
                }
            }
	    else
            {
                if (data[ix] != '=') {
                    throw new RuntimeException("Data is not Base64 encoded.");
                }
            }
        }
        if (index != out.length)
            throw new RuntimeException("Data length mismatch.");

        return out;
    }

    static private void setPassword(String passwd) {
        try {
            pbeKey =
                SecretKeyFactory.getInstance(KEYGEN_ALGORITHM).generateSecret(
                    new PBEKeySpec(passwd.toCharArray()));
            _initialized = true;    
        } catch (Exception e) {
            System.out.println("Error in initializing the password");
        }
    }

    //****************************************************
    // Testing Main program
    //****************************************************
    static public void main(String args[]) {
    
        String password = "superalan";
        System.out.println("password = "+ password);

        // *******************************************
        // Encrypting the data ...
        // *******************************************
        String encrypted = 
            CryptUtil.encrypt(CryptUtil.DEFAULT_PBE_PWD,
                                 password);
        
        System.out.println("encrypted = "+ encrypted);

        // *******************************************
        // Decrypting the data
        // *******************************************
        String decrypted = 
            CryptUtil.decrypt(CryptUtil.DEFAULT_PBE_PWD,
                                 encrypted);

        System.out.println("decrypted = "+ decrypted);
    }    
}

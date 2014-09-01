/* The contents of this file are subject to the terms
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
 * $Id: CryptoUtils.java,v 1.2 2009/09/15 13:27:13 ppetitsm Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 * Portions Copyrighted 2008 Patrick Petit Consulting
 */
package com.identarian.infocard.opensso.rp;

import com.identarian.infocard.opensso.rp.exception.InfocardException;
import java.security.Key;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** 
 *
 * @author Patrick
 */
class CryptoUtils {

    private static final String CIPHER_INSTANCE_NAME = "DES/CBC/PKCS5Padding";
    private static byte[] ivBytes = new byte[]{(byte)0x64, (byte)0xab, (byte)0xe0, (byte)0x0b,
    (byte)0xd6, (byte)0x20, (byte)0x83, (byte)0x5b };

    protected static byte[] encrypt(String plaintext, InfocardIdRepoData icData)
            throws InfocardException {

        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("DES");
            SecretKey key = keyGen.generateKey();
            Cipher cipher = Cipher.getInstance(CIPHER_INSTANCE_NAME);
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(ivBytes));
            byte[] input = plaintext.getBytes();
            byte[] cipherText = new byte[cipher.getOutputSize(input.length)];
            int ctLength = cipher.update(input, 0, input.length, cipherText, 0);
            ctLength += cipher.doFinal(cipherText, ctLength);
            icData.setEncPasswdLength(ctLength);
            icData.setKey(key);
            return cipherText;
        } catch (Exception e) {
            e.printStackTrace();
            throw new InfocardException("Fatal internal error", e);
        }
    }

    protected static String decrypt(byte[] cipherText, InfocardIdRepoData icData)
            throws InfocardException {

        try {
            Key encryptionKey = icData.getKey();
            SecretKeySpec decryptionKey = new SecretKeySpec(encryptionKey.getEncoded(),
                    encryptionKey.getAlgorithm());
            Cipher cipher = Cipher.getInstance(CIPHER_INSTANCE_NAME);
            cipher.init(Cipher.DECRYPT_MODE, decryptionKey, new IvParameterSpec(ivBytes));
            byte[] plainText = new byte[cipher.getOutputSize(icData.getEncPasswdLenght())];
            int ptLength = cipher.update(cipherText, 0, icData.getEncPasswdLenght(), plainText, 0);
            cipher.doFinal(plainText, ptLength);
            return new String(plainText).trim();
        } catch (Exception e) {
            e.printStackTrace();
            throw new InfocardException("Fatal internal error", e);
        }
    }
}

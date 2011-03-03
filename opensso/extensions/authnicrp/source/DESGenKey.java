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
 * $Id: DESGenKey.java,v 1.1 2009/09/15 13:27:13 ppetitsm Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 * Portions Copyrighted 2008 Patrick Petit Consulting
 */
import java.security.NoSuchAlgorithmException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * This class is used to generate a unique key used for DES
 * encryption/decryption purpose.
 */
public class DESGenKey {

    public static void main(String[] args) {
        try {
            KeyGenerator kg = KeyGenerator.getInstance("DES");

            SecretKey key = kg.generateKey();
            byte[] desKey = key.getEncoded();
                 StringBuffer sbuf = new StringBuffer();
            for (byte b : desKey) {
                sbuf.append(String.format("%02x", (b & 0xFF)));
            }
            System.out.println("DES key: " + sbuf);
        } catch (NoSuchAlgorithmException noe) {
            System.out.println("DESGenKey.main: NoSuchAlgorithmException " +
                    "occured while generating the key " + noe);
        }
    }
}

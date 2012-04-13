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
 * $Id: SecureLogHelperJCEImpl.java,v 1.6 2008/06/25 05:43:38 qcheng Exp $
 *
 */



package com.sun.identity.log.secure;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.sun.identity.log.LogConstants;
import com.sun.identity.log.LogManagerUtil;
import com.sun.identity.log.spi.Debug;
import com.sun.identity.security.keystore.AMPassword;
import com.sun.identity.security.keystore.AMCallbackHandler;
import com.sun.identity.security.keystore.AMX509KeyManager;
import com.sun.identity.security.keystore.AMX509KeyManagerFactory;

/**
 * A helper class for secure logging that generates the MAC and maintaining 
 * the key state
 * Refer to Secure Logging Scheme on CMS website
 **/
public class SecureLogHelperJCEImpl extends SecureLogHelper {
    
    /**
     * Static variables to use random or DES3 for generation of key data
     */
    static private AMX509KeyManager ksManager = null;
    
    void initializeKeyStoreManager(AMPassword passwd) 
        throws Exception {
        String keystore = LogManagerUtil.getLogManager().
            getProperty(LogConstants.LOGGER_CERT_STORE);
        ksManager = AMX509KeyManagerFactory.createAMX509KeyManager(
                    "JKS", keystore, (String)null, (AMCallbackHandler)passwd);
    }    
    
    /**
     * Signs the given MAC
     * @param mac the mac to be signed
     * @return signed MAC for given mac entry
     * @throws Exception if it fails to sign the MAC
     */
    public byte[] signMAC(byte[] mac)
    throws Exception{
        try{
            PrivateKey loggerPrivKey = null;
            try{
                loggerPrivKey = ksManager.getPrivateKey(loggerKey);
            }catch(Exception e) {
                Debug.error("SecureLogHelper.signMAC() : " +
                    " Exception : ", e);
            }
            Signature loggerSign = Signature.getInstance(signingAlgorithm);
                loggerSign.initSign(loggerPrivKey);
            loggerSign.update(mac);
            byte[] signedBytes = loggerSign.sign();
            writeToSecretStore(signedBytes, logFileName,
                loggerPass, currentSignature);
            return signedBytes;
        }catch(Exception e){
            Debug.error("SecureLogHelper.signMAC() : " +
                " Exception : ", e);
            throw new Exception(e.getMessage());
        }
    }
    
    /**
     * Verifies the given signature
     * @param signedObject the signature to be verified
     * @param mac mac entry for the signature
     * @return true if signature for mac is valid
     * @throws Exception if it fails to verify signature value for mac entry
     */
    public boolean verifySignature(byte[] signedObject,byte[] mac)
    throws Exception{
        try{
            PublicKey loggerPubKey = null;
            X509Certificate certs[] = ksManager.getCertificateChain(loggerKey);
            X509Certificate cert = certs[0];
            
            loggerPubKey = cert.getPublicKey();
            
            Signature verifySign = Signature.getInstance(signingAlgorithm);
            verifySign.initVerify(loggerPubKey);
            verifySign.update(mac);
            
            return verifySign.verify(signedObject);
        }catch(Exception e){
            Debug.error("SecureLogHelper.verifySignature() : " +
                " Exception : ", e);
            throw new Exception(e.getMessage());
        }
    }

    /**
     * Returns matched secret data from from the secret Storage. 
     * At a time there are only 3 things in logger's secure store file 
     *    - initialkey, currentkey and current signature
     * In the verifier secure store file there is just the initial key of the
     * logger and the currentKey
     * @param filename file for secret storage
     * @param dataType The kind of data to be read, whether it is a
     *                 signature or a key
     * @param password password for the file
     * @return secure data that is matched with dataType
     * @throws Exception if it fails to read secret data from secret store
     */
    byte[] readFromSecretStore(String filename,
        String dataType, 
        AMPassword password
    ) throws Exception {
        byte[] cryptoData = null;
        File file = new File(filename);
        // open input file for reading
        FileInputStream fis = new FileInputStream(file);
        KeyStore store = KeyStore.getInstance("jceks");
        store.load(fis, password.getChars());
        fis.close();

        KeyStore.ProtectionParameter params = 
            new KeyStore.PasswordProtection(password.getChars());
        KeyStore.SecretKeyEntry keyentry = 
            (KeyStore.SecretKeyEntry)store.getEntry(dataType, params);
        if (keyentry != null) {
            SecretKey sdata = keyentry.getSecretKey();
            cryptoData = (byte[]) sdata.getEncoded();
        }

        return cryptoData;
    }
    
    /**
     * Writes to the secret Storage. If the data to be written is a key, then
     * writes the older signature also. If it is a signature then writes the
     * older key also
     * @param cryptoMaterial The data to be written to the secret storage
     * @param filename The file for secret storage
     * @param password The password for the file
     * @param dataType The kind of cryptoMaterial, whether it is a signature
     * or a key
     * @throws Exception if it fails to write secret data from secret store
     */
    void writeToSecretStore(
        byte[] cryptoMaterial, 
        String filename, 
        AMPassword password, 
        String dataType
    ) throws Exception {
        KeyStore store = KeyStore.getInstance("jceks");
        File file = new File(filename);
        if (file.exists()) {
            FileInputStream fis = new FileInputStream(file);
            store.load(fis, password.getChars());
            fis.close();
        } else {
            store.load(null, new char[0]);
        }

        if (store.containsAlias(dataType)) {
            store.deleteEntry(dataType);
        }

        SecretKeySpec data = new SecretKeySpec(cryptoMaterial, "DESede");
        KeyStore.SecretKeyEntry secKeyEntry = 
            new KeyStore.SecretKeyEntry(data);
        KeyStore.ProtectionParameter params =
            new KeyStore.PasswordProtection(password.getChars());
        store.setEntry(dataType, secKeyEntry, params);

        FileOutputStream fos = new FileOutputStream(file);
        store.store(fos, password.getChars());
        fos.close();
    }
}

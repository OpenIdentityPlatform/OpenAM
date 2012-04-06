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
 * $Id: SecureLogHelperJSSImpl.java,v 1.2 2008/06/25 05:43:38 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.log.secure;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

import org.mozilla.jss.CryptoManager;
import org.mozilla.jss.asn1.ANY;
import org.mozilla.jss.asn1.ASN1Value;
import org.mozilla.jss.asn1.BMPString;
import org.mozilla.jss.asn1.OCTET_STRING;
import org.mozilla.jss.asn1.SEQUENCE;
import org.mozilla.jss.asn1.SET;
import org.mozilla.jss.crypto.PBEAlgorithm;
import org.mozilla.jss.crypto.X509Certificate;
import org.mozilla.jss.pkcs12.AuthenticatedSafes;
import org.mozilla.jss.pkcs12.PFX;
import org.mozilla.jss.pkcs12.SafeBag;
import org.mozilla.jss.pkix.primitive.Attribute;
import org.mozilla.jss.util.Password;

import com.iplanet.am.util.JSSInit;
import com.sun.identity.security.keystore.AMPassword;
import com.sun.identity.log.spi.Debug;

/**
 * A helper implementation class for secure logging that generates 
 * the MAC and maintaining the key state
 * Refer to Secure Logging Scheme on CMS website
 **/
public class SecureLogHelperJSSImpl extends SecureLogHelper {
    
    /**
     * Static variables to use random or DES3 for generation of key data
     */
    static private CryptoManager cryptoMgr = null;
    
    void initializeKeyStoreManager(AMPassword password) 
    throws Exception {
        String method = "SecureLogHelperJSSImpl.initializeKeyStoreManager ";
        try {
            cryptoMgr = CryptoManager.getInstance();
            } catch (CryptoManager.NotInitializedException ex){
            if (Debug.messageEnabled()) {
                Debug.message(method +
                    "CryptoManager.NotInitializedException : ", ex);
            }
            JSSInit.initialize();
        }
            
        return;
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
            X509Certificate cert = null;
            try {
                cert = cryptoMgr.findCertByNickname(loggerKey);
            } catch (Exception e) {
                Debug.error("SecureLogHelper.signMAC() : " +
                    " Exception : ", e);
            }
            
            try{
                loggerPrivKey = cryptoMgr.findPrivKeyByCert(cert);
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
            X509Certificate cert = cryptoMgr.findCertByNickname(loggerKey);
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
     *  Creates local key identifier.
     *  @param encodedBytes The bytes whose hash has to be generated
     *  @return key identifier
     *  @throws Exception if it fails to generate key identifier
     */
    private byte[] createLocalKeyId(byte[] encodedBytes)
    throws Exception {
        try{
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(encodedBytes);
            return md.digest();
        }catch (Exception e){
            Debug.error("SecureLogHelper.createLocalKeyId() : " +
                " Exception : ", e);
            throw new Exception("Failed to create Key ID - " + e.toString());
        }
    }
    
    /**
     * Creates bag attributes.
     * @param nickName The nickname of the key / signature
     * @param localKeyId A hash of the entry to uniquely identify the given
     * key / signature
     * @throws Exception if it fails to generate key identifier
     */
    private SET createBagAttrs(String nickName, byte localKeyId[])
    throws Exception {
        try {
            SET attrs = new SET();
            SEQUENCE nickNameAttr = new SEQUENCE();
            nickNameAttr.addElement(SafeBag.FRIENDLY_NAME);
            SET nickNameSet = new SET();
            nickNameSet.addElement(new BMPString(nickName));
            nickNameAttr.addElement(nickNameSet);
            attrs.addElement(nickNameAttr);
            SEQUENCE localKeyAttr = new SEQUENCE();
            localKeyAttr.addElement(SafeBag.LOCAL_KEY_ID);
            SET localKeySet = new SET();
            localKeySet.addElement(new OCTET_STRING(localKeyId));
            localKeyAttr.addElement(localKeySet);
            attrs.addElement(localKeyAttr);
            return attrs;
        }catch (Exception e){
            Debug.error("SecureLogHelper.createBagAttrs() : " +
                " Exception : ", e);
            throw new Exception("Failed to create Key Bag - " + e.toString());
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
    byte[] readFromSecretStore(String filename, String dataType, 
                                               AMPassword password)
    throws Exception{
        // open input file for reading
        FileInputStream infile=null;
        infile = new FileInputStream(filename);
        
        // Decode the P12 file
        PFX.Template pfxt = new PFX.Template();
        PFX pfx = (PFX) pfxt.decode(new BufferedInputStream(infile, 2048));
        
        // Verify the MAC on the PFX.  This is important to be sure
        // it hasn't been tampered with.
        StringBuffer reason = new StringBuffer();
        MessageDigest md = MessageDigest.getInstance("SHA");
        Password jssPasswd = new Password(new String(md.digest(
                        password.getByteCopy()), "UTF-8").toCharArray());
        md.reset();

        if(!pfx.verifyAuthSafes(jssPasswd, reason) ){
            throw new Exception("AuthSafes failed to verify because: "
                + reason.toString());
        }
        AuthenticatedSafes authSafes = pfx.getAuthSafes();
        SEQUENCE safeContentsSequence = authSafes.getSequence();
        byte[] cryptoData = null;
        // Loop over contents of the authenticated safes
        for(int i=0; i < safeContentsSequence.size(); i++) {
            // The safeContents may or may not be encrypted.  We always send
            // the password in.  It will get used if it is needed.  If the
            // decryption of the safeContents fails for some reason (like
            // a bad password), then this method will throw an exception
            SEQUENCE safeContents = authSafes.getSafeContentsAt(jssPasswd, i);
            
            SafeBag safeBag = null;
            ASN1Value val = null;
            // Go through all the bags in this SafeContents
            for(int j=0; j < safeContents.size(); j++) {
                safeBag = (SafeBag) safeContents.elementAt(j);
                // look for bag attributes and then choose the key
                SET attribs = safeBag.getBagAttributes();
                if( attribs == null ){
                    Debug.error("Bag has no attributes");
                }else{
                    for(int b=0; b < attribs.size(); b++) {
                        Attribute a = (Attribute) attribs.elementAt(b);
                        if( a.getType().equals(SafeBag.FRIENDLY_NAME)){
                            // the friendly name attribute is a nickname
                            BMPString bs = (BMPString) ((ANY)a.getValues().
                                            elementAt(0)).decodeWith(BMPString.
                                            getTemplate());
                            if(dataType.equals(bs.toString())) {
                                // look at the contents of the bag
                                val = safeBag.getInterpretedBagContent();
                                break;
                            }
                        }
                    }
                }
            }
            if( val instanceof ANY )
                cryptoData = ((ANY) val).getContents();
        }
        // Close the file
        infile.close();
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
    void writeToSecretStore(byte[] cryptoMaterial, String filename, 
                                            AMPassword password, String dataType)
    throws Exception{
        byte[] oldDataFromSecretStorage = null;
        String oldDataType = null;
        
        MessageDigest md = MessageDigest.getInstance("SHA");
        Password jssPasswd = new Password(new String(md.digest(
                        password.getByteCopy()), "UTF-8").toCharArray());
        md.reset();
        
        // Do this only when the logger's file is being used
        if(filename.equals(logFileName) && loggerInitialized){
            // need to read the currentKey from the secret storage if the
            // dataType passed is currentSignature
            // since we have to maintain the current key as well as the
            // current signature in the PKCS12 file
            if(dataType.equals(currentSignature)) {
                oldDataFromSecretStorage = readFromSecretStore(logFileName, 
                                                          currentKey, password);
                oldDataType = currentKey;
            }else if(dataType.equals(currentKey)){
                // need to read the currentSignature 
                    // for the same reason as above
                oldDataFromSecretStorage = readFromSecretStore(logFileName,
                                                    currentSignature, password);
                oldDataType = currentSignature;
            }
        }
        // Start building the new contents by adding the older content first
        AuthenticatedSafes newAuthSafes = new AuthenticatedSafes();
        if(oldDataFromSecretStorage != null){
            SEQUENCE oldSafeContents = 
                             AddToSecretStore(oldDataFromSecretStorage,oldDataType);
            // Add the old contents to the existing safe
            newAuthSafes.addEncryptedSafeContents(
                                 PBEAlgorithm.PBE_SHA1_DES3_CBC , jssPasswd,
            null,AuthenticatedSafes.DEFAULT_ITERATIONS, oldSafeContents);
        }
        // Get the initial Key also and add it as old content, only if it is
        // not being added for the first time
        if((filename.equals(logFileName)) && 
                        !dataType.equals(initialKey) && loggerInitialized){
            byte[] key = readFromSecretStore(filename, initialKey, password);
            if(key != null){
                SEQUENCE initialKeySafeContents = 
                                       AddToSecretStore(key,initialKey);
                newAuthSafes.addEncryptedSafeContents(
                            PBEAlgorithm.PBE_SHA1_DES3_CBC, jssPasswd, null,
                            AuthenticatedSafes.DEFAULT_ITERATIONS ,
                            initialKeySafeContents);
            }
        }
        if((filename.equals(verifierFileName)) && 
                        !dataType.equals(initialKey) && verifierInitialized){
            byte[] key = readFromSecretStore(filename, initialKey, password);
            if(key != null){
                SEQUENCE initialKeySafeContents = 
                             AddToSecretStore(key,initialKey);
                newAuthSafes.addEncryptedSafeContents(
                               PBEAlgorithm.PBE_SHA1_DES3_CBC, jssPasswd, null,
                               AuthenticatedSafes.DEFAULT_ITERATIONS,
                               initialKeySafeContents);
            }
        }
        // Add the new contents
        SEQUENCE encSafeContents = AddToSecretStore(cryptoMaterial,dataType);
        
        // Add the new contents to the existing safe
        newAuthSafes.addEncryptedSafeContents(PBEAlgorithm.PBE_SHA1_DES3_CBC, 
                                jssPasswd, null,
                        AuthenticatedSafes.DEFAULT_ITERATIONS, encSafeContents);
        PFX newpfx = new PFX(newAuthSafes);
        newpfx.computeMacData(jssPasswd, null, 5);
        
        // write the new PFX out to the logger
        FileOutputStream fos = new FileOutputStream(filename);
        newpfx.encode(fos);
        fos.close();
    }
    
    /**
     * Adds  secret information to the secret Storage.
     * @param cryptoMaterial : The data to be added
     */
    private SEQUENCE AddToSecretStore(byte[] cryptoMaterial, String DataType)
    throws Exception{
        SEQUENCE encSafeContents = new SEQUENCE();
        ASN1Value data = new OCTET_STRING(cryptoMaterial);
        byte localKeyId[] = createLocalKeyId(cryptoMaterial);
        SET keyAttrs = createBagAttrs(DataType, localKeyId);
        // attributes: user friendly name, Local Key ID
        SafeBag keyBag = new SafeBag(SafeBag.SECRET_BAG,data,keyAttrs);
        encSafeContents.addElement(keyBag);
        return encSafeContents;
    }
}

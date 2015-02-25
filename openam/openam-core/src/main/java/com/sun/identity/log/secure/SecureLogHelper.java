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
 * $Id: SecureLogHelper.java,v 1.6 2009/04/07 23:24:33 hvijay Exp $
 *
 */

package com.sun.identity.log.secure;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.StringTokenizer;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import com.sun.identity.log.LogConstants;
import com.sun.identity.log.LogManagerUtil;
import com.sun.identity.log.spi.Debug;
import com.sun.identity.security.keystore.AMPassword;

/**
 * A helper class for logging that generates the MAC and maintaining the key
 * state
 **/
public abstract class SecureLogHelper {
    /**
     * Static variables to use random or DES3 for generation of key data
     */
    static int keyLength = 168;
    
    static String currentKeyName = null;
    static String loggerKey = "Logger";
    static String verifier = "Verifier";
    static final String initialKey = "InitialKey";
    static final String currentKey = "CurrentKey";
    static final String currentSignature = "CurrentSignature";
    
    byte[] currentLoggerKey = null;
    byte[] currentVerifierKey = null;
    byte[] currentVerifierSignature = null;
    byte[] lastMac = null;
    byte[] currentMAC = null;
    
    byte[] lastLoggerKey = null;
    
    String logFileName = null;
    String verifierFileName = null;
    
    boolean loggerInitialized = false;
    boolean verifierInitialized = false;
    
    boolean LoggerLastLine = false;
    boolean VerifierLastLine = false;
    
    AMPassword loggerPass = null;
    AMPassword verifierPass = null;

    static String signingAlgorithm = null;
    
    static {
        try {
            signingAlgorithm = LogManagerUtil.getLogManager().
                getProperty(LogConstants.SECURITY_SIGNING_ALGORITHM);
        } catch (Exception e) {
            signingAlgorithm = LogConstants.DEFAULT_SECURITY_SIGNING_ALGORITHM;
        }
    }

    /**
     * Signs the given MAC and returns the signature
     * @param mac the mac to be signed
     * @return the signature of given MAC
     * @throws Exception if it fails to sign the MAC
     */
    abstract public byte[] signMAC(byte[] mac)
    throws Exception;

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
    abstract byte[] readFromSecretStore(
        String filename,
        String dataType, 
        AMPassword password
    ) throws Exception;

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
    abstract void writeToSecretStore(
        byte[] cryptoMaterial,
        String filename, 
        AMPassword password,
        String dataType
    ) throws Exception;
    
    /**
     * Verifies the given MAC
     * @param signedObject : the signedObject to be verified
     * @param mac : signed mac
     * @return true if signedObject is verified without any problem
     * @throws Exception if signedObject can not be verified
     */
    abstract public boolean verifySignature(byte[] signedObject, byte[] mac)
    throws Exception;
    
    abstract void initializeKeyStoreManager(AMPassword passwd)
    throws Exception; 
    
    /**
     * Initialize logger by generating a new MACing key and storing it in
     * the secure storage
     * Also creates a file for the verifier with the same password. This file
     * is overwritten
     * with a new verifier(Auditor) supplied password when the Auditor logs
     * into the system for the first time
     * This method should be called only once for a given initial key
     * @param loggerFileName Logger related JCEKS file
     * @param LoggerPassword The password for the logging JCEKS file
     * @param verFileName : Verifier related JCEKS file
     * @param verifierPassword : The password for the verifier JCEKS file
     * @throws Exception if it fails to initialize SecureLogHelper
     */
    public synchronized void initializeSecureLogHelper(String loggerFileName,
                                                   AMPassword LoggerPassword, 
                                                   String verFileName, 
                                                   AMPassword verifierPassword) 
    throws Exception{
        logFileName = loggerFileName;
        verifierFileName = verFileName;
        
        loggerPass = LoggerPassword;
        AMPassword tempVerifierPass = verifierPassword;
        
        loggerInitialized = isInitialized(loggerFileName,loggerPass);
        if(!loggerInitialized) {
            initializeKeyStoreManager(LoggerPassword);
            
            // Generate an initial  key
            KeyGenerator keygen = KeyGenerator.getInstance("DESede");
            SecretKey k0 = keygen.generateKey();
            currentLoggerKey = k0.getEncoded();
            // Store the key securely
            // Should use a public / private keypair but limitations of JSS
            // prevents this hence using a PKCS12 store to store the key 
            // generated. This key is stored with an initial password. This 
            // password will be changed for the verifier file when the verifier 
            // is initialized ( the Auditor logs in) add initial key to the 
            // secret store. Write twice to the logger's PKCS12 file as the 
            // initialKey remains the same but the currentKey changes
            writeToSecretStore(currentLoggerKey, loggerFileName, 
                loggerPass, initialKey);
            loggerInitialized = true;
            writeToSecretStore(currentLoggerKey, loggerFileName, 
                loggerPass, currentKey);
            writeToSecretStore(currentLoggerKey, verifierFileName, 
                tempVerifierPass, initialKey);
        } else {
            if (Debug.messageEnabled()) {
                Debug.message(logFileName + " Logger Module is already " + 
                    " initialized");
            }
            currentLoggerKey = readFromSecretStore(loggerFileName, 
                currentKey, loggerPass);
        }
    }
    
    /**
     * Initialize the verifier by using the logger generated PKCS12 file
     * and looking for the appropriate content in that and overwriting with
     * the new password
     * @param oldPassword This was set by the administrator and the Auditor
     *                    wants to overwrite this password.
     * @param newPassword The administrator / auditor's new password
     * @throws Exception if it fails to replace the password 
     */
    public synchronized void initializeVerifier(
        String verFileName, 
        AMPassword oldPassword,
        AMPassword newPassword
    ) throws Exception {
        verifierFileName = verFileName;
        AMPassword oldPass = oldPassword;
        verifierPass = newPassword;
        initializeKeyStoreManager(verifierPass);
        
        if(oldPassword != null)
            verifierInitialized = isInitialized(verFileName, verifierPass);
        else
            verifierInitialized = true;
        
        if(!verifierInitialized){
            currentVerifierKey = readFromSecretStore(verifierFileName, 
                initialKey, oldPass);
            
            // Password is erased from the object, so we need to keep this
            AMPassword newverpass = verifierPass;
            
            // Re-write the verifier file with the new password. Also create the
            // currentKey in the secret Storage
            writeToSecretStore(currentVerifierKey, verifierFileName, 
                verifierPass, initialKey);
            verifierInitialized = true;
            
            writeToSecretStore(currentVerifierKey, verifierFileName, 
                newverpass, currentKey);
        }else{
            currentVerifierKey = readFromSecretStore(verifierFileName, 
                currentKey,verifierPass);
        }
    }
    
    /**
     * ReInitialize the verifier
     * @param verFileName Filename of the verifier
     * @param password administrator / auditor password
     * @throws Exception if it fails to reinitialize verifier
     */
    public synchronized void reinitializeVerifier(
        String verFileName, 
        AMPassword password
    ) throws Exception {
        initializeKeyStoreManager(password);
        verifierInitialized = isInitialized(verFileName, password);
        if(verifierInitialized){
            currentVerifierKey = readFromSecretStore(verifierFileName, 
                initialKey, password);
            
            // Password is erased from the object, so we need to keep this
            AMPassword newverpass = (AMPassword) password.clone();
            writeToSecretStore(currentVerifierKey, verifierFileName, 
                newverpass, currentKey);
            
        }else{
            throw new Exception(logFileName + " Verifier is not initialized");
        }
    }
        
    /**
     * Returns the last generated MAC for the logger
     * @return the last generated MAC for the logger
     */
    public byte[] getLastMAC() {
        return currentMAC;
    }
    
    /**
     * Returns the bytes from the last generated signature for the logger
     * @return the bytes from the last generated signature for the logger
     * @throws Exception if it fails to read the last signature 
     */
    public byte[] getLastSignatureBytes()
    throws Exception {
        return readFromSecretStore(logFileName, currentSignature, loggerPass);
    }
    
    /**
     * Returns the Logger File Name.
     * @return the name of Logger's file name
     */
    public String getLoggerFileName() {
        return logFileName;
    }
    
    /**
     * Returns the Verifier File Name.
     * @return the name of Verifier's file name
     */
    public String getVerifierFileName() {
        return verifierFileName;
    }

    /**
     * Returns the current key from secure storage, generates the MAC and
     * also generates a new key and stores it back in the secure storage. 
     * Does not store the initialKey into the log file but replaces
     * it with the currentKey
     * @param LogEntry   The actual log entry
     * @return MAC for given log entry
     * @throws Exception if it fails to generate the MAC
     */
    public synchronized byte[] generateLogEntryMAC(String LogEntry)
    throws Exception {
        byte[] key = null;
        lastMac = currentMAC;
        key = readFromSecretStore(logFileName, currentKey, loggerPass);
        if((currentLoggerKey != null ) && 
           (equalByteArrays(currentLoggerKey, key) == false)) {
            throw new Exception("Possible Intrusion or " + " Misconfiguration");
        }
        
        currentLoggerKey = key;
        // Generate the MAC
        currentMAC = getDigest(LogEntry, currentLoggerKey);
        
        // Calculate the next key k1 and store it in the currentLoggerKey
        // k1 = SHA1 (k0)
        MessageDigest md = MessageDigest.getInstance("SHA");
        md.update(currentLoggerKey);
        currentLoggerKey = md.digest();
        
        // Write the key to the secret store
        writeToSecretStore(currentLoggerKey, logFileName,
                loggerPass, currentKey);
        return currentMAC;
    }
    
    /**
     * Verifies the current MAC by taking the currentVerifierKey
     * and update the currentVerifierKey
     * @param LogEntry log entry whose mac has to be verified
     * @param mac mac with which to be verified
     * @return true if mac for log entry is valid
     * @throws Exception if it fails to verify mac value for log entry
     */
    public boolean verifyMAC(String LogEntry, byte[] mac)
        throws Exception {
        boolean macValid = false;
        
        try{
            byte[] verifierKey = readFromSecretStore(verifierFileName, 
                currentKey, verifierPass);
            if((currentVerifierKey!= null ) && 
             !(new String(currentVerifierKey).equals(new String(verifierKey)))){
                throw new Exception(verifierFileName + 
                    " Possible Intrusion or " + " Misconfiguration");
            }
            
            currentVerifierKey = verifierKey;
            
            byte[] digest = getDigest(LogEntry, currentVerifierKey);
            
            if(equalByteArrays(mac,digest)){
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(currentVerifierKey);
                currentVerifierKey = md.digest();
                // Write the key to the secret store
                writeToSecretStore(currentVerifierKey, verifierFileName, 
                    verifierPass, currentKey);
                macValid = true;
            } 
        }catch (Exception e){
            Debug.error("SecureLogHelper.verifyMAC() : " +
                " Exception : ", e);
        }
        
        return macValid;
    }
    
    /**
     * Set the Logger's last line
     * @param islastLine true if current is last line of logger
     */
    public void setLastLineforLogger(boolean islastLine) {
        LoggerLastLine = islastLine;
        if(lastLoggerKey == null) {
            lastLoggerKey = new byte[currentLoggerKey.length];
        }
        System.arraycopy(currentLoggerKey, 0, lastLoggerKey, 0, 
            currentLoggerKey.length);
    }
    
    /**
     * Set the Verfier's last line
     * @param islastLine true if current is last line of logger
     */
    public void setLastLineforVerifier(boolean islastLine) {
        VerifierLastLine = islastLine;
    }
    
    /**
     * Compare the logger and the verifier keys
     * @return false if LoggerLastLine and VerifierLastLine are equal
     */
    public boolean isIntrusionTrue() {
        boolean intrusion = false;
        if(LoggerLastLine && VerifierLastLine){
            intrusion = !(new String(currentVerifierKey).
                equals(new String(lastLoggerKey)));
        }
        
        return intrusion;
    }
    
    /**
     * Converts a given byte block to comprehensible hexadecimal String
     * @param  block The data to be converted
     * @return hex string of given byte block
     */
    public String toHexString(byte[] block) {
        StringBuffer buf = new StringBuffer();
        
        int len = block.length;
        for (int i = 0; i < len; i++){
            bytetohex((byte)(block[i]+ 128), buf);
            if (i < len-1){
                buf.append(":");
            }
        }
        return buf.toString();
    }
    
    /**
     * Converts a given byte to hexChar
     * @param  b : The byte to be converted
     * @param  buf : Converted data gets added here
     */
    public void bytetohex(byte b, StringBuffer buf) {
        char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
        '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        int high = ((b & 0xf0) >> 4);
        int low = (b & 0x0f);
        buf.append(hexChars[high]);
        buf.append(hexChars[low]);
    }
    
    /**
     * Converts a given hex String separated by colons to a byte array.
     * @param str string to be converted.
     * @return byte array of given string
     */
    public static byte[] toByteArray(String str) {
        int len = str.length();
        //      String[] newStrArray = str.split(":");
        StringTokenizer st = new StringTokenizer(str,":");
        String[] newStrArray = new String[st.countTokens()];
        int j = 0;
        while(st.hasMoreTokens()){
            newStrArray[j]= st.nextToken();
            j++;
        }
        
        byte[] ret = new byte[newStrArray.length];
        int tmp;
        for(int i = 0; i < newStrArray.length; i++) {
            tmp = Integer.parseInt(newStrArray[i], 16);
            ret[i] = (byte)(tmp - 128);
        }
        return ret;
    }
    
    /**
     * Finds out whether the bytes[] are equal
     * @param  buf1 : First byte[] to be checked
     * @param  buf2 : Second byte[] to be checked
     * @return true if they are same
     * @throws IOException if they can not be comapred
     */
    public boolean equalByteArrays( byte[] buf1, byte[] buf2)
    throws IOException{
        ByteArrayInputStream b1 = new ByteArrayInputStream(buf1);
        ByteArrayInputStream b2 = new ByteArrayInputStream(buf2);
        for(int i=0; i < b1.available();){
            int byte1 = b1.read();
            int byte2 = b2.read();
            if(byte1 != byte2){
                b1.close();
                b2.close();
                return false;
            }
        }
        b1.close();
        b2.close();
        return true;
    }
    
    /**
     * Returns a digest based on the given LogEntry and the given KeyMaterial
     * @param LogEntry : The data whose digest is to be generated
     * @param keyMaterial : The key related data
     * @return generated digest value
     * @throws Exception if it fails to generate digest value for given 
     *                   LogEntry and the given KeyMaterial
     */
    public byte[] getDigest(String LogEntry, byte[] keyMaterial)
    throws Exception{
        // Generate the MAC for the LogEntry
        // SHA1 (k0, SHA1 (Timestamp0, Data0, k0)
        MessageDigest md1 = MessageDigest.getInstance("SHA");
        MessageDigest md2 = MessageDigest.getInstance("SHA");
        md2.update(LogEntry.getBytes());
        md2.update(keyMaterial);
        md1.update(keyMaterial);
        keyMaterial = null;
        md1.update(md2.digest());
        return md1.digest();
    }
    
    /**
     * Sets Name of Logger's Key name
     * @param  name Name for Logger's Key name
     */
    public static void setLoggerKeyName(String name) {
        loggerKey = name;
    }

    /**
     * Returns Name of Logger's Key name
     * @return name Name for Logger's Key name
     */
    public static String getLoggerKeyName() {
        return loggerKey;
    }

    /**
     * Returns true if logger is already initialized
     * @param  filename logger filename to be checked
     * @param  password for logger file
     * @return true if logger is already initialized
     */
    boolean isInitialized(String filename, AMPassword password){
        boolean isInitialized = false;
        try{
            byte[] k0 = null;
            FileInputStream infile=null;
            infile = new FileInputStream(filename);
            k0 = readFromSecretStore( filename, currentKey, password);
            if(k0 != null) {
                isInitialized = true;
            }
            else {
                isInitialized = false;
            }
            infile.close();
        }catch(Exception e) {
            //If the exception occurs before infile.close, isInitialized will be
            //false. If it occurs at infile.close, the flag will already have
            //been set by that time. Hence just return isInitialized as it is in
            //the end.
            if(Debug.messageEnabled()) {
                Debug.message("SecureLogHelper.isInitialized() : " + 
                    e.getMessage() +
                    " : returning false");
            }
        }
        
        return isInitialized;
    }
}

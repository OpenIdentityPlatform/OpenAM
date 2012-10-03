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
 * $Id: LogVerifier.java,v 1.7 2008/06/25 05:43:38 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.log.secure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Vector;
import java.util.logging.LogManager;

import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.SystemTimer;
import com.sun.identity.log.LogConstants;
import com.sun.identity.log.LogManagerUtil;
import com.sun.identity.log.LogReader;
import com.sun.identity.log.Logger;
import com.sun.identity.log.handlers.SecureFileHandler;
import com.sun.identity.log.spi.Debug;
import com.sun.identity.log.spi.Token;
import com.sun.identity.log.spi.VerifierAction;
import com.sun.identity.security.keystore.AMPassword;

/**
 * This class is verifying signature that is generated with the MAC value 
 * for each log entry.
 */
public class LogVerifier{
    
    private static String PREFIX = "_secure.";
    private String curMAC = null;
    private VerifyTask verifier;
    private String prevSignature = null;
    private boolean verified = true;
    private SecureLogHelper helper;
    private AMPassword verPassword;
    private String name;
    private LogManager manager = LogManagerUtil.getLogManager();
    private boolean verificationOn = false;
    
    /**
     * Constructs <code>LogVerifier</code> object
     * @param log logger name associated with this verifier
     * @param verPass the password for verifier secure store
     */
    public LogVerifier(String log, AMPassword verPass) {
        name = log;
        verPassword = verPass;
    }
    
    /**
     * Return verification flag
     * @return verification flag
     */
    public boolean getVerificationFlag() {
        return verificationOn;
    }
    
    /**
     *  Inner class which extends the abstract GeneralTaskRunnable class and
     *  impelements the run method which is run periodically which does the
     *  actual verification.
     */
    class VerifyTask extends GeneralTaskRunnable {
        
        private long runPeriod;
        
        public VerifyTask(long runPeriod) {
            this.runPeriod = runPeriod;
        }
        
        /**
         *  Method that runs at an interval as specified in the timer object.
         */
        public void run(){
            try{
                verify();
            } catch(Exception e) {
                Debug.error(name+":Error running verifier thread", e);
            }
            verificationOn = false;
        }
        
        /**
         *  Methods that need to be implemented from GeneralTaskRunnable.
         */
        
        public boolean isEmpty() {
            return true;
        }
        
        public boolean addElement(Object obj) {
            return false;
        }
        
        public boolean removeElement(Object obj) {
            return false;
        }
        
        public long getRunPeriod() {
            return runPeriod;
        }
        
    }
    
    /**
     *  Method that starts the log Verifier thread
     */
    public void startLogVerifier(){
        String period = 
            manager.getProperty(LogConstants.LOGVERIFY_PERIODINSECONDS);
        long interval;
        if((period != null) || (period.length() != 0)) {
            interval = Long.parseLong(period);
        } else {
            interval = LogConstants.LOGVERIFY_PERIODINSECONDS_DEFAULT;
        }
        interval *= 1000;
        if(verifier == null){
            verifier = new VerifyTask(interval);
            SystemTimer.getTimer().schedule(verifier, new Date(((
                System.currentTimeMillis() + interval) / 1000) * 1000));
            if (Debug.messageEnabled()) {
                Debug.message(name+":Verifier Thread Started");
            }
        }
    }
    
    /**
     *  Method to stop the log verifier thread if it is running
     */
    public void stopLogVerifier() {
        if (verifier != null) {
            verifier.cancel();
            verifier = null;
        }
    }
    
    /**
     *  Verifies the passed LogRecord to check for tampering.
     *
     *  @param  record String array of the elements of the record.
     *  @param  macPos position of the mac header in the array.
     *  @return a boolean value of the result of the verification
     */
    private boolean verifyLogRecord(String[] record, int macPos)
    throws Exception {
        // Creating the data part for verification
        StringBuilder data = new StringBuilder();
        for(int m = 0; m < record.length-2; m++) {
            data.append(record[m]);
        }
        curMAC = record[macPos];
        verified = 
            helper.verifyMAC(data.toString(), helper.toByteArray(curMAC));
        return verified;
    }
    
    /**
     * Verifies the signature entry in the log file for tampering.
     *
     * @param  String array of the elements of the record.
     * @param  position of the signature field value in the array
     * @return a boolean value of the result of the verification
     */
    private boolean verifySignature(String[] record, int signPos, int recPos)
    throws Exception {
        String curSign = record[signPos];
        
        // Regenerate the MAC that was signed.
        byte[] prevMAC = helper.toByteArray(curMAC);
        byte[] newMAC ;
        if((prevSignature == null) || prevSignature.equals("")) {
            newMAC = new byte[prevMAC.length];
            System.arraycopy(prevMAC, 0, newMAC, 0, prevMAC.length);
        }else{
            newMAC = new byte[prevMAC.length + 
                              helper.toByteArray(prevSignature).length];
            System.arraycopy(prevMAC, 0, newMAC, 0, prevMAC.length);
            System.arraycopy(helper.toByteArray(prevSignature), 0, newMAC,
            prevMAC.length, helper.toByteArray(prevSignature).length);
        }
        // If this is the last record in the file then dont update the 
        // prevSignature as the first record in the next file is also 
        // the same signature.
        if(recPos != 0) {
            prevSignature = curSign;
        }
        verified = helper.verifySignature(helper.toByteArray(curSign), newMAC);
        return verified;
    }
    
    /**
     * Checks each record in the list of log files for tampering.
     * @return a boolean value as a result of the verification
     * @throws Exception if it fails to verify any mac value in the log entry.
     */
    public boolean verify()
    throws Exception{
        Logger logger = (com.sun.identity.log.Logger)Logger.getLogger(name);
        ArrayList fileList = new ArrayList();
        String[][] tmpResult = new String[1][1];
        Object token = new Object();
        synchronized(logger) {
            verificationOn = true;
            long start = System.currentTimeMillis();
            helper = SecureFileHandler.getSecureLogHelper(name);
            fileList = SecureFileHandler.getCurrentFileList(name);
            if (fileList == null) {
                Debug.error("No fileList found in handler.");
                return VerifierAction.doVerifierAction(name, verified);
            }
            token = 
               Token.createToken("AUDITOR", new String(verPassword.getChars()));
            tmpResult = 
               LogReader.read((String)fileList.get(fileList.size() - 1), token);
        }
        
        for(int i = 0; i < fileList.size() - 1; i++) {
            String[][] result = new String[1][1];
            try{
                result = LogReader.read((String)fileList.get(i),token);
            }catch(Exception e){
                Debug.error("Error in reading File : "+fileList.get(i));
            }
            // Check if the result of read is null or empty string array.
            if(result != null && result.length != 0){
                Vector header = new Vector(result[0].length);
                // Extracting the field names as header from the first line 
                // of the returned string array.

                header.addAll(Arrays.asList(result[0]));
                
                int signPos = -1, macPos = -1;
                String signFldName, macFldName;
                signFldName = LogConstants.SIGNATURE_FIELDNAME;
                macFldName = LogConstants.MAC_FIELDNAME;
                
                for(int l = 0; l < header.size(); l++){
                    if((((String)header.get(l))).equalsIgnoreCase(signFldName)) 
                    {
                        signPos = l;
                        break;
                    }
                }
                for(int l = 0; l < header.size(); l++){
                    if((((String)header.get(l))).equalsIgnoreCase(macFldName)) {
                        macPos = l;
                        break;
                    }
                }
                if ((signPos == -1) || (macPos == -1)) {
                    Debug.error("Could not locate mac and sign header");
                    return VerifierAction.doVerifierAction(name, verified);
                }
                
                // Now check each record to see if it is a signature record or 
                // a log record.
                for(int k = 1; k < result.length; k++) {
                    if (Debug.messageEnabled()) {
                        Debug.message(name + ":Start checking records " + 
                            result.length + ":" + fileList.get(i));
                    }
                    if(result[k][signPos].equals("-")) {
                        verified = verifyLogRecord(result[k], macPos);
                        if(!verified) {
                            Debug.error("Log Record Verification " +
                                "Failed in file:" +
                                (String)fileList.get(i) + " at record no. "+ k);
                            break;
                        }
                        if (Debug.messageEnabled()) {
                            Debug.message(name+
                                ":Log Record Verification Succeeded in file:"+
                                (String)fileList.get(i) + "at record no."+ k);
                        }
                    } else {
                        /*
                         * To check if this is the last signature in the file 
                         * an additional parameter has to be passed to the 
                         * verifySignature since the signature is the same
                         * as the first signature in the next file. This is 
                         * to ensure that prevSignature is not updated with 
                         * the last signature in the file.
                         * Bcos the checking of the last signature in the file 
                         * will be the same for the first signature for the 
                         * next file.
                         */
                        int lastRecInFile = 0;
                        lastRecInFile = (result.length - 1)  - k;
                        verified = 
                            verifySignature(result[k], signPos, lastRecInFile);
                        if(!verified) {
                            Debug.error("Log Signature Verification " +
                                "Failed in file:" +
                                (String)fileList.get(i) + " at record no. "+ k);
                            break;
                        }
                            
                        if (Debug.messageEnabled()) {
                            Debug.message("Log Signature Verification " +
                                "Succeeded in file:" +
                                (String)fileList.get(i) + "at record no."+ k);
                        }
                    }
                } // end of loop k . i.e. verification check for current file 
                  // is over
            } else {
                if (Debug.messageEnabled()) {
                    Debug.message("LogVerifier::verify::Empty return " +
                        "from read of " + (String)fileList.get(i) +
                        ":" + fileList.get(i));
                }
                verified = false;
                break;
            }
            if (!verified) {
                break;
            }
        } // end of loop i i.e. current filelist verification is over.
        
        // This is for the current file that was read at the start.
        // This is done bcos in the time that the verifier reaches a point 
        // where  it starts verifying this file it might have already been 
        // timestamped, bcos of the logging that is going on in parallel.
        
        if(tmpResult != null && tmpResult.length != 0){
            Vector header = new Vector(tmpResult[0].length);
            // Extracting the field names as header from the first line of the
            // returned string array.
            header.addAll(Arrays.asList(tmpResult[0]));
            
            int signPos = -1, macPos = -1;
            String signFldName, macFldName;
            signFldName = LogConstants.SIGNATURE_FIELDNAME;
            macFldName = LogConstants.MAC_FIELDNAME;
            
            for(int l = 0; l < header.size(); l++){
                if((((String)header.get(l))).equalsIgnoreCase(signFldName)) {
                    signPos = l;
                    break;
                }
            }
            for(int l = 0; l < header.size(); l++){
                if((((String)header.get(l))).equalsIgnoreCase(macFldName)) {
                    macPos = l;
                    break;
                }
            }
            if ((signPos == -1) || (macPos == -1)) {
                Debug.error("Could not locate mac and sign header");
                return VerifierAction.doVerifierAction(name, verified);
            }
            
            // Now check each record to see if it is a signature record 
            // or a log record.
            for(int k = 1; k < tmpResult.length; k++) {
                if (Debug.messageEnabled()) {
                    Debug.message(name+":Start checking records " + 
                        tmpResult.length+":"+fileList.get(fileList.size() - 1));
                }
                if(tmpResult[k][signPos].equals("-")) {
                    verified = verifyLogRecord(tmpResult[k], macPos);
                    if(!verified) {
                        Debug.error("Log Record Verification Failed in file:"+
                        (String)fileList.get(fileList.size() - 1) + 
                        " at record no. "+ k);
                        break;
                    }
                    
                    if (Debug.messageEnabled()) {
                        Debug.message(name+":Log Record Verification " +
                            "Succeeded in file:" +
                            (String)fileList.get(fileList.size() - 1) + 
                            "at record no."+ k);
                    }
                } else {
                    // To check if this is the last signature in the file an 
                    // additional parameter has to be passed to the 
                    // verifySignature since the signature is the same
                    // as the first signature in the next file. 
                    // This is to ensure that prevSignature is not updated 
                    // with the last signature in the file.
                    // Bcos the checking of the last signature in the file 
                    // will be the same for the first signature for the 
                    // next file.
                    int lastRecInFile = 0;
                    lastRecInFile = (tmpResult.length - 1)  - k;
                    verified = verifySignature(tmpResult[k], 
                        signPos, lastRecInFile);
                    if(!verified) {
                        Debug.error("Log Signature Verification Failed " +
                            "in file:" +
                            (String)fileList.get(fileList.size() - 1) + 
                            " at record no. " + k);
                        break;
                    }
                    if (Debug.messageEnabled()) {
                        Debug.message("Log Signature Verification Succeeded" +
                            " in file:"+
                            (String)fileList.get(fileList.size() - 1) +
                            "at record no."+ k);
                    }
                }
            } // end of loop k. i.e. verification check for current file is over
        } else {
            if (Debug.messageEnabled()) {
                Debug.message("LogVerifier::verify::Empty return from read of "
                            + (String)fileList.get(fileList.size() - 1) + ":" + 
                            fileList.get(fileList.size() - 1) );
            }
            verified = false;
        }
        
        prevSignature = null;
        curMAC = null;
        String path = manager.getProperty(LogConstants.LOG_LOCATION);
        if(!path.endsWith("/"))
            path += "/";
        String verKeyStoreName = path + PREFIX + "ver." + name;
        helper.setLastLineforVerifier(true);
        boolean intrusion = helper.isIntrusionTrue();
        if(intrusion) {
            Debug.error(name+" Last Line check in Verifier failed." +
            " Possible intrusion detected");
            verified = false;
        }
        helper.setLastLineforVerifier(false);
        helper.reinitializeVerifier(verKeyStoreName , verPassword);
        if (Debug.messageEnabled()) {
            Debug.message(name + ":Done Verifying");
        }
        return VerifierAction.doVerifierAction(name, verified);
    }
}

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
 * $Id: ISArchiveVerify.java,v 1.11 2008/10/27 18:14:12 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.log.cli;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.Vector;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.internal.AuthPrincipal;
import com.sun.identity.log.LogConstants;
import com.sun.identity.log.LogManager;
import com.sun.identity.log.LogManagerUtil;
import com.sun.identity.log.LogReader;
import com.sun.identity.log.handlers.SecureFileHandler;
import com.sun.identity.log.secure.SecureLogHelper;
import com.sun.identity.log.secure.VerifierList;
import com.sun.identity.log.spi.VerifierAction;
import com.sun.identity.security.keystore.AMPassword;
import com.sun.identity.setup.Bootstrap;
import com.sun.identity.setup.ConfiguratorException;
import com.sun.identity.tools.bundles.VersionCheck;
import java.util.Locale;

/**
 * This Archive verify class provides the way for verifying LogRecords that 
 * has logged with mac and signature. It will detect any intrusion made for
 * secure log file.
 */
public class ISArchiveVerify{
    private static String PREFIX = "_secure.";
    private String curMAC = null;
    private String prevSignature = null;
    private boolean verified = true;
    private SecureLogHelper helper;
    private AMPassword verPassword;
    
    private static ResourceBundle bundle =
	com.sun.identity.shared.locale.Locale.getInstallResourceBundle(
	    "amLogging");
    static final int INVALID = 0;
    static final int LOGNAME = 1;
    static final int PATH = 2;
    static final int USERNAME = 3;
    static final int PASSWORD = 4;
    
    static Map OPTIONS = new HashMap();
    
    static {
        OPTIONS.put("-l", new Integer(LOGNAME));
        OPTIONS.put("-p", new Integer(PATH));
        OPTIONS.put("-u", new Integer(USERNAME));
        OPTIONS.put("-w", new Integer(PASSWORD));
    }
    
    static int getToken(String arg) {
        try {
            return(((Integer)OPTIONS.get(arg)).intValue());
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Main method for the class. It drives verify procedure by invoking 
     * runCommand method.
     * @param args
     * @throws Exception if it fails to processing verification.
     */
    public static void main(String[] args) throws Exception {
        if ((args.length == 0) || (args.length != 8)) {
            System.err.println(bundle.getString("amverifyarchive-usage"));
            System.exit(1);
        }

        runCommand(args);
    }

    static private void runCommand(String[] argv) throws Exception {
        try {
            Bootstrap.load();
        } catch (ConfiguratorException ex) {
            System.err.println(ex.getL10NMessage(Locale.getDefault()));
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Cannot bootstrap the system" + e.getMessage());
            System.exit(1);
        }

        if (VersionCheck.isVersionValid() == 1) {
            System.exit(1);
        }

        int ln = 0;
        int path = 0;
        int uname = 0;
        int passwd= 0;
        int i = 0;

        try {
            while (i < argv.length) {
                int opt = getToken(argv[i]);

                switch(opt) {
                case LOGNAME:
                    i++;
                    ln = i;
                    break;
                case PATH:
                    i++;
                    path = i;
                    break;
                case USERNAME:
                    i++;
                    uname = i;
                    break;
                case PASSWORD:
                    i++;
                    passwd = i;
                    break;
                default:
                    throw new Exception();//bundle.getString("invalidOpt"));
                }
                i++;
            }
        } catch (Exception e) {
            if (e.getMessage() != null) {
                System.err.println(e.getMessage());
            }

            e.printStackTrace();
            System.err.println(bundle.getString("amverifyarchive-usage"));
            System.exit(1);
        }

        try {
            ISArchiveVerify iav = new ISArchiveVerify();
            boolean verified = iav.verifyArchive(
                argv[ln], argv[path], argv[uname], argv[passwd]);

            if (verified) {
                System.out.println(
                    bundle.getString("verificationOfLogArchiveFor") + " " + 
                        argv[ln] + " " +
                        bundle.getString("archiveVerificationPassed"));
            } else {
                System.out.println(
                    bundle.getString("verificationOfLogArchiveFor") + " " +
                        argv[ln] + " " +
                        bundle.getString("archiveVerificationFailed"));
            }
        } catch(Exception e) {
            if (e.getMessage() != null) {
                System.err.println(e.getMessage());
            }
            e.printStackTrace();
            System.exit(1);
        }

        System.exit(0);
    }
    
    /**
     * Verifies the passed LogRecord to check for tampering.
     *
     * @param  String array of the elements of the record.
     * @param  position of the mac header in the array.
     * @return a boolean value of the result of the verification
     * @throws Exception if it fails to verify the record.
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
     * @throws Exception if it fails to verify the signature.
     */
    private boolean verifySignature(String[] record, int signPos, int recPos)
        throws Exception {
        String curSign = record[signPos];
        
        //
        //  if curMAC is null, there's apparently a missing
        //  _secure.<file>.access.<date> (or .error.date)
        //
        if (curMAC == null) {
            return false;
        }

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
        // If this is the last record in the file then dont update 
        // the prevSignature as the first record in the next file is also 
        // the same signature.
        if(recPos != 0) {
            prevSignature = curSign;
        }
        verified = helper.verifySignature(helper.toByteArray(curSign), newMAC);
        return verified;
    }
    
    /**
     *  Verifies the complete archive including the current set and all 
     *  the previous sets for the specified log.
     *  @param logName the name of the log for which the complete Archive is 
     *  to be verified.
     *  @param path Fully quallified path name for log file
     *  @param uname userv name for logger user
     *  @param passwd Password for logger user
     *  @return value of the status of verification.
     *  @throws Exception if it fails to verify the archive.
     */
    public boolean verifyArchive(
        String logName,
        String path, 
        String uname,
        String passwd
    ) throws Exception{
        String log = logName;
        LogManager lm = (LogManager)LogManagerUtil.getLogManager();
        lm.readConfiguration();
        verPassword = new AMPassword(passwd.toCharArray());

        SSOToken ssoToken = null;
        SSOTokenManager ssoMngr = null;
        try {
            ssoMngr = SSOTokenManager.getInstance();
            ssoToken = 
                    ssoMngr.createSSOToken(new AuthPrincipal(uname), passwd);
        } catch (SSOException ssoe) {
            System.out.println(bundle.getString("archiveVerification")
                + "SSOException: " + ssoe.getMessage());
            return false;
        } catch (UnsupportedOperationException uoe) {
            System.out.println(bundle.getString("archiveVerification")
                + "UnsupportedOperationException: " + uoe.getMessage());
            return false;
        }

        // This function will be used to verify all the files in the current and
        // previous sets for the logname and types.
        VerifierList vl = new VerifierList();
        if(!path.endsWith("/")) {
            path += "/";
        }
        TreeMap tm = vl.getKeysAndFiles(new File(path), logName);
        if(tm.size() == 0){
            System.out.println(bundle.getString("archiveVerification") + 
                bundle.getString("noFilesToVerify") + ", size == 0");
            return true;
        }
        
        // To get the list of all keyfiles for that particular logname.type
        Object[] keyFiles = (tm.keySet()).toArray();
        String verFile = new String();

        if (keyFiles.length == 1) {
            System.out.println(bundle.getString("archiveVerification") + 
                bundle.getString("noFilesToVerify") +
                ", keyFiles.length == 1");
        }
        for(int i = 1; i < keyFiles.length; i++) {
            helper = SecureFileHandler.getSecureLogHelperInst();

            // This is the set of files for that particular keystore.
            Vector logFiles = (Vector)tm.get(keyFiles[i]);
            // Iterate through the list and start verification from 
            // the first file.
            String tmpName = ((String)keyFiles[i]).
                              substring(((String)keyFiles[i]).indexOf(".") + 1);
            verFile = tmpName.substring(tmpName.indexOf("."));
            verFile = PREFIX + "ver" + verFile;
            // Initialize the SecureLogHelper object for the current keystores.
            helper.initializeVerifier(path + verFile, verPassword, verPassword);
            helper.reinitializeVerifier(path + verFile, verPassword);
            // Start verifying the Files associated with the current keystore
            curMAC = null;
            prevSignature = null;
            for(int j = 0; j < logFiles.size(); j++) {
                // flag to indicate that last record in the file is being 
                // verified. This record is the same for the first record 
                // of the next file.
                System.out.println(bundle.getString("fileBeingVerified") 
                    + (String)logFiles.elementAt(j));
                int lastRecInFile = 0;
                // Read the logRecords in the File.
                String[][] result = new String[1][1];
                try{
                    result = 
                        LogReader.read((String)logFiles.elementAt(j), ssoToken);
                }catch(Exception e){
                    e.printStackTrace();
                }
                // Check if the result of a read operation is a null or 
                // empty string.
                if(result != null || result.length != 0) {
                    Vector header = new Vector(result[0].length);
                    // Extracting the field names as header from the first 
                    // line of the returned string array.
                    header.addAll(Arrays.asList(result[0]));
                    int signPos = -1, macPos = -1;
                    String signFldName, macFldName;
                    signFldName = LogConstants.SIGNATURE_FIELDNAME;
                    macFldName = LogConstants.MAC_FIELDNAME;
                    for(int l = 0; l < header.size(); l++){
                        if((((String)header.get(l))).
                                equalsIgnoreCase(signFldName)) {
                            signPos = l;
                            break;
                        }
                    } // end of loop l
                    for(int l = 0; l < header.size(); l++){
                        if((((String)header.get(l))).
                                equalsIgnoreCase(macFldName)) {
                            macPos = l;
                            break;
                        }
                    }// end of loop l
                    if ((signPos == -1) || (macPos == -1)) {
                        return VerifierAction.doVerifierAction(log, verified);
                    }
                    // Now check each record to see if it is a signature record 
                    // or a log record.
                    for(int k = 1; k < result.length ; k++) {
			// add 2 for MAC and Signature fields
			if (result[k].length < (LogConstants.MAX_FIELDS+2)) {
			    System.err.println(
                                bundle.getString("recordVerificationFailed")
				+ (String)logFiles.elementAt(j)
				+ "\n\t #fields in record #" + (k-1) + " ("
				+ result[k].length + ") < 14\n");
			    verified = false;
			    break;
			}
                        if(result[k][signPos].equals("-")) {
                            verified = verifyLogRecord(result[k], macPos);
                            if(!verified){
                                System.err.println(
                                    bundle.getString("recordVerificationFailed")
                                    + (String)logFiles.elementAt(j) + " " +
                                    bundle.getString("atRecordNumber") + k);
                                break;
                            }
                            System.out.println(
                                bundle.getString("recordVerificationPassed") +
                                (String)logFiles.elementAt(j) + " " +
                                bundle.getString("atRecordNumber") + k);
                        } else {
                            // To check if this is the last signature in the 
                            // file an additional parameter has to be passed 
                            // to the verifySignature since the signature is 
                            // the same as the first signature in the next file.
                            // This is to ensure that prevSignature is not 
                            // updated with the last signature in the file.
                            // Bcos the checking of the last signature in the 
                            // file will be the same for the first signature 
                            // for the next file.
                            lastRecInFile = (result.length - 1)  - k;
                            verified = 
                                verifySignature(result[k], signPos,
                                    lastRecInFile);
                            if(!verified){
                                System.err.println(
                                 bundle.getString("signatureVerificationFailed")
                                    + (String)logFiles.elementAt(j) +
                                    bundle.getString("atRecordNumber") + k);
                                break;
                            }
                            System.out.println(
                                bundle.getString("signatureVerificationPassed")
                                + (String)logFiles.elementAt(j) + 
                                bundle.getString("atRecordNumber") + k);
			}
                    }// end of loop k i.e. end of records for this logFile.
                }else{
                    System.err.println(
                        bundle.getString("archiveVerification") + 
                        bundle.getString("emptyReturn") +
                        (String)logFiles.elementAt(j));
                }
                if(!verified){
                    return verified;
                }
            }// end of loop j i.e. end of Files for the current keystore.
            
            helper.reinitializeVerifier(path + verFile, verPassword);
        }// end of loop i
        return verified;
    }// end of verifyArchive
    
}

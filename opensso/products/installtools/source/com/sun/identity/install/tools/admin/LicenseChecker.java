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
 * $Id: LicenseChecker.java,v 1.3 2008/06/25 05:51:17 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.admin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import com.sun.identity.install.tools.util.Console;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.ConfigUtil;

public class LicenseChecker {

    public boolean checkLicenseAcceptance() throws Exception {
        boolean accepted = isLicenseAcceptedByUser();

        if (!accepted) {
            Debug.log("License not yet accepted.");
            Console.println();

            LocalizedMessage licenseHeader = LocalizedMessage
                    .get(MSG_LICENSE_HEADER);
            Console.println(licenseHeader);
            String result = Console.pause();
            if (result != null && !result.equalsIgnoreCase("n")) {
                displayLicense();
            }
            Console.println();
            LocalizedMessage licensePrompt = LocalizedMessage
                    .get(MSG_LICENSE_PROMPT);
            Console.print(licensePrompt, STR_NO);
            boolean done = false;
            while (!done) {
                String response = Console.readLine();
                if (response == null || response.trim().length() == 0
                        || response.trim().equalsIgnoreCase(STR_NO)) {
                    Debug.log("License agreement declined by user.");
                    done = true;
                } else if (response.trim().equalsIgnoreCase(STR_YES)) {
                    Debug.log("License agreement accepted by user.");
                    accepted = true;
                    done = true;
                } else {
                    LocalizedMessage errorInvalidSelection = LocalizedMessage
                            .get(MSG_ERROR_INVALID_SELECTION);
                    Console.println();
                    Console.println(errorInvalidSelection);
                    Console.println();

                    Console.print(licensePrompt, STR_NO);
                }
            }
            if (accepted) {
                makeLicenceLogEntry();
            }
        } else {
            Debug.log("License already accepted by the user");
        }

        return accepted;
    }

    public LicenseChecker() throws Exception {
        Debug.log("Starting LicenseChecker");
        if (!getLicenseFile().exists()) 
        {
            Debug.log("License file not found: " + getLicenseFile());

            LocalizedMessage noLicenseFileError = LocalizedMessage
                    .get(MSG_ERROR_NO_LICNESE_FILE);
            Console.println();
            Console.println(noLicenseFileError);
            Console.println();

            throw new Exception("Failed to located the license file: "
                    + getLicenseFile());
        } else {
            Debug.log("License files found");
        }

        String userName = System.getProperty(STR_PROPERTY_USERNAME);
        Debug.log("User Name: " + userName);
        if (userName == null || userName.trim().length() == 0) {
            throw new Exception("Failed to identify username");
        }
        setUserName(userName);
        setLicenseAcceptedByUser(getLicenseStateForUser());
    }

    private void makeLicenceLogEntry() {
        Debug.log("Attempting to make a license log entry");
        File licenseLogFile = getLicenseLogFile();
        FileInputStream finStream = null;
        FileOutputStream foutStream = null;
        Properties props = new Properties();
        try {
            if (!licenseLogFile.exists()) {
                licenseLogFile.createNewFile();
                Debug.log("Created new license log file");
            }
            finStream = new FileInputStream(licenseLogFile);
            props.load(finStream);
            finStream.close();

            SimpleDateFormat sdf = new SimpleDateFormat(STR_DATEFORMAT);
            String dateText = sdf.format(new Date());

            props.put(getUserName(), dateText);
            foutStream = new FileOutputStream(licenseLogFile);
            MessageFormat mf = new MessageFormat(STR_LICENSE_LOG_HEADER);
            SimpleDateFormat sdfHeader = new SimpleDateFormat(
                    STR_DATEFORMAT_HEADER);

            props.store(foutStream, mf.format(new Object[] { sdfHeader
                    .format(new Date()) }));

            Debug.log("Added user entry to license log file");
        } catch (Exception ex) {
            Debug.log("Failed to create license log entry", ex);
        } finally {
            if (finStream != null) {
                try {
                    finStream.close();
                } catch (Exception ex2) {
                    Debug.log("Failed to close file input stream", ex2);
                }
            }
            if (foutStream != null) {
                try {
                    foutStream.close();
                } catch (Exception ex3) {
                    Debug.log("Faled to close file output stream", ex3);
                }
            }
        }
    }

    private boolean getLicenseStateForUser() {
        boolean result = false;
        Debug.log("Checking for previous license acceptance");
        File licenseLogFile = getLicenseLogFile();
        if (licenseLogFile.exists()) {
            Debug.log("Licence log file exists: " + licenseLogFile);
            String userName = getUserName();
            Debug.log("Checking entry for user: " + userName);

            Properties props = new Properties();
            FileInputStream finStream = null;
            FileOutputStream foutStream = null;
            try {
                finStream = new FileInputStream(licenseLogFile);
                props.load(finStream);
                finStream.close();
                String userAcceptanceDate = props.getProperty(userName);
                Debug.log("User acceptence entry: " + userAcceptanceDate);
                if (userAcceptanceDate == null
                        || userAcceptanceDate.trim().length() == 0) {
                    Debug.log("This user has not accepted the license yet");
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat(
                            STR_DATEFORMAT);
                    Date userDate = sdf.parse(userAcceptanceDate);
                    if (userDate != null) {
                        Date now = new Date();
                        if (now.after(userDate)) {
                            Debug.log("License check is valid: " + userDate);
                            result = true;
                        } else {
                            foutStream = new FileOutputStream(licenseLogFile);
                            Debug.log("User date is in the future!");
                            Debug.log("User entry will be purged");
                            props.remove(userName);
                            MessageFormat mf = new MessageFormat(
                                    STR_LICENSE_LOG_HEADER);
                            SimpleDateFormat sdfHeader = new SimpleDateFormat(
                                    STR_DATEFORMAT_HEADER);

                            props.store(foutStream, mf.format(new Object[] { 
                                    sdfHeader.format(now) }));
                        }
                    } else {
                        Debug.log("User date could not be calculated");
                    }
                }
            } catch (Exception ex) {
                Debug.log("Exception while loading license log data", ex);
                result = false;
            } finally {
                if (finStream != null) {
                    try {
                        finStream.close();
                    } catch (Exception ex2) {
                        Debug.log("Failed to close file input stream", ex2);
                    }
                }
                if (foutStream != null) {
                    try {
                        foutStream.close();
                    } catch (Exception ex3) {
                        Debug.log("Failed to close file output stream", ex3);
                    }
                }
            }
        } else {
            Debug.log("License log file not found");
        }

        Debug.log("User License Acceptance = " + result);

        return result;
    }

    private void displayLicense() throws Exception {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(getLicenseFile()));
            String nextLine = null;
            int index = 0;
            String result = null;
            while ((nextLine = reader.readLine()) != null) {
                if (index == 15) {
                    result = Console.pause();
                    if (result != null && result.equalsIgnoreCase("n")) {
                        break;
                    }
                    index = 0;
                }
                Console.printlnRawText(nextLine);
                index++;
            }
        } catch (Exception ex) {
            Debug.log("Failed to display license text", ex);
            ex.fillInStackTrace();
            throw ex;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ex2) {
                    Debug.log("Failed to close license file reader", ex2);
                }
            }
        }
    }

    private File getLicenseFile() {
        return new File(ConfigUtil.getHomePath() + "/" + STR_LICENSE_FILENAME);
    }

    private File getLicenseLogFile() {
        return new File(ConfigUtil.getDataDirPath() + "/" + 
                STR_LICENSE_LOGFILE);
    }

    private void setUserName(String userName) {
        _userName = userName;
    }

    private String getUserName() {
        return _userName;
    }

    private void setLicenseAcceptedByUser(boolean flag) {
        _accepted = flag;
    }

    private boolean isLicenseAcceptedByUser() {
        return _accepted;
    }

    private String _userName;

    private boolean _accepted;

    public static final String STR_LICENSE_FILENAME = "license.txt";

    public static final String STR_LICENSE_LOGFILE = 
        "license.log";

    public static final String STR_LICENSE_LOG_HEADER = 
        "Copyright {0} (c) Sun Microsystems. DO NOT EDIT.";

    public static final String STR_PROPERTY_USERNAME = "user.name";

    public static final String MSG_ERROR_NO_LICNESE_FILE = 
        "error_no_license_file";

    public static final String MSG_LICENSE_HEADER = 
        "license_header";

    public static final String MSG_LICENSE_PROMPT = 
        "license_prompt";

    public static final String MSG_ERROR_INVALID_SELECTION = 
        "invalid_selection";

    public static final String STR_NO = "no";

    public static final String STR_YES = "yes";

    public static final String STR_DATEFORMAT = "MM/dd/yyyy hh:mm:ss z";

    public static final String STR_DATEFORMAT_HEADER = "yyyy";
}

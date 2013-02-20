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
 * $Id: RevisionNumberTest.java,v 1.3 2009/01/26 23:49:38 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/**
 * RevisionNumberTest automates the following test cases:
 * CLI_revision-number01, CLI_revision-number02, CLI_revision-number03
 * CLI_revision-number04, CLI_revision-number05
 */

package com.sun.identity.qatest.cli;

import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.cli.CLIExitCodes;
import com.sun.identity.qatest.common.cli.FederationManagerCLI;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class RevisionNumberTest extends TestCommon implements CLIExitCodes {
    private String locTestName;
    private ResourceBundle rb;
    private String serviceName;
    private String initRevisionNo;
    private String revisionNumberToSet;
    private boolean useVerboseOption;
    private boolean useDebugOption;
    private boolean useLongOptions;
    private String expectedMessage;
    private String getRevExpectedExitCode;
    private String setRevExpectedExitCode;
    private String description;
    private FederationManagerCLI cli;

    public RevisionNumberTest() {
        super("RevisionNumberTest");
    }

    /**
     * This method is intended to provide initial setup.
     * Gets the service name specified in the service-name property in the 
     * RevisionNumberTest.properties.
     */
    @Parameters({"testName"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String testName) 
    throws Exception {
        Object[] params = {testName};
        entering("setup", params);
        try {
            locTestName = testName;
            rb = ResourceBundle.getBundle("cli" + fileseparator + 
                    "RevisionNumberTest"); 
            serviceName = ((String)rb.getString(locTestName + 
                    "-service-name"));
            initRevisionNo = ((String)rb.getString(locTestName + 
                    "-init-revision-number"));
            useVerboseOption = ((String)rb.getString(locTestName + 
                    "-use-verbose-option")).equals("true");
            useDebugOption = ((String)rb.getString(locTestName + 
                    "-use-debug-option")).equals("true");
            useLongOptions = ((String)rb.getString(locTestName + 
                    "-use-long-options")).equals("true");
            log(Level.FINEST, "setup", "service-name: " + serviceName);
            log(Level.FINEST, "setup", "use-verbose-option: " + 
                    useVerboseOption);
            log(Level.FINEST, "setup", "use-debug-option: " + useDebugOption);
            log(Level.FINEST, "setup", "use-long-options: " + useLongOptions);

            Reporter.log("ServiceName: " + serviceName);
            Reporter.log("Initial Revision Number: " + initRevisionNo);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);

            cli = new FederationManagerCLI(useDebugOption, useVerboseOption, 
                    useLongOptions);
            if (cli.createService(serviceName, initRevisionNo) != 
                    SUCCESS_STATUS) {
                log(Level.SEVERE, "setup", 
                        "Service creation failed.");
                assert false;
            }
            cli.logCommand("setup");
            cli.resetArgList();
            exiting("setup");
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        } 
    }

    /**
     * This method is used to execute tests involving "ssoadm get-revision-
     * number" and "ssoadm set-revision-number"
     * using input data from the RevisionNumberTest.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testRevisionNumber() 
    throws Exception {
        entering("testGetRevisionNumber", null);
        boolean stringsFound = false;
        
        try {
            expectedMessage = (String) rb.getString(locTestName + 
                    "-message-to-find");
            getRevExpectedExitCode = (String) rb.getString(locTestName + 
                    "-get-expected-exit-code");
            setRevExpectedExitCode = (String) rb.getString(locTestName + 
                    "-set-expected-exit-code");            
            serviceName = (String) rb.getString(locTestName + 
                    "-service-name");
            revisionNumberToSet = (String) rb.getString(locTestName +
                    "-set-revision-number");
            description = (String) rb.getString(locTestName + "-description");

            log(Level.FINEST, "testGetRevisionNumber", "description: " + 
                    description);
            log(Level.FINEST, "testGetRevisionNumber", "service-name:" + 
                    serviceName);
            log(Level.FINEST, "testGetRevisionNumber", "use-debug-option: " + 
                    useDebugOption);
            log(Level.FINEST, "testGetRevisionNumber", "use-verbose-option: " + 
                    useVerboseOption);
            log(Level.FINEST, "testGetRevisionNumber", "use-long-options: " + 
                    useLongOptions);
            log(Level.FINEST, "testGetRevisionNumber", "message-to-find: " + 
                    expectedMessage);
            log(Level.FINEST, "testGetRevisionNumber", 
                    "get-expected-exit-code: " + getRevExpectedExitCode);
            log(Level.FINEST, "testGetRevisionNumber", 
                    "set-expected-exit-code: " + setRevExpectedExitCode);

            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);
            Reporter.log("ServiceName: " + serviceName);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("GetExpectedExitCode: " + getRevExpectedExitCode);
            Reporter.log("SetExpectedExitCode: " + setRevExpectedExitCode);
            
            /*
             * Gets the revision number of service specified in service-name
             * from RevisionNumberTest.properties
             */
            cli = new FederationManagerCLI(useDebugOption, useVerboseOption, 
                    useLongOptions);            
            int getCommandStatus = cli.getRevisionNumber(serviceName);
            cli.logCommand("testRevisionNumber");
            String revisionNumberBU = getRevisionNumber(cli
                    .getCommand().getOutput());
            log(Level.FINEST, "testRevsionNumber", "Revision Number of " +
                    "service " + serviceName + "is: " + revisionNumberBU);
            if (getRevExpectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                stringsFound = cli.findStringsInOutput(expectedMessage, ";");
                log(Level.FINEST, "testRevisionNumber", 
                        "Output Messages Found: " + stringsFound);
                assert (getCommandStatus == 
                        new Integer(getRevExpectedExitCode).intValue()) && 
                     stringsFound;
            } else {
                if (!getRevExpectedExitCode.equals(
                        new Integer(INVALID_OPTION_STATUS).toString())) {
                    stringsFound = 
			cli.findStringsInError(expectedMessage, ";");
                } else {
                    String argString = cli.getAllArgs().replaceFirst(
                            cli.getCliPath(), "ssoadm ");
                    Object[] params = {argString};
                    String usageError = MessageFormat.format(expectedMessage, 
                            params);
                    stringsFound = cli.findStringsInError(usageError, 
                            ";" + newline);                      
                }
                log(logLevel, "testGetRevisionNumber", "Error Messages Found: "  
                        + stringsFound);
                assert (getCommandStatus == 
                    new Integer(getRevExpectedExitCode).intValue()) && 
                    stringsFound;
            }
            cli.resetArgList();
            
            /*
             * Sets the revision number of service specified in service-name
             * with revision number specified in set-revision-number
             * from RevisionNumberTest.properties
             */
            cli = new FederationManagerCLI(useDebugOption, useVerboseOption, 
                    useLongOptions);
            int setCommandStatus = cli.setRevisionNumber(serviceName,
                    revisionNumberToSet);
            cli.logCommand("testRevisionNumber");
            if (setRevExpectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                stringsFound = cli.findStringsInOutput(expectedMessage, ";");
                log(Level.FINEST, "testRevisionNumber", 
                        "Output Messages Found: " + stringsFound);
                assert (setCommandStatus == 
                        new Integer(setRevExpectedExitCode).intValue()) && 
			stringsFound;
            } else {
                if (!setRevExpectedExitCode.equals(
                        new Integer(INVALID_OPTION_STATUS).toString())) {
                    stringsFound = 
			cli.findStringsInError(expectedMessage, ";");
                } else {
                    String argString = cli.getAllArgs().replaceFirst(
                            cli.getCliPath(), "ssoadm ");
                    Object[] params = {argString};
                    String usageError = MessageFormat.format(expectedMessage, 
                            params);
                    stringsFound = cli.findStringsInError(usageError, 
                            ";" + newline);                      
                }
                log(logLevel, "testRevisionNumber", "Error Messages Found: " + 
                        stringsFound);
                assert (setCommandStatus == 
                    new Integer(setRevExpectedExitCode).intValue()) 
                    && stringsFound;
            }
            cli.resetArgList();

            /*
             * Gets the new revision number of service specified in service-name
             * from RevisionNumberTest.properties
             */
            cli = new FederationManagerCLI(useDebugOption, useVerboseOption, 
                    useLongOptions);
            int getNewCommandStatus = cli.getRevisionNumber(serviceName);
            cli.logCommand("testRevisionNumber");
            String newRevisionNumber = getRevisionNumber(cli
                    .getCommand().getOutput());
            log(Level.FINE, "testRevsionNumber", "Revision Number of " +
                    "service " + serviceName + "is: " + newRevisionNumber);
            
            if (setRevExpectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                if (!newRevisionNumber.equals(revisionNumberToSet))
                    assert false;
            } 
            if (getRevExpectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                stringsFound = cli.findStringsInOutput(expectedMessage, ";");
                log(Level.FINEST, "testRevisionNumber", 
                        "Output Messages Found: " + stringsFound);
                assert (getNewCommandStatus == 
                        new Integer(getRevExpectedExitCode).intValue()) && 
			stringsFound;
            } else {
                if (!getRevExpectedExitCode.equals(
                        new Integer(INVALID_OPTION_STATUS).toString())) {
                    stringsFound = 
			cli.findStringsInError(expectedMessage, ";");
                } else {
                    String argString = cli.getAllArgs().replaceFirst(
                            cli.getCliPath(), "ssoadm ");
                    Object[] params = {argString};
                    String usageError = MessageFormat.format(expectedMessage, 
                            params);
                    stringsFound = cli.findStringsInError(usageError, 
                            ";" + newline);                      
                }
                log(logLevel, "testRevisionNumber", "Error Messages Found: " + 
                        stringsFound);
                assert (getNewCommandStatus == 
                    new Integer(getRevExpectedExitCode).intValue())
                    && stringsFound;
            }
            cli.resetArgList();
            exiting("testRevisionNumber");
        } catch (Exception e) {
            log(Level.SEVERE, "testRevisionNumber", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        } 
    }
    
    /**
     * This method removes services that were created during the setup 
     * using "ssoadm delete-svc".
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup() 
    throws Exception {
        entering("cleanup", null);
        try {            
            cli = new FederationManagerCLI(useDebugOption, useVerboseOption, 
                    useLongOptions);
            if (cli.deleteService(serviceName) != SUCCESS_STATUS) {
                log(Level.SEVERE, "cleanup", 
                        "Service deletion failed.");
                assert false;
            }
            cli.logCommand("cleanup");
            cli.resetArgList();
            
            log(Level.FINEST, "cleanup", "useDebugOption: " + useDebugOption);
            log(Level.FINEST, "cleanup", "useVerboseOption: " + 
                    useVerboseOption);
            log(Level.FINEST, "cleanup", "useLongOptions: " + useLongOptions);
            
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            exiting("cleanup");
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        } 
    }  
    
    /**
     * This method returns Revision number from string with revision number.
     * e.g. If string with revision number is - Revision number for session 
     * service was 70. It returns 70. If revision number is not present it 
     * will return empty string. 
     * @param strWithRevisionNumber - String with revision number.
     * @return RevisionNumber 
     */
    private String getRevisionNumber(StringBuffer strWithRevisionNumber) {
        for (String s : strWithRevisionNumber.toString()
                .split(" |[^A-Za-z0-9]")) {
            if (Pattern.matches("[0-9]+",s)) {
                return s;
            }
        }
        return " ";        
    }
}

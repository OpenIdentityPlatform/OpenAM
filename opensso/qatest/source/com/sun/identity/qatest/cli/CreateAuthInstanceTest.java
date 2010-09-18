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
 * $Id: CreateAuthInstanceTest.java,v 1.6 2009/01/26 23:48:56 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.cli;

import com.sun.identity.qatest.common.cli.CLIExitCodes;
import com.sun.identity.qatest.common.cli.FederationManagerCLI;
import com.sun.identity.qatest.common.TestCommon;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.Reporter;

/**
 * <code>CreateAuthInstanceTest</code> is used to execute tests involving the 
 * create-auth-instance sub-command of ssoadm.  This class allows the user to 
 * execute "ssoadm create-auth-instance" with a variety or arguments 
 * (e.g with short or long options, with a password file or password argument, 
 * with a locale argument and a variety of input values.  The properties file 
 * <code>CreateAuthInstanceTest.properties</code> contains the input values 
 * which are read by this class.
 *
 * This class automates the following test cases:
 * CLI_create-auth-instance01, CLI_create-auth-instance02, 
 * CLI_create-auth-instance03, CLI_create-auth-instance04,
 * CLI_create-auth-instance05, CLI_create-auth-instance06, 
 * CLI_create-auth-instance07, CLI_create-auth-instance08,
 * CLI_create-auth-instance09, CLI_create-auth-instance10, 
 * CLI_create-auth-instance11, CLI_create-auth-instance12,
 * CLI_create-auth-instance13, CLI_create-auth-instance14, 
 * CLI_create-auth-instance15, CLI_create-auth-instance16,
 * CLI_create-auth-instance17, CLI_create-auth-instance18,
 * CLI_create-auth-instance19, CLI_create-auth-instance20, 
 * CLI_create-auth-instance21, CLI_create-auth-instance22,
 * CLI_create-auth-instance23, CLI_create-auth-instance24,
 * CLI_create-auth-instance25, CLI_create-auth-instance26,
 * CLI_create-auth-instance27, CLI_create-auth-instance28
 */
public class CreateAuthInstanceTest extends TestCommon implements CLIExitCodes {
    
    private String locTestName;
    private ResourceBundle rb;
    private String setupRealms;
    private String realm;
    private String name;
    private String authType;
    private boolean useVerboseOption;
    private boolean useDebugOption;
    private boolean useLongOptions;
    private String expectedMessage;
    private String expectedExitCode;
    private String description;
    private FederationManagerCLI cli;
    
    /** 
     * Creates a new instance of CreateAuthInstanceTest 
     */
    public CreateAuthInstanceTest() {
        super("CreateAuthInstanceTest");
    }
    
    /**
     * This method is intended to provide initial setup.
     * Creates any realms specified in the setup-realms property in the 
     * CreateAuthInstanceTest.properties.
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
                    "CreateAuthInstanceTest");
            setupRealms = (String)rb.getString(locTestName + 
                    "-create-setup-realms");
            useVerboseOption = ((String)rb.getString(locTestName + 
                    "-use-verbose-option")).equals("true");
            useDebugOption = ((String)rb.getString(locTestName + 
                    "-use-debug-option")).equals("true");
            useLongOptions = ((String)rb.getString(locTestName + 
                    "-use-long-options")).equals("true");
                
            log(Level.FINEST, "setup", "use-verbose-option: " + 
                    useVerboseOption);
            log(Level.FINEST, "setup", "use-debug-option: " + useDebugOption);
            log(Level.FINEST, "setup", "use-long-options: " + useLongOptions);
            log(Level.FINEST, "setup", "create-setup-realms: " + setupRealms);
             
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("SetupRealms: " + setupRealms);

            cli = new FederationManagerCLI(useDebugOption, useVerboseOption, 
                    useLongOptions);
            
            if (setupRealms != null && !setupRealms.equals("")) {
                if (!cli.createRealms(setupRealms)) {
                    log(Level.SEVERE, "setup", 
                            "All the realms failed to be created.");
                    assert false;
                }
            }
            
            exiting("setup");
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        } 
    }
    
    /**
     * This method is used to execute tests involving 
     * "ssoadm create-auth-instance" using input data from the 
     * CreateAuthInstanceTest.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testAuthInstanceCreation() 
    throws Exception {
        entering("testAuthInstanceCreation", null);
        boolean stringsFound = false;
        boolean instanceFound = false;
        boolean errorFound = false;
        
        try {
            expectedMessage = (String) rb.getString(locTestName + 
                    "-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-expected-exit-code");
            realm = (String) rb.getString(locTestName + 
                    "-create-auth-instance-realm");
            name = (String) rb.getString(locTestName + 
                    "-create-auth-instance-name");
            authType = (String) rb.getString(locTestName + 
                    "-create-auth-instance-authtype");
            description = (String) rb.getString(locTestName + "-description");

            log(Level.FINEST, "testAuthInstanceCreation", "description: " + 
                    description);
            log(Level.FINEST, "testAuthInstanceCreation", 
                    "use-debug-option: " + useDebugOption);
            log(Level.FINEST, "testAuthInstanceCreation", 
                    "use-verbose-option: " + useVerboseOption);
            log(Level.FINEST, "testAuthInstanceCreation", 
                    "use-long-options: " + useLongOptions);
            log(Level.FINEST, "testAuthInstanceCreation", 
                    "expected-exit-code: " + expectedExitCode);
            log(Level.FINEST, "testAuthInstanceCreation", 
                    "create-auth-instance-realm: " + realm);
            log(Level.FINEST, "testAuthInstanceCreation", 
                    "create-auth-instance-name: " + name);
            log(Level.FINEST, "testAuthInstanceCreation", 
                    "create-auth-instance-authtype: " + authType);

            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);
            Reporter.log("Realm: " + realm);
            Reporter.log("InstanceName: " + name);
            Reporter.log("AuthInstanceType: " + authType);

            log(Level.FINE, "testAuthInstanceCreation", "Creating " + authType + 
                    " instance named " + name + ".");
            int commandStatus = cli.createAuthInstance(realm, name, authType);
            cli.logCommand("testAuthInstanceCreation");

            log(Level.FINEST, "testAuthInstanceCreation", "message-to-find: " + 
                    expectedMessage);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            
            String msg = (String) rb.getString(locTestName + 
                    "-message-to-find");           

            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                if (msg.equals("")) {           
                    if (!useVerboseOption) {
                        expectedMessage = 
                                (String) rb.getString("success-message");
                    } else {
                        expectedMessage = 
                                (String) rb.getString(
                                "verbose-success-message");
                    }
                } else {
                    expectedMessage = msg;
                }                
                stringsFound = cli.findStringsInOutput(expectedMessage, ";");
                cli.resetArgList();
                FederationManagerCLI listCLI = 
                        new FederationManagerCLI(useDebugOption, 
                        useVerboseOption, useLongOptions);
                instanceFound = listCLI.findAuthInstances(realm, name + "," + 
                        authType);
            } else if (expectedExitCode.equals(
                    new Integer(INVALID_OPTION_STATUS).toString())) {
                expectedMessage = (String) rb.getString("usage");                
                String argString = cli.getAllArgs().replaceFirst(
                        cli.getCliPath(), "ssoadm ");
                Object[] params = {argString};
                String errorMessage = 
                        (String) rb.getString("invalid-usage-message");
                String usageError = MessageFormat.format(errorMessage, params);
                stringsFound = cli.findStringsInOutput(expectedMessage, ";");                
                errorFound = cli.findStringsInError(usageError, ";");
            } else {
                expectedMessage = msg;                                
                errorFound = cli.findStringsInError(expectedMessage, ";");
            }
            cli.resetArgList();

            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                log(Level.FINEST, "testAuthInstanceCreation", 
                        "Output Messages Found: " + stringsFound);
                log(Level.FINEST, "testAuthInstanceCreation", "Instance Found: "
                        + instanceFound); 
                assert (commandStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound &&
                        instanceFound;
            } else {
                log(Level.FINEST, "testAuthInstanceCreation", 
                        "Error Messages Found: " + stringsFound);
                assert (commandStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound;
            }     
            exiting("testAuthInstanceCreation");
        } catch (Exception e) {
            log(Level.SEVERE, "testAuthInstanceCreation", e.getMessage());
            e.printStackTrace();
            throw e;
        } 
    }
    
    /**
     * This method remove realms and authentication instances that were created 
     * during the setup and testAuthInstanceCreation methods using 
     * "ssoadm delete-auth-insatances" and "ssoadm delete-realm".
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup() 
    throws Exception {
        int cleanupExitStatus = -1;

        entering("cleanup", null);
        try {            
            log(Level.FINEST, "cleanup", "useDebugOption: " + useDebugOption);
            log(Level.FINEST, "cleanup", "useVerboseOption: " + 
                    useVerboseOption);
            log(Level.FINEST, "cleanup", "useLongOptions: " + useLongOptions);
            
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
       
            FederationManagerCLI cleanupCli = 
                    new FederationManagerCLI(useDebugOption, 
                    useVerboseOption, useLongOptions);

            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                log(Level.FINE, "cleanup", "Deleting auth instance " + name);
                cleanupExitStatus = cleanupCli.deleteAuthInstances(realm, name);
                cleanupCli.logCommand("cleanup");
                if (cleanupExitStatus != SUCCESS_STATUS) {
                    log(Level.SEVERE, "cleanup", "The deletion of auth " + 
                            "instance " + name + " failed with status " + 
                            cleanupExitStatus + ".");
                    assert false;
                }
                cleanupCli.resetArgList();
                FederationManagerCLI listCli = 
                        new FederationManagerCLI(useDebugOption, 
                        useVerboseOption, useLongOptions);
                if (listCli.findAuthInstances(realm, name + "," + authType)) {
                    log(Level.SEVERE, "cleanup", "Auth instance " + name + 
                            " was found after deletion.");
                    assert false;
                }
            }
            
            if (!setupRealms.equals("")) {
                String[] realms = setupRealms.split(";");
                for (int i=realms.length-1; i >= 0; i--) {
                    log(Level.FINE, "cleanup", "Removing realm " + 
                        realms[i]);
                    Reporter.log("SetupRealmToDelete: " + realms[i]);
                    cleanupExitStatus = cleanupCli.deleteRealm(realms[i], true); 
                    cleanupCli.logCommand("cleanup");
                    cleanupCli.resetArgList();
                    if (cleanupExitStatus != SUCCESS_STATUS) {
                        log(Level.SEVERE, "cleanup", "The removal of realm " + 
                                realms[i] + " failed with exit status " + 
                                cleanupExitStatus + ".");
                        assert false;
                    }
                } 
            }            
            exiting("cleanup");
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        } 
    }
}

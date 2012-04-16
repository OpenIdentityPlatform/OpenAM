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
 * $Id: CreateRealmTest.java,v 1.11 2009/01/26 23:48:57 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/**
 * CreateRealmTest automates the following test cases:
 * CLI_create-realm01, CLI_create-realm02, CLI_create-realm03, 
 * CLI_create-realm04, CLI_create-realm05, CLI_create-realm06, 
 * CLI_create-realm07, CLI_create-realm08, CLI_create-realm09, 
 * CLI_create-realm10, CLI_create-realm11, CLI_create-realm12, 
 * CLI_create-realm13, CLI_create-realm14, CLI_create-realm15,
 * and CLI_create-realm16.
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
 * <code>CreateRealmTest</code> is used to execute tests involving the 
 * create-realm sub-command of ssoadm.  This class allows the user to execute
 * "ssoadm create-realm" with a variety or arguments (e.g with short or long 
 * options, with a password file or password argument, with a locale argument,
 * etc.) and a variety of input values.  The properties file 
 * <code>CreateRealmTest.properties</code> contains the input values which are 
 * read by this class.
 */
public class CreateRealmTest extends TestCommon implements CLIExitCodes {
    
    private String locTestName;
    private ResourceBundle rb;
    private String setupRealms;
    private String realmToCreate;
    private boolean useVerboseOption;
    private boolean useDebugOption;
    private boolean useLongOptions;
    private String expectedMessage;
    private String expectedExitCode;
    private String description;
    private FederationManagerCLI cli;
    
    /** 
     * Creates a new instance of CreateRealmTest 
     */
    public CreateRealmTest() {
        super("CreateRealmTest");
    }
    
    /**
     * This method is intended to provide initial setup.
     * Creates any realms specified in the setup-realms property in the 
     * createRealmTest.properties.
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
                    "CreateRealmTest");
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
            log(Level.SEVERE, "setup", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        } 
    }
    
    /**
     * This method is used to execute tests involving "ssoadm create-realm"
     * using input data from the CreateRealmTest.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testRealmCreation() 
    throws Exception {
        entering("completeRealmCreation", null);
        boolean stringsFound = false;
        boolean realmFound = false;
        
        try {
            expectedMessage = (String) rb.getString(locTestName + 
                    "-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-expected-exit-code");
            realmToCreate = (String) rb.getString(locTestName + 
                    "-create-realm");
            description = (String) rb.getString(locTestName + "-description");

            log(Level.FINEST, "testRealmCreation", "description: " + 
                    description);
            log(Level.FINEST, "testRealmCreation", "use-debug-option: " + 
                    useDebugOption);
            log(Level.FINEST, "testRealmCreation", "use-verbose-option: " + 
                    useVerboseOption);
            log(Level.FINEST, "testRealmCreation", "use-long-options: " + 
                    useLongOptions);
            log(Level.FINEST, "testRealmCreation", "message-to-find: " + 
                    expectedMessage);
            log(Level.FINEST, "testRealmCreation", "expected-exit-code: " + 
                    expectedExitCode);
            log(Level.FINEST, "testRealmCreation", "create-realm: " + 
                    realmToCreate);

            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);
            Reporter.log("RealmToCreate: " + realmToCreate);
            
            int commandStatus = cli.createRealm(realmToCreate);
            cli.logCommand("testRealmCreation");

            if (realmToCreate.length() > 0) {
                FederationManagerCLI listCLI = 
                        new FederationManagerCLI(useDebugOption, 
                        useVerboseOption, useLongOptions);
                realmFound = listCLI.findRealms(realmToCreate);
                log(Level.FINEST, "testRealmCreation", "Realm " + 
                        realmToCreate + " Found: " + realmFound);
            }

            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                stringsFound = cli.findStringsInOutput(expectedMessage, ";");
                log(Level.FINEST, "testRealmCreation", 
                        "Output Messages Found: " + stringsFound);
                assert (commandStatus == 
                    new Integer(expectedExitCode).intValue()) && 
                        stringsFound && realmFound;
            } else {
                if (!expectedExitCode.equals(
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
                log(logLevel, "testRealmCreation", "Error Messages Found: " + 
                        stringsFound);
                assert (commandStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound;
            }
            cli.resetArgList();
            exiting("testRealmCreation");
        } catch (Exception e) {
            log(Level.SEVERE, "testRealmCreation", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        } 
    }
    
    /**
     * This method remove any realms that were created during the setup and
     * testRealmCreation methods using "ssoadm delete-realm".
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup() 
    throws Exception {
        entering("cleanup", null);
        try {            
            log(Level.FINEST, "cleanup", "useDebugOption: " + useDebugOption);
            log(Level.FINEST, "cleanup", "useVerboseOption: " + 
                    useVerboseOption);
            log(Level.FINEST, "cleanup", "useLongOptions: " + useLongOptions);
            
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            
            if (!realmToCreate.equals("")) {
                log(Level.FINEST, "cleanup", "realmToDelete: "  + 
                        realmToCreate);
                Reporter.log("RealmToDelete: " + realmToCreate);
                cli.deleteRealm(realmToCreate, true);
                cli.logCommand("cleanup");
                cli.resetArgList();
            }
            
            if (!setupRealms.equals("")) {
                String[] realms = setupRealms.split(";");
                for (int i=realms.length-1; i >= 0; i--) {
                    if (!realms[i].equals(realmToCreate)) {
                        log(Level.FINEST, "cleanup", "setupRealmToDelete: " + 
                            realms[i]);
                        Reporter.log("SetupRealmToDelete: " + realms[i]);
                        int exitStatus = cli.deleteRealm(realms[i], true); 
                        cli.logCommand("cleanup");
                        cli.resetArgList();
                        if (exitStatus != SUCCESS_STATUS) {
                            log(Level.SEVERE, "cleanup", 
                                    "Realm deletion returned the failed exit " +
                                    "status " + exitStatus + ".");
                            assert false;
                        }
                        if (cli.findRealms(realms[i])) {
                            log(Level.SEVERE, "cleanup", "Deleted realm " + 
                                    realms[i] + " still exists.");
                            assert false;
                        }
                        cli.resetArgList();
                    }
                }
            }
            exiting("cleanup");
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        } 
    }
}

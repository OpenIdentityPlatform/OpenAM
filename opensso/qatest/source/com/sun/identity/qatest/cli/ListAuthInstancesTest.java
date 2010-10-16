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
 * $Id: ListAuthInstancesTest.java,v 1.6 2009/01/26 23:49:34 nithyas Exp $
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
 * <code>ListAuthInstancesTest</code> is used to execute tests involving the 
 * list-auth-instances sub-command of ssoadm.  This class allows the user to 
 * execute "ssoadm list-auth-instances" with a variety or arguments 
 * (e.g with short or long options, with a password file or password argument, 
 * with a locale argument and a variety of input values.  The properties file 
 * <code>ListAuthInstancesTest.properties</code> contains the input values 
 * which are read by this class.
 *
 * This class automates the following test cases:
 * CLI_list-auth-instances01, CLI_list-auth-instances02, 
 * CLI_list-auth-instances03, CLI_list-auth-instances04,
 * CLI_list-auth-instances05, CLI_list-auth-instances06, 
 * CLI_list-auth-instances07, CLI_list-auth-instances08,
 * CLI_list-auth-instances09, CLI_list-auth-instances10, 
 * CLI_list-auth-instances11, CLI_list-auth-instances12,
 * CLI_list-auth-instances13, CLI_list-auth-instances14, 
 * CLI_list-auth-instances15, CLI_list-auth-instances16,
 * CLI_list-auth-instances17, CLI_list-auth-instances18,
 * CLI_list-auth-instances19, CLI_list-auth-instances20, 
 * CLI_list-auth-instances21, CLI_list-auth-instances22,
 * CLI_list-auth-instances23, CLI_list-auth-instances24,
 * CLI_list-auth-instances25, CLI_list-auth-instances26,
 */
public class ListAuthInstancesTest extends TestCommon implements CLIExitCodes {
    
    private String locTestName;
    private ResourceBundle rb;
    private String setupRealms;
    private String setupAuthInstances;
    private String authRealm;
    private String name;
    private String instancesToFind;
    private String cleanupInstances;
    private boolean useVerboseOption;
    private boolean useDebugOption;
    private boolean useLongOptions;
    private String expectedMessage;
    private String expectedExitCode;
    private String description;
    private FederationManagerCLI cli;
    
    /** 
     * Creates a new instance of ListAuthInstancesTest 
     */
    public ListAuthInstancesTest() {
        super("ListAuthInstancesTest");
    }
    
    /**
     * This method is intended to provide initial setup.
     * Creates any realms specified in the setup-realms property and creates
     * authentication instances specified in the setup-identities property in 
     * the ListAuthInstancesTest.properties.
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
                    "ListAuthInstancesTest");
            setupRealms = (String)rb.getString(locTestName + 
                    "-create-setup-realms");
            setupAuthInstances = (String)rb.getString(locTestName + 
                    "-create-setup-auth-instances");
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
            
            if (setupAuthInstances != null && !setupAuthInstances.equals("")) {
                if (!cli.createAuthInstances(setupAuthInstances)) {
                    log(Level.SEVERE, "setup", 
                            "All the auth instances failed to be created");
                    assert false;
                } else {
                    String[] instances = setupAuthInstances.split("\\|");
                    StringBuffer instanceBuffer = new StringBuffer();
                    for (int i=0; i < instances.length; i++) {
                        String[] instanceData = instances[i].split(",");
                        instanceBuffer.append(instanceData[0]).append(",").
                                append(instanceData[1]).append(",").
                                append(instanceData[2]);
                        if (i < instances.length - 1) {
                            instanceBuffer.append(";");
                        }
                    }
                    cleanupInstances = instanceBuffer.toString();
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
     * "ssoadm list-auth-instances" using input data from the 
     * ListAuthInstancesTest.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testListAuthInstances() 
    throws Exception {
        entering("testListAuthInstances", null);
        boolean stringsFound = false;
        boolean instancesFound = false;
        boolean errorFound = false;
        boolean authSuccessful = true;
        String attributesToFind;
        
        try {
            expectedMessage = (String) rb.getString(locTestName + 
                    "-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-expected-exit-code");
            authRealm = (String) rb.getString(locTestName + 
                    "-list-auth-instances-realm");
            instancesToFind = (String) rb.getString(locTestName + 
                    "-auth-instances-to-find");
            description = (String) rb.getString(locTestName + "-description");
 
            log(Level.FINEST, "testListAuthInstances", "description: " + 
                    description);
            log(Level.FINEST, "testListAuthInstances", 
                    "use-debug-option: " + useDebugOption);
            log(Level.FINEST, "testListAuthInstances", 
                    "use-verbose-option: " + useVerboseOption);
            log(Level.FINEST, "testListAuthInstances", 
                    "use-long-options: " + useLongOptions);
            log(Level.FINEST, "testListAuthInstances", 
                    "expected-exit-code: " + expectedExitCode);
            log(Level.FINEST, "testListAuthInstances", 
                    "list-auth-instances-realm: " + authRealm);
            log(Level.FINEST, "testListAuthInstances", 
                    "auth-instances-to-find: " + instancesToFind);
            
            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);
            Reporter.log("Realm: " + authRealm);
            Reporter.log("InstancesToFind: " + instancesToFind);
            
            log(Level.FINE, "testListAuthInstances", "Listing the auth "
                    + "instances in realm " + authRealm + ".");
            int commandStatus = cli.listAuthInstances(authRealm);
            cli.logCommand("testListAuthInstances");

            log(Level.FINEST, "testListAuthInstances", "message-to-find: " + 
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
                instancesFound = cli.findAuthInstances(authRealm, 
                        instancesToFind);
                cli.resetArgList();
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
                log(Level.FINEST, "testListAuthInstances", 
                        "Output Messages Found: " + stringsFound);
                assert (commandStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound &&
                        instancesFound;
            } else {
                log(Level.FINEST, "testListAuthInstances", 
                        "Error Messages Found: " + stringsFound);
                assert (commandStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound;
            }     
            exiting("testListAuthInstances");
        } catch (Exception e) {
            log(Level.SEVERE, "testListAuthInstances", e.getMessage());
            e.printStackTrace();
            throw e;
        } 
    }
    
    /**
     * This method remove any authentication instances and realms that were 
     * created during the setup method using "ssoadm delete-auth-instances" and 
     * "ssoadm delete-realm".
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
            
            cli.resetArgList(); 
                       
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                log(Level.FINE, "cleanup", "Deleting auth instances " + 
                        cleanupInstances);
                String[] instancesToDelete = cleanupInstances.split(";");

                for (String instance: instancesToDelete) {
                    String[] instanceData = instance.split(",");
                    String instanceRealm = instanceData[0];
                    String nameToDelete = instanceData[1];
                    String typeToDelete = instanceData[2];
                    FederationManagerCLI listCli = 
                            new FederationManagerCLI(useDebugOption, 
                            useVerboseOption, useLongOptions);                   
                    cleanupExitStatus = 
                            listCli.deleteAuthInstances(instanceRealm, 
                            nameToDelete);
                    listCli.logCommand("cleanup");
                    listCli.resetArgList();
                    log(Level.FINE, "cleanup", "Deleting authentication " +
                            "instance " + nameToDelete + " ...");
                    if (cleanupExitStatus != SUCCESS_STATUS) {
                        log(Level.SEVERE, "cleanup", "The deletion of auth " + 
                                "instance " + nameToDelete + " in realm " + 
                                instanceRealm + " failed with status " + 
                                cleanupExitStatus + ".");
                        assert false;
                    }
                    if (listCli.findAuthInstances(instanceRealm, 
                            nameToDelete + "," + typeToDelete)) {
                        log(Level.SEVERE, "cleanup", "Auth instance " + 
                                nameToDelete + " was found after deletion.");
                        assert false;
                    }  
                    listCli.resetArgList();
                }
            }
            
            if (!setupRealms.equals("")) {
                String[] realms = setupRealms.split(";");
                for (int i=realms.length-1; i >= 0; i--) {
                    log(Level.FINE, "cleanup", "Removing realm " + 
                        realms[i]);
                    Reporter.log("SetupRealmToDelete: " + realms[i]);
                    FederationManagerCLI cleanupCli = 
                            new FederationManagerCLI(useDebugOption, 
                            useVerboseOption, useLongOptions);  
                    cleanupExitStatus = cleanupCli.deleteRealm(realms[i], true); 
                    cleanupCli.logCommand("cleanup");
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

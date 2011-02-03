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
 * $Id: DeleteAuthInstancesTest.java,v 1.4 2009/01/26 23:48:58 nithyas Exp $
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
 * <code>DeleteAuthInstancesTest</code> is used to execute tests involving the 
 * delete-auth-instances sub-command of ssoadm.  This class allows the user to 
 * execute "ssoadm delete-auth-instances" with a variety or arguments 
 * (e.g with short or long options, with a password file or password argument, 
 * with a locale argument and a variety of input values.  The properties file 
 * <code>DeleteAuthInstancesTest.properties</code> contains the input values 
 * which are read by this class.
 *
 * This class automates the following test cases:
 * CLI_delete-auth-instances01, CLI_delete-auth-instances02, 
 * CLI_delete-auth-instances03, CLI_delete-auth-instances04,
 * CLI_delete-auth-instances05, CLI_delete-auth-instances06, 
 * CLI_delete-auth-instances07, CLI_delete-auth-instances08,
 * CLI_delete-auth-instances09, CLI_delete-auth-instances10, 
 * CLI_delete-auth-instances11, CLI_delete-auth-instances12,
 * CLI_delete-auth-instances13, CLI_delete-auth-instances14, 
 * CLI_delete-auth-instances15, CLI_delete-auth-instances16,
 * CLI_delete-auth-instances17, CLI_delete-auth-instances18,
 * CLI_delete-auth-instances19, CLI_delete-auth-instances20, 
 * CLI_delete-auth-instances21, CLI_delete-auth-instances22,
 * CLI_delete-auth-instances23, CLI_delete-auth-instances24
 */
public class DeleteAuthInstancesTest extends TestCommon 
implements CLIExitCodes {
    
    private String locTestName;
    private ResourceBundle rb;
    private String setupRealms;
    private String setupAuthInstances;
    private String authRealm;
    private String instanceNames;
    private String instancesToFind;
    private boolean useDatafileOption;
    private boolean useVerboseOption;
    private boolean useDebugOption;
    private boolean useLongOptions;
    private String expectedMessage;
    private String expectedExitCode;
    private String description;
    private FederationManagerCLI cli;
    
    /** 
     * Creates a new instance of DeleteAuthInstancesTest 
     */
    public DeleteAuthInstancesTest() {
        super("DeleteAuthInstancesTest");
    }
    
    /**
     * This method is intended to provide initial setup.
     * Creates any realms specified in the setup-realms property and creates
     * any identities specified in the setup-identities property in the 
     * DeleteAuthInstancesTest.properties.
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
                    "DeleteAuthInstancesTest");
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
     * "ssoadm delete-auth-instances" using input data from the 
     * DeleteAuthInstancesTest.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testDeleteAuthInstances() 
    throws Exception {
        entering("testDeleteAuthInstances", null);
        boolean stringsFound = false;
        boolean instancesFound = false;
        boolean errorFound = false;
        
        try {
            expectedMessage = (String) rb.getString(locTestName + 
                    "-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-expected-exit-code");
            authRealm = (String) rb.getString(locTestName + 
                    "-delete-auth-instances-realm");
            instanceNames = (String) rb.getString(locTestName + 
                    "-delete-auth-instances-names");
            instancesToFind = (String) rb.getString(locTestName + 
                    "-auth-instances-to-find");
            description = (String) rb.getString(locTestName + "-description");
 
            log(Level.FINEST, "testDeleteAuthInstances", "description: " + 
                    description);
            log(Level.FINEST, "testDeleteAuthInstances", 
                    "use-debug-option: " + useDebugOption);
            log(Level.FINEST, "testDeleteAuthInstances", 
                    "use-verbose-option: " + useVerboseOption);
            log(Level.FINEST, "testDeleteAuthInstances", 
                    "use-long-options: " + useLongOptions);
            log(Level.FINEST, "testDeleteAuthInstances", 
                    "expected-exit-code: " + expectedExitCode);
            log(Level.FINEST, "testDeleteAuthInstances", 
                    "delete-auth-instances-realm: " + authRealm);
            log(Level.FINEST, "testDeleteAuthInstances", 
                    "delete-auth-instances-names: " + instanceNames);
            log(Level.FINEST, "testDeleteAuthInstances", 
                    "auth-instances-to-find: " + instancesToFind);
            
            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);
            Reporter.log("Realm: " + authRealm);
            Reporter.log("InstanceNames: " + instanceNames);
            Reporter.log("AuthInstancesToFind: " + instancesToFind);
            
            log(Level.FINE, "testDeleteAuthInstances", "Deleting the auth " +
                    "instance(s) " + instanceNames);
            int commandStatus = cli.deleteAuthInstances(authRealm, 
                    instanceNames);
            cli.logCommand("testDeleteAuthInstances");

            log(Level.FINEST, "testDeleteAuthInstances", "message-to-find: " + 
                    expectedMessage);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            
            String msg = (String) rb.getString(locTestName + 
                    "-message-to-find");           

            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                if (msg.equals("")) {  
                    String [] instancesToDelete = instanceNames.split(";");
                    String stringToRetrieve = "success-message";
                    if (instancesToDelete.length > 1) {
                        stringToRetrieve = "success-message-plural";
                    }
                    if (!useVerboseOption) {
                        expectedMessage = 
                                (String) rb.getString(stringToRetrieve);
                    } else {
                        expectedMessage = 
                                new StringBuffer((String) rb.getString(
                                "verbose-success-message")).append(";").
                                append((String) rb.getString(stringToRetrieve)).
                                toString();
                    }
                } else {
                    expectedMessage = msg;
                }                
                stringsFound = cli.findStringsInOutput(expectedMessage, ";");
                cli.resetArgList();
                FederationManagerCLI listCLI = 
                        new FederationManagerCLI(useDebugOption, 
                        useVerboseOption, useLongOptions);
                instancesFound = listCLI.findAuthInstances(authRealm, 
                        instancesToFind);
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
                log(Level.FINEST, "testDeleteAuthInstances", 
                        "Output Messages Found: " + stringsFound);
                log(Level.FINEST, "testDeleteAuthInstances", 
                        "Instances Found: " + instancesFound); 
                assert (commandStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound &&
                        instancesFound;
            } else {
                log(Level.FINEST, "testDeleteAuthInstances", 
                        "Error Messages Found: " + stringsFound);
                assert (commandStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound;
            }     
            exiting("testDeleteAuthInstances");
        } catch (Exception e) {
            log(Level.SEVERE, "testDeleteAuthInstances", e.getMessage());
            e.printStackTrace();
            throw e;
        } 
    }
    
    /**
     * This method remove any instances and realms that were created during the 
     * setup and testDeleteAuthInstances methods using "ssoadm delete-realm".
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup() 
    throws Exception {
        int exitStatus = -1;

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
                        
            if (!setupRealms.equals("")) {
                String[] realms = setupRealms.split(";");
                for (int i=realms.length-1; i >= 0; i--) {
                    log(Level.FINE, "cleanup", "Removing realm " + 
                        realms[i]);
                    Reporter.log("SetupRealmToDelete: " + realms[i]);
                    exitStatus = cli.deleteRealm(realms[i], true); 
                    cli.logCommand("cleanup");
                    cli.resetArgList();
                    if (exitStatus != SUCCESS_STATUS) {
                        log(Level.SEVERE, "cleanup", "The removal of realm " + 
                                realms[i] + " failed with exit status " + 
                                exitStatus + ".");
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

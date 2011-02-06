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
 * $Id: DeleteRealmTest.java,v 1.15 2009/06/24 22:29:32 srivenigan Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/**
 * DeleteRealmTest automates the following test cases:
 * CLI_delete-realm01, CLI_delete-realm02, CLI_delete-realm03, 
 * CLI_delete-realm04, CLI_delete-realm05, CLI_delete-realm06, 
 * CLI_delete-realm07, CLI_delete-realm08, CLI_delete-realm09, 
 * and CLI_delete-realm10
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
 * <code>DeleteRealmTest</code> is used to execute tests involving the 
 * delete-realm sub-command of ssoadm.  This class allows the user to execute
 * "ssoadm delete-realm" with a variety or arguments (e.g with short or long 
 * options, with a locale argument, etc.) and a variety of input values.  The 
 * properties file <code>DeleteRealmTest.properties</code> contains the input 
 * values which are read by this class.
 */
public class DeleteRealmTest extends TestCommon implements CLIExitCodes {
    
    private String locTestName;
    private ResourceBundle rb;
    private String setupRealms;
    private String realmToDelete;
    private String realmsDeleted;
    private String realmsExisting;
    private boolean useVerboseOption;
    private boolean useDebugOption;
    private boolean useLongOptions;
    private boolean useRecursiveOption;
    private String expectedMessage;
    private String expectedExitCode;
    private String description;
    private FederationManagerCLI cli;
    
    /** 
     * Creates a new instance of DeleteRealmTest 
     */
    public DeleteRealmTest() {
        super("DeleteRealmTest");
    }
    
    /**
     * This method is intended to provide initial setup.
     * Creates any realms specified in the setup-realms property in the 
     * deleteRealmTest.properties.
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
                    "DeleteRealmTest-Generated");
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
     * This method is used to execute tests involving "ssoadm delete-realm"
     * using input data from the DeleteRealmTest.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testRealmDeletion() 
    throws Exception {
        entering("testRealmDeletion", null);
        boolean stringsFound = false;
        boolean removedRealmsFound = false;
        boolean existingRealmsFound = true;
        
        try {
            description = (String) rb.getString(locTestName + "-description");
            useRecursiveOption = ((String)rb.getString(locTestName + 
                    "-use-recursive-option")).equals("true");
            expectedMessage = (String) rb.getString(locTestName + 
                    "-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-expected-exit-code");
            realmToDelete = (String) rb.getString(locTestName + 
                    "-delete-realm");
            realmsDeleted = (String) rb.getString(locTestName + 
                    "-realms-deleted");
            realmsExisting = (String) rb.getString(locTestName + 
                    "-realms-existing");
            
            log(Level.FINEST, "testRealmDeletion", "description: " + 
                    description);
            log(Level.FINEST, "testRealmDeletion", "use-debug-option: " + 
                    useDebugOption);
            log(Level.FINEST, "testRealmDeletion", "use-verbose-option: " + 
                    useVerboseOption);
            log(Level.FINEST, "testRealmDeletion", "use-long-options: " + 
                    useLongOptions);
            log(Level.FINEST, "testRealmDeletion", "use-recursive-option: " + 
                    useRecursiveOption);
            log(Level.FINEST, "testRealmDeletion", "expected-exit-code: " + 
                    expectedExitCode);
            log(Level.FINEST, "testRealmDeletion", "delete-realm: " + 
                    realmToDelete);
            log(Level.FINEST, "testRealmDeletion", "existing-realms: " + 
                    realmsExisting);
            log(Level.FINEST, "testRealmDeletion", "realms-deleted: " + 
                    realmsDeleted);

            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);            
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);
            Reporter.log("RealmToDelete: " + realmToDelete);
            Reporter.log("RealmsRemaining: " + realmsExisting);
            Reporter.log("RealmsDeleted: " + realmsDeleted);
            
            int commandStatus = cli.deleteRealm(realmToDelete, 
                    useRecursiveOption);
            cli.logCommand("testRealmDeletion");

            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                stringsFound = cli.findStringsInOutput(expectedMessage, ";");
                log(Level.FINEST, "testRealmDeletion", 
                        "Output Messages Found: " + stringsFound);
            } else {
                if (expectedExitCode.equals(
                        new Integer(INVALID_OPTION_STATUS).toString())) {
                    String argString = cli.getAllArgs().replaceFirst(
                        cli.getCliPath(), "ssoadm ");
                    Object[] params = {argString};
                    String usageError = 
                            MessageFormat.format(expectedMessage, params);
                    expectedMessage = usageError;
                }
                stringsFound = cli.findStringsInError(expectedMessage, ";");
                log(Level.FINEST, "testRealmDeletion", 
                        "Error Messages Found: " + stringsFound);
            }
            cli.resetArgList();
            
            log(Level.FINEST, "testRealmDeletion", "message-to-find: " + 
                    expectedMessage);
            Reporter.log("ExpectedMessage: " + expectedMessage);
         
            if ((realmsDeleted != null) && (realmsDeleted.length() > 0)) {
                removedRealmsFound = cli.findRealms(realmsDeleted);
                cli.resetArgList();
            }
            
            if ((realmsExisting != null) && (realmsExisting.length() > 0)) {
                FederationManagerCLI listCLI = 
                    new FederationManagerCLI(useDebugOption, useVerboseOption, 
                        useLongOptions);
                existingRealmsFound = listCLI.findRealms(realmsExisting);
            }            
                         
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                assert (commandStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound &&
                        !removedRealmsFound && existingRealmsFound;
            } else {
                assert (commandStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound &&
                        existingRealmsFound;
            }
            
            exiting("testRealmDeletion");
        } catch (Exception e) {
            log(Level.SEVERE, "testRealmDeletion", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        } 
    }
    
    /**
     * This method remove any realms that were created during the setup and
     * testRealmDeletion methods using "ssoadm delete-realm".
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
            log(Level.FINEST, "cleanup", "realmsDeleted: " + realmsDeleted);
            
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("RealmDeleted: " + realmsDeleted);
            
            if (!realmsExisting.equals("")) {
                String[] realms = realmsExisting.split(";");
                FederationManagerCLI cleanupCli = 
                        new FederationManagerCLI(useDebugOption, 
                        useVerboseOption, useLongOptions);
                if (!expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                    log(Level.FINEST, "cleanup", "All setup realms will be " +
                            "deleted in cleanup");
                    realmToDelete = "";
                }
                for (int i=realms.length-1; i >= 0; i--) {             
                    log(Level.FINEST, "cleanup", "setupRealmToDelete: " + 
                            realms[i]);
                    Reporter.log("SetupRealmToDelete: " + realms[i]);
                    if (!realms[i].equals(realmToDelete)) {
                        int exitStatus = cleanupCli.deleteRealm(realms[i], 
                                true); 
                        cleanupCli.logCommand("cleanup");
                        cleanupCli.resetArgList();
                        if (exitStatus != SUCCESS_STATUS) {
                            log(Level.SEVERE, "cleanup", 
                                    "Realm deletion returned the failed exit " +
                                    "status " + exitStatus + ".");
                            assert false;
                        }
                        FederationManagerCLI listCli = 
                                new FederationManagerCLI(useDebugOption, 
                                useVerboseOption, useLongOptions);
                        if (listCli.findRealms(realms[i])) {
                            log(Level.SEVERE, "cleanup", "Deleted realm " + 
                                    realms[i] + " still exists.");
                            assert false;
                        }
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

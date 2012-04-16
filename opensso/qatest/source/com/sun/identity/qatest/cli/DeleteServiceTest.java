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
 * $Id: DeleteServiceTest.java,v 1.2 2009/01/26 23:49:31 nithyas Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */

/**
 * DeleteServiceTest automates the following test cases:
 * CLI_Delete-svc01, CLI_Delete-svc02, CLI_Delete-svc03 
 * CLI_Delete-svc04, CLI_Delete-svc05, CLI_Delete-svc06 
 * CLI_Delete-svc07, CLI_Delete-svc08, CLI_Delete-svc09 
 * CLI_Delete-svc10
 */ 

package com.sun.identity.qatest.cli;

import com.sun.identity.qatest.common.cli.CLIExitCodes;
import com.sun.identity.qatest.common.cli.FederationManagerCLI;
import com.sun.identity.qatest.common.TestCommon;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.List;
import java.util.logging.Level;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.Reporter;

/**
 * <code>DeleteServiceTest</code> is used to execute tests involving the 
 * delete-svc sub-command of ssoadm.  This class allows the user 
 * to execute "ssoadm delete-svc" with a variety of arguments (e.g with short 
 * or long options, with a password file or password argument, with a locale 
 * argument, etc.) and a variety of input values. The properties file 
 * <code>CreateServiceTest.properties</code> contains the input values 
 * which are read by this class.
 */
public class DeleteServiceTest extends TestCommon implements CLIExitCodes {
    
    private String locTestName;
    private ResourceBundle rb;
    private boolean useVerboseOption;
    private boolean useDebugOption;
    private boolean useLongOptions;
    private String expectedMessage;
    private String expectedExitCode;
    private String description;
    private String serviceToDelete;
    private List createdServiceList;
    private boolean useRecursiveOption;
    private boolean deletePolicyRule;
    private FederationManagerCLI cli;    
    
    /** 
     * Creates a new instance of CreateServiceTest 
     */
    public DeleteServiceTest() {
        super("DeleteServiceTest");
    }
    
    /**
     * This method is intended to provide initial setup.
     * Creates any services specified in the setup-services property in the 
     * DeleteServiceTest.properties.
     */
    @Parameters({"testName"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String testName) 
    throws Exception {
        Object[] params = {testName};
        createdServiceList = new ArrayList();        
        entering("setup", params);
        try {
            locTestName = testName;
            rb = ResourceBundle.getBundle("cli" + fileseparator +
            		"DeleteServiceTest");
            useVerboseOption = ((String)rb.getString(locTestName + 
                    "-use-verbose-option")).equals("true");
            useDebugOption = ((String)rb.getString(locTestName + 
                    "-use-debug-option")).equals("true");
            useLongOptions = ((String)rb.getString(locTestName + 
                    "-use-long-options")).equals("true");
            serviceToDelete = (String) rb.getString(locTestName + 
                    "-delete-service");
            useRecursiveOption = ((String) rb.getString(locTestName + 
                    "-continue-deleting-service")).equals("true");
            expectedExitCode = (String) rb.getString(locTestName + 
            "-expected-exit-code");
            
            log(Level.FINEST, "setup", "use-verbose-option: " + 
                    useVerboseOption);
            log(Level.FINEST, "setup", "use-debug-option: " + useDebugOption);
            log(Level.FINEST, "setup", "use-long-options: " + useLongOptions);

            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            cli = new FederationManagerCLI(useDebugOption, useVerboseOption, 
                    useLongOptions);
            
            if (!serviceToDelete.trim().equals("")) {
                // create the setup services.
                int exitStatus = cli.createService(serviceToDelete, 
                    "10", true, useRecursiveOption);
                cli.logCommand("setup");
                if (exitStatus == SUCCESS_STATUS) {
                    // adding service(s) to created services list
                    String[] serviceStrings = serviceToDelete.split(",");
                    for (int i=0; i < serviceStrings.length; i++) {
                        createdServiceList.add(serviceStrings[i]);
                    }
                    log(Level.FINEST, "testServiceDeletion", "Created services" +
                        "list is: " + createdServiceList.toString());
                }
                if (exitStatus != SUCCESS_STATUS ) {
                    if(!expectedExitCode.equals(
                            new Integer(exitStatus).toString())) {
                        log(Level.FINEST, "setup", "Failed to create the setup " +
                        "services: " + serviceToDelete);
                        assert false;
                    }
                }
                cli.resetArgList();
            }
            exiting("setup");
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        } 
    }

    /**
     * This method is used to execute tests involving "ssoadm delete-service"
     * using input data from the DeleteServiceTest.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testServiceDeletion() 
    throws Exception {
        entering("testServiceDeletion", null);
        boolean stringsFound = false;
        try {
            description = (String) rb.getString(locTestName + "-description");
            useRecursiveOption = ((String)rb.getString(locTestName + 
                    "-continue-deleting-service")).equals("true");
            expectedMessage = (String) rb.getString(locTestName + 
                    "-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-expected-exit-code");
            deletePolicyRule = ((String) rb.getString(locTestName + 
                    "-delete-policy-rule")).equals("true");
            
            log(Level.FINEST, "testServiceDeletion", "description: " + 
                    description);
            log(Level.FINEST, "testServiceDeletion", "delete-service(s): " + 
                    serviceToDelete);
            log(Level.FINEST, "testServiceDeletion", "use-debug-option: " + 
                    useDebugOption);
            log(Level.FINEST, "testServiceDeletion", "use-verbose-option: " + 
                    useVerboseOption);
            log(Level.FINEST, "testServiceDeletion", "use-long-options: " + 
                    useLongOptions);
            log(Level.FINEST, "testServiceDeletion", "continue-deleting-" +
                    "service: " + useRecursiveOption);
            log(Level.FINEST, "testServiceDeletion", "expected-exit-code: " + 
                    expectedExitCode);
            log(Level.FINEST, "testServiceDeletion", "delete-ploicy-rule: " +
                    deletePolicyRule);
            log(Level.FINEST, "testServiceDeletion", "message-to-find: " + 
                    expectedMessage);

            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);            
            Reporter.log("ServiceToDelete: " + serviceToDelete);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);
            Reporter.log("DeletePolicyRule: " + deletePolicyRule);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            
            // delete the services created in setup.
            int commandStatus = cli.deleteService(serviceToDelete, 
                    useRecursiveOption, deletePolicyRule);
            cli.logCommand("testServiceDeletion");
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                stringsFound = cli.
                        findStringsInOutput(expectedMessage, ";");
                log(Level.FINEST, "testServiceDeletion", 
                        "Output Messages Found: " + stringsFound);
                assert (commandStatus ==
                        new Integer(expectedExitCode).intValue()) &&
                        stringsFound;
            } else {
                if (expectedExitCode.equals(
                        new Integer(INVALID_OPTION_STATUS).toString())) {
                    String argString = cli.
                            getAllArgs().replaceFirst(
                            cli.getCliPath(), "ssoadm ");
                    Object[] params = {argString};
                    String usageError = 
                            MessageFormat.format(expectedMessage, params);
                    expectedMessage = usageError;
                }
                stringsFound = cli.
                        findStringsInError(expectedMessage, ";");
                log(Level.FINEST, "testServiceDeletion", 
                        "Error Messages Found: " + stringsFound);
                
                assert (commandStatus ==
                    new Integer(expectedExitCode).intValue()) && stringsFound;
            }
            cli.resetArgList();
            
            if (!serviceToDelete.trim().equals("")) {
                // verify if service exists by using "ssoadm get-revision-number"
                for (int i=0; i < createdServiceList.size(); i++) {
                    commandStatus = cli.
                        getRevisionNumber((String)createdServiceList.get(i));
                    cli.logCommand("testServiceDeletion");
                    if (commandStatus == SUCCESS_STATUS) {
                        if (expectedExitCode.equals(new Integer(
                                INVALID_OPTION_STATUS).toString()) 
                                || expectedExitCode.trim().equals("127"))  {
                            log(Level.FINEST, "testServiceDeletion", "Clean " +
                                    "Services " +createdServiceList.get(i) + 
                                    " faailed to get deleted.");                            
                            cli.resetArgList();
                            commandStatus = cli.deleteService(serviceToDelete, 
                                   true, false);
                            cli.logCommand("testServiceDeletion");
                            cli.resetArgList();
                        } else {
                            log(Level.FINEST, "testServiceDeletion", "Service " +
                                createdServiceList.get(i) + " not deleted.");
                            log(Level.FINEST, "testServiceDeletion",
                                cli.getCommand().getOutput());                            
                            cli.resetArgList();
                            assert false;
                        }
                    } else {
                        log(Level.FINEST, "testServiceDeletion",
                            "Service was deleted successfully");
                        cli.resetArgList();
                    }
                }
            }
            exiting("testServiceDeletion");
        } catch (Exception e) {
            log(Level.SEVERE, "testServiceDeletion", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        } 
    }
        
    /**
     * This method remove any Services that were created during the setup and
     * testServiceDeletion methods using "ssoadm delete-Service".
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
            exiting("cleanup");
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        } 
    }
}


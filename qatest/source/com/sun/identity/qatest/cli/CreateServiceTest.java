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
 * $Id: CreateServiceTest.java,v 1.2 2009/01/26 23:48:58 nithyas Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */

/**
 * CreateServiceTest automates the following test cases:
 * CLI_create-svc01, CLI_create-svc02, CLI_create-svc03
 * CLI_create-svc04, CLI_create-svc05, CLI_create-svc06
 * CLI_create-svc07, CLI_create-svc08, CLI_create-svc09
 * CLI_create-svc10
 */

package com.sun.identity.qatest.cli;

import com.sun.identity.qatest.common.cli.CLIExitCodes;
import com.sun.identity.qatest.common.cli.FederationManagerCLI;
import com.sun.identity.qatest.common.TestCommon;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.List;
import java.util.ArrayList;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.Reporter;

/**
 * <code>CreateServiceTest</code> is used to execute tests involving the
 * create-svc sub-command of ssoadm.  This class allows the user
 * to execute "ssoadm create-svc" with a variety of arguments (e.g with short
 * or long options, with a password file or password argument, with a locale
 * argument, etc.) and a variety of input values. The properties file
 * <code>CreateServiceTest.properties</code> contains the input values
 * which are read by this class.
 */
public class CreateServiceTest extends TestCommon implements CLIExitCodes {

    private String locTestName;
    private ResourceBundle rb;
    private String createService;
    private boolean validServiceXml;
    private String revisionNumber;
    private boolean useVerboseOption;
    private boolean useDebugOption;
    private boolean useLongOptions;
    private String expectedMessage;
    private String expectedExitCode;
    private String description;
    private List createdServiceList;
    private boolean continueAddingService;
    private FederationManagerCLI cli;

    /**
     * Creates a new instance of CreateServiceTest
     */
    public CreateServiceTest() {
        super("CreateServiceTest");
    }

    /**
     * This method is intended to provide initial setup.
     * setup loads the properties in CreateServiceTest.properties.
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
            		"CreateServiceTest");
            useVerboseOption = ((String)rb.getString(locTestName +
                    "-use-verbose-option")).equals("true");
            useDebugOption = ((String)rb.getString(locTestName +
                    "-use-debug-option")).equals("true");
            useLongOptions = ((String)rb.getString(locTestName +
                    "-use-long-options")).equals("true");
            continueAddingService = ((String)rb.getString(locTestName +
                    "-continue-adding-service")).equals("true");

            log(Level.FINEST, "setup", "use-verbose-option: " +
                    useVerboseOption);
            log(Level.FINEST, "setup", "use-debug-option: " + useDebugOption);
            log(Level.FINEST, "setup", "use-long-options: " + useLongOptions);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("UseRecursiveOptions: " + continueAddingService);
                        
            cli = new FederationManagerCLI(useDebugOption, useVerboseOption, 
                    useLongOptions);
            exiting("setup");
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * This method is used to execute tests involving "ssoadm create-svc"
     * using input data from the CreateServiceTest.properties file and verifies
     * service creation using "ssoadm get-revision-number".
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testServiceCreation()
    throws Exception {
        entering("testServiceCreation", null);
        boolean stringsFound = false;

        try {
            description = (String) rb.getString(locTestName + "-description");
            createService = (String) rb.getString(locTestName +
                    "-create-service");
            validServiceXml = ((String) rb.getString(locTestName +
                    "-use-valid-service-xml")).equals("true");
            revisionNumber = (String) rb.getString(locTestName +
                    "-init-revision-number");
            expectedMessage = (String) rb.getString(locTestName +
                    "-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName +
                    "-expected-exit-code");

            log(Level.FINEST, "testServiceCreation", "description: " +
                    description);
            log(Level.FINEST, "testServiceCreation", "use-debug-option: " +
                    useDebugOption);
            log(Level.FINEST, "testServiceCreation", "use-verbose-option: " +
                    useVerboseOption);
            log(Level.FINEST, "testServiceCreation", "use-long-options: " +
                    useLongOptions);
            log(Level.FINEST, "testServiceCreation", "message-to-find: " +
                    expectedMessage);
            log(Level.FINEST, "testServiceCreation", "use-recursive-option: " +
                    continueAddingService);
            log(Level.FINEST, "testServiceCreation", "expected-exit-code: " +
                    expectedExitCode);

            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);
            Reporter.log("UserRecursiveOption: " + continueAddingService);

            log(Level.FINEST, "testServiceCreation", "Service(s)ToCreate: " +
                        createService + " with revision number(s) " +
                        revisionNumber);
            int commandStatus = cli.createService(
                        createService, revisionNumber,
                        validServiceXml, continueAddingService);
            cli.logCommand("testServiceCreation");

            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                    stringsFound = cli.
                            findStringsInOutput(expectedMessage, ";");
                    log(Level.FINEST, "testServiceCreation", "Service(s) " +
                        createService + " with revision number(s) " +
                        revisionNumber + " is created. ");
                    
                    // adding service(s) to created services list
                    String[] serviceStrings = createService.split(",");
                    for (int i=0; i < serviceStrings.length; i++) {
                        createdServiceList.add(serviceStrings[i]);
                    }
                    log(Level.FINEST, "testServiceCreation", "Created services" +
                            "list is: " + createdServiceList.toString());
                    assert (commandStatus ==
                    new Integer(expectedExitCode).intValue()) &&
                        stringsFound;
                } else {
                if (!expectedExitCode.equals(
                        new Integer(INVALID_OPTION_STATUS).toString())) {
                    stringsFound =
                       cli.findStringsInError(expectedMessage, ";");
                } else {
                    String argString = cli.getAllArgs().
                          replaceFirst(cli.getCliPath(), "ssoadm ");
                    Object[] params = {argString};
                    String usageError = MessageFormat.format(expectedMessage,
                            params);
                    stringsFound = cli.
                            findStringsInError(usageError, ";" + newline);
                }
                log(logLevel, "testServiceCreation", "Error Messages Found: " +
                        stringsFound);
                assert (commandStatus ==
                    new Integer(expectedExitCode).intValue()) && stringsFound;
                }
                cli.resetArgList();

                // verify the created service by "ssoadm get-revision-number"
                for (int i=0; i < createdServiceList.size(); i++) {
                     commandStatus = cli.
                            getRevisionNumber((String)createdServiceList.get(i));
                     cli.logCommand("testServiceCreation");
                    if (commandStatus == SUCCESS_STATUS) {
                        log(Level.FINEST, "testServiceCreation", "Service " +
                                createdServiceList.get(i) + " found.");
                        log(Level.FINEST, "testServiceCreation",
                                cli.getCommand().getOutput());
                    } else {
                        log(Level.FINEST, "testServiceCreation",
                                "Failed to get Service, Revision number");
                        assert false;
                    }
                    cli.resetArgList();
                }
                exiting("testServiceCreation");
        } catch (Exception e) {
            log(Level.SEVERE, "testServiceCreation", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * This method remove any Services that were created during the setup and
     * testServiceCreation methods using "ssoadm delete-svc".
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

            if (!createService.trim().equals("")) {
                if (createdServiceList.size() > 0) {
                    FederationManagerCLI cli =
                            new FederationManagerCLI(useDebugOption,
                    useVerboseOption, useLongOptions);
                    int exitStatus = cli.deleteService(
                            createService, continueAddingService, false);
                    cli.logCommand("cleanup");
                    if (exitStatus != SUCCESS_STATUS){
                        log(Level.SEVERE, "cleanup",
                            "Service deletion returned the failed exit " +
                            "status " + exitStatus + ".");
                        assert false;
                    }
                    cli.resetArgList();
                }
                createdServiceList.clear();
            }
            exiting("cleanup");
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        }
    }
}

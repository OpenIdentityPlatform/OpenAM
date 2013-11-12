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
 * $Id: GenericCLITest.java,v 1.6 2009/01/26 23:49:32 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.cli;

import com.sun.identity.qatest.common.cli.CLIExitCodes;
import com.sun.identity.qatest.common.cli.FederationManagerCLI;
import com.sun.identity.qatest.common.TestCommon;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.Reporter;

/**
 * <code>GenericCLITest</code> is used to execute tests involving the 
 * delete-identities sub-command of ssoadm.  This class allows the user to 
 * execute an ssoadm command by specifying a subcommand and a list of 
 * arguments. Properties files named GenericCLITest_*.properties contain the 
 * input values which are read by this class.
 *
 * This class automates the following test cases:
 * CLI_show-identity-types01, CLI_show-identity-types02, 
 * CLI_show-identity-types03, CLI_show-identity-types04,
 * CLI_show-identity-types05, CLI_show-identity-types06,
 * CLI_show-identity-operations01, CLI_show-identity-operations02,
 * CLI_show-identity-operations03, CLI_show-identity-operations04,
 * CLI_show-identity-operations05, CLI_show-identity-operations06,
 * CLI_show-identity-operations07, CLI_show-identity-operations08,
 * CLI_show-identity-operations09, CLI_show-identity-operations10,
 */

public class GenericCLITest extends TestCommon implements CLIExitCodes {
    
    private String locTestName;
    private ResourceBundle rb;
    private boolean useVerboseOption;
    private boolean useDebugOption;
    private boolean useLongOptions;
    private String subcommand;
    private String argList;
    private String expectedMessage;
    private String expectedErrorMessage;
    private String expectedExitCode;
    private String description;
    private FederationManagerCLI cli;
    
    /** 
     * Creates a new instance of GenericCLITest 
     */
    public GenericCLITest() {
        super("GenericCLITest");
    }
    
    /**
     * This method is intended to provide initial setup.
     * Creates a <code>FederationManagerCLI</code> object. 
     */
    @Parameters({"testName", "propFile"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String testName, String propFile) 
    throws Exception {
        Object[] params = {testName, propFile};
        entering("setup", params);
        try {
            locTestName = testName;
            rb = ResourceBundle.getBundle("cli" + fileseparator + propFile);

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
                                
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            
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
     * This method is used to execute tests with the subcommand and arguments
     * from the GenericCLITest_*.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testGenericCLICommand() 
    throws Exception {
        entering("testGenericCLICommand", null);
        boolean stringsFound = false;
        boolean errorFound = false;
        int commandStatus = -1;
        
        try {
            description = (String) rb.getString(locTestName + "-description");
            subcommand = (String) rb.getString("subcommand");
            argList = (String) rb.getString(locTestName + "-arg-list");
            expectedMessage = (String) rb.getString(locTestName + 
                    "-message-to-find");
            expectedErrorMessage = (String) rb.getString(locTestName + 
                    "-error-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-expected-exit-code");
 
            log(Level.FINEST, "testGenericCLICommand", "description: " + 
                    description);
            log(Level.FINEST, "testGenericCLICommand", "use-debug-option: " + 
                    useDebugOption);
            log(Level.FINEST, "testGenericCLICommand", "use-verbose-option: " + 
                    useVerboseOption);
            log(Level.FINEST, "testGenericCLICommand", "use-long-options: " + 
                    useLongOptions);
            log(Level.FINEST, "testGenericCLICommand", "subcommand: " + 
                    subcommand);
            log(Level.FINEST, "testGenericCLICommand", "arg-list: " + 
                    argList);
            log(Level.FINEST, "testGenericCLICommand", "message-to-find: " + 
                    expectedMessage);
            log(Level.FINEST, "testGenericCLICommand", "error-message-to-find: "
                    + expectedErrorMessage);
            log(Level.FINEST, "testGenericCLICommand", "expected-exit-code: " + 
                    expectedExitCode);

            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("Subcommand: " + subcommand);
            Reporter.log("ArgumentList: " + argList);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedErrorMessage: " + expectedErrorMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);

            commandStatus = cli.executeCommand(subcommand, argList);
            cli.logCommand("testGenericCLICommand");
            cli.resetArgList();
                               
            log(Level.FINEST, "testGenericCLICommand", "Exit status: " + 
                    commandStatus);
            
            stringsFound = cli.findStringsInOutput(expectedMessage, ";");
            log(Level.FINEST, "testGenericCLICommand", 
                        "Output Messages Found: " + stringsFound);
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                assert (commandStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound;
            } else {
                errorFound = cli.findStringsInError(expectedMessage, ";");                
                log(Level.FINEST, "testGenericCLICommand", 
                        "Error Messages Found: " + errorFound);
                assert (commandStatus == 
                        new Integer(expectedExitCode).intValue()) && 
                        stringsFound && errorFound;
            }   
            exiting("testGenericCLICommand");
        } catch (Exception e) {
            log(Level.SEVERE, "testGenericCLICommand", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        } 
    }
    
    /**
     * This method is a placeholder.  Does not perform any cleanup activities.
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup() {
        entering("cleanup", null);
        exiting("cleanup");
    }
}

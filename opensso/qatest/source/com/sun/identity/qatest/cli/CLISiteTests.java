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
 * $Id: CLISiteTests.java,v 1.1 2009/06/24 22:21:40 srivenigan Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.cli;

import com.sun.identity.qatest.common.cli.CLIExitCodes;
import com.sun.identity.qatest.common.cli.FederationManagerCLI;
import com.sun.identity.qatest.common.TestCommon;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.Reporter;

/**
 * <code>CLISiteTests</code> executes tests involving create-site, delete-site,
 * show-site, list-sites sub-commands of ssoadm.  This class allows the user
 * to execute an ssoadm command by specifying a subcommand and a list of 
 * arguments. Properties file named CLISiteTests.properties contain the 
 * input values which are read by this class.
 *
 * This class automates the following test cases:
 * CLI_site_test01, CLI_site_test02, CLI_site_test03, 
 * CLI_site_test04, CLI_site_test05, CLI_site_test06, 
 * CLI_site_test07, CLI_site_test08, CLI_site_test09, 
 * CLI_site_test10, CLI_site_test11, CLI_site_test12,
 * CLI_site_test13, CLI_site_test14, CLI_site_test15,
 * CLI_site_test16, CLI_site_test17, CLI_site_test18,
 * CLI_site_test19, CLI_site_test20, CLI_site_test21,
 * CLI_site_test22, CLI_site_test23, CLI_site_test24,
 * CLI_site_test25
 * 
 * CLI_site_test$ includes create-site, show-site,
 * list-sites and delete-site sub commands.
 */

public class CLISiteTests extends TestCommon implements CLIExitCodes {
    
    private String locTestName;
    private ResourceBundle rb;
    private boolean useVerboseOption;
    private boolean useDebugOption;
    private boolean useLongOptions;
    private String createSiteArgs;
    private String setupSites;
    private String showSite;
    private String deleteSite;
    private String expectedMessage;
    private String expectedErrorMessage;
    private String expectedExitCode;
    private String description;
    private FederationManagerCLI cli;
    
    /** 
     * Creates a new instance of CLISiteTests
     */
    public CLISiteTests() {
        super("CLISiteTests");
    }
    
    /**
     * This method is intended to provide initial setup.
     * Creates a <code>FederationManagerCLI</code> object. 
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
                    "CLISiteTests");
            description = (String)rb.getString(locTestName + "-description");
            setupSites = (String)rb.getString(locTestName + "-setup-sites");
            useVerboseOption = ((String)rb.getString(locTestName + 
                    "-use-verbose-option")).equals("true");
            useDebugOption = ((String)rb.getString(locTestName + 
                    "-use-debug-option")).equals("true");
            useLongOptions = ((String)rb.getString(locTestName + 
                    "-use-long-options")).equals("true");
                
            log(Level.FINEST, "setup", "description: " + description);
            log(Level.FINEST, "setup", "setup-sites: " + setupSites);            
            log(Level.FINEST, "setup", "use-verbose-option: " + 
                    useVerboseOption);
            log(Level.FINEST, "setup", "use-debug-option: " + useDebugOption);
            log(Level.FINEST, "setup", "use-long-options: " + useLongOptions);
            Reporter.log("SetupSites: " + setupSites);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            
            cli = new FederationManagerCLI(useDebugOption, useVerboseOption, 
                    useLongOptions);
            if(setupSites != null && !setupSites.equals("")) {
                if (!cli.createSites(setupSites)){
                    log(Level.FINEST, "setup", "All sites failed to get " +
                            "created.");
            		assert false;
            	}
            }
            exiting("setup");
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage(), null);
            cleanup();
            e.printStackTrace();
            throw e;
        } 
    }
    
    /**
     * This method is used to execute tests with the subcommand and arguments
     * from the CLISiteTests.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testCreateSiteCommand() 
    throws Exception {
        entering("testCreateSiteCommand", null);
        boolean stringsFound = false;
        boolean errorFound = false;
        int commandStatus = -1;
        
        try {
            createSiteArgs = (String) rb.getString(locTestName + 
                    "-create-site-args");
            expectedMessage = (String) rb.getString(locTestName + 
                    "-create-expected-message");
            expectedErrorMessage = (String) rb.getString(locTestName + 
                    "-create-error-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-create-expected-exit-code");
 
            log(Level.FINEST, "testCreateSiteCommand", "site name: " + 
                    createSiteArgs);
            log(Level.FINEST, "testCreateSiteCommand", 
                    "create-message-to-find: " + expectedMessage);
            log(Level.FINEST, "testCreateSiteCommand", 
                    "create-error-message-to-find: " + expectedErrorMessage);
            log(Level.FINEST, "testCreateSiteCommand", 
                    "create-expected-exit-code: " + expectedExitCode);
            Reporter.log("TestName: " + locTestName);
            Reporter.log("SiteArgumentList: " + createSiteArgs);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedErrorMessage: " + expectedErrorMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);

            if (createSiteArgs != null) {
                String[] siteArgs = createSiteArgs.split(";");
                String siteName = "";
                String sitePrimaryUrl = "";
                String secondaryUrls = "";
                if (siteArgs.length >= 1) {
                    siteName = siteArgs[0];
                    if (siteArgs.length >= 2) {
                        sitePrimaryUrl = siteArgs[1];
                        if (siteArgs.length > 2) {
                            secondaryUrls = siteArgs[2];
                        }
                    }
                    log(Level.FINEST, "testCreateSiteCommand", "Creating site " 
                            + "with args:" + siteName + ", " + sitePrimaryUrl + 
                            ", " + secondaryUrls);
                    commandStatus = cli.createSite(siteName, sitePrimaryUrl, 
                            secondaryUrls);
                } else {
                    log(Level.FINEST, "testCreateSiteCommand", "Attempt to " +
                            "create site with no arguments");
                    commandStatus = cli.createSite("", "", "");
                }
            }
            cli.logCommand("testCreateSiteCommand");
            cli.resetArgList();
                               
            log(Level.FINEST, "testCreateSiteCommand", "Exit status: " + 
                    commandStatus);
            
            stringsFound = cli.findStringsInOutput(expectedMessage, ";");
            log(Level.FINEST, "testCreateSiteCommand", 
                        "Output Messages Found: " + stringsFound);
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                assert (commandStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound;
            } else {
                errorFound = cli.findStringsInError(expectedErrorMessage, ";");                
                log(Level.FINEST, "testCreateSiteCommand", 
                        "Error Messages Found: " + errorFound);
                assert (commandStatus == 
                        new Integer(expectedExitCode).intValue()) && 
                        stringsFound && errorFound;
            }   
            exiting("testCreateSiteCommand");
        } catch (Exception e) {
            log(Level.SEVERE, "testCreateSiteCommand", e.getMessage(), null);
            cleanup();
            e.printStackTrace();
            throw e;
        } 
    }

    /**
     * This method is used to execute tests with the subcommand and arguments
     * from the CLISiteTests.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testShowSiteCommand() 
    throws Exception {
        entering("testShowSiteCommand", null);
        boolean stringsFound = false;
        boolean errorFound = false;
        int exitStatus = -1;
        
        try {
            showSite = (String) rb.getString(locTestName + "-show-site-name");
            expectedMessage = (String) rb.getString(locTestName + 
                    "-show-expected-message");
            expectedErrorMessage = (String) rb.getString(locTestName + 
                    "-show-error-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-show-expected-exit-code");
 
            log(Level.FINEST, "testShowSiteCommand", "show-site: " + 
                    showSite);
            log(Level.FINEST, "testShowSiteCommand", "show-expected-message: " + 
                    expectedMessage);
            log(Level.FINEST, "testShowSiteCommand", 
                    "show-error-message-to-find: " + expectedErrorMessage);
            log(Level.FINEST, "testShowSiteCommand", 
                    "show-expected-exit-code: " + expectedExitCode);

            Reporter.log("TestName: " + locTestName);
            Reporter.log("ShowSiteName: " + showSite);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedErrorMessage: " + expectedErrorMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);

            FederationManagerCLI showCLI = new FederationManagerCLI(
                    useDebugOption, useVerboseOption, useLongOptions);
            exitStatus = showCLI.showSite(showSite);
            showCLI.logCommand("testShowSiteCommand");
            log(Level.FINEST, "testShowSiteCommand", "Exit status: " + 
                    exitStatus);
            
            stringsFound = showCLI.findStringsInOutput(expectedMessage, ";");
            log(Level.FINEST, "testShowSiteCommand", 
                        "Output Messages Found: " + stringsFound);
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                assert (exitStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound;
            } else {
                errorFound = showCLI.findStringsInError(expectedMessage, ";");                
                log(Level.FINEST, "testShowSiteCommand", 
                        "Error Messages Found: " + errorFound);
                assert (exitStatus == 
                        new Integer(expectedExitCode).intValue()) && 
                        stringsFound && errorFound;
            }   
            showCLI.resetArgList();            
            exiting("testShowSiteCommand");
        } catch (Exception e) {
            log(Level.SEVERE, "testShowSiteCommand", e.getMessage(), null);
            cleanup();            
            e.printStackTrace();
            throw e;
        } 
    }

    /**
     * This method is used to execute tests with the subcommand and arguments
     * from the CLISiteTests.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testListSitesCommand() 
    throws Exception {
        entering("testListSitesCommand", null);
        boolean stringsFound = false;
        boolean errorFound = false;
        int exitStatus = -1;
        
        try {
            expectedMessage = (String) rb.getString(locTestName + 
                    "-list-expected-message");
            expectedErrorMessage = (String) rb.getString(locTestName + 
                    "-list-error-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-list-expected-exit-code");
                    
            log(Level.FINEST, "testListSitesCommand", "list-expected-message: " 
                    + expectedMessage);
            log(Level.FINEST, "testListSitesCommand", 
                    "list-error-message-to-find: " + expectedErrorMessage);
            log(Level.FINEST, "testListSitesCommand", 
                    "list-expected-exit-code: " + expectedExitCode);
            Reporter.log("TestName: " + locTestName);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedErrorMessage: " + expectedErrorMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);

            FederationManagerCLI listSitesCLI = new FederationManagerCLI(
                    useDebugOption, useVerboseOption, useLongOptions);
            log(Level.FINEST, "testListSitesCommand", "List sites available: ");
            exitStatus = listSitesCLI.listSites();
            listSitesCLI.logCommand("testListSitesCommand");
            log(Level.FINEST, "testListSitesCommand", "Exit status: " + 
                    exitStatus);
            
            stringsFound = listSitesCLI.findStringsInOutput(expectedMessage, 
                    ";");
            log(Level.FINEST, "testListSitesCommand", 
                        "Output Messages Found: " + stringsFound);
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                assert (exitStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound;
            } else {
                errorFound = listSitesCLI.findStringsInError(expectedMessage,
                        ";");                
                log(Level.FINEST, "testListSitesCommand", 
                        "Error Messages Found: " + errorFound);
                assert (exitStatus == 
                        new Integer(expectedExitCode).intValue()) && 
                        stringsFound && errorFound;
            }
            listSitesCLI.resetArgList();            
            exiting("testListSitesCommand");
        } catch (Exception e) {
            log(Level.SEVERE, "testListSitesCommand", e.getMessage(), null);
            cleanup();            
            e.printStackTrace();
            throw e;
        } 
    }    
    
    /**
     * This method is used to execute tests with the subcommand and arguments
     * from the CLISiteTests.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testDeleteSiteCommand() 
    throws Exception {
        entering("testDeleteSiteCommand", null);
        boolean stringsFound = false;
        boolean errorFound = false;
        int exitStatus = -1;
        
        try {
            deleteSite = (String) rb.getString(locTestName + 
                    "-delete-site-name");
            expectedMessage = (String) rb.getString(locTestName + 
                    "-delete-expected-message");
            expectedErrorMessage = (String) rb.getString(locTestName + 
                    "-delete-error-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-delete-expected-exit-code");
 
            log(Level.FINEST, "testDeleteSiteCommand", "delete-site-name: " +
                    deleteSite);
            log(Level.FINEST, "testDeleteSiteCommand", 
                    "delete-expected-message: " + expectedMessage);
            log(Level.FINEST, "testDeleteSiteCommand", 
                    "delete-error-message-to-find: " + expectedErrorMessage);
            log(Level.FINEST, "testDeleteSiteCommand", 
                    "delete-expected-exit-code: " + expectedExitCode);
            Reporter.log("TestName: " + locTestName);
            Reporter.log("DeleteSite: " + deleteSite);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedErrorMessage: " + expectedErrorMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);

            FederationManagerCLI deleteSiteCLI = new FederationManagerCLI(
                    useDebugOption, useVerboseOption, useLongOptions);            
            exitStatus = deleteSiteCLI.deleteSite(deleteSite);
            deleteSiteCLI.logCommand("testDeleteSiteCommand");
            log(Level.FINEST, "testDeleteSiteCommand", "Exit status: " + 
                    exitStatus);
            
            stringsFound = deleteSiteCLI.findStringsInOutput(expectedMessage, 
                    ";");
            log(Level.FINEST, "testDeleteSiteCommand", "Output Messages " +
                    "Found: " + stringsFound);
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                assert (exitStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound;
            } else {
                errorFound = deleteSiteCLI.findStringsInError(expectedMessage, 
                        ";");                
                log(Level.FINEST, "testDeleteSiteCommand", 
                        "Error Messages Found: " + errorFound);
                assert (exitStatus == 
                        new Integer(expectedExitCode).intValue()) && 
                        stringsFound && errorFound;
            }
            deleteSiteCLI.resetArgList();
            exiting("testDeleteSiteCommand");
        } catch (Exception e) {
            log(Level.SEVERE, "testDeleteSiteCommand", e.getMessage(), null);
            cleanup();            
            e.printStackTrace();
            throw e;
        } 
    }    
    
    /**
     * Cleanup removes all the sites being created in setup.
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup() 
    throws Exception {
        entering("cleanup", null);
        FederationManagerCLI deleteSitesCLI = 
            new FederationManagerCLI(useDebugOption, 
            useVerboseOption, useLongOptions);            
    	
    	if (setupSites != null && !setupSites.equals("")) {
            String[] sitesArgs = setupSites.split("\\|");
            String[] siteArgs = null;
            String siteList = "";
            for (String s : sitesArgs) {
                siteArgs = s.split(";");
                siteList += siteArgs[0] + ";" ;
            }
            siteList = siteList.substring(0, siteList.length()-1);
            if (!deleteSitesCLI.deleteSites(siteList)) {
                log(Level.SEVERE, "cleanup", "Failed to delete all the " +
                        "existing sites");
                assert false;
            } else {
                log(Level.FINEST, "cleanup", "All sites deleted successfully");
            }
        } else {
            log(Level.SEVERE, "cleanup", "Site list is empty.");
        }
        exiting("cleanup");
    }
}


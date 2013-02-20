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
 * $Id: CLISiteMemberTests.java,v 1.1 2009/06/24 22:27:54 srivenigan Exp $
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
 * <code>CLISiteMemberTests</code> executes tests involving add-site-members, 
 * show-site-members, remove-site-members sub-commands of ssoadm.  This class 
 * allows the user to execute an ssoadm command by specifying a subcommand and 
 * a list of arguments. Properties file named CLISiteMemberTests.properties 
 * contain the input values which are read by this class.
 *
 * This class automates the following test cases:
 * CLI_site_member_test01, CLI_site_member_test02, CLI_site_member_test03, 
 * CLI_site_member_test04, CLI_site_member_test05, CLI_site_member_test06, 
 * CLI_site_member_test07, CLI_site_member_test08, CLI_site_member_test09, 
 * CLI_site_member_test10, CLI_site_member_test11
 *  
 * CLI_site_member_test$ includes add-site-members, show-site-members,
 * and remove-site-members sub commands.
 */

public class CLISiteMemberTests extends TestCommon implements CLIExitCodes {
    
    private String locTestName;
    private ResourceBundle rb;
    private boolean useVerboseOption;
    private boolean useDebugOption;
    private boolean useLongOptions;
    private String addSiteMemberArgs;
    private String setupSites;
    private String showSiteMembers;
    private String removeSiteArgs;
    private String expectedMessage;
    private String expectedErrorMessage;
    private String expectedExitCode;
    private String description;
    
    /** 
     * Creates a new instance of CLISiteMemberTests
     */
    public CLISiteMemberTests() {
        super("CLISiteMemberTests");
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
                    "CLISiteMemberTests");

            description = (String) rb.getString(locTestName + "-description");
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
            
            FederationManagerCLI setupCLI = new FederationManagerCLI(
            		useDebugOption, useVerboseOption, useLongOptions);
            if(setupSites != null && !setupSites.equals("")) {
            	if (!setupCLI.createSites(setupSites)){
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
     * from the CLISiteMemberTests.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testAddSiteMembersCommand() 
    throws Exception {
        entering("testAddSiteMembersCommand", null);
        boolean stringsFound = false;
        boolean errorFound = false;
        int exitStatus = -1;
        
        try {
            addSiteMemberArgs = (String) rb.getString(locTestName + 
                    "-add-site-members-args");
            expectedMessage = (String) rb.getString(locTestName + 
                    "-add-site-members-expected-message");
            expectedErrorMessage = (String) rb.getString(locTestName + 
                    "-add-site-members-error-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-add-site-members-expected-exit-code");
 
            log(Level.FINEST, "testAddSiteMembersCommand", "site arguments: " + 
                    addSiteMemberArgs);
            log(Level.FINEST, "testAddSiteMembersCommand", 
                    "add-site-members-expected-message: " + expectedMessage);
            log(Level.FINEST, "testAddSiteMembersCommand", 
                    "add-site-members-error-message-to-find: " + 
                    expectedErrorMessage);
            log(Level.FINEST, "testAddSiteMembersCommand", 
                    "add-site-members-expected-exit-code: " + expectedExitCode);

            Reporter.log("TestName: " + locTestName);
            Reporter.log("SiteArgumentList: " + addSiteMemberArgs);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedErrorMessage: " + expectedErrorMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);

            FederationManagerCLI addSiteMembersCLI = new FederationManagerCLI(
                    useDebugOption, useVerboseOption, useLongOptions);
            String[] siteArgs = addSiteMemberArgs.split(";");
            if (siteArgs.length >= 1) {
            	String siteName = siteArgs[0];
            	String serverNames = "";
            	if (siteArgs.length > 1) {
            		serverNames = siteArgs[1]; 
            	}
                exitStatus = addSiteMembersCLI.addSiteMembers(siteName, 
                        serverNames);
            } else {
            	log(Level.FINEST, "setup", "Add site members argument list" +
                        " is empty");
            }
            addSiteMembersCLI.logCommand("testAddSiteMembersCommand");
            addSiteMembersCLI.resetArgList();
                               
            log(Level.FINEST, "testAddSiteMembersCommand", "Exit status: " + 
                    exitStatus);
            
            stringsFound = addSiteMembersCLI.findStringsInOutput(
                    expectedMessage, ";");
            log(Level.FINEST, "testAddSiteMembersCommand", 
                        "Output Messages Found: " + stringsFound);
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                assert (exitStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound;
            } else {
                errorFound = addSiteMembersCLI.findStringsInError(
                        expectedErrorMessage, ";");                
                log(Level.FINEST, "testAddSiteMembersCommand", 
                        "Error Messages Found: " + errorFound);
                assert (exitStatus == 
                        new Integer(expectedExitCode).intValue()) && 
                        stringsFound && errorFound;
            }   
            exiting("testAddSiteMembersCommand");
        } catch (Exception e) {
            log(Level.SEVERE, "testAddSiteMembersCommand", e.getMessage(), 
                    null);
            cleanup();
            e.printStackTrace();
            throw e;
        } 
    }

    /**
     * This method is used to execute tests with the subcommand and arguments
     * from the CLISiteMemberTests.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"testAddSiteMembersCommand"})
    public void testShowSiteMembersCommand() 
    throws Exception {
        entering("testShowSiteMembersCommand", null);
        boolean stringsFound = false;
        boolean errorFound = false;
        int exitStatus = -1;
        
        try {
            showSiteMembers = (String) rb.getString(locTestName + 
                    "-show-site-members-site-name");
            expectedMessage = (String) rb.getString(locTestName + 
                    "-show-site-members-expected-message");
            expectedErrorMessage = (String) rb.getString(locTestName + 
                    "-show-site-members-error-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-show-site-members-expected-exit-code");
 
            log(Level.FINEST, "testShowSiteMembersCommand", 
                    "show-site-members-site-name: " + showSiteMembers);
            log(Level.FINEST, "testShowSiteMembersCommand", 
                    "show-site-members-expected-message: " + expectedMessage);
            log(Level.FINEST, "testShowSiteMembersCommand", 
                    "show-site-members-error-message-to-find: " + 
                    expectedErrorMessage);
            log(Level.FINEST, "testShowSiteMembersCommand", 
                    "show-site-members-expected-exit-code: " + 
                    expectedExitCode);

            Reporter.log("TestName: " + locTestName);
            Reporter.log("ShowSiteName: " + showSiteMembers);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedErrorMessage: " + expectedErrorMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);

            FederationManagerCLI showSiteMembersCLI = 
                        new FederationManagerCLI(useDebugOption, 
                        useVerboseOption, useLongOptions);
            exitStatus = showSiteMembersCLI.showSiteMembers(showSiteMembers);
            showSiteMembersCLI.logCommand("testShowSiteMembersCommand");
            log(Level.FINEST, "testShowSiteMembersCommand", "Exit status: " + 
                    exitStatus);
            
            stringsFound = showSiteMembersCLI.findStringsInOutput(
                    expectedMessage, ";");
            log(Level.FINEST, "testShowSiteMembersCommand", 
                        "Output Messages Found: " + stringsFound);
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                assert (exitStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound;
            } else {
                errorFound = showSiteMembersCLI.findStringsInError(
                        expectedMessage, ";");                
                log(Level.FINEST, "testShowSiteMembersCommand", 
                        "Error Messages Found: " + errorFound);
                assert (exitStatus == 
                        new Integer(expectedExitCode).intValue()) && 
                        stringsFound && errorFound;
            }   
            showSiteMembersCLI.resetArgList();            
            exiting("testShowSiteMembersCommand");
        } catch (Exception e) {
            log(Level.SEVERE, "testShowSiteMembersCommand", e.getMessage(), 
                    null);
            cleanup();
            e.printStackTrace();
            throw e;
        } 
    }

    /**
     * This method is used to execute tests with the subcommand and arguments
     * from the CLISiteMemberTests.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
      dependsOnMethods={"testAddSiteMembersCommand"})
    public void testRemoveSiteMembersCommand() 
    throws Exception {
        entering("testRemoveSiteMembersCommand", null);
        boolean stringsFound = false;
        boolean errorFound = false;
        int exitStatus = -1;
        
        try {
            removeSiteArgs = (String) rb.getString(locTestName + 
                        "-remove-site-members-args");
            expectedMessage = (String) rb.getString(locTestName + 
                    "-remove-site-members-expected-message");
            expectedErrorMessage = (String) rb.getString(locTestName + 
                    "-remove-site-members-error-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-remove-site-members-expected-exit-code");
 
            log(Level.FINEST, "testRemoveSiteMembersCommand", 
                    "remove-site-members-args: " + removeSiteArgs);
            log(Level.FINEST, "testRemoveSiteMembersCommand", 
                    "delete-message-to-find: " + expectedMessage);
            log(Level.FINEST, "testRemoveSiteMembersCommand", 
                    "delete-error-message-to-find: " + expectedErrorMessage);
            log(Level.FINEST, "testRemoveSiteMembersCommand", 
                    "delete-expected-exit-code: " + expectedExitCode);

            Reporter.log("TestName: " + locTestName);
            Reporter.log("RemoveSite(s): " + removeSiteArgs);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedErrorMessage: " + expectedErrorMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);

            FederationManagerCLI removeSiteMembersCLI = 
                    new FederationManagerCLI(useDebugOption, useVerboseOption, 
                    useLongOptions);
            String[] siteArgs = removeSiteArgs.split(";");
            if (siteArgs.length >= 1) {
            	String siteName = siteArgs[0];
            	String serverNames = "";
            	if (siteArgs.length > 1) {
            		serverNames = siteArgs[1]; 
            	}
                exitStatus = removeSiteMembersCLI.removeSiteMembers(siteName, 
                        serverNames);
            }
            removeSiteMembersCLI.logCommand("testRemoveSiteMembersCommand");
            log(Level.FINEST, "testRemoveSiteMembersCommand", "Exit status: " + 
                    exitStatus);
            
            stringsFound = removeSiteMembersCLI.findStringsInOutput(
                    expectedMessage, ";");
            log(Level.FINEST, "testRemoveSiteMembersCommand", 
                        "Output Messages Found: " + stringsFound);
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                assert (exitStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound;
            } else {
                errorFound = removeSiteMembersCLI.findStringsInError(
                        expectedMessage, ";");                
                log(Level.FINEST, "testRemoveSiteMembersCommand", 
                        "Error Messages Found: " + errorFound);
                assert (exitStatus == 
                        new Integer(expectedExitCode).intValue()) && 
                        stringsFound && errorFound;
            }
            removeSiteMembersCLI.resetArgList();
            exiting("testRemoveSiteMembersCommand");
        } catch (Exception e) {
            log(Level.SEVERE, "testRemoveSiteMembersCommand", e.getMessage(), 
                    null);
            cleanup();
            e.printStackTrace();
            throw e;
        } 
    }    
    
    /**
     * Cleanup cleans all the sites that are created in setup.
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

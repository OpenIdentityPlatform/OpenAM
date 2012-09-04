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
 * $Id: CLISiteUrlTests.java,v 1.1 2009/06/24 22:26:40 srivenigan Exp $
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
 * <code>CLISiteUrlTests</code> executes tests involving add-site-sec-urls, 
 * set-site-pri-url, set-site-sec-urls and remove-site-sec-urls sub-commands of 
 * ssoadm.  This class allows the user to execute an ssoadm command by 
 * specifying a subcommand and a list of arguments. Properties file named 
 * CLISiteUrlTests.properties contain input values which are read by this class.
 *
 * This class automates the following test cases:
 * CLI_site_url_test01, CLI_site_url_test02, CLI_site_url_test03, 
 * CLI_site_url_test04, CLI_site_url_test05, CLI_site_url_test06, 
 * CLI_site_url_test07, CLI_site_url_test08, CLI_site_url_test09, 
 * CLI_site_url_test10, CLI_site_url_test11
 *  
 * CLI_site_url_test$ includes add-site-sec-urls, set-site-pri-url, 
 * set-site-sec-urls and remove-site-sec-urls sub commands.
 */

public class CLISiteUrlTests extends TestCommon implements CLIExitCodes {
    
    private String locTestName;
    private ResourceBundle rb;
    private boolean useVerboseOption;
    private boolean useDebugOption;
    private boolean useLongOptions;
    private String addSiteSecUrlsArgs;
    private String setupSites;
    private String setSitePriUrlArgs;
    private String removeSiteSecUrls;
    private String expectedMessage;
    private String expectedErrorMessage;
    private String expectedExitCode;
    private String description;
    
    /** 
     * Creates a new instance of CLISiteUrlTests
     */
    public CLISiteUrlTests() {
        super("CLISiteUrlTests");
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
                    "CLISiteUrlTests");

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
     * from the CLISiteUrlTests.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testAddSiteSecUrlsCommand() 
    throws Exception {
        entering("testAddSiteSecUrlsCommand", null);
        boolean stringsFound = false;
        boolean errorFound = false;
        int exitStatus = -1;
        
        try {
            addSiteSecUrlsArgs = (String) rb.getString(locTestName + 
                    "-add-site-sec-urls-args");
            expectedMessage = (String) rb.getString(locTestName + 
                    "-add-site-sec-urls-expected-message");
            expectedErrorMessage = (String) rb.getString(locTestName + 
                    "-add-site-sec-urls-error-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-add-site-sec-urls-expected-exit-code");
 
            log(Level.FINEST, "testAddSiteSecUrlsCommand", 
            		"add-site-sec-urls-args: " + addSiteSecUrlsArgs);
            log(Level.FINEST, "testAddSiteSecUrlsCommand", 
                    "add-site-sec-urls-expected-message: " + expectedMessage);
            log(Level.FINEST, "testAddSiteSecUrlsCommand", 
                    "add-site-sec-urls-error-message-to-find: " + 
                    expectedErrorMessage);
            log(Level.FINEST, "testAddSiteSecUrlsCommand", 
                    "add-site-sec-urls-expected-exit-code: " + 
                    expectedExitCode);
            Reporter.log("TestName: " + locTestName);
            Reporter.log("SiteArgumentList: " + addSiteSecUrlsArgs);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedErrorMessage: " + expectedErrorMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);

            FederationManagerCLI addSiteSecUrlsCLI = new FederationManagerCLI(
                    useDebugOption, useVerboseOption, useLongOptions);
            String[] siteArgs = addSiteSecUrlsArgs.split(";");
            if (siteArgs.length >= 1) {
            	String siteName = siteArgs[0];
            	String serverNames = "";
            	if (siteArgs.length > 1) {
            		serverNames = siteArgs[1]; 
            	}
                exitStatus = addSiteSecUrlsCLI.addSiteSecUrls(siteName, 
                        serverNames);
            } else {
            	log(Level.FINEST, "setup", "Add site sec url arguments list" +
                        " is empty");
            }
            addSiteSecUrlsCLI.logCommand("testAddSiteSecUrlsCommand");
            log(Level.FINEST, "testAddSiteSecUrlsCommand", "Exit status: " + 
                    exitStatus);
            
            stringsFound = addSiteSecUrlsCLI.findStringsInOutput(
                    expectedMessage, ";");
            log(Level.FINEST, "testAddSiteSecUrlsCommand", 
                        "Output Messages Found: " + stringsFound);
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                assert (exitStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound;
            } else {
                errorFound = addSiteSecUrlsCLI.findStringsInError(
                        expectedErrorMessage, ";");                
                log(Level.FINEST, "testAddSiteSecUrlsCommand", 
                        "Error Messages Found: " + errorFound);
                assert (exitStatus == 
                        new Integer(expectedExitCode).intValue()) && 
                        stringsFound && errorFound;
            }
            addSiteSecUrlsCLI.resetArgList();            
            exiting("testAddSiteSecUrlsCommand");
        } catch (Exception e) {
            log(Level.SEVERE, "testAddSiteSecUrlsCommand", e.getMessage(), 
                    null);
            cleanup();
            e.printStackTrace();
            throw e;
        } 
    }

    /**
     * This method is used to execute tests with the subcommand and arguments
     * from the CLISiteUrlTests.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"testAddSiteSecUrlsCommand"})
    public void testSetSitePriUrlCommand() 
    throws Exception {
        entering("testSetSitePriUrlCommand", null);
        boolean stringsFound = false;
        boolean errorFound = false;
        int exitStatus = -1;
        
        try {
            setSitePriUrlArgs = (String) rb.getString(locTestName + 
                    "-set-site-pri-url-args");
            expectedMessage = (String) rb.getString(locTestName + 
                    "-set-site-pri-url-expected-message");
            expectedErrorMessage = (String) rb.getString(locTestName + 
                    "-set-site-pri-url-error-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-set-site-pri-url-expected-exit-code");
            log(Level.FINEST, "testSetSitePriUrlCommand", 
                    "set-site-pri-url-args: " + setSitePriUrlArgs);
            log(Level.FINEST, "testSetSitePriUrlCommand", 
                    "set-site-pri-url-expected-message: " + expectedMessage);
            log(Level.FINEST, "testSetSitePriUrlCommand", 
                    "set-site-pri-url-error-message-to-find: " + 
                    expectedErrorMessage);
            log(Level.FINEST, "testSetSitePriUrlCommand", 
                    "set-site-pri-url-expected-exit-code: " + 
                    expectedExitCode);
            Reporter.log("TestName: " + locTestName);
            Reporter.log("ShowSiteName: " + setSitePriUrlArgs);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedErrorMessage: " + expectedErrorMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);

            FederationManagerCLI setSitePriUrlCLI = 
                        new FederationManagerCLI(useDebugOption, 
                        useVerboseOption, useLongOptions);
            String[] sitePriUrlArgs = setSitePriUrlArgs.split(";");
            if (sitePriUrlArgs.length >= 1) {
            	String siteName = sitePriUrlArgs[0];
            	String serverNames = "";
            	if (sitePriUrlArgs.length > 1) {
            		serverNames = sitePriUrlArgs[1]; 
            	}
                exitStatus = setSitePriUrlCLI.setSitePriUrl(siteName, 
                        serverNames);
            } else {
            	log(Level.FINEST, "setup", "Add site sec url arguments list" +
                        " is empty");
            }
            setSitePriUrlCLI.logCommand("testSetSitePriUrlCommand");
            stringsFound = setSitePriUrlCLI.findStringsInOutput(
                    expectedMessage, ";");
            log(Level.FINEST, "testSetSitePriUrlCommand", 
                        "Output Messages Found: " + stringsFound);
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                assert (exitStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound;
            } else {
                errorFound = setSitePriUrlCLI.findStringsInError(
                        expectedMessage, ";");                
                log(Level.FINEST, "testSetSitePriUrlCommand", 
                        "Error Messages Found: " + errorFound);
                assert (exitStatus == 
                        new Integer(expectedExitCode).intValue()) && 
                        stringsFound && errorFound;
            }   
            setSitePriUrlCLI.resetArgList();            
            exiting("testSetSitePriUrlCommand");
        } catch (Exception e) {
            log(Level.SEVERE, "testSetSitePriUrlCommand", e.getMessage(), 
                    null);
            cleanup();
            e.printStackTrace();
            throw e;
        } 
    }

    /**
     * This method is used to execute tests with the subcommand and arguments
     * from the CLISiteUrlTests.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testSetSiteSecUrlsCommand() 
    throws Exception {
        entering("testSetSiteSecUrlsCommand", null);
        boolean stringsFound = false;
        boolean errorFound = false;
        int exitStatus = -1;
        
        try {
            removeSiteSecUrls = (String) rb.getString(locTestName +
            		"-set-site-sec-urls-args");
            expectedMessage = (String) rb.getString(locTestName + 
                    "-set-site-sec-urls-expected-message");
            expectedErrorMessage = (String) rb.getString(locTestName + 
                    "-set-site-sec-urls-error-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-set-site-sec-urls-expected-exit-code");
 
            log(Level.FINEST, "testSetSiteSecUrlsCommand", 
                    "set-site-sec-urls-args: " + removeSiteSecUrls);
            log(Level.FINEST, "testSetSiteSecUrlsCommand", 
                    "set-site-sec-urls-expected-message: " + expectedMessage);
            log(Level.FINEST, "testSetSiteSecUrlsCommand", 
                    "set-site-sec-urls-error-message-to-find: " + 
                    expectedErrorMessage);
            log(Level.FINEST, "testSetSiteSecUrlsCommand", 
                    "set-site-sec-urls-expected-exit-code: " + 
                    expectedExitCode);
            Reporter.log("TestName: " + locTestName);
            Reporter.log("SetSecSiteUrlArgs: " + removeSiteSecUrls);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedErrorMessage: " + expectedErrorMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);

            FederationManagerCLI setSiteSecUrlsCLI = 
                    new FederationManagerCLI(useDebugOption, useVerboseOption, 
                    useLongOptions);
            String[] siteArgs = removeSiteSecUrls.split(";");
            if (siteArgs.length >= 1) {
            	String siteName = siteArgs[0];
            	String siteSecUrls = "";
            	if (siteArgs.length > 1) {
            		siteSecUrls = siteArgs[1]; 
            	}
                exitStatus = setSiteSecUrlsCLI.setSiteSecUrls(siteName, 
                        siteSecUrls);
            }
            setSiteSecUrlsCLI.logCommand("testSetSiteSecUrlsCommand");
            log(Level.FINEST, "testSetSiteSecUrlsCommand", "Exit status: " + 
                    exitStatus);
            
            stringsFound = setSiteSecUrlsCLI.findStringsInOutput(
                    expectedMessage, ";");
            log(Level.FINEST, "testSetSiteSecUrlsCommand", 
                        "Output Messages Found: " + stringsFound);
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                assert (exitStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound;
            } else {
                errorFound = setSiteSecUrlsCLI.findStringsInError(
                        expectedMessage, ";");                
                log(Level.FINEST, "testSetSiteSecUrlsCommand", 
                        "Error Messages Found: " + errorFound);
                assert (exitStatus == 
                        new Integer(expectedExitCode).intValue()) && 
                        stringsFound && errorFound;
            }
            setSiteSecUrlsCLI.resetArgList();
            exiting("testSetSiteSecUrlsCommand");
        } catch (Exception e) {
            log(Level.SEVERE, "testSetSiteSecUrlsCommand", e.getMessage(), 
                    null);
            cleanup();
            e.printStackTrace();
            throw e;
        } 
    }    
    
    /**
     * This method is used to execute tests with the subcommand and arguments
     * from the CLISiteUrlTests.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testRemoveSiteSecUrlsCommand() 
    throws Exception {
        entering("testRemoveSiteSecUrlsCommand", null);
        boolean stringsFound = false;
        boolean errorFound = false;
        int exitStatus = -1;
        
        try {
            removeSiteSecUrls = (String) rb.getString(locTestName +
            		"-remove-site-sec-urls-args");
            expectedMessage = (String) rb.getString(locTestName + 
                    "-remove-site-sec-urls-expected-message");
            expectedErrorMessage = (String) rb.getString(locTestName + 
                    "-remove-site-sec-urls-error-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-remove-site-sec-urls-expected-exit-code");
 
            log(Level.FINEST, "testRemoveSiteSecUrlsCommand", 
                    "remove-site-sec-urls-args: " + removeSiteSecUrls);
            log(Level.FINEST, "testRemoveSiteSecUrlsCommand", 
                    "remove-site-sec-urls-expected-message: " + 
                    expectedMessage);
            log(Level.FINEST, "testRemoveSiteSecUrlsCommand", 
                    "remove-site-sec-urls-error-message-to-find: " + 
                    expectedErrorMessage);
            log(Level.FINEST, "testRemoveSiteSecUrlsCommand", 
                    "remove-site-sec-urls-expected-exit-code: " + 
                    expectedExitCode);
            Reporter.log("TestName: " + locTestName);
            Reporter.log("SetSecSiteUrlArgs: " + removeSiteSecUrls);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedErrorMessage: " + expectedErrorMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);

            FederationManagerCLI removeSiteSecUrlsCLI = 
                    new FederationManagerCLI(useDebugOption, useVerboseOption, 
                    useLongOptions);
            String[] siteArgs = removeSiteSecUrls.split(";");
            if (siteArgs.length >= 1) {
            	String siteName = siteArgs[0];
            	String siteSecUrls = "";
            	if (siteArgs.length > 1) {
            		siteSecUrls = siteArgs[1]; 
            	}
                exitStatus = removeSiteSecUrlsCLI.removeSiteSecUrls(siteName, 
                		siteSecUrls);
            }
            removeSiteSecUrlsCLI.logCommand("testRemoveSiteSecUrlsCommand");
            log(Level.FINEST, "testRemoveSiteSecUrlsCommand", "Exit status: " + 
                    exitStatus);
            
            stringsFound = removeSiteSecUrlsCLI.findStringsInOutput(
                    expectedMessage, ";");
            log(Level.FINEST, "testRemoveSetSiteSecUrlsCommand", 
                        "Output Messages Found: " + stringsFound);
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                assert (exitStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound;
            } else {
                errorFound = removeSiteSecUrlsCLI.findStringsInError(
                        expectedMessage, ";");                
                log(Level.FINEST, "testRemoveSetSiteSecUrlsCommand", 
                        "Error Messages Found: " + errorFound);
                assert (exitStatus == 
                        new Integer(expectedExitCode).intValue()) && 
                        stringsFound && errorFound;
            }
            removeSiteSecUrlsCLI.resetArgList();
            exiting("testRemoveSiteSecUrlsCommand");
        } catch (Exception e) {
            log(Level.SEVERE, "testRemoveSiteSecUrlsCommand", e.getMessage(), 
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


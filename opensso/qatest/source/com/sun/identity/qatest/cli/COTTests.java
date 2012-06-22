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
 * $Id: COTTests.java,v 1.1 2009/06/02 20:52:05 vimal_67 Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

/**
 * COTTests automates the following test cases:
 * CLI_crlide-ROOTREALMCOT1, CLI_crlide-rootrealmcot2, CLI_crlide-RootRealmCOT3
 * CLI_crlide-RootRealmCOT4-alreadyexist, CLI_crlide-ROOTREALMCOT5-longoption
 * CLI_crlide-rootrealmcot6-longoption, CLI_crlide-RootRealmCOT7-longoption
 * CLI_crlide-RootRealmCOT8-alreadyexist-longoption, CLI_crlide-subRealmCOT1
 * CLI_crlide-subRealmCOT2-longoption, CLI_crlide-subRealmCOT3-alreadyexist
 * CLI_crlide-subRealmCOT4-alreadyexist-longoption,
 * CLI_crlide-RootRealmCOT9-Prefix,
 * CLI_crlide-RootRealmCOT10-Prefix-alreadyexist,
 * CLI_crlide-RootRealmCOT11-Prefix-longoption
 * CLI_crlide-RootRealmCOT12-Prefix-alreadyexist-longoption
 * CLI_crlide-subRealmCOT5-Prefix, CLI_crlide-subRealmCOT6-Prefix-alreadyexist
 * CLI_crlide-subRealmCOT7-Prefix-longoption
 * CLI_crlide-subRealmCOT8-Prefix-alreadyexist-longoption
 * CLI_crlide-RootRealmCOT13-Prefix-VerboseDebug
 * CLI_crlide-RootRealmCOT14-Prefix-alreadyexist-VerboseDebug
 * CLI_crlide-RootRealmCOT15-Prefix-longoption-VerboseDebug
 * CLI_crlide-RootRealmCOT16-Prefix-alreadyexist-longoption-VerboseDebug
 * CLI_crlide-RootRealmCOT16-Prefix-alreadyexist-longoption-VerboseDebug
 * CLI_crlide-RootRealmCOT16-Prefix-alreadyexist-longoption-VerboseDebug
 * CLI_crlide-subRealmCOT10-Prefix-alreadyexist-VerboseDebug
 * CLI_crlide-subRealmCOT11-Prefix-longoption-VerboseDebug
 * CLI_crlide-subRealmCOT12-Prefix-alreadyexist-longoption-VerboseDebug
 * CLI_lide-nonexistingCOT, CLI_lide-nonexistingCOT-longoption
 * CLI_lide-nonexistingCOT-VerboseDebug,
 * CLI_lide-nonexistingCOT-longoption-VerboseDebug
 * CLI_crlide-RootRealmCOT16$, CLI_crlide-RootRealmCOT17$-longoption
 * CLI_crlide-RootRealmCOT18$-VerboseDebug
 * CLI_crlide-RootRealmCOT19$-longoption-VerboseDebug
 * CLI_crlide-subRealmCOT13$, CLI_crlide-subRealmCOT14$-longoption
 * CLI_crlide-subRealmCOT15$-VerboseDebug
 * CLI_crlide-subRealmCOT16$-longoption-VerboseDebug
 * CLI_crlide-RootRealmCOT20$$, CLI_crlide-RootRealmCOT21$$-longoption
 * CLI_crlide-RootRealmCOT22$$-VerboseDebug
 * CLI_crlide-RootRealmCOT23$$-longoption-VerboseDebug
 * CLI_crlide-subRealmCOT17$$, CLI_crlide-subRealmCOT18$$-longoption
 * CLI_crlide-subRealmCOT19$$-VerboseDebug
 * CLI_crlide-subRealmCOT20$$-longoption-VerboseDebug
 * CLI_crlide-RootRealmCOT24-SpecCharacters
 * CLI_crlide-RootRealmCOT25-SpecCharacters-longoption
 * CLI_crlide-RootRealmCOT26-SpecCharacters-VerboseDebug
 * CLI_crlide-RootRealmCOT27-SpecCharacters-longoption-VerboseDebug
 * CLI_crlide-subRealmCOT21-SpecCharacters
 * CLI_crlide-subRealmCOT22-SpecCharacters-longoption
 * CLI_crlide-subRealmCOT23-SpecCharacters-VerboseDebug
 * CLI_crlide-subRealmCOT24-SpecCharacters-longoption-VerboseDebug
 */

package com.sun.identity.qatest.cli;

import com.sun.identity.qatest.common.cli.CLIExitCodes;
import com.sun.identity.qatest.common.cli.FederationManagerCLI;
import com.sun.identity.qatest.common.TestCommon;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.Reporter;

/**
 * <code>COTTests</code> is used to execute tests involving the 
 * create-cot, list-cots, delete-cot sub-command of ssoadm. This class allows
 * the user to execute "ssoadm create-cot", "ssoadm list-cots" and
 * "ssoadm delete-cot" with a variety or arguments (e.g with short or long
 * options, with a password file or password argument, with a locale argument,
 * etc.) and a variety of input values.  The properties file <code>
 * COTTests.properties</code> contains the input values which are read by
 * this class.
 */
public class COTTests extends TestCommon implements CLIExitCodes {
    
    private String locTestName;
    private ResourceBundle rb;
    private String setupRealms;    
    private String cotRealm;
    private String cotNameToCreate;
    private String setupCots;
    private String trustedproviders;   
    private String prefix;   
    private boolean useVerboseOption;
    private boolean useDebugOption;
    private boolean useLongOptions;    
    private String expectedMessage;
    private String expectedExitCode;
    private String description;
    private Set hsetcots;
    private FederationManagerCLI cli;
    
    /** 
     * Creates a new instance of COTTests
     */
    public COTTests() {
        super("COTTests");
    }
    
    /**
     * This method is intended to provide initial setup.
     * Creates any realms specified in the setup-realms property and creates
     * any cots specified in the setup-cots property in the COTTests.properties.
     */
    @Parameters({"testName"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
    "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    @SuppressWarnings("empty-statement")
    public void setup(String testName) 
    throws Exception {
        Object[] params = {testName};
        entering("setup", params);
        try {            
            locTestName = testName;
            rb = ResourceBundle.getBundle("cli" + fileseparator + "COTTests");
            hsetcots = new HashSet();
            setupRealms = (String)rb.getString(locTestName + 
                    "-create-setup-realms");
            setupCots = (String)rb.getString(locTestName +
                    "-create-setup-cots");
            useVerboseOption = ((String)rb.getString(locTestName + 
                    "-use-verbose-option")).equals("true");
            useDebugOption = ((String)rb.getString(locTestName + 
                    "-use-debug-option")).equals("true");
            useLongOptions = ((String)rb.getString(locTestName + 
                    "-use-long-options")).equals("true");
                
            log(logLevel, "setup", "use-verbose-option: " + useVerboseOption);
            log(logLevel, "setup", "use-debug-option: " + useDebugOption);
            log(logLevel, "setup", "use-long-options: " + useLongOptions);
            log(logLevel, "setup", "create-setup-realms: " + setupRealms);            
             
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

            if (setupCots != null) {
                if (setupCots.length() > 0) {
                    StringTokenizer cotTokenizer =
                            new StringTokenizer(setupCots, "|");
                    while (cotTokenizer.hasMoreTokens()) {
                        StringTokenizer tokenizer =
                                new StringTokenizer(cotTokenizer.nextToken(),
                                ",");                        
                        String cotRealm = tokenizer.nextToken();
                        String cotName = tokenizer.nextToken();
                        int commandStatus = cli.createCot(cotName, cotRealm);
                        hsetcots.add(cotName + "," + cotRealm);
                        cli.logCommand("setup");                        
                    }
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
     * This method is used to execute tests involving "ssoadm create-cot"
     * using input data from the COTTests.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
    "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testCreate()
    throws Exception {
        entering("testCreate", null);
        boolean stringsFound = false;
        int commandStatus = 1000;       
        
        try {
            expectedMessage = (String) rb.getString(locTestName +
                    "-create-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName +
                    "-create-expected-exit-code");
            cotRealm = (String) rb.getString(locTestName +
                    "-cot-realm");
            cotNameToCreate = (String) rb.getString(locTestName +
                    "-cot-name");
            trustedproviders = (String) rb.getString(locTestName +
                    "-trustedproviders");
            prefix = (String) rb.getString(locTestName +
                    "-prefix");
            description = (String) rb.getString(locTestName +
                    "-create-description");

            log(logLevel, "testCreate", "description: " + description);
            log(logLevel, "testCreate", "use-debug-option: " + useDebugOption);
            log(logLevel, "testCreate", "use-verbose-option: " +
                    useVerboseOption);
            log(logLevel, "testCreate", "use-long-options: " + useLongOptions);
            log(logLevel, "testCreate", "message-to-find: " + expectedMessage);
            log(logLevel, "testCreate",
                    "expected-exit-code: " + expectedExitCode);
            log(logLevel, "testCreate", "cot-realm: " + cotRealm);
            log(logLevel, "testCreate", "cot-name: " + cotNameToCreate);
            log(logLevel, "testCreate", "trustedproviders: " +
                    trustedproviders);
            log(logLevel, "testCreate", "prefix: " + prefix);

            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);
            Reporter.log("COTRealmToCreate: " + cotRealm);
            Reporter.log("cotNameToCreate: " + cotNameToCreate);
            Reporter.log("COTTrustedProviders: " + trustedproviders);
            Reporter.log("COTprefix: " + prefix);
            
            cli = new FederationManagerCLI(useDebugOption,
                    useVerboseOption, useLongOptions);

            if (!cotNameToCreate.equals("")) {
                if (!prefix.equals("")) {
                    commandStatus = cli.createCot(cotNameToCreate,
                            cotRealm, prefix);                    
                } else if (!prefix.equals("") && !trustedproviders.equals("")) {
                    commandStatus = cli.createCot(cotNameToCreate,
                            cotRealm, trustedproviders, prefix);
                } else {
                    commandStatus = cli.createCot(cotNameToCreate, cotRealm);                    
                }              
            }
            cli.logCommand("testCreate");

            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                stringsFound = cli.findStringsInOutput(expectedMessage, ";");
            } else {
                if (!expectedExitCode.equals(
                    new Integer(INVALID_OPTION_STATUS).toString())) {
                    stringsFound = cli.findStringsInError(expectedMessage, ";");
                } else {
                    String argString = cli.getAllArgs().replaceFirst(
                            cli.getCliPath(), "ssoadm ");
                    Object[] params = {argString};
                    String usageError = MessageFormat.format(
                            expectedMessage, params);
                    stringsFound = cli.findStringsInError(
                            usageError, ";" + newline);
                }
            }
            cli.resetArgList();
            if (!cotNameToCreate.equals("")) {
                if (expectedExitCode.equals(
                        new Integer(SUCCESS_STATUS).toString())) {
                    log(logLevel, "testCreate",
                            "Output Messages Found: " + stringsFound);
                    assert (commandStatus ==
                            new Integer(expectedExitCode).intValue()) &&
                            stringsFound;
                } else {
                    log(logLevel, "testCreate",
                            "Error Messages Found: " + stringsFound);
                    assert (commandStatus ==
                            new Integer(expectedExitCode).intValue()) &&
                            stringsFound;
                }
            }
            exiting("testCreate");
        } catch (Exception e) {
            log(Level.SEVERE, "testCreate", e.getMessage());
            e.printStackTrace();
            throw e;
        } 
    }

    /**
     * This method is used to execute tests involving "ssoadm list-cots"
     * using input data from the COTTests.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
    "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"}, 
            ignoreMissingDependencies = true,
            dependsOnMethods = {"testCreate"})
    public void testLists()
    throws Exception {
        entering("testLists", null);
        boolean stringsFound = false;
        int commandStatus = 1000;

        try {
            expectedMessage = (String) rb.getString(locTestName +
                    "-list-message-to-find");
            cotNameToCreate = (String) rb.getString(locTestName +
                    "-cot-name");
            if (!cotNameToCreate.equals("")) {
                expectedMessage = "  " + expectedMessage;
            }
            expectedExitCode = (String) rb.getString(locTestName +
                    "-list-expected-exit-code");
            cotRealm = (String) rb.getString(locTestName + "-cot-realm");
            description = (String) rb.getString(locTestName +
                    "-list-description");
            log(logLevel, "testLists", "description: " + description);
            log(logLevel, "testLists", "use-debug-option: " +
                    useDebugOption);
            log(logLevel, "testLists", "use-verbose-option: " +
                    useVerboseOption);
            log(logLevel, "testLists", "use-long-options: " +
                    useLongOptions);
            log(logLevel, "testLists", "message-to-find: " +
                    expectedMessage);
            log(logLevel, "testLists", "expected-exit-code: " +
                    expectedExitCode);
            log(logLevel, "testLists", "cot-realm: " + cotRealm);

            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);
            Reporter.log("COTRealmToCreate: " + cotRealm);

            cli = new FederationManagerCLI(useDebugOption,
                    useVerboseOption, useLongOptions);
            commandStatus = cli.listCots(cotRealm);
            cli.logCommand("testLists");
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                stringsFound = cli.findStringsInOutput(expectedMessage, ";");
            } else {
                if (!expectedExitCode.equals(new Integer(
                        INVALID_OPTION_STATUS).toString())) {
                    stringsFound = cli.findStringsInError(expectedMessage, ";");
                } else {
                    String argString = cli.getAllArgs().replaceFirst(
                            cli.getCliPath(), "ssoadm ");
                    Object[] params = {argString};
                    String usageError = MessageFormat.format(
                            expectedMessage, params);
                    stringsFound = cli.findStringsInError(usageError, ";" +
                            newline);
                }
            }
            cli.resetArgList();
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                log(logLevel, "testLists",
                        "Output Messages Found: " + stringsFound);
                assert (commandStatus ==
                        new Integer(expectedExitCode).intValue()) &&
                        stringsFound;
            } else {
                log(logLevel, "testLists", "Error Messages Found: " +
                        stringsFound);
                assert (commandStatus == new Integer(
                        expectedExitCode).intValue()) && stringsFound;
            }
            exiting("testLists");
        } catch (Exception e) {
            log(Level.SEVERE, "testLists", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * This method removes any realms and cot that were created during
     * the setup and testCreate methods using "ssoadm delete-realm"
     * and "ssoadm delete-cot".
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
    "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"}, 
            ignoreMissingDependencies = true,
            dependsOnMethods = {"testLists"})
    public void testDelete()
    throws Exception {
        entering("testDelete", null);
        boolean stringsFound = false;
        int commandStatus = 1000;
        
        try {
            cotRealm = (String) rb.getString(locTestName +
                    "-cot-realm");
            cotNameToCreate = (String) rb.getString(locTestName +
                    "-cot-name");
            expectedMessage = (String) rb.getString(locTestName +
                    "-delete-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName +
                    "-delete-expected-exit-code");            
            description = (String) rb.getString(locTestName +
                    "-delete-description");

            log(logLevel, "testDelete", "description: " + description);
            log(logLevel, "testDelete", "use-debug-option: " +
                    useDebugOption);
            log(logLevel, "testDelete", "use-verbose-option: " +
                    useVerboseOption);
            log(logLevel, "testDelete", "use-long-options: " +
                    useLongOptions);
            log(logLevel, "testDelete", "message-to-find: " +
                    expectedMessage);
            log(logLevel, "testDelete", "expected-exit-code: " +
                    expectedExitCode);
            log(logLevel, "testDelete", "cot-realm: " + cotRealm);
            log(logLevel, "testDelete", "cot-name: " + cotNameToCreate);

            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);
            Reporter.log("COTRealm: " + cotRealm);
            Reporter.log("COTNameToDelete: " + cotNameToCreate);

            cli = new FederationManagerCLI(useDebugOption,
                    useVerboseOption, useLongOptions);

            if (!cotNameToCreate.equals("")) {
                commandStatus = cli.deleteCot(cotNameToCreate, cotRealm);
            } else {
                commandStatus = cli.deleteCot("NonExistingCot", cotRealm);
            }

            cli.logCommand("testDelete");
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                stringsFound = cli.findStringsInOutput(expectedMessage, ";");
            } else {
                if (!expectedExitCode.equals(new Integer(
                        INVALID_OPTION_STATUS).toString())) {
                    stringsFound = cli.findStringsInError(expectedMessage, ";");
                } else {
                    String argString = cli.getAllArgs().replaceFirst(
                            cli.getCliPath(), "ssoadm ");
                    Object[] params = {argString};
                    String usageError = MessageFormat.format(
                            expectedMessage, params);
                    stringsFound = cli.findStringsInError(usageError, ";" +
                            newline);
                }
            }
            cli.resetArgList();
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                log(logLevel, "testDelete",
                        "Output Messages Found: " + stringsFound);
                assert (commandStatus ==
                        new Integer(expectedExitCode).intValue()) &&
                        stringsFound;
            } else {
                log(logLevel, "testDelete", "Error Messages Found: " +
                        stringsFound);
                assert (commandStatus == new Integer(
                        expectedExitCode).intValue()) && stringsFound;
            }
            exiting("testDelete");
        } catch (Exception e) {
            log(Level.SEVERE, "testDelete", e.getMessage());
            e.printStackTrace();
            throw e;
        } 
    }

    /**
     * This method cleans up the system
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
    "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup() 
    throws Exception {
       
        entering("cleanup", null);
        boolean stringsFound = false;
        int commandStatus = 1000;
        int cleanupExitStatus = -1;
        FederationManagerCLI idCli = null;
        FederationManagerCLI cleanupCli = null;
        
        try {
            Iterator itercots = hsetcots.iterator();
            while (itercots.hasNext()) {
                String cotName = "";
                String cotRealm = "";
                String st = (String) itercots.next();
                int index = st.indexOf(",");
                if (index != -1) {
                    cotName = st.substring(0, index).trim();
                    cotRealm = st.substring(index + 1, st.length()).trim();
                }
                log(logLevel, "cleanup", "setupCOTToDelete: "  + cotName);
                log(logLevel, "cleanup", "setupCOTRealmToDelete: "  + cotRealm);
                Reporter.log("SetupCOTToDelete: " + cotName);
                Reporter.log("SetupCOTRealmToDelete: " + cotRealm);
                idCli = new FederationManagerCLI(useDebugOption,
                                    useVerboseOption, useLongOptions);
                idCli.deleteCot(cotName, cotRealm);
                idCli.logCommand("cleanup");
                idCli.resetArgList();
            }

            if (!setupRealms.equals("")) {
                String[] realms = setupRealms.split(";");
                for (int i = realms.length-1; i >= 0; i--) {
                    log(Level.FINEST, "cleanup", "setupRealmToDelete: " +
                        realms[i]);
                    Reporter.log("SetupRealmToDelete: " + realms[i]);
                    cleanupExitStatus = cli.deleteRealm(realms[i], true);
                    cli.logCommand("cleanup");
                    if (cleanupExitStatus != SUCCESS_STATUS) {
                        assert false;
                    }
                }
            }
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        }  finally {
            if (idCli != null) {
                idCli.resetArgList();
            }
            if (cleanupCli != null) {
                cleanupCli.resetArgList();
            }
            if (cli != null) {
                cli.resetArgList();
            }
        }
        exiting("cleanup");
    }
}

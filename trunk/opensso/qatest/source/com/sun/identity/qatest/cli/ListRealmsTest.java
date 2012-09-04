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
 * $Id: ListRealmsTest.java,v 1.11 2009/01/26 23:49:36 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/**
 * ListRealmTest automates the following test cases:
 * CLI_list-realm01, CLI_list-realm02, CLI_list-realm03, CLI_list-realm04,
 * CLI_list-realm05, CLI_list-realm06, CLI_list-realm07, CLI_list-realm08,
 * CLI_list-realm09, CLI_list-realm10, CLI_list-realm11, CLI_list-realm12, 
 * and CLI_list-realm13.
 */

package com.sun.identity.qatest.cli;

import com.sun.identity.qatest.common.cli.CLIExitCodes;
import com.sun.identity.qatest.common.cli.FederationManagerCLI;
import com.sun.identity.qatest.common.TestCommon;
import java.text.MessageFormat;
import java.util.Iterator;
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
 * <code>ListRealmsTest</code> is used to execute tests involving the
 * list-realms sub-command of ssoadm.  This class allows the user to execute
 * "ssoadm list-realms" with a variety or arguments (e.g with short or long
 * options, with a locale argument, with the recursive option, with the
 * filter option, etc.) and a variety of input values.  The properties file
 * <code>ListRealmsTest.properties</code> contains the input values which are
 * read by this class.
 */
public class ListRealmsTest extends TestCommon implements CLIExitCodes {
    
    private String locTestName;
    private ResourceBundle rb;
    private String setupRealms;
    private String searchRealm;
    private boolean useVerboseOption;
    private boolean useDebugOption;
    private boolean useLongOptions;
    private boolean useRecursiveOption;
    private boolean useFilterOption;
    private String filter;
    private String expectedMessage;
    private String expectedExitCode;
    private String description;
    private String realmsToFind;
    private FederationManagerCLI cli;
    private Vector realmsNotShown;
    
    /** 
     * Creates a new instance of ListRealmsTest 
     */
    public ListRealmsTest() {
        super("ListRealmsTest");
    }
    
    /**
     * This method is intended to provide initial setup.
     * Creates any realms specified in the setup-realms property in the 
     * ListRealmsTest.properties.
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
                    "ListRealmsTest-Generated");
            setupRealms = (String)rb.getString(locTestName + 
                    "-create-setup-realms");
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
            
            realmsNotShown = new Vector();
            if (setupRealms != null) {
                if (setupRealms.length() > 0) {
                    StringTokenizer tokenizer = new StringTokenizer(setupRealms, 
                            ";");
                    while (tokenizer.hasMoreTokens()) {
                        String realmToCreate = tokenizer.nextToken();
                        cli.createRealm(realmToCreate);
                        cli.logCommand("setup");
                        cli.resetArgList();
                        realmsNotShown.add(realmToCreate);
                    }
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
     * This method is used to execute tests involving "ssoadm list-realms"
     * using input data from the ListRealmsTest.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testRealmSearch() 
    throws Exception {
        entering("testRealmSearch", null);
        boolean stringsFound = false;
        boolean realmsFound = true;
        boolean otherRealmsListed = false;
        
        try {
            description = (String) rb.getString(locTestName + "-description");
            expectedMessage = (String) rb.getString(locTestName + 
                    "-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-expected-exit-code");
            searchRealm = (String) rb.getString(locTestName + 
                    "-search-realm");
            useRecursiveOption = ((String) rb.getString(locTestName + 
                    "-use-recursive-option")).equals("true");
            useFilterOption = ((String) rb.getString(locTestName + 
                    "-use-filter-option")).equals("true");
            filter = (String) rb.getString(locTestName + "-filter");
            realmsToFind = (String) rb.getString(locTestName + 
                    "-realms-to-find");

            log(logLevel, "testRealmSearch", "description: " + description);
            log(logLevel, "testRealmSearch", "use-debug-option: " + 
                    useDebugOption);
            log(logLevel, "testRealmSearch", "use-verbose-option: " + 
                    useVerboseOption);
            log(logLevel, "testRealmSearch", "use-long-options: " + 
                    useLongOptions);
            log(logLevel, "testRealmSearch", "message-to-find: " + 
                    expectedMessage);
            log(logLevel, "testRealmSearch", "expected-exit-code: " + 
                    expectedExitCode);
            log(logLevel, "testRealmSearch", "search-realm: " + 
                    searchRealm);
            log(logLevel, "testRealmSearch", "use-recursive-option: " + 
                    useRecursiveOption);
            log(logLevel, "testRealmSearch", "use-filter-option: " + 
                    useFilterOption);
            log(logLevel, "testRealmSearch", "filter: \"" + filter + "\"");
            log(logLevel, "testRealmSearch", "realms-to-find: " + realmsToFind);

            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);
            Reporter.log("SearchRealm: " + searchRealm);
            Reporter.log("UseRecursiveOption: " + useRecursiveOption);
            Reporter.log("UseFilterOption: " + useFilterOption);
            Reporter.log("Filter: " + filter);
            
            int commandStatus;
            if (useFilterOption) {
                commandStatus = cli.listRealms(searchRealm, filter, 
                    useRecursiveOption);
            } else {
                commandStatus = cli.listRealms(searchRealm, useRecursiveOption);
            }
            cli.logCommand("testRealmSearch");

            int searchLength;
            if (searchRealm.equals("/")) {
                searchLength = 1;
            } else {
                searchLength = searchRealm.length() + 1;
            }
            if (realmsToFind.length() > 0) {
                StringTokenizer tokenizer = new StringTokenizer(realmsToFind, 
                        ";");
                while (tokenizer.hasMoreTokens()) {
                    String token = tokenizer.nextToken();
                    boolean prefixFound = token.startsWith(searchRealm);
                    realmsNotShown.removeElement(token);
                    if (prefixFound && (token != null)) {
                        if (token.length() > searchLength) {
                            String tokenRealm = 
                                    token.substring(searchLength);
                            if (!cli.findStringInOutput(tokenRealm)) {
                                log(logLevel, "testRealmSearch", "Realm " + 
                                        tokenRealm + " was not found.");
                                realmsFound = false;
                            } else {
                                log(logLevel, "testRealmSearch", "Realm " + 
                                        tokenRealm + " was found.");
                            }
                        } 
                    } else {
                        log(Level.SEVERE, "testRealmSearch", "Realm " + token + 
                                " in realmsToFind is null.");
                        realmsFound = false;
                    }
                }
                log(logLevel, "testRealmSearch", "Expected Realms Found: " + 
                        realmsFound);
            }
            
            for (Iterator i = realmsNotShown.iterator(); i.hasNext(); ) {
                String realmNotShown = (String)i.next();
                if (realmNotShown.length() > searchLength) {
                    log(logLevel, "testRealmSearch", "Searching for " + 
                            realmNotShown + " in list-realms output.");
                    String searchString = 
                            realmNotShown.substring(searchLength);
                    if (cli.findStringInOutput(searchString)) {
                        log(logLevel, "testRealmSearch", "Realm " + 
                                searchString + 
                                " was found in the list-realms output.");
                        otherRealmsListed=true;
                    }
                }
            }
            
            log(logLevel, "testRealmSearch", "Unexpected realms Found: " + 
                    otherRealmsListed);

            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                stringsFound = cli.findStringsInOutput(expectedMessage, ";");
                log(logLevel, "testRealmSearch", "Output Messages Found: " + 
                        stringsFound);
                assert (commandStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound &&
                        realmsFound && !otherRealmsListed;
            } else {
                if (!expectedExitCode.equals(
                        new Integer(INVALID_OPTION_STATUS).toString())) {
                    stringsFound = 
                        cli.findStringsInError(expectedMessage, ";");
                } else {
                    String argString = cli.getAllArgs().replaceFirst(
                            cli.getCliPath(), "ssoadm ");
                    Object[] params = {argString};
                    String usageError = MessageFormat.format(expectedMessage,
                            params);
                    stringsFound = cli.findStringsInError(usageError,
                            ";" + newline);
                }
                log(logLevel, "testRealmSearch", "Error Messages Found: " + 
                        stringsFound);
                assert (commandStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound;
            }
            cli.resetArgList();            
            exiting("testRealmSearch");
        } catch (Exception e) {
            log(Level.SEVERE, "testRealmSearch", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        } 
    }
    
    /**
     * This method remove any realms that were created during the setup and
     * testRealmSearch methods using "ssoadm delete-realm".
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup() 
    throws Exception {
        entering("cleanup", null);
        try {            
            log(logLevel, "cleanup", "useDebugOption: " + useDebugOption);
            log(logLevel, "cleanup", "useVerboseOption: " + useVerboseOption);
            log(logLevel, "cleanup", "useLongOptions: " + useLongOptions);
            
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            
            if (!setupRealms.equals("")) {
                String[] realms = setupRealms.split(";");
                FederationManagerCLI cleanupCli = 
                        new FederationManagerCLI(useDebugOption, 
                        useVerboseOption, useLongOptions);
                for (int i=realms.length-1; i >= 0; i--) {
                    log(Level.FINEST, "cleanup", "setupRealmToDelete: " + 
                        realms[i]);
                    Reporter.log("SetupRealmToDelete: " + realms[i]);
                    int exitStatus = cleanupCli.deleteRealm(realms[i], true); 
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
            exiting("cleanup");
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        } 
    }
}

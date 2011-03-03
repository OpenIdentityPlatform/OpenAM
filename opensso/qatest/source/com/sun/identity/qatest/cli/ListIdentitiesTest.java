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
 * $Id: ListIdentitiesTest.java,v 1.7 2009/01/26 23:49:35 nithyas Exp $
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
import java.util.Vector;
import java.util.logging.Level;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.Reporter;

/**
 * <code>ListIdentitiesTest</code> is used to execute tests involving the 
 * list-identities sub-command of ssoadm.  This class allows the user to execute
 * "ssoadm list-identities" with a variety or arguments (e.g with short or long 
 * options, with a locale argument, with a list of attributes or a datafile 
 * containing attributes, etc.) and a variety of input values.  The properties 
 * file <code>ListIdentitiesTest.properties</code> contains the input values 
 * which are read by this class.
 *
 * <code>ListIdentitiesTest</code> automates the following test cases:
 * CLI_list-identities01, CLI_list-identities02, CLI_list-identities03,
 * CLI_list-identities04, CLI_list-identities05, CLI_list-identities06,
 * CLI_list-identities07, CLI_list-identities08, CLI_list-identities09,
 * CLI_list-identities10, CLI_list-identities11, CLI_list-identities12,
 * CLI_list-identities13, CLI_list-identities14, CLI_list-identities15,
 * CLI_list-identities16, CLI_list-identities17, CLI_list-identities18,
 * CLI_list-identities19, CLI_list-identities20, CLI_list-identities21,
 * CLI_list-identities22, CLI_list-identities23, CLI_list-identities24,
 * CLI_list-identities25, CLI_list-identities26, CLI_list-identities27,
 * CLI_list-identities28, CLI_list-identities29, CLI_list-identities30,
 * CLI_list-identities31, CLI_list-identities32.
 */
public class ListIdentitiesTest extends TestCommon implements CLIExitCodes {
    
    private String locTestName;
    private ResourceBundle rb;
    private String setupRealms;
    private String setupIdentities;
    private String searchRealm;
    private String searchFilter;
    private String idNamesToFind;
    private String idTypeToSearch;
    private boolean useVerboseOption;
    private boolean useDebugOption;
    private boolean useLongOptions;
    private String expectedMessage;
    private String expectedExitCode;
    private String description;
    private boolean idsFound;
    private FederationManagerCLI cli;
    private Vector idsNotDeleted;
    
    /** 
     * Creates a new instance of ListIdentitiesTest 
     */
    public ListIdentitiesTest() {
        super("ListIdentitiesTest");
    }
    
    /**
     * This method is intended to provide initial setup.
     * Creates any realms specified in the setup-realms property in the 
     * DeleteIdentitiesTest.properties.
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
                    "ListIdentitiesTest");
            setupRealms = (String)rb.getString(locTestName + 
                    "-create-setup-realms");
            setupIdentities = (String)rb.getString(locTestName + 
                    "-create-setup-identities");
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
            log(Level.FINEST, "setup", "create-setup-identities: " + 
                    setupIdentities);
             
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("SetupRealms: " + setupRealms);
            Reporter.log("SetupIdentities: " + setupIdentities);

            cli = new FederationManagerCLI(useDebugOption, useVerboseOption, 
                    useLongOptions);

            int exitStatus = -1;
            if (setupRealms != null && !setupRealms.equals("")) {
                if (!cli.createRealms(setupRealms)) {
                    log(Level.SEVERE, "setup", 
                            "All the realms failed to be created.");
                    assert false;
                }
            }
            
            String setupID = null;
            if (setupIdentities != null) {
                if (setupIdentities.length() > 0) {
                    String [] ids = setupIdentities.split("\\|");
                    for (int i=0; i < ids.length; i++) {
                        log(Level.FINEST, "setup", "Setup id:" + ids[i]);
                        String [] idArgs = ids[i].split("\\,");
                        if (idArgs.length >= 3) {
                            String idRealm = idArgs[0];
                            String idName = idArgs[1];
                            String idType = idArgs[2];
                            log(Level.FINEST, "setup", "Realm for setup id: " + 
                                    idRealm);
                            log(Level.FINEST, "setup", "Name for setup id: " + 
                                    idName);
                            log(Level.FINEST, "setup", "Type for setup id: " + 
                                    idType);                            
                            String idAttributes = null;
                            if (idArgs.length > 3) {
                                idAttributes = idArgs[3];
                                exitStatus = cli.createIdentity(idRealm, idName,
                                        idType, idAttributes);
                            } else {                                
                                exitStatus = cli.createIdentity(idRealm, idName,
                                        idType);
                            }
                            cli.logCommand("setup");
                            cli.resetArgList();
                            if (exitStatus != SUCCESS_STATUS) {
                                assert false;
                                log(Level.SEVERE, "setup", "The creation of " + 
                                        idName + " failed with exit status " +
                                        exitStatus + ".");
                            }
                        } else {
                            assert false;
                            log(Level.SEVERE, "setup", "The setup identity " + 
                                    setupIdentities + " must have a realm, an " 
                                    + "identity name, and an identity type");
                        }
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
     * This method is used to execute tests involving "ssoadm list-identities"
     * using input data from the ListIdentitiesTest.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testIdentitySearch() 
    throws Exception {
        entering("testIdentitySearch", null);
        boolean stringsFound = false;
        boolean errorFound = false;
        int commandStatus = -1;
        
        try {
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-expected-exit-code");
            searchRealm = (String) rb.getString(locTestName + "-search-realm");
            idTypeToSearch = (String) rb.getString(locTestName + 
                    "-search-idtype");    

            log(Level.FINEST, "testIdentitySearch", "expected-exit-code: " + 
                    expectedExitCode);
            log(Level.FINEST, "testIdentitySearch", "search-realm: " + 
                    searchRealm);
            log(Level.FINEST, "testIdentitySearch", "search-idtype: " + 
                    idTypeToSearch);
            
            Reporter.log("ExpectedExitCode: " + expectedExitCode);
            Reporter.log("SearchRealm: " + searchRealm);            
            Reporter.log("IdTypeToSearch: " + idTypeToSearch);

            searchFilter = (String) rb.getString(locTestName + 
                    "-search-filter");                    
            idNamesToFind = (String) rb.getString(locTestName + 
                    "-search-ids-to-find");
            description = (String) rb.getString(locTestName + "-description");

            log(Level.FINEST, "testIdentitySearch", "description: " + 
                    description);
            log(Level.FINEST, "testIdentitySearch", "use-debug-option: " + 
                    useDebugOption);
            log(Level.FINEST, "testIdentitySearch", "use-verbose-option: " + 
                    useVerboseOption);
            log(Level.FINEST, "testIdentitySearch", "use-long-options: " + 
                    useLongOptions);
            log(Level.FINEST, "testIdentitySearch", "message-to-find: " + 
                    expectedMessage);

            log(Level.FINEST, "testIdentitySearch", "search-filter: " + 
                    searchFilter);
            log(Level.FINEST, "testIdentitySearch", "search-ids-to-find: " + 
                    idNamesToFind);

            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("SearchFilter: " + searchFilter);
            Reporter.log("IdNamesToFind: " + idNamesToFind);

            commandStatus = cli.listIdentities(searchRealm, searchFilter, 
                    idTypeToSearch);
            cli.logCommand("testIdentitySearch");

            String msg = (String) rb.getString(locTestName + 
                    "-message-to-find");  
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                cli.resetArgList();
                Object[] params = {searchRealm, idTypeToSearch}; 
                if (msg.equals("")) {           
                    if (!useVerboseOption) {
                        String successString = 
                                (String) rb.getString("success-message");
                        expectedMessage = 
                                MessageFormat.format(successString, params);
                    } else {
                        String verboseSuccessString = 
                                (String) rb.getString(
                                "verbose-success-message");
                        expectedMessage = 
                                MessageFormat.format(verboseSuccessString,
                                params);
                    }
                } else {
                    expectedMessage = 
                            MessageFormat.format(msg, params);
                }                
                if (!idNamesToFind.equals("")) {
                    idsFound = cli.findIdentities(searchRealm, searchFilter,
                        idTypeToSearch, idNamesToFind); 
                } else {
                    idsFound = true;
                }
                cli.logCommand("testIdentitySearch");
                stringsFound = cli.findStringsInOutput(expectedMessage, ";");
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
                   
            log(Level.FINEST, "testIdentitySearch", "Exit status: " + 
                    commandStatus);
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                log(Level.FINEST, "testIdentitySearch", 
                        "Output Messages Found: " + stringsFound);
                log(Level.FINEST, "testIdentitySearch", "Identities Found: " + 
                        idsFound);
                assert (commandStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound &&
                        idsFound;
            } else {
                log(Level.FINEST, "testIdentitySearch", "Error Messages Found: "
                        + errorFound);
                if (expectedExitCode.equals(
                        new Integer(INVALID_OPTION_STATUS).toString())) {
                    log(Level.FINEST, "testIdentitySearch", 
                            "Output Messages Found: " + stringsFound); 
                    assert (commandStatus == 
                            new Integer(expectedExitCode).intValue()) && 
                            stringsFound && errorFound;                    
                } else {
                    assert (commandStatus == 
                            new Integer(expectedExitCode).intValue()) && 
                            errorFound;
                }
            }   
            exiting("testIdentitySearch");
        } catch (Exception e) {
            log(Level.SEVERE, "testIdentitySearch", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        } 
    }
    
    /**
     * This method remove any realms that were created during the setup and
     * testIdentitySearch methods using "ssoadm list-identities".
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
            
            if (setupIdentities != null) {
                if (setupIdentities.length() > 0) {
                    String [] cleanupIds = setupIdentities.split("\\|");
                    for (int i = 0; i < cleanupIds.length; i++) {
                        String [] idArgs = cleanupIds[i].split("\\,");
                        if (idArgs.length >= 3) {
                            String idRealm = idArgs[0];
                            String idName = idArgs[1];
                            String idType = idArgs[2];
                            
                            log(Level.FINEST, "cleanup", "idRealm: " + idRealm);
                            log(Level.FINEST, "cleanup", "idName: " + idName);
                            log(Level.FINEST, "cleanup", "idType: " + idType);
                            
                            Reporter.log("IdRealm: " + idRealm);
                            Reporter.log("IdNameToRemove: " + idName);
                            Reporter.log("IdentityTypeToRemove: " + idType);
                            
                            exitStatus = 
                                    cli.deleteIdentities(idRealm, idName, 
                                    idType);
                            cli.resetArgList();
                            if (exitStatus != SUCCESS_STATUS) {
                                assert false;
                            }
                        } else {
                            log(Level.SEVERE, "cleanup", "The setup identity " + 
                                    setupIdentities + " must have a realm, " +
                                    "an identity name, and an identity type");
                        }
                    }
                }
            }

            if (!setupRealms.equals("")) {
                String[] realms = setupRealms.split(";");
                for (int i=realms.length-1; i >= 0; i--) {
                    log(Level.FINEST, "cleanup", "setupRealmToDelete: " + 
                        realms[i]);
                    Reporter.log("SetupRealmToDelete: " + realms[i]);
                    exitStatus = cli.deleteRealm(realms[i], true); 
                    cli.logCommand("cleanup");
                    cli.resetArgList();
                    if (exitStatus != SUCCESS_STATUS) {
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

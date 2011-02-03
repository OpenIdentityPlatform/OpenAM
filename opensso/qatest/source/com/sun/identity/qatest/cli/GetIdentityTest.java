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
 * $Id: GetIdentityTest.java,v 1.6 2009/01/26 23:49:33 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
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
 * <code>GetIdentityTest</code> is used to execute tests involving the 
 * get-identity sub-command of ssoadm.  This class allows the user to execute 
 * "ssoadm get-identity" with short or long options and a variety of input 
 * values.  The properties file <code>GetIdentityTest.properties</code> contains 
 * the input values which are read by this class.
 *
 * This class automates the following test cases:
 * CLI_get-identity01, CLI_get-identity02, CLI_get-identity03, 
 * CLI_get-identity04, CLI_get-identity05, CLI_get-identity06, 
 * CLI_get-identity07, CLI_get-identity08, CLI_get-identity09, 
 * CLI_get-identity10, CLI_get-identity11, CLI_get-identity12
 */

public class GetIdentityTest extends TestCommon implements CLIExitCodes {
    
    private String locTestName;
    private ResourceBundle rb;
    private String setupRealms;
    private String setupIdentities;
    private String attributesToFind;
    private String attributesNotFound;
    private String idRealm;
    private String idName;
    private String idType;
    private String attributeNames;
    private boolean useVerboseOption;
    private boolean useDebugOption;
    private boolean useLongOptions;
    private String expectedMessage;
    private String expectedExitCode;
    private String description;
    private FederationManagerCLI cli;
    
    /** 
     * Creates a new instance of GetIdentityTest 
     */
    public GetIdentityTest() {
        super("GetIdentityTest");
    }
    
    /**
     * This method is intended to provide initial setup.
     * Creates any realms specified in the setup-realms property in the 
     * GetIdentityTest.properties file.
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
                    "GetIdentityTest");
            setupRealms = (String)rb.getString(locTestName + 
                    "-create-setup-realms");
            useVerboseOption = ((String)rb.getString(locTestName + 
                    "-use-verbose-option")).equals("true");
            useDebugOption = ((String)rb.getString(locTestName + 
                    "-use-debug-option")).equals("true");
            useLongOptions = ((String)rb.getString(locTestName + 
                    "-use-long-options")).equals("true");
            setupIdentities = (String) rb.getString(locTestName + 
                    "-create-setup-identities");
                
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
            
            log(Level.FINE, "setup", "Creating the following realms: " + 
                    setupRealms);
            
            if (!cli.createRealms(setupRealms)) {
                log(Level.SEVERE, "setup", 
                        "All the realms failed to be created.");
                assert false;
            }

            FederationManagerCLI idCli = 
                    new FederationManagerCLI(useDebugOption, useVerboseOption, 
                    useLongOptions);

            if (!idCli.createIdentities(setupIdentities)) {
                log(Level.SEVERE, "setup", 
                        "All the identities failed to be created.");                
                assert false;
            }
 
            exiting("setup");
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        } 
    }
    
    /**
     * This method is used to execute tests involving 
     * "ssoadm get-identity" using input data from the 
     * GetIdentityTest.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testGetIdentityAttributes() 
    throws Exception {
        entering("testGetIdentityAttributes", null);
        boolean stringsFound = false;
        boolean attributesFound = true;
        boolean otherAttributesFound = false;
        boolean errorFound = false;
        int commandStatus = -1;
        
        try {
            description = (String) rb.getString(locTestName + "-description");
            expectedMessage = (String) rb.getString(locTestName + 
                    "-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-expected-exit-code");
            idRealm = (String) rb.getString(locTestName + 
                    "-get-identity-realm");
            idName = (String) rb.getString(locTestName + 
                    "-get-identity-name");
            idType = (String) rb.getString(locTestName + "-get-identity-type");
            attributeNames = (String) rb.getString(locTestName + 
                    "-get-identity-attribute-names");
            attributesToFind = (String) rb.getString(locTestName + 
                    "-get-identity-attributes-to-find");
            attributesNotFound = (String) rb.getString(locTestName + 
                    "-get-identity-attributes-not-to-find");
 
            log(Level.FINEST, "testGetIdentityAttributes", "description: " + 
                    description);
            log(Level.FINEST, "testGetIdentityAttributes", 
                    "use-debug-option: " + useDebugOption);
            log(Level.FINEST, "testGetIdentityAttributes", 
                    "use-verbose-option: " + useVerboseOption);
            log(Level.FINEST, "testGetIdentityAttributes", 
                    "use-long-options: " + useLongOptions);
            log(Level.FINEST, "testGetIdentityAttributes", "message-to-find: "
                    + expectedMessage);
            log(Level.FINEST, "testGetIdentityAttributes", 
                    "expected-exit-code: " + expectedExitCode);
            log(Level.FINEST, "testGetIdentityAttributes", 
                    "get-identity-realm: " + idRealm);
            log(Level.FINEST, "testGetIdentityAttributes", "get-identity-name: "
                    + idName);
            log(Level.FINEST, "testGetIdentityAttributes", "get-identity-type: "
                    + idType);
            log(Level.FINEST, "testGetIdentityAttributes", 
                    "get-identity-attribute-names: " + attributeNames);
            log(Level.FINEST, "testGetIdentityAttributes", 
                    "get-identity-attributes-to-find: " + attributesToFind);
            log(Level.FINEST, "testGetIdentityAttributes", 
                    "get-identity-attributes-not-to-find: " + 
                    attributesNotFound);
 
            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);
            Reporter.log("Realm: " + realm);
            Reporter.log("IdenityName: " + idName);
            Reporter.log("IdentityType: " + idType);
            Reporter.log("AttributeNames: " + attributeNames);
            Reporter.log("AttributesToFind: " + attributesToFind);
            Reporter.log("AttributesNotFound: " + attributesNotFound);
            
            String msg = (String) rb.getString(locTestName + 
                    "-message-to-find");  
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                Object[] params = {idName}; 
                if (msg.equals("")) {           
                    if (useVerboseOption) {
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
            } else if (expectedExitCode.equals(
                    new Integer(INVALID_OPTION_STATUS).toString())) {
                expectedMessage = (String) rb.getString("usage");
            } else {
                expectedMessage = msg;                
            }

            cli.resetArgList();
            commandStatus = cli.getIdentity(idRealm, idName, idType, 
                    attributeNames);
            cli.logCommand("testGetIdentityAttributes");
            cli.resetArgList();
            
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                FederationManagerCLI searchCLI = 
                        new FederationManagerCLI(useDebugOption, 
                        useVerboseOption, useLongOptions);
                if (attributesToFind.length() > 0) {
                    attributesFound = searchCLI.findIdentityAttributes(idRealm, 
                            idName, idType, attributeNames, attributesToFind);
                }
                searchCLI.logCommand("testGetIdentityAttributes");
                searchCLI.resetArgList();
                if (attributesNotFound.length() > 0) {
                    String[] attributes = attributesNotFound.split(";");
                    for (int i=0; i < attributes.length; i++) {
                        log(Level.FINE, "testGetIdentityAttributes", 
                                "Searching for attribute " + attributes[i] +
                                "which should not appear.");
                        if (searchCLI.findIdentityAttributes(idRealm, idName, 
                                 idType, attributeNames, attributes[i])) {
                             log(Level.SEVERE, "testGetIdentityAttributes", 
                                     "Found unexpected attirbute " + 
                                     attributes[i]);
                             otherAttributesFound = true;
                         }
                    }
                }
            } else if (expectedExitCode.equals(
                    new Integer(INVALID_OPTION_STATUS).toString())) {
                String argString = cli.getAllArgs().replaceFirst(
                        cli.getCliPath() + fileseparator + "ssoadm", "ssoadm ");
                Object[] params = {argString};
                String errorMessage = 
                        (String) rb.getString("invalid-usage-message");
                String usageError = MessageFormat.format(errorMessage, params);
                errorFound = cli.findStringsInError(usageError, ";");
            } else {
                errorFound = cli.findStringsInError(expectedMessage, ";");
            }
            
            if (!expectedMessage.equals("")) {
                if (expectedExitCode.equals(
                        new Integer(SUCCESS_STATUS).toString()) ||
                        expectedExitCode.equals(
                        new Integer(INVALID_OPTION_STATUS).toString())) {
                     stringsFound = cli.findStringsInOutput(expectedMessage, 
                             ";");                    
                }
            } else {
                stringsFound = true;
            }
            cli.resetArgList();
                   
            log(Level.FINEST, "testGetIdentityAttributes", "Exit status: " + 
                    commandStatus);
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                log(Level.FINEST, "testGetIdentityAttributes", 
                        "Output Messages Found: " + stringsFound);
                log(Level.FINEST, "testGetIdentityAttributes", 
                        "Attributes Found: " + attributesFound);
                log(Level.FINEST, "testGetIdentityAttributes",
                        "Other Attributes Found: " + otherAttributesFound);
                assert (commandStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound &&
                        attributesFound && !otherAttributesFound;
            } else {
                log(Level.FINEST, "testGetIdentityAttributes", 
                        "Error Messages Found: " + errorFound);
                if (expectedExitCode.equals(
                        new Integer(INVALID_OPTION_STATUS).toString())) {
                    log(Level.FINEST, "testGetIdentityAttributes", 
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
            exiting("testGetIdentityAttributes");
        } catch (Exception e) {
            log(Level.SEVERE, "testGetIdentityAttributes", e.getMessage(), 
                    null);
            e.printStackTrace();
            throw e;
        } 
    }
    
    /**
     * This method remove any realms and identities that were created during 
     * the setup and testGetIdentityAttributes methods using 
     * "ssoadm delete-identities" and "ssoadm delete-realm".
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
                        log(Level.FINE, "cleanup", "Removing identity " + 
                                cleanupIds[i]);
                        String [] idArgs = cleanupIds[i].split("\\,");
                        if (idArgs.length >= 3) {
                            String cleanupRealm = idArgs[0];
                            String cleanupName = idArgs[1];
                            String cleanupType = idArgs[2];
                            
                            log(Level.FINEST, "cleanup", "idRealm: " + 
                                cleanupRealm);
                            log(Level.FINEST, "cleanup", "idName: " + 
                                    cleanupName);
                            log(Level.FINEST, "cleanup", "idType: " + 
                                    cleanupType);
                            
                            Reporter.log("IdRealm: " + cleanupRealm);
                            Reporter.log("IdNameToRemove: " + cleanupName);
                            Reporter.log("IdentityTypeToRemove: " + 
                                    cleanupType);
                            
                            exitStatus = 
                                    cli.deleteIdentities(cleanupRealm, 
                                    cleanupName, cleanupType);
                            cli.logCommand("cleanup");
                            cli.resetArgList();
                            if (exitStatus != new Integer(SUCCESS_STATUS).
                                    intValue()) {
                                log(Level.SEVERE, "cleanup", 
                                        "Removal of identity " + cleanupName + 
                                        " failed with exit status " + 
                                        exitStatus);
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
                    log(Level.FINE, "cleanup", "Removing realm " + 
                        realms[i]);
                    Reporter.log("SetupRealmToDelete: " + realms[i]);
                    exitStatus = cli.deleteRealm(realms[i], true); 
                    cli.logCommand("cleanup");
                    cli.resetArgList();
                    if (exitStatus != SUCCESS_STATUS) {
                        log(Level.SEVERE, "cleanup", "The removal of realm " + 
                                realms[i] + " failed with exit status " + 
                                exitStatus);
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

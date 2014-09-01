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
 * $Id: CreateIdentityTest.java,v 1.13 2009/01/26 23:48:57 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/**
 * CreateIdentityTest automates the following test cases:
 * CLI_create-identity01, CLI_create-identity02, CLI_create-identity03, 
 * CLI_create-identity04, CLI_create-identity05, CLI_create-identity06, 
 * CLI_create-identity07, CLI_create-identity08, CLI_create-identity09, 
 * CLI_create-identity10, CLI_create-identity11, CLI_create-identity12, 
 * CLI_create-identity13, CLI_create-identity14, CLI_create-identity15,
 * CLI_create-identity16, CLI_create-identity17, CLI_create-identity18, 
 * CLI_create-identity19, CLI_create-identity20, CLI_create-identity21, 
 * CLI_create-identity22, CLI_create-identity23, CLI_create-identity24, 
 * CLI_create-identity25, CLI_create-identity26, CLI_create-identity27,
 * CLI_create-identity28, CLI_create-identity29, CLI_create-identity30, 
 * CLI_create-identity31, CLI_create-identity32, CLI_create-identity33, 
 * CLI_create-identity34, CLI_create-identity35, CLI_create-identity36,
 * CLI_create-identity37, CLI_create-identity38, CLI_create-identity39.
 */

package com.sun.identity.qatest.cli;

import com.sun.identity.qatest.common.cli.CLIExitCodes;
import com.sun.identity.qatest.common.cli.FederationManagerCLI;
import com.sun.identity.qatest.common.TestCommon;
import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.logging.Level;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.Reporter;

/**
 * <code>CreateIdentityTest</code> is used to execute tests involving the 
 * create-identity sub-command of ssoadm.  This class allows the user to execute
 * "ssoadm create-identity" with a variety or arguments (e.g with short or long 
 * options, with a password file or password argument, with a locale argument,
 * with a list of attributes or a datafile containing attributes, etc.) 
 * and a variety of input values.  The properties file 
 * <code>CreateIdentityTest.properties</code> contains the input values which 
 * are read by this class.
 */
public class CreateIdentityTest extends TestCommon implements CLIExitCodes {
    
    private String locTestName;
    private ResourceBundle rb;
    private String setupRealms;
    private String setupIdentities;
    private String realmForId;
    private String idNameToCreate;
    private String idTypeToCreate;
    private String idAttributeValues;
    private boolean useVerboseOption;
    private boolean useDebugOption;
    private boolean useLongOptions;
    private boolean useAttributeValuesOption;
    private boolean useDatafileOption;
    private String expectedMessage;
    private String expectedExitCode;
    private String description;
    private FederationManagerCLI cli;
    
    /** 
     * Creates a new instance of CreateIdentityTest 
     */
    public CreateIdentityTest() {
        super("CreateIdentityTest");
    }
    
    /**
     * This method is intended to provide initial setup.
     * Creates any realms specified in the setup-realms property and creates
     * any identities specified in the setup-identities property in the 
     * CreateIdentityTest.properties.
     */
    @Parameters({"testName"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String testName) 
    throws Exception {
        Object[] params = {testName};
        entering("setup", params);
        try {
            locTestName = testName;
            rb = ResourceBundle.getBundle("cli" + fileseparator + 
                    "CreateIdentityTest-Generated");
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
                
            log(logLevel, "setup", "use-verbose-option: " + useVerboseOption);
            log(logLevel, "setup", "use-debug-option: " + useDebugOption);
            log(logLevel, "setup", "use-long-options: " + useLongOptions);
            log(logLevel, "setup", "create-setup-realms: " + setupRealms);
            log(logLevel, "setup", "create-setup-identities: " + 
                    setupIdentities);
             
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("SetupRealms: " + setupRealms);
            Reporter.log("SetupIdentities: " + setupIdentities);

            cli = new FederationManagerCLI(useDebugOption, useVerboseOption, 
                    useLongOptions);
            
            if (setupRealms != null && !setupRealms.equals("")) {
                if (!cli.createRealms(setupRealms)) {
                    log(Level.SEVERE, "setup", 
                            "All the realms failed to be created.");
                    assert false;
                }
            }
            
            if (setupIdentities != null) {
                if (setupIdentities.length() > 0) {
                    StringTokenizer idTokenizer = 
                            new StringTokenizer(setupIdentities, "|");
                    while (idTokenizer.hasMoreTokens()) {
                        StringTokenizer tokenizer = 
                                new StringTokenizer(idTokenizer.nextToken(), 
                                ",");
                        if (tokenizer.countTokens() >= 3) {
                            String idRealm = tokenizer.nextToken();
                            String idName = tokenizer.nextToken();
                            String idType = tokenizer.nextToken();
                            String idAttributes = null;
                            if (tokenizer.hasMoreTokens()) {
                                idAttributes = tokenizer.nextToken();
                                cli.createIdentity(idRealm, idName, idType, 
                                    idAttributes);
                            } else {
                                cli.createIdentity(idRealm, idName, idType);
                            }
                            cli.logCommand("setup");
                        } else {
                            log(Level.SEVERE, "setup", "The setup identity " + 
                                    setupIdentities + 
                                    " must have a realm, an " +
                                    "identity name, and an identity type");
                            assert false;
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
     * This method is used to execute tests involving "ssoadm create-identities"
     * using input data from the CreateIdentityTest.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testIdentityCreation() 
    throws Exception {
        entering("testIdentityCreation", null);
        boolean stringsFound = false;
        boolean idFound = false;
        
        try {
            expectedMessage = (String) rb.getString(locTestName + 
                    "-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-expected-exit-code");
            realmForId = (String) rb.getString(locTestName + 
                    "-create-identity-realm");
            idNameToCreate = (String) rb.getString(locTestName + 
                    "-create-identity-name");
            idTypeToCreate = (String) rb.getString(locTestName + 
                    "-create-identity-type");
            useAttributeValuesOption = ((String)rb.getString(locTestName + 
                    "-use-attribute-values-option")).equals("true");
            useDatafileOption = ((String)rb.getString(locTestName + 
                    "-use-datafile-option")).equals("true"); 
            idAttributeValues = (String) rb.getString(locTestName + 
                    "-create-identity-attributes");
            description = (String) rb.getString(locTestName + "-description");

            log(logLevel, "testIdentityCreation", "description: " + 
                    description);
            log(logLevel, "testIdentityCreation", "use-debug-option: " + 
                    useDebugOption);
            log(logLevel, "testIdentityCreation", "use-verbose-option: " + 
                    useVerboseOption);
            log(logLevel, "testIdentityCreation", "use-long-options: " + 
                    useLongOptions);
            log(logLevel, "testIdentityCreation", "message-to-find: " + 
                    expectedMessage);
            log(logLevel, "testIdentityCreation", "expected-exit-code: " + 
                    expectedExitCode);
            log(logLevel, "testIdentityCreation", "create-identity-realm: " + 
                    realmForId);
            log(logLevel, "testIdentityCreation", "create-identity-name: " + 
                    idNameToCreate);
            log(logLevel, "testIdentityCreation", "create-identity-type: " + 
                    idTypeToCreate);
            log(logLevel, "testIdentityCreation", 
                    "use-attribute-values-option: " + useAttributeValuesOption);
            log(logLevel, "testIdentityCreation", "use-datafile-option: " + 
                    useDatafileOption);
            log(logLevel, "testIdentityCreation", 
                    "create-identity-attributes: " + idAttributeValues);

            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);
            Reporter.log("IdRealmToCreate: " + realmForId);
            Reporter.log("IdNameToCreate: " + idNameToCreate);
            Reporter.log("IdTypeToCreate: " + idTypeToCreate);
            Reporter.log("UseAttributeValuesOption: " + 
                    useAttributeValuesOption);
            Reporter.log("UseDatafileOption: " + useDatafileOption);
            Reporter.log("IdAttributeValues: " + idAttributeValues);
            
            int commandStatus = cli.createIdentity(realmForId, idNameToCreate, 
                    idTypeToCreate, idAttributeValues, useAttributeValuesOption,
                    useDatafileOption);
            cli.logCommand("testIdentityCreation");

            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                stringsFound = cli.findStringsInOutput(expectedMessage, ";");
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
            }     
            cli.resetArgList();

            if (idNameToCreate.length() > 0) {
                FederationManagerCLI listCLI = 
                        new FederationManagerCLI(useDebugOption, 
                        useVerboseOption, useLongOptions);
                idFound = listCLI.findIdentities(realmForId, idNameToCreate, 
                        idTypeToCreate, idNameToCreate);
                log(logLevel, "testIdentityCreation", idTypeToCreate + 
                        "identity " + idNameToCreate + " Found: " + idFound);
            }

            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                log(logLevel, "testIdentityCreation", "Output Messages Found: "
                        + stringsFound);
                log(logLevel, "testIdentityCreation", "ID Found: " + 
                        idFound); 
                assert (commandStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound &&
                        idFound;
            } else {
                log(logLevel, "testIdentityCreation", 
                        "Error Messages Found: " + stringsFound);
                assert (commandStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound;
            }     
            exiting("testIdentityCreation");
        } catch (Exception e) {
            log(Level.SEVERE, "testIdentityCreation", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        } 
    }
    
    /**
     * This method remove any realms and identities that were created during 
     * the setup and testIdentityCreation methods using "ssoadm delete-realm" 
     * and "ssoadm delete-identities".
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup() 
    throws Exception {
        int cleanupExitStatus = -1;
        FederationManagerCLI idCli = null;
        FederationManagerCLI cleanupCli = null;

        entering("cleanup", null);
        try {            
            log(logLevel, "cleanup", "useDebugOption: " + useDebugOption);
            log(logLevel, "cleanup", "useVerboseOption: " + useVerboseOption);
            log(logLevel, "cleanup", "useLongOptions: " + useLongOptions);
            
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            
            if (!idNameToCreate.equals("")) {
                log(logLevel, "cleanup", "identityToDelete: "  + 
                        idNameToCreate);
                Reporter.log("IdentityNameToDelete: " + idNameToCreate);
                idCli = new FederationManagerCLI(useDebugOption, 
                                    useVerboseOption, useLongOptions);
                idCli.deleteIdentities(realmForId, idNameToCreate, 
                        idTypeToCreate);
                idCli.logCommand("cleanup");
                idCli.resetArgList();
            }

            if (setupIdentities != null) {
                if (setupIdentities.length() > 0) {
                    String [] cleanupIds = setupIdentities.split("\\|");
                    for (int i = 0; i < cleanupIds.length; i++) {
                        String [] idArgs = cleanupIds[i].split("\\,");
                        if (idArgs.length >= 3) {
                            String idRealm = idArgs[0];
                            String idName = idArgs[1];
                            String idType = idArgs[2];
                            
                            log(Level.FINEST, "cleanup", "idRealm: " + 
                                idRealm);
                            log(Level.FINEST, "cleanup", "idName: " + idName);
                            log(Level.FINEST, "cleanup", "idType: " + idType);
                            
                            Reporter.log("IdRealm: " + idRealm);
                            Reporter.log("IdNameToRemove: " + idName);
                            Reporter.log("IdentityTypeToRemove: " + idType);

                            idCli.resetArgList();
                            if (idCli.findIdentities(idRealm, "*", idType,
                                    idName)) {
                                cleanupCli =
                                        new FederationManagerCLI(useDebugOption,
                                        useVerboseOption, useLongOptions);
                                cleanupExitStatus =
                                        cleanupCli.deleteIdentities(idRealm,
                                        idName, idType);
                                cleanupCli.resetArgList();
                                if (cleanupExitStatus != SUCCESS_STATUS) {
                                    log(Level.SEVERE, "cleanup",
                                            "The deletion of " + idType +
                                            " identity " + idName +
                                            " returned the failed exit status "
                                            + cleanupExitStatus + ".");
                                    assert false;
                                }
                                if (cli.findIdentities(idRealm, "*", idType,
                                        idName)) {
                                    log(Level.SEVERE, "cleanup", "The " +
                                            idType + "identity " + idName +
                                            " was not deleted.");
                                    assert false;
                                }
                                cli.resetArgList();
                            } else {
                               log(Level.FINEST, "cleanup", "The " + idType +
                                       " identity " + idName +
                                       " was not found.");
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
                    cleanupExitStatus = cli.deleteRealm(realms[i], true); 
                    cli.logCommand("cleanup");
                    if (cleanupExitStatus != SUCCESS_STATUS) {
                        assert false;
                    }
                } 
            }            
            exiting("cleanup");
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        } finally {
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
    }
}

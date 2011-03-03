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
 * $Id: UpdateDatastoreTest.java,v 1.1 2009/02/11 19:23:14 srivenigan Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */

/**
 * <code>UpdateDatastoresTest</code> automates the following test cases:
 * CLI_update-datastore01, CLI_update-datastore02, CLI_update-datastore03
 * CLI_update-datastore04, CLI_update-datastore05, CLI_update-datastore06
 * CLI_update-datastore07, CLI_update-datastore08, CLI_update-datastore09
 * CLI_update-datastore10
 */

package com.sun.identity.qatest.cli;

import com.sun.identity.qatest.common.LDAPCommon;
import com.sun.identity.qatest.common.SMSConstants;
import com.sun.identity.qatest.common.cli.CLIExitCodes;
import com.sun.identity.qatest.common.cli.FederationManagerCLI;
import com.sun.identity.qatest.common.TestCommon;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.Reporter;

/**
 * <code>UpdateDatastoreTest</code> is used to execute tests involving the
 * update-datastore sub-command of ssoadm.  This class allows the user to 
 * execute "ssoadm update-datastore" with a variety of arguments (e.g with short
 * or long options, with a password file or password argument, with a locale
 * argument, etc.) and a variety of input values. The properties file
 * <code>UpdateDatastoreTest.properties</code> contains the input values
 * which are read by this class.
 */
public class UpdateDatastoreTest extends TestCommon implements CLIExitCodes {

    private List list;
    private String locTestName;
    private ResourceBundle rb;
    private String setupDatastores;
    private String updateDatastore;
    private String realmName;
    private String datastoreType;
    private String attrValues;
    private boolean useDataFile;
    private boolean useVerboseOption;
    private boolean useDebugOption;
    private boolean useLongOptions;
    private String expectedMessage;
    private String expectedExitCode;
    private String description;
    private FederationManagerCLI cli;
    boolean identitiesCreated = false;
    private String ldapServerSchemaName = 
        "sun-idrepo-ldapv3-config-ldap-server=";
    private String orgSchemaName = 
        "sun-idrepo-ldapv3-config-organization_name=";
    private String authIdSchema = "sun-idrepo-ldapv3-config-authid=";
    private String authPwSchema = "sun-idrepo-ldapv3-config-authpw=";

    /**
     * Creates a new instance of UpdateDatastoreTest
     */
    public UpdateDatastoreTest() {
        super("UpdateDatastoreTest");
    }

    /**
     * This method is intended to provide initial setup.
     * setup loads the properties in UpdateDatastoreTest.properties.
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
            		"UpdateDatastoreTest");
            realmName =  (String) rb.getString(locTestName +
                    "-realm-name");
            datastoreType =  (String) rb.getString(locTestName +
            	    "-datastore-type");
            setupDatastores = (String) rb.getString(locTestName +
                    "-setup-datastores");            
            useVerboseOption = ((String)rb.getString(locTestName +
                    "-use-verbose-option")).equals("true");
            useDebugOption = ((String)rb.getString(locTestName +
                    "-use-debug-option")).equals("true");
            useLongOptions = ((String)rb.getString(locTestName +
                    "-use-long-options")).equals("true");
            expectedExitCode = (String) rb.getString(locTestName +
                    "-expected-exit-code");

            log(Level.FINEST, "setup", "use-verbose-option: " +
                    useVerboseOption);
            log(Level.FINEST, "setup", "use-debug-option: " + useDebugOption);
            log(Level.FINEST, "setup", "use-long-options: " + useLongOptions);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);

            cli = new FederationManagerCLI(useDebugOption, useVerboseOption, 
                    useLongOptions);
            if (!realmName.equals("/")) {
            	int exitStatus = cli.createRealm(realmName);
                log(Level.FINE, "setup", "Creating Realm: " + realmName);
            	cli.logCommand("setup");
                if (exitStatus != SUCCESS_STATUS) {
                    log(Level.WARNING, "setup",
                            "Realm creation returned the failed exit "
                            + "status " + exitStatus + ".");
                    assert false;
                }            	
            }
            cli.resetArgList();
            if (!setupDatastores.trim().equals("")) {
                // Create test datastores.
                String[] datastoreStrings = setupDatastores.split(",");
                String[] datastoreTypes = datastoreType.split(";");
                for (int i = 0; i < datastoreStrings.length; i++) {
                    String attrValues =
                            "sun-idrepo-ldapv3-config-referrals=true";
                    // Setup schema in the directories.
                    LDAPCommon ldc = null;
                    ResourceBundle dsCommon = ResourceBundle.getBundle("cli" + 
                            fileseparator + "DatastoreCommon");        
                    String ldapServer = dsCommon.getString(datastoreTypes[i] + 
                           "-ldap-server");
                    String ldapPort = dsCommon.getString(datastoreTypes[i] +
                           "-ldap-port");
                    String ldapBindDN = dsCommon.getString(datastoreTypes[i] + 
                            "-authid");
                    String ldapPwd = dsCommon.getString(datastoreTypes[i] + 
                            "-authpw");
                    String orgName = dsCommon.getString(datastoreTypes[i] + 
                            "-root-suffix");

                   log(Level.FINEST, "testDatastoreCreation", "Ldap Server: " + 
                           ldapServer);
                   log(Level.FINEST, "testDatastoreCreation", "Ldap Port: " + 
                           ldapPort);
                   log(Level.FINEST, "testDatastoreCreation", "Ldap Bind DN: " + 
                           ldapBindDN);
                   log(Level.FINEST, "testDatastoreCreation", "Ldap Password: " 
                           + ldapPwd);
                   log(Level.FINEST, "testDatastoreCreation", "Organization " +
                           "Name: " + orgName);
                   
                   ldc = new LDAPCommon(ldapServer, ldapPort, ldapBindDN, 
                           ldapPwd, orgName);
                    ResourceBundle smsGblCfg = ResourceBundle.getBundle(
                            "config" + fileseparator + "default" +
                            fileseparator + "UMGlobalConfig");
                    if (datastoreTypes[i].equals("LDAPv3ForAMDS")) {
                        String schemaString = (String) smsGblCfg.getString(
                                SMSConstants.UM_SCHEMNA_LIST + "." +
                                SMSConstants.UM_DATASTORE_SCHEMA_TYPE_AMDS);
                        String schemaAttributes = (String) smsGblCfg.getString(
                                SMSConstants.UM_SCHEMNA_ATTR + "." +
                                SMSConstants.UM_DATASTORE_SCHEMA_TYPE_AMDS);
                        ldc.loadAMUserSchema(schemaString,
                                schemaAttributes);
                        ldc.disconnectDServer();
                        Thread.sleep(5000);
                    } else if (datastoreTypes[i].equals("LDAPv3ForAD")) {
                        String schemaString = (String) smsGblCfg.getString(
                                SMSConstants.UM_SCHEMNA_LIST + "." +
                                SMSConstants.UM_DATASTORE_SCHEMA_TYPE_AD);
                        String schemaAttributes = (String) smsGblCfg.getString(
                                SMSConstants.UM_SCHEMNA_ATTR + "." +
                                SMSConstants.UM_DATASTORE_SCHEMA_TYPE_AD);
                        ldc.loadAMUserSchema(schemaString,
                                schemaAttributes);
                        ldc.disconnectDServer();
                        Thread.sleep(5000);

                        //Add user attributes of LDAPv3ForAD to attribute list.
                        ResourceBundle cfgData = ResourceBundle.getBundle(
                                "config" + fileseparator + "default" + 
                                fileseparator + 
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX);
                        String userAtt = cfgData.getString(
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." 
                                + datastoreTypes[i] + "." +
                                SMSConstants.UM_LDAPv3_USER_ATTR);
                        list = new ArrayList<String>();
                        if (userAtt.indexOf("|") != 0) {
                            List locList = getAttributeList(userAtt, "|");
                            for (int j = 0; j < locList.size(); j++) {
                                list.add(SMSConstants.UM_LDAPv3_USER_ATTR +
                                        "=" + (String) locList.get(j));
                            }
                        }
                        StringBuffer buff = new StringBuffer(attrValues);
                        if (!attrValues.equals("")) {
                            buff.append(";");
                        }
                        for (int k = 0; k < list.size(); k++) {
                            buff.append((String) list.get(k) + ";");
                        }
                        attrValues = buff.toString();
                        log(Level.FINEST, "testDatastoreCreation", "Attribute" +
                                " values: " + attrValues);
                    }

                    StringBuffer buf = new StringBuffer(attrValues);
                    if (!attrValues.equals("")) {
                        buf.append(";");
                    }
                    buf.append(ldapServerSchemaName + ldapServer + ":" + 
                            ldapPort + ";");
                    buf.append(authIdSchema + ldapBindDN + ";");
                    buf.append(authPwSchema + ldapPwd + ";");
                    buf.append(orgSchemaName + orgName);
                    attrValues = buf.toString();
                    log(Level.FINEST, "setup", "Attribute" +
                            " values: " + attrValues);

                    int exitStatus = cli.createDatastore(realmName,
                            datastoreStrings[i], datastoreTypes[i], attrValues,
                            false);
                    log(Level.FINE, "setup", "Creating datastore: " + 
                            datastoreStrings[i]);
                    cli.logCommand("setup");
                    if (exitStatus == SUCCESS_STATUS) {
                        log(Level.FINEST, "setup", "Created " + "datastore: " + 
                                datastoreStrings[i]);
                        cli.resetArgList();
                    } else {
                        cli.resetArgList();
                        if (!expectedExitCode.equals(
                                new Integer(exitStatus).toString())) {
                            log(Level.FINEST, "setup", "Failed to create the " +
                                    "setup datastores: " + datastoreStrings[i]);
                            assert false;
                        }
                    }
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
     * This method is used to execute tests involving "ssoadm update-datastore"
     * using input data from the UpdateDatastoreTest.properties file and 
     * verifies datastore updation using "ssoadm show-datastore".
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testDatastoreModification()
    throws Exception {
        entering("testDatastoreModification", null);
        boolean stringsFound = false;

        try {
            description = (String) rb.getString(locTestName + "-description");
            updateDatastore = (String) rb.getString(locTestName +
                    "-update-datastore");
            realmName =  (String) rb.getString(locTestName +
                    "-realm-name");
            attrValues =  (String) rb.getString(locTestName +
                    "-attribute-values");
            useDataFile =  ((String) rb.getString(locTestName +
                    "-use-datafile-option")).equals("true");
            expectedMessage = (String) rb.getString(locTestName +
                    "-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName +
                    "-expected-exit-code");

            log(Level.FINEST, "testDatastoreModification", "description: " +
                    description);
            log(Level.FINEST, "testDatastoreModification", "create-datastore: " 
                    + updateDatastore);
            log(Level.FINEST, "testDatastoreModification", "realm-name: " +
                    realmName);
            log(Level.FINEST, "testDatastoreModification", "attribute-values: " 
                    + attrValues);
            log(Level.FINEST, "testDatastoreModification", "use-data-file: " +
                    useDataFile);
            log(Level.FINEST, "testDatastoreModification", "use-verbose-option: " 
                    + useVerboseOption);
            log(Level.FINEST, "testDatastoreModification", "use-long-options: " 
                    + useLongOptions);
            log(Level.FINEST, "testDatastoreModification", "message-to-find: " +
                    expectedMessage);
            log(Level.FINEST, "testDatastoreModification", "expected-exit-code: " 
                    + expectedExitCode);

            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);
            Reporter.log("UpdateDatastore: " + updateDatastore);
            Reporter.log("RealmName: " + realmName);
            Reporter.log("AttributeValues: " + attrValues);
            Reporter.log("UseDataFile: " + useDataFile);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);

            log(Level.FINEST, "testDatastoreModification", "DataStoreToCreate: " 
                    + updateDatastore);
            int commandStatus = cli.updateDatastore(realmName, updateDatastore, 
            		attrValues, useDataFile);
            cli.logCommand("testDatastoreModification");

            if (expectedExitCode.equals(new Integer(
                    SUCCESS_STATUS).toString())) {
                stringsFound = cli.findStringsInOutput(expectedMessage, ";");
                log(Level.FINEST, "testDatastoreModification", "DataStore " +
                        updateDatastore + " is created. ");
                assert (commandStatus == new Integer(
                        expectedExitCode).intValue()) && stringsFound;
                cli.resetArgList();
                // verify updated datastore using "show-datastore", by 
                // creating three users, updating the filter of the created 
                // datastore and list the users with updated search filter.  
                FederationManagerCLI displayCLI = new FederationManagerCLI(
                        useDebugOption, useVerboseOption, useLongOptions);
                int exitStatus = displayCLI.showDatastore(
                        realmName, updateDatastore);
                displayCLI.logCommand("testDatastoreCreation");
                assert (exitStatus ==
                    new Integer(expectedExitCode).intValue());
                boolean attrsFound =
                        displayCLI.findStringsInOutput(attrValues, ";");
                displayCLI.resetArgList();
                if (attrsFound) {
                    log(Level.FINEST, "testDatastoreDeletion", 
                         "Datastore update command verified successfully");
                } else {
                    log(Level.FINEST, "testDatastoreDeletion", 
                            "Datastore update command failed");
                    assert false; 
                }                  
                identitiesCreated = cli.createIdentities(realmName + 
                        ",user1," + "User|" + realmName + ",user2," 
                        + "User|" + realmName + ",user11," + "User");
                cli.logCommand("testDatastoreCreation");
                assert (exitStatus ==
                        new Integer(expectedExitCode).intValue());                     
                cli.resetArgList();
                exitStatus = cli.updateDatastore(
                        realmName, updateDatastore, 
                        "sun-idrepo-ldapv3-config-users-search-" +
                        "filter=(uid=user1*)", false);
                cli.logCommand("testDatastoreCreation");
                assert (exitStatus == new Integer(
                        expectedExitCode).intValue());    
                cli.resetArgList();
                log(Level.FINEST, "testDatastoreDeletion", "Datastore " 
                        + "ldap search filter updated successfully");
                exitStatus = cli.listIdentities(realmName, 
                        "user1*", "User");
                cli.logCommand("testDatastoreCreation");
                assert (exitStatus == new Integer(
                        expectedExitCode).intValue()); 
                attrsFound = cli.findStringsInOutput("user2", ";");
                if (!attrsFound) {
                    log(Level.FINEST, "testDatastoreDeletion", 
                            "Ldap search filter filtered user - " +
                            "Success.");
                } else {
                    log(Level.FINEST, "testDatastoreDeletion", "Datastore " 
                            + "Ldap search filter failed to update.");
                }
            } else {
                if (!expectedExitCode.equals(
                        new Integer(INVALID_OPTION_STATUS).toString())) {
                    stringsFound = cli.findStringsInError(expectedMessage, ";");
                } else {
                    String argString = cli.getAllArgs().
                          replaceFirst(cli.getCliPath(), "ssoadm ");
                    Object[] params = {argString};
                    String usageError = MessageFormat.format(expectedMessage,
                            params);
                    stringsFound = cli.
                            findStringsInError(usageError, ";" + newline);
                }
                log(logLevel, "testDatastoreModification", 
                        "Error Messages Found: " + stringsFound);
                assert (commandStatus == new Integer(
                        expectedExitCode).intValue()) && stringsFound;
            }
            cli.resetArgList();
            exiting("testDatastoreModification");
        } catch (Exception e) {
            log(Level.SEVERE, "testDatastoreModification", e.getMessage(), null);
            cleanup();
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Cleanup the datastores that were created during the setup and
     * testDatastoreModification methods using "ssoadm delete-datastore".
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
            if (!updateDatastore.trim().equals("")) {
                if (identitiesCreated) {
                    int exitStatus = cli.deleteIdentities(realmName, "user1 " +
                            "user2 user11", "User");
                    cli.logCommand("cleanup");
                    if (exitStatus != SUCCESS_STATUS) {
                        log(Level.SEVERE, "cleanup",
                                "Datastore deletion returned the failed exit "
                                + "status " + exitStatus + ".");
                        assert false;                        
                    }
                    cli.resetArgList();
                }
                int exitStatus = cli.deleteDatastores(realmName,
                        updateDatastore);
                cli.logCommand("cleanup");
                if (exitStatus != SUCCESS_STATUS) {
                    log(Level.SEVERE, "cleanup",
                            "Datastore deletion returned the failed exit "
                            + "status " + exitStatus + ".");
                    assert false;
                }
                cli.resetArgList();
            }
            if (!realmName.equals("/")) {
                int exitStatus = cli.deleteRealm(realmName);
                cli.logCommand("cleanup");
                if (exitStatus != SUCCESS_STATUS) {
                    log(Level.SEVERE, "cleanup",
                            "Realm deletion returned the failed exit "
                            + "status " + exitStatus + ".");
                    assert false;
                }
            }
            cli.resetArgList();
            exiting("cleanup");
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        }
    }
}

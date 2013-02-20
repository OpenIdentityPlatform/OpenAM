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
 * $Id: CreateDatastoreTest.java,v 1.4 2009/06/02 17:08:40 cmwesley Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */

/**
 * CreateDataStoreTest automates the following test cases:
 * CLI_create-datastore01, CLI_create-datastore02, CLI_create-datastore03
 * CLI_create-datastore04, CLI_create-datastore05, CLI_create-datastore06
 * CLI_create-datastore07, CLI_create-datastore08, CLI_create-datastore09
 * CLI_create-datastore10
 */

package com.sun.identity.qatest.cli;

import com.iplanet.sso.SSOToken;
import com.sun.identity.qatest.common.LDAPCommon;
import com.sun.identity.qatest.common.SMSConstants;
import com.sun.identity.qatest.common.cli.CLIExitCodes;
import com.sun.identity.qatest.common.cli.FederationManagerCLI;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.authentication.AuthenticationCommon;
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
 * <code>CreateDatastoreTest</code> is used to execute tests involving the
 * create-datastore sub-command of ssoadm.  This class allows the user to 
 * execute "ssoadm create-datastore" with a variety of arguments (e.g with short
 * or long options, with a password file argument, with a locale argument, etc.) 
 * and a variety of input values. The properties file 
 * <code>CreateDatastoreTest.properties</code> contains the input values
 * which are read by this class.
 */
public class CreateDatastoreTest extends TestCommon implements CLIExitCodes {

    private String locTestName;
    private ResourceBundle rb;
    private String createDatastore;
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
    private boolean authInstanceCreated = false;
    private boolean identityCreated = false;
    private String ldapServerSchemaName = 
            "sun-idrepo-ldapv3-config-ldap-server=";
    private String orgSchemaName = 
            "sun-idrepo-ldapv3-config-organization_name=";
    private String authIdSchema = "sun-idrepo-ldapv3-config-authid=";
    private String authPwSchema = "sun-idrepo-ldapv3-config-authpw=";
    private List<String> list;

    /**
     * Creates a new instance of CreateDatastoreTest
     */
    public CreateDatastoreTest() {
        super("CreateDatastoreTest");
        rb = ResourceBundle.getBundle("cli" + fileseparator +
                "CreateDatastoreTest");        
    }

    /**
     * This method is intended to provide initial setup.
     * setup loads the properties in CreateDatastoreTest.properties.
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
            realmName = (String) rb.getString(locTestName +
                    "-realm-name");
            useVerboseOption = ((String) rb.getString(locTestName +
                    "-use-verbose-option")).equals("true");
            useDebugOption = ((String) rb.getString(locTestName +
                    "-use-debug-option")).equals("true");
            useLongOptions = ((String) rb.getString(locTestName +
                    "-use-long-options")).equals("true");

            log(Level.FINEST, "setup", "realm-name:" + realmName);
            log(Level.FINEST, "setup", "use-verbose-option: " +
                    useVerboseOption);
            log(Level.FINEST, "setup", "use-debug-option: " + useDebugOption);
            log(Level.FINEST, "setup", "use-long-options: " + useLongOptions);
            Reporter.log("Realm Name: " + realmName);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);

            cli = new FederationManagerCLI(useDebugOption, useVerboseOption,
                    useLongOptions);
            if (!realmName.equals("/")) {
                int exitStatus = cli.createRealm(realmName);
                log(Level.FINE, "setup", "Creating realm: " + realmName);
                cli.logCommand("setup");
                if (exitStatus != SUCCESS_STATUS) {
                    log(Level.SEVERE, "setup",
                            "Realm creation returned the failed exit " + 
                            "status " + exitStatus + ".");
                    assert false;
                }
            }
            cli.resetArgList();
            exiting("setup");
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage(), null);
            cleanup();
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * This method is used to execute tests involving "ssoadm create-datastore"
     * using input data from the CreateDataStoreTest.properties file and 
     * verifies datastore creation using "ssoadm create-identity".
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testDatastoreCreation()
    throws Exception {
    	entering("testDatastoreCreation", null);
        boolean stringsFound = false;

        try {
            description = (String) rb.getString(locTestName + "-description");
            createDatastore = (String) rb.getString(locTestName +
                    "-create-datastore");
            realmName =  (String) rb.getString(locTestName +
                    "-realm-name");
            datastoreType =  (String) rb.getString(locTestName +
                    "-datastore-type");
            attrValues =  (String) rb.getString(locTestName +
                    "-attribute-values");
            useDataFile =  ((String) rb.getString(locTestName +
                    "-use-datafile-option")).equals("true");
            expectedMessage = (String) rb.getString(locTestName +
                    "-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName +
                    "-expected-exit-code");

            log(Level.FINEST, "testDatastoreCreation", "description: " +
                    description);
            log(Level.FINEST, "testDatastoreCreation", "create-datastore: " +
                    createDatastore);
            log(Level.FINEST, "testDatastoreCreation", "realm-name: " +
                    realmName);
            log(Level.FINEST, "testDatastoreCreation", "datastore-type: " +
                    datastoreType);
            log(Level.FINEST, "testDatastoreCreation", "attribute-values: " +
                    attrValues);
            log(Level.FINEST, "testDatastoreCreation", "use-data-file: " +
                    useDataFile);
            log(Level.FINEST, "testDatastoreCreation", "use-verbose-option: " +
                    useVerboseOption);
            log(Level.FINEST, "testDatastoreCreation", "use-long-options: " +
                    useLongOptions);
            log(Level.FINEST, "testDatastoreCreation", "message-to-find: " +
                    expectedMessage);
            log(Level.FINEST, "testDatastoreCreation", "expected-exit-code: " +
                    expectedExitCode);

            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);
            Reporter.log("RealmName: " + realmName);
            Reporter.log("DatastoreType: " + datastoreType);
            Reporter.log("AttributeValues: " + attrValues);
            Reporter.log("UseDataFile: " + useDataFile);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);
            
            //Setup schema in the directories before creating data stores.
            LDAPCommon ldc = null;
            ResourceBundle dsCommon = ResourceBundle.getBundle("cli" + 
                     fileseparator + "DatastoreCommon");        
            String ldapServer = dsCommon.getString(datastoreType + 
                    "-ldap-server");
            String ldapPort = dsCommon.getString(datastoreType + "-ldap-port");
            String ldapBindDN = dsCommon.getString(datastoreType + "-authid");
            String ldapPwd = dsCommon.getString(datastoreType + "-authpw");
            String orgName = dsCommon.getString(datastoreType + "-root-suffix");

            log(Level.FINEST, "testDatastoreCreation", "Ldap Server: " + 
                    ldapServer);
            log(Level.FINEST, "testDatastoreCreation", "Ldap Port: " + ldapPort);
            log(Level.FINEST, "testDatastoreCreation", "Ldap Bind DN: " + 
                    ldapBindDN);
            log(Level.FINEST, "testDatastoreCreation", "Ldap Password: " + 
                    ldapPwd);
            log(Level.FINEST, "testDatastoreCreation", "Organization Name: " + 
                    orgName);
            
            ldc = new LDAPCommon(ldapServer, ldapPort, ldapBindDN, ldapPwd, 
                    orgName);
            ResourceBundle smsGblCfg = ResourceBundle.getBundle("config" + 
                    fileseparator + "default" + fileseparator + 
                    "UMGlobalConfig");
            if (datastoreType.equals("LDAPv3ForAMDS")) {
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
            } else if (datastoreType.equals("LDAPv3ForAD")) {
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
            	ResourceBundle cfgData = ResourceBundle.getBundle("config" +
                        fileseparator + "default" + fileseparator + 
                        SMSConstants.UM_DATASTORE_PARAMS_PREFIX);
                String userAtt = cfgData.getString(SMSConstants.
                        UM_DATASTORE_PARAMS_PREFIX + "." + datastoreType + "." +
                        SMSConstants.UM_LDAPv3_USER_ATTR);
                list = new ArrayList<String>();
                if (userAtt.indexOf("|") != 0) {
                    List locList = getAttributeList(userAtt, "|");
                    for (int j = 0; j < locList.size(); j++) {
                        list.add(SMSConstants.UM_LDAPv3_USER_ATTR + "=" +
                                (String) locList.get(j));
                    }
                }
                StringBuffer buff = new StringBuffer(attrValues);
                if (!attrValues.equals(""))
                buff.append(";");                
                for (int i=0; i < list.size(); i++) {
                    buff.append((String)list.get(i) + ";");
                }
                attrValues = buff.toString();
                log(Level.FINEST, "testDatastoreCreation", "Attribute values: " 
                        + attrValues);              
            }
            
            StringBuffer buf = new StringBuffer(attrValues);
            if (!attrValues.equals(""))
            buf.append(";");
            buf.append(ldapServerSchemaName + ldapServer + ":" + ldapPort + ";");
            buf.append(authIdSchema + ldapBindDN + ";");
            buf.append(authPwSchema + ldapPwd + ";");
            buf.append(orgSchemaName + orgName);
            attrValues = buf.toString();
            log(Level.FINEST, "testDatastoreCreation", "Attribute values: " +
                    attrValues);

            //Create datastore in the realm
            log(Level.FINEST, "testDatastoreCreation", "DataStoreToCreate: " +
                    createDatastore);
            int commandStatus = cli.createDatastore(realmName, createDatastore, 
            		datastoreType, attrValues, useDataFile);
            cli.logCommand("testDatastoreCreation");

            if (expectedExitCode.equals(
            		new Integer(SUCCESS_STATUS).toString())) {
            	stringsFound = cli.
                            findStringsInOutput(expectedMessage, ";");
                log(Level.FINEST, "testDatastoreCreation", "DataStore " +
                        createDatastore + " is created. ");
                assert (commandStatus ==
                    new Integer(expectedExitCode).intValue()) &&
                        stringsFound;
                cli.resetArgList();

                //Delete existing datastore in realm created.
                int exitStatus = cli.deleteDatastores(realmName, 
                        "qatest_ldapv3foramds");
                log(Level.FINEST, "testDatastoreCreation", "Deleting the " +
                        "default datastore from the realm: " + realmName);
                cli.logCommand("testDatastoreCreation");
                cli.resetArgList();
                
                // Verify identities, auth instances can be created in
                // in the new test datastore created.
                String attrVals = "userpassword=testDatastoreUser;uid=" +
                		"testDatastoreUser;sn=testDatastoreUser;" +
                		"cn=testDatastoreUser";
                exitStatus = cli.createIdentity(realmName, 
                		"testDatastoreUser", "User", attrVals);
                cli.logCommand("testDatastoreCreation");
                identityCreated = true;
                assert (exitStatus ==
                    new Integer(expectedExitCode).intValue());
                cli.resetArgList();
                exitStatus = cli.createAuthInstance(realmName,
                        "testDatastoreAuth", "DataStore");
                cli.logCommand("testDatastoreCreation");
                assert (exitStatus ==
                        new Integer(expectedExitCode).intValue());
                authInstanceCreated = true;
                cli.resetArgList();
                exitStatus = cli.updateAuthInstance(
                        realmName, "testDatastoreAuth", 
                        "sunAMAuthDataStoreAuthLevel=15", false);
                cli.logCommand("testDatastoreCreation");
                assert (exitStatus ==
                        new Integer(expectedExitCode).intValue());
                AuthenticationCommon ac = new AuthenticationCommon("cli");
                SSOToken token = ac.performRemoteLogin(realmName, "module",
                        "DataStore", "testDatastoreUser", "testDatastoreUser");
                if (token != null) {
                    log(Level.FINEST, "testDatastoreCreation", 
                            "Datastore authentification successful");
                } else {
                    log(Level.SEVERE, "testDatastoreCreation",
                        "Datastore authentification unsuccessful");
                }
                cli.resetArgList();
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
                    stringsFound = cli.
                            findStringsInError(usageError, ";" + newline);
                }
                log(logLevel, "testDatastoreCreation", "Error Messages Found: " 
                        + stringsFound);
                assert (commandStatus ==
                    new Integer(expectedExitCode).intValue()) && stringsFound;
                }
            cli.resetArgList();
            exiting("testDatastoreCreation");
        } catch (Exception e) {
            log(Level.SEVERE, "testDatastoreCreation", e.getMessage(), null);
            cleanup();
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Cleanup the datastores that were created during the setup and
     * testDatastoreCreation methods using "ssoadm delete-datastore".
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
            FederationManagerCLI cleanupCLI = new FederationManagerCLI(
                    useDebugOption, useVerboseOption, useLongOptions);
            if (!createDatastore.trim().equals("")) {
                if (identityCreated) {
                    int exitStatus = cleanupCLI.deleteIdentities(realmName, 
                            "testDatastoreUser", "User");
                    log(Level.FINE, "cleanup", "Cleaning Identity:" +
                            " testDatastoreUser");
                    cleanupCLI.logCommand("cleanup");
                    if (exitStatus != SUCCESS_STATUS) {
                        log(Level.SEVERE, "cleanup",
                            "Identity deletion returned the failed exit "
                            + "status " + exitStatus + ".");
                    }
                    cleanupCLI.resetArgList();
                }
                if (authInstanceCreated) {
                    int exitStatus = cleanupCLI.deleteAuthInstances(realmName, 
                            "testDatastoreAuth");
                    log(Level.FINE, "cleanup", "Cleaning auth instance" +
                            "testDatastoreAuth");
                    cleanupCLI.logCommand("cleanup");
                    if (exitStatus != SUCCESS_STATUS) {
                        log(Level.SEVERE, "cleanup",
                            "Auth instance deletion returned the failed exit "
                            + "status " + exitStatus + ".");
                    }
                    cleanupCLI.resetArgList();
                }
                int exitStatus = cleanupCLI.deleteDatastores(realmName,
                        createDatastore);
                log(Level.FINE, "cleanup", "Delete datastores: " + 
                        createDatastore + " in realm: " + realmName);
                cleanupCLI.logCommand("cleanup");
                if (exitStatus != SUCCESS_STATUS) {
                    log(Level.SEVERE, "cleanup",
                            "Service deletion returned the failed exit "
                            + "status " + exitStatus + ".");
                    assert false;
                }
                cleanupCLI.resetArgList();
            }
            if (!realmName.equals("/")) {
                int exitStatus = cleanupCLI.deleteRealm(realmName);
                log(Level.FINE, "cleanup", "Deleting Realm: " + realmName);
                cleanupCLI.logCommand("cleanup");
                if (exitStatus != SUCCESS_STATUS) {
                    log(Level.SEVERE, "cleanup",
                            "Realm deletion returned the failed exit "
                            + "status " + exitStatus + ".");
                    assert false;
                }
                cleanupCLI.resetArgList();            
            }
            exiting("cleanup");
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        }
    }
}

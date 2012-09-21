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
 * $Id: DeleteDatastoresTest.java,v 1.2 2009/02/11 19:31:24 srivenigan Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */

/**
 * DeleteDataStoresTest automates the following test cases:
 * CLI_delete-datastores01, CLI_delete-datastores02, CLI_delete-datastores03 
 * CLI_delete-datastores04, CLI_delete-datastores05, CLI_delete-datastores06 
 * CLI_delete-datastores07, CLI_delete-datastores08, CLI_delete-datastores09 
 * CLI_delete-datastores10
 */ 

package com.sun.identity.qatest.cli;

import com.sun.identity.qatest.common.cli.CLIExitCodes;
import com.sun.identity.qatest.common.cli.FederationManagerCLI;
import com.sun.identity.qatest.common.LDAPCommon;
import com.sun.identity.qatest.common.SMSConstants;
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
 * <code>DeleteDatastoresTest</code> is used to execute tests involving the 
 * delete-datastores sub-command of ssoadm.  This class allows the user to 
 * execute "ssoadm delete-datastores" with a variety of arguments (e.g with 
 * short or long options, with a password file or password argument, with a 
 * locale argument, etc.) and a variety of input values. The properties file 
 * <code>DeleteDatastoresTest.properties</code> contains the input values 
 * which are read by this class.
 */
public class DeleteDatastoresTest extends TestCommon implements CLIExitCodes {
    
    private String locTestName;
    private ResourceBundle rb;
    private String realmName;
    private String datastoreType;
    private boolean setupDatastore;
    private boolean useVerboseOption;
    private boolean useDebugOption;
    private boolean useLongOptions;
    private String expectedMessage;
    private String expectedExitCode;
    private String description;
    private String testDatastores;
    private FederationManagerCLI cli;    
    private List list;
    private String ldapServerSchemaName = 
            "sun-idrepo-ldapv3-config-ldap-server=";
    private String orgSchemaName = 
            "sun-idrepo-ldapv3-config-organization_name=";
    private String authIdSchema = "sun-idrepo-ldapv3-config-authid=";
    private String authPwSchema = "sun-idrepo-ldapv3-config-authpw=";
    
    /** 
     * Creates a new instance of DeleteDatastoresTest 
     */
    public DeleteDatastoresTest() {
        super("DeleteDatastoresTest");
    }
    
    /**
     * This method is intended to provide initial setup.
     * Deletes any datastores specified in the delete-datastores property in the 
     * DeleteDataStoresTest.properties.
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
            		"DeleteDatastoresTest");
            realmName =  (String) rb.getString(locTestName +
                    "-realm-name");
            setupDatastore = ((String)rb.getString(locTestName + 
                    "-setup-datastore")).equals("true");
            datastoreType = (String) rb.getString(locTestName +
                    "-datastore-type");
            useVerboseOption = ((String)rb.getString(locTestName + 
                    "-use-verbose-option")).equals("true");
            useDebugOption = ((String)rb.getString(locTestName + 
                    "-use-debug-option")).equals("true");
            useLongOptions = ((String)rb.getString(locTestName + 
                    "-use-long-options")).equals("true");
            testDatastores = (String) rb.getString(locTestName + 
                    "-delete-datastores");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-expected-exit-code");
            
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
            if (!testDatastores.trim().equals("") && setupDatastore) {
                // Create test datastores.
                String[] datastoreStrings = testDatastores.split(",");
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

                    log(Level.FINEST, "setup", "Ldap Server: " + 
                            ldapServer);
                    log(Level.FINEST, "setup", "Ldap Port: " + 
                            ldapPort);
                    log(Level.FINEST, "setup", "Ldap Bind DN: "  
                            + ldapBindDN);
                    log(Level.FINEST, "setup", "Ldap Password: "
                            + ldapPwd);
                    log(Level.FINEST, "setup", "Organization " +
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
     * This method is used to execute tests involving "ssoadm delete-datastores"
     * using input data from the DeleteDatastoresTest.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testDatastoreDeletion() 
    throws Exception {
        entering("testDatastoreDeletion", null);
        boolean stringsFound = false;
        try {
            description = (String) rb.getString(locTestName + "-description");
            expectedMessage = (String) rb.getString(locTestName + 
                    "-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-expected-exit-code");
            
            log(Level.FINEST, "testDatastoreDeletion", "description: " + 
                    description);
            log(Level.FINEST, "testDatastoreDeletion", "delete-datastores: " + 
                    testDatastores);
            log(Level.FINEST, "testDatastoreDeletion", "delete-datastores: " + 
                    testDatastores);
            log(Level.FINEST, "testDatastoreDeletion", "use-debug-option: " + 
                    useDebugOption);
            log(Level.FINEST, "testDatastoreDeletion", "use-verbose-option: " + 
                    useVerboseOption);
            log(Level.FINEST, "testDatastoreDeletion", "use-long-options: " + 
                    useLongOptions);
            log(Level.FINEST, "testDatastoreDeletion", "expected-exit-code: " + 
                    expectedExitCode);
            log(Level.FINEST, "testDatastoreDeletion", "message-to-find: " + 
                    expectedMessage);
            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);            
            Reporter.log("ServiceToDelete: " + testDatastores);
            Reporter.log("RealmName: " + realmName);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            
            // delete the services created in setup.
            int commandStatus = cli.deleteDatastores(realmName, 
                    testDatastores);
            log(Level.FINE, "testDatastoreDeletion", "Deleting datastores: "
                    + testDatastores);
            cli.logCommand("testDatastoreDeletion");
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                stringsFound = cli.
                        findStringsInOutput(expectedMessage, ";");
                log(Level.FINEST, "testDatastoreDeletion", 
                        "Output Messages Found: " + stringsFound);
                assert (commandStatus ==
                        new Integer(expectedExitCode).intValue()) &&
                        stringsFound;
            } else {
                if (expectedExitCode.equals(
                        new Integer(INVALID_OPTION_STATUS).toString())) {
                    String argString = cli.getAllArgs().replaceFirst(
                            cli.getCliPath(), "ssoadm ");
                    Object[] params = {argString};
                    String usageError = 
                            MessageFormat.format(expectedMessage, params);
                    expectedMessage = usageError;
                }
                stringsFound = cli.
                        findStringsInError(expectedMessage, ";");
                log(Level.FINEST, "testDatastoreDeletion", 
                        "Error Messages Found: " + stringsFound);
                assert (commandStatus ==
                    new Integer(expectedExitCode).intValue()) && stringsFound;
            }
            cli.resetArgList();
            
            if (!testDatastores.trim().equals("") && setupDatastore) {
                // verify if datastore exists by using "ssoadm list-datastores"
                commandStatus = cli.listDatastores(realmName);
                log(Level.FINE, "testDatastoreDeletion", "Listing test " +
                        "datastores in realm" + realmName);
                cli.logCommand("testDatastoreDeletion");
                boolean datastoresFound = cli.findStringsInOutput(
                        testDatastores, ",");
                assert (commandStatus == new Integer(
                        expectedExitCode).intValue()) && !datastoresFound; 
                log(Level.FINEST, "testDatastoreDeletion", 
                        "Datastores deleted successfully");
                cli.resetArgList();
            }
            exiting("testDatastoreDeletion");
        } catch (Exception e) {
            log(Level.SEVERE, "testDatastoreDeletion", e.getMessage(), null);
            cleanup();
            e.printStackTrace();
            throw e;
        } 
    }
        
    /**
     * This method removes any realms, datastores that were created during 
     * the setup and testDatastoreDeletion methods.
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
            if (!realmName.equals("/")) {
                int exitStatus = cli.deleteRealm(realmName);
                log(Level.FINE, "cleanup", "Deleting realm: " + realmName);
                cli.logCommand("cleanup");
                if (exitStatus != SUCCESS_STATUS) {
                    log(Level.SEVERE, "cleanup", "Realm deletion returned the" +
                            " failed exit status " + exitStatus + ".");
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


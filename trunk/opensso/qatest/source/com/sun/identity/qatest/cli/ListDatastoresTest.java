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
 * $Id: ListDatastoresTest.java,v 1.1 2009/02/11 19:33:48 srivenigan Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.cli;

import com.sun.identity.qatest.common.LDAPCommon;
import com.sun.identity.qatest.common.SMSConstants;
import com.sun.identity.qatest.common.cli.CLIExitCodes;
import com.sun.identity.qatest.common.cli.FederationManagerCLI;
import com.sun.identity.qatest.common.TestCommon;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
 * <code>ListDatastoresTest</code> is used to execute tests involving the 
 * list-datastores sub-command of ssoadm.  This class allows the user to 
 * execute an ssoadm command by specifying a subcommand and a list of 
 * arguments. Properties files named ListDatastoresTest.properties contain the 
 * input values which are read by this class.
 *
 * This class automates the following test cases:
 * CLI_list-datastores01, CLI_list-datastores02, 
 * CLI_list-datastores03, CLI_list-datastores04,
 * CLI_list-datastores05, CLI_list-datastores06,
 * CLI_list-datastores07
 */

public class ListDatastoresTest extends TestCommon implements CLIExitCodes {
    
    private String locTestName;
    private ResourceBundle rb;
    private boolean useVerboseOption;
    private boolean useDebugOption;
    private boolean useLongOptions;
    private String subcommand;
    private String argList;
    private String expectedMessage;
    private String expectedErrorMessage;
    private String expectedExitCode;
    private String description;
    private String setupRealm;
    private String setupDatastores;
    private String setupDatastoreTypes;
    private String datastoreToFind;
    private String searchRealm;
    private FederationManagerCLI cli;
    private List list;
    private String ldapServerSchemaName = 
        "sun-idrepo-ldapv3-config-ldap-server=";
    private String orgSchemaName = 
        "sun-idrepo-ldapv3-config-organization_name=";
    private String authIdSchema = "sun-idrepo-ldapv3-config-authid=";
    private String authPwSchema = "sun-idrepo-ldapv3-config-authpw=";
    
    /** 
     * Creates a new instance of ListDatastoresTest 
     */
    public ListDatastoresTest() {
        super("ListDatastoresTest");
    }
    
    /**
     * This method is intended to provide initial setup.
     * Creates a <code>FederationManagerCLI</code> object. 
     */
    @Parameters({"testName", "propFile"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String testName) 
    throws Exception {
        Object[] params = {testName};
        entering("setup", params);
        try {
            locTestName = testName;
            rb = ResourceBundle.getBundle("cli" + fileseparator + 
                    "ListDatastoresTest");
            setupRealm = (String)rb.getString(locTestName + 
                    "-create-setup-realm");
            setupDatastores = (String)rb.getString(locTestName + 
                    "-create-setup-datastores");
            setupDatastoreTypes = (String)rb.getString(locTestName + 
                    "-create-datastore-types");
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
            log(Level.FINEST, "setup", "create-setup-realms: " + setupRealm);
            log(Level.FINEST, "setup", "create-setup-identities: " + 
            		setupDatastores);
             
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("SetupRealms: " + setupRealm);
            Reporter.log("SetupIdentities: " + setupDatastores);

            cli = new FederationManagerCLI(useDebugOption, useVerboseOption, 
                    useLongOptions);

            int exitStatus = -1;
            if (setupRealm != null && !setupRealm.equals("")) {
                if (!cli.createRealms(setupRealm)) {
                    log(Level.SEVERE, "setup", 
                            "All the realms failed to be created.");
                    assert false;
                }
            }
            if (setupDatastores != null) {
                String[] datastoreStrings = setupDatastores.split(",");
                String[] datastoreTypes = setupDatastoreTypes.split(";");
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
                    String ldapBindDN = dsCommon.getString(datastoreTypes[i]  + 
                            "-authid");
                    String ldapPwd = dsCommon.getString(datastoreTypes[i] + 
                            "-authpw");
                    String orgName = dsCommon.getString(datastoreTypes[i] + 
                            "-root-suffix");

                    log(Level.FINEST, "testDatastoreCreation", "Ldap Server: " + 
                            ldapServer);
                    log(Level.FINEST, "testDatastoreCreation", "Ldap Port: " + 
                            ldapPort);
                    log(Level.FINEST, "testDatastoreCreation", "Ldap Bind DN: " 
                            + ldapBindDN);
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

                   exitStatus = cli.createDatastore(setupRealm,
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
     * This method is used to execute tests with the subcommand list-datastores
     * and arguments from the ListDatastoresTest.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testDatastoreSearch() 
    throws Exception {
        entering("testDatastoreSearch", null);
        boolean stringsFound = false;
        boolean errorFound = false;
        int commandStatus = -1;
        
        try {
            description = (String) rb.getString(locTestName + "-description");
            searchRealm = (String) rb.getString(locTestName + 
                    "-search-realm");
            datastoreToFind = (String) rb.getString(locTestName + 
                    "-search-ds-to-find");
            expectedMessage = (String) rb.getString(locTestName + 
                    "-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-expected-exit-code");
 
            log(Level.FINEST, "testDatastoreSearch", "description: " + 
                    description);
            log(Level.FINEST, "testDatastoreSearch", "use-debug-option: " + 
                    useDebugOption);
            log(Level.FINEST, "testDatastoreSearch", "use-verbose-option: " + 
                    useVerboseOption);
            log(Level.FINEST, "testDatastoreSearch", "use-long-options: " + 
                    useLongOptions);
            log(Level.FINEST, "testDatastoreSearch", "message-to-find: " + 
                    expectedMessage);
            log(Level.FINEST, "testDatastoreSearch", "expected-exit-code: " + 
                    expectedExitCode);

            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("Subcommand: " + subcommand);
            Reporter.log("ArgumentList: " + argList);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);

            FederationManagerCLI listCLI = new FederationManagerCLI(
                    useDebugOption, useVerboseOption, useLongOptions);
            commandStatus = listCLI.listDatastores(searchRealm);
            listCLI.logCommand("testDatastoreSearch");
            String msg = (String) rb.getString(locTestName + 
                    "-message-to-find");  
            log(Level.FINEST, "testDatastoreSearch", "Exit status: " + 
                    commandStatus);
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                listCLI.resetArgList();
                Object[] params = {searchRealm, datastoreToFind}; 
                if (msg.equals("")) {
                    if (!useVerboseOption) {
                        String successString = 
                                (String) rb.getString("success-message");
                        expectedMessage = 
                                MessageFormat.format(successString, params);
                    } else {
                        String verboseSuccessString = (String) rb.getString(
                                "verbose-success-message");
                        expectedMessage = MessageFormat.format(
                                verboseSuccessString, params);
                    }
                } else {
                    expectedMessage = 
                            MessageFormat.format(msg, params);
                }                
                stringsFound = listCLI.findStringsInOutput(expectedMessage, ";");
                boolean dsfound = listCLI.findStringsInOutput(
                        datastoreToFind, ";");

                log(Level.FINEST, "testDatastoreSearch", 
                        "Output Messages Found: " + stringsFound);
                log(Level.FINEST, "testDatastoreSearch", "Datastores Found: " + 
                        datastoreToFind);
                assert (commandStatus == 
                        new Integer(expectedExitCode).intValue()) 
                        && dsfound && stringsFound;
            } else if (expectedExitCode.equals(
                    new Integer(INVALID_OPTION_STATUS).toString())) {
                expectedMessage = (String) rb.getString("usage");                
                String argString = listCLI.getAllArgs().replaceFirst(
                        listCLI.getCliPath(), "ssoadm ");
                Object[] params = {argString};
                String errorMessage = 
                        (String) rb.getString("invalid-usage-message");
                String usageError = MessageFormat.format(errorMessage, params);
                stringsFound = listCLI.findStringsInOutput(expectedMessage, ";");                
                errorFound = listCLI.findStringsInError(usageError, ";");
                log(Level.FINEST, "testDatastoreSearch", 
                        "Output Messages Found: " + stringsFound); 
                assert (commandStatus == 
                        new Integer(expectedExitCode).intValue()) 
                        && stringsFound && errorFound;                    
            } else {
                expectedMessage = msg;      
                errorFound = listCLI.findStringsInError(expectedMessage, ";");
                assert (commandStatus == 
                            new Integer(expectedExitCode).intValue()) && 
                            errorFound;
            }
            listCLI.resetArgList();
            exiting("testDatastoreSearch");
        } catch (Exception e) {
            log(Level.SEVERE, "testDatastoreSearch", e.getMessage(), null);
            cleanup();
            e.printStackTrace();
            throw e;
        } 
    }
    
    /**
     * This method remove any realms that were created during the setup and
     * testDatastoreSearch methods using "ssoadm list-datastores".
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup() 
    throws Exception {
        entering("cleanup", null);
        int exitStatus = -1;
        try {            
            log(Level.FINEST, "cleanup", "useDebugOption: " + useDebugOption);
            log(Level.FINEST, "cleanup", "useVerboseOption: " + 
                    useVerboseOption);
            log(Level.FINEST, "cleanup", "useLongOptions: " + useLongOptions);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            
            if(!setupDatastores.equals("") ) {
                exitStatus = cli.deleteDatastores(setupRealm, 
                        setupDatastores);
                log(Level.FINE, "testDatastoreDeletion", "Deleting datastores: "
                        + setupDatastores);
                if (exitStatus != SUCCESS_STATUS) {
                    log(Level.SEVERE, "cleanup", "Datastore deletion returned" +
                            " the failed exit status " + exitStatus + ".");
                    assert false;
                }
                cli.logCommand("testDatastoreDeletion");    
                cli.resetArgList();
            }
            if (!setupRealm.equals("/")) {
                exitStatus = cli.deleteRealm(setupRealm);
                log(Level.FINE, "cleanup", "Deleting realm: " + setupRealm);
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




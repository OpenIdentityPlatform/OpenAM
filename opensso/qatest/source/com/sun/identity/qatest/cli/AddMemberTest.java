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
 * $Id: AddMemberTest.java,v 1.8 2009/01/26 23:48:52 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
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
 * <code>AddMemberTest</code> is used to execute tests involving the 
 * add-member sub-command of ssoadm.  This class allows the user to execute
 * "ssoadm add-member" with a variety or arguments (e.g with short or long 
 * options, with a password file or password argument, with a locale argument,
 * with a list of attributes or a datafile containing attributes, etc.) 
 * and a variety of input values.  The properties file 
 * <code>AddMemberTest.properties</code> contains the input values which 
 * are read by this class.
 *
 * This class automates the following test cases:
 * CLI_add-member01, CLI_add-member02, CLI_add-member03, CLI_add-member04,
 * CLI_add-member05, CLI_add-member06, CLI_add-member07, CLI_add-member08,
 * CLI_add-member09, CLI_add-member10, CLI_add-member11, CLI_add-member12,
 * CLI_add-member13, CLI_add-member14, CLI_add-member15, CLI_add-member16
 */
public class AddMemberTest extends TestCommon implements CLIExitCodes {
    
    private String locTestName;
    private ResourceBundle rb;
    private String setupRealms;
    private String setupIdentities;
    private String setupMembers;
    private String realmForId;
    private String memberIdName;
    private String memberIdType;
    private String idName;
    private String idType;
    private boolean useVerboseOption;
    private boolean useDebugOption;
    private boolean useLongOptions;
    private String expectedMessage;
    private String expectedExitCode;
    private String description;
    private FederationManagerCLI cli;
    
    /** 
     * Creates a new instance of AddMemberTest 
     */
    public AddMemberTest() {
        super("AddMemberTest");
    }
    
    /**
     * This method is intended to provide initial setup.
     * Creates any realms specified in the setup-realms property and creates
     * any identities specified in the setup-identities property in the 
     * AddMemberTest.properties.
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
                    "AddMemberTest");
            setupRealms = (String)rb.getString(locTestName + 
                    "-create-setup-realms");
            setupIdentities = (String)rb.getString(locTestName + 
                    "-create-setup-identities");
            setupMembers = (String) rb.getString(locTestName + 
                    "-create-setup-members");
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
            
            if (!setupMembers.equals("")) {
                log(Level.FINEST, "setup", "create-setup-members: " + 
                        setupMembers);
                Reporter.log("SetupMembers: " + setupMembers);
            }

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
                        log(Level.FINE, "setup", "Creating id " + ids[i]);
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
            
            if (setupMembers != null) {
                if (setupMembers.length() > 0) {
                    String[] members = setupMembers.split("\\|");
                    for (int i=0; i < members.length; i++) {
                        log(Level.FINE, "setup", "Adding member" + members[i]);
                        String [] memberArgs = members[i].split("\\,");  
                        if (memberArgs.length == 5) {
                            String setupIdRealm = memberArgs[0];
                            String setupMemberName = memberArgs[1];
                            String setupMemberType = memberArgs[2];
                            String setupIdName = memberArgs[3];
                            String setupIdType = memberArgs[4];
                            log(Level.FINEST, "setup", "Identity realm: " + 
                                    setupIdRealm);
                            log(Level.FINEST, "setup", "Member name: " + 
                                    setupMemberName);
                            log(Level.FINEST, "setup", "Member type: " + 
                                    setupMemberType);
                            log(Level.FINEST, "setup", "Identity name: " + 
                                    setupIdName);
                            log(Level.FINEST, "setup", "Identity type: " + 
                                    setupIdType);   
                            exitStatus = cli.addMember(setupIdRealm, 
                                    setupMemberName, setupMemberType, 
                                    setupIdName, setupIdType);
                            cli.logCommand("setup");
                            cli.resetArgList();
                            if (exitStatus != SUCCESS_STATUS) {
                                log(Level.SEVERE, "setup", "The addition of " + 
                                        setupMemberName + " as a member in " + 
                                        setupIdName + 
                                        " failed with exit status " + 
                                        exitStatus + ".");   
                                assert false;
                            }
                        } else {
                            log(Level.SEVERE, "setup", "The setup member " + 
                                    memberArgs + 
                                    " must have a realm, a member name, " +
                                    " a member type, an identity name, and " +
                                    " an identity type");
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
     * using input data from the AddMemberTest.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testMemberAddition() 
    throws Exception {
        entering("testMemberAddition", null);
        boolean stringsFound = false;
        boolean memberFound = false;
        boolean errorFound = false;
        
        try {
            expectedMessage = (String) rb.getString(locTestName + 
                    "-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-expected-exit-code");
            realmForId = (String) rb.getString(locTestName + 
                    "-add-member-realm");
            idName = (String) rb.getString(locTestName + 
                    "-add-member-idname");
            idType = (String) rb.getString(locTestName + 
                    "-add-member-idtype");
            memberIdName = (String) rb.getString(locTestName + 
                    "-add-member-name");
            memberIdType = (String) rb.getString(locTestName + 
                    "-add-member-type");
            description = (String) rb.getString(locTestName + "-description");

            log(Level.FINEST, "testMemberAddition", "description: " + 
                    description);
            log(Level.FINEST, "testMemberAddition", "use-debug-option: " + 
                    useDebugOption);
            log(Level.FINEST, "testMemberAddition", "use-verbose-option: " + 
                    useVerboseOption);
            log(Level.FINEST, "testMemberAddition", "use-long-options: " + 
                    useLongOptions);
            log(Level.FINEST, "testMemberAddition", "expected-exit-code: " + 
                    expectedExitCode);
            log(Level.FINEST, "testMemberAddition", "add-member-realm: " + 
                    realmForId);
            log(Level.FINEST, "testMemberAddition", "add-member-idname: " + 
                    idName);
            log(Level.FINEST, "testMemberAddition", "add-member-idtype: " + 
                    idType);
            log(Level.FINEST, "testMemberAddition", "add-member-name: " + 
                    memberIdName);
            log(Level.FINEST, "testMemberAddition", "add-member-type:" + 
                    memberIdType);

            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);
            Reporter.log("IdRealmToCreate: " + realmForId);
            Reporter.log("IdName: " + idName);
            Reporter.log("IdType: " + idType);
            Reporter.log("MemberIdName: " + memberIdName);
            Reporter.log("MemberIdType: " + memberIdType);
            
            log(Level.FINE, "testMemberAddition", "Adding " + memberIdType + 
                    " identity " + memberIdName + " to " + idType + " identity "
                    + idName);
            int commandStatus = cli.addMember(realmForId, memberIdName, 
                    memberIdType, idName, idType);
            cli.logCommand("testMemberAddition");

            log(Level.FINEST, "testMemberAddition", "message-to-find: " + 
                    expectedMessage);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            
            String msg = (String) rb.getString(locTestName + 
                    "-message-to-find");           

            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                Object[] params = {memberIdName, idName}; 
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
                stringsFound = cli.findStringsInOutput(expectedMessage, ";");
                cli.resetArgList();
                FederationManagerCLI listCLI = 
                        new FederationManagerCLI(useDebugOption, 
                        useVerboseOption, useLongOptions);
                memberFound = listCLI.findMembers(realmForId, idName, idType,
                        memberIdType, memberIdName); 
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

            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                log(Level.FINEST, "testMemberAddition", 
                        "Output Messages Found: " + stringsFound);
                log(Level.FINEST, "testMemberAddition", "Member Found: " + 
                        memberFound); 
                assert (commandStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound &&
                        memberFound;
            } else {
                log(Level.FINEST, "testMemberAddition", 
                        "Error Messages Found: " + stringsFound);
                assert (commandStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound;
            }     
            exiting("testMemberAddition");
        } catch (Exception e) {
            log(Level.SEVERE, "testMemberAddition", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        } 
    }
    
    /**
     * This method remove any members, identities, and realms that were created 
     * during the setup and testMemberAddition methods using 
     * "ssoadm remove-member", "ssoadm delete-realm" and 
     * "ssoadm delete-identities".
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
            
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                log(Level.FINE, "cleanup", "Removing member " + memberIdName + 
                        "from " + idName);
                log(Level.FINEST, "cleanup", "memberToDelete: "  + 
                        memberIdName);
                Reporter.log("MemberNameToReomve: " + memberIdName);
                cli.removeMember(realmForId, memberIdName, memberIdType, idName,
                        idType);
                cli.logCommand("cleanup");
                cli.resetArgList();
            }

            if (setupIdentities != null) {
                if (setupIdentities.length() > 0) {
                    String [] cleanupIds = setupIdentities.split("\\|");
                    for (int i = 0; i < cleanupIds.length; i++) {
                        log(Level.FINE, "cleanup", "Removing identity " + 
                                cleanupIds[i]);
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
                            
                            exitStatus = 
                                    cli.deleteIdentities(idRealm, idName, 
                                    idType);
                            cli.resetArgList();
                            if (exitStatus != SUCCESS_STATUS) {
                                log(Level.SEVERE, "cleanup", 
                                        "Removal of identity " + idName + 
                                        "failed with exit status " + 
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

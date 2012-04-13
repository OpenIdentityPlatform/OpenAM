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
 * $Id: RemoveMemberTest.java,v 1.7 2009/01/26 23:49:37 nithyas Exp $
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
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.Reporter;

/**
 * <code>RemoveMemberTest</code> is used to execute tests involving the 
 * delete-identities sub-command of ssoadm.  This class allows the user to 
 * execute "ssoadm delete-identities" with a variety or arguments (e.g with 
 * short or long options, with a locale argument, with a list of attributes 
 * or a datafile containing attributes, etc.) and a variety of input values.  
 * The properties file <code>RemoveMemberTest.properties</code> contains 
 * the input values which are read by this class.
 *
 * This class automates the following test cases:
 * CLI_remove-member01, CLI_remove-member02, CLI_reomve-member03, 
 * CLI_remove-member04, CLI_remove-member05, CLI_remove-member06, 
 * CLI_remove-member07, CLI_remove-member08, CLI_remove-member09, 
 * CLI_remove-member10, CLI_remove-member11, CLI_remove-member12,
 * CLI_remove-member13, CLI_remove-member14, CLI_remove-member15, 
 * CLI_remove-member16
 */
public class RemoveMemberTest extends TestCommon implements CLIExitCodes {
    
    private String locTestName;
    private ResourceBundle rb;
    private String setupRealms;
    private String setupIdentities;
    private String setupMembers;
    private String realmForId;
    private String memberIdNameToDelete;
    private String memberIdTypeToDelete;
    private String idName;
    private String idType;
    private boolean useVerboseOption;
    private boolean useDebugOption;
    private boolean useLongOptions;
    private String expectedMessage;
    private String expectedExitCode;
    private String description;
    private FederationManagerCLI cli;
    private Vector membersNotRemoved;
    
    /** 
     * Creates a new instance of RemoveMemberTest 
     */
    public RemoveMemberTest() {
        super("RemoveMemberTest");
    }
    
    /**
     * This method is intended to provide initial setup.
     * Creates any realms specified in the setup-realms property, creates
     * any identities specified in the setup-identities property, and adds any 
     * members in the setup-members in the RemoveMemberTest.properties file.
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
                    "RemoveMemberTest");
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
            
            membersNotRemoved = new Vector();
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
                            
                            Reporter.log("SetupIdRealm: " + setupIdRealm);
                            Reporter.log("SetupMemberName: " + setupMemberName);
                            Reporter.log("SetupMemberType: " + setupMemberType);
                            Reporter.log("SetupIdName: " + setupIdName);
                            Reporter.log("SetupIdType: " + setupIdType);
                            
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
                            } else {
                                membersNotRemoved.add(members[i]);
                                log(Level.FINEST, "setup", "membersNotRemoved: "
                                        + membersNotRemoved);
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
     * This method is used to execute tests involving "ssoadm remove-member"
     * using input data from the RemoveMemberTest.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testMemberRemoval() 
    throws Exception {
        entering("testMemberRemoval", null);
        boolean stringsFound = false;
        boolean errorFound = false;
        boolean memberRemovedFound = true;
        boolean remainingMemberRemoved = false;
        
        try {
            expectedMessage = (String) rb.getString(locTestName + 
                    "-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-expected-exit-code");
            realmForId = (String) rb.getString(locTestName + 
                    "-remove-member-realm");
            idName = (String) rb.getString(locTestName + 
                    "-remove-member-idname");
            idType = (String) rb.getString(locTestName + 
                    "-remove-member-idtype");            
            memberIdNameToDelete = (String) rb.getString(locTestName + 
                    "-remove-member-name");
            memberIdTypeToDelete = (String) rb.getString(locTestName + 
                    "-remove-member-type");
            description = (String) rb.getString(locTestName + "-description");

            log(Level.FINEST, "testMemberRemoval", "description: " + 
                    description);
            log(Level.FINEST, "testMemberRemoval", "use-debug-option: " + 
                    useDebugOption);
            log(Level.FINEST, "testMemberRemoval", "use-verbose-option: " + 
                    useVerboseOption);
            log(Level.FINEST, "testMemberRemoval", "use-long-options: " + 
                    useLongOptions);
            log(Level.FINEST, "testMemberRemoval", "message-to-find: " + 
                    expectedMessage);
            log(Level.FINEST, "testMemberRemoval", "expected-exit-code: " + 
                    expectedExitCode);
            log(Level.FINEST, "testMemberRemoval", 
                    "delete-identities-realm: " + realmForId);
            log(Level.FINEST, "testMemberRemoval", "remove-member-idname: " + 
                    idName);
            log(Level.FINEST, "testMemberRemoval", "remove-member-idtype: " + 
                    idType);
            log(Level.FINEST, "testMemberRemoval", "remove-member-name: " + 
                    memberIdNameToDelete);
            log(Level.FINEST, "testMemberRemoval", "remove-member-type: " + 
                    memberIdTypeToDelete);

            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);
            Reporter.log("IdRealm: " + realmForId);
            Reporter.log("IdName: " + idName);
            Reporter.log("IdType: " + idType);
            Reporter.log("MemberIdNameToDelete: " + memberIdNameToDelete);
            Reporter.log("MemberIdTypeToDelete: " + memberIdTypeToDelete);
            
            int commandStatus = cli.removeMember(realmForId, 
                    memberIdNameToDelete, memberIdTypeToDelete, idName, idType);
            cli.logCommand("testMemberRemoval");

            String msg = (String) rb.getString(locTestName + 
                    "-message-to-find");  
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                StringBuffer removeIdBuffer = new StringBuffer(realmForId).
                        append(",").append(memberIdNameToDelete).append(",").
                        append(memberIdTypeToDelete).append(",").append(idName).
                        append(",").append(idType);
                log(Level.FINEST, "testMemberRemoval", "removeIdBuffer: " + 
                        removeIdBuffer);
                log(Level.FINEST, "testMemberRemoval", "membersNotRemoved: " +
                        membersNotRemoved);
                membersNotRemoved.remove(removeIdBuffer.toString());
                Object[] params = {memberIdNameToDelete, idName}; 
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
                    expectedMessage = MessageFormat.format(msg, params);
                }
                stringsFound = cli.findStringsInOutput(expectedMessage, ";");
            } else if (expectedExitCode.equals(
                    new Integer(INVALID_OPTION_STATUS).toString())) {
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
                stringsFound = cli.findStringsInError(expectedMessage, ";");
            }
            cli.resetArgList();
            
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                for (Iterator i = membersNotRemoved.iterator(); i.hasNext(); ) {                
                    FederationManagerCLI listCLI = 
                            new FederationManagerCLI(useDebugOption, 
                            useVerboseOption, useLongOptions);
                    String remainingMember = (String)i.next();
                    String [] memberArgs = remainingMember.split("\\,"); 
                    
                    if (memberArgs.length == 5) {
                        String remainingIdRealm = memberArgs[0];
                        String remainingMemberName = memberArgs[1];
                        String remainingMemberType = memberArgs[2];
                        String remainingIdName = memberArgs[3];
                        String remainingIdType = memberArgs[4];

                        log(Level.FINEST, "testMemberRemoval", 
                                "remainingIdRealm: " + remainingIdRealm);
                        log(Level.FINEST, "testMemberRemoval", 
                                "remainingMemberName: " + remainingMemberName);
                        log(Level.FINEST, "testMemberRemoval", 
                                "remainingMemberType: " + remainingMemberType);
                        log(Level.FINEST, "testMemberRemoval", 
                                "remainingIdentityName: " + remainingIdName);
                        log(Level.FINEST, "testMemberRemoval", 
                                "remainingIdentityType: " + remainingIdType);  

                        Reporter.log("RemainingIdRealm: " + remainingIdRealm);
                        Reporter.log("RemainingMemberName: " + 
                                remainingMemberName);
                        Reporter.log("RemainingMemberType: " + 
                                remainingMemberType);
                        Reporter.log("RemainingIdName: " + remainingIdName);
                        Reporter.log("RemainingIdType: " + remainingIdType);
                        
                        log(Level.FINE, "testMemberRemoval", 
                                "Verifying that the remaining members were not " 
                                 + "removed.");
                        if (!listCLI.findMembers(remainingIdRealm, 
                                remainingIdName, remainingIdType, 
                                remainingMemberType, remainingMemberName)) {
                            remainingMemberRemoved = true;
                            log(Level.SEVERE, "testMemberRemoval", 
                                    "ERROR: The member " + remainingMemberName + 
                                    " was removed from " + 
                                    remainingIdName + ".");
                        } else {
                            log(Level.FINEST, "testMemberRemoval", 
                                    "The member identity " + remainingMemberName
                                    + " was not removed from " + remainingIdName
                                    + ".");
                        }
                    } else {
                            log(Level.SEVERE, "testMemberRemoval", 
                                    "The member " + remainingMember + memberArgs
                                    + " must have a realm, a member name, " +
                                    " a member type, an identity name, and " +
                                    " an identity type");
                            assert false;                          
                    }
                }            

                if (memberIdNameToDelete.length() > 0) {
                    FederationManagerCLI listCLI2 = 
                            new FederationManagerCLI(useDebugOption, 
                            useVerboseOption, useLongOptions);
                    log(Level.FINE, "testMemberRemoval", 
                            "Verifying removal of the member " + 
                            memberIdNameToDelete.toLowerCase());
                    memberRemovedFound = listCLI2.findMembers(realmForId, 
                            idName, idType, memberIdTypeToDelete, 
                            memberIdNameToDelete);
                    listCLI2.resetArgList();
                }
                
                log(Level.FINEST, "testMemberRemoval", 
                        "Output Messages Found: " + stringsFound);
                log(Level.FINEST, "testMemberRemoval", 
                        "Remaining member deleted: " + remainingMemberRemoved);
                log(Level.FINEST, "testMemberRemoval", "Removed member found: " 
                        + memberRemovedFound);
                assert (commandStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound &&
                        !remainingMemberRemoved && !memberRemovedFound;
            } else if (expectedExitCode.equals(
                    new Integer(INVALID_OPTION_STATUS).toString())) {
                 log(Level.FINEST, "testMemberRemoval", 
                        "Output Messages Found: " + stringsFound);   
                 log(Level.FINEST, "testMemberRemoval", 
                        "Error Messages Found: " + errorFound);   
                 assert (commandStatus == 
                         new Integer(expectedExitCode).intValue()) && 
                         stringsFound && errorFound;
            } else {
                log(Level.FINEST, "testMemberRemoval", 
                        "Error Messages Found: " + stringsFound);
                assert (commandStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound;
            }     
            exiting("testMemberRemoval");
        } catch (Exception e) {
            log(Level.SEVERE, "testMemberRemoval", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        } 
    }
    
    /**
     * This method remove any realms and identities that were created during 
     * the setup and testMemberRemoval methods using 
     * "ssoadm remove-member", "ssoadm delete-identities", and 
     * "ssoadm delete-realm".
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
            
            for (Iterator i = membersNotRemoved.iterator(); i.hasNext(); ) {                
                String remainingMember = (String)i.next();
                String [] memberArgs = remainingMember.split("\\,"); 

                if (memberArgs.length == 5) {
                    String remainingIdRealm = memberArgs[0];
                    String remainingMemberName = memberArgs[1];
                    String remainingMemberType = memberArgs[2];
                    String remainingIdName = memberArgs[3];
                    String remainingIdType = memberArgs[4];

                    log(Level.FINEST, "testMemberRemoval", 
                            "Identity realm: " + remainingIdRealm);
                    log(Level.FINEST, "testMemberRemoval", "Member name: " + 
                            remainingMemberName);
                    log(Level.FINEST, "testMemberRemoval", "Member type: " + 
                            remainingMemberType);
                    log(Level.FINEST, "testMemberRemoval", "Identity name: "
                            + remainingIdName);
                    log(Level.FINEST, "testMemberRemoval", "Identity type: "
                            + remainingIdType);  

                    Reporter.log("RemainingIdRealm: " + remainingIdRealm);
                    Reporter.log("RemainingMemberName: " + 
                            remainingMemberName);
                    Reporter.log("RemainingMemberType: " + 
                            remainingMemberType);
                    Reporter.log("RemainingIdName: " + remainingIdName);
                    Reporter.log("RemainingIdType: " + remainingIdType);

                    log(Level.FINE, "testMemberRemoval", 
                            "Removing remaining member " + remainingMemberName +
                            ".");
                    exitStatus = cli.removeMember(remainingIdRealm, 
                            remainingMemberName, remainingMemberType, 
                            remainingIdName, remainingIdType);
                    if (exitStatus != SUCCESS_STATUS) {
                        log(Level.SEVERE, "cleanup", 
                                "ERROR: The member " + remainingMemberName + 
                                " was not removed from " + remainingIdName + 
                                ".");
                        assert false;
                    } else {
                        log(Level.FINEST, "cleanup", 
                                "The member identity " + remainingMemberName
                                + " was removed from " + remainingIdName + ".");
                    }
                } else {
                        log(Level.SEVERE, "cleanup", "The member " + 
                                remainingMember + 
                                " must have a realm, member name, member type, "
                                + "an identity name, and an identity type");
                        assert false;                          
                }
            }

            if (setupIdentities != null) {
                if (setupIdentities.length() > 0) {
                    String [] cleanupIds = setupIdentities.split("\\|");
                    for (int i = 0; i < cleanupIds.length; i++) {
                        log(Level.FINE, "cleanup", "Removing identity " + 
                                cleanupIds[i]);
                        String [] idArgs = cleanupIds[i].split("\\,");
                        if (idArgs.length >= 3) {
                            String removeRealm = idArgs[0];
                            String removeName = idArgs[1];
                            String removeType = idArgs[2];
                            
                            log(Level.FINEST, "cleanup", "idRealm: " + 
                                removeRealm);
                            log(Level.FINEST, "cleanup", "idName: " + 
                                    removeName);
                            log(Level.FINEST, "cleanup", "idType: " + 
                                    removeType);
                            
                            Reporter.log("IdRealm: " + removeRealm);
                            Reporter.log("IdNameToRemove: " + removeName);
                            Reporter.log("IdentityTypeToRemove: " + removeType);
                            
                            exitStatus = 
                                    cli.deleteIdentities(removeRealm, 
                                    removeName, removeType);
                            cli.resetArgList();
                            if (exitStatus != SUCCESS_STATUS) {
                                log(Level.SEVERE, "cleanup", 
                                        "Removal of identity " + removeName + 
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

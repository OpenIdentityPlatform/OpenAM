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
 * $Id: ShowMembersTest.java,v 1.8 2009/01/26 23:49:40 nithyas Exp $
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
 * <code>ShowMembersTest</code> is used to execute tests involving the 
 * delete-identities sub-command of ssoadm.  This class allows the user to 
 * execute "ssoadm delete-identities" with a variety or arguments (e.g with 
 * short or long options, with a locale argument, with a list of attributes 
 * or a datafile containing attributes, etc.) and a variety of input values.  
 * The properties file <code>ShowMembersTest.properties</code> contains 
 * the input values which are read by this class.
 *
 * This class automates the following test cases:
 * CLI_show-members01, CLI_show-members02, CLI_reomve-member03, 
 * CLI_show-members04, CLI_show-members05, CLI_show-members06, 
 * CLI_show-members07, CLI_show-members08, CLI_show-members09, 
 * CLI_show-members10, CLI_show-members11, CLI_show-members12,
 * CLI_show-members13, CLI_show-members14, CLI_show-members15, 
 * CLI_show-members16, CLI_show-members17, CLI_show-members18,
 * CLI_show-members19, CLI_show-members20, CLI_show-members21, 
 * CLI_show-members22, CLI_show-members23, CLI_show-members24
 */
public class ShowMembersTest extends TestCommon implements CLIExitCodes {
    
    private String locTestName;
    private ResourceBundle rb;
    private String setupRealms;
    private String setupIdentities;
    private String setupMembers;
    private String searchRealm;
    private String idName;
    private String idType;
    private String memberType;
    private String membersToFind;
    private boolean useVerboseOption;
    private boolean useDebugOption;
    private boolean useLongOptions;
    private String expectedMessage;
    private String expectedExitCode;
    private String description;
    private FederationManagerCLI cli;
    
    /** 
     * Creates a new instance of ShowMembersTest 
     */
    public ShowMembersTest() {
        super("ShowMembersTest");
    }
    
    /**
     * This method is intended to provide initial setup.
     * Creates any realms specified in the setup-realms property, creates
     * any identities specified in the setup-identities property, and adds any 
     * members in the setup-members in the ShowMembersTest.properties file.
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
                    "ShowMembersTest");
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
                            String setupRealm = idArgs[0];
                            String setupName = idArgs[1];
                            String setupType = idArgs[2];
                            log(Level.FINEST, "setup", "Realm for setup id: " + 
                                    setupRealm);
                            log(Level.FINEST, "setup", "Name for setup id: " + 
                                    setupName);
                            log(Level.FINEST, "setup", "Type for setup id: " + 
                                    setupType);                            
                            String idAttributes = null;
                            if (idArgs.length > 3) {
                                idAttributes = idArgs[3];
                                exitStatus = cli.createIdentity(setupRealm,
                                        setupName, setupType, idAttributes);
                            } else {                                
                                exitStatus = cli.createIdentity(setupRealm, 
                                        setupName, setupType);
                            }
                            cli.logCommand("setup");
                            cli.resetArgList();
                            if (exitStatus != SUCCESS_STATUS) {
                                assert false;
                                log(Level.SEVERE, "setup", "The creation of " + 
                                        setupName + " failed with exit status " 
                                        + exitStatus + ".");
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
                            
                            Reporter.log("SetupIdRealm: " + setupIdRealm);
                            Reporter.log("SetupMemberName: " + setupMemberName);
                            Reporter.log("SetupMemberType: " + setupMemberType);
                            Reporter.log("SetupIdName: " + setupIdName);
                            Reporter.log("SetupIdType: " + setupIdType);
                            
                            if (!setupIdType.equals("filteredrole")) {
                                exitStatus = cli.addMember(setupIdRealm, 
                                        setupMemberName, setupMemberType, 
                                        setupIdName, setupIdType);
                                cli.logCommand("setup");
                                cli.resetArgList();
                                if (exitStatus != SUCCESS_STATUS) {
                                    log(Level.SEVERE, "setup", 
                                            "The addition of " + setupMemberName
                                            + " as a member in " + setupIdName + 
                                            " failed with exit status " + 
                                            exitStatus + ".");   
                                    assert false;
                                } 
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
     * This method is used to execute tests involving "ssoadm show-members"
     * using input data from the ShowMembersTest.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testMemberSearch() 
    throws Exception {
        entering("testMemberSearch", null);
        boolean stringsFound = false;
        boolean membersFound = false;
        boolean errorFound = false;
        int commandStatus = -1;
        
        try {
            expectedMessage = (String) rb.getString(locTestName + 
                    "-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-expected-exit-code");
            searchRealm = (String) rb.getString(locTestName + 
                    "-show-members-realm");
            idName = (String) rb.getString(locTestName + 
                    "-show-members-idname");
            idType = (String) rb.getString(locTestName + 
                    "-show-members-idtype");  
            memberType = (String) rb.getString(locTestName + 
                    "-show-members-membertype");
            membersToFind = (String) rb.getString(locTestName + 
                    "-show-members-membership-list");
            description = (String) rb.getString(locTestName + "-description");

            log(Level.FINEST, "testMemberSearch", "description: " + 
                    description);
            log(Level.FINEST, "testMemberSearch", "use-debug-option: " + 
                    useDebugOption);
            log(Level.FINEST, "testMemberSearch", "use-verbose-option: " + 
                    useVerboseOption);
            log(Level.FINEST, "testMemberSearch", "use-long-options: " + 
                    useLongOptions);
            log(Level.FINEST, "testMemberSearch", "message-to-find: " + 
                    expectedMessage);
            log(Level.FINEST, "testMemberSearch", "expected-exit-code: " + 
                    expectedExitCode);
            log(Level.FINEST, "testMemberSearch", "show-members-realm: " + 
                    searchRealm);
            log(Level.FINEST, "testMemberSearch", "show-members-idname: " + 
                    idName);
            log(Level.FINEST, "testMemberSearch", "show-members-idtype: " + 
                    idType);
            log(Level.FINEST, "testMemberSearch", "show-members-membertype: " +
                    memberType);
            log(Level.FINEST, "testMemberSearch", 
                    "show-members-membership-list: " + membersToFind);

            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);
            Reporter.log("SearchRealm: " + searchRealm);
            Reporter.log("IdName: " + idName);
            Reporter.log("IdType: " + idType);
            Reporter.log("MemberType: " + memberType);
            Reporter.log("MembersToFind: " + membersToFind);
            
            expectedMessage = (String) rb.getString(locTestName + 
                    "-message-to-find");  
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                if (expectedMessage.equals("")) {           
                    if (useVerboseOption) {
                        expectedMessage = (String) rb.getString(
                                "verbose-success-message");
                    }
                }
            } else if (expectedExitCode.equals(
                    new Integer(INVALID_OPTION_STATUS).toString())) {
                expectedMessage = (String) rb.getString("usage");
            } 

            commandStatus = cli.showMembers(searchRealm, memberType, idName,
                    idType);
            cli.logCommand("testIdentitySearch");
            cli.resetArgList();
            
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                cli.resetArgList();
                if (!membersToFind.equals("")) {
                    membersFound = cli.findMembers(searchRealm, idName, idType,
                        memberType, membersToFind); 
                } else {
                    membersFound = true;
                }
                cli.logCommand("testIdentitySearch");
            } else if (expectedExitCode.equals(
                    new Integer(INVALID_OPTION_STATUS).toString())) {
                String argString = cli.getAllArgs().replaceFirst(
                        cli.getCliPath(), "ssoadm ");
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
                   
            log(Level.FINEST, "testIdentitySearch", "Exit status: " + 
                    commandStatus);
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                log(Level.FINEST, "testIdentitySearch", 
                        "Output Messages Found: " + stringsFound);
                log(Level.FINEST, "testIdentitySearch", "Members Found: " + 
                        membersFound);
                assert (commandStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound &&
                        membersFound;
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
     * This method remove any realms and identities that were created during 
     * the setup and testMemberSearch methods using 
     * "ssoadm show-members", "ssoadm delete-identities", and 
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
            
            String[] membersToRemove = setupMembers.split("\\|");
            for (int i=0; i < membersToRemove.length; i++) {
                String remainingMember = membersToRemove[i];
                String [] memberArgs = remainingMember.split("\\,"); 

                if (memberArgs.length == 5) {
                    String remainingIdRealm = memberArgs[0];
                    String remainingMemberName = memberArgs[1];
                    String remainingMemberType = memberArgs[2];
                    String remainingIdName = memberArgs[3];
                    String remainingIdType = memberArgs[4];

                    log(Level.FINEST, "testMemberSearch", 
                            "Identity realm: " + remainingIdRealm);
                    log(Level.FINEST, "testMemberSearch", "Member name: " + 
                            remainingMemberName);
                    log(Level.FINEST, "testMemberSearch", "Member type: " + 
                            remainingMemberType);
                    log(Level.FINEST, "testMemberSearch", "Identity name: "
                            + remainingIdName);
                    log(Level.FINEST, "testMemberSearch", "Identity type: "
                            + remainingIdType);  

                    Reporter.log("RemainingIdRealm: " + remainingIdRealm);
                    Reporter.log("RemainingMemberName: " + 
                            remainingMemberName);
                    Reporter.log("RemainingMemberType: " + 
                            remainingMemberType);
                    Reporter.log("RemainingIdName: " + remainingIdName);
                    Reporter.log("RemainingIdType: " + remainingIdType);

                    log(Level.FINE, "testMemberSearch", 
                            "Removing remaining member " + remainingMemberName +
                            ".");
                    exitStatus = cli.removeMember(remainingIdRealm, 
                            remainingMemberName, remainingMemberType, 
                            remainingIdName, remainingIdType);
                    cli.logCommand("cleanup");
                    cli.resetArgList();
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
                            if (exitStatus != SUCCESS_STATUS) {
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

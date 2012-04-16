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
 * $Id: UpdateAuthInstanceTest.java,v 1.7 2009/01/26 23:49:40 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.cli;

import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.qatest.common.cli.CLIExitCodes;
import com.sun.identity.qatest.common.cli.FederationManagerCLI;
import com.sun.identity.qatest.common.TestCommon;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.Reporter;

/**
 * <code>UpdateAuthInstanceTest</code> is used to execute tests involving the 
 * update-auth-instance sub-command of ssoadm.  This class allows the user to 
 * execute "ssoadm update-auth-instance" with a variety or arguments 
 * (e.g with short or long options, with a password file or password argument, 
 * with a locale argument and a variety of input values.  The properties file 
 * <code>UpdateAuthInstanceTest.properties</code> contains the input values 
 * which are read by this class.
 *
 * This class automates the following test cases:
 * CLI_update-auth-instance01, CLI_update-auth-instance02, 
 * CLI_update-auth-instance03, CLI_update-auth-instance04,
 * CLI_update-auth-instance05, CLI_update-auth-instance06, 
 * CLI_update-auth-instance07, CLI_update-auth-instance08,
 * CLI_update-auth-instance09, CLI_update-auth-instance10, 
 * CLI_update-auth-instance11, CLI_update-auth-instance12,
 * CLI_update-auth-instance13, CLI_update-auth-instance14, 
 * CLI_update-auth-instance15, CLI_update-auth-instance16,
 * CLI_update-auth-instance17, CLI_update-auth-instance18,
 * CLI_update-auth-instance19, CLI_update-auth-instance20, 
 * CLI_update-auth-instance21, CLI_update-auth-instance22,
 * CLI_update-auth-instance23, CLI_update-auth-instance24,
 * CLI_update-auth-instance25, CLI_update-auth-instance26,
 * CLI_update-auth-instance27, CLI_update-auth-instance28,
 * CLI_update-auth-instance29, CLI_update-auth-instance30,
 * CLI_update-auth-instance31, CLI_update-auth-instance32, 
 * CLI_update-auth-instance33, CLI_update-auth-instance34,
 * CLI_update-auth-instance35, CLI_update-auth-instance36, 
 * CLI_update-auth-instance37, CLI_update-auth-instance38,
 * CLI_update-auth-instance39, CLI_update-auth-instance40, 
 * CLI_update-auth-instance41, CLI_update-auth-instance42,
 * CLI_update-auth-instance43, CLI_update-auth-instance44, 
 * CLI_update-auth-instance45, CLI_update-auth-instance46,
 * CLI_update-auth-instance47, CLI_update-auth-instance48,
 * CLI_update-auth-instance49, CLI_update-auth-instance50, 
 * CLI_update-auth-instance51, CLI_update-auth-instance52,
 * CLI_update-auth-instance53, CLI_update-auth-instance54,
 * CLI_update-auth-instance55, CLI_update-auth-instance56
 */
public class UpdateAuthInstanceTest extends TestCommon implements CLIExitCodes {
    
    private String locTestName;
    private ResourceBundle rb;
    private String setupRealms;
    private String setupAuthInstances;
    private String authRealm;
    private String name;
    private String attributeValues;
    private String username;
    private String password;
    private boolean useDatafileOption;
    private boolean useVerboseOption;
    private boolean useDebugOption;
    private boolean useLongOptions;
    private boolean useRemoteAuth; 
    private boolean createUser;
    private String expectedMessage;
    private String expectedExitCode;
    private String description;
    private FederationManagerCLI cli;
    
    /** 
     * Creates a new instance of UpdateAuthInstanceTest 
     */
    public UpdateAuthInstanceTest() {
        super("UpdateAuthInstanceTest");
    }
    
    /**
     * This method is intended to provide initial setup.
     * Creates any realms specified in the setup-realms property and creates
     * authentication instances specified in the setup-identities property in 
     * the UpdateAuthInstanceTest.properties.
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
                    "UpdateAuthInstanceTest-Generated");
            setupRealms = (String)rb.getString(locTestName + 
                    "-create-setup-realms");
            setupAuthInstances = (String)rb.getString(locTestName + 
                    "-create-setup-auth-instances");
            useVerboseOption = ((String)rb.getString(locTestName + 
                    "-use-verbose-option")).equals("true");
            useDebugOption = ((String)rb.getString(locTestName + 
                    "-use-debug-option")).equals("true");
            useLongOptions = ((String)rb.getString(locTestName + 
                    "-use-long-options")).equals("true");
            useRemoteAuth = ((String)rb.getString(locTestName + 
                    "-use-remote-auth")).equals("true"); 
                
            log(Level.FINEST, "setup", "use-verbose-option: " + 
                    useVerboseOption);
            log(Level.FINEST, "setup", "use-debug-option: " + useDebugOption);
            log(Level.FINEST, "setup", "use-long-options: " + useLongOptions);
            log(Level.FINEST, "setup", "create-setup-realms: " + setupRealms);
            log(Level.FINEST, "testAuthInstanceCreation", "use-remote-auth: " + 
                    useRemoteAuth);
            
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("SetupRealms: " + setupRealms);
            Reporter.log("UseRemoteAuth: " + useRemoteAuth);

            cli = new FederationManagerCLI(useDebugOption, useVerboseOption, 
                    useLongOptions);
                       
            if (setupRealms != null && !setupRealms.equals("")) {
                if (!cli.createRealms(setupRealms)) {
                    log(Level.SEVERE, "setup", 
                            "All the realms failed to be created.");
                    assert false;
                }
            }
            
            if (setupAuthInstances != null && !setupAuthInstances.equals("")) {
                if (!cli.createAuthInstances(setupAuthInstances)) {
                    log(Level.SEVERE, "setup", 
                            "All the auth instances failed to be created");
                    assert false;
                }
            }
            
            exiting("setup");
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        } 
    }
    
    /**
     * This method is used to execute tests involving 
     * "ssoadm update-auth-instance" using input data from the 
     * UpdateAuthInstanceTest.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testUpdateAuthInstance() 
    throws Exception {
        entering("testUpdateAuthInstance", null);
        boolean stringsFound = false;
        boolean attributesFound = false;
        boolean errorFound = false;
        boolean authSuccessful = true;
        String attributesToFind;
        
        try {
            expectedMessage = (String) rb.getString(locTestName + 
                    "-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-expected-exit-code");
            authRealm = (String) rb.getString(locTestName + 
                    "-update-auth-instance-realm");
            name = (String) rb.getString(locTestName + 
                    "-update-auth-instance-name");
            attributeValues = (String) rb.getString(locTestName + 
                    "-update-auth-instance-attribute-values");
            attributesToFind= (String) rb.getString(locTestName +
                    "-attributes-to-find");
            useDatafileOption = ((String)rb.getString(locTestName + 
                    "-use-datafile-option")).equals("true");  
            createUser = ((String)rb.getString(locTestName + 
                    "-create-user")).equals("true"); 
            description = (String) rb.getString(locTestName + "-description");
 
            log(Level.FINEST, "testUpdateAuthInstance", "description: " + 
                    description);
            log(Level.FINEST, "testUpdateAuthInstance", 
                    "use-debug-option: " + useDebugOption);
            log(Level.FINEST, "testUpdateAuthInstance", 
                    "use-verbose-option: " + useVerboseOption);
            log(Level.FINEST, "testUpdateAuthInstance", 
                    "use-long-options: " + useLongOptions);
            log(Level.FINEST, "testUpdateAuthInstance", 
                    "expected-exit-code: " + expectedExitCode);
            log(Level.FINEST, "testUpdateAuthInstance", 
                    "update-auth-instance-realm: " + authRealm);
            log(Level.FINEST, "testUpdateAuthInstance", 
                    "update-auth-instance-name: " + name);
            log(Level.FINEST, "testUpdateAuthInstance", 
                    "update-auth-instance-attribute-values: " + 
                    attributeValues);
            log(Level.FINEST, "testUpdateAuthInstance", 
                    "use-datafile-option: " + useDatafileOption);
            log(Level.FINEST, "testUpdateAuthInstance", "attributes-to-find: " +
                    attributesToFind);
            log(Level.FINEST, "testAuthInstanceCreation", "use-remote-auth: " + 
                    useRemoteAuth);
            log(Level.FINEST, "testAuthInstanceCreation", "create-user: " + 
                    createUser);
            
            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);
            Reporter.log("Realm: " + authRealm);
            Reporter.log("InstanceName: " + name);
            Reporter.log("UseDatafileOption: " + useDatafileOption);
            Reporter.log("AttributeValues: " + attributeValues);
            Reporter.log("AttributesToFind: " + attributesToFind);
            Reporter.log("UseRemoteAuth: " + useRemoteAuth);
            Reporter.log("CreateUser: " + createUser);
            
            log(Level.FINE, "testUpdateAuthInstance", "Updating the attributes "
                    + "for the instance " + name + ".");
            int commandStatus = cli.updateAuthInstance(authRealm, name, 
                    attributeValues, useDatafileOption);
            cli.logCommand("testUpdateAuthInstance");

            log(Level.FINEST, "testUpdateAuthInstance", "message-to-find: " + 
                    expectedMessage);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            
            String msg = (String) rb.getString(locTestName + 
                    "-message-to-find");           

            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                if (msg.equals("")) {           
                    if (!useVerboseOption) {
                        expectedMessage = 
                                (String) rb.getString("success-message");
                    } else {
                        expectedMessage = 
                                (String) rb.getString(
                                "verbose-success-message");
                    }
                } else {
                    expectedMessage = msg;
                }                
                stringsFound = cli.findStringsInOutput(expectedMessage, ";");
                cli.resetArgList();
                FederationManagerCLI listCLI = 
                        new FederationManagerCLI(useDebugOption, 
                        useVerboseOption, useLongOptions);
                attributesFound = listCLI.findAuthInstanceAttributes(authRealm, 
                        name, attributesToFind);
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

            if (useRemoteAuth) {
                if (performRemoteAuth() != AuthContext.Status.SUCCESS) {
                    log(Level.SEVERE, "testUpdateAuthInstance", "Login with " + 
                            "auth instance " + name + " was not successful.");
                    assert false;
                } else {
                    log(Level.FINEST, "testUpdateAuthInstance", "Login with " +
                            "auth instance " + name + " was successful.");
                }
            }            
            
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                log(Level.FINEST, "testUpdateAuthInstance", 
                        "Output Messages Found: " + stringsFound);
                log(Level.FINEST, "testUpdateAuthInstance", 
                        "Attributes Found: " + attributesFound); 
                log(Level.FINEST, "testUpdateAuthInstance",
                        "Remote Auth Successful: " + authSuccessful);
                assert (commandStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound &&
                        attributesFound && authSuccessful;
            } else {
                log(Level.FINEST, "testUpdateAuthInstance", 
                        "Error Messages Found: " + stringsFound);
                assert (commandStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound;
            }     
            exiting("testUpdateAuthInstance");
        } catch (Exception e) {
            log(Level.SEVERE, "testUpdateAuthInstance", e.getMessage());
            e.printStackTrace();
            throw e;
        } 
    }
    
    /**
     * Use the created auth module to perform a remote authentication.
     *
     * @return A <code>AuthContext.Status</code> value indicating whether 
     * the authentication was successful.
     */
    private AuthContext.Status performRemoteAuth() 
    throws Exception {
       Callback[] callbacks = null;
       AuthContext loginContext = null;
       boolean authSuccessful = false;
       AuthContext.Status authStatus = AuthContext.Status.NOT_STARTED;

       username = (String) rb.getString(locTestName + "-auth-username");
       password = (String) rb.getString(locTestName + "-auth-password");

       log(Level.FINE, "performRemoteAuth", "Set the attribute " +
               "iplanet-am-auth-dynamic-profile-creation in auth " + 
               "service");
       int attrStatus = cli.setServiceAttributes(authRealm, 
               "iPlanetAMAuthService", 
               "iplanet-am-auth-dynamic-profile-creation=ignore;" +
               "sunEnableModuleBasedAuth=true", 
               false);
       cli.logCommand("performRemoteAuth");
       if (attrStatus != SUCCESS_STATUS) {
           log(Level.SEVERE, "performRemoteAuth", "Unable to set the " +
                   "auth service attribute " +
                   "iplanet-am-auth-dynamic-profile-creation");
           assert false;
       }
       cli.resetArgList();
       
       if (createUser) {
           StringBuffer attrBuffer = new StringBuffer();
           attrBuffer.append("givenname=").append(username).append(";").
                   append("cn=").append(username).append(";").append("sn=").
                   append(username).append(";").append("userpassword=").
                   append(password).append(";").
                   append("inetuserstatus=Active");
           cli.resetArgList();
           int userCreationStatus = cli.createIdentity(authRealm, 
                   username, "User", attrBuffer.toString());
           cli.logCommand("testUpdateAuthInstance");
           if (userCreationStatus != SUCCESS_STATUS) {
               log(Level.SEVERE, "performRemoteAuth", "Failed to create " +
                       username + " for remote auth");
               assert false;
           }
       }

       try {
           loginContext = new AuthContext(authRealm.substring(1));
           AuthContext.IndexType indexType = 
                   AuthContext.IndexType.MODULE_INSTANCE;
           loginContext.login(indexType, name);
       } catch (AuthLoginException ale) {
           log(Level.SEVERE, "performRemoteAuth", username +
                   " failed to login.");
           assert false;
       }

       while (loginContext.hasMoreRequirements()) {
           callbacks = loginContext.getRequirements();
           if (callbacks != null) {
               try {
                   for (Callback callback: callbacks) {
                       if (callback instanceof NameCallback) {
                           NameCallback nameCallback = 
                                   (NameCallback) callback;
                           nameCallback.setName(username);
                       }

                       if (callback instanceof PasswordCallback) {
                           PasswordCallback passwordCallback =
                                   (PasswordCallback) callback;
                           passwordCallback.setPassword(
                                   password.toCharArray());
                       }
                   }
                   loginContext.submitRequirements(callbacks);
               } catch (Exception e) {
                   log(Level.SEVERE, "performRemoteAuth", username +
                           " login resulted in an exception.");
                   assert false;
               }
           }      
       }
       return (loginContext.getStatus());
    }
    
    /**
     * This method remove any authentication instances and realms that were 
     * created during the setup method using "ssoadm delete-auth-instances" and 
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
            
            cli.resetArgList(); 
            
            if (useRemoteAuth) {
                if (createUser) {
                    int delStatus = cli.deleteIdentities(authRealm, username, 
                            "User");
                    cli.logCommand("cleanup");
                    cli.resetArgList();
                    if (delStatus != SUCCESS_STATUS) {
                        log(Level.SEVERE, "cleanup", "Unable to delete " + 
                                username + ".");
                        assert false;
                    }
                }
            }
            
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                log(Level.FINE, "cleanup", "Deleting auth instance " + name);
                FederationManagerCLI cleanupCli = 
                        new FederationManagerCLI(useDebugOption, 
                        useVerboseOption, useLongOptions);
                exitStatus = cleanupCli.deleteAuthInstances(authRealm, name);
                cleanupCli.logCommand("cleanup");
                if (exitStatus != SUCCESS_STATUS) {
                    log(Level.SEVERE, "cleanup", "The deletion of auth " + 
                            "instance " + name + " failed with status " + 
                            exitStatus + ".");
                    assert false;
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
                                exitStatus + ".");
                        assert false;
                    }
                } 
            }            
            exiting("cleanup");
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        } 
    }
}

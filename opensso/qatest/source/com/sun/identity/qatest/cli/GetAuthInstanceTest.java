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
 * $Id: GetAuthInstanceTest.java,v 1.5 2009/01/26 23:49:32 nithyas Exp $
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
 * <code>GetAuthInstanceTest</code> is used to execute tests involving the 
 * get-auth-instance sub-command of ssoadm.  This class allows the user to 
 * execute "ssoadm get-auth-instance" with a variety or arguments 
 * (e.g with short or long options, with a password file or password argument, 
 * with a locale argument and a variety of input values.  The properties file 
 * <code>GetAuthInstanceTest.properties</code> contains the input values 
 * which are read by this class.
 *
 * This class automates the following test cases:
 * CLI_get-auth-instance01, CLI_get-auth-instance02, CLI_get-auth-instance03, 
 * CLI_get-auth-instance04, CLI_get-auth-instance05, CLI_get-auth-instance06, 
 * CLI_get-auth-instance07, CLI_get-auth-instance08, CLI_get-auth-instance09, 
 * CLI_get-auth-instance10, CLI_get-auth-instance11, CLI_get-auth-instance12,
 * CLI_get-auth-instance13, CLI_get-auth-instance14, CLI_get-auth-instance15, 
 * CLI_get-auth-instance16, CLI_get-auth-instance17, CLI_get-auth-instance18,
 * CLI_get-auth-instance19, CLI_get-auth-instance20, CLI_get-auth-instance21, 
 * CLI_get-auth-instance22, CLI_get-auth-instance23, CLI_get-auth-instance24,
 * CLI_get-auth-instance25, CLI_get-auth-instance26, CLI_get-auth-instance27,
 * CLI_get-auth-instance28
 */
public class GetAuthInstanceTest extends TestCommon implements CLIExitCodes {
    
    private String locTestName;
    private ResourceBundle rb;
    private String setupRealms;
    private String setupAuthInstances;
    private String setupAuthInstanceAttributes;
    private String authRealm;
    private String name;
    private boolean useVerboseOption;
    private boolean useDebugOption;
    private boolean useLongOptions;
    private String expectedMessage;
    private String expectedExitCode;
    private String description;
    private FederationManagerCLI cli;
    
    /** 
     * Creates a new instance of GetAuthInstanceTest 
     */
    public GetAuthInstanceTest() {
        super("GetAuthInstanceTest");
    }
    
    /**
     * This method is intended to provide initial setup.
     * Creates any realms specified in the setup-realms property and creates
     * any identities specified in the setup-identities property in the 
     * GetAuthInstanceTest.properties.
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
                    "GetAuthInstanceTest");
            setupRealms = (String)rb.getString(locTestName + 
                    "-create-setup-realms");
            setupAuthInstances = (String)rb.getString(locTestName + 
                    "-create-setup-auth-instances");
            setupAuthInstanceAttributes = (String)rb.getString(locTestName +
                    "-setup-auth-instance-attributes");
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
            log(Level.FINEST, "setup", "create-setup-auth-instances: " +
                    setupAuthInstances);
            log(Level.FINEST, "setup", 
                    "setup-auth-instance-attributes: " + 
                    setupAuthInstanceAttributes);
            
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("SetupRealms: " + setupRealms);
            Reporter.log("SetupAuthInstances: " + setupAuthInstances);

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
            
            if (setupAuthInstanceAttributes != null && 
                    !setupAuthInstanceAttributes.equals("")) {
                if (!cli.updateAuthInstances(setupAuthInstanceAttributes)) {
                    log(Level.SEVERE, "setup", 
                            "All the auth instances failed to be updated");
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
     * "ssoadm get-auth-instance" using input data from the 
     * GetAuthInstanceTest.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testGetAuthInstance() 
    throws Exception {
        entering("testGetAuthInstance", null);
        boolean stringsFound = false;
        boolean attributesFound = false;
        boolean errorFound = false;
        String attributesToFind;
        
        try {
            expectedMessage = (String) rb.getString(locTestName + 
                    "-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-expected-exit-code");
            authRealm = (String) rb.getString(locTestName + 
                    "-get-auth-instance-realm");
            name = (String) rb.getString(locTestName + 
                    "-get-auth-instance-name");
            attributesToFind= (String) rb.getString(locTestName +
                    "-attributes-to-find");
            description = (String) rb.getString(locTestName + "-description");
 
            log(Level.FINEST, "testGetAuthInstance", "description: " + 
                    description);
            log(Level.FINEST, "testGetAuthInstance", 
                    "use-debug-option: " + useDebugOption);
            log(Level.FINEST, "testGetAuthInstance", 
                    "use-verbose-option: " + useVerboseOption);
            log(Level.FINEST, "testGetAuthInstance", 
                    "use-long-options: " + useLongOptions);
            log(Level.FINEST, "testGetAuthInstance", 
                    "expected-exit-code: " + expectedExitCode);
            log(Level.FINEST, "testGetAuthInstance", 
                    "get-auth-instance-realm: " + authRealm);
            log(Level.FINEST, "testGetAuthInstance", 
                    "get-auth-instance-name: " + name);
            log(Level.FINEST, "testGetAuthInstance", "attributes-to-find: " +
                    attributesToFind);
            
            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);
            Reporter.log("Realm: " + authRealm);
            Reporter.log("InstanceName: " + name);
            Reporter.log("AttributesToFind: " + attributesToFind);
            
            log(Level.FINE, "testGetAuthInstance", "Updating the attributes "
                    + "for the instance " + name + ".");
            int commandStatus = cli.getAuthInstance(authRealm, name);
            cli.logCommand("testGetAuthInstance");

            log(Level.FINEST, "testGetAuthInstance", "message-to-find: " + 
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
                
                log(Level.FINE, "testGetAuthInstance", "attributes-to-find: "  +
                        attributesToFind);
                Reporter.log("AttributesToFind: " + attributesToFind);
                
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
            
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                log(Level.FINEST, "testGetAuthInstance", 
                        "Output Messages Found: " + stringsFound);
                log(Level.FINEST, "testGetAuthInstance", 
                        "Attributes Found: " + attributesFound); 
                assert (commandStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound &&
                        attributesFound;
            } else {
                log(Level.FINEST, "testGetAuthInstance", 
                        "Error Messages Found: " + stringsFound);
                assert (commandStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound;
            }     
            exiting("testGetAuthInstance");
        } catch (Exception e) {
            log(Level.SEVERE, "testGetAuthInstance", e.getMessage());
            e.printStackTrace();
            throw e;
        } 
    }
    
    /**
     * This method remove any members, identities, and realms that were created 
     * during the setup and testGetAuthInstance methods using 
     * "ssoadm delete-auth-instances" and "ssoadm delete-realm".
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup() 
    throws Exception {
        int cleanupExitStatus = -1;

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
            
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                log(Level.FINE, "cleanup", "Deleting auth instance " + name);
                FederationManagerCLI cleanupCli = 
                        new FederationManagerCLI(useDebugOption, 
                        useVerboseOption, useLongOptions);
                cleanupExitStatus = cleanupCli.deleteAuthInstances(authRealm, 
                        name);
                cleanupCli.logCommand("cleanup");
                if (cleanupExitStatus != SUCCESS_STATUS) {
                    log(Level.SEVERE, "cleanup", "The deletion of auth " + 
                            "instance " + name + " failed with status " + 
                            cleanupExitStatus + ".");
                    assert false;
                }
            }
            
            if (!setupRealms.equals("")) {
                String[] realms = setupRealms.split(";");
                for (int i=realms.length-1; i >= 0; i--) {
                    log(Level.FINE, "cleanup", "Removing realm " + 
                        realms[i]);
                    Reporter.log("SetupRealmToDelete: " + realms[i]);
                    cleanupExitStatus = cli.deleteRealm(realms[i], true); 
                    cli.logCommand("cleanup");
                    cli.resetArgList();
                    if (cleanupExitStatus != SUCCESS_STATUS) {
                        log(Level.SEVERE, "cleanup", "The removal of realm " + 
                                realms[i] + " failed with exit status " + 
                                cleanupExitStatus + ".");
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

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
 * $Id: GetRealmTest.java,v 1.5 2009/01/26 23:49:34 nithyas Exp $
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
 * <code>GetRealmTest</code> is used to execute tests involving the 
 * get-realm sub-command of ssoadm.  This class allows the user to execute 
 * "ssoadm get-realm" with short or long options and a variety of input values.  
 * The properties file <code>GetRealmTest.properties</code> contains 
 * the input values which are read by this class.
 *
 * This class automates the following test cases:
 * CLI_get-realm01, CLI_get-realm02, CLI_get-realm03, CLI_get-realm04, 
 * CLI_get-realm05, CLI_get-realm06
 */

public class GetRealmTest extends TestCommon implements CLIExitCodes {
    
    private String locTestName;
    private ResourceBundle rb;
    private String setupRealms;
    private String setupRealmAttributes;
    private String attributesToFind;    
    private String realm;
    private String serviceName;
    private boolean useVerboseOption;
    private boolean useDebugOption;
    private boolean useLongOptions;
    private String expectedMessage;
    private String expectedExitCode;
    private String description;
    private FederationManagerCLI cli;
    
    /** 
     * Creates a new instance of GetRealmTest 
     */
    public GetRealmTest() {
        super("GetRealmTest");
    }
    
    /**
     * This method is intended to provide initial setup.
     * Creates any realms specified in the setup-realms property in the 
     * GetRealmTest.properties file.
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
                    "GetRealmTest");
            setupRealms = (String)rb.getString(locTestName + 
                    "-create-setup-realms");
            useVerboseOption = ((String)rb.getString(locTestName + 
                    "-use-verbose-option")).equals("true");
            useDebugOption = ((String)rb.getString(locTestName + 
                    "-use-debug-option")).equals("true");
            useLongOptions = ((String)rb.getString(locTestName + 
                    "-use-long-options")).equals("true");
            setupRealmAttributes = (String) rb.getString(locTestName + 
                    "-setup-realm-attributes");
                
            log(Level.FINEST, "setup", "use-verbose-option: " + 
                    useVerboseOption);
            log(Level.FINEST, "setup", "use-debug-option: " + useDebugOption);
            log(Level.FINEST, "setup", "use-long-options: " + useLongOptions);
            log(Level.FINEST, "setup", "create-setup-realms: " + setupRealms);
            log(Level.FINEST, "setup", "setup-realm-attributes: " + 
                    setupRealmAttributes);

            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("SetupRealms: " + setupRealms);
            Reporter.log("SetupRealmAttributes: " + setupRealmAttributes);

            cli = new FederationManagerCLI(useDebugOption, useVerboseOption, 
                    useLongOptions);
            
            log(Level.FINE, "setup", "Creating the following realms: " + 
                    setupRealms);
            
            if (!cli.createRealms(setupRealms)) {
                log(Level.SEVERE, "setup", 
                        "All the realms failed to be created.");
                assert false;
            }
            
            if (setupRealmAttributes.length() > 0) {
                log(Level.FINE, "setup", 
                        "Setting the following realm attributes: " + 
                        setupRealmAttributes);
                String[] realmsToSet = setupRealmAttributes.split("\\|");
                for (int i=0; i < realmsToSet.length; i++) {
                    String[] setArgs = realmsToSet[i].split("\\,");
                    if (setArgs.length == 3) {
                        String realmToSet = setArgs[0];
                        String serviceToSet = setArgs[1];
                        String attrsToSet = setArgs[2];
                        int setStatus = cli.setRealmAttributes(realmToSet, 
                                serviceToSet, attrsToSet, false);
                        cli.logCommand("setup");
                        cli.resetArgList();
                        if (setStatus != 0) {
                            log(Level.SEVERE, "setup", "set-realm-attributes" + 
                                    " failed with exit status " + setStatus);
                            assert false;
                        }
                    } else {
                        log(Level.SEVERE, "setup", 
                                "Setting realm attributes requires a realm " +
                                "name, a service name, and one or more " +
                                "attribute name/value pairs.");
                        assert false;
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
     * This method is used to execute tests involving 
     * "ssoadm get-realm" using input data from the 
     * GetRealmTest.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testRealmGetAttributeValues() 
    throws Exception {
        entering("testRealmGetAttributeValues", null);
        boolean stringsFound = false;
        boolean attributesFound = false;
        boolean errorFound = false;
        int commandStatus = -1;
        
        try {
            description = (String) rb.getString(locTestName + "-description");
            expectedMessage = (String) rb.getString(locTestName + 
                    "-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-expected-exit-code");
            realm = (String) rb.getString(locTestName + 
                    "-get-realm-realm");
            serviceName = (String) rb.getString(locTestName + 
                    "-get-realm-servicename");
            attributesToFind = (String) rb.getString(locTestName + 
                    "-get-realm-attributes-to-find");
 
            log(Level.FINEST, "testRealmGetAttributeValues", "description: " + 
                    description);
            log(Level.FINEST, "testRealmGetAttributeValues", 
                    "use-debug-option: " + useDebugOption);
            log(Level.FINEST, "testRealmGetAttributeValues", 
                    "use-verbose-option: " + useVerboseOption);
            log(Level.FINEST, "testRealmGetAttributeValues", 
                    "use-long-options: " + useLongOptions);
            log(Level.FINEST, "testRealmGetAttributeValues", "message-to-find: "
                    + expectedMessage);
            log(Level.FINEST, "testRealmGetAttributeValues", 
                    "expected-exit-code: " + expectedExitCode);
            log(Level.FINEST, "testRealmGetAttributeValues", "get-realm-realm: "
                    + realm);
            log(Level.FINEST, "testRealmGetAttributeValues", 
                    "get-realm-servicename: " + serviceName);
            log(Level.FINEST, "testRealmGetAttributeValues", 
                    "get-realm-attributes-to-find: " + attributesToFind);
 
            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);
            Reporter.log("Realm: " + realm);
            Reporter.log("ServiceName: " + serviceName);
            Reporter.log("AttributesToFind: " + attributesToFind);
            
            String msg = (String) rb.getString(locTestName + 
                    "-message-to-find");  
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                Object[] params = {realm}; 
                if (msg.equals("")) {           
                    if (useVerboseOption) {
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
            } else if (expectedExitCode.equals(
                    new Integer(INVALID_OPTION_STATUS).toString())) {
                expectedMessage = (String) rb.getString("usage");
            } else {
                expectedMessage = msg;                
            }

            commandStatus = cli.getRealm(realm, serviceName);
            cli.logCommand("testRealmGetAttributeValues");
            cli.resetArgList();
            
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                FederationManagerCLI searchCLI = 
                        new FederationManagerCLI(useDebugOption, 
                        useVerboseOption, useLongOptions);
                attributesFound = searchCLI.findRealmAttributes(realm, 
                        serviceName, attributesToFind);
                searchCLI.logCommand("testRealmGetAttributeValues");
            } else if (expectedExitCode.equals(
                    new Integer(INVALID_OPTION_STATUS).toString())) {
                String argString = cli.getAllArgs().replaceFirst(
                        cli.getCliPath() + fileseparator + "ssoadm", "ssoadm ");
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
                   
            log(Level.FINEST, "testRealmGetAttributeValues", "Exit status: " + 
                    commandStatus);
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                log(Level.FINEST, "testRealmGetAttributeValues", 
                        "Output Messages Found: " + stringsFound);
                log(Level.FINEST, "testRealmGetAttributeValues", 
                        "Attributes Found: " + attributesFound);
                assert (commandStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound &&
                        attributesFound;
            } else {
                log(Level.FINEST, "testRealmGetAttributeValues", 
                        "Error Messages Found: " + errorFound);
                if (expectedExitCode.equals(
                        new Integer(INVALID_OPTION_STATUS).toString())) {
                    log(Level.FINEST, "testRealmGetAttributeValues", 
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
            exiting("testRealmGetAttributeValues");
        } catch (Exception e) {
            log(Level.SEVERE, "testRealmGetAttributeValues", e.getMessage(), 
                    null);
            e.printStackTrace();
            throw e;
        } 
    }
    
    /**
     * This method remove any realms and identities that were created during 
     * the setup and testRealmGetAttributeValues methods using 
     * ssoadm delete-realm".
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

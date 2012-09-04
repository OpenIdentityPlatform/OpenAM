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
 * $Id: AddAttrDefsTest.java,v 1.2 2009/01/26 23:48:52 nithyas Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */

/**
 * AddAttrDefsTest automates the following test cases:
 * CLI_add-attr-defs01, CLI_add-attr-defs02, CLI_add-attr-defs03
 * CLI_add-attr-defs04, CLI_add-attr-defs05, CLI_add-attr-defs06
 * CLI_add-attr-defs07, CLI_add-attr-defs08, CLI_add-attr-defs09
 * CLI_add-attr-defs10
 */

package com.sun.identity.qatest.cli;

import com.sun.identity.qatest.common.cli.CLIExitCodes;
import com.sun.identity.qatest.common.cli.FederationManagerCLI;
import com.sun.identity.qatest.common.TestCommon;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.Reporter;

/**
 * 
 * <code>AddAttrDefsTest</code> is used to execute tests involving the
 * add-attr-defs sub-command of ssoadm.  This class allows the user
 * to execute "ssoadm add-attr-defs" with a variety of arguments (e.g with short
 * or long options, with a password file or password argument, with a locale
 * argument, etc.) and a variety of input values. The properties file
 * <code>AddAttrDefsTest.properties</code> contains the input values
 * which are read by this class.
 */
public class AddAttrDefsTest extends TestCommon implements CLIExitCodes {

    private String locTestName;
    private ResourceBundle rb;
    private boolean useVerboseOption;
    private boolean useDebugOption;
    private boolean useLongOptions;
    private boolean useDataFile;
    private String expectedMessage;
    private String expectedExitCode;
    private String attributesToAdd;
    private String description;
    private String serviceName;
    private String schemaType;
    private String subSchemaName;
    private ArrayList attNameList;
    private String savedAttributes;
    private boolean setupAttributesSaved;
    private FederationManagerCLI cli;

    /**
     * Creates a new instance of AddAttrDefsTest
     */
    public AddAttrDefsTest() {
        super("AddAttrDefsTest");
    }

    /**
     * This method is intended to provide initial setup. Gets all the attribute
     * default values that are being operated in test case and stores in list.
     * setup loads the properties in AddAttrDefsTest.properties.
     */
    @Parameters({"testName"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String testName)
    throws Exception {
        Object[] params = {testName};
        setupAttributesSaved = false;
        entering("setup", params);
        try {
            locTestName = testName;
            rb = ResourceBundle.getBundle("cli" + fileseparator +
            		"AddAttrDefsTest");
            useVerboseOption = ((String)rb.getString(locTestName +
                    "-use-verbose-option")).equals("true");
            useDebugOption = ((String)rb.getString(locTestName +
                    "-use-debug-option")).equals("true");
            useLongOptions = ((String)rb.getString(locTestName +
                    "-use-long-options")).equals("true");
            attributesToAdd = (String) rb.getString(locTestName +
                    "-attributes-to-find");
            serviceName = (String) rb.getString(locTestName +
                    "-service-name");
            schemaType = (String) rb.getString(locTestName +
                    "-schema-type");
            subSchemaName = (String) rb.getString(locTestName +
                    "-sub-schema-name");            
            expectedExitCode = (String) rb.getString(locTestName +
                    "-expected-exit-code");
            expectedExitCode = (String) rb.getString(locTestName +
                    "-expected-exit-code");
            log(Level.FINEST, "setup", "use-verbose-option: " +
                    useVerboseOption);
            log(Level.FINEST, "setup", "use-debug-option: " + useDebugOption);
            log(Level.FINEST, "setup", "use-long-options: " + useLongOptions);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);

            cli = new FederationManagerCLI(useDebugOption, 
                    useVerboseOption, useLongOptions);
            // get attribute names
            attNameList = new ArrayList();
            Map parseStringToMap = parseStringToMap(attributesToAdd, ";", "");
            Iterator itr = parseStringToMap.keySet().iterator();
            while(itr.hasNext()) {
            	attNameList.add(itr.next());
            }
            // execute command "ssoadm get-attr-defs"
            int exitStatus = cli.getAttrDefs(serviceName, schemaType, 
            		subSchemaName,attNameList);
            cli.logCommand("setup");
            // If SUCCESS store attributes in list
            if (exitStatus == SUCCESS_STATUS ) {
            	savedAttributes = cli.getCommand()
                        .getOutput().toString();
            	if (savedAttributes.indexOf("=") != -1) {
            		String[] str = savedAttributes.split("\n");
            		String finalString = "";
            		for (int i=0; i < str.length-1; i++) {
            			if (str[i].indexOf("=") != -1){
            				finalString = finalString + str[i] + ";";
            			}
            		}
            		savedAttributes = finalString.
                                substring(0, finalString.length()-1);
            		setupAttributesSaved = true;
            	} else {
            		log(Level.FINEST, "setup", 
                                "Attribute values are not found");
            	}
            } else {
                log(Level.SEVERE, "setup", "Getting attributes failed");
            }
            cli.resetArgList();
            exiting("setup");
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * This method is used to execute tests involving "ssoadm add-attr-defs"
     * using input data from the AddAttrDefsTest.properties file and verifies
     * attributeservice creation using "ssoadm add-attr-defs".
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testAddAttrDefs()
    throws Exception {
        entering("testAddAttrDefs", null);
        boolean stringsFound = false;

        try {
            description = (String) rb.getString(locTestName + "-description");
            expectedMessage = (String) rb.getString(locTestName +
                    "-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName +
                    "-expected-exit-code");
            useDataFile = ((String)rb.getString(locTestName +
                    "-use-datafile-option")).equals("true");

            log(Level.FINEST, "testAddAttrDefs", "description: " +
                    description);
            log(Level.FINEST, "testAddAttrDefs", "service-name: " +
                    serviceName);
            log(Level.FINEST, "testAddAttrDefs", "schema-type: " +
                    schemaType);
            log(Level.FINEST, "testAddAttrDefs", "attributes-to-find: " +
                    attributesToAdd);
            log(Level.FINEST, "testAddAttrDefs", "use-datafile-option: " +
                    useDataFile);
            log(Level.FINEST, "testAddAttrDefs", "expected-exit-code: " +
                    expectedExitCode);
            log(Level.FINEST, "testAddAttrDefs", "use-verbose-option: " +
                    useVerboseOption);
            log(Level.FINEST, "testAddAttrDefs", "use-long-options: " +
                    useLongOptions);
            log(Level.FINEST, "testAddAttrDefs", "message-to-find: " +
                    expectedMessage);
            log(Level.FINEST, "testAddAttrDefs", "expected-exit-code: " +
                    expectedExitCode);

            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);
            Reporter.log("ServiceName: " + serviceName);
            Reporter.log("SchemaType: " + schemaType);
            Reporter.log("AttributesToFind: " + attributesToAdd);
            Reporter.log("UseDatafileOption: " + useDataFile);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);

            int commandStatus = cli.addAttrDefs(serviceName, 
                    schemaType, attributesToAdd, useDataFile, subSchemaName);
            cli.logCommand("testAddAttrDefs");
            Map attribValueMap = parseStringToMap(attributesToAdd, ";", "");

            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                    stringsFound = cli.
                            findStringsInOutput(expectedMessage, ";");
                    log(Level.FINEST, "testAddAttrDefs", "Default attributes " +
                        "have been added to the service: " + serviceName);
                    assert (commandStatus ==
                    new Integer(expectedExitCode).intValue()) &&
                        stringsFound;
            } else {
                if (!expectedExitCode.equals(
                        new Integer(INVALID_OPTION_STATUS).toString())) {
                    stringsFound = cli.findStringsInError(expectedMessage, ";");
                } else {
                    String argString = cli.getAllArgs().replaceFirst(
                            cli.getCliPath(), "ssoadm ");
                    Object[] params = {argString};
                    String usageError = MessageFormat.format(expectedMessage,
                            params);
                    stringsFound = cli.findStringsInError(
                            usageError, ";" + newline);
                }
                log(logLevel, "testAddAttrDefs", "Error Messages Found: " +
                        stringsFound);
                assert (commandStatus ==
                    new Integer(expectedExitCode).intValue()) && stringsFound;
            }
            cli.resetArgList();
            
            // Verify added attributes using "ssoadm get-attr-defs".
            commandStatus = cli.getAttrDefs(serviceName, schemaType,
                    subSchemaName, attNameList);
            cli.logCommand("testAddAttrDefs");

            if (commandStatus == SUCCESS_STATUS) {
                String str = cli.getCommand().getOutput().toString();
                if (str.indexOf("=") != -1) {
                    Map updatedAttrMap = parseStringToMap(
                            attributesToAdd, "\n", "");
                    if (isAttrValuesEqual(attribValueMap, updatedAttrMap)) {
                        log(Level.FINEST, "testAddAttrDefs", "Verifeid the " +
                                "attributes that are modified.");
                    } 
                } else {
                	log(Level.FINEST, "setup", "Attribute " +
                                "values are not found");
                }
            } else {
                log(Level.SEVERE, "setup", "Getting attributes failed");
            }
            cli.resetArgList();
            exiting("testAddAttrDefs");
        } catch (Exception e) {
            log(Level.SEVERE, "testAddAttrDefs", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * This method remove any Services that were created during the setup and
     * testAddAttrDefs methods using "ssoadm set-attr-defs".
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
            if (setupAttributesSaved) {
                // Reset the attributes using "ssoadm set-attr-defs"
                int exitStatus = cli.setAttrDefs(serviceName, 
                        schemaType, savedAttributes, useDataFile, subSchemaName);
                cli.logCommand("cleanup");
                if (exitStatus == SUCCESS_STATUS) {
                    log(Level.FINEST, "cleanup", "Attributes has been reset " +
                            "successfully");
                } else {
                    log(Level.SEVERE, "cleanup",
                            "Attributes failed to reset for " + serviceName + 
                            "with the failed exit status " + exitStatus + ".");
                    assert false;
                }
                cli.resetArgList();
            }
            exiting("cleanup");
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        }
    }
}

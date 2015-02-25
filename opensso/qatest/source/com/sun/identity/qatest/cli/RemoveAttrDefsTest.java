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
 * $Id: RemoveAttrDefsTest.java,v 1.2 2009/01/26 23:49:37 nithyas Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */

/**
 * RemoveAttrDefsTest automates the following test cases:
 * CLI_remove-attr-defs01, CLI_remove-attr-defs02, CLI_remove-attr-defs03
 * CLI_remove-attr-defs04, CLI_remove-attr-defs05, CLI_remove-attr-defs06
 * CLI_remove-attr-defs07, CLI_remove-attr-defs08, CLI_remove-attr-defs09
 * CLI_remove-attr-defs10
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
 * <code>RemoveAttrDefsTest</code> is used to execute tests involving the
 * remove-attr-defs sub-command of ssoadm.  This class allows the user to 
 * execute "ssoadm remove-attr-defs" with a variety of arguments (e.g with short
 * or long options, with a password file or password argument, with a locale
 * argument, etc.) and a variety of input values. The properties file
 * <code>RemoveAttrDefsTest.properties</code> contains the input values
 * which are read by this class.
 */
public class RemoveAttrDefsTest extends TestCommon implements CLIExitCodes {

    private String locTestName;
    private ResourceBundle rb;
    private boolean useVerboseOption;
    private boolean useDebugOption;
    private boolean useLongOptions;
    private boolean useDataFile;
    private String expectedMessage;
    private String expectedExitCode;
    private String description;
    private String serviceName;
    private String schemaType;
    private String subSchemaName;
    private String attrNames;
    private ArrayList attNameList;
    private String savedAttributes;
    private boolean setupAttributesSaved;
    private FederationManagerCLI cli;

    /**
     * Creates a new instance of RemoveAttrDefsTest
     */
    public RemoveAttrDefsTest() {
        super("RemoveAttrDefsTest");
    }

    /**
     * This method is intended to provide initial setup. Gets all the attribute
     * default values that are being operated in test case and stores in list.
     * setup loads the properties in RemoveAttrDefsTest.properties.
     */
    @Parameters({"testName"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String testName)
    throws Exception {
        Object[] params = {testName};
        attNameList = new ArrayList();
        entering("setup", params);
        try {
            locTestName = testName;
            rb = ResourceBundle.getBundle("cli" + fileseparator +
            		"RemoveAttrDefsTest");
            useVerboseOption = ((String)rb.getString(locTestName +
                    "-use-verbose-option")).equals("true");
            useDebugOption = ((String)rb.getString(locTestName +
                    "-use-debug-option")).equals("true");
            useLongOptions = ((String)rb.getString(locTestName +
                    "-use-long-options")).equals("true");
            serviceName = (String) rb.getString(locTestName +
                    "-service-name");
            schemaType = (String) rb.getString(locTestName +
                    "-schematype");
            subSchemaName = (String) rb.getString(locTestName +
                    "-sub-schema");            
            expectedExitCode = (String) rb.getString(locTestName +
                    "-expected-exit-code");
            attrNames = (String)rb.getString(locTestName + "-attribute-names");
            log(Level.FINEST, "setup", "use-verbose-option: " +
                    useVerboseOption);
            log(Level.FINEST, "setup", "use-debug-option: " + useDebugOption);
            log(Level.FINEST, "setup", "use-long-options: " + useLongOptions);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            
            cli = new FederationManagerCLI(
                    useDebugOption, useVerboseOption, useLongOptions);
            // get attribute names
            String[] attName = attrNames.split(";");
            for (int i=0; i < attName.length; i++) {
                attNameList.add(attName[i]);
            }
            // execute command "ssoadm get-attr-defs"
            int exitStatus = cli.getAttrDefs(serviceName, schemaType, 
            		subSchemaName,attNameList);
            cli.logCommand("setup");
            // if SUCCESS store attributes in list
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
            		savedAttributes = finalString.substring(0,
                                finalString.length()-1);
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
     * using input data from the RemoveAttrDefsTest.properties file and verifies
     * service attributes using "ssoadm get-attr-defs".
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testRemoveAttrDefs()
    throws Exception {
        entering("testAddAttrDefs", null);
        boolean stringsFound = false;
        try {
            description = (String) rb.getString(locTestName + "-description");
            expectedMessage = (String) rb.getString(locTestName +
                    "-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName +
                    "-expected-exit-code");
            serviceName = (String) rb.getString(locTestName +
                    "-service-name");
            schemaType = (String) rb.getString(locTestName +
                    "-schematype");
            subSchemaName = (String) rb.getString(locTestName +
                    "-sub-schema");

            log(Level.FINEST, "testAddAttrDefs", "description: " +
                    description);
            log(Level.FINEST, "testAddAttrDefs", "service-name: " +
                    serviceName);
            log(Level.FINEST, "testAddAttrDefs", "schema-type: " +
                    schemaType);
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
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);

            // execute command "ssoadm remove-attr-defs"
            int commandStatus = cli.removeAttrDefs(serviceName, 
                    schemaType, subSchemaName, attrNames);
            cli.logCommand("testRemoveAttrDefs");

            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                    stringsFound = cli.
                            findStringsInOutput(expectedMessage, ";");
                    log(Level.FINEST, "testRemoveAttrDefs", "Default attributes " +
                        "have been removed from the service: " + serviceName);
                    assert (commandStatus ==
                    new Integer(expectedExitCode).intValue()) &&
                        stringsFound;
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
                    stringsFound = cli.findStringsInError(
                            usageError, ";" + newline);
                }
                log(logLevel, "testRemoveAttrDefs", 
                        "Error Messages Found: " + stringsFound);
                assert (commandStatus ==
                    new Integer(expectedExitCode).intValue()) && stringsFound;
            }
            cli.resetArgList();
            
            // verify attributes are removed properly using 
            // "ssoadm get-attr-defs".
            commandStatus = cli.getAttrDefs(serviceName, schemaType,
                    subSchemaName, attNameList);
            cli.logCommand("testRemoveAttrDefs");
            if (commandStatus == SUCCESS_STATUS) {
            	String str = cli.getCommand().getOutput().toString();
            	//check if output has empty attribute map
            	if (str.indexOf("=") != 0) {
                    Map updatedAttrMap = getAttributeMap(str, "\n");
                    Iterator itr = updatedAttrMap.keySet().iterator();
                    String str1 = "";
                    while (itr.hasNext()) {
                       str1 += (String)updatedAttrMap.get(itr.next());
                    }
                    if (str1.trim().equals("")) {
                        log(Level.FINEST, "testRemoveAttrDefs", "Verifeid that " +
                                "attributes are removed for the service: " + 
                                serviceName );
                    }
                } else {
                    log(Level.FINEST, "setup", "Attribute values are not found");
                }
            } else {
                log(Level.SEVERE, "setup", "Getting attributes failed");
            }
            cli.resetArgList();
            exiting("testRemoveAttrDefs");
        } catch (Exception e) {
            log(Level.SEVERE, "testRemoveAttrDefs", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * This method adds attributes that were removed during the setup and
     * testRemoveAttrDefs methods using "ssoadm set-attr-defs".
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
                // reset the attributes using "ssoadm set-attr-defs"
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

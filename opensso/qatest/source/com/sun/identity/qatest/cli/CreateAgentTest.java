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
 * $Id: CreateAgentTest.java,v 1.3 2009/01/26 23:48:55 nithyas Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */

/**
 * CreateAgentTest automates the following test cases:
 * CLI_create-agent01, CLI_create-agent01a,
 * CLI_create-agent02,CLI_create-agent02a,
 * CLI_create-agent03,CLI_create-agent03a,
 * CLI_create-agent04a,CLI_create-agent04b,CLI_create-agent04c,
 * CLI_create-agent05,CLI_create-agent06,CLI_create-agent07,CLI_create-agent08,
 * CLI_create-agent09
 * 
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
 * <code>CreateAgentTest</code> is used to execute tests involving the 
 * create-agent sub-command of ssoadm.  This class allows the user to execute
 * "ssoadm create-agent" with a variety or arguments (e.g with short or long 
 * options, with a password file or password argument, with a locale argument,
 * with a list of attributes or a datafile containing attributes, etc.) 
 * and a variety of input values.  The properties file 
 * <code>CreateAgentTest.properties</code> contains the input values which 
 * are read by this class.
 */
public class CreateAgentTest extends TestCommon implements CLIExitCodes {
    
    private String locTestName;
    private ResourceBundle rb;
    private String setupRealms;
    private String setupAgents;
    private String realmForAgent;
    private String agentNameToCreate;
    private String agentTypeToCreate;
    private String agentAttributeValues;
    private boolean useVerboseOption;
    private boolean useDebugOption;
    private boolean useLongOptions;
    private boolean useAttributeValuesOption;
    private boolean useDatafileOption;
    private String expectedMessage;
    private String expectedExitCode;
    private String description;
    private FederationManagerCLI cli;
    
    /** 
     * Creates a new instance of CreateAgentTest 
     */
    public CreateAgentTest() {
        super("CreateAgentTest");
    }
    
    /**
     * This method is intended to provide initial setup.
     * Creates any realms specified in the setup-realms property and creates
     * any agents specified in the setup-agents property in the 
     * CreateAgentTest.properties.
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
                    "CreateAgentTest");
            setupRealms = (String)rb.getString(locTestName + 
                    "-create-setup-realms");
            setupAgents = (String)rb.getString(locTestName + 
                    "-create-setup-agents");
            useVerboseOption = ((String)rb.getString(locTestName + 
                    "-use-verbose-option")).equals("true");
            useDebugOption = ((String)rb.getString(locTestName + 
                    "-use-debug-option")).equals("true");
            useLongOptions = ((String)rb.getString(locTestName + 
                    "-use-long-options")).equals("true");
                
            log(Level.FINE, "setup", "use-verbose-option: " + useVerboseOption);
            log(Level.FINE, "setup", "use-debug-option: " + useDebugOption);
            log(Level.FINE, "setup", "use-long-options: " + useLongOptions);
            log(Level.FINE, "setup", "create-setup-realms: " + setupRealms);
            log(Level.FINE, "setup", "create-setup-agents: " + 
                    setupAgents);
             
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("SetupRealms: " + setupRealms);
            Reporter.log("SetupAgents: " + setupAgents);            

            cli = new FederationManagerCLI(useDebugOption, useVerboseOption, 
                    useLongOptions);
            
            if (setupRealms != null && !setupRealms.equals("")) {
                if (!cli.createRealms(setupRealms)) {
                    log(Level.SEVERE, "setup", 
                            "All the realms failed to be created.");
                    assert false;
                }
            }
            
            if (setupAgents != null) {
                if (setupAgents.length() > 0) {
                    StringTokenizer idTokenizer = 
                            new StringTokenizer(setupAgents, "|");
                    while (idTokenizer.hasMoreTokens()) {
                        StringTokenizer tokenizer = 
                                new StringTokenizer(idTokenizer.nextToken(), 
                                ",");
                        if (tokenizer.countTokens() >= 4) {
                            String agentRealm = tokenizer.nextToken();
                            String agentName = tokenizer.nextToken();
                            String agentType = tokenizer.nextToken();
                            String agentAttributes = null;
                            if (tokenizer.hasMoreTokens()) {
                                agentAttributes = tokenizer.nextToken();
                                cli.createAgent(agentRealm, agentName, 
                                        agentType, agentAttributes); 
                            } else {
                                cli.createAgent(agentRealm, agentName, 
                                        agentType, "");
                            }
                            cli.logCommand("setup");
                            cli.resetArgList();
                        } else {
                            log(Level.SEVERE, "setup", "The setup agent " + 
                                    setupAgents + 
                                    " must have a realm, an " +
                                    "agent name, an agent type and a password");
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
     * This method is used to execute tests involving "ssoadm create-agent"
     * using input data from the CreateAgentTest.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testAgentCreation() 
    throws Exception {
        entering("testAgentCreation", null);
        boolean stringsFound = false;
        boolean agentFound = false;
        
        try {
            expectedMessage = (String) rb.getString(locTestName + 
                    "-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-expected-exit-code");
            realmForAgent = (String) rb.getString(locTestName + 
                    "-create-agent-realm");
            agentNameToCreate = (String) rb.getString(locTestName + 
                    "-create-agent-name");
            agentTypeToCreate = (String) rb.getString(locTestName + 
                    "-create-agent-type");
            useAttributeValuesOption = ((String)rb.getString(locTestName + 
                    "-use-attribute-values-option")).equals("true");
            useDatafileOption = ((String)rb.getString(locTestName + 
                    "-use-datafile-option")).equals("true"); 
            agentAttributeValues = (String) rb.getString(locTestName + 
                    "-create-agent-attributes");
            description = (String) rb.getString(locTestName + "-description");

            log(Level.FINE, "testAgentCreation", "description: " + 
                    description);
            log(Level.FINE, "testAgentCreation", "use-debug-option: " + 
                    useDebugOption);
            log(Level.FINE, "testAgentCreation", "use-verbose-option: " + 
                    useVerboseOption);
            log(Level.FINE, "testAgentCreation", "use-long-options: " + 
                    useLongOptions);
            log(Level.FINE, "testAgentCreation", "message-to-find: " + 
                    expectedMessage);
            log(Level.FINE, "testAgentCreation", "expected-exit-code: " + 
                    expectedExitCode);
            log(Level.FINE, "testAgentCreation", "create-agent-realm: " + 
                    realmForAgent);
            log(Level.FINE, "testAgentCreation", "create-agent-name: " + 
                    agentNameToCreate);
            log(Level.FINE, "testAgentCreation", "create-agent-type: " + 
                    agentTypeToCreate);
            log(Level.FINE, "testAgentCreation", 
                    "use-attribute-values-option: " + useAttributeValuesOption);
            log(Level.FINE, "testAgentCreation", "use-datafile-option: " + 
                    useDatafileOption);
            log(Level.FINE, "testAgentCreation", 
                    "create-agent-attributes: " + agentAttributeValues);

            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);
            Reporter.log("agentRealmToCreate: " + realmForAgent);
            Reporter.log("agentNameToCreate: " + agentNameToCreate);
            Reporter.log("agentTypeToCreate: " + agentTypeToCreate);
            Reporter.log("UseAttributeValuesOption: " + 
                    useAttributeValuesOption);
            Reporter.log("UseDatafileOption: " + useDatafileOption);
            Reporter.log("agentAttributeValues: " + agentAttributeValues);
            
            int commandStatus = cli.createAgent(realmForAgent, 
                    agentNameToCreate, agentTypeToCreate, agentAttributeValues, 
                    useAttributeValuesOption, useDatafileOption);
            cli.logCommand("testAgentCreation");

            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                stringsFound = cli.findStringsInOutput(expectedMessage, ";");
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
                    stringsFound = cli.findStringsInError(usageError,
                            ";" + newline);
                }
            }     
            cli.resetArgList();

            if (agentNameToCreate.length() > 0) {
                FederationManagerCLI listCLI = 
                        new FederationManagerCLI(useDebugOption, 
                        useVerboseOption, useLongOptions);
                agentFound = listCLI.findAgents(realmForAgent, 
                        agentNameToCreate, agentTypeToCreate, 
                        agentNameToCreate);
                log(Level.FINE, "testAgentCreation", agentTypeToCreate + 
                        "agent " + agentNameToCreate + " Found: " + agentFound);
            } 

            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                log(Level.FINE, "testAgentCreation", "Output Messages Found: "
                        + stringsFound);
                log(Level.FINE, "testAgentCreation", "ID Found: " + 
                        agentFound); 
                assert (commandStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound &&
                        agentFound;
            } else {
                log(Level.FINE, "testAgentCreation", 
			"Error Messages Found: " + stringsFound);
                assert (commandStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound;
            }     
            exiting("testAgentCreation");
        } catch (Exception e) {
            log(Level.SEVERE, "testAgentCreation", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        } 
    }
    
    /**
     * This method remove any realms and agents that were created during 
     * the setup and testAgentCreation methods using "ssoadm delete-realm" 
     * and "ssoadm delete-agents".
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup() 
    throws Exception {
        int cleanupExitStatus = -1;
        int cleanupSetupExitStatus = -1;        
        boolean agentFound = false;
        FederationManagerCLI listCLI = new FederationManagerCLI(useDebugOption, 
                useVerboseOption, useLongOptions);        
        entering("cleanup", null);
        try {            
            log(Level.FINE, "cleanup", "useDebugOption: " + useDebugOption);
            log(Level.FINE, "cleanup", "useVerboseOption: " + useVerboseOption);
            log(Level.FINE, "cleanup", "useLongOptions: " + useLongOptions);
            
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            
            FederationManagerCLI cleanupCli = new FederationManagerCLI(
                    useDebugOption, useVerboseOption, useLongOptions);

            if (!agentNameToCreate.equals("")) {
                log(Level.FINE, "cleanup", "agentToDelete: "  + 
                        agentNameToCreate);
                Reporter.log("AgentNameToDelete: " + agentNameToCreate);
                agentFound = cleanupCli.findAgents(realmForAgent, 
                        agentNameToCreate, agentTypeToCreate, 
                        agentNameToCreate);
                cleanupCli.resetArgList();
                if (agentFound) {
                    cleanupExitStatus = cleanupCli.deleteAgents(
                            realmForAgent, agentNameToCreate, 
                            agentTypeToCreate);
                    cleanupCli.logCommand("cleanup");
                    if (cleanupExitStatus != SUCCESS_STATUS) {
                        log(Level.SEVERE, "cleanup", 
                                "The deletion of agent " + agentNameToCreate
                                + " ,of type " + agentTypeToCreate + 
                                " returned the failed exit status " 
                                + cleanupExitStatus + ".");
                        assert false;
                    }
                    cleanupCli.resetArgList();
                    if (listCLI.findAgents(realmForAgent, agentNameToCreate, 
                    
                            agentTypeToCreate, agentNameToCreate)) {
                        log(Level.SEVERE, "cleanup", "The agent : " + 
                                agentNameToCreate + " of type " + 
                                agentTypeToCreate + " was not deleted.");
                        assert false;
                    }
                    cleanupCli.resetArgList();
                }
            }
            
            FederationManagerCLI cleanupSetupCli = new FederationManagerCLI(
                    useDebugOption, useVerboseOption, useLongOptions);

            if (setupAgents != null) {
                if (setupAgents.length() > 0) {
                    String [] cleanupAgents = setupAgents.split("\\|");
                    for (int i = 0; i < cleanupAgents.length; i++) {
                        String [] agentArgs = cleanupAgents[i].split("\\,");
                        if (agentArgs.length >= 3) {
                            String agentRealm = agentArgs[0];
                            String agentName = agentArgs[1];
                            String agentType = agentArgs[2];
                            
                            log(Level.FINEST, "cleanup", "agentRealm: " + 
				agentRealm);
                            log(Level.FINEST, "cleanup", "agentName: " + 
                                    agentName);
                            log(Level.FINEST, "cleanup", "agentType: " + 
                                    agentType);
                            
                            Reporter.log("AgentRealm: " + agentRealm);
                            Reporter.log("AgentNameToRemove: " + agentName);
                            Reporter.log("AgentTypeToRemove: " + agentType);
                            
                            if (agentName.equals(agentNameToCreate) && 
                                    cleanupExitStatus == SUCCESS_STATUS) {
                                log(Level.FINEST, "cleanup", "Agent : " + 
                                        agentName + ", already deleted");
                            } else {
                                agentFound = cleanupSetupCli.findAgents(
                                        realmForAgent, agentNameToCreate, 
                                        agentTypeToCreate, agentNameToCreate);
                                cleanupSetupCli.resetArgList();
                                if (agentFound) {
                                    cleanupSetupExitStatus = 
                                            cleanupCli.deleteAgents(
                                            agentRealm, agentName, agentType);
                                    cleanupSetupCli.resetArgList();
                                    if (cleanupSetupExitStatus != 
                                            SUCCESS_STATUS) {
                                        log(Level.SEVERE, "cleanup", 
                                                "The deletion of agent : " + 
                                                agentName + " ,of type " + 
                                                agentType + " returned the " +
                                                "failed exit status " 
                                                + cleanupExitStatus + ".");
                                        assert false;
                                    }
                                    listCLI = new FederationManagerCLI(
                                            useDebugOption, useVerboseOption, 
                                            useLongOptions);        
                                    if (listCLI.findAgents(agentRealm, 
                                            agentName, agentType, agentName)) {
                                        log(Level.SEVERE, "cleanup", "The " +
                                                "agent" + agentName + " of " +
                                                "type " + agentType 
                                                + " was not deleted.");
                                        assert false;
                                    }
                                    listCLI.resetArgList();
                                }
                            }
                        } else {
                            log(Level.SEVERE, "cleanup", "The setup agents " + 
                                    setupAgents + " must have a realm, " +
                                    "an agent name, and an agent type");
                        }
                    }
                }
            }
            listCLI = new FederationManagerCLI(useDebugOption, 
                    useVerboseOption, useLongOptions);    
            listCLI.resetArgList();
            if (!setupRealms.equals("")) {
                String[] realms = setupRealms.split(";");
                for (int i=realms.length-1; i >= 0; i--) {
                    log(Level.FINEST, "cleanup", "setupRealmToDelete: " + 
                        realms[i]);
                    Reporter.log("SetupRealmToDelete: " + realms[i]);
                    cleanupExitStatus = listCLI.deleteRealm(realms[i], true); 
                    listCLI.logCommand("cleanup");
                    listCLI.resetArgList();
                    if (cleanupExitStatus != SUCCESS_STATUS) {
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

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
 * $Id: DeleteIdentitiesTest.java,v 1.8 2009/01/26 23:48:59 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/**
 * DeleteIdentitiesTest automates the following test cases:
 * CLI_delete-identities01, CLI_delete-identities02, CLI_delete-identities03,
 * CLI_delete-identities04, CLI_delete-identities05, CLI_delete-identities06, 
 * CLI_delete-identities07, CLI_delete-identities08, CLI_delete-identities09,
 * CLI_delete-identities10, CLI_delete-identities11, CLI_delete-identities12, 
 * CLI_delete-identities13, CLI_delete-identities14, CLI_delete-identities15,
 * CLI_delete-identities16, CLI_delete-identities17, CLI_delete-identities18, 
 * CLI_delete-identities19, CLI_delete-identities20, CLI_delete-identities21, 
 * CLI_delete-identities22, CLI_delete-identities23, CLI_delete-identities24, 
 * CLI_delete-identities25, CLI_delete-identities26, CLI_delete-identities27,
 * CLI_delete-identities28, CLI_delete-identities29, CLI_delete-identities30, 
 * CLI_delete-identities31, CLI_delete-identities32, CLI_delete-identities33, 
 * CLI_delete-identities34, CLI_delete-identities35, CLI_delete-identities36, 
 * CLI_delete-identities37, CLI_delete-identities38, 
 * and CLI_delete-identities39.
 */

package com.sun.identity.qatest.cli;

import com.sun.identity.qatest.common.cli.CLIExitCodes;
import com.sun.identity.qatest.common.cli.FederationManagerCLI;
import com.sun.identity.qatest.common.TestCommon;
import java.text.MessageFormat;
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
 * <code>DeleteIdentitiesTest</code> is used to execute tests involving the 
 * delete-identities sub-command of ssoadm.  This class allows the user to 
 * execute "ssoadm delete-identities" with a variety or arguments (e.g with 
 * short or long options, with a locale argument, with a list of attributes 
 * or a datafile containing attributes, etc.) and a variety of input values.  
 * The properties file <code>DeleteIdentitiesTest.properties</code> contains 
 * the input values which are read by this class.
 */
public class DeleteIdentitiesTest extends TestCommon implements CLIExitCodes {
    
    private String locTestName;
    private ResourceBundle rb;
    private String setupRealms;
    private String setupIdentities;
    private String realmForId;
    private String idNamesToDelete;
    private String idTypeToDelete;
    private boolean useVerboseOption;
    private boolean useDebugOption;
    private boolean useLongOptions;
    private String expectedMessage;
    private String expectedExitCode;
    private String description;
    private FederationManagerCLI cli;
    private Vector idsNotDeleted;
    
    /** 
     * Creates a new instance of DeleteIdentitiesTest 
     */
    public DeleteIdentitiesTest() {
        super("DeleteIdentitiesTest");
    }
    
    /**
     * This method is intended to provide initial setup.
     * Creates any realms specified in the setup-realms property and creates
     * any identities specified in the setup-identities property in the 
     * DeleteIdentitiesTest.properties.
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
                    "DeleteIdentitiesTest");
            setupRealms = (String)rb.getString(locTestName + 
                    "-create-setup-realms");
            setupIdentities = (String)rb.getString(locTestName + 
                    "-create-setup-identities");
            useVerboseOption = ((String)rb.getString(locTestName + 
                    "-use-verbose-option")).equals("true");
            useDebugOption = ((String)rb.getString(locTestName + 
                    "-use-debug-option")).equals("true");
            useLongOptions = ((String)rb.getString(locTestName + 
                    "-use-long-options")).equals("true");
                
            log(logLevel, "setup", "use-verbose-option: " + useVerboseOption);
            log(logLevel, "setup", "use-debug-option: " + useDebugOption);
            log(logLevel, "setup", "use-long-options: " + useLongOptions);
            log(logLevel, "setup", "create-setup-realms: " + setupRealms);
            log(logLevel, "setup", "create-setup-identities: " + 
                    setupIdentities);
             
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("SetupRealms: " + setupRealms);
            Reporter.log("SetupIdentities: " + setupIdentities);

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
            
            idsNotDeleted = new Vector();
            String setupID = null;
            if (setupIdentities != null) {
                if (setupIdentities.length() > 0) {
                    StringTokenizer idTokenizer = 
                            new StringTokenizer(setupIdentities, "|");
                    while (idTokenizer.hasMoreTokens()) {
                        setupID = idTokenizer.nextToken();
                        StringTokenizer tokenizer = 
                                new StringTokenizer(setupID, ",");
                        if (tokenizer.countTokens() >= 3) {
                            String idRealm = tokenizer.nextToken();
                            String idName = tokenizer.nextToken();
                            String idType = tokenizer.nextToken();
                            idsNotDeleted.add(idRealm + "," + idName + "," + 
                                    idType);
                            log(Level.FINEST, "setup", "idsNotDeleted: " + 
                                    idsNotDeleted);
                            String idAttributes = null;
                            if (tokenizer.hasMoreTokens()) {
                                idAttributes = tokenizer.nextToken();
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
                                log(Level.SEVERE, "setup", "The identity " + 
                                        idName + " failed to be created.");
                            }
                        } else {
                            assert false;
                            log(Level.SEVERE, "setup", "The setup identity " + 
                                    setupIdentities + 
                                    " must have a realm, an " +
                                    "identity name, and an identity type");
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
     * This method is used to execute tests involving "ssoadm delete-identities"
     * using input data from the DeleteIdentitiesTest.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testIdentityDeletion() 
    throws Exception {
        entering("testIdentityDeletion", null);
        boolean stringsFound = false;
        boolean idsRemovedFound = true;
        boolean remainingIdsRemoved = false;
        
        try {
            expectedMessage = (String) rb.getString(locTestName + 
                    "-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-expected-exit-code");
            realmForId = (String) rb.getString(locTestName + 
                    "-delete-identities-realm");
            idNamesToDelete = (String) rb.getString(locTestName + 
                    "-delete-identities-names");
            idTypeToDelete = (String) rb.getString(locTestName + 
                    "-delete-identities-type");
            description = (String) rb.getString(locTestName + "-description");

            log(Level.FINEST, "testIdentityDeletion", "description: " + 
                    description);
            log(Level.FINEST, "testIdentityDeletion", "use-debug-option: " + 
                    useDebugOption);
            log(Level.FINEST, "testIdentityDeletion", "use-verbose-option: " + 
                    useVerboseOption);
            log(Level.FINEST, "testIdentityDeletion", "use-long-options: " + 
                    useLongOptions);
            log(Level.FINEST, "testIdentityDeletion", "message-to-find: " + 
                    expectedMessage);
            log(Level.FINEST, "testIdentityDeletion", "expected-exit-code: " + 
                    expectedExitCode);
            log(Level.FINEST, "testIdentityDeletion", 
                    "delete-identities-realm: " + realmForId);
            log(Level.FINEST, "testIdentityDeletion", 
                    "delete-identities-names: " + idNamesToDelete);
            log(Level.FINEST, "testIdentityDeletion", 
                    "delete-identities-type: " +  idTypeToDelete);

            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);
            Reporter.log("IdRealmToDelete: " + realmForId);
            Reporter.log("IdNamesToDelete: " + idNamesToDelete);
            Reporter.log("IdTypeToDelete: " + idTypeToDelete);
            
            int commandStatus = cli.deleteIdentities(realmForId, 
                    idNamesToDelete, idTypeToDelete);
            cli.logCommand("testIdentityDeletion");
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                stringsFound = cli.findStringsInOutput(expectedMessage, ";" + 
                        newline);
            } else if (expectedExitCode.equals(
                    new Integer(INVALID_OPTION_STATUS).toString())) {
                String argString = cli.getAllArgs().replaceFirst(
                        cli.getCliPath(), "ssoadm ");
                Object[] params = {argString};
                String usageError = MessageFormat.format(expectedMessage, 
                        params);
                stringsFound = cli.findStringsInError(usageError, 
                        ";" + newline);
            } else {
                stringsFound = cli.findStringsInError(expectedMessage, ";" + 
                        newline);
            }
            cli.resetArgList();
            
            StringTokenizer tokenizer = new StringTokenizer(idNamesToDelete, 
                    " ");
            while (tokenizer.hasMoreTokens()) {
                String nameToRemove = tokenizer.nextToken();
                String removeID = realmForId + "," + nameToRemove + "," + 
                        idTypeToDelete;
                if (idsNotDeleted.contains(removeID)) {
                    idsNotDeleted.remove(removeID);
                    log(Level.FINEST, "testIdentityDeletion", "Removed " + 
                            idTypeToDelete + " identity entry " + removeID + 
                            " from idsNotDeleted.");
                } else {
                    log(Level.FINEST, "testIdentityDeletion", idTypeToDelete + 
                            " identity " + nameToRemove + 
                            " was not removed from idsNotDeleted.");
                }
                log(Level.FINEST, "setup", "idsNotDeleted: " + idsNotDeleted);
            }

            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                for (Iterator i = idsNotDeleted.iterator(); i.hasNext(); ) {
                    FederationManagerCLI listCLI = 
                            new FederationManagerCLI(useDebugOption, 
                            useVerboseOption, useLongOptions);
                    String idNotDeleted = (String)i.next();
                    StringTokenizer idTokenizer = 
                            new StringTokenizer(idNotDeleted, ",");
                    String idNotDeletedRealm = idTokenizer.nextToken();
                    String idNotDeletedName = idTokenizer.nextToken();
                    String idNotDeletedType = idTokenizer.nextToken();
                    log(Level.FINER, "testIdentityDeletion", 
                            "Verifying that " + idNotDeleted + 
                            " was not deleted.");
                    if (!listCLI.findIdentities(idNotDeletedRealm, "*", 
                            idNotDeletedType, idNotDeletedName)) {
                        remainingIdsRemoved = true;
                        log(Level.INFO, "testIdentityDeletion", 
                                idNotDeletedType + 
                                " identity " + idNotDeletedName + 
                                " was removed.");
                    } else {
                        log(Level.FINEST, "testIdentityDeletion", 
                                idNotDeletedType + " identity " + 
                                idNotDeletedName + " was not removed.");
                    }
                    listCLI.resetArgList();
                }            

                if (idNamesToDelete.length() > 0) {
                    FederationManagerCLI listCLI2 = 
                            new FederationManagerCLI(useDebugOption, 
                            useVerboseOption, useLongOptions);
                    log(Level.FINEST, "testIdentityDeletion", 
                            "Verifying removal of the following " + 
                            idTypeToDelete.toLowerCase()+ " identities: " +
                                    idNamesToDelete + " were removed.");
                    idsRemovedFound = listCLI2.findIdentities(realmForId, "*",
                            idTypeToDelete, idNamesToDelete);
                    listCLI2.resetArgList();
                }                
                log(Level.FINEST, "testIdentityDeletion", 
                        "Output Messages Found: " + stringsFound);
                log(Level.FINEST, "testIdentityDeletion", 
                        "Remaining IDs deleted: " + remainingIdsRemoved);
                log(Level.FINEST, "testIdentityDeletion", "Deleted IDs found: " 
                        + idsRemovedFound);
                assert (commandStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound &&
                        !remainingIdsRemoved && !idsRemovedFound;
            } else {
                log(Level.FINEST, "testIdentityDeletion", 
                        "Error Messages Found: " + stringsFound);
                assert (commandStatus == 
                    new Integer(expectedExitCode).intValue()) && stringsFound;
            }     
            exiting("testIdentityDeletion");
        } catch (Exception e) {
            log(Level.SEVERE, "testIdentityDeletion", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        } 
    }
    
    /**
     * This method remove any realms and identities that were created during 
     * the setup and testIdentityDeletion methods using 
     * "ssoadm delete-realm" and "ssoadm delete-identities".
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
            
            if (!idNamesToDelete.equals("")) {
                log(Level.FINEST, "cleanup", "identityToDelete: "  + 
                        idNamesToDelete);
                Reporter.log("IdentityNameToDelete: " + idNamesToDelete);
                cli.deleteIdentities(realmForId, idNamesToDelete, 
                        idTypeToDelete);
                cli.logCommand("cleanup");
                cli.resetArgList();
            }
            
            if (setupIdentities != null) {
                if (setupIdentities.length() > 0) {
                    StringTokenizer idTokenizer = 
                            new StringTokenizer(setupIdentities, "|");
                    while (idTokenizer.hasMoreTokens()) {
                        StringTokenizer tokenizer = 
                                new StringTokenizer(idTokenizer.nextToken(), 
                                ",");
                        if (tokenizer.countTokens() >= 3) {
                            String idRealm = tokenizer.nextToken();
                            String idName = tokenizer.nextToken();
                            String idType = tokenizer.nextToken();
                            cli.deleteIdentities(idRealm, idName, idType);
                            cli.resetArgList();
                        } else {
                            log(Level.SEVERE, "cleanup", "The setup identity " + 
                                    setupIdentities + " must have a realm, " +
                                    "an identity name, and an identity type");
                        }
                    }
                }
            }

            if (setupRealms != null) {
                if (setupRealms.length() > 0) {
                    StringTokenizer tokenizer = new StringTokenizer(setupRealms, 
                            ";");
                    List realmList = getListFromTokens(tokenizer);
                    int numOfRealms = realmList.size();
                    for (int i=numOfRealms-1; i>=0; i--) {
                        cli.deleteRealm((String)realmList.get(i));
                        cli.logCommand("cleanup");
                        cli.resetArgList();
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

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
 * $Id: MetadataTests.java,v 1.1 2009/07/13 03:00:52 vimal_67 Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.cli;

import com.sun.identity.qatest.common.cli.CLIExitCodes;
import com.sun.identity.qatest.common.cli.FederationManagerCLI;
import com.sun.identity.qatest.common.TestCommon;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.io.File;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.Reporter;

/**
 * <code>MetadataTests</code> is used to execute tests involving the
 * create-metadata-templ, import-entity, export-entity, list-entities,
 * delete-entity sub-commands of ssoadm. This class allows
 * the user to execute "ssoadm create-metadata-templ", "ssoadm import-entity",
 * "ssoadm export-entity", "ssoadm list-entities" and "ssoadm delete-entity" 
 * with a variety or arguments (e.g with short or long options, with a password
 * file or password argument, with a locale argument, etc.) and a variety of
 * input values.  The properties file <code> MetadataTests.properties</code>
 * contains the input values which are read by this class.
 */
public class MetadataTests extends TestCommon implements CLIExitCodes {

    private String locTestName;
    private ResourceBundle rb;
    private String setupRealms;
    private String midp;
    private String msp;
    private String optionalattributes;
    private String metadatafile;
    private String extendeddatafile;
    private String cotname;
    private String cotrealm;
    private String spec;
    private String entityid;
    private String setupCots;
    private String trustedproviders;
    private String sign;
    private String extendedonly;
    private boolean useVerboseOption;
    private boolean useDebugOption;
    private boolean useLongOptions;
    private String expectedMessage;
    private String expectedExitCode;
    private String description;
    private Set hsetcots;
    private FederationManagerCLI cli;
    private File file;

    /**
     * Creates a new instance of MetadataTests
     */
    public MetadataTests() {
        super("MetadataTests");
    }

    /**
     * This method is intended to provide initial setup. Creates any realms 
     * specified in the setup-realms property and creates any cots specified in
     * the setup-cots property in the MetadataTests.properties and it takes
     * testName parameter from the extended-federationSAML.xml and
     * extended-federationSAML_sec.xml
     */
    @Parameters({"testName"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
    "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    @SuppressWarnings("empty-statement")
    public void setup(String testName)
    throws Exception {
        Object[] params = {testName};
        entering("setup", params);
        try {
            locTestName = testName;
            rb = ResourceBundle.getBundle("cli" + fileseparator +
                    "MetadataTests");
            hsetcots = new HashSet();
            setupRealms = (String)rb.getString(locTestName +
                    "-create-setup-realms");
            setupCots = (String)rb.getString(locTestName +
                    "-create-setup-cots");
            metadatafile = (String) rb.getString(locTestName + "-metadatafile");
            extendeddatafile = (String) rb.getString(locTestName +
                    "-extendeddatafile");
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

            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("SetupRealms: " + setupRealms);

            cli = new FederationManagerCLI(useDebugOption, useVerboseOption,
                    useLongOptions);

            // deleting if the metadata's already present
            file = new File(metadatafile);
            boolean metadataExist = file.exists();
            if (metadataExist)
                file.delete();
            file = new File(extendeddatafile);
            boolean extendeddataExist = file.exists();
            if (extendeddataExist)
                file.delete();

            // creating setup realms
            if (setupRealms != null && !setupRealms.equals("")) {
                if (!cli.createRealms(setupRealms)) {
                    log(Level.SEVERE, "setup",
                            "All the realms failed to be created.");
                    assert false;
                }
            }

            // creating setup cots
            if (setupCots != null) {
                if (setupCots.length() > 0) {
                    StringTokenizer cotTokenizer =
                            new StringTokenizer(setupCots, "|");
                    while (cotTokenizer.hasMoreTokens()) {
                        StringTokenizer tokenizer =
                                new StringTokenizer(cotTokenizer.nextToken(),
                                ",");
                        String cotRealm = tokenizer.nextToken();
                        String cotName = tokenizer.nextToken();
                        int commandStatus = cli.createCot(cotName, cotRealm);
                        hsetcots.add(cotName + "," + cotRealm);
                        cli.logCommand("setup");
                    }
                }
            }

            exiting("setup");
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage(), null);
            e.printStackTrace();
            cleanup();
            throw e;
        }
    }

    /**
     * This method is used to execute tests of "ssoadm create-metadata-templ"
     * using input data from the MetadataTests.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
    "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testCreateMT()
    throws Exception {
        entering("testCreateMT", null);
        boolean stringsFound = false;
        int commandStatus = 1000;        

        try {
            expectedMessage = (String) rb.getString(locTestName +
                    "-create-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName +
                    "-create-expected-exit-code");
            entityid = (String) rb.getString(locTestName +
                    "-entityid");
            metadatafile = (String) rb.getString(locTestName +
                    "-metadatafile");            
            extendeddatafile = (String) rb.getString(locTestName +
                    "-extendeddatafile");            
            midp = (String) rb.getString(locTestName +
                    "-create-midp");
            msp = (String) rb.getString(locTestName +
                    "-create-msp");
            optionalattributes = (String) rb.getString(locTestName +
                    "-create-optionalattributes");
            spec = (String) rb.getString(locTestName +
                    "-spec");
            description = (String) rb.getString(locTestName +
                    "-create-description");

            log(logLevel, "testCreateMT", "description: " + description);
            log(logLevel, "testCreateMT", "use-debug-option: " +
                    useDebugOption);
            log(logLevel, "testCreateMT", "use-verbose-option: " +
                    useVerboseOption);
            log(logLevel, "testCreateMT", "use-long-options: " +
                    useLongOptions);
            log(logLevel, "testCreateMT", "message-to-find: " +
                    expectedMessage);
            log(logLevel, "testCreateMT",
                    "expected-exit-code: " + expectedExitCode);
            log(logLevel, "testCreateMT", "entityid: " + entityid);
            log(logLevel, "testCreateMT", "metadatafile: " + metadatafile);
            log(logLevel, "testCreateMT", "extendeddatafile: " +
                    extendeddatafile);
            log(logLevel, "testCreateMT", "idp metaalias: " + midp);
            log(logLevel, "testCreateMT", "sp metaalias: " + msp);
            log(logLevel, "testCreateMT", "optional attributes: " +
                    optionalattributes);
            log(logLevel, "testCreateMT", "specification: " + spec);

            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);
            Reporter.log("Entityid: " + entityid);
            Reporter.log("metadatafile: " + metadatafile);
            Reporter.log("extendeddatafile: " + extendeddatafile);
            Reporter.log("idp metaalias: " + midp);
            Reporter.log("sp metaalias: " + msp);
            Reporter.log("optional attributes: " + optionalattributes);
            Reporter.log("specification: " + spec);

            cli = new FederationManagerCLI(useDebugOption,
                    useVerboseOption, useLongOptions);

            if (optionalattributes.equals("")) {
                commandStatus = cli.createMetadataTempl(entityid, metadatafile,
                        extendeddatafile, midp, msp, spec);
            } else {
                commandStatus = cli.createMetadataTempl(entityid, metadatafile,
                        extendeddatafile, midp, msp, optionalattributes, spec);
            }           

            cli.logCommand("testCreateMT");

            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                stringsFound = cli.findStringsInOutput(expectedMessage, ";");
            } else {
                if (!expectedExitCode.equals(
                    new Integer(INVALID_OPTION_STATUS).toString())) {
                    stringsFound = cli.findStringsInError(expectedMessage, ";");
                } else {
                    String argString = cli.getAllArgs().replaceFirst(
                            cli.getCliPath(), "ssoadm ");
                    Object[] params = {argString};
                    String usageError = MessageFormat.format(
                            expectedMessage, params);
                    stringsFound = cli.findStringsInError(
                            usageError, ";" + newline);
                }
            }
            cli.resetArgList();
            if (!entityid.equals("")) {
                if (expectedExitCode.equals(
                        new Integer(SUCCESS_STATUS).toString())) {
                    log(logLevel, "testCreateMT",
                            "Output Messages Found: " + stringsFound);
                    assert (commandStatus ==
                            new Integer(expectedExitCode).intValue()) &&
                            stringsFound;
                } else {
                    log(logLevel, "testCreateMT",
                            "Error Messages Found: " + stringsFound);
                    assert (commandStatus ==
                            new Integer(expectedExitCode).intValue()) &&
                            stringsFound;
                }
            } else {
                if (expectedExitCode.equals(
                        new Integer(INVALID_OPTION_STATUS).toString())) {
                    log(logLevel, "testCreateMT",
                            "Expected Exit Code Found: " + expectedExitCode);
                } else {
                    log(logLevel, "testCreateMT",
                            "Expected Exit Code Not Found: " +
                            expectedExitCode);
                    assert false;
                }
            }
            exiting("testCreateMT");
        } catch (Exception e) {
            log(Level.SEVERE, "testCreateMT", e.getMessage(), null);
            e.printStackTrace();
            cleanup();
            throw e;
        }
    }

    /**
     * This method is used to execute tests of "ssoadm import-entity"
     * using input data from the MetadataTests.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
    "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
            ignoreMissingDependencies = true,
            dependsOnMethods = {"testCreateMT"})
    public void testImportEntity()
    throws Exception {
        entering("testImportEntity", null);
        boolean stringsFound = false;
        int commandStatus = 1000;

        try {
            expectedMessage = (String) rb.getString(locTestName +
                    "-importentity-message-to-find");
            cotname = (String) rb.getString(locTestName + "-cot-name");
            expectedExitCode = (String) rb.getString(locTestName +
                    "-importentity-expected-exit-code");
            cotrealm = (String) rb.getString(locTestName + "-cot-realm");
            metadatafile = (String) rb.getString(locTestName + "-metadatafile");
            extendeddatafile = (String) rb.getString(locTestName +
                    "-extendeddatafile");            
            spec = (String) rb.getString(locTestName + "-spec");
            description = (String) rb.getString(locTestName +
                    "-importentity-description");
            
            log(logLevel, "testImportEntity", "description: " + description);
            log(logLevel, "testImportEntity", "use-debug-option: " +
                    useDebugOption);
            log(logLevel, "testImportEntity", "use-verbose-option: " +
                    useVerboseOption);
            log(logLevel, "testImportEntity", "use-long-options: " +
                    useLongOptions);
            log(logLevel, "testImportEntity", "message-to-find: " +
                    expectedMessage);
            log(logLevel, "testImportEntity", "expected-exit-code: " +
                    expectedExitCode);
            log(logLevel, "testImportEntity", "-cot-realm: " + cotrealm);
            log(logLevel, "testImportEntity", "-cot-name: " + cotname);

            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);
            Reporter.log("COTRealmToCreate: " + cotrealm);
            Reporter.log("COTNameToCreate: " + cotname);

            cli = new FederationManagerCLI(useDebugOption,
                    useVerboseOption, useLongOptions);

            commandStatus = cli.importEntity(cotrealm, metadatafile,
                    extendeddatafile, cotname, spec);
            
            cli.logCommand("testImportEntity");
            
            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                stringsFound = cli.findStringsInOutput(expectedMessage, ";");
            } else {
                if (!expectedExitCode.equals(new Integer(
                        INVALID_OPTION_STATUS).toString())) {
                    stringsFound = cli.findStringsInError(expectedMessage, ";");
                } else {
                    String argString = cli.getAllArgs().replaceFirst(
                            cli.getCliPath(), "ssoadm ");
                    Object[] params = {argString};
                    String usageError = MessageFormat.format(
                            expectedMessage, params);
                    stringsFound = cli.findStringsInError(usageError, ";" +
                            newline);
                }
            }
            cli.resetArgList();
            if (!cotrealm.equals("")) {
                if (expectedExitCode.equals(
                        new Integer(SUCCESS_STATUS).toString())) {
                    log(logLevel, "testImportEntity",
                            "Output Messages Found: " + stringsFound);
                    assert (commandStatus ==
                            new Integer(expectedExitCode).intValue()) &&
                            stringsFound;
                } else {
                    log(logLevel, "testImportEntity", "Error Messages Found: " +
                            stringsFound);
                    assert (commandStatus == new Integer(
                            expectedExitCode).intValue()) && stringsFound;
                }
            } else {
                if (expectedExitCode.equals(
                        new Integer(INVALID_OPTION_STATUS).toString())) {
                    log(logLevel, "testImportEntity",
                            "Expected Exit Code Found: " + expectedExitCode);
                } else {
                    log(logLevel, "testImportEntity",
                            "Expected Exit Code Not Found: " +
                            expectedExitCode);
                    assert false;
                }
            }
            exiting("testImportEntity");
        } catch (Exception e) {
            log(Level.SEVERE, "testImportEntity", e.getMessage(), null);
            e.printStackTrace();
            cleanup();
            throw e;
        }
    }

    /**
     * This method is used to execute tests of "ssoadm list-entities"
     * using input data from the MetadataTests.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
    "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
            ignoreMissingDependencies = true,
            dependsOnMethods = {"testImportEntity"})
    public void testListEntities()
    throws Exception {
        entering("testListEntities", null);
        boolean stringsFound = false;
        int commandStatus = 1000;

        try {
            entityid = (String) rb.getString(locTestName + "-entityid");
            expectedMessage = (String) rb.getString(locTestName +
                    "-listentities-message-to-find");
            if (!entityid.equals("")) {
                expectedMessage = "  " + expectedMessage;
            }            
            expectedExitCode = (String) rb.getString(locTestName +
                    "-listentities-expected-exit-code");
            cotrealm = (String) rb.getString(locTestName + "-cot-realm");
            spec = (String) rb.getString(locTestName + "-spec");
            description = (String) rb.getString(locTestName +
                    "-listentities-description");

            log(logLevel, "testListEntities", "description: " + description);
            log(logLevel, "testListEntities", "use-debug-option: " +
                    useDebugOption);
            log(logLevel, "testListEntities", "use-verbose-option: " +
                    useVerboseOption);
            log(logLevel, "testListEntities", "use-long-options: " +
                    useLongOptions);
            log(logLevel, "testListEntities", "message-to-find: " +
                    expectedMessage);
            log(logLevel, "testListEntities", "expected-exit-code: " +
                    expectedExitCode);            
            log(logLevel, "testListEntities", "-cot-realm: " + cotrealm);
            log(logLevel, "testListEntities", "-spec: " + spec);

            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);            
            Reporter.log("COTRealmToCreate: " + cotrealm);
            Reporter.log("Specification: " + spec);

            cli = new FederationManagerCLI(useDebugOption,
                    useVerboseOption, useLongOptions);

            commandStatus = cli.listEntities(cotrealm, spec);

            cli.logCommand("testListEntities");

            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                stringsFound = cli.findStringsInOutput(expectedMessage, ";");
            } else {
                if (!expectedExitCode.equals(new Integer(
                        INVALID_OPTION_STATUS).toString())) {
                    stringsFound = cli.findStringsInError(expectedMessage, ";");
                } else {
                    String argString = cli.getAllArgs().replaceFirst(
                            cli.getCliPath(), "ssoadm ");
                    Object[] params = {argString};
                    String usageError = MessageFormat.format(
                            expectedMessage, params);
                    stringsFound = cli.findStringsInError(usageError, ";" +
                            newline);
                }
            }
            cli.resetArgList();
            if (!cotrealm.equals("")) {
                if (expectedExitCode.equals(
                        new Integer(SUCCESS_STATUS).toString())) {
                    log(logLevel, "testListEntities",
                            "Output Messages Found: " + stringsFound);
                    assert (commandStatus ==
                            new Integer(expectedExitCode).intValue()) &&
                            stringsFound;
                } else {
                    log(logLevel, "testListEntities", "Error Messages Found: " +
                            stringsFound);
                    assert (commandStatus == new Integer(
                            expectedExitCode).intValue()) && stringsFound;
                }
            } else {
                if (expectedExitCode.equals(
                        new Integer(INVALID_OPTION_STATUS).toString())) {
                    log(logLevel, "testListEntities",
                            "Expected Exit Code Found: " + expectedExitCode);
                } else {
                    log(logLevel, "testListEntities",
                            "Expected Exit Code Not Found: " +
                            expectedExitCode);
                    assert false;
                }
            }
            exiting("testListEntities");
        } catch (Exception e) {
            log(Level.SEVERE, "testListEntities", e.getMessage(), null);
            e.printStackTrace();
            cleanup();
            throw e;
        }
    }

    /**
     * This method is used to execute tests of "ssoadm export-entity"
     * using input data from the MetadataTests.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
    "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
            ignoreMissingDependencies = true,
            dependsOnMethods = {"testImportEntity"})
    public void testExportEntity()
    throws Exception {
        entering("testExportEntity", null);
        boolean stringsFound = false;
        int commandStatus = 1000;        

        try {
            expectedMessage = (String) rb.getString(locTestName +
                    "-exportentity-message-to-find");            
            expectedExitCode = (String) rb.getString(locTestName +
                    "-exportentity-expected-exit-code");
            entityid = (String) rb.getString(locTestName + "-entityid");
            metadatafile = (String) rb.getString(locTestName + "-metadatafile");
            extendeddatafile = (String) rb.getString(locTestName +
                    "-extendeddatafile");
            sign = (String) rb.getString(locTestName + "-exportentity-sign");
            cotrealm = (String) rb.getString(locTestName + "-cot-realm");
            spec = (String) rb.getString(locTestName + "-spec");
            description = (String) rb.getString(locTestName +
                    "-exportentity-description");

            log(logLevel, "testExportEntity", "description: " + description);
            log(logLevel, "testExportEntity", "use-debug-option: " +
                    useDebugOption);
            log(logLevel, "testExportEntity", "use-verbose-option: " +
                    useVerboseOption);
            log(logLevel, "testExportEntity", "use-long-options: " +
                    useLongOptions);
            log(logLevel, "testExportEntity", "message-to-find: " +
                    expectedMessage);
            log(logLevel, "testExportEntity", "expected-exit-code: " +
                    expectedExitCode);
            log(logLevel, "testExportEntity", "-cot-realm: " + cotrealm);
            log(logLevel, "testExportEntity", "-spec: " + spec);
            log(logLevel, "testExportEntity", "-sign: " + sign);
            log(logLevel, "testExportEntity", "-entityid: " + entityid);

            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);
            Reporter.log("COTRealmToCreate: " + cotrealm);
            Reporter.log("Specification: " + spec);
            Reporter.log("Sign: " + sign);
            Reporter.log("EntityID: " + entityid);

            cli = new FederationManagerCLI(useDebugOption,
                    useVerboseOption, useLongOptions);

            // deleting if the metadata's already present
            file = new File(metadatafile);
            boolean metadataExist = file.exists();
            if (metadataExist)
                file.delete();
            file = new File(extendeddatafile);
            boolean extendeddataExist = file.exists();
            if (extendeddataExist)
                file.delete();
            
            commandStatus = cli.exportEntity(entityid, cotrealm, sign,
                    metadatafile, extendeddatafile, spec);

            // checking if the metadata's are created by export-entity command
            if (!entityid.equals("") && !cotrealm.equals("")) {
                file = new File(metadatafile);
                metadataExist = file.exists();
                if (!metadataExist) {
                    log(logLevel, "testExportEntity", "Standard metadata is" +
                            " not created");
                    assert false;
                }
                file = new File(extendeddatafile);
                extendeddataExist = file.exists();
                if (!extendeddataExist) {
                    log(logLevel, "testExportEntity", "Extended metadata is" +
                            " not created");
                    assert false;
                }
            }

            cli.logCommand("testExportEntity");

            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                stringsFound = cli.findStringsInOutput(expectedMessage, ";");
            } else {
                if (!expectedExitCode.equals(new Integer(
                        INVALID_OPTION_STATUS).toString())) {
                    stringsFound = cli.findStringsInError(expectedMessage, ";");
                } else {
                    String argString = cli.getAllArgs().replaceFirst(
                            cli.getCliPath(), "ssoadm ");
                    Object[] params = {argString};
                    String usageError = MessageFormat.format(
                            expectedMessage, params);
                    stringsFound = cli.findStringsInError(usageError, ";" +
                            newline);
                }
            }
            cli.resetArgList();
            if (!entityid.equals("") && !cotrealm.equals("")) {
                if (expectedExitCode.equals(
                        new Integer(SUCCESS_STATUS).toString())) {
                    log(logLevel, "testExportEntity",
                            "Output Messages Found: " + stringsFound);
                    assert (commandStatus ==
                            new Integer(expectedExitCode).intValue()) &&
                            stringsFound;
                } else {
                    log(logLevel, "testExportEntity", "Error Messages Found: " +
                            stringsFound);
                    assert (commandStatus == new Integer(
                            expectedExitCode).intValue()) && stringsFound;
                }
            } else {
                if (expectedExitCode.equals(
                        new Integer(INVALID_OPTION_STATUS).toString())) {
                    log(logLevel, "testExportEntity",
                            "Expected Exit Code Found: " + expectedExitCode);
                } else {
                    log(logLevel, "testExportEntity",
                            "Expected Exit Code Not Found: " +
                            expectedExitCode);
                    assert false;
                }
            }
            exiting("testExportEntity");
        } catch (Exception e) {
            log(Level.SEVERE, "testExportEntity", e.getMessage(), null);
            e.printStackTrace();
            cleanup();
            throw e;
        }
    }

    /**
     * This method is used to execute tests of "ssoadm delete-entity"
     * using input data from the MetadataTests.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
    "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
            ignoreMissingDependencies = true,
            dependsOnMethods = {"testListEntities"})
    public void testDeleteEntity()
    throws Exception {
        entering("testDeleteEntity", null);
        boolean stringsFound = false;
        int commandStatus = 1000;

        try {
            expectedMessage = (String) rb.getString(locTestName +
                    "-deleteentity-message-to-find");            
            expectedExitCode = (String) rb.getString(locTestName +
                    "-deleteentity-expected-exit-code");
            entityid = (String) rb.getString(locTestName + "-entityid");
            cotrealm = (String) rb.getString(locTestName + "-cot-realm");
            extendedonly = (String) rb.getString(locTestName +
                    "-deleteentity-extendedonly");
            extendeddatafile = (String) rb.getString(locTestName +
                    "-extendeddatafile");
            spec = (String) rb.getString(locTestName + "-spec");
            description = (String) rb.getString(locTestName +
                    "-deleteentity-description");

            log(logLevel, "testDeleteEntity", "description: " + description);
            log(logLevel, "testDeleteEntity", "use-debug-option: " +
                    useDebugOption);
            log(logLevel, "testDeleteEntity", "use-verbose-option: " +
                    useVerboseOption);
            log(logLevel, "testDeleteEntity", "use-long-options: " +
                    useLongOptions);
            log(logLevel, "testDeleteEntity", "message-to-find: " +
                    expectedMessage);
            log(logLevel, "testDeleteEntity", "expected-exit-code: " +
                    expectedExitCode);
            log(logLevel, "testDeleteEntity", "-entityid: " + entityid);
            log(logLevel, "testDeleteEntity", "-cot-realm: " + cotrealm);
            log(logLevel, "testDeleteEntity", "-extended-data-file: " +
                    extendeddatafile);
            log(logLevel, "testDeleteEntity", "-extended-only: " +
                    extendedonly);
            log(logLevel, "testDeleteEntity", "-spec: " + spec);

            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);
            Reporter.log("EntityID: " + entityid);
            Reporter.log("COTRealmToCreate: " + cotrealm);
            Reporter.log("ExtendedOnly: " + extendedonly);
            Reporter.log("Specification: " + spec);

            cli = new FederationManagerCLI(useDebugOption,
                    useVerboseOption, useLongOptions);

            commandStatus = cli.deleteEntity(entityid, cotrealm,
                    extendeddatafile, extendedonly, spec);

            cli.logCommand("testDeleteEntity");

            if (expectedExitCode.equals(
                    new Integer(SUCCESS_STATUS).toString())) {
                stringsFound = cli.findStringsInOutput(expectedMessage, ";");
            } else {
                if (!expectedExitCode.equals(new Integer(
                        INVALID_OPTION_STATUS).toString())) {
                    stringsFound = cli.findStringsInError(expectedMessage, ";");
                } else {
                    String argString = cli.getAllArgs().replaceFirst(
                            cli.getCliPath(), "ssoadm ");
                    Object[] params = {argString};
                    String usageError = MessageFormat.format(
                            expectedMessage, params);
                    stringsFound = cli.findStringsInError(usageError, ";" +
                            newline);
                }
            }
            cli.resetArgList();
            if (!entityid.equals("") && !cotrealm.equals("")) {
                if (expectedExitCode.equals(
                        new Integer(SUCCESS_STATUS).toString())) {
                    log(logLevel, "testDeleteEntity",
                            "Output Messages Found: " + stringsFound);
                    assert (commandStatus ==
                            new Integer(expectedExitCode).intValue()) &&
                            stringsFound;
                } else {
                    log(logLevel, "testDeleteEntity", "Error Messages Found: " +
                            stringsFound);
                    assert (commandStatus == new Integer(
                            expectedExitCode).intValue()) && stringsFound;
                }
            } else {
                if (expectedExitCode.equals(
                        new Integer(INVALID_OPTION_STATUS).toString())) {
                    log(logLevel, "testDeleteEntity",
                            "Expected Exit Code Found: " + expectedExitCode);                    
                } else {
                    log(logLevel, "testDeleteEntity",
                            "Expected Exit Code Not Found: " +
                            expectedExitCode);
                    assert false;
                }
            }
            exiting("testDeleteEntity");
        } catch (Exception e) {
            log(Level.SEVERE, "testDeleteEntity", e.getMessage(), null);
            e.printStackTrace();
            cleanup();
            throw e;
        }
    }
    
    /**
     * This method cleans up the cots, realms and metadata's that were created
     * in the setup and in the tests.
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
    "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup()
    throws Exception {
        int cleanupExitStatus = -1;
        FederationManagerCLI idCli = null;
        FederationManagerCLI cleanupCli = null;        
        entering("cleanup", null);
        try {
            metadatafile = (String) rb.getString(locTestName + "-metadatafile");
            extendeddatafile = (String) rb.getString(locTestName +
                    "-extendeddatafile");

            // deleting the metadata's from the system
            file = new File(metadatafile);
            boolean metadataExist = file.exists();
            if (metadataExist)
                file.delete();
            file = new File(extendeddatafile);
            boolean extendeddataExist = file.exists();
            if (extendeddataExist)
                file.delete();

            // deleting the setup cots
            Iterator itercots = hsetcots.iterator();
            while (itercots.hasNext()) {
                String cotName = "";
                String cotRealm = "";
                String st = (String) itercots.next();
                int index = st.indexOf(",");
                if (index != -1) {
                    cotName = st.substring(0, index).trim();
                    cotRealm = st.substring(index + 1, st.length()).trim();
                }
                log(logLevel, "cleanup", "COTToDelete: "  + cotName);
                log(logLevel, "cleanup", "COTRealmToDelete: "  + cotRealm);
                Reporter.log("COTToDelete: " + cotName);
                Reporter.log("COTRealmToDelete: " + cotRealm);
                idCli = new FederationManagerCLI(useDebugOption,
                                    useVerboseOption, useLongOptions);
                idCli.deleteCot(cotName, cotRealm);
                idCli.logCommand("cleanup");
                idCli.resetArgList();
            }

            // deleting the setup realms
            if (!setupRealms.equals("")) {
                String[] realms = setupRealms.split(";");
                for (int i = realms.length-1; i >= 0; i--) {
                    log(Level.FINEST, "cleanup", "setupRealmToDelete: " +
                        realms[i]);
                    Reporter.log("SetupRealmToDelete: " + realms[i]);
                    cleanupExitStatus = cli.deleteRealm(realms[i], true);
                    cli.logCommand("cleanup");
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
        }finally {
            if (idCli != null) {
                idCli.resetArgList();
            }
            if (cleanupCli != null) {
                cleanupCli.resetArgList();
            }
            if (cli != null) {
                cli.resetArgList();
            }
        }
    }
}



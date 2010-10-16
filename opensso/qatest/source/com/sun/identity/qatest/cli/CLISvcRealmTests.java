package com.sun.identity.qatest.cli;

import com.gargoylesoftware.htmlunit.WebClient;
import com.sun.identity.qatest.common.cli.CLIExitCodes;
import com.sun.identity.qatest.common.cli.FederationManagerCLI;
import com.sun.identity.qatest.common.TestCommon;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.testng.Reporter;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;


/**
 * <code>CLISvcRealmTests</code> executes tests involving add-svc-realm, 
 * get-realm-svc-attrs, list-realm-assignable-svcs, show-realm-svcs and 
 * remove-svc-realm sub-commands of ssoadm.  This class allows the user to 
 * execute an ssoadm command by specifying a subcommand and a list of 
 * arguments. Properties file named CLISvcRealmTests.properties contain input 
 * values which are read by this class.
 *
 * This class automates the following test cases:
 * CLI_svc_realm_test01, CLI_svc_realm_test02, CLI_svc_realm_test03, 
 * CLI_svc_realm_test04, CLI_svc_realm_test05, CLI_svc_realm_test06, 
 * CLI_svc_realm_test07, CLI_svc_realm_test08, CLI_svc_realm_test09, 
 * CLI_svc_realm_test10, CLI_svc_realm_test11, CLI_svc_realm_test12,
 * CLI_svc_realm_test13, CLI_svc_realm_test14, CLI_svc_realm_test15,
 * CLI_svc_realm_test16, CLI_svc_realm_test17, CLI_svc_realm_test18,
 * CLI_svc_realm_test19
 */

public class CLISvcRealmTests extends TestCommon implements CLIExitCodes {
    
    private String locTestName;
    private ResourceBundle rb;
    private boolean useVerboseOption;
    private boolean useDebugOption;
    private boolean useLongOptions;
    private String expectedMessage;
    private String expectedErrorMessage;
    private String expectedExitCode;
    private String description;
    private String setupRealms;
    private String addServiceRealmAttrs;
    private String realmService;
    private String expOrEntSvcs;
    private boolean useDataFile;
    private boolean skipTheTest;
    private boolean useMandatoryOption;
    
    /** 
     * Creates a new instance of CLISvcRealmTests
     */
    public CLISvcRealmTests() {
        super("CLISvcRealmTests");
    }
    
    /**
     * This method is intended to provide initial setup.
     * Creates a <code>FederationManagerCLI</code> object. 
     * testName - name of the test executed.
     */
    @Parameters({"testName"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String testName) 
    throws Exception {
        Object[] params = {testName};
        skipTheTest = false;
        entering("setup", params);
        try {
            locTestName = testName;
            rb = ResourceBundle.getBundle("cli" + fileseparator + 
                    "CLISvcRealmTests");
            description = (String) rb.getString(locTestName + "-description");
            expOrEntSvcs = (String) rb.getString("enterprise-or-express-svcs");
            setupRealms = ((String) rb.getString(locTestName + 
                    "-setup-realms"));
            realmService = (String) rb.getString(locTestName + 
                    "-realm-svc-name");
            useVerboseOption = ((String)rb.getString(locTestName + 
                    "-use-verbose-option")).equals("true");
            useDebugOption = ((String)rb.getString(locTestName + 
                    "-use-debug-option")).equals("true");
            useLongOptions = ((String)rb.getString(locTestName + 
                    "-use-long-options")).equals("true");
            log(Level.FINEST, "setup", "testname: " + locTestName);
            log(Level.FINEST, "setup", "description: " + description);
            log(Level.FINEST, "setup", "express-or-enterprise-svcs: " + 
                    expOrEntSvcs);
            log(Level.FINEST, "setup", "setup-realms: " + setupRealms);
            log(Level.FINEST, "setup", "realm-svc: " + realmService);
            log(Level.FINEST, "setup", "use-verbose-option: " + 
                    useVerboseOption);
            log(Level.FINEST, "setup", "use-debug-option: " + 
                    useDebugOption);
            log(Level.FINEST, "setup", "use-long-options: " + 
                    useLongOptions);
            Reporter.log("TestName: " + locTestName);
            Reporter.log("Description: " + description);
            Reporter.log("ExpressOrEnterpriseSvcs: " + expOrEntSvcs);
            Reporter.log("CreateRealm: " + setupRealms);
            Reporter.log("CreateRealmService: " + realmService);
            Reporter.log("UseDebugOption: " + useDebugOption);
            Reporter.log("UseVerboseOption: " + useVerboseOption);
            Reporter.log("UseLongOptions: " + useLongOptions);
            if (realmService != null && !realmService.equals("") && 
                    expOrEntSvcs.contains(realmService) && isNightlyBuild()) {
                skipTheTest = true;
                throw new SkipException("Skipping setup for " + locTestName 
                        + " svc realm cli test on OpenSSO nightly build.");
            } else {
                FederationManagerCLI setupRealmsCLI = new FederationManagerCLI(
                        useDebugOption, useVerboseOption, useLongOptions);
                if (setupRealms != null && !setupRealms.equals("")) {
                    if (!setupRealmsCLI.createRealms(setupRealms)) {
                        log(Level.SEVERE, "setup", 
                                "All the realms failed to be created.");
                        assert false;
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
     * This method is used to execute tests with the subcommand "add-svc-realm"
     * using arguments from the CLISvcRealmTests.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testaddServiceRealmCommand() 
    throws Exception {
        entering("testaddServiceRealmCommand", null);
        boolean stringsFound = false;
        boolean errorFound = false;
        int exitStatus = -1;
        
        try {
            addServiceRealmAttrs = (String) rb.getString(locTestName + 
                    "-add-svc-realm-attrs");
            useDataFile = ((String) rb.getString(locTestName +
                    "-add-svc-realm-use-datafile")).equals("true");
            expectedMessage = (String) rb.getString(locTestName + 
                    "-add-svc-realm-expected-message");
            expectedErrorMessage = (String) rb.getString(locTestName + 
                    "-add-svc-realm-error-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-add-svc-realm-expected-exit-code");
 
            log(Level.FINEST, "testaddServiceRealmCommand", 
                    "add-svc-realm-attrs: " + addServiceRealmAttrs);
            log(Level.FINEST, "testaddServiceRealmCommand", 
                    "use-datafile-option: " + useDataFile);
            log(Level.FINEST, "testaddServiceRealmCommand", 
                    "add-svc-realm-expected-message: " + expectedMessage);
            log(Level.FINEST, "testaddServiceRealmCommand", 
                    "add-svc-realm-error-message-to-find: " + 
                    expectedErrorMessage);
            log(Level.FINEST, "testaddServiceRealmCommand", 
                    "add-svc-realm-expected-exit-code: " + 
                    expectedExitCode);
            Reporter.log("TestName: " + locTestName);
            Reporter.log("AddRealmServiceAttrs: " + addServiceRealmAttrs);
            Reporter.log("UseDatafileOption: " + useDataFile);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedErrorMessage: " + expectedErrorMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);

            FederationManagerCLI addSvcRealmCLI = new FederationManagerCLI(
                    useDebugOption, useVerboseOption, useLongOptions);
            exitStatus = addSvcRealmCLI.addSvcRealm(setupRealms, realmService, 
                    addServiceRealmAttrs, useDataFile);
            addSvcRealmCLI.logCommand("testaddServiceRealmCommand");
            log(Level.FINEST, "testaddServiceRealmCommand", "Exit status: " + 
                    exitStatus);
            
            stringsFound = addSvcRealmCLI.findStringsInOutput(
                    expectedMessage, ";");
            log(Level.FINEST, "testaddServiceRealmCommand", 
                    "Output Messages Found: " + stringsFound);
            if (expectedExitCode.equals(new Integer(
                    SUCCESS_STATUS).toString())) {
                assert (exitStatus == new Integer(expectedExitCode).intValue()) 
                        && stringsFound;
            } else {
                String argString = addSvcRealmCLI.getAllArgs().replaceFirst(
                        addSvcRealmCLI.getCliPath(), "ssoadm ");
                Object[] params = {argString};
                String usageError = MessageFormat.format(expectedErrorMessage, 
                        params);
                errorFound = addSvcRealmCLI.findStringsInError(usageError, ";");                
                log(Level.FINEST, "testaddServiceRealmCommand", 
                        "Error Messages Found: " + errorFound);
                assert (exitStatus == new Integer(expectedExitCode).intValue()) 
                        && stringsFound && errorFound;
            }
            addSvcRealmCLI.resetArgList();            
            exiting("testaddServiceRealmCommand");
        } catch (Exception e) {
            log(Level.SEVERE, "testaddServiceRealmCommand", e.getMessage(), 
                    null);
            cleanup();
            e.printStackTrace();
            throw e;
        } 
    }

    /**
     * This method is used to execute tests with subcommand 
     * "get-realm-svc-attrs" using arguments from the 
     * CLISvcRealmTests.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"}, 
      ignoreMissingDependencies = true,
      dependsOnMethods="testaddServiceRealmCommand")
    public void testGetRealmServiceAttrsCommand() 
    throws Exception {
        entering("testGetRealmServiceAttrsCommand", null);
        boolean stringsFound = false;
        boolean errorFound = false;
        int exitStatus = -1;
        try {
            expectedMessage = (String) rb.getString(locTestName + 
                    "-get-realm-svc-attrs-expected-message");
            expectedErrorMessage = (String) rb.getString(locTestName + 
                    "-get-realm-svc-attrs-error-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-get-realm-svc-attrs-expected-exit-code");
            log(Level.FINEST, "testGetRealmServiceAttrsCommand", 
                    "get-realm-svc-attrs-expected-message: " + expectedMessage);
            log(Level.FINEST, "testGetRealmServiceAttrsCommand", 
                    "get-realm-svc-attrs-error-message-to-find: " + 
                    expectedErrorMessage);
            log(Level.FINEST, "testGetRealmServiceAttrsCommand", 
                    "update-expected-exit-code: " + expectedExitCode);
            Reporter.log("TestName: " + locTestName);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedErrorMessage: " + expectedErrorMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);

            FederationManagerCLI getRealmSvcAttrsCLI = 
                    new FederationManagerCLI(useDebugOption,
                        		useVerboseOption, useLongOptions);
            exitStatus = getRealmSvcAttrsCLI.getRealmSvcAttributes(setupRealms,
                    realmService);
            getRealmSvcAttrsCLI.logCommand("testGetRealmServiceAttrsCommand");
            stringsFound = getRealmSvcAttrsCLI.findStringsInOutput(
                    expectedMessage, ";");
            log(Level.FINEST, "testGetRealmServiceAttrsCommand", 
                    "Output Messages Found: " + stringsFound);
            if (expectedExitCode.equals(new Integer(
                    SUCCESS_STATUS).toString())) {
                assert (exitStatus == new Integer(expectedExitCode).intValue())
                        && stringsFound;
            } else {
                String argString = 
                        getRealmSvcAttrsCLI.getAllArgs().replaceFirst(
                        getRealmSvcAttrsCLI.getCliPath(), "ssoadm ");
                Object[] params = {argString};
                String usageError = MessageFormat.format(expectedErrorMessage, 
                        params);
                errorFound = getRealmSvcAttrsCLI.findStringsInError(usageError, 
                        ";");                
                assert (exitStatus == new Integer(expectedExitCode).intValue()) 
                        && stringsFound && errorFound;
            }   
            getRealmSvcAttrsCLI.resetArgList();            
            exiting("testGetRealmServiceAttrsCommand");
        } catch (Exception e) {
            log(Level.SEVERE, "testGetRealmServiceAttrsCommand", e.getMessage(), 
                    null);
            cleanup();
            e.printStackTrace();
            throw e;
        } 
    }

    /**
     * This method is used to execute tests with the subcommand 
     * "list-assignable-svcs" using arguments from the 
     * CLISvcRealmTests.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testListRealmAssignableServicesCommand() 
    throws Exception {
        entering("testListRealmAssignableServicesCommand", null);
        boolean stringsFound = false;
        boolean errorFound = false;
        int exitStatus = -1;
        
        try {
            expectedMessage = (String) rb.getString(locTestName + 
                    "-list-realm-assignable-svcs-expected-message");
            expectedErrorMessage = (String) rb.getString(locTestName + 
                    "-list-realm-assignable-svcs-error-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-list-realm-assignable-svcs-expected-exit-code");
 
            log(Level.FINEST, "testListRealmAssignableServicesCommand", 
                    "list-realm-assignable-svcs-expected-message: " + 
                    expectedMessage);
            log(Level.FINEST, "testListRealmAssignableServicesCommand", 
                    "list-realm-assignable-svcs-error-message-to-find: " + 
                    expectedErrorMessage);
            log(Level.FINEST, "testListRealmAssignableServicesCommand", 
                    "list-realm-assignable-svcs-expected-exit-code: " + 
                    expectedExitCode);
            Reporter.log("TestName: " + locTestName);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedErrorMessage: " + expectedErrorMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);

            FederationManagerCLI listRealmAssignableSvcsCLI = 
                    new FederationManagerCLI(useDebugOption, useVerboseOption, 
                    useLongOptions);
            exitStatus = listRealmAssignableSvcsCLI.listRealmAssignableSvcs(
            		setupRealms);
            listRealmAssignableSvcsCLI.logCommand(
            		"testListRealmAssignableServicesCommand");
            log(Level.FINEST, "testListRealmAssignableServicesCommand", 
            		"Exit status: " + exitStatus);

            log(Level.FINEST, "testListRealmAssignableServicesCommand", 
                    "Output Messages Found: " + stringsFound);
            if (expectedExitCode.equals(new Integer(
                    SUCCESS_STATUS).toString())) {
                if (!isNightlyBuild()) {
                    expectedMessage += ";" + (String) rb.getString(
                            "enterprise-or-express-svcs");
                }
                stringsFound = listRealmAssignableSvcsCLI.findStringsInOutput(
                        expectedMessage, ";");                
                assert (exitStatus == new Integer(expectedExitCode).intValue()) 
                        && stringsFound;
            } else {
                String argString = 
                        listRealmAssignableSvcsCLI.getAllArgs().replaceFirst(
                        listRealmAssignableSvcsCLI.getCliPath(), "ssoadm ");
                Object[] params = {argString};
                String usageError = MessageFormat.format(expectedErrorMessage, 
                        params);
                errorFound = listRealmAssignableSvcsCLI.findStringsInError(
                        usageError, ";");
                stringsFound = listRealmAssignableSvcsCLI.findStringsInOutput(
                        expectedMessage, ";");                
                assert (exitStatus == new Integer(expectedExitCode).intValue()) 
                        && stringsFound && errorFound;
            }
            listRealmAssignableSvcsCLI.resetArgList();
            exiting("testListRealmAssignableServicesCommand");
        } catch (Exception e) {
            log(Level.SEVERE, "testListRealmAssignableServicesCommand",
                    e.getMessage(), null);
            cleanup();
            e.printStackTrace();
            throw e;
        } 
    }    

    /**
     * This method is used to execute tests with the subcommand "show-realm-svcs"
     * using arguments from the CLISvcRealmTests.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testShowRealmServicesCommand() 
    throws Exception {
        entering("testShowRealmServicesCommand", null);
        boolean stringsFound = false;
        boolean errorFound = false;
        int exitStatus = -1;
        
        try {
            useMandatoryOption = ((String) rb.getString(locTestName + 
                    "-show-realm-svc-mandatory-option")).equals("true");
            expectedMessage = (String) rb.getString(locTestName + 
                    "-show-realm-svcs-expected-message");
            expectedErrorMessage = (String) rb.getString(locTestName + 
                    "-show-realm-svcs-error-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-show-realm-svcs-expected-exit-code");
 
            log(Level.FINEST, "testShowRealmServicesCommand", 
                    "show-realm-svcs-expected-message: " + expectedMessage);
            log(Level.FINEST, "testShowRealmServicesCommand", 
                    "show-realm-svcs-error-message-to-find: " + 
                    expectedErrorMessage);
            log(Level.FINEST, "testShowRealmServicesCommand", 
                    "show-realm-svcs-expected-exit-code: " + 
                    expectedExitCode);
            Reporter.log("TestName: " + locTestName);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedErrorMessage: " + expectedErrorMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);

            FederationManagerCLI showRealmSvcsCLI = 
                    new FederationManagerCLI(useDebugOption, useVerboseOption, 
                    useLongOptions);
            exitStatus = showRealmSvcsCLI.showRealmSvcs(setupRealms, 
                    useMandatoryOption);
            showRealmSvcsCLI.logCommand("testShowRealmServicesCommand");
            log(Level.FINEST, "testShowRealmServicesCommand", "Exit status: " + 
                    exitStatus);
            
            stringsFound = showRealmSvcsCLI.findStringsInOutput(
                    expectedMessage, ";");
            log(Level.FINEST, "testShowRealmServicesCommand", "Output " +
                    "Messages Found: " + stringsFound);
            if (expectedExitCode.equals(new Integer(
                    SUCCESS_STATUS).toString())) {
                assert (exitStatus == new Integer(expectedExitCode).intValue()) 
                        && stringsFound;
            } else {
                String argString = showRealmSvcsCLI.getAllArgs().replaceFirst(
                        showRealmSvcsCLI.getCliPath(), "ssoadm ");
                Object[] params = {argString};
                String usageError = MessageFormat.format(expectedErrorMessage, 
                        params);
                errorFound = showRealmSvcsCLI.findStringsInError(usageError, 
                        ";");                
                assert (exitStatus == new Integer(expectedExitCode).intValue()) 
                        && stringsFound && errorFound;
            }
            showRealmSvcsCLI.resetArgList();
            exiting("testShowRealmServicesCommand");
        } catch (Exception e) {
            log(Level.SEVERE, "testShowRealmServicesCommand", e.getMessage(), 
                    null);
            cleanup();
            e.printStackTrace();
            throw e;
        } 
    }    

    /**
     * This method is used to execute tests with the subcommand and arguments
     * from the CLISvcRealmTests.properties file.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
      ignoreMissingDependencies = true,      
      dependsOnMethods="testaddServiceRealmCommand")
    public void testRemoveServiceRealmCommand() 
    throws Exception {
        entering("testRemoveServiceRealmCommand", null);
        boolean stringsFound = false;
        boolean errorFound = false;
        int exitStatus = -1;
        
        try {
            expectedMessage = (String) rb.getString(locTestName + 
                    "-remove-realm-svcs-expected-message");
            expectedErrorMessage = (String) rb.getString(locTestName + 
                    "-remove-realm-svcs-error-message-to-find");
            expectedExitCode = (String) rb.getString(locTestName + 
                    "-remove-realm-svcs-expected-exit-code");
 
            log(Level.FINEST, "testRemoveServiceRealmCommand", 
                    "remove-realm-svcs-expected-message: " + expectedMessage); 
            log(Level.FINEST, "testRemoveServiceRealmCommand", 
                    "remove-realm-svcs-error-message-to-find: " + 
                    expectedErrorMessage);
            log(Level.FINEST, "testRemoveServiceRealmCommand", 
                    "remove-realm-svcs-expected-exit-code: " + 
                    expectedExitCode);
            Reporter.log("TestName: " + locTestName);
            Reporter.log("ExpectedMessage: " + expectedMessage);
            Reporter.log("ExpectedErrorMessage: " + expectedErrorMessage);
            Reporter.log("ExpectedExitCode: " + expectedExitCode);

            FederationManagerCLI removeSvcRealmCLI = 
                    new FederationManagerCLI(useDebugOption, useVerboseOption, 
                    useLongOptions);
            exitStatus = removeSvcRealmCLI.removeSvcRealm(setupRealms, 
                    realmService);
            removeSvcRealmCLI.logCommand("testRemoveServiceRealmCommand");
            log(Level.FINEST, "testRemoveServiceRealmCommand", "Exit status: " + 
                    exitStatus);

            stringsFound = removeSvcRealmCLI.findStringsInOutput(
                    expectedMessage, ";");
            log(Level.FINEST, "testRemoveServiceRealmCommand", "Output " +
                    "Messages Found: " + stringsFound);
            if (expectedExitCode.equals(new Integer(
                    SUCCESS_STATUS).toString())) {
                assert (exitStatus == new Integer(expectedExitCode).intValue()) 
                        && stringsFound;
            } else {
                String argString = removeSvcRealmCLI.getAllArgs().replaceFirst(
                        removeSvcRealmCLI.getCliPath(), "ssoadm ");
                Object[] params = {argString};
                String usageError = MessageFormat.format(expectedErrorMessage, 
                        params);
                errorFound = removeSvcRealmCLI.findStringsInError(usageError, 
                        ";");
                assert (exitStatus == new Integer(expectedExitCode).intValue()) 
                        && stringsFound && errorFound;
            }
            removeSvcRealmCLI.resetArgList();
            exiting("testRemoveServiceRealmCommand");
        } catch (Exception e) {
            log(Level.SEVERE, "testRemoveServiceRealmCommand", e.getMessage(), 
                    null);
            cleanup();
            e.printStackTrace();
            throw e;
        } 
    }    

    
    /**
     * Cleanup cleans all the sites that are created in setup.
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup() 
    throws Exception {
        entering("cleanup", null);
        String[] realms = setupRealms.split(";");
        FederationManagerCLI cleanupCli = new FederationManagerCLI(
                useDebugOption, useVerboseOption, useLongOptions);
        if (!skipTheTest) {
            if (setupRealms != null && !setupRealms.equals("")) {
                for (int i=realms.length-1; i >= 0; i--) {             
                    log(Level.FINEST, "cleanup", "setupRealmToDelete: " + 
                            realms[i]);
                    Reporter.log("SetupRealmToDelete: " + realms[i]);
                    int exitStatus = cleanupCli.deleteRealm(realms[i], true);
                    cleanupCli.logCommand("cleanup");
                    cleanupCli.resetArgList();
                    if (exitStatus != SUCCESS_STATUS) {
                        log(Level.SEVERE, "cleanup", "Realm deletion returned" +
                                " the failed exit status " + exitStatus + ".");
                        assert false;
                    }
                    FederationManagerCLI listCli = new FederationManagerCLI(
                            useDebugOption, useVerboseOption, useLongOptions);
                    if (listCli.findRealms(realms[i])) {
                        log(Level.SEVERE, "cleanup", "Deleted realm " + 
                                realms[i] + " still exists.");
                        assert false;
                    }
                } 
            }
        }
        exiting("cleanup");
    }

    /**
     * This method checks if build used is a nightly build.
     * 
     * @return - "true" if build is nightly.
     * @throws java.lang.Exception
     */
     private boolean isNightlyBuild()
     throws Exception {
         boolean isNightly = true;
         WebClient webClient = new WebClient();
         String openssoVersion = getServerConfigValue(webClient, 
                 "OpenSSO Version");
         if (openssoVersion.toLowerCase().contains("express") || 
                 openssoVersion.toLowerCase().contains("enterprise")) {
             isNightly = false;
         }
         return isNightly;
     }
}

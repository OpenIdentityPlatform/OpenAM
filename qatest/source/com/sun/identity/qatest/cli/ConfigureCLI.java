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
 * $Id: ConfigureCLI.java,v 1.7 2009/02/13 15:36:57 cmwesley Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.cli;

import com.sun.identity.qatest.common.TestConstants;
import com.sun.identity.qatest.common.cli.CLIUtility;
import com.sun.identity.qatest.common.cli.JarUtility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.testng.Reporter;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

/**
 * This class verifies that the CLI can be executed on this host.
 */
public class ConfigureCLI extends CLIUtility {
    
    ResourceBundle rb_amconfig = ResourceBundle.getBundle(
                    TestConstants.TEST_PROPERTY_AMCONFIG);
    private static String osName = System.getProperty("os.name").toLowerCase();
    private boolean removeTools = false;
    private File cliDir;
    
    /** Creates a new instance of ConfigureCLI */
    public ConfigureCLI() {
        super(cliPath + System.getProperty("file.separator") + "setup");
    }
       
    /**
     * Checks if the local host name on which tests are executing
     * is same as the one mentioned in AMConfig.properties file. If
     * same, continue the execution else abort. 
     */
    @BeforeSuite(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
        "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void configureCLI()
    throws Exception {
        entering("configureCLI", null);
        try {
            replaceAuthTags("cli");
            String amconfigHost = 
                    rb_amconfig.getString(TestConstants.KEY_AMC_HOST);
            log(Level.FINEST, "configureCLI", "Value of " + 
                    TestConstants.KEY_AMC_HOST + ": " + amconfigHost);
            String[] fqdnElements = amconfigHost.split("\\.");
            String fqdnHostname = 
                    InetAddress.getLocalHost().getHostName();
            if (fqdnElements.length > 0) {
                log(Level.FINEST, "configureCLI", "AMConfig hostname: " + 
                        fqdnElements[0]);
                log(Level.FINEST, "configureCLI", "Hostname from getHostName: " 
                        + fqdnHostname);
                
                ResourceBundle rb_cli = ResourceBundle.getBundle("cli" +
                        fileseparator + "cliTest");
                String toolsPath = rb_cli.getString("cli-path");
                cliDir = new File(toolsPath);
                String cliAbsPath = cliDir.getAbsolutePath();
                File binDir = new File(new StringBuffer(cliAbsPath).
                        append(fileseparator).append(uri).append(fileseparator).
                        append("bin").toString());
                if (!binDir.exists()) {
                    if (cliDir.exists() && cliDir.isDirectory()) {
                        File zipFile = new File(new StringBuffer(cliAbsPath).
                                append(fileseparator).
                                append("ssoAdminTools.zip").toString());
                        if (zipFile.exists() && zipFile.isFile()) {
                            JarUtility jar = new JarUtility();
                            log(Level.FINE, "configureCLI",
                                    "Expanding the ssoAdminTools.zip file");
                            SimpleDateFormat sdf =
                                    new SimpleDateFormat("yyyyMMddHHmmss");
                            String dateString = sdf.format(new Date());
                            cliPath = new StringBuffer(getBaseDir()).
                                    append(fileseparator).append("ssoadm_").
                                    append(dateString).toString();
                            cliDir = new File(cliPath);
                            binDir = new File(new StringBuffer(cliPath).
                                    append(fileseparator).append(uri).
                                    append(fileseparator).append("bin").
                                    toString());
                            if (!cliDir.mkdir()) {
                                log(Level.SEVERE, "configureCLI",
                                        "Unable to create directory " +
                                        cliPath);
                                assert false;
                            }
                            int unzipStatus = jar.expandWar(zipFile, cliDir);
                            jar.logCommand("configureCLI");
                            removeTools = true;
                            if (unzipStatus != 0) {
                                log(Level.SEVERE, "configureCLI",
                                        "The expansion of the zip file failed");
                                assert false;
                            }
                        }

                        String setupPerms = "744";
                        if (osName.contains("windows")) {
                            setupPerms = "+r";
                        }
                        changePermissions(setupPerms, cliPath +
                                System.getProperty("file.separator") + "setup");
                        configureTools();
                        log(Level.FINE, "configureCLI", 
                                "Sleeping for 30 seconds to create utilities");
                        Thread.sleep(30000);
                        if (!binDir.exists()) {
                            log(Level.SEVERE, "configureCLI", 
                                    "The setup script failed to create " + 
                                    binDir.getAbsolutePath());
                            assert false;
                        }
                    } else {
                        log(Level.SEVERE, "configureCLI", "The directory " + 
                                cliAbsPath + " is not a directory.");
                        assert false;
                    }
                }
                File cliPassFile = new File(passwdFile);
                if (!cliPassFile.exists()) {
                    BufferedWriter out = 
                            new BufferedWriter(new FileWriter(cliPassFile));
                    out.write(adminPassword);
                    out.close();

                    String readPerms = "400";
                    if (osName.contains("windows")) {
                        readPerms = "+r";
                    }
                    changePermissions(readPerms, passwdFile);
                }
            } else {
                log(Level.SEVERE, "configureCLI", "ERROR: Unable to get host " +
                        "name from " + amconfigHost + ".");
                Reporter.log("ERROR: Unable to get host " +
                        "name from " + amconfigHost + ".");
                assert false;
            }
        } catch (Exception e) {
            log(Level.SEVERE, "configureCLI", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        }
        exiting("configureCLI");
    }

    /**
     * Add execute permission to a file
     * @param targetFile - The file to make executable
     * @throws java.lang.Exception
     */
    private void changePermissions(String perms, String targetFile)
    throws Exception {
        clearArguments(2);

        if (!osName.contains("windows")) {
            setWorkingDir(new File("/bin"));
            setArgument(0, "/bin/chmod");
            setArgument(1, perms);
        } else {
            setArgument(0, "attrib");
            setArgument(1, perms);
        }
        setArgument(2, targetFile);
        log(Level.FINE, "changePermissions",
                "Making the file " + targetFile + " executable");
        executeCommand(60);
        logCommand("changePermissions");
    }
    
    /**
     * Execute the setup script to configure the administration utilities.
     */
    private void configureTools() 
    throws Exception {
        clearArguments(2);
        setArgument(0,
                cliPath + System.getProperty("file.separator") + "setup");
        setArgument(1, "-p");
        setArgument(2, rb_amconfig.getString(TestConstants.KEY_ATT_CONFIG_DIR));
        setWorkingDir(new File(cliPath));
        executeCommand(60);
        logCommand("configureTools");
    }

    /**
     * Remove the configured directory after execution is complete
     */
    @AfterSuite(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
        "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanupTools()
    throws Exception {
        if (removeTools) {
            log(Level.FINE, "cleanupTools",
                    "Removing tools directory " + cliPath);
            if (!removeDir(new File(cliPath))) {
                log(Level.SEVERE, "cleanupTools",
                        "Unable to delete tools directory " + cliPath);
                assert false;
            }
        }
    }

    /**
     * Remove a directory and its contents if any exist.
     * dirToRemove - a File object containing the directory to be removed.
     * @return - a boolean value indiciating whether the directory removal
     * was successful or not.
     */
    private boolean removeDir(File dirToRemove) throws Exception {
        if (dirToRemove.exists()) {
            File[] dirFiles = dirToRemove.listFiles();
            for (File dirFile: dirFiles) {
                if (dirFile.isDirectory() &&
                        dirFile.getAbsolutePath().startsWith(cliPath)) {
                    removeDir(dirFile);
                } else {
                    dirFile.delete();
                }
            }
        }
        return(dirToRemove.delete());
    }

}

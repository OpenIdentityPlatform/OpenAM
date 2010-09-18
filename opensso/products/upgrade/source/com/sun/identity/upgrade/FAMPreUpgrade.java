/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
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
 * $Id: FAMPreUpgrade.java,v 1.7 2008/10/11 05:05:53 bina Exp $
 *
 */
package com.sun.identity.upgrade;

import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;


/**
 * This class contains methods to start the preupgrade process.
 * 
 */
public class FAMPreUpgrade {

    final static String CONFIG_DIR = "configDir";
    static BufferedReader inbr = null;
    static String dsHost = "";
    static String dsPort = "";
    static String dirMgrDN = "cn=Directory Manager";
    static String dirMgrPass;
    static String amAdminPass;
    static String amAdminUser;
    static String configDir;
    static String basedir = null;
    static String stagingDir = null;
    static String installDir = null;
    static String fmStagingDir = null;
    static String backupDir = null;
    static String logDir = null;
    static String amConfigDir = null;
    static String amConfigFileLocation = null;
    static String serverConfigLocation = null;
    static String famUpgradeConfigDir = null;
    boolean isAM = true;
    String instanceType = "AM"; //default value
    public static ResourceBundle bundle;

    String DEBUG_PROPERTY = "com.iplanet.services.debug.directory";
    String WIN_UPGRADE_FILE = "ssopre80upgrade.bat";
    String WIN_FAM_UPGRADE_FILE = "ssoupgrade.bat";
    String AMCONFIG_FILE = "AMConfig.properties";
    String SERVERCONFIG_FILE = "serverconfig.xml";
    static String RESOURCE_BUNDLE_NAME = "ssoUpgrade";
    final static int AUTH_SUCCESS =
            com.sun.identity.authentication.internal.AuthContext.AUTH_SUCCESS;
    final static String AMADMIN_USER_PROPERTY =
            "com.sun.identity.authentication.super.user";
    final static String DS_PORT_PROPERTY = "com.iplanet.am.directory.port";
    final static String DS_HOST_PROPERTY = "com.iplanet.am.directory.host";
    final static String LOG_FILE = "ssopre80upgrade.log";
    static BufferedWriter writer = null;
    Properties amConfigProperties = null;

    static {
        bundle = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME);
    }

    /**
     * Start of OpenSSO 8.0 preupgrade 
     */
    public static void main(String args[]) {
        try {

            FAMPreUpgrade famPreUpgrade = new FAMPreUpgrade();
            famPreUpgrade.getBaseDir();
            String logLoc = basedir + File.separator + LOG_FILE;
            writer = new BufferedWriter(new FileWriter(logLoc));
            log(bundle.getString("preupg-msg"));
            log(bundle.getString("upg-base-dir") + " : " + basedir);
            famPreUpgrade.initVariables();
            famPreUpgrade.copyConfigFiles();
            famPreUpgrade.backupLogs();
            famPreUpgrade.backupDebugs();
            log(bundle.getString("preupg-log-file") + ": " + logLoc, true);
            log(bundle.getString("preupg-done"), true);
            log(bundle.getString("preupg-info"),true);
            writer.close();
        } catch (Exception e) {
            log("Error : " + e.getMessage());
        }
        System.exit(0);
    }

    /**
     * Logs messages to preupgrade log file.
     * 
     * @param message the message to be logged
     * @param sysOut boolean value true if message should be logged to standard
     *        out.
     */
    static void log(String message, boolean sysOut) {
        System.out.println(message);
        log(message);
    }

    /**
     * Logs message to preupgrade log file
     * 
     * @param message the message to be logged
     */
    static void log(String message) {
        try {
            writer.write(message);
            writer.newLine();
        } catch (Exception e) {
            // do nothing , 
            // in this message will not be logged
        }
    }

    /**
     * Intializes required input parameters.
     */
    public void initVariables() {
        inbr = new BufferedReader(new InputStreamReader(System.in));
        // get pre OpenSSO install dir
        isAM = isAMInstance();
        if (isAM) {
            getInstallDir();
        } else {
            getFMStagingDir();
        }

        // load AMConfig.properties file
        loadAMConfigProperties();

        // set serverconfigxml path
        setServerConfigPath();
        // get the backup directory to copy logs/debugs
        getBackupDir();
        // directory where opensso.zip is unzipped
        getBaseDir();
        // the cofiguration directory
        getConfigDir();
        // the staging directory
        getStagingDir();
        getDSInfo();
        getDirManagerInfo();
        getAMAdminInfo();
        getLogDir();

        setFAMUpgradeConfigDir();
        copyConfigFiles();
        backupLogs();
        backupDebugs();

        String upgradeDir = basedir + File.separator + "upgrade";
        Properties p = new Properties();
        p.put("STAGING_DIR", stagingDir + File.separator + "WEB-INF");
        p.put("FAM_CONFIG_DIR", configDir);
        p.put("FAM_UPGRADE_DIR", upgradeDir);
        String file = new StringBuffer().append(basedir)
                .append(File.separator).append("upgrade")
                .append(File.separator).append("scripts")
                .append(File.separator).append(WIN_FAM_UPGRADE_FILE).toString();

        //replace parameters in the upgrade scripts
        replaceTag(file, p);
    }

    /**
     * Backups the log files to backup directory.
     */
    void backupLogs() {
        String classMethod = "FAMPreUpgrade:backupLogs : ";
        try {
            File logDirObj = new File(logDir);
            File backupLogDir = new File(backupDir + File.separator + "log");
            backupLogDir.mkdir();
            // copy all log files to backup dir
            String[] fileList = logDirObj.list();
            for (int i = 0; i < fileList.length; i++) {
                File source = new File(logDir + File.separator + fileList[i]);
                String name = source.getName();
                File backupLogsDir =
                        new File(backupLogDir + File.separator + name);
                copyFile(source, backupLogsDir);
            }
        } catch (Exception e) {
            log(classMethod + "Error backing up log files");
        }
    }

    /**
     * Back up the debug files to the backup directory.
     */
    void backupDebugs() {
        String classMethod = "FAMPreUpgrade:backupDebugs : ";
        try {
            File debugDir =
                    new File((String) amConfigProperties.get(DEBUG_PROPERTY));
            File backupDebugDir =
                    new File(backupDir + File.separator + "debug");
            backupDebugDir.mkdir();
            // copy all log files to backup dir
            String[] fileList = debugDir.list();
            for (int i = 0; i < fileList.length; i++) {
                File source = new File(debugDir + File.separator + fileList[i]);
                String name = source.getName();
                File backupDir =
                        new File(backupDebugDir + File.separator + name);
                copyFile(source, backupDir);
            }
        } catch (Exception e) {
            log(classMethod + "Error backing up debug files" + e.getMessage());
        }
    }

    /**
     * Copies existing pre OpenSSO version of AMConfig.properties and 
     * serverconfig.xml to the upgrade config directory.
     */
    void copyConfigFiles() {
        String classMethod = "FAMPreUpgrade:copyConfigFiles : ";
        File file1 = new File(amConfigFileLocation);
        File toFile = new File(famUpgradeConfigDir +
                File.separator + "AMConfig.properties.bak");
        try {
            copyFile(file1, toFile);
            file1 = new File(serverConfigLocation);
            toFile = new File(famUpgradeConfigDir +
                    File.separator + "serverConfig.xml.bak");
            copyFile(file1, toFile);
        } catch (Exception e) {
            log(classMethod + "Error copying config files");
        }
    }

    /**
     * Reads the backup directory from standard input.
     * This method creates the backup directory if it
     * does not exist.
     */
    void getBackupDir() {
        System.out.print(bundle.getString("preupg-info-backup-dir") + " :" );
        String temp = readInput();
        if (temp != null && temp.length() > 0) {
            backupDir = temp;
            File backup = new File(backupDir);
            if (!(backup.exists())) {
                backup.mkdirs();
            }
        }
    }

    /**
     * Gets the default OpenSSO configuration directory.
     */
    void getConfigDir() {
        configDir = System.getProperty("configDir");
    }

    /**
     * Gets the location of the OpenSSO install directory.
     */
    void getInstallDir() {
        System.out.print(bundle.getString("preupg-info-am-install-dir") + " :");
        String temp = readInput();
        if (temp != null && temp.length() > 0) {
            installDir = temp;
        }
    }

    /**
     * Gets the instance type AM / FM .
     * 
     * @return a boolean value true if AM instance else false.
     */
    boolean isAMInstance() {
        String classMethod = "FAMPreUpgrade:isAMInstance : ";
        instanceType = "AM";
        System.out.print(bundle.getString("preupg-info-instance-type") +
                " : [" + instanceType + "] : ");
        String temp = readInput();
        if (temp != null && temp.length() > 0) {
            instanceType = temp;
        }
        log(classMethod + instanceType + "instance.");
        return instanceType.equalsIgnoreCase("AM");
    }

    /**
     * Gets the location of the AM 7.0 staging directory.
     */
    void getFMStagingDir() {
        String classMethod = "FAMPreUpgrade:getFMStagingDir : ";
        System.out.print(bundle.getString("preupg-info-fm-staging-dir") + ": ");
        String temp = readInput();
        if (temp != null && temp.length() > 0) {
            fmStagingDir = temp;
        }
        log(classMethod + "FM Staging Directory : " + fmStagingDir);
    }

    /**
     * Gets the location of the upgrade base directory.
     */
    void getBaseDir() {
        String classMethod = "FAMPreUpgrade:getBaseDir : ";
        basedir = System.getProperty("basedir");
        log(classMethod + "FAM Base Dir : " + basedir);
    }

    /**
     * Gets the location of the OpenSSO Staging directory.
     */
    void getStagingDir() {
        String classMethod = "FAMPreUpgrade:getStagingDir: ";
        stagingDir = System.getProperty("stagingDir");
        log(classMethod + "FAM Staging Dir : " + stagingDir);
    }

    /**
     * Returns the <code>AuthContext</code>.
     *
     * @param bindUser the user distinguished name.
     * @param bindPwd the user password.
     * @return <code>AuthContext</code> object
     * @throws javax.security.auth.login.LoginException on error.
     */
    private static com.sun.identity.authentication.internal.AuthContext 
            getLDAPAuthContext(String bindUser, String bindPwd)
            throws Exception {
        com.sun.identity.authentication.internal.AuthPrincipal principal =
                new com.sun.identity.authentication.internal.AuthPrincipal(
                bindUser);
        com.sun.identity.authentication.internal.AuthContext authContext =
                new com.sun.identity.authentication.internal.AuthContext(
                principal, bindPwd.toCharArray());
        return authContext;
    }

    /**
     * Returns the ssoToken used for admin operations.
     * NOTE: this might be replaced later.
     *
     * @param bindUser the user distinguished name.
     * @param bindPwd the user password
     * @return the <code>SSOToken</code>
     */
    private static SSOToken ldapLoginInternal(
            String bindUser,
            String bindPwd) {

        String classMethod = "UpgradeUtils:ldapLoginInternal : ";
        SSOToken ssoToken = null;
        try {
            com.sun.identity.authentication.internal.AuthContext ac =
                    getLDAPAuthContext(bindUser, bindPwd);
            if (ac.getLoginStatus() == AUTH_SUCCESS) {
                ssoToken = ac.getSSOToken();
            } else {
                ssoToken = null;
            }
        } catch (Exception le) {
            log(classMethod + "Error creating SSOToken" + le.getMessage());

        }
        return ssoToken;
    }

    /**
     * Returns the SSOToken.
     *
     * @return Admin Token.
     */
    public static SSOToken getSSOToken() {
        SSOToken ssoToken = null;
        String classMethod = "FAMPreUpgrade:getSSOToken: ";
        if (ssoToken == null) {
            ssoToken = ldapLoginInternal(amAdminUser, amAdminPass);
        }
        if (ssoToken != null) {
            try {
                String principal = ssoToken.getProperty("Principal");
                log(classMethod + "Principal in SSOToken :" + principal);

            } catch (Exception e) {
                log(classMethod + "Error creating SSOToken " + e.getMessage());
            }
        }
        return ssoToken;
    }

    void getLogDir() {
        String classMethod = "FAMPreUpgrade:getLogDir";
        try {
            String serviceName = "iPlanetAMLoggingService";
            String attributeName = "iplanet-am-logging-location";
            ServiceSchemaManager sm =
                    new ServiceSchemaManager(serviceName, getSSOToken());
            ServiceSchema ss = sm.getSchema("Global");
            Map attributeDefaults = ss.getAttributeDefaults();
            String value = "";
            if (attributeDefaults.containsKey(attributeName)) {
                HashSet hashSet =
                        (HashSet) attributeDefaults.get(attributeName);
                value = (String) (hashSet.iterator().next());
            }
            logDir = value;
        } catch (Exception e) {
            log(classMethod + "Cannot retrieve log directory");
        }
    }

    /**
     * Gets the Directory Server hostname.
     */
    void getDSInfo() {
        String classMethod = "FAMPreUpgrade:getDSInfo : ";
        dsHost = (String) amConfigProperties.get(DS_HOST_PROPERTY);
        System.out.print(bundle.getString("preupg-info-directory-host") + "[");
        System.out.print(dsHost);
        System.out.print("] :");
        String temp = readInput();
        if (temp != null && temp.length() > 0) {
            dsHost = temp;
        }
        dsPort = (String) amConfigProperties.get(DS_PORT_PROPERTY);
        System.out.print(bundle.getString("preupg-info-directory-port") + "[");
        System.out.print(dsPort);
        System.out.print("] : ");
        temp = readInput();
        if (temp != null && temp.length() > 0) {
            dsPort = temp;
        }
        log(classMethod + "Directory Server Host " + dsHost);
        log(classMethod + "Directory Server Port : " + dsPort);
    }

    /**
     * Gets Directory Manager DN password
     */
    private void getDirManagerInfo() {
        String classMethod = "FAMPreUpgrade:getDirManagerInfo : ";
        System.out.print(bundle.getString("preupg-info-directory-manager-dn") 
            + " [");
        System.out.print(dirMgrDN);
        System.out.print("] : ");
        String temp = dirMgrDN;
        dirMgrDN = readInput();
        if (dirMgrDN != null && dirMgrDN.length() == 0) {
            dirMgrDN = temp;
        }
        try {
            char[] dirMgrPassChar =
                    getPassword(System.in, 
                        bundle.getString("preupg-info-directory-manager-pass") 
                       + ": ");
            dirMgrPass = String.valueOf(dirMgrPassChar);
        } catch (IOException ioe) {
            log(classMethod + "Error " + ioe.getMessage());
        }
        log(classMethod + "Directory Manager DN :" + dirMgrDN);
    }

    /**
     * Gets the amAdmin user and password.
     */
    private void getAMAdminInfo() {
        String classMethod = "FAMUpgrade:getAMAdminInfo :";
        amAdminUser = (String) amConfigProperties.get(AMADMIN_USER_PROPERTY);
        System.out.print(bundle.getString("upg-info-admin-user-dn") + "[");
        System.out.print(amAdminUser);
        System.out.print("] :");
        String temp = readInput();
        if (temp != null && temp.length() > 0) {
            amAdminUser = temp;
        }
        try {
            char[] amAdminPassChar =
                    getPassword(System.in, 
                      bundle.getString("upg-info-admin-user-dn") + " : ");
            amAdminPass = String.valueOf(amAdminPassChar);
        } catch (IOException ioe) {
            log(classMethod + "Error : " + ioe.getMessage());
        }
        log(classMethod + " amAdminUser : " + amAdminUser);
    }

    /**
     * Load the existing instance AMConfig.properties
     */
    private void loadAMConfigProperties() {
        amConfigDir = null;
        if (!isAM) {
            amConfigDir = new StringBuffer().append(fmStagingDir)
                .append(File.separator)
                .append("web-src").append(File.separator).append("WEB-INF")
                .append(File.separator).append("classes").toString();
        } else {
            amConfigDir = new StringBuffer().append(installDir)
                .append(File.separator).append("config").toString();
        }
        amConfigFileLocation = new StringBuffer().append(amConfigDir)
            .append(File.separator).append(AMCONFIG_FILE).toString();
        amConfigProperties = getProperties(amConfigFileLocation);
    }

    /**
     * Sets the path of the serverconfig.xml
     */
    private void setServerConfigPath() {
        serverConfigLocation = null;
        if (!isAM) {
            serverConfigLocation = new StringBuffer().append(fmStagingDir)
                    .append(File.separator).append("web-src")
                    .append(File.separator).append("WEB-INF")
                    .append(File.separator).append("config")
                    .append(File.separator)
                    .append(SERVERCONFIG_FILE).toString();
        } else {
            serverConfigLocation = new StringBuffer().append(installDir)
                    .append(File.separator).append("config")
                    .append(File.separator)
                    .append(SERVERCONFIG_FILE).toString();
        }
    }

    /**
     * Sets the FAM Upgrade Config Directory.
     */
    private void setFAMUpgradeConfigDir() {
        String classMethod = "FAMPreUpgrade:setFAMUpgradeConfigDir : ";
        famUpgradeConfigDir = new StringBuffer().append(basedir)
                .append(File.separator).append("upgrade")
                .append(File.separator).append("config").toString();
        log(classMethod + famUpgradeConfigDir);
    }

    /**
     * Reads the input from the user
     */
    private static String readInput() {
        String input = "";
        try {
            input = inbr.readLine();
        } catch (IOException ioe) {
            log("Error while reading input IOException :" +
                    ioe.getMessage());
        }
        return input;
    }//End of readInput method

    /**
     * Makes a copy of a file.
     */
    void copyFile(File in, File out) {
        String classMethod = "FAMPreUpgrade:copyFile : ";
        try {
            FileInputStream fis = new FileInputStream(in);
            FileOutputStream fos = new FileOutputStream(out);
            byte[] buf = new byte[1024];
            int i = 0;
            while ((i = fis.read(buf)) != -1) {
                fos.write(buf, 0, i);
            }
            fis.close();
            fos.close();

        } catch (Exception e) {
            log(classMethod + "Error copying File " + e.getMessage());
        }
    }

    /**
     * Returns the properties in a files as <code>Properties</code> object.
     * 
     * @param name of the properties file.
     */
    public static Properties getProperties(String file) {

        String classMethod = "FAMPreUpgrade:getProperties : ";
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(file));
        } catch (FileNotFoundException fe) {
            log(classMethod + "File Not found" + file + fe.getMessage());
        } catch (IOException ie) {
            log(classMethod + "Error reading file" + file);
        }
        return properties;
    }

    /**
     * Replaces tags in a file
     * 
     * @param fname name of the file
     * @param p the property tags.
     */
    static void replaceTag(String fname, Properties p) {
        String line;
        StringBuffer sb = new StringBuffer();
        try {
            FileInputStream fis = new FileInputStream(fname);
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(fis));
            while ((line = reader.readLine()) != null) {
                //line = line.replaceAll();
                Enumeration e = p.propertyNames();
                while (e.hasMoreElements()) {
                    String oldPattern = (String) e.nextElement();
                    String newPattern = (String) p.getProperty(oldPattern);
                    newPattern = newPattern.replaceAll("\\\\", "\\\\\\\\");
                    line = line.replaceAll(oldPattern, newPattern);
                }
                sb.append(line + System.getProperty("line.separator"));
            }
            reader.close();
            BufferedWriter out = new BufferedWriter(new FileWriter(fname));
            out.write(sb.toString());
            out.close();
        } catch (Exception e) {
            // do nothing
        }
    }

    /**
     * Returns the password .
     * 
     * @param in the <code>InputStream</code>
     * @param prompt the prompt message
     * @return the password as a character array.
     * @throws java.io.IOException if there is an error.
     */
    public final char[] getPassword(InputStream in,
            String prompt) throws IOException {
        MaskingThread maskingthread = new MaskingThread(prompt);
        Thread thread = new Thread(maskingthread);
        thread.start();

        char[] lineBuffer;
        char[] buf;
        int i;

        buf = lineBuffer = new char[128];
        int room = buf.length;
        int offset = 0;
        int c;

        loop:
        while (true) {
            switch (c = in.read()) {
                case -1:
                case '\n':
                    break loop;
                case '\r':
                    int c2 = in.read();
                    if ((c2 != '\n') && (c2 != -1)) {
                        if (!(in instanceof PushbackInputStream)) {
                            in = new PushbackInputStream(in);
                        }
                        ((PushbackInputStream) in).unread(c2);
                    } else {
                        break loop;
                    }
                default:
                    if (--room < 0) {
                        buf = new char[offset + 128];
                        room = buf.length - offset - 1;
                        System.arraycopy(lineBuffer, 0, buf, 0, offset);
                        Arrays.fill(lineBuffer, ' ');
                        lineBuffer = buf;
                    }
                    buf[offset++] = (char) c;
                    break;
            }
        }
        maskingthread.stopMasking();
        if (offset == 0) {
            return null;
        }
        char[] ret = new char[offset];
        System.arraycopy(buf, 0, ret, 0, offset);
        Arrays.fill(buf, ' ');
        return ret;
    }

    /**
     * This class attempts to erase characters echoed to the console.
     */
    class MaskingThread extends Thread {

        private volatile boolean stop;
        private char echochar = '*';

        /**
         *@param prompt The prompt displayed to the user
         */
        public MaskingThread(String prompt) {
            System.out.print(prompt);
        }

        /**
         * Begin masking until asked to stop.
         */
        public void run() {

            int priority = Thread.currentThread().getPriority();
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

            try {
                stop = true;
                while (stop) {
                    System.out.print("\010" + echochar);
                    try {
                        // attempt masking at this rate
                        Thread.currentThread().sleep(1);
                    } catch (InterruptedException iex) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            } finally { // restore the original priority

                Thread.currentThread().setPriority(priority);
            }
        }

        /**
         * Instruct the thread to stop masking.
         */
        public void stopMasking() {
            this.stop = false;
        }
    }
}

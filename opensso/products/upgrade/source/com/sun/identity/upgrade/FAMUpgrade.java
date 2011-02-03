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
 * $Id: FAMUpgrade.java,v 1.13 2009/09/28 20:03:22 goodearth Exp $
 *
 */
package com.sun.identity.upgrade;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.ldap.DSConfigMgr;
import com.sun.identity.setup.Bootstrap;
import com.sun.identity.setup.BootstrapCreator;
import com.sun.identity.shared.debug.Debug;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import com.sun.identity.shared.encode.Hash;

/**
 * This class contains methods to start the upgrade process.
 * 
 */
public class FAMUpgrade {

    final static String UPGRADE_CONFIG_FILE = "ssoUpgradeConfig.properties";
    final static String AMADMIN_USER_PROPERTY = 
        "com.sun.identity.authentication.super.user";
    final static String DS_PORT_PROPERTY = "com.iplanet.am.directory.port";
    final static String DS_HOST_PROPERTY = "com.iplanet.am.directory.host";
    final static String NEW_DIR = "s_10";
    final static String DEFAULT_VERSION = "10";
    final static String CONFIG_DIR = "configDir";
    final static String BOOTSTRAP_FILE = "bootstrap";
    final static String UNASSIGN_SERVICE_LDIF = "unAssignService.ldif";
    final static String PROVIDER_CONFIG_SVC = "iPlanetAMProviderConfigService";
    final static String AUTH_DOMAIN_SVC = 
        "iPlanetAMAuthenticationDomainConfigService";
    static BufferedReader inbr = null;
    static String dsHost = "";
    static String dsPort = "";
    static String dirMgrDN = "cn=Directory Manager";
    static String dirMgrPass;
    static String amAdminPass;
    static String amAdminUser;
    static String configDir;
    static boolean enableRealms = false;
    static boolean realmMode = false;
    static Debug debug = Debug.getInstance("ssoUpgrade");
    static String basedir = null;
    static String stagingDir = null;

    /**
     * Start of upgrade 
     * 
     *  TODO : 
     * 1. Localize the messages 
     * 2. Log the message
     * 3. Constants should be defined at the top.
     * 
     */
    public static void main(String args[]) {
        try {
            System.out.println(UpgradeUtils.bundle.getString("upg-welcome"));
            FAMUpgrade famUpgrade = new FAMUpgrade();
            famUpgrade.bootStrapNow();
            famUpgrade.initVariables();
            UpgradeUtils.setBindDN(amAdminUser);
            UpgradeUtils.setBindPass(amAdminPass);
            UpgradeUtils.setDSHost(dsHost);
            UpgradeUtils.setDirMgrDN(dirMgrDN);
            UpgradeUtils.setDSPort(new Integer(dsPort).intValue());
            UpgradeUtils.setdirPass(dirMgrPass);
            UpgradeUtils.setBaseDir(basedir);
            UpgradeUtils.setStagingDir(stagingDir);
            UpgradeUtils.setConfigDir(configDir);
            // replace tags
            // load the properties
            Properties properties =
                    UpgradeUtils.getProperties(basedir +
                    File.separator + "upgrade" + File.separator +
                    "config" + File.separator + UPGRADE_CONFIG_FILE);
            String instanceType = (String)properties.get("INSTANCE_TYPE");
            if ((instanceType != null && instanceType.equalsIgnoreCase("FM")) 
                                      || UpgradeUtils.isRealmMode()) {
                System.out.println(
                    UpgradeUtils.bundle.getString("upg-realm-mode"));
                realmMode = true;
            //invoke migrateToRealm
            } else {
                System.out.println(
                    UpgradeUtils.bundle.getString("upg-legacy-mode"));
                System.out.print(UpgradeUtils.bundle.getString(
                    "upg-info-enable-realm-mode"));
                String temp = readInput();
                if (temp != null && temp.length() > 0) {
                   if (temp.equalsIgnoreCase("y") || 
                       temp.equalsIgnoreCase("Y")) {
                       enableRealms = true;
                   }
                }
                if (debug.messageEnabled()) {
                    debug.message("isRealmEnabled: " + enableRealms);
                }
            }
            updateProperties(properties);
            UpgradeUtils.setProperties(properties);
            String dir = getDir();
            UpgradeUtils.replaceTags(new File(dir), properties);
            String ldifDir = getLdifDir();
            UpgradeUtils.replaceTags(new File(ldifDir), properties);
            copyNewXMLFiles();
            if (UpgradeUtils.isFMInstance()) {
                UpgradeUtils.createServicesForFM();
            }
            famUpgrade.startUpgrade();
            // Migrate to realms
            String ldifFile = new StringBuffer().append(getLdifDir())
                .append(File.separator)
                .append(UNASSIGN_SERVICE_LDIF).toString();
            UpgradeUtils.loadLdif(ldifFile);
            UpgradeUtils.removeService(PROVIDER_CONFIG_SVC,"1.1");
            UpgradeUtils.removeService(AUTH_DOMAIN_SVC);
            if (enableRealms) {
                // migrate to realm mode
                UpgradeUtils.doMigration70();
            }
            DSConfigMgr dsCfg = DSConfigMgr.getDSConfigMgr();
            SystemProperties.setServerInstanceName(
                UpgradeUtils.getServerInstance(null));
            String bootstrap =
                BootstrapCreator.getInstance().getBootStrapURL(dsCfg);
            if (debug.messageEnabled()) {
                debug.message("Bootstrap : " + bootstrap);
            }
            UpgradeUtils.writeToFile(configDir+File.separator+
                BOOTSTRAP_FILE,bootstrap);

            // backup AMConfig.properties and serverconfig.xml
            File amConfig = new File(configDir + File.separator 
                + "AMConfig.properties");
            File amConfigbak = new File(configDir + File.separator 
                + ".AMConfig.properties");
            amConfig.renameTo(amConfigbak);

            File sConfig = new File(configDir + File.separator 
                + "serverconfig.xml");
            File sConfigbak = new File(configDir + File.separator 
                + ".serverconfig.xml");
            sConfig.renameTo(sConfigbak);
        } catch (Exception e) {
            debug.error("Error in main: " + e);
        }
        System.exit(0);
    }

    /**
     * Returns the upgrade services path 
     */
    static String getDir() {
        // xml files under the upgrade directory
        return new StringBuffer().append(basedir)
                .append(File.separator)
                .append("upgrade")
                .append(File.separator)
                .append("services").toString();
    }

    static String getLdifDir() {
        // xml files under the upgrade directory
        return new StringBuffer().append(basedir)
                .append(File.separator)
                .append("upgrade")
                .append(File.separator)
                .append("config").toString();
    }
    /**
     * Loads the AMConfig.properties
     */
    void bootStrapNow() {
        try {
            if (configDir == null) {
                configDir = System.getProperty(CONFIG_DIR);
            }
            Bootstrap.load(configDir,true);
        } catch (Exception e) {
            System.err.println("Cannot bootstrap the system" + e.getMessage());
            System.exit(1);
        }
    }

   /**
    * Intializes required input parameters.
    */
    public void initVariables() {
        inbr = new BufferedReader(new InputStreamReader(System.in));
        // directory where opensso.zip is unzipped
        getBaseDir();
        // the cofiguration directory
        getConfigDir();
        // the staging directory
        getStagingDir();
        getDSInfo();
        getDirManagerInfo();
        getAMAdminInfo();
    }

    /**
     * Gets the default OpenSSO configuration directory.
     */
    void getConfigDir() {
        do {
            System.out.print(UpgradeUtils.bundle.getString(
                                "upg-info-config-dir") + " : ");
            String temp = readInput();
            if (temp != null && temp.length() > 0) {
                configDir = temp;
            }
        } while (!isValidDir(
                configDir + File.separator + "AMConfig.properties"));
    }

    /**
     * Gets the location of the base directory.
     */
    void getBaseDir() {
        do {
            System.out.print(UpgradeUtils.bundle.getString(
                          "upg-info-upgrade-base-dir") + " : ");
            String temp = readInput();
            if (temp != null && temp.length() > 0) {
                basedir = temp;
            }
        } while (!isValidDir(basedir + File.separator + "upgrade"));
    }

    boolean isValidDir(String dir) {
        boolean isValid = false;
        try {
            File f = new File(dir);
            if (f.exists()) {
                isValid = true;
            } else {
                System.out.println(
                    UpgradeUtils.bundle.getString("upg-error-invalid-dir") 
                    + " !!");
            }
        } catch (Exception e) {
            // do nothing
        }
        return isValid;
    }

    /**
     * Gets the location of the Staging directory.
     */
    void getStagingDir() {
        String openssoPath = new StringBuffer().append("WEB-INF")
                .append(File.separator).append("lib")
                .append(File.separator).append("amserver.jar").toString();
        do {
            System.out.print(
                UpgradeUtils.bundle.getString("upg-info-opensso-staging-dir") 
                                  + "  : ");
            String temp = readInput();
            if (temp != null && temp.length() > 0) {
                stagingDir = temp;
            }
        } while (!isValidDir(stagingDir + File.separator + openssoPath));
    }

    /**
     * Gets the Directory Server hostname.
     */
    void getDSInfo() {
        do {
            dsHost = SystemProperties.get(DS_HOST_PROPERTY);
            if (dsHost == null) {
                dsHost = "";
            }
            System.out.print(UpgradeUtils.bundle.getString(
                               "preupg-info-directory-host")+ "[");
            System.out.print(dsHost);
            System.out.print("] :");
            String temp = readInput();
            if (temp != null && temp.length() > 0) {
                dsHost = temp;
            }
            dsPort = SystemProperties.get(DS_PORT_PROPERTY);
            if (dsPort == null) {
                dsPort = "";
            }
            System.out.print(UpgradeUtils.bundle.getString(
                "preupg-info-directory-port") + "[");
            System.out.print(dsPort);
            System.out.print("] :");
            temp = readInput();
            if (temp != null && temp.length() > 0) {
                dsPort = temp;
            }
        } while (!UpgradeUtils.isValidServer(dsHost, dsPort));
    }
    
    /**
     * Gets Directory Manager DN password
     */
    private void getDirManagerInfo() {
        do {
            System.out.print(UpgradeUtils.bundle.getString(
                            "preupg-info-directory-manager-dn") + " [");
            System.out.print(dirMgrDN);
            System.out.print("] : ");
            String temp = dirMgrDN;
            dirMgrDN = readInput();
            if (dirMgrDN != null && dirMgrDN.length() == 0) {
                dirMgrDN = temp;
            }
            try {
                char[] dirMgrPassChar =
                        getPassword(System.in, UpgradeUtils.bundle.getString(
                              "preupg-info-directory-manager-pass") + " : ");
                dirMgrPass = String.valueOf(dirMgrPassChar);
            } catch (IOException ioe) {
                UpgradeUtils.debug.error("Error " + ioe.getMessage());
            }
        } while (!UpgradeUtils.isValidCredentials(
                dsHost,dsPort,dirMgrDN,dirMgrPass));
    }

    /**
     * Gets the amAdmin user and password.
     */
    private void getAMAdminInfo() {
        String classMethod = "FAMUpgrade:getAMAdminInfo :";
        amAdminUser = SystemProperties.get(AMADMIN_USER_PROPERTY);
        do {
            System.out.print(UpgradeUtils.bundle.getString(
                "upg-info-admin-user-dn") +  "[");
            System.out.print(amAdminUser);
            System.out.print("] :");
            String temp = readInput();
            if (temp != null && temp.length() > 0) {
                amAdminUser = temp;
            }
            try {
                char[] amAdminPassChar =getPassword(System.in,
                        "Enter OpenSSO Admin User Password : ");
                amAdminPass = String.valueOf(amAdminPassChar);
            } catch (IOException ioe) {
                debug.error(classMethod + "Error : ", ioe);
            }
        } while (!UpgradeUtils.isValidCredentials(
                dsHost, dsPort,amAdminUser,amAdminPass));
    }


    /** 
     * Upgrades the services schema for different services .
     */
    public void startUpgrade() {
        System.out.println(UpgradeUtils.bundle.getString("upg-start") + " : ");
        String servicesDir = basedir + File.separator 
                + "upgrade" + File.separator + "services";
        // get file list
        File fileList = new File(servicesDir);
        String[] list = fileList.list();
        List listArray = Arrays.asList(list);
        Collections.sort(listArray, String.CASE_INSENSITIVE_ORDER);
        Iterator aa = listArray.iterator();
        while (aa.hasNext()) {
            String value = (String) aa.next();
            System.out.println(value);
            String serviceName = null;
            int in = value.indexOf("_");
            if (in != -1) {
                serviceName = value.substring(in + 1);
            }
            // serviceName
            System.out.println("*********************************************");
            System.out.println(UpgradeUtils.bundle.getString(
                "upg-migrate-service-name") + " : " + serviceName);
            File fileL = new File(servicesDir + File.separator + value);
            String[] ll = fileL.list();
            List lArray = Arrays.asList(ll);
            Collections.sort(lArray, String.CASE_INSENSITIVE_ORDER);
            Iterator ab = lArray.iterator();

            int currentVersion = UpgradeUtils.getServiceRevision(serviceName);
            String currentRev = new Integer(currentVersion).toString();

            boolean newService = false;
            boolean isSuccess = false;
            List migrateList = new ArrayList();
            for (int k = 0; k < lArray.size(); k++) {
                if (currentVersion != -1) {
                    String dir = (String) lArray.get(k);
                    if (dir.startsWith(currentRev)) {
                        migrateList = lArray.subList(k, lArray.size());
                        break;
                    }
                } else {
                    migrateList.addAll(lArray);
                    if (lArray.size() == 1) {
                        newService = true;
                    }
                    migrateList.remove(NEW_DIR);
                    break;
                }
            }
            Collections.sort(migrateList);
            boolean isNew = false;
            if (currentVersion != -1) {
                System.out.println(UpgradeUtils.bundle.getString(
                    "upg-service-name") + " :"+ 
                    UpgradeUtils.bundle.getString("upg-rev-number") 
                   + ":"+ currentVersion);
            } else {
                System.out.println(
                    UpgradeUtils.bundle.getString("upg-new-service") +
                            " : " + serviceName );
                isNew = true;
            }

            Iterator fileIterator = migrateList.iterator();
            // iterate through the service dirs.
            String fromVer = "";
            String endVer = "";
            try {
                while (fileIterator.hasNext() || isNew) {
                    String dirName = null;
                    if (isNew) {
                        dirName = NEW_DIR;
                        endVer = DEFAULT_VERSION;
                        isNew = false;
                    } else {
                        isNew = false;
                        dirName = (String) fileIterator.next();
                        if (dirName.equals(NEW_DIR)) {
                               continue;
                        }
                        int index = dirName.indexOf("_");
                        if (index != -1) {
                            fromVer = dirName.substring(0, index);
                            endVer = dirName.substring(
                                    index + 1, dirName.length());
                        }
                    }
                    isSuccess = false;
                    String urlString = new StringBuffer().append("file:///")
                            .append(servicesDir)
                            .append(File.separator)
                            .append(value)
                            .append(File.separator)
                            .append(dirName)
                            .append(File.separator).toString();
                    URL url1 = new URL(urlString);
                    urlString = new StringBuffer()
                            .append("file:///")
                            .append(basedir)
                            .append(File.separator)
                            .append("upgrade")
                            .append(File.separator)
                            .append("lib")
                            .append(File.separator)
                            .append("upgrade.jar").toString();
                    URL url2 = new URL(urlString);
                    URL[] urls = {url1, url2};
                    URLClassLoader cLoader = new URLClassLoader(urls);
                    MigrateTasks mClass =
                            (MigrateTasks) 
                            cLoader.loadClass("Migrate").newInstance();
                    if (mClass.preMigrateTask() &&
                            mClass.migrateService() &&
                            mClass.postMigrateTask()) {
                        isSuccess = true;
                    }
                } // while rev dirs.
                if (isSuccess && !newService) {
                    UpgradeUtils.setServiceRevision(serviceName, endVer);
                }
            } catch (Exception e) {
                System.out.println(UpgradeUtils.bundle.getString("upg-error") 
                    + ":" + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Reads the input from the user
     */
    private static String readInput() {
        String input = "";
        try {
            input = inbr.readLine();
        } catch (IOException ioe) {
            System.out.print(UpgradeUtils.bundle.getString(
                     "upg-invalid-input") + " :" + ioe.getMessage());
        }
        return input;
    }//End of readInput method
    
    /**
     * Returns a list of files in a directory.
     * 
     * @param dirName the directory name
     * @param fileList the file list to be retrieved.
     */
    public static void getFiles(File dirName,
            LinkedList fileList) {
        File[] fromFiles = dirName.listFiles();
        for (int i = 0; i < fromFiles.length; i++) {
            fileList.addLast(fromFiles[i]);
            if (fromFiles[i].isDirectory()) {
                getFiles(fromFiles[i], fileList);
            }
        }
    }

    
    /**
     * Makes a copy of new xml files for tag swapping.
     */
    static void copyNewXMLFiles() {
        String classMethod = "FAMUgprade:copyNewXMLFiles";
        String upgradeNewXMLDir =
                basedir + File.separator + "upgrade" + File.separator + "xml";

        try {
            // create directory upgrade/xml
            File f = new File(upgradeNewXMLDir);
            f.mkdir();
            // copy all files from xml directory to upgrade/xml directory.
            File xmlF = new File(basedir + File.separator + "xml");
            String[] xmlfileList = xmlF.list();
            for (int i = 0; i < xmlfileList.length; i++) {
                File source = new File(basedir + File.separator +
                        "xml" + File.separator + xmlfileList[i]);
                String name = source.getName();
                File target =
                        new File(upgradeNewXMLDir + File.separator + name);
                copyFile(source, target);
            }
        } catch (Exception e) {
            debug.error(classMethod + "Error copying new xmls" ,e );
        }
    }

    /**
     * Makes a copy of a file.
     */
    static void copyFile(File in, File out) throws Exception {
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
            e.printStackTrace();
        }
    }

    /**
     * Properties to be tagswapped.
     */
    private static void updateProperties(Properties p) {
        p.put("@XML_COMMENT_START@","");
        p.put("@XML_COMMENT_END@","");
        p.put("@SM_CONFIG_ROOT_SUFFIX@",UpgradeUtils.getRootSuffix());
        p.put("ROOT_SUFFIX",UpgradeUtils.getRootSuffix());
        p.put("SM_CONFIG_BASEDN",UpgradeUtils.getRootSuffix());
        p.put("NORMALIZED_ORGBASE",UpgradeUtils.getNormalizedRootSuffix());
        p.put("NORMALIZED_RS",UpgradeUtils.getNormalizedRootSuffix());
        p.put("DIRECTORY_SERVER",dsHost);
        p.put("DIRECTORY_PORT",dsPort);
        p.put("People_NM_ORG_ROOT_SUFFIX",
                UpgradeUtils.getPeopleOrgRootSuffix());
        p.put("ORG_ROOT_SUFFIX",UpgradeUtils.getNormalizedRootSuffix());
        p.put("SSO_ROOT_SUFFIX",UpgradeUtils.getNormalizedRootSuffix());
        p.put("RSUFFIX_HAT",UpgradeUtils.getRootSuffix().replaceAll(",", "^"));
        p.put("SERVER_HOST",UpgradeUtils.getServerHost());
        p.put("RS_RDN",UpgradeUtils.getRsDN());
        p.put("HASHADMINPASSWD",Hash.hash(amAdminPass));
        p.put("@HASHLDAPUSERPASSWD@",
                Hash.hash((String)p.get("LDAP_USER_PASS")));
        if (realmMode) {
            p.put("@AM_COEXIST@","false");
            p.put("@AM_REALM@","true");
        } else {
            p.put("@AM_COEXIST@","true");
            p.put("@AM_REALM@","false");
        }
        p.put("@AMSDK_I18N_KEY@","a101");
        p.put("@USER_NAMING_ATTR@",(String) p.get("USER_NAMING_ATTR"));
        p.put("@ORG_NAMING_ATTR@",(String) p.get("ORG_NAMING_ATTR"));
        p.put("@ORG_OBJECT_CLASS@",(String) p.get("ORG_OBJECT_CLASS"));
        p.put("@USER_OBJECT_CLASS@",(String) p.get("USER_OBJECT_CLASS"));
        p.put("@AMLDAPUSERPASSWD@",(String) p.get("LDAP_USER_PASS"));
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

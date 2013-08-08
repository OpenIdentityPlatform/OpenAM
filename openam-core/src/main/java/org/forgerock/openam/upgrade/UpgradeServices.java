/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock, Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.openam.upgrade;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.util.AMEncryption;
import com.iplanet.services.util.ConfigurableKey;
import com.iplanet.services.util.JCEEncryption;
import com.iplanet.sso.SSOToken;
import com.sun.identity.password.plugins.PasswordGenerator;
import com.sun.identity.password.plugins.RandomPasswordGenerator;
import com.sun.identity.password.ui.model.PWResetException;
import com.sun.identity.setup.AMSetupServlet;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Hash;
import com.sun.identity.sm.ServiceManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import org.forgerock.openam.upgrade.steps.UpgradeStep;

/**
 * This is the primary upgrade class that determines the how the services need
 * to be upgraded and performs the upgrade.
 *
 * @author steve
 */
public class UpgradeServices {

    public static final String LF = "%LF%";
    private static ResourceBundle BUNDLE = ResourceBundle.getBundle("amUpgrade");
    private static final String CREATED_DATE = "%CREATED_DATE%";
    private static final String EXISTING_VERSION = "%EXISTING_VERSION%";
    private static final String NEW_VERSION = "%NEW_VERSION%";
    private static final Debug debug = Debug.getInstance("amUpgrade");
    private static final String DEFAULT_PASSWORD = "f7e2lu!l3d";
    private static final String HTML_BR = "<br/>";
    private static final String TXT_LF = "\n";
    private static final String REPORT_FILENAME = "upgradereport";
    private static final String DATE_FORMAT = "yyyyMMddHHmmss";
    private static volatile UpgradeServices instance;
    private final List<UpgradeStep> upgradeSteps = new ArrayList<UpgradeStep>();
    private final SimpleDateFormat dateFormat;
    private final String createdDate;

    private UpgradeServices() throws UpgradeException {
        dateFormat = new SimpleDateFormat(DATE_FORMAT);
        createdDate = dateFormat.format(new Date());
        for (String className : UpgradeUtils.getPropertyValues("upgradesteps", "upgrade.step.order")) {
            try {
                UpgradeStep step = Class.forName(className).asSubclass(UpgradeStep.class).newInstance();
                step.initialize();
                if (step.isApplicable()) {
                    upgradeSteps.add(step);
                }
            } catch (Exception ex) {
                debug.error("An error occurred while initializing upgrade steps", ex);
                throw new UpgradeException("Unable to initialize upgrade steps");
            }
        }
    }

    /**
     * Returns the singleton instance of UpgradeServices, which will hold all the details about the different upgrade
     * steps.
     *
     * @return The singleton UpgradeServices instance.
     * @throws UpgradeException If there was an error while initializing the upgrade steps.
     */
    public static UpgradeServices getInstance() throws UpgradeException {
        if (instance == null) {
            synchronized (UpgradeServices.class) {
                if (instance == null) {
                    instance = new UpgradeServices();
                }
            }
        }
        return instance;
    }

    /**
     * Creates the <code>upgrade</code> and <code>backups</code> folders if they are not already present.
     *
     * @param baseDir The base directory of the OpenAM configuration.
     * @throws UpgradeException If there was an error while creating the necessary directories.
     */
    public static void createUpgradeDirectories(String baseDir) throws UpgradeException {
            String upgradeDir = baseDir + File.separator + "upgrade";
            String backupDir = baseDir + File.separator + "backups";

            createDirectory(backupDir);
            createDirectory(upgradeDir);
    }

    private static void createDirectory(String dirName) throws UpgradeException {
        File d = new File(dirName);

        if (d.exists() && d.isFile()) {
            throw new UpgradeException("Directory: " + dirName
                    + " cannot be created as file of the same name already exists");
        }

        if (!d.exists()) {
            if (UpgradeUtils.debug.messageEnabled()) {
                UpgradeUtils.debug.message("Created directory: " + dirName);
            }

            if (!d.mkdir()) {
                throw new UpgradeException("Unable to create directory: " + dirName);
            }
        } else if (!d.canWrite()) {
            // make bootstrap writable if it is not
            if (!d.setWritable(true)) {
                throw new UpgradeException("Unable to make " + dirName + " directory writable");
            }
        }
    }

    /**
     * Kick off the upgrade process.
     *
     * @param adminToken A valid admin SSOToken.
     * @throws UpgradeException If an error occurred while performing upgrade.
     */
    public void upgrade(SSOToken adminToken) throws UpgradeException {
        createUpgradeDirectories(SystemProperties.get(SystemProperties.CONFIG_PATH));
        if (debug.messageEnabled()) {
            debug.message("Upgrade startup.");
        }

        UpgradeProgress.reportStart("upgrade.writingbackup");
        writeBackup(adminToken);
        UpgradeProgress.reportEnd("upgrade.success");

        for (UpgradeStep upgradeStep : upgradeSteps) {
            upgradeStep.perform();
        }

        UpgradeProgress.reportStart("upgrade.writinglog");
        writeReport(adminToken);
        UpgradeProgress.reportEnd("upgrade.success");

        if (debug.messageEnabled()) {
            debug.message("Upgrade complete.");
        }

        // reset the version is newer flag
        // reenable this after the CLI upgrade notifies the console correctly
        //AMSetupServlet.isVersionNewer();
        AMSetupServlet.upgradeCompleted();
    }

    /**
     * Writes the detailed upgrade report to a file.
     *
     * @param adminToken Valid admin SSOToken.
     * @throws UpgradeException If there was an error while writing the report file.
     */
    protected void writeReport(SSOToken adminToken) throws UpgradeException {
        try {
            String baseDir = SystemProperties.get(SystemProperties.CONFIG_PATH);
            String reportFile = baseDir + File.separator + "upgrade" + File.separator + REPORT_FILENAME + "."
                    + createdDate;

            File f = new File(reportFile);

            // if exists then there has been an error
            if (f.exists()) {
                throw new UpgradeException("File " + f.getName() + " already exist!");
            }

            AMSetupServlet.writeToFile(reportFile, generateDetailedUpgradeReport(adminToken, false));
        } catch (IOException ioe) {
            throw new UpgradeException(ioe.getMessage());
        }
    }

    /**
     * Generates a short upgrade report suitable for the upgrade screen.
     *
     * @param adminToken Valid admin SSOToken.
     * @param html Whether the output should be HTML or plain text.
     * @return The short upgrade report.
     */
    public String generateShortUpgradeReport(SSOToken adminToken, boolean html) {
        String delimiter = html ? HTML_BR : TXT_LF;
        StringBuilder report = new StringBuilder();
        for (UpgradeStep upgradeStep : upgradeSteps) {
            report.append(upgradeStep.getShortReport(delimiter));
        }

        return report.toString();
    }

    /**
     * Generates a detailed upgrade report suitable for reviewing changes.
     *
     * @param adminToken Valid admin SSOToken.
     * @param html Whether the output should be HTML or plain text.
     * @return The detailed upgrade report.
     */
    public String generateDetailedUpgradeReport(SSOToken adminToken, boolean html) {
        String delimiter = html ? HTML_BR : TXT_LF;
        Map<String, String> tags = new HashMap<String, String>();
        tags.put(LF, delimiter);
        tags.put(CREATED_DATE, createdDate);
        tags.put(EXISTING_VERSION, UpgradeUtils.getCurrentVersion());
        tags.put(NEW_VERSION, UpgradeUtils.getWarFileVersion());
        StringBuilder report = new StringBuilder(tagSwapReport(tags, "report"));
        for (UpgradeStep upgradeStep : upgradeSteps) {
            report.append(upgradeStep.getDetailedReport(delimiter)).append(delimiter);
        }

        return report.toString();
    }

    /**
     * Creates a backup of the services/subconfigurations in case there is a need for a rollback.
     *
     * @param adminToken Valid admin SSOToken.
     * @throws UpgradeException If there was an error while creating the backup.
     */
    protected void writeBackup(SSOToken adminToken) throws UpgradeException {
        FileOutputStream fout = null;
        String baseDir = SystemProperties.get(SystemProperties.CONFIG_PATH);
        String backupDir = baseDir + File.separator + "backups" + File.separator;
        File backupFile = new File(backupDir + "servicebackup." + createdDate);
        File backupPasswdFile = new File(backupDir + "servicebackup.password." + createdDate);
        String backupPassword = generateBackupPassword();

        if (backupFile.exists()) {
            debug.error("Upgrade cannot continue as backup file exists! " + backupFile.getName());
            throw new UpgradeException("Upgrade cannot continue as backup file exists");
        }

        try {
            fout = new FileOutputStream(backupFile);
            ServiceManager sm = new ServiceManager(adminToken);
            AMEncryption encryptObj = new JCEEncryption();
            ((ConfigurableKey)encryptObj).setPassword(backupPassword);

            String resultXML = sm.toXML(encryptObj);
            resultXML += "<!-- " + Hash.hash(backupPassword) + " -->";

            fout.write(resultXML.getBytes("UTF-8"));
        } catch (Exception ex) {
            debug.error("Unable to write backup: ", ex);
            throw new UpgradeException("Unable to write backup: " + ex.getMessage());
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException ioe) {
                    //ignored
                }
            }
        }

        if (backupPasswdFile.exists()) {
            debug.error("Upgrade cannot continue as backup password file exists! " + backupPasswdFile.getName());
            throw new UpgradeException("Upgrade cannot continue as backup password file exists");
        }

        PrintWriter out = null;

        try {
            out = new PrintWriter(new FileOutputStream(backupPasswdFile));
            out.println(backupPassword);
            out.flush();
        } catch (IOException ioe) {
            debug.error("Unable to write backup: ", ioe);
            throw new UpgradeException("Unable to write backup: " + ioe.getMessage());
        } catch (Exception ex) {
            debug.error("Unable to write backup: ", ex);
            throw new UpgradeException("Unable to write backup: " + ex.getMessage());
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private String generateBackupPassword() {
        PasswordGenerator passwordGenerator = new RandomPasswordGenerator();
        String password = null;

        try {
            password = passwordGenerator.generatePassword(null);
        } catch (PWResetException pre) {
            // default implementation will not do this
            password = DEFAULT_PASSWORD;
        }

        return password;
    }

    /**
     * Runs tagswap on a report, so the report structure is not hardcoded.
     *
     * @param reportContents Key-value mapping for the tags, these will be used to replace the localized report.
     * @param key The i18n key in the localization file for this given report.
     * @return The tagswapped report.
     */
    public static String tagSwapReport(Map<String, String> reportContents, String key) {
        return tagSwap(reportContents, BUNDLE.getString(key));
    }

    /**
     * Tagswaps a given String based on the passed in tags.
     *
     * @param tags Key-value mapping for the tags, these will be used to replace the localized report.
     * @param content The content that needs to be tagswapped.
     * @return The tagswapped content.
     */
    public static String tagSwap(Map<String, String> tags, String content) {
        for (Map.Entry<String, String> contents : tags.entrySet()) {
            content = content.replace(contents.getKey(), contents.getValue().toString());
        }
        return content;
    }
}

/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All Rights Reserved
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

/*
   Portions Copyrighted 2013 ForgeRock, AS.
 */

package org.forgerock.openam.upgrade;

import com.sun.identity.setup.AMSetupServlet;
import com.sun.identity.setup.SetupConstants;
import com.sun.identity.shared.debug.Debug;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletContext;

import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.requests.Requests;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldif.EntryReader;
import org.forgerock.opendj.ldif.LDIF;
import org.forgerock.opendj.ldif.LDIFEntryReader;
import org.forgerock.opendj.ldif.LDIFEntryWriter;
import org.opends.server.core.LockFileManager;
import org.opends.server.tools.RebuildIndex;
import org.opends.server.util.TimeThread;


/**
 * Upgrade tool for upgrading the embedded instance of OpenDS to OpenDJ.
 * <p>
 * The upgrade is idempotent and, as a result, may be called multiple times.
 * This is particularly useful if an upgrade fails to complete since the upgrade
 * can be invoked again in order to complete the upgrade, assuming any
 * outstanding issues have been resolved.
 * <p>
 * The following code illustrates how OpenDJ should be upgraded:
 * <pre>
 * // Pass in the embedded OpenDS install path here.
 * OpenDJUpgrader upgrader = new OpenDJUpgrader("/home/opensso/opensso/opends");
 *
 * if (upgrader.isUpgradeRequired())
 * {
 *   upgrader.upgrade();
 * }
 * </pre>
 * <p>
 * <b>Note:</b> it is not possible to revert an upgrade. It is assumed that
 * users will backup their OpenAM before performing an upgrade allowing them to
 * revert their changes by restoring from backup.
 */
public final class OpenDJUpgrader {
    private static final String ZIP_FILE = "/WEB-INF/template/opendj/opendj.zip";

    private static final void closeIfNotNull(final Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (final Exception ignored) {
                // Do nothing.
            }
        }
    }

    private final String installRoot;
    private final File upgradeMarker;
    private final NavigableSet<Integer> versionHistory;
    private final int currentVersion;
    private final int newVersion;
    private final ServletContext servletCtx;
    private final int dj245Version = 7743;
    private final int dj246Version = 8102;

    /**
     * Creates a new OpenDJ upgrader.
     *
     * @param installRoot
     *          The installation root of the embedded OpenDS instance, which is
     *          usually {@code ~/openam/opends}.
     */
    public OpenDJUpgrader(final String installRoot, final ServletContext servletCtx) {
        this.installRoot = installRoot;
        this.servletCtx = servletCtx;
        this.upgradeMarker = new File(installRoot + "/.upgrade_marker");

        // Determine the current version. Note that if the upgrade marker exists
        // then that implies that OpenDS was only partially upgraded.
        this.newVersion = readNewVersion();
        this.versionHistory = getVersionHistory();
        if (upgradeMarker.exists() && versionHistory.last() == newVersion) {
            // Previous upgrade has not completed, so the last version is not
            // accurate.
            this.currentVersion = versionHistory.lower(newVersion);
        } else {
            this.currentVersion = versionHistory.last();
        }
    }

    /**
     * Returns {@code true} if the version of OpenDJ contained in the OpenAM WAR
     * file is more recent (newer) than the the version of OpenDS/OpenDJ last used
     * by OpenAM.
     *
     * @return {@code true} if an upgrade should be performed.
     */
    public boolean isUpgradeRequired() {
        return newVersion > currentVersion;
    }

    /**
     * Upgrades the embedded DS instance to OpenDJ 2.6 See the class
     * description for more detailed information.
     *
     * @throws Exception
     *           If an unexpected exception occurred.
     */
    public void upgrade() throws Exception {
        // First determine if upgrade is required.
        if (!checkUpgradePreconditions()) {
            return;
        }

        // Create a marker file which will be removed only on completion.
        upgradeMarker.createNewFile();

        // Check DS version if it is less than 2.4.5
        // then use the old method to upgrade
        if (currentVersion < dj245Version) {

            // First back up customized configuration files.
            backupFile("config/config.ldif");
            backupFile("config/admin-backend.ldif");
            backupFile("config/java.properties");

            // Unpack opendj.zip over the top of the existing installation.
            unpackZipFile(true);
            callOldDJUpgrade();
        } else {
            // Unpack opendj.zip over the top of the existing installation.
            unpackZipFile(false);

            // check if this is OpenAM10.10/OpenDJ 2.4.6 and if so
            // delete the cts-add schema.  See AME-932
            if (currentVersion == dj246Version) {
                try {
                    File badSchema = new File(installRoot + File.separator + "config"
                            + File.separator + "schema" + File.separator + "cts-add-schema.ldif");
                    badSchema.delete();
                } catch (Exception e) {
                    // do nothing here, we don't care if the file
                    // doesn't exist
                }
                // copy replacement over
                File goodSchema = new File(servletCtx.getRealPath(File.separator + "WEB-INF" + File.separator +
                        "template" + File.separator + "ldif" + File.separator +"sfha" + File.separator +
                        "99-cts-add-schema-backport.ldif"));
                //adding the backport compatible cts schema file
                File moveTo = new File(installRoot+ File.separator + "config"+ File.separator +
                        "schema" + File.separator + "99-cts-add-schema-backport.ldif");
                copy(goodSchema, moveTo);
            }



            // call the new OpenDJ upgrade mechanism
            int ret = callDJUpgradeMechanism();

            if (ret == 0) {
                message("Upgrade completed successfully");
                upgradeMarker.delete();

                // Attempt to release the server lock, OpenDJ does not do this after an upgrade
                // Work around for OPENDJ-1078
                final String lockFile = LockFileManager.getServerLockFileName();
                LockFileManager.releaseLock(lockFile, new StringBuilder());

                // Work around for OPENDJ-1079
                fixDJConfig();
            } else {
                throw new UpgradeException("OpenDJ upgrade failed with code:  "+ret);
            }
        }
    }

    private void callOldDJUpgrade() throws Exception{
        deleteFile("lib/OpenDS.jar");
        deleteFile("lib/activation.jar");

        // Patch the current configuration with the differences between the default
        // current configuration and the default new configuration.
        patchConfiguration();

        // Restore remaining backups (config has been patched).
        restoreFile("config/admin-backend.ldif");
        restoreFile("config/java.properties");

        // Rebuild all indexes for all local DB backends.
        final List<DN> baseDNs = findBaseDNs();
        for (final DN baseDN : baseDNs) {
            rebuildAllIndexes(baseDN);
        }

        // Log completion an remove marker.
        message("Upgrade completed successfully");
        upgradeMarker.delete();
    }

    private int callDJUpgradeMechanism() {

        final String[] args = {
                "--configClass", "org.opends.server.extensions.ConfigFileHandler",
                "--configFile", installRoot + "/config/config.ldif",
                "--acceptLicense",
                "--force",
                "--no-prompt"
        };
        // put system properties out in the env so DJ knows where it's at

        System.setProperty("INSTALL_ROOT", installRoot);
        System.setProperty("org.opends.server.ServerRoot", installRoot);
        return org.opends.server.tools.upgrade.UpgradeCli.main(args, true, System.out, System.err);
    }


    // Work around for OPENDJ-1079 is to remove the HTTP Connection Handler from the
    // config.  This entry breaks Tomcat6/OpenAM and must be removed

    private void fixDJConfig() {
        File readConfig = new File(installRoot+"/config/config.ldif");
        File writeConfig = new File(installRoot+"/config/config.ldif.UPDATED");

        String currentLine;
        String targetLine = new String("dn: cn=HTTP Connection Handler");

        BufferedReader read = null;
        BufferedWriter write = null;

        try {
            read = new BufferedReader(
                    new FileReader(readConfig));

            write = new BufferedWriter(
                    new FileWriter(writeConfig));

            while ((currentLine = read.readLine()) != null) {
                if (currentLine.contains(targetLine)) {
                    while(((currentLine = read.readLine()) != null) &&
                            (currentLine.length() != 0)) {
                        // skip lines in the target dn
                        continue;
                    }
                } else {
                    // write out the lines we're not interested in
                    write.write(currentLine+"\n");
                }
            }

            // delete old config and rename new config
            readConfig.delete();
            writeConfig.renameTo(new File(installRoot+"/config/config.ldif"));

        } catch(FileNotFoundException fnfe) {
            error("Could not find file:  "+fnfe.getMessage());
        } catch (IOException ioe) {
            error("Could not read/write file:  "+ioe.getMessage());
        } finally {
            closeIfNotNull(write);
            closeIfNotNull(read);

        }
    }

    private void backupFile(final String fileName)
            throws IOException {
        message("Backing up file " + fileName + "...");
        final File currentFile = new File(installRoot, fileName);
        final File backupFile = getBackupFileName(fileName);

        if (backupFile.exists()) {
            message("skipped (already backed up)");
        } else {
            try {
                copy(currentFile, backupFile);
                message("done");
            } catch (final IOException ioe) {
                // File may not exist - benign.
                message("failed: " + ioe.getMessage());
                throw ioe;
            }
        }
    }

    private void deleteFile(final String fileName) {
        message("Deleting file " + fileName + "...");
        final File file = new File(installRoot + "/" + fileName);

        if (file.delete()) {
            message("done");
        } else {
            // File may not exist - benign.
            message("skipped (not found?)");
        }
    }

    private void restoreFile(final String fileName)
            throws IOException {
        message("Restoring file " + fileName + "...");
        final File currentFile = new File(installRoot, fileName);
        final File backupFile = getBackupFileName(fileName);

        try {
            copy(backupFile, currentFile);
            message("done");
        } catch (final IOException ioe) {
            // File may not exist - benign.
            error("failed: ", ioe);
            throw ioe;
        }
    }

    private final void copy(final File from, final File to)
            throws IOException {
        FileInputStream is = null;
        FileOutputStream os = null;

        try {
            is = new FileInputStream(from);
            os = new FileOutputStream(to);
            copy(is, os);
        } finally {
            closeIfNotNull(os);
            closeIfNotNull(is);
        }
    }

    private List<DN> findBaseDNs()
            throws IOException {
        final List<DN> baseDNs = new LinkedList<DN>();
        final SearchRequest request = Requests.newSearchRequest(
                "cn=backends,cn=config", SearchScope.WHOLE_SUBTREE,
                "(objectclass=ds-cfg-local-db-backend)", "ds-cfg-base-dn");
        FileInputStream is = null;
        LDIFEntryReader reader = null;

        try {
            is = new FileInputStream(installRoot + "/config/config.ldif");
            reader = new LDIFEntryReader(is);
            final EntryReader filteredReader = LDIF.search(reader, request);

            while (filteredReader.hasNext()) {
                final Entry entry = filteredReader.readEntry();
                final Attribute values = entry.getAttribute("ds-cfg-base-dn");

                if (values != null) {
                    for (final ByteString value : values) {
                        baseDNs.add(DN.valueOf(value.toString()));
                    }
                }
            }
        } finally {
            closeIfNotNull(reader);
            closeIfNotNull(is);
        }

        return baseDNs;
    }

    private void copy(final InputStream from, final OutputStream to)
            throws IOException {
        final BufferedInputStream bis = new BufferedInputStream(from);
        final BufferedOutputStream bos = new BufferedOutputStream(to);

        for (int b = bis.read(); b != -1; b = bis.read()) {
            bos.write(b);
        }

        bos.flush();
    }



    private boolean checkUpgradePreconditions() {
        if (currentVersion == 0) {
            error("Upgrade failed: unable to determine the current OpenDS/OpenDJ version");
            return false;
        }

        if (newVersion == 0) {
            error("Upgrade failed: unable to determine the new OpenDJ version");
            return false;
        }

        if (!isUpgradeRequired()) {
            message("Upgrade not required: installed OpenDJ is up to date");
            return false;
        }

        // Upgrade is required.
        if (upgradeMarker.exists()) {
            error("Upgrade required: continuing incomplete upgrade from "
              + currentVersion + " to " + newVersion);
        } else {
            error("Upgrade required: upgrading from " + currentVersion + " to "
              + newVersion);
        }
        
        return true;
    }

    private NavigableSet<Integer> getVersionHistory() {
        final File upgradeDirName = new File(installRoot, "config/upgrade");
        final String pattern = "schema.ldif.";
        final NavigableSet<Integer> versions = new TreeSet<Integer>();

        // Always include 0 in order to avoid checking for empty/null.
        versions.add(0);

        if (upgradeDirName.exists() && upgradeDirName.isDirectory()) {
            final String[] configFiles = upgradeDirName.list(new FilenameFilter() {
                public boolean accept(final File dir, final String name) {
                    return name.startsWith(pattern);
                }
            });

            for (final String configFile : configFiles) {
                if (configFile.length() > 0) {
                    final String version = configFile.substring(pattern.length());
              
                    try {
                        versions.add(Integer.parseInt(version));
                    } catch (final NumberFormatException nfe) {
                        // TODO: log something?
                    }
                }
            }
        }

        return versions;
    }

    private void message(final String msg) {
        Debug.getInstance(SetupConstants.DEBUG_NAME).message(msg);
    }
    
    private void message(final String msg, final Throwable th) {
        Debug.getInstance(SetupConstants.DEBUG_NAME).message(msg, th);
    }
    
    private void error(final String msg) {
        Debug.getInstance(SetupConstants.DEBUG_NAME).error(msg);
    }
    
    private void error(final String msg, final Throwable th) {
        Debug.getInstance(SetupConstants.DEBUG_NAME).error(msg, th);
    }

    private File getBackupFileName(final String fileName) {
        final File backupFile = new File(installRoot, fileName + "."
                + currentVersion + ".bak");
        return backupFile;
    }

    private void patchConfiguration()
            throws IOException {
        message("Patching configuration config/config.ldif...");
        InputStream defaultCurrentConfig = null;
        InputStream defaultNewConfig = null;
        InputStream currentConfig = null;
        OutputStream newCurrentConfig = null;

        try {
            defaultCurrentConfig = new FileInputStream(installRoot + "/"
                    + "config/upgrade/config.ldif." + currentVersion);
            defaultNewConfig = new FileInputStream(installRoot + "/"
                    + "config/upgrade/config.ldif." + newVersion);
            currentConfig = new FileInputStream(
                    getBackupFileName("config/config.ldif"));
            newCurrentConfig = new FileOutputStream(installRoot + "/"
                    + "config/config.ldif");

            final LDIFEntryReader defaultCurrentConfigReader = new LDIFEntryReader(
                    defaultCurrentConfig);
            final LDIFEntryReader defaultNewConfigReader = new LDIFEntryReader(
                    defaultNewConfig);
            final LDIFEntryReader currentConfigReader = new LDIFEntryReader(
                    currentConfig);
            final LDIFEntryWriter newConfigWriter = new LDIFEntryWriter(
                    newCurrentConfig);

            LDIF.copyTo(
                    LDIF.patch(currentConfigReader,
                            LDIF.diff(defaultCurrentConfigReader, defaultNewConfigReader)),
                    newConfigWriter);
            newConfigWriter.flush();
            message("done");
        } catch (final IOException ioe) {
            message("failed: " + ioe.getMessage());
            throw ioe;
        } finally {
            closeIfNotNull(newCurrentConfig);
            closeIfNotNull(currentConfig);
            closeIfNotNull(defaultNewConfig);
            closeIfNotNull(defaultCurrentConfig);
        }
    }

    private int readNewVersion() {
        final String pattern = "template/config/upgrade/schema.ldif.";

        InputStream is = null;
        ZipInputStream zis = null;
        
        try {
            is = AMSetupServlet.getResourceAsStream(servletCtx, ZIP_FILE);
            zis = new ZipInputStream(is);
            
            for (ZipEntry zipEntry = zis.getNextEntry(); zipEntry != null; zipEntry = zis.getNextEntry()) {
                final String fileName = zipEntry.getName();
            
                if (fileName.startsWith(pattern)) {
                    final String version = fileName.substring(pattern.length());
              
                    try {
                        return Integer.parseInt(version);
                    } catch (final NumberFormatException nfe) {
                        // TODO: log something?
                    }
                }
            
                zis.closeEntry();
            }
        } catch (final IOException ioe) {
            // TODO: log something?
            return 0;
        } finally {
            closeIfNotNull(zis);
            closeIfNotNull(is);
        }

        // TODO: No version found, log something?
        return 0;
    }

    private void unpackZipFile(boolean oldStructure)
    throws IOException {
        message("Unzipping " + ZIP_FILE + "...");
        InputStream is = null;
        ZipInputStream zis = null;
        FileOutputStream fos = null;
        
        try {
            is = AMSetupServlet.getResourceAsStream(servletCtx, ZIP_FILE);
            zis = new ZipInputStream(is);

            for (ZipEntry zipEntry = zis.getNextEntry(); zipEntry != null; zipEntry = zis.getNextEntry()) {
                File outputFileName;
                // If we're upgrading with the old mechanism we need
                // to strip the "template/" from the directory path so
                // the files end up in the correct location
                if (oldStructure && zipEntry.getName().contains("template")) {
                    String newName = zipEntry.getName().replace("template/", "");
                    outputFileName = new File(installRoot + "/" + newName);
                } else {
                    outputFileName = new File(installRoot + "/" + zipEntry.getName());
                }
                if (zipEntry.isDirectory()) {
                    outputFileName.mkdir();
                } else {
                    // Copy the file.
                    fos = new FileOutputStream(outputFileName);
                    copy(zis, fos);
                    fos.close();

                    // Set permissions.
                    if (zipEntry.getName().endsWith(".sh") || zipEntry.getName().startsWith("bin")) {
                        outputFileName.setExecutable(true);
                    }
                }

                zis.closeEntry();
            }
      
            message("done");
        } catch (final IOException ioe) {
            error("failed: ", ioe);
            throw ioe;
        } finally {
            closeIfNotNull(fos);
            closeIfNotNull(zis);
            closeIfNotNull(is);
        }
    }

    private void rebuildAllIndexes(final DN baseDN)
            throws Exception {
        // @formatter:off
        final String[] args = {
                "--configClass", "org.opends.server.extensions.ConfigFileHandler",
                "--configFile", installRoot + "/config/config.ldif",
                "--rebuildAll",
                "--baseDN", baseDN.toString()
        };
        // @formatter:on

        message("Rebuilding indexes for suffix \"" + baseDN.toString() + "\"...");
        final OutputStream stdout = new ByteArrayOutputStream();
        final OutputStream stderr = new ByteArrayOutputStream();

        try {
            TimeThread.start();
            System.setProperty("org.opends.server.ServerRoot", installRoot);
            final int rc = RebuildIndex.mainRebuildIndex(args, true, stdout, stderr);

            if (rc == 0) {
                message(stdout.toString());
                message("done");
            } else {
                throw new IOException("failed with return code " + rc);
            }
        } catch (final Exception ex) {
            error(stderr.toString());
            error("Rebuilding indexes", ex);
            throw ex;
        } finally {
            TimeThread.stop();
        }
    }
}

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
     * Returns the revision number associated with the version of OpenDS/OpenDJ
     * that was last used by OpenAM. Note that if an upgrade has partially
     * completed then the returned revision number will still be accurate, i.e. it
     * will not reference the new version.
     *
     * @return The revision number associated with the version of OpenDS/OpenDJ
     *         that was last used by OpenAM.
     */
    public int getCurrentVersion() {
        return currentVersion;
    }

    /**
     * Returns the revision number associated with the version of OpenDJ contained
     * in the OpenAM WAR file.
     *
     * @return The revision number associated with the version of OpenDJ contained
     *         in the OpenAM WAR file.
     */
    public int getNewVersion() {
        return newVersion;
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
     * Upgrades the embedded OpenDS instance to OpenDJ 2.4.4. See the class
     * description for me detailed information.
     *
     * @throws Exception
     *           If an unexpected exception occurred.
     */
    public void upgrade()
    throws Exception {
        // First determine if upgrade is required.
        if (!checkUpgradePreconditions()) {
            return;
        }

        // Create a marker file which will be removed only on completion.
        upgradeMarker.createNewFile();

        // First back up customized configuration files.
        backupFile("config/config.ldif");
        backupFile("config/admin-backend.ldif");
        backupFile("config/java.properties");

        // Unpack opendj.zip over the top of the existing installation.
        unpackZipFile();

        // Copy remaining JAR files.
        copyFileFromWAR("lib/OpenDJ-2012-20-02.jar");           // Was OpenDJ.jar before Maven Support.
        copyFileFromWAR("lib/sleepycat-je-2011-04-07.jar");            // Was je.jar before Maven Support.
        copyFileFromWAR("lib/mail-1.4.5.jar");                 // Was mail.jar before Maven Support.

        // Delete files which are no longer needed.
        deleteFile("lib/OpenDS.jar");
        deleteFile("lib/activation.jar");

        // Restore remaining backups (config has been patched).
        restoreFile("config/admin-backend.ldif");
        restoreFile("config/java.properties");

        // Patch the current configuration with the differences between the default
        // current configuration and the default new configuration.
        patchConfiguration();

        // Rebuild all indexes for all local DB backends.
        final List<DN> baseDNs = findBaseDNs();
        for (final DN baseDN : baseDNs) {
            rebuildAllIndexes(baseDN);
        }

        // Log completion an remove marker.
        message("Upgrade completed successfully");
        upgradeMarker.delete();
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

    private void copy(final InputStream from, final OutputStream to)
    throws IOException {
        final BufferedInputStream bis = new BufferedInputStream(from);
        final BufferedOutputStream bos = new BufferedOutputStream(to);
        
        for (int b = bis.read(); b != -1; b = bis.read()) {
            bos.write(b);
        }
        
        bos.flush();
    }

    private void copyFileFromWAR(final String fileName)
    throws IOException {
        message("Copying file " + fileName + " from WAR...");
        InputStream is = null;
        OutputStream os = null;

        try {
            is = AMSetupServlet.getResourceAsStream(servletCtx, "/WEB-INF/" + fileName);
            os = new FileOutputStream(new File(installRoot + "/" + fileName));
            copy(is, os);
            message("done");
        } catch (final IOException ioe) {
            message("failed: " + ioe.getMessage());
            throw ioe;
        } finally {
            closeIfNotNull(is);
            closeIfNotNull(os);
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

    private File getBackupFileName(final String fileName) {
        final File backupFile = new File(installRoot, fileName + "."
            + currentVersion + ".bak");
        return backupFile;
    }

    private NavigableSet<Integer> getVersionHistory() {
        final File upgradeDirName = new File(installRoot, "config/upgrade");
        final String pattern = "config.ldif.";
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
        final String pattern = "config/upgrade/config.ldif.";

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
                message("done");
            } else {
                throw new IOException("failed with return code " + rc);
            }
        } catch (final Exception ex) {
            error("Rebuilding indexes", ex);
            throw ex;
        } finally {
            TimeThread.stop();
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

    private void unpackZipFile()
    throws IOException {
        message("Unzipping " + ZIP_FILE + "...");
        InputStream is = null;
        ZipInputStream zis = null;
        FileOutputStream fos = null;
        
        try {
            is = AMSetupServlet.getResourceAsStream(servletCtx, ZIP_FILE);
            zis = new ZipInputStream(is);
      
            for (ZipEntry zipEntry = zis.getNextEntry(); zipEntry != null; zipEntry = zis.getNextEntry()) {
                final File outputFileName = new File(installRoot + "/" + zipEntry.getName());
        
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
}

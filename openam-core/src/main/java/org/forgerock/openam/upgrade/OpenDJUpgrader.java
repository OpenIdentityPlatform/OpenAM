/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2016 ForgeRock AS.
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
 */

package org.forgerock.openam.upgrade;

import static java.nio.file.Files.copy;

import com.sun.identity.setup.AMSetupUtils;
import com.sun.identity.setup.SetupConstants;
import com.sun.identity.shared.debug.Debug;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.annotation.Nonnull;
import javax.servlet.ServletContext;

import org.forgerock.openam.ldap.LDAPRequests;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldif.EntryReader;
import org.forgerock.opendj.ldif.LDIF;
import org.forgerock.opendj.ldif.LDIFEntryReader;
import org.forgerock.opendj.ldif.LDIFEntryWriter;
import org.forgerock.util.annotations.VisibleForTesting;
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
    private static final OpenDJVersion DJ_245_VERSION = OpenDJVersion.valueOf("2.4.5.7743");
    private static final OpenDJVersion DJ_246_VERSION = OpenDJVersion.valueOf("2.4.6.8102");

    /**
     * List of system properties that need to be set to the DJ installation root before running the DJ upgrade tasks.
     */
    public static final List<String> INSTALL_ROOT_PROPERTIES = Arrays.asList("INSTALL_ROOT",
            "org.opends.server.ServerRoot", "org.opends.quicksetup.Root");


    private final String installRoot;
    private final File upgradeMarker;
    private final OpenDJVersion currentVersion;
    private final OpenDJVersion newVersion;
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
        OpenDJVersion currentVersion = readCurrentVersion();
        if (upgradeMarker.exists() && newVersion.equals(currentVersion)) {
            // Previous upgrade has not completed, so the last version is not
            // accurate.
            this.currentVersion = readVersionFromFile(upgradeMarker);
        } else {
            this.currentVersion = currentVersion;
        }
    }

    private OpenDJVersion readCurrentVersion() {
        return readVersionFromFile(new File(installRoot, "config/buildinfo"));
    }

    private OpenDJVersion readVersionFromFile(final File file) {
        try {
            String version = IOUtils.readStream(new FileInputStream(file)).trim();
            return OpenDJVersion.valueOf(version);
        } catch (IOException e) {
            error("Unable to read OpenDJ version from file: " + file, e);
            return OpenDJVersion.UNKNOWN;
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
        return newVersion.isMoreRecentThan(currentVersion) ||
                (newVersion.equals(currentVersion) && newVersion.isDifferentBuildTo(currentVersion));
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
        try (BufferedWriter out = new BufferedWriter(new FileWriter(upgradeMarker))) {
            out.write(currentVersion.toString());
            out.write('\n');
        }

        // Check DS version if it is less than 2.4.5
        // then use the old method to upgrade
        if (currentVersion.isOlderThan(DJ_245_VERSION)) {

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
            if (currentVersion.equals(DJ_246_VERSION)) {
                try {
                    File badSchema = new File(installRoot + File.separator + "config"
                            + File.separator + "schema" + File.separator + "cts-add-schema.ldif");
                    delete(badSchema);
                } catch (RuntimeException e) {
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
                copy(goodSchema.toPath(), moveTo.toPath());
            }



            // call the new OpenDJ upgrade mechanism
            int ret = callDJUpgradeMechanism();

            if (ret == 0) {
                message("Upgrade completed successfully");
                delete(upgradeMarker);

                // Attempt to release the server lock, OpenDJ does not do this after an upgrade
                // Work around for OPENDJ-1078
                final String lockFile = LockFileManager.getServerLockFileName();
                LockFileManager.releaseLock(lockFile, new StringBuilder());
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
        delete(upgradeMarker);
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
        for (String property : INSTALL_ROOT_PROPERTIES) {
            System.setProperty(property, installRoot);
        }
        return org.opends.server.tools.upgrade.UpgradeCli.main(args, true, System.out, System.err);
    }

    private void backupFile(final String fileName) throws IOException {
        message("Backing up file " + fileName + "...");
        final File currentFile = new File(installRoot, fileName);
        final File backupFile = getBackupFileName(fileName);

        if (backupFile.exists()) {
            message("skipped (already backed up)");
        } else {
            try {
                copy(currentFile.toPath(), backupFile.toPath());
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

    private void restoreFile(final String fileName) throws IOException {
        message("Restoring file " + fileName + "...");
        final File currentFile = new File(installRoot, fileName);
        final File backupFile = getBackupFileName(fileName);

        try {
            copy(backupFile.toPath(), currentFile.toPath());
            message("done");
        } catch (final IOException ioe) {
            // File may not exist - benign.
            error("failed: ", ioe);
            throw ioe;
        }
    }

    private List<DN> findBaseDNs() throws IOException {
        final List<DN> baseDNs = new LinkedList<DN>();
        final SearchRequest request = LDAPRequests.newSearchRequest("cn=backends,cn=config", SearchScope.WHOLE_SUBTREE,
                "(objectclass=ds-cfg-backend)", "ds-cfg-base-dn");

        try (LDIFEntryReader reader = new LDIFEntryReader(new FileInputStream(installRoot + "/config/config.ldif"))) {
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
        }

        return baseDNs;
    }


    private boolean checkUpgradePreconditions() {
        if (OpenDJVersion.UNKNOWN.equals(currentVersion)) {
            error("Upgrade failed: unable to determine the current OpenDS/OpenDJ version");
            return false;
        }

        if (OpenDJVersion.UNKNOWN.equals(newVersion)) {
            error("Upgrade failed: unable to determine the new OpenDJ version");
            return false;
        }

        if (!isUpgradeRequired()) {
            message("Upgrade not required: installed OpenDJ is up to date");
            return false;
        }

        // Upgrade is required.
        if (upgradeMarker.exists()) {
            error("Upgrade required: continuing incomplete upgrade from " + currentVersion + " to " + newVersion);
        } else {
            error("Upgrade required: upgrading from " + currentVersion + " to " + newVersion);
        }
        
        return true;
    }


    private static void message(final String msg) {
        Debug.getInstance(SetupConstants.DEBUG_NAME).message(msg);
    }

    private static void error(final String msg) {
        Debug.getInstance(SetupConstants.DEBUG_NAME).error(msg);
    }
    
    private static void error(final String msg, final Throwable th) {
        Debug.getInstance(SetupConstants.DEBUG_NAME).error(msg, th);
    }

    private File getBackupFileName(final String fileName) {
        final File backupFile = new File(installRoot, fileName + "."
                + currentVersion + ".bak");
        return backupFile;
    }

    private void patchConfiguration() throws IOException {
        message("Patching configuration config/config.ldif...");

        try (InputStream defaultCurrentConfig = new FileInputStream(installRoot + "/config/upgrade/config.ldif."
                + currentVersion);
            InputStream defaultNewConfig = new FileInputStream(installRoot + "/config/upgrade/config.ldif."
                    + newVersion);
            InputStream currentConfig = new FileInputStream(getBackupFileName("config/config.ldif"));
            OutputStream newCurrentConfig = new FileOutputStream(installRoot + "/config/config.ldif")) {

            final LDIFEntryReader defaultCurrentConfigReader = new LDIFEntryReader(defaultCurrentConfig);
            final LDIFEntryReader defaultNewConfigReader = new LDIFEntryReader(defaultNewConfig);
            final LDIFEntryReader currentConfigReader = new LDIFEntryReader(currentConfig);
            final LDIFEntryWriter newConfigWriter = new LDIFEntryWriter(newCurrentConfig);

            LDIF.copyTo(
                    LDIF.patch(currentConfigReader,
                            LDIF.diff(defaultCurrentConfigReader, defaultNewConfigReader)),
                    newConfigWriter);
            newConfigWriter.flush();
            message("done");
        } catch (final IOException ioe) {
            message("failed: " + ioe.getMessage());
            throw ioe;
        }
    }

    private OpenDJVersion readNewVersion() {
        final String buildinfo = "template/config/buildinfo";

        try (ZipInputStream zis = new ZipInputStream(AMSetupUtils.getResourceAsStream(servletCtx, ZIP_FILE))) {

            for (ZipEntry zipEntry = zis.getNextEntry(); zipEntry != null; zipEntry = zis.getNextEntry()) {
                final String fileName = zipEntry.getName();

                if (fileName.equals(buildinfo)) {
                    String version = IOUtils.readStream(zis);
                    return OpenDJVersion.valueOf(version);
                }
            
                zis.closeEntry();
            }
        } catch (final IOException ioe) {
            error("Error reading DJ version number", ioe);
            return OpenDJVersion.UNKNOWN;
        }

        // TODO: No version found, log something?
        return OpenDJVersion.UNKNOWN;
    }

    private void unpackZipFile(boolean oldStructure) throws IOException {
        message("Unzipping " + ZIP_FILE + "...");

        try (ZipInputStream zis = new ZipInputStream(AMSetupUtils.getResourceAsStream(servletCtx, ZIP_FILE))) {

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
                    try (FileOutputStream fos = new FileOutputStream(outputFileName)) {
                        IOUtils.copyStream(zis, fos);
                    }

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
        }
    }

    private void rebuildAllIndexes(final DN baseDN) throws Exception {
        // @formatter:off
        final String[] args = {
                "--configClass", "org.opends.server.extensions.ConfigFileHandler",
                "--configFile", installRoot + "/config/config.ldif",
                "--rebuildAll",
                "--baseDN", baseDN.toString(),
                "--noPropertiesFile"
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

    /**
     * Attempts to delete the given file and logs a message if it is not successful.
     *
     * @param fileToDelete the file to delete.
     */
    private static void delete(File fileToDelete) {
        if (!fileToDelete.delete()) {
            message("Unable to delete file: " + fileToDelete.getPath());
        }
    }

    /**
     * Represents OpenDJ version information extracted from the installation directory or zip file buildinfo. Version
     * numbers are compared to each other using only the major, minor and patch version numbers. If two versions are
     * considered equal they may still be from different builds and this should be tested manually using the
     * {@link #isDifferentBuildTo(OpenDJVersion)} method.
     */
    @VisibleForTesting
    static class OpenDJVersion implements Comparable<OpenDJVersion> {
        static final OpenDJVersion UNKNOWN = new OpenDJVersion(-1, -1, -1, null);

        private final int major;
        private final int minor;
        private final int patch;
        private final String build;

        private OpenDJVersion(final int major, final int minor, final int patch, final String build) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
            this.build = build;
        }

        /**
         * Parses a version string of the form {@literal "major.minor.patch.build"} where major, minor and patch are
         * expected to be integers, and build is expected to be an arbitrary string (a Git commit hash in DJ 3.0, an
         * SVN revision number in DJ 2). No attempt is made to accept version strings that deviate from this format.
         *
         * @param version the version string.
         * @return the parsed version string or {@link #UNKNOWN} if the version string is invalid.
         */
        static OpenDJVersion valueOf(String version) {
            try {
                final String[] parts = version.split("\\.");
                final int major = Integer.parseInt(parts[0]);
                final int minor = Integer.parseInt(parts[1]);
                final int patch = Integer.parseInt(parts[2]);
                final String build = parts[3].trim();

                return new OpenDJVersion(major, minor, patch, build);
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
                error("Invalid OpenDJ version string: " + version);
                return UNKNOWN;
            }
        }

        @Override
        public int compareTo(final @Nonnull OpenDJVersion that) {
            int result = compare(this.major, that.major);
            if (result == 0) {
                result = compare(this.minor, that.minor);
            }
            if (result == 0) {
                result = compare(this.patch, that.patch);
            }
            return result;
        }

        public boolean isMoreRecentThan(OpenDJVersion that) {
            return this.compareTo(that) > 0;
        }

        public boolean isDifferentBuildTo(OpenDJVersion that) {
            return !StringUtils.compareCaseInsensitiveString(this.build, that.build);
        }

        public boolean isOlderThan(OpenDJVersion that) {
            return this.compareTo(that) < 0;
        }

        private int compare(int a, int b) {
            return Integer.valueOf(a).compareTo(b);
        }

        @Override
        public boolean equals(final Object that) {
            return this == that || that instanceof OpenDJVersion && this.compareTo(((OpenDJVersion) that)) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(major, minor, patch, build);
        }

        @Override
        public String toString() {
            return String.format(Locale.ENGLISH, "%d.%d.%d.%s", major, minor, patch, build);
        }
    }
}

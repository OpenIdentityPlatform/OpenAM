/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.upgrade;

import static org.forgerock.openam.setup.EmbeddedOpenDJManager.OPENDJ_DIR;
import static org.forgerock.openam.utils.Time.newDate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;

import com.sun.identity.setup.ConfiguratorException;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.setup.ZipUtils;

/**
 * Manager for creating embedded OpenDJ backup directories and performing embedded OpenDJ backup.
 */
public class EmbeddedOpenDJBackupManager {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

    private final Debug logger;
    private final ZipUtils zipUtils;
    private final String baseDirectory;

    /**
     * Constructs a new {@code EmbeddedOpenDJBackupManager} instance.
     *
     * @param logger logger A {@link Debug} instance.
     * @param zipUtils An instance of the {@link ZipUtils}.
     * @param baseDirectory The base configuration directory for OpenAM.
     */
    public EmbeddedOpenDJBackupManager(Debug logger, ZipUtils zipUtils, String baseDirectory) {
        this.logger = logger;
        this.zipUtils = zipUtils;
        this.baseDirectory = baseDirectory;
    }

    /**
     * Performs a backup of the embedded OpenDJ instance, including creating the required
     * upgrade and backup directories.
     *
     * @throws ConfiguratorException If the embedded OpenDJ could not be backed up.
     */
    void createOpenDJBackup() throws ConfiguratorException {
        createBackupDirectories();
        File backupFile = getBackupFileLocation();
        File opendjDirectory = new File(baseDirectory, OPENDJ_DIR);
        try {
            zipUtils.zipDirectory(opendjDirectory, backupFile);
        } catch (IOException e) {
            throw new ConfiguratorException("Failed to create OpenDJ backup: " + e.getMessage());
        }
    }

    /**
     * Creates the OpenDJ backup directories, {@literal upgrade} and {@literal backups}.
     *
     * @throws ConfiguratorException If the OpenDJ backup directories could not be created.
     */
    public void createBackupDirectories() throws ConfiguratorException {
        try {
            createBackupDirectory("upgrade");
            createBackupDirectory("backups");
        } catch (ConfiguratorException e) {
            throw new ConfiguratorException("Upgrade cannot create backup directory. " + e.getMessage());
        }
    }

    private void createBackupDirectory(String name) throws ConfiguratorException {
        File directory = new File(baseDirectory, name);
        if (directory.exists() && directory.isFile()) {
            throw new ConfiguratorException("Directory:" + directory
                    + " cannot be created as file of the same name already exists");
        }
        if (!directory.exists()) {
            if (!directory.mkdir()) {
                throw new ConfiguratorException("Unable to create directory: " + directory);
            }
            logger.message("Created embedded OpenDJ upgrade directory: {}", directory);
        } else if (!directory.canWrite() && !directory.setWritable(true)) {
            throw new ConfiguratorException("Unable to make " + directory + " directory writable");
        }
    }

    private File getBackupFileLocation() throws ConfiguratorException {
        String backupFilename = "opendj.backup." + DATE_FORMAT.format(newDate()) + ".zip";
        File backupFile = Paths.get(baseDirectory, "backups", backupFilename).toFile();
        if (backupFile.exists()) {
            throw new ConfiguratorException("Upgrade cannot continue as backup file already exists! " + backupFile);
        }
        return backupFile;
    }
}

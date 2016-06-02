/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2015 ForgeRock AS.
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
 *
 * Portions Copyrighted 2014 Nomura Research Institute, Ltd
 */
package org.forgerock.openam.upgrade;

import com.sun.identity.shared.debug.Debug;

import java.io.File;

/**
 * Utility class that deals with creating directories for the upgrade process.
 *
 * @since 13.0.0
 */
public class UpgradeDirectoryUtils {

    private static final Debug DEBUG = Debug.getInstance("amUpgrade");

    private UpgradeDirectoryUtils() {
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
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Created directory: " + dirName);
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
}

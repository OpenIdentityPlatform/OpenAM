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
 * Copyright 2015 ForgeRock AS.
 */

package com.sun.identity.setup;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletContext;

import org.forgerock.openam.upgrade.VersionUtils;

/**
 * Manages the setup process and the associated state during the setup and upgrade processes.
 *
 * @since 13.0.0
 */
@Singleton
class AMSetupManager {

    private final ServletContext servletContext;

    /**
     * Constructs a new AMSetupManager.
     *
     * @param servletContext The {@link ServletContext}.
     */
    @Inject
    AMSetupManager(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    /**
     * <p>Determines if OpenAM is configured.</p>
     *
     * Invoked from the {@link AMSetupFilter} to decide which page needs to be displayed.
     *
     * @return {@code true} if OpenAM is already configured, {@code false} otherwise.
     */
    boolean isConfigured() {
        return AMSetupServlet.isConfigured(servletContext);
    }

    /**
     * Determines if OpenAM is configured with the latest valid configuration.
     *
     * @return {@code true} if the OpenAM configuration is valid.
     */
    boolean isCurrentConfigurationValid() {
        return AMSetupServlet.isCurrentConfigurationValid();
    }

    /**
     * Returns the location of the bootsrap file.
     *
     * @return The location of the bootstrap file or {@code null} if the file cannot be located.
     * @throws ConfiguratorException If the deployment application real path cannot be determined.
     */
    String getBootStrapFileLocation() {
        return AMSetupServlet.getBootStrapFile();
    }

    /**
     * Determines if the upgrade process is complete.
     *
     * @return {@code true} if the upgrade process is complete, {@code false} otherwise.
     */
    boolean isUpgradeCompleted() {
        return AMSetupServlet.isUpgradeCompleted();
    }

    /**
     * Determines if the deployed version of OpenAM is newer than the current OpenAM configuration.
     *
     * @return {@code true} if the deployed version of OpenAM is newer, {@code false} otherwise.
     */
    boolean isVersionNewer() {
        return VersionUtils.isVersionNewer();
    }

    /**
     * Gets the user home directory where the bootstrap directories and files will be be written.
     *
     * @return The user home directory.
     */
    File getUserHomeDirectory() {
        return new File(System.getProperty("user.home"));
    }
}

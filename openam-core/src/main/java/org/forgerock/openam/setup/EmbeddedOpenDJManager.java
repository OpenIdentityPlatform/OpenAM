/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AMSetupServlet.java,v 1.117 2010/01/20 17:01:35 veiming Exp $
 *
 * Portions Copyrighted 2010-2016 ForgeRock AS.
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package org.forgerock.openam.setup;

import jakarta.servlet.ServletException;
import java.io.File;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.upgrade.OpenDJUpgrader;

/**
 * Manager for determining the state of the embedded OpenDJ instance and upgrading it as necessary.
 *
 * <p>The state of the embedded OpenDJ instance is determined on construction and is available for
 * introspection by calling the {@link #getState()} method.</p>
 *
 * <p>The embedded OpenDJ instance can be upgraded by calling the {@link #upgrade()} method. Note
 * that this method should not be called unless the {@link #getState()} method returns
 * {@link State#UPGRADE_REQUIRED}.</p>
 */
public class EmbeddedOpenDJManager {

    public static final String OPENDJ_DIR = "opends";

    private final Debug logger;
    private final String baseDirectory;
    private final OpenDJUpgrader upgrader;

    private State state = State.NO_EMBEDDED_INSTANCE;

    /**
     * Constructs a new {@code EmbeddedOpenDJManager} instance.
     *
     * @param logger A {@link Debug} instance.
     * @param baseDirectory The base configuration directory for OpenAM.
     * @param upgrader An instance of the {@link OpenDJUpgrader}.
     */
    public EmbeddedOpenDJManager(Debug logger, String baseDirectory, OpenDJUpgrader upgrader) {
        this.logger = logger;
        this.baseDirectory = baseDirectory;
        this.upgrader = upgrader;
        init();
    }

    private void init() {
        boolean isEmbeddedOpenDJPresent = new File(baseDirectory, OPENDJ_DIR).exists();
        if (isEmbeddedOpenDJPresent) {
            if (upgrader.isUpgradeRequired()) {
                state = State.UPGRADE_REQUIRED;
            } else {
                state = State.CONFIGURED;
            }
        }
    }

    /**
     * Gets the state the embedded OpenDJ instance is in.
     *
     * @return The current state.
     */
    public State getState() {
        return state;
    }

    /**
     * Performs an upgrade of the embedded OpenDJ instance.
     *
     * <p>This method should not be called unless the {@link #getState()} method returns
     * {@link State#UPGRADE_REQUIRED}.</p>
     *
     * @return The state of the embedded OpenDJ instance after upgrade.
     * @throws ServletException If the embedded OpenDJ instance could not be upgraded.
     * @throws IllegalStateException If the embedded OpenDJ instance does not require upgrading.
     */
    public State upgrade() throws ServletException {
        if (state.equals(State.NO_EMBEDDED_INSTANCE)) {
            throw new IllegalStateException("Cannot upgrade embedded instance as no embedded instance has "
                    + "been configured");
        }
        if (!state.equals(State.UPGRADE_REQUIRED)) {
            throw new IllegalStateException("Embedded instance does not require upgrading");
        }
        try {
            upgrader.upgrade();
            state = State.UPGRADED;
            return state;
        } catch (Exception e) {
            logger.error("Failed to upgrade OpenDJ", e);
            throw new ServletException("An error occurred while upgrading the embedded OpenDJ instance", e);
        }
    }

    /**
     * The possible states the embedded OpenDJ instance can be in.
     */
    public enum State {
        NO_EMBEDDED_INSTANCE,
        UPGRADE_REQUIRED,
        UPGRADED,
        CONFIGURED
    }
}

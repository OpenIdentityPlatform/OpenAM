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
package org.forgerock.openam.selfservice.config.flows;

import org.forgerock.selfservice.core.config.StageConfig;

/**
 * Auto login stage configuration.
 *
 * @since 13.5.0
 */
final class AutoLoginStageConfig implements StageConfig {

    /**
     * Stage name.
     */
    public static final String NAME = "autoLoginStage";

    private String realm;

    /**
     * Gets the configured realm.
     *
     * @return the realm
     */
    public String getRealm() {
        return realm;
    }

    /**
     * Sets the realm.
     *
     * @param realm
     *         the realm
     *
     * @return this config
     */
    public AutoLoginStageConfig setRealm(String realm) {
        this.realm = realm;
        return this;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getProgressStageClassName() {
        return AutoLoginStage.class.getName();
    }

}

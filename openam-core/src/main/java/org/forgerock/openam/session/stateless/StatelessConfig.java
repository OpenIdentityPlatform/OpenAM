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
 * Portions copyright 2025 3A Systems LLC.
 */
package org.forgerock.openam.session.stateless;

import com.iplanet.am.util.SystemPropertiesWrapper;

import jakarta.inject.Inject;

/**
 * Responsible for providing configuration to all parts of the Stateless Session code base.
 */
public class StatelessConfig {
    private final SystemPropertiesWrapper properties;

    @Inject
    public StatelessConfig(SystemPropertiesWrapper properties) {
        this.properties = properties;
    }

    /**
     * @see org.forgerock.openam.session.stateless.StatelessConstants#STATELESS_JWT_CACHE_MAX_SIZE
     * @return The default JWT Cache size, or another size as defined by the user.
     */
    public int getJWTCacheSize() {
        return properties.getAsInt(StatelessConstants.STATELESS_JWT_CACHE_MAX_SIZE, 10000);
    }
}

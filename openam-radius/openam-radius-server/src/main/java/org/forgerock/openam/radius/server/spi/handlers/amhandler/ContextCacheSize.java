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
/**
 *
 */
package org.forgerock.openam.radius.server.spi.handlers.amhandler;

import com.iplanet.am.util.SystemProperties;

/**
 * Class to wrap up the calculation of the ContextCache size. As this is based on a system property
 * it can change through out the lifetime of the RadiusServer. Encapsulating this in a class allows
 * optimization of how the size is determined without affecting the OpenAMAuthHandler.
 *
 */
public class ContextCacheSize {

    private int desiredCacheSize;

    /**
     * Key used to obtain the maximum number of allowable concurrent sessions.
     */
    private static final String SYSTEM_MAX_PROPS_KEY = "com.iplanet.am.session.maxSessions";


    /**
     * Returns the desired CONTEXT_CACHE size for the OpenAMAuthHandler.
     *
     * @return the desired CONTEXT_CACHE size for the OpenAMAuthHandler.
     */
    public synchronized int getDesiredCacheSize() {
        desiredCacheSize = SystemProperties.getAsInt(SYSTEM_MAX_PROPS_KEY, 5000) / 2;
        return desiredCacheSize;
    }
}

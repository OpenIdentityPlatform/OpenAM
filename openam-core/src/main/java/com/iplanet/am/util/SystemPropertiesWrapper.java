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
package com.iplanet.am.util;

/**
 * Wrapper which defines a simple dependency injectable dependency for code wishing to access
 * system properties.
 */
public class SystemPropertiesWrapper {
    /**
     * Wraps the call to {@link com.iplanet.am.util.SystemProperties#getAsInt(String, int)}.
     *
     * @param key System Properties key to retrieve the value for.
     * @param def Default if no value is found.
     * @return Possibly null string.
     */
    public int getAsInt(String key, int def) {
        return SystemProperties.getAsInt(key, def);
    }
}

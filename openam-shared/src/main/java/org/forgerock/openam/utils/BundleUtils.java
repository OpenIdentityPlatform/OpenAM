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

package org.forgerock.openam.utils;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Utility methods for dealing with {@code ResourceBundle}s.
 */
public final class BundleUtils {

    /**
     * Try to get a value from the bundle, and if it doesn't exist, return a default.
     * @param bundle The bundle.
     * @param key The key.
     * @param defaultValue The default value.
     * @return The bundle's value for the given key, or {@code defaultValue} if there is no value.
     */
    public static String getStringWithDefault(ResourceBundle bundle, String key, String defaultValue) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return defaultValue;
        }

    }

    private BundleUtils() {

    }
}

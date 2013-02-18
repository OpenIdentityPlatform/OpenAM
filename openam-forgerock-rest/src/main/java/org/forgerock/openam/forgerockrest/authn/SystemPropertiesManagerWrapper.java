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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openam.forgerockrest.authn;

import com.sun.identity.shared.configuration.SystemPropertiesManager;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Simple wrapper for SystemPropertiesManager so calls to it can be mocked out in tests.
 */
@Singleton
public class SystemPropertiesManagerWrapper {

    /**
     * Constructs an instance of the SystemPropertiesManagerWrapper.
     */
    @Inject
    public SystemPropertiesManagerWrapper() {
    }

    /**
     * Returns property string.
     *
     * @param key Key of the property.
     * @return property string.
     */
    public String get(String key) {
        return SystemPropertiesManager.get(key);
    }

    /**
     * Returns property string.
     *
     * @param key Key of the property.
     * @param defaultValue Default value if the property is not found.
     * @return property string.
     */
    public String get(String key, String defaultValue) {
        return SystemPropertiesManager.get(key, defaultValue);
    }
}

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
package org.forgerock.openam.scripting.service;

import org.forgerock.openam.scripting.ScriptException;

import javax.security.auth.Subject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Temporary class to represent a mock script configuration service.
 */
public class ScriptConfigurationServiceMockImpl implements ScriptConfigurationService {

    public static final Map<String, ScriptConfiguration> configStore = new HashMap<String, ScriptConfiguration>();

    @Override
    public ScriptConfiguration saveScriptConfiguration(Subject subject, String realm,
                                                       ScriptConfiguration config) throws ScriptException {
        configStore.put(config.getUuid(), config);
        return config;
    }

    @Override
    public void deleteScriptConfiguration(Subject subject, String realm, String uuid) throws ScriptException {
        if (!configStore.containsKey(uuid)) {
            throw new ScriptException("Script with UUID '" + uuid + "' could not be found.");
        }
        configStore.remove(uuid);
    }

    @Override
    public Set<ScriptConfiguration> getScriptConfigurations(Subject subject, String realm)
            throws ScriptException {

        return new HashSet<ScriptConfiguration>(configStore.values());
    }

    @Override
    public ScriptConfiguration getScriptConfiguration(Subject subject, String realm, String uuid)
            throws ScriptException {

        if (!configStore.containsKey(uuid)) {
            throw new ScriptException("Script with UUID '" + uuid + "' could not be found.");
        }
        return configStore.get(uuid);
    }

    @Override
    public ScriptConfiguration updateScriptConfiguration(Subject subject, String realm,
                                                         ScriptConfiguration config) throws ScriptException {
        if (!configStore.containsKey(config.getUuid())) {
            throw new ScriptException("Script with UUID '" + config.getUuid() + "' could not be found.");
        }
        return configStore.put(config.getUuid(), config);
    }
}

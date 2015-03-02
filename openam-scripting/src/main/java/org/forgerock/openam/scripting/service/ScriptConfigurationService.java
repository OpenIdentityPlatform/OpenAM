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
import java.util.Set;

/**
 * The {@code ScriptingService} is responsible for access to the persisted {@code ScriptConfiguration}
 * instances. It is the layer on top of the {@code ScriptingStore}, which is responsible for access
 * to all the persisted scripting related data.
 *
 * @since 13.0.0
 */
public interface ScriptConfigurationService {

    /**
     * Save the script configuration in the data store under the script's realm.
     * @param subject The subject with privilege to create scripts.
     * @param realm The realm in which to create the script.
     * @param config The script configuration to save.
     * @return The saved script configuration.
     */
    public ScriptConfiguration saveScriptConfiguration(Subject subject, String realm, ScriptConfiguration config)
            throws ScriptException;

    /**
     * Delete the script with the given UUID stored under the given realm from the data store.
     * @param subject The subject with privilege to delete scripts in this realm.
     * @param realm The realm from which to delete the script.
     * @param uuid The unique identifier for the script.
     */
    public void deleteScriptConfiguration(Subject subject, String realm, String uuid) throws ScriptException;

    /**
     * Retrieve the scripts stored under the specified realm from the data store.
     * @param subject The subject with privilege to access the scripts in this realm.
     * @param realm The realm from which to retrieve the scripts.
     * @return A set of script configurations.
     */
    public Set<ScriptConfiguration> getScriptConfigurations(Subject subject, String realm) throws ScriptException;

    /**
     * Retrieve the script stored under the specified realm from the data store.
     * @param subject The subject with privilege to access the script in this realm.
     * @param realm The realm from which to retrieve the script.
     * @return The script with the given UUID or null if it cannot be found.
     */
    public ScriptConfiguration getScriptConfiguration(Subject subject, String realm, String uuid) throws ScriptException;

    /**
     * Update the given script.
     * @param subject The subject with privilege to update the script.
     * @param realm The realm in which to update the script.
     * @param config The script to update.
     * @return The updated resource type.
     */
    public ScriptConfiguration updateScriptConfiguration(Subject subject, String realm, ScriptConfiguration config)
            throws ScriptException;
}

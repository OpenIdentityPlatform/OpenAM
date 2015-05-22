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

import org.forgerock.util.query.QueryFilter;
import org.forgerock.openam.scripting.ScriptException;

import java.util.Set;

/**
 * An interface for access to the persisted instances of the provided type. It is the layer on top of the
 * {@code ScriptingDataStore}, which is responsible for access to all the persisted scripting related data.
 *
 * @since 13.0.0
 */
public interface ScriptingService<T> {

    /**
     * Create the scripting configuration and persist it.
     * @param config The configuration to create and persist.
     * @return The created configuration object.
     * @throws ScriptException if the operation was not successful
     */
    public T create(T config) throws ScriptException;

    /**
     * Delete the configuration with the given UUID.
     * @param uuid The unique identifier for the configuration.
     * @throws ScriptException if the operation was not successful
     */
    public void delete(String uuid) throws ScriptException;

    /**
     * Retrieve the stored configuration.
     * @return A set of configuration objects.
     * @throws ScriptException if the operation was not successful
     */
    public Set<T> getAll() throws ScriptException;

    /**
     * Retrieve the stored configuration.
     * @return The configuration object with the given UUID or null if it cannot be found.
     * @throws ScriptException if the operation was not successful
     */
    public Set<T> get(QueryFilter<String> queryFilter) throws ScriptException;

    /**
     * Retrieve the stored configuration.
     * @return The configuration object with the given UUID or null if it cannot be found.
     * @throws ScriptException if the operation was not successful
     */
    public T get(String uuid) throws ScriptException;

    /**
     * Update the given scripting configuration and persist it.
     * @param config The configuration to update and persist.
     * @return The updated configuration object.
     * @throws ScriptException if the operation was not successful
     */
    public T update(T config) throws ScriptException;

}

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
package org.forgerock.openam.scripting.datastore;

import org.forgerock.util.query.QueryFilter;
import org.forgerock.openam.scripting.ScriptException;

import java.util.Set;

/**
 * An interface for managing script related data in the data store.
 *
 * @since 13.0.0
 */
public interface ScriptingDataStore<T> {

    /**
     * Save the scripting data in the data store.
     * @param data The data to save.
     * @throws ScriptException if the operation was not successful
     */
    public void save(T data) throws ScriptException;

    /**
     * Delete the data with the given UUID from the data store.
     * @param uuid The unique identifier for the data.
     * @throws ScriptException if the operation was not successful
     */
    public void delete(String uuid) throws ScriptException;

    /**
     * Retrieve the data stored from the data store.
     * @return A set of data objects.
     * @throws ScriptException if the operation was not successful
     */
    public Set<T> getAll() throws ScriptException;

    /**
     * Retrieve the data stored from the data store.
     * @return The data object with the given UUID or null if it cannot be found.
     * @throws ScriptException if the operation was not successful
     */
    public T get(String uuid) throws ScriptException;

    /**
     * Check to see if data with the given UUID already.
     * @param uuid The unique identifier for the data.
     * @return True if the data already exists.
     * @throws ScriptException if the operation was not successful
     */
    public boolean containsUuid(String uuid) throws ScriptException;

    /**
     * Check to see if data with the given name already exists.
     * @param name The name for the data.
     * @return True if the data already exists.
     * @throws ScriptException if the operation was not successful
     */
    public boolean containsName(String name) throws ScriptException;

    /**
     * Retrieve the data stored from the data store based on the given query filter.
     * @param filter the query filter.
     * @return a set of matching data objects.
     * @throws ScriptException should an error occur during lookup
     */
    public Set<T> get(QueryFilter<String> filter) throws ScriptException;
}

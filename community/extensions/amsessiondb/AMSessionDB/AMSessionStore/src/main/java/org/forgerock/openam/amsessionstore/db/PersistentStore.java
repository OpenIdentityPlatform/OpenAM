/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package org.forgerock.openam.amsessionstore.db;

import java.util.Map;
import java.util.Set;
import org.forgerock.openam.amsessionstore.common.AMRecord;

/**
 * This interface defines the functionality of the pluggable persistent stores
 * 
 * @author steve
 */
public interface PersistentStore {
    /**
     * Takes an AMRecord and writes this to the store
     * 
     * @param record The record to store
     * @throws StoreException 
     */
    public void write(AMRecord record) throws StoreException;
    
    /**
     * Reads a record from the store with the specified id
     * 
     * @param id The primary key of the record to find
     * @return The AMRecord if found
     * @throws StoreException
     * @throws NotFoundException 
     */
    public AMRecord read(String id) throws StoreException, NotFoundException;
    
    /**
     * Reads a record with the secondary key from the underlying store. The
     * return value is a <code><Set>String</code>. Each string represents the 
     * token id of the matching session found.
     * 
     * @param id The secondary key on which to search the store
     * @return A Set of Strings of the matching records, if any.
     * @throws StoreException
     * @throws NotFoundException 
     */
    public Set<String> readWithSecKey(String id) throws StoreException, NotFoundException;
    
    /**
     * Deletes a record from the store.
     * 
     * @param id The id of the record to delete from the store
     * @throws StoreException
     * @throws NotFoundException 
     */
    public void delete(String id) throws StoreException, NotFoundException;
    
    /**
     * Delete all records in the store
     * that have an expiry date older than the one specified.
     * 
     * @param expDate The expDate in seconds
     * @throws StoreException 
     */
    public void deleteExpired(long expDate) throws StoreException;
    
    /**
     * Shut down the store
     */
    public void shutdown();
    
    /**
     * Returns the count of the records found in the store with a given matching
     * id. 
     * 
     * The return value is <code>Map<String, Long></code> where the key is the 
     * token.id of the users session and the long is the expiry time of the session.
     * 
     * @param id
     * @return
     * @throws StoreException 
     */
    public Map<String, Long> getRecordCount(String id) throws StoreException;
    
    /**
     * Returns the current set of store statistics.
     * 
     * @return 
     */
    public DBStatistics getDBStatistics();
}

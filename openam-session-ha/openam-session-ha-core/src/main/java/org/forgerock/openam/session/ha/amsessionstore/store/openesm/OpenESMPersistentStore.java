/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Portions Copyrighted [2010-2012] [ForgeRock AS]
 *
 */

package org.forgerock.openam.session.ha.amsessionstore.store.openesm;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.exceptions.NotFoundException;
import com.iplanet.dpro.session.exceptions.StoreException;
import com.iplanet.dpro.session.service.AMSessionRepository;
import com.iplanet.dpro.session.service.InternalSession;
import com.sun.identity.common.GeneralTaskRunnable;
import org.forgerock.openam.session.model.*;

import java.util.*;


/**
 * Provide Implementation of AMSessionRepository using the External
 * Enterprise Session Manager to handle the persistence for the
 *
 * This allows for the Session Management and Replication Aspects to be
 * in a secondary instance.
 *
 * @author steve
 * @author jeff.schenk@forgerock.com
 */
public class OpenESMPersistentStore extends GeneralTaskRunnable implements AMSessionRepository {


    // TODO -- Class is a placeholder for a possible OpenESM. (Enterprise Session Manager)


    /**
     * Retrieves session state from the repository.
     *
     * @param sid session ID
     * @return <code>InternalSession</code> object retrieved from the repository
     * @throws Exception if anything goes wrong
     */
    @Override
    public InternalSession retrieve(SessionID sid) throws Exception {
        return null;  // TODO -- Implement
    }

    /**
     * Saves session state to the repository If it is a new session (version ==
     * 0) it inserts new record(s) to the repository. For an existing session it
     * updates repository record instead while checking that versions in the
     * InternalSession and in the record match In case of version mismatch or
     * missing record IllegalArgumentException will be thrown
     *
     * @param is reference to <code>InternalSession</code> object being saved.
     * @throws Exception if anything goes wrong.
     */
    @Override
    public void save(InternalSession is) throws Exception {
        // TODO -- Implement
    }

    /**
     * Deletes session record from the repository.
     *
     * @param sid session ID.
     * @throws Exception if anything goes wrong.
     */
    @Override
    public void delete(SessionID sid) throws Exception {
        // TODO -- Implement
    }

    /**
     * Deletes the expired session records , this is mainly used by the
     * background clean up thread to cleanup the expired session records from
     * the <code>Sessionrepository</code>
     *
     * @throws Exception when unable to deleted the session record from the
     *                   repository
     */
    @Override
    public void deleteExpired() throws Exception {
        // TODO -- Implement
    }

    /**
     * Deletes a record from the store.
     *
     * @param id The id of the record to delete from the store
     * @throws com.iplanet.dpro.session.exceptions.StoreException
     *
     * @throws com.iplanet.dpro.session.exceptions.NotFoundException
     *
     */
    @Override
    public void delete(String id) throws StoreException, NotFoundException {
        // TODO -- Implement
    }

    /**
     * Delete all records in the store
     * that have an expiry date older than the one specified.
     *
     * @param expDate The expDate in seconds
     * @throws com.iplanet.dpro.session.exceptions.StoreException
     *
     */
    @Override
    public void deleteExpired(long expDate) throws StoreException {
        // TODO -- Implement
    }

    /**
     * Returns the expiration information of all sessions belonging to a user.
     * The returned value will be a Map (sid->expiration_time).
     *
     * @param uuid User's universal unique ID.
     * @throws Exception if there is any problem with accessing the session
     *                   repository.
     */
    @Override
    public Map getSessionsByUUID(String uuid) throws Exception {
        return null;  // TODO -- Implement
    }

    /**
     * Takes an AMRecord and writes this to the store
     *
     * @param amRootEntity The record object to store
     * @throws com.iplanet.dpro.session.exceptions.StoreException
     *
     */
    @Override
    public void write(AMRootEntity amRootEntity) throws StoreException {
        // TODO -- Implement
    }

    /**
     * Reads a record from the store with the specified id
     *
     * @param id The primary key of the record to find
     * @return AMRootEntity The AMRecord if found
     * @throws com.iplanet.dpro.session.exceptions.StoreException
     *
     * @throws com.iplanet.dpro.session.exceptions.NotFoundException
     *
     */
    @Override
    public AMRootEntity read(String id) throws StoreException, NotFoundException {
        return null;  // TODO -- Implement
    }

    /**
     * Reads a record with the secondary key from the underlying store. The
     * return value is a <code><Set>String</code>. Each string represents the
     * token id of the matching session found.
     *
     * @param id The secondary key on which to search the store
     * @return A Set of Strings of the matching records, if any.
     * @throws com.iplanet.dpro.session.exceptions.StoreException
     *
     * @throws com.iplanet.dpro.session.exceptions.NotFoundException
     *
     */
    @Override
    public Set<String> readWithSecKey(String id) throws StoreException, NotFoundException {
        return null;  // TODO -- Implement
    }

    /**
     * Shut down the store
     */
    @Override
    public void shutdown() {
        // TODO -- Implement
    }

    /**
     * Returns the count of the records found in the store with a given matching
     * id.
     * <p/>
     * The return value is <code>Map<String, Long></code> where the key is the
     * token.id of the users session and the long is the expiry time of the session.
     *
     * @param id
     * @return
     * @throws com.iplanet.dpro.session.exceptions.StoreException
     *
     */
    @Override
    public Map<String, Long> getRecordCount(String id) throws StoreException {
        return null;  // TODO -- Implement
    }

    /**
     * Returns the current set of store statistics.
     *
     * @return DBStatistics
     */
    @Override
    public DBStatistics getDBStatistics() {
        return null;  // TODO -- Implement
    }

    /**
     * Adds an element to this TaskRunnable.
     *
     * @param key Element to be added to this TaskRunnable
     * @return a boolean to indicate whether the add success
     */
    @Override
    public boolean addElement(Object key) {
        return false;  // TODO -- Implement
    }

    /**
     * Removes an element from this TaskRunnable.
     *
     * @param key Element to be removed from this TaskRunnable
     * @return A boolean to indicate whether the remove success
     */
    @Override
    public boolean removeElement(Object key) {
        return false;  // TODO -- Implement
    }

    /**
     * Indicates whether this TaskRunnable is empty.
     *
     * @return A boolean to indicate whether this TaskRunnable is empty
     */
    @Override
    public boolean isEmpty() {
        return false;  // TODO -- Implement
    }

    /**
     * Returns the run period of this TaskRunnable.
     *
     * @return A long value to indicate the run period
     */
    @Override
    public long getRunPeriod() {
        return 0;  // TODO -- Implement
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p/>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        // TODO -- Implement
    }

}

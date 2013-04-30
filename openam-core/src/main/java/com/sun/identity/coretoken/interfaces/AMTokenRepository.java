/* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: AMSessionRepository.java,v 1.4 2008/06/25 05:41:30 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted 2012-2013 ForgeRock, Inc.
 */
package com.sun.identity.coretoken.interfaces;

import java.util.Calendar;
import java.util.Map;
import java.util.Set;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.exceptions.NotFoundException;
import com.iplanet.dpro.session.exceptions.StoreException;

import com.iplanet.dpro.session.service.InternalSession;
import org.forgerock.openam.session.model.AMRootEntity;
import org.forgerock.openam.session.model.DBStatistics;

/**
 * The <code>AMTokenRepository</code> interface provides methods to
 * <code>retrieve</code> , <code>save</code> , <code>delete</code> the session
 * from the session repository.
 *
 * @see <code>CTSPersistentStore</code>
 *
 */
public interface AMTokenRepository {

    static final String DEBUG_NAME = "amSessionRepository";

    static final String CTS_REPOSITORY_CLASS_PROPERTY =
            "com.sun.am.session.SessionRepositoryImpl";

    static final String IS_SFO_ENABLED =
                "iplanet-am-session-sfo-enabled";

    static final String SYS_PROPERTY_SM_CONFIG_ROOT_SUFFIX =
            "iplanet-am-config-root-suffix";

    static final String SYS_PROPERTY_SESSION_HA_REPOSITORY_ROOT_SUFFIX =
            "iplanet-am-session-sfo-store-root-suffix";

    static final String SYS_PROPERTY_SESSION_HA_REPOSITORY_TYPE =
            "iplanet-am-session-sfo-store-type";

    static final String SYS_PROPERTY_TOKEN_ROOT_SUFFIX =
            "iplanet-am-config-token-root-suffix";

    static final String CLEANUP_GRACE_PERIOD =
            "com.sun.identity.session.repository.cleanupGracePeriod";

    static final String SYS_PROPERTY_EXPIRED_SEARCH_LIMIT =
            "forgerock-openam-session-expired-search-limit";

    /**
     * Retrieves session state from the repository.
     *
     * @param sid session ID
     * @return <code>InternalSession</code> object retrieved from the repository
     * @throws Exception if anything goes wrong
     */
    public InternalSession retrieve(SessionID sid) throws Exception;

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
    public void save(InternalSession is) throws Exception;

    /**
     * Deletes session record from the repository.
     *
     * @param sid session ID.
     * @throws Exception if anything goes wrong.
     */
    public void delete(SessionID sid) throws Exception;

    /**
     * Deletes the expired session records , this is mainly used by the
     * background clean up thread to cleanup the expired session records from
     * the <code>Sessionrepository</code>
     *
     * @throws Exception when unable to deleted the session record from the
     *                   repository
     */
    public void deleteExpired() throws Exception;

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
     * @param expirationDate The Calendar Entry depicting the time in which all existing Session
     *                       objects should be deleted if less than this time.
     * @throws StoreException
     */
    public void deleteExpired(Calendar expirationDate) throws StoreException;


    /**
     * Returns the expiration information of all sessions belonging to a user.
     * The returned value will be a Map (sid->expiration_time).
     *
     * @param uuid User's universal unique ID.
     * @throws Exception if there is any problem with accessing the session
     *                   repository.
     */
    public Map<String, Long> getSessionsByUUID(String uuid) throws Exception;

    /**
     * Merge of additional methods from PersistentStore Class for new session-ha.
     */


    /**
     * Takes an AMRecord and writes this to the store
     *
     * @param amRootEntity The record object to store
     * @throws StoreException
     */
    public void write(AMRootEntity amRootEntity) throws StoreException;

    /**
     * Reads a record from the store with the specified id
     *
     * @param id The primary key of the record to find
     * @return AMRootEntity The AMRecord if found
     * @throws StoreException
     * @throws NotFoundException
     */
    public AMRootEntity read(String id) throws StoreException, NotFoundException;

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
     * Shut down the store
     */
    public void shutdown();

    /**
     * Returns the count of the records found in the store with a given matching
     * id.
     * <p/>
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
     * @return DBStatistics
     */
    public DBStatistics getDBStatistics();

}

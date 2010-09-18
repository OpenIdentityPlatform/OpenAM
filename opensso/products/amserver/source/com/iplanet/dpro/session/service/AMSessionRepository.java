/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
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

package com.iplanet.dpro.session.service;

import java.util.Map;

import com.iplanet.dpro.session.SessionID;

 /**
  * The <code>AMSessionRepository</code> interface provides methods to
  * <code>retrive</code> , <code>save</code> , <code>delete</code> the session
  * from the session repository.Any </code>Session<code> repository mechanisms
  * such as <code>JDBCSessionRepository</code>,
  I <code> JMQSessionRepository</code> implements this interface.
  *
  * @see com.iplanet.dpro.session.jdbc.JDBCSessionRepository
  * @see com.iplanet.dpro.session.JMQSessionRepository
  */ 
public interface AMSessionRepository {

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
     * repository
     */
    public void deleteExpired() throws Exception;

    /**
     * Returns the expiration information of all sessions belonging to a user.
     * The returned value will be a Map (sid->expiration_time).
     * 
     * @param uuid User's universal unique ID.
     * @throws Exception if there is any problem with accessing the session
     *         repository.
     */
    public Map getSessionsByUUID(String uuid) throws Exception;
}

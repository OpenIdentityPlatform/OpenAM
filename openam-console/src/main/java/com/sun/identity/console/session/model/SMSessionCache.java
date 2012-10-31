/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SMSessionCache.java,v 1.3 2008/07/10 23:27:24 veiming Exp $
 *
 */

package com.sun.identity.console.session.model;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/* - NEED NOT LOG - */

/** 
 * This class caches session information.
 */
public class SMSessionCache
    implements Serializable 
{
    private static Debug debug = SMProfileModelImpl.debug;

    private String searchErrorMessage;
    private List sessions = new ArrayList();

    /**
     * Constructs an instance of <code>SMSessionCache</code>.
     *
     * @param sessions Collection of sessions.
     * @param searchErrorMsg Search Error Message.
     * @param modelImpl Session Profile Implementation class.
     */
    public SMSessionCache(
        Collection sessions,
        String searchErrorMsg,
        SMProfileModelImpl modelImpl
    ) {
        searchErrorMessage = searchErrorMsg;
        Map mapSessions = retainSessionsWithUserID(sessions, modelImpl);
        cacheSessions(mapSessions, modelImpl);
    }

    /**
     * Returns search error message.
     *
     * @return search error message.
     */
    public String getErrorMessage() {
        return searchErrorMessage;
    }

    /**
     * Returns a session information, <code>SMSessionData</code>.
     *
     * @return a page of session information, <code>SMSessionData</code>.
     */
    public List getSessions() {
        return sessions;
    }

    private void cacheSessions(Map mapSessions, SMProfileModelImpl modelImpl) {
        List sorted = new ArrayList(mapSessions.keySet());
        int sz = sorted.size();

        for (int i = 0; i < sz; i++) {
            sessions.add(mapSessions.get(sorted.get(i)));
        }
    }

    /**
     * Returns a map of user ID to session object for session that contains
     * user ID.
     */
    private Map retainSessionsWithUserID(
        Collection sessions,
        SMProfileModelImpl modelImpl
    ) {
        Map results = new HashMap(sessions.size() *2);

        for (Iterator iter = sessions.iterator(); iter.hasNext(); ) {
            Session sess = (Session)iter.next();

            try {
                String userId = sess.getProperty(SMProfileModelImpl.USER_ID);

                if (userId != null) {
                    String id = sess.getID().toString();

                    SMSessionData sData = new SMSessionData();
                    sData.setUserId(userId);
                    sData.setId(id);
                    sData.setTimeRemain(sess.getTimeLeft()/60);
                    sData.setMaxSessionTime(sess.getMaxSessionTime());
                    sData.setIdleTime(sess.getIdleTime()/60);
                    sData.setMaxIdleTime(sess.getMaxIdleTime());
                    results.put(userId +id, sData);
                }
            } catch (SessionException se) {
                debug.warning("SMSessionCache.retainSessionsWithUserID", se);
            }
        }

        return results;
    }

}

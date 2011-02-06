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
 * $Id: SessionCount.java,v 1.5 2008/06/25 05:41:31 qcheng Exp $
 *
 */

package com.iplanet.dpro.session.service;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.share.SessionRequest;
import com.iplanet.dpro.session.share.SessionResponse;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.session.util.RestrictedTokenContext;

 
 /**
  * <code>SessionCount</code> represents the session count for a given user
  * in 3 different mutually exclusive deployment modes from which the user 
  * sessions can be obtained.
  *
  * <ul>
  * <li> SINGLE_SERVER_MODE : 
  *I    Simply return the local sessions for the given user.
  * <li> MULTI_SERVER_MODE :  
  * Query other AM servers for the sessions for the  given user Add the numbers 
  * up (including the local one) and return the value. If any of the AM servers 
  * is down, simply ignors it since all the sessions maintained by that instance
  * are not available.                        
  * <li> SFO_MODE : Fetch the sessions for the given user directly from the 
  * session repository.
  * </ul>
  */
public class SessionCount {

    // SessionInfoMap: uuid -> Set (list of sids)
    private static Map uuidSessionMap = Collections
            .synchronizedMap(new HashMap());

    /* Single server mode*/
    static final int SINGLE_SERVER_MODE = 1;

    /* Multiserver mode */
    static final int MULTI_SERVER_MODE = 2;

    /* Directly from Session repository*/
    static final int SFO_MODE = 3;

    private static int deploymentMode = 0;

    private static Debug debug = SessionService.sessionDebug;

    private static SSOToken adminToken = null;

    static {
        try {
            SSOTokenManager.getInstance();
        } catch (Exception ssoe) {
            debug.error("SessionConstraint: Failied to get the "
                    + "SSOTokenManager instance.");
        }

        if (getSS().isSiteEnabled()) {
            if (getSS().isSessionFailoverEnabled()) {
                deploymentMode = SFO_MODE;
            } else {
                deploymentMode = MULTI_SERVER_MODE;
            }
        } else {
            deploymentMode = SINGLE_SERVER_MODE;
        }
    }

    static int getDeploymentMode() {

        return deploymentMode;
    }

    static SessionService getSS() {
        SessionService ss = SessionService.getSessionService();
        if (ss == null) {
            debug.error("SessionConstraint: "
                    + " Failed to get the session service instance");
        }
        return ss;
    }

    /**
     * Returns the expiration information of all sessions belonging to a user
     * (uuid). The returned value will be a Map (sid->expiration_time).
     * 
     * @param uuid
     *            User's universal unique ID.
     * @return user Sessions
     * @exception Exception
     *             if there is any problem with accessing the session
     *             repository.
     */
    public static Map getAllSessionsByUUID(String uuid) throws Exception {

       
        Map sessions = null;

        switch (deploymentMode) {
        case SINGLE_SERVER_MODE:
            sessions = getSessionsFromLocalServer(uuid);
            break;
        case MULTI_SERVER_MODE:
            sessions = getSessionsFromPeerServers(uuid);
            break;
        case SFO_MODE:
            sessions = getSessionsFromRepository(uuid);
            break;
        default:
            break;
        }
        return sessions;
    }

    /*
     * Get user sessions from local server
     */
    static Map getSessionsFromLocalServer(String uuid) {

        Set sessions = (Set) uuidSessionMap.get(uuid);
        Map retSessions = new HashMap();

        if (sessions != null) {
            synchronized (sessions) {
                Iterator i = sessions.iterator();
                while (i.hasNext()) {
                    SessionID sid = (SessionID) i.next();
                    InternalSession is = getSS().getInternalSession(sid);
                    retSessions.put(sid.toString(), new Long(is
                            .getExpirationTime()));
                }
            }
        }
        return retSessions;
    }

    /*
     * Get user sessions from session repository
     */
    private static Map getSessionsFromPeerServers(String uuid) {

        Map sessions = getSessionsFromLocalServer(uuid);
        String localServerID = getSS().getLocalServerID();

        Set serverIDs = null;
        try {
            serverIDs = WebtopNaming.getSiteNodes(localServerID);
        } catch (Exception e) {
            debug.error("Failed to get the serverIDs from " + "WebtopNaming.",
                    e);
            return sessions;
        }

        for (Iterator m = serverIDs.iterator(); m.hasNext();) {
            String serverID = (String) m.next();
            if (serverID.equals(localServerID)) {
                continue;
            }
            try {
                URL svcurl = Session.getSessionServiceURL(serverID);
                SessionRequest sreq = new SessionRequest(
                        SessionRequest.GetSessionCount, getAdminToken()
                                .getTokenID().toString(), false);
                sreq.setUUID(uuid);
                SessionResponse sres = getSessionResponse(svcurl, sreq);
                sessions.putAll(sres.getSessionsForGivenUUID());
            } catch (SessionException se) {
                if (debug.messageEnabled()) {
                    debug.message("SessionConstraint: "
                            + "peer AM server is down...");
                }
            }
        }
        return sessions;
    }

    private static Map getSessionsFromRepository(String uuid) throws Exception {

        AMSessionRepository repo = SessionService.getSessionService()
                .getRepository();
        Map sessions = null;
        try {
            sessions = repo.getSessionsByUUID(uuid);
        } catch (Exception e) {
            debug.error("SessionCount.getSessionsFromRepository: "+
                "Session repository is not available", e);            
            throw e;
        }
        return sessions;
    }

    /**
     * Increments the session count
     * @param internalsession for the user
     *
     */
    static void incrementSessionCount(InternalSession is) {

        if (deploymentMode == SINGLE_SERVER_MODE) {
            Set sessions = (Set) uuidSessionMap.get(is.getUUID());
            if (sessions != null) {
                sessions.add(is.getID());
            } else {
                sessions = Collections.synchronizedSet(new HashSet());
                sessions.add(is.getID());
                uuidSessionMap.put(is.getUUID(), sessions);
            }
        }
    }

    /**
     * Decrements the session count
     * @param is the <code>InternalSession</code> for the user
     *
     */
    static void decrementSessionCount(InternalSession is) {

        String uuid = is.getUUID();
        SessionID sid = is.getID();

        if (deploymentMode == SINGLE_SERVER_MODE) {
            Set sessions = (Set) uuidSessionMap.get(uuid);
            if (sessions != null) {
                sessions.remove(sid);
                if (sessions.isEmpty()) {
                    uuidSessionMap.remove(uuid);
                }
            }

        }
    }

    private static SessionResponse getSessionResponse(URL svcurl,
            SessionRequest sreq) throws SessionException {

        try {
            Object context = RestrictedTokenContext.getCurrent();
            if (context != null) {
                sreq.setRequester(RestrictedTokenContext.marshal(context));
            }

            SessionResponse sres = Session.sendPLLRequest(svcurl, sreq);
            if (sres.getException() != null) {
                throw new SessionException(sres.getException());
            }
            return sres;
        } catch (SessionException se) {
            throw se;
        } catch (Exception e) {
            throw new SessionException(e);
        }
    }

   /*
    * Gets the admin token for checking the session constraints for the users
    * @return admin <code>SSOTken</code>
    */
    static SSOToken getAdminToken() {

        if (adminToken == null) {
            try {
                adminToken = getSS().getSessionServiceToken();
            } catch (Exception e) {
                debug.error("Failed to get the admin token for "
                        + "Session constraint checking.", e);
            }
        }
        return adminToken;
    }
}

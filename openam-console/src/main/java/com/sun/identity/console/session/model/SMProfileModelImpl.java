/*
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
 * $Id: SMProfileModelImpl.java,v 1.5 2008/06/25 05:43:21 qcheng Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package com.sun.identity.console.session.model;

import static org.forgerock.openam.session.SessionConstants.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.session.SessionCache;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.sso.SSOException;
import com.sun.identity.common.SearchResults;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.sm.SMSException;

/* - LOG COMPLETE - */

/**
 * Class that provides methods to manage Session Management.
 */
public class SMProfileModelImpl extends AMModelBase
    implements SMProfileModel
{
    public  final static String USER_ID = "UserId";

    private SMSessionCache smSessionCache;
    private SessionCache sessionCache;
    private static String DELIMITER = "|";
    private Map serverNamesMap = null;
    private String serverName = null;
    private boolean validSession = true;
    
    /**
     * Constructs a session management profile model implementation object.
     *
     * @param request  HTTP Servlet request.
     * @param map of user information.
     */
    public SMProfileModelImpl(HttpServletRequest request, Map map) {
        super(request, map);
        this.sessionCache = InjectorHolder.getInstance(SessionCache.class);
    }

    /**
     * Sets server name.
     *
     * @param value sever name.
     */
    public void setProfileServerName(String value) {
        serverName = value;
    }

    /**
     * Initializes sessions list.
     *
     * @param pattern user id pattern to search for.
     * @throws AMConsoleException if unable to initialized the session list.
     */
    private void initSessionsList(String pattern) 
        throws AMConsoleException
    {
        pattern = pattern.toLowerCase();
        String[] params = {serverName, pattern};
        logEvent("ATTEMPT_GET_CURRENT_SESSIONS", params);

        try {
            Session session = sessionCache.getSession(
                    new SessionID(getUserSSOToken().getTokenID().toString()));
            SearchResults<Session> result = session.getValidSessions(
                serverName, pattern);
            Set<Session> sessions = result.getSearchResults();
            String errorMessage =
                AMAdminUtils.getSearchResultWarningMessage(result, this);
            smSessionCache = new SMSessionCache(sessions, errorMessage, this);
            logEvent("SUCCEED_GET_CURRENT_SESSIONS", params);
        } catch (SessionException se) {
            String strError = getErrorString(se);
            String[] paramsEx = {serverName, pattern, strError};
            logEvent("SESSION_EXCEPTION_GET_CURRENT_SESSIONS", paramsEx);
            throw new AMConsoleException(strError);
        }
    }

    private Session getCurrentSession()
        throws AMConsoleException
    {
        Session session;

        try {
            session = sessionCache.getSession(
                new SessionID(getUserSSOToken().getTokenID().toString()));
        } catch (SessionException se) {
            throw new AMConsoleException(getErrorString(se));
        }

        return session;
    }

    private Map<String, Session> getValidSessions(Session session, String pattern) throws AMConsoleException {
        Map<String, Session> sessions = Collections.emptyMap();

        try {
            SearchResults<Session> result = session.getValidSessions(serverName, pattern);
            Set<Session> validSessions = result.getSearchResults();

            if ((validSessions != null) && !validSessions.isEmpty()) {
                sessions = new HashMap<>(validSessions.size());

                for (Session s : validSessions) {
                    if (s != null) {
                        sessions.put(s.getID().toString(), s);
                    }
                }
            }
        } catch (SessionException se) {
            throw new AMConsoleException(getErrorString(se));
        }

        return sessions;
    }

    public List<String> invalidateSessions(List<String> list, String pattern) throws AMConsoleException {
        List<String> failList = Collections.emptyList();

        if ((list != null) && !list.isEmpty()) {
            Session session = getCurrentSession();
            Map<String, Session> validSessions = getValidSessions(session, pattern);
            list.retainAll(validSessions.keySet());

            if (!list.isEmpty()) {
                String currentSessionHandler = null;

                try {
                    currentSessionHandler =
                        session.getProperty(SESSION_HANDLE_PROP);
                } catch (SessionException se) {
                    throw new AMConsoleException(getErrorString(se));
                }

                String[] params = new String[2];
                params[0] = serverName;
                String curSessionId = null;

                failList = new ArrayList<String>(list.size());

                for (String sessionId : list) {
                    Session s = validSessions.get(sessionId);
                    params[1] = sessionId;
                    boolean isCurrentSession = false;

                    try {
                        isCurrentSession = currentSessionHandler.equals(s.getProperty(SESSION_HANDLE_PROP));
                    } catch (SessionException se) {
                        logEvent("SESSION_EXCEPTION_INVALIDATE_SESSIONS",
                                params);
                        throw new AMConsoleException(getErrorString(se));
                    }

                    if (isCurrentSession) {
                        curSessionId = sessionId;
                        validSession = false;
                    } else {
                        try {
                            logEvent("ATTEMPT_INVALIDATE_SESSIONS", params);
                            session.destroySession(s);
                            logEvent("SUCCEED_INVALIDATE_SESSIONS",params);
                        } catch (SessionException se) {
                            String[] paramsEx = {serverName, sessionId, getErrorString(se)};
                            logEvent("SESSION_EXCEPTION_INVALIDATE_SESSIONS", paramsEx);
                            try {
                                failList.add(s.getProperty(USER_ID));
                            } catch (SessionException e) {
                                debug.error("SMProfileModelImpl.invalidateSessions", e);
                            }

                            debug.error("SMProfileModelImpl.invalidateSessions", se);
                        }
                    }
                }
            
                if (!validSession) {
                    params[1] = curSessionId;
                    logEvent("ATTEMPT_INVALIDATE_SESSIONS", params);

                    try {
                        session.destroySession(session);
                        logEvent("SUCCEED_INVALIDATE_SESSIONS",params);
                    } catch (SessionException se) {
                        String[] paramsEx = {serverName, curSessionId,
                            getErrorString(se)};
                        logEvent("SESSION_EXCEPTION_INVALIDATE_SESSIONS",
                            paramsEx);
                    }
                }
            }
        }

        return failList;
    }

    /**
     * Returns true if current user session state is valid.
     *
     * @return true is the session is valid.
     */
    public boolean isSessionValid() {
        return validSession;
    }

    /**
     * Returns session cache.
     *
     * @param pattern Pattern for search.
     * @return session cache.
     * @throws AMConsoleException if unable to get the session cache.
     */
    public SMSessionCache getSessionCache(String pattern) 
        throws AMConsoleException
    {
        if (smSessionCache == null) {
            initSessionsList(pattern);
        }

        return smSessionCache;
    }

    /**
     * Sets session cache.
     *
     * @param cache Session cache.
     */
    public void setSmSessionCache(SMSessionCache cache) {
        smSessionCache = cache;
    }

    /**
     * Returns set of server names.
     *
     * @return set of server names.
     */
    public Map getServerNames() {
        if (serverNamesMap == null || serverNamesMap.isEmpty()) {
            try {
                Set names = ServerConfiguration.getServerInfo(
                    getUserSSOToken());
                names = parseServerNames(names);
                serverNamesMap = getMapValues(names);
            } catch (SSOException e) {
                debug.error("SMProfileModelImpl.getServerNames", e);
            } catch (SMSException e) {
                debug.error("SMProfileModelImpl.getServerNames", e);                
            }
        }
        return serverNamesMap;
    }


    private Set parseServerNames(Set servers) {
        Set serverList = new HashSet();
        Iterator iter = servers.iterator();
        while(iter.hasNext()) {
            String serverEntry = (String)iter.next();
            int index = serverEntry.indexOf(DELIMITER);
            if (index != -1) {
                String server = serverEntry.substring(0, index);
                serverList.add(server);
            } else {
                if (debug.warningEnabled()) {
                    debug.warning("SMProfileModelImpl.parseServerNames:" +
                        "This server entry is not proper, ignoring:"
                        + serverEntry);
                }
            }
        }
        return serverList;
    }

    /**
     * Returns the map of server names(host:port) and url.
     *
     * @param set names of server instance.
     * returns the map of server names(host:port) and url. 
     */
    private Map getMapValues(Set set) {
        Map map = Collections.EMPTY_MAP;
        if (set != null && !set.isEmpty()) {
            Iterator iter = set.iterator();
            map = new HashMap(set.size()*2);
            while (iter.hasNext()) {
                String url = (String)iter.next();
                try {
                     URL u = new URL(url);
                     String host = u.getHost();
                     int port = u.getPort();
                     if (port == -1) {
                         map.put(host, url);
                     } else {
                         String server = host + ":" + port;
                         map.put(server, url);
                     }
                } catch (MalformedURLException e) {
                    debug.error("SMProfileModelImpl.getMapValues", e);
                }
            }
        }
        return map;
    }

}

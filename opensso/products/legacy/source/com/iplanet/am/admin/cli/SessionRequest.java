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
 * $Id: SessionRequest.java,v 1.3 2009/01/28 05:35:11 ww203982 Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.util.Locale;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.DisplayUtils;
import com.sun.identity.common.SearchResults;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.logging.Level;
import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.shared.ldap.util.DN;

/**
 * This class handles Session command line request.
 */
class SessionRequest {
    private final static String USER_ID = "UserId";

    private SessionID curSessionID;
    private Session curSession;
    private ResourceBundle bundle;
    private String serverName;
    
    /**
     * Constructs a session request object.
     *
     * @param ssoToken Single Sign On Token of command line application.
     * @param serverName name of server where session resides.
     * @param bundle resource bundle to retrieve localized strings.
     * @throws AdminException if cannot obtain session of command line
     *         application.
     */
    SessionRequest(SSOToken ssoToken, String serverName, ResourceBundle bundle)
        throws AdminException
    {
        this.serverName = trimTrailingSlash(serverName);
        this.bundle = bundle;
        curSessionID = new SessionID(ssoToken.getTokenID().toString());
        
        try {
            curSession = Session.getSession(curSessionID);
        } catch (SessionException se) {
            throw new AdminException(se);
        }
    }

    /**
     * Displays list of valid sessions.
     *
     * @param pattern to filter sessions.
     */
    public void displaySessions(String pattern)
        throws AdminException
    {
        pattern = pattern.trim();
        StringTokenizer st = new StringTokenizer(serverName, ":");

        if (st.countTokens() == 3) {
            List sList = getSessionList(serverName, pattern);

            if ((sList != null) && !sList.isEmpty()) {
                for (Iterator iter = sList.iterator(); iter.hasNext(); ) {
                    printSessionInformation((SessionData)iter.next());
                }
                promptForInvalidation(sList);
            } else {
                System.err.println(bundle.getString("sessionsListEmpty"));
            }
        } else {
            System.err.println(bundle.getString("serverNameError"));
        }
    }
    
    /**
     * Gets list of valid sessions excluding the session that is created
     * by <code>amAdmin</code>.
     *
     * @param name of server.
     * @param pattern user id pattern to search for.
     * @return sessions list.
     * @throws AdminException if fails to get session handler.
     */
    private List getSessionList(String name, String pattern)
        throws AdminException
    {
        List list = Collections.EMPTY_LIST;
                                                                                
        try {
            System.out.println(bundle.getString("getSessionList")  + " " +
                bundle.getString("serverName") + " = " + name);
            String currentSessionHandler =
                curSession.getProperty(Session.SESSION_HANDLE_PROP);
            SearchResults result = curSession.getValidSessions(name, null);

            String warning = getSearchResultWarningMessage(result);
            if (warning.length() > 0) {
                System.out.println(warning);
            }

            Hashtable sessions = (Hashtable)result.getResultAttributes();
            list = new ArrayList(sessions.size());
            boolean isCurrentSession = false;
            int i = 0;
                                                                                
            for (Iterator iter = (Iterator)sessions.values().iterator();
                iter.hasNext();
            ) {
                boolean isCurr = false;
                Session sess = (Session)iter.next();

                // need to check current session only if we have not found it.
                if (!isCurrentSession) {
                    try {
                        isCurr = sess.getProperty(Session.SESSION_HANDLE_PROP)
                            .equals(currentSessionHandler);
                    } catch (SessionException se) {
                        throw new AdminException(se);
                    }
                    isCurrentSession = isCurr;
                }

                String userId = sess.getProperty(USER_ID);

                if (userId != null) {
                    userId = dnToName(userId);

                    if (DisplayUtils.wildcardMatch(userId, pattern)) {
                        // -1 indicates that it is current session.
                        int idx = (isCurr) ? -1 : i++;
                        SessionData sData =
                            createSessionData(idx, userId, sess);

                        if (idx == -1) {
                            list.add(0, sData);
                        } else {
                            list.add(sData);
                        }
                    }
                }
            }
        } catch (SessionException se) {
            System.err.println(bundle.getString("invalidServiceHostName"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
    
    /**
     * Gets relative distinguished name.
     *
     * @param dn distinguished name
     * @return name of relative distinguished name
     */
    private String dnToName(String dn) {
        String ret = dn;
                                                                                
        if (DN.isDN(dn)) {
            String[] comps = LDAPDN.explodeDN(dn, true);
            ret = comps[0];
        }
        
        return ret;
    }

    /**
     * Prompts user to enter indices of sessions to be invalidated.
     *
     * @param sessionList list of sessions.
     */
    private void promptForInvalidation(List sessionList)
        throws AdminException
    {
        List snList = new ArrayList(sessionList.size());
        System.out.println(bundle.getString("toInvalidate"));
        System.out.println(bundle.getString("CRToExit"));

        try {
            BufferedReader buffReader = new BufferedReader(
                new InputStreamReader(System.in));
            String si = buffReader.readLine();
            StringTokenizer st = new StringTokenizer(si, " ");
            if (st.countTokens() != 0) {
                boolean matched = true;

                while(st.hasMoreTokens() && matched) {
                    String sn = st.nextToken();
                    matched = sessionIndexMatched(sessionList, sn);

                    if (matched) {
                        snList.add(sn);
                    } else {
                        System.err.println(
                            bundle.getString("selectionNotInList"));
                    }
                }

                if (matched) {
                    invalidateSessions(sessionList, snList);
                }
            }
        } catch (IOException ioe) {
            System.err.println(
                bundle.getString("ioExceptionReadingInput") + " " + ioe);
        }
    }
    
    /**
     * Returns true if <code>idx</code> is an valid index in the session list.
     *
     * @param sessionList session list
     * @param idx to be validated.
     * @return true if idx is an valid index in the session list.
     */
    private boolean sessionIndexMatched(List sessionList, String idx) {
        boolean match = false;

        try {
            int index = Integer.parseInt(idx);
            match = (index >= 0) && (index < sessionList.size());
        } catch (NumberFormatException nfe) {
            // ignore if user enters an non integer
        }

        return match;
    }

    /**
     * Destroy a session.
     *
     * @param session used to destory <code>sess</code>
     * @param sess session to be destroyed
     */
    private void destroySession(Session session, Session sess) {
        try {
            session.destroySession(sess);
        } catch (SessionException se) {
            // ignore session may be already destroyed.
        }
    }

    /**
     * Invalidates Sessions.
     *
     * @param sessionList list of valid session.
     * @param list of session index to be invalidated.
     */
    private void invalidateSessions(List sessionList, List list)
        throws AdminException
    {
        if ((list != null) && !list.isEmpty()) {
            for (Iterator iter = sessionList.iterator(); iter.hasNext(); ) {
                SessionData sData = (SessionData)iter.next();
                String strIndex = String.valueOf(sData.index);

                if (list.contains(strIndex)) {
                    destroySession(curSession, sData.session);

//                    AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
//                        (bundle.getString("statusmsg36") + " " + sData.userId));
                    String[] params = {sData.userId};
                    AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
                        Level.INFO, AdminUtils.STATUS_MSG36, params);
                }
            }
            System.out.println(bundle.getString("destroySessionSucceeded"));
        }
    }

    /**
     * Creates Session Data object.
     *
     * @param index of object.
     * @param userId user ID.
     * @param sess session associated with this Session Data object.
     * @return Session Data object
     */
    private SessionData createSessionData(
        int index,
        String userId,
        Session sess
    ) throws SessionException
    {
        SessionData sData = new SessionData();
        sData.userId = userId;
        sData.clientID = sess.getClientID();
        sData.index = index;
        sData.session = sess;
        sData.timeRemain = String.valueOf(sess.getTimeLeft()/60);
        sData.maxSessionTime = String.valueOf(sess.getMaxSessionTime());
        sData.idleTime = String.valueOf(sess.getIdleTime()/60);
        sData.maxIdleTime = String.valueOf(sess.getMaxIdleTime());
        return sData;
    }
    
    /**
     * Prints Session Information.
     *
     * @param sData Session Data object.
     */
    private void printSessionInformation(SessionData sData) {
        if (sData.index == -1) {
            System.out.print(bundle.getString("currentSession") + " ");
        } else {
            System.out.print(bundle.getString("index") + " " + sData.index +
                " ");
        }

        System.out.println(
            bundle.getString("userId") + " " + sData.userId + " " +
            bundle.getString("timeRemain") + " " + sData.timeRemain + " " +
            bundle.getString("maxSessionTime") + " " +
                sData.maxSessionTime + " " +
            bundle.getString("idleTime") + " " + sData.idleTime + " " +
            bundle.getString("maxIdleTime") + " " + sData.maxIdleTime);
    }
    
    /**
     * Returns search results warning message.  <code>SearchResult</code>
     * returns an error code whenever size or time limit is reached.
     * This method interprets the error code and return the appropriate
     * warning message.  Empty string is returned if no limits are reached.
     *
     * @param results Access Management Search Result object.
     * @param model to retrieve localized string.
     * @return search results warning message.
     */
    private String getSearchResultWarningMessage(SearchResults results)
    {
        String message = null;
        Object args[] = null;

        if (results != null) {
            int errorCode = results.getErrorCode();

            if (errorCode == SearchResults.SIZE_LIMIT_EXCEEDED) {
                message = Locale.getString(bundle,
                    "sizeLimitExceeded", args);
            } else if (errorCode == SearchResults.TIME_LIMIT_EXCEEDED) {
                message = Locale.getString(bundle,
                    "timeLimitExceeded", args);
            }
        }

        return (message != null) ? message : "";
    }

    /**
     * This class stores session information
     */
    private class SessionData {
        private int index = 0;
        private String userId = null;
        private String clientID = null;
        private Session session = null;
        private String timeRemain = null;
        private String maxSessionTime = null;
        private String idleTime = null;
        private String maxIdleTime = null;
    }

    /**
     * Trims trailing slash in string.
     *
     * @param str to act on.
     * @return trimmed <code>str</code>.
     */
    private String trimTrailingSlash(String str) {
        int len = str.length();

        while ((len > 0) && (str.charAt(len-1) == '/')) {
            str = str.substring(0, len-1);
            len--;
        }

        return str;
    }
}

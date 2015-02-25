/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SessionCommand.java,v 1.9 2010/01/04 18:59:21 veiming Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */

package com.sun.identity.cli;


import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.common.DisplayUtils;
import com.sun.identity.common.SearchResults;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.shared.ldap.util.DN;
import java.util.Map;

/**
 * Displays active sessions.
 */
public class SessionCommand extends AuthenticatedCommand {
    private static final String ARGUMENT_HOST_NAME = "host";
    private final static String USER_ID = "UserId";
    private final static String QUIET_PARAM = "quiet";

    private Session curSession;
    private SessionID curSessionID;

    public void handleRequest(RequestContext rc) 
        throws CLIException {
        super.handleRequest(rc);
        Authenticator auth = Authenticator.getInstance();
        String bindUser = getAdminID();
        AuthContext lc = auth.sessionBasedLogin(
            getCommandManager(), bindUser, getAdminPassword());
        try {
            boolean isQuiet = isOptionSet(QUIET_PARAM);
            handleRequest(lc.getSSOToken(), isQuiet);

            try {
                lc.logout();
            } catch (AuthLoginException e) {
                throw new CLIException(
                    e, ExitCodes.SESSION_BASED_LOGOUT_FAILED);
            }
        } catch (Exception e) {
            throw new CLIException(e, ExitCodes.SESSION_BASED_LOGIN_FAILED);
        }
    }

    private void handleRequest(SSOToken ssoToken, boolean isQuiet)
        throws CLIException {
        IOutput ouputWriter = getOutputWriter();

        List sList = displaySessions(ssoToken);

        if ((sList != null) && !sList.isEmpty()) {
            for (Iterator i= sList.iterator(); i.hasNext(); ) {
                printSessionInformation(ouputWriter, (SessionData)i.next());
            }
            if ((sList.size() > 1) && !isQuiet) {
                promptForInvalidation(ouputWriter, sList);
            }
        } else {
            ouputWriter.printlnMessage(getResourceString(
                "session-no-sessions"));
        }
    }

    private void promptForInvalidation(IOutput ouputWriter, List sessionList)
        throws CLIException
    {
        List snList = new ArrayList(sessionList.size());
        ouputWriter.printlnMessage(getResourceString("session-to-invalidate"));
        ouputWriter.printlnMessage(getResourceString("session-cr-to-exit"));

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
                        ouputWriter.printlnError(getResourceString(
                            "session-selection-not-in-list"));
                    }
                }

                if (matched) {
                    invalidateSessions(ouputWriter, sessionList, snList);
                }
            }
        } catch (IOException ioe) {
            ouputWriter.printlnError(getResourceString(
                "session-io-exception-reading-input") + " " + ioe);
        }
    }

    private void destroySession(Session session, SessionData sData)
        throws CLIException
    {
        try {
            Session sess = sData.session;
            String[] params = {sData.userId};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_SESSION_DESTROY", params);
            session.destroySession(sess);
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_SESSION_DESTROY", params);
        } catch (SessionException se) {
            String[] params = {sData.userId, se.getMessage()};
            debugError("SessionCommand.destroySession", se);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_SESSION_DESTROY", params);
        }
    }

    private void invalidateSessions(
        IOutput ouputWriter,
        List sessionList,
        List list
    ) throws CLIException
    {
        if ((list != null) && !list.isEmpty()) {
            for (Iterator iter = sessionList.iterator(); iter.hasNext(); ) {
                SessionData sData = (SessionData)iter.next();
                String strIndex = String.valueOf(sData.index);

                if (list.contains(strIndex)) {
                    destroySession(curSession, sData);
                }
            }
            ouputWriter.printlnMessage(getResourceString(
                "session-destroy-session-succeeded"));
        }
    }


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


    private void printSessionInformation(IOutput ouputWriter, SessionData sData
    ) {
        if (sData.index == -1) {
            ouputWriter.printMessage(
                getResourceString("session-current-session") + " ");
        } else {
            ouputWriter.printMessage(
                getResourceString("session-index") + " " + sData.index +
                " ");
        }

        ouputWriter.printlnMessage(
            getResourceString("session-userId") + " " + sData.userId + " " +
            getResourceString("session-time-remain") + " " +
            sData.timeRemain + " " +
            getResourceString("session-max-session-time") + " " +
            sData.maxSessionTime + " " +
            getResourceString("session-idle-time") + " " + sData.idleTime +
            " " + getResourceString("session-max-idle-time") + " " +
            sData.maxIdleTime);
    }


    private List displaySessions(SSOToken ssoToken)
        throws CLIException
    {
        String origHost = getStringOptionValue(ARGUMENT_HOST_NAME);
        String host = trimTrailingSlash(origHost);
        StringTokenizer st = new StringTokenizer(host, ":"); 
        if (st.countTokens() != 3) {
            Object[] params = {origHost};
            throw new CLIException(MessageFormat.format(
                getResourceString("session-invalid-host-name"), params),
                ExitCodes.INVALID_OPTION_VALUE);

        }

        curSessionID = new SessionID(ssoToken.getTokenID().toString());
        String filter = getStringOptionValue(IArgument.FILTER);
        if ((filter == null) || (filter.trim().length() == 0)) {
            filter = "*";
        }

        try {
            curSession = Session.getSession(curSessionID);
            return getSessionList(host, filter);
        } catch (SessionException se) {
            throw new CLIException(se, ExitCodes.SESSION_BASED_LOGIN_FAILED);
        }
    }

    private String trimTrailingSlash(String str) {
        int len = str.length();
        while ((len > 0) && (str.charAt(len-1) == '/')) {
            str = str.substring(0, len-1);
            len--;
        }
        return str;
    }

    private List getSessionList(String name, String pattern)
        throws CLIException
    {
        IOutput output = getOutputWriter();
        List list = new ArrayList();
                                                                                
        try {
            String currentSessionHandler = curSession.getProperty(
                Session.SESSION_HANDLE_PROP);
            SearchResults result = curSession.getValidSessions(name, null);
            String warning = getSearchResultWarningMessage(result);
            if (warning.length() > 0) {
                output.printlnMessage(warning);
            }

            Map<String, Session> sessions = (Map<String, Session>) result.getResultAttributes();
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
                        throw new CLIException(se,
                            ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
                    }
                    isCurrentSession = isCurr;
                }

                String userId = sess.getProperty(USER_ID);
                if (userId != null) {
                    userId = dnToName(userId);
                    if (DisplayUtils.wildcardMatch(userId, pattern)) {
                        // -1 indicates that it is current session.
                        int idx = (isCurr) ? -1 : i++;
                        SessionData sData = createSessionData(
                            idx, userId, sess);
                        if (idx == -1) {
                            list.add(0, sData);
                        } else {
                            list.add(sData);
                        }
                    }
                }
            }
        } catch (SessionException se) {
            throw new CLIException(se, ExitCodes.INVALID_OPTION_VALUE);
        }

        return list;
    }

    private String getSearchResultWarningMessage(SearchResults results) {
        String message = null;
        if (results != null) {
            int errorCode = results.getErrorCode();
            if (errorCode == SearchResults.SIZE_LIMIT_EXCEEDED) {
                message = getResourceString("sizeLimitExceeded");
            } else if (errorCode == SearchResults.TIME_LIMIT_EXCEEDED) {
                message = getResourceString("timeLimitExceeded");
            }
        }
        return (message != null) ? message : "";
    }

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

    private String dnToName(String dn) {
        String ret = dn;
        if (DN.isDN(dn)) {
            String[] comps = LDAPDN.explodeDN(dn, true);
            ret = comps[0];
        }
        return ret;
    }

}

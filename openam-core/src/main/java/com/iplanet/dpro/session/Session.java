/*
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
 * $Id: Session.java,v 1.25 2009/08/14 17:53:35 weisun2 Exp $
 *
 * Portions copyright 2010-2016 ForgeRock AS.
 */

package com.iplanet.dpro.session;

import static org.forgerock.openam.session.SessionConstants.*;
import static org.forgerock.openam.utils.Time.*;

import java.net.URL;
import java.security.AccessController;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.blacklist.BlacklistException;
import org.forgerock.openam.blacklist.Blacklistable;
import org.forgerock.openam.session.AMSession;
import org.forgerock.openam.session.SessionCache;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.session.SessionCookies;
import org.forgerock.openam.session.SessionPLLSender;
import org.forgerock.openam.session.SessionServiceURLService;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.operations.ClientSdkSessionOperationStrategy;
import com.iplanet.dpro.session.operations.ServerSessionOperationStrategy;
import com.iplanet.dpro.session.operations.SessionOperationStrategy;
import com.iplanet.dpro.session.operations.SessionOperations;
import com.iplanet.dpro.session.operations.strategies.ClientSdkOperations;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.dpro.session.service.SessionState;
import com.iplanet.dpro.session.share.SessionBundle;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.dpro.session.share.SessionRequest;
import com.iplanet.dpro.session.share.SessionResponse;
import com.iplanet.services.comm.client.PLLClient;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.SearchResults;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.session.util.RestrictedTokenAction;
import com.sun.identity.session.util.RestrictedTokenContext;
import com.sun.identity.shared.debug.Debug;

/**
 * The <code>Session</code> class represents a session. It contains session
 * related information such as session ID, session type (user/application),
 * client ID (user ID or application ID), session idle time, time left on the
 * session, and session state. It also allows applications to add listener for
 * session events.
 *
 * @see com.iplanet.dpro.session.SessionID
 * @see com.iplanet.dpro.session.SessionListener
 */

public class Session implements Blacklistable, AMSession{

    public static final String CACHED_BASE_POLLING_PROPERTY = "com.iplanet.am.session.client.polling.cacheBased";

    /**
     * Defines the type of Session that has been created. Where 0 for User
     * Session; and 1 for Application Session.
     */
    private int sessionType;

    /**
     * Identification string of the Client using this Session.
     */
    private String clientID;

    /**
     * Name of the Domain or Organization through which the User/Client has been
     * Authenticated.
     */
    private String clientDomain;

    /**
     * Total Maximum time allowed for the session, in minutes.
     */
    private long maxSessionTime;

    /**
     * Maximum idle time allowed for the session, in minutes.
     */
    private long maxIdleTime;

    /**
     * Maximum time for which the cached session is used, in minutes.
     */
    private long maxCachingTime;

    /**
     * The time for which the session has been idle, in seconds.
     */
    private long sessionIdleTime;

    /**
     * Total time left for the session, in seconds.
     */
    private long sessionTimeLeft;

    /**
     * Time after which the session will be considered invalid, in milliseconds.
     */
    private long sessionExpiryTime;

    /*
     * This is the time value (computed as System.currentTimeMillis()/1000) when
     * the session timed out. Value zero means the session has not timed out.
     */
    private volatile long timedOutAt = 0;

    /**
     * Four possible values for the state of the session:
     * <ul>
     *     <li>Invalid</li>
     *     <li>Valid</li>
     *     <li>Inactive</li>
     *     <li>Destroyed</li>
     * </ul>
     */
    protected SessionState sessionState = SessionState.INVALID;

    /**
     * If this is a Remote session that has been destroyed but not yet removed from the
     * sessionTable, this flag is used to avoid repeated notification of the DESTROY event
     * to session listeners.
     */
    private AtomicBoolean removed = new AtomicBoolean(false);


    /**
     * All session related properties are stored as key-value pair in this
     * table.
     */
    protected Hashtable<String, String> sessionProperties = new Hashtable<>();

    /**
     * URL of the Session Server, where this session resides.
     * Note: This field only exists to optimise client mode.
     */
    private URL sessionServiceURL;

    /**
     * Last time the client sent a request associated with this session, as the
     * number of seconds since midnight January 1, 1970 GMT.
     */
    private volatile long latestRefreshTime;

    /**
     * Indicates whether the latest access time need to be reset on the session.
     */
    private volatile boolean needToReset = false;

    /**
     * Indicates whether to use local or remote calls to Session Service
     */
    protected boolean sessionIsLocal = false;


    private String cookieStr;

    private Boolean cookieMode = null;

    private TokenRestriction restriction = null;

    private Object context = null;

    // Debug instance
    private static Debug sessionDebug = Debug.getInstance(SessionConstants.SESSION_DEBUG);

    /**
     * Set of session event listeners for THIS session only
     */
    private Set<SessionListener> localSessionEventListeners = new HashSet<SessionListener>();

    private Requests requests;

    private final SessionCookies sessionCookies;
    private final SessionCache sessionCache;
    private final SessionServiceURLService sessionServiceURLService;
    private final SessionOperationStrategy sessionOperationStrategy;
    private final SessionService sessionService;

    private SessionID sessionID;

    /**
     * Constructor used by this package only.
     *
     * ClientSDK: This code has to operate both on the serer and the client. It needs
     * to be able to resolve dependencies in an appropriate way in both conditions.
     */
    public Session(SessionID sid) {
        sessionID = sid;

        if (SystemProperties.isServerMode()) {
            // Had to choose, final or @Inject approach. Went final.
            sessionCookies = InjectorHolder.getInstance(SessionCookies.class);
            sessionCache = InjectorHolder.getInstance(SessionCache.class);
            sessionServiceURLService = InjectorHolder.getInstance(SessionServiceURLService.class);
            sessionService = InjectorHolder.getInstance(SessionService.class);
            sessionOperationStrategy = InjectorHolder.getInstance(ServerSessionOperationStrategy.class);
            requests = InjectorHolder.getInstance(Requests.class);
        } else {
            sessionService = null; // Intentionally null in client mode.
            sessionCache = SessionCache.getInstance();
            sessionCookies = SessionCookies.getInstance();
            sessionServiceURLService = SessionServiceURLService.getInstance();
            requests = new Requests(null, null, new SessionPLLSender(sessionCookies));
            sessionOperationStrategy = new ClientSdkSessionOperationStrategy(
                    new ClientSdkOperations(sessionDebug, requests, null, sessionServiceURLService, null, null));
        }
    }

    private Session(SessionID sessionId, boolean sessionIsLocal) {
        this(sessionId);
        this.sessionIsLocal = sessionIsLocal;
    }

    public void setSessionIsLocal(boolean sessionIsLocal) {
        this.sessionIsLocal = sessionIsLocal;
    }

    public AtomicBoolean getRemoved() {
        return removed;
    }

    public SessionID getSessionID() {
        return sessionID;
    }

    public String getCookieStr() {
        return cookieStr;
    }

    public void setCookieStr(String str) {
        this.cookieStr = str;
    }

    //todo: Object...
    public void setContext(Object context) {
        this.context = context;
    }

    /**
     * Returns the session ID.
     * @return The session ID.
     */
    public SessionID getID() {
        return sessionID;
    }

    /**
     * Updates the ID of the session. Used when updating a stateless session.
     *
     * @param sessionID the new session ID for the session.
     */
    protected void setID(SessionID sessionID) {
        this.sessionID = sessionID;
    }

    /**
     * Returns the session type.
     *
     * @return The session type.
     */
    public int getType() {
        return sessionType;
    }

    /**
     * Returns the client ID in the session.
     *
     * @return The client ID in the session.
     */
    public String getClientID() {
        return clientID;
    }

    /**
     * Returns the client domain in the session.
     *
     * @return The client domain in the session.
     */
    public String getClientDomain() {
        return clientDomain;
    }

    /**
     * Returns the maximum session time in minutes.
     *
     * @return The maximum session time.
     */
    public long getMaxSessionTime() {
        return maxSessionTime;
    }

    /**
     * Returns the maximum session idle time in minutes.
     *
     * @return The maximum session idle time.
     */
    public long getMaxIdleTime() {
        return maxIdleTime;
    }

    /**
     * Returns true if the session has timed out.
     * @return <code>true</code> if session timed out,
     *         <code>false</code>otherwise
     * @exception SessionException
     */
    public boolean isTimedOut() throws SessionException {
        /**
         * Before going to the server, check if the session has been already
         * marked TimedOut or not.
         */
        if (timedOutAt > 0) {
            return true;
        }
        if (!usingCachedBasedPolling() && maxCachingTimeReached()){
            try {
                refresh(false);
            } catch (SessionTimedOutException e) {
                latestRefreshTime = currentTimeMillis() / 1000;
                timedOutAt = latestRefreshTime;
            }
        }
        return timedOutAt > 0;
    }

    /**
     * Returns the maximum session caching time in minutes.
     *
     * @return The maximum session caching time.
     */
    public long getMaxCachingTime() {
        return maxCachingTime;
    }

    /**
     * Returns the session idle time in seconds.
     *
     * @return The session idle time.
     * @exception SessionException if the session reached its maximum session
     *            time, or the session was destroyed, or there was an error
     *            during communication with session service.
     */
    public long getIdleTime() throws SessionException {
        refreshSessionIfStale();
        return sessionIdleTime;
    }

    /**
     * Returns the time left for this session in seconds.
     *
     * @return The time left for this session.
     * @exception SessionException is thrown if the session reached its
     *            maximum session time, or the session was destroyed, or
     *            there was an error during communication with session
     *            service.
     */
    public long getTimeLeft() throws SessionException {
        refreshSessionIfStale();
        return sessionTimeLeft;
    }

    private void refreshSessionIfStale() throws SessionException {
        if (!usingCachedBasedPolling() && maxCachingTimeReached()) {
            refresh(false);
        }
    }

    /**
     * The time (in milliseconds from the UTC epoch) until this session can be removed from a session blacklist. This
     * is guaranteed to be some time after the session has expired.
     *
     * @return the at which the session expires (if it has not already) plus a purge delay.
     * @throws BlacklistException if the session has already expired or an error occurs.
     */
    @Override
    public long getBlacklistExpiryTime() throws BlacklistException {
        try {
            refreshSessionIfStale();
            return sessionExpiryTime;
        } catch (SessionException e) {
            throw new BlacklistException(e);
        }
    }

    /**
     * Gets the time at which the Session was last refreshed from the master copy (in millis).
     * @return The latest time at which the session was refreshed.
     */
    public long getLatestRefreshTime() {
        return latestRefreshTime;
    }

    /**
     * Returns the state of the session.
     *
     * @param reset
     *            This parameter indicates that whether the Session Service
     *            needs to reset the latest access time on this session.
     * @return The state of the session. The session state is one of the
     *         following: <code>INVALID, VALID, INACTIVE, and DESTROYED</code>.
     * @exception SessionException is thrown if the session reached its
     *            maximum session time, or the session was destroyed, or
     *            there was an error during communication with session
     *            service.
     */
    public SessionState getState(boolean reset) throws SessionException {
        if (!usingCachedBasedPolling() && maxCachingTimeReached()) {
            refresh(reset);
        } else {
            if (reset) {
                needToReset = true;
            }
        }
        return sessionState;
    }

    public void setState(SessionState state) {
        sessionState = state;
    }

    /**
     * Gets the property stored in this session.
     *
     * @param name The property name.
     * @return The property value in String format.
     * @exception SessionException is thrown if the session reached its
     *            maximum session time, or the session was destroyed, or
     *            there was an error during communication with session
     *            service.
     */
    public String getProperty(String name) throws SessionException {
        if (name == null ? sessionCookies.getLBCookieName() != null : !name.equals(sessionCookies.getLBCookieName())) {
            if ((!usingCachedBasedPolling() && maxCachingTimeReached()) ||
                !sessionProperties.containsKey(name)) {
                refresh(false);
            }
        }
        return sessionProperties.get(name);
    }

    /**
     * Given a restricted token, returns the SSOTokenID of the master token
     * can only be used if the requester is an app token
     *
     * @param s Must be an app token
     * @param restrictedId The SSOTokenID of the restricted token
     * @return The SSOTokenID string of the master token
     * @throws SSOException If the master token cannot be dereferenced
     */
    public String dereferenceRestrictedTokenID(Session s, String restrictedId)
    throws SessionException {
        String masterSID;

        try {
            masterSID = sessionService.deferenceRestrictedID(s, restrictedId);
        }
        catch (Exception e) {
            sessionDebug.error("unable to find master token for  " + restrictedId, e);
            throw new SessionException(e);
        }

        return masterSID;
    }

    /**
     * Returns true if the SSOTokenID associated with this SSOToken is a
     * restricted token, false otherwise.
     *
     * @return true if the token is restricted
     * @throws SSOException If we are unable to determine if the session is
     *              restricted
     */
    public boolean isRestricted() throws SessionException {
        return restriction != null;
    }

    /**
     * Gets the property stored in this session.
     *
     * @param name The property name.
     * @return The property value in String format only
     *         when run in the server mode else return null
     */
    public String getPropertyWithoutValidation(String name) {
        if (SystemProperties.isServerMode()) {
            return sessionProperties.get(name);
        }
        return null;
    }

    /**
     * Sets a property for this session.
     *
     * @param name The property name.
     * @param value The property value.
     * @exception SessionException if the session reached its maximum session
     *            time, or the session was destroyed, or there was an error
     *            during communication with session service, or if the property
     *            name or value was null.
     */
    public void setProperty(String name, String value) throws SessionException {
        if (name == null || value == null) {
            throw new SessionException("Session property name/value cannot be null");
        }
        try {
            SessionOperations operation = sessionOperationStrategy.getOperation(this.getID());
            operation.setProperty(this, name, value);
            sessionProperties.put(name, value);
        } catch (Exception e) {
            throw new SessionException(e);
        }
    }

    /**
     * Used to find out if the maximum caching time has reached or not.
     */
    public boolean maxCachingTimeReached() {
        long cachingtime = currentTimeMillis() / 1000 - latestRefreshTime;
        return cachingtime > maxCachingTime * 60;
    }

    /**
     * Gets the Session Service URL for this session object.
     *
     * @return The Session Service URL for this session.
     * @exception SessionException when cannot get Session URL.
     */
    public URL getSessionServiceURL() throws SessionException {
        if (SystemProperties.isServerMode()) {
            return sessionServiceURLService.getSessionServiceURL(sessionID);
        }

        // we can cache the result because in client mode
        // session service location does not change
        // dynamically
        if (sessionServiceURL == null) {
            sessionServiceURL = sessionServiceURLService.getSessionServiceURL(sessionID);
        }

        return sessionServiceURL;
    }

    /**
     * Destroys a session.
     *
     * @param session The session to be destroyed.
     * @exception SessionException if there was an error during
     *            communication with session service, or the corresponding
     *            session reached its maximum session/idle time, or the
     *            session was destroyed.
     */
    public void destroySession(Session session) throws SessionException {
        try {
            SessionOperations operation = sessionOperationStrategy.getOperation(this.getID());
            operation.destroy(this, session);
        } catch (Exception e) {
            throw new SessionException(e);
        }
        finally {
            sessionCache.removeSID(session.getID());
        }
    }

    /**
     * Logs out a session.
     *
     * @throws SessionException if there was an error during communication
     *         with session service. If the session logged out already,
     *         no exception will be thrown.
     */
    public void logout() throws SessionException {
        try {
            SessionOperations operation = sessionOperationStrategy.getOperation(this.getID());
            operation.logout(this);
            sessionCache.removeSID(sessionID);

        } catch (Exception e) {
            throw new SessionException(e);
        }
    }

    /**
     * Adds a session listener for session change events.
     *
     * @param listener Session Listener object.
     * @exception SessionException if the session state is not valid.
     */
    public void addSessionListener(SessionListener listener) throws SessionException {
        addSessionListener(listener, false);
    }

    /**
     * Adds a session listener for session change events.
     *
     * @param listener Session Listener object.
     * @param force whether to ignore whether a Session is in the Invalid state. If false will throw an exception if
     *              the Session is Invalid.
     * @exception SessionException if the session state is not valid.
     */
    public void addSessionListener(SessionListener listener, boolean force) throws SessionException {
        if (!force && sessionState != SessionState.VALID) {
            throw new SessionException(SessionBundle.rbName, "invalidSessionState", null);
        }
        localSessionEventListeners.add(listener);
    }

    /**
     * Gets all valid sessions from the specified session server. This session
     * is subject to access control in order to get all sessions.
     *
     * @param server
     *            The session server name. If the server name contains protocol
     *            and port, the protocol and port will be used. Otherwise, the
     *            server protocol and port is default to the same protocol and
     *            port of the calling session.
     * @return A Vector of Session objects.
     * @exception SessionException if there was an error during
     *                communication with session service.
     */
    public SearchResults<Session> getValidSessions(String server, String pattern)
            throws SessionException {
        String protocol = sessionID.getSessionServerProtocol();
        String host = server;
        String port = sessionID.getSessionServerPort();
        String uri = sessionID.getSessionServerURI();
        int pos = host.indexOf("://");
        if (pos != -1) {
            protocol = host.substring(0, pos);
            host = host.substring(pos + 3);
        }
        pos = host.indexOf(":");
        if (pos != -1) {
            port = host.substring(pos + 1);
            host = host.substring(0, pos);
            int pos1 = port.indexOf("/");
            if (pos1 != -1 ) {
                uri = port.substring(pos1);
                port = port.substring(0, pos1);
            }
        }

        URL svcurl = sessionServiceURLService.getSessionServiceURL(protocol, host, port, uri);
        return getValidSessions(svcurl, pattern);
    }

    /**
     * Get all the event listeners for this Session.
     *
     * @return SessionEventListener vector
     */
    public Set<SessionListener> getLocalSessionEventListeners() {
        return localSessionEventListeners;
    }

    /**
     * Returns all the valid sessions for a particular Session Service URL. If a
     * user is not allowed to access the Sessions of the input Session Server,
     * it will return null.
     *
     * @param svcurl Session Service URL.
     * @exception SessionException
     */
    private SearchResults<Session> getValidSessions(URL svcurl, String pattern)
            throws SessionException {
        try {
            int status;
            int totalResultCount;
            Set<SessionInfo> infos = null;

            boolean isLocal = false;
            if (sessionService != null && sessionService.isLocalSessionService(svcurl)) {
                SearchResults<SessionInfo> searchResults = sessionService.getValidSessions(this, pattern);
                infos = searchResults.getSearchResults();
                totalResultCount = searchResults.getTotalResultCount();
                status = searchResults.getErrorCode();
                isLocal = true;
            } else {
                SessionRequest sreq =
                        new SessionRequest(SessionRequest.GetValidSessions, sessionID.toString(), false);

                if (pattern != null) {
                    sreq.setPattern(pattern);
                }

                SessionResponse sres = requests.getSessionResponseWithRetry(svcurl, sreq, this);
                infos = new HashSet<>(sres.getSessionInfo());
                totalResultCount = infos.size();
                status = sres.getStatus();
            }

            Set<Session> sessions = new HashSet<>();

            for (SessionInfo info : infos) {
                SessionID sid = new SessionID(info.getSessionID());
                Session session = new Session(sid, isLocal);
                session.sessionServiceURL = svcurl;
                session.update(info);
                sessions.add(session);
            }

            return new SearchResults<>(totalResultCount, sessions, status);
        } catch (Exception ex) {
            sessionDebug.error("Session:getValidSession : ", ex);
            throw new SessionException(SessionBundle.rbName, "getValidSessionsError", null);
        }
    }

    /**
     * Gets the latest session from session server and updates the local cache
     * of this session.
     *
     * @param reset The flag to indicate whether to reset the latest session
     *        access time in the session server.
     * @exception SessionException if the session reached its
     *            maximum session time, or the session was destroyed, or
     *            there was an error during communication with session
     *            service.
     */
    public void refresh(final boolean reset) throws SessionException {
        // recalculate whether session is local or remote on every refresh
        // this is just an optmization
        // it is functionally safe to always use remote mode
        // but it is not efficient
        // this check takes care of migration "remote -> local"
        // reverse migration "local - > remote" will be
        // done by calling Session.markNonLocal() from
        // SessionService.handleReleaseSession()
        sessionIsLocal = checkSessionLocal();

        Object activeContext = RestrictedTokenContext.getCurrent();
        if (activeContext == null) {
            activeContext = this.context;
        }
        try {
            RestrictedTokenContext.doUsing(activeContext,
                    new RestrictedTokenAction() {
                        public Object run() throws Exception {
                            doRefresh(reset);
                            return null;
                        }
                    });

        } catch (Exception e) {
            sessionCache.removeSID(sessionID);
            if (sessionDebug.messageEnabled()) {
                sessionDebug.message("session.Refresh " + "Removed SID:"
                        + sessionID);
            }
            throw new SessionException(e);
        }
    }

   /*
    * Refreshes the Session Information
    * @param <code>true</code> refreshes the Session Information
    */
   private void doRefresh(boolean reset) throws SessionException {
        boolean flag = reset || needToReset;
        needToReset = false;

       SessionOperations operation = sessionOperationStrategy.getOperation(this.getID());
       SessionInfo info = operation.refresh(this, flag);

        long oldMaxCachingTime = maxCachingTime;
        long oldMaxIdleTime = maxIdleTime;
        long oldMaxSessionTime = maxSessionTime;
        update(info);
        sessionCache.notifySessionRefresh(this, oldMaxCachingTime, oldMaxIdleTime, oldMaxSessionTime);
    }

    /**
     * Updates the session from the session information server.
     *
     * @param info Session Information.
     */
    public synchronized void update(SessionInfo info) throws SessionException {
        if (info.getSessionType().equals("user")) {
            sessionType = USER_SESSION;
        } else if (info.getSessionType().equals("application")) {
            sessionType = APPLICATION_SESSION;
        }
        clientID = info.getClientID();
        clientDomain = info.getClientDomain();
        maxSessionTime = info.getMaxTime();
        maxIdleTime = info.getMaxIdle();
        maxCachingTime = info.getMaxCaching();
        sessionIdleTime = info.getTimeIdle();
        sessionExpiryTime = info.getExpiryTime(TimeUnit.MILLISECONDS);
        sessionTimeLeft = info.getTimeLeft();
        sessionState = SessionState.valueOf(info.getState().toUpperCase());
        sessionProperties = info.getProperties();
        if (timedOutAt <= 0) {
            String sessionTimedOutProp = sessionProperties.get("SessionTimedOut");
            if (sessionTimedOutProp != null) {
                try {
                    timedOutAt = Long.parseLong(sessionTimedOutProp);
                } catch (NumberFormatException e) {
                    sessionDebug.error("Invalid timeout value "
                            + sessionTimedOutProp, e);
                }
            }
        }
        latestRefreshTime = currentTimeMillis() / 1000;
        // note : do not use getProperty() call here to avoid unexpected
        // recursion via
        // refresh()
        String restrictionProp = sessionProperties.get(TOKEN_RESTRICTION_PROP);
        if (restrictionProp != null) {
            try {
                setRestriction(TokenRestrictionFactory.unmarshal(restrictionProp));
            } catch (Exception e) {
                throw new SessionException(e);
            }
        }
    }

    /**
     * Sets a token restriction on this session. Optional operation - specific sub-classes can throw an exception if
     * not supported.
     *
     * @param restriction the restriction to apply to this session.
     * @throws UnsupportedOperationException if this session type does not support token restrictions.
     */
    protected void setRestriction(TokenRestriction restriction) {
        this.restriction = restriction;
    }

    /**
      * populate context object with admin token
      * @exception SessionException
      * @param appSSOToken application SSO Token to bet set
      */

     void createContext(SSOToken appSSOToken) throws SessionException
     {

        if (appSSOToken == null) {
             if (sessionDebug.warningEnabled())  {
                 sessionDebug.warning("Session."
                     + "createContext():, "
                     + "cannot obtain application SSO token, "
                     + "defaulting to IP address");
             }
         } else {
                 sessionDebug.message("Session."
                     + "createContext():, "
                     + "setting context to  application SSO token");
                 context = appSSOToken;
         }
     }

   /**
     * Handle exception coming back from server in the Sessionresponse
     * @exception SessionException
     * @param sres SessionResponse object holding the exception
     */

    void processSessionResponseException(SessionResponse sres, SSOToken appSSOToken) throws SessionException {
        try {
            if (sessionDebug.messageEnabled()) {
                sessionDebug.message("Session."
                        + "processSessionResponseException: exception received"
                        + " from server:" + sres.getException());
            }
            // Check if this exception was thrown due to Session Time out or not
            // If yes then set the private variable timedOutAt to the current
            // time But before that check if this timedOutAt is already set
            // or not. No need of setting it again
            String exceptionMessage = sres.getException();
            if(timedOutAt <= 0) {
               if (exceptionMessage.indexOf("SessionTimedOutException") != -1) {
                    timedOutAt = currentTimeMillis() / 1000;
                }
            }
            if (exceptionMessage.indexOf(SessionBundle.getString(
                    "appTokenInvalid")) != -1)  {
                if (sessionDebug.messageEnabled()) {
                    sessionDebug.message("Session."
                        + "processSessionResponseException: AppTokenInvalid = TRUE");
                }

                if (!SystemProperties.isServerMode()) {
                    if (sessionDebug.messageEnabled()) {
                        sessionDebug.message("Session."
                            + "processSessionResponseException: Destroying AppToken");
                    }

                    AdminTokenAction.invalid();
                    RestrictedTokenContext.clear();

                    if (sessionDebug.warningEnabled()) {
                        sessionDebug.warning("Session."
                            +"processSessionResponseException"
                            +" processSessionResponseException"
                            +": server responded with app token invalid"
                            +" error,refetching the app sso token");
                    }
                    SSOToken newAppSSOToken = (SSOToken)
                        AccessController.doPrivileged(
                                AdminTokenAction.getInstance());

                    if (sessionDebug.messageEnabled()) {
                        sessionDebug.message("Session."
                            + "processSessionResponseException: creating New AppToken"
                            + " TokenID = " + newAppSSOToken);
                    }
                    createContext(newAppSSOToken);
                } else {
                    if (sessionDebug.messageEnabled()) {
                        sessionDebug.message("Session."
                            + "processSessionResponseException: AppToken invalid in" +
                              " server mode; throwing exception");
                    }
                    RestrictedTokenContext.clear();
                    throw new SessionException(sres.getException());
                }
            } else {
                throw new SessionException(sres.getException());
            }
        } catch (Exception ex) {
            throw new SessionException(ex);
        }
    }

    /**
     * Add listener to Internal Session.
     */
    public void addInternalSessionListener() {
        try {
            if (SessionNotificationHandler.handler == null) {
                SessionNotificationHandler.handler = new SessionNotificationHandler(SessionCache.getInstance());
                PLLClient.addNotificationHandler(SESSION_SERVICE,
                        SessionNotificationHandler.handler);
            }
            String url = WebtopNaming.getNotificationURL().toString();
            if (isLocal()) {
                sessionService.addSessionListener(sessionID, url);
            } else {
                SessionRequest sreq = new SessionRequest(
                        SessionRequest.AddSessionListener,
                        sessionID.toString(), false);
                sreq.setNotificationURL(url);
                requests.sendRequestWithRetry(getSessionServiceURL(), sreq, this);
            }
        } catch (Exception e) {
            //todo : something! :-D
        }
    }

    /**
     * Returns true if cookies are supported else false. The
     * <code>cookieSupport</code> value is first determined from the Session ID
     * object , if that is null then it is determined based on the cookie mode
     * value set in the Session object else <code>cookieSupport</code> value is
     * retrieved from the session property <code>cookieSupport</code>. If
     * cookie Support value is not determined then the the default "false" is
     * assumed.
     */
    public boolean getCookieSupport() {
        boolean cookieSupport = false;
        try {
            Boolean cookieMode = sessionID.getCookieMode();
            if (cookieMode != null) {
                cookieSupport = cookieMode.booleanValue();
            } else if (this.cookieMode != null) {
                cookieSupport = this.cookieMode.booleanValue();
            } else {
                String cookieSupportStr = getProperty("cookieSupport");
                if (cookieSupportStr != null) {
                    cookieSupport = cookieSupportStr.equals("true");
                }
            }
        } catch (Exception ex) {
            sessionDebug.error("Error getting cookieSupport value: ", ex);
            cookieSupport = true;
        }
        if (sessionDebug.messageEnabled()) {
            sessionDebug.message("Session: getCookieSupport: " + cookieSupport);
        }
        return cookieSupport;
    }

    /**
     * Set the cookie Mode based on whether the request has cookies or not. This
     * method is called from <code>createSSOToken(request)</code> method in
     * <code>SSOTokenManager</code>.
     *
     * @param cookieMode whether request has cookies or not.
     */
    public void setCookieMode(Boolean cookieMode) {
        if (sessionDebug.messageEnabled()) {
            sessionDebug.message("CookieMode is:" + cookieMode);
        }
        if (cookieMode != null) {
            this.cookieMode = cookieMode;
        }
    }

    /**
     * Indicates whether local or remote invocation of Sesion Service should be
     * used
     *
     * @return true if local invocation should be used, false otherwise
     */
    boolean isLocal() {
        return sessionIsLocal;
    }

    /**
     * Actively checks whether current session should be considered local (so
     * that local invocations of Session Service methods are to be used).
     *
     * @return true if the session local.
     */
    private boolean checkSessionLocal() throws SessionException {
        if (SystemProperties.isServerMode()) {
            return sessionService.checkSessionLocal(sessionID);
        } else {
            return false;
        }
    }

    public TokenRestriction getRestriction() throws SessionException {
        return restriction;
    }

    Object getContext() {
        return context;
    }

    /**
     * Returns a stable ID that can be used as a unique identifier when storing this session.
     *
     * @return a unique stable storage id.
     */
    @Override
    public String getStableStorageID() {
        return sessionID.getExtension().getStorageKey();
    }

    private boolean usingCachedBasedPolling() {
        return SystemProperties.getAsBoolean(CACHED_BASE_POLLING_PROPERTY, false);
    }
}

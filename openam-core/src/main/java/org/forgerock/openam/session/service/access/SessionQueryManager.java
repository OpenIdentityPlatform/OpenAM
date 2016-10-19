/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.session.service.access;

import static com.iplanet.dpro.session.service.SessionState.*;
import static org.forgerock.openam.utils.Time.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.dpro.session.PartialSession;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.session.authorisation.SessionChangeAuthorizer;
import org.forgerock.openam.session.service.access.persistence.SessionPersistenceStore;
import org.forgerock.openam.session.service.access.persistence.caching.InternalSessionCache;
import org.forgerock.openam.utils.CrestQuery;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionServiceConfig;
import com.iplanet.dpro.session.share.SessionBundle;
import com.iplanet.dpro.session.share.SessionInfo;
import com.sun.identity.common.DNUtils;
import com.sun.identity.common.SearchResults;
import com.sun.identity.shared.debug.Debug;

/**
 * Class responsible for allowing searches over groups of sessions. Does not return complete session objects,
 * but objects which capture some of the information within.
 */
public class SessionQueryManager {
    private Debug debug;
    private SessionPersistenceStore sessionPersistenceStore;
    private final InternalSessionCache internalSessionCache;
    private final SessionChangeAuthorizer sessionChangeAuthorizer;
    private final SessionServiceConfig serviceConfig;

    /**
     * Creates a session query manager.
     * @param debug The debug object.
     * @param sessionPersistenceStore The store which is being used for queries.
     * @param internalSessionCache The cache which is being used for queries (TODO: AME-12496 will remove this)
     */
    @Inject
    public SessionQueryManager(@Named(SessionConstants.SESSION_DEBUG) final Debug debug,
                               SessionPersistenceStore sessionPersistenceStore,
                               InternalSessionCache internalSessionCache,
                               SessionChangeAuthorizer sessionChangeAuthorizer,
                               SessionServiceConfig serviceConfig) {
        this.debug = debug;
        this.sessionPersistenceStore = sessionPersistenceStore;
        this.internalSessionCache = internalSessionCache;
        this.sessionChangeAuthorizer = sessionChangeAuthorizer;
        this.serviceConfig = serviceConfig;
    }

    /**
     * Return partial sessions matching the provided CREST query filter from the CTS servers.
     *
     * @param crestQuery The CREST query based on which we should look for matching sessions.
     * @return The collection of matching partial sessions.
     * @throws SessionException  If the request fails.
     */
    public Collection<PartialSession> getMatchingValidSessions(CrestQuery crestQuery) throws SessionException {
        try {
            return sessionPersistenceStore.searchPartialSessions(crestQuery);
        } catch (CoreTokenException cte) {
            debug.error("An error occurred whilst querying CTS for matching sessions", cte);
            throw new SessionException(cte);
        }
    }

    /**
     * Returns the expiration information of all sessions belonging to a user
     * (uuid). The returned value will be a Map (sid->expiration_time).
     *
     * @param uuid
     *            User's universal unique ID.
     * @return user Sessions
     * @exception SessionException
     *             if there is any problem with accessing the session
     *             repository.
     */
    public Map<String, Long> getAllSessionsByUUID(String uuid) throws SessionException {
        return sessionPersistenceStore.getAllSessionsByUUID(uuid);
    }

    /**
     * Gets all valid Internal Sessions, depending on the value of the user's
     * preferences.
     *
     * @param s
     * @throws SessionException
     */
    public SearchResults<SessionInfo> getValidSessions(Session s, String pattern) throws SessionException {
        if (s.getState(false) != VALID) {
            throw new SessionException(SessionBundle
                    .getString("invalidSessionState")
                    + s.getID().toString());
        }

        try {

            SearchResults<InternalSession> sessions = getValidInternalSessions(pattern);

            Collection<InternalSession> sessionsWithPermission = sessionChangeAuthorizer.filterPermissionToAccess(
                    s.getSessionID(), sessions.getSearchResults());


            Set<SessionInfo> infos = new HashSet<>(sessionsWithPermission.size());

            for (InternalSession session : sessionsWithPermission) {
                SessionInfo info = session.toSessionInfo();
                // replace session id with session handle to prevent impersonation
                info.setSessionID(session.getSessionHandle());
                infos.add(info);
            }

            return new SearchResults<>(sessions.getTotalResultCount(), infos, sessions.getErrorCode());
        } catch (Exception e) {
            throw new SessionException(e);
        }
    }

    /**
     * Get all valid Internal Sessions matched with pattern.
     */
    private SearchResults<InternalSession> getValidInternalSessions(String pattern)
            throws SessionException {
        Set<InternalSession> sessions = new HashSet<>();
        int errorCode = SearchResults.SUCCESS;

        if (pattern == null) {
            pattern = "*";
        }

        try {
            long startTime = currentTimeMillis();

            pattern = pattern.toLowerCase();
            List<InternalSession> allValidSessions = getValidInternalSessions();
            boolean matchAll = pattern.equals("*");

            for (InternalSession sess : allValidSessions) {
                if (!matchAll) {
                    // For application sessions, the client ID
                    // will not be in the DN format but just uid.
                    String clientID = (!sess.isAppSession()) ?
                            DNUtils.DNtoName(sess.getClientID()) :
                            sess.getClientID();

                    if (clientID == null) {
                        continue;
                    } else {
                        clientID = clientID.toLowerCase();
                    }

                    if (!matchFilter(clientID, pattern)) {
                        continue;
                    }
                }

                if (sessions.size() == serviceConfig.getMaxSessionListSize()) {
                    errorCode = SearchResults.SIZE_LIMIT_EXCEEDED;
                    break;
                }
                sessions.add(sess);

                if ((currentTimeMillis() - startTime) >=
                        serviceConfig.getSessionRetrievalTimeout()) {
                    errorCode = SearchResults.TIME_LIMIT_EXCEEDED;
                    break;
                }
            }
        } catch (Exception e) {
            debug.error("SessionService : "
                    + "Unable to get Session Information ", e);
            throw new SessionException(e);
        }
        return new SearchResults<>(sessions.size(), sessions, errorCode);
    }

    /**
     * Get all valid Internal Sessions.
     */
    private List<InternalSession> getValidInternalSessions() {

        List<InternalSession> sessions = new ArrayList<>();
        Collection<InternalSession> allSessions = getAllInternalSessions();
        for (InternalSession session : allSessions) {
            if (session.getState() == VALID
                    && (!session.isAppSession() || serviceConfig.isReturnAppSessionEnabled())) {
                sessions.add(session);
            }
        }
        return sessions;
    }

    /**
     * Returns true if the given pattern is contained in the string.
     *
     * @param string  to examine
     * @param pattern to match
     * @return true if string matches <code>filter</code>
     */
    private boolean matchFilter(String string, String pattern) {
        if (pattern.equals("*") || pattern.equals(string)) {
            return true;
        }

        int length = pattern.length();
        int wildCardIndex = pattern.indexOf("*");

        if (wildCardIndex >= 0) {
            String patternSubStr = pattern.substring(0, wildCardIndex);

            if (!string.startsWith(patternSubStr, 0)) {
                return false;
            }

            int beginIndex = patternSubStr.length() + 1;
            int stringIndex = 0;

            if (wildCardIndex > 0) {
                stringIndex = beginIndex;
            }

            String sub = pattern.substring(beginIndex, length);

            while ((wildCardIndex = pattern.indexOf("*", beginIndex)) != -1) {
                patternSubStr = pattern.substring(beginIndex, wildCardIndex);

                if (string.indexOf(patternSubStr, stringIndex) == -1) {
                    return false;
                }

                beginIndex = wildCardIndex + 1;
                stringIndex = stringIndex + patternSubStr.length() + 1;
                sub = pattern.substring(beginIndex, length);
            }

            if (string.endsWith(sub)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get all sessions in the internal session cache.
     * TODO: this method should be replaced with a call to the CTS. It may simply be able to return SessionInfos at that time.
     * @return a collection of all internal sessions in the internal session cache.
     */
    private Collection<InternalSession> getAllInternalSessions() {
        return internalSessionCache.getAllSessions();
    }
}

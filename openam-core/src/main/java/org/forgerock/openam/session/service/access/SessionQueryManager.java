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
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.session.service.access;

import static com.iplanet.dpro.session.service.SessionState.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.iplanet.dpro.session.SessionID;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.dpro.session.PartialSession;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.session.authorisation.SessionChangeAuthorizer;
import org.forgerock.openam.session.service.access.persistence.SessionPersistenceStore;
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
    private final SessionChangeAuthorizer sessionChangeAuthorizer;
    private final SessionServiceConfig serviceConfig;

    /**
     * Creates a session query manager.
     * @param debug The debug object.
     * @param sessionPersistenceStore The store which is being used for queries.
     */
    @Inject
    public SessionQueryManager(@Named(SessionConstants.SESSION_DEBUG) final Debug debug,
                               SessionPersistenceStore sessionPersistenceStore,
                               SessionChangeAuthorizer sessionChangeAuthorizer,
                               SessionServiceConfig serviceConfig) {
        this.debug = debug;
        this.sessionPersistenceStore = sessionPersistenceStore;
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
     * @param actingSession
     * @throws SessionException
     */
    public SearchResults<SessionInfo> getValidSessions(Session actingSession, String pattern) throws SessionException {
        if (actingSession.getState(false) != VALID) {
            throw new SessionException(SessionBundle.getString("invalidSessionState") + actingSession.getID().toString());
        }
        try {
            Collection<InternalSession> sessions = sessionPersistenceStore.getValidSessions();
            SessionID actorSessionID = actingSession.getSessionID();
            boolean isAdmin = sessionChangeAuthorizer.hasTopLevelAdminRole(actorSessionID);
            Set<String> orgs = sessionChangeAuthorizer.getSessionSubjectOrganisations(actorSessionID);
            Set<SessionInfo> infos = new HashSet<>(sessions.size());
            for (InternalSession session : sessions) {
                boolean include = session.isUserSession() || serviceConfig.isReturnAppSessionEnabled();
                if(include && sessionClientMatchesPattern(session, pattern)
                        && actorCanAccessSesion(isAdmin, orgs, session)) {
                    SessionInfo info = session.toSessionInfo();
                    // replace session id with session handle to prevent impersonation
                    info.setSessionID(session.getSessionHandle());
                    infos.add(info);
                }

            }
            return new SearchResults<>(infos.size(), infos, SearchResults.SUCCESS);
        } catch (Exception e) {
            throw new SessionException(e);
        }
    }

    /**
     * Checks if the session client matches the pattern
     */
    private boolean sessionClientMatchesPattern(InternalSession session, String pattern) {
        boolean matches = true;
        if (pattern != null && !pattern.equals("*")) {
            String clientId = (!session.isAppSession()) ? DNUtils.DNtoName(session.getClientID()) : session.getClientID();
            matches = clientId != null && matchFilter(clientId.toLowerCase(), pattern);
        }
        return matches;
    }

    /**
     * Checks if the acting session can access to the session
     * Derived based on the actors role and actors orgs
     */
    private boolean actorCanAccessSesion(boolean actorIsAadmin, Set<String> actorOrgs, InternalSession session) {
        return actorIsAadmin ? true : actorOrgs == null ?  false : actorOrgs.contains(session.getClientDomain());
    }

    /**
     * Returns true if the given pattern is contained in the string.
     *
     * @param string  to examine
     * @param pattern to match
     * @return true if string matches <code>filter</code>
     */
    private boolean matchFilter(String string, String pattern) {
        if (pattern == null || pattern.equals("*") || pattern.equals(string)) {
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
}

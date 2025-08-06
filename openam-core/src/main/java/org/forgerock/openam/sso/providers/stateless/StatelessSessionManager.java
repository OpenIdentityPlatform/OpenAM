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
* Copyright 2015-2016 ForgeRock AS.
* Portions copyright 2025 3A Systems LLC.
*/

package org.forgerock.openam.sso.providers.stateless;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

import org.forgerock.json.jose.exceptions.JwtRuntimeException;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.session.stateless.cache.StatelessJWTCache;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionServerConfig;
import com.iplanet.dpro.session.service.SessionServiceConfig;
import com.iplanet.dpro.session.share.SessionInfo;
import com.sun.identity.shared.debug.Debug;

/**
 * Responsible for creating StatelessSession and SessionInfo instances from JWT
 * tokens. These operations are crucial for the Stateless Session implementation.
 *
 * Also provides logic methods to determine whether a SessionID/Token ID or
 * HttpServletRequest contains a JWT token.
 *
 * @since 13.0.0
 */
public class StatelessSessionManager {

    private static final Debug debug = Debug.getInstance(SessionConstants.SESSION_DEBUG);

    // Injected
    private final StatelessJWTCache cache;
    private final SessionServerConfig sessionServerConfig;
    private final SessionServiceConfig sessionServiceConfig;

    /**
     * Guice initialised constructor.
     * @param cache Non null.
     * @param sessionServerConfig Non null.
     * @param sessionServiceConfig Non null.
     */
    @Inject
    public StatelessSessionManager(StatelessJWTCache cache,
                                   SessionServerConfig sessionServerConfig,
                                   SessionServiceConfig sessionServiceConfig) {
        this.cache = cache;
        this.sessionServerConfig = sessionServerConfig;
        this.sessionServiceConfig = sessionServiceConfig;
    }

    // Lazy initialised to avoid early init issues.
    private JwtSessionMapper getJwtSessionMapper() {
        return sessionServiceConfig.getJwtSessionMapper();
    }

    /**
     * @param request Non null.
     * @return True if the HttpServletRequest contained the Jwt Token.
     * @throws SessionException If there was any expected error.
     */
    public boolean containsJwt(HttpServletRequest request) throws SessionException {
        return containsJwt(new SessionID(request));
    }

    /**
     * Determines whether the given token id contains a client-side JWT.
     *
     * @param tokenId the token id to check.
     * @return {@code true} if the token contains a client-side session JWT.
     */
    public boolean containsJwt(String tokenId) {
        return containsJwt(new SessionID(tokenId));
    }

    /**
     * Determines whether the given session id contains a client-side JWT.
     *
     * @param sid the session id to check.
     * @return {@code true} if the session id contains a client-side session JWT.
     */
    public boolean containsJwt(SessionID sid) {
        return getJWTFromSessionID(sid, false) != null;
    }

    /**
     * Generates a StatelessSession based on the String which is assumed to contain
     * a Jwt token. This will be parsed and used to generate the StatelessSession.
     *
     * Side Effect: calls #getSessionInfo which may cache the JWT/SessionInfo.
     *
     * @param tokenId Non null TokenID to use for parsing.
     * @return A non null StatelessSessionID.
     * @throws SessionException
     */
    public StatelessSession generate(String tokenId) throws SessionException {
        return generate(new SessionID(tokenId));
    }

    /**
     * Will create the SessionInfo from the JWT contained within the
     * SessionID.
     *
     * Side Effect: Will cache the generated JWT and SessionInfo combination.
     *
     * @param sessionID Maybe null SessionID.
     *
     * @return SessionInfo Non null SessionInfo which corresponds to the SessionID.
     *
     * @throws SessionException If there was any problem with getting the SessionInfo
     * from the JWT within with SessionID
     */
    public SessionInfo getSessionInfo(SessionID sessionID) throws SessionException {
        String jwt = getJWTFromSessionID(sessionID, true);
        if (cache.contains(jwt)) {
            debug.message("StatelessSessionFactory.getSessionInfo: JWT {} found in cache", jwt);
            return cache.getSessionInfo(jwt);
        }

        SessionInfo sessionInfo;
        try {
            sessionInfo = getJwtSessionMapper().fromJwt(jwt);
        } catch (JwtRuntimeException e) {
            debug.message("StatelessSessionFactory.getSessionInfo: JWT {} Does not map to passed sessionID {}", jwt, sessionID, e);
            throw new SessionException(e);
        }
        cache.cache(sessionInfo, jwt);
        return sessionInfo;
    }

    /**
     * Generates a StatelessSession by converting the SessionInfo into a JWT token
     * which can then be used to build up the StatelessSession.
     *
     * Note: Will cache this generated JWT as a side effect.
     *
     * @param info The SessionInfo to generate the StatelessSession from. Non null.
     * @return A non null StatelessSession.
     * @throws SessionException If there was an unexpected error.
     */
    public StatelessSession generate(SessionInfo info) throws SessionException {
        // NB: We are not getting JWT from cache because info could have changed contents.
        // It is safer to generate the JWT based on the updated state of the SessionInfo.

        String jwt = getJwtSessionMapper().asJwt(info);
        SessionID sessionID = SessionID.generateStatelessSessionID(sessionServerConfig, info.getClientDomain(), jwt);
        cache.cache(info, jwt);

        return new StatelessSession(sessionID, info, this);
    }

    /**
     * Updates a stateless SessionID to reflect the new state of the session and removes the previous SessionID from
     * the cache.
     *
     * @param previousId the previous session id to remove from the cache.
     * @param newSessionInfo the new session state to reflect in the updated session ID.
     * @return the updated session ID.
     * @throws SessionException if there was an unexpected error.
     */
    SessionID updateSessionID(SessionID previousId, SessionInfo newSessionInfo) throws SessionException {
        cache.remove(getJWTFromSessionID(previousId, true));
        String jwt = getJwtSessionMapper().asJwt(newSessionInfo);
        return SessionID.generateStatelessSessionID(sessionServerConfig, newSessionInfo.getClientDomain(), jwt);
    }

    /**
     * @param internalSession Non null Internal Session to convert to a StatelessSession.
     * @return Non null StatelessSession.
     * @throws SessionException If anything unexpected failed.
     */
    public StatelessSession generate(InternalSession internalSession) throws SessionException {
        SessionInfo sessionInfo = internalSession.toSessionInfo(false);

        return generate(sessionInfo);
    }

    /**
     * @param sessionID Non null SessionID to convert to a StatelessSession.
     * @return Non null StatelessSession.
     * @throws SessionException If anything unexpected failed.
     */
    public StatelessSession generate(SessionID sessionID) throws SessionException {
        SessionInfo sessionInfo = getSessionInfo(sessionID);
        return new StatelessSession(sessionID, sessionInfo, this);
    }

    /**
     * Understands the detail around extracting the encoded JWT from the SessionID.
     * @param sessionID Possibly null SessionID.
     * @param cleanupC66Encoding Whether to undo damage done to the JWT by c66 decoding if detected.
     * @return Null if there was no JWT present, otherwise a valid JWT.
     */
    public static String getJWTFromSessionID(SessionID sessionID, boolean cleanupC66Encoding) {
        if (sessionID == null || sessionID.toString().isEmpty()) {
            return null;
        }
        String tail = sessionID.getTail();
        if (tail == null) {
            return null;
        }
        if (tail.isEmpty()) {
            return null;
        }
        if (cleanupC66Encoding && sessionID.isC66Encoded()) {
            // Undo damage done to the JWT by lossy c66 decoding:
            tail = tail.replace('=', '.').replace('/', '_').replace('+', '-');
        }
        return tail;
    }
}

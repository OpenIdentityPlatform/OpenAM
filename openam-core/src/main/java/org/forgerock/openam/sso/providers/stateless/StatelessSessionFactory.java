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
* Copyright 2015 ForgeRock AS.
*/

package org.forgerock.openam.sso.providers.stateless;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionConstants;
import com.iplanet.dpro.session.service.SessionServerConfig;
import com.iplanet.dpro.session.service.SessionServiceConfig;
import com.iplanet.dpro.session.share.SessionEncodeURL;
import com.iplanet.dpro.session.share.SessionInfo;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.CookieUtils;
import org.forgerock.openam.session.stateless.cache.StatelessJWTCache;
import org.forgerock.openam.utils.StringUtils;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

/**
 * Responsible for creating StatelessSession and SessionInfo instances from JWT
 * tokens. These operations are crucial for the Stateless Session implementation.
 *
 * Also provides logic methods to determine whether a SessionID/Token ID or
 * HttpServletRequest contains a JWT token.
 *
 * @since 13.0.0
 */
public class StatelessSessionFactory {

    private static final String cookieName = CookieUtils.getAmCookieName();
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
    public StatelessSessionFactory(StatelessJWTCache cache,
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
        String tokenId = tokenIdFromRequest(request);
        return isValidJwt(tokenId);
    }

    /**
     * Determines whether the given token id contains a client-side JWT.
     *
     * @param tokenId the token id to check.
     * @return {@code true} if the token contains a client-side session JWT.
     */
    public boolean containsJwt(String tokenId) {
        return isValidJwt(tokenId);
    }

    /**
     * Determines whether the given session id contains a client-side JWT.
     *
     * @param sid the session id to check.
     * @return {@code true} if the session id contains a client-side session JWT.
     */
    public boolean containsJwt(SessionID sid) {
        return getJWTFromSessionID(sid) != null;
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
        final SessionID sessionID = new SessionID(tokenId);
        SessionInfo sessionInfo = getSessionInfo(sessionID);
        return new StatelessSession(sessionID, sessionInfo);
    }

    /**
     * Will create the SessionInfo from the JWT contained within the
     * SessionID.
     *
     * Side Effect: Will cache the generated JWT and SessionInfo combination.
     *
     * @param sessionID Maybe null SessionID.
     * @return SessionInfo which corresponds to this SessionID.
     */
    public SessionInfo getSessionInfo(SessionID sessionID) {
        String jwt = getJWTFromSessionID(sessionID);
        if (cache.contains(jwt)) {
            return cache.getSessionInfo(jwt);
        }
        SessionInfo sessionInfo = getJwtSessionMapper().fromJwt(jwt);
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
        SessionID sessionID = SessionID.generateSessionID(sessionServerConfig, info.getClientDomain(), jwt);
        cache.cache(info, sessionID.toString());

        return new StatelessSession(sessionID, info);
    }

    /**
     * @param internalSession Non null Internal Session to convert to a StatelessSession.
     * @return Non null StatelessSession.
     * @throws SessionException If anything unexpected failed.
     */
    public StatelessSession generate(InternalSession internalSession) throws SessionException {
        SessionInfo sessionInfo = internalSession.toSessionInfo(false);

        sessionInfo.setSessionID(null);
        sessionInfo.setSecret(java.util.UUID.randomUUID().toString());

        sessionInfo.getProperties().put(
                org.forgerock.openam.session.SessionConstants.SESSION_HANDLE_PROP,
                internalSession.getSessionHandle());

        return generate(sessionInfo);
    }

    /**
     * @param sessionID Non null SessionID to convert to a StatelessSession.
     * @return Non null StatelessSession.
     * @throws SessionException If anything unexpected failed.
     */
    public StatelessSession generate(SessionID sessionID) throws SessionException {
        return generate(sessionID.toString());
    }

    /**
     * @param tokenId Possibly null, empty, or timed out JWT.
     * @return True if the TokenID JWT represents a valid SessionInfo which has not timed out.
     */
    private boolean isValidJwt(String tokenId) {
        if (StringUtils.isEmpty(tokenId)) {
            return false;
        }
        try {
            StatelessSession statelessSession;
            if (cache.contains(tokenId)) {
                /**
                 * NB: We cannot use the JWTCache to map in the reverse direction (SessionInfo-JWT)
                 * because the SessionInfo object can change contents, but remain the same reference
                 * in the cache. Therefore the only way to maintain consistent state is to generate
                 * the JWT from the SessionInfo each time.
                 *
                 * We can re-evaluate this if it becomes a hot-spot.
                 */
                statelessSession = generate(cache.getSessionInfo(tokenId));
            } else {
                SessionID sessionID = new SessionID(tokenId);
                if (!containsJwt(sessionID)) {
                    return false;
                }
                statelessSession = generate(sessionID);
            }
            return statelessSession.getTimeLeft() >= 0;
        } catch (SessionException e) {
            debug.message("Failed to validate JWT {0}", tokenId, e);
            return false;
        }
    }

    /**
     * Extracts the TokenID from a HttpServletRequest.
     *
     * Note: The following logic was taken from SessionID ctor taking HttpServletRequest (line 117)
     *
     * @param request Non null.
     * @return TokenID if the Cookie Name is defined in system properties, and either the
     * request is a forward request or simply contains the appropriate cookie.
     */
    private static String tokenIdFromRequest(HttpServletRequest request) {
        String cookieValue;
        String tokenId = null;

        if (cookieName != null) {
            // check if this is a forward from authentication service case.
            // if yes, find Session ID in the request URL first, otherwise
            // find Session ID in the cookie first
            String isForward = (String) request.getAttribute(Constants.FORWARD_PARAM);
            debug.message("SessionID(HttpServletRequest) : is forward = {0}", isForward);
            if ((isForward != null) && isForward.equals(Constants.FORWARD_YES_VALUE)) {
                String realReqSid = SessionEncodeURL.getSidFromURL(request);
                if (realReqSid != null) {
                    tokenId = realReqSid;
                } else {
                    cookieValue = CookieUtils.getCookieValueFromReq(request, cookieName);
                    if (cookieValue != null) {
                        tokenId = cookieValue;
                    }
                }
            } else {
                cookieValue = CookieUtils.getCookieValueFromReq(request, cookieName);

                // if no cookie found in the request then check if the URL has it.
                if (cookieValue == null) {
                    String realReqSid = SessionEncodeURL.getSidFromURL(request);
                    if (realReqSid != null) {
                        tokenId = realReqSid;
                    }
                } else {
                    tokenId = cookieValue;
                }
            }
        }

        return tokenId;
    }

    /**
     * Understands the detail around extracting the encoded JWT from the SessionID.
     * @param sessionID Possibly null SessionID.
     * @return Null if there was no JWT present, otherwise a valid JWT.
     */
    public static String getJWTFromSessionID(SessionID sessionID) {
        if (sessionID == null) {
            return null;
        }
        String tail = sessionID.getTail();
        if (tail == null) {
            return null;
        }
        if (tail.isEmpty()) {
            return null;
        }
        return tail.replace('=', '.'); // undo damage done to JWT by c66 decoding
    }
}

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

package org.forgerock.openam.core.rest.session;

import static org.forgerock.json.JsonValue.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.services.naming.WebtopNamingQuery;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.rest.session.query.SessionQueryManager;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.utils.StringUtils;

/**
 * A Util class with common functions used by SessionResource, SessionResourceV2 and ActionHandlers
 */
public class SessionResourceUtil {

    private static final Debug LOGGER = Debug.getInstance(SessionConstants.SESSION_DEBUG);


    public static final String VALID = "valid";
    public static final String IDLE_TIME = "idletime";
    public static final String REALM = "realm";
    public static final String UID = "uid";
    public static final String MAX_IDLE_TIME = "maxidletime";
    public static final String MAX_SESSION_TIME = "maxsessiontime";
    public static final String MAX_TIME = "maxtime";
    public static final String HEADER_USER_ID = "userid";
    public static final String HEADER_TIME_REMAINING = "timeleft";
    public static final String SESSION_INFO_USER_ID = "UserId";

    private final SSOTokenManager ssoTokenManager;
    private final SessionQueryManager queryManager;
    private final WebtopNamingQuery webtopNamingQuery;

    /**
     * Creates an instance of the SessionResourceUtil
     *
     * @param ssoTokenManager An instance of the SSOTokenManager.
     * @param sessionQueryManager An instance of the SessionQueryManager. Must not null.
     * @param webtopNamingQuery An Instance of the WebtopNamingQuery.
     */
    @Inject
    public SessionResourceUtil(final SSOTokenManager ssoTokenManager,
            final SessionQueryManager sessionQueryManager, final WebtopNamingQuery webtopNamingQuery) {
        this.ssoTokenManager = ssoTokenManager;
        this.queryManager = sessionQueryManager;
        this.webtopNamingQuery = webtopNamingQuery;
    }

    /**
     * tokenId may, or may not, specify a valid token.  If it does, retrieve it and the carefully refresh it so
     * as not to alter its idle time setting.  If it does not exist, or is invalid, throw an SSOException.
     *
     * @param tokenId The id of the token to retrieve and cautiously refresh
     * @return a valid SSOToken
     * @throws SSOException if the token id does not identify a valid token
     */
    public SSOToken getTokenWithoutResettingIdleTime(String tokenId) throws SSOException {
        SSOToken ssoToken = null;
        if (StringUtils.isNotEmpty(tokenId)) {

            ssoToken = ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime(tokenId);
            if (ssoToken != null) {
                // Most important to not call refreshSession here as that may update the idle time
                ssoTokenManager.refreshSessionWithoutIdleReset(ssoToken);

                // if the token is not valid after all, forget we saw it.
                if (!ssoTokenManager.isValidToken(ssoToken, false)) {
                    ssoToken = null;
                }
            }
        }
        if (ssoToken == null) {
            throw new SSOException("The tokenId " + tokenId + " is not valid");
        }
        return ssoToken;
    }

    /**
     * Returns a collection of all Server ID that are known to the OpenAM instance.
     *
     * @return A non null, possibly empty collection of server ids.
     */
    public Collection<String> getAllServerIds() {
        try {
            return webtopNamingQuery.getAllServerIDs();
        } catch (Exception e) {
            LOGGER.error("SessionResource.getAllServerIds() :: WebtopNaming throw irrecoverable error.");
            throw new IllegalStateException("Cannot recover from this error", e);
        }
    }

    /**
     * @param serverId Server to query.
     * @return A non null collection of SessionInfos from the named server.
     */
    public Collection<SessionInfo> generateNamedServerSession(String serverId) {
        List<String> serverList = Arrays.asList(new String[]{serverId});
        Collection<SessionInfo> sessions = queryManager.getAllSessions(serverList);
        if (LOGGER.messageEnabled()) {
            LOGGER.message("SessionResource.generateNmaedServerSession :: retrieved session list for server, " +
                    serverId);
        }
        return sessions;
    }

    /**
     * @return A non null collection of SessionInfo instances queried across all servers.
     */
    public Collection<SessionInfo> generateAllSessions() {
        Collection<SessionInfo> sessions = queryManager.getAllSessions(getAllServerIds());
        if (LOGGER.messageEnabled()) {
            LOGGER.message("SessionResource.generateNmaedServerSession :: retrieved session list for all servers.");
        }
        return sessions;
    }

    /**
     * Creates the JsonValue representing the single sign on token
     *
     * @param ssoToken The single sign on token
     * @return The json value representation
     * @throws IdRepoException If single sign on token is invalid
     * @throws SSOException If single sign on token does not have a valid identifier
     */
    public JsonValue jsonValueOf(SSOToken ssoToken) throws IdRepoException, SSOException {
        AMIdentity identity = getIdentity(ssoToken);
        return json(
                object(
                        field(UID, identity.getName()),
                        field(REALM, convertDNToRealm(identity.getRealm())),
                        field(IDLE_TIME, ssoToken.getIdleTime()),
                        field(MAX_IDLE_TIME, ssoToken.getMaxIdleTime()),
                        field(MAX_SESSION_TIME, ssoToken.getMaxSessionTime()),
                        field(MAX_TIME, ssoToken.getTimeLeft())
                )
        );
    }

    /**
     * Creates a AMIdentity from the specified SSOToken.
     *
     * @param ssoToken The SSOToken.
     * @return The AMIdentity.
     * @throws IdRepoException If a problem occurs creating the AMIdentity.
     * @throws SSOException If a problem occurs creating the AMIdentity.
     */
    public AMIdentity getIdentity(SSOToken ssoToken) throws IdRepoException, SSOException {
        return new AMIdentity(ssoToken);
    }

    /**
     * Returns realm name in "/" separated format for the provided
     * realm/organization name in DN format.
     *
     * Null or empty orgName would return "/"
     *
     * @param orgName Name of organization.
     * @return DN format "/" separated realm name of organization name.
     */
    public String convertDNToRealm(String orgName) {
        return DNMapper.orgNameToRealmName(orgName);
    }

    /**
     * Creates the JsonValue representing the SessionInfo
     *
     * @param session The session Info
     * @return The json value representing the SessionInfo
     */
    public JsonValue jsonValueOf(SessionInfo session) {
        return json(
                object(
                        field(HEADER_USER_ID, session.getProperties().get(SESSION_INFO_USER_ID)),
                        field(HEADER_TIME_REMAINING, TimeUnit.SECONDS.toMinutes(session.getTimeLeft()))
                )
        );
    }

    /**
     * Creates a json representation to denote invalis session
     *
     * @return The json representing invalis session
     */
    public JsonValue invalidSession() {
        return json(object(field(VALID, false)));
    }
}

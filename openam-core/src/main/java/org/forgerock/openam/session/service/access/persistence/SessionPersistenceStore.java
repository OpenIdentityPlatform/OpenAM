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

package org.forgerock.openam.session.service.access.persistence;

import static org.forgerock.openam.session.SessionConstants.*;
import static org.forgerock.util.time.Duration.duration;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.collect.ImmutableMap;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.JsonPointer;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.adapters.SessionAdapter;
import org.forgerock.openam.cts.api.fields.SessionTokenField;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.filter.TokenFilterBuilder;
import org.forgerock.openam.cts.api.filter.TokenFilterBuilder.FilterAttributeBuilder;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.api.tokens.TokenIdFactory;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.dpro.session.PartialSession;
import org.forgerock.openam.dpro.session.PartialSessionFactory;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.CrestQuery;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openam.utils.TimeUtils;
import org.forgerock.util.Reject;
import org.forgerock.util.query.QueryFilter;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionConstraint;
import com.iplanet.dpro.session.service.InternalSessionEventBroker;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.dpro.session.service.SessionServiceConfig;
import com.iplanet.dpro.session.service.SessionState;
import com.sun.identity.session.util.SessionUtilsWrapper;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;

/**
 * This class is responsible for loading sessions from the CTS.
 */
public class SessionPersistenceStore {

    private static final boolean caseSensitiveUUID = SystemProperties.getAsBoolean(Constants.CASE_SENSITIVE_UUID);

    private static final Map<String, CoreTokenField> JSON_TO_CTS_MAP = ImmutableMap.<String, CoreTokenField>builder()
            .put(JSON_SESSION_USERNAME, CoreTokenField.USER_ID)
            .put(JSON_SESSION_UNIVERSAL_ID, CoreTokenField.USER_ID)
            .put(JSON_SESSION_REALM, SessionTokenField.REALM.getField())
            .put(JSON_SESSION_HANDLE, SessionTokenField.SESSION_HANDLE.getField())
            .put(JSON_SESSION_LATEST_ACCESS_TIME, SessionTokenField.LATEST_ACCESS_TIME.getField())
            .put(JSON_SESSION_MAX_IDLE_EXPIRATION_TIME, SessionTokenField.MAX_IDLE_EXPIRATION_TIME.getField())
            .put(JSON_SESSION_MAX_SESSION_EXPIRATION_TIME, SessionTokenField.MAX_SESSION_EXPIRATION_TIME.getField())
            .build();
    private final Debug debug;
    private final CTSPersistentStore coreTokenService;
    private final SessionAdapter tokenAdapter;
    private final TokenIdFactory tokenIdFactory;
    private final SessionServiceConfig sessionServiceConfig;
    private final PartialSessionFactory partialSessionFactory;
    private final IdentityUtils identityUtils;

    @Inject
    public SessionPersistenceStore(@Named(SessionConstants.SESSION_DEBUG) final Debug debug,
                                   final CTSPersistentStore coreTokenService,
                                   final SessionAdapter tokenAdapter,
                                   final TokenIdFactory tokenIdFactory,
                                   final SessionServiceConfig sessionServiceConfig,
                                   final PartialSessionFactory partialSessionFactory,
                                   final IdentityUtils identityUtils) {
        this.debug = debug;
        this.coreTokenService = coreTokenService;
        this.tokenAdapter = tokenAdapter;
        this.tokenIdFactory = tokenIdFactory;
        this.sessionServiceConfig = sessionServiceConfig;
        this.partialSessionFactory = partialSessionFactory;
        this.identityUtils = identityUtils;
    }

    /**
     * Persist the provided session to the CTS, or update it if it is already there.
     * @param session The session to persist.
     * @throws CoreTokenException If the operation fails.
     */
    public void save(InternalSession session) throws CoreTokenException {
        coreTokenService.update(tokenAdapter.toToken(session));
    }

    /**
     * Remove the provided session from the CTS.
     * @param sessionID The id of the session to delete from the CTS.
     * @throws CoreTokenException If the operation fails.
     */
    public void delete(SessionID sessionID) throws CoreTokenException {
        String tokenId = tokenIdFactory.toSessionTokenId(sessionID);
        coreTokenService.delete(tokenId);
    }

    /**
     * This will recover the specified session from the repository based on the provided session id.
     * Returns null if no session was recovered.
     * @param sessionID Session ID
     */
    public InternalSession recoverSession(SessionID sessionID) {

        String tokenId = tokenIdFactory.toSessionTokenId(sessionID);
        Token token = null;

        try {
            token = coreTokenService.read(tokenId);
        } catch (CoreTokenException e) {
            debug.error("Failed to retrieve session by its handle", e);
        }
        if (token == null) {
            return null;
        }

        return getInternalSessionFromToken(token);
    }

    /**
     * This will recover the specified session from the repository based on the provided session handle.
     * Returns null if no session was recovered.
     * @param sessionHandle Session Handle
     */
    public InternalSession recoverSessionByHandle(String sessionHandle) {

        final TokenFilter tokenFilter = new TokenFilterBuilder()
                .withAttribute(SessionTokenField.SESSION_HANDLE.getField(), sessionHandle)
                .build();

        Token token = null;

        try {
            final Collection<Token> results = coreTokenService.query(tokenFilter);
            if (results.isEmpty()) {
                return null;
            }
            if (results.size() != 1) {
                debug.error("Duplicate session handle found in Core Token Service");
                return null;
            }
            token = CollectionUtils.getFirstItem(results);
        } catch (CoreTokenException e) {
            debug.error("Failed to retrieve session by its handle", e);
        }
        if (token == null) {
            return null;
        }
        return getInternalSessionFromToken(token);
    }

    /**
     * Return partial sessions matching the provided CREST query filter from the CTS servers.
     *
     * @param crestQuery The CREST query based on which we should look for matching sessions.
     * @return The collection of matching partial sessions.
     * @throws CoreTokenException If the partial query CTS call fails.
     */
    public Collection<PartialSession> searchPartialSessions(CrestQuery crestQuery) throws CoreTokenException {
        final QueryFilter<JsonPointer> queryFilter = crestQuery.getQueryFilter();
        Reject.ifNull(queryFilter, "Query Filter must be specified in the request");
        FilterAttributeBuilder filterAttributeBuilder = new TokenFilterBuilder()
                .withSizeLimit(sessionServiceConfig.getMaxSessionListSize())
                .withTimeLimit(duration(10, TimeUnit.SECONDS)).and();
        queryFilter.accept(new SessionQueryFilterVisitor(identityUtils), filterAttributeBuilder);
        final TokenFilter tokenFilter = filterAttributeBuilder.build();
        Reject.ifTrue(QueryFilter.and().equals(tokenFilter.getQuery()), "The query filter must specify the realm");

        filterAttributeBuilder.withAttribute(SessionTokenField.SESSION_STATE.getField(), SessionState.VALID.toString());
        addFieldsToFilter(filterAttributeBuilder, crestQuery.getFields());
        Collection<PartialSession> results;
        final Collection<PartialToken> partialTokens = coreTokenService.attributeQuery(
                tokenFilter);
        results = new ArrayList<>(partialTokens.size());
        for (PartialToken partialToken : partialTokens) {
            results.add(partialSessionFactory.fromPartialToken(partialToken));
        }
        return results;
    }

    private void addFieldsToFilter(FilterAttributeBuilder filterAttributeBuilder, List<JsonPointer> fields) {
        if (CollectionUtils.isNotEmpty(fields)) {
            for (JsonPointer field : fields) {
                CoreTokenField coreTokenField = JSON_TO_CTS_MAP.get(field.leaf());
                if (coreTokenField != null) {
                    filterAttributeBuilder.returnAttribute(coreTokenField);
                }
            }
        } else {
            for (CoreTokenField coreTokenField : JSON_TO_CTS_MAP.values()) {
                filterAttributeBuilder.returnAttribute(coreTokenField);
            }
        }
    }

    private InternalSession getInternalSessionFromToken(Token token) {

        /*
         * As a side effect of deserialising an InternalSession, we must trigger
         * the InternalSession to reschedule its timing task to ensure it
         * maintains the session expiry function.
         */
        InternalSession session = tokenAdapter.fromToken(token);
        session.setSessionServiceDependencies(InjectorHolder.getInstance(SessionService.class),
                InjectorHolder.getInstance(SessionServiceConfig.class),
                InjectorHolder.getInstance(InternalSessionEventBroker.class),
                InjectorHolder.getInstance(SessionUtilsWrapper.class),
                InjectorHolder.getInstance(SessionConstraint.class),
                debug);
        return session;
    }

    /**
     * Get a restricted session from a given SessionID.
     *
     * @param sessionID the ID of the restricted session to retrieve
     * @return a restricted Internal Session
     */
    public InternalSession getByRestrictedID(SessionID sessionID) {
        if (sessionID == null || StringUtils.isEmpty(sessionID.toString()) || sessionID.isSessionHandle()) {
            return null;
        }

        try {
            Collection<Token> collection =
                    coreTokenService.query(new TokenFilterBuilder().withQuery(
                            QueryFilter.contains(
                                    SessionTokenField.RESTRICTED_TOKENS.getField(),
                                    sessionID.toString()))
                            .build());

            if (CollectionUtils.isEmpty(collection)) {
                return null;
            }
            Token token = CollectionUtils.getFirstItem(collection);
            if (null == token) {
                return null;
            }
            return getInternalSessionFromToken(token);
        } catch (CoreTokenException e) {
            debug.error("Failed to retrieve restricted session by restricted ID: {}", sessionID.toString(), e);
        }

        return null;
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
        if (!caseSensitiveUUID) {
            uuid = uuid.toLowerCase();
        }

        Map<String, Long> sessions;
        try {
            sessions = getSessionsFromRepository(uuid);
        } catch (CoreTokenException e) {
            throw new SessionException(e);
        }

        if (sessions == null) {
            sessions = Collections.emptyMap();
            debug.error("Error: Unable to determine session count for user: " + uuid +
                    " returning empty map");
        }

        return sessions;
    }

    private Map<String, Long> getSessionsFromRepository(String uuid) throws CoreTokenException {

        try {
            // Filter and Query the CTS
            TokenFilter filter = new TokenFilterBuilder()
                    .returnAttribute(SessionTokenField.SESSION_ID.getField())
                    .returnAttribute(CoreTokenField.EXPIRY_DATE)
                    .and()
                    .withAttribute(CoreTokenField.USER_ID, uuid)
                    .build();
            Collection<PartialToken> partialTokens = coreTokenService.attributeQuery(filter);

            if (debug.messageEnabled()) {
                debug.message(MessageFormat.format(
                        "getSessionsFromRepository query success:\n" +
                                "Query: {0}\n" +
                                "Count: {1}",
                        filter,
                        partialTokens.size()));
            }

            // Populate the return Map from the query results.
            Map<String, Long> sessions = new HashMap<String, Long>();
            for (PartialToken partialToken : partialTokens) {
                // Session ID
                String sessionId = partialToken.getValue(SessionTokenField.SESSION_ID.getField());

                // Expiration Date converted to Unix Time
                Calendar timestamp = partialToken.getValue(CoreTokenField.EXPIRY_DATE);
                long unixTime = TimeUtils.toUnixTime(timestamp);

                sessions.put(sessionId, unixTime);
            }

            if (debug.messageEnabled()) {
                debug.message(MessageFormat.format(
                        "getSessionsFromRepository query results:\n" +
                                "{0}",
                        sessions));
            }

            return sessions;
        } catch (CoreTokenException e) {
            debug.error("SessionCount.getSessionsFromRepository: "+
                    "Session repository is not available", e);
            throw e;
        }
    }

    /**
     * Returns all the valid sessions from the CTS server
     *
     * @return The collection of all valid sessions
     * @throws CoreTokenException if the call to CTS to get valid session fails
     */
    public Collection<InternalSession> getValidSessions() throws CoreTokenException {
        TokenFilter filter = new TokenFilterBuilder()
                .withSizeLimit(sessionServiceConfig.getMaxSessionListSize())
                .withTimeLimit(duration(10, TimeUnit.SECONDS)).and()
                .withAttribute(SessionTokenField.SESSION_STATE.getField(), SessionState.VALID.toString())
                .build();
        Collection<Token> tokens = coreTokenService.query(filter);
        List<InternalSession> results = new ArrayList<>(tokens.size());
        for (Token token : tokens) {
            results.add(getInternalSessionFromToken(token));
        }
        return results;
    }
}

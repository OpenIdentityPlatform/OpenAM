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
 * Copyright 2013-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */
package org.forgerock.openam.cts.adapters;

import static java.util.concurrent.TimeUnit.*;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.inject.Inject;

import org.forgerock.openam.core.DNWrapper;
import org.forgerock.openam.cts.CoreTokenConfig;
import org.forgerock.openam.cts.api.fields.SessionTokenField;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.api.tokens.TokenIdFactory;
import org.forgerock.openam.cts.utils.JSONSerialisation;
import org.forgerock.openam.cts.utils.blob.TokenBlobUtils;
import org.forgerock.openam.cts.utils.blob.strategies.AttributeCompressionStrategy;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.CrestQuery;
import org.forgerock.openam.utils.TimeUtils;
import org.forgerock.util.annotations.VisibleForTesting;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.sun.identity.session.util.SessionUtils;

/**
 * SessionAdapter is responsible for providing conversions to and from InternalSession
 * and managing the details around data conversion for this class.
 */
public class SessionAdapter implements TokenAdapter<InternalSession> {

    // Injected
    private final TokenIdFactory tokenIdFactory;
    private final CoreTokenConfig config;
    private final JSONSerialisation serialisation;
    private final TokenBlobUtils blobUtils;
    private final DNWrapper dnWrapper;

    /**
     * The field name Pattern is required for internal Session JSON fudging.
     */
    private static final Pattern LATEST_ACCESSED_TIME = getLatestAccessedTimeRegexp();

    /**
     * Creates a default instance with dependencies defined.
     *
     * @param tokenIdFactory Non null.
     * @param config Non null.
     * @param serialisation Non null.
     * @param blobUtils A collection of Binary Object utilities.
     */
    @Inject
    public SessionAdapter(TokenIdFactory tokenIdFactory, CoreTokenConfig config, JSONSerialisation serialisation,
            TokenBlobUtils blobUtils, DNWrapper dnWrapper) {
        this.tokenIdFactory = tokenIdFactory;
        this.config = config;
        this.serialisation = serialisation;
        this.blobUtils = blobUtils;
        this.dnWrapper = dnWrapper;
    }

    /**
     * Convert from InternalSession to a Token.
     *
     * The InternalSession SessionID instance provides the primary key for the Token.
     *
     * Expiry time is a combination of the InternalSession expiration time and a grace
     * period.
     *
     * Realm is stored in a searchable attribute as it is used by
     * {@link com.iplanet.dpro.session.service.SessionService#getMatchingSessions(Session, CrestQuery)} to find all
     * sessions in a given realm. As such this attribute is stored in the realm name format for ease of use.
     *
     * @param session Non null.
     * @return Non null populated Token.
     */
    public Token toToken(InternalSession session) {
        String tokenId = tokenIdFactory.toSessionTokenId(session);
        Token token = new Token(tokenId, TokenType.SESSION);

        // User Id
        String userId = config.getUserId(session);
        token.setUserId(userId);

        // Session state
        String state = session.getState().name();
        token.setAttribute(SessionTokenField.SESSION_STATE.getField(), state);

        // Expiry Date
        Calendar expiryTimeStamp = TimeUtils.fromUnixTime(
                session.getExpirationTime(MILLISECONDS) + config.getSessionExpiryGracePeriod(MILLISECONDS),
                MILLISECONDS);
        token.setExpiryTimestamp(expiryTimeStamp);

        // Max session expiration time
        setDateAttributeFromMillis(token,
                SessionTokenField.MAX_SESSION_EXPIRATION_TIME,
                session.getMaxSessionExpirationTime(MILLISECONDS));

        // Max idle expiration time
        setDateAttributeFromMillis(token,
                SessionTokenField.MAX_IDLE_EXPIRATION_TIME,
                session.getMaxIdleExpirationTime(MILLISECONDS));

        // Realm value
        token.setAttribute(SessionTokenField.REALM.getField(), dnWrapper.orgNameToRealmName(session.getClientDomain()));

        // SessionID
        token.setAttribute(SessionTokenField.SESSION_ID.getField(), SessionUtils.getEncrypted(session.getID().toString()));

        // Binary data
        String jsonBlob = serialisation.serialise(session);
        blobUtils.setBlobFromString(token, jsonBlob);

        String latestAccessTime = filterLatestAccessTime(token);
        if (latestAccessTime != null) {
            token.setAttribute(SessionTokenField.LATEST_ACCESS_TIME.getField(), latestAccessTime);
        }

        // Restricted Tokens
        if (CollectionUtils.isNotEmpty(session.getRestrictedTokens())) {
            setRestrictedTokens(token, session);
        }

        // Session handle
        token.setAttribute(SessionTokenField.SESSION_HANDLE.getField(), SessionUtils.getEncrypted(session.getSessionHandle()));

        //"am.protected.oauth2.uid"
        final String tokenUid=session.getProperty("am.protected.oauth2.uid");
        if (tokenUid!=null) {
        	token.setAttribute(CoreTokenField.STRING_FOURTEEN, tokenUid.toLowerCase());
        }
        
        return token;
    }

    private void setRestrictedTokens(Token token, InternalSession session) {
        for (SessionID restrictedToken : session.getRestrictedTokens()) {
            token.setMultiAttribute(SessionTokenField.RESTRICTED_TOKENS.getField(), restrictedToken.toString());
        }
    }

    @VisibleForTesting
    static void setDateAttributeFromMillis(Token token, SessionTokenField field, long millis) {
        token.setAttribute(field.getField(), TimeUtils.fromUnixTime(millis, MILLISECONDS));
    }

    /**
     * Convert from a Token to an Internal Session.
     *
     * Simply deserialise the InternalSession from the JSON blob.
     *
     * @param token Token to be converted back to its original format.
     * @return Non null InternalSession.
     */
    public InternalSession fromToken(Token token) {
        String jsonBlob = blobUtils.getBlobAsString(token);
        int index = findIndexOfValidField(jsonBlob);

        // Do we need to insert the LatestAccessTime Into the Blob?
        String latestAccessTime = token.getAttribute(SessionTokenField.LATEST_ACCESS_TIME.getField());
        if (latestAccessTime != null && index != -1) {
            // Assemble the Sting to insert
            // latestAccessTime
            String fieldName = SessionTokenField.LATEST_ACCESS_TIME.getInternalSessionFieldName();
            // "latestAccessTime":
            String jsonField = JSONSerialisation.jsonAttributeName(fieldName);
            // "latestAccessTime":12345,
            String addition = jsonField + latestAccessTime + ",";

            // Insert the string into the JSON Blob
            jsonBlob = jsonBlob.substring(0, index) + addition + jsonBlob.substring(index, jsonBlob.length());
        }

        InternalSession session = serialisation.deserialise(jsonBlob, InternalSession.class);
        if (session.getSessionHandle() == null) {
            //Originally the sessionHandle was stored in the serialize token, so if after the deserialization the
            //sessionHandle field is not set, then we should attempt to retrieve the value directly from the token.
            session.setSessionHandle(SessionUtils.getDecrypted(token.<String>getAttribute(SessionTokenField.SESSION_HANDLE.getField())));
        }
        return session;
    }

    /**
     * Search the JSON blob contents and locate a valid field within the JSON.
     *
     * @param blob The serialised JSON blob string to search.
     * @return -1 if no fields were found, or the index of the start position of a valid JSON field.
     */
    int findIndexOfValidField(String blob) {
        for (Field field : AttributeCompressionStrategy.getAllValidFields(InternalSession.class)) {
            String search = JSONSerialisation.jsonAttributeName(field.getName());
            int index = blob.indexOf(search);
            if (index != -1) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Latest Accessed Time is a tricky field as it is internal to the InternalSession and only
     * accessible when the Token has been serialised.
     *
     * @param token Token which will be examined for the serialised field. Non null.
     */
    String filterLatestAccessTime(Token token) {
        String contents = blobUtils.getBlobAsString(token);
        Matcher matcher = LATEST_ACCESSED_TIME.matcher(contents);
        if (!matcher.find()) {
            return null;
        }

        String latestAccessTime = matcher.group(1);
        contents = contents.substring(0, matcher.start()) + contents.substring(matcher.end(), contents.length());
        blobUtils.setBlobFromString(token, contents);

        return latestAccessTime;
    }

    /**
     * Helper function to simplify code around this Pattern.
     * @return Non null.
     */
    private static Pattern getLatestAccessedTimeRegexp() {
        return Pattern.compile(
                JSONSerialisation.jsonAttributeName(
                        SessionTokenField.LATEST_ACCESS_TIME.getInternalSessionFieldName())
                        + "\\s*([0-9]+),?");
    }
}

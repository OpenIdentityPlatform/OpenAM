/**
 * Copyright 2013 ForgeRock, Inc.
 *
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
 */
package org.forgerock.openam.cts.adapters;

import javax.inject.Inject;
import com.iplanet.dpro.session.service.InternalSession;
import org.forgerock.openam.cts.CoreTokenConfig;
import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.api.fields.SessionTokenField;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.api.tokens.TokenIdFactory;
import org.forgerock.openam.cts.utils.JSONSerialisation;
import org.forgerock.openam.cts.utils.LDAPDataConversion;
import org.forgerock.openam.cts.utils.blob.TokenBlobUtils;
import org.forgerock.openam.cts.utils.blob.strategies.AttributeCompressionStrategy;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SessionAdapter is responsible for providing conversions to and from InternalSession
 * and managing the details around data conversion for this class.
 *
 * @author robert.wapshott@forgerock.com
 */
public class SessionAdapter implements TokenAdapter<InternalSession> {

    // Injected
    private final TokenIdFactory tokenIdFactory;
    private final CoreTokenConfig config;
    private final JSONSerialisation serialisation;
    private final LDAPDataConversion dataConversion;
    private final TokenBlobUtils blobUtils;

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
     * @param blobUtils
     */
    @Inject
    public SessionAdapter(TokenIdFactory tokenIdFactory, CoreTokenConfig config,
                          JSONSerialisation serialisation, LDAPDataConversion dataConversion,
                          TokenBlobUtils blobUtils) {
        this.tokenIdFactory = tokenIdFactory;
        this.config = config;
        this.serialisation = serialisation;
        this.dataConversion = dataConversion;
        this.blobUtils = blobUtils;
    }

    /**
     * Convert from InternalSession to a Token.
     *
     * The InternalSession SessionID instance provides the primary key for the Token.
     *
     * Expiry time is a combination of the InternalSession expiration time and a grace
     * period.
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

        // Expiry Date
        long epochedTimeInSeconds = session.getExpirationTime() + config.getSessionExpiryGracePeriod();
        Calendar expiryTimeStamp = dataConversion.fromEpochedSeconds(epochedTimeInSeconds);
        token.setExpiryTimestamp(expiryTimeStamp);

        // Binary data
        String jsonBlob = serialisation.serialise(session);
        blobUtils.setBlobFromString(token, jsonBlob);

        String latestAccessTime = filterLatestAccessTime(token);
        if (latestAccessTime != null) {
            token.setAttribute(SessionTokenField.LATEST_ACCESS_TIME.getField(), latestAccessTime);
        }

        return token;
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
        String latestAccessTime = token.getValue(SessionTokenField.LATEST_ACCESS_TIME.getField());
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

        return serialisation.deserialise(jsonBlob, InternalSession.class);
    }

    /**
     * Search the JSON blob contents and locate a valid field within the JSON.
     *
     * @param blob The serialised JSON blob string to search.
     * @return -1 if no fields were found, or the index of the start position of a valid JSON field.
     */
    public int findIndexOfValidField(String blob) {
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
    public String filterLatestAccessTime(Token token) {
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

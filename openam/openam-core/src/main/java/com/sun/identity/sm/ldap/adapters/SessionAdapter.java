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
package com.sun.identity.sm.ldap.adapters;

import com.iplanet.dpro.session.service.InternalSession;
import com.sun.identity.sm.ldap.CoreTokenConfig;
import com.sun.identity.sm.ldap.api.TokenType;
import com.sun.identity.sm.ldap.api.tokens.Token;
import com.sun.identity.sm.ldap.api.tokens.TokenIdFactory;
import com.sun.identity.sm.ldap.utils.JSONSerialisation;
import com.sun.identity.sm.ldap.utils.LDAPDataConversion;

import java.util.Calendar;

/**
 * SessionAdapter is responsible for providing conversions to and from InternalSession
 * and managing the details around data conversion for this class.
 *
 * @author robert.wapshott@forgerock.com
 */
public class SessionAdapter implements TokenAdapter<InternalSession> {

    private final TokenIdFactory tokenIdFactory;
    private final CoreTokenConfig config;
    private final JSONSerialisation serialisation;
    private final LDAPDataConversion dataConversion;

    /**
     * Creates a default instance with dependencies defined.
     *
     * @param tokenIdFactory Non null.
     * @param config Non null.
     * @param serialisation Non null.
     */
    public SessionAdapter(TokenIdFactory tokenIdFactory, CoreTokenConfig config,
                          JSONSerialisation serialisation, LDAPDataConversion dataConversion) {
        this.tokenIdFactory = tokenIdFactory;
        this.config = config;
        this.serialisation = serialisation;
        this.dataConversion = dataConversion;
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
        token.setBlob(jsonBlob.getBytes());

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
        String jsonBlob = new String(token.getBlob());
        return serialisation.deserialise(jsonBlob, InternalSession.class);
    }
}

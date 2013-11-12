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
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.api.fields.SAMLTokenField;
import org.forgerock.openam.cts.api.tokens.SAMLToken;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.api.tokens.TokenIdFactory;
import org.forgerock.openam.cts.utils.JSONSerialisation;
import org.forgerock.openam.cts.utils.LDAPDataConversion;
import org.forgerock.openam.cts.utils.blob.TokenBlobUtils;

import java.text.MessageFormat;
import java.util.Calendar;

/**
 * TokenAdapter for SAML tokens. SAML tokens in particular have no specific hierarchy so the SAMLToken
 * class exists to simplify this problem.
 *
 * @author robert.wapshott@forgerock.com
 */
public class SAMLAdapter implements TokenAdapter<SAMLToken> {

    // Injected
    private final TokenIdFactory tokenIdFactory;
    private final JSONSerialisation serialisation;
    private final LDAPDataConversion dataConversion;
    private final TokenBlobUtils blobUtils;

    /**
     * Default constructor with dependencies exposed.
     *
     * @param tokenIdFactory Non null.
     * @param serialisation Non null.
     */
    @Inject
    public SAMLAdapter(TokenIdFactory tokenIdFactory, JSONSerialisation serialisation,
                       LDAPDataConversion dataConversion, TokenBlobUtils blobUtils) {
        this.tokenIdFactory = tokenIdFactory;
        this.serialisation = serialisation;
        this.dataConversion = dataConversion;
        this.blobUtils = blobUtils;
    }

    /**
     * Convert the SAMLToken to a Token.
     *
     *
     * This conversion performs the additional mapping needed when dealing with SAMLTokens.
     *
     * @param samlToken Non null.
     * @return Non null Token.
     */
    public Token toToken(SAMLToken samlToken) {
        String tokenId = tokenIdFactory.toSAMLPrimaryTokenId(samlToken.getPrimaryKey());
        Token token = new Token(tokenId, TokenType.SAML2);

        // Expiry Date
        Calendar timestamp = dataConversion.fromEpochedSeconds(samlToken.getExpiryTime());
        token.setExpiryTimestamp(timestamp);

        // Persist the SAML token class, because there is no obvious hierarchy to the SAML tokens.
        String className = samlToken.getToken().getClass().getName();
        token.setAttribute(SAMLTokenField.OBJECT_CLASS.getField(), className);
        // Persist the SAML secondary key because it can be queried over.

        String secondaryKey = samlToken.getSecondaryKey();
        if (secondaryKey != null) {
            secondaryKey = tokenIdFactory.toSAMLSecondaryTokenId(secondaryKey);
            token.setAttribute(SAMLTokenField.SECONDARY_KEY.getField(), secondaryKey);
        }

        // Binary data
        String jsonBlob = serialisation.serialise(samlToken.getToken());
        blobUtils.setBlobFromString(token, jsonBlob);

        return token;
    }

    /**
     * Convert from a Token using deserialsied JSON blob to rebuild the SAMLToken.
     *
     * @param token Token to be converted back to its original format.
     * @return Non null SAMLToken.
     */
    public SAMLToken fromToken(Token token) {
        // Use the persisted field to work out the type of class that was persisted.
        String className = token.getValue(SAMLTokenField.OBJECT_CLASS.getField());

        Class<?> c;
        try {
            c = Class.forName(className);
        } catch (ClassNotFoundException e) {
            String message = MessageFormat.format(
                    CoreTokenConstants.DEBUG_HEADER +
                            "Could not deserialise SAML Token because class not found:\n" +
                            "Class Name: {0}\n" +
                            "Token: {1}",
                    className,
                    token);
            throw new IllegalStateException(message, e);
        }

        // Binary Data
        String jsonBlob = blobUtils.getBlobAsString(token);
        Object blob = serialisation.deserialise(jsonBlob, c);

        // Expiry Date
        long expiryTime = dataConversion.toEpochedSeconds(token.getExpiryTimestamp());

        // Secondary Key
        String secondaryKey = token.getValue(SAMLTokenField.SECONDARY_KEY.getField());

        SAMLToken samlToken = new SAMLToken(token.getTokenId(), secondaryKey, expiryTime, blob);

        return samlToken;
    }
}

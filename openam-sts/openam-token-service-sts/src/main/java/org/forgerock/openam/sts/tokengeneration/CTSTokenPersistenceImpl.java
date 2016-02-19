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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.sts.tokengeneration;

import static java.util.Locale.*;
import static java.util.TimeZone.*;
import static org.forgerock.openam.utils.Time.*;

import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.filter.TokenFilterBuilder;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.CTSTokenPersistenceException;
import org.forgerock.openam.sts.TokenIdGenerationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.token.CTSTokenIdGenerator;
import org.forgerock.openam.sts.user.invocation.STSIssuedTokenState;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.utils.TimeUtils;
import org.forgerock.util.query.QueryFilter;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @see CTSTokenPersistence
 */
public class CTSTokenPersistenceImpl implements CTSTokenPersistence {
    private final CTSPersistentStore ctsPersistentStore;
    private final CTSTokenIdGenerator ctsTokenIdGenerator;

    @Inject
    CTSTokenPersistenceImpl(CTSPersistentStore ctsPersistentStore,
                            CTSTokenIdGenerator ctsTokenIdGenerator) {
        this.ctsPersistentStore = ctsPersistentStore;
        this.ctsTokenIdGenerator = ctsTokenIdGenerator;
    }

    @Override
    public void persistToken(String stsId, TokenType tokenType, String tokenString, String subjectId,
                             long issueInstantMillis, long tokenLifetimeSeconds) throws CTSTokenPersistenceException {
        try {
            final String tokenId = ctsTokenIdGenerator.generateTokenId(tokenType, tokenString);
            final Token ctsToken = generateToken(stsId, tokenString.getBytes(AMSTSConstants.UTF_8_CHARSET_ID),
                    tokenId, subjectId, issueInstantMillis, tokenLifetimeSeconds, tokenType);
            ctsPersistentStore.create(ctsToken);
        } catch (TokenIdGenerationException e) {
            throw new CTSTokenPersistenceException(e.getCode(), "Exception caught generating id for CTS-persisted " +
                    tokenType + "  token: " + e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            throw new CTSTokenPersistenceException(ResourceException.INTERNAL_ERROR, "Exception caught getting byte[] " +
                    "representation of issued " + tokenType + " token for CTS persistence: " + e, e);
        } catch (CoreTokenException e) {
            throw new CTSTokenPersistenceException(ResourceException.INTERNAL_ERROR,
                    "Exception caught persisting issued " + tokenType + " token in the CTS: " + e.getMessage(), e);
        }
    }

    @Override
    public String getToken(String tokenId) throws CTSTokenPersistenceException {
        try {
            final Token ctsToken =  ctsPersistentStore.read(tokenId);
            if (ctsToken != null) {
                return new String(ctsToken.getBlob(), AMSTSConstants.UTF_8_CHARSET_ID);
            } else {
                return null;
            }
        } catch (CoreTokenException e) {
            throw new CTSTokenPersistenceException(ResourceException.INTERNAL_ERROR, "Exception caught querying CTS " +
                    "for token id: " + tokenId + ": " + e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            throw new CTSTokenPersistenceException(ResourceException.INTERNAL_ERROR, "Unsupported encoding pulling " +
                    "token bytes for token id: " + tokenId + " from the CTS: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteToken(String tokenId) throws CTSTokenPersistenceException {
        try {
            ctsPersistentStore.delete(tokenId);
        } catch (CoreTokenException e) {
            throw new CTSTokenPersistenceException(ResourceException.INTERNAL_ERROR, e.getMessage(), e);
        }
    }

    @Override
    public List<STSIssuedTokenState> listTokens(QueryFilter<CoreTokenField> queryFilter) throws CTSTokenPersistenceException {
        Collection<PartialToken> partialTokens;
        try {
            partialTokens = ctsPersistentStore.attributeQuery(buildTokenFilter(queryFilter));
        } catch (CoreTokenException e) {
            throw new CTSTokenPersistenceException(ResourceException.INTERNAL_ERROR, e.getMessage(), e);
        }

        List<STSIssuedTokenState> issuedTokens = new ArrayList<>(partialTokens.size());
        for (PartialToken partialToken : partialTokens) {
            issuedTokens.add(marshalIssuedTokenState(partialToken));
        }
        return issuedTokens;
    }

    private Token generateToken(String stsId, byte[] tokenBytes, String tokenId, String subjectId, long issueInstantMillis,
                                long tokenLifetimeSeconds, TokenType tokenType) {
        final Token ctsToken = new Token(tokenId, org.forgerock.openam.tokens.TokenType.STS);
        ctsToken.setAttribute(CoreTokenField.BLOB, tokenBytes);
        ctsToken.setAttribute(CoreTokenField.USER_ID, subjectId);
        ctsToken.setAttribute(CoreTokenField.EXPIRY_DATE,
                timeOf(issueInstantMillis + (tokenLifetimeSeconds * 1000)));
        ctsToken.setAttribute(CTS_TOKEN_FIELD_STS_ID, stsId);
        ctsToken.setAttribute(CTS_TOKEN_FIELD_STS_TOKEN_TYPE, tokenType.name());
        return ctsToken;
    }

    private Calendar now() {
        return getCalendarInstance(getTimeZone("UTC"), ROOT);
    }

    private Calendar timeOf(final long utcMillis) {
        final Calendar calendar = now();
        calendar.setTimeInMillis(utcMillis);
        return calendar;
    }

    private TokenFilter buildTokenFilter(QueryFilter<CoreTokenField> queryFilter) {
        return buildReturnAttributes()
                .withQuery(queryFilter)
                .build();
    }

    private TokenFilterBuilder buildReturnAttributes() {
        return new TokenFilterBuilder()
                .returnAttribute(CoreTokenField.TOKEN_ID)
                .returnAttribute(CTSTokenPersistence.CTS_TOKEN_FIELD_STS_TOKEN_TYPE)
                .returnAttribute(CTSTokenPersistence.CTS_TOKEN_FIELD_STS_ID)
                .returnAttribute(CoreTokenField.EXPIRY_DATE)
                .returnAttribute(CoreTokenField.USER_ID);
    }

    private STSIssuedTokenState marshalIssuedTokenState(PartialToken partialToken) throws CTSTokenPersistenceException {
        try {
            final Calendar timestamp = partialToken.getValue(CoreTokenField.EXPIRY_DATE);
            final long unixTime = TimeUtils.toUnixTime(timestamp);
            final String userId = partialToken.getValue(CoreTokenField.USER_ID);
            final String tokenType = partialToken.getValue(CTS_TOKEN_FIELD_STS_TOKEN_TYPE);
            final String stsId = partialToken.getValue(CTS_TOKEN_FIELD_STS_ID);
            final String tokenId = partialToken.getValue(CoreTokenField.TOKEN_ID);
            return STSIssuedTokenState.builder()
                    .tokenId(tokenId)
                    .stsId(stsId)
                    .expirationTimeInSecondsFromEpoch(unixTime)
                    .tokenType(tokenType)
                    .principalName(userId)
                    .build();
        } catch (NullPointerException e) {
            throw new CTSTokenPersistenceException(ResourceException.INTERNAL_ERROR, "Null field encountered in CTS " +
                    "token query results: " + e.getMessage(), e);
        }
    }
}

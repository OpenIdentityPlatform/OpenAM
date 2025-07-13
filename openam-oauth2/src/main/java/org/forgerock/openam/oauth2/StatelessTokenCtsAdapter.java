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
 * Portions Copyrighted 2025 3A Systems, LLC.
 */

package org.forgerock.openam.oauth2;

import static java.util.Locale.ROOT;
import static org.forgerock.openam.tokens.TokenType.OAUTH_STATELESS;
import static org.forgerock.openam.utils.Time.getCalendarInstance;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.openam.cts.adapters.TokenAdapter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.utils.TimeUtils;

/**
 * Implementation of the TokenAdapter for the StatelessTokenMetadata objects.
 */
public class StatelessTokenCtsAdapter implements TokenAdapter<StatelessTokenMetadata> {

    public static final CoreTokenField GRANT_ID_FIELD = CoreTokenField.STRING_FIFTEEN;
    public static final CoreTokenField CLIENT_ID_FIELD = CoreTokenField.STRING_NINE;
    public static final CoreTokenField SCOPE_FIELD = CoreTokenField.STRING_ONE;
    public static final CoreTokenField REALM_FIELD = CoreTokenField.STRING_EIGHT;
    public static final CoreTokenField NAME_FIELD = CoreTokenField.STRING_TEN;
    public static final CoreTokenField GRANT_TYPE_FIELD = CoreTokenField.STRING_TWELVE;
    public static final CoreTokenField USERNAME_FIELD = CoreTokenField.STRING_THREE;

    @Override
    public Token toToken(StatelessTokenMetadata metadata) {
        Token token = new Token(metadata.getId(), OAUTH_STATELESS);
        token.setUserId(metadata.getResourceOwnerId());
        Calendar calendar = getCalendarInstance(TimeUtils.UTC, ROOT);
        calendar.setTimeInMillis(metadata.getExpiryTime());
        token.setExpiryTimestamp(calendar);
        token.setAttribute(GRANT_ID_FIELD, metadata.getGrantId());
        token.setAttribute(CLIENT_ID_FIELD, metadata.getClientId());
        token.setAttribute(SCOPE_FIELD, StringUtils.join(metadata.getScope(), ','));
        token.setAttribute(REALM_FIELD, metadata.getRealm());
        token.setAttribute(NAME_FIELD, metadata.getName());
        token.setAttribute(GRANT_TYPE_FIELD, metadata.getGrantType());
        token.setAttribute(USERNAME_FIELD, metadata.getResourceOwnerId());
        return token;
    }

    @Override
    public StatelessTokenMetadata fromToken(Token token) {
        String resourceOwnerId = token.getUserId();
        long expiryTime = token.getExpiryTimestamp().getTime().getTime();
        String grantId = token.getAttribute(GRANT_ID_FIELD);
        String clientId = token.getAttribute(CLIENT_ID_FIELD);
        Set<String> scope = new HashSet<>(Arrays.asList(((String) token.getAttribute(SCOPE_FIELD)).split(",", 0)));
        String realm = token.getAttribute(REALM_FIELD);
        String name = token.getAttribute(NAME_FIELD);
        String grantType = token.getAttribute(GRANT_ID_FIELD);
        return new StatelessTokenMetadata(token.getTokenId(), resourceOwnerId, expiryTime, grantId, clientId, scope,
                realm, name, grantType);
    }
}

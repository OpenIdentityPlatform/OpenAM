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

package org.forgerock.openam.oauth2;

import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.EXPIRE_TIME;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.TOKEN_TYPE;
import static org.forgerock.oauth2.core.OAuth2Constants.Custom.CLAIMS;
import static org.forgerock.oauth2.core.OAuth2Constants.Custom.NONCE;
import static org.forgerock.oauth2.core.OAuth2Constants.Params.GRANT_TYPE;
import static org.forgerock.openam.utils.Time.currentTimeMillis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.OAuth2Constants;

/**
 * Models a stateless OpenAM OAuth2 access token.
 */
public final class StatelessAccessToken implements AccessToken {

    protected Map<String, Object> extraData = new HashMap<>();

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("OAuth2CoreToken");

    private final Jwt jwt;

    private String jwtString;

    /**
     * Constructs a new StatelessAccessToken backed with the specified {@code Jwt}.
     *
     * @param jwt The stateless token.
     * @param jwtString The JWT string.
     */
    public StatelessAccessToken(Jwt jwt, String jwtString) {
        this.jwt = jwt;
        this.jwtString = jwtString;
    }

    @Override
    public String getTokenId() {
        return jwtString;
    }

    @Override
    public String getTokenName() {
        return OAuth2Constants.Params.ACCESS_TOKEN;
    }

    @Override
    public String getRealm() {
        return jwt.getClaimsSet().getClaim(OAuth2Constants.Params.REALM, String.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<String> getScope() {
        Object scope = jwt.getClaimsSet().getClaim("scope");
        if (scope instanceof List) {
            return new HashSet<>(jwt.getClaimsSet().getClaim("scope", List.class));
        } else {
            return new HashSet<>(jwt.getClaimsSet().getClaim("scope", Set.class));
        }
    }

    @Override
    public String getClientId() {
        return jwt.getClaimsSet().getAudience().get(0);
    }

    @Override
    public String getNonce() {
        return jwt.getClaimsSet().getClaim(NONCE, String.class);
    }

    @Override
    public String getSessionId() {
        throw new UnsupportedOperationException("Stateless access tokens do not support the session id claim");
    }

    @Override
    public String getResourceOwnerId() {
        return jwt.getClaimsSet().getSubject();
    }

    @Override
    public String getClaims() {
        return jwt.getClaimsSet().getClaim(CLAIMS, String.class);
    }

    @Override
    public long getExpiryTime() {
        return jwt.getClaimsSet().getExpirationTime().getTime();
    }

    @Override
    public String getTokenType() {
        return OAuth2Constants.Bearer.BEARER;
    }

    @Override
    public String getGrantType() {
        return jwt.getClaimsSet().getClaim(GRANT_TYPE, String.class);
    }

    protected String getResourceString(String s) {
        return RESOURCE_BUNDLE.getString(s);
    }

    @Override
    public Map<String, Object> toMap() {
        final Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put(getResourceString(OAuth2Constants.Params.ACCESS_TOKEN), jwt.build());
        tokenMap.put(getResourceString(TOKEN_TYPE), "Bearer");
        tokenMap.put(getResourceString(EXPIRE_TIME),
                (jwt.getClaimsSet().getExpirationTime().getTime() - currentTimeMillis()) / 1000);
        tokenMap.putAll(extraData);
        return tokenMap;
    }

    @Override
    public Map<String, Object> getTokenInfo() {
        JwtClaimsSet claimsSet = jwt.getClaimsSet();
        Map<String, Object> tokenInfo = new HashMap<>();
        for (String key : claimsSet.keys()) {
            tokenInfo.put(key, claimsSet.get(key).getObject());
        }
        return tokenInfo;
    }

    /**
     * <p>Adds additional data to the Access Token.</p>
     *
     * <p>If the value is {@code null} then this method will ensure that the key is not present in the map.</p>
     *
     * @param key The key.
     * @param value The value.
     */
    @Override
    public void addExtraData(String key, String value) {
        if (value != null) {
            extraData.put(key, value);
        } else {
            extraData.remove(key);
        }
    }

    @Override
    public String getAuditTrackingId() {
        return null;
    }

    /**
     * Determines if the Access Token is expired.
     *
     * @return {@code true} if current time is greater than the expiry time.
     */
    @Override
    public boolean isExpired() {
        return currentTimeMillis() > getExpiryTime();
    }

    public JsonValue toJsonValue() {
        return new JsonValue(new Object());
    }
}

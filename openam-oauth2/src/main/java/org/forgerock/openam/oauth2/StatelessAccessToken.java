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

import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.EXPIRE_TIME;
import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.TOKEN_TYPE;
import static org.forgerock.openam.oauth2.OAuth2Constants.Custom.NONCE;
import static org.forgerock.openam.oauth2.OAuth2Constants.Custom.SSO_TOKEN_ID;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.GRANT_TYPE;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.ACCESS_TOKEN;
import static org.forgerock.openam.oauth2.OAuth2Constants.Bearer.BEARER;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.openam.audit.AuditConstants;

/**
 * Models a stateless OpenAM OAuth2 access token.
 */
public final class StatelessAccessToken extends StatelessToken implements AccessToken {

    protected Map<String, Object> extraData = new HashMap<>();

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("OAuth2CoreToken");

    /**
     * Constructs a new StatelessAccessToken backed with the specified {@code Jwt}.
     *
     * @param jwt The stateless token.
     * @param jwtString The JWT string.
     */
    public StatelessAccessToken(Jwt jwt, String jwtString) {
        super(jwt, jwtString);
    }

    @Override
    public String getTokenName() {
        return ACCESS_TOKEN;
    }

    @Override
    public String getNonce() {
        return jwt.getClaimsSet().getClaim(NONCE, String.class);
    }

    @Override
    public String getSessionId() {
        return (String) extraData.get(SSO_TOKEN_ID);
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
        tokenMap.put(getResourceString(ACCESS_TOKEN), jwt.build());
        tokenMap.put(getResourceString(TOKEN_TYPE), BEARER);
        tokenMap.put(getResourceString(EXPIRE_TIME), getTimeLeft());
        tokenMap.putAll(extraData);
        return tokenMap;
    }

    @Override
    public AuditConstants.TrackingIdKey getAuditTrackingIdKey() {
        return AuditConstants.TrackingIdKey.OAUTH2_ACCESS;
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
}

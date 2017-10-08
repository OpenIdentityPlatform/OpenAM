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

import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.*;
import static org.forgerock.openam.oauth2.OAuth2Constants.JWTTokenParams.ACR;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.REFRESH_TOKEN;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.REDIRECT_URI;
import static org.forgerock.openam.oauth2.OAuth2Constants.Bearer.BEARER;

import java.util.HashMap;
import java.util.Map;

import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.oauth2.core.RefreshToken;
import org.forgerock.openam.audit.AuditConstants;

/**
 * Models a stateless OpenAM OAuth2 refresh token.
 */
public class StatelessRefreshToken extends StatelessToken implements RefreshToken {

    /**
     * Constructs a new StatelessRefreshToken backed with the specified {@code Jwt}.
     *
     * @param jwt The stateless token.
     * @param jwtString The JWT string.
     */
    public StatelessRefreshToken(Jwt jwt, String jwtString) {
        super(jwt, jwtString);
    }

    @Override
    public String getTokenName() {
        return REFRESH_TOKEN;
    }

    @Override
    public String getAuthenticationContextClassReference() {
        return jwt.getClaimsSet().getClaim(ACR, String.class);
    }

    @Override
    public String getRedirectUri() {
        return jwt.getClaimsSet().getClaim(REDIRECT_URI, String.class);
    }

    @Override
    public String getAuthModules() {
        return jwt.getClaimsSet().getClaim(AUTH_MODULES, String.class);
    }

    @Override
    public Map<String, Object> toMap() {
        final Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put(getResourceString(REFRESH_TOKEN), jwt.build());
        tokenMap.put(getResourceString(TOKEN_TYPE), BEARER);
        tokenMap.put(getResourceString(EXPIRE_TIME), getTimeLeft());
        return tokenMap;
    }

    @Override
    public AuditConstants.TrackingIdKey getAuditTrackingIdKey() {
        return AuditConstants.TrackingIdKey.OAUTH2_REFRESH;
    }

    protected Long getTimeLeft() {
        if (isNeverExpires()) {
            return null;
        } else {
            return super.getTimeLeft();
        }
    }

    /**
     * Get whether or not token expires.
     *
     * @return Whether or not token expires.
     */
    private boolean isNeverExpires() {
        return getExpiryTime() == defaultExpireTime();
    }

    protected long defaultExpireTime() {
        return -1;
    }

    /**
     * Determines if the Refresh Token has expired.
     *
     * @return {@code true} if current time is greater than the expiry time.
     */
    @Override
    public boolean isExpired() {
        return ! isNeverExpires() && super.isExpired();
    }

    @Override
    public String getAuthGrantId() {
        return jwt.getClaimsSet().getClaim(AUTH_GRANT_ID, String.class);
    }

    /**
     * Gets the display String for the given String.
     *
     * @param string The String.
     * @return The display String.
     */
    protected String getResourceString(final String string) {
        return string;
    }
}

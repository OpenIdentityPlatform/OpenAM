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

import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;

import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;

import static org.forgerock.oauth2.core.OAuth2Constants.Bearer.BEARER;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.SCOPE;
import static org.forgerock.oauth2.core.OAuth2Constants.Custom.CLAIMS;
import static org.forgerock.oauth2.core.OAuth2Constants.Params.REALM;
import static org.forgerock.openam.utils.Time.currentTimeMillis;

public abstract class StatelessToken {

    protected final Jwt jwt;

    public StatelessToken(Jwt jwt) {
        this.jwt = jwt;
    }

    public String getRealm() {
        return jwt.getClaimsSet().getClaim(REALM, String.class);
    }

    @SuppressWarnings("unchecked")
    public Set<String> getScope() {
        Object scope = jwt.getClaimsSet().getClaim(SCOPE);
        if (scope instanceof List) {
            return new HashSet<>(jwt.getClaimsSet().getClaim(SCOPE, List.class));
        } else {
            return new HashSet<>(jwt.getClaimsSet().getClaim(SCOPE, Set.class));
        }
    }

    public String getClientId() {
        return jwt.getClaimsSet().getAudience().get(0);
    }

    public String getResourceOwnerId() {
        return jwt.getClaimsSet().getSubject();
    }

    public String getClaims() {
        return jwt.getClaimsSet().getClaim(CLAIMS, String.class);
    }

    public long getExpiryTime() {
        return jwt.getClaimsSet().getExpirationTime().getTime();
    }

    public String getTokenType() {
        return BEARER;
    }

    public Map<String, Object> getTokenInfo() {
        JwtClaimsSet claimsSet = jwt.getClaimsSet();
        Map<String, Object> tokenInfo = new HashMap<>();
        for (String key : claimsSet.keys()) {
            tokenInfo.put(key, claimsSet.get(key).getObject());
        }
        return tokenInfo;
    }

    public JsonValue toJsonValue() {
        return new JsonValue(new Object());
    }

    protected Long getTimeLeft() {
        return (getExpiryTime() - currentTimeMillis()) / 1000;
    }

    /**
     * Determines if the Access Token is expired.
     *
     * @return {@code true} if current time is greater than the expiry time.
     */
    public boolean isExpired() {
        return currentTimeMillis() > getExpiryTime();
    }
}

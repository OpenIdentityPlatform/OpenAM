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

import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.AUTH_MODULES;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.EXPIRE_TIME;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.TOKEN_TYPE;
import static org.forgerock.oauth2.core.OAuth2Constants.JWTTokenParams.ACR;
import static org.forgerock.openam.utils.Time.currentTimeMillis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.RefreshToken;
import org.forgerock.oauth2.core.StatefulRefreshToken;

/**
 * Models a stateless OpenAM OAuth2 refresh token.
 */
public class StatelessRefreshToken extends StatefulRefreshToken {

    private final Jwt jwt;
    private final String jwtString;

    /**
     * Constructs a new StatelessRefreshToken backed with the specified {@code Jwt}.
     *
     * @param jwt The stateless token.
     * @param jwtString The JWT string.
     */
    public StatelessRefreshToken(Jwt jwt, String jwtString) {
        super(null, null, null, null, null, 0L, null, null, null, null, null);
        this.jwt = jwt;
        this.jwtString = jwtString;
    }

    /**
     * All state is retrieved directly from the {@code Jwt}. No state is store internally.
     *
     * @param tokenId {@inheritDoc}
     */
    @Override
    protected void setTokenId(String tokenId) {
    }

    /**
     * All state is retrieved directly from the {@code Jwt}. No state is store internally.
     *
     * @param resourceOwnerId {@inheritDoc}
     */
    @Override
    protected void setResourceOwnerId(String resourceOwnerId) {
    }

    /**
     * All state is retrieved directly from the {@code Jwt}. No state is store internally.
     *
     * @param clientId {@inheritDoc}
     */
    @Override
    protected void setClientId(String clientId) {
    }

    /**
     * All state is retrieved directly from the {@code Jwt}. No state is store internally.
     *
     * @param redirectUri {@inheritDoc}
     */
    @Override
    protected void setRedirectUri(String redirectUri) {
    }

    /**
     * All state is retrieved directly from the {@code Jwt}. No state is store internally.
     *
     * @param scope {@inheritDoc}
     */
    @Override
    protected void setScope(Set<String> scope) {
    }

    /**
     * All state is retrieved directly from the {@code Jwt}. No state is store internally.
     *
     * @param expiryTime {@inheritDoc}
     */
    @Override
    protected void setExpiryTime(long expiryTime) {
    }

    /**
     * All state is retrieved directly from the {@code Jwt}. No state is store internally.
     *
     * @param tokenType {@inheritDoc}
     */
    @Override
    protected void setTokenType(String tokenType) {
    }

    /**
     * All state is retrieved directly from the {@code Jwt}. No state is store internally.
     *
     * @param tokenName {@inheritDoc}
     */
    @Override
    protected void setTokenName(String tokenName) {
    }

    /**
     * All state is retrieved directly from the {@code Jwt}. No state is store internally.
     *
     * @param grantType {@inheritDoc}
     */
    @Override
    protected void setGrantType(String grantType) {
    }

    /**
     * All state is retrieved directly from the {@code Jwt}. No state is store internally.
     *
     * @param authModules {@inheritDoc}
     */
    @Override
    public void setAuthModules(String authModules) {
    }

    /**
     * All state is retrieved directly from the {@code Jwt}. No state is store internally.
     *
     * @param acr {@inheritDoc}
     */
    @Override
    protected void setAuthenticationContextClassReference(String acr) {
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
        return jwt.getClaimsSet().getClaim("realm", String.class);
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
    public String getAuthenticationContextClassReference() {
        return jwt.getClaimsSet().getClaim(ACR, String.class);
    }

    @Override
    public long getExpiryTime() {
        return jwt.getClaimsSet().getExpirationTime().getTime();
    }

    @Override
    public String getClientId() {
        return jwt.getClaimsSet().getAudience().get(0);
    }

    @Override
    public String getResourceOwnerId() {
        return jwt.getClaimsSet().getSubject();
    }

    @Override
    public String getRedirectUri() {
        return jwt.getClaimsSet().getClaim(OAuth2Constants.Params.REDIRECT_URI, String.class);
    }

    @Override
    public String getTokenType() {
        return OAuth2Constants.Bearer.BEARER;
    }

    @Override
    public String getAuthModules() {
        return jwt.getClaimsSet().getClaim(AUTH_MODULES, String.class);
    }

    @Override
    public Map<String, Object> toMap() {
        final Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put(getResourceString(OAuth2Constants.Params.ACCESS_TOKEN), jwt.build());
        tokenMap.put(getResourceString(TOKEN_TYPE), "Bearer");
        tokenMap.put(getResourceString(EXPIRE_TIME),
                getExpiryTime() == -1
                        ? null
                        : (jwt.getClaimsSet().getExpirationTime().getTime() - currentTimeMillis()) / 1000);
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

    @Override
    public String toString() {
        return jwtString;
    }
}

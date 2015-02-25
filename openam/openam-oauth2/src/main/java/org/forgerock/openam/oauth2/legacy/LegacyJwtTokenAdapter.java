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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.oauth2.legacy;

import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.oauth2.OAuthProblemException;
import org.forgerock.openidconnect.OpenIdConnectToken;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Adapter between new {@link OpenIdConnectToken} and legacy {@link CoreToken}.
 *
 * @since 12.0.0
 */
@Deprecated
public class LegacyJwtTokenAdapter extends CoreToken {

    private final OpenIdConnectToken token;

    public LegacyJwtTokenAdapter(OpenIdConnectToken token) throws ServerException {
        super(token.getTokenId(), token);
        this.token = token;
    }

    @Override
    protected void setTokenID(String id) {
        throw new UnsupportedOperationException("LegacyJwtTokenAdapter does not support the setTokenID method");
    }

    @Override
    protected void setUserName(String userName) {
        throw new UnsupportedOperationException("LegacyJwtTokenAdapter does not support the setUserName method");
    }

    @Override
    protected void setRealm(String realm) {
        throw new UnsupportedOperationException("LegacyJwtTokenAdapter does not support the setRealm method");
    }

    @Override
    protected void setExpireTime(long expireTime) {
        throw new UnsupportedOperationException("LegacyJwtTokenAdapter does not support the setExpireTime method");
    }

    @Override
    protected void setNonce(String nonce) {
        throw new UnsupportedOperationException("LegacyJwtTokenAdapter does not support the setNonce method");
    }

    @Override
    protected void setTokenType(String tokenType) {
        throw new UnsupportedOperationException("LegacyJwtTokenAdapter does not support the setTokenType method");
    }

    @Override
    protected void setTokenName(String tokenName) {
        throw new UnsupportedOperationException("LegacyJwtTokenAdapter does not support the setTokenName method");
    }

    @Override
    protected void setGrantType(String grantType) {
        throw new UnsupportedOperationException("LegacyJwtTokenAdapter does not support the setGrantType method");
    }

    @Override
    public String getTokenID() {
        try {
            return token.getTokenId();
        } catch (ServerException e) {
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, e.getMessage());
        }
    }

    @Override
    public String getNonce() {
        throw new UnsupportedOperationException("LegacyJwtTokenAdapter does not support the getNonce method");
    }

    @Override
    public String getParent() {
        throw new UnsupportedOperationException("LegacyJwtTokenAdapter does not support the getParent method");
    }

    @Override
    public boolean isIssued() {
        throw new UnsupportedOperationException("LegacyJwtTokenAdapter does not support the isIssued method");
    }

    @Override
    public String getRefreshToken() {
        throw new UnsupportedOperationException("LegacyJwtTokenAdapter does not support the getRefreshToken method");
    }

    @Override
    public String getUserID() {
        throw new UnsupportedOperationException("LegacyJwtTokenAdapter does not support the getUserID method");
    }

    @Override
    public String getRealm() {
        throw new UnsupportedOperationException("LegacyJwtTokenAdapter does not support the getRealm method");
    }

    @Override
    public long getExpireTime() {
        throw new UnsupportedOperationException("LegacyJwtTokenAdapter does not support the getExpireTime method");
    }

    @Override
    public Set<String> getScope() {
        throw new UnsupportedOperationException("LegacyJwtTokenAdapter does not support the getScope method");
    }

    @Override
    public boolean isExpired() {
        throw new UnsupportedOperationException("LegacyJwtTokenAdapter does not support the isExpired method");
    }

    @Override
    public String getTokenType() {
        throw new UnsupportedOperationException("LegacyJwtTokenAdapter does not support the getTokenType method");
    }

    @Override
    public String getTokenName() {
        return token.getTokenName();
    }

    @Override
    public String getClientID() {
        throw new UnsupportedOperationException("LegacyJwtTokenAdapter does not support the getClientID method");
    }

    @Override
    public String getRedirectURI() {
        throw new UnsupportedOperationException("LegacyJwtTokenAdapter does not support the getRedirectURI method");
    }

    @Override
    public Set<String> getParameter(String paramName) {
        throw new UnsupportedOperationException("LegacyJwtTokenAdapter does not support the getParameter method");
    }

    @Override
    public String getIssued() {
        throw new UnsupportedOperationException("LegacyJwtTokenAdapter does not support the getIssued method");
    }

    @Override
    public void setIssued() {
        throw new UnsupportedOperationException("LegacyJwtTokenAdapter does not support the setIssued method");
    }

    @Override
    public String getGrantType() {
        throw new UnsupportedOperationException("LegacyJwtTokenAdapter does not support the getGrantType method");
    }

    @Override
    public Map<String, Object> convertToMap() {
        try {
            return token.toMap();
        } catch (ServerException e) {
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getTokenInfo() {
        return new HashMap<String, Object>();
    }
}

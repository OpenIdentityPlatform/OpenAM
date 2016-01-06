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
* Copyright 2015-2016 ForgeRock AS.
*/
package org.forgerock.openam.rest.audit;

import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.RefreshToken;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.audit.AuditConstants.TrackingIdKey;
import org.restlet.Request;
import org.restlet.data.ChallengeResponse;

import static org.forgerock.openam.audit.AuditConstants.TrackingIdKey.OAUTH2_REFRESH;

/**
 * A provider which provides user id and context details for auditing purposes. This provider draws its details
 * from an OAuth2 {@link RefreshToken} if one is available.
 *
 * @since 13.0.0
 */
public class OAuth2AuditRefreshTokenContextProvider extends OAuth2AuditOAuth2TokenContextProvider {

    private final OAuth2RequestFactory<?, Request> requestFactory;
    private final TokenStore tokenStore;

    /**
     * Create a new instance of OAuth2AuditRefreshTokenContextProvider, which will use the supplied {@link TokenStore},
     * {@link OAuth2RequestFactory}.
     *
     * @param tokenStore The helper to use for reading authentication JWTs.
     * @param requestFactory The factory for creating OAuth2Request instances.
     */
    public OAuth2AuditRefreshTokenContextProvider(TokenStore tokenStore,
            OAuth2RequestFactory<?, Request> requestFactory) {
        this.tokenStore = tokenStore;
        this.requestFactory = requestFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUserId(Request request) {
        String userId = getUserIdFromRefreshTokenFromAuthorizationHeader(request);
        if (userId != null) {
            return userId;
        }

        userId = getUserIdFromRefreshTokenFromRequest(request);
        if (userId != null) {
            return userId;
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTrackingId(Request request) {
        String trackingId;

        trackingId = getTrackingIdFromRefreshTokenFromAuthorizationHeader(request);
        if (trackingId != null) {
            return trackingId;
        }

        trackingId = getTrackingIdFromRefreshTokenFromRequest(request);
        if (trackingId != null) {
            return trackingId;
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TrackingIdKey getTrackingIdKey() {
        return OAUTH2_REFRESH;
    }

    private String getUserIdFromRefreshTokenFromAuthorizationHeader(Request request) {
        String userId = null;

        RefreshToken refreshToken = retrieveRefreshTokenFromChallengeResponse(request);
        if (refreshToken != null) {
            userId = getUserIdFromToken(refreshToken);
        }

        return userId;
    }

    private String getUserIdFromRefreshTokenFromRequest(Request request) {
        String userId = null;

        RefreshToken refreshToken = retrieveRefreshTokenFromRequest(request);
        if (refreshToken != null) {
            userId = getUserIdFromToken(refreshToken);
        }

        return userId;
    }

    private String getTrackingIdFromRefreshTokenFromAuthorizationHeader(Request request) {
        String trackingId = null;

        RefreshToken refreshToken = retrieveRefreshTokenFromChallengeResponse(request);
        if (refreshToken != null) {
            trackingId = getTrackingIdFromToken(refreshToken);
        }

        return trackingId;
    }

    private String getTrackingIdFromRefreshTokenFromRequest(Request request) {
        String trackingId = null;

        RefreshToken refreshToken = retrieveRefreshTokenFromRequest(request);
        if (refreshToken != null) {
            trackingId = getTrackingIdFromToken(refreshToken);
        }

        return trackingId;
    }

    private RefreshToken retrieveRefreshTokenFromChallengeResponse(Request request) {
        RefreshToken refreshToken;

        ChallengeResponse challengeResponse = request.getChallengeResponse();

        if (challengeResponse == null) {
            return null;
        }

        String bearerToken = challengeResponse.getRawValue();

        if ("undefined".equals(bearerToken)) {
            return null;
        }

        OAuth2Request oAuth2Request = requestFactory.create(request);
        try {
            refreshToken = tokenStore.readRefreshToken(oAuth2Request, bearerToken);
        } catch (ServerException | InvalidGrantException | NotFoundException e) {
            return null;
        }

        return refreshToken;
    }

    private RefreshToken retrieveRefreshTokenFromRequest(Request request) {

        RefreshToken token;
        token = requestFactory.create(request).getToken(RefreshToken.class);

        return token;
    }
}

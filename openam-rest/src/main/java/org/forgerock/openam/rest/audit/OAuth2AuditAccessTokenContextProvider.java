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

import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.audit.AuditConstants.TrackingIdKey;
import org.restlet.Request;
import org.restlet.data.ChallengeResponse;

import static org.forgerock.openam.audit.AuditConstants.TrackingIdKey.OAUTH2_ACCESS;

/**
 * A provider which provides user id and context details for auditing purposes. This provider draws its details
 * from an OAuth2 {@link AccessToken} if one is available.
 *
 * @since 13.0.0
 */
public class OAuth2AuditAccessTokenContextProvider extends OAuth2AuditOAuth2TokenContextProvider {

    private final OAuth2RequestFactory<?, Request> requestFactory;
    private final TokenStore tokenStore;

    /**
     * Create a new instance of OAuth2AuditAccessTokenContextProvider, which will use the supplied {@link TokenStore},
     * {@link OAuth2RequestFactory}.
     *
     * @param tokenStore The helper to use for reading authentication JWTs.
     * @param requestFactory The factory for creating OAuth2Request instances.
     */
    public OAuth2AuditAccessTokenContextProvider(TokenStore tokenStore,
            OAuth2RequestFactory<?, Request> requestFactory) {
        this.tokenStore = tokenStore;
        this.requestFactory = requestFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUserId(Request request) {
        String userId = getUserIdFromAccessTokenFromAuthorizationHeader(request);
        if (userId != null) {
            return userId;
        }

        userId = getUserIdFromAccessTokenFromRequest(request);
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

        trackingId = getTrackingIdFromAccessTokenFromAuthorizationHeader(request);
        if (trackingId != null) {
            return trackingId;
        }

        trackingId = getTrackingIdFromAccessTokenFromRequest(request);
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
        return OAUTH2_ACCESS;
    }

    private String getUserIdFromAccessTokenFromAuthorizationHeader(Request request) {
        String userId = null;

        AccessToken accessToken = retrieveAccessTokenFromChallengeResponse(request);
        if (accessToken != null) {
            userId = getUserIdFromToken(accessToken);
        }

        return userId;
    }

    private String getUserIdFromAccessTokenFromRequest(Request request) {
        String userId = null;

        AccessToken accessToken = retrieveAccessTokenFromRequest(request);
        if (accessToken != null) {
            userId = getUserIdFromToken(accessToken);
        }

        return userId;
    }

    private String getTrackingIdFromAccessTokenFromAuthorizationHeader(Request request) {
        String trackingId = null;

        AccessToken accessToken = retrieveAccessTokenFromChallengeResponse(request);
        if (accessToken != null) {
            trackingId = getTrackingIdFromToken(accessToken);
        }

        return trackingId;
    }

    private String getTrackingIdFromAccessTokenFromRequest(Request request) {
        String trackingId = null;

        AccessToken accessToken = retrieveAccessTokenFromRequest(request);
        if (accessToken != null) {
            trackingId = getTrackingIdFromToken(accessToken);
        }

        return trackingId;
    }

    private AccessToken retrieveAccessTokenFromChallengeResponse(Request request) {

        AccessToken token;

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
            token = tokenStore.readAccessToken(oAuth2Request, bearerToken);
        } catch (ServerException | InvalidGrantException | NotFoundException e) {
            return null;
        }

        return token;
    }

    private AccessToken retrieveAccessTokenFromRequest(Request request) {

        AccessToken token;

        token = requestFactory.create(request).getToken(AccessToken.class);

        return token;
    }
}

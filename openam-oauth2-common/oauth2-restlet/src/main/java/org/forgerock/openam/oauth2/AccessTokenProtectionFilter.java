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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.oauth2;

import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.core.exceptions.InsufficientScopeException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidTokenException;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.Status;
import org.restlet.routing.Filter;

import com.sun.identity.shared.debug.Debug;

/**
 * A Restlet filter to fetch a bearer access token from the Authorization header, and verify it against the internal
 * token store.
 */
public class AccessTokenProtectionFilter extends Filter {

    private final Debug debug = Debug.getInstance("UmaProvider");
    private final String requiredScope;
    private final TokenStore tokenStore;
    private final OAuth2RequestFactory<Request> requestFactory;

    public AccessTokenProtectionFilter(String requiredScope, TokenStore tokenStore,
            OAuth2RequestFactory<Request> requestFactory, Restlet next) {
        this.requiredScope = requiredScope;
        this.tokenStore = tokenStore;
        this.requestFactory = requestFactory;
        setNext(next);
    }

    @Override
    protected int beforeHandle(Request request, Response response) {
        ChallengeResponse challengeResponse = request.getChallengeResponse();
        Status failure = null;
        if (challengeResponse == null) {
            failure = new Status(401, new InvalidTokenException());
        } else {
            String tokenId = challengeResponse.getRawValue();
            try {
                OAuth2Request oAuth2Request = requestFactory.create(request);
                AccessToken accessToken = tokenStore.readAccessToken(oAuth2Request, tokenId);
                if (accessToken == null || accessToken.isExpired()) {
                    failure = new Status(401, new InvalidTokenException());
                } else if (requiredScope != null && !accessToken.getScope().contains(requiredScope)) {
                    failure = new Status(403, new InsufficientScopeException(requiredScope));
                } else {
                    oAuth2Request.setToken(AccessToken.class, accessToken);
                }
            } catch (ServerException e) {
                failure = new Status(500, e);
            } catch (InvalidGrantException e) {
                debug.message("Error loading token with id: " + tokenId, e);
                failure = new Status(401, new InvalidTokenException());
            }
        }
        if (failure != null) {
            response.setStatus(failure);
            return STOP;
        }
        return super.beforeHandle(request, response);
    }
}

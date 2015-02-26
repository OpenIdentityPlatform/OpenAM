/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of company]"
 */

package org.forgerock.restlet.ext.oauth2.consumer;

import org.forgerock.openam.ext.cts.repo.DefaultOAuthTokenStoreImpl;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.model.BearerToken;
import org.forgerock.openam.oauth2.provider.OAuth2TokenStore;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.*;

import java.util.Map;

public class BearerTokenExtractor extends AccessTokenExtractor<BearerToken> {
    OAuth2TokenStore store;

    public BearerTokenExtractor(OAuth2TokenStore store){
        this.store = store;
    }
    public BearerTokenExtractor(){
        this.store = new DefaultOAuthTokenStoreImpl();
    }

    /**
     * Extracts the access token from the contents of an {@link org.restlet.Response}
     *
     * @param response
     *            the contents of the response
     * @return OAuth2 access token
     */
     public BearerToken extract(Response response){
      return null;
     }

    /**
     * Extracts the access token from the contents of an {@link org.restlet.Request}
     * <p/>
     * This method used to get the token from the redirect GET
     *
     * @return OAuth2 access token @ param request the contents of the request
     */
     public BearerToken extract(OAuth2Utils.ParameterLocation tokenLocation,
     Request request){
         return null;
     }
    public ChallengeResponse createChallengeResponse(BearerToken token){
        return null;
    }

    public ChallengeRequest createChallengeRequest(String realm){
        return null;
    }

    public ChallengeRequest createChallengeRequest(String realm,
                                                            OAuthProblemException exception){
        return null;
    }

    public Form createForm(BearerToken token){
        return null;
    }

    protected BearerToken extractRequestToken(ChallengeResponse challengeResponse)
            throws OAuthProblemException{
        String tokenID = challengeResponse.getRawValue();
        return (BearerToken) store.readAccessToken(tokenID);
    }

    /**
     * @param request
     * @return
     * @throws OAuthProblemException
     */
    protected BearerToken extractRequestToken(Request request) throws OAuthProblemException{
        BearerToken token = null;
        if (request.getResourceRef().hasQuery()) {
            token = extractRequestToken(request.getResourceRef().getQueryAsForm());
        } else if (request.getResourceRef().hasFragment()) {
            token = extractRequestToken(new Form(request.getResourceRef().getFragment()));
        }
        // TODO add the ability to get access_token from body
        return token;
    }

    protected BearerToken extractRequestToken(Response response) throws OAuthProblemException{
        return null;
    }

    protected BearerToken extractRequestToken(Form parameters) throws OAuthProblemException{
        String tokenID;
        Map<String, String> map = parameters.getValuesMap();
        tokenID = map.get("access_token");
        return (BearerToken) store.readAccessToken(tokenID);
    }

}

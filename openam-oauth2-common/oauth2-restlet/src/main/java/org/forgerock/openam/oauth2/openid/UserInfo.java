/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013-2014 ForgeRock AS.
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
 * "Portions copyright [year] [name of copyright owner]"
 */

package org.forgerock.openam.oauth2.openid;

import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.ScopeValidator;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.openam.oauth2.OAuth2ConfigurationFactory;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.oauth2.core.CoreToken;
import org.forgerock.openam.oauth2.provider.OAuth2ProviderSettings;
import org.forgerock.openam.oauth2.provider.OAuth2TokenStore;
import org.forgerock.oauth2.core.Scope;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;

public class UserInfo extends ServerResource {

    private final TokenStore tokenStore;
    private final ScopeValidator scopeValidator;

    @Inject
    public UserInfo(final TokenStore tokenStore, final ScopeValidator scopeValidator) {
        this.tokenStore = tokenStore;
        this.scopeValidator = scopeValidator;
    }

    @Get
    @Post
    public Representation getUserInfo(){
        return new JsonRepresentation(executeScopePlugin());
    }

    private Map<String,Object> executeScopePlugin(){

        final String tokenid = getRequest().getChallengeResponse().getRawValue();
        final AccessToken token = tokenStore.readAccessToken(tokenid);

        if (scopeValidator != null) {
            return scopeValidator.getUserInfo(token);
        } else {
            OAuth2Utils.DEBUG.error("AbstractFlow::Exception during userinfo scope execution");
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(getRequest());
        }
    }
}

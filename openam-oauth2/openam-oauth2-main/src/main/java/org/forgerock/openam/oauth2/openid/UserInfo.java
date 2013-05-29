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
 * "Portions copyright [year] [name of copyright owner]"
 */

package org.forgerock.openam.oauth2.openid;

import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.openam.ext.cts.repo.DefaultOAuthTokenStoreImpl;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.model.CoreToken;
import org.forgerock.openam.oauth2.provider.OAuth2TokenStore;
import org.forgerock.openam.oauth2.provider.Scope;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import java.util.Map;

public class UserInfo extends ServerResource {

    @Get
    @Post
    public Representation getUserInfo(){
        return new JsonRepresentation(executeScopePlugin());
    }

    private Map<String,Object> executeScopePlugin(){
            Map<String, Object> userinfo = null;
            String pluginClass = null;
            Scope scopeClass = null;
            String tokenid = getRequest().getChallengeResponse().getRawValue();
            OAuth2TokenStore store = new DefaultOAuthTokenStoreImpl();
            CoreToken token = store.readAccessToken(tokenid);
            try {
                pluginClass =
                        OAuth2Utils.getOAuth2ProviderSetting(OAuth2Constants.OAuth2ProviderService.SCOPE_PLUGIN_CLASS,
                                String.class,
                                getRequest());
                if (pluginClass != null && !pluginClass.isEmpty()){
                    scopeClass = (Scope) Class.forName(pluginClass).newInstance();
                }
            } catch (Exception e){
                OAuth2Utils.DEBUG.error("AbstractFlow::Exception during userinfo scope execution", e);
                throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(getRequest());
            }

            // Validate the granted scope
            if (scopeClass != null && pluginClass != null){
                userinfo = scopeClass.getUserInfo(token);
            } else {
                OAuth2Utils.DEBUG.error("AbstractFlow::Exception during userinfo scope execution");
                throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(getRequest());
            }

            return userinfo;
        }

}

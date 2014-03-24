/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012-2014 ForgeRock AS.
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

package org.forgerock.restlet.ext.oauth2.flow.responseTypes;

import org.forgerock.openam.oauth2.OAuth2ConfigurationFactory;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.CoreToken;
import org.forgerock.openam.oauth2.provider.OAuth2TokenStore;
import org.forgerock.openam.oauth2.provider.ResponseType;

import java.util.Map;
import java.util.Set;

/**
 *
 * Implements the Implicit Flow
 *
 * @see <a href="http://tools.ietf.org/html/rfc6749#section-4.2">4.2.  Implicit Grant</a>
 */
public class TokenResponseType implements ResponseType {

    public CoreToken createToken(Map<String, Object> data){
        OAuth2TokenStore store = OAuth2ConfigurationFactory.Holder.getConfigurationFactory().getTokenStore();
        return store.createAccessToken((String)data.get(OAuth2Constants.CoreTokenParams.TOKEN_TYPE),
                (Set<String>)data.get(OAuth2Constants.CoreTokenParams.SCOPE),
                (String)data.get(OAuth2Constants.CoreTokenParams.REALM),
                (String)data.get(OAuth2Constants.CoreTokenParams.USERNAME),
                (String)data.get(OAuth2Constants.CoreTokenParams.CLIENT_ID),
                (String)data.get(OAuth2Constants.CoreTokenParams.REDIRECT_URI),
                null,
                null,
                (String)data.get(OAuth2Constants.Params.GRANT_TYPE));
    }

    public String getReturnLocation(){
        return "FRAGMENT";
    }

    public String URIParamValue(){
        return "access_token";
    }
}

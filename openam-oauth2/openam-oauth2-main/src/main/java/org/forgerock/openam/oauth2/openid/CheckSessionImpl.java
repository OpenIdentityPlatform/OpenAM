/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS All rights reserved.
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

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.openam.oauth2.model.ClientApplication;
import org.forgerock.openam.oauth2.model.impl.ClientApplicationImpl;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckSessionImpl implements CheckSession {

    SSOTokenManager ssoTokenManager = null;

    public CheckSessionImpl(){
        try {
            ssoTokenManager = SSOTokenManager.getInstance();
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("CheckSessionImpl():Unable to get the SSOTokenManager", e);
        }
    }

    public CheckSessionImpl(SSOTokenManager ssoTokenManager){
        this.ssoTokenManager = ssoTokenManager;
    }
    /**
     * {@inheritDoc}
     */
    public String getCookieName(){
        return SystemProperties.get("com.iplanet.am.cookie.name");
    }

    /**
     * {@inheritDoc}
     */
    public String getClientSessionURI(HttpServletRequest request){
        SignedJwt jwt = getIDToken(request);
        if (jwt == null){
            return "";
        }
        List<String> clients = jwt.getClaimsSet().getAudience();
        String realm = (String)jwt.getClaimsSet().getClaim("realm");
        if (clients != null && !clients.isEmpty()){
            String client = clients.iterator().next();

            AMIdentity id = OAuth2Utils.getClientIdentity(client, realm);
            ClientApplication clientApplication = new ClientApplicationImpl(id);
            return clientApplication.getClientSessionURI();
        }
        return "";
    }

    /**
     * {@inheritDoc}
     */
    public boolean getValidSession(HttpServletRequest request){
        SignedJwt jwt = getIDToken(request);
        if (jwt == null){
            return false;
        }
        try {
            String sessionID = (String) jwt.getClaimsSet().getClaim(OAuth2Constants.JWTTokenParams.OPS);
            SSOToken ssoToken = ssoTokenManager.createSSOToken(sessionID);
            return ssoTokenManager.isValidToken(ssoToken);
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("CheckSessionImpl.getValidSession():Unable to get the SSO token in the JWT", e);
            return false;
        }

    }

    private SignedJwt getIDToken(HttpServletRequest request){
        URI referer = null;
        try {
            referer = new URI(request.getHeader("Referer"));
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("No id_token supplied to the checkSesison endpoint", e);
            return null;
        }
        Map<String, String> map = null;
        if (referer != null && referer.getQuery() != null && !referer.getQuery().isEmpty()){
            String query =  referer.getQuery();
            String[] params = query.split("&");
            map = new HashMap<String, String>();
            for (String param : params){
                int split = param.indexOf('=');
                String name = param.substring(0, split);
                String value = param.substring(split+1, param.length());
                map.put(name, value);
            }
        }

        if (map != null && map.containsKey("id_token")){
            String id_token = map.get("id_token");

            JwtReconstruction jwtReconstruction = new JwtReconstruction();
            return jwtReconstruction.reconstructJwt(id_token, SignedJwt.class);
        }
        return null;
    }

}

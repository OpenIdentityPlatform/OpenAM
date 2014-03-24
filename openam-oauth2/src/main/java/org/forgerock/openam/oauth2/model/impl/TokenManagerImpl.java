/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock AS. All rights reserved.
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
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.openam.oauth2.model.impl;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.openam.oauth2.exceptions.TokenTypeNotFoundException;
import org.forgerock.openam.oauth2.model.TokenManager;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TokenManagerImpl implements TokenManager {

    private static TokenManagerImpl instance = null;
    private static Map<String, Map<String, String>> realmTokens = null;
    //private static OAuth2TokenStore tokenStore = new DefaultOAuthTokenStoreImpl();

    static {
        realmTokens = new HashMap<String, Map<String, String>>();
    }

    public static TokenManagerImpl getInstance(){
        if (instance == null){
            instance = new TokenManagerImpl();
        }
        return instance;
    }

    public Map<String, Object> dataNeededToCreateToken(String tokenID, String org){
        return null;
    }

    public Object createToken(String tokenID, Map<String, Set<String>> tokenData, String realm) throws TokenTypeNotFoundException, Exception {

        if (realm == null){
            realm = "/";
        }

        //boolean to prevent double reads
        boolean read = false;

        Map<String, String> tokens = realmTokens.get(realm);
        if (tokens == null || tokens.isEmpty()){
            //TODO read from realm to get tokens then retry
            updateCache(realm);
            read = true;
            tokens = realmTokens.get(realm);
            if (tokens == null || tokens.isEmpty()){
                throw new TokenTypeNotFoundException();
            }
        }
        String tokenClassName = tokens.get(tokenID);
        if (tokenClassName == null || tokenClassName.isEmpty()){
            //TODO read from realm again
            if (!read){
                updateCache(realm);
            }
            read = true;
            tokenClassName = tokens.get(tokenID);
            if (tokenClassName == null || tokenClassName.isEmpty()){
                throw new TokenTypeNotFoundException();
            }
            throw new TokenTypeNotFoundException();
        }

        Class clazz = null;
        Object tokenClass = null;
        try{
            clazz = Class.forName(tokenClassName);
            tokenClass = Class.forName(tokenClassName).newInstance();
            Method method = clazz.getDeclaredMethod("create", Map.class);
            method.invoke(tokenClass, tokenData);
        } catch (Exception e){
            throw new Exception(e);
        }
        //persistent storage.create(tokenID, tokenClass)
        return tokenClass;
    }

    private void updateCache(String realm){

        ServiceConfig scm = null;
        try {
            SSOToken token = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
            ServiceConfigManager mgr = new ServiceConfigManager(token, OAuth2Constants.OAuth2ProviderService.NAME, OAuth2Constants.OAuth2ProviderService.VERSION);
            scm = mgr.getOrganizationConfig(realm, null);
        } catch (SSOException e){
            //TODO
        } catch (SMSException e){
            //TODO
        }
        Map<String, Set<String>> attrs = scm.getAttributes();
        Set<String> tokens = attrs.get(OAuth2Constants.OAuth2ProviderService.TOKEN_PLUGIN_LIST);
        Map<String, String> realmData = realmTokens.get(realm);

        if (tokens == null || tokens.isEmpty()){
            realmData = new HashMap<String, String>();
            return;
        }

        if (realmData == null){
            realmData = new HashMap<String, String>();
            realmTokens.put(realm,  realmData);
        }

        for (String tokenParts : tokens){
            String[] parts = tokenParts.split("\\|");
            if (parts.length != 2){
                continue;
            }
            realmData.put(parts[0], parts[1]);
        }
    }
}

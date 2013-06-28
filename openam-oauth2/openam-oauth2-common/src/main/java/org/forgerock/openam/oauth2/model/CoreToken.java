/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock US Inc. All Rights Reserved
 *
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
 * information:
 *
 * "Portions copyright [year] [name of copyright owner]".
 *
 */
package org.forgerock.openam.oauth2.model;


import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;

import java.util.*;

public class CoreToken extends JsonValue implements Token {

    private String id;
    private static ResourceBundle rb = ResourceBundle.getBundle("OAuth2CoreToken");

    public CoreToken(){
        super(new HashMap<String, Object>());
    }

    public CoreToken(String id, JsonValue value){
        super(value);
        this.id = id;

    }

    public CoreToken(String id, String userName, String realm, long expireTime, String tokenType, String tokenName){
        super(new HashMap<String, Object>());
        setTokenID(id);
        setUserName(userName);
        setRealm(realm);
        setExpireTime(expireTime);
        setTokenType(tokenType);
        setTokenName(tokenName);
    }

    /**
     * @{inheritDoc}
     */
    public Map<String, Object> convertToMap(){
        Map<String, Object> tokenMap = new HashMap<String, Object>();
        tokenMap.put(rb.getString(OAuth2Constants.CoreTokenParams.TOKEN_TYPE), getParameter(OAuth2Constants.CoreTokenParams.TOKEN_TYPE));
        tokenMap.put(rb.getString(OAuth2Constants.CoreTokenParams.EXPIRE_TIME), (System.currentTimeMillis() - getExpireTime())/1000);
        return tokenMap;
    }

    /**
     * Gets information about the token for the tokeninfo end point
     * @return
     */
    public Map<String, Object> getTokenInfo() {
        Map<String, Object> tokenMap = new HashMap<String, Object>();
        tokenMap.put(rb.getString(OAuth2Constants.CoreTokenParams.TOKEN_TYPE), getTokenType());
        tokenMap.put(rb.getString(OAuth2Constants.CoreTokenParams.EXPIRE_TIME), (System.currentTimeMillis() - getExpireTime())/1000);
        tokenMap.put(rb.getString(OAuth2Constants.CoreTokenParams.REALM), getRealm());
        tokenMap.put(rb.getString(OAuth2Constants.CoreTokenParams.SCOPE), getScope());
        return tokenMap;
    }

    protected void setTokenID(String id){
        this.id = id;
        this.put(OAuth2Constants.CoreTokenParams.ID, OAuth2Utils.stringToSet(id));
    }

    protected void setUserName(String userName) {
        this.put(OAuth2Constants.CoreTokenParams.USERNAME, OAuth2Utils.stringToSet(userName));
    }

    protected void setRealm(String realm) {
        if (realm == null || realm.isEmpty()){
            this.put(OAuth2Constants.CoreTokenParams.REALM, OAuth2Utils.stringToSet("/"));
        } else {
            this.put(OAuth2Constants.CoreTokenParams.REALM, OAuth2Utils.stringToSet(realm));
        }
    }

    protected void setExpireTime(long expireTime) {
        this.put(OAuth2Constants.CoreTokenParams.EXPIRE_TIME, OAuth2Utils.stringToSet(String.valueOf((expireTime * 1000) + System.currentTimeMillis())));
    }

    protected void setTokenType(String tokenType) {
        this.put(OAuth2Constants.CoreTokenParams.TOKEN_TYPE, OAuth2Utils.stringToSet(tokenType));
    }

    protected void setTokenName(String tokenName) {
        this.put(OAuth2Constants.CoreTokenParams.TOKEN_NAME, OAuth2Utils.stringToSet(tokenName));
    }

    /**
     * Get tokens id
     *
     * @return
     *          ID of token
     */
    public String getTokenID(){
        if (id != null){
            return id;
        } else {
            JsonValue val = this.get(OAuth2Constants.CoreTokenParams.ID);
            if (val != null){
                id = val.asString();
                return val.asString();
            }
        }
        return null;
    }

    /**
     * Gets the parent token
     * @return the id of the parent token
     */
    public String getParent(){
        Set<String> value = this.getParameter(OAuth2Constants.CoreTokenParams.PARENT);
        if (value != null && !value.isEmpty()){
            return value.iterator().next();
        }
        return null;
    }

    /**
     * Gets the issued state for code type
     * @return true or false if issued or not
     */
    public boolean isIssued(){
        if (this.getParameter(OAuth2Constants.CoreTokenParams.ISSUED) != null){
            return Boolean.parseBoolean(this.getParameter(OAuth2Constants.CoreTokenParams.ISSUED).iterator().next());
        } else {
            return false;
        }
    }

    /**
     * Gets the refresh token id
     * @return id of refresh token
     */
    public String getRefreshToken(){
        Set<String> value = this.getParameter(OAuth2Constants.CoreTokenParams.REFRESH_TOKEN);
        if (value != null && !value.isEmpty()){
            return value.iterator().next();
        }
        return null;
    }

    /**
     * Get tokens UserID
     *
     * @return
     *          ID of user
     */
    public String getUserID() {
        Set<String> value = this.getParameter(OAuth2Constants.CoreTokenParams.USERNAME);
        if (value != null && !value.isEmpty()){
            return value.iterator().next();
        }
        return null;
    }

    /**
     * Get Tokens Realm
     *
     * @return
     *          the realm
     */
    public String getRealm() {
        Set<String> value = this.getParameter(OAuth2Constants.CoreTokenParams.REALM);
        if (value != null && !value.isEmpty()){
            return value.iterator().next();
        }
        return null;
    }

    /**
     * Returns the seconds until token expire.
     *
     * @return time of expiry expressed as milliseconds since the epoch.
     */
    public long getExpireTime() {
        Set<String> value = this.getParameter(OAuth2Constants.CoreTokenParams.EXPIRE_TIME);
        if (value != null && !value.isEmpty()){
            return Long.parseLong(value.iterator().next());
        }
        return 0;
    }

    /**
     * Get tokens client
     *
     * @return
     *          the {@link SessionClient} for the token
     */
    public SessionClient getClient(){

        return new SessionClientImpl(getClientID(), getRedirectURI());
    }

    /**
     * Gets the tokens scope
     *
     * @return
     *          Set of strings that are the tokens scope
     */
    public Set<String> getScope(){
        Set<String> value = this.getParameter(OAuth2Constants.CoreTokenParams.SCOPE);
        if (value != null && !value.isEmpty()){
            return value;
        }
        return Collections.EMPTY_SET;
    }

    /**
     * Checks if token is expired
     *
     * @return
     *          true if expired
     *          false if not expired
     */
    public boolean isExpired() {
        return (System.currentTimeMillis() > getExpireTime());
    }

    /**
     * Returns the token type
     *
     * @return The type of token. For example {@link BearerToken}
     */
    public String getTokenType(){
        Set<String> value = this.getParameter(OAuth2Constants.CoreTokenParams.TOKEN_TYPE);
        if (value != null && !value.isEmpty()){
            return value.iterator().next();
        }
        return null;
    }

    /**
     * Returns the name of the token
     *
     * @return The name of token. Will be either access_token, code, refresh_token
     */
    public String getTokenName(){
        Set<String> value = this.getParameter(OAuth2Constants.CoreTokenParams.TOKEN_NAME);
        if (value != null && !value.isEmpty()){
            return value.iterator().next();
        }
        return null;
    }

    /**
     * Returns the client_id associated token
     *
     * @return The client_id associated with token
     */
    public String getClientID(){
        Set<String> value = this.getParameter(OAuth2Constants.CoreTokenParams.CLIENT_ID);
        if (value != null && !value.isEmpty()){
            return value.iterator().next();
        }
        return null;
    }

    /**
     * Returns the redirect_uri associated token
     *
     * @return The  redirect_uri associated with token
     */
    public String getRedirectURI(){
        Set<String> value = this.getParameter(OAuth2Constants.CoreTokenParams.REDIRECT_URI);
        if (value != null && !value.isEmpty()){
            return value.iterator().next();
        }
        return null;
    }
    /**
     * Gets any parameter stored in the token
     * @return
     */
    public Set<String> getParameter(String paramName){
        JsonValue param = get(paramName);
        if (param != null){
            return (Set<String>)param.getObject();
        }
        return null;
    }

    public String getIssued(){
        Set<String> value = this.getParameter(OAuth2Constants.CoreTokenParams.ISSUED);
        if (value != null && !value.isEmpty()){
            return value.iterator().next();
        }
        return null;
    }

    public void setIssued(){
        this.put(OAuth2Constants.CoreTokenParams.ISSUED, OAuth2Utils.stringToSet("true"));
    }

}

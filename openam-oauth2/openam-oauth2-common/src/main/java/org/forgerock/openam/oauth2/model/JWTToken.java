/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock AS All rights reserved.
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

package org.forgerock.openam.oauth2.model;

import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.jwe.EncryptedJwt;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.JwsHeader;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.model.impl.ClientApplicationImpl;
import org.forgerock.openam.oauth2.provider.OAuth2ProviderSettings;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.restlet.Request;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

public class JWTToken extends CoreToken implements Token {

    private JwtClaimsSet jwtClaimsSet = new JwtClaimsSet();
    private JwtBuilderFactory jwtBuilderFactory = new JwtBuilderFactory();
    private static ResourceBundle rb = ResourceBundle.getBundle("OAuth2CoreToken");

    /**
     *  Constructs a JWT token
     * @param iss issuer of the response
     * @param sub The subject of the jwt
     * @param aud The audience this JWT is meant for
     * @param azp The OAuth 2 client allowed to use this JWT as an access token
     * @param exp The time the JWT expires in seconds since epoc
     * @param iat The time the JWT was issued at in seconds since epoc.
     * @param ath The time the authorization for the JWT was made.
     * @param realm The realm the JWT belongs too.
     * @param nonce The nonce passed into the request
     * @param ops The session id used to create the JWT
     */
    public JWTToken(String iss, String sub, String aud, String azp, long exp, long iat, long ath, String realm, String nonce, String ops){
        setIssuer(iss);
        setSubject(sub);
        setAudience(aud);
        setAuthorizeParty(azp);
        setExpirationTime(exp);
        setIssueTime(iat);
        setAuthTime(ath);
        setRealm(realm);
        setNonce(nonce);
        setOPS(ops);
        setTokenType(OAuth2Constants.JWTTokenParams.JWT_TOKEN);
        setTokenName(OAuth2Constants.JWTTokenParams.ID_TOKEN);
        jwtClaimsSet = jwtBuilderFactory.claims().claims(asMap()).build();
    }

    public SignedJwt sign(PrivateKey pk) throws SignatureException {
        String clientID = getClientID();
        AMIdentity id = OAuth2Utils.getClientIdentity(clientID, getRealm());
        ClientApplication clientApplication = new ClientApplicationImpl(id);
        String algorithm = clientApplication.getIDTokenSignedResponseAlgorithm();
        JwsAlgorithm jwsAlgorithm = JwsAlgorithm.getJwsAlgorithm(algorithm);
        if (jwsAlgorithm == null){
            OAuth2Utils.DEBUG.error("JWTToken.sign()::Unable to find jws algorithm for: " + algorithm);
            throw new SignatureException();
        }
        JwsHeader header = new JwsHeader();
        header.setAlgorithm(jwsAlgorithm);
        header.setContentType("JWT");
        return new SignedJwt(header, jwtClaimsSet, pk);
    }

    public EncryptedJwt encrypt(PublicKey pk, JweAlgorithm alg, EncryptionMethod enc) throws SignatureException {
        return jwtBuilderFactory.jwe(pk)
                .headers()
                .alg(alg)
                .enc(enc)
                .done()
                .claims(jwtClaimsSet)
                .asJwt();
    }

    public String build() {
        return jwtBuilderFactory.jwt()
                .claims(jwtClaimsSet)
                .build();
    }

    /**
     * Creates an JWT ID Token
     *
     * @param id
     *            Id of the access Token
     * @param value
     *            A JsonValue map to populate this token with.
     */
    public JWTToken(String id, JsonValue value) {
        super(id, value);
    }

    @Override
    /**
     * @{inheritDoc}
     */
    public Map<String, Object> convertToMap(){
        Map<String, Object> tokenMap = new HashMap<String, Object>();
        try {
            tokenMap.put(OAuth2Constants.JWTTokenParams.ID_TOKEN,
                    this.sign(OAuth2Utils.getServerKeyPair(Request.getCurrent()).getPrivate()));
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Cant sign JWT", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Cant sign JWT");
        }
        return tokenMap;
    }

    /**
     * @{inheritDoc}
     */
    public Map<String, Object> getTokenInfo() {
        Map<String, Object> tokenMap = new HashMap<String, Object>();
        return tokenMap;
    }

    /**
     * @{inheritDoc}
     */
    public String getTokenID(){
        String s = null;
        try {
            s = this.sign(OAuth2Utils.getServerKeyPair(Request.getCurrent()).getPrivate()).build();
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Cant get JWT id", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Cant get JWT id");
        }
        return s;
    }
    /**
     *  Sets the issuer of the response
     * @param issuer user identifier of the issuer
     */
    private void setIssuer(String issuer){
        if (issuer == null || issuer.isEmpty()){
            return;
        }
        super.put(OAuth2Constants.JWTTokenParams.ISS, issuer);
    }

    /**
     *  Sets the subject of the JWT enduser
     * @param subject identifier of the end user
     */
    private void setSubject(String subject){
        if (subject == null || subject.isEmpty()){
            return;
        }
        super.put(OAuth2Constants.JWTTokenParams.SUB, subject);
    }

    /**
     * Sets the audience this JWT is intended for. Must contain the OAuth 2 client_id
     * @param audience The identifier of the audience
     */
    private void setAudience(String audience){
        if (audience == null || audience.isEmpty()){
            return;
        }
        super.put(OAuth2Constants.JWTTokenParams.AUD, audience);

    }

    /**
     * Sets the authorizing party of this JWT. The client_id that can use this token as an access_token
     * @param party identifier of the client_id
     */
    private void setAuthorizeParty(String party){
        if (party == null || party.isEmpty()){
            return;
        }
        super.put(OAuth2Constants.JWTTokenParams.AZP, party);
    }

    /**
     * Sets the expiration time of the token in seconds
     * @param time time the token expires in seconds since epoc
     */
    private void setExpirationTime(long time){
        super.put(OAuth2Constants.JWTTokenParams.EXP, time);
    }

    /**
     * The time in seconds the JWT was issued
     * @param time Seconds since epoc
     */
    private void setIssueTime(long time){
        super.put(OAuth2Constants.JWTTokenParams.IAT, time);
    }

    private void setOPS(String ops){
        super.put(OAuth2Constants.JWTTokenParams.OPS, ops);
    }

    /**
     * The time in seconds the enduser authenticated for the token
     * @param time Seconds since epoc
     */
    private void setAuthTime(long time){
        super.put(OAuth2Constants.JWTTokenParams.ATH, time);
    }

    protected void setTokenType(String tokenType){
        if (tokenType == null || tokenType.isEmpty()){
            return;
        }
        super.put(OAuth2Constants.CoreTokenParams.TOKEN_TYPE, tokenType);
    }

    protected void setTokenName(String tokenName){
        if (tokenName == null || tokenName.isEmpty()){
            return;
        }
        super.put(OAuth2Constants.CoreTokenParams.TOKEN_NAME, tokenName);
    }

    protected void setRealm(String realm){
        if (realm == null || realm.isEmpty()){
            return;
        }
        this.put(OAuth2Constants.CoreTokenParams.REALM, realm);
    }

    /**
     * Adds the request nonce to the JWT
     * @param nonce
     */
    protected void setNonce(String nonce){
        if (nonce == null || nonce.isEmpty()){
            return;
        }
        super.put(OAuth2Constants.JWTTokenParams.NONCE, nonce);
    }

    /**
     * {@inheritDoc}
     */
    public String getUserID() {
        return this.get(OAuth2Constants.JWTTokenParams.SUB).asString();
    }

    /**
     * {@inheritDoc}
     */
    public String getRealm() {
        return this.get(OAuth2Constants.CoreTokenParams.REALM).asString();
    }

    /**
     * {@inheritDoc}
     */
    public long getExpireTime() {
        return Long.parseLong(this.get(OAuth2Constants.JWTTokenParams.EXP).asString());
    }

    /**
     * {@inheritDoc}
     */
    public boolean isExpired() {
        return (System.currentTimeMillis() > getExpireTime());
    }

    /**
     * {@inheritDoc}
     */
    public String getTokenType(){
        return this.get(OAuth2Constants.CoreTokenParams.TOKEN_TYPE).asString();
    }

    /**
     * {@inheritDoc}
     */
    public String getTokenName(){
        return this.get(OAuth2Constants.CoreTokenParams.TOKEN_NAME).asString();
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getScope(){
        return Collections.EMPTY_SET;
    }

    /**
     * {@inheritDoc}
     */
    public String getClientID(){
        return this.get(OAuth2Constants.JWTTokenParams.AUD).asString();
    }
}


/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock Inc. All rights reserved.
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

import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.model.ClientApplication;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.restlet.Request;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of a {@link ClientApplication}
 */
public class ClientApplicationImpl implements ClientApplication{
    //oauth2 options
    private static final String AUTO_GRANT = "com.forgerock.openam.oauth2provider.autoGrant";
    private static final String TOKEN_TYPE = "com.forgerock.openam.oauth2provider.tokenType";

    AMIdentity id = null;

    public ClientApplicationImpl(AMIdentity id) {
        this.id = id;
    }

    /**
     * {@inheritDoc}
     */
    public String getClientId(){
        return id.getName();
    }

    /**
     * {@inheritDoc}
     */
    public ClientType getClientType(){
        ClientType clientType = null;
        try {
            Set<String> clientTypeSet = id.getAttribute(OAuth2Constants.OAuth2Client.CLIENT_TYPE);
            if (clientTypeSet.iterator().next().equalsIgnoreCase("CONFIDENTIAL")){
                clientType = ClientType.CONFIDENTIAL;
            } else {
                clientType = ClientType.PUBLIC;
            }
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Unable to get "+ OAuth2Constants.OAuth2Client.CLIENT_TYPE +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.CLIENT_TYPE +" from repository");
        }
        return clientType;
    }

    /**
     * {@inheritDoc}
     */
    public Set<URI> getRedirectionURIs(){
        Set<URI> redirectionURIs = null;
        try {
            Set<String> redirectionURIsSet = id.getAttribute(OAuth2Constants.OAuth2Client.REDIRECT_URI);
            redirectionURIsSet = convertAttributeValues(redirectionURIsSet);
            redirectionURIs = new HashSet<URI>();
            for (String uri : redirectionURIsSet){
                redirectionURIs.add(URI.create(uri));
            }
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Unable to get "+ OAuth2Constants.OAuth2Client.REDIRECT_URI +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.REDIRECT_URI +" from repository");
        }
        return redirectionURIs;
    }

    /**
     * {@inheritDoc}
     */
    public String getAccessTokenType(){
        /*
        Set<String> tokenTypesSet = null;
        try {
            tokenTypesSet = id.getAttribute(TOKEN_TYPE);
        } catch (Exception e){
            OAuth2Utils.debug.error("Unable to get access token type from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get access token type from repository");
        }
        return tokenTypesSet.iterator().next();
        */
        return OAuth2Constants.Bearer.BEARER;
    }

    /**
     * {@inheritDoc}
     */
    public String getClientAuthenticationSchema(){
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getAllowedGrantScopes(){
        Set<String> scopes = null;
        try {
            scopes = id.getAttribute(OAuth2Constants.OAuth2Client.SCOPES);
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Unable to get "+ OAuth2Constants.OAuth2Client.SCOPES +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.SCOPES +" from repository");
        }
        return convertAttributeValues(scopes);
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getDefaultGrantScopes(){
        Set<String> scopes = null;
        try {
            scopes = id.getAttribute(OAuth2Constants.OAuth2Client.DEFAULT_SCOPES);
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Unable to get "+ OAuth2Constants.OAuth2Client.DEFAULT_SCOPES +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.DEFAULT_SCOPES +" from repository");
        }
        return convertAttributeValues(scopes);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAutoGrant(){
        Set<String> autoGrantSet = null;
        boolean grant = false;
        try {
            autoGrantSet = id.getAttribute(AUTO_GRANT);
            grant = Boolean.parseBoolean(autoGrantSet.iterator().next());
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Unable to get "+ AUTO_GRANT +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ AUTO_GRANT +" from repository");
        }
        return grant;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getDisplayName(){
        Set<String> displayName = null;
        try {
            displayName = id.getAttribute(OAuth2Constants.OAuth2Client.NAME);
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Unable to get "+ OAuth2Constants.OAuth2Client.RESPONSE_TYPES +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.RESPONSE_TYPES +" from repository");
        }
        return convertAttributeValues(displayName);
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getDisplayDescription(){
        Set<String> displayDescription = null;
        try {
            displayDescription = id.getAttribute(OAuth2Constants.OAuth2Client.DESCRIPTION);
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Unable to get "+ OAuth2Constants.OAuth2Client.RESPONSE_TYPES +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.RESPONSE_TYPES +" from repository");
        }
        return convertAttributeValues(displayDescription);
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getResponseTypes(){

        Set<String> set = null;
        try {
            set = id.getAttribute(OAuth2Constants.OAuth2Client.RESPONSE_TYPES);
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Unable to get "+ OAuth2Constants.OAuth2Client.RESPONSE_TYPES +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.RESPONSE_TYPES +" from repository");
        }
        return convertAttributeValues(set);

    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getGrantTypes(){

        Set<String> set = null;
        try {
            set = id.getAttribute(OAuth2Constants.OAuth2Client.GRANT_TYPES);
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Unable to get "+ OAuth2Constants.OAuth2Client.GRANT_TYPES +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.GRANT_TYPES +" from repository");
        }
        return convertAttributeValues(set);

    }

    /**
     * {@inheritDoc}
     */
    public String getContacts(){

        Set<String> set = null;
        try {
            set = id.getAttribute(OAuth2Constants.OAuth2Client.CONTACTS);
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Unable to get "+ OAuth2Constants.OAuth2Client.CONTACTS +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.CONTACTS +" from repository");
        }
        return set.iterator().next();

    }

    /**
     * {@inheritDoc}
     */
    public String getClientName(){

        Set<String> set = null;
        try {
            set = id.getAttribute(OAuth2Constants.OAuth2Client.NAME);
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Unable to get "+ TOKEN_TYPE +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.NAME +" from repository");
        }
        return set.iterator().next();

    }

    /**
     * {@inheritDoc}
     */
    public String getLogoURI(){

        Set<String> set = null;
        try {
            set = id.getAttribute(OAuth2Constants.OAuth2Client.LOGO_URI);
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Unable to get "+ OAuth2Constants.OAuth2Client.LOGO_URI +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.LOGO_URI +" from repository");
        }
        return set.iterator().next();

    }

    /**
     * {@inheritDoc}
     */
    public String getTokenEndpointAuthMethod(){

        Set<String> set = null;
        try {
            set = id.getAttribute(OAuth2Constants.OAuth2Client.TOKEN_ENDPOINT_AUTH_METHOD);
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Unable to get "+ OAuth2Constants.OAuth2Client.TOKEN_ENDPOINT_AUTH_METHOD +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.TOKEN_ENDPOINT_AUTH_METHOD +" from repository");
        }
        return set.iterator().next();

    }

    /**
     * {@inheritDoc}
     */
    public String getPolicyURI(){

        Set<String> set = null;
        try {
            set = id.getAttribute(OAuth2Constants.OAuth2Client.POLICY_URI);
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Unable to get "+ OAuth2Constants.OAuth2Client.POLICY_URI +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.POLICY_URI +" from repository");
        }
        return set.iterator().next();

    }

    /**
     * {@inheritDoc}
     */
    public String getTosURI(){

        Set<String> set = null;
        try {
            set = id.getAttribute(OAuth2Constants.OAuth2Client.TOS_URI);
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Unable to get "+ OAuth2Constants.OAuth2Client.TOS_URI +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.TOS_URI +" from repository");
        }
        return set.iterator().next();

    }

    /**
     * {@inheritDoc}
     */
    public String getJwksURI(){

        Set<String> set = null;
        try {
            set = id.getAttribute(OAuth2Constants.OAuth2Client.JKWS_URI);
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Unable to get "+ OAuth2Constants.OAuth2Client.JKWS_URI +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.JKWS_URI +" from repository");
        }
        return set.iterator().next();

    }

    /**
     * {@inheritDoc}
     */
    public String getSectorIdentifierURI(){

        Set<String> set = null;
        try {
            set = id.getAttribute(OAuth2Constants.OAuth2Client.SECTOR_IDENTIFIER_URI);
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Unable to get "+ OAuth2Constants.OAuth2Client.SECTOR_IDENTIFIER_URI +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.SECTOR_IDENTIFIER_URI +" from repository");
        }
        return set.iterator().next();

    }

    /**
     * {@inheritDoc}
     */
    public SubjectType getSubjectType(){
        SubjectType subjectType = null;
        try {
            Set<String> clientTypeSet = id.getAttribute(OAuth2Constants.OAuth2Client.SUBJECT_TYPE);
            if (clientTypeSet.iterator().next().equalsIgnoreCase("PAIRWISE")){
                subjectType = SubjectType.PAIRWISE;
            } else {
                subjectType = SubjectType.PUBLIC;
            }
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Unable to get "+ OAuth2Constants.OAuth2Client.SUBJECT_TYPE +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.SUBJECT_TYPE +" from repository");
        }
        return subjectType;
    }

    /**
     * {@inheritDoc}
     */
    public String getRequestObjectSigningAlgorithm(){

        Set<String> set = null;
        try {
            set = id.getAttribute(OAuth2Constants.OAuth2Client.REQUEST_OBJECT_SIGNING_ALG);
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Unable to get "+ OAuth2Constants.OAuth2Client.REQUEST_OBJECT_SIGNING_ALG +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.REQUEST_OBJECT_SIGNING_ALG +" from repository");
        }
        return set.iterator().next();

    }

    /**
     * {@inheritDoc}
     */
    public String getUserInfoSignedResponseAlgorithm(){

        Set<String> set = null;
        try {
            set = id.getAttribute(OAuth2Constants.OAuth2Client.USERINFO_SIGNED_RESPONSE_ALG);
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Unable to get "+ OAuth2Constants.OAuth2Client.USERINFO_SIGNED_RESPONSE_ALG +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.USERINFO_SIGNED_RESPONSE_ALG +" from repository");
        }
        return set.iterator().next();

    }

    /**
     * {@inheritDoc}
     */
    public String getUserInfoEncryptedResposneAlgorithm(){

        Set<String> set = null;
        try {
            set = id.getAttribute(OAuth2Constants.OAuth2Client.USERINFO_ENCRYPTED_RESPONSE_ALG);
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Unable to get "+ OAuth2Constants.OAuth2Client.USERINFO_ENCRYPTED_RESPONSE_ALG +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.USERINFO_ENCRYPTED_RESPONSE_ALG +" from repository");
        }
        return set.iterator().next();

    }

    /**
     * {@inheritDoc}
     */
    public String getUserInfoEncryptedResponseEncoding(){

        Set<String> set = null;
        try {
            set = id.getAttribute(OAuth2Constants.OAuth2Client.USERINFO_SIGN_AND_ENC_RESPONSE_ALG);
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Unable to get "+ OAuth2Constants.OAuth2Client.USERINFO_SIGN_AND_ENC_RESPONSE_ALG +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.USERINFO_SIGN_AND_ENC_RESPONSE_ALG +" from repository");
        }
        return set.iterator().next();

    }

    /**
     * {@inheritDoc}
     */
    public String getIDTokenSignedResponseAlgorithm(){

        Set<String> set = null;
        try {
            set = id.getAttribute(OAuth2Constants.OAuth2Client.IDTOKEN_SIGNED_RESPONSE_ALG);
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Unable to get "+ OAuth2Constants.OAuth2Client.IDTOKEN_SIGNED_RESPONSE_ALG +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.IDTOKEN_SIGNED_RESPONSE_ALG +" from repository");
        }
        return set.iterator().next();

    }

    /**
     * {@inheritDoc}
     */
    public String getIDTokenEncryptedResposneAlgorithm(){

        Set<String> set = null;
        try {
            set = id.getAttribute(OAuth2Constants.OAuth2Client.IDTOKEN_ENCRYPTED_RESPONSE_ALG);
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Unable to get "+ OAuth2Constants.OAuth2Client.IDTOKEN_ENCRYPTED_RESPONSE_ALG +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.IDTOKEN_ENCRYPTED_RESPONSE_ALG +" from repository");
        }
        return set.iterator().next();

    }

    /**
     * {@inheritDoc}
     */
    public String getIDTokenEncryptedResponseEncoding(){

        Set<String> set = null;
        try {
            set = id.getAttribute(OAuth2Constants.OAuth2Client.IDTOKEN_ENC_AND_SIGNED_RESPONSE_ALG);
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Unable to get "+ OAuth2Constants.OAuth2Client.IDTOKEN_ENC_AND_SIGNED_RESPONSE_ALG +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.IDTOKEN_ENC_AND_SIGNED_RESPONSE_ALG +" from repository");
        }
        return set.iterator().next();

    }

    /**
     * {@inheritDoc}
     */
    public String getDefaultMaxAge(){

        Set<String> set = null;
        try {
            set = id.getAttribute(OAuth2Constants.OAuth2Client.DEFAULT_MAX_AGE);
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Unable to get "+ OAuth2Constants.OAuth2Client.DEFAULT_MAX_AGE +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.DEFAULT_MAX_AGE +" from repository");
        }
        return set.iterator().next();

    }

    /**
     * {@inheritDoc}
     */
    public String getRequireAuthTime(){

        Set<String> set = null;
        try {
            set = id.getAttribute(OAuth2Constants.OAuth2Client.REQUIRE_AUTH_TIME);
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Unable to get "+ OAuth2Constants.OAuth2Client.REQUIRE_AUTH_TIME +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.REQUIRE_AUTH_TIME +" from repository");
        }
        return set.iterator().next();

    }

    /**
     * {@inheritDoc}
     */
    public String getDefaultACRValues(){

        Set<String> set = null;
        try {
            set = id.getAttribute(OAuth2Constants.OAuth2Client.DEFAULT_ACR_VALS);
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Unable to get "+ OAuth2Constants.OAuth2Client.DEFAULT_ACR_VALS +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.DEFAULT_ACR_VALS +" from repository");
        }
        return set.iterator().next();

    }

    /**
     * {@inheritDoc}
     */
    public String getinitiateLoginURI(){

        Set<String> set = null;
        try {
            set = id.getAttribute(OAuth2Constants.OAuth2Client.INIT_LOGIN_URL);
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Unable to get "+ OAuth2Constants.OAuth2Client.INIT_LOGIN_URL +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.INIT_LOGIN_URL +" from repository");
        }
        return set.iterator().next();

    }

    /**
     * {@inheritDoc}
     */
    public String getPostLogoutRedirectionURI(){

        Set<String> set = null;
        try {
            set = id.getAttribute(OAuth2Constants.OAuth2Client.POST_LOGOUT_URI);
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Unable to get "+ OAuth2Constants.OAuth2Client.POST_LOGOUT_URI +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.POST_LOGOUT_URI +" from repository");
        }
        return set.iterator().next();

    }

    /**
     * {@inheritDoc}
     */
    public String getRequestURIS(){

        Set<String> set = null;
        try {
            set = id.getAttribute(OAuth2Constants.OAuth2Client.REQUEST_URLs);
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Unable to get "+ OAuth2Constants.OAuth2Client.REQUEST_URLs +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.REQUEST_URLs +" from repository");
        }
        return set.iterator().next();

    }

    /**
     * {@inheritDoc}
     */
    public String getAccessToken(){

        Set<String> set = null;
        try {
            set = id.getAttribute(OAuth2Constants.OAuth2Client.ACCESS_TOKEN);
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("Unable to get "+ OAuth2Constants.OAuth2Client.ACCESS_TOKEN +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.ACCESS_TOKEN +" from repository");
        }
        return set.iterator().next();

    }

    private Set<String> convertAttributeValues(Set<String> input) {
        Set<String> result = new HashSet<String>();
        for (String param : input) {
            int idx = param.indexOf('=');
            if (idx != -1) {
                String value = param.substring(idx + 1).trim();
                if (!value.isEmpty()) {
                    result.add(value);
                }
            }
        }

        return result;
    }
}

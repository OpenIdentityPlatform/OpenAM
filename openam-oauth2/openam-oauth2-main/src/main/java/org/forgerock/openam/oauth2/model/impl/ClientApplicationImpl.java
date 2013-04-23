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
 * "Portions Copyrighted [2012] [Forgerock Inc]"
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
    private static final String CLIENT_TYPE = "com.forgerock.openam.oauth2provider.clientType";
    private static final String REDIRECTION_URIS = "com.forgerock.openam.oauth2provider.redirectionURIs";
    private static final String SCOPES = "com.forgerock.openam.oauth2provider.scopes";
    private static final String DEFAULT_SCOPES = "com.forgerock.openam.oauth2provider.defaultScopes";
    private static final String NAME = "com.forgerock.openam.oauth2provider.name";
    private static final String DESCRIPTION = "com.forgerock.openam.oauth2provider.description";
    private static final String AUTO_GRANT = "com.forgerock.openam.oauth2provider.autoGrant";
    private static final String TOKEN_TYPE = "com.forgerock.openam.oauth2provider.tokenType";

    AMIdentity id = null;

    public ClientApplicationImpl(AMIdentity id) {
        this.id = id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getClientId(){
        return id.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClientType getClientType(){
        ClientType clientType = null;
        try {
            Set<String> clientTypeSet = id.getAttribute(CLIENT_TYPE);
            if (clientTypeSet.iterator().next().equalsIgnoreCase("CONFIDENTIAL")){
                clientType = ClientType.CONFIDENTIAL;
            } else {
                clientType = ClientType.PUBLIC;
            }
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("ClientApplicationImpl::Unable to get client type from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get client type from repository");
        }
        return clientType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<URI> getRedirectionURIs(){
        Set<URI> redirectionURIs = null;
        try {
            Set<String> redirectionURIsSet = convertAttributeValues(id.getAttribute(REDIRECTION_URIS));
            redirectionURIs = new HashSet<URI>();
            for (String uri : redirectionURIsSet){
                redirectionURIs.add(URI.create(uri));
            }
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("ClientApplicationImpl::Unable to get redirection URLs from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get redirection URLs from repository");
        }
        return redirectionURIs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    public String getClientAuthenticationSchema(){
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getAllowedGrantScopes(){
        Set<String> scopes = null;
        try {
            scopes = convertAttributeValues(id.getAttribute(SCOPES));
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("ClientApplicationImpl::Unable to get allowed grant scopes from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get allowed grant scopes from repository");
        }
        return scopes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getDefaultGrantScopes(){
        Set<String> scopes = null;
        try {
            scopes = convertAttributeValues(id.getAttribute(DEFAULT_SCOPES));
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("ClientApplicationImpl::Unable to get defualt grant scopes from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get default grant scopes from repository");
        }
        return scopes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAutoGrant(){
        Set<String> autoGrantSet = null;
        boolean grant = false;
        try {
            autoGrantSet = id.getAttribute(AUTO_GRANT);
            grant = Boolean.parseBoolean(autoGrantSet.iterator().next());
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("ClientApplicationImpl::Unable to get auto grant status from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get auto grant status from repository");
        }
        return grant;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getDisplayName(){
        Set<String> displayName = null;
        try {
            displayName = convertAttributeValues(id.getAttribute(NAME));
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("ClientApplicationImpl::Unable to get display name from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get display name from repository");
        }
        return displayName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getDisplayDescription(){
        Set<String> displayDescription = null;
        try {
            displayDescription = convertAttributeValues(id.getAttribute(DESCRIPTION));
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("ClientApplicationImpl::Unable to get display decription from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get display description from repository");
        }
        return displayDescription;
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

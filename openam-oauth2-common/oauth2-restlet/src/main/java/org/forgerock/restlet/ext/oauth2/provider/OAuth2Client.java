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
 * "Portions copyright [year] [name of copyright owner]"
 */

package org.forgerock.restlet.ext.oauth2.provider;

import java.net.URI;

import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.model.ClientApplication;
import org.forgerock.openam.oauth2.model.SessionClient;
import org.restlet.security.User;

/**
 * Implements a OAuth2 client.
 */
public class OAuth2Client extends User {

    private final ClientApplication client;

    public OAuth2Client(ClientApplication client) {
        super(client.getClientId());
        this.client = client;
    }

    public ClientApplication getClient() {
        return client;
    }

    /**
     * Validate the {@code redirectionURI} and return an object used in the
     * session.
     * <p/>
     * Throws
     * {@link org.forgerock.openam.oauth2.exceptions.OAuthProblemException.OAuthError#REDIRECT_URI_MISMATCH}
     * <p/>
     * The authorization server SHOULD require all clients to register their
     * redirection endpoint prior to utilizing the authorization endpoint
     * <p/>
     * The authorization server SHOULD require the client to provide the
     * complete redirection URI (the client MAY use the "state" request
     * parameter to achieve per-request customization). If requiring the
     * registration of the complete redirection URI is not possible, the
     * authorization server SHOULD require the registration of the URI scheme,
     * authority, and path (allowing the client to dynamically vary only the
     * query component of the redirection URI when requesting authorization).
     * <p/>
     * The authorization server MAY allow the client to register multiple
     * redirection endpoints.
     * 
     * @param redirectionURI
     * @return
     * @throws org.forgerock.openam.oauth2.exceptions.OAuthProblemException
     * 
     * @see <a
     *      href="http://tools.ietf.org/html/draft-ietf-oauth-v2-24#section-3.1.2.2">3.1.2.2.
     *      Registration Requirements</a>
     * @see <a
     *      href="http://tools.ietf.org/html/draft-ietf-oauth-v2-24#section-3.1.2.3">3.1.2.3.
     *      Dynamic Configuration</a>
     */
    public SessionClient getClientInstance(String redirectionURI) throws OAuthProblemException {
        if (OAuth2Utils.isBlank(redirectionURI)) {
            if (getClient().getRedirectionURIs().isEmpty()) {
                // Redirect URI is not registered and not in the request
                OAuth2Utils.DEBUG.error("OAuth2Client::Missing parameter: redirect_uri");
                throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(null,
                        "Missing parameter: redirect_uri");
            } else if (getClient().getRedirectionURIs().size() == 1) {
                return new SessionClientImpl(getClient().getClientId(), getClient()
                        .getRedirectionURIs().iterator().next().toString());
            } else {
                OAuth2Utils.DEBUG.error("OAuth2Client::Missing parameter: redirect_uri");
                throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(null,
                        "Missing parameter: redirect_uri");
            }
        } else {
            URI request = URI.create(redirectionURI);
            if (request.getFragment() != null){
                OAuth2Utils.DEBUG.error("OAuth2Client:: Redirect URI cannot contain a fragment");
                throw OAuthProblemException.OAuthError.REDIRECT_URI_MISMATCH.handle(null);
            }
            if (!request.isAbsolute()){
                OAuth2Utils.DEBUG.error("OAuth2Client:: Redirect URI must be absolute");
                throw OAuthProblemException.OAuthError.REDIRECT_URI_MISMATCH.handle(null);
            }
            for (URI uri : getClient().getRedirectionURIs()) {
                if (uri.equals(request)) {
                    return new SessionClientImpl(getClient().getClientId(), uri.toString());
                }
            }
            OAuth2Utils.DEBUG.error("OAuth2Client:: Redirect URI mismatch");
            throw OAuthProblemException.OAuthError.REDIRECT_URI_MISMATCH.handle(null);
        }
    }

    public class SessionClientImpl implements SessionClient {

        private static final long serialVersionUID = 1934721539808864899L;

        private String clientId;
        private String redirectUri;

        private SessionClientImpl(String clientId, String redirectUri) {
            this.clientId = clientId;
            this.redirectUri = redirectUri;
        }

        public String getClientId() {
            return clientId;
        }

        public String getRedirectUri() {
            return redirectUri;
        }
    }

}

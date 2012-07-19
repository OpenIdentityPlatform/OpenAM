/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
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
package org.forgerock.restlet.ext.oauth2.provider;

import java.util.Collection;
import java.util.logging.Level;

import org.forgerock.restlet.ext.oauth2.OAuth2;
import org.forgerock.restlet.ext.oauth2.OAuth2Utils;
import org.forgerock.restlet.ext.oauth2.OAuthProblemException;
import org.forgerock.restlet.ext.oauth2.model.ClientApplication;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Status;
import org.restlet.security.Authenticator;

/**
 * Since this client authentication method involves a password, the
 * authorization server MUST protect any endpoint utilizing it against brute
 * force attacks.
 * 
 * @see <a
 *      href="http://tools.ietf.org/html/draft-ietf-oauth-v2-24#section-2.3">2.3.
 *      Client Authentication</a>
 */
public class ClientAuthenticationFilter extends Authenticator {

    private volatile ClientVerifier verifier;

    public ClientAuthenticationFilter(Context context) {
        super(context, true);
        setMultiAuthenticating(false);
        setOptional(true);
    }

    public ClientVerifier getVerifier() {
        return verifier;
    }

    public void setVerifier(ClientVerifier verifier) {
        this.verifier = verifier;
    }

    /**
     * If the client type is confidential or the client was issued client
     * credentials (or assigned other authentication requirements), the client
     * MUST authenticate with the authorization server as described in Section
     * 3.2.1.
     * 
     * @param request
     * @param response
     * @return
     * @see <a
     *      hrfe="http://tools.ietf.org/html/draft-ietf-oauth-v2-25#section-3.2.1"
     *      >3.2.1. Client Authentication</a>
     */
    protected boolean authenticate(Request request, Response response) {
        boolean result = false;
        boolean loggable = request.isLoggable() && getLogger().isLoggable(Level.FINE);

        if (getVerifier() != null) {
            String client_id =
                    OAuth2Utils.getRequestParameter(request, OAuth2.Params.CLIENT_ID, String.class);
            ClientApplication client;
            try {
                if (request.getChallengeResponse() != null) {
                    client = getVerifier().verify(request.getChallengeResponse());
                    request.getClientInfo().setUser(new OAuth2Client(client));
                } else {
                    String client_secret =
                            OAuth2Utils.getRequestParameter(request, OAuth2.Params.CLIENT_SECRET,
                                    String.class);
                    client = getVerifier().verify(client_id, client_secret);
                    request.getClientInfo().setUser(new OAuth2Client(client));
                }
            } catch (OAuthProblemException e) {
                if (null != client_id) {
                    Collection<ChallengeScheme> scheme =
                            getVerifier().getRequiredAuthenticationScheme(client_id);
                    // Todo Rechellenge the client
                }
                // TODO doError
            }
            result = true;
        } else {
            getLogger().warning("Authentication failed. No verifier provided.");
            response.setStatus(Status.SERVER_ERROR_INTERNAL,
                    "Authentication failed. No verifier provided.");
        }
        return result;
    }
}

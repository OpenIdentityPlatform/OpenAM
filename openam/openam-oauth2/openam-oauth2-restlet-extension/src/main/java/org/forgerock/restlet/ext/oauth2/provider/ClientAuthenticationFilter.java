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
 * "Portions copyright [year] [name of copyright owner]"
 */

/**
 * Portions copyright 2012-2013 ForgeRock Inc
 */
package org.forgerock.restlet.ext.oauth2.provider;

import java.io.*;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Collection;
import java.util.Map;

import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.openam.oauth2.provider.ClientVerifier;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.model.ClientApplication;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
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

        if (getVerifier() != null) {
            String client_id =
                    OAuth2Utils.getRequestParameter(request, OAuth2Constants.Params.CLIENT_ID, String.class);
            ClientApplication client;
            try {
                //this will be removed when client authentication is moved into the grant code
                String grantType = OAuth2Utils.getRequestParameter(request, OAuth2Constants.Params.GRANT_TYPE, String.class);
                if (grantType != null && grantType.equalsIgnoreCase(OAuth2Constants.SAML20.GRANT_TYPE_URI)){
                    //the assertion acts as the client authentication
                    client = getVerifier().findClient(client_id, request);
                    request.getClientInfo().setUser(new OAuth2Client(client));
                    result = true;
                } else {

                    client = getVerifier().verify(request, response);
                    request.getClientInfo().setUser(new OAuth2Client(client));
                    result = true;
                }
            } catch (OAuthProblemException e) {
                if (null != client_id) {
                    Collection<ChallengeScheme> scheme =
                            getVerifier().getRequiredAuthenticationScheme(client_id);
                }
                response.setEntity(new JacksonRepresentation<Map>(e.getErrorMessage()));
                throw e;
            }
        } else {
            OAuth2Utils.DEBUG.warning("ClientAuthenticationFilter::Authentication failed. No verifier provided.");
            response.setStatus(Status.SERVER_ERROR_INTERNAL,
                    "Authentication failed. No verifier provided.");
        }
        return result;
    }
}

/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All rights reserved.
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
 * "Portions Copyrighted [2012] [ForgeRock Inc]"
 */
package org.forgerock.openam.oauth2.provider;

import java.util.Collection;

import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.model.ClientApplication;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ChallengeScheme;

/**
 * The authorization server SHOULD require all clients to register their
 * redirection endpoint prior to utilizing the authorization endpoint
 * <p/>
 * The authorization server SHOULD require the client to provide the complete
 * redirection URI (the client MAY use the "state" request parameter to achieve
 * per-request customization). If requiring the registration of the complete
 * redirection URI is not possible, the authorization server SHOULD require the
 * registration of the URI scheme, authority, and path (allowing the client to
 * dynamically vary only the query component of the redirection URI when
 * requesting authorization).
 * <p/>
 * The authorization server MAY allow the client to register multiple
 * redirection endpoints.
 * 
 * @supported.all.api
 */
public interface ClientVerifier {
    /**
     * Authenticates the client
     *
     * @param request
     *            the HTTP Request
     * @param response
     *            the HTTP Response
     * @return Client if the credentials are correct
     * @throws OAuthProblemException
     *             when authentication failed or null if authentication fails
     */
    public ClientApplication verify(Request request, Response response)
            throws OAuthProblemException;

    /**
     * Get the configured HTTP Authentication scheme for the given
     * {@code client_id}
     * <p/>
     * The authorization server MAY support any suitable HTTP authentication
     * scheme matching its security requirements. When using other
     * authentication methods, the authorization server MUST define a mapping
     * between the client identifier (registration record) and authentication
     * scheme.
     * 
     * @param client_id
     * @return
     */
    public Collection<ChallengeScheme> getRequiredAuthenticationScheme(String client_id);


    /**
     * Find the client given a clientId.
     * @param clientId the client id to find
     * @param request the request that wants the client
     * @return
     * @throws OAuthProblemException
     */
    public ClientApplication findClient(String clientId, Request request) throws OAuthProblemException;

}

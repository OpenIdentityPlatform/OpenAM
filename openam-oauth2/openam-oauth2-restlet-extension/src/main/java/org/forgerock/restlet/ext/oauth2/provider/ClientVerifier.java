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

import org.forgerock.restlet.ext.oauth2.OAuthProblemException;
import org.forgerock.restlet.ext.oauth2.model.ClientApplication;
import org.restlet.data.ChallengeResponse;
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
 * @author $author$
 * @version $Revision$ $Date$
 */
public interface ClientVerifier {

    /**
     * Authenticates the client and use the
     * {@link org.restlet.engine.header.HeaderConstants#HEADER_AUTHORIZATION}
     * <p/>
     * TODO Implement verify(Request request, Response response) instead
     * 
     * @param challengeResponse
     *            from the HTTP Request
     * @return Client if the credentials are correct
     * @throws org.forgerock.restlet.ext.oauth2.OAuthProblemException
     *             when authentication failed
     */
    public ClientApplication verify(ChallengeResponse challengeResponse)
            throws OAuthProblemException;

    /**
     * Authenticates the client and use the
     * {@link org.forgerock.restlet.ext.oauth2.OAuth2.Params#CLIENT_ID} and
     * {@link org.forgerock.restlet.ext.oauth2.OAuth2.Params#CLIENT_SECRET} from
     * application/x-www-form-urlencoded Web From
     * 
     * @param client_id
     * @param client_secret
     * @return Client if the credentials are correct
     * @throws OAuthProblemException
     *             when authentication failed
     */
    public ClientApplication verify(String client_id, String client_secret)
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
     * Find the client by {@code client_id}
     * 
     * @param client_id
     *            client_id from the request
     * @return null if there is no client with the given identifier.
     */
    public ClientApplication findClient(String client_id);
}

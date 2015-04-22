/*
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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014-15 ForgeRock AS.
 */

package org.forgerock.openidconnect;

import java.net.URI;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.OAuth2Jwt;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;

/**
 * Models an OpenId Connect client registration in the OAuth2 provider.
 *
 * @since 12.0.0
 */
public interface OpenIdConnectClientRegistration extends ClientRegistration {

    /**
     * Gets the OpenId Token signed response algorithm.
     *
     * @return The OpenId token signed response algorithm.
     */
    String getIDTokenSignedResponseAlgorithm();

    /**
     * Gets the token_endpoint_auth_method configured for this client.
     */
    String getTokenEndpointAuthMethod();

    /**
     * Gets the subject type of this client. PAIRWISE or PUBLIC.
     */
    String getSubjectType();

    /**
     * Verifies that the supplied jwt is signed by this client.
     */
    boolean verifyJwtIdentity(OAuth2Jwt jwt);

    /**
     * Gets the subject identifier uri.
     */
    URI getSectorIdentifierUri();

    /**
     * Retrieve the sub value, appropriate for the client subject type, or null
     * if there are issues with its formation.
     */
    String getSubValue(String id, OAuth2ProviderSettings providerSettings);


}

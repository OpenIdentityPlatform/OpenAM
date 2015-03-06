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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.uma;

import javax.security.auth.Subject;
import java.net.URI;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Evaluator;
import org.forgerock.oauth2.core.exceptions.ServerException;

/**
 * Models all of the possible settings the UMA provider can have and that can be configured.
 *
 * @since 13.0.0
 */
public interface UmaProviderSettings extends UmaSettings {

    /**
     * Gets the identifier of this issuer.
     *
     * @return The issuer.
     * @throws ServerException If there is a problem reading the configuration.
     */
    URI getIssuer() throws ServerException;

    /**
     * Gets the URI for the OAuth2 token endpoint.
     *
     * @return The OAuth2 token endpoint.
     */
    URI getTokenEndpoint();

    /**
     * Gets the URI for the OAuth2 authorize endpoint.
     *
     * @return The OAuth2 authorize endpoint.
     */
    URI getAuthorizationEndpoint();

    /**
     * Gets the URI for the OAuth2 token introspection endpoint.
     *
     * @return The OAuth2 token introspection endpoint.
     */
    URI getTokenIntrospectionEndpoint();

    /**
     * Gets the URI for the OAuth2 resource set registration endpoint.
     *
     * @return The OAuth2 resource set registration endpoint.
     */
    URI getResourceSetRegistrationEndpoint();

    /**
     * Gets the URI for the UMA permission registration endpoint.
     *
     * @return The UMA permission registration endpoint.
     */
    URI getPermissionRegistrationEndpoint();

    /**
     * Gets the URI for the UMA RPT authorization request endpoint.
     *
     * @return The UMA RPT authorization request  endpoint.
     */
    URI getRPTEndpoint();

    /**
     * Gets the Client registration endpoint.
     *
     * @return The Client registration endpoint.
     */
    URI getDynamicClientEndpoint();

    /**
     * Gets the UMA requesting party claims endpoint.
     *
     * @return The UMA requesting party claims endpoint
     */
    URI getRequestingPartyClaimsEndpoint();

    Evaluator getPolicyEvaluator(Subject subject) throws EntitlementException;

    UmaTokenStore getUmaTokenStore();
}

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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.oidc;

import org.forgerock.jaspi.modules.openid.exceptions.FailedToLoadJWKException;
import org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolver;

import java.net.URL;

/**
 * Interface consumed by the OpenIdConnect authN module. It provides thread-safe access and creation to OpenIdResolver
 * instances, the interface defining the verification of OpenID Connect ID Tokens. Fundamentally, OpenIdResolver instances
 * may need to pull web-based key state to verify ID Tokens, and the latency of this initialization should not be incurred
 * by each instantiation of the OpenIdConnect authN module. This interface defines a concern that ultimately relates to
 * the memoization of OpenIdResolver instances.
 */
public interface OpenIdResolverCache {
    /**
     * @param issuer The issuer (iss claim) for the OpenID Connect ID Token jwt
     * @return The OpenIdResolver instance which can validate jwts issued by the specified issuer. If no issuer has
     * been configured, null will be returned.
     */
    OpenIdResolver getResolverForIssuer(String issuer);

    /**
     * @param  issuer The string corresponding to the issuer. This information is present in the OIDC discovery data, but
     *                because it is possible that the OpenIdResolverCache is called concurrently to create the same OpenIdResolver,
     *                the issuer String will allow the implementation to short-circuit concurrent calls if the desired state
     *                has already been created by a previous call.
     * @param wellKnownProviderUrl The url referencing the json object defining the OIDC discovery data
     *                             (http://openid.net/specs/openid-connect-discovery-1_0-21.html) defining the specifics
     *                             of a particular OIDC Provider
     * @return The OpenIdResolver instantiated with this OIDC discovery data.
     * @throws IllegalStateException if the issuer parameter does not match the discovery document referenced by url.
     *         FailedToLoadJWKException If the jwk descriptor could not be loaded from url referenced in config url
     */
    OpenIdResolver createResolver(String issuer, URL wellKnownProviderUrl) throws IllegalStateException, FailedToLoadJWKException;
}

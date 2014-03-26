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
 * by each instantiation of the OpenIdConnect authN module.
 */
public interface OpenIdResolverCache {
    /**
     * @param cryptoContextDefinitionValue Either the discovery url, jwk url, or client_secret used to configure the authN module.
     *                                     This value is the key into the Map of OpenIdResolver instances.
     * @return The OpenIdResolver instance which can validate jwts issued by the specified issuer. If no issuer has
     * been configured, null will be returned.
     */
    OpenIdResolver getResolverForIssuer(String cryptoContextDefinitionValue);

    /**
     * @param  issuerFromJwk The string corresponding to the issuer. This information is present in the OIDC discovery data, but
     *                it is used to insure that the issuer string configured for the login module, and the issuer string
     *                pulled from the configuration url, match.
     * @param cryptoContextType Identifies the manner in which the crypto context was defined (discovery url, jwk url, or client_secret)
     * @param cryptoContextValue The specific value of the discovery url, jwk url, or client_secret
     * @param cryptoContextValueUrl If the cryptoContextType corresponds to the discovery or jwk url, the URL format of the
     *                              string. Passed so that the implementation does not need to handle the MalformedURLException
     * @return The OpenIdResolver instantiated with this OIDC discovery data.
     * @throws IllegalStateException if the issuer parameter does not match the discovery document referenced by the discovery url.
     *         FailedToLoadJWKException If the jwk descriptor could not be loaded from url referenced by the discovery or jwk url
     *         IllegalArgumentException if the cryptoContextType specification is unknown
     */
    OpenIdResolver createResolver(String issuerFromJwk, String cryptoContextType, String cryptoContextValue, URL cryptoContextValueUrl)
            throws FailedToLoadJWKException;
}

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
import org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolverFactory;

import javax.inject.Inject;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @see org.forgerock.openam.authentication.modules.oidc.OpenIdResolverCache
 */
public class OpenIdResolverCacheImpl implements OpenIdResolverCache {
    private final OpenIdResolverFactory openIdResolverFactory;
    private final ConcurrentHashMap<String, OpenIdResolver> resolverMap;

    @Inject
    OpenIdResolverCacheImpl(OpenIdResolverFactory openIdResolverFactory) {
        this.openIdResolverFactory = openIdResolverFactory;
        resolverMap = new ConcurrentHashMap<String, OpenIdResolver>();
    }

    public OpenIdResolver getResolverForIssuer(String issuer) {
        return resolverMap.get(issuer);
    }

    /*
    It is possible that two callers are calling this method at once. I want to leverage the uncontested reads
    of the ConcurrentHashMap, and I don't want to synchronize the writes to the ConcurrentHashMap above the
    synchronization applied by the CHM in puts. The drawback of this approach is the possible redundant creation of
    a OpenIdResolver if two concurrent calls target the currently-uncreated OpenIdResolver.
     */
    public OpenIdResolver createResolver(String issuer, URL wellKnownProviderUrl) throws IllegalStateException, FailedToLoadJWKException {
        OpenIdResolver newResolver = openIdResolverFactory.createFromOpenIDConfigUrl(wellKnownProviderUrl);
        if (!issuer.equals(newResolver.getIssuer())) {
            throw new IllegalStateException("The specified issuer, " + issuer + ", does not match the issuer, "
                    + newResolver.getIssuer() + " referenced by the configuration url, " + wellKnownProviderUrl.toString());
        }
        OpenIdResolver oldResolver = null;
        if ((oldResolver = resolverMap.putIfAbsent(issuer, newResolver)) != null) {
            return oldResolver;
        }
        return newResolver;
    }
}

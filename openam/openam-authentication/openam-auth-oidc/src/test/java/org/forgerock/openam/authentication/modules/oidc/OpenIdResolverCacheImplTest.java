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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import org.forgerock.jaspi.modules.openid.exceptions.FailedToLoadJWKException;
import org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolverFactory;
import org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolver;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.testng.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;



public class OpenIdResolverCacheImplTest {
    private static final String FAUX_CONIFIG_URL_STRING = "https://host.com/.well-known/openid-configuration";
    private static final String FAUX_JWK_URL_STRING = "https://host.com/my/jwk";
    private static final String FAUX_CLIENT_SECRET = "shhh";
    private static final String FAUX_ISSUER = "accounts.somecompany.com";
    private OpenIdResolverCache cache;
    private OpenIdResolverFactory factory;
    private OpenIdResolver configResolver;
    private OpenIdResolver jwkResolver;
    private OpenIdResolver clientSecretResolver;

    class MyModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(OpenIdResolverFactory.class).toInstance(factory);
            bind(OpenIdResolverCache.class).to(OpenIdResolverCacheImpl.class);
        }
    }

    @BeforeTest
    public void initialize() throws FailedToLoadJWKException {
        configResolver = mock(OpenIdResolver.class);
        when(configResolver.getIssuer()).thenReturn(FAUX_ISSUER);
        jwkResolver = mock(OpenIdResolver.class);
        when(jwkResolver.getIssuer()).thenReturn(FAUX_ISSUER);
        clientSecretResolver = mock(OpenIdResolver.class);
        when(clientSecretResolver.getIssuer()).thenReturn(FAUX_ISSUER);
        factory = mock(OpenIdResolverFactory.class);
        when(factory.createFromOpenIDConfigUrl(any(URL.class))).thenReturn(configResolver);
        when(factory.createJWKResolver(any(String.class), any(URL.class), anyInt(), anyInt())).thenReturn(jwkResolver);
        when(factory.createSharedSecretResolver(any(String.class), any(String.class))).thenReturn(clientSecretResolver);

        cache = Guice.createInjector(new MyModule()).getInstance(OpenIdResolverCache.class);
    }

    @Test
    public void testBasicCreation() throws MalformedURLException, FailedToLoadJWKException {
        OpenIdResolver localConfigResolver = createConfigResolver();
        assertTrue(localConfigResolver == configResolver);

        OpenIdResolver localJwkResolver = createJwtResolver();
        assertTrue(localJwkResolver == jwkResolver);

        OpenIdResolver localClientSecretResolver =  createSecretResolver();
        assertTrue(localClientSecretResolver == clientSecretResolver);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void createInvalidResolver() throws MalformedURLException, FailedToLoadJWKException {
        cache.createResolver("issuer_string_that_does_not_match_resolver", OpenIdConnectConfig.CRYPTO_CONTEXT_TYPE_CONFIG_URL,
                FAUX_CONIFIG_URL_STRING, new URL(FAUX_CONIFIG_URL_STRING));

    }

    @Test
    public void testBasicLookup() throws MalformedURLException, FailedToLoadJWKException {
        createConfigResolver();
        createJwtResolver();
        createSecretResolver();
        OpenIdResolver localConfigResolver =
                cache.getResolverForIssuer(FAUX_CONIFIG_URL_STRING);
        assertTrue(localConfigResolver == configResolver);

        OpenIdResolver localJwkResolver =
                cache.getResolverForIssuer(FAUX_JWK_URL_STRING);
        assertTrue(localJwkResolver == jwkResolver);

        OpenIdResolver localClientSecretResolver =
                cache.getResolverForIssuer(FAUX_CLIENT_SECRET);
        assertTrue(localClientSecretResolver == clientSecretResolver);

    }

    private OpenIdResolver createConfigResolver() throws MalformedURLException, FailedToLoadJWKException {
        return cache.createResolver(FAUX_ISSUER, OpenIdConnectConfig.CRYPTO_CONTEXT_TYPE_CONFIG_URL, FAUX_CONIFIG_URL_STRING,
                new URL(FAUX_CONIFIG_URL_STRING));
    }

    private OpenIdResolver createJwtResolver() throws MalformedURLException, FailedToLoadJWKException {
        return cache.createResolver(FAUX_ISSUER, OpenIdConnectConfig.CRYPTO_CONTEXT_TYPE_JWK_URL, FAUX_JWK_URL_STRING,
                new URL(FAUX_JWK_URL_STRING));

    }
    private OpenIdResolver createSecretResolver() throws FailedToLoadJWKException {
        return cache.createResolver(FAUX_ISSUER, OpenIdConnectConfig.CRYPTO_CONTEXT_TYPE_CLIENT_SECRET, FAUX_CLIENT_SECRET,
                null);
    }

}
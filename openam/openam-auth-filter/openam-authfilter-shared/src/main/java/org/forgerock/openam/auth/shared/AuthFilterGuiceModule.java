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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.openam.auth.shared;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOTokenManager;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.oauth2.OpenAMTokenStore;
import org.forgerock.openam.utils.Config;

/**
 * Responsible for defining the mappings needed by the OpenAM Auth Filter module.
 */
@GuiceModule
public class AuthFilterGuiceModule extends AbstractModule {

    private static final String SSO_TOKEN_COOKIE_NAME_PROPERTY = "com.iplanet.am.cookie.name";

    @Override
    protected void configure() {
        bind(String.class)
                .annotatedWith(Names.named(AuthnRequestUtils.SSOTOKEN_COOKIE_NAME))
                .toProvider(new Provider<String>() {
                    public String get() {
                        return SystemProperties.get(SSO_TOKEN_COOKIE_NAME_PROPERTY);
                    }
                });
        bind(new TypeLiteral<Config<String>>() {})
                .annotatedWith(Names.named(AuthnRequestUtils.ASYNC_SSOTOKEN_COOKIE_NAME))
                .toInstance(new Config<String>() {

            public boolean isReady() {
                return SystemProperties.get(SSO_TOKEN_COOKIE_NAME_PROPERTY) != null;
            }

            public String get() {
                return SystemProperties.get(SSO_TOKEN_COOKIE_NAME_PROPERTY);
            }
        });
        bind(SSOTokenManager.class)
                 .toProvider(new Provider<SSOTokenManager>() {
            public SSOTokenManager get() {
                try {
                    return SSOTokenManager.getInstance();
                } catch (SSOException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
        bind(new TypeLiteral<Config<TokenStore>>() {}).toInstance(new Config<TokenStore>() {
            public boolean isReady() {
                return true;
            }

            public TokenStore get() {
                return InjectorHolder.getInstance(OpenAMTokenStore.class);
            }
        });
    }
}

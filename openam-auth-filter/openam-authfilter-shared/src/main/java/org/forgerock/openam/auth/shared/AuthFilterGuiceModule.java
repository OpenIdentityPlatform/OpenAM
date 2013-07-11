/**
 * Copyright 2013 ForgeRock, Inc.
 *
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
 */
package org.forgerock.openam.auth.shared;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.name.Names;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOTokenManager;
import org.forgerock.openam.guice.AMGuiceModule;

/**
 * Responsible for defining the mappings needed by the OpenAM Auth Filter module.
 *
 * @author robert.wapshott@forgerock.com
 */
@AMGuiceModule
public class AuthFilterGuiceModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(String.class)
                .annotatedWith(Names.named(AuthnRequestUtils.SSOTOKEN_COOKIE_NAME))
                .toProvider(new Provider<String>() {
                    public String get() {
                        return SystemProperties.get("com.iplanet.am.cookie.name");
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
    }
}

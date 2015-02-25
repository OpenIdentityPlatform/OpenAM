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

import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.util.Reject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class JwtHandlerConfig {
    private static Debug logger = Debug.getInstance("amAuth");

    static final String ISSUER_NAME_KEY = "openam-auth-openidconnect-issuer-name";
    static final String CRYPTO_CONTEXT_TYPE_KEY = "openam-auth-openidconnect-crypto-context-type";
    static final String CRYPTO_CONTEXT_VALUE_KEY = "openam-auth-openidconnect-crypto-context-value";

    static final String CRYPTO_CONTEXT_TYPE_CONFIG_URL = ".well-known/openid-configuration_url";
    static final String CRYPTO_CONTEXT_TYPE_JWK_URL = "jwk_url";
    static final String CRYPTO_CONTEXT_TYPE_CLIENT_SECRET = "client_secret";

    protected final String configuredIssuer;
    protected final String cryptoContextType;
    protected final String cryptoContextValue;
    private final URL cryptoContextUrlValue;

    public JwtHandlerConfig(Map options) {
        configuredIssuer = CollectionHelper.getMapAttr(options, ISSUER_NAME_KEY);
        cryptoContextType = CollectionHelper.getMapAttr(options, CRYPTO_CONTEXT_TYPE_KEY);
        cryptoContextValue = CollectionHelper.getMapAttr(options, CRYPTO_CONTEXT_VALUE_KEY);
        Reject.ifFalse(cryptoContextType == null || CRYPTO_CONTEXT_TYPE_CLIENT_SECRET.equals(cryptoContextType) ||
                CRYPTO_CONTEXT_TYPE_JWK_URL.equals(cryptoContextType) ||
                CRYPTO_CONTEXT_TYPE_CONFIG_URL.equals(cryptoContextType), "The value corresponding to key " +
                CRYPTO_CONTEXT_TYPE_KEY + " does not correspond to an expected value. Its value:" + cryptoContextType);
        if (CRYPTO_CONTEXT_TYPE_CONFIG_URL.equals(cryptoContextType) || CRYPTO_CONTEXT_TYPE_JWK_URL.equals(cryptoContextType)) {
            try {
                cryptoContextUrlValue = new URL(cryptoContextValue);
            } catch (MalformedURLException e) {
                final String message = "The crypto context value string, " + cryptoContextValue + " is not in valid URL format: " + e;
                logger.error(message, e);
                throw new IllegalArgumentException(message);
            }
        } else {
            cryptoContextUrlValue = null;
        }
    }

    public String getConfiguredIssuer() {
        return configuredIssuer;
    }

    public String getCryptoContextType() {
        return cryptoContextType;
    }

    public String getCryptoContextValue() {
        return cryptoContextValue;
    }

    public URL getCryptoContextUrlValue() {
        return cryptoContextUrlValue;
    }

}

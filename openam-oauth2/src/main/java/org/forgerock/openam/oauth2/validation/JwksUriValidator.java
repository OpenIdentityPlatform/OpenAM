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
 * Copyright 2026 3A Systems, LLC.
 */
package org.forgerock.openam.oauth2.validation;

import java.net.MalformedURLException;
import java.net.URL;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.validation.ValidationException;
import com.sun.identity.shared.validation.ValidatorBase;

/**
 * Validates that a client-supplied {@code jwks_uri} is safe for the server to fetch, to prevent
 * server-side request forgery (SSRF) — GHSA-g7cv-hh35-cc7c.
 *
 * <p>A {@code jwks_uri} is accepted only when both of the following hold:
 *
 * <ul>
 *   <li>the scheme is {@code http} or {@code https}. The JWK Set is fetched through
 *       {@link java.net.URLConnection}, which also speaks {@code file}, {@code ftp} and
 *       {@code jar}, so without this check a registered {@code file:///etc/passwd} would be read
 *       off the server's own disk;</li>
 *   <li>none of the host's resolved addresses is loopback, wildcard, link-local (incl. the cloud
 *       metadata endpoints), private/site-local, multicast or otherwise special-purpose, as
 *       decided by {@link com.sun.identity.common.SsrfUrlValidator}.</li>
 * </ul>
 *
 * <p>Plain {@code http} is tolerated here, unlike in
 * {@link org.forgerock.openam.oauth2.validation.SsrfUrlValidator}, the guard for
 * {@code sector_identifier_uri} (written out in full: two classes on this module's build path
 * share that simple name). The OpenID Connect specification does not mandate TLS for
 * {@code jwks_uri} the way it does for {@code sector_identifier_uri}, and requiring it would break
 * working registrations without closing the SSRF hole that this class exists to close.
 *
 * <p>The address check — but never the scheme check — can be lifted at runtime, without a
 * rebuild, by setting {@link #ALLOW_ANY_ADDRESS_PROPERTY} to {@code true}. That is the escape
 * hatch for the deployment whose relying parties genuinely publish their JWK Sets on an internal
 * address; it deliberately does not re-enable {@code file://} and friends.
 *
 * <p>Two limits are worth knowing about, both shared with the other SSRF guards in OpenAM. The
 * host is resolved here and resolved again when the connection is opened, so a hostile DNS server
 * can answer differently the second time (DNS rebinding); closing that window needs the connection
 * pinned to the address that was validated, which {@code HttpURLConnection} does not offer. And if
 * the OpenAM JVM is configured with an HTTP proxy, the proxy — not this code — resolves the host,
 * so the address check says nothing about where the request actually lands.
 */
public final class JwksUriValidator extends ValidatorBase {

    private static final JwksUriValidator instance = new JwksUriValidator();

    /**
     * Key in the {@code amValidation} resource bundle for the "unsafe jwks_uri" error message.
     */
    public static final String ERROR_CODE = "errorCode7";

    /**
     * When {@code true}, a {@code jwks_uri} may resolve to a loopback, private or link-local
     * address. The {@code http}/{@code https} scheme allowlist still applies. Default
     * {@code false}.
     */
    public static final String ALLOW_ANY_ADDRESS_PROPERTY =
            "org.openidentityplatform.oauth2.oidc.jwks-uri.allow-any-address";

    private JwksUriValidator() {
    }

    /**
     * Returns an instance of this validator.
     */
    public static JwksUriValidator getInstance() {
        return instance;
    }

    /**
     * {@inheritDoc}
     *
     * @param url the client-supplied {@code jwks_uri}.
     * @throws ValidationException if the URL is not safe for the server to fetch.
     */
    @Override
    protected void performValidation(String url) throws ValidationException {
        if (!isSafe(url)) {
            throw new ValidationException(resourceBundleName, ERROR_CODE);
        }
    }

    /**
     * The same decision as {@link #validate(String)}, reported as a boolean.
     *
     * <p>Building a {@link ValidationException} resolves a message out of the {@code amValidation}
     * bundle, which throws an unchecked exception of its own if the bundle and the code ever drift
     * apart. Callers that fetch the URL — rather than answer a registration request — want the
     * verdict without that, so they use this method.
     *
     * @param url the {@code jwks_uri} to check.
     * @return {@code true} if the URL is safe for the server to fetch.
     */
    public static boolean isSafe(String url) {
        return isAllowedScheme(url)
                && (SystemProperties.getAsBoolean(ALLOW_ANY_ADDRESS_PROPERTY, false)
                        || com.sun.identity.common.SsrfUrlValidator.isSafeRemoteUrl(url, false));
    }

    private static boolean isAllowedScheme(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        final String protocol;
        try {
            protocol = new URL(url).getProtocol();
        } catch (MalformedURLException e) {
            return false;
        }
        return "https".equalsIgnoreCase(protocol) || "http".equalsIgnoreCase(protocol);
    }
}

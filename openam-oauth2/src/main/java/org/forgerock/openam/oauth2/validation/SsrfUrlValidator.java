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
 * Copyright 2026 3A Systems LLC.
 */
package org.forgerock.openam.oauth2.validation;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.validation.ValidatorBase;
import com.sun.identity.shared.validation.ValidationException;

/**
 * Validates that a client-supplied URL is safe for the server to fetch, to prevent
 * server-side request forgery (SSRF) — GHSA-7c7p-4mff-c9vg.
 *
 * <p>A URL is considered safe only when it uses the {@code https} scheme (which the OpenID
 * Connect specification already mandates for {@code sector_identifier_uri}, and which also
 * rules out {@code file://}, {@code http://}, {@code ftp://}, {@code gopher://} SSRF variants)
 * and none of the host's resolved addresses point at a loopback, wildcard, link-local
 * (incl. cloud metadata {@code 169.254.0.0/16}), private/site-local, multicast, IPv6 unique-local
 * or otherwise special-purpose address — including one reachable only through the IPv4 address
 * embedded in an IPv6 transition literal (NAT64, 6to4, Teredo, ISATAP, IPv4-mapped). The scheme
 * and address checks are delegated to {@link com.sun.identity.common.SsrfUrlValidator}, the single
 * shared implementation, whose javadoc lists the blocked set in full; this class only adds the
 * {@code https}-only requirement and the runtime escape hatch below.
 *
 * <p>The check can be disabled at runtime, without a rebuild, by setting the system property
 * {@link #ALLOW_ANY_URL_PROPERTY} to {@code true} (for the atypical case of a relying party
 * whose file is hosted on an internal address).
 */
public final class SsrfUrlValidator extends ValidatorBase {

    private static final SsrfUrlValidator instance = new SsrfUrlValidator();

    /**
     * Key in the {@code amValidation} resource bundle for the "unsafe URL" error message.
     */
    public static final String ERROR_CODE = "errorCode6";

    private SsrfUrlValidator() {
    }

    /**
     * Returns an instance of this validator.
     */
    public static SsrfUrlValidator getInstance() {
        return instance;
    }

    /**
     * When {@code true}, {@link #performValidation(String)} accepts any URL (restores the
     * pre-fix behaviour). Default {@code false}.
     */
    public static final String ALLOW_ANY_URL_PROPERTY =
            "org.openidentityplatform.oauth2.oidc.sector-identifier-uri.allow-any-url";

    /**
     * {@inheritDoc}
     *
     * @param url the client-supplied URL.
     * @throws ValidationException if the URL is not safe for the server to fetch.
     */
    @Override
    protected void performValidation(String url) throws ValidationException {
        if (!isSafeRemoteUrl(url)) {
            throw new ValidationException(resourceBundleName, ERROR_CODE);
        }
    }

    private boolean isSafeRemoteUrl(String url) {
        return SystemProperties.getAsBoolean(ALLOW_ANY_URL_PROPERTY, false)
                // https only; scheme, host resolution and address blocking live in the shared class.
                || com.sun.identity.common.SsrfUrlValidator.isSafeRemoteUrl(url, true);
    }
}

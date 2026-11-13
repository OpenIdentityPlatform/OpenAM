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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

import static org.forgerock.oauth2.core.Utils.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.forgerock.http.MutableUri;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;

/**
 * Validates that the redirect uri on the request matches against the client's registered redirect uris.
 *
 * @since 12.0.0
 */
public class RedirectUriValidator {

    /**
     * Validates that the requested redirect uri matches against one of the pre-registered redirect uris on the
     * client's registration.
     *
     * @param clientRegistration The client's registration.
     * @param redirectUri The redirect uri.
     * @throws InvalidRequestException If the request is missing any required parameters or is otherwise malformed.
     * @throws RedirectUriMismatchException If the redirect uri on the request does not match the redirect uri
     *          registered for the client.
     */
    public void validate(ClientRegistration clientRegistration, String redirectUri) throws InvalidRequestException,
            RedirectUriMismatchException {

        if (isEmpty(redirectUri)) {
            // A loopback port pattern is not a usable destination, so it cannot stand in for redirect_uri.
            if (clientRegistration.getRedirectUris().size() == 1
                    && loopbackPatternHost(clientRegistration.getRedirectUris().iterator().next()) == null) {
                return;
            }

            throw new InvalidRequestException("Missing parameter: redirect_uri");
        }

        MutableUri mutableRequest = null;
        try {
	        mutableRequest = MutableUri.uri(redirectUri);
	        if(mutableRequest.getRawQuery() != null) {
	        	mutableRequest.setQuery(null);
	        }
	        if(mutableRequest.getRawFragment() != null) {
	        	mutableRequest.setRawFragment(null);
	        }
        }catch(URISyntaxException e) {
        	 throw new InvalidRequestException("Invalid parameter: redirect_uri");
        }
        final URI request = mutableRequest.asURI();

        if (request.getFragment() != null) {
            throw new RedirectUriMismatchException();
        }
        if (!request.isAbsolute()) {
            throw new RedirectUriMismatchException();
        }
        for (URI uri : clientRegistration.getRedirectUris()) {
            if (uri.equals(request) || isLoopbackMatch(uri, request)) {
                //GOOD CASE
                return;
            }
        }
        throw new RedirectUriMismatchException();
    }

    /**
     * Matches loopback redirect uris for native apps (RFC 8252 section 7.3), which receive the authorization
     * response on an ephemeral port of the loopback interface. Scheme, host and path must match the
     * registered uri; only the port is flexible. As for every redirect uri, the query of the request is not
     * compared. A registered {@code http} redirect uri on {@code localhost},
     * {@code 127.0.0.1} or {@code [::1]} accepts:
     * <ul>
     *     <li>any port when it has no port, e.g. {@code http://127.0.0.1/callback} (RFC 8252 section 7.3);</li>
     *     <li>any port when its port is {@code *}, e.g. {@code http://localhost:*}{@code /callback};</li>
     *     <li>a port within a range, e.g. {@code http://localhost:49152-65535/callback}.</li>
     * </ul>
     * Port patterns are only supported on {@code localhost} and {@code 127.0.0.1}, as {@link URI} cannot parse
     * them on an IPv6 literal. All other redirect uris, including loopback ones with a single explicit port,
     * keep exact matching.
     *
     * @param registered A redirect uri registered for the client.
     * @param request The redirect uri on the request.
     * @return {@code true} if the request is allowed by the registered loopback redirect uri.
     */
    static boolean isLoopbackMatch(URI registered, URI request) {
        if (!"http".equalsIgnoreCase(request.getScheme()) || request.getRawUserInfo() != null
                || request.getHost() == null || !isLoopbackHost(request.getHost())
                || !"http".equalsIgnoreCase(registered.getScheme())
                || !Objects.equals(pathOf(registered), pathOf(request))
                || !Objects.equals(registered.getRawQuery(), request.getRawQuery())) {
            return false;
        }
        final int requestPort = request.getPort() == -1 ? 80 : request.getPort();
        if (registered.getHost() != null) {
            return registered.getPort() == -1 && registered.getRawUserInfo() == null
                    && registered.getHost().equalsIgnoreCase(request.getHost());
        }
        final Matcher pattern = LOOPBACK_PORT_PATTERN.matcher(String.valueOf(registered.getRawAuthority()));
        if (!pattern.matches() || !pattern.group(1).equalsIgnoreCase(request.getHost())) {
            return false;
        }
        if (pattern.group(3) == null) {
            return true;
        }
        try {
            final int low = Integer.parseInt(pattern.group(3));
            final int high = Integer.parseInt(pattern.group(4));
            return low <= requestPort && requestPort <= high;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Returns the host of a loopback redirect uri registered with a port pattern, e.g. {@code localhost} for
     * {@code http://localhost:*}{@code /callback}, for which {@link URI#getHost()} is {@code null}.
     *
     * @param registered A redirect uri registered for the client.
     * @return The host, or {@code null} if the uri is not a loopback port pattern.
     */
    public static String loopbackPatternHost(URI registered) {
        final Matcher pattern = LOOPBACK_PORT_PATTERN.matcher(String.valueOf(registered.getRawAuthority()));
        return pattern.matches() ? pattern.group(1) : null;
    }

    private static boolean isLoopbackHost(String host) {
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "[::1]".equals(host);
    }

    private static String pathOf(URI uri) {
        return isEmpty(uri.getRawPath()) ? "/" : uri.getRawPath();
    }

    /** {@code localhost:*} or {@code 127.0.0.1:low-high}; groups: host, port pattern, low, high. */
    private static final Pattern LOOPBACK_PORT_PATTERN =
            Pattern.compile("(?i)(localhost|127\\.0\\.0\\.1):(\\*|(\\d{1,5})-(\\d{1,5}))");
}

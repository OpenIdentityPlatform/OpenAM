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
package org.forgerock.openam.shared.security.whitelist;

import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.regex.Pattern;
import org.forgerock.openam.utils.StringUtils;

/**
 * Validates redirect URLs that arrive as request parameters on endpoints which have no whitelist of their own.
 *
 * <p>{@link RedirectUrlValidator} considers every URL valid when the configuration holds no patterns to match
 * against, which makes it fail open on a default installation. This validator falls back to the origin of this
 * server instead of allowing everything:
 *
 * <ul>
 *     <li>relative URLs are always accepted, they can not leave this server;</li>
 *     <li>only <code>http</code> and <code>https</code> URLs are considered at all;</li>
 *     <li>a URL pointing at this deployment - the same scheme, host and port as either the incoming request or
 *     the configured server URL, and a path inside the context this server is deployed under - is accepted
 *     whether or not a whitelist exists, since it can not be an open redirect;</li>
 *     <li>any other URL has to match the configured patterns, and is refused when there are none.</li>
 * </ul>
 *
 * @param <T> The type identifying the configuration the whitelist is read from.
 */
public class FailSafeRedirectUrlValidator<T> {

    private static final Debug DEBUG = Debug.getInstance("patternMatching");

    /**
     * An ASCII host and an optional numeric port, nothing percent encoded: the only authority left worth reading
     * once <code>java.net.URI</code> has declined to parse one as server based.
     */
    private static final Pattern BARE_AUTHORITY = Pattern.compile("[A-Za-z0-9._-]+(?::[0-9]{1,5})?");

    private final ValidDomainExtractor<T> domainExtractor;

    /**
     * Creates a validator reading its whitelist through the provided extractor.
     *
     * @param domainExtractor The extractor of the configured URL patterns.
     */
    public FailSafeRedirectUrlValidator(final ValidDomainExtractor<T> domainExtractor) {
        this.domainExtractor = domainExtractor;
    }

    /**
     * Tells whether it is safe to redirect to the given URL.
     *
     * @param request The request being processed, used to establish the origin of this server.
     * @param url The URL to validate.
     * @param configInfo Identifies the configuration holding the whitelist, may be <code>null</code>.
     * @return <code>true</code> if the URL may be used in a redirect.
     */
    public boolean isRedirectUrlValid(final HttpServletRequest request, final String url, final T configInfo) {
        return isRedirectUrlValid(request, url, configInfo == null
                ? Collections.<T>emptyList() : Collections.singletonList(configInfo));
    }

    /**
     * Tells whether it is safe to redirect to the given URL, considering several configurations.
     *
     * <p>The whitelists of all the provided configurations are honoured together. This suits endpoints that can be
     * reached for more than one role, where the caller can not tell up front which one holds the configuration.
     *
     * @param request The request being processed, used to establish the origin of this server.
     * @param url The URL to validate.
     * @param configInfos Identifies the configurations holding the whitelist, may be empty.
     * @return <code>true</code> if the URL may be used in a redirect.
     */
    public boolean isRedirectUrlValid(final HttpServletRequest request, final String url,
            final Collection<T> configInfos) {
        if (StringUtils.isEmpty(url) || !RedirectUrlValidator.isWithinLengthLimit(url)) {
            return false;
        }

        final URI uri = RedirectUrlValidator.parseRedirectUrl(url);
        if (uri == null) {
            return false;
        }

        // A relative URL can only ever point back at this server.
        if (RedirectUrlValidator.isRelative(url, uri)) {
            return true;
        }

        if (!RedirectUrlValidator.hasRedirectableScheme(uri)) {
            return false;
        }

        // A URL pointing back at this deployment can not be an open redirect, so it needs no whitelist. Checking
        // it first also keeps this server's own endpoints reachable for a deployment whose whitelist names only
        // the partners it federates with, which is the usual reason to configure one.
        if (isSameOrigin(request, uri) && isWithinDeploymentUri(request, uri)) {
            return true;
        }

        // Read the whitelist once: reading it again to hand it to another validator would let a transient
        // configuration error between the two reads turn a configured whitelist into permission for anything.
        final Collection<String> patterns = extractPatterns(configInfos);
        if (patterns.isEmpty()) {
            DEBUG.message("FailSafeRedirectUrlValidator: no URL patterns are configured, "
                    + "so the URL {} is only accepted when it points back at this server", url);
            return false;
        }

        DEBUG.message("FailSafeRedirectUrlValidator: validating the URL {} against patterns: {}", url, patterns);
        return RedirectUrlValidator.matchesPatterns(url, patterns);
    }

    /**
     * Collects the patterns of every provided configuration into a single whitelist.
     *
     * @param configInfos Identifies the configurations holding the whitelist, may be <code>null</code> or empty.
     * @return The configured patterns, never <code>null</code>.
     */
    private Collection<String> extractPatterns(final Collection<T> configInfos) {
        if (configInfos == null || configInfos.isEmpty()) {
            return Collections.emptySet();
        }
        final Collection<String> patterns = new LinkedHashSet<String>();
        for (final T configInfo : configInfos) {
            if (configInfo == null) {
                continue;
            }
            final Collection<String> configured = domainExtractor.extractValidDomains(configInfo);
            if (configured != null) {
                patterns.addAll(configured);
            }
        }
        return patterns;
    }

    /**
     * Tells whether an absolute or scheme relative URL points at this server.
     *
     * @param request The request being processed, may be <code>null</code>.
     * @param uri The URL to compare against this server.
     * @return <code>true</code> if scheme, host and port all match the request or the configured server URL.
     */
    private boolean isSameOrigin(final HttpServletRequest request, final URI uri) {
        // This server never names userinfo in its own URLs, and carrying the userinfo of
        // "https://evil.tld@openam.example.com/" into the Location header is nothing this needs to allow.
        if (uri.getUserInfo() != null) {
            return false;
        }

        String host = uri.getHost();
        int port = uri.getPort();

        if (host == null) {
            // getHost() is null for an authority java.net.URI will not parse as server based - a hostname holding
            // an underscore, for one - where the authority still carries it. Falling back keeps this server's own
            // URLs recognisable on such a deployment.
            final String authority = getBareAuthority(uri);
            if (authority == null) {
                return false;
            }
            final int colon = authority.indexOf(':');
            if (colon < 0) {
                host = authority;
            } else {
                host = authority.substring(0, colon);
                port = parsePort(authority.substring(colon + 1));
                if (port == -1) {
                    return false;
                }
            }
        }

        final String scheme = uri.getScheme();
        if (request != null && matchesOrigin(scheme, host, port,
                request.getScheme(), request.getServerName(), request.getServerPort())) {
            return true;
        }

        // Behind a TLS terminating proxy the container sees the internal scheme and port, while this server's own
        // URLs carry the ones it is configured with, so the configured origin has to count as well.
        final String serverProtocol = getServerProperty(Constants.AM_SERVER_PROTOCOL);
        final String serverHost = getServerProperty(Constants.AM_SERVER_HOST);
        final int serverPort = parsePort(getServerProperty(Constants.AM_SERVER_PORT));
        if (StringUtils.isNotEmpty(serverProtocol) && StringUtils.isNotEmpty(serverHost) && serverPort != -1) {
            return matchesOrigin(scheme, host, port, serverProtocol, serverHost, serverPort);
        }
        return false;
    }

    /**
     * Reads the authority of a URL whose host <code>java.net.URI</code> did not parse, where it is a bare host.
     *
     * <p>The raw authority is read, not the decoded one: <code>getAuthority()</code> percent decodes, so
     * <code>https://v%C4%B1ct%C4%B1m.com/</code> would read as a host spelt with the dotless i, U+0131, which a
     * browser resolves to <code>xn--vctm-lzac.com</code> and which a case insensitive comparison still takes for
     * <code>victim.com</code>. Anything but an ASCII host and an optional numeric port is refused, which also
     * refuses userinfo: <code>getUserInfo()</code> is null too when the authority did not parse, so the userinfo of
     * <code>openam@attacker.example.com</code> would otherwise read as the host.
     *
     * @param uri The URL, whose <code>getHost()</code> is <code>null</code>.
     * @return The authority as <code>host</code> or <code>host:port</code>, or <code>null</code> when the URL
     * names none or names one that is not a bare host.
     */
    public static String getBareAuthority(final URI uri) {
        final String authority = uri.getRawAuthority();
        return authority != null && BARE_AUTHORITY.matcher(authority).matches() ? authority : null;
    }

    /**
     * Tells whether the path of a URL lies inside the deployment of this server.
     *
     * <p>The origin on its own is not enough. A whitelist narrows by path as well as by host, and
     * <code>RedirectUrlValidator</code> matches every absolute URL against it, so accepting this server's whole
     * host would widen a configured <code>https://openam.example.com/openam/*</code> back out to every application
     * sharing the host. Only the URLs of this deployment are accepted without a whitelist; anything else on the
     * same host is left to the configured patterns.
     *
     * <p>This bounds the absolute form only. A relative URL is accepted whatever its path, here as in
     * {@link RedirectUrlValidator}, and the container resolves it against the origin of the request, so a
     * whitelist has never narrowed by path the one way a caller can always reach.
     *
     * @param request The request being processed, may be <code>null</code>.
     * @param uri The URL, already known to point at the origin of this server.
     * @return <code>true</code> if the URL belongs to this deployment.
     */
    private boolean isWithinDeploymentUri(final HttpServletRequest request, final URI uri) {
        if (request != null) {
            // An empty context path means this server is the root deployment, where every path of the origin is
            // its own.
            return isWithinDeploymentUri(request.getContextPath(), uri, true);
        }
        // Without a request the configured deployment descriptor is all there is, and where that is not
        // configured either nothing on this host is known to belong to this server, so the whitelist decides.
        return isWithinDeploymentUri(getServerProperty(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR), uri, false);
    }

    /**
     * Tells whether the path of a URL lies inside the named deployment.
     *
     * @param deploymentUri The context this server is deployed under, may be <code>null</code> or empty.
     * @param uri The URL, already known to point at the origin of this server.
     * @param unknownIsRoot What an absent deployment URI means: the root deployment, or an unknown one.
     * @return <code>true</code> if the URL belongs to that deployment.
     */
    private static boolean isWithinDeploymentUri(final String deploymentUri, final URI uri,
            final boolean unknownIsRoot) {
        if (StringUtils.isEmpty(deploymentUri)) {
            return unknownIsRoot;
        }
        if ("/".equals(deploymentUri)) {
            return true;
        }

        // getPath() percent decodes, and URI.normalize() runs before that on the raw path, so neither of them
        // removes a dot segment written as "%2e%2e" or hidden behind the path parameters of "..;" - both of which
        // a servlet container resolves into a climb out of this deployment. A URL of this server has no business
        // holding one, so any dot segment sends the URL to the whitelist rather than through this fallback.
        final String path = uri.getPath();
        if (path == null) {
            return false;
        }
        for (final String rawSegment : path.split("/", -1)) {
            final int parameters = rawSegment.indexOf(';');
            final String segment = parameters < 0 ? rawSegment : rawSegment.substring(0, parameters);
            if (".".equals(segment) || "..".equals(segment)) {
                return false;
            }
        }

        return path.equals(deploymentUri) || path.startsWith(deploymentUri + "/");
    }

    /**
     * Reads a property describing the URL this server is deployed under. Overridable so that the tests can supply
     * an origin without touching the global configuration.
     *
     * @param key The name of the property.
     * @return The configured value, or <code>null</code> when this server is not configured yet.
     */
    protected String getServerProperty(final String key) {
        return SystemPropertiesManager.get(key);
    }

    /**
     * Parses a configured port number.
     *
     * @param port The configured value, may be <code>null</code>.
     * @return The port, or <code>-1</code> when it is missing or not a number.
     */
    private static int parsePort(final String port) {
        if (StringUtils.isEmpty(port)) {
            return -1;
        }
        try {
            return Integer.parseInt(port.trim());
        } catch (final NumberFormatException nfe) {
            DEBUG.warning("FailSafeRedirectUrlValidator: {} is not a valid port number", port);
            return -1;
        }
    }

    /**
     * Compares the scheme, host and port of a URL against one origin of this server.
     *
     * @param urlScheme The scheme of the URL, <code>null</code> for a scheme relative URL.
     * @param host The host of the URL, already known to be non <code>null</code>.
     * @param urlPort The port of the URL, <code>-1</code> when it names none.
     * @param originScheme The scheme of the origin.
     * @param originHost The host of the origin.
     * @param originPort The port of the origin.
     * @return <code>true</code> if the URL points at that origin.
     */
    private boolean matchesOrigin(final String urlScheme, final String host, final int urlPort,
            final String originScheme, final String originHost, final int originPort) {
        // A scheme relative URL inherits the scheme it is resolved against.
        String scheme = urlScheme;
        if (scheme == null) {
            scheme = originScheme;
        }
        // Not equalsIgnoreCase for the host: it folds U+0131 and U+0130 onto "i", which a browser does not.
        if (originScheme == null || originHost == null || scheme == null
                || !scheme.equalsIgnoreCase(originScheme)
                || !host.toLowerCase(Locale.ROOT).equals(originHost.toLowerCase(Locale.ROOT))) {
            return false;
        }

        int port = urlPort;
        if (port == -1) {
            port = "https".equalsIgnoreCase(scheme) ? 443 : 80;
        }
        return port == originPort;
    }
}

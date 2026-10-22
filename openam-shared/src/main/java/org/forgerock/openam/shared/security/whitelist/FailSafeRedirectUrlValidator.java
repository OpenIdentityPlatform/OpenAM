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
import java.net.URISyntaxException;
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
 *     <li>a URL pointing at this deployment - the same scheme, host and port as one of the origins this server
 *     answers on, and a path inside the context this server is deployed under - is accepted whether or not a
 *     whitelist exists, since it can not be an open redirect;</li>
 *     <li>any other URL has to match the configured patterns, and is refused when there are none - unless a
 *     subclass asks for the legacy reading of an unconfigured whitelist through
 *     {@link #allowAnyUrlWithoutWhitelist()};</li>
 *     <li>when a whitelist could not be read at all, only the URLs of this deployment are accepted, whatever a
 *     subclass makes of an unconfigured one.</li>
 * </ul>
 *
 * <p>This server answers on more than one origin, and each of them is accepted, because none is right on its own:
 *
 * <ul>
 *     <li>the <code>com.iplanet.am.server.protocol</code>, <code>com.iplanet.am.server.host</code> and
 *     <code>com.iplanet.am.server.port</code> properties, the same values the federation code builds its own local
 *     URL from. They are the only correct answer behind a TLS terminating proxy, where the servlet container sees
 *     the internal connection while the deployment is configured with the address browsers reach it at;</li>
 *     <li>the origin the servlet container reports. It is the only correct answer on a site installation, where the
 *     properties above describe this instance rather than the load balancer in front of the site, and it is what
 *     OpenAM itself builds the URLs it hands out from - <code>RPSigninRequest</code> mints its <code>wreply</code>
 *     from {@link jakarta.servlet.http.HttpServletRequest#getRequestURL()} and the SAML2 IdP proxy builds its
 *     RelayState from the <code>Host</code> header;</li>
 *     <li>whatever a subclass adds through {@link #getAdditionalOrigins(HttpServletRequest, Collection)} - the
 *     realm's Base URL Source service, for instance.</li>
 * </ul>
 *
 * <p>The container view is derived from the <code>Host</code> header, so accepting it widens what counts as this
 * server to whatever name resolves to it. That is the deployment's FQDN map to control, the same way it controls
 * every other URL OpenAM builds from the header; a redirect lure cannot exploit it, because the header on the
 * victim's request is the one their own browser sends.
 *
 * <p>Host names are compared as they stand, apart from ASCII case, a trailing root label and the brackets of an
 * IPv6 literal: a deployment whose name is an internationalised one has to name itself in
 * <code>com.iplanet.am.server.host</code> in ASCII compatible form, the same the whitelist path expects of it.
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
     * @param request The request being processed, consulted as one of the views of this server's origin, may be
     * <code>null</code>.
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
     * @param request The request being processed, consulted as one of the views of this server's origin, may be
     * <code>null</code>.
     * @param url The URL to validate.
     * @param configInfos Identifies the configurations holding the whitelist, may be empty.
     * @return <code>true</code> if the URL may be used in a redirect.
     */
    public boolean isRedirectUrlValid(final HttpServletRequest request, final String url,
            final Collection<T> configInfos) {
        return isRedirectUrlValid(request, url, configInfos, false);
    }

    /**
     * Tells whether it is safe to redirect to the given URL when the whitelist that governs it could not be
     * consulted at all, because the caller could not work out which configuration holds it.
     *
     * <p>An unreadable whitelist is not an empty one: {@link #allowAnyUrlWithoutWhitelist()} has no say here, and
     * the URL is only accepted when it is relative or points back at this deployment.
     *
     * @param request The request being processed, consulted as one of the views of this server's origin, may be
     * <code>null</code>.
     * @param url The URL to validate.
     * @return <code>true</code> if the URL may be used in a redirect.
     */
    public boolean isOwnOriginRedirectUrl(final HttpServletRequest request, final String url) {
        return isRedirectUrlValid(request, url, Collections.<T>emptyList(), true);
    }

    /**
     * Tells whether it is safe to redirect to the given URL.
     *
     * @param request The request being processed, may be <code>null</code>.
     * @param url The URL to validate.
     * @param configInfos Identifies the configurations holding the whitelist, may be empty.
     * @param whitelistUnreadable <code>true</code> when the caller already knows the whitelist can not be read.
     * @return <code>true</code> if the URL may be used in a redirect.
     */
    private boolean isRedirectUrlValid(final HttpServletRequest request, final String url,
            final Collection<T> configInfos, final boolean whitelistUnreadable) {
        if (StringUtils.isEmpty(url)) {
            DEBUG.message("FailSafeRedirectUrlValidator: no URL to validate");
            return false;
        }

        final int maxUrlLength = getMaxUrlLength();
        if (url.length() > maxUrlLength) {
            DEBUG.message("FailSafeRedirectUrlValidator: the URL was length {} which is longer than the allowed "
                    + "maximum of {}", url.length(), maxUrlLength);
            return false;
        }

        final URI uri;
        try {
            uri = new URI(url);
        } catch (final URISyntaxException urise) {
            // This is the one branch the URL itself stays out of. Every message below it describes a value
            // java.net.URI has already accepted, and URI refuses a raw CR or LF; this one describes precisely
            // a value it could not read, so it is the only one a CRLF can reach, and the plain text debug
            // file is where that forges lines. The exception is not handed over either - its own message
            // quotes the input - so what is left is the reason and the index, which is the part that says
            // what was wrong with it.
            DEBUG.message("FailSafeRedirectUrlValidator: the URL is not a valid URI: {} at index {}",
                    urise.getReason(), urise.getIndex());
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
        if (isSameOrigin(request, uri, configInfos) && isWithinDeploymentUri(request, uri)) {
            return true;
        }

        // Read the whitelist once: reading it again to hand it to another validator would let a transient
        // configuration error between the two reads turn a configured whitelist into permission for anything.
        final Collection<String> patterns = new LinkedHashSet<String>();
        final boolean unreadable = !readPatterns(configInfos, patterns) || whitelistUnreadable;
        if (!patterns.isEmpty()) {
            DEBUG.message("FailSafeRedirectUrlValidator: validating the URL {} against patterns: {}", url, patterns);
            return RedirectUrlValidator.matchesPatterns(url, patterns);
        }

        if (unreadable) {
            DEBUG.warning("FailSafeRedirectUrlValidator: the URL patterns could not be read, so the URL {} is "
                    + "only accepted when it points back at this server", url);
            return false;
        }

        if (allowAnyUrlWithoutWhitelist()) {
            DEBUG.message("FailSafeRedirectUrlValidator: no URL patterns are configured and the legacy behaviour "
                    + "was asked for, so the URL {} is accepted as it is", url);
            return true;
        }

        DEBUG.message("FailSafeRedirectUrlValidator: no URL patterns are configured, "
                + "so the URL {} is only accepted when it points back at this server", url);
        return false;
    }

    /**
     * Collects the patterns of every provided configuration into a single whitelist.
     *
     * <p>Every configuration is read exactly once. A configuration whose whitelist can not be read - the extractor
     * answers <code>null</code> or throws - contributes nothing: the remaining ones still decide. That it could
     * not be read is reported back, because an unreadable whitelist is not an empty one: reading it as one would
     * hand {@link #allowAnyUrlWithoutWhitelist()} to a deployment that did populate its list.
     *
     * @param configInfos Identifies the configurations holding the whitelist, may be <code>null</code> or empty.
     * @param patterns Receives the configured patterns.
     * @return <code>true</code> if every configuration could be read.
     */
    private boolean readPatterns(final Collection<T> configInfos, final Collection<String> patterns) {
        if (configInfos == null || configInfos.isEmpty()) {
            return true;
        }
        boolean readable = true;
        for (final T configInfo : configInfos) {
            if (configInfo == null) {
                continue;
            }
            try {
                final Collection<String> configured = domainExtractor.extractValidDomains(configInfo);
                if (configured == null) {
                    // The extractor could not read the configuration, rather than read it and find no list in it:
                    // the store is unreachable, or the role the list hangs off could not be resolved.
                    readable = false;
                } else {
                    patterns.addAll(configured);
                }
            } catch (final RuntimeException re) {
                DEBUG.error("FailSafeRedirectUrlValidator: cannot read the URL patterns of {}", configInfo, re);
                readable = false;
            }
        }
        return readable;
    }

    /**
     * Tells whether an unconfigured whitelist should be read the way {@link RedirectUrlValidator} reads it, as
     * permission to redirect anywhere.
     *
     * <p>Always <code>false</code> here: a subclass whose endpoints need an escape hatch for deployments that
     * cannot populate the whitelist wires one in by overriding this. It has no say once the whitelist is
     * populated, which is checked before this is reached, nor when the whitelist could not be read at all - an
     * unreadable list is not an empty one, and reading it as one would hand the escape hatch to a deployment
     * that did populate it.
     *
     * @return <code>true</code> to accept any http or https URL when no whitelist is configured.
     */
    protected boolean allowAnyUrlWithoutWhitelist() {
        return false;
    }

    /**
     * Returns the maximum accepted length of a redirect URL. Overridden by the tests.
     *
     * @return The maximum accepted length of a redirect URL.
     */
    int getMaxUrlLength() {
        return RedirectUrlValidator.getMaxUrlLength();
    }

    /**
     * Tells whether an absolute or scheme relative URL points at this server.
     *
     * @param request The request being processed, may be <code>null</code>.
     * @param uri The URL to compare against this server.
     * @param configInfos Identifies the configurations additional origins may be read from.
     * @return <code>true</code> if scheme, host and port all match one of the origins this server answers on.
     */
    private boolean isSameOrigin(final HttpServletRequest request, final URI uri, final Collection<T> configInfos) {
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
        if (request != null) {
            // A container answering an HTTP/1.0 request, or a request wrapper filling in nothing, reports no port
            // at all; comparing that against the default a URL is normalised to would drop every absolute URL
            // naming this very server.
            final int requestPort = request.getServerPort() > 0
                    ? request.getServerPort() : defaultPortFor(request.getScheme());
            if (matchesOrigin(scheme, host, port, request.getScheme(), request.getServerName(), requestPort)) {
                return true;
            }
        }

        // Behind a TLS terminating proxy the container sees the internal scheme and port, while this server's own
        // URLs carry the ones it is configured with, so the configured origin has to count as well.
        if (matchesOrigin(scheme, host, port, getConfiguredScheme(), getConfiguredHost(), getConfiguredPort())) {
            return true;
        }

        // Consulted last, once the cheap comparisons above have failed, since a subclass may have to read
        // configuration to answer.
        final String effectiveScheme = scheme != null ? scheme : request != null ? request.getScheme() : null;
        if (effectiveScheme != null) {
            final String origin = origin(effectiveScheme, host, port != -1 ? port : defaultPortFor(effectiveScheme));
            if (getAdditionalOrigins(request, configInfos).contains(origin)) {
                return true;
            }
        }

        DEBUG.message("FailSafeRedirectUrlValidator: the URL {} points at none of the origins this server answers "
                + "on", uri);
        return false;
    }

    /**
     * The origins this server answers on besides the one the container reports for the incoming request and the
     * one it is configured with.
     *
     * <p>This implementation adds none. It is only consulted once the other origins have already failed to match,
     * so it costs nothing on the common path. Subclasses that can reach further configuration - the realm's Base
     * URL Source service, for instance - override this and add to it.
     *
     * @param request The request being processed, may be <code>null</code>.
     * @param configInfos Identifies the configurations the origins may be read from, may be empty.
     * @return The additional origins, never <code>null</code> and safe for a subclass to add to. Use
     * {@link #addOrigin(Collection, String)} so that what is added is normalised the same way.
     */
    protected Collection<String> getAdditionalOrigins(final HttpServletRequest request,
            final Collection<T> configInfos) {
        return new LinkedHashSet<String>();
    }

    /**
     * Adds the origin of the given URL to the collection, ignoring a URL that has none.
     *
     * @param origins The collection to add to.
     * @param url The URL to take the origin of, may be <code>null</code>.
     */
    protected static void addOrigin(final Collection<String> origins, final String url) {
        if (StringUtils.isEmpty(url)) {
            return;
        }
        final URI uri;
        try {
            uri = new URI(url);
        } catch (final URISyntaxException urise) {
            DEBUG.warning("FailSafeRedirectUrlValidator: {} is not a valid URI, ignoring it as an origin", url, urise);
            return;
        }
        final String scheme = uri.getScheme();
        final String host = uri.getHost();
        if (scheme == null || host == null) {
            // java.net.URI hands back no host for one it will not parse as a name - anything with an underscore
            // in it, for instance - and throws nothing while doing so.
            DEBUG.warning("FailSafeRedirectUrlValidator: no scheme and host could be read out of {}, "
                    + "so it is not an origin a redirect URL can be matched against", url);
            return;
        }
        origins.add(origin(scheme, host, uri.getPort() > 0 ? uri.getPort() : defaultPortFor(scheme)));
    }

    /**
     * Renders a scheme, host and port as the single string origins are compared as.
     *
     * @param scheme The scheme.
     * @param host The host.
     * @param port The port.
     * @return The normalised origin.
     */
    private static String origin(final String scheme, final String host, final int port) {
        return scheme.toLowerCase(Locale.ROOT) + "://" + canonicalHost(host) + ':' + port;
    }

    /**
     * The port a URL of the given scheme is served on when it names none.
     *
     * @param scheme The scheme, may be <code>null</code>.
     * @return The default port.
     */
    private static int defaultPortFor(final String scheme) {
        return "https".equalsIgnoreCase(scheme) ? 443 : 80;
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
     * Returns the scheme this server is configured to be reached at. Overridden by the tests.
     *
     * @return The configured scheme, or <code>null</code> when it is not configured.
     */
    String getConfiguredScheme() {
        return getServerProperty(Constants.AM_SERVER_PROTOCOL);
    }

    /**
     * Returns the host this server is configured to be reached at. Overridden by the tests.
     *
     * @return The configured host, or <code>null</code> when it is not configured.
     */
    String getConfiguredHost() {
        return getServerProperty(Constants.AM_SERVER_HOST);
    }

    /**
     * Returns the port this server is configured to be reached at. Overridden by the tests.
     *
     * @return The configured port, or <code>-1</code> when it is not configured.
     */
    int getConfiguredPort() {
        return parsePort(getServerProperty(Constants.AM_SERVER_PORT));
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
     * @param originScheme The scheme of the origin, may be <code>null</code> when it is not configured.
     * @param originHost The host of the origin, may be <code>null</code> when it is not configured.
     * @param originPort The port of the origin, not positive when it is not configured.
     * @return <code>true</code> if the URL points at that origin.
     */
    private static boolean matchesOrigin(final String urlScheme, final String host, final int urlPort,
            final String originScheme, final String originHost, final int originPort) {
        if (StringUtils.isEmpty(originScheme) || StringUtils.isEmpty(originHost) || originPort <= 0) {
            return false;
        }
        // A scheme relative URL inherits the scheme it is resolved against.
        final String scheme = urlScheme != null ? urlScheme : originScheme;
        // Not equalsIgnoreCase for the host: it folds U+0131 and U+0130 onto "i", which a browser does not.
        if (!scheme.equalsIgnoreCase(originScheme)
                || !canonicalHost(host).equals(canonicalHost(originHost))) {
            return false;
        }

        final int port = urlPort != -1 ? urlPort : defaultPortFor(scheme);
        return port == originPort;
    }

    /**
     * Puts a host name into the form the comparison is made in: lower case in the root locale, without a trailing
     * root label and without the brackets of an IPv6 literal.
     *
     * <p>A root anchored name names the host it is compared against, so it may not be turned away: with no
     * whitelist configured that would deny a legitimate redirect back to this server. Only that one form is
     * folded - the comparison stays an equality, never a suffix match. <code>java.net.URI</code> keeps the
     * brackets of an IPv6 literal, <code>HttpServletRequest.getServerName()</code> may not, so neither side is
     * compared with them.
     *
     * <p>No IDNA mapping is applied. {@link java.net.IDN} implements IDNA2003, which <em>maps</em> characters rather
     * than preserving them: it turns <code>gro&szlig;.example.com</code> into <code>gross.example.com</code>, a
     * separately registrable third party domain, which would then be taken for this server's own origin. It also
     * never produces the form a browser reaching that host actually sends, the UTS46 one -
     * <code>xn--gro-7ka.example.com</code> - so it would not even serve the names it was applied for. A deployment
     * whose name is an internationalised one therefore has to set <code>com.iplanet.am.server.host</code> to the
     * ASCII compatible form of it, which is what the whitelist path demands as well:
     * {@link com.sun.identity.shared.whitelist.URLPatternMatcher} matches the host of a URL as it stands too.
     *
     * @param host The host name to canonicalise, never <code>null</code>.
     * @return The name in lower case, without a trailing root label or IPv6 brackets.
     */
    private static String canonicalHost(final String host) {
        String canonical = host.toLowerCase(Locale.ROOT);
        if (canonical.length() > 1 && canonical.endsWith(".")) {
            canonical = canonical.substring(0, canonical.length() - 1);
        }
        if (canonical.length() > 1 && canonical.charAt(0) == '[' && canonical.charAt(canonical.length() - 1) == ']') {
            canonical = canonical.substring(1, canonical.length() - 1);
        }
        return canonical;
    }
}

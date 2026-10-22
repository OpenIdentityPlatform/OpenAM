/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: FSUtils.java,v 1.10 2009/11/20 23:52:57 ww203982 Exp $
 *
 * Portions Copyrighted 2013-2016 ForgeRock AS.
 * Portions Copyrighted 2025-2026 3A Systems LLC.
 */

package com.sun.identity.federation.common;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Predicate;

import com.sun.identity.common.SystemConfigurationException;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.encode.CookieUtils;
import com.sun.identity.shared.locale.Locale;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.opendj.ldap.DN;

/**
 * This class contain constants used in the SDK.
 */
public class FSUtils {
    public static String deploymentURI =
        SystemConfigurationUtil.getProperty(
            "com.iplanet.am.services.deploymentDescriptor");

    public static final String BUNDLE_NAME = "libIDFF";
    public static IFSConstants sc;
    public static ResourceBundle bundle =
        Locale.getInstallResourceBundle(BUNDLE_NAME);
    public static Debug debug = Debug.getInstance("libIDFF");    
    private static SecureRandom random = new SecureRandom();
    public static final String FSID_PREFIX = "f"; 
    public static volatile IDFFMetaManager metaInstance = null;

    /**
     * When the last attempt to build {@link #metaInstance} failed, or <code>0</code>.
     * Read outside the lock so that a failing attempt is not repeated by every concurrent
     * caller for the length of an SMS outage.
     */
    private static volatile long metaManagerFailedAt = 0L;

    /** How long a failed meta manager creation is remembered before it is retried. */
    private static final long META_MANAGER_RETRY_INTERVAL = 2000L;

    private static String server_protocol =
        SystemPropertiesManager.get(Constants.AM_SERVER_PROTOCOL);
    private static String server_host =
        SystemPropertiesManager.get(Constants.AM_SERVER_HOST);
    private static String server_port =
        SystemPropertiesManager.get(Constants.AM_SERVER_PORT);
    private static String server_uri = SystemPropertiesManager.get(
        Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
    private static String localURL = server_protocol + "://" + server_host +
            ":" + server_port + server_uri;
    private static int int_server_port = 0;
    static {
        try {
            int_server_port = Integer.parseInt(server_port);
        } catch (NumberFormatException nfe) {
            debug.error("Unable to parse port " + server_port, nfe);
        }
    }

    /**
     * Constructor
     */
    private FSUtils() {
    }

    /**
     * Sets the locale of the resource bundle
     *
     */
    public static void setLocale(String localeName){
        try {
            bundle = ResourceBundle.getBundle(
                BUNDLE_NAME, Locale.getLocale(localeName));
        } catch (MissingResourceException mre) {
            System.err.println(mre.getMessage());
            System.exit(1);
        }
    } 

    /**
     * Generates an ID String with length of IFSConstants.ID_LENGTH.
     * @return string the ID String; or null if it fails.
     */
    public static String generateID() {
        if (random == null) {
            return null;
        }
        byte bytes[] = new byte[IFSConstants.ID_LENGTH];
        random.nextBytes(bytes);
        String encodedID = FSID_PREFIX + SAMLUtils.byteArrayToHexString(bytes);
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSUtils.generateID: generated id is " +
                encodedID);
        }

        return encodedID;
    }
    
    /**
     * Generates source ID String 
     * @param entityID the entity ID of the source site 
     * @return source ID 
     */
    public static String generateSourceID(String entityID) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (Exception e) {
            FSUtils.debug.error("FSUtils.generateSourceID: Exception:",e);
            return null;
        }
        char chars[] = entityID.toCharArray();
        byte bytes[] = new byte[chars.length];
        for (int i = 0; i < chars.length; i++) {
            bytes[i] = (byte) chars[i];
        }

        md.update(bytes);
        return SAMLUtils.byteArrayToString(md.digest());
    }
    
    /**
     * Generates assertion handle.
     * @return 20-byte random string to be used to form an artifact.
     */
    public static String generateAssertionHandle() {
        String result = null;
        String encodedID = generateID();
        if (encodedID != null) {
            try {
                result = encodedID.substring(0, 20);
            } catch (Exception e) {
                FSUtils.debug.error("FSUtil.generateAssertionHandle:", e);
            }
        }
        return result;
    }
    
    /**
     * Converts a string to Base64 encoded string.
     * @param succinctID provider's succinctID string
     * @return Base64 encoded string
     */
    public static String stringToBase64(String succinctID) {
        String encodedID = null;
        try {
            encodedID = Base64.encode(SAMLUtils.stringToByteArray(succinctID))
                .trim();
        } catch (Exception e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSUtils:stringToBase64: exception encode input:", e);
            }
        }
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("base 64 source id is :"+encodedID);
        }
        return encodedID;

    }
    /**
     * Checks content length of a http request to avoid dos attack.
     * In case IDFF inter-op with other IDFF vendor who may not provide content
     * length in HttpServletRequest. We decide to support no length restriction
     * for Http communication. Here, we use a special value (e.g. 0) to
     * indicate that no enforcement is required.
     * @param request <code>HttpServletRequest</code> instance to be checked.
     * @exception ServletException if context length of the request exceeds
     *     maximum content length allowed.
     */

    public static void checkHTTPRequestLength(HttpServletRequest request)
        throws ServletException
    {
        // avoid the DOS attack for SOAP messaging 
        int maxContentLength = SAMLUtils.getMaxContentLength();

        if (maxContentLength != 0) {
            int length =  request.getContentLength();

            if (length == -1) {
                throw new ServletException(bundle.getString("unknownLength"));
            }

            if (length > maxContentLength) {
                if (debug.messageEnabled()) {
                    debug.message("FSUtils.checkHTTPRequestLength: " +
                        "content length too large" + length); 
                }
                throw new ServletException(
                    bundle.getString("largeContentLength"));
             }
        }    
    }
   
/**
     * The port a URL actually addresses. {@code URL.getPort()} is <code>-1</code> when the
     * port is not written out, while {@code HttpServletRequest.getServerPort()} always
     * reports a number, so comparing the two raw reads
     * <code>http://host.example.com/openam/landing</code> as a different container than the
     * request it was issued on. Cookie-less clients would lose URL rewriting on the
     * deployment's own host.
     * @param url the URL to read the port of.
     * @return the explicit port, or the scheme's default port when none was written.
     */
    private static int effectivePort(URL url) {
        int port = url.getPort();
        return port == -1 ? url.getDefaultPort() : port;
    }

    /**
     * Parses a target that has to be an absolute URL. A relative or protocol relative target
     * can be neither mapped to a resource nor redirected to safely - a
     * <code>sendRedirect("//host/x")</code> leaves this deployment for another host - so it is
     * refused rather than left to make {@code new URL} throw, which committed an empty 200.
     * @param url the target to parse.
     * @return the parsed target, or <code>null</code> if it is not an absolute URL.
     */
    private static URL absoluteURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException mue) {
            return null;
        }
    }

    /**
     * Tells whether a URL addresses this deployment, as the deployment's own configuration
     * names it: this server's URL, or a server or site of the platform server list, with a
     * path under its context. Unlike {@link #isSameContainer}, nothing is read from the
     * request: its host and port come from the Host header, which is whatever the sender
     * wrote. The configuration is read on each call, as {@link #deploymentURI} is not.
     * @param url the URL to test.
     * @return <code>true</code> if the URL is on this deployment.
     */
    public static boolean isDeploymentURL(String url) {
        URL target;
        try {
            target = new URL(url);
        } catch (MalformedURLException mue) {
            return false;
        }
        if (isUnder(target,
                SystemPropertiesManager.get(Constants.AM_SERVER_PROTOCOL),
                SystemPropertiesManager.get(Constants.AM_SERVER_HOST),
                SystemPropertiesManager.get(Constants.AM_SERVER_PORT),
                SystemPropertiesManager.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR))) {
            return true;
        }
        List servers;
        List sites;
        try {
            servers = SystemConfigurationUtil.getServerList();
            // getRemoteServiceURLs removes the sites from the shared server list
            sites = SystemConfigurationUtil.getSiteList();
        } catch (SystemConfigurationException | RuntimeException e) {
            // with no configuration plugin named - the library outside the war - the read
            // fails with a NullPointerException, on every call
            debug.message("FSUtils.isDeploymentURL: cannot read the server list", e);
            return false;
        }
        return isUnderAny(target, servers) || isUnderAny(target, sites);
    }

    private static boolean isUnderAny(URL target, List bases) {
        if (bases == null) {
            return false;
        }
        for (Object entry : bases) {
            URL base;
            try {
                base = new URL((String) entry);
            } catch (MalformedURLException mue) {
                continue;
            }
            if (isUnder(target, base.getProtocol(), base.getHost(),
                    String.valueOf(effectivePort(base)), base.getPath())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isUnder(URL target, String protocol, String host, String port,
        String path) {

        String prefix = path + "/";
        // the platform list holds its entries lower cased, context path included
        return target.getProtocol().equals(protocol)
            && target.getHost().equalsIgnoreCase(host)
            && String.valueOf(effectivePort(target)).equals(port)
            && target.getPath().regionMatches(true, 0, prefix, 0, prefix.length());
    }

    /**
     * Test if url in argument is
     *  in  the same web container as current opensso web
     * apps serving the request.
     * @param request HttpServletRequest
     * @param url the URL to test against the current web container
     * @return true if request and url are in the same web container else false
     */
    public static boolean isSameContainer(
            HttpServletRequest request,
            String url) {

        URL target;
        try {
            target = new URL(url);
        } catch (MalformedURLException ex) {
            FSUtils.debug.error("FSUtils.isSameContainer: Exception occured", ex);
            return false;
        }
        return isSameContainer(request, url, target);
    }

    /**
     * {@link #isSameContainer(HttpServletRequest, String)} on a target already parsed.
     * @param request HttpServletRequest
     * @param url the URL to test, as written.
     * @param target the same URL, parsed.
     * @return true if request and url are in the same web container else false
     */
    private static boolean isSameContainer(
            HttpServletRequest request,
            String url,
            URL target) {

        boolean result = false;
        FSUtils.debug.message("FSUtils.isSameContainer: called");

        //get source host and port
        String sourceHost = request.getServerName();
        int sourcePort = request.getServerPort();
        if (debug.messageEnabled()) {
            FSUtils.debug.message("FSUtils.isSameContainer: " +
                "SourceHost=" + sourceHost + " SourcePort=" + sourcePort);
        }
        //get target host and port
        String targetHost = target.getHost();
        int targetPort = effectivePort(target);
        if (debug.messageEnabled()) {
            FSUtils.debug.message("FSUtils.isSameContainer: targetHost=" +
                targetHost + " targetPort=" + targetPort);
        }
        int index = url.indexOf(deploymentURI + "/");
        if (!(targetHost.equals(sourceHost)) ||
                !(sourcePort == targetPort) ||
                !(index > 0)) {
            if (debug.messageEnabled()) {
                FSUtils.debug.message("FSUtils.isSameContainer: Source and "
                    + "Target are not on the same container.");
            }

        } else {
            if (debug.messageEnabled()) {
                FSUtils.debug.message("FSUtils.isSameContainer: Source and " +
                    "Target are on the same container.");
            }
            result = true;
        }
        return result;
    }
 
    // These roots are served by internal endpoints only and must never be reachable through a
    // forward driven by a caller supplied URL. Defence in depth for the <dispatcher> elements
    // on the matching <filter-mapping>s in openam-server-only/src/main/webapp/WEB-INF/web.xml,
    // which are the deep fix; keep the two in step when a security filter is added there.
    private static final String[] INTERNAL_FORWARD_ROOTS = {
        "/notifications", "/ws", "/jaxrpc"
    };

    // The roots of the <filter-mapping>s in web.xml that name no <dispatcher>. Such a filter runs
    // on a REQUEST dispatch only, so a forward reaches the endpoint with it skipped; a target
    // under one of these is redirected to instead, which the browser makes as a request of its
    // own. Lower case, as fold() leaves a path. Keep in step with web.xml as the list above.
    private static final String[] REQUEST_ONLY_FILTER_ROOTS = {
        // FQDNValidationFilter, CacheForFiveMinutes, CacheForAMonth
        "/xui",
        // FQDNValidationFilter, CORSFilter
        "/oauth2",
        // CORSFilter
        "/json",
        // JatoAuditFilter; XUIFilter also on /idm/EndUser
        "/service", "/federation", "/realm", "/agentconfig", "/sts", "/delegation", "/idm",
        "/debug.jsp",
        // SsoAdmJspAuditFilter
        "/ssoadm.jsp"
    };

    /** How much of an untrusted value {@link #forLog} keeps. */
    private static final int MAX_LOGGED_LENGTH = 256;

    /**
     * Renders an untrusted value for a single log line. Strips CR/LF so a request
     * parameter cannot forge additional entries in the debug file, and truncates so a
     * caller cannot write an arbitrary volume to it: several of these values come from
     * query parameters on unauthenticated endpoints and are logged before any length
     * limit is applied to them. A prefix identifies the value just as well.
     * @param value the value to render, may be <code>null</code>.
     * @return the value with CR and LF replaced and its length bounded, or
     *  <code>"null"</code>.
     */
    public static String forLog(String value) {
        if (value == null) {
            return "null";
        }
        String single = value.replace('\r', '_').replace('\n', '_');
        if (single.length() <= MAX_LOGGED_LENGTH) {
            return single;
        }
        return single.substring(0, MAX_LOGGED_LENGTH) + "...[truncated, "
            + single.length() + " characters]";
    }

    /**
     * Tells whether a target names the host and the port the request arrived on, reading the
     * port exactly as it is written: a URL with no port is <em>not</em> on the request's port.
     * <p>
     * Deliberately stricter than {@link #isSameContainer}, which reads an unwritten port as
     * the scheme's default so that URL rewriting still reaches the deployment's own host.
     * That reading must not widen the forward/redirect decision as a side effect: a same-host
     * target written without a port has always been redirected to, and a FORWARD dispatch
     * runs only the filters that declare {@code <dispatcher>FORWARD</dispatcher>}, which the
     * {@code /*} security filters in web.xml do not.
     * @param request the request being served.
     * @param target the target URL, parsed.
     * @return <code>true</code> if host and written port both match the request's.
     */
    private static boolean writesTheRequestsPort(HttpServletRequest request, URL target) {
        return target.getHost().equals(request.getServerName())
            && request.getServerPort() == target.getPort();
    }

    /**
     * Drops the query string from a forward target, the way a container does before
     * mapping it. Only <code>?</code> is a delimiter here: a container treats
     * <code>#</code> as an ordinary path character, so it gets its own reading rather
     * than being cut unconditionally.
     * @param path the path to strip.
     * @return the path up to the first <code>?</code>.
     */
    private static String cutAtQuery(String path) {
        int index = path.indexOf('?');
        return index == -1 ? path : path.substring(0, index);
    }

    /**
     * Drops everything from the first <code>#</code>, for the reading in which a
     * <code>#</code> is taken to open a fragment.
     * @param path the path to strip.
     * @return the path up to the first <code>#</code>.
     */
    private static String cutAtFragment(String path) {
        int index = path.indexOf('#');
        return index == -1 ? path : path.substring(0, index);
    }

    /**
     * Removes <code>;</code> path parameters the way a servlet container does: from each
     * <code>;</code> up to the next <code>/</code>, on the still encoded string. This runs
     * before decoding, so an encoded <code>%3B</code> is not a delimiter, and the parameter
     * of the last segment extends to the end of the path.
     * @param path the path to strip.
     * @return the path without its path parameters.
     */
    private static String stripPathParams(String path) {
        StringBuilder stripped = new StringBuilder(path.length());
        int i = 0;
        while (i < path.length()) {
            char c = path.charAt(i);
            if (c == ';') {
                int slash = path.indexOf('/', i);
                if (slash == -1) {
                    break;
                }
                i = slash;
            } else {
                stripped.append(c);
                i++;
            }
        }
        return stripped.toString();
    }

    /**
     * Removes <code>;</code> path parameters to the end of the segment they appear in,
     * treating both <code>/</code> and <code>\</code> as segment separators. A stricter
     * reading than {@link #stripPathParams}, kept because it turns <code>..;x</code> into a
     * traversal that the other reading leaves inert - whichever a container takes, one of
     * the two readings sees the internal path.
     * @param path the path to strip.
     * @return the path without its path parameters.
     */
    private static String stripSegmentParams(String path) {
        StringBuilder stripped = new StringBuilder(path.length());
        boolean inParams = false;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '/' || c == '\\') {
                inParams = false;
            } else if (c == ';') {
                inParams = true;
            }
            if (!inParams) {
                stripped.append(c);
            }
        }
        return stripped.toString();
    }

    /**
     * Collapses a path the way a container normalizes a dispatch target: <code>\</code>
     * treated as a separator, empty and <code>.</code> segments dropped and <code>..</code>
     * segments applied. Only a segment that is exactly <code>..</code> traverses, which is
     * what makes the path parameter readings differ.
     * @param path the path to fold.
     * @return the folded path, lower cased for comparison.
     */
    private static String fold(String path) {
        Deque<String> segments = new ArrayDeque<String>();
        StringTokenizer tokenizer =
            new StringTokenizer(path.replace('\\', '/'), "/");
        while (tokenizer.hasMoreTokens()) {
            String segment = tokenizer.nextToken();
            if (segment.length() == 0 || segment.equals(".")) {
                continue;
            }
            if (segment.equals("..")) {
                segments.pollLast();
            } else {
                segments.addLast(segment);
            }
        }
        StringBuilder folded = new StringBuilder();
        for (String segment : segments) {
            folded.append('/').append(segment);
        }
        return folded.toString().toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * Tells whether any reading a container might plausibly take of this path lands inside
     * one of the roots.
     * <p>
     * Emulating one container exactly is not enough: the readings differ in where the path
     * is cut, whether <code>;</code> parameters run to the next <code>/</code> or to the end
     * of the segment, and whether decoding happens before or after either. A reading that
     * folds more aggressively than the container can fold an internal segment away and
     * report a path that looks harmless - which is how <code>/notifications;\..</code> was
     * read as <code>""</code> while the container mapped it to <code>/notifications</code>.
     * So every reading is tested and any one of them landing inside a root counts. Matching a
     * contrived path too often is acceptable; missing one is not.
     * @param path the (possibly still encoded) path to test.
     * @param roots the roots to test against, lower case.
     * @return <code>true</code> if any reading lands inside one of the roots.
     */
    private static boolean resolvesInside(String path, String[] roots) {
        String query = cutAtQuery(path);
        String[] bases = { query, cutAtFragment(query), path };
        for (int i = 0; i < bases.length; i++) {
            if (readingLandsIn(bases[i], roots)) {
                return true;
            }
        }
        return false;
    }

    private static boolean readingLandsIn(String base, String[] roots) {
        // the container reading: strip parameters on the encoded string, then decode, then fold
        if (landsIn(fold(decodeLenient(stripPathParams(base))), roots)
            // a container that does not strip path parameters at all
            || landsIn(fold(decodeLenient(base)), roots)
            // parameters taken to the end of the segment, before and after decoding
            || landsIn(fold(stripSegmentParams(decodeLenient(base))), roots)
            || landsIn(fold(decodeLenient(stripSegmentParams(base))), roots))
        {
            return true;
        }
        // a decoder that drops a stray '%' rather than keeping it, as decodeLenient does
        if (base.indexOf('%') != -1) {
            String noPercent = base.replace("%", "");
            if (landsIn(fold(stripPathParams(noPercent)), roots)
                || landsIn(fold(stripSegmentParams(noPercent)), roots))
            {
                return true;
            }
        }
        // a decoder that maps '+' to a space in a path, which no container is known to do
        return base.indexOf('+') != -1
            && landsIn(fold(stripSegmentParams(base.replace('+', ' '))), roots);
    }

    /**
     * Tells whether a folded path lands inside one of the roots, cutting a delimiter that
     * only surfaced during decoding. Folding before the cut is the order that matters here:
     * it can move a root segment in front of a <code>?</code> that a <code>%3F</code>
     * turned into a real delimiter, which cutting first would have discarded along with the
     * traversal that follows it.
     * @param folded the folded path.
     * @param roots the roots to test against, lower case.
     * @return <code>true</code> if it, or any prefix cut from it, is inside one of the roots.
     */
    private static boolean landsIn(String folded, String[] roots) {
        return isUnderRoot(folded, roots)
            || isUnderRoot(cutAtQuery(folded), roots)
            || isUnderRoot(cutAtFragment(cutAtQuery(folded)), roots);
    }

    private static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    /**
     * Tells whether the target carries a <code>%</code> that does not begin a complete hex
     * escape. Such a target is not a valid URI path and {@code UDecoder} refuses it, so the
     * container would throw out of {@code getRequestDispatcher} and leave the response
     * uncommitted. Refusing here turns that blank 200 into a 400. A correctly encoded
     * literal percent - <code>%25</code> - is unaffected.
     * @param path the raw target.
     * @return <code>true</code> if an escape is malformed.
     */
    private static boolean hasMalformedEscape(String path) {
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) == '%'
                && (i + 2 >= path.length()
                    || !isHexDigit(path.charAt(i + 1))
                    || !isHexDigit(path.charAt(i + 2))))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether the path carries a control character. Containers disagree on whether a
     * decoded NUL truncates a path, so a target that decodes to one is refused outright.
     * @param path the path to test.
     * @return <code>true</code> if a control character is present.
     */
    private static boolean hasControlCharacter(String path) {
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c < 0x20 || c == 0x7f) {
                return true;
            }
        }
        return false;
    }

    /**
     * Decodes <code>%XX</code> escapes without throwing on a malformed one: a <code>%</code>
     * that does not begin a complete hex escape is kept literally. The raw target has already
     * been refused by {@link #hasMalformedEscape} at that point, so this only concerns a
     * percent that a previous decoding round produced - <code>%2525</code> and the like - and
     * failing there would refuse a legitimate target such as
     * <code>/config/federation/default/100%25done.jsp</code> with a 400.
     * <p>
     * Unlike {@code URLEncDec.decode} this does not map <code>+</code> to a space: no
     * container does so when mapping a dispatch path. That reading is kept as one of the
     * alternatives in {@link #readingLandsIn} instead, where it can only refuse more.
     * @param path the path to decode.
     * @return the decoded path.
     */
    private static String decodeLenient(String path) {
        StringBuilder decoded = new StringBuilder(path.length());
        int i = 0;
        int length = path.length();
        while (i < length) {
            char c = path.charAt(i);
            if (c == '%' && i + 2 < length
                && isHexDigit(path.charAt(i + 1)) && isHexDigit(path.charAt(i + 2)))
            {
                // consecutive escapes are one byte run: a multi-byte character must be
                // decoded as a whole or its bytes turn into replacement characters
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                while (i + 2 < length && path.charAt(i) == '%'
                    && isHexDigit(path.charAt(i + 1)) && isHexDigit(path.charAt(i + 2)))
                {
                    bytes.write(Integer.parseInt(path.substring(i + 1, i + 3), 16));
                    i += 3;
                }
                decoded.append(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
            } else {
                decoded.append(c);
                i++;
            }
        }
        return decoded.toString();
    }

    private static boolean isUnderRoot(String canonicalPath, String[] roots) {
        for (int i = 0; i < roots.length; i++) {
            String root = roots[i];
            if (canonicalPath.equals(root) ||
                canonicalPath.startsWith(root + "/"))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Canonicalizes a forward target at least as strictly as the servlet
     * container would and tells whether it may be dispatched to.
     * @param resource the resource path a forward is about to target.
     * @return <code>false</code> if the path resolves inside an internal only
     *  root or cannot be canonicalized, <code>true</code> otherwise.
     */
    static boolean isForwardTargetAllowed(String resource) {
        if (resource == null) {
            return false;
        }
        try {
            if (hasMalformedEscape(resource)) {
                return false;
            }
            return !anyDecoding(resource,
                path -> hasControlCharacter(path) || resolvesInside(path, INTERNAL_FORWARD_ROOTS));
        } catch (Exception ex) {
            // none of the helpers throws, but they all run on caller data: any
            // unexpected failure must refuse the forward
            return false;
        }
    }

    /**
     * Tells whether a forward target lies under a root whose filters web.xml maps for REQUEST
     * dispatches only, so that a forward would reach the endpoint with those filters skipped.
     * @param resource a resource path {@link #isForwardTargetAllowed} has allowed.
     * @return <code>true</code> if the target has to be reached by a request of its own.
     */
    static boolean hasRequestOnlyFilters(String resource) {
        return anyDecoding(resource, path -> resolvesInside(path, REQUEST_ONLY_FILTER_ROOTS));
    }

    /**
     * Decodes repeatedly so a multiply-encoded target cannot slip a segment past a guard on a
     * container that decodes the dispatch path more than once, and tests every intermediate
     * form rather than only the fully decoded one.
     * @param resource the raw target.
     * @param test the test for one form of it.
     * @return <code>true</code> if any form passes the test.
     */
    private static boolean anyDecoding(String resource, Predicate<String> test) {
        String path = resource;
        for (int i = 0; i < 4; i++) {
            if (test.test(path)) {
                return true;
            }
            String decoded = decodeLenient(path);
            if (decoded.equals(path)) {
                return false;
            }
            path = decoded;
        }
        return test.test(path);
    }

    /**
     * Forwards or redirects to a new URL. This method will do forwarding
     * if the target url is in  the same web deployment URI as current web 
     * apps. Otherwise will do redirecting.   
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @param url the target URL to be forwarded to redirected.  
     */
    public static void forwardRequest(
        HttpServletRequest request,
        HttpServletResponse response,
        String url)
    {
        FSUtils.debug.message("FSUtils.forwardRequest: called");
        String newUrl = null;
        // Parsed once, here, and handed to every decision below, so no two of them can read
        // the string differently. Not through the public isSameContainer, which logs a parse
        // failure at error level with a stack trace: a relative RelayState on an
        // unauthenticated endpoint would write one to the debug file per request.
        URL target = absoluteURL(url);
        if (target == null) {
            // Neither dispatchable nor safe to redirect to: "//host/x" would send the
            // caller to another host. Previously new URL(url) threw further down and the
            // container committed an empty 200.
            FSUtils.debug.error("FSUtils.forwardRequest: target is not an absolute URL:"
                + forLog(url));
            sendErrorIfUncommitted(response, HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        // Taken once, on the URL as it arrived, and reused for the forward/redirect decision
        // below. Deriving it twice let the rewrite gate and the dispatch gate disagree.
        boolean sameContainer = isSameContainer(request, url, target);
        try {
            SessionProvider sessionProvider = SessionManager.getProvider();
            Object token = sessionProvider.getSession(request);
            // Only rewrite a target served by this container. URL rewriting
            // appends the session token to the URL for cookie-less clients, so
            // rewriting an off-host target would hand the token to that host.
            if ((token != null) && (sessionProvider.isValid(token)) && sameContainer) {
                newUrl = sessionProvider.rewriteURL(token, url);
            }
        } catch (Exception se) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSUtils.forwardReqeust: couldn't rewrite url: " +
                    se.getMessage());
            }
            newUrl = null;
        }
        if (newUrl == null) {
            newUrl = url;
        }
        // A provider whose rewriteURL does more than append a path parameter can move the
        // target; the dispatch decision has to be about the string actually dispatched.
        URL dispatched = target;
        if (sameContainer && !newUrl.equals(url)) {
            URL rewritten = absoluteURL(newUrl);
            if (rewritten == null || !isSameContainer(request, newUrl, rewritten)) {
                // rewriteURL moved the target off this container, or out of URL form. The
                // rewritten string is the one carrying the session token, so it must not be
                // sent anywhere: fall back to the URL as it arrived and redirect that instead.
                FSUtils.debug.error("FSUtils.forwardRequest: URL rewriting moved the target "
                    + "off this container; redirecting the un-rewritten URL:" + forLog(url));
                newUrl = url;
                sameContainer = false;
            } else {
                dispatched = rewritten;
            }
        }

        try {
            /**
             * IBM websphere is not able to handle forwards with long urls.
             */ 
            boolean isWebSphere = false;
            String container = SystemConfigurationUtil.getProperty(
                Constants.IDENTITY_WEB_CONTAINER);
            if (container != null && (container.indexOf("IBM") != -1)) {
               isWebSphere = true;
            }

            int index = newUrl.indexOf(deploymentURI + "/");
            if (!sameContainer || !writesTheRequestsPort(request, dispatched)
                || !(index > 0) || isWebSphere)
            {
                FSUtils.debug.message("FSUtils.forwardRequest: Source and " +
                    "Target are not on the same container." + 
                    "Redirecting to target");            
                response.sendRedirect(newUrl);
                return;
            } else {      
                String resource = newUrl.substring(
                    index + deploymentURI.length());
                if (!isForwardTargetAllowed(resource)) {
                    if (FSUtils.debug.warningEnabled()) {
                        FSUtils.debug.warning("FSUtils.forwardRequest: refused "
                            + "forward to internal resource: " + forLog(resource));
                    }
                    sendErrorIfUncommitted(response, HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
                // The target comes from the request (goto/RelayState). A forward
                // bypasses the web.xml filters and can reach /WEB-INF, so refuse
                // anything that is not a plain in-app path and dispatch the form
                // the container maps.
                String dispatchPath = ForwardPathValidator.forwardTarget(resource);
                if (dispatchPath == null) {
                    FSUtils.debug.warning("FSUtils.forwardRequest: refusing to "
                        + "forward to a path with traversal or a reserved "
                        + "directory");
                    sendErrorIfUncommitted(response, HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
                if (hasRequestOnlyFilters(resource)) {
                    FSUtils.debug.message("FSUtils.forwardRequest: target has filters that "
                        + "do not run on a forward. Redirecting to target");
                    response.sendRedirect(newUrl);
                    return;
                }
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSUtils.forwardRequest: Forwarding to :" + forLog(dispatchPath));
                }
                RequestDispatcher dispatcher =
                    request.getRequestDispatcher(dispatchPath);
                if (dispatcher == null) {
                    // The container declined to map the target - it decodes to a path it will
                    // not dispatch to, such as one climbing above the context root. Forwarding
                    // on null NPEs and leaves the container to commit an empty 200.
                    FSUtils.debug.error("FSUtils.forwardRequest: container returned no "
                        + "dispatcher for resource:" + forLog(dispatchPath));
                    sendErrorIfUncommitted(response, HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
                try {
                    dispatcher.forward(request, response);
                } catch (Exception e) {
                    FSUtils.debug.error("FSUtils.forwardRequest: Exception "
                        + "occured while trying to forward to resource:" +
                        forLog(dispatchPath), e);
                    sendErrorIfUncommitted(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            } 
        } catch (Exception ex) {
            FSUtils.debug.error("FSUtils.forwardRequest: Exception occured",ex);
            sendErrorIfUncommitted(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Answers a status when nothing has been written yet. A forward that fails part way through
     * has already committed, and there the status stands as it is; the case this covers is a
     * failure before anything reached the client, which otherwise leaves the container to
     * commit an empty 200 that the caller reads as a successful federation. A status that
     * cannot be written is logged here rather than thrown, so that no caller's catch answers
     * the same response a second time.
     * @param response the response to write the status to.
     * @param status the status to send.
     */
    private static void sendErrorIfUncommitted(HttpServletResponse response, int status) {
        if (response.isCommitted()) {
            return;
        }
        try {
            response.sendError(status);
        } catch (Exception ioe) {
            FSUtils.debug.error(
                "FSUtils.forwardRequest: could not write the error status", ioe);
        }
    }

    /**
     * Returns entity ID from the Succinct ID.
     * @param realm The realm under which the entity resides.
     * @param succinctID Succinct ID.
     * @return String entity ID; or <code>null</code> for failure in 
     *  converting the succinct id to entity id.
     */ 
    private static String getProviderIDFromSuccinctID(
        String realm, String succinctID) {
        if (succinctID == null) {
            return null;
        }
        try {
            metaInstance = getIDFFMetaManager();
            if (metaInstance != null) {
                return metaInstance.getEntityIDBySuccinctID(realm, succinctID);
            }
        } catch(Exception ex) {
            debug.error("FSUtils.getProviderIDFromSuccinctID::", ex);
        }
        return null;
    }

    /**
     * Finds the preferred IDP from the HttpServletRequest.
     * @param realm The realm under which the entity resides.
     * @param request HttpServletRequest.
     * @return String preferred IDP entity ID; or <code>null</code> for failure
     *  or unable to find in the request.
     */
    public static String findPreferredIDP(
        String realm, HttpServletRequest request) {

        if (request == null) {
            return null;
        }

        String succinctID = request.getParameter(IFSConstants.PROVIDER_ID_KEY);
        if ((succinctID == null) || succinctID.length() == 0) { 
           debug.message("FSUtils.findPreferredIDP::Pref IDP not found.");
           return null;
        }

        succinctID = succinctID.trim();
        String preferredSuccinctId = null;
        StringTokenizer st = new StringTokenizer(succinctID, " ");
        while(st.hasMoreTokens()){
            preferredSuccinctId = st.nextToken();
            if ((preferredSuccinctId.length() < 28) &&
                 st.hasMoreTokens())
            {
                preferredSuccinctId = 
                    preferredSuccinctId + "+" + st.nextToken();
            }
        }

        preferredSuccinctId = SAMLUtils.byteArrayToString(
            Base64.decode(preferredSuccinctId));

        return getProviderIDFromSuccinctID(realm, preferredSuccinctId);
    }

    /**
     * Removes new line characters (useful for Base64 decoding)
     * @param s String
     * @return result String 
     */
    public static String removeNewLineChars(String s) {
        String retString = null;
        if ((s != null) && (s.length() > 0) && (s.indexOf('\n') != -1)) {
            char[] chars = s.toCharArray();
            int len = chars.length;
            StringBuffer sb = new StringBuffer(len);
            for (int i = 0; i < len; i++) {
                char c = chars[i];
                if (c != '\n') {
                    sb.append(c);
                }
            }
            retString = sb.toString();
        } else {
            retString = s;
        }
        return retString;
    }

    /**
     * Returns an instance of the IDFF meta manager class.
     * @return <code>IDFFMetaManager</code> instance; or <code>null</code>
     *  if it cannot retrieve the instance.
     */
    public static IDFFMetaManager getIDFFMetaManager() {
        IDFFMetaManager instance = metaInstance;
        if (instance != null) {
            return instance;
        }
        // The federation endpoints resolve the manager per request rather than latching the
        // first answer, so that a transient SMS failure recovers without a restart. Without
        // this window every concurrent request would queue on the monitor below and pay its
        // own failing constructor for the whole length of an outage.
        long failedAt = metaManagerFailedAt;
        if (failedAt != 0
            && System.currentTimeMillis() - failedAt < META_MANAGER_RETRY_INTERVAL)
        {
            return null;
        }
        synchronized (IDFFMetaManager.class) {
            if (metaInstance != null) {
                return metaInstance;
            }
            try {
                // TODO: generate admin session and pass it in
                metaInstance = new IDFFMetaManager(null);
                metaManagerFailedAt = 0L;
                return metaInstance;
            } catch (Exception e) {
                metaManagerFailedAt = System.currentTimeMillis();
                FSUtils.debug.error ("FSUtils.getIDFFMetaManager:"
                    + " Could not create meta Manager", e);
                return null;
            }
        }
    }

    /*
     * Returns the Authentication Domain URL Mappings for the given 
     * organization.
     * @param orgDN dn of the organization/realm name
     * @return authentication domain
     */
    public static String getAuthDomainURL(String orgDN) {
        if (orgDN == null || orgDN.length() == 0) {
            return "/";
        }
        if (LDAPUtils.isDN(orgDN)) {
            DN orgdn = DN.valueOf(orgDN);
            return orgdn.rdn().toString();
        } else {
            // should be realm name
            if (orgDN.startsWith("/")) {
                if (orgDN.trim().equals("/")) {
                    return "/";
                } else if (!orgDN.trim().endsWith("/")) {
                    int loc = orgDN.lastIndexOf("/");
                    return (orgDN.substring(loc + 1).trim());
                } else {
                    // error case, but allow to continue
                    debug.error("getAuthDomainURL.invalid org URL " + orgDN);
                }
            } else {
                // error case, but allow to continue
                debug.error("getAuthDomainURLList invalid org URL " + orgDN);
            }
        }
        return null;
    }

    public static boolean requireAddCookie(HttpServletRequest request) {
        List remoteServiceURLs = FSUtils.getRemoteServiceURLs(request);
        if ((remoteServiceURLs == null) || (remoteServiceURLs.isEmpty())) {
            return false;
        }

        Cookie lbCookie = CookieUtils.getCookieFromReq(request, getlbCookieName());
        return lbCookie == null;
    }

    public static boolean requireRedirect(HttpServletRequest request) {

        // turn off cookie hash redirect by default
        String tmpStr = SystemPropertiesManager.get(
                "com.sun.identity.federation.cookieHashRedirectEnabled");
        if ((tmpStr == null) || (!(tmpStr.equalsIgnoreCase("true")))) {
            return false;
        }

        String redirected = request.getParameter("redirected");
        if (redirected != null) {
            if (debug.messageEnabled()) {
                debug.message("FSUtils.needSetLBCookieAndRedirect: " +
                        " redirected already and lbCookie not set correctly.");
            }
            return false;
        }

        return true;
    }

    /**
     * Detects if a request simply needs loadbalancer cookies adding and to be redirected to
     * be handled elsewhere.
     * <p>
     * Gated on <code>com.sun.identity.federation.cookieHashRedirectEnabled</code>, which is off by
     * default; see {@link #requireAddCookie} and {@link #requireRedirect} for the rest of the gate.
     *
     * @param request The HTTP request in question.
     * @param response The response associated with the request.
     * @param isIDP Whether this entity is acting as an IDP.
     * @return <code>false</code> if the caller still owns the response and has to carry on,
     *     <code>true</code> if the request has been bounced and the caller must return without
     *     writing anything further. A bounce that failed after committing the response also
     *     returns <code>true</code>, see {@link #requireStopAfterFailedBounce}.
     */
    public static boolean needSetLBCookieAndRedirect(HttpServletRequest request, HttpServletResponse response,
                                                     boolean isIDP) {

        if (!requireAddCookie(request)) {
            return false;
        }

        if (debug.messageEnabled()) {
            debug.message("FSUtils.needSetLBCookieAndRedirect:" +
                " lbCookie not set.");
        }

        setlbCookie(request, response);

        if (!requireRedirect(request)) {
            return false;
        }

        String queryString = request.getQueryString();
        StringBuilder reqURLSB = new StringBuilder();
        reqURLSB.append(request.getRequestURL().toString())
            .append("?redirected=1");
        if (queryString != null) {
            reqURLSB.append("&").append(queryString);
        }

        try {
            String reqMethod = request.getMethod();
            if (reqMethod.equals("POST")) {
                String samlMessageName = null;
                String samlMessage = null;
                if (isIDP) {
                    samlMessageName = IFSConstants.SAML_REQUEST;
                    samlMessage = request.getParameter(samlMessageName);
                } else {
                    samlMessageName = IFSConstants.SAML_RESPONSE;
                    samlMessage = request.getParameter(samlMessageName);
                    if (samlMessage == null) {
                        samlMessageName = IFSConstants.SAML_ART;
                        samlMessage = request.getParameter(samlMessageName);
                    }
                }
                if (samlMessage == null) {
                    return false;
                }
                String relayState = request.getParameter(
                        IFSConstants.RELAY_STATE);
                FSUtils.postToTarget(request, response, samlMessageName,
                        samlMessage, IFSConstants.RELAY_STATE, relayState,
                        reqURLSB.toString());
            } else if (reqMethod.equals("GET")) {
                response.sendRedirect(reqURLSB.toString());
            } else {
                return false;
            }
            return true;
        } catch (IOException ioe) {
            return requireStopAfterFailedBounce(response, ioe);
        } catch (SAML2Exception saml2E) {
            return requireStopAfterFailedBounce(response, saml2E);
        }
    }

    /**
     * Reports whether a failed load balancer cookie bounce has to stop the caller.
     * <p>
     * Every caller reads a <code>false</code> from {@link #needSetLBCookieAndRedirect} as "no bounce
     * was needed, carry on", which is only safe while the response is still untouched. The forward
     * to the auto submit JSP can fail after the JSP has already written the SAML message and the
     * container has flushed it - a signed assertion larger than the response buffer plus a client
     * that aborts is enough. Carrying on from there would consume the one-time-use assertion, mint
     * a session, and then call <code>sendError</code> or <code>sendRedirect</code> on a committed
     * response. So once the response is committed the caller is told the request was handled.
     * <p>
     * A forward that failed while its output was still buffered leaves the caller in charge, but the
     * half rendered auto submit form has to go: appending the caller's own page to it would produce
     * one document carrying both, and the leftover
     * <code>&lt;body onload="document.forms[0].submit()"&gt;</code> would submit the stale SAML
     * message. So the buffer is dropped before handing the response back.
     * <p>
     * Only the buffer: <code>reset()</code> would also clear the headers, and by this point
     * {@link #setlbCookie} has already put the load balancer cookie on the response. Dropping that
     * would send the client away without the cookie the whole bounce exists to deliver, so the
     * no-cache headers the sink set are left in place as the lesser cost.
     * <p>
     * The same limit applies to the GET branch, where the failure came out of
     * <code>sendRedirect</code>: the status line and <code>Location</code> it had already set stay
     * on the response, and the servlet API offers no way to remove a header. A caller that carries
     * on there would have its page discarded by a client following the redirect. Reaching that
     * needs <code>sendRedirect</code> to throw without committing, which Tomcat does not do unless
     * the context is configured to send a redirect body.
     *
     * @param response The response associated with the request.
     * @param cause The failure that came out of the forward.
     * @return <code>true</code> if the response is already committed and the caller must stop.
     */
    public static boolean requireStopAfterFailedBounce(HttpServletResponse response, Exception cause) {
        debug.error("Failed to bounce the request through the auto submitting JSP", cause);
        if (response.isCommitted()) {
            return true;
        }
        response.resetBuffer();
        return false;
    }

    /**
     * Gets remote service URLs
     * @param request http request
     * @return remote service URLs
     */
    public static List getRemoteServiceURLs(HttpServletRequest request) {
        String requestURL = request.getScheme() + "://" +
                request.getServerName() + ":" +
                request.getServerPort();
        if (debug.messageEnabled()) {
            debug.message("FSUtils.getRemoteServiceURLs: requestURL = " +
                    requestURL);
        }

        List serverList = null;

        try {
            serverList = SystemConfigurationUtil.getServerList();
            List siteList = SystemConfigurationUtil.getSiteList();
            if (debug.messageEnabled()) {
                debug.message("FSUtils.getRemoteServiceURLs: servers=" +
                    serverList + ", siteList=" + siteList);
            }
            serverList.removeAll(siteList);
            if (debug.messageEnabled()) {
                debug.message("FSUtils.getRemoteServiceURLs: new servers=" +
                    serverList);
            }
        } catch (Exception ex) {
            if (debug.messageEnabled()) {
                debug.message("FSUtils.getRemoteServiceURLs:", ex);
            }
        }
        if (serverList == null) {
            return null;
        }

        List remoteServiceURLs = new ArrayList();
        for(Iterator iter = serverList.iterator(); iter.hasNext();) {
            String serviceURL = (String)iter.next();
            if ((!serviceURL.equalsIgnoreCase(requestURL)) &&
                    (!serviceURL.equalsIgnoreCase(localURL))) {
                remoteServiceURLs.add(serviceURL);
            }
        }

        if (debug.messageEnabled()) {
            debug.message("FSUtils.getRemoteServiceURLs: " +
                    "remoteServiceURLs = " + remoteServiceURLs);
        }
        return remoteServiceURLs;
    }

    /**
     * Sets load balancer cookie.
     * @param response HttpServletResponse object
     */
    public static void setlbCookie(HttpServletRequest request, HttpServletResponse response) {
        String cookieName = getlbCookieName();
        String cookieValue = getlbCookieValue();
        Cookie cookie = null;
        if ((cookieName != null) && (cookieName.length() != 0)) {
            Set<String> domains = SystemConfigurationUtil.getCookieDomainsForRequest(request);
            for (String domain : domains) {
                cookie = CookieUtils.newCookie(cookieName, cookieValue, "/", domain);
                CookieUtils.addCookieToResponse(response, cookie);
            }
        }
    }

    public static String getlbCookieName() {
        return SystemPropertiesManager.get(Constants.AM_LB_COOKIE_NAME,
            "amlbcookie");
    }

    public static String getlbCookieValue() {
        String loadBalanceCookieValue =
            SystemPropertiesManager.get(Constants.AM_LB_COOKIE_VALUE);
        if ((loadBalanceCookieValue == null) ||
            (loadBalanceCookieValue.length() == 0)) {
            if (SystemConfigurationUtil.isServerMode()) {
               try {
                   return SystemConfigurationUtil.getServerID(server_protocol,
                       server_host, int_server_port, server_uri);
               } catch (SystemConfigurationException scex) {
                   debug.error("FSUtils.getlbCookieValue:", scex);
                   return null;
               }
            }
        }
        return loadBalanceCookieValue;
    }

    /**
     * Forwards to the auto submitting JSP so that the SAML message is re-posted to
     * <code>targetURL</code>.
     * <p>
     * IDFF and SAML2 re-post through the very same <code>autosubmitaccessrights.jsp</code>, so this
     * is a thin delegation to {@link SAML2Utils#postToTarget} rather than a second implementation:
     * that JSP renders the values with bare JSP EL, and one sink is what keeps the encoding it
     * needs from drifting away on one of the two paths.
     *
     * @param request The HTTP request in question.
     * @param response The response associated with the request.
     * @param SAMLmessageName Name of the SAML message parameter.
     * @param SAMLmessageValue Value of the SAML message parameter.
     * @param relayStateName Name of the relay state parameter.
     * @param relayStateValue Value of the relay state parameter, may be null.
     * @param targetURL The URL the message is re-posted to.
     * @throws SAML2Exception if the forward fails.
     */
    public static void postToTarget(HttpServletRequest request, HttpServletResponse response,
        String SAMLmessageName, String SAMLmessageValue, String relayStateName,
        String relayStateValue, String targetURL) throws SAML2Exception {

        SAML2Utils.postToTarget(request, response, SAMLmessageName, SAMLmessageValue,
                relayStateName, relayStateValue, targetURL);
    }

}

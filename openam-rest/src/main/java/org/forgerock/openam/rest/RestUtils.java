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
 * Copyright 2012-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.rest;

import static org.forgerock.json.resource.http.HttpUtils.PROTOCOL_VERSION_1;

import jakarta.mail.internet.MimeUtility;
import jakarta.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.delegation.DelegationEvaluator;
import com.sun.identity.delegation.DelegationEvaluatorImpl;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.delegation.DelegationPermission;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.session.util.SessionUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.sm.SMSException;

import org.forgerock.http.header.AcceptApiVersionHeader;
import org.forgerock.http.routing.Version;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.http.HttpUtils;
import org.forgerock.services.context.Context;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A collection of ForgeRock-REST based utility functions.
 */
final public class RestUtils {

    private static final Debug debug = Debug.getInstance("frRest");
    
    private static Map<String, List<String>> adminUserIds = new ConcurrentHashMap<String, List<String>>();

    /**
     * Returns TokenID from headers
     *
     * @param context Context which contains the headers.
     * @return String with TokenID
     */
    static public String getCookieFromServerContext(Context context) {
        return ServerContextHelper.getCookieFromServerContext(context);
    }

    public static boolean isAdmin(Context context) {
        return isAdmin(context, null);
    }

    public static boolean isAdmin(Context context, String role) {
        boolean isAdmin = false;
        try {
            String realm = context.asContext(RealmContext.class).getRealm().asPath();
            SSOToken userSSOToken = SSOTokenManager.getInstance().createSSOToken(getCookieFromServerContext(context));

            String universalId = userSSOToken.getPrincipal().getName();
            List<String> roles = adminUserIds.get(universalId);
            if (roles != null && roles.contains(role)) {
                return true;
            }

            // Simple check to see if user is super user and if so dont need to perform delegation check
            if (SessionUtils.isAdmin(AccessController.doPrivileged(AdminTokenAction.getInstance()), userSSOToken)) {
                return true;
            }

            DelegationEvaluator delegationEvaluator = new DelegationEvaluatorImpl();
            DelegationPermission delegationPermission = new DelegationPermission();
            Map<String, Set<String>> envParams = Collections.<String, Set<String>>emptyMap();
            
            //If the user has delegated admin permissions in the realm they are currently logged in to,
            //they have read access to global-config endpoints
            if (isGlobalRole(role)) {
                delegationPermission.setVersion("1.0");
                delegationPermission.setConfigType("organizationconfig");
                delegationPermission.setOrganizationName(realm);
                delegationPermission.setServiceName("sunAMRealmService");
                delegationPermission.setActions(CollectionUtils.asSet("DELEGATE"));
                isAdmin = delegationEvaluator.isAllowed(userSSOToken, delegationPermission, envParams);
                if (!isAdmin) {
                    return false;
                }
            } else {
                delegationPermission.setConfigType(null);
                delegationPermission.setVersion("*");
                delegationPermission.setSubConfigName("default");
                delegationPermission.setOrganizationName(realm);
                delegationPermission.setActions(CollectionUtils.asSet("READ"));

                for (Iterator i = getServiceNames(role).iterator(); i.hasNext(); ) {
                    String name = (String) i.next();
                    delegationPermission.setServiceName(name);
                    isAdmin = delegationEvaluator.isAllowed(userSSOToken, delegationPermission, envParams);
                    if (!isAdmin) {
                        // If the user lacks any of the permissions to read realms or global config,
                        // then we should not give them the role in question
                        return false;
                    }
                }
            }

            if (roles == null) {
                roles = new ArrayList<String>();
            }
            roles.add(role);
            adminUserIds.put(universalId, roles);
            return isAdmin;

        } catch (DelegationException | SSOException | SMSException e) {
            debug.error("RestUtils::Failed to determine if user is an admin", e);
            adminUserIds.clear();
        }

        return isAdmin;
    }

    private static Set<String> getServiceNames(String role) throws SMSException, SSOException {
        if (isGlobalRole(role)) {
            return LegacyUIConfigHolder.globalService;
        }
        return LegacyUIConfigHolder.realmService;
    }
    
    private static boolean isGlobalRole(String role) {
        if (StringUtils.isNotEmpty(role) && role.contains("global")) {
            return true;
        }
        return false; 
    }

    static public void hasPermission(final Context context) throws SSOException, IdRepoException, ForbiddenException {
        SSOTokenManager mgr = SSOTokenManager.getInstance();
        SSOToken ssotok = mgr.createSSOToken(getCookieFromServerContext(context));
        mgr.validateToken(ssotok);
        mgr.refreshSession(ssotok);
        AMIdentity amIdentity = new AMIdentity(ssotok);

        if (!(amIdentity.equals(AdminUserIdHolder.superAdminUserId))) {
            debug.error("Unauthorized user.");
            throw new ForbiddenException("Access Denied");
        }
    }

    /**
     * Signals to the handler that the current operation is unsupported.
     */
    public static <T> Promise<T, ResourceException> generateUnsupportedOperation() {
        return new NotSupportedException("Operation is not supported.").asPromise();
    }

    /**
     * Signals to the handler that the request was invalid for this endpoint.
     */
    public static <T> Promise<T, ResourceException> generateBadRequestException() {
        return generateBadRequestException("Bad request.");
    }

    /**
     * Signals to the handler that the request was invalid for this endpoint.
     */
    public static <T> Promise<T, ResourceException> generateBadRequestException(String msg) {
        return new BadRequestException(msg).asPromise();
    }

    /**
     * Signals to the handler that the requested resource was not found.
     */
    public static <T> Promise<T, ResourceException> generateNotFoundException(Request request) {
        return new NotFoundException("Resource '" + request.getResourcePath() + "' not found").asPromise();
    }

    /**
     * Parses out deployment url
     * @param deploymentURL
     */
    public static StringBuilder getFullDeploymentURI(final String deploymentURL) throws URISyntaxException{

        // get URI
        String deploymentURI = null;
        URI uriHold = new URI(deploymentURL);
        String uri = uriHold.getPath();
        //Parse out the deployment URI
        int firstSlashIndex = uri.indexOf("/");
        int secondSlashIndex = uri.indexOf("/", firstSlashIndex + 1);
        if (secondSlashIndex != -1) {
            deploymentURI = uri.substring(0, secondSlashIndex);
        }
        //Build string that consist of protocol,host,port, and deployment uri
        StringBuilder fullDepURL = new StringBuilder(100);
        fullDepURL.append(uriHold.getScheme()).append("://")
                .append(uriHold.getHost()).append(":")
                .append(uriHold.getPort())
                .append(deploymentURI);
        return fullDepURL;
    }

    /**
     * Gets an SSOToken for Administrator
     * @return
     */
    public static SSOToken getToken() {
        return AccessController.doPrivileged(AdminTokenAction.getInstance());
    }

    /**
     * Returns the value of the named header field from the request, decoding it if it is mime-encoded. If the header
     * is not mime-encoded then it is returned as-is. If no such header is present, then {@code null} is returned. If
     * there are multiple values for the header, then the first value is returned.
     *
     * @param serverContext the context of the request. Must contain a {@link HttpContext}.
     * @param headerName the name of the header to get.
     * @return the decoded header value, or {@code null} if no such header exists in the request.
     *
     * @see <a href="https://tools.ietf.org/html/rfc2047">RFC 2047: MIME Part 3: Message Header Extensions for Non-ASCII
     * Text</a>
     */
    public static String getMimeHeaderValue(final Context serverContext, final String headerName) {
        final HttpContext httpContext = serverContext.asContext(HttpContext.class);
        final String headerValue = httpContext.getHeaderAsString(headerName);
        try {
            return headerValue == null ? null : MimeUtility.decodeText(headerValue);
        } catch (UnsupportedEncodingException ex) {
            if (debug.warningEnabled()) {
                debug.warning("Unable to decode mime header: " + ex);
            }
            return headerValue;
        }
    }

    /**
     * Returns the value of the named header field from the request, decoding it if it is mime-encoded. If the header
     * is not mime-encoded then it is returned as-is. If no such header is present, then {@code null} is returned.If
     * there are multiple values for the header, then the first value is returned.
     * @param request HTTPServletRequest
     * @param headerName the name of the header to get.
     * @return the decoded header value, or {@code null} if no such header exists in the request.
     *
     * @see <a href="https://tools.ietf.org/html/rfc2047">RFC 2047: MIME Part 3: Message Header Extensions for Non-ASCII
     * Text</a>
     */
    public static String getMimeHeaderValue(HttpServletRequest request, final String headerName) {
        final String headerValue = request.getHeader(headerName);
        try {
            return headerValue == null ? null : MimeUtility.decodeText(headerValue);
        } catch (UnsupportedEncodingException ex) {
            if (debug.warningEnabled()) {
                debug.warning("Unable to decode mime header: " + ex);
            }
            return headerValue;
        }
    }

    /**
     * Get the CREST protocol version from the content.
     * @param context The request context.
     * @return The protocol version.
     */
    public static Version crestProtocolVersion(Context context) {
        Reject.ifFalse(context.containsContext(HttpContext.class), "Context does not contain an HttpContext");
        String versionHeader = context.asContext(HttpContext.class).getHeaderAsString(AcceptApiVersionHeader.NAME);
        if (versionHeader == null) {
            return HttpUtils.DEFAULT_PROTOCOL_VERSION;
        }
        Version requestedProtocolVersion = AcceptApiVersionHeader.valueOf(versionHeader).getProtocolVersion();
        return requestedProtocolVersion == null ? HttpUtils.DEFAULT_PROTOCOL_VERSION : requestedProtocolVersion;
    }

    /**
     * Check to see if the create request should be treated as a client-provided ID create request in a CREST
     * contract-conformant way.
     * <p>
     *     When handling CREST create requests with protocol 1.0, OpenAM returned a 'Conflict' error response regardless
     *     of whether the resource's ID was client-specified or server-generated. In the case of the former, a create
     *     request was supposed to always have an {@code if-none-match} header (note: OpenAM functional tests generally
     *     did not conform to this either). This being the case, the correct response would be precondition-failed,
     *     rather than conflict.
     * </p>
     * <p>
     *     As OpenAM will have to honour its incorrect behaviour for pre-OpenAM 14 clients, this method can be used to
     *     distinguish a request that should be handled in a conformant way rather than one that should be handled in
     *     the legacy way. New resource providers should <i>not</i> use this method, but should correctly handle all
     *     client-provided ID request (where {@link CreateRequest#getNewResourceId()} return a non-null value) properly.
     * </p>
     * @param serverContext The context for the request.
     * @param createRequest The request object.
     * @return How the request should be handled.
     */
    public static boolean isContractConformantUserProvidedIdCreate(Context serverContext, CreateRequest createRequest) {
        return createRequest.getNewResourceId() != null
                && crestProtocolVersion(serverContext).compareTo(PROTOCOL_VERSION_1) > 0;
    }

    /**
     * Lazy initialization holder idiom.
     */
    private static final class AdminUserIdHolder {
        static final AMIdentity superAdminUserId;

        static {
            final SSOToken adminToken = getToken();
            final String adminUser = SystemProperties.get(Constants.AUTHENTICATION_SUPER_USER);
            if (adminUser != null) {
                superAdminUserId = new AMIdentity(adminToken, adminUser, IdType.USER, "/", null);
            } else {
                superAdminUserId = null;
                debug.error("SystemProperties AUTHENTICATION_SUPER_USER not set");
            }
        }
    }

    /**
     * This inner class loads service names required for accessing tabs on JATO based admin console.
     */
    private static final class LegacyUIConfigHolder {
        private static final String CONFIG_FILENAME = "/amConsoleConfig.xml";

        private static Set<String> globalService = new HashSet<String>();
        private static Set<String> realmService = new HashSet<String>();

        // Until JATO is completely replaced with XUI, we are going to base the permission behavior on old JATO UI. 
        // Since AMViewConfig doesn't reflect changes to amConsoleConfig.xml dynamically,
        // we are only loading services list once.
        static {
            Document doc = parseDocument(CONFIG_FILENAME);
            if (doc != null) {
                loadLegacyConsoleConfig(doc);
            }
        }

        /**
         * parse loaded amConsoleConfig.xml to get the list of service names 
         */
        private static void loadLegacyConsoleConfig(Document doc) {
            NodeList nodes = doc.getElementsByTagName("tabs");
            if ((nodes == null) || (nodes.getLength() != 1)) {
                debug.error("RestUtils.loadLegacyConsoleConfig(): failed to load tab config");
                return;
            }

            Node root = nodes.item(0);
            NodeList children = root.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeName().equalsIgnoreCase("tab")) {
                    NamedNodeMap attrs = child.getAttributes();
                    if (attrs != null) {
                        List<String> accessLevels = getAttributes(attrs, "accesslevel");
                        List<String> serviceNames = getAttributes(attrs, "permissions");
                        if (CollectionUtils.isEmpty(accessLevels)) {
                            realmService.addAll(serviceNames);
                        } else {
                            globalService.addAll(serviceNames);
                        }
                    }
                }
            }
        }

        /**
         * read accessLevel and permissions for each service names on a tab
         */
        private static List<String> getAttributes(NamedNodeMap attrs, String attrName) {
            Node nodeID = attrs.getNamedItem(attrName);
            if (nodeID != null) {
                String value = nodeID.getNodeValue().trim();
                if (StringUtils.isNotEmpty(value)) {
                    List<String> values = Arrays.asList(value.split(","));
                    return values;
                }
            }
            return Collections.EMPTY_LIST;
        }

        /**
         * read amConsoleConfig.xml from classpath
         */
        private static Document parseDocument(String fileName) {
            Document document = null;
            try (InputStream is = RestUtils.class.getClassLoader().getResourceAsStream(fileName)) {
                DocumentBuilder documentBuilder = XMLUtils.getSafeDocumentBuilder(false);
                document = documentBuilder.parse(is);
            } catch (Exception e) {
                debug.error("RestUtils.parseDocument", e);
            }
            return document;
        }
    }
}

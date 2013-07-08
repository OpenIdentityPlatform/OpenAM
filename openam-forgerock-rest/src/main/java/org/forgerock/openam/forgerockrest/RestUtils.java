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
 * Copyright 2012 ForgeRock Inc.
 */
package org.forgerock.openam.forgerockrest;


import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idsvcs.Token;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.servlet.HttpContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.util.List;
import java.util.Map;


/**
 * A collection of ForgeRock-REST based utility functions.
 *
 * @author alin.brici@forgerock.com
 * @author robert.wapshott@forgerock.com
 */
final public class  RestUtils {

    private static final SSOToken token;
    private static final String adminUser;
    private static final AMIdentity adminUserId;

    static {
        token = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
        adminUser = SystemProperties.get(Constants.AUTHENTICATION_SUPER_USER);

        if (adminUser != null) {
            adminUserId = new AMIdentity(token,
                    adminUser, IdType.USER, "/", null);
        } else {
            adminUserId = null;
            RestDispatcher.debug.error("SystemProperties AUTHENTICATION_SUPER_USER not set");
        }
    }

    /**
     * Returns TokenID from headers
     *
     * @param context ServerContext which contains the headers.
     * @return String with TokenID
     */
    static public String getCookieFromServerContext(ServerContext context) {
        List<String> cookies = null;
        String cookieName = null;
        HttpContext header = null;
        try {
            cookieName = SystemProperties.get("com.iplanet.am.cookie.name");
            if (cookieName == null || cookieName.isEmpty()) {
                RestDispatcher.debug.error("IdentityResource.getCookieFromServerContext() :: " +
                        "Cannot retrieve SystemProperty : com.iplanet.am.cookie.name");
                return null;
            }
            header = context.asContext(HttpContext.class);
            if (header == null) {
                RestDispatcher.debug.error("IdentityResource.getCookieFromServerContext() :: " +
                        "Cannot retrieve ServerContext as HttpContext");
                return null;
            }
            //get the cookie from header directly   as the name of com.iplanet.am.cookie.am
            cookies = header.getHeaders().get(cookieName.toLowerCase());
            if (cookies != null && !cookies.isEmpty()) {
                for (String s : cookies) {
                    if (s == null || s.isEmpty()) {
                        return null;
                    } else {
                        return s;
                    }
                }
            } else {  //get cookie from header parameter called cookie
                cookies = header.getHeaders().get("cookie");
                if (cookies != null && !cookies.isEmpty()) {
                    for (String cookie : cookies) {
                        String cookieNames[] = cookie.split(";"); //Split parameter up
                        for (String c : cookieNames) {
                            if (c.contains(cookieName)) { //if com.iplanet.am.cookie.name exists in cookie param
                                String amCookie = c.replace(cookieName + "=", "").trim();
                                return amCookie; //return com.iplanet.am.cookie.name value
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            RestDispatcher.debug.error("IdentityResource.getCookieFromServerContext() :: " +
                    "Cannot get cookie from ServerContext!" + e);
        }
        return null;
    }

    static public boolean isAdmin(final ServerContext context){

        Token admin = new Token();
        admin.setId(getCookieFromServerContext(context));
        SSOToken ssotok = null;
        AMIdentity amIdentity = null;

        try {
            SSOTokenManager mgr = SSOTokenManager.getInstance();
            ssotok = mgr.createSSOToken(getCookieFromServerContext(context));
            amIdentity = new AMIdentity(ssotok);

            if (!(amIdentity.equals(adminUserId))){
                RestDispatcher.debug.error("Unauthorized user.");
                return false;
            }
            return true;
        } catch (SSOException e) {
            RestDispatcher.debug.error("IdentityResource.idFromSession() :: Cannot retrieve SSO Token: " + e);
        } catch (IdRepoException ex) {
            RestDispatcher.debug.error("IdentityResource.idFromSession() :: Cannot retrieve user from IdRepo" + ex);
        }
        return false;
    }
    static public void hasPermission(final ServerContext context) throws SSOException, IdRepoException, ForbiddenException {
        //Checks to see if User is amadmin, currently only amAdmin can access realms
        Token admin = new Token();
        admin.setId(getCookieFromServerContext(context));
        SSOToken ssotok = null;
        AMIdentity amIdentity = null;

        SSOTokenManager mgr = SSOTokenManager.getInstance();
        ssotok = mgr.createSSOToken(getCookieFromServerContext(context));
        mgr.validateToken(ssotok);
        mgr.refreshSession(ssotok);
        amIdentity = new AMIdentity(ssotok);

        if (!(amIdentity.equals(adminUserId))){
            RestDispatcher.debug.error("Unauthorized user.");
            throw new ForbiddenException("Access Denied");
        }
    }

    /**
     * Signals to the handler that the current operation is unsupported.
     *
     * @param handler Non null handler.
     */
    public static void generateUnsupportedOperation(ResultHandler handler) {
        NotSupportedException exception = new NotSupportedException("Operation is not supported.");
        handler.handleError(exception);
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
        return token;
    }
}

/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ConsoleServletBase.java,v 1.7 2009/03/24 23:57:32 babysunil Exp $
 *
 */

/*
 * Portions Copyrighted 2011-2012 ForgeRock AS
 */
package com.sun.identity.console.base;


import com.iplanet.am.util.BrowserEncoding;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.jato.ApplicationServletBase;
import com.iplanet.jato.CompleteRequestException;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.ViewBeanManager;
import com.iplanet.jato.view.ViewBean;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.common.FQDNUtils;
import com.sun.identity.common.RequestUtils;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import org.owasp.esapi.ESAPI;


/**
 * This is the base class for all the module servlets. The basic function of 
 * this class is to perform authentication check. If the user is not 
 * authenticated, then he is redirected to the login page. If the user is 
 * entering for the first time after login, then he is redirected to the 
 * <code>AMAdminFrame</code>. This servlet will do nothing if the user  has a
 * valid SSO Token.
 * Every module servlet in console must extend from this servlet.
 */
public abstract class ConsoleServletBase
    extends ApplicationServletBase
{
    static final String PARAM_REDIRECT = "amconsoleRedirect";
    static final String URL_ADMIN_FRAME = "/base/AMAdminFrame";
    static final String LOGIN_PARAM = "?service=adminconsoleservice&goto=";

    public ConsoleServletBase() {
        // Disable the "strict session timeouts" warnings
        // in the server container log.
        setEnforceStrictSessionTimeout(true);
    }

    /**
     * Forwards request to login view bean if user has not logged in.
     *
     * @param requestContext request context.
     * @throws ServletException if redirection fails.
     */
    protected void onBeforeRequest(RequestContext requestContext)
        throws ServletException
    {
        HttpServletRequest req = requestContext.getRequest();

        String host = req.getHeader("Host");

        if (host == null) {
            /*
             * This case will not happen, unless the user agent does not set
             * this header while making this connection.
             */
            host = getConsoleHost();
        } else {
            String validHost = validateHost(host);
            if (validHost != null) {
                try {
                    requestContext.getResponse().sendRedirect(
                        replaceHostNameInUrl(req, validHost));
                } catch (IOException ioe) {
                    getDebug().error("ConsoleServletBase.onBeforeRequest, " +
                        "failed to redirect to fully qualified host");
                }
                throw new CompleteRequestException();
            }
        }

        validateSSOToken(requestContext);
    }

    private String replaceHostNameInUrl(
        HttpServletRequest req,
        String newHostName
    ) {
        StringBuilder buff = new StringBuilder(1024);
        String protocol = RequestUtils.getRedirectProtocol(
            req.getScheme(), newHostName);
        buff.append(protocol)
            .append("://")
            .append(newHostName)
            .append(req.getRequestURI());

        String queryString = req.getQueryString();

        if (queryString != null) {
            buff.append("?")
                .append(queryString);
        }

        return buff.toString();
    }

    private void validateSSOToken(RequestContext requestContext)
        throws ServletException
    {
        try {
            /*
             * Since all supported web containers now support
             * servlet 2.3 and above, we use setCharacterEncoding
             * to set request charset.
             */

            HttpServletRequest req = requestContext.getRequest();
            SSOToken token = checkAuthentication(req);
            String enc = token.getProperty("CharSet");

            try {
                String jCharset = BrowserEncoding.mapHttp2JavaCharset(enc);
                req.setCharacterEncoding(jCharset);
            } catch (UnsupportedEncodingException ex) {
                getDebug().error("ConsoleServletBase.validateSSOToken " +
                    "Unsupported encoding", ex);
            }
         } catch (SSOException soe) {
            browserRedirect(requestContext,
                formGotoUrl(requestContext.getRequest()));
            throw new CompleteRequestException();
        }
    }

    private void browserRedirect(RequestContext requestContext, String url) {
        ViewBeanManager mgr = requestContext.getViewBeanManager();
        AMLoginViewBean vb = (AMLoginViewBean)mgr.getViewBean(
            AMLoginViewBean.class);
        if (getDebug().messageEnabled()) {
            getDebug().message("ConsoleServletBase.browserRedirect " +
                "redirecting unauthenticated user to " + url);
        }
        vb.setLoginURL(url);
        vb.forwardTo(requestContext);
    }

    /**
     * Ignores HTTP session time out.  Console uses SSO Token Session.
     *
     * @param requestContext - The JATO request context.
     */
    protected void onSessionTimeout(RequestContext requestContext)
        throws ServletException {
        // do nothing
    }    

    /**
     * Forwards to invalid URL view bean, in case of an invalid target 
     * request handler (page).
     *
     * @param requestContext - request context
     * @param handlerName - name of handler
     * @throws ServletException
     */
    protected void onRequestHandlerNotFound(
        RequestContext requestContext,
        String handlerName)
        throws ServletException
    {
        ViewBeanManager viewBeanManager = requestContext.getViewBeanManager();
        ViewBean targetView = viewBeanManager.getViewBean(
            AMInvalidURLViewBean.class);
        targetView.forwardTo(requestContext);
        throw new CompleteRequestException();
    }

    /**
     * Forwards to invalid URL view bean, in case of no handler specified
     *
     * @param requestContext - request context
     * @throws ServletException
     */
    protected void onRequestHandlerNotSpecified(RequestContext requestContext)
        throws ServletException
    {
        ViewBeanManager viewBeanManager = requestContext.getViewBeanManager();
        ViewBean targetView = viewBeanManager.getViewBean(
            AMInvalidURLViewBean.class);
        targetView.forwardTo(requestContext);
        throw new CompleteRequestException();
    }

    @Override
    protected void onPageSessionDeserializationException(
            RequestContext requestContext,
            ViewBean viewBean,
            Exception e)
            throws ServletException, IOException {
        //redirect, since forwardTo would carry the invalid pagesession
        requestContext.getResponse().sendRedirect("../base/AMInvalidURL");
        throw new CompleteRequestException();
    }

    /**
     * Forwards to uncaught exception view bean, to respond to uncaught 
     * application error messages.
     *
     * @param requestContext - request context
     * @param e Exception that was not handled by the application.
     * @throws ServletException
     * @throws IOException
     */
    protected void onUncaughtException(
        RequestContext requestContext,
        Exception e)
        throws ServletException, IOException
    {
        getDebug().error("ConsoleServletBase.onUncaughtException", e);
        requestContext.getResponse().sendRedirect(
            "../base/AMUncaughtException");
    }

    /**
     * Checks if the user is authenticated, that is, if SSO Token is available
     * and whether the token is still valid, else throws
     * <code>SSOException</code>. 
     *
     * @param request  HTTP Servlet request object.
     * @return The valid SSO Token.
     * @throws SSOException if SSO Token is invalid
     */
    private SSOToken checkAuthentication(HttpServletRequest request) 
        throws SSOException
    {
        SSOTokenManager manager = SSOTokenManager.getInstance();
        SSOToken ssoToken = manager.createSSOToken(request);
        manager.validateToken(ssoToken);
        return ssoToken;
    } 

    /**
     * Return appropriate redirect URL if the SSO is failed
     *
     * @param request HTTP Servlet request
     */  
    private String formGotoUrl(HttpServletRequest request) {
        StringBuilder redirectURL = new StringBuilder(2048);
        String host = request.getHeader("Host");

        if (host == null) {
            getDebug().message(
                "ConsoleServletBase.formGotoURL Host header is null.");
            /*
             * This case will not happen, unless the user agent does not
             * set this header while making this connection.
             */
            host = getConsoleHost();
        }

        String loginURL = SystemProperties.get(Constants.LOGIN_URL);

        if ((loginURL != null) && (loginURL.trim().length() > 0)) {
            redirectURL.append(loginURL);
        } else {
            if (isConsoleRemote()) {
                redirectURL.append(getServerURL())
                    .append(getServerURI())
                    .append(AMAdminConstants.URL_LOGIN);
            } else {
                String protocol = RequestUtils.getRedirectProtocol(
                    request.getScheme(), host);
                redirectURL.append(protocol)
                    .append("://")
                    .append(host)
                    .append(getServerURI())
                    .append(AMAdminConstants.URL_LOGIN);
            }
        }

        redirectURL.append(LOGIN_PARAM);

        if (isConsoleRemote()) {
            redirectURL.append(getConsoleURL());
        } else {
            String protocol = RequestUtils.getRedirectProtocol(
                request.getScheme(), host);
            redirectURL.append(protocol)
                .append("://")
                .append(host);
        }

        redirectURL.append(getConsoleURI())
            .append(URL_ADMIN_FRAME);

        /*
         * We only append query parameters are passed from amconsole URI. e.g.
         * http://<host>.<domain>:<port>/amconsole?org=dc%3Diplanet%2Cdc%3Dcom
         * should be append to this URL. We use a query parameter,
         * amconsoleRedirect to track this kind of parameter.
         */
        String amconsoleRedirect = request.getParameter(PARAM_REDIRECT);

        if ((amconsoleRedirect != null) && amconsoleRedirect.equals("1")) {
            String queryParam = getQueryParameters(request);
            redirectURL.append(queryParam);

            /** TBD
            if (!containOrgDomainParam(queryParam)) {
                String orgParam = getOrganizationQueryParam(request,host);

                if (orgParam != null) {
                    redirectURL.append(orgParam);
                }
            } */
        }

        return redirectURL.toString();
    }

    private String validateHost(String host) {
        String validHostname = null;
        String hostname = null;
        String port = null;
        int idx = host.indexOf(':');

        if (idx != -1) {
            port = host.substring(idx+1);
            hostname = host.substring(0, idx);
        } else {
            hostname = host;
        }

        if (!hostname.equalsIgnoreCase(getConsoleHost())) {
            hostname = FQDNUtils.getInstance().getFullyQualifiedHostName(
                hostname);

            if (hostname != null) {
                /*
                 * this required because FQDNUtils default hostname to
                 * server host. for the case of remote console 
                 * installation, default should be console host.
                */
                if (isConsoleRemote() && (hostname.equals(getServerHost()))) {
                    hostname = getConsoleHost();
                }

                validHostname = (port != null) ?
                    hostname + ":" + port : hostname;
            }
        }

        return validHostname;
    }

    /**
     * Returns the query string portion of the URL.  Example, 
     * <code>http://<host>:<port>/amconsole/?org=iplanet</code>, This method
     * returns <code>&org=iplanet</code>.  This method returns empty string
     * if there is no query string.
     *
     * @param request HTTP Servlet Request.
     * @return query string portion of the URL.
     */
    private String getQueryParameters(HttpServletRequest request) {
        String queryString = request.getQueryString();

        if ((queryString != null) && (queryString.length() > 0)) {
            int index = queryString.indexOf(PARAM_REDIRECT);

            if (index != -1) {
                String x = queryString.substring(0, index);
                if ( queryString.length() >
                     index + PARAM_REDIRECT.length() + 3 ) {
                    x += queryString.substring(
                           index + PARAM_REDIRECT.length() + 3);
                }
                queryString = x;
            }
            boolean isValid = ESAPI.validator().isValidInput("AMLogin_params", queryString, "HTTPURI", 1024, true);
            return isValid ? "&" + queryString : "";
        }

        return (queryString != null) ? queryString : "";
    }
    
    private static String getConsoleHost() {
        return SystemProperties.get(Constants.AM_CONSOLE_HOST);
    }
    
    private static String getServerHost() {
        return SystemProperties.get(Constants.AM_SERVER_HOST);
    }
    
    private static String getConsoleURI() {
        String uri = SystemProperties.get(
            Constants.AM_CONSOLE_DEPLOYMENT_DESCRIPTOR);
        if ((uri != null) && !uri.startsWith("/")) {
            uri = "/" + uri;
        }
        return uri;
    }

     private static String getServerURI() {
        String uri = SystemProperties.get(
            Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
        if ((uri != null) && !uri.startsWith("/")) {
            uri = "/" + uri;
        }
        return uri;
    }

    private static String getConsoleURL() {
        return SystemProperties.get(Constants.AM_CONSOLE_PROTOCOL) + "://" +
            getConsoleHost() + ":" + 
            SystemProperties.get(Constants.AM_CONSOLE_PORT);
    }

    private static String getServerURL() {
        return SystemProperties.get(Constants.AM_SERVER_PROTOCOL) + "://" +
            getServerHost() + ":" + 
            SystemProperties.get(Constants.AM_SERVER_PORT);
    }

    
    private static boolean isConsoleRemote() {
        return Boolean.valueOf(
          SystemProperties.get(Constants.AM_CONSOLE_REMOTE)).booleanValue();
    }
    
    private static Debug getDebug() {
        return Debug.getInstance(AMAdminConstants.CONSOLE_DEBUG_FILENAME);
    }
}

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
 */

package com.sun.identity.federation.common;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;

import com.sun.identity.common.SystemConfigurationException;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.common.SAML2Exception;
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
    public static IDFFMetaManager metaInstance = null;    

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
     * Test if url in argument is
     *  in  the same web container as current opensso web
     * apps serving the request.
     * @param request HttpServletRequest
     * @param url
     * @return true if request and url are in the same web container else false
     */
    public static boolean isSameContainer(
            HttpServletRequest request,
            String url) {

        boolean result = false;
        FSUtils.debug.message("FSUtils.isSameContainer: called");

        try {
            //get source host and port
            String sourceHost = request.getServerName();
            int sourcePort = request.getServerPort();
            if (debug.messageEnabled()) {
		FSUtils.debug.message("FSUtils.isSameContainer: " +
                    "SourceHost=" + sourceHost + " SourcePort=" + sourcePort);
 	    }
            //get target host and port
            URL target = new URL(url);
            String targetHost = target.getHost();
            int targetPort = target.getPort();
            if (debug.messageEnabled()) {
		FSUtils.debug.message("FSUtils.isSameContainer: targetHost=" + 
			targetHost + " targetPort=" + targetPort);
            }
            int index = url.indexOf(deploymentURI + "/");
            if (!(sourceHost.equals(targetHost)) ||
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
        } catch (Exception ex) {
            FSUtils.debug.error("FSUtils.isSameContainer: Exception occured", ex);
        }
        return result;
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
        try {
            SessionProvider sessionProvider = SessionManager.getProvider();
            Object token = sessionProvider.getSession(request);
            if ((token != null) && (sessionProvider.isValid(token))) {
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

        try {
            //get source host and port
            String sourceHost = request.getServerName();            
            int sourcePort = request.getServerPort();
            FSUtils.debug.message("FSUtils.forwardRequest: " +
                "SourceHost=" + sourceHost + " SourcePort="+ sourcePort);
            //get target host and port
            URL target = new URL(newUrl);
            String targetHost = target.getHost();
            int targetPort = target.getPort();            
            FSUtils.debug.message("FSUtils.forwardRequest: targetHost=" 
                + targetHost + " targetPort=" + targetPort);
 
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
            if( !(sourceHost.equals(targetHost)) || 
                !(sourcePort == targetPort) || 
                !(index > 0) || isWebSphere)
            {
                FSUtils.debug.message("FSUtils.forwardRequest: Source and " +
                    "Target are not on the same container." + 
                    "Redirecting to target");            
                response.sendRedirect(newUrl);
                return;
            } else {      
                String resource = newUrl.substring(
                    index + deploymentURI.length());
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSUtils.forwardRequest: Forwarding to :" + resource);
                }  
                RequestDispatcher dispatcher = 
                    request.getRequestDispatcher(resource);
                try {
                    dispatcher.forward(request, response);
                } catch (Exception e) {
                    FSUtils.debug.error("FSUtils.forwardRequest: Exception "
                        + "occured while trying to forward to resource:" +
                        resource , e);
                }
            } 
        } catch (Exception ex) {
            FSUtils.debug.error("FSUtils.forwardRequest: Exception occured",ex);
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
        if (metaInstance == null){
            synchronized (IDFFMetaManager.class) {
                try {
                    // TODO: generate admin session and pass it in
                    metaInstance = new IDFFMetaManager(null);
                    return metaInstance;
                } catch (Exception e) {
                    FSUtils.debug.error ("FSUtils.getIDFFMetaManager:"
                        + " Could not create meta Manager", e);
                    return null;
                }
            }
        }
        return metaInstance;
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
     *
     * @param request The HTTP request in question.
     * @param response The response associated with the request.
     * @param isIDP Whether this entity is acting as an IDP.
     * @return false if not, otherwise redirects.
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
            debug.error("FSUtils.needSetLBCookieAndRedirect: ", ioe);
        } catch (SAML2Exception saml2E) {
            debug.error("FSUtils.needSetLBCookieAndRedirect: ", saml2E);
        }
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

    public static void postToTarget(HttpServletRequest request, HttpServletResponse response,
        String SAMLmessageName, String SAMLmessageValue, String relayStateName,
        String relayStateValue, String targetURL) throws SAML2Exception {

        request.setAttribute("TARGET_URL", targetURL);
        request.setAttribute("SAML_MESSAGE_NAME", SAMLmessageName);
        request.setAttribute("SAML_MESSAGE_VALUE", SAMLmessageValue);
        request.setAttribute("RELAY_STATE_NAME", relayStateName);
        request.setAttribute("RELAY_STATE_VALUE", relayStateValue);
        request.setAttribute("SAML_POST_KEY", bundle.getString("samlPostKey"));

        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache,no-store");

        try {
            request.getRequestDispatcher("/saml2/jsp/autosubmitaccessrights.jsp").forward(request, response);
        } catch (ServletException sE) {
            handleForwardException(sE);
        } catch (IOException ioE) {
            handleForwardException(ioE);
        }
    }

    /**
     * Handles any exception when attempting to forward.
     *
     * @param exception
     *         Thrown and caught exception
     * @throws SAML2Exception
     *         Single general exception that is thrown on
     */
    private static void handleForwardException(Exception exception) throws SAML2Exception {
        debug.error("Failed to forward to auto submitting JSP", exception);
        throw new SAML2Exception(bundle.getString("postToTargetFailed"));
    }

}

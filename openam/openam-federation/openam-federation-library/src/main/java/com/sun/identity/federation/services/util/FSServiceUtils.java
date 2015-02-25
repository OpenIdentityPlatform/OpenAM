/**
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
 * $Id: FSServiceUtils.java,v 1.11 2008/11/10 22:56:59 veiming Exp $
 *
 */
/**
 * Portions Copyrighted 2012-2014 ForgeRock AS
 */
package com.sun.identity.federation.services.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import com.sun.identity.common.SystemConfigurationException;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.federation.accountmgmt.FSAccountFedInfo;
import com.sun.identity.federation.accountmgmt.FSAccountManager;
import com.sun.identity.federation.accountmgmt.FSAccountMgmtException;
import com.sun.identity.federation.common.FSException;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.plugins.FederationSPAdapter;
import com.sun.identity.federation.services.FSSPAuthenticationContextInfo;
import com.sun.identity.liberty.ws.meta.jaxb.AffiliationDescriptorType;
import com.sun.identity.liberty.ws.meta.jaxb.IDPDescriptorType;
import com.sun.identity.liberty.ws.meta.jaxb.SPDescriptorType;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.xml.XMLUtils;
import org.w3c.dom.Node;

/**
 * Util class to provide methods to manage ID-FF service.
 */
public class FSServiceUtils {
    
    private static IDFFMetaManager metaManager = null;
    private static MessageFactory fac = null;
    private static List cookieList = null;
    private static boolean signingOn = false;
    private static boolean signingOptional = false;
    private static final String templatePath =
        Constants.FILE_SEPARATOR + IFSConstants.CONFIG_DIR +
        Constants.FILE_SEPARATOR + IFSConstants.FEDERATION_DIR;


    static {
        try {
            fac = MessageFactory.newInstance ();
        } catch (Exception ex) {
            FSUtils.debug.error ("FSServiceUtils::static block) "+
                "could not get factory instance");
            ex.printStackTrace ();
        }
        try {
            cookieList = SystemConfigurationUtil.getCookieDomains();
        } catch (SystemConfigurationException se) {
            FSUtils.debug.error ("FSServiceUtils::staticBlock " +
                "SystemConfigurationException while reading", se);
        }

        String signing = SystemConfigurationUtil.getProperty(
            "com.sun.identity.federation.services.signingOn", "optional");
        if (signing.equalsIgnoreCase(IFSConstants.TRUE)) {
            signingOn = true;
        } else if (signing.equalsIgnoreCase(IFSConstants.OPTIONAL)) {
            signingOptional = true;
        }
        metaManager = FSUtils.getIDFFMetaManager();

    };

    // constructor
    private FSServiceUtils () {
    }
    
    /**
     * Returns <code>true</code> if signing is enabled; otherwise, it will
     * return false. If signing is enabled, all the liberty requests/responses
     * must be signed/verfied.
     * @return <code>true</code> if signing is on; otherwise, return
     *  <code>false</code>
     */
    public static boolean isSigningOn () {
        return signingOn;
    }
    
    /**
     * Returns <code>true</code> if signing is optional else it will return
     * <code>false</code>. If signing is optional, sign/verfy 
     * <code>Response/Assertion</code> only if it is required by the
     * specification.
     * @return <code>true</code> if signing is optional; otherwise return
     *  <code>false</code>
     */
    public static boolean isSigningOptional () {
        return signingOptional;
    }
    
    /**
     * Returns common login page URL based on the deployment descriptor and
     * the meta alias associated with the hosted provider.
     * @param metaAlias the meta alias of the hosted provider
     * @param resourceUrl resource URL to redirect to
     * @param requestId the <code>AuthnRequest</code> Id
     * @param request <code>HttpServletRequest</code> object
     * @param baseURL deployment base URL
     * @return the common login page URL; or <code>null</code> if an error
     *  occurred during the process.
     */
    public static String getCommonLoginPageURL (
        String metaAlias,
        String resourceUrl,
        String requestId,
        HttpServletRequest request,
        String baseURL)
    {
        if(metaAlias == null || metaAlias.equals ("")){
            FSUtils.debug.error ("FSServiceUtils.getCommonLoginPageURL: " +
                "metaAlias is null");
            return null;
        }
        String realm = IDFFMetaUtils.getRealmByMetaAlias(metaAlias);
        BaseConfigType hostConfig = null;
        try{
            String role = metaManager.getProviderRoleByMetaAlias(metaAlias);
            String entityId = metaManager.getEntityIDByMetaAlias(metaAlias);
            if (role != null) {
                if (role.equalsIgnoreCase(IFSConstants.SP)) {
                    hostConfig = metaManager.getSPDescriptorConfig(
                        realm, entityId);
                } else if (role.equalsIgnoreCase(IFSConstants.IDP)) {
                    hostConfig = metaManager.getIDPDescriptorConfig(
                        realm, entityId);
                }
            }
        }catch(Exception e){
            FSUtils.debug.error ("FSServiceUtils.getCommonLoginPageURL: " +
                "Could not obtain local config");
            return null;
        }
        if (hostConfig == null) {
            FSUtils.debug.error("FSServiceUtils.getCommonLoginPageURL: " +
                "Could not obtain hosted extended meta.");
            return null;
        }
        String loginPageUrl = getCommonLoginPageURL(request, hostConfig);
        StringBuffer commonLoginPageUrl = new StringBuffer (loginPageUrl);
        if (loginPageUrl.indexOf ('?') == -1){
            commonLoginPageUrl.append ("?");
        } else {
            commonLoginPageUrl.append ("&");
        }
        commonLoginPageUrl.append (IFSConstants.META_ALIAS).append ("=").
            append (metaAlias).append ("&");
        String postloginUrl = baseURL + IFSConstants.POST_LOGIN_PAGE;
        StringBuffer gotoBuffer = new StringBuffer(postloginUrl);
        if (postloginUrl.indexOf('?') == -1){
            gotoBuffer.append ("?");
        } else{
            gotoBuffer.append ("&");
        }
        gotoBuffer.append (IFSConstants.META_ALIAS)
            .append ("=").append (metaAlias);
        if (resourceUrl == null || resourceUrl.length() == 0) {
            resourceUrl = IDFFMetaUtils.getFirstAttributeValueFromConfig(
                hostConfig, IFSConstants.PROVIDER_HOME_PAGE_URL);
        }
        if(resourceUrl != null && !resourceUrl.equals ("")){
            gotoBuffer.append("&").append (IFSConstants.LRURL)
                .append ("=").append (URLEncDec.encode (resourceUrl));
            commonLoginPageUrl.append (IFSConstants.LRURL).append ("=").
                append (URLEncDec.encode (resourceUrl)).append("&");
        }
        commonLoginPageUrl.append (IFSConstants.GOTOKEY).append ("=").
            append (URLEncDec.encode (gotoBuffer.toString ()));
        String org = FSUtils.getAuthDomainURL(realm);
        if(org != null && org.length() != 0){
            commonLoginPageUrl.append("&").append(IFSConstants.ORGKEY).
                append ("=").append (URLEncDec.encode (org));
        }
        
        if(requestId != null && !requestId.equals ("")){
            commonLoginPageUrl.append("&").
                append(IFSConstants.AUTH_REQUEST_ID).append ("=").
                append (URLEncDec.encode (requestId));
        }
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "FSServiceUtils.getCommonLoginPageURL: List LoginPage: " +
                commonLoginPageUrl.toString ());
        }
        return commonLoginPageUrl.toString ();
    }
    
    /* Returns <code>URL</code> in the format of: 
     * <code>protocol://host:port/deployment_descriptor</code>.
     * The value of each field is retrieved from
     * <code>AMConfig.proeprties</code>.
     * @return the string with the combined value instance
     */
    public static String getBaseURL () {
        String deployDesc = SystemConfigurationUtil.getProperty(
            "com.iplanet.am.services.deploymentDescriptor");
        String protocol = SystemConfigurationUtil.getProperty(
            "com.iplanet.am.server.protocol");
        String host = SystemConfigurationUtil.getProperty(
            "com.iplanet.am.server.host");
        String port = SystemConfigurationUtil.getProperty(
            "com.iplanet.am.server.port");
        return protocol + "://" + host + ":" + port + deployDesc;
    }

    /**
     * Retrieves meta alias of a provider from http request.
     * @param request <code>HttpServletRequest</code> object
     * @return meta alias of a provider embeded in the request url
     */
    public static String getMetaAlias (HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message ("FSServiceUtil.getMetaAlias request uri = "
                + uri);
        }
        int index = uri.indexOf (IFSConstants.META_ALIAS);
        if ((index == -1) || 
            (index + IFSConstants.META_ALIAS.length() == uri.length()))
        {
            FSUtils.debug.message (
                "FSServiceUtil.getMetaAlias no metaAlias in request");
            return null;
        }
        return uri.substring(index + IFSConstants.META_ALIAS.length());
    }
    
    /**
     * Retrieves locale of a http request.
     * @param request <code>HttpServletRequest</code> object
     * @return locale of the request; or <code>null</code> if locale cannot
     *  be retrieved.
     */
    public static String getLocale (HttpServletRequest request) {
        if(request == null) {
            FSUtils.debug.error ("FSServiceUtil.getLocale: Request param is "
                + "null, returning null");
            return null;
        }
        try {
            SessionProvider sessionProvider = SessionManager.getProvider();
            Object ISSSOToken = sessionProvider.getSession(request);
            String[] returnStr = sessionProvider.getProperty(
                ISSSOToken, "Locale");
            if ((returnStr != null) && (returnStr.length > 0)) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message (
                        "FSServiceUtil.getLocale returning locale from token "
                        +  returnStr[0]);
                }
                return returnStr[0];
            }
        } catch (SessionException ssoe) {
            FSUtils.debug.error ("FSServiceUtil::getLocale():SessionException:",
                ssoe);
        } catch (UnsupportedOperationException ex) {
            FSUtils.debug.error ("FSServiceUtil::getLocale():Exception:", ex);
        }
        Locale locale = request.getLocale ();
        if (locale != null) {
            String returnStr = locale.toString ();
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message (
                    "FSServiceUtil.getLocale returning :locale from request:" 
                    + returnStr);
            }
            return returnStr;
        }
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message ("FSServiceUtil.getLocale Not able to get "
                + "locale from request either from token or header. returning "
                + "null ");
        }
        return null;
    }
   
    /**
     * Returns base URL for OpenSSO services deployment.
     * @param request HttpServletRequest
     * @return service base url
     */
    public static String getServicesBaseURL (HttpServletRequest request) {
        String protocol = request.getScheme ();
        String host = request.getServerName ();
        int  port = request.getServerPort ();
        String deployDesc = SystemConfigurationUtil.getProperty(
            "com.iplanet.am.services.deploymentDescriptor");
        String amserverURI = protocol + "://" + host + ":" + port + deployDesc;
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message (
                "FSServiceUtil.getServicesBaseURL hostString is " + 
                amserverURI);
        }
        return amserverURI;

    }

    /**
     * Returns base url of a request.
     * @param request <code>HttpServletRequest</code> object
     * @return base url
     */
    public static String getBaseURL (HttpServletRequest request) {
        if (request == null) {
            return getBaseURL();
        }
        String protocol = request.getScheme ();
        String hostStr = protocol + "://" + request.getHeader("Host") + "/";
        String requestURL = request.getRequestURL ().toString ();
        String tmpurl = null;          
        if (protocol.equals("http")) {
            tmpurl = requestURL.substring(8);
        }else{
            tmpurl = requestURL.substring(9);
        }

        int startIndex = tmpurl.indexOf("/") + 1;
        String tmpStr = tmpurl.substring (startIndex);
        int endIndex = tmpStr.indexOf ("/");
        String deployDesc = tmpStr.substring (0,endIndex);
        if (deployDesc != null && deployDesc != "") {
            String returnStr = hostStr + deployDesc;
            return returnStr;
        }
        return hostStr;
   }
    
    /**
     * Outputs the DOM representation given as root as XML string.
     * @param root The <code>DOM</code> representation to be outputted
     * @return string representation of <code>DOM</code> node.
     * @exception TransformerException, TransformerConfigurationException,
     *  FileNotFoundException
     */
    public static String printDocument (Node root) throws TransformerException, TransformerConfigurationException,
            FileNotFoundException {
        TransformerFactory tf = XMLUtils.getTransformerFactory();
        Transformer transformer = tf.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(root), new StreamResult(writer));
        return writer.getBuffer().toString();
    }
    
    
    /**
     * Converts <code>SOAP</code> message to <code>DOM</code> element.
     * @param message <code>SOAP</code> message
     * @return <code>DOM</code> element
     */
    public static org.w3c.dom.Node createSOAPDOM (SOAPMessage message){
        try{
            ByteArrayOutputStream str = new ByteArrayOutputStream();
            message.writeTo(str);
            String xml = str.toString();
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSU.createSOAPDOM, Intermediate xml: "
                    + xml);
            }
            DocumentBuilder db = XMLUtils.getSafeDocumentBuilder(false);
            Document doc = db.parse(new ByteArrayInputStream(xml.getBytes()));
            return doc;
        } catch (Exception e) {
            FSUtils.debug.error (
                "FSServiceUtils.createSOAPDOM: Exception: ", e);
            return null;
        }
    }

    /**
     * Converts <code>DOM</code> document to <code>SOAP</code> message.
     * @param doc <code>DOM</code> document
     * @return <code>SOAP</code> message
     */
    public static SOAPMessage convertDOMToSOAP(Document doc) {
        try{
            MimeHeaders headers = new MimeHeaders();
            headers.addHeader("Content-Type", "text/xml");
            return fac.createMessage(headers,
                new ByteArrayInputStream((printDocument(doc)).getBytes()));
        } catch (Exception e) {
            FSUtils.debug.error ("FSServiceUtils.convertDOMToSOAP: "
            + "Exception: " + e.getMessage ());
            return null;
        }
    }
 
    /**
     * Determines whether the request contains LECP header or not.
     * @param request <code>HttpServletRequest</code> object
     * @return <code>true</code> if the request contains LECP header;
     *  <code>false</code> otherwise.
     */
    public static boolean isLECPProfile(HttpServletRequest request) {
        java.util.Enumeration headerNames = request.getHeaderNames();
        while(headerNames.hasMoreElements()) {
            String hn = headerNames.nextElement().toString();
            String hv = request.getHeader(hn);
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("header " + hn + " val " + hv);
            }
        }
        String lecpHeaderValue = (String)request.getHeader(
            IFSConstants.LECP_HEADER_NAME);
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(" value of lecp in header "
                + lecpHeaderValue);
        }
        if(lecpHeaderValue == null) {
            lecpHeaderValue = (String)request.getHeader(
                (IFSConstants.LECP_HEADER_NAME).toLowerCase());
        }
        if(lecpHeaderValue != null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns list of cookie domains.
     * @return List of cookie domains configured.
     */
    public synchronized static List getCookieDomainList(){
          return cookieList;
    }
    
    /**
     * Gets the Affiliation ID for the provider that it belongs.
     * @param realm The realm under which the entity resides.
     * @param entityID provider's entity ID.
     * @return Affiliation ID.
     */
    public static String getAffiliationID(String realm, String entityID) {
        if (metaManager != null) {
            Set affiliations = metaManager.getAffiliateEntity(realm, entityID);
            if (affiliations != null && !affiliations.isEmpty()) {
                AffiliationDescriptorType affiliateDescriptor =
                    (AffiliationDescriptorType) 
                        affiliations.iterator().next();
                return affiliateDescriptor.getAffiliationID();
            }
        }
        return null;
    }

    /**
     * Parses the attribute map configuration and returns as java
     * <code>java.util.Map</code>.
     * @param list attribute configuration.
     * @return configured attribute mapping with key as the SAML
     *         attribute and the value being the local attribute.
     */
    public static Map parseAttributeConfig(List list) {
        Map map = new HashMap();
        if(list == null || list.isEmpty()) {
           if(FSUtils.debug.messageEnabled()) {
              FSUtils.debug.message("FSServiceUtils.parseAttributeConfig: " +
              "Input put list is empty");
           }
           return map;
        }
        Iterator iter = list.iterator();
        while(iter.hasNext()) {
            String entry = (String)iter.next();
            if(entry.indexOf("=") != -1) {
               StringTokenizer st = new StringTokenizer(entry, "=");
               map.put(st.nextToken(), st.nextToken());
            }
        }
        return map;
    }

    /**
     * Displays error page.
     * @param response <code>HttpServletResponse</code> object
     * @param commonErrorPage redirect url for error page
     * @param errorLocaleString locale string for the error message
     * @param remarkLocaleString locale string for the error remark
     */
    public static void showErrorPage(
        HttpServletResponse response,
        String commonErrorPage,
        String errorLocaleString,
        String remarkLocaleString)
    {
        StringBuffer errorPage = new StringBuffer();
        errorPage.append(commonErrorPage);
        char delimiter;
        if (commonErrorPage.indexOf(IFSConstants.QUESTION_MARK) < 0) {
            delimiter = IFSConstants.QUESTION_MARK;
        } else {
            delimiter = IFSConstants.AMPERSAND;
        }
        errorPage.append(delimiter)
            .append(IFSConstants.FEDERROR)
            .append(IFSConstants.EQUAL_TO)
            .append(URLEncDec.encode(FSUtils.bundle.getString(
                errorLocaleString)))
            .append(IFSConstants.AMPERSAND)
            .append(IFSConstants.FEDREMARK)
            .append(IFSConstants.EQUAL_TO)
            .append(URLEncDec.encode(FSUtils.bundle.getString(
                remarkLocaleString)));
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("Redirecting to Error page : "
                + errorPage.toString());
        }
        try {
            response.sendRedirect(errorPage.toString());
        } catch (IOException e){
            FSUtils.debug.error("Failed to redirect to error page");
        }
    }

    /**
     * Redirects the HTTP request to the Authentication module.
     * The authentication URL is built based on the organization that is
     * associated with the provider
     * @param request <code>HttpServletRequest</code> object that contains the
     *  request the client has made of the servlet.
     * @param response <code>HttpServletResponse</code> object that contains
     *  the response the servlet sends to the client.
     * @param hostedProviderAlias meta alias that identifies the local hosted 
     *  provider
     * @exception IOException If an input or output exception occurs
     */
    public static void redirectForAuthentication(
        HttpServletRequest request,
        HttpServletResponse response,
        String hostedProviderAlias)
        throws IOException
    {
        FSUtils.debug.message(
            "Entered FSServiceUtils::redirectForAuthentication");
        try {
            char authDelimiter;
            StringBuffer authURL = new StringBuffer();
            authURL.append(FSServiceUtils.getBaseURL(request))
                .append(IFSConstants.PRE_LOGIN_PAGE);
            if ((authURL.toString()).indexOf(IFSConstants.QUESTION_MARK) < 0) {
                authDelimiter = IFSConstants.QUESTION_MARK;
            } else {
                authDelimiter = IFSConstants.AMPERSAND;
            }
            authURL.append(authDelimiter)
                .append(IFSConstants.META_ALIAS)
                .append(IFSConstants.EQUAL_TO)
                .append(hostedProviderAlias);

            String parameterString = getParameterString(request);
            if (parameterString != null && parameterString.length() > 0) {
                authURL.append(IFSConstants.AMPERSAND).append(parameterString);
            }
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("Redirecting for authentication to: " +
                    authURL.toString());
            }
            response.sendRedirect(authURL.toString());
            return;
        } catch (IOException e) {
            FSUtils.debug.error("Error when redirecting : ", e);
            return;
        }
    }

    /**
     * Returns the parameters in the request as a HTTP URL string.
     * It returns all the parameters from the original request
     * @param request <code>HttpServletRequest</code> object that contains the
     *  request the client has made of the servlet.
     * @return The parameters of the request as String.
     */
    private static String getParameterString(
        HttpServletRequest request)
    {
        StringBuffer parameterString = new StringBuffer();
        Enumeration e = request.getParameterNames();
        while (e.hasMoreElements()) {
            String paramName = e.nextElement().toString();
            String[] values = request.getParameterValues(paramName);
            for (int i = 0; values != null && i < values.length; i++) {
                parameterString.append(paramName)
                               .append(IFSConstants.EQUAL_TO)
                               .append(values[i])
                               .append(IFSConstants.AMPERSAND);
            }
        }
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("Parameter String: " + parameterString);
        }
        return parameterString.toString();
    }

    /**
     * Invoked at the end when an operation is done.
     * The isSuccess determines if success message or failure message is
     * displayed.
     * @param response the <code>HttpServletResponse</code> object
     * @param opDoneURL where to go when an operation is done
     * @param isSuccess determines the content of the operation-done.jsp
     * @param successString success string to be appended to url if
     *  <code>isSuccess</code> is true.
     * @param failureString failure string to be appended to url if
     *  <code>isSuccess</code> is false.
     */
    public static void returnLocallyAfterOperation(
        HttpServletResponse response,
        String opDoneURL,
        boolean isSuccess,
        String successString,
        String failureString)
    {
        try {
            StringBuffer finalReturnURL = new StringBuffer();
            finalReturnURL.append(opDoneURL);
            char delimiter;
            if (opDoneURL.indexOf(IFSConstants.QUESTION_MARK) < 0) {
                delimiter = IFSConstants.QUESTION_MARK;
            } else {
                delimiter = IFSConstants.AMPERSAND;
            }
            finalReturnURL.append(delimiter)
                .append(IFSConstants.LOGOUT_STATUS)
                .append(IFSConstants.EQUAL_TO);
            if (isSuccess) {
                finalReturnURL.append(successString);
            } else {
                finalReturnURL.append(failureString);
            }
            response.sendRedirect(finalReturnURL.toString());
            return;
        } catch(IOException e) {
            FSUtils.debug.error("Redirect failed. Control halted:", e);
            return;
        }
    }

    /**
     * Determines the return location and redirects based on
     * federation termination Return URL of the provider that sent the
     * termination request
     * @param response http response object
     * @param retURL operation return url
     * @param commonErrorPage where to go if error occurs
     * @param errorLocaleString locale string for federation error
     * @param remarkLocaleString locale string for federation remark
     */
    public static void returnToSource(
        HttpServletResponse response,
        String retURL,
        String commonErrorPage,
        String errorLocaleString,
        String remarkLocaleString)
    {
        try {
            if (retURL == null || retURL.length() < 1) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("returnToSource returns sendError" +
                        "as source provider is unknown");
                }
                showErrorPage(response, commonErrorPage,
                    errorLocaleString, remarkLocaleString);
                return;
            } else {
                if (FSUtils.debug.messageEnabled()) {
                   FSUtils.debug.message("returnToSource returns URL : " +
                       retURL);
                }
                response.sendRedirect(retURL);
                return;
            }
        } catch(IOException exx) {
            FSUtils.debug.error("Redirect/sendError failed. Control halted:",
                exx);
        }
    }

    /**
     * Returns the list of circle of trusts page URL.
     * @param request <code>HttpServletRequest</code> object
     * @param hostedConfig hosted provider's extended meta
     * @return the list of circle of trusts page URL.
     */
    public static String getConsentPageURL(HttpServletRequest request,
        BaseConfigType hostedConfig)
    {
        String tempUrl = IDFFMetaUtils.getFirstAttributeValueFromConfig(
            hostedConfig, IFSConstants.LISTOFCOTS_PAGE_URL);
        if (tempUrl == null || tempUrl.length() == 0) {
            tempUrl = getDefaultPageURL(
                request, IFSConstants.LISTOFCOTS_PAGE_NAME);
        }
        return tempUrl;
    }

    /**
     * Returns common login page URL.
     * @param request <code>HttpServletRequest</code> object
     * @param hostedConfig hosted provider's extended meta
     * @return common login page URL.
     */
    public static String getCommonLoginPageURL(HttpServletRequest request,
        BaseConfigType hostedConfig)
    {
        String tempUrl = IDFFMetaUtils.getFirstAttributeValueFromConfig(
            hostedConfig, IFSConstants.SSO_FAILURE_REDIRECT_URL);
        if (tempUrl == null || tempUrl.length() == 0) {
            tempUrl = getDefaultPageURL(
                request, IFSConstants.COMMON_LOGIN_PAGE_NAME);
        }
        return tempUrl;
    }

    /**
     * Returns error page URL.
     * @param request <code>HttpServletRequest</code> object
     * @param hostedConfig hosted provider's extended meta
     * @param metaAlias hosted provider's meta alias
     * @return error page URL.
     */
    public static String getErrorPageURL(HttpServletRequest request,
        BaseConfigType hostedConfig, String metaAlias)
    {
        String tempUrl = IDFFMetaUtils.getFirstAttributeValueFromConfig(
            hostedConfig, IFSConstants.ERROR_PAGE_URL);
        if (tempUrl == null || tempUrl.length() == 0) {
            tempUrl = getDefaultPageURL(
                request, IFSConstants.ERROR_PAGE_NAME);
            tempUrl = addMetaAlias(tempUrl, metaAlias);
        }
        return tempUrl;
    }

    /**
     * Returns termination done page URL.
     * @param request <code>HttpServletRequest</code> object
     * @param hostedConfig hosted provider's extended meta
     * @param metaAlias hosted provider's meta alias
     * @return termination done page URL.
     */
    public static String getTerminationDonePageURL(HttpServletRequest request,
        BaseConfigType hostedConfig, String metaAlias)
    {
        String tempUrl = IDFFMetaUtils.getFirstAttributeValueFromConfig(
            hostedConfig, IFSConstants.TERMINATION_DONE_URL);
        if (tempUrl == null || tempUrl.length() == 0) {
            tempUrl = getDefaultPageURL(
                request, IFSConstants.TERMINATION_DONE_PAGE_NAME);
            tempUrl = addMetaAlias(tempUrl, metaAlias);
        }
        return tempUrl;
    }

    /**
     * Returns registration done page URL.
     * @param request <code>HttpServletRequest</code> object
     * @param hostedConfig hosted provider's extended meta
     * @param metaAlias hosted provider's meta alias
     * @return registration done page URL.
     */
    public static String getRegistrationDonePageURL(HttpServletRequest request,
        BaseConfigType hostedConfig, String metaAlias)
    {
        String tempUrl = IDFFMetaUtils.getFirstAttributeValueFromConfig(
            hostedConfig, IFSConstants.REGISTRATION_DONE_URL);
        if (tempUrl == null || tempUrl.length() == 0) {
            tempUrl = getDefaultPageURL(
                request, IFSConstants.NAME_REGISTRATION_DONE_PAGE_NAME);
            tempUrl = addMetaAlias(tempUrl, metaAlias);
        }
        return tempUrl;
    }

    /**
     * Returns logout done page URL.
     * @param request <code>HttpServletRequest</code> object
     * @param hostedConfig hosted provider's extended meta
     * @param metaAlias hosted provider's meta alias
     * @return logout done page URL.
     */
    public static String getLogoutDonePageURL(HttpServletRequest request,
        BaseConfigType hostedConfig, String metaAlias)
    {
        String tempUrl = IDFFMetaUtils.getFirstAttributeValueFromConfig(
            hostedConfig, IFSConstants.LOGOUT_DONE_URL);
        if (tempUrl == null || tempUrl.length() == 0) {
            tempUrl = getDefaultPageURL(
                request, IFSConstants.LOGOUTDONE_PAGE_NAME);
            tempUrl = addMetaAlias(tempUrl, metaAlias);
        }
        return tempUrl;
    }

    /**
     * Returns federation done page URL.
     * @param request <code>HttpServletRequest</code> object
     * @param hostedConfig hosted provider's extended meta
     * @param metaAlias hosted provider's meta alias
     * @return federation done page URL.
     */
    public static String getFederationDonePageURL(HttpServletRequest request,
        BaseConfigType hostedConfig, String metaAlias)
    {
        String tempUrl = IDFFMetaUtils.getFirstAttributeValueFromConfig(
            hostedConfig, IFSConstants.FEDERATION_DONE_URL);
        if (tempUrl == null || tempUrl.length() == 0) {
            tempUrl = getDefaultPageURL(
                request, IFSConstants.FEDERATIONDONE_PAGE_NAME);
            tempUrl = addMetaAlias(tempUrl, metaAlias);
        }
        return tempUrl;
    }

    /**
     * Returns do federate page URL.
     *
     * @param request <code>HttpServletRequest</code> object
     * @param hostedConfig hosted provider's extended meta
     * @return do federate page URL.
     */
    public static String getDoFederatePageURL(HttpServletRequest request,
        BaseConfigType hostedConfig)
    {
        String tempUrl = IDFFMetaUtils.getFirstAttributeValueFromConfig(
            hostedConfig, IFSConstants.DOFEDERATE_PAGE_URL);
        if (tempUrl == null || tempUrl.length() == 0) {
            tempUrl = getDefaultPageURL(
                request, IFSConstants.DOFEDERATE_PAGE_NAME);
        }
        return tempUrl;
    }

    /**
     * Returns default page URL.
     *
     * @param request HTTP Servlet Request.
     * @param pageName the page name whose url to be retrieved
     * @return String the Page URL.
     */
    public static String getDefaultPageURL(
        HttpServletRequest request, String pageName)
    {
        return getBaseURL(request) + templatePath + Constants.FILE_SEPARATOR +
            IFSConstants.DEFAULT_DIR + Constants.FILE_SEPARATOR + pageName;
    }

    /**
     * Appends meta alias to the URL.
     * @param url string url
     * @param metaAlias meta alias to be appended
     * @return the url with meta alias added
     */
    public static String addMetaAlias(String url, String metaAlias) {
        if (url == null || url.length() == 0 ||
            metaAlias == null || metaAlias.length() == 0) 
        {
            return url;
        } else {
            if (url.indexOf(IFSConstants.QUESTION_MARK) == -1) {
                return url + IFSConstants.QUESTION_MARK +
                    IFSConstants.META_ALIAS + IFSConstants.EQUAL_TO +
                    metaAlias ;
            } else {
                return url + IFSConstants.AMPERSAND +
                    IFSConstants.META_ALIAS + IFSConstants.EQUAL_TO +
                    metaAlias ;
            }
        }
    }

    /**
     * Finds approriate assertion consumer service URL.
     * @param spDescriptor sevice provider's meta descriptor
     * @param id requested assertion consumer service url id. It could be
     *  <code>null</code>.
     * @return assertion consumer server URL.
     */
    public static String getAssertionConsumerServiceURL(
        SPDescriptorType spDescriptor, String id)
    {
        if (spDescriptor == null) {
            return null;
        }
        String matching = null;
        String defaultValue = null;
        String first = null;
        List urls = spDescriptor.getAssertionConsumerServiceURL();
        if (urls != null && !urls.isEmpty()) {
            Iterator iter = urls.iterator();
            SPDescriptorType.AssertionConsumerServiceURLType curUrl = null;
            while (iter.hasNext()) {
                curUrl = (SPDescriptorType.AssertionConsumerServiceURLType)
                    iter.next();
                String curId = curUrl.getId();
                String curValue = curUrl.getValue();
                if (id != null && curId != null && curId.equals(id)) {
                    return curValue;
                }
                if (curUrl.isIsDefault()) {
                    defaultValue = curValue;
                }
                if (first == null) {
                    first = curValue;
                }
            }
        }
        if (defaultValue != null) {
            return defaultValue;
        } else {
            return first;
        }
    }

    /**
     * Returns the first profile from the list.
     * @param profiles list of profiles
     * @return the first profile of the list
     */
    public static String getFirstProtocolProfile(List profiles) {
        String retVal = "";
        if (profiles == null || profiles.isEmpty()) {
            return retVal;
        }
        return (String) profiles.iterator().next();
    }

    /**
     * Returns service provider's authentication context mappings.
     * @param hostConfig hosted provider' setended meta
     * @return authentication context mapping
     */
    public static Map getSPAuthContextInfo(BaseConfigType hostConfig) {
        if (hostConfig == null) {
            return null;
        }
        Map retMap = new HashMap();
        List mappings = IDFFMetaUtils.getAttributeValueFromConfig(
            hostConfig, IFSConstants.SP_AUTHNCONTEXT_MAPPING);
        if (mappings != null && !mappings.isEmpty()) {
            Iterator iter = mappings.iterator();
            while (iter.hasNext()) {
                String mapping = (String) iter.next();
                try {
                    FSSPAuthenticationContextInfo info = 
                        new FSSPAuthenticationContextInfo(mapping);
                    retMap.put(info.getAuthenticationContext(), info);
                } catch (FSException fe) {
                    FSUtils.debug.error(
                        "FSServiceUtils.getSPAuthContextInfo: wrong mapping:" +
                        mapping + " with exception:", fe);
                }
            }
        }
        return retMap;
    }

    /**
     * Determines if the registration profile is SOAP or not.
     * @param userID id of the user subject to registration
     * @param remoteEntityId remote provider's entity ID
     * @param remoteDescriptor remote provider's meta descriptor
     * @param metaAlias hosted provider's meta alias
     * @param hostedDescriptor hosted provider's meta descriptor
     * @return <code>true</code> if the registration profile is SOAP;
     *  <code>false</code> otherwise.
     */
    public static boolean isRegisProfileSOAP(
        String userID,
        String remoteEntityId,
        SPDescriptorType remoteDescriptor,
        String metaAlias,
        IDPDescriptorType hostedDescriptor)
    {
        FSAccountFedInfo acctInfo = null;
        try {
            acctInfo = FSAccountManager.getInstance(
                metaAlias).readAccountFedInfo(userID, remoteEntityId);
        } catch (FSAccountMgmtException e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("in exception:", e);
            }
            return true;
        }
        String hostedProfile =
            FSServiceUtils.getFirstProtocolProfile(
                hostedDescriptor.getRegisterNameIdentifierProtocolProfile());
        String remoteProfile =
            FSServiceUtils.getFirstProtocolProfile(
                remoteDescriptor.getRegisterNameIdentifierProtocolProfile());
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("host profile is:" + hostedProfile +
                "\nremote profile is " + remoteProfile);
        }
        if (acctInfo.isRoleIDP()) {
            if (hostedProfile.equalsIgnoreCase(
                    IFSConstants.REGISTRATION_SP_SOAP_PROFILE) ||
                hostedProfile.equalsIgnoreCase(
                    IFSConstants.REGISTRATION_IDP_SOAP_PROFILE))
            {
                return true;
            } else if (hostedProfile.equalsIgnoreCase(
                    IFSConstants.REGISTRATION_SP_HTTP_PROFILE) ||
                hostedProfile.equalsIgnoreCase(
                    IFSConstants.REGISTRATION_IDP_HTTP_PROFILE))
            {
                return false;
            } else {
                FSUtils.debug.error("FSServiceUtils.isRegisProfileSOAP:" +
                    "Invalid registration profile.");
                return true;
            }
        } else {
            if (remoteProfile.equalsIgnoreCase(
                    IFSConstants.REGISTRATION_SP_SOAP_PROFILE) ||
                remoteProfile.equalsIgnoreCase(
                    IFSConstants.REGISTRATION_IDP_SOAP_PROFILE))
            {
                return true;
            } else if (remoteProfile.equalsIgnoreCase(
                    IFSConstants.REGISTRATION_SP_HTTP_PROFILE) ||
                remoteProfile.equalsIgnoreCase(
                    IFSConstants.REGISTRATION_IDP_HTTP_PROFILE))
            {
                return false;
            } else {
                FSUtils.debug.error("FSServiceUtils.isRegisProfileSOAP:" +
                    "Invalid registration profile.");
                return true;
            }
        }
    }

    /**
     * Returns the minor version of supported protocol.
     * @param protocolEnum list of supported protocols
     * @return minor version of first supported protocol
     */
    public static int getMinorVersion(List protocolEnum) {
        int ver = 0;
        if (protocolEnum == null || protocolEnum.isEmpty()) {
            FSUtils.debug.message(
                "FSServiceUtils.getMinorVersion:null protocolEnum");
        } else {
            String minorVersion = (String) protocolEnum.iterator().next();
            if (minorVersion.equalsIgnoreCase(IFSConstants.ENUM_ZERO)) {
                ver = 0;
            } else if (minorVersion.equalsIgnoreCase(IFSConstants.ENUM_ONE)) {
                ver = 2;
            }
        }
        return ver;
    }

    public static FederationSPAdapter getSPAdapter(
        String hostEntityID, BaseConfigType hostSPConfig)
    {
        FSUtils.debug.message("FSServiceUtils.getSPAdapter");
        if (hostSPConfig == null) {
            FSUtils.debug.message("FSServiceUtils.getSPAdapter:null");
            return null;
        }
        try {
            String adapterName = IDFFMetaUtils.getFirstAttributeValueFromConfig(
                hostSPConfig, IFSConstants.FEDERATION_SP_ADAPTER);
            List adapterEnv = IDFFMetaUtils.getAttributeValueFromConfig(
                hostSPConfig, IFSConstants.FEDERATION_SP_ADAPTER_ENV);
            String realm = IDFFMetaUtils.getRealmByMetaAlias(
                hostSPConfig.getMetaAlias());
            if (adapterName != null && adapterName.length() != 0) {
                Class adapterClass = Class.forName(adapterName.trim());
                FederationSPAdapter adapterInstance = 
                    (FederationSPAdapter) adapterClass.newInstance();
                Set newEnv = new HashSet();
                if (adapterEnv != null && !adapterEnv.isEmpty()) {
                    newEnv.addAll(adapterEnv);
                }
                newEnv.add(FederationSPAdapter.ENV_REALM + realm);
                adapterInstance.initialize(hostEntityID, newEnv);
               
                return adapterInstance;
            }
        } catch (Exception e) {
          FSUtils.debug.error(
              "FSServiceUtils.getSPAdapter: Unable to get provider", e);
        }
        return null;
    } 
}

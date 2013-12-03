/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AuthViewBeanBase.java,v 1.13 2010/01/22 03:31:35 222713 Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 * Portions Copyrighted 2013 Nomura Research Institute, Ltd
 */
package com.sun.identity.authentication.UI;

import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.iplanet.am.util.BrowserEncoding;
import com.sun.identity.shared.debug.Debug;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.ViewBeanBase;
import com.iplanet.jato.view.html.StaticTextField;
import com.sun.identity.authentication.client.AuthClientUtils;
import com.sun.identity.common.ISLocaleContext;
import com.sun.identity.shared.encode.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Map;
import java.util.Set;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.UnsupportedEncodingException;

/**
 * This class is a default implementation of <code>ViewBean</code> auth UI.
 */
public abstract class AuthViewBeanBase extends ViewBeanBase {
    private static String[] ignoreList = {
        "goto", "encoded", "IDtoken0", "IDtoken1", "IDtoken2", "IDButton", "AMAuthCookie", "IDToken3"
    };

    private  java.util.Locale accLocale ;
    static Debug loginDebug = Debug.getInstance("amLoginViewBean");
    
    /**
     * Creates <code>AuthViewBeanBase</code> object.
     * @param pageName name of page for auth UI.
     */
    public AuthViewBeanBase(String pageName ) {
        super(pageName);
        registerChildren();
    }
    
    /** registers child views */
    protected void registerChildren() {
        registerChild(PAGE_ENCODING, StaticTextField.class);
        registerChild(SERVICE_URI, StaticTextField.class);
    }
    
    
    protected View createChild(String name) {
        if (name.equals(PAGE_ENCODING)) {
            return new StaticTextField(this, PAGE_ENCODING, "");
        } else if (name.equals(SERVICE_URI)) {
            return new StaticTextField(this, name, serviceUri);
        }
        throw new IllegalArgumentException(
        "Invalid child name [" + name + "]");
        
    }
    
    protected void setPageEncoding(HttpServletRequest request,
    HttpServletResponse response) {
        /** Set the codeset of the page **/
        String client_type = AuthClientUtils.getClientType(request);
        String content_type = AuthClientUtils.getContentType(client_type);
        
        accLocale = fallbackLocale;
        if (accLocale == null) {
            ISLocaleContext localeContext = new ISLocaleContext();
            localeContext.setLocale(request);
            accLocale = localeContext.getLocale();
        }
        
        String charset = AuthClientUtils.getCharSet(client_type, accLocale);

        if (response != null) {
            response.setContentType(content_type+";charset="+charset);
        }
        
        String jCharset = BrowserEncoding.mapHttp2JavaCharset(charset);
        if (loginDebug.messageEnabled()) {
            loginDebug.message("In setPageEncoding - charset : " + charset);
            loginDebug.message("In setPageEncoding - JCharset : " + jCharset);
        }
        setDisplayFieldValue(PAGE_ENCODING, jCharset);
    }

    /** 
     * Returns the validated and Base64 ecoded query params value.
     * @param request from which query parameters have to be extracted.
     * @return a String the validated and Base64 ecoded query params String
     */
    public String getEncodedQueryParams(HttpServletRequest request)
    {
        // create a map to exclude the duplication of the SunQueryParamsString
        Map<String, String> tmpMap = new HashMap<String, String>();

        String returnQueryParams = "";
        StringBuilder queryParams = new StringBuilder();
        queryParams.append("");
        Enumeration parameters = request.getParameterNames();
        for (; parameters.hasMoreElements();) {
            boolean ignore = false;
            String parameter = (String) parameters.nextElement();
            for (int i = 0; i < ignoreList.length; i++) {
                if (parameter.equalsIgnoreCase(ignoreList[i])) {
                    ignore = true;
                    break;
                }
            }
            if (loginDebug.messageEnabled()) {
                loginDebug.message("getEncodedQueryParams: parameter is:"
                        + parameter);
            }
            if (!ignore) {
                String value = request.getParameter(parameter);
                if ("SunQueryParamsString".equalsIgnoreCase(parameter)) {
                    try {
                        value = new String(Base64.decode(value), "UTF-8");
                        String[] splitParams = value.split("&");
                        for (String param: splitParams) {
                            String[] keyAndValue = param.split("=", 0);
                            tmpMap.put(keyAndValue[0], keyAndValue[1]);
                        }
                    } catch (Exception e) {
                        loginDebug.message("getEncodedQueryParams: "
                                + "failed to decode SunQueryParamsString. ", e);
                    }
                } else {
                    tmpMap.put(parameter, value);
                }
            }
        }
        if (!tmpMap.isEmpty()) {
            int cnt = 0;
            for (Map.Entry<String, String> entrySet : tmpMap.entrySet()) {
                cnt++;
                queryParams.append(entrySet.getKey());
                queryParams.append('=');
                if (loginDebug.messageEnabled()) {
                    loginDebug.message("getEncodedQueryParams: parameter "
                            + "value:" + entrySet.getValue());
                }
                queryParams.append(entrySet.getValue());
                if (tmpMap.size() > cnt) {
                    queryParams.append('&');
                }
            }
        }
        String queryParamsString = queryParams.toString();
        if  (loginDebug.messageEnabled()) {
            loginDebug.message("getEncodedQueryParams: SunQueryParamsString is"
                +":"+ queryParamsString);
        }
        if ((queryParamsString != null) &&
            (queryParamsString.length() != 0))
        {
            returnQueryParams = getEncodedInputValue(queryParamsString);
        }

        if (loginDebug.messageEnabled()) {
            loginDebug.message("getEncodedQueryParams:returnQueryParams : "
                + returnQueryParams);
        }
        return returnQueryParams;
    }

    /**
     * Returns the validated and Base64 ecoded URL value.
     * @param inputURL input URL string value 
     * @param encoded value of "encoded" parameter to tell wheather 
     * the inputURL is already encoded or not
     * @param request HttpServletRequest object
     * @return a String the validated and Base64 ecoded URL value
     */
    public String getValidatedInputURL(String inputURL, String encoded, 
        HttpServletRequest request) {
        String returnURL = "";
        if ((inputURL != null) && (inputURL.length() != 0) && 
            (!inputURL.equalsIgnoreCase("null"))){
            if ((encoded == null) || (encoded.length() == 0) || 
                (encoded.equals("false"))) {
                returnURL = getEncodedInputURL(inputURL, request);
            } else {
                try {
                    String msg = new String(Base64.decode(inputURL), "UTF-8");
                    returnURL = inputURL;
                } catch (RuntimeException rtex) {
                    loginDebug.warning(
                        "getValidatedInputURL:RuntimeException");                
                } catch (UnsupportedEncodingException ueex) {
                    loginDebug.warning("getValidatedInputURL:" + 
                                       "UnsupportedEncodingException");                
                }  
            }
        }

        if (loginDebug.messageEnabled()) {
            loginDebug.message("getValidatedInputURL:returnURL : " 
                               + returnURL);
        }
        return returnURL;
    }

    /** 
     * Returns the Base64 ecoded URL value.
     * @param inputURL input URL string value
     * @param request HttpServletRequest object
     * @return a String the Base64 ecoded URL value
     */
    private String getEncodedInputURL(String inputURL, 
        HttpServletRequest request) {
        String returnURL = inputURL;
        try {
            URL url = new URL(inputURL);
        } catch (MalformedURLException mfe) {
            loginDebug.warning("Input URL is not standard www URL.");
            String requestURL = request.getRequestURL().toString();
            String requestURI = request.getRequestURI();
            int index = requestURL.indexOf(requestURI);
            String newURL = null;
            if (index != -1) {
                newURL = requestURL.substring(0, index) + inputURL;
            } else {
                index = requestURL.indexOf(serviceUri);
                if (index != -1) {
                    newURL = requestURL.substring(0, index) + inputURL;
                }
            }
            try {
                URL url = new URL(newURL);
            } catch (MalformedURLException mfe1) {
                loginDebug.warning("Relative URL is not standard www URL.");
                returnURL = "";                
            }
        }
        
        if ((returnURL != null) && (returnURL.length() != 0)) {
            try {            
                returnURL = Base64.encode(returnURL.getBytes("UTF-8"));                        
            } catch (UnsupportedEncodingException ueex) {
                loginDebug.warning("getEncodedInputURL:" + 
                    "UnsupportedEncodingException");
                returnURL = "";
            }
        }
        
        if (loginDebug.messageEnabled()) {
            loginDebug.message("getEncodedInputURL:returnURL : " + returnURL);
        }
        return returnURL;
    }

    /** 
     * Returns the Base64 ecoded URL value.
     * @param inputValue input string value
     * @return a String the Base64 ecoded URL value
     */
    public String getEncodedInputValue(String inputValue) {
        String returnValue = "";
        
        if ((inputValue != null) && (inputValue.length() != 0) && 
            (!inputValue.equalsIgnoreCase("null"))) {        
            try {                
                returnValue = Base64.encode(inputValue.getBytes("UTF-8"));                            
            } catch (UnsupportedEncodingException ueex) {
                loginDebug.warning("getEncodedInputValue:" + 
                               "UnsupportedEncodingException");            
            }
        }

        if (loginDebug.messageEnabled()) {
            loginDebug.message("getEncodedInputValue:returnValue : " 
                + returnValue);
        }

        return returnValue;
    }
    
    /**
     * Returns <code>Locale</code> for auth request.
     * @return <code>Locale</code> for auth request.
     */
    public java.util.Locale getRequestLocale() {
        return accLocale;
    }
    
    /**
     * Returns tile index for auth UI.
     * @return tile index for auth UI.
     */
    public abstract String getTileIndex();
    
    /**
     * Parameter name for page encoding.
     */
    public static final String PAGE_ENCODING = "gx_charset";
    /**
     * Parameter name for service uri.
     */
    public static final String SERVICE_URI = "ServiceUri";
    
    /**
     * Configured service uri.
     */
    public static String serviceUri = AuthClientUtils.getServiceURI();
    
    //to be used in case session is destroyed
    protected java.util.Locale fallbackLocale;
    /**
     * When HTTP request is made, we get authcontext and get current locale and
     * store it in fallbackLocale. Before the login page is displayed, login
     * modules can have LoginState object which has the locale settings. But
     * after going through login process LoginState might have changed locale
     * based on user preference  or LoginState may not exist if LoginFailure in
     * such case we need to fallback to this locale for responding to user
     */

    public static Set storeCookies = new HashSet();    

    public static AMResourceBundleCache rbCache =
    AMResourceBundleCache.getInstance();
    /**
     * Resource bundle with <code>Locale</code>
     */
    public ResourceBundle rb = null;
}


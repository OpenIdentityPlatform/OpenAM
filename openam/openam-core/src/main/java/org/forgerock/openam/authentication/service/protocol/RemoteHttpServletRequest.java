/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 ForgeRock, Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package org.forgerock.openam.authentication.service.protocol;

import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.common.CaseInsensitiveHashSet;
import java.io.Serializable;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * This class encapsulates a HttpServletRequest extending from RemoteServletRequst.
 * The information or state that can be serialized is with non serialized 
 * content handled sensibly.
 * 
 * @author Steve Ferris steve.ferris@forgerock.com
 */
public class RemoteHttpServletRequest extends RemoteServletRequest 
        implements HttpServletRequest, Serializable {
    public static final long serialVersionUID = 42L;
    
    /**
     * The values of the request to be serialized.
     */
    private String authType = null;
    // normal cookies are not serialized.
    private transient Cookie[] cookies = null;
    private RemoteCookie[] internalCookies = null;
    private Set headerNames = new CaseInsensitiveHashSet();
    private Map internalHeader = new CaseInsensitiveHashMap();
    private Map internalHeaders = new CaseInsensitiveHashMap();
    private String method = null;
    private String pathInfo = null;
    private String pathTranslated = null;
    private String contextPath = null;
    private String queryString = null;
    private String remoteUser = null;
    private Principal userPrincipal = null;
    private String requestedSessionId = null;
    private String requestURI = null;
    private StringBuffer requestURL = null;
    private String servletPath = null;
    private RemoteSession remoteSession = null;
    
    /**
     * Called during deserialization.
     */
    public RemoteHttpServletRequest() {
        super();
    }
    
    /**
     * Creates a serializable request around the normal request.
     * 
     * @param request The request to encapsulate.
     */
    public RemoteHttpServletRequest(HttpServletRequest request) {
        super(request);
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            // create copies of the cookies
            if (cookies.length > 0) {
                internalCookies = new RemoteCookie[cookies.length];
            }

            for (int c = 0; c < cookies.length; c++) {
                internalCookies[c] = new RemoteCookie(cookies[c]);
            }
        }
       
        // iterate over the headers
        Enumeration hNames = getHeaderNames();
        
        while (hNames.hasMoreElements()) {
            String headerName = (String) hNames.nextElement();
            internalHeader.put(headerName, getHeader(headerName));
            internalHeaders.put(headerName, createSet(getHeaders(headerName)));
            headerNames.add(headerName);
        }
        
        method = getMethod();
        pathInfo = getPathInfo();
        pathTranslated = getPathTranslated();
        contextPath = getContextPath();
        queryString = getQueryString();
        remoteUser = getRemoteUser();
        userPrincipal = getUserPrincipal();
        requestedSessionId = getRequestedSessionId();
        requestURI = getRequestURI();
        requestURL = getRequestURL();
        servletPath = getServletPath();
        remoteSession = new RemoteSession(getSession());
    }
    
    private Set createSet(Enumeration headers) {
        Set headersSet = new HashSet();
        
        while (headers.hasMoreElements()) {
            headersSet.add(headers.nextElement());
        }
        
        return headersSet;
    } 
           
    /**
     * Retrieves a reference to the internal request.
     * 
     * @return The internal request.
     */
    private HttpServletRequest _getHttpServletRequest() {
	return (HttpServletRequest) super.getRequest();
    }

    /**
     * The default behavior of this method is to return getAuthType()
     * on the wrapped request object. Serialized.
     * 
     * @return The auth type of the request.
     */
    public String getAuthType() {
	return (this._getHttpServletRequest() != null) ? 
            this._getHttpServletRequest().getAuthType() : authType;
    }
   
    /**
     * The default behavior of this method is to return getCookies()
     * on the wrapped request object. Serialized. 
     * 
     * @return The cookies associates with the request
     */
    public Cookie[] getCookies() {
        if (this._getHttpServletRequest() != null) {
            return this._getHttpServletRequest().getCookies();
        } else {
                if (internalCookies != null) {
                Cookie[] externalCookies = new Cookie[internalCookies.length];

                for (int c = 0; c < externalCookies.length; c++) {
                    externalCookies[c] = internalCookies[c].getCookie();
                }

                return externalCookies;
            } else {
                return null;
            }
        } 
    }

    /**
     * The default behavior of this method is to return getDateHeader(String name)
     * on the wrapped request object. Not Serialized as we cannot differentiate
     * date headers from normal headers. 
     * 
     * @return The named date header in seconds or -1 if not available.
     */
    public long getDateHeader(String name) {
	return (this._getHttpServletRequest() != null) ? 
            this._getHttpServletRequest().getDateHeader(name) : -1;
    }
        	
    /**
     * The default behavior of this method is to return getHeader(String name)
     * on the wrapped request object. Serialized.
     * 
     * @return The value of the named header.
     */
    public String getHeader(String name) {
	return (this._getHttpServletRequest() != null) ? 
            this._getHttpServletRequest().getHeader(name) : (String) internalHeader.get(name);
    }
       
    /**
     * The default behavior of this method is to return getHeaders(String name)
     * on the wrapped request object. Serialized.
     * 
     * @return Enumeration of the value of the named header.
     */
    public Enumeration getHeaders(String name) {
	return (this._getHttpServletRequest() != null) ? 
            this._getHttpServletRequest().getHeaders(name) : new Vector((HashSet) internalHeaders.get(name)).elements();
    }  

    /**
     * The default behavior of this method is to return getHeaderNames()
     * on the wrapped request object. Serialized.
     * 
     * @return Enumeration of header names
     */
    public Enumeration getHeaderNames() {
	return (this._getHttpServletRequest() != null) ? 
            this._getHttpServletRequest().getHeaderNames() : new Vector(headerNames).elements();
    }
    
    /**
     * The default behavior of this method is to return getIntHeader(String name)
     * on the wrapped request object. Serialized.
     * 
     * @return The value of the header expressed as an int
     * @throws NumberFormatException if the header cannot be expressed as an int
     */
    public int getIntHeader(String name) {
	return (this._getHttpServletRequest() != null) ? 
            this._getHttpServletRequest().getIntHeader(name) : Integer.parseInt((String) internalHeader.get(name));
    }
    
    /**
     * The default behavior of this method is to return getMethod()
     * on the wrapped request object. Serialized.
     * 
     * @return The method associated with the request.
     */
    public String getMethod() {
	return (this._getHttpServletRequest() != null) ? 
            this._getHttpServletRequest().getMethod() : method;
    }
    
    /**
     * The default behavior of this method is to return getPathInfo()
     * on the wrapped request object. Serialized.
     * 
     * @return The path info associated with the request.
     */
    public String getPathInfo() {
	return (this._getHttpServletRequest() != null) ? 
            this._getHttpServletRequest().getPathInfo() : pathInfo;
    }

    /**
     * The default behavior of this method is to return getPathTranslated()
     * on the wrapped request object. Serialized.
     * 
     * @return The translated path of the request.
     */
    public String getPathTranslated() {
	return (this._getHttpServletRequest() != null) ? 
            this._getHttpServletRequest().getPathTranslated() :
            pathTranslated;
    }

    /**
     * The default behavior of this method is to return getContextPath()
     * on the wrapped request object. Serialized.
     * 
     * @return The context path of the request.
     */
    public String getContextPath() {
	return (this._getHttpServletRequest() != null) ? 
            this._getHttpServletRequest().getContextPath() :
            contextPath;
    }
    
    /**
     * The default behavior of this method is to return getQueryString()
     * on the wrapped request object. Serialized.
     * 
     * @return The query string of the request.
     */
    public String getQueryString() {
	return (this._getHttpServletRequest() != null) ? 
            this._getHttpServletRequest().getQueryString() :
            queryString;
    }
    
    /**
     * The default behavior of this method is to return getRemoteUser()
     * on the wrapped request object. Serialized.
     * 
     * @return The remote user of the request, if set null otherwise.
     */
    public String getRemoteUser() {
	return (this._getHttpServletRequest() != null) ? 
            this._getHttpServletRequest().getRemoteUser() : remoteUser;
    }
 
    /**
     * The default behavior of this method is to return isUserInRole(String role)
     * on the wrapped request object. Not serialized.
     * 
     * @return True if user is in named role, false otherwise.
     */
    public boolean isUserInRole(String role) {
	return (this._getHttpServletRequest() != null) ? 
            this._getHttpServletRequest().isUserInRole(role) : false;
    }
    
    /**
     * The default behavior of this method is to return getUserPrincipal()
     * on the wrapped request object. Serialized.
     * 
     * @return The principal of the authenticated user, if available otherwise null.
     */
    public java.security.Principal getUserPrincipal() {
	return (this._getHttpServletRequest() != null) ? 
            this._getHttpServletRequest().getUserPrincipal() : userPrincipal;
    }
    
    /**
     * The default behavior of this method is to return getRequestedSessionId()
     * on the wrapped request object. Serialized.
     * 
     * @return The session id of the request, if available.
     */
    public String getRequestedSessionId() {
	return (this._getHttpServletRequest() != null) ? 
            this._getHttpServletRequest().getRequestedSessionId() : requestedSessionId;
    }
    
    /**
     * The default behavior of this method is to return getRequestURI()
     * on the wrapped request object. Serialized.
     * 
     * @return The request URI
     */
    public String getRequestURI() {
	return (this._getHttpServletRequest() != null) ? 
            this._getHttpServletRequest().getRequestURI() : requestURI;
    }
    
    /**
     * The default behavior of this method is to return getRequestURL()
     * on the wrapped request object. Serialized.
     * 
     * @return The requests URL
     */
    public StringBuffer getRequestURL() {
	return (this._getHttpServletRequest() != null) ? 
            this._getHttpServletRequest().getRequestURL() : requestURL;
    }
	
    /**
     * The default behavior of this method is to return getServletPath()
     * on the wrapped request object. Serialized.
     * 
     * @return The servlets path.
     */
    public String getServletPath() {
	return (this._getHttpServletRequest() != null) ? 
            this._getHttpServletRequest().getServletPath() : servletPath;
    }
        
    /**
     * The default behavior of this method is to return getSession(boolean create)
     * on the wrapped request object. Not Serailzed.
     * 
     * @return The HttpSession associated with the request or null if unavailable.
     */
    public HttpSession getSession(boolean create) {
	return (this._getHttpServletRequest() != null) ? 
            this._getHttpServletRequest().getSession(create) : remoteSession;
    }
    
    /**
     * The default behavior of this method is to return getSession()
     * on the wrapped request object. Not Serialized.
     * 
     * @return The HttpSession associated with the request or null if unavailable.
     */
    public HttpSession getSession() {
	return (this._getHttpServletRequest() != null) ? 
            this._getHttpServletRequest().getSession() : remoteSession;
    }
    
    /**
     * The default behavior of this method is to return isRequestedSessionIdValid()
     * on the wrapped request object. Not Serialized.
     * 
     * @return true if the session id is valid, false otherwise.
     */ 
    public boolean isRequestedSessionIdValid() {
	return (this._getHttpServletRequest() != null) ? 
            this._getHttpServletRequest().isRequestedSessionIdValid() : false;
    }
     
    /**
     * The default behavior of this method is to return isRequestedSessionIdFromCookie()
     * on the wrapped request object. Not serialized.
     * 
     * @return true if the session id is from a cookie, false otherwise.
     */
    public boolean isRequestedSessionIdFromCookie() {
	return (this._getHttpServletRequest() != null) ? 
            this._getHttpServletRequest().isRequestedSessionIdFromCookie() : false;
    }
    
    /**
     * The default behavior of this method is to return isRequestedSessionIdFromURL()
     * on the wrapped request object. Not Serialized.
     * 
     * @return true if the session id is from a URL, false otherwise.
     */ 
    public boolean isRequestedSessionIdFromURL() {
	return (this._getHttpServletRequest() != null) ? 
            this._getHttpServletRequest().isRequestedSessionIdFromURL() : false;
    }
    
    /**
     * The default behavior of this method is to return isRequestedSessionIdFromUrl()
     * on the wrapped request object. Not Serialized.
     * 
     * @return true if the session id is from a URL, false otherwise.
     */
    public boolean isRequestedSessionIdFromUrl() {
	return (this._getHttpServletRequest() != null) ? 
            this._getHttpServletRequest().isRequestedSessionIdFromUrl() : false;
    }    
}

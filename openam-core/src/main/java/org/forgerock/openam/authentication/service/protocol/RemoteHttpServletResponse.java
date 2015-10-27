/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010-2015 ForgeRock AS.
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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import org.forgerock.openam.utils.CollectionUtils;

/**
 * Encapsulates a HttpServletResponse and makes that state that is serializable
 * transferable. 
 */
public class RemoteHttpServletResponse extends RemoteServletResponse implements HttpServletResponse, Serializable {
    public static final long serialVersionUID = 42L;

    // transferrable state
    private Set<RemoteCookie> internalCookies = new HashSet<>();
    private Map<String, List<String>> internalHeaders = new HashMap<>();
    private Map<String, Set<Long>> internalDateHeaders = new HashMap<>();

    /**
     * Creates a new RemoteHttpServletResponse encapsulating a normal response
     * 
     * @param response The response to be encapsulated.
     */
    public RemoteHttpServletResponse(HttpServletResponse response) {
        super(response);
    }
    
    /**
     * Used by deserialization.
     */
    public RemoteHttpServletResponse() {
        super();
    }
        
    /**
     * Retrieves a reference to the encapsulated response. Not Serialized.
     * 
     * @return The encapsulated response, null if not available.
     */
    private HttpServletResponse _getHttpServletResponse() {
	return (HttpServletResponse) super.getResponse();
    }
    
    /**
     * The default behavior of this method is to call addCookie(Cookie cookie)
     * on the wrapped response object. Serialized. Updates the underlying
     * response if available.
     * 
     * @param cookie The cookie to add to the request.
     */
    public void addCookie(Cookie cookie) {
        if (this._getHttpServletResponse() != null) {
            this._getHttpServletResponse().addCookie(cookie);
            internalCookies.add(new RemoteCookie(cookie));
        } else {
            internalCookies.add(new RemoteCookie(cookie));
        }
    }
    
    /**
     * Returns the cookies associated with the request. Serialized.
     * 
     * @return A Set containing the cookies.
     */
    public Set<RemoteCookie> getCookies() {
        return internalCookies;
    }

    /**
     * The default behavior of this method is to call containsHeader(String name)
     * on the wrapped response object. Serialized.
     * 
     * @return true if the header is set, false otherwise.
     */
    public boolean containsHeader(String name) {
	    return this._getHttpServletResponse() != null ?
                this._getHttpServletResponse().containsHeader(name) : internalHeaders.containsKey(name);
    }
    
    /**
     * The default behavior of this method is to call encodeURL(String url)
     * on the wrapped response object. Not Serialized.
     * 
     * @return The encoded URL, null if not available.
     */
    public String encodeURL(String url) {
	    return this._getHttpServletResponse() != null ? this._getHttpServletResponse().encodeURL(url) : null;
    }

    /**
     * The default behavior of this method is to return encodeRedirectURL(String url)
     * on the wrapped response object. Not serialized.
     * 
     * @return Encoded redirect URL, null if not available.
     */
    public String encodeRedirectURL(String url) {
	    return this._getHttpServletResponse() != null ? this._getHttpServletResponse().encodeRedirectURL(url) : null;
    }

    /**
     * The default behavior of this method is to call encodeUrl(String url)
     * on the wrapped response object. Not Serialized.
     * 
     * @return The encoded URL, null if unavailable.
     */
    public String encodeUrl(String url) {
	    return this._getHttpServletResponse() != null ? this._getHttpServletResponse().encodeUrl(url) : null;
    }
    
    /**
     * The default behavior of this method is to return encodeRedirectUrl(String url)
     * on the wrapped response object. Not Serialized.
     * 
     * @return The encoded redirect URL, null if not available.
     */
    public String encodeRedirectUrl(String url) {
	    return this._getHttpServletResponse() != null ? this._getHttpServletResponse().encodeRedirectUrl(url) : null;
    }
    
    /**
     * The default behavior of this method is to call sendError(int sc, String msg)
     * on the wrapped response object. Not serialized.
     * 
     * @param sc The error code
     * @param msg The error message
     * @throws IOException If the error code cannot be set.
     */
    public void sendError(int sc, String msg) throws IOException {
	    if (this._getHttpServletResponse() != null) {
            this._getHttpServletResponse().sendError(sc, msg);
        }
    }

    /**
     * The default behavior of this method is to call sendError(int sc)
     * on the wrapped response object. Not serialized.
     * 
     * @param sc The error code
     * @throws IOException If the error code cannot be set.
     */
    public void sendError(int sc) throws IOException {
        if (this._getHttpServletResponse() != null) {
            this._getHttpServletResponse().sendError(sc);
        }
    }

    /**
     * The default behavior of this method is to return sendRedirect(String location)
     * on the wrapped response object. Not serialized. 
     * 
     * @param location The location of the 302 redirect.
     * @throws IOException If the redirect cannot be set.
     */
    public void sendRedirect(String location) throws IOException {
        if (this._getHttpServletResponse() != null) {
            this._getHttpServletResponse().sendRedirect(location);
        }
    }
    
    /**
     * The default behavior of this method is to call setDateHeader(String name, long date)
     * on the wrapped response object. Serialized.
     * 
     * @param name The name of the date header
     * @param date The date expressed in seconds.
     */
    public void setDateHeader(String name, long date) {
        if (this._getHttpServletResponse() != null) {
            this._getHttpServletResponse().setDateHeader(name, date);
        } 
        
        Set<Long> dSet = new HashSet<>();
        dSet.add(date);
        internalDateHeaders.put(name, dSet);
    }
    
    /**
     * The default behavior of this method is to call addDateHeader(String name, long date)
     * on the wrapped response object. Serialized.
     * 
     * @param name The name of the date header to add
     * @param date The value of the date expressed in seconds.
     */
    public void addDateHeader(String name, long date) {
        if (this._getHttpServletResponse() != null) {
            this._getHttpServletResponse().addDateHeader(name, date);
        }
        
        if (internalDateHeaders.containsKey(name)) {
            Set<Long> existingSet = internalDateHeaders.get(name);
            existingSet.add(date);
            internalDateHeaders.put(name, existingSet);
        } else {
            Set<Long> dSet = new HashSet<>();
            dSet.add(date);
            internalDateHeaders.put(name, dSet);
        }
    }
    
    /**
     * The Map of set date headers. Serialized.
     * 
     * @return The Map of date headers.
     */
    public Map<String, Set<Long>> getDateHeaders() {
        return internalDateHeaders;
    }
    
    /**
     * The default behavior of this method is to return setHeader(String name, String value)
     * on the wrapped response object. Serialized.
     * 
     * @param name The name of the header
     * @param value The value of the header
     */
    public void setHeader(String name, String value) {
        if (this._getHttpServletResponse() != null) {
            this._getHttpServletResponse().setHeader(name, value);
        } 
       
        List<String> vSet = new ArrayList<>();
        vSet.add(value);
        internalHeaders.put(name, vSet);
    }
    
    /**
     * The default behavior of this method is to return addHeader(String name, String value)
     * on the wrapped response object. Serialized.
     * 
     * @param name The name of the header
     * @param value The value of the header
     */
    public void addHeader(String name, String value) {
        if (this._getHttpServletResponse() != null) {
            this._getHttpServletResponse().addHeader(name, value);
        }
        
        if (internalHeaders.containsKey(name)) {
            List<String> existingSet = internalHeaders.get(name);
            existingSet.add(value);
            internalHeaders.put(name, existingSet);
        } else {
            List<String> vSet = new ArrayList<>();
            vSet.add(value);
            internalHeaders.put(name, vSet);
        }
    }
    
    /**
     * The Map of the set headers. Serialized.
     * 
     * @return Map of set headers.
     */
    public Map<String, List<String>> getHeaders() {
        return internalHeaders;
    }
    
    /**
     * The default behavior of this method is to call setIntHeader(String name, int value)
     * on the wrapped response object. Serialized.
     * 
     * @param name The name of the header
     * @param value The value of the header
     */
    public void setIntHeader(String name, int value) {
        if (this._getHttpServletResponse() != null) {
            this._getHttpServletResponse().setIntHeader(name, value);
        }
        
        List<String> iSet = new ArrayList<>();
        iSet.add(String.valueOf(value));
        internalHeaders.put(name, iSet);
    }
    
    /**
     * The default behavior of this method is to call addIntHeader(String name, int value)
     * on the wrapped response object. Serialized.
     * 
     * @param name The name of the header
     * @param value The value of the header
     */
    public void addIntHeader(String name, int value) {
        if (this._getHttpServletResponse() != null) {
            this._getHttpServletResponse().addIntHeader(name, value);
        }
        
        if (internalHeaders.containsKey(name)) {
            List<String> existingSet = internalHeaders.get(name);
            existingSet.add(Integer.toString(value));
            internalHeaders.put(name, existingSet);
        } else {
            List<String> iSet = new ArrayList<>();
            iSet.add(String.valueOf(value));
            internalHeaders.put(name, iSet);
        }
    }

    /**
     * The default behavior of this method is to call setStatus(int sc)
     * on the wrapped response object. Not Serialized.
     * 
     * @param sc The status code of the response
     */
    public void setStatus(int sc) {
        if (this._getHttpServletResponse() != null) {
            this._getHttpServletResponse().setStatus(sc);
        }
    }
    
    /**
     * The default behavior of this method is to call setStatus(int sc, String sm)
     * on the wrapped response object. Not Serialized.
     * 
     * @param sc The status code of the response
     */
     public void setStatus(int sc, String sm) {
         if (this._getHttpServletResponse() != null) {
             this._getHttpServletResponse().setStatus(sc, sm);
         }
     }

    @Override
    public int getStatus() {
        if (this._getHttpServletResponse() != null) {
            this._getHttpServletResponse().getStatus();
        }

        return 0;
    }

    @Override
    public String getHeader(String s) {
        if (CollectionUtils.isNotEmpty(internalHeaders)) {
            return internalHeaders.get(s).get(0); //return first
        }

        return null;
    }

    @Override
    public Collection<String> getHeaders(String s) {
        if (internalHeaders.keySet().contains(s)) {
            return internalHeaders.get(s);
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public Collection<String> getHeaderNames() {
        return internalHeaders.keySet();
    }
}

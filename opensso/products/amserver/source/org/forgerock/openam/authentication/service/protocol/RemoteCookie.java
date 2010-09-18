/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 ForgeRock AS. All Rights Reserved
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

import java.io.Serializable;
import javax.servlet.http.Cookie;

/**
 * Encapsulates a Cookie making it Serializable. 
 * 
 * @author Steve Ferris steve.ferris@forgerock.com
 */
public class RemoteCookie implements Serializable {
    public static final long serialVersionUID = 42L;
    
    /* The Cookie being wrapped
     */
    private transient Cookie cookie = null;
    
    /* The serializable data
     */
    private String comment = null;
    private String domain = null;
    private int maxAge = -1;
    private String name = null;
    private String path = null;
    private boolean secure = false;
    private String value = null;
    private int version = -1;
    
    public RemoteCookie() {
        // do nothing
    }
    
    /**
     * Creates a RemoteCookie based on the contents of the supplied Cookie.
     * 
     * @param cookie The Cookie to be serialized
     */
    public RemoteCookie(Cookie cookie) {
        this.cookie = cookie;
        comment = cookie.getComment();
        domain = cookie.getDomain();
        maxAge = cookie.getMaxAge();
        name = cookie.getName();
        path = cookie.getPath();
        secure = cookie.getSecure();
        value = cookie.getValue();
        version = cookie.getVersion();
    }
    
    /**
     * Returns a Cookie representing the state of this serialized cookie
     * 
     * @return Cookie representing original state of the Cookie
     */
    public Cookie getCookie() {
        Cookie newCookie = new Cookie(name, value);
        
        if (comment != null)
            newCookie.setComment(comment);
        
        if (domain != null)
            newCookie.setDomain(domain);
        
        if (maxAge != -1)
            newCookie.setMaxAge(maxAge);
        
        if (path != null)
            newCookie.setPath(path);
        
        if (secure)
            newCookie.setSecure(secure);
        
        if (version != -1)
            newCookie.setVersion(version);
        
        return newCookie;
    }
    
    /**
     * Retrieves the cookie comment
     * 
     * @return The comment of the cookie or null
     */
    public String getComment() {
        return comment;
    }
    
    /**
     * Retrieves the domain of the cookie
     * 
     * @return The cookie domain if set
     */
    public String getDomain() {
        return domain;
    }
    
    /**
     * Retrieves the cookies maximum age
     * 
     * @return The cookies expiry or -1 if not set
     */
    public int getMaxAge() {
        return maxAge;
    }
    
    /**
     * Retrieves the name of the cookie
     * 
     * @return String representing the cookie name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Retrieves the path of the cookie, if set
     * 
     * @return The cookie path or null
     */
    public String getPath() {
        return path;
    }
    
    /**
     * Is this cookie secure?
     * 
     * @return true if the cookie is secure, false (default) otherwise
     */
    public boolean getSecure() {
        return secure;
    }
    
    /**
     * Retrieves the value of the cookie
     * 
     * @return A String representing the cookies value
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Retrieves the version of the cookie
     * 
     * @return The version of the cookie, -1 if not set
     */
    public int getVersion() {
        return version;
    }
    
    /**
     * Sets the cookies comment to supplied value. Updates underlying cookie
     * implementation if present.
     * 
     * @param comment The cookies new comment
     */
    public void setComment(String comment) {
        if (cookie != null) 
            cookie.setComment(comment);
        
        this.comment = comment;
    }
    
    /**
     * Set the cookie domain to the supplied value. Updates the underlying 
     * cookie value if present.
     * 
     * @param domain The cookie domain
     */
    public void setDomain(String domain) {
        if (cookie != null) 
            cookie.setDomain(domain);
        
        this.domain = domain;
    }
    
    /**
     * Sets the cookies maximum age (in seconds since the epoch). Updates the
     * underlying cookie if present.
     * 
     * @param expiry The cookies expiry
     */
    public void setMaxAge(int expiry) {
        if (cookie != null) 
            cookie.setMaxAge(expiry);
        
        this.maxAge = expiry;
    }
    
    /**
     * Sets the path of the cookie. Updates the underlying cookie if present.
     * 
     * @param uri The new uri path of the cookie
     */
    public void setPath(String uri) {
        if (cookie != null) 
            cookie.setPath(uri);
        
        this.path = uri;
    }
    
    /**
     * Sets the secure flag of the cookie updating the underlying cookie 
     * representation if present.
     * 
     * @param secure The new secure flag setting of the cookie
     */
    public void setSecure(boolean secure) {
        if (cookie != null) 
            cookie.setSecure(secure);
        
        this.secure = secure;
    }
    
    /**
     * Set the cookie to have a new value, updating the underlying cookie 
     * representatio if present.
     * 
     * @param newValue The new value of the cookie
     */
    public void setValue(String newValue) {
        if (cookie != null) 
            cookie.setValue(newValue);
        
        this.value = newValue;
    }
    
    /**
     * Set the version of the cookie, updating the underlying representation if
     * present.
     * 
     * @param version
     */
    public void setVersion(int version) {
        if (cookie != null) 
            cookie.setVersion(version);
        
        this.version = version;
    }
}

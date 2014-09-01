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

import com.sun.identity.shared.debug.Debug;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;

/**
 * This class encapsulates a ServletRequest allowing its state to be serialized.
 * 
 * @author Steve Ferris steve.ferris@forgerock.com
 */
public class RemoteServletRequest implements ServletRequest, Serializable {
    public static final long serialVersionUID = 42L;
        
    /* The request whos state is to be serialized
     */
    protected transient ServletRequest request = null;
    
    /* The fields to hold the state of the request
     */
    private Set attributeNames = new HashSet();
    private Map internalAttributes = new HashMap();
    private String characterEncoding = null;
    private int contentLength = -1;
    private String contentType = null;
    private Map internalParameters = new HashMap();
    private Map internalParamererMap = new HashMap();
    private Set internalParameterNames = new HashSet();
    private Map internalParameterValues = new HashMap();
    private String protocol = null;
    private String scheme = null;
    private String serverName = null;
    private int serverPort = -1;
    private String remoteAddr = null;
    private String remoteHost = null;
    private Locale locale = null;
    private Set locales = new HashSet();
    private boolean isSecure = false;
    private String localName = null;
    private String localAddr = null;
    private int localPort = -1;
    private int remotePort = -1;
    
    protected transient Debug debug = null;
    private static final String CLASS = "RemoteServletRequest";
    public static final String SERIALIZABLE_INT = "java.io.Serializable";

    /**
     * 
     * Creates a ServletRequest adaptor wrapping the given request object. 
     * 
     * @param request The ServletRequest to be serialized
     * @throws java.lang.IllegalArgumentException if the request is null
     */
    public RemoteServletRequest(ServletRequest request) {
        debug = Debug.getInstance("remoteRequest");
        
        if (debug.messageEnabled()) {
            debug.message(CLASS + " <init>");
        }
        
	if (request == null) {
	    throw new IllegalArgumentException("Request cannot be null");   
	}
        
	this.request = request;
        
        // process the requests parameters
        processRequest();
    }
    
    protected void processRequest() {
        // iterate over the attributes, storing those that can be serialized
        Enumeration aNames = getAttributeNames();
        
        while (aNames.hasMoreElements()) {
            String attributeName = (String) aNames.nextElement();
            
            if (isSerializable(getAttribute(attributeName))) {
                attributeNames.add(attributeName);
                internalAttributes.put(attributeName, getAttribute(attributeName));
            }
        }
                
        characterEncoding = getCharacterEncoding();
        contentLength = getContentLength();
        contentType = getContentType();
        
        // iterate over the parameters storing those that can be serialized
        Enumeration pNames = getParameterNames();
        
        while (pNames.hasMoreElements()) {
            String parameterName = (String) pNames.nextElement();
            
            if (isSerializable(getParameter(parameterName))) {
                internalParameters.put(parameterName, getParameter(parameterName));
                internalParameterValues.put(parameterName, getParameterValues(parameterName));
                internalParameterNames.add(parameterName);  

            } 
        }
        
        internalParamererMap.putAll(getParameterMap());
        protocol = getProtocol();
        scheme = getScheme();
        serverName = getServerName();
        serverPort = getServerPort();
        remoteAddr = getRemoteAddr();
        remoteHost = getRemoteHost();
        locale = getLocale();
        localAddr = getLocalAddr();
        localName = getLocalName();
        localPort = getLocalPort();
        remotePort = getRemotePort();
        
        Enumeration lNames = getLocales();
        
        while (lNames.hasMoreElements()) {
            locales.add((Locale) lNames.nextElement());
        }
        
        isSecure = isSecure();
    }

    /**
     * Called during the deserialization process.
     */
    public RemoteServletRequest() {
        debug = Debug.getInstance("remoteRequest");
    }
    
    /**
     * Return the wrapped request object.
     * 
     * @return The ServletRequest or null if the object has been deserialized
     */
    public ServletRequest getRequest() {
        return this.request;
    }
	
    /**
     * Sets the request object being wrapped. 
     * 
     * @throws java.lang.IllegalArgumentException if the request is null.
     */
    public void setRequest(ServletRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        
        this.request = request;
        processRequest();
        
        if (debug.messageEnabled()) {
            debug.message("RemoteServletRequest::setRequest new settings");
        }
    }

    /**
     * The default behavior of this method is to call getAttribute(String name)
     * on the wrapped request object. Only those attributes that are serializable
     * will have been serialized. Some attributes may be missing post 
     * serialization.
     *
     * @param name The name of the attibute to get
     * @return The value of the attribute or null. 
     */
    public Object getAttribute(String name) {
	return (request != null) ? this.request.getAttribute(name) : 
            internalAttributes.get(name);
    }
    
    /**
     * The default behavior of this method is to return getAttributeNames()
     * on the wrapped request object. Serialized.
     * 
     * @return Enumeration of attribute names
     */
    public Enumeration getAttributeNames() {
	return (request != null) ? this.request.getAttributeNames() : 
            new Vector(attributeNames).elements();
    }    
    
    /**
     * The default behavior of this method is to return getCharacterEncoding()
     * on the wrapped request object. Serialized.
     * 
     * @return The character encoding of this request
     */
    public String getCharacterEncoding() {
	return (request != null) ? this.request.getCharacterEncoding() : characterEncoding;
    }
	
    /**
     * The default behavior of this method is to set the character encoding
     * on the wrapped request object, if available. After serialization justs
     * updates the internal state.
     * 
     * @param enc The new character encoding for the request
     */
    public void setCharacterEncoding(String enc) throws java.io.UnsupportedEncodingException {
	if (request != null) {
            this.request.setCharacterEncoding(enc);
        } else {
            characterEncoding = enc;
        }
    }
    
    /**
     * The default behavior of this method is to return getContentLength()
     * on the wrapped request object. Serialized.
     * 
     * @return The content length of the request
     */
    public int getContentLength() {
	return (request != null) ? this.request.getContentLength() : contentLength;
    }
    
   /**
    * The default behavior of this method is to return getContentType()
    * on the wrapped request object. Serialized.
    *
    * @return The content type of the request 
    */
    public String getContentType() {
	return (request != null) ? this.request.getContentType() : contentType;
    }
    
    /**
     * The default behavior of this method is to return getInputStream()
     * on the wrapped request object. <b>Not serialized, null post serialization.</b>
     * 
     * @return The InputStream if available.
     */
    public ServletInputStream getInputStream() throws IOException {
	return (request != null) ? this.request.getInputStream() : null;
    }

    /**
     * Returns the Internet Protocol (IP) address of the interface on which the request was received.
     *
     * @return a <code>String</code> containing the IP address on which the request was received.
     * @since 2.4
     */
    public String getLocalAddr() {
        return (request != null) ? this.request.getLocalAddr() : localAddr;
    }

    /**
     * Returns the host name of the Internet Protocol (IP) interface on which the request was received.
     *
     * @return a <code>String</code> containing the host name of the IP on which the request was received.
     * @since 2.4
     */
    public String getLocalName() {
        return (request != null) ? this.request.getLocalName() : localName;
    }

    /**
     * Returns the Internet Protocol (IP) source port of the client or last proxy that sent the request.
     *
     * @return an integer specifying the port number
     * @since 2.4
     */
    public int getRemotePort() {
        return (request != null) ? this.request.getRemotePort() : remotePort;
    }

    /**
     * Returns the Internet Protocol (IP) port number of the interface on which the request was received.
     *
     * @return an integer specifying the port number
     * @since 2.4
     */
    public int getLocalPort() {
        return (request != null) ? this.request.getLocalPort() : localPort;
    }

    /**
     * The default behavior of this method is to return getParameter(String name)
     * on the wrapped request object. Serialized. Only those parameters that
     * are serializable are transferred.
     * 
     * @return The value of the specified parameter or null.
     */
    public String getParameter(String name) {
	return (request != null) ? this.request.getParameter(name) : 
            (String) internalParameters.get(name);
    }
    
    /**
     * The default behavior of this method is to return getParameterMap()
     * on the wrapped request object. Serialized. Only those parameters that
     * are serailzable are transferred.
     * 
     * @return A map of the parameters.
     */
    public Map getParameterMap() {
	return (request != null) ? this.request.getParameterMap() : 
            internalParamererMap;
    }
    
    /**
     * The default behavior of this method is to return getParameterNames()
     * on the wrapped request object. Serialized.
     * 
     * @return Enumeration of paramters names.
     */     
    public Enumeration getParameterNames() {
	return (request != null) ? this.request.getParameterNames() : 
            new Vector(internalParameterNames).elements();
    }

    /**
     * The default behavior of this method is to return getParameterValues(String name)
     * on the wrapped request object. Serilized. Only those parameters values
     * that are serializable are transferred.
     * 
     * @return Array of Strings of the specified parameter values.
     */
    public String[] getParameterValues(String name) {
	return (request != null) ? this.request.getParameterValues(name) : 
            (String[]) internalParameterValues.get(name);
    }
    
    /**
     * The default behavior of this method is to return getProtocol()
     * on the wrapped request object. Serialized.
     * 
     * @return The protocol associated with the request.
     */
    public String getProtocol() {
	return (request != null) ? this.request.getProtocol() : protocol;
    }
    
    /**
     * The default behavior of this method is to return getScheme()
     * on the wrapped request object. Serialized.
     * 
     * @return The scheme associated with the request.
     */
    public String getScheme() {
	return (request != null) ? this.request.getScheme() : scheme;
    }
    
    /**
     * The default behavior of this method is to return getServerName()
     * on the wrapped request object. Serialized.
     * 
     * @return The name of the server. 
     */
    public String getServerName() {
	return (request != null) ? this.request.getServerName() : serverName;
    }
    
    /**
     * The default behavior of this method is to return getServerPort()
     * on the wrapped request object. Serialized.
     * 
     * @return The port of the server.
     */
    public int getServerPort() {
	return (request != null) ? this.request.getServerPort() : serverPort;
    }
    
    /**
     * The default behavior of this method is to return getReader()
     * on the wrapped request object. <b>Not serialized, null post serialization.</b>
     */
    public BufferedReader getReader() throws IOException {
	return (request != null) ? this.request.getReader() : null;
    }
    
    /**
     * The default behavior of this method is to return getRemoteAddr()
     * on the wrapped request object. Serialized.
     * 
     * @return The remote address of the client associated with the request.
     */
    public String getRemoteAddr() {
	return (request != null) ? this.request.getRemoteAddr() : remoteAddr;
    }
    
    /**
     * The default behavior of this method is to return getRemoteHost()
     * on the wrapped request object. Serialized.
     * 
     * @return The remote host of the client associated with the request.
     */
    public String getRemoteHost() {
	return (request != null) ? this.request.getRemoteHost() : remoteHost;
    }
    
    /**
     * The default behavior of this method is to return setAttribute(String name, Object o)
     * on the wrapped request object. If the underlying request is available, it
     * is updated.
     * 
     * @param name The name of the attribute to set.
     * @param o The value of the attribute.
     */
    public void setAttribute(String name, Object o) {
        if (request != null) {
            this.request.setAttribute(name, o);
            internalAttributes.put(name, o);
        } else {
            internalAttributes.put(name, o);
        }
    }
    
    /**
     * The default behavior of this method is to call removeAttribute(String name)
     * on the wrapped request object. Updates the underlying request if available.
     * 
     * @param name The name of the attribute to remove.
     */
    public void removeAttribute(String name) {
        if (request != null) {
            this.request.removeAttribute(name);
            internalAttributes.remove(name);        
        } else {
            internalAttributes.remove(name);
        }
    }
    
    /**
     * The default behavior of this method is to return getLocale()
     * on the wrapped request object. Serialized.
     * 
     * @return The locale of the request.
     */
    public Locale getLocale() {
	return (request != null) ? this.request.getLocale() : locale;
    }
    
    /**
     * The default behavior of this method is to return getLocales()
     * on the wrapped request object. Serialized.
     * 
     * @return Enumeration of the locales of the request.
     */
    public Enumeration getLocales() {
	return (request != null) ? this.request.getLocales() : 
            new Vector(locales).elements();
    }
    
    /**
     * The default behavior of this method is to return isSecure()
     * on the wrapped request object. Serialized.
     * 
     * @return true if the cookie is secure, false otherwise.
     */
    public boolean isSecure() {
	return (request != null) ? this.request.isSecure() : isSecure;
    }
    
    /**
     * The default behavior of this method is to return getRequestDispatcher(String path)
     * on the wrapped request object. <b>Not serialized, null post serialization.</b>
     * 
     * @return RequestDispatcher or null if not available.
     */
    public RequestDispatcher getRequestDispatcher(String path) {
	return (request != null) ? this.request.getRequestDispatcher(path) : null;
    }
    
    /**
     * The default behavior of this method is to return getRealPath(String path)
     * on the wrapped request object. <b>Not serialized, null post serialization.</b>
     * 
     * @return The real path of the request.
     */
    public String getRealPath(String path) {
	return (request != null) ? this.request.getRealPath(path) : null;
    }    
    
    /**
     * Tests if an object implements the java.io.Serializable interface.
     * 
     * @param obj The obj to test for serialization.
     * @return true if the object implements Serializable, false otherwise.
     */
    protected boolean isSerializable(Object obj) {
        if (obj == null)
            return false;
        
        Class[] interfaces = obj.getClass().getInterfaces();
        
        for (int i = 0; i < interfaces.length; i++) {
            if (interfaces[i].getName().equals(SERIALIZABLE_INT)) {
                return true;
            }
        }
        
        return false;
    }
    
    protected String printNames(Class[] interfaces) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < interfaces.length; i++) {
            buffer.append("i=").append(interfaces[i].getName());
        }
        
        return buffer.toString();
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Attributes       : ").append(internalAttributes)
                .append("Charcter encoding: ").append(characterEncoding)
                .append("Content length   : ").append(contentLength)
                .append("Content type     : ").append(contentType)
                .append("Parameters       : ").append(internalParamererMap)
                .append("Protocol         : ").append(protocol)
                .append("Scheme           : ").append(scheme)
                .append("Server name      : ").append(serverName)
                .append("Server port      : ").append(serverPort)
                .append("Remote host      : ").append(remoteHost)
                .append("Remote addr      : ").append(remoteAddr)
                .append("Locale           : ").append(locale)
                .append("Locales          : ").append(locales)
                .append("IsSecure         : ").append(isSecure)
                .append("Local name       : ").append(localName)
                .append("Local addr       : ").append(localAddr)
                .append("Local port       : ").append(localPort)
                .append("Remote port      : ").append(remotePort);

        return buffer.toString();
    }
}

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
 * $Id: SessionProvider.java,v 1.7 2008/06/25 05:47:28 qcheng Exp $
 *
 * Portions Copyrighted 2014-2016 ForgeRock AS.
 */

package com.sun.identity.plugin.session;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Interface used for creating sessions, and for accessing session
 * information.
 *
 * @supported.all.api
 */
public interface SessionProvider {

    /**
     * This constant string is used in the implementation and calling
     * of the first method for passing a realm name in a map.
     */
    String REALM = "realm";

    /**
     * This constant string is used in the implementation and calling
     * of the first method for passing a principal name in a map.
     */
    String PRINCIPAL_NAME = "principalName";

    /**
     * This constant string is used in the implementation and calling
     * of the first method for passing an authentication level in a map.
     */
    String AUTH_LEVEL = "AuthLevel";

    /**
     * This constant string is used as a property name to indicate
     * the authentication method. Typically it is used as the second
     * name parameter in the <code>getProperty</code> method.
     */
    String AUTH_METHOD = "authMethod";

    /**
     * This constant string is used as a property name to indicate
     * the authentication instant. Typically it is used as the second
     * name parameter in the <code>getProperty</code> method.
     */
    String AUTH_INSTANT = "authInstant";
  
    /**
     * This constant string is used as a property name to indicate
     * the client host.
     */
    String HOST = "Host";

    /**
     * This constant string is used as a property name to indicate
     * the client hostname.
     */
    String HOST_NAME = "HostName";

    /**
     * The name of the request attribute under which the user attributes shall be stored. This is used by the
     * Federation authentication module (hosted SP scenario) when dynamic account creation is enabled.
     */
    String ATTR_MAP = "org.forgerock.openam.authentication.userAttrMap";

    /** 
     * Meaningful only for Service Provider side, the implementation of this
     * method will create a local session for the local user identified by
     * the information in the map. The underline mechanism of the
     * session creation and management is application specific.
     * For example, it could be cookie setting or URL rewriting, which 
     * is expected to be done by the implementation of this method.
     * Note that only the first input parameter is mandatory. Normally,
     * at least one of the last two parameters should not be null
     * 
     * @param info a Map with keys and values being of type String; The
     *        keys will include <code>SessionProvider.PRINCIPAL_NAME</code>
     *        (returned from <code>SPAccountMapper</code>),
     *        <code>SessionProvider.REALM</code>, 
     *        <code>SessionProvider.AUTH_LEVEL</code>, 
     *        <code>SessionProvider.AUTH_INSTANT</code>, and may include
     *        <code>"resourceOffering"</code> and/or <code>"idpEntityID"</code>;
     *        The implementation of this method could choose to set some of
     *        the information contained in the map into the newly created
     *        Session by calling <code>setProperty()</code>, later the target
     *        application may consume the information. 
     * @param request the <code>HttpServletRequesa</code>t the user made to
     *        initiate the Single Sign On; Note that it should be the initial
     *        request coming from the browser as opposed to the possible
     *        subsequent back-channel HTTP request for delivering
     *        SOAP message.
     * @param response the <code>HttpServletResponse</code> that will be sent
     *        to the user (for example it could be used to set a cookie).
     * @param targetApplication the original resource that was requested
     *        as the target of the Single Sign On by the end user; If needed,
     *        this String could be modified, e.g., by appending query
     *        string(s) or by URL rewriting, hence this is an in/out parameter.
     * @return the newly created local user session.
     * @throws SessionException if an error occurred during session creation.
     */ 
    public Object createSession(
        Map info,                       // in
        HttpServletRequest request,     // in
        HttpServletResponse response,   // in/out
        StringBuffer targetApplication  // in/out
    ) throws SessionException;

    /**
     * Returns the corresponding session object.
     * May be used by both SP and IDP side for getting an existing
     * session given an session ID.
     * @param sessionID the unique session handle.
     * @return the corresponding session object.
     * @throws SessionException if an error occurred during session
     * retrieval.
     */
    public Object getSession(String sessionID)
        throws SessionException;

    /**
     * Returns the corresponding session object.
     * May be used by both SP and IDP side for getting an existing
     * session given a browser initiated HTTP request.
     * @param request the browser initiated HTTP request.
     * @return the corresponding session object.
     * @throws SessionException if an error occurred during session
     * retrieval.
     */
    public Object getSession(HttpServletRequest request)
        throws SessionException;

    /**
     * May be used by both SP and IDP side to invalidate a session.
     * In case of SLO with SOAP, the last two input parameters
     * would have to be null
     * @param session the session to be invalidated
     * @param request the browser initiated HTTP request.
     * @param response the HTTP response going back to browser.
     * @throws SessionException if an error occurred during session
     * retrieval.     
     */
    public void invalidateSession(
        Object session,
        HttpServletRequest request,   // optional input
        HttpServletResponse response  // optional input
    ) throws SessionException;

    /**
     * Returns <code>true</code> if the session is valid.
     * This is useful for toolkit clean-up thread.
     *
     * @param session Session object.
     * @return <code>true</code> if the session is valid.
     */
    public boolean isValid(Object session)
        throws SessionException;

    /**
     * Returns session ID.
     * The returned session ID should be unique and not 
     * change during the lifetime of this session
     *
     * @return session ID.
     */
    public String getSessionID(Object session);

    /**
     * Returns princiapl name, or user name given the session
     * object. 
     * @param session Session object.
     * @return principal name, or user name.
     * @throws SessionException if getting the principal name
     * causes an error.
     */
    public String getPrincipalName(Object session)
        throws SessionException;
        
    /**
     * Stores a property in the session object. This is an
     * optional method.
     *
     * @param session the session object.
     * @param name the property name.
     * @param values the property values.
     * @throws UnsupportedOperationException if this method
     * is not supported.
     * @throws SessionException if setting the property in
     * the session causes an error.
     */
    public void setProperty(
        Object session,
        String name,
        String[] values
    ) throws UnsupportedOperationException, SessionException;

    /**
     * Returns property value of a session object. This
     * is an optional method.
     *
     * @param session the session object.
     * @param name the property name.
     * @return the property values.
     * @throws UnsupportedOperationException if this method
     * is not supported.
     * @throws SessionException if getting the property from
     * the session causes an error.
     */
    public String[] getProperty(Object session, String name)
        throws UnsupportedOperationException, SessionException;

    /**
     * Returns rewritten URL.
     * Rewrites an URL with session information in case
     * cookie setting is not supported.
     *
     * @param session the session object.
     * @param URL the URL to be rewritten.
     * @return the rewritten URL.
     * @throws SessionException if rewritting the URL
     * causes an error.
     */
    public String rewriteURL(Object session, String URL)
        throws SessionException;
    
    /**
     * Registers a listener for the session. This is an optional
     * method.
     * @param session the session object.
     * @param listener listener for the session invalidation event.
     * @throws UnsupportedOperationException if this method
     * is not supported.
     * @throws SessionException if adding the listener in the
     * session causes an error.
     */
    public void addListener(Object session, SessionListener listener)
        throws UnsupportedOperationException, SessionException;
    
    /**
     * Sets a load balancer cookie in the suppled HTTP response. The load
     * balancer cookie's value is set per server instance and is used to
     * support sticky load balancing.
     * 
     * @param response the <code>HttpServletResponse</code> that will be sent
     *        to the user.
     */
    public void setLoadBalancerCookie(HttpServletRequest request, HttpServletResponse response);

    /**
     * Returns the time left for this session in seconds.
     * @param session Session object.
     * @return The time left for this session.
     * @exception A SessionException is thrown if the session reached its maximum
     * session time, or the session was destroyed, or there was an error during
     * communication with session service.
     */
    public long getTimeLeft(Object session) throws SessionException; 
}


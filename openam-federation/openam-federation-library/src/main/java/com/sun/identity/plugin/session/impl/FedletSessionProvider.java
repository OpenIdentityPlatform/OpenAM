/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: FedletSessionProvider.java,v 1.4 2008/08/06 17:28:17 exu Exp $
 *
 * Portions Copyrighted 2015-2016 ForgeRock AS.
 */

package com.sun.identity.plugin.session.impl;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionListener;
import com.sun.identity.plugin.session.SessionProvider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;


/**
 * The <code>FedletSessionProvider</code> class is an implementation of
 * <code>SessionProvider</code> interface for Fedlet deployment.
 * The implementation performs no operation on the methods.
 */
public class FedletSessionProvider implements SessionProvider {
    private static Debug debug = Debug.getInstance("libPlugins");

    /**
     * Default Constructor
     */
    public FedletSessionProvider() {
    }

    /**
     * Indicates whether a secret originally comes from this class or not
     * @param secret the secret string to be matched
     * @return true if there is a match, false otherwise
     */
    public static boolean matchSecret(String secret) {
        return false; 
    }

    /** 
     * Meaningful only for SP side, the implementation of this method
     * will create a local session for the local user identified by
     * the information in the map. The underline mechanism of the
     * session creation and management is application specific.
     * For example, it could be cookie setting or url rewriting, which 
     * is expected to be done by the implementation of this method.
     * Note that only the first input parameter is mandatory. Normally,
     * at least one of the last two parameters should not be null
     * 
     * @param info a Map with keys and values being of type String; The
     *             keys will include "principalName" (returned from
     *             SPAccountMapper), "realm", "authLevel", and may
     *             include "resourceOffering" and/or "idpEntityID";
     *             The implementation of this method could choose to set
     *             some of the information contained in the map into the
     *             newly created Session by calling setProperty(), later
     *             the target application may consume the information. 
     * @param request the HttpServletRequest the user made to initiate
     *                the SSO; Note that it should be the initial request
     *                coming from the browser as opposed to the possible
     *                subsequent back-channel HTTP request for delivering
     *                SOAP message.
     * @param response the HttpServletResponse that will be sent to the
     *                 user (for example it could be used to set a cookie).
     * @param targetApplication the original resource that was requested
     *                          as the target of the SSO by the end user;
     *                          If needed, this String could be modified,
     *                          e.g., by appending query string(s) or by
     *                          url rewriting, hence this is an in/out
     *                          parameter.
     * @return the newly created local user session.
     * @throws SessionException if an error occurred during session
     * creation.
     */ 
    public Object createSession(
        Map info,                       // in
        HttpServletRequest request,     // in
        HttpServletResponse response,   // in/out
        StringBuffer targetApplication  // in/out
    ) throws SessionException {
        debug.message("FedletSessionProvider.createSession(1) called " +info);
        return info.get(SessionProvider.PRINCIPAL_NAME);
    }
    
    /**
     * May be used by both SP and IDP side for getting an existing
     * session given an session ID.
     * @param sessionID the unique session handle.
     * @return the corresponding session object.
     * @throws SessionException if an error occurred during session
     * retrieval.
     */
    public Object getSession(String sessionID)
        throws SessionException {
        debug.message("FedletSessionProvider.getSession(1) called");
        return sessionID;
    }
    
    /**
     * May be used by both SP and IDP side for getting an existing
     * session given a browser initiated HTTP request.
     * @param request the browser initiated HTTP request.
     * @return the corresponding session object.
     * @throws SessionException if an error occurred during session
     * retrieval.
     */
    public Object getSession(HttpServletRequest request)
        throws SessionException {
        debug.message("FedletSessionProvider.getSession(2) called");
        return null;
    }
    
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
    ) throws SessionException {
        debug.message("FedletSessionProvider.invalidateSession called");
    }

    /**
     * Indicates whether the session is still valid.
     * This is useful for toolkit clean-up thread.
     * @param session Session object
     * @return boolean value indicating whether the session
     * is still valid
     */
    public boolean isValid(Object session)
        throws SessionException {
        debug.message("FedletSessionProvider.isValid called");
        return false;
    }   

    /**
     * The returned session ID should be unique and not 
     * change during the lifetime of this session.
     */
    public String getSessionID(Object session) {
        debug.message("FedletSessionProvider.getSessionID called");
        return (String) session;
    }

    /**
     * Returns princiapl name, or user name given the session
     * object. 
     * @param session Session object.
     * @return principal name, or user name. 
     * @throws SessionException if this operation causes an error.
     */
    public String getPrincipalName(Object session)
        throws SessionException {    
        debug.message("FedletSessionProvider.getPrincipalName called");
        return (String) session;
    }
        
    /**
     * Stores a property in the session object.
     * @param session the session object.
     * @param name the property name.
     * @param values the property values.
     * @throws SessionException if setting the property causes an error.
     */
    public void setProperty(
        Object session,
        String name,
        String[] values
    ) throws SessionException {
        debug.message("FedletSessionProvider.setProperty(1) called");
    }

    /**
     * Retrieves a property from the session object.
     * @param session the session object.
     * @param name the property name.
     * @return the property values.
     * @throws SessionException if getting the property causes an error.
     */
    public String[] getProperty(Object session, String name)
        throws SessionException {
        debug.message("FedletSessionProvider.getProperty(1) called");
        return null;
    }

    /*
     * Rewrites an URL with session information in case
     * cookie setting is not supported.
     * @param session the session object.
     * @param URL the URL to be rewritten.
     * @return the rewritten URL.
     * @throws SessionException if rewritting the URL
     * causes an error.
     */
    public String rewriteURL(Object session, String URL)
        throws SessionException {
        debug.message("FedletSessionProvider.rewriteURL(1) called");
        return URL;
    }

    /**
     * Registers a listener for the session.
     *
     * @param session the session object.
     * @param listener listener for the session invalidation event.
     * 
     * @throws SessionException if adding the listener caused an error.
     */
    public void addListener(Object session, SessionListener listener)
        throws SessionException {
        debug.message("FedletSessionProvider.addListener called");
    }

    /**
     * Sets a load balancer cookie in the suppled HTTP response. The load
     * balancer cookie's value is set per server instance and is used to
     * support sticky load balancing.
     *
     * @param request The HTTP request.
     * @param response the <code>HttpServletResponse</code> that will be sent
     *        to the user.
     */
    public void setLoadBalancerCookie(HttpServletRequest request, HttpServletResponse response) {
        debug.message("FedletSessionProvider.setLoadBalancerCookie called");
    }
    
    public long getTimeLeft(Object session) {
         return 0;  
    }   
}

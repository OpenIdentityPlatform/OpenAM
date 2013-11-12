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
 * $Id: IOpenSSORequestHandler.java,v 1.1 2009/01/30 12:09:40 kalpanakm Exp $
 *
 */


package com.sun.opensso.agents.jsr196;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Map;


/**
 *
 *  IOpenSSORequestHandler  would act as an interface between Filter Framework 
 *  and the jsr115/jsr196 based agent
 * 
 */

public interface IOpenSSORequestHandler {
    
     /**
      * Initialize the Request handler with a configuration map.
      * @param config the configuration map.
      */
     public void init(Map config);

    /**
     * Checks whether client should be authenticated or not. 
     *
     * @param subject the subject that may be used by the callers
     *        to store Principals and credentials validated in the request.
     *
     * @param request the <code>HttpServletRequest</code> associated with 
     *        this Client message request.
     * 
     * @param response the <code>HttpServletResponse</code> associated with 
     *        this Client message request
     *
     * @return true if the client should be authenticated. 
     *
     */
    public boolean shouldAuthenticate(Subject subject, 
                HttpServletRequest request, HttpServletResponse response);

    /**
     * Returns Login URL for client to be redirected.
     * @param request the <code>HttpServletRequest</code>.
     * 
     * @param response the <code>HttpServletResponse</code>
     *
     * @return String Login URL
     */
    public String getLoginURL(HttpServletRequest request, HttpServletResponse response) 
            throws Exception;
    
    /**
     * 
     * Returns the principal related to the subject
     * 
     * @param request the <code>HttpServletRequest</code> from which the
     *        principal is retrieved
     * @param subject the Subject 
     * 
     * @return name of the principal
     */
    
    public String getAuthPrincipal(HttpServletRequest request, Subject subject);
    
    
    /**
     * 
     * Returns the group names associated with the principal present in request
     * 
     * @param request the <code>HttpServletRequest</code> from which the 
     *        principal's group names are retrieved.
     * 
     * @param subject
     * 
     * @return array of group names
     */
    
    public String[] getAuthGroup(HttpServletRequest request, Subject subject);
    
}

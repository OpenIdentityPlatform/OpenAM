/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: WSFederationAction.java,v 1.3 2008/06/25 05:48:09 qcheng Exp $
 *
 * Portions Copyrighted 2016 ForgeRock AS.
 */

package com.sun.identity.wsfederation.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.identity.wsfederation.common.WSFederationException;

/**
 * This is the base class for WS-Federation request and response handling.
 */
public abstract class WSFederationAction {
    protected HttpServletRequest request;
    protected HttpServletResponse response;
    
    /**
     * Creates a new instance of WSFederationAction
     * @param request HTTPServletRequest for this interaction
     * @param response HTTPServletResponse for this interaction
     */
    public WSFederationAction(HttpServletRequest request,
        HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }
    
    /**
     * Processes the sign-out request, returning a response via the 
     * HttpServletResponse passed to the constructor.
     */
    abstract public void process() throws ServletException, IOException, WSFederationException;
}

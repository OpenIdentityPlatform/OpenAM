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
 * $Id: AmWLJ2EEAuthHandler.java,v 1.2 2008/06/25 05:52:22 qcheng Exp $
 *
 */

package com.sun.identity.agents.weblogic.v10;

import com.sun.identity.agents.filter.IJ2EEAuthenticationHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import weblogic.servlet.security.ServletAuthentication;
import javax.security.auth.callback.CallbackHandler;
import weblogic.security.SimpleCallbackHandler;

/**
 * Class AmWLJ2EEAuthHandler does the local authtntication to Weblogic
 * application server.
 *
 */
public class AmWLJ2EEAuthHandler implements IJ2EEAuthenticationHandler {
    
    /**
     * Method authenticate
     *
     * @param userName User Name logging in
     * @param password Password for the user logging in
     * @param request HTTPServletRequest object
     * @param response HttpServletResponse object
     * @param extraData Callback data
     *
     * @return boolean status if authentication succeeded or failed
     *
     */
    public boolean authenticate(
            String userName, 
            String password,
            HttpServletRequest request,
            HttpServletResponse response,
            Object extraData) {
        
        int value = ServletAuthentication.FAILED_AUTHENTICATION;
        
        if((userName != null) && (password != null)) {
            CallbackHandler handler = new SimpleCallbackHandler(
                    userName,
                    password.getBytes());
            value = ServletAuthentication.authenticate(
                    handler,
                    (HttpServletRequest)request);
        }
        
        return (value == ServletAuthentication.AUTHENTICATED);
    }
}


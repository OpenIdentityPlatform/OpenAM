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
 * $Id: FAMWSSWLFilterImpl.java,v 1.2 2008/06/25 05:54:48 qcheng Exp $
 *
 */

package com.sun.identity.wssagents.weblogic;

import com.sun.identity.wssagents.common.FAMWSSFilterImpl;


import java.security.Principal;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Set;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;

import weblogic.servlet.security.ServletAuthentication;
import weblogic.security.SimpleCallbackHandler;

/**
 * This class is an extension of the <code>FAMWSSFilterImpl</code> class 
 * specifically for WebLogic servers. It is deployed with a webservice provider.
 * The filter intercepts and validates the incoming SOAP request from a 
 * webservice client. It also intercepts and secures the outgoing SOAP response 
 * from the webservice provider that the servlet filter is associated with.
 * In addition, it sets the user principals to the Weblogic server.
 */
public class FAMWSSWLFilterImpl extends FAMWSSFilterImpl {
    
    private void setPrincipals(Subject subject, HttpServletRequest req) {
       
        // Set the subject to the container
        int value = ServletAuthentication.FAILED_AUTHENTICATION;
        
        String userName; 
        Set principals = subject.getPrincipals();
        if ((principals != null) && (principals.size() > 0)) {
            userName = ((Principal)principals.iterator().next()).getName();
            if (_logger != null) {
                 _logger.log(Level.FINE, "FAMWSSWLFilterImpl.setPrincipals: "+
                        "username from subject is " + userName);
            }
            if (userName != null) {
                CallbackHandler cbhandler = new SimpleCallbackHandler(
                                  userName, userName.getBytes());
                value = ServletAuthentication.authenticate(cbhandler, req);
                if (value == ServletAuthentication.AUTHENTICATED) {
                    if (_logger != null) {
                       _logger.log(Level.FINE, "FAMWSSWLFilterImpl.setPrincipals: "+
                                     "Authentication succeeded!");
                    }
                } else {
                    if (_logger != null) {
                       _logger.log(Level.FINE, "FAMWSSWLFilterImpl.setPrincipals: "+
                                     "Authentication failed!");
                    }
                }
            } else {
                if (_logger != null) {
                   _logger.log(Level.FINE, "FAMWSSWLFilterImpl.setPrincipals: "+
                                     "User principal name not found!");
                }
            }
        } else {
            if (_logger != null) {
               _logger.log(Level.SEVERE, "FAMWSSWLFilterImpl.setPrincipals: "+
                                     "Subject doesn't contain principals!");
            }
        }     
    }
    
    private static Logger _logger = null;
    static {
        LogManager logManager = LogManager.getLogManager();
        _logger = logManager.getLogger(
            "javax.enterprise.system.core.security");
    }
}

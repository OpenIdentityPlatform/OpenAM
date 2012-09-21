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
 * $Id: FAMClientAuthContext.java,v 1.3 2009/05/05 01:16:12 mallas Exp $
 *
 */

package com.sun.identity.wssagents.common.provider;

import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.config.ClientAuthContext;
import java.util.logging.Logger;
import java.util.logging.Level;

public class FAMClientAuthContext implements ClientAuthContext {

    private static final Logger logger =
                   Logger.getLogger("com.sun.identity.wssagents.security");
     
    private CallbackHandler handler = null;
    //***************AuthModule Instance**********
    FAMClientAuthModule authModule = null;

    /** Creates a new instance of FAMClientAuthContext */
    public FAMClientAuthContext(String operation, Subject subject, Map map, 
        CallbackHandler callbackHandler) {
        
        this.handler = callbackHandler;
        String providerName = (String) map.get("providername");
        int svcIndex = providerName.lastIndexOf("}");
        providerName = providerName.substring(svcIndex+1);
        
        if(logger.isLoggable(Level.FINE)) {
           logger.log(Level.FINE, "FAMClientAuthContext providername : " 
                      + providerName); 
        }
               
        authModule = new FAMClientAuthModule();
        map.put("providername", providerName);
        try {
            authModule.initialize(null, null, null, map);
        } catch (AuthException e) {
            if(logger.isLoggable(Level.SEVERE))
               logger.log(Level.SEVERE, "FAMClientAuthContext.:"
                    + "Initialization failed :", e);            
        }
    }

    public AuthStatus secureRequest(MessageInfo messageInfo, 
        Subject clientSubject) throws AuthException {

        try {
            return authModule.secureRequest(messageInfo, clientSubject);
        } catch (AuthException e) {
            if(logger.isLoggable(Level.WARNING)) {
               logger.log(Level.WARNING,
                   "FAMClientAuthContext.secureRequest failed : ", e );                 
            }     
            return AuthStatus.SEND_FAILURE;
        }
        
    }

    public AuthStatus validateResponse(MessageInfo messageInfo, 
        Subject clientSubject, Subject serviceSubject) throws AuthException {

        try {
            return authModule.validateResponse(messageInfo, clientSubject, 
                serviceSubject);
        } catch (AuthException e) {
            if(logger.isLoggable(Level.WARNING)) {
               logger.log(Level.WARNING, "FAMClientAuthContext.validateResponse:"
                       + "failed : ", e);
            }
            return AuthStatus.SEND_FAILURE;
        }
        
    }

    public void cleanSubject(MessageInfo messageInfo, Subject subject) 
        throws AuthException {
        try {
            authModule.cleanSubject(messageInfo, subject);
        } catch (AuthException e) {
            if(logger.isLoggable(Level.WARNING)) {
               logger.log(Level.WARNING, "FAMClientAuthContext.cleanSubject:"
                       + "failed : ", e);
            }            
        }
    }
   
}

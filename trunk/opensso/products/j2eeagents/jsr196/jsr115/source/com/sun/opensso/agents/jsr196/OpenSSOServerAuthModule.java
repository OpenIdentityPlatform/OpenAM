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
 * $Id: OpenSSOServerAuthModule.java,v 1.1 2009/01/30 12:09:41 kalpanakm Exp $
 *
 */

package com.sun.opensso.agents.jsr196;

import java.util.Map;
import java.io.IOException;

import javax.security.auth.Subject;
import javax.security.auth.message.module.ServerAuthModule;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.auth.message.callback.GroupPrincipalCallback;

import com.sun.identity.agents.arch.IModuleAccess;


/**
 *
 * JSR196 Provider. Implements the ServerAuthModule.
 * 
 * 
 * @author kalpana
 */
public class OpenSSOServerAuthModule implements ServerAuthModule {
    
    private static Class _handler;
    
    private MessagePolicy requestPolicy;
    private MessagePolicy responsePolicy;
    private CallbackHandler cHandler;
    private Map mOptions;
    
    private IModuleAccess modAccess;
    
    private OpenSSORequestHandler handler;
    
    /**
     * Just returns null. Not implemented.
     * 
     * @return
     */
    
        
    public Class[] getSupportedMessageTypes() {
        return null;
    }
    
    /**
     * <p>Initialize this module with request and response message policies to
     * enforce, a CallbackHandler, and any module-specific configuration
     * properties.</p>
     * <p>The request policy and the response policy must not both be null.</p>
     *
     * @param requestPolicy the request policy this module must enforce, or null.
     * @param responsePolicy the response policy this module must enforce, or null.
     * @param handler CallbackHandler used to request information.
     * @param options a Map of module-specific configuration properties.
     * @throws AuthException - if module initialization fails, including for the case
     * where the options argument contains elements that are
     * not supported by the module.
     */
    
    public void initialize(MessagePolicy reqPolicy, MessagePolicy resPolicy, CallbackHandler l_handler,
	       Map l_options) throws AuthException {
        
        requestPolicy = reqPolicy;
        responsePolicy = resPolicy;
        cHandler = l_handler;
        mOptions = l_options;
                
        
        handler = OpenSSORequestHandler.getInstance();
        handler.init(l_options);                         
    }
    
    /**
     * 
     * @see javax.security.auth.message.module.ServerAuthModule
     * 
     */
     
    public void cleanSubject(MessageInfo messageInfo, Subject subject)
	throws AuthException {
        
            if(subject == null) {
                throw new AuthException("nullSubject");
            }
    }
    
    /**
     * 
     * @see javax.security.auth.message.module.ServerAuthModule
     * 
     */
      
    public AuthStatus secureResponse(MessageInfo messageInfo, 
        Subject serviceSubject) throws AuthException {
            return AuthStatus.SUCCESS;
    }
    
    /**
     * 
     * @see javax.security.auth.message.module.ServerAuthModule
     * 
     */
    
    public AuthStatus validateRequest(MessageInfo messageInfo,
			       Subject clientSubject,
			       Subject serviceSubject) throws AuthException {
        
        HttpServletRequest request = (HttpServletRequest)messageInfo.getRequestMessage();       
        HttpServletResponse response = (HttpServletResponse)messageInfo.getResponseMessage();
        
        boolean auth = false;
        String loginURL = null;
        
        try {
            auth = handler.shouldAuthenticate(clientSubject, request, response);   
        }catch(Exception e){
             e.printStackTrace();
             AuthException aue = new AuthException(e.getMessage());
             aue.initCause(e);
             throw(aue);
        }
        
        if (auth) {
            /* 
             * Authentication required.
             * Get the loginURL and redirect to the loginURL
             * loginURL is typically the OpenSSO login page 
             * 
             */
            try {                                
                loginURL = handler.getLoginURL(request, response);    
             }catch(Exception ex){
                ex.printStackTrace();
                AuthException aue = new AuthException(ex.getMessage());
                aue.initCause(ex);
                throw(aue);
            }
            
            try {
                response.sendRedirect(loginURL);
            } catch (IOException ie) {
                throw new AuthException(ie.getMessage());
            } catch (UnsupportedOperationException uae) {
                try {
                    response.sendRedirect(loginURL);
                } catch (IOException ie) {
                    AuthException ae = new AuthException(ie.getMessage());
                    ae.initCause(ie);
                    throw (ae);
                }
            }
        } else {
            /*
             * Already Authenticated.
             * Set the username and group info, so that the webcontainer gets auth info.
             */
            String username = handler.getAuthPrincipal(request, clientSubject);
            String[] groups = handler.getAuthGroup(request, clientSubject);
            
                        
            try {
            
            cHandler.handle(new Callback[]{new CallerPrincipalCallback(clientSubject, username)});
            cHandler.handle(new Callback[]{new GroupPrincipalCallback(clientSubject,groups)});
            } catch (Exception e) {
                AuthException aie = new AuthException(e.getMessage());
                aie.initCause(e);
                throw (aie);
            }
            
        }
        return AuthStatus.SUCCESS;        
    }          
}

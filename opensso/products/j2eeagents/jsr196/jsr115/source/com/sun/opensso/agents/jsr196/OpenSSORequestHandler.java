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
 * $Id: OpenSSORequestHandler.java,v 1.1 2009/01/30 12:09:41 kalpanakm Exp $
 *
 */

package com.sun.opensso.agents.jsr196;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.security.auth.Subject;

import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.iplanet.sso.SSOTokenManager;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;

import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;
import com.sun.identity.agents.arch.IModuleAccess;
import com.sun.identity.agents.arch.AgentConfiguration;
import com.sun.identity.agents.filter.AmFilterManager;

/**
 *
 * @author kalpana
 * 
 * OpenSSORequestHandler acts as interface between the JSR196/JSR115 providers
 * and the agentsdk.
 * 
 */
public class OpenSSORequestHandler implements IOpenSSORequestHandler {
    
    public PsuedoAmAgentFilter fil = null;
    public IModuleAccess modAccess = null;       
    private static OpenSSORequestHandler handler = new OpenSSORequestHandler();       
    private static String sAgentApp = null;
    private boolean shouldCont = true;
    
    private OpenSSORequestHandler() {              
        fil = new PsuedoAmAgentFilter();
        fil.initializeForJSR196();        
        modAccess = AmFilterManager.getModuleAccess();    
        String agentUrl = AgentConfiguration.getClientNotificationURL();
        int idx = agentUrl.lastIndexOf('/');
        int idx1 = agentUrl.lastIndexOf('/', idx-1);
        // get /agentapp
        sAgentApp = agentUrl.substring(idx1, idx);        
        System.out.println("AgentApp --> " + sAgentApp);
    }
    
    /**
     * 
     * @return singleton instance of OpenSSORequestHandler
     * 
     */
    
    public static OpenSSORequestHandler getInstance() {
        return handler;        
    }
    
    /**
     * 
     * @return the Filter Instance associated with the Handler
     * 
     */
    
    public PsuedoAmAgentFilter getFilter() {
        return fil;
    }
    
    public void init(Map config) {
        
    }
    
    /**
     * 
     * @return boolean - the result of the evaluation of the various handlers
     *                   depending on the Agent Property settings
     */
    
    public boolean shouldContinue(){                
        return shouldCont;
    }
    
    /**
     * 
     * This method determines whether authenication is required or not depending
     * on the parameters passed on   
     *      
     * @param subject 
     * @param request
     * @param response
     * @return boolean true if authenication is required false if not required
     * 
     */
    
    public boolean shouldAuthenticate(Subject subject, HttpServletRequest request, 
                                HttpServletResponse response){                
                
        shouldCont = fil.getConfigurationResult(request, response);        
        //shouldCont = shouldCont || request.getContextPath().equals("/agentapp");
        shouldCont = shouldCont || request.getContextPath().equals(sAgentApp);
        if (modAccess.isLogMessageEnabled()) {
            modAccess.logMessage("OSSORH: Should Authenticate :: " + 
                    request.getContextPath() +"shouldCont");
        }
        if (!shouldCont) {
            if(setTokenInSubject(subject, request)) {
                if(modAccess.isLogMessageEnabled()) {
                    modAccess.logMessage("OSSORH: HTTPRequestHandler.shouldAuthenticate:: " + 
                        "valid SSOToken exists");
           }
           return false;
        }                  
        return true;             
        }
        else             
            return !shouldCont;             
    }
    
    /**
     *
     * Returns the login url 
     *
     * @param request
     * @param response
     * @return
     * @throws java.lang.Exception
     */
    
    public String getLoginURL(HttpServletRequest request, HttpServletResponse response) throws Exception {               
        
        String loginURL = fil.getLoginURL(request, response);
                
        StringBuffer requestURL = request.getRequestURL();
        
        String gotoparam = SystemPropertiesManager.get(
                "com.sun.identity.agents.config.redirect.param", "goto");
        loginURL = loginURL + "?" + gotoparam + "=" + requestURL.toString();
        String query = request.getQueryString();
        if(query != null) {
           loginURL = loginURL + "&" + query;
        }
        if (modAccess.isLogMessageEnabled()) {
            modAccess.logMessage("OSSORH: LoginURL  :: " + loginURL);
        }
        return loginURL;               
    }
    
    /**
     * 
     * get the Authentication principal associated with the Request
     * 
     * @param request
     * @param subject
     * @return
     */
    
    public String getAuthPrincipal(HttpServletRequest request, Subject subject) {
        String usrname = null;
        try {
            SSOTokenManager manager = SSOTokenManager.getInstance();
            SSOToken ssoToken = manager.createSSOToken(request);
            
            AMIdentity amid = new AMIdentity(ssoToken);
            
            usrname = amid.getName();                        
            
        }catch (SSOException soe) {
            // donot handle or throw the exceptions. 
            // used for handling non-enforced urls
            return usrname;
        }catch (Exception e) {
            return usrname;
        }
        return usrname;
        
    }
    
    /**
     * 
     * get the group names associated with the principal in the request
     * 
     * @param request
     * @param subject
     * @return
     */
    
    public String[] getAuthGroup(HttpServletRequest request, Subject subject) {
        Set groups = null;
        Iterator itr = null;
        String[] retval = null;
        int i = 0;
        try {
            SSOTokenManager manager = SSOTokenManager.getInstance();
            SSOToken ssoToken = manager.createSSOToken(request);
            
            AMIdentity amid = new AMIdentity(ssoToken);
            
            groups = amid.getMemberships(IdType.GROUP);
            
            retval = new String[groups.size()];
                                    
            for(itr= groups.iterator(); itr.hasNext(); ){
                AMIdentity groupname = (AMIdentity) itr.next();
                retval[i++] = groupname.getName();                
            }                            
        } catch (SSOException soe){
            return retval;
        }catch (Exception e){
            return retval;
        }
        return retval;        
    }
    
    /**
     * 
     * verifies whether there is a valid token present in the request
     * 
     * @param subject
     * @param request
     * @return
     */
    
    private boolean setTokenInSubject(Subject subject,
            HttpServletRequest request) {
        try {
            SSOTokenManager manager = SSOTokenManager.getInstance();
            SSOToken ssoToken = manager.createSSOToken(request);
            if(manager.isValidToken(ssoToken)) {
               addSSOToken(ssoToken, subject);
               if(modAccess.isLogMessageEnabled()) {
                  modAccess.logMessage("HTTPRequestHandler.setTokenInSubject: " 
                          + " Valid SSOToken ");               
               }
               return true;
            } else {
                return false;
            }
        } catch (SSOException se) {
            if(modAccess.isLogMessageEnabled()) {
                modAccess.logMessage("HTTPRequestHandler.setTokenInSubject: " + 
                        "Invalid SSOToken ");
            }
            return false;
        } catch (Exception e) {
            if(modAccess.isLogMessageEnabled()) {
                modAccess.logMessage("HTTPRequestHandler.setTokenInSubject: " + 
                        "Can not set SSOToken in Subject ", e);
            }
            return false;
        }
    }    
    
    private void addSSOToken(SSOToken ssoToken, Subject subject)
            throws Exception {

         final SSOToken sToken = ssoToken;
         final Subject subj = subject;
         AccessController.doPrivileged(new PrivilegedAction() {
                public java.lang.Object run() {
                    subj.getPrivateCredentials().add(sToken);
                    return null;
                }
         });         
    }     
}

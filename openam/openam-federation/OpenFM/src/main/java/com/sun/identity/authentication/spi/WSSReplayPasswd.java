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
 * $Id: WSSReplayPasswd.java,v 1.3 2009/11/10 08:37:28 mrudul_uchil Exp $
 *
 */

package com.sun.identity.authentication.spi;

import com.iplanet.services.util.Crypt;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import java.util.Map;
import java.util.Set;
import java.security.AccessController;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.shared.debug.Debug;

/**
 * This class is used to set the encrypted password as a session property.
 * This is a convenient class primarily used for web services security 
 * user name token profile where the end user password is encrypted.  
 */
public class WSSReplayPasswd implements AMPostAuthProcessInterface {
   
    private static final String PASSWORD_TOKEN = "IDToken2";
    private static boolean useHashedPassword = 
            Boolean.valueOf(SystemConfigurationUtil.getProperty(
            "com.sun.identity.wss.security.useHashedPassword", "true"));
    private static Debug debug = Debug.getInstance("WebServicesSecurity");
            
            
    
    /** 
     * Post processing on successful authentication.
     * @param requestParamsMap contains HttpServletRequest parameters
     * @param request HttpServlet  request
     * @param response HttpServlet response
     * @param ssoToken user's session
     * @throws AuthenticationException if there is an error while setting
     * the session paswword property
     */
    public void onLoginSuccess(Map requestParamsMap,
        HttpServletRequest request,
        HttpServletResponse response,
        SSOToken ssoToken) throws AuthenticationException {
        
        try {
            if(!useHashedPassword) {
               String userpasswd = request.getParameter(PASSWORD_TOKEN);
               if (userpasswd != null) {
                   ssoToken.setProperty("EncryptedUserPassword", 
                       Crypt.encrypt(userpasswd));
               }
            } else {
               String userName = ssoToken.getPrincipal().getName();
               String universalID =
                   ssoToken.getProperty("sun.am.UniversalIdentifier");
               if(debug.messageEnabled()) {
                  debug.message("WSSReplayPassword:Authenticated user : "
                          + userName);
                  debug.message("WSSReplayPassword:Authenticated UUID : "
                          + universalID);
               }
               AMIdentity amId = new AMIdentity(getAdminToken(), universalID);
               Set tmp = amId.getAttribute("userPassword");
               if(tmp != null && !tmp.isEmpty()) {
                  String userPassword = (String)tmp.iterator().next();                  
                  ssoToken.setProperty("HashedUserPassword", userPassword);
               }                
            }
        } catch (SSOException sse) {
            debug.warning("WSSReplayPasswd.onLoginSuccess: " +
                    "sso exception", sse);
        } catch (IdRepoException ire) {
            if(debug.warningEnabled()) {
               debug.warning("WSSReplayPassword.onLoginSuccess: ", ire); 
            }
            
        }
    }

    /** 
     * Post processing on failed authentication.
     * @param requestParamsMap contains HttpServletRequest parameters
     * @param req HttpServlet request
     * @param res HttpServlet response
     * @throws AuthenticationException if there is an error
     */
    public void onLoginFailure(Map requestParamsMap,
        HttpServletRequest req,
        HttpServletResponse res) throws AuthenticationException {
           
    }

    /** 
     * Post processing on Logout.
     * @param req HttpServlet request
     * @param res HttpServlet response
     * @param ssoToken user's session
     * @throws AuthenticationException if there is an error
     */
    public void onLogout(HttpServletRequest req,
        HttpServletResponse res,
        SSOToken ssoToken) throws AuthenticationException {           
    }
    
     private static SSOToken getAdminToken() {
        return (SSOToken) AccessController.doPrivileged(
                         AdminTokenAction.getInstance());                            
    }
}

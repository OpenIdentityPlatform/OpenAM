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
 * $Id: FirstTimeLogin.java,v 1.1 2009/07/24 23:05:52 manish_rustagi Exp $
 *
 */

package com.sun.identity.authentication.spi;

// import com.iplanet.am.util.Debug;
import com.sun.identity.shared.debug.Debug;

import com.iplanet.am.util.Misc;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.service.AuthUtils;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import java.io.IOException;
import java.lang.System;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * This class is used for OpenSSO/IDM integration and will check for the value
 * of the attribute specified under advance property 
 * "com.sun.identity.firsttime_login_attr_name"
 * This attribute would be like a "flag", that indicates whether a user has
 * set his personal challenge questions. A value of "true" would indicate that
 * the user has yet to set/configure them. A value of "false" would indicate 
 * that the user has indeed set/configured them. If the value of the attribute
 * is "true", OpenSSO would redirect the user to an IDM url after authentication.
 * If the value of the flag is "false", OpenSSO would do no special processing
 * and the user will be redirected to the URL, as specified in the 'goto' 
 * parameter
 */
public class FirstTimeLogin implements AMPostAuthProcessInterface {

    /**
     * Add this attribute as an advance property
     * This attribute would be like a "flag", that indicates whether a user has set his
     * personal challenge questions. A value of "true" could indicate that the user has
     * indeed set/configured them. A value of "false" could indicate that the user has
     * yet to set/configure them.
     */
    private static final String FIRSTTIME_LOGIN_ATTR_NAME =
        "com.sun.identity.firsttime_login_attr_name";  

    private static Debug debug = Debug.getInstance("FirstTimeLogin");

    /** 
     * Post processing on successful authentication.
     * @param requestParamsMap contains HttpServletRequest parameters
     * @param request HttpServlet  request
     * @param response HttpServlet response
     * @param ssoToken user's session
     * @throws AuthenticationException if there is an error
     */
    public void onLoginSuccess(Map requestParamsMap,
        HttpServletRequest request,
        HttpServletResponse response,
        SSOToken ssoToken) throws AuthenticationException {

        if (debug.messageEnabled()) {
            debug.message("FirstTimeLogin.onLoginSuccess called: Req:" + 
                request.getRequestURL());
        }

        String strAttributeName = 
            SystemProperties.get(FIRSTTIME_LOGIN_ATTR_NAME);

        try {

            if(strAttributeName != null && !strAttributeName.trim().equals("")){
                AMIdentity amIdentityUser = IdUtils.getIdentity(ssoToken);
                Map attrMap = amIdentityUser.getAttributes();
                String strAttributeValue = Misc.getMapAttr(
                    attrMap, strAttributeName, null);
                if (debug.messageEnabled()) {
                    debug.message("FirstTimeLogin.onLoginSuccess: " + 
                        strAttributeName + "=" + strAttributeValue);
                }
                // If the value of the attribute is "true", OpenSSO would redirect the user to an
                // IDM url after authentication. If the value of the flag is "false", OpenSSO would
                // do no special processing and the user will be redirected to the URL, as specified
                // in the 'goto' parameter
                if(strAttributeValue != null && strAttributeValue.equalsIgnoreCase("true")){
                    if (request != null){
                        //Change the IDM url so that it points to the correct IDM application
                        request.setAttribute(
							AMPostAuthProcessInterface.POST_PROCESS_LOGIN_SUCCESS_URL,"http://localhost:8081/idm/user/main.jsp?goto=http://mail.yahoo.com");
                    }
                }				
            }

            if (debug.messageEnabled()) {
                debug.message("FirstTimeLogin.onLoginSuccess: FirstTimeLogin " +
                    "concluded successfully");
            }
        } catch (IdRepoException ire) {
            debug.error("FirstTimeLogin.onLoginSuccess: IOException while " +
                "fetching user attributes: " + ire);
        } catch (SSOException sse) {
            debug.error("FirstTimeLogin.onLoginSuccess: SSOException " + sse);
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
            debug.message("FirstTimeLogin.onLoginFailure: called");
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
            debug.message("FirstTimeLogin.onLogout called");
    }
}
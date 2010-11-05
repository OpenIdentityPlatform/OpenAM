/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AMPostAuthProcessInterface.java,v 1.5 2009/01/16 23:31:34 higapa Exp $
 *
 */


package com.sun.identity.authentication.spi;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.iplanet.sso.SSOToken;

/**
 * The <code>AMPostAuthProcessInterface</code> interface needs to
 * be implemented by services and applications to do post
 * authentication processing.
 * <p>
 * This interface is invoked by OpenSSO Authentication
 * service on a successful authentication , failed authentication
 * or during logout.
 * <p>
 * This interface has three methods <code>onLoginSuccess</code>,
 * <code>onLoginFailure</code> and <code>onLogout</code>. The
 * <code>onLoginSucess</code> will be invoked when authentication
 * is successful. The <code>onFailure</code> will be invoked on failed
 * authentication. The <code>onLogout</code> is invoked during a logout.
 * <p>
 * The post processing class implementation can be configured per ORGANIZATION
 * or SERVICE or ROLE
 *
 * @supported.all.api
 */
public interface AMPostAuthProcessInterface {

    /**
     * Constant to represent SPI redirect URL on login success.
     * Following sample code explains how to use this onLoginSuccess.
     * <code>
     *   public void onLoginSuccess(Map requestParamsMap,HttpServletRequest request,
     *      HttpServletResponse response,SSOToken ssoToken)throws AuthenticationException
     *   {
     *     // Set redirect URL on login success, User will be redirected to this URL on success.
     *     if (request != null)
     *          request.setAttribute(
     *              AMPostAuthProcessInterface.POST_PROCESS_LOGIN_SUCCESS_URL,
     *              "http://www.sun.com");
     *   }
     *</code>
     * Note: Setting this property will take precendence over a session proeprty
     * <code> POST_PROCESS_SUCCESS_URL </code>, which can also be configured to
     * redirect users after successful authentication.
     */

    public static final String POST_PROCESS_LOGIN_SUCCESS_URL =
        "PostProcessLoginSuccessURL";

    /**
     * Constant to represent SPI redirect URL on login failure.
     * Following sample code explains how to use this onLoginFailure.
     * <code>
     *   public void onLoginFailure(Map requestParamsMap,HttpServletRequest request,
     *      HttpServletResponse response,SSOToken ssoToken)throws AuthenticationException
     *   {
     *     // Set redirect URL on login failure, User will be redirected to this URL on failure.
     *     if (request != null)
     *          request.setAttribute(
     *              AMPostAuthProcessInterface.POST_PROCESS_LOGIN_FAILURE_URL,
     *              "http://www.example.com");
     *   }
     *</code>
     */
    public static final String POST_PROCESS_LOGIN_FAILURE_URL =
        "PostProcessLoginFailureURL";

    /**
     * Constant to represent SPI redirect URL on logout.
     * Following sample code explains how to use this onLogout.
     * <code>
     *   public void onLoginFailure(Map requestParamsMap,HttpServletRequest request,
     *      HttpServletResponse response,SSOToken ssoToken)throws AuthenticationException
     *   {
     *     // Set redirect URL on logout, User will be redirected to this URL on logout.
     *     if (request != null)
     *          request.setAttribute(
     *              AMPostAuthProcessInterface.POST_PROCESS_LOGOUT_URL,
     *              "http://opensso.dev.java.net");
     *   }
     *</code>
     */
    public static final String POST_PROCESS_LOGOUT_URL =
        "PostProcessLogoutURL";

    /**
     * Post processing on successful authentication.
     *
     * @param requestParamsMap map containing <code>HttpServletRequest</code>
     *        parameters
     * @param request <code>HttpServletRequest</code> object.
     * @param response <code>HttpServletResponse</code> object.
     * @param ssoToken authenticated user's single sign token.
     * @exception AuthenticationException if there is an error.
     */
    public void onLoginSuccess(
        Map requestParamsMap,
        HttpServletRequest request,
        HttpServletResponse response,
        SSOToken ssoToken
    ) throws AuthenticationException;

    /**
     * Post processing on failed authentication.
     *
     * @param requestParamsMap map containing <code>HttpServletRequest<code>
     *        parameters.
     * @param request <code>HttpServletRequest</code> object.
     * @param response <code>HttpServletResponse</code> object.
     * @throws AuthenticationException when there is an error.
     */
    public void onLoginFailure(
        Map requestParamsMap,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws AuthenticationException;

    /**
     * Post processing on Logout.
     *
     * @param request <code>HttpServletRequest</code> object.
     * @param response <code>HttpServletResponse</code> object.
     * @param ssoToken authenticated user's single sign on token.
     * @throws AuthenticationException
     */
    public void onLogout(
        HttpServletRequest request,
        HttpServletResponse response,
        SSOToken ssoToken
    ) throws AuthenticationException;
}

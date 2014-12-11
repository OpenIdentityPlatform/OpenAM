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
 * $Id: FederationSPAdapter.java,v 1.4 2008/06/25 05:46:50 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.federation.plugins;

import com.sun.identity.federation.common.FederationException;
import com.sun.identity.federation.message.FSAuthnRequest;
import com.sun.identity.federation.message.FSAuthnResponse;
import com.sun.identity.federation.message.FSFederationTerminationNotification;
import com.sun.identity.federation.message.FSLogoutNotification;
import com.sun.identity.federation.message.FSLogoutResponse;
import com.sun.identity.federation.message.FSNameRegistrationRequest;
import com.sun.identity.federation.message.FSNameRegistrationResponse;
import com.sun.identity.federation.message.FSResponse;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The interface <code>FederationSPAdapter</code> could be implemented to 
 * perform user specific processing during federation process on the
 * Liberty Service Provider side.   
 * <p>
 * A singleton instance of this <code>FederationSPAdapter</code> will be used 
 * during runtime, so make sure implementation of the federation 
 * processing methods (except initialize() method) are thread safe. 
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public interface FederationSPAdapter {

    /**
     * Federation or Single Sign on process succeed at <code>SP</code> side.
     */
    public static final int SUCCESS = 0;

    /**
     * Response from <code>IDP</code> with Browser POST or LECP profile contains     * non-Success status code.
     */
    public static final int INVALID_AUTHN_RESPONSE = 1;

    /**
     * Response from <code>IDP</code> with Browser Artifact profile contains
     * non-Success status code.
     */
    public static final int INVALID_RESPONSE = 2;

    /**
     * Account federation failed.
     */
    public static final int FEDERATION_FAILED = 3;

    /**
     * Account federation failed because it failed to generate user token.
     */
    public static final int FEDERATION_FAILED_SSO_TOKEN_GENERATION = 4;

    /**
     * Account federation failed because it failed to generate anonymous
     * token.
     */
    public static final int FEDERATION_FAILED_ANON_TOKEN_GENERATION = 5;

    /**
     * Account federation failed because anonymous user account is inactive.
     */
    public static final int FEDERATION_FAILED_ANON_AUTH_USER_INACTIVE = 6;

    /**
     * Account federation failed because anonymous user account is locked.
     */
    public static final int FEDERATION_FAILED_ANON_AUTH_USER_LOCKED = 7;

    /**
     * Account federation failed because anonymous user account is expired.
     */
    public static final int FEDERATION_FAILED_ANON_AUTH_ACCOUNT_EXPIRED = 8;

    /**
     * Account federation failed because it failed to write account federation
     * info.
     */
    public static final int FEDERATION_FAILED_WRITING_ACCOUNT_INFO = 9;

    /**
     * Single Sign On failed.
     */
    public static final int SSO_FAILED = 10;

    /**
     * Single Sign On failed because federation info does not exist at
     * <code>SP</code> side.
     */
    public static final int SSO_FAILED_FEDERATION_DOESNOT_EXIST = 11;

    /**
     * Single Sign On failed because it failed to find auto federation user.
     */
    public static final int SSO_FAILED_AUTO_FED = 12;

    /**
     * Single Sign On failed because the user account is inactive.
     */
    public static final int SSO_FAILED_AUTH_USER_INACTIVE = 13;

    /**
     * Single Sign On failed because the user account is locked.
     */
    public static final int SSO_FAILED_AUTH_USER_LOCKED = 14;

    /**
     * Single Sign On failed because the user account is expired.
     */
    public static final int SSO_FAILED_AUTH_ACCOUNT_EXPIRED = 15;

    /**
     * Single Sign On failed because it failed to generate user token.
     */
    public static final int SSO_FAILED_TOKEN_GENERATION = 16;

    /**
     * Adapter's initialization parameter name for realm.
     */
    public static final String ENV_REALM = "REALM=";

    /**
     * Initializes the federation adapter, this method will only be executed
     * once after creation of the adapter instance.
     * @param hostedEntityID entity ID for the hosted SP
     * @param initParams  initial set of parameters(such as REALM) configured
     *  in the service provider for this adapter.
     */
    public void initialize(String hostedEntityID, Set initParams);
 
    /**
     * Invokes before federation manager sends the Single-Sing-On and Federation
     * request to IDP. 
     * @param hostedEntityID entity ID for the hosted SP
     * @param idpEntityID entity id for the IDP to which the request will 
     * 		be sent
     * @param request servlet request
     * @param response servlet response
     * @param authnRequest the authentication request to be send to IDP 
     */
    public void preSSOFederationRequest(
        String hostedEntityID, 
        String idpEntityID,
        HttpServletRequest request, 
        HttpServletResponse response, 
        FSAuthnRequest authnRequest);


    /**
     * Invokes when the FM received the Single-Sign-On and Federation response
     * from the IDP, this is called before any processing started on SP side.
     * @param hostedEntityID entity ID for the hosted SP
     * @param request servlet request
     * @param response servlet response
     * @param authnRequest the original authentication request sent from SP 
     * @param authnResponse response from IDP if Browser POST or LECP profile
     *		is used for the request, value will be null if Browser Artifact
     *		profile is used. 
     * @param samlResponse response from IDP if Browser Artifact profile is used
     *		for the request, value will be null if Browser POST or LECP 
     * 		profile is used.
     * @exception FederationException if user want to fail the process.
     */
    public void preSSOFederationProcess(
        String hostedEntityID, 
        HttpServletRequest request, 
        HttpServletResponse response, 
        FSAuthnRequest authnRequest, 
        FSAuthnResponse authnResponse,
        FSResponse samlResponse)
    throws FederationException;

    /**
     * Invokes after Single-Sign-On and Federation processing is successful.
     * @param hostedEntityID Entity ID for the hosted SP
     * @param request servlet request
     * @param response servlet response
     * @param ssoToken   user's SSO Token 
     * @param authnRequest the original authentication request sent from SP 
     * @param authnResponse response from IDP if Browser POST or LECP profile
     *		is used for the request, value will be null if Browser Artifact
     *		profile is used. 
     * @param samlResponse response from IDP if Browser Artifact profile is used
     *		for the request, value will be null if Browser POST or LECP 
     * 		profile is used.
     * @return true if browser redirection happened, false otherwise.
     * @exception FederationException if user want to fail the process.
     */
    public boolean postSSOFederationSuccess(
        String hostedEntityID, 
        HttpServletRequest request, 
        HttpServletResponse response, 
        Object ssoToken,
        FSAuthnRequest authnRequest, 
        FSAuthnResponse authnResponse,
        FSResponse samlResponse)
    throws FederationException;


    /**
     * Invokes after Single-Sign-On or Federation processing is failed.
     * @param hostedEntityID Entity ID for the hosted SP
     * @param request servlet request
     * @param response servlet response
     * @param authnRequest the original authentication request sent from SP
     * @param authnResponse response from IDP if Browser POST or LECP profile
     *          is used for the request, value will be null if Browser Artifact
     *          profile is used.
     * @param samlResponse response from IDP if Browser Artifact profile is used     *          for the request, value will be null if Browser POST or LECP
     *          profile is used.
     * @param failureCode an integer specifies the failure code. Possible
     *          failure codes are defined in this interface.
     * @return true if browser redirection happened, false otherwise.
     */
    public boolean postSSOFederationFailure(String hostedEntityID,
                                HttpServletRequest request,
                                HttpServletResponse response,
                                FSAuthnRequest authnRequest,
                                FSAuthnResponse authnResponse,
                                FSResponse samlResponse,
                                int failureCode);


    /**
     * Invokes after Register Name Identifier processing is successful 
     * @param hostedEntityID Entity ID for the hosted SP
     * @param request servlet request
     * @param response servlet response
     * @param userDN DN of the user with whom name identifier registration
     *        performed
     * @param regRequest register name identifier request, value will be
     *                null if the request object is not available
     * @param regResponse register name identifier response, value will be
     *		null if the response object is not available
     * @param regProfile register name identifier profile used, one of following
     *		IFSConstants.NAME_REGISTRATION_SP_HTTP_PROFILE
     *		IFSConstants.NAME_REGISTRATION_SP_SOAP_PROFILE
     *		IFSConstants.NAME_REGISTRATION_IDP_HTTP_PROFILE
     *		IFSConstants.NAME_REGISTRATION_IDP_SOAP_PROFILE
     */
    public void postRegisterNameIdentifierSuccess(
        String hostedEntityID,
        HttpServletRequest request,
        HttpServletResponse response,
        String userDN,
        FSNameRegistrationRequest regRequest,
        FSNameRegistrationResponse regResponse,
        String regProfile);

    /**
     * Invokes after the service provider successfully terminates federation 
     * with IDP.
     * @param hostedEntityID Entity ID for the hosted SP
     * @param request servlet request
     * @param response servlet response
     * @param userDN DN of the user with whom name identifier registration
     *        performed
     * @param notification federation termination notification message 
     * @param termProfile federation termination profile used, one of following
     *		IFSConstants.TERMINATION_SP_HTTP_PROFILE
     *		IFSConstants.TERMINATION_SP_SOAP_PROFILE
     *		IFSConstants.TERMINATION_IDP_HTTP_PROFILE
     *		IFSConstants.TERMINATION_IDP_SOAP_PROFILE
     */
    public void postTerminationNotificationSuccess(
        String hostedEntityID, 
        HttpServletRequest request, 
        HttpServletResponse response,
        String userDN,
        FSFederationTerminationNotification notification,
        String termProfile);

    /**
     * Invokes before single logout process started on FM side. This method
     * is called before the user token is invalidated on the service provider
     * side. 
     * @param hostedEntityID Entity ID for the hosted SP
     * @param request servlet request
     * @param response servlet response
     * @param userDN user DN
     * @param logoutRequest single logout request object 
     * @param logoutResponse single logout response, value will be
     *          null if the response object is not available
     * @param sloProfile single logout profile used, one of following
     *		IFSConstants.LOGOUT_SP_REDIRECT_PROFILE
     *		IFSConstants.LOGOUT_SP_SOAP_PROFILE
     *		IFSConstants.LOGOUT_IDP_REDIRECT_PROFILE
     *		IFSConstants.LOGOUT_IDP_SOAP_PROFILE
     */
    public void preSingleLogoutProcess(
        String hostedEntityID,
        HttpServletRequest request,
        HttpServletResponse response,
        String userDN,
        FSLogoutNotification logoutRequest,
        FSLogoutResponse logoutResponse,
        String sloProfile);

    /**
     * Invokes after single logout is successful completed, i.e. user token
     * has been invalidated.
     * @param hostedEntityID Entity ID for the hosted SP
     * @param request servlet request
     * @param response servlet response
     * @param userDN user DN
     * @param logoutRequest single logout request, value will be
     *          null if the request object is not available
     * @param logoutResponse single logout response, value will be
     *          null if the response object is not available
     * @param sloProfile single logout profile used, one of following
     *          IFSConstants.LOGOUT_SP_HTTP_PROFILE
     *          IFSConstants.LOGOUT_SP_SOAP_PROFILE
     *          IFSConstants.LOGOUT_IDP_HTTP_PROFILE
     *          IFSConstants.LOGOUT_IDP_SOAP_PROFILE
     */
    public void postSingleLogoutSuccess(
        String hostedEntityID, 
        HttpServletRequest request, 
        HttpServletResponse response, 
        String userDN,
        FSLogoutNotification logoutRequest, 
        FSLogoutResponse logoutResponse,
        String sloProfile);
} 

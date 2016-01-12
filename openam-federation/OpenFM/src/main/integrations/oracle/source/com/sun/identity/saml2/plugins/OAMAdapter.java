/*
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
 * $Id: OAMAdapter.java,v 1.4 2008/08/19 19:11:39 veiming Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */

package com.sun.identity.saml2.plugins;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.LogoutRequest;
import com.sun.identity.saml2.protocol.LogoutResponse;
import com.sun.identity.saml2.protocol.ManageNameIDRequest;
import com.sun.identity.saml2.protocol.ManageNameIDResponse;
import com.sun.identity.saml2.protocol.Response;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie; 
import java.util.Map;
import java.util.Hashtable;
import com.iplanet.sso.*;
import com.oblix.access.*;

/**
 * The <code>SAML2ServiceProviderAdapter</code> abstract class provides methods
 * that could be extended to perform user specific logics during SAMLv2 
 * protocol processing on the Service Provider side. The implementation class
 * could be configured on a per service provider basis in the extended
 * metadata configuration.   
 * <p>
 * A singleton instance of this <code>SAML2ServiceProviderAdapter</code>
 * class will be used per Service Provider during runtime, so make sure 
 * implementation of the methods are thread safe. 
 * @supported.all.api
 */

public class OAMAdapter extends SAML2ServiceProviderAdapter {


    /**
     * Status code for Single Sign-on success.
     */
    public static final int SUCCESS = 0;

    /**
     * Status code for invalid response from <code>IDP</code>.
     */
    public static final int INVALID_RESPONSE = 1;

    /**
     * Status code for federation failure due to unable to write account 
     * federation info.
     */
    public static final int FEDERATION_FAILED_WRITING_ACCOUNT_INFO = 3;

    /**
     * Status code for Single Sign-On failure due to internal session error.
     */
    public static final int SSO_FAILED_SESSION_ERROR = 4;

    /**
     * Status code for Single Sign-On failure due attribute mapping error.
     */
    public static final int SSO_FAILED_ATTRIBUTE_MAPPING = 5;

    /**
     * Status code for Single Sign-On failure due to no user mapping.
     */
    public static final int SSO_FAILED_NO_USER_MAPPING = 6;

    /**
     * Status code for Single Sign-On failure due to inactive user account.
     */
    public static final int SSO_FAILED_AUTH_USER_INACTIVE = 7;

    /**
     * Status code for Single Sign-On failure due to locked user account.
     */
    public static final int SSO_FAILED_AUTH_USER_LOCKED = 8;

    /**
     * Status code for Single Sign-On failure due to expired user account.
     */
    public static final int SSO_FAILED_AUTH_ACCOUNT_EXPIRED = 9;

    /**
     * Status code for Single Sign-On failure due to unable to generate 
     * user session. 
     */
    public static final int SSO_FAILED_SESSION_GENERATION = 10;

    /**
     * Constants for hosted entity id parameter
     */
    public static final String HOSTED_ENTITY_ID = "HOSTED_ENTITY_ID";
    
    /**
     * Constants for the realm of the hosted entity parameter.
     */
    public static final String REALM = "REALM";

    private String cookieDomain = null;
    private String famCookieName = null;
    private String oamCookieName = null;
    private String protocol = null;
    private String resource = null;
    private String action = null;
    private ObConfig obconfig = null;

    /**
     * Initializes the federation adapter, this method will only be executed
     * once after creation of the adapter instance.
     * @param initParams  initial set of parameters configured in the service
     * 		provider for this adapter. One of the parameters named
     *          <code>HOSTED_ENTITY_ID</code> refers to the ID of this 
     *          hosted service provider entity, one of the parameters named
     *          <code>REALM</code> refers to the realm of the hosted entity.
     */
    public void initialize(Map initParams) {

         String sdkInstallDir = (String)initParams.get("OracleAMSDKDir");
         protocol = (String)initParams.get("Protocol");
         resource = (String)initParams.get("Resource");
         action = (String)initParams.get("Action");
         cookieDomain = (String)initParams.get("CookieDomain");
         famCookieName = (String)initParams.get("FAMCookieName");
         oamCookieName = (String)initParams.get("OAMCookieName");

         System.out.println("Agent initialization details:" +
                   sdkInstallDir + " "+
                   protocol + " " +
                   action + " " +
                   famCookieName + " " +
                   oamCookieName + " " +
                   resource); 
         try {
             obconfig = new ObConfig();
             obconfig.initialize(sdkInstallDir);
         } catch (ObAccessException oae) {
             oae.printStackTrace();
             System.out.println("Oblix SDK initialization failed");
         }
                   
    }
 
    /**
     * Invokes before OpenAM sends the
     * Single-Sign-On request to IDP. 
     * @param hostedEntityID entity ID for the hosted SP
     * @param idpEntityID entity id for the IDP to which the request will 
     * 		be sent. This will be null in ECP case.
     * @param realm Realm of the hosted SP.
     * @param request servlet request
     * @param response servlet response
     * @param authnRequest the authentication request to be send to IDP 
     * @exception SAML2Exception if user want to fail the process.
     */
    public void preSingleSignOnRequest(
        String hostedEntityID, 
        String idpEntityID,
        String realm,
        HttpServletRequest request, 
        HttpServletResponse response, 
        AuthnRequest authnRequest)
    throws SAML2Exception {
        return;
    }


    /**
     * Invokes when the OpenAM Server received the Single-Sign-On
     * response from the IDP, this is called before any processing started on
     * SP side.
     * @param hostedEntityID entity ID for the hosted SP
     * @param realm Realm of the hosted SP.
     * @param request servlet request
     * @param response servlet response
     * @param authnRequest the original authentication request sent from SP, 
     *       null if this is IDP initiated SSO.
     * @param ssoResponse response from IDP 
     * @param profile protocol profile used, one of the following values: 
     *     <code>SAML2Constants.HTTP_POST</code>, 
     *     <code>SAML2Constants.HTTP_ARTIFACT</code>,
     *     <code>SAML2Constants.PAOS</code>
     * @exception SAML2Exception if user want to fail the process.
     */
    public void preSingleSignOnProcess(
        String hostedEntityID, 
        String realm,
        HttpServletRequest request, 
        HttpServletResponse response, 
        AuthnRequest authnRequest, 
        Response ssoResponse,
        String profile)
    throws SAML2Exception {
        return;
    }

    /**
     * Invokes after Single-Sign-On processing succeeded.
     * @param hostedEntityID Entity ID for the hosted SP
     * @param realm Realm of the hosted SP.
     * @param request servlet request
     * @param response servlet response
     * @param session user's session
     * @param authnRequest the original authentication request sent from SP, 
     *       null if this is IDP initiated SSO.
     * @param ssoResponse response from IDP 
     * @param profile protocol profile used, one of the following values: 
     *     <code>SAML2Constants.HTTP_POST</code>, 
     *     <code>SAML2Constants.HTTP_ARTIFACT</code>,
     *     <code>SAML2Constants.PAOS</code>
     * @param isFederation true if this is federation case, false otherwise.
     * @return true if browser redirection happened after processing, 
     *     false otherwise. Default to false. 
     * @exception SAML2Exception if user want to fail the process.
     */
    public boolean postSingleSignOnSuccess(
        String hostedEntityID, 
        String realm,
        HttpServletRequest request, 
        HttpServletResponse response, 
        Object session,
        AuthnRequest authnRequest, 
        Response ssoResponse,
        String profile, 
        boolean isFederation)
    throws SAML2Exception {

        try {
            SSOToken ssoToken = (SSOToken)session;
            String famSession = ssoToken.getTokenID().toString();
            if(famSession == null) {
               throw new SAML2Exception("No OpenSSO Session found"); 
            }

            ObResourceRequest orq = new ObResourceRequest(
                     protocol, resource, action);
            if(orq.isProtected()) {
               ObAuthenticationScheme authScheme = 
                          new ObAuthenticationScheme(orq); 
               System.out.println("Authentication scheme is " + 
                             authScheme.getName());
               Hashtable creds = new Hashtable();
               creds.put("famsession", famSession);
               ObUserSession userSession = new ObUserSession(orq, creds);
               if(userSession.getStatus() == ObUserSession.LOGGEDIN) {
                  System.out.println("logged in principal: " +
                        userSession.getUserIdentity()); 
                  String sessionid = userSession.getSessionToken();
                  System.out.println("User session: " + sessionid);
                  Cookie oamCookie = new Cookie(oamCookieName, sessionid);
                  oamCookie.setDomain(cookieDomain);
                  response.addCookie(oamCookie);
               }
            }

            return false;   
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new SAML2Exception(ex.getMessage());
        }
    }


    /**
     * Invokes after Single Sign-On processing failed.
     * @param hostedEntityID Entity ID for the hosted SP
     * @param realm Realm of the hosted SP.
     * @param request servlet request
     * @param response servlet response
     * @param authnRequest the original authentication request sent from SP,
     *       null if this is IDP initiated SSO.
     * @param ssoResponse response from IDP 
     * @param profile protocol profile used, one of the following values: 
     *     <code>SAML2Constants.HTTP_POST</code>, 
     *     <code>SAML2Constants.HTTP_ARTIFACT</code>,
     *     <code>SAML2Constants.PAOS</code>
     * @param failureCode an integer specifies the failure code. Possible
     *          failure codes are defined in this interface.
     * @return true if browser redirection happened, false otherwise. Default to
     *         false.
     */
    public boolean postSingleSignOnFailure(
        String hostedEntityID,
        String realm,
        HttpServletRequest request,
        HttpServletResponse response,
        AuthnRequest authnRequest,
        Response ssoResponse,
        String profile, 
        int failureCode) {
        return false;
    }


    /**
     * Invokes after new Name Identifier processing succeeded. 
     * @param hostedEntityID Entity ID for the hosted SP
     * @param realm Realm of the hosted SP.
     * @param request servlet request
     * @param response servlet response
     * @param userID Universal ID of the user with whom the new name identifier
     *        request performed
     * @param idRequest New name identifier request, value will be
     *                null if the request object is not available
     * @param idResponse New name identifier response, value will be
     *		null if the response object is not available
     * @param binding Binding used for new name identifier request, 
     *        one of following values:
     *		<code>SAML2Constants.SOAP</code>,
     *		<code>SAML2Constants.HTTP_REDIRECT</code>
     */
    public void postNewNameIDSuccess(
        String hostedEntityID,
        String realm,
        HttpServletRequest request,
        HttpServletResponse response,
        String userID,
        ManageNameIDRequest idRequest,
        ManageNameIDResponse idResponse,
        String binding) {
        return;
    }

    /**
     * Invokes after Terminate Name Identifier processing succeeded. 
     * @param hostedEntityID Entity ID for the hosted SP
     * @param realm Realm of the hosted SP.
     * @param request servlet request
     * @param response servlet response
     * @param userID Universal ID of the user with whom name id termination 
     *        performed.
     * @param idRequest Terminate name identifier request. 
     * @param idResponse Terminate name identifier response, value will be
     *		null if the response object is not available
     * @param binding binding used for Terminate Name Identifier request, 
     *      one of following values:
     *		<code>SAML2Constants.SOAP</code>,
     *		<code>SAML2Constants.HTTP_REDIRECT</code>
     */
    public void postTerminateNameIDSuccess(
        String hostedEntityID, 
        String realm,
        HttpServletRequest request, 
        HttpServletResponse response,
        String userID,
        ManageNameIDRequest idRequest,
        ManageNameIDResponse idResponse,
        String binding) {
        return;
    }

    /**
     * Invokes before single logout process started on <code>SP</code> side. 
     * This method is called before the user session is invalidated on the 
     * service provider side. 
     * @param hostedEntityID Entity ID for the hosted SP
     * @param realm Realm of the hosted SP.
     * @param request servlet request
     * @param response servlet response
     * @param userID universal ID of the user 
     * @param logoutRequest single logout request object 
     * @param logoutResponse single logout response, value will be
     *          null if the response object is not available
     * @param binding binding used for Single Logout request, 
     *      one of following values:
     *		<code>SAML2Constants.SOAP</code>,
     *		<code>SAML2Constants.HTTP_REDIRECT</code>
     * @exception SAML2Exception if user want to fail the process.
     */
    public void preSingleLogoutProcess(
        String hostedEntityID,
        String realm,
        HttpServletRequest request,
        HttpServletResponse response,
        String userID,
        LogoutRequest logoutRequest,
        LogoutResponse logoutResponse,
        String binding) 
    throws SAML2Exception {
        return;
    }

    /**
     * Invokes after single logout process succeeded, i.e. user session 
     * has been invalidated.
     * @param hostedEntityID Entity ID for the hosted SP
     * @param realm Realm of the hosted SP.
     * @param request servlet request
     * @param response servlet response
     * @param userID universal ID of the user 
     * @param logoutRequest single logout request, value will be
     *          null if the request object is not available
     * @param logoutResponse single logout response, value will be
     *          null if the response object is not available
     * @param binding binding used for Single Logout request, 
     *      one of following values:
     *		<code>SAML2Constants.SOAP</code>,
     *		<code>SAML2Constants.HTTP_REDIRECT</code>
     */
    public void postSingleLogoutSuccess(
        String hostedEntityID, 
        String realm,
        HttpServletRequest request, 
        HttpServletResponse response, 
        String userID,
        LogoutRequest logoutRequest, 
        LogoutResponse logoutResponse,
        String binding) {
        return;
    }
} 

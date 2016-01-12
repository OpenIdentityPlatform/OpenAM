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
 * $Id: SMAdapter.java,v 1.4 2008/08/19 19:11:41 veiming Exp $
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
import netegrity.siteminder.javaagent.*;
import com.iplanet.sso.*;

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
 */

public class SMAdapter extends SAML2ServiceProviderAdapter {


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

    private String smServerIP = null;
    private AgentAPI agentAPI = null;
    private String agentHostName = null;
    private String resource = null;
    private String agentIP = null;
    private String agentID = null;
    private String cookieDomain = null;
    private String famCookieName = null;
    private String smCookieName = null;

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

         int authenticationPort = 0;
         int authorizationPort = 0;
         int accountingPort = 0;
         int connectionMinimum = 0;
         int connectionMaximum = 0;
         int connectionStep = 0;
         int requestTimeout = 0;
       
         String sharedSecret = (String)initParams.get("SharedSecret");
         famCookieName = (String)initParams.get("FAMCookieName");
         smCookieName = (String)initParams.get("SMCookieName");
         cookieDomain = (String)initParams.get("CookieDomain");
         String tmp  = (String)initParams.get("AuthorizationPort");
         try {
             authorizationPort = Integer.parseInt(tmp);
         } catch (Exception ex) {
            System.out.println("Invalid authorization port");
         }

         tmp = (String)initParams.get("AuthenticationPort");
         try {
             authenticationPort = Integer.parseInt(tmp);
         } catch (Exception ex) {
            System.out.println("Invalid authentication port");
         }

         tmp = (String)initParams.get("AccountingPort");
         try {
             accountingPort = Integer.parseInt(tmp);
         } catch (Exception ex) {
            System.out.println("Invalid accounting port");
         }

         tmp = (String)initParams.get("ConnectionMinimum");
         try {
             connectionMinimum = Integer.parseInt(tmp);
         } catch (Exception ex) {
            System.out.println("Invalid connection minimum");
         }

         tmp = (String)initParams.get("ConnectionMaximum");
         try {
             connectionMaximum = Integer.parseInt(tmp);
         } catch (Exception ex) {
            System.out.println("Invalid connection maximum");
         }

         tmp = (String)initParams.get("ConnectionStep");
         try {
             connectionStep = Integer.parseInt(tmp);
         } catch (Exception ex) {
            System.out.println("Invalid connection step");
         }

         tmp = (String)initParams.get("RequestTimeout");
         try {
             requestTimeout = Integer.parseInt(tmp);
         } catch (Exception ex) {
            System.out.println("Invalid request timeout");
         }

         smServerIP = (String)initParams.get("PolicyServerIPAddress");
         agentHostName = (String)initParams.get("AgentHostName");
         agentID = (String)initParams.get("AgentID");
         agentIP = (String)initParams.get("AgentIP");
         resource = (String)initParams.get("Resource");

         System.out.println("Agent initialization details:" +
                   agentHostName + " " +
                   sharedSecret + " " +
                   smServerIP + " " +
                   connectionMaximum + " " +
                   connectionMinimum + " " +
                   connectionStep + " " +
                   requestTimeout + " " +
                   authorizationPort + " "+
                   authenticationPort + " " +
                   accountingPort); 
                   
         InitDef id = new InitDef(agentHostName, sharedSecret, 
                      true, new ServerDef());
         id.addServerDef(smServerIP, connectionMinimum,
                         connectionMaximum, connectionStep,
                         requestTimeout, authorizationPort,
                         authenticationPort, accountingPort);
         agentAPI = new AgentAPI();
         int status = agentAPI.init(id);
         if(status == AgentAPI.SUCCESS) {
            System.out.println("Agent API initialization successful");
         } else {
            System.out.println("Agent API initialization failed" + status);
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
     * Invokes when the OpenAM received the Single-Sign-On response
     * from the IDP, this is called before any processing started on SP side.
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
/*
            Cookie[] cookies = request.getCookies();
            for (int i=0; i < cookies.length; i++) {
                 Cookie cookie = cookies[i];
                 if(cookie.getName().equals(famCookieName)) {
                    famSession = cookie.getValue();
                 }
            }
*/
            if(famSession == null) {
               throw new SAML2Exception("No OpenSSO Session found"); 
            }

            UserCredentials uc = new UserCredentials(
                  "FMTOKEN", "FMTOKEN" + famSession);
            SessionDef sd = new SessionDef();
            ResourceContextDef rcd = new ResourceContextDef(
                    agentID, agentHostName, resource, "GET");
            RealmDef rd = new RealmDef();
            int retCode = agentAPI.isProtected(agentIP, rcd, rd);
            if(retCode != AgentAPI.YES) {
               System.out.println("Agent is not protected."); 
               throw new SAML2Exception("Agent is not protected");
            }

            AttributeList al = new AttributeList();
            int status = agentAPI.login(agentIP, rcd, rd, uc, sd, al);
            if(status != AgentAPI.YES) {
               throw new SAML2Exception("Agent authentication failed");
            }

            Cookie smCookie = new Cookie(smCookieName, sd.spec);
            smCookie.setDomain(cookieDomain);
            response.addCookie(smCookie);
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

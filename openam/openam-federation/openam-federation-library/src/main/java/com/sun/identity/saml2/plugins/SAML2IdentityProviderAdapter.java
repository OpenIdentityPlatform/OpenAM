/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 ForgeRock AS. All Rights Reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package com.sun.identity.saml2.plugins;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.Response;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This interface <code> SAML2IdentityProviderAdapter</code> is used to perform
 * specific tasks in the IdP
 *
 * @supported.all.api
 */
public interface SAML2IdentityProviderAdapter {

    /**
     * Initializes the federation adapter, this method will only be executed
     * once after creation of the adapter instance.
     *
     * @param hostedEntityID entity ID for the hosted IDP
     * @param realm realm of the hosted IDP
     */
    public void initialize(String hostedEntityID, String realm);

    /**
     * Invokes when OpenAM receives the authentication request for the first time
     * from the SP, and is called before any processing started on the IDP side.
     * If the authentication request is subsequently cached and retrieved, this method will not be called again.
     * This method is not triggered in the case of IDP initiated SSO or a proxied request.
     *
     * @param hostedEntityID entity ID for the hosted IDP
     * @param realm realm of the hosted IDP
     * @param request servlet request
     * @param response servlet response
     * @param authnRequest the original authentication request sent from SP
     * @param reqID the id to use for continuation of processing if the adapter redirects
     * @return true if browser redirection is happening after processing, false otherwise. Default to false.
     * @throws SAML2Exception for any exceptions occurring in the adapter. The federation process will continue.
     */
    public boolean preSingleSignOn(
            String hostedEntityID,
            String realm,
            HttpServletRequest request,
            HttpServletResponse response,
            AuthnRequest authnRequest,
            String reqID)
            throws SAML2Exception;

    /**
     * Invokes when OpenAM has received the authn request, processed it, and is ready to redirect to authentication.
     * This occurs when redirecting to authentication where there is no session, or during session upgrade.
     * This method is not triggered in the case of IDP initiated SSO or a proxied request.
     *
     * @param hostedEntityID entity ID for the hosted IDP
     * @param realm realm of the hosted IDP
     * @param request servlet request
     * @param response servlet response
     * @param authnRequest the original authentication request sent from SP
     * @param session the user session or null if the user has no session
     * @param reqID the id to use for continuation of processing if the adapter redirects
     * @param relayState the relayState that will be used in the redirect
     * @return true if browser redirection is happening after processing, false otherwise. Default to false.
     * @throws SAML2Exception for any exceptions occurring in the adapter. The federation process will continue.
     */
    public boolean preAuthentication(
            String hostedEntityID,
            String realm,
            HttpServletRequest request,
            HttpServletResponse response,
            AuthnRequest authnRequest,
            Object session,
            String reqID,
            String relayState)
            throws SAML2Exception;

    /**
     * This method is invoked before sending a non-error SAML2 Response, but before the SAML Response object is
     * constructed.
     * Called after successful authentication (including session upgrade) or if a valid session already exists.
     *
     * @param authnRequest original authnrequest
     * @param hostProviderID hosted providerID.
     * @param realm realm of the hosted IDP
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @param session the user session or null if the user has no session
     * @param reqID the id to use for continuation of processing if the adapter redirects
     * @param relayState the relayState that will be used in the redirect
     * @return true if browser redirection happened after processing, false otherwise. Default to false.
     * @throws SAML2Exception if error occurs. The federation process will continue.
     */
    public boolean preSendResponse(
            AuthnRequest authnRequest,
            String hostProviderID,
            String realm,
            HttpServletRequest request,
            HttpServletResponse response,
            Object session,
            String reqID,
            String relayState)
            throws SAML2Exception;

    /**
     * Called after the SAML Response object is created, but before the Response is signed/encrypted. When artifact
     * binding is being used, this method is invoked when the response object is created, and not when the artifact
     * is actually resolved.
     * This extension point's purpose is to make it possible to adjust the content of the SAML response (for example by
     * adding custom SAML extensions), hence this method does not provide a way to abort the SAML flow.
     *
     * @param authnRequest The original SAML Authentication Request (may be null if this was an IdP initiated SSO).
     * @param res The SAML Response.
     * @param hostProviderID The entity ID of the IdP.
     * @param realm The realm the IdP belongs to.
     * @param request The HttpServletRequest object.
     * @param session The user session or null if the user has no session.
     * @param relayState The relayState that will be used in the redirect
     * @throws SAML2Exception If an error occurs. The federation process will continue.
     */
    public void preSignResponse(
            AuthnRequest authnRequest,
            Response res,
            String hostProviderID,
            String realm,
            HttpServletRequest request,
            Object session,
            String relayState) throws SAML2Exception;

    /**
     * Called before a SAML error message is returned.
     * This method is not triggered during IDP initiated SSO.
     *
     * @param request        HttpServletRequest
     * @param response       HttpServletResponse
     * @param faultCode      the fault code that will be returned in the SAML response
     * @param faultDetail    the fault detail that will be returned in the SAML response
     * @throws SAML2Exception if error occurs. The federation process will continue.
     */
    public void preSendFailureResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            String faultCode,
            String faultDetail)
            throws SAML2Exception;
}

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
 * This class <code>DefaultIDPAdapter</code> implements a SAML2 Identity Provider Adapter.
 */
public class DefaultIDPAdapter implements SAML2IdentityProviderAdapter {

    /**
     * Default Constructor.
     */
    public DefaultIDPAdapter() {
    }

    /**
     * Default implementation, takes no action.
     */
    public void initialize(String hostedEntityID, String realm) {
        // Do nothing
    }

    /**
     * Default implementation, takes no action and returns false (no interruption to processing).
     */
    public boolean preSingleSignOn(
            String hostedEntityID,
            String realm,
            HttpServletRequest request,
            HttpServletResponse response,
            AuthnRequest authnRequest,
            String reqID) throws SAML2Exception {
        return false;
    }

    /**
     * Default implementation, takes no action and returns false (no interruption to processing).
     */
    public boolean preAuthentication(
            String hostedEntityID,
            String realm,
            HttpServletRequest request,
            HttpServletResponse response,
            AuthnRequest authnRequest,
            Object session,
            String reqID,
            String relayState) throws SAML2Exception {
        return false;
    }

    /**
     * Default implementation, takes no action and returns false (no interruption to processing).
     */
    public boolean preSendResponse(
            AuthnRequest authnRequest,
            String hostProviderID,
            String realm,
            HttpServletRequest request,
            HttpServletResponse response,
            Object session,
            String reqID,
            String relayState) throws SAML2Exception {
        return false;
    }

    /**
     * Default implementation, takes no action.
     */
    public void preSendFailureResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            String faultCode,
            String faultDetail) throws SAML2Exception {
        // Do nothing
    }

    @Override
    public void preSignResponse(AuthnRequest authnRequest, Response res, String hostProviderID, String realm,
        HttpServletRequest request, Object session, String relayState) throws SAML2Exception {
        // Do nothing
    }
}

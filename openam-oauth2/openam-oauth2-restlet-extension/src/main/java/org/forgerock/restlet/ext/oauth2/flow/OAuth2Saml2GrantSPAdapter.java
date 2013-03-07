/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
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
 * "Portions copyright [year] [name of copyright owner]"
 */

/**
 * Portions copyright 2012-2013 ForgeRock Inc
 */

package org.forgerock.restlet.ext.oauth2.flow;

import com.sun.identity.saml2.assertion.impl.AssertionImpl;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.plugins.SAML2ServiceProviderAdapter;
import com.sun.identity.saml2.protocol.*;
import com.sun.identity.shared.OAuth2Constants;
import com.sun.identity.shared.encode.Base64;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.Map;

public class OAuth2Saml2GrantSPAdapter extends SAML2ServiceProviderAdapter {

    /**
     * @{inheritDoc}
     */
    public void initialize(Map initParams){
        return;
    }

    /**
     * @{inheritDoc}
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
     * @{inheritDoc}
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
     * @{inheritDoc}
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

        AssertionImpl assertion = (AssertionImpl) ssoResponse.getAssertion().get(0);
        StringBuilder sb = new StringBuilder();
        try {
            //post assertion to the OAuth 2 token endpoint using the saml2 grant.
            sb.append("<form name=\"postForm\" action=\"");
            sb.append(hostedEntityID);
            if (hostedEntityID.endsWith("/")){
                sb.append("oauth2/access_token");
            } else {
                sb.append("/oauth2/access_token");
            }
            sb.append("\" method=\"post\">");
            sb.append("<input type=\"hidden\" name=\"grant_type\" value=\"");
            sb.append(OAuth2Constants.SAML20.GRANT_TYPE_URI);
            sb.append("\">");
            sb.append("<input type=\"hidden\" name=\"assertion\" value=\"");
            sb.append(Base64.encode(assertion.toXMLString(false,false).getBytes("UTF-8")));
            sb.append("\">");
            sb.append("<input type=\"hidden\" name=\"client_id\" value=\"");
            sb.append(hostedEntityID);
            sb.append("\">");
            sb.append("</form>");
            sb.append("<script language=\"Javascript\">");
            sb.append("document.postForm.submit();");
            sb.append("</script>");
            PrintWriter pw = response.getWriter();
            pw.print(sb.toString());
        } catch (UnsupportedEncodingException e) {
            SAML2Utils.debug.error("OAuth2Saml2GrantSPAdapter.postSingleSignOnSuccess: Unsuppored Encoding Exception: "
                    + e.getMessage());
        } catch (IOException e){
            SAML2Utils.debug.error("OAuth2Saml2GrantSPAdapter.postSingleSignOnSuccess: IOException: " + e.getMessage());
        }
        return true;
    }


    /**
     * @{inheritDoc}
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
     * @{inheritDoc}
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
     * @{inheritDoc}
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
     * @{inheritDoc}
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
     * @{inheritDoc}
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

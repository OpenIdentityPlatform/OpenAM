/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2012-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.oauth2.saml2.core;

import com.sun.identity.saml2.assertion.impl.AssertionImpl;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.plugins.SAML2ServiceProviderAdapter;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.LogoutRequest;
import com.sun.identity.saml2.protocol.LogoutResponse;
import com.sun.identity.saml2.protocol.ManageNameIDRequest;
import com.sun.identity.saml2.protocol.ManageNameIDResponse;
import com.sun.identity.saml2.protocol.Response;

import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.encode.Base64url;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
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
            PrintWriter out,
            Object session,
            AuthnRequest authnRequest,
            Response ssoResponse,
            String profile,
            boolean isFederation)
            throws SAML2Exception {

        AssertionImpl assertion = (AssertionImpl) ssoResponse.getAssertion().get(0);
        StringBuilder sb = new StringBuilder();
        //post assertion to the OAuth 2 token endpoint using the saml2 grant.
        sb.append("<form name=\"postForm\" action=\"");
        sb.append(hostedEntityID);
        if (hostedEntityID.endsWith("/")){
            sb.append("oauth2/access_token");
        } else {
            sb.append("/oauth2/access_token");
        }
        sb.append("?realm=" + (StringUtils.isEmpty(realm) ? "/" : realm));
        sb.append("\" method=\"post\">");
        sb.append("<input type=\"hidden\" name=\"grant_type\" value=\"");
        sb.append(OAuth2Constants.SAML20.GRANT_TYPE_URI);
        sb.append("\">");
        sb.append("<input type=\"hidden\" name=\"assertion\" value=\"");
        sb.append(Base64url.encode(assertion.toXMLString(false, false).getBytes(StandardCharsets.UTF_8)));
        sb.append("\">");
        sb.append("<input type=\"hidden\" name=\"client_id\" value=\"");
        sb.append(hostedEntityID);
        sb.append("\">");
        sb.append("</form>");
        sb.append("<script language=\"Javascript\">");
        sb.append("document.postForm.submit();");
        sb.append("</script>");
        out.print(sb.toString());

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

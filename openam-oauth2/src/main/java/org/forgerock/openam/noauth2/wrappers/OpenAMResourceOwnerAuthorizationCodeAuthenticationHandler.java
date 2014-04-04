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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.noauth2.wrappers;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import org.forgerock.oauth2.core.exceptions.AccessDeniedException;
import org.forgerock.oauth2.core.exceptions.AuthenticationRedirectRequiredException;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.InteractionRequiredException;
import org.forgerock.oauth2.core.exceptions.LoginRequiredException;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OpenIDPromptParameter;
import org.forgerock.oauth2.core.ResourceOwner;
import org.forgerock.oauth2.core.ResourceOwnerAuthorizationCodeAuthenticationHandler;
import org.forgerock.openam.oauth2.OAuth2Utils;
import org.forgerock.openam.oauth2.provider.impl.OpenAMUser;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.errors.EncodingException;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @since 12.0.0
 */
public class OpenAMResourceOwnerAuthorizationCodeAuthenticationHandler implements ResourceOwnerAuthorizationCodeAuthenticationHandler {    //TODO some of this should be come core...

    private final HttpServletRequest request;

    OpenAMResourceOwnerAuthorizationCodeAuthenticationHandler(final HttpServletRequest request) {
        this.request = request;
    }

    public ResourceOwner authenticate() throws AuthenticationRedirectRequiredException, BadRequestException, InteractionRequiredException, LoginRequiredException, AccessDeniedException {

        final String prompt = request.getParameter("prompt");

        final OpenIDPromptParameter openIDPromptParameter = new OpenIDPromptParameter(prompt);

        if (!openIDPromptParameter.isValid()) {
            //TODO log
            throw new BadRequestException("Invalid prompt parameter");
        }

        SSOToken token = null;
        try {
            token = getToken(request);
        } catch (SSOException e) {
            OAuth2Utils.DEBUG.warning("Error authenticating user against OpenAM: ", e);
        }

        if (token != null) {

            if (openIDPromptParameter.promptLogin()){
                try {
                    final SSOTokenManager mgr = SSOTokenManager.getInstance();
                    mgr.destroyToken(token);
                } catch (SSOException e) {
                    OAuth2Utils.DEBUG.error("Error destorying SSOToken: ", e);
                }
                redirect(request);
            }

            try {
                return new OpenAMUser(token.getProperty("UserToken"), token);
            } catch (SSOException e) {
                OAuth2Utils.DEBUG.error("Error authenticating user against OpenAM: ", e);
                throw new LoginRequiredException();
            }
        } else {
            if (openIDPromptParameter.noPrompts()) {
                OAuth2Utils.DEBUG.error("Not pre-authenticated and prompt parameter equals none.");
                throw new InteractionRequiredException();
            } else if (openIDPromptParameter.promptConsent() && !openIDPromptParameter.promptLogin()) {
                OAuth2Utils.DEBUG.error("Prompt parameter doesn't allow the login prompt: ");
                throw new LoginRequiredException();
            } else {
                redirect(request);
            }
        }

        //TODO log
        throw new AccessDeniedException("The authorization server can not authorize the resource owner.");
    }

    protected void redirect(final HttpServletRequest request) throws AuthenticationRedirectRequiredException, AccessDeniedException {
        if (OAuth2Utils.DEBUG.messageEnabled()){
            OAuth2Utils.DEBUG.message("Redirecting to OpenAM login page");
        }
        final String authURL;
        final URI authURI;

        authURL = getAuthURL(request);

        try {
            authURI = new URI(authURL);
        } catch (URISyntaxException e){
            OAuth2Utils.DEBUG.error("Unable to construct authURI", e);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getMessage(), e);
        }
        final String realm = OAuth2Utils.getRealm(request);
        final String moduleName = (String) request.getAttribute("module");
        final String serviceName = (String) request.getAttribute("service");
        final String locale = (String) request.getAttribute("locale");

        final StringBuilder queryString = new StringBuilder("?");

        if (null != realm && !realm.isEmpty()) {
            queryString.append(OAuth2Constants.Custom.REALM).append("=").append(realm).append("&");
        }
        if (null != locale && !locale.isEmpty()) {
            queryString.append(OAuth2Constants.Custom.LOCALE).append("=").append(locale).append("&");
        }
        if (null != moduleName && !moduleName.isEmpty()) {
            queryString.append(OAuth2Constants.Custom.MODULE).append("=").append(moduleName).append("&");
        } else if (null != serviceName && !serviceName.isEmpty()) {
            queryString.append(OAuth2Constants.Custom.SERVICE).append("=").append(serviceName).append("&");
        }

        final String goUri;
        try {
            goUri = ESAPI.encoder().encodeForURL(request.getRequestURL().toString() + "?" + removePromptQueryParameter(request.getQueryString()));
        } catch (EncodingException e) {
            //TODO log
            throw new AccessDeniedException(e);
        }

        queryString.append(OAuth2Constants.Custom.GOTO).append("=").append(goUri);

        final URI redirectUri;
        if (queryString.length() > 0) {
            redirectUri = URI.create(authURL + queryString.toString());
        } else {
            redirectUri = URI.create(authURL);
        }

        throw new AuthenticationRedirectRequiredException(redirectUri);
    }

    private String removePromptQueryParameter(final String requestUri) {
        return requestUri.replaceAll("prompt", "_prompt");
    }

    private String getAuthURL(final HttpServletRequest request) {
        final String uri = request.getRequestURI();
        String deploymentURI = uri;
        int firstSlashIndex = uri.indexOf("/");
        int secondSlashIndex = uri.indexOf("/", firstSlashIndex + 1);
        if (secondSlashIndex != -1) {
            deploymentURI = uri.substring(0, secondSlashIndex);
        }
        final StringBuffer sb = new StringBuffer(100);
        sb.append(request.getScheme()).append("://")
                .append(request.getServerName()).append(":")
                .append(request.getServerPort())
                .append(deploymentURI)
                .append("/UI/Login");
        return sb.toString();
    }

    protected SSOToken getToken(final HttpServletRequest request) throws SSOException {
        final SSOTokenManager mgr = SSOTokenManager.getInstance();
        return mgr.createSSOToken(request);
    }
}

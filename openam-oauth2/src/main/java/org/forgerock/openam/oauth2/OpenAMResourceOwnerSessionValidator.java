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

package org.forgerock.openam.oauth2;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.ResourceOwner;
import org.forgerock.oauth2.core.ResourceOwnerSessionValidator;
import org.forgerock.oauth2.core.exceptions.AccessDeniedException;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.InteractionRequiredException;
import org.forgerock.oauth2.core.exceptions.LoginRequiredException;
import org.forgerock.oauth2.core.exceptions.ResourceOwnerAuthenticationRequired;
import org.forgerock.openidconnect.OpenIdPrompt;
import org.owasp.esapi.errors.EncodingException;
import org.restlet.Request;
import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.data.Reference;
import org.restlet.ext.servlet.ServletUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;

import static org.forgerock.oauth2.core.Utils.isEmpty;

/**
 * Validates whether a resource owner has a current authenticated session.
 *
 * @since 12.0.0
 */
@Singleton
public class OpenAMResourceOwnerSessionValidator implements ResourceOwnerSessionValidator {

    private final Debug logger = Debug.getInstance("OAuth2Provider");
    private final SSOTokenManager ssoTokenManager;

    @Inject
    public OpenAMResourceOwnerSessionValidator(SSOTokenManager ssoTokenManager) {

        this.ssoTokenManager = ssoTokenManager;
    }

    /**
     * {@inheritDoc}
     */
    public ResourceOwner validate(OAuth2Request request) throws ResourceOwnerAuthenticationRequired,
            AccessDeniedException, BadRequestException, InteractionRequiredException, LoginRequiredException {

        final String prompt = request.getParameter(OAuth2Constants.Custom.PROMPT);

        final OpenIdPrompt openIdPrompt = new OpenIdPrompt(prompt);

        if (!openIdPrompt.isValid()) {
            logger.message("Invalid prompt parameter");
            throw new BadRequestException("Invalid prompt parameter");
        }

        SSOToken token = null;
        try {
            token = ssoTokenManager.createSSOToken(getHttpServletRequest(request.<Request>getRequest()));
        } catch (SSOException e) {
            logger.warning("Error authenticating user against OpenAM: ", e);
        }

        try {
            if (token != null) {

                if (openIdPrompt.isPromptLogin()) {
                    try {
                        ssoTokenManager.destroyToken(token);
                    } catch (SSOException e) {
                        logger.error("Error destorying SSOToken: ", e);
                    }
                    throw authenticationRequired(request);
                }

                try {
                    final AMIdentity id = IdUtils.getIdentity(
                            AccessController.doPrivileged(AdminTokenAction.getInstance()),
                            token.getProperty(Constants.UNIVERSAL_IDENTIFIER));
                    return new OpenAMResourceOwner(token.getProperty("UserToken"), id);
                } catch (SSOException e) {
                    logger.error("Error authenticating user against OpenAM: ", e);
                    throw new LoginRequiredException();
                } catch (IdRepoException e) {
                    logger.error("Error authenticating user against OpenAM: ", e);
                    throw new LoginRequiredException();
                }
            } else {
                if (openIdPrompt.isNoPrompt()) {
                    logger.error("Not pre-authenticated and prompt parameter equals none.");
                    throw new InteractionRequiredException();
                } else if (openIdPrompt.isPromptConsent() && !openIdPrompt.isPromptLogin()) {
                    logger.error("Prompt parameter doesn't allow the login prompt: ");
                    throw new LoginRequiredException();
                } else {
                    throw authenticationRequired(request);
                }
            }
        } catch (EncodingException e) {
            throw new AccessDeniedException(e);
        } catch (URISyntaxException e) {
            throw new AccessDeniedException(e);
        }
    }

    private ResourceOwnerAuthenticationRequired authenticationRequired(OAuth2Request request)
            throws AccessDeniedException, EncodingException, URISyntaxException {

        final Request req = request.getRequest();
        final String authURL = getAuthURL(getHttpServletRequest(req));
        final URI authURI = new URI(authURL);
        final Reference loginRef = new Reference(authURI);

        final String realm = request.getParameter(OAuth2Constants.Custom.REALM);
        final String moduleName = request.getParameter(OAuth2Constants.Custom.MODULE);
        final String serviceName = request.getParameter(OAuth2Constants.Custom.SERVICE);
        final String locale = request.getParameter(OAuth2Constants.Custom.LOCALE);

        if (!isEmpty(realm)) {
            loginRef.addQueryParameter(OAuth2Constants.Custom.REALM, realm);
        }
        if (!isEmpty(locale)) {
            loginRef.addQueryParameter(OAuth2Constants.Custom.LOCALE, locale);
        }
        if (!isEmpty(moduleName)) {
            loginRef.addQueryParameter(OAuth2Constants.Custom.MODULE, moduleName);
        } else if (!isEmpty(serviceName)) {
            loginRef.addQueryParameter(OAuth2Constants.Custom.SERVICE, serviceName);
        }

        removeLoginPrompt(req);

        loginRef.addQueryParameter(OAuth2Constants.Custom.GOTO, req.getResourceRef().toString());

        return new ResourceOwnerAuthenticationRequired(loginRef.toUri());
    }

    /**
     * Removes "login" from prompt query parameter.
     *
     * This needs to be done before redirecting the user to login so that an infinite redirect loop is avoided.
     */
    private void removeLoginPrompt(Request req) {
        Form query = req.getResourceRef().getQueryAsForm();
        Parameter param = query.getFirst(OAuth2Constants.Custom.PROMPT);
        if (param != null && param.getSecond() != null) {
            String newValue = param.getSecond().toLowerCase().replace(OpenIdPrompt.PROMPT_LOGIN, "").trim();
            param.setSecond(newValue);
        }
        req.getResourceRef().setQuery(query.getQueryString());
    }

    /**
     * Derive full URL for login screen
     */
    private String getAuthURL(HttpServletRequest request) {
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

    /**
     * Hide static method call behind an instance method that can be overridden by unit tests.
     */
    HttpServletRequest getHttpServletRequest(Request req) {
        return ServletUtils.getRequest(req);
    }

}

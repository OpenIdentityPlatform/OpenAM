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

package org.forgerock.oauth2;

import org.forgerock.common.SessionManager;
import org.forgerock.common.UserStore;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.ResourceOwner;
import org.forgerock.oauth2.core.ResourceOwnerSessionValidator;
import org.forgerock.oauth2.core.exceptions.AccessDeniedException;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.InteractionRequiredException;
import org.forgerock.oauth2.core.exceptions.LoginRequiredException;
import org.forgerock.oauth2.core.exceptions.ResourceOwnerAuthenticationRequired;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.openidconnect.OpenIdPrompt;
import org.owasp.esapi.errors.EncodingException;
import org.restlet.Request;
import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.data.Reference;
import org.restlet.ext.servlet.ServletUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @since 12.0.0
 */
@Singleton
public class ResourceOwnerSessionValidatorImpl implements ResourceOwnerSessionValidator {

    private final SessionManager sessionManager;
    private final UserStore userStore;

    @Inject
    public ResourceOwnerSessionValidatorImpl(final SessionManager sessionManager, final UserStore userStore) {
        this.sessionManager = sessionManager;
        this.userStore = userStore;
    }

    public ResourceOwner validate(OAuth2Request request) throws ResourceOwnerAuthenticationRequired,
            AccessDeniedException, BadRequestException, InteractionRequiredException, LoginRequiredException {

        final OpenIdPrompt openIdPrompt = new OpenIdPrompt(request);

        if (!openIdPrompt.isValid()) {
            throw new BadRequestException("Invalid prompt parameter \"" + openIdPrompt.getOriginalValue());
        }

        final String sessionId = getSessionId(ServletUtils.getRequest(request.<Request>getRequest()));

        try {
            if (sessionId != null) {
                if (openIdPrompt.containsLogin()) {
                    sessionManager.delete(sessionId);
                    throw authenticationRequired(request);
                }

                final String clientId = sessionManager.get(sessionId);
                if (clientId == null) {
                    throw authenticationRequired(request);
                }
                return userStore.get(clientId);
            } else {
                if (openIdPrompt.containsNone()) {
                    throw new InteractionRequiredException();
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

    private String getSessionId(final HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (final Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals("FR_OAUTH2_SESSION_ID")) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private ResourceOwnerAuthenticationRequired authenticationRequired(final OAuth2Request request)
            throws AccessDeniedException, EncodingException, URISyntaxException {

        final Request req = request.getRequest();
        final String authURL = getAuthURL(ServletUtils.getRequest(req));
        final URI authURI = new URI(authURL);
        final Reference loginRef = new Reference(authURI);

        //remove prompt parameter
        Form query = req.getResourceRef().getQueryAsForm();
        Parameter p = query.getFirst("prompt");
        if (p != null) {
            p.setFirst("_prompt");
        }
        req.getResourceRef().setQuery(query.getQueryString());

        loginRef.addQueryParameter(OAuth2Constants.Custom.GOTO, req.getResourceRef().toString());

        return new ResourceOwnerAuthenticationRequired(loginRef.toUri());
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
                .append("/login");
        return sb.toString();
    }
}

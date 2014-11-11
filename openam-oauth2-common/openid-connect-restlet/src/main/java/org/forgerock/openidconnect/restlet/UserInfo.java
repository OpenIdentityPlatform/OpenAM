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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.openidconnect.restlet;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.oauth2.restlet.ExceptionHandler;
import org.forgerock.oauth2.restlet.OAuth2RestletException;
import org.forgerock.openidconnect.UserInfoService;
import org.restlet.Request;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import javax.inject.Inject;

/**
 * Handles requests to the OpenId Connect userinfo endpoint for retrieving information about the user who granted the
 * authorization for the token.
 *
 * @since 11.0.0
 */
public class UserInfo extends ServerResource {

    private final OAuth2RequestFactory<Request> requestFactory;
    private final UserInfoService userInfoService;
    private final ExceptionHandler exceptionHandler;

    /**
     * Constructs a new UserInfo.
     *
     * @param requestFactory An instance of the OAuth2RequestFactory.
     * @param userInfoService An instance of the UserInfoService.
     * @param exceptionHandler An instance of the ExceptionHandler.
     */
    @Inject
    public UserInfo(OAuth2RequestFactory<Request> requestFactory, UserInfoService userInfoService,
            ExceptionHandler exceptionHandler) {
        this.requestFactory = requestFactory;
        this.userInfoService = userInfoService;
        this.exceptionHandler = exceptionHandler;
    }

    /**
     * Handles GET and POST requests to the OpenId Connect userinfo endpoint for retrieving information about the
     * user who granted the authorization for the token.
     *
     * @return The representation of the user's information.
     * @throws OAuth2RestletException If an error occurs whilst retrieving the user's information.
     */
    @Get
    @Post("form:json")
    public Representation getUserInfo(Representation body) throws OAuth2RestletException {

        final OAuth2Request request = requestFactory.create(getRequest());

        try {
            final JsonValue userInfo = userInfoService.getUserInfo(request);
            return new JsonRepresentation(userInfo.asMap());
        } catch (OAuth2Exception e) {
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(), null);
        }
    }

    /**
     * Handles any exception that is thrown when processing a OAuth2 authorization request.
     *
     * @param throwable The throwable.
     */
    @Override
    protected void doCatch(Throwable throwable) {
        exceptionHandler.handle(throwable, getResponse());
    }
}

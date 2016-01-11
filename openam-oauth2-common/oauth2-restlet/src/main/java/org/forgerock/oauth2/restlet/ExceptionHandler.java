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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.oauth2.restlet;

import org.forgerock.oauth2.core.OAuth2Constants.UrlLocation;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.rest.representations.JacksonRepresentationFactory;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.routing.Redirector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import java.util.HashMap;
import java.util.Map;

import static org.forgerock.oauth2.core.Utils.isEmpty;

/**
 * Handles any exception that is thrown when processing a OAuth2 request, by converting to a OAuth2RestletException,
 * if not already.
 *
 * @since 12.0.0
 */
public class ExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("OAuth2Provider");

    private final OAuth2Representation representation;
    private final BaseURLProviderFactory baseURLProviderFactory;
    private final OAuth2RequestFactory<?, Request> requestFactory;
    private final JacksonRepresentationFactory jacksonRepresentationFactory;

    /**
     * Constructs a new ExceptionHandler.
     *
     * @param representation An instance of the OAuth2Representation.
     * @param baseURLProviderFactory The factory to create {@code BaseURLProvider} instances.
     * @param jacksonRepresentationFactory The factory to create {@code JacksonRepresentation} instances.
     */
    @Inject
    public ExceptionHandler(OAuth2Representation representation, BaseURLProviderFactory baseURLProviderFactory,
            OAuth2RequestFactory<?, Request> requestFactory,
            JacksonRepresentationFactory jacksonRepresentationFactory) {
        this.representation = representation;
        this.baseURLProviderFactory = baseURLProviderFactory;
        this.requestFactory = requestFactory;
        this.jacksonRepresentationFactory = jacksonRepresentationFactory;
    }

    /**
     * Handles any exception that is thrown when processing a OAuth2 request.
     *
     * @param throwable The throwable.
     * @param context The Restlet context.
     * @param request The Restlet request.
     * @param response The Restlet response.
     */
    public void handle(Throwable throwable, Context context, Request request, Response response) {

        if (throwable.getCause() instanceof OAuth2RestletException) {
            final OAuth2RestletException e = (OAuth2RestletException) throwable.getCause();
            handle(e, context, request, response);
        } else {
            final ServerException serverException = new ServerException(throwable);
            final OAuth2RestletException exception = new OAuth2RestletException(serverException.getStatusCode(),
                    serverException.getError(), serverException.getMessage(), null);
            handle(exception, context, request, response);
        }
    }

    /**
     * Handles a OAuth2RestletException that is thrown when processing a OAuth2 authorization request.
     * <br/>
     * If the OAuth2RestletException has a status of {@link Status#REDIRECTION_TEMPORARY} the user agent will be
     * redirected to the redirect uri set on the exception.
     * <br/>
     * If the OAuth2RestletException does not have a redirect status but still has a redirect uri set, the user
     * agent will be redrected to the redirect uri with the exception message in the redirect uri.
     * <br/>
     * In all other cases the OAuth2 error page will be presented.
     *
     * @param exception The OAuth2RestletException.
     * @param context The Restlet context.
     * @param request The Restlet request.
     * @param response The Restlet response.
     */
    private void handle(OAuth2RestletException exception, Context context, Request request, Response response) {

        if (exception.getStatus().equals(Status.REDIRECTION_TEMPORARY)) {
            Redirector redirector = new Redirector(new Context(), exception.getRedirectUri(),
                    Redirector.MODE_CLIENT_PERMANENT);
            redirector.handle(request, response);
            return;
        } else {
            response.setStatus(exception.getStatus());
        }

        if (!isEmpty(exception.getRedirectUri())) {
            Reference ref = new Reference(exception.getRedirectUri());
            if (UrlLocation.FRAGMENT.equals(exception.getParameterLocation())) {
                ref.setFragment(representation.toForm(exception.asMap()).getQueryString());
            } else {
                ref.addQueryParameters(representation.toForm(exception.asMap()));
            }
            final Redirector redirector = new Redirector(context, ref.toString(), Redirector.MODE_CLIENT_FOUND);
            redirector.handle(request, response);
            return;
        }
        final Map<String, String> data = new HashMap<>(exception.asMap());
        final String realm = requestFactory.create(request).getParameter("realm");
        data.put("realm", realm);
        data.put("baseUrl", baseURLProviderFactory.get(realm).getRootURL(ServletUtils.getRequest(request)));
        response.setEntity(representation.getRepresentation(context, "page", "error.ftl", data));
    }

    /**
     * Handles general OAuth2 exceptions from Restlet endpoints.
     * <br/>
     * If the throwable is not a OAuth2RestletException then it will be wrapped as a ServerException.
     * <br/>
     * If the throwable is a OAuth2RestletException then it will be set on the response as a Json representation.
     *
     * @param throwable The throwable.
     * @param response The Restlet response.
     */
    public void handle(Throwable throwable, Response response) {
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("Unhandled exception: " + throwable, throwable);
        }

        final OAuth2RestletException exception = toOAuth2RestletException(throwable);
        response.setStatus(exception.getStatus());
        response.setEntity(jacksonRepresentationFactory.create(exception.asMap()));
    }

    private OAuth2RestletException toOAuth2RestletException(Throwable throwable) {
        if (throwable instanceof OAuth2RestletException) {
            return (OAuth2RestletException) throwable;
        } else if (throwable.getCause() instanceof OAuth2RestletException) {
            return (OAuth2RestletException) throwable.getCause();
        } else if (throwable.getCause() instanceof OAuth2Exception) {
            final OAuth2Exception exception = (OAuth2Exception) throwable.getCause();
            return new OAuth2RestletException(exception.getStatusCode(), exception.getError(), exception.getMessage(),
                    null);
        } else {
            final ServerException serverException = new ServerException(throwable);
            final OAuth2RestletException oauthException =
                    new OAuth2RestletException(serverException.getStatusCode(), serverException.getError(),
                            serverException.getMessage(), null);
            return oauthException;
        }
    }
}

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
 * Portions Copyrighted 2025-2026 3A Systems LLC.
 */

package org.forgerock.oauth2.restlet;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

import org.forgerock.oauth2.core.AuthorizationService;
import org.forgerock.oauth2.core.AuthorizationToken;
import org.forgerock.oauth2.core.CsrfProtection;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.RedirectUriResolver;
import org.forgerock.oauth2.core.ResourceOwnerSessionValidator;
import org.forgerock.oauth2.core.exceptions.CsrfException;
import org.forgerock.oauth2.core.exceptions.DuplicateRequestParameterException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.oauth2.core.exceptions.OAuth2ProviderNotFoundException;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.core.exceptions.ResourceOwnerAuthenticationRequired;
import org.forgerock.oauth2.core.exceptions.ResourceOwnerConsentRequired;
import org.forgerock.openam.rest.jakarta.servlet.ServletUtils;
import org.forgerock.openam.rest.representations.JacksonRepresentationFactory;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openam.xui.XUIState;
import org.restlet.data.Dimension;
import org.restlet.data.MediaType;
import org.restlet.data.Preference;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.routing.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles requests to the OAuth2 authorize endpoint.
 *
 * @since 12.0.0
 */
public class AuthorizeResource extends ConsentRequiredResource {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");

    private final OAuth2RequestFactory requestFactory;
    private final AuthorizationService authorizationService;
    private final ExceptionHandler exceptionHandler;
    private final OAuth2Representation representation;
    private final Set<AuthorizeRequestHook> hooks;
    private final RedirectUriResolver redirectUriResolver;
    private final JacksonRepresentationFactory jacksonRepresentationFactory;


    /**
     * Constructs a new AuthorizeResource.
     *
     * @param requestFactory An instance of the OAuth2RequestFactory.
     * @param authorizationService An instance of the AuthorizationService.
     * @param exceptionHandler An instance of the ExceptionHandler.
     * @param representation An instance of the OAuth2Representation.
     */
    @Inject
    public AuthorizeResource(OAuth2RequestFactory requestFactory, AuthorizationService authorizationService,
            ExceptionHandler exceptionHandler, OAuth2Representation representation, Set<AuthorizeRequestHook> hooks,
            XUIState xuiState, @Named("OAuth2Router") Router router, BaseURLProviderFactory baseURLProviderFactory,
            RedirectUriResolver redirectUriResolver, ResourceOwnerSessionValidator resourceOwnerSessionValidator,
            CsrfProtection csrfProtection, JacksonRepresentationFactory jacksonRepresentationFactory) {
        super(router, baseURLProviderFactory, xuiState, resourceOwnerSessionValidator, csrfProtection);
        this.requestFactory = requestFactory;
        this.authorizationService = authorizationService;
        this.exceptionHandler = exceptionHandler;
        this.representation = representation;
        this.hooks = hooks;
        this.redirectUriResolver = redirectUriResolver;
        this.jacksonRepresentationFactory = jacksonRepresentationFactory;
    }

    /**
     * Handles GET requests to the OAuth2 authorize endpoint.
     * <br>
     * This method will be called when a client has requested a resource owner grants it authorization to access a
     * resource.
     *
     * @return The body to be sent in the response to the user agent.
     * @throws OAuth2RestletException If a OAuth2 error occurs whilst processing the authorization request.
     */
    @Get
    public Representation authorize() throws OAuth2RestletException {

        final OAuth2Request request = requestFactory.create(getRequest());

        for (AuthorizeRequestHook hook : hooks) {
            hook.beforeAuthorizeHandling(request, getRequest(), getResponse());
        }

        try {
            final AuthorizationToken authorizationToken = authorizationService.authorize(request);

            final String redirectUri = redirectUriResolver.resolve(request);

            Representation response = representation.toRepresentation(getContext(), getRequest(), getResponse(), authorizationToken,
                    redirectUri);

            for (AuthorizeRequestHook hook : hooks) {
                hook.afterAuthorizeSuccess(request, getRequest(), getResponse());
            }

            return response;

        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("client_id")) {
                throw new OAuth2RestletException(400, "invalid_request", e.getMessage(),
                        request.<String>getParameter("state"));
            }
            throw new OAuth2RestletException(400, "invalid_request", e.getMessage(),
                    request.<String>getParameter("redirect_uri"), request.<String>getParameter("state"));
        } catch (ResourceOwnerAuthenticationRequired e) {
            if (wantsJson()) {
                // A redirect to the login page is not actionable by a non-browser client.
                throw new OAuth2RestletException(401, "login_required", e.getMessage(),
                        request.<String>getParameter("state"));
            }
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(),
                    e.getRedirectUri().toString(), null);
        } catch (ResourceOwnerConsentRequired e) {
            if (wantsJson()) {
                return consentRepresentation(e, request);
            }
            return representation.getRepresentation(getContext(), request, "authorize.ftl",
                    getDataModel(e, request));
        } catch (InvalidClientException e) {
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(),
                    request.<String>getParameter("state"));
        } catch (RedirectUriMismatchException e) {
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(),
                    request.<String>getParameter("state"));
        } catch (DuplicateRequestParameterException e) {
            throw new OAuth2RestletException(400, "invalid_request", e.getMessage(),
                    request.<String>getParameter("state"));
        } catch (OAuth2ProviderNotFoundException e) {
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(),
                    request.<String>getParameter("state"));
        } catch (OAuth2Exception e) {
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(),
                    request.<String>getParameter("redirect_uri"), request.<String>getParameter("state"),
                    e.getParameterLocation());
        }
    }

    /**
     * Handles POST requests to the OAuth2 authorize endpoint.
     * <br>
     * This method will be called when a user has given their consent for an authorization request.
     *
     * @param entity The entity on the request.
     * @return The body to be sent in the response to the user agent.
     * @throws OAuth2RestletException If a OAuth2 error occurs whilst processing the authorization request.
     */
    @Post
    public Representation authorize(Representation entity) throws OAuth2RestletException {

        final OAuth2Request request = requestFactory.create(getRequest());

        for (AuthorizeRequestHook hook : hooks) {
            hook.beforeAuthorizeHandling(request, getRequest(), getResponse());
        }

        final boolean consentGiven = "allow".equalsIgnoreCase(request.<String>getParameter("decision"));
        final boolean saveConsent = "on".equalsIgnoreCase(request.<String>getParameter("save_consent"));

        try {
            final AuthorizationToken authorizationToken = authorizationService.authorize(request, consentGiven,
                    saveConsent);

            final String redirectUri = redirectUriResolver.resolve(request);
            Representation response = representation.toRepresentation(getContext(), getRequest(), getResponse(), authorizationToken,
                    redirectUri);

            for (AuthorizeRequestHook hook : hooks) {
                hook.afterAuthorizeSuccess(request, getRequest(), getResponse());
            }

            return response;

        } catch (ResourceOwnerAuthenticationRequired e) {
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(),
                    e.getRedirectUri().toString(), null);
        } catch (InvalidClientException e) {
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(),
                    request.<String>getParameter("state"));
        } catch (RedirectUriMismatchException e) {
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(),
                    request.<String>getParameter("state"));
        } catch (DuplicateRequestParameterException e) {
            throw new OAuth2RestletException(400, "invalid_request", e.getMessage(),
                    request.<String>getParameter("state"));
        } catch (CsrfException e) {
            throw new OAuth2RestletException(400, "bad_request", e.getMessage(),
                    request.<String>getParameter("state"));
        } catch (OAuth2Exception e) {
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(),
                    request.<String>getParameter("redirect_uri"), request.<String>getParameter("state"),
                    e.getParameterLocation());
        }
    }

    /**
     * Whether this request asked for the JSON consent representation rather than the HTML consent page.
     *
     * @return {@code true} if JSON was explicitly preferred.
     */
    private boolean wantsJson() {
        return prefersJson(getRequest().getClientInfo().getAcceptedMediaTypes());
    }

    /**
     * Builds the JSON consent representation, refusing to hand the CSRF token to another origin.
     *
     * <p>The OAuth2 endpoints are covered by the CORS filter, which echoes the caller's {@code Origin} with
     * {@code Access-Control-Allow-Credentials} when so configured. Without this check a deployment that
     * allows credentialed CORS would let any page read the token and then forge a consent decision.</p>
     *
     * @param consentRequired The details for requesting consent.
     * @param request The OAuth2 request.
     * @return The JSON consent representation.
     * @throws OAuth2RestletException If the request came from a foreign origin.
     */
    private Representation consentRepresentation(ResourceOwnerConsentRequired consentRequired, OAuth2Request request)
            throws OAuth2RestletException {
        if (isForeignOrigin(request)) {
            logger.debug("Refusing to serve the JSON consent representation to a foreign origin");
            throw new OAuth2RestletException(403, "access_denied", "Cross-origin consent requests are not allowed",
                    request.<String>getParameter("state"));
        }
        // The representation now depends on the Accept header; Restlet renders this as "Vary: Accept".
        getResponse().getDimensions().add(Dimension.MEDIA_TYPE);
        return jacksonRepresentationFactory.create(getConsentModel(consentRequired, request).asMap());
    }

    /**
     * Whether the request carries an {@code Origin} that is not this deployment's own. Requests without the
     * header - non-browser clients, and top-level navigations - are not cross-origin.
     *
     * <p>Extracted as a seam so the branch can be unit-tested without static mocking.</p>
     *
     * @param request The OAuth2 request.
     * @return {@code true} if the request originates from another origin.
     */
    protected boolean isForeignOrigin(OAuth2Request request) {
        final HttpServletRequest servletRequest = ServletUtils.getRequest(getRequest());
        final String origin = servletRequest == null ? null : servletRequest.getHeader("Origin");
        if (StringUtils.isBlank(origin)) {
            return false;
        }
        return !sameOrigin(origin, baseURLProviderFactory.get(request.<String>getParameter("realm"))
                .getRootURL(servletRequest));
    }

    /**
     * Compares an {@code Origin} header against a deployment URL on scheme, host and port.
     *
     * @param origin The value of the {@code Origin} header.
     * @param baseUrl The deployment's root URL.
     * @return {@code true} if both denote the same origin.
     */
    static boolean sameOrigin(String origin, String baseUrl) {
        try {
            final URI actual = new URI(origin);
            final URI expected = new URI(baseUrl);
            return actual.getScheme() != null && actual.getScheme().equalsIgnoreCase(expected.getScheme())
                    && actual.getHost() != null && actual.getHost().equalsIgnoreCase(expected.getHost())
                    && effectivePort(actual) == effectivePort(expected);
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static int effectivePort(URI uri) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    /**
     * Decides whether JSON was explicitly preferred over HTML. Only an exact {@code application/json} entry
     * counts, and it has to outrank everything that can carry HTML. A client that expresses no preference
     * between the two - a bare {@code *&#47;*} (curl's default), or a library default such as
     * {@code application/json, text/plain, *&#47;*} - keeps getting the consent page, so existing clients
     * that scrape it are unaffected.
     *
     * @param accepted The accepted media types, in any order.
     * @return {@code true} if JSON was named explicitly and is preferred over HTML.
     */
    static boolean prefersJson(List<Preference<MediaType>> accepted) {
        float json = 0f;
        float html = 0f;
        for (Preference<MediaType> preference : accepted) {
            final MediaType mediaType = preference.getMetadata();
            if (MediaType.APPLICATION_JSON.equals(mediaType, true)) {
                json = Math.max(json, preference.getQuality());
            } else if (mediaType.includes(MediaType.TEXT_HTML, true)) {
                html = Math.max(html, preference.getQuality());
            }
        }
        return json > 0f && json > html;
    }

    /**
     * Handles any exception that is thrown when processing a OAuth2 authorization request.
     *
     * <p>Errors that carry a redirect uri keep being reported by redirecting to the client, as RFC 6749
     * 4.1.2.1 requires. The rest are rendered as the error page, or as JSON if that is what was asked for.</p>
     *
     * @param throwable The throwable.
     */
    @Override
    protected void doCatch(Throwable throwable) {
        if (wantsJson() && !hasRedirectUri(throwable)) {
            exceptionHandler.handle(throwable, getResponse());
            return;
        }
        exceptionHandler.handle(throwable, getContext(), getRequest(), getResponse());
    }

    private static boolean hasRedirectUri(Throwable throwable) {
        final Throwable cause = throwable instanceof OAuth2RestletException ? throwable : throwable.getCause();
        return cause instanceof OAuth2RestletException
                && StringUtils.isNotBlank(((OAuth2RestletException) cause).getRedirectUri());
    }
}

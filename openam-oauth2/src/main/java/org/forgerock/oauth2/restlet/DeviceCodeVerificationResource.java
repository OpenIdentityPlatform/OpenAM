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
 * Copyright 2015-2016 ForgeRock AS.
 * Portions Copyrighted 2025-2026 3A Systems LLC.
 */
package org.forgerock.oauth2.restlet;

import static org.forgerock.openam.oauth2.OAuth2Constants.Params.CLIENT_ID;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.SCOPE;
import static org.forgerock.openam.utils.Time.currentTimeMillis;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import org.forgerock.oauth2.core.AuthorizationService;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.CsrfProtection;
import org.forgerock.oauth2.core.DeviceCode;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.ResourceOwner;
import org.forgerock.oauth2.core.ResourceOwnerSessionValidator;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.core.Utils;
import org.forgerock.oauth2.core.exceptions.AccessDeniedException;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.InteractionRequiredException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidScopeException;
import org.forgerock.oauth2.core.exceptions.LoginRequiredException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.core.exceptions.ResourceOwnerAuthenticationRequired;
import org.forgerock.oauth2.core.exceptions.ResourceOwnerConsentRequired;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.oauth2.OAuth2Utils;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openam.xui.XUIState;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.data.CacheDirective;
import org.restlet.data.Form;
import org.restlet.data.Language;
import org.restlet.data.Preference;
import org.restlet.data.Reference;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.forgerock.openam.rest.jakarta.servlet.ServletUtils;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.routing.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.identity.authentication.util.ISAuthConstants;

/**
 * A restlet resource for user codes
 *
 * @see <a href=https://tools.ietf.org/html/draft-ietf-oauth-v2-05#section-3.7>OAuth 2.0 Protocol - Device Flow</a>
 * @since 13.0.0
 */
public class DeviceCodeVerificationResource extends ConsentRequiredResource {
    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");

    private static final String FORM = "templates/CodeVerificationForm.ftl";
    private static final String THANKS_PAGE = "templates/CodeThanks.ftl";

    private final OAuth2Representation representation;
    private final TokenStore tokenStore;
    private final OAuth2RequestFactory requestFactory;
    private final AuthorizationService authorizationService;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final ExceptionHandler exceptionHandler;
    private final ResourceOwnerSessionValidator resourceOwnerSessionValidator;
    private final ClientRegistrationStore clientRegistrationStore;
    private final OAuth2Utils oAuth2Utils;
    private final CsrfProtection csrfProtection;

    /**
     * Constructs user code verification resource for OAuth2 Device Flow
     * @param router The base router
     * @param exceptionHandler An instance of the ExceptionHandler.
     * @param oAuth2Utils An OAuth2Utils instance.
     * @param csrfProtection An instance of the CsrfProtection.
     */
    @Inject
    public DeviceCodeVerificationResource(XUIState xuiState, @Named("OAuth2Router") Router router,
            BaseURLProviderFactory baseURLProviderFactory, OAuth2Representation representation,
            TokenStore tokenStore, OAuth2RequestFactory requestFactory,
            AuthorizationService authorizationService, OAuth2ProviderSettingsFactory providerSettingsFactory,
            ExceptionHandler exceptionHandler, ResourceOwnerSessionValidator resourceOwnerSessionValidator,
            ClientRegistrationStore clientRegistrationStore, OAuth2Utils oAuth2Utils, CsrfProtection csrfProtection) {
        super(router, baseURLProviderFactory, xuiState, resourceOwnerSessionValidator, csrfProtection);
        this.representation = representation;
        this.tokenStore = tokenStore;
        this.requestFactory = requestFactory;
        this.authorizationService = authorizationService;
        this.providerSettingsFactory = providerSettingsFactory;
        this.exceptionHandler = exceptionHandler;
        this.resourceOwnerSessionValidator = resourceOwnerSessionValidator;
        this.clientRegistrationStore = clientRegistrationStore;
        this.oAuth2Utils = oAuth2Utils;
        this.csrfProtection = csrfProtection;
    }

    /**
     * Handles POST requests to the OAuth2 device/user endpoint. Only a submitted form reaches this method: a
     * bare navigation to the endpoint arrives as a GET, which any foreign origin can trigger with a plain link
     * and which therefore never authorizes anything.
     */
    @Post
    public Representation verify(Representation body) throws ServerException, NotFoundException,
            InvalidGrantException, OAuth2RestletException {
        final Request restletRequest = getRequest();
        OAuth2Request request = requestFactory.create(restletRequest);

        preventCaching();

        // Checked once, ahead of every branch below. This advisory exists because the guard was moved inside
        // one of those branches and its sibling left uncovered; the consent decision path avoids that shape by
        // checking first thing (AuthorizationService.authorize(request, consentGiven, saveConsent) - note that
        // its single argument sibling, which this method also calls, has no check of its own and relies on
        // this one). This ordering is not itself a defence against enumeration of live user codes - anyone
        // can mint a token of their own via a GET first - only rate limiting and equal timing would be.
        if (csrfProtection.isCsrfAttack(request)) {
            logger.debug("Device code verification rejected: csrf parameter matched neither the user code "
                    + "form token nor the resource owner's session id");
            throw new OAuth2RestletException(400, "bad_request", null, request.<String>getParameter("state"));
        }

        final String userCode = getSubmittedUserCode(request);
        if (StringUtils.isEmpty(userCode)) {
            // readDeviceCode turns this into a CTS equality filter, which rejects a null assertion value with
            // an unchecked exception that would escape as a 500. A request naming no code gets the same answer
            // as one naming a code that resolves to nothing.
            return getUserCodeForm(request, "not_found");
        }

        DeviceCode deviceCode;
        try {
            deviceCode = tokenStore.readDeviceCode(userCode, request);
        } catch (InvalidGrantException e) {
            return getUserCodeForm(request, "not_found");
        }

        // An expired code, or one that has already been approved, is treated as an unknown user code
        if (deviceCode == null || deviceCode.isIssued() || deviceCode.isAuthorized()
                || deviceCode.getExpiryTime() < currentTimeMillis()) {
            return getUserCodeForm(request, "not_found");
        }

        addRequestParamsFromDeviceCode(restletRequest, deviceCode);

        try {
            OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
            ClientRegistration clientRegistration =
                 clientRegistrationStore.get(request.<String>getParameter(CLIENT_ID), request);
            final boolean requireConsent = !providerSettings.clientsCanSkipConsent()
                 || !clientRegistration.isConsentImplied();

            if (requireConsent) {
                // Read from the body alone. getParameter would give the query string priority, and the user
                // code form carries no decision field of its own, so a link to
                // ?decision=allow&save_consent=on would answer the consent question on the resource owner's
                // behalf: their own submission contradicts nothing, the consent page naming the client and
                // the scopes is never rendered, and save_consent records the grant permanently. Every
                // legitimate producer sends both in the body - the consent page as form fields, the
                // documented direct POST with --data.
                final String decision = request.getBodyParameter("decision");
                if (StringUtils.isNotEmpty(decision)) {
                    final boolean consentGiven = "allow".equalsIgnoreCase(decision);
                    final boolean saveConsent = "on".equalsIgnoreCase(request.getBodyParameter("save_consent"));
                    if (saveConsent) {
                        saveConsent(request);
                    }
                    if (consentGiven) {

                    	authorizeAndUpdateDeviceCode(deviceCode,request);
                        
                    } else {
                        tokenStore.deleteDeviceCode(deviceCode.getClientId(), deviceCode.getDeviceCode(), request);
                    }
                } else {
                    authorizationService.authorize(request);
                }
            } else {
                // Consent is implied, so no consent page is rendered and the user code form is the only page
                // that posts back here. What keeps a foreign origin from binding the resource owner's session
                // to a device code of its choosing is the CSRF check at the top of this method.
                authorizeAndUpdateDeviceCode(deviceCode, request);
            }
            
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("client_id")) {
                throw new OAuth2RestletException(400, "invalid_request", e.getMessage(),
                        request.<String>getParameter("state"));
            }
            throw new OAuth2RestletException(400, "invalid_request", e.getMessage(),
                    request.<String>getParameter("redirect_uri"), request.<String>getParameter("state"));
        } catch (ResourceOwnerAuthenticationRequired e) {
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(),
                    e.getRedirectUri().toString(), null);
        } catch (ResourceOwnerConsentRequired e) {
            return representation.getRepresentation(getContext(), request, "authorize.ftl",
                    getDataModel(e, request));
        } catch (InvalidClientException | RedirectUriMismatchException e) {
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(),
                    request.<String>getParameter("state"));
        } catch (OAuth2Exception e) {
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(),
                    request.<String>getParameter("redirect_uri"), request.<String>getParameter("state"),
                    e.getParameterLocation());
        }

        return getTemplateRepresentation(THANKS_PAGE, request, null);
    }

    private void saveConsent(OAuth2Request request) throws NotFoundException, ServerException, InvalidScopeException,
            AccessDeniedException, ResourceOwnerAuthenticationRequired, InteractionRequiredException,
            BadRequestException, LoginRequiredException, InvalidClientException {
        OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
        ResourceOwner resourceOwner = resourceOwnerSessionValidator.validate(request);
        ClientRegistration clientRegistration =
                clientRegistrationStore.get(request.<String>getParameter(CLIENT_ID), request);
        Set<String> scope = Utils.splitScope(request.<String>getParameter(SCOPE));
        Set<String> validatedScope = providerSettings.validateAuthorizationScope(clientRegistration, scope, request);
        providerSettings.saveConsent(resourceOwner, clientRegistration.getClientId(), validatedScope);
    }

    /**
     * Renders the user code form, minting the CSRF token that its submission has to carry. This form is the only
     * page of the device verification flow that posts back to this endpoint, so it is the only place a token
     * needs to be issued from. A cross-origin page can read neither the token nor the cookie backing it.
     *
     * <p>The form is deliberately left empty. Nothing distinguishes a user code the resource owner chose from
     * one a foreign page chose for them - {@code /oauth2/device/code} answers with a bare
     * {@code verification_uri} and never issues a {@code verification_uri_complete}, so the endpoint has no
     * legitimate producer of such a link to recognise - and filling the code in would turn this page into a
     * one-click approval of whichever code arrived in the query string.</p>
     *
     * @param request The OAuth2 request.
     * @return The user code form.
     */
    private Representation getUserCodeForm(OAuth2Request request) {
        return getUserCodeForm(request, null);
    }

    /**
     * Renders the user code form with an error to report above it.
     *
     * <p>Every render of this template mints a token, not only the one the happy path goes through: which of
     * the two a theme shows is the template's business, and a rendered form whose token is empty is one whose
     * every submission is refused.</p>
     *
     * @param request The OAuth2 request.
     * @param errorCode The error code to display, or {@code null} if there is none.
     * @return The user code form.
     */
    private Representation getUserCodeForm(OAuth2Request request, String errorCode) {
        return getTemplateRepresentation(FORM, request, errorCode,
                Collections.singletonMap("csrf", csrfProtection.createCsrfToken(request)));
    }

    /**
     * Marks the response uncacheable. Every page this endpoint serves either carries a freshly minted CSRF
     * token - the user code form, and the consent page {@link #getDataModel} builds - or reports the state of
     * a user code, so no intermediary may hand any of them to a second visitor. The authorize and token
     * endpoints get the same treatment from {@code OAuth2Filter}, which this route is not behind.
     */
    private void preventCaching() {
        getResponse().getCacheDirectives().add(CacheDirective.noStore());
    }

    /**
     * Resolves the user code the request is asking to act on, refusing a request that carries two different
     * ones.
     *
     * <p>The user code form posts back to the URL it was served from, so a submission that started as a
     * navigation to {@code /oauth2/device/user?user_code=...} carries the code from that link in the query
     * string alongside the code the user typed into the form. {@link OAuth2Request#getParameter} gives the
     * query string priority, so the link, not the user, would decide which device code gets authorized, and
     * the user would be confirming a code they were never shown. Neither value has a stronger claim than the
     * other, so a disagreement is refused rather than resolved.</p>
     *
     * <p>Clients that send the code in one place only, and the documented direct POST that repeats the same
     * code in both, are unaffected. The comparison is on presence rather than on emptiness: a form submitted
     * with the field cleared carries an empty {@code user_code}, and treating that as absent would hand the
     * decision straight back to the query string.</p>
     *
     * @param request The OAuth2 request.
     * @return The user code to act on, which may be {@code null}.
     * @throws OAuth2RestletException If the query string and the request body carry different user codes.
     */
    private String getSubmittedUserCode(OAuth2Request request) throws OAuth2RestletException {
        final String queryUserCode = request.getQueryParameter(OAuth2Constants.DeviceCode.USER_CODE);
        final String bodyUserCode = request.getBodyParameter(OAuth2Constants.DeviceCode.USER_CODE);
        if (queryUserCode != null && bodyUserCode != null && !queryUserCode.equals(bodyUserCode)) {
            logger.debug("Device code verification rejected: the query string and the request body carry "
                    + "different user codes");
            throw new OAuth2RestletException(400, "invalid_request", null, request.<String>getParameter("state"));
        }
        return request.getParameter(OAuth2Constants.DeviceCode.USER_CODE);
    }

    private Representation getTemplateRepresentation(String template, OAuth2Request request, String errorCode) {
        return getTemplateRepresentation(template, request, errorCode, Collections.<String, String>emptyMap());
    }

    /**
     * Builds the representation of one of this endpoint's pages.
     *
     * @param template The template to render.
     * @param request The OAuth2 request.
     * @param errorCode The error code to display, or {@code null} if there is none.
     * @param extraValues Template specific values, merged over the common ones.
     * @return The representation to render.
     */
    private Representation getTemplateRepresentation(String template, OAuth2Request request, String errorCode,
            Map<String, String> extraValues) {
        TemplateRepresentation response = getTemplateFactory(getContext()).getTemplateRepresentation(template);
        Map<String, String> dataModel = new HashMap<>();
        dataModel.put("errorCode", errorCode);
        dataModel.put("baseUrl", getRootUrl(request));
        List<String> locale = new ArrayList<>();
        for (Preference<Language> language : getRequest().getClientInfo().getAcceptedLanguages()) {
            locale.add(language.getMetadata().getName());
        }
        dataModel.put("locale", oAuth2Utils.join(locale, " "));
        dataModel.put("realm", request.<String>getParameter(OAuth2Constants.Params.REALM));
        dataModel.putAll(extraValues);
        response.setDataModel(dataModel);
        return response;
    }

    /**
     * The public address of this deployment as the realm's Base URL Source configures it, context path
     * included - the one the device authorization response hands out as the verification URI.
     *
     * @param request The OAuth2 request.
     * @return The root URL, never ending in a {@code /}.
     */
    private String getRootUrl(OAuth2Request request) {
        return baseURLProviderFactory.get(request.<String>getParameter(OAuth2Constants.Params.REALM))
                .getRootURL(ServletUtils.getRequest(getRequest()));
    }

    private void addRequestParamsFromDeviceCode(Request restletRequest, DeviceCode deviceCode) {
        Map<String, Object> codeAttributes = (Map<String, Object>) deviceCode.getObject();

        for (Map.Entry<String, Object> entry : codeAttributes.entrySet()) {
            Object value = entry.getValue();

            if (!entry.getKey().equals(OAuth2Constants.Params.SCOPE)) {
                value = ((List<String>) value).iterator().next();
            } else {
                value = oAuth2Utils.join((List<String>) value, " ");
            }

            if (entry.getKey().equals(OAuth2Constants.CoreTokenParams.CLIENT_ID)) {
                restletRequest.getAttributes().put(OAuth2Constants.Params.CLIENT_ID, value);
            } else {
                restletRequest.getAttributes().put(entry.getKey(), value);
            }
        }
    }

    /**
     * Handles GET requests to the OAuth2 device/user endpoint, returning a form to allow the user to submit their
     * user code.
     *
     * <p>A GET never acts on a user code, whatever the request carries. It is the one request a foreign origin
     * can make the resource owner's browser send with its session cookie attached - {@code SameSite=Lax} blocks
     * a cross-site POST but not a top-level navigation - so it may only render this form.</p>
     *
     * <p>A user code in the query string is not merely ignored, it is redirected away. The form has no action
     * of its own, so it posts back to the address it was served from, and a code left in that address would be
     * submitted alongside the one the user typed - which {@link #getSubmittedUserCode} then refuses, leaving
     * whoever followed such a link with a bare 400 and no form to retry on. Serving the form only from an
     * address that carries no code makes the query string that reaches the POST the user's own.</p>
     *
     * @return The form to allow the user to submit their user code, or {@code null} if the request is being
     * redirected to the same address without its user code.
     */
    @Get
    public Representation userCodeForm() {
        preventCaching();
        final OAuth2Request request = requestFactory.create(getRequest());
        final Reference withoutUserCode = withoutUserCode(request);
        if (withoutUserCode != null) {
            redirectSeeOther(withoutUserCode);
            return null;
        }
        return getUserCodeForm(request);
    }

    /**
     * Strips the user code from the address the form is about to be served from, leaving the path and every
     * other query string parameter - the realm this endpoint may be addressed with, among them - in place.
     *
     * <p>Scheme, host, port and context path come from the realm's Base URL Source, like the page's own
     * {@code baseUrl} and the verification URI the device was given, rather than from the container's view of
     * the request: behind a proxy that terminates TLS without telling the connector, the container sees a
     * plain-http address on its own host, and sending the browser there would break a request that renders
     * the form. Only the path below the context path is the request's own, so nothing in it can make the
     * redirect name another host. The reference has to be absolute: Restlet writes the Location header through
     * {@link Reference#getTargetRef()}, and that refuses a relative reference that has no base to resolve
     * against.</p>
     *
     * @param request The OAuth2 request.
     * @return The same address without its user code, or {@code null} if it carries none.
     */
    private Reference withoutUserCode(OAuth2Request request) {
        final Form query = getRequest().getResourceRef().getQueryAsForm();
        if (!query.removeAll(OAuth2Constants.DeviceCode.USER_CODE)) {
            return null;
        }
        // Both undecoded, so the path keeps the encoding the browser sent it with
        final HttpServletRequest servletRequest = ServletUtils.getRequest(getRequest());
        final String path = servletRequest.getRequestURI().substring(servletRequest.getContextPath().length());
        final Reference redirect = new Reference(getRootUrl(request) + path);
        redirect.setQuery(query.isEmpty() ? null : query.getQueryString());
        return redirect;
    }

    /**
     * Gets an instance of the TemplateFactory.
     *
     * @param context The Restlet context.
     * @return An instance of the TemplateFactory.
     */
    private TemplateFactory getTemplateFactory(Context context) {
        Object factory = context.getAttributes().get(TemplateFactory.class.getName());
        if (factory instanceof TemplateFactory) {
            return (TemplateFactory) factory;
        }

        final TemplateFactory newFactory = TemplateFactory.newInstance(context);
        context.getAttributes().put(TemplateFactory.class.getName(), newFactory);
        return newFactory;
    }

    /**
     * Handles any exception that is thrown when processing a OAuth2 authorization request.
     *
     * @param throwable The throwable.
     */
    @Override
    protected void doCatch(Throwable throwable) {
        exceptionHandler.handle(throwable, getContext(), getRequest(), getResponse());
    }
    
    
    private void authorizeAndUpdateDeviceCode(DeviceCode deviceCode, OAuth2Request request) throws OAuth2Exception {

        ResourceOwner resourceOwner = resourceOwnerSessionValidator.validate(request);
        deviceCode.setAcrValues(getAuthenticationContextClassReferenceFromRequest(request));
        SSOToken token = resourceOwnerSessionValidator.getResourceOwnerSession(request);

        if (token != null) {

            try {
            	deviceCode.setAuthModules(token.getProperty(ISAuthConstants.AUTH_TYPE));
            } catch (SSOException e) {
                logger.warn("Could not get list of auth modules from authentication", e);
            }
        }
        
        deviceCode.setResourceOwnerId(resourceOwner.getId());
        deviceCode.setAuthorized(true);
        tokenStore.updateDeviceCode(deviceCode, request);
    }
    
    private String getAuthenticationContextClassReferenceFromRequest(OAuth2Request request) {
        return (String) request.getRequest().getAttributes().get(OAuth2Constants.JWTTokenParams.ACR);
    }
}

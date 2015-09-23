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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.oauth2.restlet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.oauth2.core.AuthorizationService;
import org.forgerock.oauth2.core.AuthorizationToken;
import org.forgerock.oauth2.core.DeviceCode;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.core.exceptions.ResourceOwnerAuthenticationRequired;
import org.forgerock.oauth2.core.exceptions.ResourceOwnerConsentRequired;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.oauth2.OAuth2Utils;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openam.xui.XUIState;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.data.Language;
import org.restlet.data.Preference;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.routing.Router;

/**
 * A restlet resource for user codes
 *
 * @see <a href=https://tools.ietf.org/html/draft-ietf-oauth-v2-05#section-3.7>OAuth 2.0 Protocol - Device Flow</a>
 * @since 13.0.0
 */
public class DeviceCodeVerificationResource extends ConsentRequiredResource {
    private static final String FORM = "templates/CodeVerificationForm.ftl";
    private static final String THANKS_PAGE = "templates/CodeThanks.ftl";

    private final OAuth2Representation representation;
    private final TokenStore tokenStore;
    private final OAuth2RequestFactory<Request> requestFactory;
    private final AuthorizationService authorizationService;
    private final ExceptionHandler exceptionHandler;

    /**
     * Constructs user code verification resource for OAuth2 Device Flow
     * @param router The base router
     * @param exceptionHandler
     */
    @Inject
    public DeviceCodeVerificationResource(XUIState xuiState, @Named("OAuth2Router") Router router,
            BaseURLProviderFactory baseURLProviderFactory, OAuth2Representation representation,
            TokenStore tokenStore, OAuth2RequestFactory<Request> requestFactory,
            AuthorizationService authorizationService, ExceptionHandler exceptionHandler) {
        super(router, baseURLProviderFactory, xuiState);
        this.representation = representation;
        this.tokenStore = tokenStore;
        this.requestFactory = requestFactory;
        this.authorizationService = authorizationService;
        this.exceptionHandler = exceptionHandler;
    }

    /**
     * Handles POST requests to the OAuth2 device/user endpoint.
     */
    @Post
    public Representation verify(Representation body) throws ServerException, NotFoundException,
            InvalidGrantException, OAuth2RestletException {
        final Request restletRequest = getRequest();
        OAuth2Request request = requestFactory.create(restletRequest);

        DeviceCode deviceCode = tokenStore.readDeviceCode(request.<String>getParameter(OAuth2Constants.DeviceCode.USER_CODE),
                request);

        if (deviceCode == null || deviceCode.isIssued()) {
            return getTemplateRepresentation(FORM, request, "not_found");
        }

        addRequestParamsFromDeviceCode(restletRequest, deviceCode);

        try {
            AuthorizationToken token = null;
            final String decision = request.getParameter("decision");
            if (StringUtils.isNotEmpty(decision)) {
                final boolean consentGiven = "allow".equalsIgnoreCase(decision);
                final boolean saveConsent = "on".equalsIgnoreCase(request.<String>getParameter("save_consent"));
                if (consentGiven) {
                    token = authorizationService.authorize(request, consentGiven, saveConsent);
                } else {
                    tokenStore.deleteDeviceCode(deviceCode.getClientId(), deviceCode.getDeviceCode(), request);
                }
            } else {
                token = authorizationService.authorize(request);
            }

            if (token != null) {
                deviceCode.setTokens(token.getToken());
                tokenStore.updateDeviceCode(deviceCode, request);
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
        } catch (InvalidClientException e) {
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(),
                    request.<String>getParameter("state"));
        } catch (RedirectUriMismatchException e) {
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(),
                    request.<String>getParameter("state"));
        } catch (OAuth2Exception e) {
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(),
                    request.<String>getParameter("redirect_uri"), request.<String>getParameter("state"),
                    e.getParameterLocation());
        }

        return getTemplateRepresentation(THANKS_PAGE, request, null);
    }

    private Representation getTemplateRepresentation(String template, OAuth2Request request, String errorCode) {
        TemplateRepresentation response = getTemplateFactory(getContext()).getTemplateRepresentation(template);
        Map<String, String> dataModel = new HashMap<>();
        dataModel.put("errorCode", errorCode);
        dataModel.put("baseUrl", baseURLProviderFactory.get(request.<String>getParameter("realm"))
                .getURL(ServletUtils.getRequest(getRequest())));
        List<String> locale = new ArrayList<>();
        for (Preference<Language> language : getRequest().getClientInfo().getAcceptedLanguages()) {
            locale.add(language.getMetadata().getName());
        }
        dataModel.put("locale", OAuth2Utils.join(locale, " "));
        dataModel.put("realm", request.<String>getParameter(OAuth2Constants.Params.REALM));
        response.setDataModel(dataModel);
        return response;
    }

    private void addRequestParamsFromDeviceCode(Request restletRequest, DeviceCode deviceCode) {
        Map<String, Object> codeAttributes = (Map<String, Object>) deviceCode.getObject();

        for (Map.Entry<String, Object> entry : codeAttributes.entrySet()) {
            Object value = entry.getValue();

            if (!entry.getKey().equals(OAuth2Constants.Params.SCOPE)) {
                value = ((Set<String>) value).iterator().next();
            } else {
                value = OAuth2Utils.join((Set<String>) value, " ");
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
     * user code
     *
     * @return The form to allow the user to submit their user code
     */
    @Get
    public Representation userCodeForm() throws OAuth2RestletException, InvalidGrantException, NotFoundException,
            ServerException {
        final OAuth2Request request = requestFactory.create(getRequest());
        if (request.getParameter(OAuth2Constants.DeviceCode.USER_CODE) != null) {
            return verify(null);
        } else {
            return getTemplateRepresentation(FORM, request, null);
        }
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
}

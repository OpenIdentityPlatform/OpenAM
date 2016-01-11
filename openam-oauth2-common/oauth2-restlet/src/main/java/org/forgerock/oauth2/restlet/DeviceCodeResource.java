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
 */

package org.forgerock.oauth2.restlet;

import static org.forgerock.oauth2.core.OAuth2Constants.Custom.*;
import static org.forgerock.oauth2.core.OAuth2Constants.DeviceCode.*;
import static org.forgerock.oauth2.core.OAuth2Constants.Params.*;
import static org.forgerock.openam.utils.StringUtils.isEmpty;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.DeviceCode;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.openam.oauth2.OAuth2Utils;
import org.forgerock.openam.rest.representations.JacksonRepresentationFactory;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openam.utils.StringUtils;
import org.restlet.Request;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Restlet resource for issuing new device codes.
 * @since 13.0.0
 */
public class DeviceCodeResource extends ServerResource {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");
    private final TokenStore tokenStore;
    private final OAuth2RequestFactory<?, Request> requestFactory;
    private final ClientRegistrationStore clientRegistrationStore;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final BaseURLProviderFactory baseURLProviderFactory;
    private final ExceptionHandler exceptionHandler;
    private final JacksonRepresentationFactory jacksonRepresentationFactory;
    private final OAuth2Utils oAuth2Utils;

    @Inject
    public DeviceCodeResource(TokenStore tokenStore, OAuth2RequestFactory<?, Request> requestFactory,
            ClientRegistrationStore clientRegistrationStore, OAuth2ProviderSettingsFactory providerSettingsFactory,
            BaseURLProviderFactory baseURLProviderFactory, ExceptionHandler exceptionHandler,
            JacksonRepresentationFactory jacksonRepresentationFactory, OAuth2Utils oAuth2Utils) {
        this.tokenStore = tokenStore;
        this.requestFactory = requestFactory;
        this.clientRegistrationStore = clientRegistrationStore;
        this.providerSettingsFactory = providerSettingsFactory;
        this.baseURLProviderFactory = baseURLProviderFactory;
        this.exceptionHandler = exceptionHandler;
        this.jacksonRepresentationFactory = jacksonRepresentationFactory;
        this.oAuth2Utils = oAuth2Utils;
    }

    @Post
    public Representation issueCode(Representation body) throws OAuth2RestletException {
        final Request restletRequest = getRequest();
        OAuth2Request request = requestFactory.create(restletRequest);

        String state = request.getParameter(STATE);
        // Client ID, Response Type and Scope are required, all other parameters are optional
        String clientId = request.getParameter(CLIENT_ID);
        String scope = request.getParameter(SCOPE);
        String responseType = request.getParameter(RESPONSE_TYPE);
        try {
            if (isEmpty(clientId) || isEmpty(scope) || isEmpty(responseType)) {
                throw new OAuth2RestletException(400, "bad_request",
                        "client_id, scope and response_type are required parameters", state);
            } else {
                // check client_id exists
                clientRegistrationStore.get(clientId, request);
            }

            if (scope == null) {
                scope = "";
            }
            final String maxAge = request.getParameter(MAX_AGE);
            DeviceCode code = tokenStore.createDeviceCode(
                    oAuth2Utils.split(scope, " "),
                    null,
                    clientId,
                    request.<String>getParameter(NONCE),
                    request.<String>getParameter(RESPONSE_TYPE),
                    request.<String>getParameter(STATE),
                    request.<String>getParameter(ACR_VALUES),
                    request.<String>getParameter(PROMPT),
                    request.<String>getParameter(UI_LOCALES),
                    request.<String>getParameter(LOGIN_HINT),
                    maxAge == null ? null : Integer.valueOf(maxAge),
                    request.<String>getParameter(CLAIMS),
                    request,
                    request.<String>getParameter(CODE_CHALLENGE),
                    request.<String>getParameter(CODE_CHALLENGE_METHOD));

            Map<String, Object> result = new HashMap<>();
            OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);

            result.put(DEVICE_CODE, code.getDeviceCode());
            result.put(USER_CODE, code.getUserCode());
            result.put(EXPIRES_IN, providerSettings.getDeviceCodeLifetime());
            result.put(INTERVAL, providerSettings.getDeviceCodePollInterval());

            String verificationUrl = providerSettings.getVerificationUrl();
            if (StringUtils.isBlank(verificationUrl)) {
                final HttpServletRequest servletRequest = ServletUtils.getRequest(restletRequest);
                final String realm = request.getParameter(OAuth2Constants.Custom.REALM);
                verificationUrl = baseURLProviderFactory.get(realm).getRootURL(servletRequest) + "/oauth2/device/user";
            }
            result.put(VERIFICATION_URL, verificationUrl);

            return jacksonRepresentationFactory.create(result);
        } catch (OAuth2Exception e) {
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(), state);
        }
    }

    @Override
    protected void doCatch(Throwable throwable) {
        if (!(throwable.getCause() instanceof OAuth2RestletException)) {
            logger.error("Exception when issuing device tokens", throwable.getCause());
        }
        exceptionHandler.handle(throwable, getResponse());
    }
}

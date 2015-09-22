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

import static org.forgerock.oauth2.core.OAuth2Constants.Custom.*;
import static org.forgerock.oauth2.core.OAuth2Constants.DeviceCode.*;
import static org.forgerock.oauth2.core.OAuth2Constants.Params.*;

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
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.oauth2.OAuth2Utils;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openam.utils.StringUtils;
import org.restlet.Request;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

/**
 * A Restlet resource for issuing new device codes.
 * @since 13.0.0
 */
public class DeviceCodeResource extends ServerResource {

    private final TokenStore tokenStore;
    private final OAuth2RequestFactory<Request> requestFactory;
    private final ClientRegistrationStore clientRegistrationStore;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final BaseURLProviderFactory baseURLProviderFactory;

    @Inject
    public DeviceCodeResource(TokenStore tokenStore, OAuth2RequestFactory<Request> requestFactory,
            ClientRegistrationStore clientRegistrationStore, OAuth2ProviderSettingsFactory providerSettingsFactory,
            BaseURLProviderFactory baseURLProviderFactory) {
        this.tokenStore = tokenStore;
        this.requestFactory = requestFactory;
        this.clientRegistrationStore = clientRegistrationStore;
        this.providerSettingsFactory = providerSettingsFactory;
        this.baseURLProviderFactory = baseURLProviderFactory;
    }

    @Post
    public Representation issueCode()
            throws BadRequestException, NotFoundException, InvalidClientException, ServerException {
        final Request restletRequest = getRequest();
        OAuth2Request request = requestFactory.create(restletRequest);

        // Client ID is required, all other parameters are optional
        String clientId = request.getParameter(CLIENT_ID);
        if (StringUtils.isEmpty(clientId)) {
            throw new BadRequestException("client_id is a required parameter");
        } else {
            // check client_id exists
            clientRegistrationStore.get(clientId, request);
        }

        String scope = request.getParameter(SCOPE);
        if (scope == null) {
            scope = "";
        }
        final String maxAge = request.getParameter(MAX_AGE);
        DeviceCode code = tokenStore.createDeviceCode(
                OAuth2Utils.split(scope, " "),
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
            verificationUrl = baseURLProviderFactory.get(realm).getURL(servletRequest) + "/oauth2/device/user";
        }
        result.put(VERIFICATION_URL, verificationUrl);

        return new JacksonRepresentation<>(result);
    }

}

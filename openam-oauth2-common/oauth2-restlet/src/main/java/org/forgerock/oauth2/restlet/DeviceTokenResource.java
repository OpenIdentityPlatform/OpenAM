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

import static org.forgerock.oauth2.core.OAuth2Constants.Params.*;
import static org.forgerock.openam.utils.StringUtils.*;

import javax.inject.Inject;

import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.DeviceCode;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.core.exceptions.AuthorizationDeclinedException;
import org.forgerock.oauth2.core.exceptions.AuthorizationPendingException;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.ExpiredTokenException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.restlet.Request;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Restlet resource for issuing new device codes.
 * @since 13.0.0
 */
public class DeviceTokenResource extends ServerResource {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");
    private final TokenStore tokenStore;
    private final OAuth2RequestFactory<Request> requestFactory;
    private final ClientRegistrationStore clientRegistrationStore;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final ExceptionHandler exceptionHandler;

    @Inject
    public DeviceTokenResource(TokenStore tokenStore, OAuth2RequestFactory<Request> requestFactory,
            ClientRegistrationStore clientRegistrationStore, OAuth2ProviderSettingsFactory providerSettingsFactory,
            ExceptionHandler exceptionHandler) {
        this.tokenStore = tokenStore;
        this.requestFactory = requestFactory;
        this.clientRegistrationStore = clientRegistrationStore;
        this.providerSettingsFactory = providerSettingsFactory;
        this.exceptionHandler = exceptionHandler;
    }

    @Post
    public Representation issueTokens(Representation body) throws OAuth2RestletException {
        final Request restletRequest = getRequest();
        OAuth2Request request = requestFactory.create(restletRequest);

        String state = null;
        // Client ID, Secret and code are required, all other parameters are optional
        final String clientId = request.getParameter(CLIENT_ID);
        final String clientSecret = request.getParameter(CLIENT_SECRET);
        final String code = request.getParameter(CODE);
        try {
            if (isEmpty(clientId) || isEmpty(clientSecret) || isEmpty(code)) {
                throw new BadRequestException("client_id, client_secret and code are required parameters");
            }

            ClientRegistration client = clientRegistrationStore.get(clientId, request);
            if (!clientSecret.equals(client.getClientSecret())) {
                throw new InvalidClientException();
            }

            DeviceCode deviceCode = tokenStore.readDeviceCode(clientId, code, request);

            if (deviceCode != null) {
                state = deviceCode.getState();
            }

            if (deviceCode == null ||
                    !clientId.equals(deviceCode.getClientId()) ||
                    !request.getParameter(REALM).equals(deviceCode.getRealm())) {
                throw new AuthorizationDeclinedException();
            }

            try {
                if (deviceCode.isIssued()) {
                    return new JacksonRepresentation<>(deviceCode.getTokens());
                }

                if (deviceCode.getExpiryTime() < System.currentTimeMillis()) {
                    throw new ExpiredTokenException();
                }
            } finally {
                try {
                    tokenStore.deleteDeviceCode(clientId, code, request);
                } catch (OAuth2Exception e) {
                    logger.warn("Could not delete issued/expired device code", e);
                }
            }

            OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);

            try {
                final long lastPollTime = deviceCode.getLastPollTime();
                if (lastPollTime + (providerSettings.getDeviceCodePollInterval() * 1000) > System.currentTimeMillis()) {
                    throw new BadRequestException("slow_down", "The polling interval has not elapsed since the last request");
                }

                throw new AuthorizationPendingException();
            } finally {
                deviceCode.poll();
                tokenStore.updateDeviceCode(deviceCode, request);
            }
        } catch (OAuth2Exception e) {
            logger.debug("Exception when issuing device tokens", e);
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

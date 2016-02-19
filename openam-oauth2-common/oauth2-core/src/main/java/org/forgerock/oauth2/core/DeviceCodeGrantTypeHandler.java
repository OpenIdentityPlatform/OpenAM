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

package org.forgerock.oauth2.core;

import static org.forgerock.oauth2.core.OAuth2Constants.Params.*;
import static org.forgerock.openam.utils.StringUtils.isEmpty;
import static org.forgerock.openam.utils.Time.*;

import javax.inject.Inject;
import java.util.Set;

import org.forgerock.oauth2.core.exceptions.AuthorizationDeclinedException;
import org.forgerock.oauth2.core.exceptions.AuthorizationPendingException;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailureFactory;
import org.forgerock.oauth2.core.exceptions.ExpiredTokenException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidCodeException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidScopeException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the GrantTypeHandler for the OAuth2 Device Code grant.
 *
 * @since 13.0.0
 */
public class DeviceCodeGrantTypeHandler extends GrantTypeHandler {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");
    private final TokenStore tokenStore;
    private final ClientRegistrationStore clientRegistrationStore;
    private final ClientAuthenticationFailureFactory failureFactory;
    private final GrantTypeAccessTokenGenerator accessTokenGenerator;

    @Inject
    public DeviceCodeGrantTypeHandler(OAuth2ProviderSettingsFactory providerSettingsFactory,
            ClientAuthenticator clientAuthenticator, TokenStore tokenStore,
            ClientRegistrationStore clientRegistrationStore, ClientAuthenticationFailureFactory failureFactory,
            OAuth2UrisFactory urisFactory, GrantTypeAccessTokenGenerator accessTokenGenerator) {
        super(providerSettingsFactory, urisFactory, clientAuthenticator);
        this.tokenStore = tokenStore;
        this.clientRegistrationStore = clientRegistrationStore;
        this.failureFactory = failureFactory;
        this.accessTokenGenerator = accessTokenGenerator;
    }

    @Override
    protected AccessToken handle(OAuth2Request request, ClientRegistration clientRegistration,
            OAuth2ProviderSettings providerSettings) throws RedirectUriMismatchException, InvalidRequestException,
            InvalidGrantException, InvalidCodeException, ServerException, UnauthorizedClientException,
            InvalidScopeException, NotFoundException, InvalidClientException, AuthorizationDeclinedException,
            ExpiredTokenException, BadRequestException, AuthorizationPendingException {

        // Client ID, Secret and code are required, all other parameters are optional
        final String clientId = request.getParameter(CLIENT_ID);
        final String clientSecret = request.getParameter(CLIENT_SECRET);
        final String code = request.getParameter(CODE);

        if (isEmpty(clientId) || isEmpty(clientSecret) || isEmpty(code)) {
            throw new BadRequestException("client_id, client_secret and code are required parameters");
        }

        ClientRegistration client = clientRegistrationStore.get(clientId, request);
        if (!clientSecret.equals(client.getClientSecret())) {
            throw failureFactory.getException();
        }

        DeviceCode deviceCode = tokenStore.readDeviceCode(clientId, code, request);

        if (deviceCode == null ||
                !clientId.equals(deviceCode.getClientId()) ||
                !request.getParameter(REALM).equals(deviceCode.getRealm())) {
            throw new AuthorizationDeclinedException();
        }

        try {
            if (deviceCode.isAuthorized()) {
                String grantType = request.getParameter(OAuth2Constants.Params.GRANT_TYPE);
                Set<String> scope = deviceCode.getScope();
                String resourceOwnerId = deviceCode.getResourceOwnerId();
                String validatedClaims = providerSettings.validateRequestedClaims(
                        deviceCode.getStringProperty(OAuth2Constants.Custom.CLAIMS));
                return generateAccessToken(providerSettings, grantType, clientId, resourceOwnerId, scope,
                        validatedClaims, request);
            }

            if (deviceCode.getExpiryTime() < currentTimeMillis()) {
                throw new ExpiredTokenException();
            }
        } finally {
            try {
                tokenStore.deleteDeviceCode(clientId, code, request);
            } catch (OAuth2Exception e) {
                logger.warn("Could not delete issued/expired device code", e);
            }
        }

        try {
            final long lastPollTime = deviceCode.getLastPollTime();
            if (lastPollTime + (providerSettings.getDeviceCodePollInterval() * 1000) > currentTimeMillis()) {
                throw new BadRequestException("slow_down", "The polling interval has not elapsed since the last request");
            }

            throw new AuthorizationPendingException();
        } finally {
            deviceCode.poll();
            tokenStore.updateDeviceCode(deviceCode, request);
        }
    }

    private AccessToken generateAccessToken(OAuth2ProviderSettings providerSettings, String grantType, String clientId,
            String resourceOwnerId, Set<String> scope, String validatedClaims, OAuth2Request request)
            throws ServerException, NotFoundException {
        return accessTokenGenerator.generateAccessToken(providerSettings, grantType, clientId, resourceOwnerId, null,
                scope, validatedClaims, null, null, request);
    }
}

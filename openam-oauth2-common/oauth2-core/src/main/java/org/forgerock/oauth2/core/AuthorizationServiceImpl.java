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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

import static org.forgerock.oauth2.core.OAuth2Constants.Custom.*;
import static org.forgerock.oauth2.core.OAuth2Constants.Params.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import org.forgerock.guava.common.base.Predicates;
import org.forgerock.guava.common.collect.Maps;
import org.forgerock.oauth2.core.exceptions.AccessDeniedException;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailureFactory;
import org.forgerock.oauth2.core.exceptions.InteractionRequiredException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidScopeException;
import org.forgerock.oauth2.core.exceptions.LoginRequiredException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.core.exceptions.ResourceOwnerAuthenticationRequired;
import org.forgerock.oauth2.core.exceptions.ResourceOwnerConsentRequired;
import org.forgerock.oauth2.core.exceptions.ResourceOwnerConsentRequiredException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.forgerock.oauth2.core.exceptions.UnsupportedResponseTypeException;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles authorization requests from OAuth2 clients to the OAuth2 provider to grant authorization for a specific
 * client by a specific resource owner.
 *
 * @since 12.0.0
 */
public class AuthorizationServiceImpl implements AuthorizationService {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");
    private final List<AuthorizeRequestValidator> requestValidators;
    private final ResourceOwnerSessionValidator resourceOwnerSessionValidator;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final ResourceOwnerConsentVerifier consentVerifier;
    private final ClientRegistrationStore clientRegistrationStore;
    private final AuthorizationTokenIssuer tokenIssuer;
    private final ClientAuthenticationFailureFactory failureFactory;

    /**
     * Constructs a new AuthorizationServiceImpl.
     * @param requestValidators A {@code List} of AuthorizeRequestValidators.
     * @param resourceOwnerSessionValidator An instance of the ResourceOwnerSessionValidator.
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     * @param consentVerifier An instance of the ResourceOwnerConsentVerifier.
     * @param clientRegistrationStore An instance of the ClientRegistrationStore.
     * @param tokenIssuer An instance of the AuthorizationTokenIssuer.
     * @param failureFactory The factory which creates ClientExceptions
     */
    @Inject
    public AuthorizationServiceImpl(List<AuthorizeRequestValidator> requestValidators,
            ResourceOwnerSessionValidator resourceOwnerSessionValidator,
            OAuth2ProviderSettingsFactory providerSettingsFactory, ResourceOwnerConsentVerifier consentVerifier,
            ClientRegistrationStore clientRegistrationStore, AuthorizationTokenIssuer tokenIssuer,
            ClientAuthenticationFailureFactory failureFactory) {
        this.requestValidators = requestValidators;
        this.resourceOwnerSessionValidator = resourceOwnerSessionValidator;
        this.providerSettingsFactory = providerSettingsFactory;
        this.consentVerifier = consentVerifier;
        this.clientRegistrationStore = clientRegistrationStore;
        this.tokenIssuer = tokenIssuer;
        this.failureFactory = failureFactory;
    }

    /**
     * {@inheritDoc}
     */
    public AuthorizationToken authorize(OAuth2Request request) throws ResourceOwnerAuthenticationRequired,
            ResourceOwnerConsentRequired, InvalidClientException, UnsupportedResponseTypeException,
            RedirectUriMismatchException, InvalidRequestException, AccessDeniedException, ServerException,
            LoginRequiredException, BadRequestException, InteractionRequiredException,
            ResourceOwnerConsentRequiredException, InvalidScopeException, NotFoundException {

        final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);

        for (final AuthorizeRequestValidator requestValidator : requestValidators) {
            requestValidator.validateRequest(request);
        }

        final String clientId = request.getParameter(CLIENT_ID);
        final ClientRegistration clientRegistration = clientRegistrationStore.get(clientId, request);

        final Set<String> scope = Utils.splitScope(request.<String>getParameter(SCOPE));
        //plugin point
        final Set<String> validatedScope = providerSettings.validateAuthorizationScope(clientRegistration, scope,
                request);

        // is resource owner authenticated?
        final ResourceOwner resourceOwner = resourceOwnerSessionValidator.validate(request);

        final boolean consentSaved = providerSettings.isConsentSaved(resourceOwner,
                clientRegistration.getClientId(), validatedScope);

        //plugin point
        final boolean haveConsent = consentVerifier.verify(consentSaved, request, clientRegistration);

        if (!haveConsent) {
            String localeParameter = request.getParameter(LOCALE);
            String uiLocaleParameter = request.getParameter(UI_LOCALES);
            Locale locale = getLocale(uiLocaleParameter, localeParameter);
            if (locale == null) {
                locale = request.getLocale();
            }

            UserInfoClaims userInfo = null;
            try {
                userInfo = providerSettings.getUserInfo(request.getToken(AccessToken.class), request);
            } catch (UnauthorizedClientException e) {
                logger.debug("Couldn't get user info - continuing to display consent page without claims.", e);
            }

            String clientName = clientRegistration.getDisplayName(locale);
            if (clientName == null) {
                clientName = clientRegistration.getClientId();
                logger.warn("Client does not have a display name or client name set. using client ID {} for display",
                        clientName);
            }
            final String displayDescription = clientRegistration.getDisplayDescription(locale);
            final String clientDescription = displayDescription == null ? "" : displayDescription;
            final Map<String, String> scopeDescriptions = getScopeDescriptions(validatedScope,
                    clientRegistration.getScopeDescriptions(locale));
            final Map<String, String> claimDescriptions = getClaimDescriptions(userInfo.getValues(),
                    clientRegistration.getClaimDescriptions(locale));
            throw new ResourceOwnerConsentRequired(clientName, clientDescription, scopeDescriptions, claimDescriptions,
                    userInfo, resourceOwner.getName(providerSettings));
        }

        return tokenIssuer.issueTokens(request, clientRegistration, resourceOwner, scope, providerSettings);
    }

    private Locale getLocale(String uiLocalParameter, String localeParameter) {
        if (!StringUtils.isEmpty(uiLocalParameter)) {
            return Locale.forLanguageTag(uiLocalParameter);
        } else if (!StringUtils.isEmpty(localeParameter)) {
            return Locale.forLanguageTag(localeParameter);
        } else {
            return null;
        }
    }

    /**
     * Gets the scope descriptions for the requested scopes.
     *
     * @param claims The claims being provided.
     * @param claimDescriptions The descriptions for all possible allowed claims.
     * @return A {@code Set} of requested scope descriptions.
     */
    private Map<String, String> getClaimDescriptions(Map<String, Object> claims, Map<String, String> claimDescriptions) {
        return Maps.filterKeys(claimDescriptions, Predicates.in(claims.keySet()));
    }

    /**
     * Gets the scope descriptions for the requested scopes.
     *
     * @param scopes The requested scopes.
     * @param scopeDescriptions The descriptions for all possible allowed scopes.
     * @return A {@code Set} of requested scope descriptions.
     */
    private Map<String, String> getScopeDescriptions(Set<String> scopes, Map<String, String> scopeDescriptions) {
        return Maps.filterKeys(scopeDescriptions, Predicates.in(scopes));
    }

    /**
     * {@inheritDoc}
     */
    public AuthorizationToken authorize(OAuth2Request request, boolean consentGiven, boolean saveConsent)
            throws AccessDeniedException, ResourceOwnerAuthenticationRequired, InvalidClientException,
            UnsupportedResponseTypeException, InvalidRequestException, RedirectUriMismatchException, ServerException,
            LoginRequiredException, BadRequestException, InteractionRequiredException, InvalidScopeException, NotFoundException {

        final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);

        for (final AuthorizeRequestValidator requestValidator : requestValidators) {
            requestValidator.validateRequest(request);
        }

        final ResourceOwner resourceOwner = resourceOwnerSessionValidator.validate(request);
        final ClientRegistration clientRegistration =
                clientRegistrationStore.get(request.<String>getParameter(CLIENT_ID), request);

        if (!consentGiven) {
            logger.debug("Resource Owner did not authorize the request");
            throw new AccessDeniedException("Resource Owner did not authorize the request",
                    Utils.getRequiredUrlLocation(request, clientRegistration));
        }

        final Set<String> scope = Utils.splitScope(request.<String>getParameter(SCOPE));
        final Set<String> validatedScope = providerSettings.validateAuthorizationScope(clientRegistration, scope,
                request);

        if (saveConsent) {
            providerSettings.saveConsent(resourceOwner, clientRegistration.getClientId(), validatedScope);
        }

        return tokenIssuer.issueTokens(request, clientRegistration, resourceOwner, scope, providerSettings);
    }
}

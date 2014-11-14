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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

import org.forgerock.oauth2.core.exceptions.AccessDeniedException;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.InteractionRequiredException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidScopeException;
import org.forgerock.oauth2.core.exceptions.LoginRequiredException;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.core.exceptions.ResourceOwnerAuthenticationRequired;
import org.forgerock.oauth2.core.exceptions.ResourceOwnerConsentRequired;
import org.forgerock.oauth2.core.exceptions.ResourceOwnerConsentRequiredException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnsupportedResponseTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.forgerock.oauth2.core.OAuth2Constants.Params.*;
import static org.forgerock.oauth2.core.OAuth2Constants.Custom.*;

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

    /**
     * Constructs a new AuthorizationServiceImpl.
     *
     * @param requestValidators A {@code List} of AuthorizeRequestValidators.
     * @param resourceOwnerSessionValidator An instance of the ResourceOwnerSessionValidator.
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     * @param consentVerifier An instance of the ResourceOwnerConsentVerifier.
     * @param clientRegistrationStore An instance of the ClientRegistrationStore.
     * @param tokenIssuer An instance of the AuthorizationTokenIssuer.
     */
    @Inject
    public AuthorizationServiceImpl(List<AuthorizeRequestValidator> requestValidators,
            ResourceOwnerSessionValidator resourceOwnerSessionValidator,
            OAuth2ProviderSettingsFactory providerSettingsFactory, ResourceOwnerConsentVerifier consentVerifier,
            ClientRegistrationStore clientRegistrationStore, AuthorizationTokenIssuer tokenIssuer) {
        this.requestValidators = requestValidators;
        this.resourceOwnerSessionValidator = resourceOwnerSessionValidator;
        this.providerSettingsFactory = providerSettingsFactory;
        this.consentVerifier = consentVerifier;
        this.clientRegistrationStore = clientRegistrationStore;
        this.tokenIssuer = tokenIssuer;
    }

    /**
     * {@inheritDoc}
     */
    public AuthorizationToken authorize(OAuth2Request request) throws ResourceOwnerAuthenticationRequired,
            ResourceOwnerConsentRequired, InvalidClientException, UnsupportedResponseTypeException,
            RedirectUriMismatchException, InvalidRequestException, AccessDeniedException, ServerException,
            LoginRequiredException, BadRequestException, InteractionRequiredException,
            ResourceOwnerConsentRequiredException, InvalidScopeException {

        final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);

        for (final AuthorizeRequestValidator requestValidator : requestValidators) {
            requestValidator.validateRequest(request);
        }

        final ClientRegistration clientRegistration =
                clientRegistrationStore.get(request.<String>getParameter(CLIENT_ID), request);

        final Set<String> scope = Utils.splitScope(request.<String>getParameter(SCOPE));
        //plugin point
        final Set<String> validatedScope = providerSettings.validateAuthorizationScope(clientRegistration, scope,
                request);

        // is resource owner authenticated?
        final ResourceOwner resourceOwner = resourceOwnerSessionValidator.validate(request);

        final boolean consentSaved = providerSettings.isConsentSaved(resourceOwner,
                clientRegistration.getClientId(), validatedScope);

        //plugin point
        final boolean haveConsent = consentVerifier.verify(consentSaved, request);

        if (!haveConsent) {
            String localeParameter = request.getParameter(LOCALE);
            Locale locale = null;
            if (localeParameter == null || localeParameter.isEmpty()) {
                locale = request.getLocale();
            } else {
                String[] localeComponents = localeParameter.split("-");
                if (localeComponents.length == 1) {
                    locale = new Locale(localeComponents[0]);
                } else if (localeComponents.length == 2) {
                    locale = new Locale(localeComponents[0], localeComponents[1]);
                } else if (localeComponents.length > 2) {
                    locale = new Locale(localeComponents[0], localeComponents[1], localeComponents[2]);
                }
            }
            final String clientName = clientRegistration.getDisplayName(locale);
            final String clientDescription = clientRegistration.getDisplayDescription(locale);
            final Set<String> scopeDescriptions = getScopeDescriptions(validatedScope,
                    clientRegistration.getScopeDescriptions(locale));
            throw new ResourceOwnerConsentRequired(clientName, clientDescription, scopeDescriptions);
        }

        return tokenIssuer.issueTokens(request, clientRegistration, resourceOwner, scope, providerSettings);
    }

    /**
     * Gets the scope descriptions for the requested scopes.
     *
     * @param scopes The requested scopes.
     * @param scopeDescriptions The descriptions for all possible allowed scopes.
     * @return A {@code Set} of requested scope descriptions.
     */
    private Set<String> getScopeDescriptions(Set<String> scopes, Map<String, String> scopeDescriptions) {
        final Set<String> list = new LinkedHashSet<String>();
        for (final String scope : scopes) {
            for (final Map.Entry<String, String> scopeDescription : scopeDescriptions.entrySet()) {
                if (scopeDescription.getKey().equalsIgnoreCase(scope)) {
                    list.add(scopeDescription.getValue());
                }
            }
        }
        return list;
    }

    /**
     * {@inheritDoc}
     */
    public AuthorizationToken authorize(OAuth2Request request, boolean consentGiven, boolean saveConsent)
            throws AccessDeniedException, ResourceOwnerAuthenticationRequired, InvalidClientException,
            UnsupportedResponseTypeException, InvalidRequestException, RedirectUriMismatchException, ServerException,
            LoginRequiredException, BadRequestException, InteractionRequiredException, InvalidScopeException {

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

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

package org.forgerock.oauth2.core;

import static org.forgerock.openam.oauth2.OAuth2Constants.Custom.LOCALE;
import static org.forgerock.openam.oauth2.OAuth2Constants.Custom.UI_LOCALES;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.CLIENT_ID;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.SCOPE;

import javax.inject.Inject;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import org.forgerock.oauth2.core.exceptions.AccessDeniedException;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailureFactory;
import org.forgerock.oauth2.core.exceptions.CsrfException;
import org.forgerock.oauth2.core.exceptions.DuplicateRequestParameterException;
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
public class AuthorizationService {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");
    private final List<AuthorizeRequestValidator> requestValidators;
    private final ResourceOwnerSessionValidator resourceOwnerSessionValidator;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final ResourceOwnerConsentVerifier consentVerifier;
    private final ClientRegistrationStore clientRegistrationStore;
    private final AuthorizationTokenIssuer tokenIssuer;
    private final ClientAuthenticationFailureFactory failureFactory;
    private final CsrfProtection csrfProtection;

    /**
     * Constructs a new AuthorizationServiceImpl.
     * @param requestValidators A {@code List} of AuthorizeRequestValidators.
     * @param resourceOwnerSessionValidator An instance of the ResourceOwnerSessionValidator.
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     * @param consentVerifier An instance of the ResourceOwnerConsentVerifier.
     * @param clientRegistrationStore An instance of the ClientRegistrationStore.
     * @param tokenIssuer An instance of the AuthorizationTokenIssuer.
     * @param failureFactory The factory which creates ClientExceptions
     * @param csrfProtection An instance of the CsrfProtection.
     */
    @Inject
    public AuthorizationService(List<AuthorizeRequestValidator> requestValidators,
            ResourceOwnerSessionValidator resourceOwnerSessionValidator,
            OAuth2ProviderSettingsFactory providerSettingsFactory, ResourceOwnerConsentVerifier consentVerifier,
            ClientRegistrationStore clientRegistrationStore, AuthorizationTokenIssuer tokenIssuer,
            ClientAuthenticationFailureFactory failureFactory, CsrfProtection csrfProtection) {
        this.requestValidators = requestValidators;
        this.resourceOwnerSessionValidator = resourceOwnerSessionValidator;
        this.providerSettingsFactory = providerSettingsFactory;
        this.consentVerifier = consentVerifier;
        this.clientRegistrationStore = clientRegistrationStore;
        this.tokenIssuer = tokenIssuer;
        this.failureFactory = failureFactory;
        this.csrfProtection = csrfProtection;
    }

    /**
     * Handles an initial authorization request from a OAuth2 client, validates the request is valid and contains
     * the required parameters, checks the resource owner has authenticated and given their consent for the client to
     * be authorized before issuing an AuthorizationToken.
     * <br/>
     * If the resource owner is not authenticated, then the user-agent is redirected to a login page for the
     * resource owner to authenticate. And if the resource owner has not given their consent (or have not requested
     * it to be saved from a previous authorization request) the user-agent is redirected to the user consent page.
     * The user-agent is then redirected back to the OAuth2 authorize endpoint.
     * <br/>
     * An AuthorizationToken is only ever issued by this method if the resource owner has previously given their
     * consent. In the case where the user-agent is redirected to the user consent page, when the user-agent is
     * redirected back to OAuth2 authorize endpoint the #authorize(OAuth2Request, boolean, boolean) method on this class
     * must be called.
     *
     * @param request The OAuth2Request for the client requesting authorization. Must not be {@code null}.
     * @return An AuthorizationToken containing the OAuth2 tokens granted as apart of the authorize call.
     * @throws ResourceOwnerAuthenticationRequired If the resource owner needs to authenticate before the authorize
     * request can be allowed.
     * @throws ResourceOwnerConsentRequired If the resource owner's consent is required before the authorize request
     * can be allowed.
     * @throws InvalidClientException If either the request does not contain the client's id or the client fails to be
     * authenticated.
     * @throws UnsupportedResponseTypeException If the requested response type is not supported by either the client
     * or the OAuth2 provider.
     * @throws RedirectUriMismatchException If the redirect uri on the request does not match the redirect uri
     * registered for the client.
     * @throws InvalidRequestException If the request is missing any required parameters or is otherwise malformed.
     * @throws AccessDeniedException If resource owner authentication fails.
     * @throws ServerException If any internal server error occurs.
     * @throws LoginRequiredException If authenticating the resource owner fails.
     * @throws BadRequestException If the request is malformed.
     * @throws InteractionRequiredException If the OpenID Connect prompt parameter enforces that the resource owner
     * is not asked to authenticate, but the resource owner does not have a current authenticated session.
     * @throws ResourceOwnerConsentRequiredException If the OpenID Connect prompt parameter enforces that the resource
     * owner is not asked for consent, but the resource owners consent has not been previously stored.
     * @throws IllegalArgumentException If the request is missing any required parameters.
     * @throws InvalidScopeException If the requested scope is invalid, unknown, or malformed.
     * @throws NotFoundException If the realm does not have an OAuth 2.0 provider service.
     * @throws DuplicateRequestParameterException If the request contains duplicate parameter.
     */
    public AuthorizationToken authorize(OAuth2Request request) throws ResourceOwnerAuthenticationRequired,
            ResourceOwnerConsentRequired, InvalidClientException, UnsupportedResponseTypeException,
            RedirectUriMismatchException, InvalidRequestException, AccessDeniedException, ServerException,
            LoginRequiredException, BadRequestException, InteractionRequiredException,
            ResourceOwnerConsentRequiredException, InvalidScopeException, NotFoundException,
            DuplicateRequestParameterException {

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

        final boolean requireConsent = !providerSettings.clientsCanSkipConsent()
                || !clientRegistration.isConsentImplied();

        if (requireConsent) {
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
                    userInfo = providerSettings.getUserInfo(
                            clientRegistration,
                            request.getToken(AccessToken.class),
                            request);
                } catch (UnauthorizedClientException e) {
                    logger.debug("Couldn't get user info - continuing to display consent page without claims.", e);
                }

                String clientName = clientRegistration.getDisplayName(locale);
                if (clientName == null) {
                    clientName = clientRegistration.getClientId();
                    logger.warn("Client does not have a display name or client name set. using client ID {} for " +
                            "display", clientName);
                }
                final String displayDescription = clientRegistration.getDisplayDescription(locale);
                final String clientDescription = displayDescription == null ? "" : displayDescription;
                final Map<String, String> scopeDescriptions = getScopeDescriptions(validatedScope,
                        clientRegistration.getScopeDescriptions(locale));
                final Map<String, String> claimDescriptions = getClaimDescriptions(userInfo.getValues(),
                        clientRegistration.getClaimDescriptions(locale));
                final boolean saveConsentEnabled = providerSettings.isSaveConsentEnabled();

                throw new ResourceOwnerConsentRequired(clientName, clientDescription, scopeDescriptions,
                        claimDescriptions, userInfo, resourceOwner.getName(providerSettings), saveConsentEnabled);
            }
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
     * Handles an authorization request from a OAuth2 client, validates the request is valid and contains the required
     * parameters, checks the resource owner has authenticated and given their consent for the client to be authorized
     * before issuing an AuthorizationToken.
     * <br/>
     * If the resource owner is not authenticated, then the user-agent is redirected to a login page for the
     * resource owner to authenticate.
     *
     * @param request The OAuth2Request for the client requesting authorization. Must not be {@code null}.
     * @param consentGiven {@code true} if the user has given their consent for the requesting client to be authorized.
     * @param saveConsent {@code true} if the user has requested that their consent be saved for future authorization
     * requests.
     * @return An AuthorizationToken containing the OAuth2 tokens granted as apart of the authorize call.
     * @throws AccessDeniedException If resource owner authentication fails or the resource owner does not grant
     * authorization for the client.
     * @throws ResourceOwnerAuthenticationRequired If the resource owner needs to authenticate before the authorize
     * request can be allowed.
     * @throws InvalidClientException If either the request does not contain the client's id or the client fails to be
     * authenticated.
     * @throws UnsupportedResponseTypeException If the requested response type is not supported by either the client
     * or the OAuth2 provider.
     * @throws InvalidRequestException If the request is missing any required parameters or is otherwise malformed.
     * @throws RedirectUriMismatchException If the redirect uri on the request does not match the redirect uri
     * registered for the client.
     * @throws ServerException If any internal server error occurs.
     * @throws LoginRequiredException If authenticating the resource owner fails.
     * @throws BadRequestException If the request is malformed.
     * @throws InteractionRequiredException If the OpenID Connect prompt parameter enforces that the resource owner
     * is not asked to authenticate, but the resource owner does not have a current authenticated session.
     * @throws IllegalArgumentException If the request is missing any required parameters.
     * @throws InvalidScopeException If the requested scope is invalid, unknown, or malformed.
     * @throws NotFoundException If the realm does not have an OAuth 2.0 provider service.
     * @throws DuplicateRequestParameterException If the request contains duplicate parameter.
     * @throws CsrfException If an CSRF attack is detected.
     */
    public AuthorizationToken authorize(OAuth2Request request, boolean consentGiven, boolean saveConsent)
            throws AccessDeniedException, ResourceOwnerAuthenticationRequired, InvalidClientException,
            UnsupportedResponseTypeException, InvalidRequestException, RedirectUriMismatchException, ServerException,
            LoginRequiredException, BadRequestException, InteractionRequiredException, InvalidScopeException,
            NotFoundException, DuplicateRequestParameterException, CsrfException {

        final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);

        for (final AuthorizeRequestValidator requestValidator : requestValidators) {
            requestValidator.validateRequest(request);
        }

        if (csrfProtection.isCsrfAttack(request)) {
            logger.debug("Session id from consent request does not match users session");
            throw new CsrfException();
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

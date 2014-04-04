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
import org.forgerock.oauth2.core.exceptions.ConsentRequiredException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.oauth2.core.exceptions.UnsupportedResponseTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.forgerock.oauth2.core.Utils.joinScope;

/**
 * This class is the entry point to request/gain a OAuth2 Authorization Code.
 * <br/>
 * It handles the initial request for authorization and returns a UserConsentRequest which specifies whether or not
 * the user needs to be asked for their consent. And also handles the response after the user's consent, which
 * will result in either an Authorization Code being given or an OAuth2Exception.
 *
 * @since 12.0.0
 */
public class AuthorizationService {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");
    private final ClientRegistrationStore clientRegistrationStore;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final ScopeValidator scopeValidator;
    private final RedirectUriValidator redirectUriValidator;

    /**
     * Constructs a new AuthorizationService.
     *
     * @param clientRegistrationStore An instance of the ClientRegistrationStore.
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     * @param scopeValidator An instance of the ScopeValidator.
     * @param redirectUriValidator An instance of the RedirectUriValidator.
     */
    @Inject
    public AuthorizationService(final ClientRegistrationStore clientRegistrationStore,
            final OAuth2ProviderSettingsFactory providerSettingsFactory, final ScopeValidator scopeValidator,
            final RedirectUriValidator redirectUriValidator) {
        this.clientRegistrationStore = clientRegistrationStore;
        this.providerSettingsFactory = providerSettingsFactory;
        this.scopeValidator = scopeValidator;
        this.redirectUriValidator = redirectUriValidator;
    }

    /**
     * Handles the initial request for authorization, returning a UserConsentRequest which specifies whether or not
     * the user needs to be asked for their consent before granting the Authorization Code.
     *
     * @param authorizationRequest The AuthorizationRequest instance.
     * @return A UserConsentRequest instance.
     * @throws OAuth2Exception If a problem occurs during the handling of the Authorization Code request.
     */
    public UserConsentRequest requestAuthorization(final AuthorizationRequest authorizationRequest)
            throws OAuth2Exception {

        final ClientRegistration clientRegistration = clientRegistrationStore.get(authorizationRequest.getClientId(),
                authorizationRequest.getContext());

        final String redirectUri = authorizationRequest.getRedirectUri();
        redirectUriValidator.validate(clientRegistration, redirectUri);

        final String state = authorizationRequest.getState();

        final Set<String> scope = authorizationRequest.getScope();

        final Set<String> validatedScope = scopeValidator.validateAuthorizationScope(clientRegistration, scope);

        final Set<String> responseType = authorizationRequest.getResponseType();
        validateResponseTypes(clientRegistration, responseType,
                providerSettingsFactory.getProviderSettings(authorizationRequest.getContext()));

        final ResourceOwner resourceOwner = authorizationRequest.getAuthenticationHandler().authenticate();
        if (resourceOwner == null) {
            logger.error("Unable to verify user");
            throw new InvalidGrantException();
        }

        final boolean consentSaved = providerSettingsFactory.getProviderSettings(authorizationRequest.getContext())
                .isConsentSaved(resourceOwner.getId(), clientRegistration.getClientId(), validatedScope,
                        authorizationRequest.getContext());

        final OpenIDPromptParameter prompt = authorizationRequest.getPrompt();

        if (prompt.noPrompts() && !consentSaved) {
            throw new ConsentRequiredException();
        } else if (!prompt.promptConsent() && prompt.promptLogin() && !consentSaved) {
            throw new ConsentRequiredException();
        } else if (consentSaved && !prompt.promptConsent()) {
            return new UserConsentRequest(false);
        }
        final String locale = authorizationRequest.getLocale();
        final String displayName = clientRegistration.getDisplayName(locale);
        final String displayDescription = clientRegistration.getDisplayDescription(locale);
        final Set<String> displayScope = getScopeDescriptions(validatedScope,
                clientRegistration.getAllowedScopeDescriptions(locale));

        return new UserConsentRequest(true, validatedScope, displayName, displayDescription, displayScope);
    }

    /**
     * Gets the descriptions for the requested scopes.
     *
     * @param scopes The requested scopes.
     * @param scopesWithDescriptions All the available scopes with their descriptions.
     * @return A {@code Set<String>} of the requested scope's descriptions.
     */
    private Set<String> getScopeDescriptions(final Set<String> scopes,
            final Map<String, String> scopesWithDescriptions) {
        final Set<String> list = new LinkedHashSet<String>();
        for (final String scope : scopes) {
            for (final Map.Entry<String, String> scopeDescription : scopesWithDescriptions.entrySet()) {
                if (scopeDescription.getKey().equalsIgnoreCase(scope)) {
                    list.add(scopeDescription.getValue());
                }
            }
        }
        return list;
    }

    /**
     * Validates that the requested response types are allowed for the client by the OAuth2 Provider configuration.
     *
     * @param clientRegistration The client's registration.
     * @param requestedResponseTypes The requested response types.
     * @param providerSettings The OAuth2 ProviderSettings instance.
     * @throws InvalidRequestException If no response types are allowed.
     * @throws UnsupportedResponseTypeException If the requested response types are empty or not all in the allowed
     * response types.
     */
        private void validateResponseTypes(final ClientRegistration clientRegistration,
            final Set<String> requestedResponseTypes, final OAuth2ProviderSettings providerSettings)
            throws InvalidRequestException, UnsupportedResponseTypeException {

        if (requestedResponseTypes == null || requestedResponseTypes.isEmpty()) {
            throw new UnsupportedResponseTypeException("Response type is not supported.");
        }

        final Map<String, String> allowedResponseTypes = providerSettings.getAllowedResponseTypes();

        if (allowedResponseTypes == null || allowedResponseTypes.isEmpty()) {
            throw new InvalidRequestException("Invalid Response Type.");
        }

        if (!allowedResponseTypes.keySet().containsAll(requestedResponseTypes)) {
            throw new UnsupportedResponseTypeException("Response type is not supported.");
        }

        //check if client has access to this response type
        final Set<String> clientAllowedResponseTypes = clientRegistration.getAllowedResponseTypes();
        if (!clientAllowedResponseTypes.containsAll(requestedResponseTypes)) {
            throw new UnsupportedResponseTypeException("Client does not support this response type.");
        }
    }

    /**
     * Handles the user's consent response, which will result in either an Authorization Code being given or an
     * OAuth2Exception.
     *
     * @param userConsentResponse The UserConsentResponse instance.
     * @return An Authorization object containing the tokens based from the requested response types.
     * @throws OAuth2Exception If a problem occurs during the handling of the Authorization Code request.
     */
    public Authorization authorize(final UserConsentResponse userConsentResponse) throws OAuth2Exception {

        final ResourceOwner resourceOwner = userConsentResponse.getAuthenticationHandler().authenticate();
        if (resourceOwner == null) {
            logger.error("Unable to verify user");
            throw new InvalidGrantException();
        }

        final ClientRegistration clientRegistration = clientRegistrationStore.get(userConsentResponse.getClientId(),
                userConsentResponse.getContext());

        final String redirectUri = userConsentResponse.getRedirectUri();
        redirectUriValidator.validate(clientRegistration, redirectUri);

        if (!userConsentResponse.isConsentGiven()) {
            logger.warn("AuthorizeServerResource::Resource Owner did not authorize the request");
            throw new AccessDeniedException("Resource Owner did not authorize the request");
        }

        final Set<String> scope = userConsentResponse.getScope();

        if (userConsentResponse.isSaveConsent()) {
            providerSettingsFactory.getProviderSettings(userConsentResponse.getContext())
                    .saveConsent(resourceOwner.getId(), clientRegistration.getClientId(), scope,
                            userConsentResponse.getContext());
        }

        final String state = userConsentResponse.getState();
        final String nonce = userConsentResponse.getNonce();

        final Set<String> validatedScope = scopeValidator.validateAccessTokenScope(clientRegistration, scope,
                userConsentResponse.getContext());

        final Map<String, String> allowedResponseTypes =
                providerSettingsFactory.getProviderSettings(userConsentResponse.getContext()).getAllowedResponseTypes();

        final Set<String> requestedResponseTypes = userConsentResponse.getResponseType();

        if (requestedResponseTypes == null || requestedResponseTypes.isEmpty()) {
            logger.error("Response type is empty.");
            throw new UnsupportedResponseTypeException("Response type is not supported");
        }

        final Map<String, CoreToken> tokens = new HashMap<String, CoreToken>();

        final Map<String, Object> data = new HashMap<String, Object>();
        data.put(OAuth2Constants.CoreTokenParams.TOKEN_TYPE, clientRegistration.getAccessTokenType());
        data.put(OAuth2Constants.CoreTokenParams.SCOPE, validatedScope);
        data.put(OAuth2Constants.CoreTokenParams.USERNAME, resourceOwner.getId());
        data.put(OAuth2Constants.CoreTokenParams.CLIENT_ID, clientRegistration.getClientId());
        data.put(OAuth2Constants.CoreTokenParams.REDIRECT_URI, userConsentResponse.getRedirectUri());
        data.put(OAuth2Constants.Custom.NONCE, nonce);
        data.putAll(providerSettingsFactory.getProviderSettings(userConsentResponse.getContext())
                .addAdditionalTokenData(userConsentResponse));

        boolean fragment = false;
        try {
            for (final String responseType : requestedResponseTypes) {
                if (responseType.isEmpty()) {
                    throw new UnsupportedResponseTypeException("Response type is not supported");
                }

                final String responseTypeClass = allowedResponseTypes.get(responseType);
                if (responseTypeClass == null || responseTypeClass.isEmpty()) {
                    logger.error("Response type class is empty.");
                    throw new UnsupportedResponseTypeException("Response type is not supported");
                }

                if (responseTypeClass.equalsIgnoreCase("none")) {
                    continue;
                }

                final Class<? extends ResponseType> clazz = Class.forName(responseTypeClass)
                        .asSubclass(ResponseType.class);
                final ResponseType classObj = clazz.newInstance();

                final CoreToken token = classObj.createToken(data);

                final String paramName = classObj.getURIParamValue();
                if (tokens.containsKey(paramName)) {
                    logger.error("Returning multiple response types with the same url value");
                    throw new UnsupportedResponseTypeException("Returning multiple response types with the same url "
                            + "value");
                }
                tokens.put(classObj.getURIParamValue(), token);

                if (!fragment) {
                    final String location = classObj.getReturnLocation();
                    if (location.equalsIgnoreCase("FRAGMENT")) {
                        fragment = true;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("AuthorizeServerResource.represent(): Error invoking classes for response_type", e);
            throw new UnsupportedResponseTypeException("Response type is not supported");
        }

        final Map<String, String> tokenMap = tokensToMap(tokens);

        final Map<String, String> valuesToAdd =
                scopeValidator.addAdditionalDataToReturnFromAuthorizeEndpoint(Collections.unmodifiableMap(tokens));

        if (valuesToAdd != null && !validatedScope.isEmpty()) {
            final String returnType = valuesToAdd.remove("returnType");
            if (returnType != null && !returnType.isEmpty()) {
                if (returnType.equalsIgnoreCase("FRAGMENT")) {
                    fragment = true;
                }
            }
        }
        for (final Map.Entry<String, String> entry : valuesToAdd.entrySet()) {
            tokenMap.put(entry.getKey(), entry.getValue().toString());
        }

        /*
         * scope OPTIONAL, if identical to the scope requested by the
         * client, otherwise REQUIRED. The scope of the access token as
         * described by Section 3.3.
         */
        final Set<String> scopeBefore = userConsentResponse.getScope();
        if (!scopeBefore.containsAll(scope)) {
            final Set<String> checkedScope = scopeValidator.validateAccessTokenScope(clientRegistration, scopeBefore,
                    userConsentResponse.getContext());

            if (checkedScope != null && !checkedScope.isEmpty()) {
                tokenMap.put("scope", joinScope(checkedScope));
            }

        }

        if (state != null) {
            tokenMap.put("state", state);
        }

        return new Authorization(tokenMap, fragment);
    }

    /**
     * Converts a {@code Map<String, CoreToken>} of tokens into a {@code Map<String, String>}.
     * <br/>
     * Flattens the tokens contents into a single map.
     *
     * @param tokens The tokens.
     * @return A {@code Map<String, String>} of the token information.
     */
    private Map<String, String> tokensToMap(final Map<String, CoreToken> tokens) {

        final Map<String, String> tokenMap = new HashMap<String, String>();

        for (final Map.Entry<String, CoreToken> entry : tokens.entrySet()) {
            final Map<String, Object> token = entry.getValue().convertToMap();
            if (!tokenMap.containsKey(entry.getKey())) {
                tokenMap.put(entry.getKey(), entry.getValue().getTokenID());
            }
            //if access token add extra fields
            if (entry.getValue().getTokenName().equalsIgnoreCase(OAuth2Constants.Params.ACCESS_TOKEN)) {
                for (final Map.Entry<String, Object> entryInMap : token.entrySet()) {
                    if (!tokenMap.containsKey(entryInMap.getKey())) {
                        tokenMap.put(entryInMap.getKey(), entryInMap.getValue().toString());
                    }
                }
            }
        }
        return tokenMap;
    }
}

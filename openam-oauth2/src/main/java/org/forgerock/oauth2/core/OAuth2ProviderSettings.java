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
 * Portions Copyrighted 2015 Nomura Research Institute, Ltd.
 */

package org.forgerock.oauth2.core;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.oauth2.core.Utils.isEmpty;
import static org.forgerock.oauth2.core.Utils.joinScope;
import static org.forgerock.openam.oauth2.OAuth2Constants.OAuth2ProviderService.*;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.AccessController;
import java.security.Key;
import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Hash;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.jwk.KeyUse;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SupportedEllipticCurve;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidScopeException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.forgerock.oauth2.core.exceptions.UnsupportedResponseTypeException;
import org.forgerock.oauth2.resources.ResourceSetStore;
import org.forgerock.openam.oauth2.CookieExtractor;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.oauth2.OAuthProblemException;
import org.forgerock.openam.oauth2.OpenAMAuthenticationMethod;
import org.forgerock.openam.utils.OpenAMSettingsImpl;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openidconnect.Client;
import org.forgerock.util.annotations.VisibleForTesting;
import org.forgerock.util.encode.Base64url;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Models all of the possible settings the OAuth2 provider can have and that can be configured.
 * <br/>
 * The actual implementation is responsible for providing the method in which these settings are configured. This
 * interface only describes the API for other code to get the OAuth2 provider settings.
 *
 * @since 12.0.0
 */
public class OAuth2ProviderSettings extends OpenAMSettingsImpl {

    private final Debug logger = Debug.getInstance("OAuth2Provider");
    private final String realm;
    private final ResourceSetStore resourceSetStore;
    private final CookieExtractor cookieExtractor;
    private ScopeValidator scopeValidator;
    private volatile Template loginUrlTemplate;

    /**
     * Constructs a new OpenAMOAuth2ProviderSettings.
     *
     * @param realm The realm.
     * @param resourceSetStore An instance of the ResourceSetStore for the current realm.
     * @param cookieExtractor An instance of the CookieExtractor.
     */
    public OAuth2ProviderSettings(String realm, ResourceSetStore resourceSetStore,
            CookieExtractor cookieExtractor) {
        super(OAuth2Constants.OAuth2ProviderService.NAME, OAuth2Constants.OAuth2ProviderService.VERSION);
        this.realm = realm;
        this.resourceSetStore = resourceSetStore;
        this.cookieExtractor = cookieExtractor;
        addServiceListener();
    }

    private void addServiceListener() {
        try {
            final SSOToken token = AccessController.doPrivileged(AdminTokenAction.getInstance());
            final ServiceConfigManager serviceConfigManager = new ServiceConfigManager(token,
                    OAuth2Constants.OAuth2ProviderService.NAME, OAuth2Constants.OAuth2ProviderService.VERSION);
            if (serviceConfigManager.addListener(new OAuth2ProviderSettingsChangeListener()) == null) {
                logger.error("Could not add listener to ServiceConfigManager instance. OAuth2 provider service " +
                        "changes will not be dynamically updated for realm " + realm);
            }
        } catch (Exception e) {
            String message = "OAuth2Utils::Unable to construct ServiceConfigManager: " + e;
            logger.error(message, e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, message);
        }
    }

    private final Map<String, Set<String>> attributeCache = new HashMap<String, Set<String>>();
    private final List<Map<String, Object>> jwks = new ArrayList<Map<String, Object>>();
    private Set<String> supportedScopesWithoutTranslations;
    private Set<String> supportedClaimsWithoutTranslations;

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getSetting(String realm, String attributeName) throws SSOException, SMSException {
        synchronized (attributeCache) {
            Set<String> value = attributeCache.get(attributeName);
            if (value == null) {
                value = super.getSetting(realm, attributeName);
                attributeCache.put(attributeName, value);
            }
            return value;
        }
    }

    private Set<String> getSettingStrings(String key) throws ServerException {
        try {
            return getSetting(realm, key);
        } catch (SMSException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        } catch (SSOException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        }
    }

    /**
     * Determines whether access and refresh tokens should be stateless.
     *
     * @return {@code true} if access and refresh tokens are stateless.
     * @throws ServerException If any internal server error occurs.
     */
    public boolean isStatelessTokensEnabled() throws ServerException {
        try {
            return getBooleanSetting(realm, STATELESS_TOKENS_ENABLED);
        } catch (SMSException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        } catch (SSOException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        }
    }

    /**
     * Determines whether idtokeninfo endpoint should require client authentication.
     *
     * @return {@code true} if idtokeninfo endpoint requires client authentication.
     * @throws ServerException If any internal server error occurs.
     */
    public boolean isIdTokenInfoClientAuthenticationEnabled() throws ServerException {
        try {
            return getBooleanSetting(realm, ID_TOKEN_INFO_CLIENT_AUTHENTICATION_ENABLED);
        } catch (SMSException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        } catch (SSOException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        }
    }

    /**
     * Gets the signing algorithm used when issuing stateless access and refresh tokens.
     *
     * @return The signing algorithm.
     * @throws ServerException If any internal server error occurs.
     */
    public String getTokenSigningAlgorithm() throws ServerException {
        try {
            return getStringSetting(realm, "tokenSigningAlgorithm");
        } catch (SMSException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        } catch (SSOException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        }
    }

    /**
     * Gets the Base64 encoded shared secret used to sign stateless access and refresh tokens.
     *
     * @return The Base64 encoded shared secret.
     * @throws ServerException If any internal server error occurs.
     */
    public String getTokenHmacSharedSecret() throws ServerException {
        try {
            return getStringSetting(realm, "tokenSigningHmacSharedSecret");
        } catch (SMSException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        } catch (SSOException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        }
    }

    /**
     * Gets the response types allowed by the OAuth2 provider.
     *
     * @return The allowed response types and their handler implementations.
     * @throws UnsupportedResponseTypeException If the requested response type is not supported by either the client
     *          or the OAuth2 provider.
     * @throws ServerException If any internal server error occurs.
     */
    public Map<String, ResponseTypeHandler> getAllowedResponseTypes() throws UnsupportedResponseTypeException,
            ServerException {
        try {
            Set<String> responseTypeSet = getSetting(realm, OAuth2Constants.OAuth2ProviderService.RESPONSE_TYPE_LIST);
            if (responseTypeSet == null || responseTypeSet.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<String, ResponseTypeHandler> responseTypes = new HashMap<String, ResponseTypeHandler>();
            for (String responseType : responseTypeSet){
                String[] parts = responseType.split("\\|");
                if (parts.length != 2){
                    logger.error("Response type wrong format for realm: " + realm);
                    continue;
                }
                responseTypes.put(parts[0], wrap(parts[0], parts[1]));
            }
            return responseTypes;

        } catch (SMSException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        } catch (SSOException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        }
    }

    private ResponseTypeHandler wrap(String responseTypeName, String responseTypeHandlerClassName)
            throws UnsupportedResponseTypeException {

        if (responseTypeHandlerClassName == null || responseTypeHandlerClassName.isEmpty()) {
            logger.warning("Requested a response type that is not configured. response_type=" + responseTypeName);
            throw new UnsupportedResponseTypeException("Response type is not supported");
        } else if (responseTypeHandlerClassName.equalsIgnoreCase("none")) {
            return new NoneResponseTypeHandler();
        }
        try {
            final Class<?> responseTypeHandlerClass = Class.forName(responseTypeHandlerClassName);
            return InjectorHolder.getInstance(responseTypeHandlerClass.asSubclass(ResponseTypeHandler.class));
        } catch (ClassNotFoundException e) {
            logger.error(e.getMessage());
            throw new UnsupportedResponseTypeException("Response type is not supported");
        }
    }

    /**
     * Determines if the consent can be saved or not, due to a lack of configuration.
     *
     * @return {@code true} if the consent can be saved, false if it is not configured properly.
     */
    public boolean isSaveConsentEnabled() {
        try {
            return getStringSetting(realm, OAuth2Constants.OAuth2ProviderService.SAVED_CONSENT_ATTRIBUTE)!= null;
        } catch (SMSException | SSOException e) {
            logger.error("There was a problem getting the consent configuration for realm:" + realm, e);
        }
        return false;
    }

    /**
     * Determines whether a resource owner's consent has been saved from a previous authorize request.
     *
     * @param resourceOwner The resource owner.
     * @param clientId The if of the client making the request.
     * @param scope The requested scope.
     * @return {@code true} if the resource owner has previously requested that consent should be saved from the
     *          specified client and the exact scope.
     */
    public boolean isConsentSaved(ResourceOwner resourceOwner, String clientId, Set<String> scope) {
        String consentAttribute = "";
        try {
            if (isSaveConsentEnabled()) {
                consentAttribute = getStringSetting(realm, OAuth2Constants.OAuth2ProviderService.SAVED_CONSENT_ATTRIBUTE);
                AMIdentity id = resourceOwner.getIdentity();
                if (id != null) {
                    Set<String> attributeSet = id.getAttribute(consentAttribute);
                    if (attributeSet != null) {
                        if (logger.messageEnabled()) {
                            logger.message("Existing saved consent value for resourceOwner: " + resourceOwner.getId() +
                                    " in attribute:" + consentAttribute + " in realm:" + realm + " is:" + attributeSet);
                        }
                        //check the values of the attribute set vs the scope and client requested
                        //attribute set is in the form of client_id|scope1 scope2 scope3
                        for (String consent : attributeSet) {
                            int loc = consent.indexOf(" ");
                            String consentClientId = consent.substring(0, loc);
                            String[] scopesArray = null;
                            if (loc + 1 < consent.length()) {
                                scopesArray = consent.substring(loc + 1, consent.length()).split(" ");
                            }
                            Set<String> consentScopes;
                            if (scopesArray != null && scopesArray.length > 0) {
                                consentScopes = new HashSet<String>(Arrays.asList(scopesArray));
                            } else {
                                consentScopes = new HashSet<String>();
                            }

                            //if both the client and the scopes are identical to the saved consent then approve
                            if (clientId.equals(consentClientId) && scope.equals(consentScopes)) {
                                return true;
                            }
                        }
                    } else {
                        if (logger.messageEnabled()) {
                            logger.message("No existing saved consent value for resourceOwner: " + resourceOwner.getId()
                                    + " in attribute:" + consentAttribute + " in realm:" + realm);
                        }
                    }
                }
            } else {
                logger.error("Can't save consent as it is not configured properly for the realm:" + realm);
            }
        } catch (Exception e) {
            logger.error("There was a problem getting the saved consent from the attribute: "
                    + consentAttribute + " for realm:" + realm, e);
        }

        return false;
    }

    private synchronized ScopeValidator getScopeValidator() throws ServerException {
        if (scopeValidator == null) {
            try {
                final String scopeValidatorClassName = getStringSettingValue(OAuth2Constants.OAuth2ProviderService.SCOPE_PLUGIN_CLASS);
                if (isEmpty(scopeValidatorClassName)) {
                    logger.message("Scope Validator class not set.");
                    throw new ServerException("Scope Validator class not set.");
                }
                final Class<?> scopeValidatorClass = Class.forName(scopeValidatorClassName);
                scopeValidator = InjectorHolder.getInstance(scopeValidatorClass.asSubclass(ScopeValidator.class));
            } catch (ClassNotFoundException e) {
                logger.error(e.getMessage());
                throw new ServerException(e);
            }
        }
        return scopeValidator;
    }

    /**
     * Provided as an extension point to allow the OAuth2 provider to customise the scope requested when authorization
     * is requested.
     *
     * @param clientRegistration The client registration.
     * @param scope The requested scope.
     * @return The updated scope used in the remaining OAuth2 process.
     * @throws ServerException If any internal server error occurs.
     * @throws InvalidScopeException If the requested scope is invalid, unknown, or malformed.
     */
    public Set<String> validateAuthorizationScope(ClientRegistration clientRegistration, Set<String> scope,
            OAuth2Request request) throws ServerException, InvalidScopeException {
        return getScopeValidator().validateAuthorizationScope(clientRegistration, scope, request);
    }

    /**
     * Provided as an extension point to allow the OAuth2 provider to customise the scope requested when an access token
     * is requested.
     *
     * @param clientRegistration The client registration.
     * @param scope The requested scope.
     * @param request The OAuth2 request.
     * @return The updated scope used in the remaining OAuth2 process.
     * @throws ServerException If any internal server error occurs.
     * @throws InvalidScopeException If the requested scope is invalid, unknown, or malformed.
     */
    public Set<String> validateAccessTokenScope(ClientRegistration clientRegistration, Set<String> scope,
            OAuth2Request request) throws ServerException, InvalidScopeException {
        return getScopeValidator().validateAccessTokenScope(clientRegistration, scope, request);
    }

    /**
     * Provided as an extension point to allow the OAuth2 provider to customise the scope requested when a refresh token
     * is requested.
     *
     * @param clientRegistration The client registration.
     * @param requestedScope The requested scope.
     * @param tokenScope The scope from the access token.
     * @param request The OAuth2 request.
     * @return The updated scope used in the remaining OAuth2 process.
     * @throws ServerException If any internal server error occurs.
     * @throws InvalidScopeException If the requested scope is invalid, unknown, or malformed.
     */
    public Set<String> validateRefreshTokenScope(ClientRegistration clientRegistration, Set<String> requestedScope,
            Set<String> tokenScope, OAuth2Request request) throws ServerException, InvalidScopeException {
        return getScopeValidator().validateRefreshTokenScope(clientRegistration, requestedScope, tokenScope, request);
    }

    /**
     * Gets the resource owners information based on an issued access token or request.
     *
     * @param token The access token.
     * @param request The OAuth2 request.
     * @return The claims for the resource owner's information.
     * @throws ServerException If any internal server error occurs.
     * @throws UnauthorizedClientException If the client's authorization fails.
     * @throws NotFoundException If the realm does not have an OAuth 2.0 provider service.
     */
    public UserInfoClaims getUserInfo(AccessToken token, OAuth2Request request) throws ServerException,
            UnauthorizedClientException, NotFoundException {
        return getScopeValidator().getUserInfo(token, request);
    }

    /**
     * Gets the specified access token's information.
     *
     * @param accessToken The access token.
     * @return A {@code Map<String, Object>} of the access token's information.
     * @throws ServerException If any internal server error occurs.
     */
    public Map<String, Object> evaluateScope(AccessToken accessToken) throws ServerException {
        return getScopeValidator().evaluateScope(accessToken);
    }

    /**
     * Provided as an extension point to allow the OAuth2 provider to return additional data from an authorization
     * request.
     *
     * @param tokens The tokens that will be returned from the authorization call.
     * @param request The OAuth2 request.
     * @return A {@code Map<String, String>} of the additional data to return.
     * @throws ServerException If any internal server error occurs.
     */
    public Map<String, String> additionalDataToReturnFromAuthorizeEndpoint(Map<String, Token> tokens,
            OAuth2Request request) throws ServerException {
        return getScopeValidator().additionalDataToReturnFromAuthorizeEndpoint(tokens, request);
    }

    /**
     * Provided as an extension point to allow the OAuth2 provider to return additional data from an access token
     * request.
     * <br/>
     * Any additional data to be returned should be added to the access token by invoking,
     * AccessToken#addExtraData(String, String).
     *
     * @param accessToken The access token.
     * @param request The OAuth2 request.
     * @throws ServerException If any internal server error occurs.
     * @throws InvalidClientException If either the request does not contain the client's id or the client fails to be
     *          authenticated.
     * @throws NotFoundException If the realm does not have an OAuth 2.0 provider service.
     */
    public void additionalDataToReturnFromTokenEndpoint(AccessToken accessToken, OAuth2Request request)
            throws ServerException, InvalidClientException, NotFoundException {
        getScopeValidator().additionalDataToReturnFromTokenEndpoint(accessToken, request);
    }

    /**
     * Saves the resource owner's consent for the granting authorization for the specified client with the specified
     * scope.
     *
     * @param resourceOwner The resource owner.
     * @param clientId The client id.
     * @param scope The requested scope.
     */
    public void saveConsent(ResourceOwner resourceOwner, String clientId, Set<String> scope) {

        String consentAttribute = null;
        try {
            consentAttribute = getStringSetting(realm, OAuth2Constants.OAuth2ProviderService.SAVED_CONSENT_ATTRIBUTE);
            if (consentAttribute != null) {
                AMIdentity id = resourceOwner.getIdentity();
                //get the current set of consents and add our new consent to it if they exist.
                Set<String> existing = id.getAttribute(consentAttribute);
                Set<String> consents = (existing != null) ? new HashSet<String>(existing) : new HashSet<String>(1);
                StringBuilder sb = new StringBuilder();
                if (scope == null || scope.isEmpty()) {
                    sb.append(clientId.trim()).append(" ");
                } else {
                    sb.append(clientId.trim()).append(" ").append(joinScope(scope));
                }
                consents.add(sb.toString());

                if (logger.messageEnabled()) {
                    logger.message("Saving consents:" + consents + " for resourceOwner: " + resourceOwner.getId()
                            + " in attribute:" + consentAttribute + " in realm:" + realm);
                }
                updateConsentValues(consentAttribute, id, consents);

            } else {
                logger.error("Cannot save consent as no saved consent attribute defined in realm:" + realm);
            }
        } catch (Exception e) {
            logger.error("There was a problem saving the consent into the attribute: {} for realm: {}",
                    consentAttribute, realm, e);
        }
    }

    /**
     * Revokes the resource owner's consent for the granting authorization for the specified client.
     *
     * @param userId The user id.
     * @param clientId The client id.
     */
    public void revokeConsent(String userId, String clientId) {
        String consentAttribute = null;
        try {
            consentAttribute = getStringSetting(realm, OAuth2Constants.OAuth2ProviderService.SAVED_CONSENT_ATTRIBUTE);
            if (consentAttribute != null) {
                AMIdentity id = IdUtils.getIdentity(userId, realm);

                Set<String> consents = id.getAttribute(consentAttribute);
                if (consents == null) {
                    return;
                }

                Iterator<String> iterator = consents.iterator();
                while (iterator.hasNext()) {
                    if (iterator.next().startsWith(clientId + " ")) {
                        iterator.remove();
                    }
                }
                updateConsentValues(consentAttribute, id, consents);
            }
        } catch (SMSException | SSOException | IdRepoException e) {
            logger.warning("There was a problem revoking consent from the attribute: {} for realm: {}",
                    consentAttribute, realm, e);
        }

    }

    private void updateConsentValues(String consentAttribute, AMIdentity id, Set<String> consents) throws IdRepoException, SSOException {
        //update the user profile with our new consent settings
        Map<String, Set<String>> attrs = new HashMap<>(1);
        attrs.put(consentAttribute, consents);
        id.setAttributes(attrs);
        id.store();
    }

    /**
     * Whether the OAuth2 provider should issue refresh tokens when issuing access tokens.
     *
     * @return {@code true} if refresh tokens should be issued.
     * @throws ServerException If any internal server error occurs.
     */
    public boolean issueRefreshTokens() throws ServerException {
        try {
            return getBooleanSetting(realm, OAuth2Constants.OAuth2ProviderService.ISSUE_REFRESH_TOKEN);
        } catch (SMSException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        } catch (SSOException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        }
    }

    /**
     * Whether the OAuth2 provider should issue refresh tokens when refreshing access tokens.
     *
     * @return {@code true} if refresh tokens should be issued when access tokens are refreshed.
     * @throws ServerException If any internal server error occurs.
     */
    public boolean issueRefreshTokensOnRefreshingToken() throws ServerException {
        try {
            return getBooleanSetting(realm, OAuth2Constants.OAuth2ProviderService.ISSUE_REFRESH_TOKEN_ON_REFRESHING_TOKEN);
        } catch (SMSException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        } catch (SSOException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        }
    }

    /**
     * Gets the lifetime an authorization code will have before it expires.
     *
     * @return The lifetime of an authorization code in seconds.
     * @throws ServerException If any internal server error occurs.
     */
    public long getAuthorizationCodeLifetime() throws ServerException {
        return getLongSettingValue(OAuth2Constants.OAuth2ProviderService.AUTHZ_CODE_LIFETIME_NAME);
    }

    /**
     * Gets the lifetime an access token will have before it expires.
     *
     * @return The lifetime of an access token in seconds.
     * @throws ServerException If any internal server error occurs.
     */
    public long getAccessTokenLifetime() throws ServerException {
        return getLongSettingValue(OAuth2Constants.OAuth2ProviderService.ACCESS_TOKEN_LIFETIME_NAME);
    }

    /**
     * Gets the lifetime an OpenID token will have before it expires.
     *
     * @return The lifetime of an OpenID token in seconds.
     * @throws ServerException If any internal server error occurs.
     */
    public long getOpenIdTokenLifetime() throws ServerException {
        return getLongSettingValue(OAuth2Constants.OAuth2ProviderService.JWT_TOKEN_LIFETIME_NAME);
    }

    /**
     * Gets the lifetime an refresh token will have before it expires.
     *
     * @return The lifetime of an refresh token in seconds.
     * @throws ServerException If any internal server error occurs.
     */
    public long getRefreshTokenLifetime() throws ServerException {
        return getLongSettingValue(OAuth2Constants.OAuth2ProviderService.REFRESH_TOKEN_LIFETIME_NAME);
    }

    /**
     * Gets the signing key pair of the OAuth2 provider.
     *
     * @param algorithm The signing algorithm.
     * @return The KeyPair.
     * @throws ServerException If any internal server error occurs.
     */
    public KeyPair getSigningKeyPair(JwsAlgorithm algorithm) throws ServerException {
        try {
            return getSigningKeyPair(realm, algorithm);
        } catch (SMSException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        } catch (SSOException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        }
    }

    /**
     * Gets the attributes of the resource owner that are used for authenticating resource owners.
     *
     * @return A {@code Set} of resource owner attributes.
     * @throws ServerException If any internal server error occurs.
     */
    public Set<String> getResourceOwnerAuthenticatedAttributes() throws ServerException {
        return getSettingStrings(OAuth2Constants.OAuth2ProviderService.AUTHENITCATION_ATTRIBUTES);
    }

    /**
     * Gets the supported claims for this provider.
     *
     * @return A {@code Set} of the supported claims.
     * @throws ServerException If any internal server error occurs.
     */
    public Set<String> getSupportedClaims() throws ServerException {
        return supportedClaimsWithoutTranslations = getWithoutTranslations(OAuth2Constants.OAuth2ProviderService.SUPPORTED_CLAIMS,
                supportedClaimsWithoutTranslations);
    }

    /**
     * Gets the supported claims for this provider as strings with pipe-separated translations.
     *
     * @return A {@code Set} of the supported claims.
     * @throws ServerException If any internal server error occurs.
     */
    public Set<String> getSupportedClaimsWithTranslations() throws ServerException {
        return getSettingStrings(OAuth2Constants.OAuth2ProviderService.SUPPORTED_CLAIMS);
    }

    /**
     * Gets the supported scopes for this provider without translations.
     *
     * @return A {@code Set} of the supported scopes.
     * @throws ServerException If any internal server error occurs.
     */
    public Set<String> getSupportedScopes() throws ServerException {
        return supportedScopesWithoutTranslations = getWithoutTranslations(OAuth2Constants.OAuth2ProviderService.SUPPORTED_SCOPES,
                supportedScopesWithoutTranslations);
    }

    /**
     * Gets the supported scopes for this provider.
     *
     * @return A {@code Set} of the supported scopes.
     * @throws ServerException If any internal server error occurs.
     */
    public Set<String> getSupportedScopesWithTranslations() throws ServerException {
        return getSettingStrings(OAuth2Constants.OAuth2ProviderService.SUPPORTED_SCOPES);
    }

    private Set<String> getWithoutTranslations(String key, Set<String> cached) throws ServerException {
        if (cached != null) {
            return cached;
        }
        Set<String> claims = new HashSet<>();
        try {
            synchronized (attributeCache) {
                for (String claim : getSetting(realm, key)) {
                    int pipe = claim.indexOf('|');
                    if (pipe > -1) {
                        claims.add(claim.substring(0, pipe));
                    } else {
                        claims.add(claim);
                    }
                }
                return claims;
            }
        } catch (SMSException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        } catch (SSOException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        }
    }

    /**
     * Gets the default set of scopes to give a client registering with this provider.
     *
     * @return A {@code Set} of the default scopes.
     * @throws ServerException If any internal server error occurs.
     */
    public Set<String> getDefaultScopes() throws ServerException {
        return getSettingStrings(OAuth2Constants.OAuth2ProviderService.DEFAULT_SCOPES);
    }

    /**
     * Gets the algorithms that the OAuth2 provider supports for signing OpenID tokens.
     *
     * @return A {@code Set} of the supported algorithms.
     * @throws ServerException If any internal server error occurs.
     */
    public Set<String> getSupportedIDTokenSigningAlgorithms() throws ServerException {
        return getSettingStrings(OAuth2Constants.OAuth2ProviderService.ID_TOKEN_SIGNING_ALGORITHMS);
    }

    /**
     * Gets the algorithms that the OAuth2 provider supports for encryptin OpenID tokens.
     *
     * @return A {@code Set} of the supported algorithms.
     * @throws ServerException If any internal server error occurs.
     */
    public Set<String> getSupportedIDTokenEncryptionAlgorithms() throws ServerException {
        return getSettingStrings("supportedIDTokenEncryptionAlgorithms");
    }

    /**
     * Gets the encryption methods that the OAuth2 provider supports for encryptin OpenID tokens.
     *
     * @return A {@code Set} of the supported algorithms.
     * @throws ServerException If any internal server error occurs.
     */
    public Set<String> getSupportedIDTokenEncryptionMethods() throws ServerException {
        return getSettingStrings("supportedIDTokenEncryptionMethods");
    }

    /**
     * Gets the supported version of the OpenID Connect specification.
     *
     * @return The OpenID Connect version.
     */
    public String getOpenIDConnectVersion() {
        return "3.0";
    }

    /**
     * Gets the JWK Set for this OAuth2 Authorization /OpenID Provider.
     *
     * @return The JWK Set of signing and encryption keys.
     */
    public JsonValue getJWKSet() throws ServerException {
        synchronized (jwks) {
            if (jwks.isEmpty()) {
                try {
                    Key key = getSigningKeyPair(realm, JwsAlgorithm.RS256).getPublic();
                    if (key != null && "RSA".equals(key.getAlgorithm())) {
                        jwks.add(createRSAJWK(getTokenSigningRSAKeyAlias(), (RSAPublicKey) key, KeyUse.SIG,
                                JwsAlgorithm.RS256.name()));
                    } else {
                        logger.error("Incorrect Public Key type for RSA signing algorithm");
                    }

                    Set<String> ecdsaAlgorithmAliases = getSetting(realm, TOKEN_SIGNING_ECDSA_KEYSTORE_ALIAS);
                    for (String algorithmAlias : ecdsaAlgorithmAliases) {
                        if (StringUtils.isEmpty(algorithmAlias)) {
                            logger.warning("Empty ECDSA signing key alias");
                            continue;
                        }
                        String[] aliasSplit = algorithmAlias.split("\\|");
                        if (aliasSplit.length != 2) {
                            logger.warning("Invalid ECDSA signing key alias mapping: " + algorithmAlias);
                            continue;
                        }
                        String alias = aliasSplit[1];
                        key = getSigningKeyPair(realm, JwsAlgorithm.valueOf(aliasSplit[0].toUpperCase())).getPublic();
                        if (key == null) {
                            continue;
                        }
                        if ("EC".equals(key.getAlgorithm())) {
                            jwks.add(createECJWK(alias, (ECPublicKey) key, KeyUse.SIG));
                        } else {
                            logger.error("Incorrect Public Key type for ECDSA signing algorithm. Alias: "
                                    + algorithmAlias);
                        }
                    }
                } catch (SMSException | SSOException e) {
                    throw new ServerException(e);
                }
            }
        }
        return new JsonValue(Collections.singletonMap("keys", jwks));
    }

    @VisibleForTesting
    static Map<String, Object> createRSAJWK(String alias, RSAPublicKey key, KeyUse use, String alg)
            throws ServerException {
        String kid = Hash.hash(alias + key.getModulus().toString() + key.getPublicExponent().toString());
        return json(object(field("kty", "RSA"), field(OAuth2Constants.JWTTokenParams.KEY_ID, kid),
                field("use", use.toString()), field("alg", alg),
                field("n", Base64url.encode(key.getModulus().toByteArray())),
                field("e", Base64url.encode(key.getPublicExponent().toByteArray())))).asMap();
    }

    @VisibleForTesting
    static Map<String, Object> createECJWK(String alias, ECPublicKey key, KeyUse use) throws ServerException {
        BigInteger x = key.getW().getAffineX();
        BigInteger y = key.getW().getAffineY();
        SupportedEllipticCurve curve = SupportedEllipticCurve.forKey(key);
        String kid = Hash.hash(alias + ':' + curve.getStandardName() + ':' + x.toString() + ':' + y.toString());
        return json(object(field("kty", "EC"), field(OAuth2Constants.JWTTokenParams.KEY_ID, kid),
                field("use", use.toString()), field("alg", curve.getJwsAlgorithm().name()),
                field("x", Base64url.encode(x.toByteArray())),
                field("y", Base64url.encode(y.toByteArray())),
                field("crv", curve.getStandardName()))).asMap();
    }

    private String getTokenSigningRSAKeyAlias() throws ServerException {
        String alias;
        try {
            alias = getStringSetting(realm, TOKEN_SIGNING_RSA_KEYSTORE_ALIAS);
        } catch (SSOException | SMSException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        }
        if (StringUtils.isBlank(alias)) {
            logger.error("Alias of Token Signing Key not set.");
            throw new ServerException("Alias of Token Signing Key not set.");
        } else if ("test".equals(alias)) {
            logger.warning("Alias of Token Signing Key should be changed from default, 'test'.");
        }
        return alias;
    }

    /**
     * Gets the created timestamp attribute name.
     *
     * @return The created attribute timestamp attribute name.
     */
    public String getCreatedTimestampAttributeName() throws ServerException {
        return getStringSettingValue(OAuth2Constants.OAuth2ProviderService.CREATED_TIMESTAMP_ATTRIBUTE_NAME);
    }

    /**
     * Gets the modified timestamp attribute name.
     *
     * @return The modified attribute timestamp attribute name.
     */
    public String getModifiedTimestampAttributeName() throws ServerException {
        return getStringSettingValue(OAuth2Constants.OAuth2ProviderService.MODIFIED_TIMESTAMP_ATTRIBUTE_NAME);
    }

    /**
     * Gets the subject types supported by the OAuth2 provider.
     *
     * @return A {@code Set} of supported subject types.
     * @throws ServerException If any internal server error occurs.
     */
    public Set<String> getSupportedSubjectTypes() throws ServerException {
        return getSettingStrings(OAuth2Constants.OAuth2ProviderService.SUBJECT_TYPES_SUPPORTED);
    }

    /**
     * Indicates whether clients may register without providing an access token.
     *
     * @return true if allowed, otherwise false.
     * @throws ServerException If any internal server error occurs.
     */
    public boolean isOpenDynamicClientRegistrationAllowed() throws ServerException {
        try {
            return getBooleanSetting(realm, OAuth2Constants.OAuth2ProviderService.OPEN_DYNAMIC_REGISTRATION_ALLOWED);
        } catch (SSOException e) {
            logger.message(e.getMessage());
            throw new ServerException(e);
        } catch (SMSException e) {
            logger.message(e.getMessage());
            throw new ServerException(e);
        }
    }

    /**
     * Whether to generate access tokens for clients that register without one. Only enabled if
     * {@link #isOpenDynamicClientRegistrationAllowed()} is true.
     *
     * @return true if an access token should be generated for clients that register without one.
     * @throws ServerException If any internal server error occurs.
     */
    public boolean isRegistrationAccessTokenGenerationEnabled() throws ServerException {
        try {
            return getBooleanSetting(realm, OAuth2Constants.OAuth2ProviderService.GENERATE_REGISTRATION_ACCESS_TOKENS);
        } catch (SSOException e) {
            logger.message(e.getMessage());
            throw new ServerException(e);
        } catch (SMSException e) {
            logger.message(e.getMessage());
            throw new ServerException(e);
        }
    }

    /**
     * Returns a mapping from Authentication Context Class Reference (ACR) values (typically a Level of Assurance
     * value) to concrete authentication methods.
     */
    public Map<String, AuthenticationMethod> getAcrMapping() throws ServerException {
        try {
            final Map<String, String> map = getMapSetting(realm,
                    OAuth2Constants.OAuth2ProviderService.ACR_VALUE_MAPPING);
            final Map<String, AuthenticationMethod> methods = new HashMap<String, AuthenticationMethod>(map.size());
            for (Map.Entry<String, String> entry : map.entrySet()) {
                methods.put(entry.getKey(),
                        new OpenAMAuthenticationMethod(entry.getValue(), AuthContext.IndexType.SERVICE));
            }
            return methods;
        } catch (SSOException e) {
            logger.message(e.getMessage());
            throw new ServerException(e);
        } catch (SMSException e) {
            logger.message(e.getMessage());
            throw new ServerException(e);
        }
    }

    /**
     * The default Authentication Context Class Reference (ACR) values to use for authentication if none is specified
     * in the request. This is a space-separated list of values in preference order.
     */
    public String getDefaultAcrValues() throws ServerException {
        return getStringSettingValue(OAuth2Constants.OAuth2ProviderService.DEFAULT_ACR);
    }

    private String getStringSettingValue(String key) throws ServerException {
        try {
            return getStringSetting(realm, key);
        } catch (SSOException | SMSException e) {
            logger.message("Could not get value of " + key, e);
            throw new ServerException(e);
        }
    }

    private long getLongSettingValue(String key) throws ServerException {
        try {
            return getLongSetting(realm, key);
        } catch (SSOException | SMSException e) {
            logger.error("Could not get value of " + key, e);
            throw new ServerException(e);
        }
    }

    /**
     * The mappings between amr values and auth module names.
     * @return
     */
    public Map<String, String> getAMRAuthModuleMappings() throws ServerException {
        try {
            return getMapSetting(realm, OAuth2Constants.OAuth2ProviderService.AMR_VALUE_MAPPING);
        } catch (SSOException e) {
            logger.message(e.getMessage());
            throw new ServerException(e);
        } catch (SMSException e) {
            logger.message(e.getMessage());
            throw new ServerException(e);
        }
    }

    /**
     * Checks whether the config exists.
     * @return Whether it exists.
     */
    public boolean exists() {
        try {
            return hasConfig(realm);
        } catch (Exception e) {
            logger.message("Could not access realm config", e);
            return false;
        }
    }

    /**
     * Returns the ResourceSetStore instance for the realm.
     *
     * @return The ResourceSetStore instance.
     */
    public ResourceSetStore getResourceSetStore() {
        return resourceSetStore;
    }

    /**
     * Returns whether this provider supports claims requested via 'claims' parameter.
     *
     * @return true or false.
     */
    public boolean getClaimsParameterSupported() throws ServerException {
        try {
            return getBooleanSetting(realm, OAuth2Constants.OAuth2ProviderService.CLAIMS_PARAMETER_SUPPORTED);
        } catch (SSOException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        } catch (SMSException e) {
            logger.error(e.getMessage());
        }

        return false;
    }

    /**
     * Validates that the requested claims are appropriate to be requested by the given client.
     */
    public String validateRequestedClaims(String requestedClaims) throws InvalidRequestException, ServerException {

        if (!getClaimsParameterSupported()) {
            return null;
        }

        if (StringUtils.isBlank(requestedClaims)) {
            return null;
        }

        final Set<String> claims = new HashSet<String>();

        try {
            JSONObject json = new JSONObject(requestedClaims);
            JSONObject userinfo = json.optJSONObject(OAuth2Constants.UserinfoEndpoint.USERINFO);
            JSONObject id_token = json.optJSONObject(OAuth2Constants.JWTTokenParams.ID_TOKEN);

            if (userinfo != null) {
                Iterator<String> it = userinfo.keys();
                while (it.hasNext()) {
                    claims.add(it.next());
                }
            }

            if (id_token != null) {
                Iterator<String> it = id_token.keys();
                while (it.hasNext()) {
                    claims.add(it.next());
                }
            }
        } catch (JSONException e) {
            throw new InvalidRequestException("Requested claims must be valid json.");

        }

        if (!getSupportedClaims().containsAll(claims)) {
            throw new InvalidRequestException("Requested claims must be allowed by the client's configuration");
        }

        return requestedClaims;
    }

    /**
     * Returns the token_endpoint_auth_methods available for clients to register (and subsequently auth) using.
     */
    public Set<String> getEndpointAuthMethodsSupported() {
        Set<String> supported = new HashSet<String>();

        for (Client.TokenEndpointAuthMethod method : Client.TokenEndpointAuthMethod.values()) {
            supported.add(method.getType());
        }

        return supported;
    }

    /**
     * Whether or not to enforce the Code Verifier Parameter
     * @return Whether the Code Verifier option has been configured
     * @see <a href="https://tools.ietf.org/html/draft-ietf-oauth-spop-12"</a>
     */
    public boolean isCodeVerifierRequired() throws ServerException {
        try {
            return getBooleanSetting(realm, OAuth2Constants.OAuth2ProviderService.CODE_VERIFIER);
        } catch (SSOException | SMSException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        }
    }

    /**
     * Returns the salt to use for hashing sub values upon pairwise requests.
     */
    public String getHashSalt() throws ServerException {
        return getStringSettingValue(OAuth2Constants.OAuth2ProviderService.HASH_SALT);
    }

    /**
     * Whether to always add claims to id_tokens - non-spec compliant.
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#ScopeClaims">OpenID Connect Specification</a>
     */
    public boolean isAlwaysAddClaimsToToken() throws ServerException {
        try {
            return getBooleanSetting(realm, OAuth2Constants.OAuth2ProviderService.ALWAYS_ADD_CLAIMS_TO_TOKEN);
        } catch (SSOException | SMSException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        }
    }

    /**
     * The attribute that can be used to obtain a UI-displayable name for a user's AMIdentity.
     */
    public String getUserDisplayNameAttribute() throws ServerException {
        return getStringSettingValue(OAuth2Constants.OAuth2ProviderService.USER_DISPLAY_NAME_ATTRIBUTE);
    }

    /**
     * Gets the JSON Web Key Set URI.
     *
     * @return The JWKS URI.
     * @throws ServerException If any internal server error occurs.
     */
    public String getJWKSUri() throws ServerException {
        return getStringSettingValue(OAuth2Constants.OAuth2ProviderService.JKWS_URI);
    }

    /**
     * Gets the custom login url template which will create the url to redirect resource owners to for authentication.
     *
     * @return The custom login url template.
     * @throws ServerException If the custom login url template setting could not be retrieved.
     */
    public Template getCustomLoginUrlTemplate() throws ServerException {
        try {
            String loginUrlTemplateString = getStringSetting(realm, OAuth2Constants.OAuth2ProviderService.RESOURCE_OWNER_CUSTOM_LOGIN_URL_TEMPLATE);
            if (loginUrlTemplateString != null) {
                loginUrlTemplate = new Template("customLoginUrlTemplate", new StringReader(loginUrlTemplateString),
                        new Configuration());
            }
            return loginUrlTemplate;
        } catch (SSOException | IOException | SMSException e) {
            logger.message(e.getMessage());
            throw new ServerException(e);
        }
    }

    /**
     * The URL that the user will be instructed to visit to complete their OAuth 2 login and consent when using the
     * device code flow.
     * @return The verification URL.
     * @throws ServerException If the setting could not be retrieved.
     */
    public String getVerificationUrl() throws ServerException {
        return getStringSettingValue(OAuth2Constants.OAuth2ProviderService.DEVICE_VERIFICATION_URL);
    }

    /**
     * The URL that the user will be sent to on completion of their OAuth 2 login and consent when using the device code flow.
     * @return The completion URL.
     * @throws ServerException If the setting could not be retrieved.
     */
    public String getCompletionUrl() throws ServerException {
        return getStringSettingValue(OAuth2Constants.OAuth2ProviderService.DEVICE_COMPLETION_URL);
    }

    /**
     * The lifetime of the device code.
     * @return The lifetime in seconds.
     * @throws ServerException If the setting could not be retrieved.
     */
    public int getDeviceCodeLifetime() throws ServerException {
        return (int) getLongSettingValue(OAuth2Constants.OAuth2ProviderService.DEVICE_CODE_LIFETIME);
    }

    /**
     * The polling interval for devices waiting for tokens when using the device code flow.
     * @return The interval in seconds.
     * @throws ServerException If the setting could not be retrieved.
     */
    public int getDeviceCodePollInterval() throws ServerException {
        return (int) getLongSettingValue(OAuth2Constants.OAuth2ProviderService.DEVICE_CODE_POLL_INTERVAL);
    }

    /**
     * Whether to generate and store an ops token in CTS for this OIDC provider.
     * @return <code>true</code> if ops tokens should be generated/stored in CTS.
     * @throws ServerException If the setting could not be retrieved.
     */
    public boolean shouldStoreOpsTokens() throws ServerException {
        return Boolean.parseBoolean(getStringSettingValue(OAuth2Constants.OAuth2ProviderService.STORE_OPS_TOKENS));
    }

    /**
     * Whether clients can opt to skip resource owner consent during authorization flows.
     * @return <code>true</code> if clients are allowed to opt to skip resource owner consent.
     * @throws ServerException If the setting could not be retrieved.
     */
    public boolean clientsCanSkipConsent() throws ServerException {
        return Boolean.parseBoolean(getStringSettingValue(OAuth2Constants.OAuth2ProviderService.CLIENTS_CAN_SKIP_CONSENT));
    }

    /**
     * ServiceListener implementation to clear cache when it changes.
     */
    private final class OAuth2ProviderSettingsChangeListener implements ServiceListener {

        public void schemaChanged(String serviceName, String version) {
            logger.warning("The schemaChanged ServiceListener method was invoked for service " + serviceName
                    + ". This is unexpected.");
        }

        public void globalConfigChanged(String serviceName, String version, String groupName, String serviceComponent,
                int type) {
            logger.warning("The globalConfigChanged ServiceListener method was invoked for service " + serviceName);
            //if the global config changes, all organizationalConfig change listeners are invoked as well.
        }

        public void organizationConfigChanged(String serviceName, String version, String orgName, String groupName,
                String serviceComponent, int type) {
            if (currentRealmTargetedByOrganizationUpdate(serviceName, version, orgName, type)) {
                if (logger.messageEnabled()) {
                    logger.message("Updating OAuth service configuration state for realm " + realm);
                }
                synchronized (attributeCache) {
                    attributeCache.clear();
                    jwks.clear();
                    loginUrlTemplate = null;
                }
            } else {
                if (logger.messageEnabled()) {
                    logger.message("Got service update message, but update did not target OAuth2Provider in " +
                            realm + " realm. ServiceName: " + serviceName + " version: " + version + " orgName: " +
                            orgName + " groupName: " + groupName + " serviceComponent: " + serviceComponent +
                            " type (modified=4, delete=2, add=1): " + type + " realm as DN: "
                            + DNMapper.orgNameToDN(realm));
                }
            }
        }

        /*
        The listener receives updates for all changes for each service instance in a given realm. I want to be sure
        that I only pull updates as necessary if the update pertains to this particular realm.
         */
        private boolean currentRealmTargetedByOrganizationUpdate(String serviceName, String version, String orgName,
                int type) {
            return OAuth2Constants.OAuth2ProviderService.NAME.equals(serviceName) &&
                    OAuth2Constants.OAuth2ProviderService.VERSION.equals(version) &&
                    (orgName != null) &&
                    orgName.equalsIgnoreCase(DNMapper.orgNameToDN(realm));
        }
    }

}

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

package org.forgerock.openam.oauth2;

import static org.forgerock.oauth2.core.OAuth2Constants.Params.OPENID;
import static org.forgerock.oauth2.core.OAuth2Constants.TokenEndpoint.CLIENT_CREDENTIALS;
import static org.forgerock.oauth2.core.Utils.splitScope;
import static org.forgerock.openam.scripting.ScriptConstants.EMPTY_SCRIPT_SELECTION;
import static org.forgerock.openam.scripting.ScriptConstants.OIDC_CLAIMS_NAME;

import com.iplanet.am.sdk.AMHashMap;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.SMSException;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.ScopeValidator;
import org.forgerock.oauth2.core.Token;
import org.forgerock.oauth2.core.UserInfoClaims;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidScopeException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.forgerock.openam.scripting.ScriptEvaluator;
import org.forgerock.openam.scripting.ScriptObject;
import org.forgerock.openam.scripting.SupportedScriptingLanguage;
import org.forgerock.openam.scripting.service.ScriptConfiguration;
import org.forgerock.openam.scripting.service.ScriptingServiceFactory;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.OpenAMSettings;
import org.forgerock.openam.utils.OpenAMSettingsImpl;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openidconnect.OpenIDTokenIssuer;
import org.forgerock.openidconnect.OpenIdConnectClientRegistration;
import org.forgerock.openidconnect.OpenIdConnectClientRegistrationStore;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.Request;
import org.restlet.ext.servlet.ServletUtils;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.security.auth.Subject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.security.AccessController;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Provided as extension points to allow the OpenAM OAuth2 provider to customise the requested scope of authorize,
 * access token and refresh token requests and to allow the OAuth2 provider to return additional data from these
 * endpoints as well.
 *
 * @since 12.0.0
 */
@Singleton
public class OpenAMScopeValidator implements ScopeValidator {

    private static final String MULTI_ATTRIBUTE_SEPARATOR = ",";
    private static final String DEFAULT_TIMESTAMP = "0";
    private static final DateFormat TIMESTAMP_DATE_FORMAT = new SimpleDateFormat("yyyyMMddhhmmss");
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final Debug logger = Debug.getInstance("OAuth2Provider");
    private final IdentityManager identityManager;
    private final OpenIDTokenIssuer openIDTokenIssuer;
    private final OpenAMSettings openAMSettings;
    private final ScriptEvaluator scriptEvaluator;
    private final OpenIdConnectClientRegistrationStore clientRegistrationStore;
    private final ScriptingServiceFactory scriptingServiceFactory;

    private Subject adminSubject;

    /**
     * Constructs a new OpenAMScopeValidator.
     *
     * @param identityManager An instance of the IdentityManager.
     * @param openIDTokenIssuer An instance of the OpenIDTokenIssuer.
     * @param providerSettingsFactory An instance of the CTSPersistentStore.
     * @param openAMSettings An instance of the OpenAMSettings.
     * @param scriptEvaluator An instance of the OIDC Claims ScriptEvaluator.
     * @param clientRegistrationStore An instance of the OpenIdConnectClientRegistrationStore.
     * @param scriptingServiceFactory An instance of the ScriptingServiceFactory.
     */
    @Inject
    public OpenAMScopeValidator(IdentityManager identityManager, OpenIDTokenIssuer openIDTokenIssuer,
            OAuth2ProviderSettingsFactory providerSettingsFactory, OpenAMSettings openAMSettings,
            @Named(OIDC_CLAIMS_NAME) ScriptEvaluator scriptEvaluator,
            OpenIdConnectClientRegistrationStore clientRegistrationStore,
            ScriptingServiceFactory scriptingServiceFactory) {
        this.identityManager = identityManager;
        this.openIDTokenIssuer = openIDTokenIssuer;
        this.providerSettingsFactory = providerSettingsFactory;
        this.openAMSettings = openAMSettings;
        this.scriptEvaluator = scriptEvaluator;
        this.clientRegistrationStore = clientRegistrationStore;
        this.scriptingServiceFactory = scriptingServiceFactory;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> validateAuthorizationScope(ClientRegistration client, Set<String> scope,
            OAuth2Request request) throws InvalidScopeException, ServerException {
        return validateScopes(scope, client.getDefaultScopes(), client.getAllowedScopes(), request);
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> validateAccessTokenScope(ClientRegistration client, Set<String> scope,
            OAuth2Request request) throws InvalidScopeException, ServerException {
        return validateScopes(scope, client.getDefaultScopes(), client.getAllowedScopes(), request);
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> validateRefreshTokenScope(ClientRegistration clientRegistration, Set<String> requestedScope,
            Set<String> tokenScope, OAuth2Request request) throws ServerException, InvalidScopeException {
        return validateScopes(requestedScope, tokenScope, tokenScope, request);
    }

    private Set<String> validateScopes(Set<String> requestedScopes, Set<String> defaultScopes,
            Set<String> allowedScopes, OAuth2Request request) throws InvalidScopeException, ServerException {
        Set<String> scopes;

        if (requestedScopes == null || requestedScopes.isEmpty()) {
            scopes = defaultScopes;
        } else {
            scopes = new HashSet<String>(allowedScopes);
            scopes.retainAll(requestedScopes);
            if (requestedScopes.size() > scopes.size()) {
                Set<String> invalidScopes = new HashSet<String>(requestedScopes);
                invalidScopes.removeAll(allowedScopes);
                throw InvalidScopeException.create("Unknown/invalid scope(s): " + invalidScopes.toString(), request);
            }
        }

        if (scopes == null || scopes.isEmpty()) {
            throw InvalidScopeException.create("No scope requested and no default scope configured", request);
        }

        return scopes;
    }

    /**
     * {@inheritDoc}
     */
    public UserInfoClaims getUserInfo(AccessToken token, OAuth2Request request)
            throws UnauthorizedClientException, NotFoundException {

        Map<String, Object> response = new HashMap<>();
        Bindings scriptVariables = new SimpleBindings();
        SSOToken ssoToken = getUsersSession(request);
        String realm;
        Set<String> scopes;
        AMIdentity id;
        OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
        Map<String, Set<String>> requestedClaimsValues = gatherRequestedClaims(providerSettings, request, token);

        try {
            if (token != null) {

                OpenIdConnectClientRegistration clientRegistration;
                try {
                    clientRegistration = clientRegistrationStore.get(token.getClientId(), request);
                } catch (InvalidClientException e) {
                    logger.message("Unable to retrieve client from store.");
                    throw new NotFoundException("No valid client registration found.");
                }
                final String subId = clientRegistration.getSubValue(token.getResourceOwnerId(), providerSettings);

                realm = token.getRealm(); //data comes from token when we have one
                scopes = token.getScope();
                id = identityManager.getResourceOwnerIdentity(token.getResourceOwnerId(), realm);

                response.put(OAuth2Constants.JWTTokenParams.SUB, subId);
                response.put(OAuth2Constants.JWTTokenParams.UPDATED_AT, getUpdatedAt(token.getResourceOwnerId(),
                        token.getRealm(), request));
            } else {
                //otherwise we're simply reading claims into the id_token, so grab it from the request/ssoToken
                realm = DNMapper.orgNameToRealmName(ssoToken.getProperty(ISAuthConstants.ORGANIZATION));
                id = identityManager.getResourceOwnerIdentity(ssoToken.getProperty(ISAuthConstants.USER_ID), realm);
                String scopeStr = request.getParameter(OAuth2Constants.Params.SCOPE);
                scopes = splitScope(scopeStr);
            }

            scriptVariables.put(OAuth2Constants.ScriptParams.SCOPES, getScriptFriendlyScopes(scopes));
            scriptVariables.put(OAuth2Constants.ScriptParams.IDENTITY, id);
            scriptVariables.put(OAuth2Constants.ScriptParams.LOGGER, logger);
            scriptVariables.put(OAuth2Constants.ScriptParams.CLAIMS, response);
            scriptVariables.put(OAuth2Constants.ScriptParams.SESSION, ssoToken);
            scriptVariables.put(OAuth2Constants.ScriptParams.REQUESTED_CLAIMS, requestedClaimsValues);

            ScriptObject script = getOIDCClaimsExtensionScript(realm);
            try {
                return scriptEvaluator.evaluateScript(script, scriptVariables);
            } catch (ScriptException e) {
                logger.message("Error running OIDC claims script", e);
                throw new ServerException("Error running OIDC claims script: " + e.getMessage());
            }
        } catch (ServerException e) {
            //API does not allow ServerExceptions to be thrown!
            throw new NotFoundException(e.getMessage());
        } catch (SSOException e) {
            throw new NotFoundException(e.getMessage());
        }
    }

    private Set<String> getScriptFriendlyScopes(Set<String> scopes) {
        return scopes == null ? new HashSet<String>() : new HashSet<>(scopes);
    }

    private Map<String, Set<String>> gatherRequestedClaims(OAuth2ProviderSettings providerSettings,
                                                           OAuth2Request request, AccessToken token) {
        Request req = request.getRequest();

        if (token != null) { //claims are in the extra data in the AccessToken
            String claimsJson = token.getClaims();
            if (req.getResourceRef().getLastSegment().equals(OAuth2Constants.UserinfoEndpoint.USERINFO)) {
                return gatherRequestedClaims(providerSettings, claimsJson, OAuth2Constants.UserinfoEndpoint.USERINFO);
            } else {
                return gatherRequestedClaims(providerSettings, claimsJson, OAuth2Constants.JWTTokenParams.ID_TOKEN);
            }
        } else {
            String json = request.getParameter(OAuth2Constants.Custom.CLAIMS);
            return gatherRequestedClaims(providerSettings, json, OAuth2Constants.JWTTokenParams.ID_TOKEN);
        }
    }


    /**
     * Generates a map for the claims specifically requested as per Section 5.5 of the spec.
     * Ends up mapping requested claims against a set of their optional values (empty if
     * claim is requested but no suggested/required values given).
     */
    private Map<String, Set<String>> gatherRequestedClaims(OAuth2ProviderSettings providerSettings,
                                                           String claimsJson, String objectName) {

        final Map<String, Set<String>> requestedClaims = new HashMap<String, Set<String>>();

        try {
            if (providerSettings.getClaimsParameterSupported() && claimsJson != null) {
                try {
                    final JSONObject claimsObject = new JSONObject(claimsJson);

                    JSONObject subClaimsRequest = claimsObject.getJSONObject(objectName);
                    Iterator<String> it = subClaimsRequest.keys();
                    while (it.hasNext()) {
                        final String keyName = it.next();

                        JSONObject optObj = subClaimsRequest.optJSONObject(keyName);
                        final HashSet<String> options = new HashSet<String>();

                        if (optObj != null) {
                            final JSONArray optArray = optObj.optJSONArray(OAuth2Constants.Params.VALUES);

                            if (optArray != null) {
                                for (int i = 0; i < optArray.length(); i++) {
                                    options.add(optArray.getString(i));
                                }
                            }

                            final String value = optObj.optString(OAuth2Constants.Params.VALUE);
                            if (!StringUtils.isBlank(value)) {
                                options.add(value);
                            }
                        }

                        requestedClaims.put(keyName, options);

                    }
                } catch (JSONException e) {
                    //ignorable
                }
            }
        } catch (ServerException e) {
            logger.message("Requested Claims Supported not set.");
        }

        return requestedClaims;

    }

    /**
     * Attempts to get the user's session, which can either be set on the OAuth2Request explicitly
     * or found as a cookie on the http request.
     *
     * @param request The OAuth2Request.
     * @return The user's SSOToken or {@code null} if no session was found.
     */
    private SSOToken getUsersSession(OAuth2Request request) {
        String sessionId = request.getSession();
        if (sessionId == null) {
            final HttpServletRequest req = ServletUtils.getRequest(request.<Request>getRequest());
            if (req.getCookies() != null) {
                final String cookieName = openAMSettings.getSSOCookieName();
                for (final Cookie cookie : req.getCookies()) {
                    if (cookie.getName().equals(cookieName)) {
                        sessionId = cookie.getValue();
                    }
                }
            }
        }
        SSOToken ssoToken = null;
        if (sessionId != null) {
            try {
                ssoToken = SSOTokenManager.getInstance().createSSOToken(sessionId);
            } catch (SSOException e) {
                logger.message("Session Id is not valid");
            }
        }
        return ssoToken;
    }

    private ScriptObject getOIDCClaimsExtensionScript(String realm) throws ServerException {

        OpenAMSettingsImpl settings = new OpenAMSettingsImpl(OAuth2Constants.OAuth2ProviderService.NAME,
                OAuth2Constants.OAuth2ProviderService.VERSION);
        try {
            String scriptId = settings.getStringSetting(realm,
                    OAuth2Constants.OAuth2ProviderService.OIDC_CLAIMS_EXTENSION_SCRIPT);
            if (EMPTY_SCRIPT_SELECTION.equals(scriptId)) {
                return new ScriptObject("oidc-claims-script", "", SupportedScriptingLanguage.JAVASCRIPT);
            }
            ScriptConfiguration config = getScriptConfiguration(realm, scriptId);
            return new ScriptObject(config.getName(), config.getScript(), config.getLanguage());
        } catch (org.forgerock.openam.scripting.ScriptException | SSOException | SMSException e) {
            logger.message("Error running OIDC claims script", e);
            throw new ServerException("Error running OIDC claims script: " + e.getMessage());
        }
    }

    private ScriptConfiguration getScriptConfiguration(String realm, String scriptId)
            throws org.forgerock.openam.scripting.ScriptException {

        if (adminSubject == null) {
            adminSubject = SubjectUtils.createSuperAdminSubject();
        }
        return scriptingServiceFactory.create(adminSubject, realm).get(scriptId);
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Object> evaluateScope(AccessToken accessToken) {
        final Map<String, Object> map = new HashMap<String, Object>();
        final Set<String> scopes = accessToken.getScope();

        if (scopes.isEmpty()) {
            return map;
        }

        final String resourceOwner = accessToken.getResourceOwnerId();
        final String clientId = accessToken.getClientId();
        final String realm = accessToken.getRealm();

        AMIdentity id = null;
        try {
            if (clientId != null && CLIENT_CREDENTIALS.equals(accessToken.getGrantType()) ) {
                id = identityManager.getClientIdentity(clientId, realm);
            } else if (resourceOwner != null) {
                id = identityManager.getResourceOwnerIdentity(resourceOwner, realm);
            }
        } catch (Exception e) {
            logger.error("Unable to get user identity", e);
        }
        if (id != null) {
            for (String scope : scopes) {
                try {
                    Set<String> attributes = id.getAttribute(scope);
                    StringBuilder builder = new StringBuilder();
                    if (CollectionUtils.isNotEmpty(attributes)) {
                        Iterator<String> iter = attributes.iterator();
                        while (iter.hasNext()) {
                            builder.append(iter.next());
                            if (iter.hasNext()) {
                                builder.append(MULTI_ATTRIBUTE_SEPARATOR);
                            }
                        }
                    }
                    map.put(scope, builder.toString());
                } catch (Exception e) {
                    logger.error("Unable to get attribute", e);
                }
            }
        }

        return map;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, String> additionalDataToReturnFromAuthorizeEndpoint(Map<String, Token> tokens,
            OAuth2Request request) {
        return Collections.emptyMap();
    }

    /**
     * {@inheritDoc}
     */
    public void additionalDataToReturnFromTokenEndpoint(AccessToken accessToken, OAuth2Request request)
            throws ServerException, InvalidClientException, NotFoundException {
        final Set<String> scope = accessToken.getScope();
        if (scope != null && scope.contains(OPENID)) {
            final Map.Entry<String, String> tokenEntry = openIDTokenIssuer.issueToken(accessToken, request);
            if (tokenEntry != null) {
                accessToken.addExtraData(tokenEntry.getKey(), tokenEntry.getValue());
            }
        }
    }

    private String getUpdatedAt(String username, String realm, OAuth2Request request) throws NotFoundException {
        try {
            final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
            String modifyTimestampAttributeName;
            String createdTimestampAttributeName;
            try {
                modifyTimestampAttributeName = providerSettings.getModifiedTimestampAttributeName();
                createdTimestampAttributeName = providerSettings.getCreatedTimestampAttributeName();
            } catch (ServerException e) {
                logger.error("Unable to read last modified attribute from datastore", e);
                return DEFAULT_TIMESTAMP;
            }

            if (modifyTimestampAttributeName == null && createdTimestampAttributeName == null) {
                return null;
            }

            final AMHashMap timestamps = getTimestamps(username, realm, modifyTimestampAttributeName,
                    createdTimestampAttributeName);
            final String modifyTimestamp = CollectionHelper.getMapAttr(timestamps, modifyTimestampAttributeName);

            if (modifyTimestamp != null) {
                synchronized (TIMESTAMP_DATE_FORMAT) {
                    return Long.toString(TIMESTAMP_DATE_FORMAT.parse(modifyTimestamp).getTime() / 1000);
                }
            } else {
                final String createTimestamp = CollectionHelper.getMapAttr(timestamps, createdTimestampAttributeName);

                if (createTimestamp != null) {
                    synchronized (TIMESTAMP_DATE_FORMAT) {
                        return Long.toString(TIMESTAMP_DATE_FORMAT.parse(createTimestamp).getTime() / 1000);
                    }
                } else {
                    return DEFAULT_TIMESTAMP;
                }
            }
        } catch (IdRepoException e) {
            if (logger.errorEnabled()) {
                logger.error("ScopeValidatorImpl" +
                                ".getUpdatedAt: " +
                                "error searching Identities with username : " +
                                username,
                        e
                );
            }
        } catch (SSOException e) {
            logger.warning("Error getting updatedAt attribute",
                    e);
        } catch (ParseException e) {
            logger.warning("Error getting updatedAt attribute", e);
        }

        return null;
    }

    private AMHashMap getTimestamps(String username, String realm, String modifyTimestamp,
            String createTimestamp) throws IdRepoException,
            SSOException {
        final SSOToken token = AccessController.doPrivileged(AdminTokenAction.getInstance());
        final AMIdentityRepository amIdRepo = new AMIdentityRepository(token, realm);
        final IdSearchControl searchConfig = new IdSearchControl();
        searchConfig.setReturnAttributes(new HashSet<String>(Arrays.asList(modifyTimestamp, createTimestamp)));
        searchConfig.setMaxResults(0);
        final IdSearchResults searchResults = amIdRepo.searchIdentities(IdType.USER, username, searchConfig);

        final Iterator searchResultsItr = searchResults.getResultAttributes().values().iterator();

        if (searchResultsItr.hasNext()) {
            return (AMHashMap) searchResultsItr.next();
        } else {
            logger.warning("Error retrieving timestamps from datastore");
            throw new IdRepoException();
        }
    }
}

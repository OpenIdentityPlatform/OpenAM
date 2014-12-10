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

package org.forgerock.openam.oauth2;

import com.iplanet.am.sdk.AMHashMap;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.ScopeValidator;
import org.forgerock.oauth2.core.Token;
import org.forgerock.oauth2.core.Utils;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidScopeException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.forgerock.openidconnect.OpenIDTokenIssuer;

import javax.inject.Inject;
import javax.inject.Singleton;
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

import static org.forgerock.oauth2.core.OAuth2Constants.Params.*;
import static org.forgerock.oauth2.core.OAuth2Constants.TokenEndpoint.*;
import static org.forgerock.oauth2.core.OAuth2Constants.UrlLocation.*;

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
    private static Map<String, Object> scopeToUserUserProfileAttributes;
    private static final String DEFAULT_TIMESTAMP = "0";
    private static final DateFormat TIMESTAMP_DATE_FORMAT = new SimpleDateFormat("yyyyMMddhhmmss");
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;

    static {
        scopeToUserUserProfileAttributes = new HashMap<String, Object>();
        scopeToUserUserProfileAttributes.put("email","mail");
        scopeToUserUserProfileAttributes.put("address", "postaladdress");
        scopeToUserUserProfileAttributes.put("phone", "telephonenumber");

        Map<String, Object> profileSet = new HashMap<String, Object>();
        profileSet.put("name", "cn");
        profileSet.put("given_name", "givenname");
        profileSet.put("family_name", "sn");
        profileSet.put("locale", "preferredlocale");
        profileSet.put("zoneinfo", "preferredtimezone");

        scopeToUserUserProfileAttributes.put("profile", profileSet);
    }

    private final Debug logger = Debug.getInstance("OAuth2Provider");
    private final IdentityManager identityManager;
    private final OpenIDTokenIssuer openIDTokenIssuer;

    /**
     * Constructs a new OpenAMScopeValidator.
     *
     * @param identityManager An instance of the IdentityManager.
     * @param openIDTokenIssuer An instance of the OpenIDTokenIssuer.
     */
    @Inject
    public OpenAMScopeValidator(IdentityManager identityManager, OpenIDTokenIssuer openIDTokenIssuer,
            OAuth2ProviderSettingsFactory providerSettingsFactory) {
        this.identityManager = identityManager;
        this.openIDTokenIssuer = openIDTokenIssuer;
        this.providerSettingsFactory = providerSettingsFactory;
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
    public Map<String, Object> getUserInfo(AccessToken token, OAuth2Request request)
            throws UnauthorizedClientException {

        Set<String> scopes = token.getScope();
        Map<String,Object> response = new HashMap<String, Object>();
        AMIdentity id = identityManager.getResourceOwnerIdentity(token.getResourceOwnerId(),
                ((OpenAMAccessToken) token).getRealm());

        //add the subject identifier to the response
        response.put(OAuth2Constants.JWTTokenParams.SUB, token.getResourceOwnerId());
        response.put(OAuth2Constants.JWTTokenParams.UPDATED_AT, getUpdatedAt(token.getResourceOwnerId(),
                ((OpenAMAccessToken) token).getRealm(), request));
        for(String scope: scopes){

            if (OPENID.equals(scope)) {
                continue;
            }
            //get the attribute associated with the scope
            Object attributes = scopeToUserUserProfileAttributes.get(scope);
            if (attributes == null){
                logger.error("OpenAMScopeValidator.getUserInfo()::Invalid Scope in token scope=" + scope);
            } else if (attributes instanceof String){
                Set<String> attr = null;

                //if the attribute is a string get the attribute
                try {
                    attr = id.getAttribute((String)attributes);
                } catch (IdRepoException e) {
                    if (logger.warningEnabled()) {
                        logger.warning("OpenAMScopeValidator.getUserInfo(): Unable to retrieve attribute=" +
                                attributes, e);
                    }
                } catch (SSOException e) {
                    if (logger.warningEnabled()) {
                        logger.warning("OpenAMScopeValidator.getUserInfo(): Unable to retrieve attribute=" +
                                attributes, e);
                    }
                }

                //add a single object to the response.
                if (attr != null && attr.size() == 1){
                    response.put(scope, attr.iterator().next());
                } else if (attr != null && attr.size() > 1){ // add a set to the response
                    response.put(scope, attr);
                } else {
                    if (logger.warningEnabled()) {
                        //attr is null or attr is empty
                        logger.warning("OpenAMScopeValidator.getUserInfo(): Got an empty result for attribute=" +
                                attributes + " of scope=" + scope);
                    }
                }
            } else if (attributes instanceof Map){

                //the attribute is a collection of attributes
                //for example profile can be address, email, etc...
                if (attributes != null && !((Map<String,String>) attributes).isEmpty()){
                    for (Map.Entry<String, String> entry: ((Map<String, String>) attributes).entrySet()){
                        String attribute = null;
                        attribute = entry.getValue();
                        Set<String> attr = null;

                        //get the attribute
                        try {
                            attr = id.getAttribute(attribute);
                        } catch (IdRepoException e) {
                            if (logger.warningEnabled()) {
                                logger.warning("OpenAMScopeValidator.getUserInfo(): Unable to retrieve attribute=" +
                                        attributes, e);
                            }
                        } catch (SSOException e) {
                            if (logger.warningEnabled()) {
                                logger.warning("OpenAMScopeValidator.getUserInfo(): Unable to retrieve attribute=" +
                                        attributes, e);
                            }
                        }

                        //add the attribute value(s) to the response
                        if (attr != null && attr.size() == 1){
                            response.put(entry.getKey(), attr.iterator().next());
                        } else if (attr != null && attr.size() > 1){
                            response.put(entry.getKey(), attr);
                        } else {
                            if (logger.warningEnabled()) {
                                //attr is null or attr is empty
                                logger.warning("OpenAMScopeValidator.getUserInfo(): Got an empty result for scope=" +
                                        scope);
                            }
                        }
                    }
                }
            }
        }

        return response;
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
        final String realm = ((OpenAMAccessToken) accessToken).getRealm();
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
                    if (attributes != null || !attributes.isEmpty()) {
                        Iterator<String> iter = attributes.iterator();
                        StringBuilder builder = new StringBuilder();
                        while (iter.hasNext()) {
                            builder.append(iter.next());
                            if (iter.hasNext()) {
                                builder.append(MULTI_ATTRIBUTE_SEPARATOR);
                            }
                        }
                        map.put(scope, builder.toString());
                    }
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
            throws ServerException, InvalidClientException {
        final Set<String> scope = accessToken.getScope();
        if (scope != null && scope.contains(OPENID)) {
            final Map.Entry<String, String> tokenEntry = openIDTokenIssuer.issueToken(accessToken, request);
            if (tokenEntry != null) {
                accessToken.addExtraData(tokenEntry.getKey(), tokenEntry.getValue());
            }
        }
    }

    private String getUpdatedAt(String username, String realm, OAuth2Request request) {
        try {
            final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
            String modifyTimestampAttributeName = null;
            String createdTimestampAttributeName = null;
            try {
                modifyTimestampAttributeName = providerSettings.getModifiedTimestampAttributeName();
                createdTimestampAttributeName = providerSettings.getCreatedTimestampAttributeName();
            } catch (ServerException e) {
                logger.error("Unable to read last modified attribute from datastore",
                        e);
                return DEFAULT_TIMESTAMP;
            }

            final AMHashMap timestamps = getTimestamps(username, realm, modifyTimestampAttributeName,
                    createdTimestampAttributeName);
            final String modifyTimestamp = CollectionHelper.getMapAttr(timestamps, modifyTimestampAttributeName);

            if (modifyTimestamp != null) {
                return Long.toString(TIMESTAMP_DATE_FORMAT.parse(modifyTimestamp).getTime() / 1000);
            } else {
                final String createTimestamp = CollectionHelper.getMapAttr(timestamps, createdTimestampAttributeName);

                if (createTimestamp != null) {
                    return Long.toString(TIMESTAMP_DATE_FORMAT.parse(createTimestamp).getTime() / 1000);
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

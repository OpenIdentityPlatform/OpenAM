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
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.oauth2.rest;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.JsonValueFunctions.setOf;
import static org.forgerock.openam.cts.api.fields.OAuthTokenField.CLIENT_ID;
import static org.forgerock.openam.cts.api.fields.OAuthTokenField.EXPIRY_TIME;
import static org.forgerock.openam.cts.api.fields.OAuthTokenField.ID;
import static org.forgerock.openam.cts.api.fields.OAuthTokenField.REALM;
import static org.forgerock.openam.cts.api.fields.OAuthTokenField.SCOPE;
import static org.forgerock.openam.cts.api.fields.OAuthTokenField.TOKEN_NAME;
import static org.forgerock.openam.cts.api.fields.OAuthTokenField.USER_NAME;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DELETE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DELETE_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_500_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.OAUTH2_USER_APPLICATIONS;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.PATH_PARAM;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.QUERY;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.QUERY_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.TITLE;
import static org.forgerock.openam.oauth2.OAuth2Constants.Token.OAUTH_ACCESS_TOKEN;
import static org.forgerock.openam.oauth2.OAuth2Constants.Token.OAUTH_REFRESH_TOKEN;
import static org.forgerock.util.query.QueryFilter.and;
import static org.forgerock.util.query.QueryFilter.equalTo;
import static org.forgerock.util.query.QueryFilter.or;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.forgerock.api.annotations.ApiError;
import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Delete;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Parameter;
import org.forgerock.api.annotations.Query;
import org.forgerock.api.annotations.RequestHandler;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.enums.QueryType;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.rest.resource.ContextHelper;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.forgerock.util.query.QueryFilter;
import org.joda.time.format.ISODateTimeFormat;

import com.sun.identity.common.ISLocaleContext;
import com.sun.identity.shared.debug.Debug;

/**
 * A request handler for inspecting and revoking OAuth2 applications. It requires a user and a realm to be available
 * in the request context.
 *
 * @since 13.0.0
 */
@CollectionProvider(details = @Handler(
                mvccSupported = false,
                title = OAUTH2_USER_APPLICATIONS + TITLE,
                description = OAUTH2_USER_APPLICATIONS + DESCRIPTION,
                resourceSchema = @Schema(schemaResource = "OAuth2UserApplications.resource.schema.json"),
                parameters = @Parameter(name = "user", type = "string", description = OAUTH2_USER_APPLICATIONS
                        + PATH_PARAM + "user")),
        pathParam = @Parameter(
                name = "clientId",
                type = "string",
                description = OAUTH2_USER_APPLICATIONS + PATH_PARAM + "clientid"))
public class OAuth2UserApplications {

    private final TokenStore tokenStore;
    private final OAuth2ProviderSettingsFactory oAuth2ProviderSettingsFactory;
    private final ClientRegistrationStore clientRegistrationStore;
    private final ContextHelper contextHelper;
    private final Debug debug;

    @Inject
    public OAuth2UserApplications(TokenStore tokenStore,
            OAuth2ProviderSettingsFactory oAuth2ProviderSettingsFactory,
            ClientRegistrationStore clientRegistrationStore, ContextHelper contextHelper,
            @Named("frRest") Debug debug) {
        this.tokenStore = tokenStore;
        this.oAuth2ProviderSettingsFactory = oAuth2ProviderSettingsFactory;
        this.clientRegistrationStore = clientRegistrationStore;
        this.contextHelper = contextHelper;
        this.debug = debug;
    }

    /**
     * Allows users to query OAuth2 applications that they have given their consent access to and that have active
     * access and/or refresh tokens.
     *
     * <p>Applications consist of an id, a name (the client id), a set of scopes and an expiry time. The scopes field
     * is the union of the scopes of the individual access/refresh tokens. The expiry time is the time when the last
     * access/refresh token will expire, or null if the server is configured to allow tokens to be refreshed
     * indefinitely.</p>
     *
     * @param context The request context.
     * @param queryHandler The query handler.
     * @param request Unused but necessary for used of the {@link @Query} annotation.
     * @return A promise of a query response.
     */
    @Query(operationDescription = @Operation(description = OAUTH2_USER_APPLICATIONS + QUERY_DESCRIPTION,
                    errors = @ApiError(
                            code = 500,
                            description = OAUTH2_USER_APPLICATIONS + QUERY + ERROR_500_DESCRIPTION)),
            type = QueryType.FILTER, queryableFields = {})
    public Promise<QueryResponse, ResourceException> query(Context context, QueryResourceHandler queryHandler,
            QueryRequest request) {

        String userId = contextHelper.getUserId(context);
        String realm = contextHelper.getRealm(context);

        try {
            QueryFilter<CoreTokenField> queryFilter = getQueryFilter(userId, realm);

            JsonValue tokens = tokenStore.queryForToken(realm, queryFilter);

            Map<String, Set<JsonValue>> applicationTokensMap = new HashMap<>();

            for (JsonValue token : tokens) {
                String clientId = getAttributeValue(token, CLIENT_ID.getOAuthField());
                realm = getAttributeValue(token, REALM.getOAuthField());
                if(tokenClientExists(clientId, realm, context)) {
                    Set<JsonValue> applicationTokens = applicationTokensMap.get(clientId);
                    if (applicationTokens == null) {
                        applicationTokens = new HashSet<>();
                        applicationTokensMap.put(clientId, applicationTokens);
                    }
                    applicationTokens.add(token);
                }
            }

            for (Map.Entry<String, Set<JsonValue>> applicationTokens : applicationTokensMap.entrySet()) {
                ResourceResponse resource = getResourceResponse(context, applicationTokens.getKey(),
                        applicationTokens.getValue());
                queryHandler.handleResource(resource);
            }

            return Promises.newResultPromise(Responses.newQueryResponse());
        } catch (ServerException | InvalidClientException |
                NotFoundException e) {
            debug.message("Failed to query OAuth2 clients for user {}", userId, e);
            return new InternalServerErrorException(e).asPromise();
        }
    }

    /**
     * Checks if the client to which the access token is issued exists
     */
    private boolean tokenClientExists(String clientId, String relam, Context context) {
        try {
            clientRegistrationStore.get(clientId, relam, context);
        } catch (InvalidClientException | NotFoundException e) {
            return false;
        }
        return true;
    }

    /**
     * Allows users to revoke an OAuth2 application. This will remove their consent and revoke any access and refresh
     * tokens with a matching client id.
     * @param context The request context.
     * @param resourceId The id of the OAuth2 client.
     * @return A promise of the removed application.
     */
    @Delete(operationDescription = @Operation(
            description = OAUTH2_USER_APPLICATIONS + DELETE_DESCRIPTION,
            errors = @ApiError(code = 500, description = OAUTH2_USER_APPLICATIONS + DELETE + ERROR_500_DESCRIPTION)))
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context context, String resourceId) {
        String userId = contextHelper.getUserId(context);
        String realm = contextHelper.getRealm(context);

        debug.message("Revoking access to OAuth2 client {} for user {}", resourceId, userId);

        try {
            oAuth2ProviderSettingsFactory.get(context).revokeConsent(userId, resourceId);

            QueryFilter<CoreTokenField> queryFilter = and(
                    getQueryFilter(userId, realm),
                    equalTo(CLIENT_ID.getField(), resourceId)
            );

            JsonValue tokens = tokenStore.queryForToken(realm, queryFilter);

            if (tokens.asCollection().isEmpty()) {
                return new org.forgerock.json.resource.NotFoundException().asPromise();
            }

            for (JsonValue token : tokens) {
                String tokenId = getAttributeValue(token, ID.getOAuthField());
                debug.message("Removing OAuth2 token {} with client {} for user {}", tokenId, resourceId, userId);
                tokenStore.delete(realm, tokenId);
            }

            return getResourceResponse(context, resourceId, tokens).asPromise();
        } catch (InvalidClientException | NotFoundException | ServerException e) {
            debug.message("Failed to revoke access to OAuth2 client {} for user {}", resourceId, userId, e);
            return new InternalServerErrorException(e).asPromise();
        }
    }

    private ResourceResponse getResourceResponse(Context context, String clientId, Iterable<JsonValue> tokens)
            throws NotFoundException, InvalidClientException, ServerException {
        String realm = getAttributeValue(tokens.iterator().next(), REALM.getOAuthField());
        OAuth2ProviderSettings oAuth2ProviderSettings = oAuth2ProviderSettingsFactory.get(context);

        ClientRegistration clientRegistration = clientRegistrationStore.get(clientId, realm, context);
        Map<String, String> scopeDescriptions = clientRegistration.getScopeDescriptions(getLocale(context));
        Map<String, String> scopes = new HashMap<>();
        for (JsonValue token : tokens) {
            for (String scope : getAttributeValueSet(token, SCOPE.getOAuthField())) {
                if (scopeDescriptions.containsKey(scope)) {
                    scopes.put(scope, scopeDescriptions.get(scope));
                } else {
                    scopes.put(scope, scope);
                }
            }
        }

        String displayName = clientRegistration.getDisplayName(getLocale(context));
        String expiryDateTime = calculateExpiryDateTime(tokens, oAuth2ProviderSettings);

        JsonValue content = json(object(
                field("_id", clientId),
                field("name", displayName),
                field("scopes", scopes),
                field("expiryDateTime", expiryDateTime)
        ));

        return Responses.newResourceResponse(clientId, String.valueOf(content.getObject().hashCode()), content);
    }

    private String calculateExpiryDateTime(Iterable<JsonValue> tokens, OAuth2ProviderSettings oAuth2ProviderSettings)
            throws ServerException {
        long maxExpiryMilliseconds = 0L;

        for (JsonValue token : tokens) {
            long tokenExpiryMilliseconds = Long.parseLong(getAttributeValue(token, EXPIRY_TIME.getOAuthField()));

            if (tokenExpiryMilliseconds == -1) {
                return null;
            }

            if (OAUTH_REFRESH_TOKEN.equals(getAttributeValue(token, TOKEN_NAME.getOAuthField()))) {
                if (oAuth2ProviderSettings.issueRefreshTokensOnRefreshingToken()) {
                    return null;
                }
                tokenExpiryMilliseconds += oAuth2ProviderSettings.getAccessTokenLifetime() * 1000;
            }

            if (tokenExpiryMilliseconds > maxExpiryMilliseconds) {
                maxExpiryMilliseconds = tokenExpiryMilliseconds;
            }
        }

        return ISODateTimeFormat.dateTime().print(maxExpiryMilliseconds);
    }

    private String getAttributeValue(JsonValue token, String attributeName) {
        String value;
        JsonValue jsonValue = token.get(attributeName);
        if (jsonValue.isString()) {
            value = jsonValue.asString();
        } else if (jsonValue.isCollection()) {
            value = CollectionUtils.getFirstItem(jsonValue.asCollection(String.class));
        } else {
            value = jsonValue.toString();
        }
        return value;
    }

    private Set<String> getAttributeValueSet(JsonValue token, String attributeName) {
        Set<String> value = new HashSet<>();
        JsonValue jsonValue = token.get(attributeName);
        if (jsonValue.isString()) {
            value.add(jsonValue.asString());
        } else if (jsonValue.isCollection()) {
            value = jsonValue.as(setOf(String.class));
        }
        return value;
    }

    private Locale getLocale(Context context) {
        ISLocaleContext locale = new ISLocaleContext();
        HttpContext httpContext = context.asContext(HttpContext.class);
        locale.setLocale(httpContext);

        return locale.getLocale();
    }

    private QueryFilter<CoreTokenField> getQueryFilter(String userId, String realm) {
        return and(
                equalTo(USER_NAME.getField(), userId),
                equalTo(REALM.getField(), realm),
                or(
                        equalTo(TOKEN_NAME.getField(), OAUTH_ACCESS_TOKEN),
                        equalTo(TOKEN_NAME.getField(), OAUTH_REFRESH_TOKEN)
                )
        );
    }
}

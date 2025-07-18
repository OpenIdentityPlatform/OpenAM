/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012-2016 ForgeRock AS.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions copyright [year] [name of copyright owner]"
 *
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.oauth2.rest;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.*;
import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.*;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.GRANT_TYPE;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.REALM;
import static org.forgerock.openam.oauth2.OAuth2Constants.Token.OAUTH_ACCESS_TOKEN;
import static org.forgerock.openam.oauth2.OAuth2Constants.TokenEndpoint.CLIENT_CREDENTIALS;
import static org.forgerock.openam.utils.Time.*;
import static org.forgerock.util.promise.Promises.newResultPromise;

import com.sun.identity.common.ISLocaleContext;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.sm.SMSException;
import java.util.ResourceBundle;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.net.HttpURLConnection;
import java.security.AccessController;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.openam.oauth2.OAuthTokenStore;
import org.forgerock.openam.utils.OpenAMSettings;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PermanentException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.forgerock.openam.cts.api.fields.OAuthTokenField;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.oauth2.IdentityManager;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openidconnect.Client;
import org.forgerock.openidconnect.ClientDAO;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;
import org.restlet.Request;


/**
 * This end point is deprecated as this does not support stateless token check.
 *
 * @deprecated
 * To request access tokens use @{@link org.forgerock.oauth2.restlet.TokenEndpointResource} endpoint
 * To revoke tokens use {@link TokenRevocationResource} endpoint
 *
 */
@Deprecated
public class TokenResource implements CollectionResourceProvider {

    public static final String EXPIRE_TIME_KEY = "expireTime";
    public static final CoreTokenField USERNAME_FIELD = OAuthTokenField.USER_NAME.getField();
    public static final CoreTokenField REALM_FIELD = OAuthTokenField.REALM.getField();
    public static final String INDEFINITELY = "Indefinitely";
    public static final String INDEFINITE_TOKEN_STRING_PROPERTY_NAME = "indefiniteTokenString";
    private static final String[] RESOURCE_OWNER_HIDDEN_FIELDS = new String[] {
            OAuth2Constants.CoreTokenParams.ID,
            OAuth2Constants.CoreTokenParams.PARENT,
            OAuth2Constants.CoreTokenParams.AUDIT_TRACKING_ID,
            OAuth2Constants.CoreTokenParams.AUTH_GRANT_ID,
            OAuth2Constants.CoreTokenParams.REFRESH_TOKEN,
    };
    private final ClientDAO clientDao;

    private final OAuthTokenStore tokenStore;
    private final OAuth2ProviderSettingsFactory oAuth2ProviderSettingsFactory;
    private final Debug debug;
    private static SSOToken token = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
    private static String adminUser = SystemProperties.get(Constants.AUTHENTICATION_SUPER_USER);
    private static AMIdentity adminUserId = null;
    private final OpenAMSettings authServiceSettings;

    static {
        if (adminUser != null) {
            adminUserId = new AMIdentity(token,
                    adminUser, IdType.USER, "/", null);
        }
    }

    private final IdentityManager identityManager;

    @Inject
    public TokenResource(OAuthTokenStore tokenStore, ClientDAO clientDao, IdentityManager identityManager,
            OAuth2ProviderSettingsFactory oAuth2ProviderSettingsFactory, OpenAMSettings authServiceSettings,
            @Named("frRest") Debug debug) {
        this.tokenStore = tokenStore;
        this.clientDao = clientDao;
        this.identityManager = identityManager;
        this.oAuth2ProviderSettingsFactory = oAuth2ProviderSettingsFactory;
        this.authServiceSettings = authServiceSettings;
        this.debug = debug;
    }

    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(Context context,
            CreateRequest createRequest) {
        return RestUtils.generateUnsupportedOperation();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String resourceId,
            ReadRequest request) {
        return readToken(context, resourceId);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, String resourceId,
            UpdateRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(final Context context, final String resourceId,
            DeleteRequest request) {
        return readToken(context, resourceId)
                .thenAsync(new AsyncFunction<ResourceResponse, ResourceResponse, ResourceException>() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> apply(final ResourceResponse resourceResponse)
                            throws ResourceException {
                        return deleteToken(context, resourceId, false)
                                .thenAsync(new AsyncFunction<Void, ResourceResponse, ResourceException>() {
                                    @Override
                                    public Promise<ResourceResponse, ResourceException> apply(Void value) {
                                        return newResultPromise(resourceResponse);
                                    }
                                });
                    }
                });
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, String resourceId,
            PatchRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(Context context,
            ActionRequest actionRequest) {
        return RestUtils.generateUnsupportedOperation();
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, String resourceId,
            ActionRequest request) {

        String actionId = request.getAction();

        if ("revoke".equalsIgnoreCase(actionId)) {
            return deleteToken(context, resourceId, true)
                    .thenAsync(new AsyncFunction<Void, ActionResponse, ResourceException>() {
                        @Override
                        public Promise<ActionResponse, ResourceException> apply(Void value) {
                            return newResultPromise(newActionResponse((json(object()))));
                        }
                    });
        } else if ("revokeTokens".equalsIgnoreCase(actionId)) {
            return revokeTokens(resourceId, request)
                    .thenAsync(new AsyncFunction<Void, ActionResponse, ResourceException>() {
                        @Override
                        public Promise<ActionResponse, ResourceException> apply(Void value) {
                            return newResultPromise(newActionResponse((json(object()))));
                        }
                    });
        } else {
            if (debug.errorEnabled()) {
                debug.error("TokenResource :: ACTION : Unsupported action request performed, " + actionId + " on " +
                        resourceId);
            }
            return RestUtils.generateUnsupportedOperation();
        }
    }

    /**
     * Deletes the token with the provided token id.
     *
     * @param context The context.
     * @param tokenId The token id.
     * @param deleteRefreshToken Whether to delete associated refresh token, if token id is for an access token.
     * @return {@code Void} if the token has been deleted.
     */
    private Promise<Void, ResourceException> deleteToken(Context context, String tokenId, boolean deleteRefreshToken) {
        try {
            AMIdentity uid = getUid(context);

            JsonValue token = getToken(tokenId);
            String username = getAttributeValue(token, USERNAME);
            if (username == null || username.isEmpty()) {
                if (debug.errorEnabled()) {
                    debug.error("TokenResource :: DELETE : No username associated with " +
                            "token with ID, " + tokenId + ".");
                }
                throw new PermanentException(HttpURLConnection.HTTP_NOT_FOUND, "Not Found", null);
            }

            String grantType = getAttributeValue(token, GRANT_TYPE);

            if (grantType != null && grantType.equalsIgnoreCase(CLIENT_CREDENTIALS)) {
                if (deleteRefreshToken) {
                    deleteAccessTokensRefreshToken(token);
                }
                tokenStore.delete(tokenId);
            } else {
                String realm = getAttributeValue(token, REALM);
                AMIdentity uid2 = identityManager.getResourceOwnerIdentity(username, realm);
                if (uid.equals(uid2) || uid.equals(adminUserId)) {
                    if (deleteRefreshToken) {
                        deleteAccessTokensRefreshToken(token);
                    }
                    tokenStore.delete(tokenId);
                } else {
                    if (debug.errorEnabled()) {
                        debug.error("TokenResource :: DELETE : Only the resource owner or an administrator may perform "
                        + "a delete on the token with ID, " + tokenId + ".");
                    }
                    throw new PermanentException(401, "Unauthorized", null);
                }
            }

            return newResultPromise(null);

        } catch (CoreTokenException e) {
            return new ServiceUnavailableException(e.getMessage(), e).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (SSOException e) {
            debug.error("TokenResource :: DELETE : Unable to retrieve identity of the requesting user. Unauthorized.");
            return new PermanentException(401, "Unauthorized", e).asPromise();
        } catch (IdRepoException e) {
            debug.error("TokenResource :: DELETE : Unable to retrieve identity of the requesting user. Unauthorized.");
            return new PermanentException(401, "Unauthorized", e).asPromise();
        } catch (UnauthorizedClientException e) {
            debug.error("TokenResource :: DELETE : Requesting user is unauthorized.");
            return new PermanentException(401, "Unauthorized", e).asPromise();
        }
    }

    /**
     * Deletes an access token and its associated refresh token or just the provided refresh token.
     * @param tokenId The token id.
     * @param request The action request.
     * @return {@code true} if the token has been deleted.
     */
    private Promise<Void, ResourceException> revokeTokens(final String tokenId, ActionRequest request) {
        try {
            JsonValue token = getToken(tokenId);

            String username = getAttributeValue(token, USERNAME);
            if (StringUtils.isEmpty(username)) {
                debug.error("TokenResource :: revokeTokens : No username associated with " +
                        "token with ID, " + tokenId + ".");
                throw new NotFoundException("Not Found", null);
            }

            final JsonValue jVal = request.getContent();
            final String clientIdParameter = jVal.get(OAuth2Constants.Params.CLIENT_ID).asString();
            if (StringUtils.isEmpty(clientIdParameter)) {
                debug.error("TokenResource :: revokeTokens : No clientId provided");
                throw new BadRequestException("Missing clientId", null);
            }

            final String clientId = getAttributeValue(token, OAuthTokenField.CLIENT_ID.getOAuthField());
            if (!clientId.equalsIgnoreCase(clientIdParameter)) {
                debug.error("TokenResource :: revokeTokens : clientIds do not match");
                throw new ForbiddenException("Unauthorized", null);
            } else {
                deleteAccessTokensRefreshToken(token);
                tokenStore.delete(tokenId);
            }

            return newResultPromise(null);
        } catch (CoreTokenException e) {
            return new ServiceUnavailableException(e.getMessage(), e).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    private JsonValue getToken(String tokenId) throws CoreTokenException, NotFoundException {
        JsonValue token = tokenStore.read(tokenId);
        if (token == null) {
            debug.error("TokenResource :: No token with ID, " + tokenId + " found");
            throw new NotFoundException("Token Not Found", null);
        }
        return token;
    }

    /**
     * Deletes the provided access token's refresh token.
     *
     * @param token The access token.
     * @throws CoreTokenException If there was a problem deleting the refresh token.
     */
    private void deleteAccessTokensRefreshToken(JsonValue token) throws CoreTokenException {
        if (OAUTH_ACCESS_TOKEN.equals(getAttributeValue(token, TOKEN_NAME))) {
            String refreshTokenId = getAttributeValue(token, REFRESH_TOKEN);
            if (refreshTokenId != null) {
                tokenStore.delete(refreshTokenId);
            }
        }
    }

    /**
     * Gets the value of the named attribute from the provided token.
     *
     * @param token The token.
     * @param attributeName The attribute name.
     * @return The attribute value.
     */
    private String getAttributeValue(JsonValue token, String attributeName) {
        final Collection<String> value = getAttributeAsSet(token, attributeName);
        if (value != null && !value.isEmpty()) {
            return value.iterator().next();
        }
        return null;
    }

    /**
     * Gets the {@code Set<String>} of values for the given attributeName.
     *
     * @param value The {@code JsonValue}.
     * @param attributeName The attribute name.
     * @return The attribute set.
     */
    @SuppressWarnings("unchecked")
    private Collection<String> getAttributeAsSet(JsonValue value, String attributeName) {
        final JsonValue param = value.get(attributeName);
        if (param != null) {
            return param.asCollection(String.class);
        }
        return null;
    }

    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest queryRequest,
            QueryResourceHandler handler) {
        try {
            JsonValue response;
            Collection<QueryFilter<CoreTokenField>> query = new ArrayList<QueryFilter<CoreTokenField>>();

            //get uid of submitter
            AMIdentity uid;
            try {
                uid = getUid(context);
                if (!uid.equals(adminUserId)) {
                    query.add(QueryFilter.equalTo(USERNAME_FIELD, uid.getName()));
                    query.add(QueryFilter.equalTo(REALM_FIELD, DNMapper.orgNameToRealmName(uid.getRealm())));
                }
            } catch (Exception e) {
                if (debug.errorEnabled()) {
                    debug.error("TokenResource :: QUERY : Unable to query collection as no UID discovered " +
                            "for requesting user.");
                }
                return new PermanentException(401, "Unauthorized", e).asPromise();
            }

            String id = queryRequest.getQueryId();
            String queryString;

            if (id.equals("access_token")) {
                queryString = "tokenName=access_token";
            } else {
                queryString = id;
            }

            String[] constraints = queryString.split(",");
            boolean userNamePresent = false;
            for (String constraint : constraints) {
                String[] params = constraint.split("=");
                if (params.length == 2) {
                    if (OAuthTokenField.USER_NAME.getOAuthField().equals(params[0])) {
                        userNamePresent = true;
                    }
                    query.add(QueryFilter.equalTo(getOAuth2TokenField(params[0]), params[1]));
                }
            }

            if (adminUserId.equals(uid)) {
                if (!userNamePresent) {
                    return new BadRequestException("userName field MUST be set in _queryId").asPromise();
                }
            } else if (userNamePresent) {
                return new BadRequestException("userName field MUST NOT be set in _queryId").asPromise();
            }
            response = tokenStore.query(QueryFilter.and(query));
            return handleResponse(handler, response, context, uid);

        } catch (UnauthorizedClientException e) {
            debug.error("TokenResource :: QUERY : Unable to query collection as the client is not authorized.", e);
            return new PermanentException(401, e.getMessage(), e).asPromise();
        } catch (CoreTokenException e) {
            debug.error("TokenResource :: QUERY : Unable to query collection as the token store is not available.", e);
            return new ServiceUnavailableException(e.getMessage(), e).asPromise();
        } catch (InternalServerErrorException e) {
            debug.error("TokenResource :: QUERY : Unable to query collection as writing the response failed.", e);
            return e.asPromise();
        } catch (NotFoundException e) {
            debug.error("TokenResource :: QUERY : Unable to query collection as realm does not have OAuth 2 provider.", e);
            return e.asPromise();
        }
    }

    private CoreTokenField getOAuth2TokenField(String fieldname) {
        for (OAuthTokenField field : OAuthTokenField.values()) {
            if (field.getOAuthField().equals(fieldname)) {
                return field.getField();
            }
        }
        throw new IllegalArgumentException("I don't understand the OAuth 2.0 field called " + fieldname);
    }

    private Promise<QueryResponse, ResourceException> handleResponse(QueryResourceHandler handler, JsonValue response,
            Context context, AMIdentity uid)
            throws UnauthorizedClientException, CoreTokenException, InternalServerErrorException, NotFoundException {
        ResourceResponse resource = newResourceResponse("result", "1", response);
        JsonValue value = resource.getContent();
        String acceptLanguage = context.asContext(HttpContext.class).getHeaderAsString("accept-language");

        for (JsonValue val : value) {
            Client client = getClient(val);

            val.put(EXPIRE_TIME_KEY, getExpiryDate(val, context));
            val.put(OAuth2Constants.ShortClientAttributeNames.DISPLAY_NAME.getType(), getClientName(client));
            val.put(OAuth2Constants.ShortClientAttributeNames.SCOPES.getType(), getScopes(client, val,
                    acceptLanguage));

            handler.handleResource(resource(val, uid));
        }
        return newResultPromise(newQueryResponse());
    }

    private String getClientName(Client client) throws UnauthorizedClientException {
        return client.get(OAuth2Constants.ShortClientAttributeNames.DISPLAY_NAME.getType()).get(0).asString();
    }

    private String getScopes(Client client, JsonValue entry, String acceptLanguage) throws UnauthorizedClientException {
        JsonValue allScopes = client.get(OAuth2Constants.ShortClientAttributeNames.SCOPES.getType());
        Collection<String> allowedScopes = getAttributeAsSet(entry, "scope");

        java.util.Locale locale = Locale.getLocaleObjFromAcceptLangHeader(acceptLanguage);

        List<String> displayNames = new ArrayList<String>();
        for (String allowedScope : allowedScopes) {
            displayNames.add(getDisplayName(allowedScope, allScopes, locale));
        }

        return StringUtils.join(displayNames, ",");
    }

    private String getDisplayName(String allowedScope, JsonValue allScopes, java.util.Locale serverLocale) {
        final String delimiter = "|";
        String defaultDisplayName = null;

        for (JsonValue scope : allScopes) {
            if (scope.asString().contains(delimiter)) {
                String[] values = scope.asString().split("\\" + delimiter);
                if (values.length == 3) {
                    String name = values[0];
                    String language = values[1];
                    String displayName = values[2];
                    java.util.Locale currentLocale = Locale.getLocale(language);

                    final String currentLanguage = currentLocale.getLanguage();
                    if (currentLanguage.equalsIgnoreCase("en")) {
                        defaultDisplayName = displayName;
                    }

                    if (serverLocale.getLanguage().equals(currentLanguage) && name.equals(allowedScope)) {
                        return displayName;
                    }
                }
            }
        }

        if (defaultDisplayName != null) {
            return defaultDisplayName;
        }
        
        return allowedScope;
    }

    private Client getClient(JsonValue entry) throws UnauthorizedClientException {
        final String clientId = getAttributeValue(entry, "clientID");
        final String realm = getAttributeValue(entry, "realm");

        return clientDao.read(clientId, getRequest(realm));
    }

    private OAuth2Request getRequest(final String realm) {
        return OAuth2Request.forRealm(realm);
    }

    private String getExpiryDate(JsonValue token, Context context) throws CoreTokenException,
            InternalServerErrorException, NotFoundException {

        OAuth2ProviderSettings oAuth2ProviderSettings;
        final String realm = getAttributeValue(token, "realm");
        try {
            oAuth2ProviderSettings = oAuth2ProviderSettingsFactory.getRealmProviderSettings(realm);
        } catch (org.forgerock.oauth2.core.exceptions.NotFoundException e) {
            throw new NotFoundException(e.getMessage());
        }

        try {

            boolean isRefreshTokenDefined = token.isDefined(OAuth2Constants.CoreTokenParams.REFRESH_TOKEN);
            if (isRefreshTokenDefined && oAuth2ProviderSettings.issueRefreshTokensOnRefreshingToken()) {
                return getIndefinitelyString(context);
            }

            JsonValue refreshToken = tokenStore.read(getAttributeValue(token,
                                                                       OAuth2Constants.CoreTokenParams.REFRESH_TOKEN));
            if (isRefreshTokenDefined && refreshToken != null) {
                //Use refresh token expiry
                long expiryTimeInMilliseconds = Long.parseLong(getAttributeValue(refreshToken, EXPIRE_TIME_KEY));

                if (expiryTimeInMilliseconds == -1) {
                    return getIndefinitelyString(context);
                }

                return getDateFormat(context).format(new Date(expiryTimeInMilliseconds));
            } else {
                //Use access token expiry
                long expiryTimeInMilliseconds = Long.parseLong(getAttributeValue(token, EXPIRE_TIME_KEY));
                return getDateFormat(context).format(new Date(expiryTimeInMilliseconds));
            }
        } catch (ServerException | SMSException | SSOException e) {
            throw new InternalServerErrorException(e);
        }
    }

    private DateFormat getDateFormat(Context context) throws SSOException, SMSException {
        return (new SimpleDateFormat()).getDateTimeInstance(DateFormat.MEDIUM,
                        DateFormat.SHORT, getLocale(context));
    }

    private Promise<ResourceResponse, ResourceException> readToken(Context context, String resourceId) {
        try {
            AMIdentity uid = getUid(context);

            JsonValue response;
            try {
                response = tokenStore.read(resourceId);
            } catch (CoreTokenException e) {
                if (debug.errorEnabled()) {
                    debug.error("TokenResource :: READ : No token found with ID, " + resourceId);
                }
                throw new NotFoundException("Could not find valid token with given ID", e);
            }
            if (response == null) {
                if (debug.errorEnabled()) {
                    debug.error("TokenResource :: READ : No token found with ID, " + resourceId);
                }
                throw new NotFoundException("Could not find valid token with given ID");
            }

            JsonValue expireTimeValue = response.get(OAuth2Constants.CoreTokenParams.EXPIRE_TIME);
            long expireTime;
            if (expireTimeValue.isNumber()) {
                expireTime = expireTimeValue.asLong();
            } else {
                Collection<String> expireTimeSet = expireTimeValue.asCollection(String.class);
                expireTime = Long.parseLong(expireTimeSet.iterator().next());
            }

            if (currentTimeMillis() > expireTime) {
                throw new NotFoundException("Could not find valid token with given ID");
            }

            String grantType = getAttributeValue(response, GRANT_TYPE);

            if (grantType != null && grantType.equalsIgnoreCase(OAuth2Constants.TokenEndpoint.CLIENT_CREDENTIALS)) {
                return newResultPromise(resource(response, uid));
            } else {
                String realm = getAttributeValue(response, REALM);

                String username = getAttributeValue(response, USERNAME);
                if (username == null || username.isEmpty()) {
                    if (debug.errorEnabled()) {
                        debug.error("TokenResource :: READ : No token found with ID, " + resourceId);
                    }
                    throw new NotFoundException("Could not find valid token with given ID");
                }
                AMIdentity uid2 = identityManager.getResourceOwnerIdentity(username, realm);
                if (uid.equals(adminUserId) || uid.equals(uid2)) {
                    return newResultPromise(resource(response, uid));
                } else {
                    if (debug.errorEnabled()) {
                        debug.error("TokenResource :: READ : Only the resource owner or an administrator may perform "
                                + "a read on the token with ID, " + resourceId + ".");
                    }
                    throw new PermanentException(401, "Unauthorized", null);
                }
            }
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (SSOException e) {
            debug.error("TokenResource :: READ : Unable to query collection as the IdRepo " +
                    "failed to return a valid user.", e);
            return new PermanentException(401, "Unauthorized", e).asPromise();
        } catch (IdRepoException e) {
            debug.error("TokenResource :: READ : Unable to query collection as the IdRepo " +
                    "failed to return a valid user.", e);
            return new PermanentException(401, "Unauthorized", e).asPromise();
        } catch (UnauthorizedClientException e) {
            debug.error("TokenResource :: READ : Unable to query collection as the client is not authorized.", e);
            return new PermanentException(401, "Unauthorized", e).asPromise();
        }
    }

    private ResourceResponse resource(JsonValue response, AMIdentity uid) {
        String tokenId = response.get(OAuth2Constants.CoreTokenParams.ID).asList(String.class).get(0);
        if (!adminUserId.equals(uid)) {
            tokenId = null;
            for (String field : RESOURCE_OWNER_HIDDEN_FIELDS) {
                response.remove(field);
            }
        }
        return newResourceResponse(tokenId, String.valueOf(response.getObject().hashCode()),
                response);
    }

    /**
     * Returns TokenID from headers
     *
     * @param context Context which contains the headers.
     * @return String with TokenID
     */
    private String getCookieFromServerContext(Context context) {
        return RestUtils.getCookieFromServerContext(context);
    }

    private AMIdentity getUid(Context context) throws SSOException, IdRepoException, UnauthorizedClientException {
        String cookie = getCookieFromServerContext(context);
        SSOTokenManager mgr = SSOTokenManager.getInstance();
        SSOToken token = mgr.createSSOToken(cookie);
        return identityManager.getResourceOwnerIdentity(token.getProperty("UserToken"), token.getProperty("Organization"));
    }

    public String getIndefinitelyString(Context context) {
        try {
            return ResourceBundle.getBundle("TokenResource",
                    getLocale(context)).getString(
                    INDEFINITE_TOKEN_STRING_PROPERTY_NAME);
        } catch (SSOException | SMSException e) {
            debug.error("Error retrieving resource bundle: TokenResource");
        }
        return INDEFINITELY;
    }

    private java.util.Locale getLocale(Context context) throws SSOException, SMSException {
        ISLocaleContext locale = new ISLocaleContext();
        HttpContext httpContext = context.asContext(HttpContext.class);
        locale.setLocale(httpContext);

        return locale.getLocale();
    }
}

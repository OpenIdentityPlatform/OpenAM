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
 */

package org.forgerock.openam.oauth2.rest;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.*;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.*;
import static org.forgerock.oauth2.core.OAuth2Constants.Params.GRANT_TYPE;
import static org.forgerock.oauth2.core.OAuth2Constants.Params.REALM;
import static org.forgerock.oauth2.core.OAuth2Constants.Token.OAUTH_ACCESS_TOKEN;
import static org.forgerock.oauth2.core.OAuth2Constants.TokenEndpoint.CLIENT_CREDENTIALS;
import static org.forgerock.openam.utils.Time.*;
import static org.forgerock.util.promise.Promises.newResultPromise;

import com.sun.identity.common.ISLocaleContext;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.sm.SMSException;
import java.util.ResourceBundle;
import javax.inject.Inject;
import javax.inject.Named;
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
import org.apache.commons.lang.StringUtils;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.openam.core.RealmInfo;
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
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.forgerock.openam.cts.api.fields.OAuthTokenField;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.oauth2.IdentityManager;
import org.forgerock.openam.oauth2.OAuthTokenStore;
import org.forgerock.openam.oauth2.OpenAMOAuth2ProviderSettingsFactory;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.utils.OpenAMSettings;
import org.forgerock.openidconnect.Client;
import org.forgerock.openidconnect.ClientDAO;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;

public class TokenResource implements CollectionResourceProvider {

    public static final String EXPIRE_TIME_KEY = "expireTime";
    public static final CoreTokenField USERNAME_FIELD = OAuthTokenField.USER_NAME.getField();
    public static final CoreTokenField REALM_FIELD = OAuthTokenField.REALM.getField();
    public static final String INDEFINITELY = "Indefinitely";
    public static final String INDEFINITE_TOKEN_STRING_PROPERTY_NAME = "indefiniteTokenString";
    private final ClientDAO clientDao;

    private final OAuthTokenStore tokenStore;
    private final OpenAMOAuth2ProviderSettingsFactory oAuth2ProviderSettingsFactory;
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
            OpenAMOAuth2ProviderSettingsFactory oAuth2ProviderSettingsFactory, OpenAMSettings authServiceSettings,
            @Named("frRest") Debug debug) {
        this.tokenStore = tokenStore;
        this.clientDao = clientDao;
        this.identityManager = identityManager;
        this.oAuth2ProviderSettingsFactory = oAuth2ProviderSettingsFactory;
        this.authServiceSettings = authServiceSettings;
        this.debug = debug;
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
        } else {
            if (debug.errorEnabled()) {
                debug.error("TokenResource :: ACTION : Unsupported action request performed, " + actionId + " on " +
                    resourceId);
            }
            return RestUtils.generateUnsupportedOperation();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(Context context,
            CreateRequest createRequest) {
        return RestUtils.generateUnsupportedOperation();
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

            JsonValue token = tokenStore.read(tokenId);
            if (token == null) {
                if (debug.errorEnabled()) {
                    debug.error("TokenResource :: DELETE : No token with ID, " + tokenId + " found to delete");
                }
                throw new NotFoundException("Token Not Found", null);
            }
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
        final Set<String> value = getAttributeAsSet(token, attributeName);
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
    private Set<String> getAttributeAsSet(JsonValue value, String attributeName) {
        final JsonValue param = value.get(attributeName);
        if (param != null) {
            return (Set<String>) param.getObject();
        }
        return null;
    }

    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context context, final String resourceId,
            DeleteRequest request) {
        return deleteToken(context, resourceId, false)
                .thenAsync(new AsyncFunction<Void, ResourceResponse, ResourceException>() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> apply(Void value) {
                        return newResultPromise(newResourceResponse(resourceId, "1", json(object(field("success", "true")))));
                    }
                });
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, String resourceId,
            PatchRequest request) {
        return RestUtils.generateUnsupportedOperation();
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
            return handleResponse(handler, response, context);

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

    private Promise<QueryResponse, ResourceException> handleResponse(QueryResourceHandler handler, JsonValue response, Context context) throws UnauthorizedClientException,
            CoreTokenException, InternalServerErrorException, NotFoundException {
        ResourceResponse resource = newResourceResponse("result", "1", response);
        JsonValue value = resource.getContent();
        String acceptLanguage = context.asContext(HttpContext.class).getHeaderAsString("accept-language");
        Set<HashMap<String, Set<String>>> list = (Set<HashMap<String, Set<String>>>) value.getObject();

        ResourceResponse res;
        JsonValue val;

        if (list != null && !list.isEmpty()) {
            for (HashMap<String, Set<String>> entry : list) {
                val = new JsonValue(entry);
                res = newResourceResponse("result", "1", val);
                Client client = getClient(val);

                val.put(EXPIRE_TIME_KEY, getExpiryDate(json(entry), context));
                val.put(OAuth2Constants.ShortClientAttributeNames.DISPLAY_NAME.getType(), getClientName(client));
                val.put(OAuth2Constants.ShortClientAttributeNames.SCOPES.getType(), getScopes(client, val,
                        acceptLanguage));

                handler.handleResource(res);
            }
        }
        return newResultPromise(newQueryResponse());
    }

    private String getClientName(Client client) throws UnauthorizedClientException {
        return client.get(OAuth2Constants.ShortClientAttributeNames.DISPLAY_NAME.getType()).get(0).asString();
    }

    private String getScopes(Client client, JsonValue entry, String acceptLanguage) throws UnauthorizedClientException {
        JsonValue allScopes = client.get(OAuth2Constants.ShortClientAttributeNames.SCOPES.getType());
        Set<String> allowedScopes = getAttributeAsSet(entry, "scope");

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
        return new OAuth2Request() {
                public <T> T getRequest() {
                    throw new UnsupportedOperationException("Realm parameter only OAuth2Request");
                }

                public <T> T getParameter(String name) {
                    if ("realm".equals(name)) {
                        return (T) realm;
                    }
                    throw new UnsupportedOperationException("Realm parameter only OAuth2Request");
                }

                @Override
                public JsonValue getBody() {
                    return null;
                }

            @Override
            public java.util.Locale getLocale() {
                throw new UnsupportedOperationException();
            }
        };
    }

    private String getExpiryDate(JsonValue token, Context context) throws CoreTokenException,
            InternalServerErrorException, NotFoundException {

        OAuth2ProviderSettings oAuth2ProviderSettings;
        final String realm = getAttributeValue(token, "realm");
        try {
            oAuth2ProviderSettings = oAuth2ProviderSettingsFactory.get(realm);
        } catch (org.forgerock.oauth2.core.exceptions.NotFoundException e) {
            throw new NotFoundException(e.getMessage());
        }

        try {
            if (token.isDefined("refreshToken")) {
                if (oAuth2ProviderSettings.issueRefreshTokensOnRefreshingToken()) {
                    return getIndefinitelyString(context);
                } else {
                    //Use refresh token expiry
                    JsonValue refreshToken = tokenStore.read(getAttributeValue(token, "refreshToken"));
                    long expiryTimeInMilliseconds = Long.parseLong(getAttributeValue(refreshToken, EXPIRE_TIME_KEY));

                    if (expiryTimeInMilliseconds == -1) {
                        return getIndefinitelyString(context);
                    }

                    return getDateFormat(context).format(new Date(expiryTimeInMilliseconds));
                }
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

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String resourceId,
            ReadRequest request) {

        try {
            AMIdentity uid = getUid(context);

            JsonValue response;
            ResourceResponse resource;
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
                Set<String> expireTimeSet = (Set<String>) expireTimeValue.getObject();
                expireTime = Long.parseLong(expireTimeSet.iterator().next());
            }

            if (currentTimeMillis() > expireTime) {
                throw new NotFoundException("Could not find valid token with given ID");
            }

            String grantType = getAttributeValue(response, GRANT_TYPE);

            if (grantType != null && grantType.equalsIgnoreCase(OAuth2Constants.TokenEndpoint.CLIENT_CREDENTIALS)) {
                resource =
                        newResourceResponse(OAuth2Constants.Params.ID, String.valueOf(currentTimeMillis()), response);
                return newResultPromise(resource);
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
                    resource =
                            newResourceResponse(OAuth2Constants.Params.ID, String.valueOf(currentTimeMillis()), response);
                    return newResultPromise(resource);
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

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, String resourceId,
            UpdateRequest request) {
        return RestUtils.generateUnsupportedOperation();
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

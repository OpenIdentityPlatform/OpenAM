/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012-2014 ForgeRock AS.
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
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.sm.DNMapper;
import org.apache.commons.lang.StringUtils;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PermanentException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.servlet.HttpContext;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.forgerockrest.RestUtils;
import org.forgerock.openam.oauth2.IdentityManager;
import org.forgerock.openam.oauth2.OAuthTokenStore;
import org.forgerock.openam.oauth2.OpenAMOAuth2ProviderSettingsFactory;
import org.forgerock.openidconnect.Client;
import org.forgerock.openidconnect.ClientDAO;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.HttpURLConnection;
import java.security.AccessController;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.forgerock.json.fluent.JsonValue.*;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.*;
import static org.forgerock.oauth2.core.OAuth2Constants.Params.GRANT_TYPE;
import static org.forgerock.oauth2.core.OAuth2Constants.Params.REALM;
import static org.forgerock.oauth2.core.OAuth2Constants.Token.OAUTH_ACCESS_TOKEN;
import static org.forgerock.oauth2.core.OAuth2Constants.TokenEndpoint.CLIENT_CREDENTIALS;

public class TokenResource implements CollectionResourceProvider {

    private static final DateFormat DATE_FORMATTER = (new SimpleDateFormat()).getDateTimeInstance(DateFormat.MEDIUM,
            DateFormat.SHORT);
    public static final String EXPIRE_TIME_KEY = "expireTime";
    private final ClientDAO clientDao;

    private final OAuthTokenStore tokenStore;
    private final OpenAMOAuth2ProviderSettingsFactory oAuth2ProviderSettingsFactory;
    private final Debug debug;
    private static SSOToken token = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
    private static String adminUser = SystemProperties.get(Constants.AUTHENTICATION_SUPER_USER);
    private static AMIdentity adminUserId = null;

    static {
        if (adminUser != null) {
            adminUserId = new AMIdentity(token,
                    adminUser, IdType.USER, "/", null);
        }
    }

    private final IdentityManager identityManager;

    @Inject
    public TokenResource(OAuthTokenStore tokenStore, ClientDAO clientDao, IdentityManager identityManager,
            OpenAMOAuth2ProviderSettingsFactory oAuth2ProviderSettingsFactory, @Named("frRest") Debug debug) {
        this.tokenStore = tokenStore;
        this.clientDao = clientDao;
        this.identityManager = identityManager;
        this.oAuth2ProviderSettingsFactory = oAuth2ProviderSettingsFactory;
        this.debug = debug;
    }

    @Override
    public void actionCollection(ServerContext context, ActionRequest actionRequest, ResultHandler<JsonValue> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request,
            ResultHandler<JsonValue> handler) {

        String actionId = request.getAction();

        if ("revoke".equalsIgnoreCase(actionId)) {
            if (deleteToken(context, resourceId, handler, true)) {
                handler.handleResult(json(object()));
            }
        } else {
            if (debug.errorEnabled()) {
                debug.error("TokenResource :: ACTION : Unsupported action request performed, " + actionId + " on " +
                    resourceId);
            }
            RestUtils.generateUnsupportedOperation(handler);
        }
    }

    @Override
    public void createInstance(ServerContext context, CreateRequest createRequest, ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * Deletes the token with the provided token id.
     *
     * @param context The context.
     * @param tokenId The token id.
     * @param handler The handler.
     * @param deleteRefreshToken Whether to delete associated refresh token, if token id is for an access token.
     * @return {@code true} if the token has been deleted.
     */
    private boolean deleteToken(ServerContext context, String tokenId, ResultHandler<?> handler,
            boolean deleteRefreshToken) {
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

            return true;

        } catch (CoreTokenException e) {
            handler.handleError(new ServiceUnavailableException(e.getMessage(), e));
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (SSOException e) {
            debug.error("TokenResource :: DELETE : Unable to retrieve identity of the requesting user. Unauthorized.");
            handler.handleError(new PermanentException(401, "Unauthorized", e));
        } catch (IdRepoException e) {
            debug.error("TokenResource :: DELETE : Unable to retrieve identity of the requesting user. Unauthorized.");
            handler.handleError(new PermanentException(401, "Unauthorized", e));
        } catch (UnauthorizedClientException e) {
            debug.error("TokenResource :: DELETE : Requesting user is unauthorized.");
            handler.handleError(new PermanentException(401, "Unauthorized", e));
        }

        return false;
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
    public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request,
            ResultHandler<Resource> handler) {
        if (deleteToken(context, resourceId, handler, false)) {
            Resource resource = new Resource(resourceId, "1", json(object(field("success", "true"))));
            handler.handleResult(resource);
        }
    }

    @Override
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request,
            ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    @Override
    public void queryCollection(ServerContext context, QueryRequest queryRequest, QueryResultHandler handler) {
        try {
            JsonValue response;
            Map<String, Object> query = new HashMap<String, Object>();

            //get uid of submitter
            AMIdentity uid;
            try {
                uid = getUid(context);
                if (!uid.equals(adminUserId)) {
                    query.put(USERNAME, uid.getName());
                    query.put(REALM, DNMapper.orgNameToRealmName(uid.getRealm()));
                } else {
                    query.put(USERNAME, "*");
                }
            } catch (Exception e) {
                if (debug.errorEnabled()) {
                    debug.error("TokenResource :: QUERY : Unable to query collection as no UID discovered " +
                            "for requesting user.");
                }
                handler.handleError(new PermanentException(401, "Unauthorized", e));
            }

            String id = queryRequest.getQueryId();
            String queryString;

            if (id.equals("access_token")) {
                queryString = "tokenName=access_token";
            } else {
                queryString = "";
            }

            String[] constraints = queryString.split("\\,");
            for (String constraint : constraints) {
                String[] params = constraint.split("=");
                if (params.length == 2) {
                    query.put(params[0], params[1]);
                }
            }

            response = tokenStore.query(query, TokenFilter.Type.AND);
            handleResponse(handler, response, context);

        } catch (UnauthorizedClientException e) {
            debug.error("TokenResource :: QUERY : Unable to query collection as the client is not authorized.", e);
            handler.handleError(new PermanentException(401, e.getMessage(), e));
        } catch (CoreTokenException e) {
            debug.error("TokenResource :: QUERY : Unable to query collection as the token store is not available.", e);
            handler.handleError(new ServiceUnavailableException(e.getMessage(), e));
        } catch (InternalServerErrorException e) {
            debug.error("TokenResource :: QUERY : Unable to query collection as writing the response failed.", e);
            handler.handleError(e);
        }
    }

    private void handleResponse(QueryResultHandler handler, JsonValue response, ServerContext context) throws UnauthorizedClientException,
            CoreTokenException, InternalServerErrorException {
        Resource resource = new Resource("result", "1", response);
        JsonValue value = resource.getContent();
        String acceptLanguage = context.asContext(HttpContext.class).getHeaderAsString("accept-language");
        Set<HashMap<String, Set<String>>> list = (Set<HashMap<String, Set<String>>>) value.getObject();

        Resource res;
        JsonValue val;

        if (list != null && !list.isEmpty()) {
            for (HashMap<String, Set<String>> entry : list) {
                val = new JsonValue(entry);
                res = new Resource("result", "1", val);
                Client client = getClient(val);

                val.put(EXPIRE_TIME_KEY, getExpiryDate(json(entry)));
                val.put(OAuth2Constants.ShortClientAttributeNames.DISPLAY_NAME.getType(), getClientName(client));
                val.put(OAuth2Constants.ShortClientAttributeNames.SCOPES.getType(), getScopes(client, val,
                        acceptLanguage));

                handler.handleResource(res);
            }
        }
        handler.handleResult(new QueryResult());
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

    private String getExpiryDate(JsonValue token) throws CoreTokenException, InternalServerErrorException {

        OAuth2ProviderSettings oAuth2ProviderSettings = oAuth2ProviderSettingsFactory.get(
                getAttributeValue(token, "realm"));

        try {
            if (token.isDefined("refreshToken")) {
                if (oAuth2ProviderSettings.issueRefreshTokensOnRefreshingToken()) {
                    return "Indefinitely";
                } else {
                    //Use refresh token expiry
                    JsonValue refreshToken = tokenStore.read(getAttributeValue(token, "refreshToken"));
                    long expiryTimeInMilliseconds = Long.parseLong(getAttributeValue(refreshToken, EXPIRE_TIME_KEY));
                    return DATE_FORMATTER.format(new Date(expiryTimeInMilliseconds));
                }
            } else {
                //Use access token expiry
                long expiryTimeInMilliseconds = Long.parseLong(getAttributeValue(token, EXPIRE_TIME_KEY));
                return DATE_FORMATTER.format(new Date(expiryTimeInMilliseconds));
            }
        } catch (ServerException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @Override
    public void readInstance(ServerContext context, String resourceId, ReadRequest request,
            ResultHandler<Resource> handler) {

        try {
            AMIdentity uid = getUid(context);

            JsonValue response;
            Resource resource;
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

            if (System.currentTimeMillis() > expireTime) {
                throw new NotFoundException("Could not find valid token with given ID");
            }

            String grantType = getAttributeValue(response, GRANT_TYPE);

            if (grantType != null && grantType.equalsIgnoreCase(OAuth2Constants.TokenEndpoint.CLIENT_CREDENTIALS)) {
                resource =
                        new Resource(OAuth2Constants.Params.ID, String.valueOf(System.currentTimeMillis()), response);
                handler.handleResult(resource);
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
                            new Resource(OAuth2Constants.Params.ID, String.valueOf(System.currentTimeMillis()), response);
                    handler.handleResult(resource);
                } else {
                    if (debug.errorEnabled()) {
                        debug.error("TokenResource :: READ : Only the resource owner or an administrator may perform "
                                + "a read on the token with ID, " + resourceId + ".");
                    }
                    throw new PermanentException(401, "Unauthorized", null);
                }
            }
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (SSOException e) {
            debug.error("TokenResource :: READ : Unable to query collection as the IdRepo " +
                    "failed to return a valid user.", e);
            handler.handleError(new PermanentException(401, "Unauthorized", e));
        } catch (IdRepoException e) {
            debug.error("TokenResource :: READ : Unable to query collection as the IdRepo " +
                    "failed to return a valid user.", e);
            handler.handleError(new PermanentException(401, "Unauthorized", e));
        } catch (UnauthorizedClientException e) {
            debug.error("TokenResource :: READ : Unable to query collection as the client is not authorized.", e);
            handler.handleError(new PermanentException(401, "Unauthorized", e));
        }
    }

    @Override
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request,
            ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * Returns TokenID from headers
     *
     * @param context ServerContext which contains the headers.
     * @return String with TokenID
     */
    private String getCookieFromServerContext(ServerContext context) {
        return RestUtils.getCookieFromServerContext(context);
    }

    private AMIdentity getUid(ServerContext context) throws SSOException, IdRepoException, UnauthorizedClientException {
        String cookie = getCookieFromServerContext(context);
        SSOTokenManager mgr = SSOTokenManager.getInstance();
        SSOToken token = mgr.createSSOToken(cookie);
        return identityManager.getResourceOwnerIdentity(token.getProperty("UserToken"), token.getProperty("Organization"));
    }

}

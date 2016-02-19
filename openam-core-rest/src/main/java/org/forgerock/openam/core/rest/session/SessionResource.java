/*
 * Copyright 2013-2016 ForgeRock AS.
 *
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
 */

package org.forgerock.openam.core.rest.session;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.*;
import static org.forgerock.openam.utils.Time.*;
import static org.forgerock.util.promise.Promises.newResultPromise;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.forgerock.http.header.CookieHeader;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.AdviceContext;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.openam.authentication.service.AuthUtilsWrapper;
import org.forgerock.openam.core.rest.session.query.SessionQueryManager;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.session.SessionPropertyWhitelist;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.services.context.AttributesContext;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * Represents Sessions that can queried via a REST interface.
 *
 * Currently describe three different entrypoints for this Resource, useful when querying
 * Session Information:
 *
 * <ul>
 *     <li>All - All sessions across all servers known to OpenAM.</li>
 *     <li>Servers - Lists all servers that are known to OpenAM.</li>
 *     <li>[server-id] - Lists the servers for that server instance.</li>
 * </ul>
 *
 * This resources acts as a read only resource for the most part, allowing only
 * specific, whitelisted properties to be set through it.
 */
public class SessionResource implements CollectionResourceProvider {

    private static final Debug LOGGER = Debug.getInstance(SessionConstants.SESSION_DEBUG);

    public static final String LOGOUT_ACTION_ID = "logout";
    public static final String VALIDATE_ACTION_ID = "validate";
    public static final String IS_ACTIVE_ACTION_ID = "isActive";

    /**
     * @deprecated use getTimeLeft instead.
     */
    @Deprecated
    public static final String GET_MAX_TIME_ACTION_ID = "getMaxTime"; //time remaining

    public static final String GET_TIME_LEFT_ACTION_ID = "getTimeLeft"; //time remaining
    public static final String GET_IDLE_ACTION_ID = "getIdle"; //current idle time
    public static final String GET_MAX_IDLE_ACTION_ID = "getMaxIdle"; //max idle time
    public static final String GET_MAX_SESSION_TIME_ID = "getMaxSessionTime"; //max session time

    public static final String GET_PROPERTY_ACTION_ID = "getProperty";
    public static final String SET_PROPERTY_ACTION_ID = "setProperty";
    public static final String DELETE_PROPERTY_ACTION_ID = "deleteProperty";
    public static final String GET_PROPERTY_NAMES_ACTION_ID = "getPropertyNames";

    public static final String KEYWORD_PROPERTIES = "properties";
    public static final String KEYWORD_RESULT = "result";
    public static final String KEYWORD_SUCCESS = "success";
    public static final String KEYWORD_ALL = "all";
    public static final String KEYWORD_LIST = "list";
    public static final String HEADER_USER_ID = "userid";
    public static final String HEADER_TIME_REMAINING = "timeleft";

    private final SessionQueryManager queryManager;
    private final SSOTokenManager ssoTokenManager;
    private final AuthUtilsWrapper authUtilsWrapper;
    private final Map<String, ActionHandler> actionHandlers;
    private final SessionPropertyWhitelist sessionPropertyWhitelist;

    /**
     * Dependency Injection constructor allowing the SessionResource dependency to be provided.
     *  @param sessionQueryManager An instance of the SessionQueryManager. Must not null.
     * @param ssoTokenManager An instance of the SSOTokenManager.
     * @param authUtilsWrapper A wrapper around AuthUtils static methods to facilitate testing.
     * @param sessionPropertyWhitelist A session property whitelist
     */
    @Inject
    public SessionResource(final SessionQueryManager sessionQueryManager,
                           final SSOTokenManager ssoTokenManager,
                           final AuthUtilsWrapper authUtilsWrapper,
                           final SessionPropertyWhitelist sessionPropertyWhitelist) {
        this.queryManager = sessionQueryManager;
        this.ssoTokenManager = ssoTokenManager;
        this.authUtilsWrapper = authUtilsWrapper;
        this.sessionPropertyWhitelist = sessionPropertyWhitelist;
        actionHandlers = new CaseInsensitiveHashMap<>();
        actionHandlers.put(VALIDATE_ACTION_ID, new ValidateActionHandler());
        actionHandlers.put(LOGOUT_ACTION_ID, new LogoutActionHandler());
        actionHandlers.put(IS_ACTIVE_ACTION_ID, new IsActiveActionHandler());
        actionHandlers.put(GET_IDLE_ACTION_ID, new GetIdleTimeActionHandler());
        actionHandlers.put(GET_MAX_IDLE_ACTION_ID, new GetMaxIdleTimeActionHandler());
        actionHandlers.put(GET_MAX_SESSION_TIME_ID, new GetMaxSessionTimeActionHandler());
        actionHandlers.put(GET_MAX_TIME_ACTION_ID, new GetTimeLeftActionHandler());
        actionHandlers.put(GET_TIME_LEFT_ACTION_ID, new GetTimeLeftActionHandler());
        actionHandlers.put(GET_PROPERTY_ACTION_ID, new GetPropertyActionHandler());
        actionHandlers.put(SET_PROPERTY_ACTION_ID, new SetPropertyActionHandler());
        actionHandlers.put(DELETE_PROPERTY_ACTION_ID, new DeletePropertyActionHandler());
        actionHandlers.put(GET_PROPERTY_NAMES_ACTION_ID, new GetPropertyNamesActionHandler());
    }

    /**
     * Returns a collection of all Server ID that are known to the OpenAM instance.
     *
     *  @return A non null, possibly empty collection of server ids.
     */
    public Collection<String> getAllServerIds() {
        try {
            return WebtopNaming.getAllServerIDs();
        } catch (Exception e) {
            LOGGER.error("SessionResource.getAllServerIds() :: WebtopNaming throw irrecoverable error.");
            throw new IllegalStateException("Cannot recover from this error", e);
        }
    }

    /**
     * Actions supported are:
     * <ul>
     * <li>{@link #LOGOUT_ACTION_ID}</li>
     * <li>{@link #VALIDATE_ACTION_ID}</li>
     * <li>{@link #IS_ACTIVE_ACTION_ID}</li>
     * <li>{@link #GET_MAX_TIME_ACTION_ID}</li>
     * <li>{@link #GET_IDLE_ACTION_ID}</li>
     * </ul>
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     */
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest request) {
        final String cookieName = SystemProperties.get(Constants.AM_COOKIE_NAME, "iPlanetDirectoryPro");

        String tokenId = getTokenIdFromUrlParam(request);

        if (tokenId == null) {
            tokenId = getTokenIdFromHeader(context, cookieName);
        }

        if (tokenId == null) {
            tokenId = getTokenIdFromCookie(context, cookieName);
        }

        // Should any of these actions in the future be allowed to function without an SSO token, this
        // code will have to be moved/changed.
        if (tokenId == null) {
            final BadRequestException e = new BadRequestException("iPlanetDirectoryCookie not set on request");
            LOGGER.message("SessionResource.handleNullSSOToken :: iPlanetDirectoryCookie not set on request", e);
            return e.asPromise();
        }

        return internalHandleAction(tokenId, context, request);
    }

    protected String getTokenIdFromUrlParam(ActionRequest request) {
        return request.getAdditionalParameter("tokenId");
    }

    protected String getTokenIdFromCookie(Context context, String cookieName) {
        final List<String> header = context.asContext(HttpContext.class).getHeader(cookieName.toLowerCase());
        if (!header.isEmpty()) {
            return header.get(0);
        }
        return null;
    }

    protected String getTokenIdFromHeader(Context context, String cookieName) {
        final List<String> headers = context.asContext(HttpContext.class).getHeader("cookie");

        for (String header : headers) {
            for (org.forgerock.http.protocol.Cookie cookie : CookieHeader.valueOf(header).getCookies()) {
                if (cookie.getName().equalsIgnoreCase(cookieName)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Actions supported are:
     * <ul>
     *     <li>{@link #LOGOUT_ACTION_ID}</li>
     *     <li>{@link #VALIDATE_ACTION_ID}</li>
     *     <li>{@link #IS_ACTIVE_ACTION_ID}</li>
     *     <li>{@link #GET_MAX_TIME_ACTION_ID}</li>
     *     <li>{@link #GET_IDLE_ACTION_ID}</li>
     * </ul>
     *
     * @param context {@inheritDoc}
     * @param tokenId The SSO Token Id.
     * @param request {@inheritDoc}
     */
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, String tokenId,
            ActionRequest request) {
        return internalHandleAction(tokenId, context, request);
    }

    /**
     * Handle the action specified by the user (i.e. one of those in the validActions set).
     * @param tokenId The id of the token to concentrate on.
     * @param request The ActionRequest, giving us all our parameters.
     */
    private Promise<ActionResponse, ResourceException> internalHandleAction(String tokenId, Context context, ActionRequest request) {

        final String action = request.getAction();
        final ActionHandler actionHandler = actionHandlers.get(action);

        if (actionHandler != null) {
            return actionHandler.handle(tokenId, context, request);

        } else {
            String message = String.format("Action %s not implemented for this resource", action);
            NotSupportedException e = new NotSupportedException(message);
            if (LOGGER.messageEnabled()) {
                LOGGER.message("SessionResource.actionInstance :: " + message, e);
            }
            return e.asPromise();
        }
    }

    /**
     * tokenId may, or may not, specify a valid token.  If it does, retrieve it and the carefully refresh it so
     * as not to alter its idle time setting.  If it does not exist, or is invalid, throw an SSOException.
     *
     * @param tokenId The id of the token to retrieve and cautiously refresh
     * @return a valid SSOToken
     * @throws SSOException if the token id does not identify a valid token
     */
    private SSOToken getToken(String tokenId) throws SSOException {
        SSOToken ssoToken = null;
        if (StringUtils.isNotEmpty(tokenId)) {

            ssoToken = ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime(tokenId);
            if (ssoToken != null) {
                // Most important to not call refreshSession here as that may update the idle time
                ssoTokenManager.refreshSessionWithoutIdleReset(ssoToken);

                // if the token is not valid after all, forget we saw it.
                if (!ssoTokenManager.isValidToken(ssoToken, false)) {
                    ssoToken = null;
                }
            }
        }
        if (ssoToken == null) {
            throw new SSOException("The tokenId " + tokenId + " is not valid");
        }
        return ssoToken;
    }

    /**
     * Creates a AMIdentity from the specified SSOToken.
     *
     * @param ssoToken The SSOToken.
     * @return The AMIdentity.
     * @throws IdRepoException If a problem occurs creating the AMIdentity.
     * @throws SSOException If a problem occurs creating the AMIdentity.
     */
    AMIdentity getIdentity(SSOToken ssoToken) throws IdRepoException, SSOException {
        return new AMIdentity(ssoToken);
    }

    /**
     * Converts the specified DN into a realm string.
     *
     * @param dn The DN.
     * @return The realm.
     */
    String convertDNToRealm(String dn) {
        return DNMapper.orgNameToRealmName(dn);
    }

    /**
     * Queries the session resources using one of the predefined query filters.
     *
     * all - (default) will query all Sessions across all servers.
     * list - will list the available servers which is useful for the next query
     * [server-id] - will list the available Sessions on the named server.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request,
            QueryResourceHandler handler) {
        String id = request.getQueryId();

        if (KEYWORD_LIST.equals(id)) {
            Collection<String> servers = generateListServers();
            LOGGER.message("SessionResource.queryCollection() :: Retrieved list of servers for query.");
            handler.handleResource(newResourceResponse(KEYWORD_LIST, String.valueOf(currentTimeMillis()),
                    new JsonValue(servers)));
        } else {
            Collection<SessionInfo> sessions;

            if (KEYWORD_ALL.equals(id)) {
                sessions = generateAllSessions();
                LOGGER.message("SessionResource.queryCollection() :: Retrieved list of sessions for query.");
            } else {
                sessions = generateNamedServerSession(id);
                LOGGER.message("SessionResource.queryCollection() :: Retrieved list of specified servers for query.");
            }

            for (SessionInfo session : sessions) {

                long timeleft = TimeUnit.SECONDS.toMinutes(session.getTimeLeft());
                String username = session.getProperties().get("UserId");

                Map<String, Object> map = new HashMap<String, Object>();
                map.put(HEADER_USER_ID, username);
                map.put(HEADER_TIME_REMAINING, timeleft);

                handler.handleResource(newResourceResponse("Sessions", String.valueOf(currentTimeMillis()),
                        new JsonValue(map)));
            }
        }

        return newResultPromise(newQueryResponse());
    }

    /**
     * Perform a read operation against a named session.
     *
     * {@inheritDoc}
     */
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String id, ReadRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    /**
     * @param serverId Server to query.
     * @return A non null collection of SessionInfos from the named server.
     */
    private Collection<SessionInfo> generateNamedServerSession(String serverId) {
        List<String> serverList = Arrays.asList(new String[]{serverId});
        Collection<SessionInfo> sessions = queryManager.getAllSessions(serverList);
        if (LOGGER.messageEnabled()) {
            LOGGER.message("SessionResource.generateNmaedServerSession :: retrieved session list for server, " +
                    serverId);
        }
        return sessions;
    }


    /**
     * @return A non null collection of SessionInfo instances queried across all servers.
     */
    private Collection<SessionInfo> generateAllSessions() {
        Collection<SessionInfo> sessions = queryManager.getAllSessions(getAllServerIds());
        if (LOGGER.messageEnabled()) {
            LOGGER.message("SessionResource.generateNmaedServerSession :: retrieved session list for all servers.");
        }
        return sessions;
    }


    /**
     * @return Returns a JSON Resource which defines the available servers.
     */
    private Collection<String> generateListServers() {
        return getAllServerIds();
    }

    /**
     * {@inheritDoc}
     */
    public Promise<ResourceResponse, ResourceException> createInstance(Context ctx, CreateRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    /**
     * {@inheritDoc}
     */
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context ctx, String resId,
            DeleteRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    /**
     * {@inheritDoc}
     */
    public Promise<ResourceResponse, ResourceException> patchInstance(Context ctx, String resId,
            PatchRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    /**
     * {@inheritDoc}
     */
    public Promise<ResourceResponse, ResourceException> updateInstance(Context ctx, String resId,
            UpdateRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    /**
     * Defines a delegate capable of handling a particular action for a collection or instance
     */
    private interface ActionHandler {
        Promise<ActionResponse, ResourceException> handle(String tokenId, Context context, ActionRequest request);
    }

    /**
     * Handler for 'logout' action
     */
    private final class LogoutActionHandler implements ActionHandler {

        /**
         * This class serves as a mocked HttpServletResponse, which will be passed to the AuthUtils#logout method,
         * specifically to handle when the PersistentCookieAuthModule needs to clear an existing session-jwt cookie,
         * which it does by adding an expired session-jwt cookie to the response. Because the HttpServletResponse is
         * not available to CREST services, but rather headers in the response are set via the AdviceContext, this
         * class will take set cookies and translate them into the AdviceContext associated with the current CREST
         * Context.
         */
        private final class HeaderCollectingHttpServletResponse extends HttpServletResponseWrapper {

            private static final String SET_COOKIE_HEADER = "Set-Cookie";

            private final AdviceContext adviceContext;

            private HeaderCollectingHttpServletResponse(HttpServletResponse response, AdviceContext adviceContext) {
                super(response);
                this.adviceContext = adviceContext;
            }

            @Override
            public void addCookie(Cookie cookie) {
                adviceContext.putAdvice(SET_COOKIE_HEADER,
                        new org.forgerock.caf.http.SetCookieSupport().generateHeader(cookie));

            }

            @Override
            public void setHeader(String name, String value) {
                adviceContext.putAdvice(name, value);
            }

            @Override
            public void addHeader(String name, String value) {
                adviceContext.putAdvice(name, value);
            }
        }

        @Override
        public Promise<ActionResponse, ResourceException> handle(String tokenId, Context context,
                ActionRequest request) {
            try {
                JsonValue jsonValue = logout(tokenId, context);
                return newResultPromise(newActionResponse(jsonValue));
            } catch (InternalServerErrorException e) {
                if (LOGGER.errorEnabled()) {
                    LOGGER.error("SessionResource.actionInstance :: Error performing logout for token "
                            + tokenId, e);
                }
                return e.asPromise();
            }
        }

        /**
         * Logs out a user.
         *
         * @param tokenId The id of the Token to invalidate
         * @throws InternalServerErrorException If the tokenId is invalid or could not be used to logout.
         */
        private JsonValue logout(String tokenId, Context context) throws InternalServerErrorException {

            SSOToken ssoToken;
            try {
                if (tokenId == null) {
                    if (LOGGER.messageEnabled()) {
                        LOGGER.message("SessionResource.logout() :: Null Token Id.");
                    }
                    throw new InternalServerErrorException("Null Token Id");
                }
                ssoToken = ssoTokenManager.createSSOToken(tokenId);
            } catch (SSOException ex) {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put(KEYWORD_RESULT, "Token has expired");
                if (LOGGER.messageEnabled()) {
                    LOGGER.message("SessionResource.logout() :: Token ID, " + tokenId + ", already expired.");
                }
                return new JsonValue(map);
            }
            HttpServletResponse httpServletResponse = null;
            final AdviceContext adviceContext = context.asContext(AdviceContext.class);
            if (adviceContext == null) {
                if (LOGGER.warningEnabled()) {
                    LOGGER.warning("No AdviceContext in Context, and thus no headers can be set in the HttpServletResponse.");
                }
            } else {
                httpServletResponse = new HeaderCollectingHttpServletResponse(new UnsupportedResponse(), adviceContext);
            }
            AttributesContext requestContext = context.asContext(AttributesContext.class);
            Map<String, Object> requestAttributes = requestContext.getAttributes();
            final HttpServletRequest httpServletRequest = (HttpServletRequest) requestAttributes.get(HttpServletRequest.class.getName());

            String sessionId;
            Map<String, Object> map = new HashMap<>();

            if (ssoToken != null) {
                sessionId = ssoToken.getTokenID().toString();

                try {
                    authUtilsWrapper.logout(sessionId, httpServletRequest, httpServletResponse);
                } catch (SSOException e) {
                    if (LOGGER.errorEnabled()) {
                        LOGGER.error("SessionResource.logout() :: Token ID, " + tokenId +
                                ", unable to log out associated token.");
                    }
                    throw new InternalServerErrorException("Error logging out", e);
                }

                //equiv to LogoutViewBean's POST_PROCESS_LOGOUT_URL usage
                String papRedirect = authUtilsWrapper.getPostProcessLogoutURL(httpServletRequest);
                if (!StringUtils.isBlank(papRedirect)) {
                    map.put("goto", papRedirect);
                }
            }

            map.put("result", "Successfully logged out");
            LOGGER.message("SessionResource.logout() :: Successfully logged out token, {}", tokenId);
            return new JsonValue(map);
        }
    }

    /**
     * Handler for 'validate' action
     */
    private class ValidateActionHandler implements ActionHandler {

        private static final String VALID = "valid";

        @Override
        public Promise<ActionResponse, ResourceException> handle(String tokenId, Context context,
                ActionRequest request) {
            return newResultPromise(newActionResponse(validateSession(tokenId)));
        }

        /**
         * Will validate that the specified SSO Token Id is valid or not.
         * <br/>
         * Example response:
         * { "valid": true, "uid": "demo", "realm": "/subrealm" }
         * <br/>
         * If there is any problem getting or validating the token which causes an exception the json response will be
         * false. In addition if the token is expired then the json response will be set to true. Otherwise it will be
         * set to true.
         *
         * @param tokenId The SSO Token Id.
         * @return The json response of the validation.
         */
        private JsonValue validateSession(final String tokenId) {

            try {
                final SSOToken ssoToken = ssoTokenManager.createSSOToken(tokenId);
                return validateSession(ssoToken);
            } catch (SSOException e) {
                if (LOGGER.errorEnabled()) {
                    LOGGER.error("SessionResource.validateSession() :: Unable to validate token " + tokenId, e);
                }
                return json(object(field(VALID, false)));
            }
        }

        /**
         * Will validate that the specified SSOToken is valid or not.
         * <br/>
         * Example response:
         * { "valid": true, "uid": "demo", "realm": "/subrealm" }
         * <br/>
         * If there is any problem getting or validating the token which causes an exception the json response will be
         * false. In addition if the token is expired then the json response will be set to true. Otherwise it will be
         * set to true.
         *
         * @param ssoToken The SSO Token.
         * @return The json response of the validation.
         */
        private JsonValue validateSession(final SSOToken ssoToken) {
            try {
                if (!ssoTokenManager.isValidToken(ssoToken)) {
                    if (LOGGER.messageEnabled()) {
                        LOGGER.message("SessionResource.validateSession() :: Session validation for token, " +
                                ssoToken.getTokenID() + ", returned false.");
                    }
                    return json(object(field(VALID, false)));
                }

                if (LOGGER.messageEnabled()) {
                    LOGGER.message("SessionResource.validateSession() :: Session validation for token, " +
                            ssoToken.getTokenID() + ", returned true.");
                }
                final AMIdentity identity = getIdentity(ssoToken);
                return json(object(field(VALID, true), field("uid", identity.getName()),
                        field("realm", convertDNToRealm(identity.getRealm()))));
            } catch (SSOException e) {
                if (LOGGER.errorEnabled()) {
                    LOGGER.error("SessionResource.validateSession() :: Session validation for token, " +
                            ssoToken.getTokenID() + ", failed to return.", e);
                }
                return json(object(field(VALID, false)));
            } catch (IdRepoException e) {
                if (LOGGER.errorEnabled()) {
                    LOGGER.error("SessionResource.validateSession() :: Session validation for token, " +
                            ssoToken.getTokenID() + ", failed to return.", e);
                }
                return json(object(field(VALID, false)));
            }
        }
    }

    /**
     * Handler for 'isActive' action
     */
    private class IsActiveActionHandler implements ActionHandler {

        private static final String ACTIVE = "active";

        @Override
        public Promise<ActionResponse, ResourceException> handle(String tokenId, Context context,
                ActionRequest request) {
            String refresh = request.getAdditionalParameter("refresh");
            return newResultPromise(newActionResponse(isTokenIdValid(tokenId, refresh)));
        }

        /**
         * Figure whether the token id, which has been passed as an argument to the REST call
         * is valid and optionally refresh it.  This is different from validateSession because this,
         * rather inconveniently, requires you to be logged in as admin before this can be invoked.
         *
         * @param tokenId The SSO Token Id.
         * @return a jsonic "true" or "false" depending on whether the token is valid
         */
        private JsonValue isTokenIdValid(String tokenId, String refresh) {
            boolean isActive = false;
            try {
                SSOToken theToken = getToken(tokenId);

                isActive = true;
                if (Boolean.valueOf(refresh)) {
                    ssoTokenManager.refreshSession(theToken);
                }
            } catch (SSOException ignored) {
            }
            return json(object(field(ACTIVE, isActive)));
        }
    }

    /**
     * Handler for 'getMaxTime' action - from CREST 12.0.0 onwards this means 'get remaining session time'.
     */
    private class GetTimeLeftActionHandler implements ActionHandler {

        private static final String MAX_TIME = "maxtime";

        @Override
        public Promise<ActionResponse, ResourceException> handle(String tokenId, Context context,
                ActionRequest request) {
            return newResultPromise(newActionResponse(getMaxTime(tokenId)));
        }

        /**
         * Using the token id specified by the invoker, find the token and if valid, return its remaining life in
         * seconds.
         *
         * @param tokenId The SSO Token Id.
         * @return jsonic representation of the number of seconds of remaining life, or a representation of -1 if
         * invalid.
         */
        private JsonValue getMaxTime(String tokenId) {

            long maxTime = -1;
            try {
                SSOToken theToken = getToken(tokenId);
                maxTime = theToken.getTimeLeft();
            } catch (SSOException ignored) {
            }
            return json(object(field(MAX_TIME, maxTime)));
        }
    }

    /**
     * Handler for 'getMaxSessionTime' action - from CREST 12.0.0 onwards this means 'get maximum possible
     * length of session'
     */
    private class GetMaxSessionTimeActionHandler implements ActionHandler {

        private static final String MAX_SESSION_TIME = "maxsessiontime";

        @Override
        public Promise<ActionResponse, ResourceException> handle(String tokenId, Context context,
                                                                 ActionRequest request) {
            return newResultPromise(newActionResponse(getMaxSessionTime(tokenId)));
        }

        /**
         * Using the token id specified by the invoker, find the token and if valid, return the max idle time in
         * seconds.
         *
         * @param tokenId The SSO Token Id.
         * @return jsonic representation of the number of seconds a session may exist, or a representation of -1 if
         * token is invalid.
         */
        private JsonValue getMaxSessionTime(String tokenId) {

            long maxSessionTime = -1;
            try {
                SSOToken theToken = getToken(tokenId);
                maxSessionTime = theToken.getMaxSessionTime();
            } catch (SSOException ignored) {
            }
            return json(object(field(MAX_SESSION_TIME, maxSessionTime)));
        }
    }

    /**
     * Handler for 'getMaxIdle' action
     */
    private class GetMaxIdleTimeActionHandler implements ActionHandler {

        private static final String MAX_IDLE_TIME = "maxidletime";

        @Override
        public Promise<ActionResponse, ResourceException> handle(String tokenId, Context context,
                                                                 ActionRequest request) {
            return newResultPromise(newActionResponse(getMaxIdleTime(tokenId)));
        }

        /**
         * Using the token id specified by the invoker, find the token and if valid, return the max idle time in
         * seconds.
         *
         * @param tokenId The SSO Token Id.
         * @return jsonic representation of the number of seconds a session may be idle, or a representation of -1
         * if token is invalid.
         */
        private JsonValue getMaxIdleTime(String tokenId) {

            long maxIdleTime = -1;
            try {
                SSOToken theToken = getToken(tokenId);
                maxIdleTime = theToken.getMaxIdleTime();
            } catch (SSOException ignored) {
            }
            return json(object(field(MAX_IDLE_TIME, maxIdleTime)));
        }
    }

    /**
     * Handler for 'getIdle' action
     */
    private class GetIdleTimeActionHandler implements ActionHandler {

        private static final String IDLE_TIME = "idletime";

        @Override
        public Promise<ActionResponse, ResourceException> handle(String tokenId, Context context,
                ActionRequest request) {
            return newResultPromise(newActionResponse(getIdleTime(tokenId)));
        }

        /**
         * Using the token id specified by the invoker, find the token and if valid, return the remaining idle time in
         * seconds.
         *
         * @param tokenId The SSO Token Id.
         * @return jsonic representation of the number of seconds of idle time, or a representation of -1 if token
         * invalid.
         */
        private JsonValue getIdleTime(String tokenId) {

            long idleTime = -1;
            try {
                SSOToken theToken = getToken(tokenId);
                idleTime = theToken.getIdleTime();
            } catch (SSOException ignored) {
            }
            return json(object(field(IDLE_TIME, idleTime)));
        }
    }

    /**
     * Handles 'getProperty' actions. If a field is requested, return only that field. If no field is
     * specified, return the key/value of all whitelisted fields.
     *
     * REQUEST: { 'properties' : [ 'property1', 'property2'] }
     * RESPONSE: { 'property1' : 'value1', 'property2' : 'value2' }
     */
    private class GetPropertyActionHandler implements ActionHandler {
        @Override
        public Promise<ActionResponse, ResourceException> handle(final String tokenId, final Context context,
                final ActionRequest request) {

            final JsonValue result = json(object());
            try {
                final SSOToken caller = getCallerToken(context);
                final String realm = getCallerRealm(context);
                final SSOToken target = getToken(tokenId);

                if (request.getContent() == null || request.getContent().get(KEYWORD_PROPERTIES).isNull()) {
                    for (String property : sessionPropertyWhitelist.getAllListedProperties(caller, realm)) {
                        final String value = target.getProperty(property);
                        result.add(property, value == null ? "" : value);
                    }
                } else {
                    for (String requestedResult : request.getContent().get(KEYWORD_PROPERTIES).asSet(String.class)) {
                        if (sessionPropertyWhitelist.isPropertyListed(caller, realm,
                                Collections.singleton(requestedResult))) {
                            final String value = target.getProperty(requestedResult);
                            result.add(requestedResult, value == null ? "" : value);
                        } else {
                            LOGGER.warning("User {} requested property {} on {} to get which was not whitelisted or "
                                    + "was protected.", caller.getPrincipal(), requestedResult, target.getPrincipal());
                            return new ForbiddenException().asPromise();
                        }
                    }

                }

            } catch (SSOException e) {
                LOGGER.message("Unable to read session property due to unreadable SSOToken", e);
            } catch (DelegationException e) {
                LOGGER.message("Unable to read session property due to delegation match internal error", e);
                return new InternalServerErrorException().asPromise();
            }

            return newResultPromise(newActionResponse(result));
        }
    }

    /**
     * Handles 'setProperty' actions.
     *
     * REQUEST: { 'property1' : 'value1', 'property2' : 'value2' }
     * RESPONSE: { 'success' : true }
     */
    private class SetPropertyActionHandler implements ActionHandler {
        @Override
        public Promise<ActionResponse, ResourceException> handle(final String tokenId, final Context context,
                final ActionRequest request) {
            try {
                final SSOToken caller = getCallerToken(context);
                final String realm = getCallerRealm(context);
                final SSOToken target = getToken(tokenId);

                if (request.getContent() == null || request.getContent().isNull() ||
                        request.getContent().asMap(String.class).size() == 0) {
                    return new BadRequestException().asPromise();
                }

                final Map<String, String> entrySet = request.getContent().asMap(String.class);

                if (sessionPropertyWhitelist.isPropertyListed(caller, realm, entrySet.keySet())) {
                    for (Map.Entry<String, String> entry : request.getContent().asMap(String.class).entrySet()) {
                        target.setProperty(entry.getKey(), entry.getValue());
                    }
                } else {
                    LOGGER.warning("User {} requested property/ies {} to set on {} which was not whitelisted.",
                            caller.getPrincipal(), target.getPrincipal(), entrySet.toString());
                    return new ForbiddenException().asPromise();
                }
            } catch (SSOException e) {
                LOGGER.message("Unable to set session property due to unreadable SSOToken", e);
                return newResultPromise(newActionResponse(json(object(field(KEYWORD_SUCCESS, false)))));
            } catch (DelegationException e) {
                LOGGER.message("Unable to read session property due to delegation match internal error", e);
                return new InternalServerErrorException().asPromise();
            }

            return newResultPromise(newActionResponse(json(object(field(KEYWORD_SUCCESS, true)))));
        }
    }

    /**
     * Handles 'deleteProperty' actions.
     *
     * REQUEST: { 'properties' : [ 'property1', 'property2'] }
     * RESPONSE: { 'success' : true }
     */
    private class DeletePropertyActionHandler implements ActionHandler {
        @Override
        public Promise<ActionResponse, ResourceException> handle(final String tokenId, final Context context,
                final ActionRequest request) {
            try {
                final SSOToken caller = getCallerToken(context);
                final String realm = getCallerRealm(context);
                final SSOToken target = getToken(tokenId);

                JsonValue content = request.getContent().get(KEYWORD_PROPERTIES);

                if (content == null || content.isNull()) {
                    return new BadRequestException().asPromise(); //no properties = bad request
                }

                final Set<String> propSet = request.getContent().get(KEYWORD_PROPERTIES).asSet(String.class);

                if (sessionPropertyWhitelist.isPropertyListed(caller, realm, propSet)) {
                    for (String entry : propSet) {
                        //there is no "delete" function - we can't store null in the property map so blank it
                        target.setProperty(entry, "");
                    }
                } else {
                    LOGGER.message("User {} requested property/ies {} on {} to delete which was not whitelisted.",
                            caller.getPrincipal(), propSet.toString(), target.getPrincipal());
                    return new ForbiddenException().asPromise();
                }

            } catch (SSOException e) {
                LOGGER.message("Unable to delete session property due to unreadable SSOToken", e);
                return newResultPromise(newActionResponse(json(object(field(KEYWORD_SUCCESS, false)))));
            } catch (DelegationException e) {
                LOGGER.message("Unable to read session property due to delegation match internal error", e);
                return new InternalServerErrorException().asPromise();
            }

            return newResultPromise(newActionResponse(json(object(field(KEYWORD_SUCCESS, true)))));
        }
    }

    /**
     * Handles 'getPropertyNames' actions.
     *
     * REQUEST:
     * RESPONSE: { 'properties' : [ 'property1', 'property2' ] }
     */
    private class GetPropertyNamesActionHandler implements ActionHandler {
        @Override
        public Promise<ActionResponse, ResourceException> handle(final String tokenId, final Context context,
                                                                 final ActionRequest request) {
            try {
                final SSOToken caller = getCallerToken(context);
                final String realm = getCallerRealm(context);
                return newResultPromise(newActionResponse(json(object(field(KEYWORD_PROPERTIES,
                        sessionPropertyWhitelist.getAllListedProperties(caller, realm))))));
            } catch (SSOException e) {
                LOGGER.message("Unable to read all whitelisted session properties.", e);
            }

            return new InternalServerErrorException().asPromise();
        }
    }

    private String getCallerRealm(Context context) {
        return context.asContext(RealmContext.class).getResolvedRealm();
    }

    private SSOToken getCallerToken(Context context) throws SSOException {
        return context.asContext(SSOTokenContext.class).getCallerSSOToken();
    }
}

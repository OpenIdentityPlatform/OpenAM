/*
 * Copyright 2013-2014 ForgeRock AS.
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

package org.forgerock.openam.forgerockrest.session;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.service.SessionConstants;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.AdviceContext;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.servlet.HttpContext;
import org.forgerock.openam.authentication.service.AuthUtilsWrapper;
import org.forgerock.openam.forgerockrest.RestUtils;
import org.forgerock.openam.forgerockrest.session.query.SessionQueryManager;
import org.forgerock.openam.utils.StringUtils;

import javax.inject.Inject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.forgerock.json.fluent.JsonValue.*;

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
 * This resources acts as a read only resource for the moment.
 */
public class SessionResource implements CollectionResourceProvider {

    private static final Debug LOGGER = Debug.getInstance(SessionConstants.SESSION_DEBUG);

    public static final String LOGOUT_ACTION_ID = "logout";
    public static final String VALIDATE_ACTION_ID = "validate";
    public static final String IS_ACTIVE_ACTION_ID = "isActive";
    public static final String GET_MAX_TIME_ACTION_ID = "getMaxTime";
    public static final String GET_IDLE_ACTION_ID = "getIdle";

    public static final String KEYWORD_ALL = "all";
    public static final String KEYWORD_LIST = "list";
    public static final String HEADER_USER_ID = "userid";
    public static final String HEADER_TIME_REMAINING = "timeleft";

    private final SessionQueryManager queryManager;
    private final SSOTokenManager ssoTokenManager;
    private final AuthUtilsWrapper authUtilsWrapper;
    private final Map<String, ActionHandler> actionHandlers;

    /**
     * Dependency Injection constructor allowing the SessionResource dependency to be provided.
     *
     * @param sessionQueryManager An instance of the SessionQueryManager. Must not null.
     * @param ssoTokenManager An instance of the SSOTokenManager.
     * @param authUtilsWrapper A wrapper around AuthUtils static methods to facilitate testing.
     */
    @Inject
    public SessionResource(final SessionQueryManager sessionQueryManager,
                           final SSOTokenManager ssoTokenManager,
                           final AuthUtilsWrapper authUtilsWrapper) {
        this.queryManager = sessionQueryManager;
        this.ssoTokenManager = ssoTokenManager;
        this.authUtilsWrapper = authUtilsWrapper;
        actionHandlers = new CaseInsensitiveHashMap();
        actionHandlers.put(VALIDATE_ACTION_ID, new ValidateActionHandler());
        actionHandlers.put(LOGOUT_ACTION_ID, new LogoutActionHandler());
        actionHandlers.put(IS_ACTIVE_ACTION_ID, new IsActiveActionHandler());
        actionHandlers.put(GET_IDLE_ACTION_ID, new GetIdleTimeActionHandler());
        actionHandlers.put(GET_MAX_TIME_ACTION_ID, new GetMaxTimeActionHandler());
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
     * @param handler {@inheritDoc}
     */
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
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
            handler.handleError(e);
            return;
        }

        internalHandleAction(tokenId, context, request, handler);
    }

    protected String getTokenIdFromUrlParam(ActionRequest request) {
        return request.getAdditionalParameter("tokenId");
    }

    protected String getTokenIdFromCookie(ServerContext context, String cookieName) {
        List<String> cookieValue = StringUtils.getParameter(context.asContext(HttpContext.class).getHeaderAsString("cookie"), ";",
                cookieName);

        if (!cookieValue.isEmpty()) {
            return cookieValue.get(0);
        } else {
            return null;
        }
    }

    protected String getTokenIdFromHeader(ServerContext context, String cookieName) {
        final List<String> header = context.asContext(HttpContext.class).getHeader
                (cookieName.toLowerCase());

        if (!header.isEmpty()) {
            return header.get(0);
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
     * @param handler {@inheritDoc}
     */
    public void actionInstance(ServerContext context,
                               String tokenId,
                               ActionRequest request,
                               ResultHandler<JsonValue> handler) {

        internalHandleAction(tokenId, context, request, handler);
    }

    /**
     * Handle the action specified by the user (i.e. one of those in the validActions set).
     *
     * @param tokenId The id of the token to concentrate on.
     * @param request The ActionRequest, giving us all our parameters.
     * @param handler The result handler
     */
    private void internalHandleAction(String tokenId,
                                      ServerContext context,
                                      ActionRequest request,
                                      ResultHandler<JsonValue> handler) {

        final String action = request.getAction();
        final ActionHandler actionHandler = actionHandlers.get(action);

        if (actionHandler != null) {
            actionHandler.handle(tokenId, context, request, handler);

        } else {
            String message = String.format("Action %s not implemented for this resource", action);
            NotSupportedException e = new NotSupportedException(message);
            if (LOGGER.messageEnabled()) {
                LOGGER.message("SessionResource.actionInstance :: " + message, e);
            }
            handler.handleError(e);
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
    public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        String id = request.getQueryId();

        if (KEYWORD_LIST.equals(id)) {
            Collection<String> servers = generateListServers();
            LOGGER.message("SessionResource.queryCollection() :: Retrieved list of servers for query.");
            handler.handleResource(new Resource(KEYWORD_LIST, String.valueOf(System.currentTimeMillis()),
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

                int timeleft = convertTimeLeft(session.timeleft);
                String username = (String) session.properties.get("UserId");

                Map<String, Object> map = new HashMap<String, Object>();
                map.put(HEADER_USER_ID, username);
                map.put(HEADER_TIME_REMAINING, timeleft);

                handler.handleResource(new Resource("Sessions", String.valueOf(System.currentTimeMillis()),
                        new JsonValue(map)));
            }
        }

        handler.handleResult(new QueryResult());
    }

    /**
     * Perform a read operation against a named session.
     *
     * {@inheritDoc}
     */
    public void readInstance(ServerContext context, String id, ReadRequest request, ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
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
     * Internal function for converting time in seconds to minutes.
     *
     * @param timeleft Non null string value of time in seconds.
     * @return The parsed time.
     */
    private static int convertTimeLeft(String timeleft) {
        float seconds = Long.parseLong(timeleft);
        float mins = seconds / 60;
        return Math.round(mins);
    }

    /**
     * {@inheritDoc}
     */
    public void createInstance(ServerContext ctx, CreateRequest request, ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * {@inheritDoc}
     */
    public void deleteInstance(ServerContext ctx, String resId, DeleteRequest request,
            ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * {@inheritDoc}
     */
    public void patchInstance(ServerContext ctx, String resId, PatchRequest request,
            ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * {@inheritDoc}
     */
    public void updateInstance(ServerContext ctx, String resId, UpdateRequest request,
            ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * Defines a delegate capable of handling a particular action for a collection or instance
     */
    private static interface ActionHandler {

        void handle(String tokenId, ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler);

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
         * ServerContext.
         */
        private final class CookieCollectingHttpServletResponse extends HttpServletResponseWrapper {

            private static final String SET_COOKIE_HEADER = "Set-Cookie";

            private final AdviceContext adviceContext;

            private CookieCollectingHttpServletResponse(HttpServletResponse response, AdviceContext adviceContext) {
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
                if (SET_COOKIE_HEADER.equals(name)) {
                    adviceContext.putAdvice(name, value);
                }
            }

            @Override
            public void addHeader(String name, String value) {
                if (SET_COOKIE_HEADER.equals(name)) {
                    adviceContext.putAdvice(name, value);
                }
            }
        }

        @Override
        public void handle(String tokenId, ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
            try {
                JsonValue jsonValue = logout(tokenId, context);
                handler.handleResult(jsonValue);
            } catch (InternalServerErrorException e) {
                if (LOGGER.errorEnabled()) {
                    LOGGER.error("SessionResource.actionInstance :: Error performing logout for token "
                            + tokenId, e);
                }
                handler.handleError(e);
            }
        }

        /**
         * Logs out a user.
         *
         * @param tokenId The id of the Token to invalidate
         * @throws InternalServerErrorException If the tokenId is invalid or could not be used to logout.
         */
        private JsonValue logout(String tokenId, ServerContext context) throws InternalServerErrorException {

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
                map.put("result", "Token has expired");
                if (LOGGER.messageEnabled()) {
                    LOGGER.message("SessionResource.logout() :: Token ID, " + tokenId + ", already expired.");
                }
                return new JsonValue(map);
            }
            HttpServletResponse httpServletResponse = null;
            final AdviceContext adviceContext = context.asContext(AdviceContext.class);
            if (adviceContext == null) {
                if (LOGGER.warningEnabled()) {
                    LOGGER.warning("No AdviceContext in ServerContext, and thus no headers can be set in the HttpServletResponse.");
                }
            } else {
                httpServletResponse = new CookieCollectingHttpServletResponse(new UnsupportedResponse(), adviceContext);
            }
            if (ssoToken != null) {
                try {
                    authUtilsWrapper.logout(ssoToken.getTokenID().toString(), null, httpServletResponse);
                } catch (SSOException e) {
                    if (LOGGER.errorEnabled()) {
                        LOGGER.error("SessionResource.logout() :: Token ID, " + tokenId +
                                ", unable to log out associated token.");
                    }
                    throw new InternalServerErrorException("Error logging out", e);
                }
            }

            Map<String, Object> map = new HashMap<String, Object>();
            map.put("result", "Successfully logged out");
            if (LOGGER.messageEnabled()) {
                LOGGER.message("SessionResource.logout() :: Successfully logged out token, " + tokenId);
            }
            return new JsonValue(map);
        }
    }

    /**
     * Handler for 'validate' action
     */
    private class ValidateActionHandler implements ActionHandler {

        private static final String VALID = "valid";

        @Override
        public void handle(String tokenId, ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
            handler.handleResult(validateSession(tokenId));
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
        public void handle(String tokenId, ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
            String refresh = request.getAdditionalParameter("refresh");
            handler.handleResult(isTokenIdValid(tokenId, refresh));
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
     * Handler for 'getMaxTime' action
     */
    private class GetMaxTimeActionHandler implements ActionHandler {

        private static final String MAX_TIME = "maxtime";

        @Override
        public void handle(String tokenId, ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
            handler.handleResult(getMaxTime(tokenId));
        }

        /**
         * Using the token id specified by the invoker, find the token and if valid, return its remaining life in
         * seconds.
         * @param tokenId The SSO Token Id.
         * @return jsonic representation of the number of seconds of remaining life, or a representation of -1 if invalid
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
     * Handler for 'getIdle' action
     */
    private class GetIdleTimeActionHandler implements ActionHandler {

        private static final String IDLE_TIME = "idletime";

        @Override
        public void handle(String tokenId, ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
            handler.handleResult(getIdleTime(tokenId));
        }

        /**
         * Using the token id specified by the invoker, find the token and if valid, return the remaining idle time in
         * seconds.
         * @param tokenId The SSO Token Id.
         * @return jsonic representation of the number of seconds of idle time, or a representation of -1 if token invalid
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
}

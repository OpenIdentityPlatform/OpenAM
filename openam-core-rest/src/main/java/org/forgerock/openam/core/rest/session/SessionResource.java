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

import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.utils.Time.currentTimeMillis;
import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.http.header.CookieHeader;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
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
import org.forgerock.openam.core.rest.session.action.ActionHandler;
import org.forgerock.openam.core.rest.session.action.DeletePropertyActionHandler;
import org.forgerock.openam.core.rest.session.action.GetIdleTimeActionHandler;
import org.forgerock.openam.core.rest.session.action.GetMaxIdleTimeActionHandler;
import org.forgerock.openam.core.rest.session.action.GetMaxSessionTimeActionHandler;
import org.forgerock.openam.core.rest.session.action.GetPropertyActionHandler;
import org.forgerock.openam.core.rest.session.action.GetPropertyNamesActionHandler;
import org.forgerock.openam.core.rest.session.action.GetTimeLeftActionHandler;
import org.forgerock.openam.core.rest.session.action.IsActiveActionHandler;
import org.forgerock.openam.core.rest.session.action.LogoutActionHandler;
import org.forgerock.openam.core.rest.session.action.SetPropertyActionHandler;
import org.forgerock.openam.core.rest.session.action.ValidateActionHandler;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.session.SessionPropertyWhitelist;
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

    public static final String KEYWORD_PROPERTIES = "properties";
    public static final String KEYWORD_SUCCESS = "success";

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

    public static final String KEYWORD_RESULT = "result";
    public static final String KEYWORD_ALL = "all";
    public static final String KEYWORD_LIST = "list";

    private final SessionResourceUtil sessionResourceUtil;
    private final SSOTokenManager ssoTokenManager;
    private final AuthUtilsWrapper authUtilsWrapper;
    private final SessionPropertyWhitelist sessionPropertyWhitelist;

    private final Map<String, ActionHandler> actionHandlers;

    /**
     * Dependency Injection constructor allowing the SessionResource dependency to be provided.
     *
     * @param ssoTokenManager An instance of the SSOTokenManager.
     * @param authUtilsWrapper An instance of AuthUtilsWrapper
     * @param sessionPropertyWhitelist An instance of sessionPropertyWhitelist.
     * @param sessionResourceUtil An instance of the SessionResourceUtil.
     */
    @Inject
    public SessionResource(final SSOTokenManager ssoTokenManager, final AuthUtilsWrapper authUtilsWrapper,
            final SessionPropertyWhitelist sessionPropertyWhitelist,
            final SessionResourceUtil sessionResourceUtil) {
        this.ssoTokenManager = ssoTokenManager;
        this.authUtilsWrapper = authUtilsWrapper;
        this.sessionPropertyWhitelist = sessionPropertyWhitelist;
        this.sessionResourceUtil = sessionResourceUtil;
        actionHandlers = new CaseInsensitiveHashMap<>();
        actionHandlers.put(VALIDATE_ACTION_ID,
                new ValidateActionHandler(ssoTokenManager, sessionResourceUtil));
        actionHandlers.put(LOGOUT_ACTION_ID, new LogoutActionHandler(ssoTokenManager, authUtilsWrapper));
        actionHandlers.put(IS_ACTIVE_ACTION_ID, new IsActiveActionHandler(ssoTokenManager, sessionResourceUtil));
        actionHandlers.put(GET_IDLE_ACTION_ID, new GetIdleTimeActionHandler(sessionResourceUtil));
        actionHandlers.put(GET_MAX_IDLE_ACTION_ID, new GetMaxIdleTimeActionHandler(sessionResourceUtil));
        actionHandlers.put(GET_MAX_SESSION_TIME_ID, new GetMaxSessionTimeActionHandler(sessionResourceUtil));
        actionHandlers.put(GET_MAX_TIME_ACTION_ID, new GetTimeLeftActionHandler(sessionResourceUtil));
        actionHandlers.put(GET_TIME_LEFT_ACTION_ID, new GetTimeLeftActionHandler(sessionResourceUtil));
        actionHandlers.put(GET_PROPERTY_ACTION_ID,
                new GetPropertyActionHandler(sessionPropertyWhitelist, sessionResourceUtil));
        actionHandlers.put(SET_PROPERTY_ACTION_ID,
                new SetPropertyActionHandler(sessionPropertyWhitelist, sessionResourceUtil));
        actionHandlers.put(DELETE_PROPERTY_ACTION_ID,
                new DeletePropertyActionHandler(sessionPropertyWhitelist, sessionResourceUtil));
        actionHandlers.put(GET_PROPERTY_NAMES_ACTION_ID, new GetPropertyNamesActionHandler(sessionPropertyWhitelist));

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
     *
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
                sessions = sessionResourceUtil.generateAllSessions();
                LOGGER.message("SessionResource.queryCollection() :: Retrieved list of sessions for query.");
            } else {
                sessions = sessionResourceUtil.generateNamedServerSession(id);
                LOGGER.message("SessionResource.queryCollection() :: Retrieved list of specified servers for query.");
            }

            for (SessionInfo session : sessions) {
                handler.handleResource(newResourceResponse("Sessions", String.valueOf(currentTimeMillis()),
                        sessionResourceUtil.jsonValueOf(session)));
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
     * @return Returns a JSON Resource which defines the available servers.
     */
    private Collection<String> generateListServers() {
        return sessionResourceUtil.getAllServerIds();
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
}

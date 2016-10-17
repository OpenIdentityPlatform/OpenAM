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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.core.rest.session;

import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.*;
import static org.forgerock.openam.utils.Time.currentTimeMillis;
import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.api.annotations.Action;
import org.forgerock.api.annotations.Actions;
import org.forgerock.api.annotations.ApiError;
import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Parameter;
import org.forgerock.api.annotations.Queries;
import org.forgerock.api.annotations.Query;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.enums.ParameterSource;
import org.forgerock.api.enums.QueryType;
import org.forgerock.http.header.CookieHeader;
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
import org.forgerock.openam.core.rest.session.action.GetSessionInfoActionHandler;
import org.forgerock.openam.core.rest.session.action.GetSessionPropertiesActionHandler;
import org.forgerock.openam.core.rest.session.action.LogoutActionHandler;
import org.forgerock.openam.core.rest.session.action.RefreshActionHandler;
import org.forgerock.openam.core.rest.session.action.UpdateSessionPropertiesActionHandler;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.session.SessionPropertyWhitelist;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * End point for querying the session info via a REST interface.
 *
 * Currently describe three different query entry points for this Resource,
 * useful when querying Session Information:
 *
 * <ul>
 *     <li>All - All sessions across all servers known to OpenAM.</li>
 *     <li>[server-id] - Lists the sessions for that server instance.</li>
 *     <li>[session-id] - Details opf the session information </li>
 * </ul>
 * 
 *
 * This resources acts as a read only resource.
 */
@CollectionProvider(
        details = @Handler(
                title = SESSION_RESOURCE + TITLE,
                description = SESSION_RESOURCE + DESCRIPTION,
                mvccSupported = false,
                resourceSchema = @Schema(schemaResource = "SessionResource.schema.json")
        )
)
public class SessionResourceV2 implements CollectionResourceProvider {

    private static final Debug LOGGER = Debug.getInstance(SessionConstants.SESSION_DEBUG);

    /**
     * Path Parameter Name
     */
    public static final String TOKEN_PARAM_NAME = "tokenId";

    public static final String GET_SESSION_INFO_ACTION_ID = "getSessionInfo";
    public static final String LOGOUT_ACTION_ID = "logout";
    public static final String REFRESH_ACTION_ID = "refresh";
    public static final String GET_SESSION_PROPERTIES_ACTION_ID = "getSessionProperties";
    public static final String UPDATE_SESSION_PROPERTIES_ACTION_ID = "updateSessionProperties";

    public static final String KEYWORD_ALL = "all";
    public static final String KEYWORD_SERVER_ID = "serverId";

    private final String ALL_QUERY_ID = "all";
    private final String SERVER_QUERY_ID = "server";
    private final SessionPropertyWhitelist sessionPropertyWhitelist;

    private final Map<String, ActionHandler> actionHandlers;
    private final SessionResourceUtil sessionResourceUtil;

    /**
     * Dependency Injection constructor allowing the SessionResource dependency to be provided.
     *
     * @param ssoTokenManager An instance of the SSOTokenManager.
     * @param  authUtilsWrapper An instance of the AuthUtilsWrapper.
     * @param sessionResourceUtil An instance of the SessionResourceUtil.
     * @param sessionPropertyWhitelist An instance o the SessionPropertyWhitelist.
     */
    @Inject
    public SessionResourceV2(final SSOTokenManager ssoTokenManager, AuthUtilsWrapper authUtilsWrapper,
            final SessionResourceUtil sessionResourceUtil, SessionPropertyWhitelist sessionPropertyWhitelist) {
        this.sessionResourceUtil = sessionResourceUtil;
        this.sessionPropertyWhitelist = sessionPropertyWhitelist;
        actionHandlers = new CaseInsensitiveHashMap<>();
        actionHandlers.put(REFRESH_ACTION_ID,
                new RefreshActionHandler(ssoTokenManager, sessionResourceUtil));
        actionHandlers.put(LOGOUT_ACTION_ID, new LogoutActionHandler(ssoTokenManager, authUtilsWrapper));
        actionHandlers.put(GET_SESSION_INFO_ACTION_ID, new GetSessionInfoActionHandler(sessionResourceUtil));
        actionHandlers.put(GET_SESSION_PROPERTIES_ACTION_ID,
                new GetSessionPropertiesActionHandler(sessionPropertyWhitelist, sessionResourceUtil));
        actionHandlers.put(UPDATE_SESSION_PROPERTIES_ACTION_ID,
                new UpdateSessionPropertiesActionHandler(sessionPropertyWhitelist, sessionResourceUtil));
    }

    /**
     * Actions supported are:
     * <ul>
     *     <li>{@link #GET_SESSION_INFO_ACTION_ID}</li>
     *     <li>{@link #LOGOUT_ACTION_ID}</li>
     *     <li>{@link #REFRESH_ACTION_ID}</li>
     *     <li>{@link #GET_SESSION_PROPERTIES_ACTION_ID}</li>
     *     <li>{@link #UPDATE_SESSION_PROPERTIES_ACTION_ID}</li>
     * </ul>
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     */
    @Actions({
        @Action(
                operationDescription = @Operation(
                    description = SESSION_RESOURCE + GET_SESSION_INFO_ACTION_ID + "." + ACTION_DESCRIPTION,
                    parameters = @Parameter(name = TOKEN_PARAM_NAME, type = "string", description = SESSION_RESOURCE + TOKEN_PARAM_NAME + "." + PARAMETER_DESCRIPTION),
                    errors = {
                        @ApiError(
                            code = 401,
                            description = SESSION_RESOURCE + ERROR_401_DESCRIPTION
                        )
                    }
                ),
                name = GET_SESSION_INFO_ACTION_ID,
                response = @Schema(schemaResource = "SessionResource.properties.names.schema.json")
        ),
        @Action(
            operationDescription = @Operation(
                description = SESSION_RESOURCE + LOGOUT_ACTION_ID + "." + ACTION_DESCRIPTION,
                parameters = @Parameter(name = TOKEN_PARAM_NAME, type = "string", description = SESSION_RESOURCE + TOKEN_PARAM_NAME + "." + PARAMETER_DESCRIPTION),
                errors = {
                    @ApiError(
                        code = 401,
                        description = SESSION_RESOURCE + ERROR_401_DESCRIPTION
                    )
                }
            ),
            name = LOGOUT_ACTION_ID,
            response = @Schema(schemaResource = "SessionResource.properties.names.schema.json")
        ),
        @Action(
            operationDescription = @Operation(
                description = SESSION_RESOURCE + REFRESH_ACTION_ID + "." + ACTION_DESCRIPTION,
                parameters = @Parameter(name = TOKEN_PARAM_NAME, type = "string", description = SESSION_RESOURCE + TOKEN_PARAM_NAME + "." + PARAMETER_DESCRIPTION),
                errors = {
                    @ApiError(
                        code = 401,
                        description = SESSION_RESOURCE + ERROR_401_DESCRIPTION
                    )
                }
            ),
            name = REFRESH_ACTION_ID,
            response = @Schema(schemaResource = "SessionResource.properties.names.schema.json")
        ),
        @Action(
            operationDescription = @Operation(
                description = SESSION_RESOURCE + GET_SESSION_PROPERTIES_ACTION_ID + "." + ACTION_DESCRIPTION,
                parameters = @Parameter(name = TOKEN_PARAM_NAME, type = "string", description = SESSION_RESOURCE + TOKEN_PARAM_NAME + "." + PARAMETER_DESCRIPTION),
                errors = {
                    @ApiError(
                        code = 401,
                        description = SESSION_RESOURCE + ERROR_401_DESCRIPTION
                    )
                }
            ),
            name = GET_SESSION_PROPERTIES_ACTION_ID,
            response = @Schema(schemaResource = "SessionResource.properties.names.schema.json")
        ),
        @Action(
            operationDescription = @Operation(
                description = SESSION_RESOURCE + UPDATE_SESSION_PROPERTIES_ACTION_ID + "." + ACTION_DESCRIPTION,
                parameters = @Parameter(name = TOKEN_PARAM_NAME, type = "string", description = SESSION_RESOURCE + TOKEN_PARAM_NAME + "." + PARAMETER_DESCRIPTION),
                errors = {
                    @ApiError(
                        code = 401,
                        description = SESSION_RESOURCE + ERROR_401_DESCRIPTION
                    )
                }
            ),
            name = UPDATE_SESSION_PROPERTIES_ACTION_ID,
            response = @Schema(schemaResource = "SessionResource.properties.names.schema.json")
        )
    })
    @Override
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
     * Handle the action specified by the user (i.e. one of those in the validActions set).
     * @param tokenId The id of the token.
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
     * <p>
     * all - (default) will query all Sessions across all servers.
     * [server-id] - will list the available Sessions on the named server.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Queries({
            @Query(
                    operationDescription = @Operation(
                            description = SESSION_RESOURCE + SERVER_QUERY_ID + "." + ID_QUERY_DESCRIPTION,
                            errors = {
                                    @ApiError(
                                            code = 401,
                                            description = SESSION_RESOURCE + ERROR_401_DESCRIPTION
                                    )
                            },
                            parameters = @Parameter(name = KEYWORD_SERVER_ID, type = "string", description = SESSION_RESOURCE +
                                    SERVER_QUERY_ID + "." + ID_QUERY + KEYWORD_SERVER_ID + "." + PARAMETER_DESCRIPTION,
                                    source = ParameterSource.ADDITIONAL)
                    ),
                    type = QueryType.ID,
                    id = SERVER_QUERY_ID
            ),
            @Query(
                    operationDescription = @Operation(
                            description = SESSION_RESOURCE + ALL_QUERY_ID + "." + ID_QUERY_DESCRIPTION,
                            errors = {
                                    @ApiError(
                                            code = 401,
                                            description = SESSION_RESOURCE + ERROR_401_DESCRIPTION
                                    )
                            }
                    ),
                    type = QueryType.ID,
                    id = ALL_QUERY_ID
            )
    })
    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request,
            QueryResourceHandler handler) {
        String id = request.getQueryId();

        Collection<SessionInfo> sessions;

        if (KEYWORD_ALL.equals(id)) {
            sessions =  sessionResourceUtil.generateAllSessions();
            LOGGER.message("SessionResource.queryCollection() :: Retrieved list of sessions for query.");
        } else {
            if (SERVER_QUERY_ID.equals(id)) {
                id = request.getAdditionalParameter(KEYWORD_SERVER_ID);
            }
            sessions = sessionResourceUtil.generateNamedServerSession(id);
            LOGGER.message("SessionResource.queryCollection() :: Retrieved list of specified servers for query.");
        }

        for (SessionInfo session : sessions) {
            handler.handleResource(newResourceResponse("Sessions", String.valueOf(currentTimeMillis()),
                    sessionResourceUtil.jsonValueOf(session)));
        }
        return newResultPromise(newQueryResponse());
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, String tokenIdHash,
            ActionRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String tokenHash, ReadRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(Context ctx, CreateRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context ctx, String resId,
            DeleteRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context ctx, String resId,
            PatchRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context ctx, String resId,
            UpdateRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }
}

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
 * Portions copyright 2025 3A Systems LLC.
 */
package org.forgerock.openam.core.rest.session;

import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.*;
import static org.forgerock.openam.session.SessionConstants.JSON_SESSION_REALM;
import static org.forgerock.openam.session.SessionConstants.JSON_SESSION_USERNAME;
import static org.forgerock.util.promise.Promises.newResultPromise;

import jakarta.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.service.SessionService;
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
import org.forgerock.openam.core.rest.session.action.ActionHandler;
import org.forgerock.openam.core.rest.session.action.GetSessionInfoActionHandler;
import org.forgerock.openam.core.rest.session.action.GetSessionPropertiesActionHandler;
import org.forgerock.openam.core.rest.session.action.LogoutActionHandler;
import org.forgerock.openam.core.rest.session.action.LogoutByHandleActionHandler;
import org.forgerock.openam.core.rest.session.action.RefreshActionHandler;
import org.forgerock.openam.core.rest.session.action.UpdateSessionPropertiesActionHandler;
import org.forgerock.openam.dpro.session.PartialSession;
import org.forgerock.openam.dpro.session.PartialSessionFactory;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.session.SessionPropertyWhitelist;
import org.forgerock.openam.utils.CrestQuery;
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
                resourceSchema = @Schema(fromType = PartialSession.class)
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
    public static final String LOGOUT_BY_HANDLE_ACTION_ID = "logoutByHandle";

    private final SessionPropertyWhitelist sessionPropertyWhitelist;

    private final Map<String, ActionHandler> actionHandlers;
    private final SessionResourceUtil sessionResourceUtil;
    private final SessionService sessionService;

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
            final SessionResourceUtil sessionResourceUtil, SessionPropertyWhitelist sessionPropertyWhitelist,
            SessionService sessionService, PartialSessionFactory partialSessionFactory) {
        this.sessionResourceUtil = sessionResourceUtil;
        this.sessionPropertyWhitelist = sessionPropertyWhitelist;
        this.sessionService = sessionService;
        actionHandlers = new CaseInsensitiveHashMap<>();
        actionHandlers.put(REFRESH_ACTION_ID,
                new RefreshActionHandler(ssoTokenManager, sessionResourceUtil));
        actionHandlers.put(LOGOUT_ACTION_ID, new LogoutActionHandler(ssoTokenManager, authUtilsWrapper));
        actionHandlers.put(GET_SESSION_INFO_ACTION_ID, new GetSessionInfoActionHandler(sessionResourceUtil,
                partialSessionFactory));
        actionHandlers.put(GET_SESSION_PROPERTIES_ACTION_ID,
                new GetSessionPropertiesActionHandler(sessionPropertyWhitelist, sessionResourceUtil));
        actionHandlers.put(UPDATE_SESSION_PROPERTIES_ACTION_ID,
                new UpdateSessionPropertiesActionHandler(sessionPropertyWhitelist, sessionResourceUtil));
        actionHandlers.put(LOGOUT_BY_HANDLE_ACTION_ID, new LogoutByHandleActionHandler());
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
                parameters = @Parameter(name = TOKEN_PARAM_NAME, type = "string",
                        description = SESSION_RESOURCE + TOKEN_PARAM_NAME + "." + PARAMETER_DESCRIPTION,
                        source = ParameterSource.ADDITIONAL),
                errors = {
                    @ApiError(
                        code = 401,
                        description = SESSION_RESOURCE + ERROR_401_DESCRIPTION
                    )
                }
            ),
            name = GET_SESSION_INFO_ACTION_ID,
            response = @Schema(fromType = PartialSession.class)
        ),
        @Action(
            operationDescription = @Operation(
                description = SESSION_RESOURCE + LOGOUT_ACTION_ID + "." + ACTION_DESCRIPTION,
                parameters = @Parameter(name = TOKEN_PARAM_NAME, type = "string",
                        description = SESSION_RESOURCE + TOKEN_PARAM_NAME + "." + PARAMETER_DESCRIPTION,
                        source = ParameterSource.ADDITIONAL),
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
                parameters = @Parameter(name = TOKEN_PARAM_NAME, type = "string",
                        description = SESSION_RESOURCE + TOKEN_PARAM_NAME + "." + PARAMETER_DESCRIPTION,
                        source = ParameterSource.ADDITIONAL),
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
                parameters = @Parameter(name = TOKEN_PARAM_NAME, type = "string",
                        description = SESSION_RESOURCE + TOKEN_PARAM_NAME + "." + PARAMETER_DESCRIPTION,
                        source = ParameterSource.ADDITIONAL),
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
                parameters = @Parameter(name = TOKEN_PARAM_NAME, type = "string",
                        description = SESSION_RESOURCE + TOKEN_PARAM_NAME + "." + PARAMETER_DESCRIPTION,
                        source = ParameterSource.ADDITIONAL),
                errors = {
                    @ApiError(
                        code = 401,
                        description = SESSION_RESOURCE + ERROR_401_DESCRIPTION
                    )
                }
            ),
            name = UPDATE_SESSION_PROPERTIES_ACTION_ID,
            response = @Schema(schemaResource = "SessionResource.properties.names.schema.json")
        ),
        @Action(
            operationDescription = @Operation(
                description = SESSION_RESOURCE + LOGOUT_BY_HANDLE_ACTION_ID + "." + ACTION_DESCRIPTION,
                errors = {
                    @ApiError(
                        code = 401,
                        description = SESSION_RESOURCE + ERROR_401_DESCRIPTION
                    )
                }
            ),
            name = LOGOUT_BY_HANDLE_ACTION_ID,
            request = @Schema(schemaResource = "SessionResource.logoutByHandle.request.schema.json"),
            response = @Schema(schemaResource = "SessionResource.logoutByHandle.response.schema.json")
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
     * Queries the session resources using the provided query filter. The query implementation currently only supports
     * the following searchfilters:
     * <ul>
     *     <li>username eq "foo" and realm eq "bar"</li>
     *     <li>realm eq "bar"</li>
     * </ul>
     *
     * i.e. searching using only the username is not supported currently.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Query(
            operationDescription = @Operation(
                    description = SESSION_RESOURCE + QUERY_DESCRIPTION,
                    errors = {
                            @ApiError(
                                    code = 400,
                                    description = SESSION_RESOURCE + QUERY + ERROR_400_DESCRIPTION),
                            @ApiError(
                                    code = 500,
                                    description = SESSION_RESOURCE + QUERY + ERROR_500_DESCRIPTION)}),
            queryableFields = {JSON_SESSION_USERNAME, JSON_SESSION_REALM},
            type = QueryType.FILTER
    )
    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request,
            QueryResourceHandler handler) {
        CrestQuery crestQuery = new CrestQuery(request.getQueryId(), request.getQueryFilter(), request.getFields());

        SSOTokenContext ssoTokenContext = context.asContext(SSOTokenContext.class);
        try {
            final Collection<PartialSession> matchingSessions = sessionService.getMatchingSessions(
                    ssoTokenContext.getCallerSession(), crestQuery);
            for (PartialSession matchingSession : matchingSessions) {
                handler.handleResource(newResourceResponse(null, String.valueOf(matchingSession.hashCode()),
                        matchingSession.asJson()));
            }
        } catch (IllegalArgumentException iae) {
            return new BadRequestException(iae.getMessage()).asPromise();
        } catch (SessionException se) {
            LOGGER.error("An error occurred whilst looking for matching sessions with filter '{}'", crestQuery, se);
            return new InternalServerErrorException("Unable to query for matching sessions").asPromise();
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

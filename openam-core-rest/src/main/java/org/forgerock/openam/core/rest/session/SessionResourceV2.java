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
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ACTION_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.CORE_TOKEN_RESOURCE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_401_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ID_QUERY;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ID_QUERY_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.PARAMETER_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.READ_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.SESSION_RESOURCE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.TITLE;
import static org.forgerock.openam.utils.Time.currentTimeMillis;
import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Map;

import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.idm.IdRepoException;
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
import org.forgerock.api.annotations.Read;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.enums.QueryType;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
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
import org.forgerock.openam.authentication.service.AuthUtilsWrapper;
import org.forgerock.openam.core.rest.session.action.ActionHandler;
import org.forgerock.openam.core.rest.session.action.LogoutActionHandler;
import org.forgerock.openam.core.rest.session.action.RefreshActionHandler;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.session.SessionConstants;
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
        ),
        pathParam = @Parameter(
                name = "userToken",
                type = "string",
                description = SESSION_RESOURCE + SessionResource.TOKEN_ID + "." + PARAMETER_DESCRIPTION
        )
)
public class SessionResourceV2 implements CollectionResourceProvider {

    private static final Debug LOGGER = Debug.getInstance(SessionConstants.SESSION_DEBUG);

    public static final String LOGOUT_ACTION_ID = "logout";
    public static final String REFRESH_ACTION_ID = "refresh";


    public static final String KEYWORD_ALL = "all";

    private final Map<String, ActionHandler> actionHandlers;
    private final SessionResourceUtil sessionResourceUtil;

    /**
     * Dependency Injection constructor allowing the SessionResource dependency to be provided.
     *
     * @param ssoTokenManager An instance of the SSOTokenManager.
     * @param  authUtilsWrapper An instance of the AuthUtilsWrapper.
     * @param sessionResourceUtil An instance of the SessionResourceUtil.
     */
    @Inject
    public SessionResourceV2(final SSOTokenManager ssoTokenManager, AuthUtilsWrapper authUtilsWrapper,
            final SessionResourceUtil sessionResourceUtil) {
        this.sessionResourceUtil = sessionResourceUtil;
        actionHandlers = new CaseInsensitiveHashMap<>();
        actionHandlers.put(REFRESH_ACTION_ID,
                new RefreshActionHandler(ssoTokenManager, sessionResourceUtil));
        actionHandlers.put(LOGOUT_ACTION_ID, new LogoutActionHandler(ssoTokenManager, authUtilsWrapper));
    }

    /**
     * {@inheritDoc}
     */
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    /**
     * Actions supported are:
     * <ul>
     * <li>{@link #LOGOUT_ACTION_ID}</li>
     * <li>{@link #REFRESH_ACTION_ID}</li>
     * </ul>
     *
     * @param context {@inheritDoc}
     * @param tokenId The SSO Token Id.
     * @param request {@inheritDoc}
     */
    @Actions({
        @Action(
                operationDescription = @Operation(
                        description = SESSION_RESOURCE + LOGOUT_ACTION_ID + "." + ACTION_DESCRIPTION,
                        errors = {
                                @ApiError(
                                        code = 401,
                                        description = SESSION_RESOURCE + ERROR_401_DESCRIPTION
                                )
                        },
                        parameters = @Parameter(name = "tokenId", type = "string", description = SESSION_RESOURCE + "tokenId." + PARAMETER_DESCRIPTION)
                ),
                name = LOGOUT_ACTION_ID,
                response = @Schema(schemaResource = "SessionResource.properties.names.schema.json")
        ),
        @Action(
                operationDescription = @Operation(
                        description = SESSION_RESOURCE + REFRESH_ACTION_ID + "." + ACTION_DESCRIPTION,
                        errors = {
                                @ApiError(
                                        code = 401,
                                        description = SESSION_RESOURCE + ERROR_401_DESCRIPTION
                                )
                        },
                        parameters = @Parameter(name = "tokenId", type = "string", description = SESSION_RESOURCE + "tokenId." + PARAMETER_DESCRIPTION)
                ),
                name = REFRESH_ACTION_ID,
                response = @Schema(schemaResource = "SessionResource.properties.names.schema.json")
        )})
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
                            description = SESSION_RESOURCE + "server." + ID_QUERY_DESCRIPTION,
                            errors = {
                                    @ApiError(
                                            code = 401,
                                            description = SESSION_RESOURCE + ERROR_401_DESCRIPTION
                                    )
                            },
                            parameters = @Parameter(name = "serverId", type = "string", description = SESSION_RESOURCE + "server." + ID_QUERY + "serverId." + PARAMETER_DESCRIPTION)
                    ),
                    type = QueryType.ID,
                    id = "server"
            ),
            @Query(
                    operationDescription = @Operation(
                            description = SESSION_RESOURCE + "all." + ID_QUERY_DESCRIPTION,
                            errors = {
                                    @ApiError(
                                            code = 401,
                                            description = SESSION_RESOURCE + ERROR_401_DESCRIPTION
                                    )
                            }
                    ),
                    type = QueryType.ID,
                    id = "all"
            )
    })
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request,
            QueryResourceHandler handler) {
        String id = request.getQueryId();

        Collection<SessionInfo> sessions;

        if (KEYWORD_ALL.equals(id)) {
            sessions =  sessionResourceUtil.generateAllSessions();
            LOGGER.message("SessionResource.queryCollection() :: Retrieved list of sessions for query.");
        } else {
            sessions = sessionResourceUtil.generateNamedServerSession(id);
            LOGGER.message("SessionResource.queryCollection() :: Retrieved list of specified servers for query.");
        }

        for (SessionInfo session : sessions) {
            handler.handleResource(newResourceResponse("Sessions", String.valueOf(currentTimeMillis()),
                    sessionResourceUtil.jsonValueOf(session)));
        }
        return newResultPromise(newQueryResponse());
    }

    /**
     * Perform a read operation against a named session.
     * <p>
     * {@inheritDoc}
     */
    @Read(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 500,
                            description = SESSION_RESOURCE + "error.unexpected.server.error." + DESCRIPTION)},
            description = SESSION_RESOURCE + READ_DESCRIPTION))
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String id, ReadRequest request) {
        JsonValue content;
        try {
            SSOToken ssoToken = sessionResourceUtil.getTokenWithoutResettingIdleTime(id);
            content = sessionResourceUtil.jsonValueOf(ssoToken);
        } catch (SSOException | IdRepoException e) {
           content = sessionResourceUtil.invalidSession();
        }
        return newResultPromise(newResourceResponse(id, String.valueOf(content.getObject().hashCode()), content));
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

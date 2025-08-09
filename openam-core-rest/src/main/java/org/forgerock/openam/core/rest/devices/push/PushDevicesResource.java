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

package org.forgerock.openam.core.rest.devices.push;

import static org.forgerock.json.resource.Responses.*;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DELETE_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.PATH_PARAM;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.QUERY_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.TITLE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.PUSH_DEVICES_RESOURCE;
import static org.forgerock.util.promise.Promises.*;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.forgerock.api.annotations.Action;
import org.forgerock.api.annotations.ApiError;
import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Delete;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Parameter;
import org.forgerock.api.annotations.Query;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.enums.QueryType;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.core.rest.devices.TwoFADevicesResource;
import org.forgerock.openam.core.rest.devices.UserDevicesResource;
import org.forgerock.openam.core.rest.devices.services.AuthenticatorDeviceServiceFactory;
import org.forgerock.openam.core.rest.devices.services.push.AuthenticatorPushService;
import org.forgerock.openam.core.rest.devices.services.push.AuthenticatorPushServiceFactory;
import org.forgerock.openam.rest.resource.ContextHelper;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * A user devices resource for Push authentication devices.
 *
 * @since 13.5.0
 * @see UserDevicesResource
 */
@CollectionProvider(
        details = @Handler(
                title = PUSH_DEVICES_RESOURCE + TITLE,
                description = PUSH_DEVICES_RESOURCE + DESCRIPTION,
                mvccSupported = true,
                parameters = {
                        @Parameter(
                                name = "user",
                                type = "string",
                                description = PUSH_DEVICES_RESOURCE + "pathparams.user")},
                resourceSchema = @Schema(schemaResource = "PushDevicesResource.schema.json")),
        pathParam = @Parameter(
                name = "uuid",
                type = "string",
                description = PUSH_DEVICES_RESOURCE + PATH_PARAM + DESCRIPTION))
public class PushDevicesResource extends TwoFADevicesResource<PushDevicesDao> {

    private final AuthenticatorDeviceServiceFactory<AuthenticatorPushService> pushServiceFactory;
    private final Debug debug;

    /**
     * Construct a new PushDevicesResource endpoint.
     *
     * @param dao The data access object for PushDevices.
     * @param helper The ContextHelper used to determine the URL's subject.
     * @param debug A debug instance for logging.
     * @param pushServiceFactory The Push Service Factory used to get the push service for this realm.
     */
    @Inject
    public PushDevicesResource(PushDevicesDao dao, ContextHelper helper, @Named("frRest") Debug debug,
                               @Named(AuthenticatorPushServiceFactory.FACTORY_NAME)
                                   AuthenticatorDeviceServiceFactory<AuthenticatorPushService> pushServiceFactory) {
        super(dao, helper);
        this.debug = debug;
        this.pushServiceFactory = pushServiceFactory;
    }

    /**
     * Only supports the "reset" action.
     *
     * The "reset" action will remove all Push devices from the user's profile attributes.
     *
     * A valid response will take the form:
     *
     * { "result" : true }
     */
    @Override
    @Action(name = "resetPushDevice",
            operationDescription = @Operation(
                    errors = {
                            @ApiError(
                                    code = 500,
                                    description = PUSH_DEVICES_RESOURCE + "error.unexpected.server.error.description")},
                    description = PUSH_DEVICES_RESOURCE + QUERY_DESCRIPTION),
            request = @Schema(),
            response = @Schema(schemaResource = "PushDevicesResource.action.validate.schema.json"))
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest request) {

        try {
            final AMIdentity identity = getUserIdFromUri(context); //could be admin
            final AuthenticatorPushService realmPushService = pushServiceFactory.create(getRealm(context));

            switch (request.getAction()) {
                case RESET: //deletes their profile attribute
                    try {
                        realmPushService.removeAllUserDevices(identity);
                        return newResultPromise(newActionResponse(JsonValueBuilder.jsonValue().put(RESULT, true)
                                .build()));

                    } catch (SSOException | IdRepoException e) {
                        debug.error("PushDevicesResource :: Action - Unable to reset identity attributes", e);
                        return new InternalServerErrorException().asPromise();
                    }
                default:
                    return new NotSupportedException().asPromise();
            }

        } catch (SMSException e) {
            debug.error("PushDevicesResource :: Action - Unable to communicate with the SMS.", e);
            return new InternalServerErrorException().asPromise();
        } catch (SSOException | InternalServerErrorException e) {
            debug.error("PushDevicesResource :: Action - Unable to retrieve identity data from request context", e);
            return new InternalServerErrorException().asPromise();
        }
    }

    @Override
    @Delete(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 500,
                            description = PUSH_DEVICES_RESOURCE + "error.unexpected.server.error.description")},
            description = PUSH_DEVICES_RESOURCE + DELETE_DESCRIPTION))
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context context, String resourceId,
                                                                       DeleteRequest request) {
        return super.deleteInstance(context, resourceId, request);
    }

    @Override
    @Query(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 500,
                            description = PUSH_DEVICES_RESOURCE + "error.unexpected.server.error.description")},
            description = PUSH_DEVICES_RESOURCE + QUERY_DESCRIPTION),
            type = QueryType.FILTER,
            queryableFields = "*"
    )
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request,
                                                                     QueryResourceHandler handler) {
        return super.queryCollection(context, request, handler);
    }

}
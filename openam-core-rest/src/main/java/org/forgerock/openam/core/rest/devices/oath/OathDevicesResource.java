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
 * Copyright 2015-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.core.rest.devices.oath;

import static org.forgerock.json.resource.Responses.*;
import static org.forgerock.openam.core.rest.devices.services.oath.AuthenticatorOathServiceFactory.*;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DELETE_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.PATH_PARAM;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.QUERY_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.TITLE;
import static org.forgerock.util.promise.Promises.*;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.OATH_DEVICES_RESOURCE;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.forgerock.api.annotations.Action;
import org.forgerock.api.annotations.Actions;
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
import org.forgerock.openam.core.rest.devices.services.oath.AuthenticatorOathService;
import org.forgerock.openam.rest.resource.ContextHelper;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * A user devices resource for OATH authentication devices.
 *
 * @since 13.0.0
 * @see UserDevicesResource
 */
@CollectionProvider(
        details = @Handler(
                title = OATH_DEVICES_RESOURCE + TITLE,
                description = OATH_DEVICES_RESOURCE + DESCRIPTION,
                mvccSupported = true,
                parameters = {
                        @Parameter(
                                name = "user",
                                type = "string",
                                description = OATH_DEVICES_RESOURCE + "pathparams.user")},
                resourceSchema = @Schema(schemaResource = "OathDevicesResource.schema.json")),
        pathParam = @Parameter(
                name = "uuid",
                type = "string",
                description = OATH_DEVICES_RESOURCE + PATH_PARAM + DESCRIPTION))
public class OathDevicesResource extends TwoFADevicesResource<OathDevicesDao> {

    private final AuthenticatorDeviceServiceFactory<AuthenticatorOathService> oathServiceFactory;
    private final Debug debug;

    /**
     * Constructor that sets up the data accessing object, context helpers and the factory from which to produce
     * services appropriate to each realm.
     *
     * @param dao For communicating with the datastore.
     * @param helper To understand the context of requests.
     * @param debug For debug purposes.
     * @param oathServiceFactory The factory used to generate appropriate services.
     */
    @Inject
    public OathDevicesResource(OathDevicesDao dao, ContextHelper helper,  @Named("frRest") Debug debug,
                               @Named(FACTORY_NAME)
                                   AuthenticatorDeviceServiceFactory<AuthenticatorOathService> oathServiceFactory) {
        super(dao, helper);
        this.debug = debug;
        this.oathServiceFactory = oathServiceFactory;
    }

    @Override
    @Actions({
            @Action(name = "skipOathDevice",
                    operationDescription = @Operation(
                            errors = {
                                    @ApiError(
                                            code = 500,
                                            description = OATH_DEVICES_RESOURCE + "error.unexpected.server.error." +
                                                    "description")},
                            description = OATH_DEVICES_RESOURCE + "action.skipOathDevice." + DESCRIPTION),
                    request = @Schema(schemaResource = "OathDevicesResource.action.skip.request.schema.json"),
                    response = @Schema(schemaResource = "OathDevicesResource.action.skip.response.schema.json")),
            @Action(name = "checkOathDevice",
                    operationDescription = @Operation(
                            errors = {
                                    @ApiError(
                                            code = 500,
                                            description = OATH_DEVICES_RESOURCE + "error.unexpected.server.error." +
                                                    "description")},
                            description = OATH_DEVICES_RESOURCE + "action.checkOathDevice." + DESCRIPTION),
                    request = @Schema(schemaResource = "OathDevicesResource.action.check.request.schema.json"),
                    response = @Schema(schemaResource = "OathDevicesResource.action.check.response.schema.json")),
            @Action(name = "resetOathDevice",
                    operationDescription = @Operation(
                            errors = {
                                    @ApiError(
                                            code = 500,
                                            description = OATH_DEVICES_RESOURCE + "error.unexpected.server.error." +
                                                    "description")},
                            description = OATH_DEVICES_RESOURCE + "action.resetOathDevice." + DESCRIPTION),
                    request = @Schema(schemaResource = "OathDevicesResource.action.reset.request.schema.json"),
                    response = @Schema(schemaResource = "OathDevicesResource.action.reset.response.schema.json"))})
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest request) {

        try {
            final AMIdentity identity = getUserIdFromUri(context); //could be admin
            final AuthenticatorOathService realmOathService = oathServiceFactory.create(getRealm(context));

            switch (request.getAction()) {
                case SKIP:

                    try {
                        final boolean setValue = request.getContent().get(VALUE).asBoolean();

                        realmOathService.setUserSkipOath(identity,
                                setValue ? AuthenticatorOathService.SKIPPABLE : AuthenticatorOathService.NOT_SKIPPABLE);
                        return newResultPromise(newActionResponse(JsonValueBuilder.jsonValue().build()));

                    } catch (SSOException | IdRepoException e) {
                        debug.error("OathDevicesResource :: SKIP action - Unable to set value in user store.", e);
                        return new InternalServerErrorException().asPromise();
                    }
                case CHECK:
                    try {
                        final Set resultSet = identity.getAttribute(realmOathService.getSkippableAttributeName());
                        boolean result = false;

                        if (CollectionUtils.isNotEmpty(resultSet)) {
                            String tmp = (String) resultSet.iterator().next();
                            int resultInt = Integer.valueOf(tmp);
                            if (resultInt == AuthenticatorOathService.SKIPPABLE) {
                                result = true;
                            }
                        }

                        return newResultPromise(newActionResponse(JsonValueBuilder.jsonValue().put(RESULT, result).build()));

                    } catch (SSOException | IdRepoException e) {
                        debug.error("OathDevicesResource :: CHECK action - Unable to read value from user store.", e);
                        return new InternalServerErrorException().asPromise();
                    }
                case RESET: //sets their 'skippable' selection to default (NOT_SET) and deletes their profiles attribute
                    try {

                        realmOathService.setUserSkipOath(identity, AuthenticatorOathService.NOT_SET);
                        realmOathService.removeAllUserDevices(identity);

                        return newResultPromise(newActionResponse(JsonValueBuilder.jsonValue().put(RESULT, true).build()));

                    } catch (SSOException | IdRepoException e) {
                        debug.error("OathDevicesResource :: Action - Unable to reset identity attributes", e);
                        return new InternalServerErrorException().asPromise();
                    }
                default:
                    return new NotSupportedException().asPromise();
            }

        } catch (SMSException e) {
            debug.error("OathDevicesResource :: Action - Unable to communicate with the SMS.", e);
            return new InternalServerErrorException().asPromise();
        } catch (SSOException | InternalServerErrorException e) {
            debug.error("OathDevicesResource :: Action - Unable to retrieve identity data from request context", e);
            return new InternalServerErrorException().asPromise();
        }
    }

    @Override
    @Delete(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 500,
                            description = OATH_DEVICES_RESOURCE + "error.unexpected.server.error.description")},
            description = OATH_DEVICES_RESOURCE + DELETE_DESCRIPTION))
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context context, String resourceId,
            DeleteRequest request) {

        try {
            final AuthenticatorOathService realmOathService = oathServiceFactory.create(getRealm(context));
            final AMIdentity identity = getUserIdFromUri(context); //could be admin

            Promise<ResourceResponse, ResourceException> promise = super.deleteInstance(context, resourceId, request);//make sure we successfully delete
            realmOathService.setUserSkipOath(identity, AuthenticatorOathService.NOT_SET); //then reset the skippable attr
            return promise;
        } catch (InternalServerErrorException | SMSException e) {
            debug.error("OathDevicesResource :: Delete - Unable to communicate with the SMS.", e);
            return new InternalServerErrorException().asPromise();
        } catch (SSOException | IdRepoException e) {
            debug.error("OathDevicesResource :: Delete - Unable to reset identity attributes", e);
            return new InternalServerErrorException().asPromise();
        }
    }

    @Override
    @Query(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 500,
                            description = OATH_DEVICES_RESOURCE + "error.unexpected.server.error.description")},
            description = OATH_DEVICES_RESOURCE + QUERY_DESCRIPTION),
            type = QueryType.FILTER,
            queryableFields = "*"
    )
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request,
                                                                     QueryResourceHandler handler) {
        return super.queryCollection(context, request, handler);
    }

}
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
 * Copyright 2014-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.core.rest.devices.deviceprint;

import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DELETE_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.PATH_PARAM;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.QUERY_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.TITLE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.TRUSTED_DEVICES_RESOURCE;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import jakarta.inject.Inject;

import org.forgerock.api.annotations.ApiError;
import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Delete;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Parameter;
import org.forgerock.api.annotations.Query;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.enums.QueryType;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.core.rest.devices.UserDevicesResource;
import org.forgerock.openam.rest.resource.ContextHelper;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * REST resource for a user's trusted devices.
 *
 * @since 12.0.0
 */
@CollectionProvider(
        details = @Handler(
                title = TRUSTED_DEVICES_RESOURCE + TITLE,
                description = TRUSTED_DEVICES_RESOURCE + DESCRIPTION,
                mvccSupported = true,
                parameters = {
                        @Parameter(
                                name = "user",
                                type = "string",
                                description = TRUSTED_DEVICES_RESOURCE + "pathparams.user")},
                resourceSchema = @Schema(schemaResource = "TrustedDevicesResource.schema.json")),
        pathParam = @Parameter(
                name = "uuid",
                type = "string",
                description = TRUSTED_DEVICES_RESOURCE + PATH_PARAM + DESCRIPTION))
public class TrustedDevicesResource extends UserDevicesResource<TrustedDevicesDao> {

    private static final DateFormat DATE_PARSER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final DateFormat DATE_FORMATTER =
            SimpleDateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
    private static final String LAST_SELECTED_DATE_KEY = "lastSelectedDate";

    /**
     * Constructs a new TrustedDevicesResource.
     *
     * @param dao An instance of the {@code TrustedDevicesDao}.
     * @param contextHelper An instance of the {@code ContextHelper}.
     */
    @Inject
    public TrustedDevicesResource(TrustedDevicesDao dao, ContextHelper contextHelper) {
        super(dao, contextHelper);
    }

    protected ResourceResponse convertValue(JsonValue profile) throws ParseException {
        final JsonValue lastSelectedDateJson = profile.get(LAST_SELECTED_DATE_KEY);
        final Date lastSelectedDate;
        final String formatted;

        if (lastSelectedDateJson.isString()) {
            synchronized (DATE_PARSER) {
                lastSelectedDate = DATE_PARSER.parse(lastSelectedDateJson.asString());
            }
        } else {
            lastSelectedDate = new Date(lastSelectedDateJson.asLong());
        }

        synchronized (DATE_FORMATTER) {
            formatted = DATE_FORMATTER.format(lastSelectedDate);
        }

        profile.put(LAST_SELECTED_DATE_KEY, formatted);
        return newResourceResponse(profile.get("name").asString(), profile.hashCode() + "", profile);
    }

    @Override
    @Delete(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 500,
                            description = TRUSTED_DEVICES_RESOURCE + "error.unexpected.server.error.description")},
            description = TRUSTED_DEVICES_RESOURCE + DELETE_DESCRIPTION))
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context context, String resourceId,
                                                                       DeleteRequest request) {
        return super.deleteInstance(context, resourceId, request);
    }

        @Override
    @Query(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 500,
                            description = TRUSTED_DEVICES_RESOURCE + "error.unexpected.server.error.description")},
            description = TRUSTED_DEVICES_RESOURCE + QUERY_DESCRIPTION),
            type = QueryType.FILTER,
            queryableFields = "*"
    )
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request,
                                                                     QueryResourceHandler handler) {
        return super.queryCollection(context, request, handler);
    }

}

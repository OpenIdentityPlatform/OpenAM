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
 * Copyright 2015 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */
package org.forgerock.openam.core.rest.record;

import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.*;

import jakarta.inject.Inject;

import org.forgerock.api.annotations.Action;
import org.forgerock.api.annotations.Actions;
import org.forgerock.api.annotations.ApiError;
import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Schema;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.utils.JsonObject;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

import com.sun.identity.shared.debug.Debug;

/**
 * Rest endpoint for the record feature.
 */
@CollectionProvider(details = @Handler(mvccSupported = false, title = RECORD_RESOURCE + TITLE,
        description = RECORD_RESOURCE + DESCRIPTION))
public class RecordResource {

    // Json label for the recording status
    private static final String STATUS_LABEL = "recording";
    private static final String RECORD_LABEL = "record";

    private final Debug debug = Debug.getInstance(RecordConstants.DEBUG_INSTANCE_NAME);

    // Debug recorder implements the different actions
    private DebugRecorder debugRecorder;

    @Inject
    public RecordResource(DebugRecorder debugRecorder) {
        this.debugRecorder = debugRecorder;
    }

    /**
     * Start action
     *
     * @return
     */
    @Action(name = "start",
            operationDescription = @Operation(
                    errors = {
                            @ApiError(
                                    code = 400,
                                    description = RECORD_RESOURCE + ERROR_400_DESCRIPTION)},
                    description = RECORD_RESOURCE + "operation.start.description"),
            request = @Schema(schemaResource = "RecordPropertiesRequest.schema.json"),
            response = @Schema(schemaResource = "RecordStatus.schema.json"))
    public Promise<ActionResponse, ResourceException> actionStart(Context serverContext,
                                                                   ActionRequest actionRequest) {
        JsonValue jsonValue = actionRequest.getContent();
        try {
            debugRecorder.startRecording(jsonValue);
            return actionStatus(serverContext, actionRequest);
        } catch (JsonValueException e) {
            debug.message("Record json '{}' can't be parsed", jsonValue, e);
            return new BadRequestException("Record json '" + jsonValue + "' can't be parsed", e).asPromise();
        } catch (RecordException e) {
            debug.message("Record can't be started.", e);
            return new BadRequestException("Record can't be started.", e).asPromise();
        }
    }

    /**
     * Status action
     *
     * @return
     */
    @Action(name = "status",
            operationDescription = @Operation(
                    description = RECORD_RESOURCE + "operation.status.description"),
            response = @Schema(schemaResource = "RecordStatus.schema.json"))
    public Promise<ActionResponse, ResourceException> actionStatus(Context serverContext,
                                                                    ActionRequest actionRequest) {

        Record currentRecord = debugRecorder.getCurrentRecord();
        JsonObject jsonValue = JsonValueBuilder.jsonValue();
        if (currentRecord != null) {
            jsonValue.put(STATUS_LABEL, true);
            jsonValue.put(RECORD_LABEL, currentRecord.exportJson().asMap());
        } else {
            jsonValue.put(STATUS_LABEL, false);
        }
        return newResultPromise(newActionResponse(jsonValue.build()));
    }

    /**
     * Stop action
     *
     * @return
     */
    @Action(name = "stop",
            operationDescription = @Operation(
                    errors = {
                            @ApiError(
                                    code = 400,
                                    description = RECORD_RESOURCE + ERROR_400_DESCRIPTION)},
                    description = RECORD_RESOURCE + "operation.stop.description"),
            response = @Schema(schemaResource = "RecordStatus.schema.json"))
    public Promise<ActionResponse, ResourceException> actionStop(Context serverContext,
                                                                  ActionRequest actionRequest) {
        try {
            Record record = debugRecorder.stopRecording();
            if (record == null) {
                return new BadRequestException("No record or it's already stopped.").asPromise();
            } else {
                JsonObject result = JsonValueBuilder.jsonValue();
                result.put(STATUS_LABEL, false);
                result.put(RECORD_LABEL, record.exportJson().asMap());
                return newResultPromise(newActionResponse(result.build()));
            }
        } catch (RecordException e) {
            debug.message("Record can't be stopped.", e);
            return new BadRequestException("Record can't be stopped.", e).asPromise();
        }
    }
}

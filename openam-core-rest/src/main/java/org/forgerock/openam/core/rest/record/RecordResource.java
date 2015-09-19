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
 */
package org.forgerock.openam.core.rest.record;

import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.inject.Inject;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.services.context.Context;
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
import org.forgerock.util.promise.Promise;

/**
 * Rest endpoint for the record feature.
 */
public class RecordResource implements CollectionResourceProvider {

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

    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(Context serverContext,
            ActionRequest actionRequest) {
        switch (actionRequest.getAction()) {
            case RecordConstants.START_ACTION:
                return actionStart(actionRequest.getContent());
            case RecordConstants.STATUS_ACTION:
                return actionStatus();
            case RecordConstants.STOP_ACTION:
                return actionStop();
            default:
                return RestUtils.generateUnsupportedOperation();
        }
    }

    /**
     * Start action
     *
     * @param jsonValue
     * @return
     */
    private Promise<ActionResponse, ResourceException> actionStart(JsonValue jsonValue) {
        try {
            debugRecorder.startRecording(jsonValue);
            return actionStatus();
        } catch (JsonValueException e) {
            debug.message("Record json '{}' can't be parsed", jsonValue, e);
            return new BadRequestException("Record json '" + jsonValue + "' can't be parsed", e).asPromise();
        } catch (RecordException e) {
            debug.message("Record can't be start.", e);
            return new BadRequestException("Record can't be start.", e).asPromise();
        }
    }

    /**
     * Status action
     *
     * @return
     */
    private Promise<ActionResponse, ResourceException> actionStatus() {

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
    private Promise<ActionResponse, ResourceException> actionStop() {
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

    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context serverContext, String s,
            ActionRequest actionRequest) {
        return RestUtils.generateUnsupportedOperation();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(Context serverContext,
            CreateRequest createRequest) {
        return RestUtils.generateUnsupportedOperation();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context serverContext, String s,
            DeleteRequest deleteRequest) {
        return RestUtils.generateUnsupportedOperation();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context serverContext, String s,
            PatchRequest patchRequest) {
        return RestUtils.generateUnsupportedOperation();
    }

    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(Context serverContext, QueryRequest queryRequest,
            QueryResourceHandler queryResultHandler) {
        return RestUtils.generateUnsupportedOperation();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context serverContext, String s,
            ReadRequest readRequest) {
        return RestUtils.generateUnsupportedOperation();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context serverContext, String s,
            UpdateRequest updateRequest) {
        return RestUtils.generateUnsupportedOperation();
    }
}

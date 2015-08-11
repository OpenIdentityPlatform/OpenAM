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
package org.forgerock.openam.rest.record;


import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.forgerockrest.RestUtils;
import org.forgerock.openam.utils.JsonObject;
import org.forgerock.openam.utils.JsonValueBuilder;

import javax.inject.Inject;

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
    public void actionCollection(ServerContext serverContext, ActionRequest actionRequest, ResultHandler<JsonValue>
            resultHandler) {
        switch (actionRequest.getAction()) {
            case RecordConstants.START_ACTION:
                actionStart(actionRequest.getContent(), resultHandler);
                break;
            case RecordConstants.STATUS_ACTION:
                actionStatus(resultHandler);
                break;
            case RecordConstants.STOP_ACTION:
                actionStop(resultHandler);
                break;
            default:
                RestUtils.generateUnsupportedOperation(resultHandler);
        }
    }

    /**
     * Start action
     *
     * @param jsonValue
     * @param resultHandler
     */
    private void actionStart(JsonValue jsonValue, ResultHandler<JsonValue> resultHandler) {
        try {
            debugRecorder.startRecording(jsonValue);
            actionStatus(resultHandler);
        } catch (JsonValueException e) {
            debug.message("Record json '{}' can't be parsed", jsonValue, e);
            resultHandler.handleError(ResourceException.getException(ResourceException.BAD_REQUEST, "Record json '" +
                    jsonValue + "' can't be parsed", e));
        } catch (RecordException e) {
            debug.message("Record can't be start.", e);
            resultHandler.handleError(ResourceException.getException(ResourceException.BAD_REQUEST,
                    "Record can't be start.", e));
        }
    }

    /**
     * Status action
     *
     * @param resultHandler
     */
    private void actionStatus(ResultHandler<JsonValue> resultHandler) {

        Record currentRecord = debugRecorder.getCurrentRecord();
        JsonObject jsonValue = JsonValueBuilder.jsonValue();
        if (currentRecord != null) {
            jsonValue.put(STATUS_LABEL, true);
            jsonValue.put(RECORD_LABEL, currentRecord.exportJson().asMap());
        } else {
            jsonValue.put(STATUS_LABEL, false);
        }
        resultHandler.handleResult(jsonValue.build());
    }

    /**
     * Stop action
     *
     * @param resultHandler
     */
    private void actionStop(ResultHandler<JsonValue> resultHandler) {
        try {
            Record record = debugRecorder.stopRecording();
            if (record == null) {
                resultHandler.handleError(ResourceException.getException(ResourceException.BAD_REQUEST, "No record or" +
                        " it's already stopped."));
            } else {
                JsonObject result = JsonValueBuilder.jsonValue();
                result.put(STATUS_LABEL, false);
                result.put(RECORD_LABEL, record.exportJson().asMap());
                resultHandler.handleResult(result.build());
            }
        } catch (RecordException e) {
            debug.message("Record can't be stopped.", e);
            resultHandler.handleError(ResourceException.getException(ResourceException.BAD_REQUEST,
                    "Record can't be stopped.", e));
        }
    }

    @Override
    public void actionInstance(ServerContext serverContext, String s, ActionRequest actionRequest,
            ResultHandler<JsonValue> resultHandler) {
        RestUtils.generateUnsupportedOperation(resultHandler);
    }

    @Override
    public void createInstance(ServerContext serverContext, CreateRequest createRequest, ResultHandler<Resource>
            resultHandler) {
        RestUtils.generateUnsupportedOperation(resultHandler);
    }

    @Override
    public void deleteInstance(ServerContext serverContext, String s, DeleteRequest deleteRequest,
            ResultHandler<Resource> resultHandler) {
        RestUtils.generateUnsupportedOperation(resultHandler);
    }

    @Override
    public void patchInstance(ServerContext serverContext, String s, PatchRequest patchRequest,
            ResultHandler<Resource> resultHandler) {
        RestUtils.generateUnsupportedOperation(resultHandler);
    }

    @Override
    public void queryCollection(ServerContext serverContext, QueryRequest queryRequest, QueryResultHandler
            queryResultHandler) {
        RestUtils.generateUnsupportedOperation(queryResultHandler);
    }

    @Override
    public void readInstance(ServerContext serverContext, String s, ReadRequest readRequest, ResultHandler<Resource>
            resultHandler) {
        RestUtils.generateUnsupportedOperation(resultHandler);
    }

    @Override
    public void updateInstance(ServerContext serverContext, String s, UpdateRequest updateRequest,
            ResultHandler<Resource> resultHandler) {
        RestUtils.generateUnsupportedOperation(resultHandler);
    }
}

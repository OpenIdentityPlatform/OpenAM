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

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;

import org.forgerock.openam.utils.IOUtils;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.util.thread.ExecutorServiceFactory;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

public class RecordResourceTest extends DebugTestTemplate {

    public static final String RECORD_DIRECTORY = "/record/";

    private RecordResource recordResource;
    private DebugRecorder debugRecorder;
    @Mock
    private ActionRequest request;
    @Mock
    ServerContext serverContext;
    @Mock
    ResultHandler<JsonValue> resultHandler;

    @BeforeMethod
    public void setup() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        debugRecorder = new DefaultDebugRecorder(new ExecutorServiceFactory(Mockito.mock
                (org.forgerock.util.thread.listener.ShutdownManager.class)));
        this.recordResource = new RecordResource(debugRecorder);
    }

    @Test
    public void startRecording() throws IOException {
        // Given...
        JsonValue jsonRecordProperties = JsonValueBuilder.toJsonValue(
                IOUtils.getFileContentFromClassPath(RecordResourceTest.class,
                        RECORD_DIRECTORY + "startSimpleRecord.json"));
        given(request.getAction()).willReturn("start");
        given(request.getContent()).willReturn(jsonRecordProperties);

        // When...
        recordResource.actionCollection(serverContext, request, resultHandler);

        // Then...
        verify(request).getAction();

        ArgumentCaptor<JsonValue> captor = ArgumentCaptor.forClass(JsonValue.class);
        verify(resultHandler).handleResult(captor.capture());


        RecordProperties recordPropertiesInput = RecordProperties.fromJson(jsonRecordProperties);
        RecordProperties recordPropertiesOutput = RecordProperties.fromJson(captor.getValue().get("record"));

        Assert.assertEquals(recordPropertiesInput, recordPropertiesOutput);
    }


    @Test
    public void stopRecording() throws IOException, RecordException {

        JsonValue jsonRecordProperties = JsonValueBuilder.toJsonValue(
                IOUtils.getFileContentFromClassPath(RecordResourceTest.class,
                        RECORD_DIRECTORY + "startSimpleRecord.json"));
        debugRecorder.startRecording(jsonRecordProperties);

        // Given...
        given(request.getAction()).willReturn("stop");

        // When...
        recordResource.actionCollection(serverContext, request, resultHandler);

        // Then...

        ArgumentCaptor<JsonValue> captor = ArgumentCaptor.forClass(JsonValue.class);
        verify(resultHandler).handleResult(captor.capture());

        RecordProperties recordPropertiesInput = RecordProperties.fromJson(jsonRecordProperties);
        RecordProperties recordPropertiesOutput = RecordProperties.fromJson(captor.getValue().get("record"));

        Assert.assertEquals(recordPropertiesInput, recordPropertiesOutput);
    }

}

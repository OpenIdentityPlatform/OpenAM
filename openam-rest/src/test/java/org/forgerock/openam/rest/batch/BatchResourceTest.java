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
package org.forgerock.openam.rest.batch;

import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.errors.ExceptionMappingHandler;
import org.forgerock.openam.rest.batch.helpers.Requester;
import org.forgerock.openam.scripting.ScriptEvaluator;
import org.forgerock.openam.scripting.service.ScriptingServiceFactory;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.mockito.Mockito;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * Due to the complexity opened up by the scripting
 */
public class BatchResourceTest {

    BatchResource batchResource;

    ScriptEvaluator mockScriptEvaluator = mock(ScriptEvaluator.class);
    ScriptingServiceFactory mockServiceFactory = mock(ScriptingServiceFactory.class);
    Debug mockDebug = mock(Debug.class);
    Requester mockRequester = mock(Requester.class);
    ExceptionMappingHandler mockMappingHandler = mock(ExceptionMappingHandler.class);

    @BeforeTest
    public void theSetUp() { //you need this
        batchResource = new BatchResource(mockScriptEvaluator, mockServiceFactory, mockDebug,
                mockMappingHandler, mockRequester);
    }

    @Test
    public void shouldRejectNonBatchActions() {
        //given
        ServerContext mockContext = Mockito.mock(ServerContext.class);
        ActionRequest mockRequest = mock(ActionRequest.class);
        ResultHandler<JsonValue> mockHandler = mock(ResultHandler.class);

        given(mockRequest.getAction()).willReturn("false");

        //when
        batchResource.actionCollection(mockContext, mockRequest, mockHandler);

        //then
        verify(mockHandler, times(1)).handleError(any(NotSupportedException.class));
    }

    @Test
    public void shouldRejectNullScriptId() {
        //given
        ServerContext mockContext = Mockito.mock(ServerContext.class);
        ResultHandler<JsonValue> mockHandler = mock(ResultHandler.class);
        ActionRequest action = Requests.newActionRequest("batch", "batch");
        action.setContent(JsonValueBuilder.toJsonValue("{ \"notScriptId\" : \"blah\" }"));

        //when
        batchResource.actionCollection(mockContext, action, mockHandler);

        //then
        verify(mockHandler, times(1)).handleError(any(ResourceException.class));
    }

}

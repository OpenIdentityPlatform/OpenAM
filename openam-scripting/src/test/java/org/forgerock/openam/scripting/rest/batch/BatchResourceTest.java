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
package org.forgerock.openam.scripting.rest.batch;
    
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.forgerock.json.resource.Requests.newActionRequest;

import org.forgerock.services.context.Context;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.errors.ExceptionMappingHandler;
import org.forgerock.openam.scripting.rest.batch.helpers.Requester;
import org.forgerock.openam.scripting.ScriptEvaluator;
import org.forgerock.openam.scripting.service.ScriptingServiceFactory;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.util.promise.ExceptionHandler;
import org.forgerock.util.promise.Promise;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.sun.identity.shared.debug.Debug;


/**
 * Due to the complexity opened up by the scripting
 */
public class BatchResourceTest {
    
    BatchResource batchResource;

    @Mock
    ScriptEvaluator mockScriptEvaluator;
    @Mock
    ScriptingServiceFactory mockServiceFactory;
    @Mock
    Debug mockDebug;
    @Mock
    Requester mockRequester;
	@Mock
    ExceptionMappingHandler mockMappingHandler;

    @BeforeTest
    public void theSetUp() { //you need this
    	MockitoAnnotations.initMocks(this);
        batchResource = new BatchResource(mockScriptEvaluator, mockServiceFactory, mockDebug,mockMappingHandler, mockRequester);
    }

    @Test
    public void shouldRejectNonBatchActions() {
        //given
        Context mockContext = Mockito.mock(Context.class);
        ActionRequest mockRequest = mock(ActionRequest.class);
        
        given(mockRequest.getAction()).willReturn("false");
        
        @SuppressWarnings("unchecked")
        ExceptionHandler<ResourceException> handler =  mock(ExceptionHandler.class);

        //when
        Promise<ActionResponse, ResourceException> result = batchResource.actionCollection(mockContext, mockRequest);
        result.thenOnException(handler);
        
        //then
        verify(handler, times(1)).handleException(any(ResourceException.class));
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void shouldRejectNullScriptId() {
        
        //given
        Context mockContext = mock(Context.class);
        ActionRequest action = newActionRequest("batch", "batch");
        action.setContent(JsonValueBuilder.toJsonValue("{ \"notScriptId\" : \"blah\" }"));
        @SuppressWarnings("unchecked")
        ExceptionHandler<ResourceException> handler = mock(ExceptionHandler.class);
        
        //when
        Promise<ActionResponse, ResourceException> promise = batchResource.actionCollection(mockContext, action);
        promise.thenOnException(handler);
        
        //then
        verify(handler, times(1)).handleException(any(ResourceException.class));
        verifyNoMoreInteractions(handler);

    }

}

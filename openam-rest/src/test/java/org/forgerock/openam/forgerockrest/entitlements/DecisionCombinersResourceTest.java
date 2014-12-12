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
* Copyright 2014 ForgeRock AS.
*/
package org.forgerock.openam.forgerockrest.entitlements;

import com.sun.identity.entitlement.EntitlementCombiner;
import com.sun.identity.shared.debug.Debug;
import java.util.Map;
import javax.security.auth.Subject;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.schema.JsonSchema;
import static org.fest.assertions.Assertions.assertThat;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.entitlement.ConditionTypeRegistry;
import org.forgerock.openam.entitlement.EntitlementRegistry;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.rest.resource.SubjectContext;
import org.mockito.ArgumentCaptor;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DecisionCombinersResourceTest {

    DecisionCombinersResource testResource;
    ObjectMapper mockMapper = mock(ObjectMapper.class);
    EntitlementRegistry mockRegistry = new EntitlementRegistry();
    Debug mockDebug = mock(Debug.class);

    private final String TEST_COMBINER = "testCombiner";

    @BeforeMethod
    public void setUp() {

        mockRegistry.registerDecisionCombiner(TEST_COMBINER, TestCombiner.class);

        testResource = new DecisionCombinersResource(mockDebug, mockRegistry);
    }

    @Test
    public void shouldThrowErrorWthInvalidCondition() throws JsonMappingException {
        //given
        SubjectContext mockSubjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext);
        ServerContext mockServerContext = new ServerContext(realmContext);

        Subject mockSubject = new Subject();
        given(mockSubjectContext.getCallerSubject()).willReturn(mockSubject);

        ReadRequest mockRequest = mock(ReadRequest.class);
        ResultHandler mockHandler = mock(ResultHandler.class);
        JsonSchema mockSchema = mock(JsonSchema.class);

        given(mockMapper.generateJsonSchema((Class<?>) any(Class.class))).willReturn(mockSchema);

        //when
        testResource.readInstance(mockServerContext, "invalidCondition", mockRequest, mockHandler);

        //then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockHandler, times(1)).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.NOT_FOUND);
    }

    @Test
    public void testSuccessfulJsonificationAndQuery() throws JsonMappingException {
        //given
        SubjectContext mockSubjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext);
        ServerContext mockServerContext = new ServerContext(realmContext);

        Subject mockSubject = new Subject();
        given(mockSubjectContext.getCallerSubject()).willReturn(mockSubject);

        QueryRequest mockRequest = mock(QueryRequest.class);
        QueryResultHandler mockHandler = mock(QueryResultHandler.class);
        JsonSchema mockSchema = mock(JsonSchema.class);

        given(mockMapper.generateJsonSchema((Class<?>) any(Class.class))).willReturn(mockSchema);

        //when
        testResource.queryCollection(mockServerContext, mockRequest, mockHandler);

        //then
        verify(mockHandler, times(1)).handleResource(any(Resource.class));
        verify(mockHandler, times(1)).handleResult(any(QueryResult.class));
    }

    @Test
    public void testSuccessfulJsonificationAndReadAndNamePropertyRemoved() throws JsonMappingException {
        //given
        SubjectContext mockSubjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext);
        ServerContext mockServerContext = new ServerContext(realmContext);

        Subject mockSubject = new Subject();
        given(mockSubjectContext.getCallerSubject()).willReturn(mockSubject);

        ReadRequest mockRequest = mock(ReadRequest.class);
        ResultHandler mockHandler = mock(ResultHandler.class);
        JsonSchema mockSchema = mock(JsonSchema.class);

        given(mockMapper.generateJsonSchema((Class<?>) any(Class.class))).willReturn(mockSchema);

        //when
        testResource.readInstance(mockServerContext, TEST_COMBINER, mockRequest, mockHandler);

        //then
        ArgumentCaptor<Resource> captor = ArgumentCaptor.forClass(Resource.class);
        verify(mockHandler, times(1)).handleResult(captor.capture());

        Map result = captor.getValue().getContent().asMap();
        assertThat(result.containsKey("title")).isTrue();
        assertThat(result.get("title")).isEqualTo(TEST_COMBINER);
    }

    /**
     * Test combiner type:
     */

    private class TestCombiner extends EntitlementCombiner {

        @Override
        protected boolean combine(Boolean b1, Boolean b2) {
            return false;
        }

        @Override
        protected boolean isCompleted() {
            return false;
        }

    }
}

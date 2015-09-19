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
* Copyright 2014-2015 ForgeRock AS.
*/

package org.forgerock.openam.entitlement.rest;

import static org.fest.assertions.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsonschema.JsonSchema;
import com.sun.identity.entitlement.EntitlementCombiner;
import com.sun.identity.shared.debug.Debug;
import java.util.Map;
import javax.security.auth.Subject;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.ClientContext;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.entitlement.EntitlementRegistry;
import org.forgerock.openam.entitlement.rest.DecisionCombinersResource;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.SubjectContext;
import org.forgerock.util.promise.Promise;
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

    @Test (expectedExceptions = NotFoundException.class)
    public void shouldThrowErrorWthInvalidCondition() throws JsonMappingException, ResourceException {
        //given
        SubjectContext mockSubjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);

        Subject mockSubject = new Subject();
        given(mockSubjectContext.getCallerSubject()).willReturn(mockSubject);

        ReadRequest mockRequest = mock(ReadRequest.class);
        JsonSchema mockSchema = mock(JsonSchema.class);

        given(mockMapper.generateJsonSchema((Class<?>) any(Class.class))).willReturn(mockSchema);

        //when
        Promise<ResourceResponse, ResourceException> result =
                testResource.readInstance(mockServerContext, "invalidCondition", mockRequest);

        //then
        result.getOrThrowUninterruptibly();
    }

    @Test
    public void testSuccessfulJsonificationAndQuery() throws JsonMappingException {
        //given
        SubjectContext mockSubjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);

        Subject mockSubject = new Subject();
        given(mockSubjectContext.getCallerSubject()).willReturn(mockSubject);

        QueryRequest mockRequest = mock(QueryRequest.class);
        QueryResourceHandler mockHandler = mock(QueryResourceHandler.class);
        JsonSchema mockSchema = mock(JsonSchema.class);

        given(mockMapper.generateJsonSchema((Class<?>) any(Class.class))).willReturn(mockSchema);

        //when
        testResource.queryCollection(mockServerContext, mockRequest, mockHandler);

        //then
        verify(mockHandler, times(1)).handleResource(any(ResourceResponse.class));
    }

    @Test
    public void testSuccessfulJsonificationAndReadAndNamePropertyRemoved() throws JsonMappingException, ResourceException {
        //given
        SubjectContext mockSubjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);

        Subject mockSubject = new Subject();
        given(mockSubjectContext.getCallerSubject()).willReturn(mockSubject);

        ReadRequest mockRequest = mock(ReadRequest.class);
        JsonSchema mockSchema = mock(JsonSchema.class);

        given(mockMapper.generateJsonSchema((Class<?>) any(Class.class))).willReturn(mockSchema);

        //when
        Promise<ResourceResponse, ResourceException> result =
                testResource.readInstance(mockServerContext, TEST_COMBINER, mockRequest);

        //then
        Map resultMap = result.getOrThrowUninterruptibly().getContent().asMap();
        assertThat(resultMap.containsKey("title")).isTrue();
        assertThat(resultMap.get("title")).isEqualTo(TEST_COMBINER);
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

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
import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.LogicalCondition;
import com.sun.identity.shared.debug.Debug;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
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
import org.forgerock.openam.entitlement.rest.ConditionTypesResource;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.SubjectContext;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ConditionTypesResourceTest {

    ConditionTypesResource testResource;
    ObjectMapper mockMapper = mock(ObjectMapper.class);
    EntitlementRegistry mockRegistry = new EntitlementRegistry();
    Debug mockDebug = mock(Debug.class);

    private final String TEST_CONDITION_WITH_NAME = "testConditionWithName";
    private final String TEST_LOGICAL_CONDITION = "testLogicalCondition";


    @BeforeMethod
    public void setUp() {

        mockRegistry.registerConditionType(TEST_CONDITION_WITH_NAME, TestConditionTypeWithName.class);
        mockRegistry.registerConditionType(TEST_LOGICAL_CONDITION, TestLogicalConditionTypeWithName.class);

        testResource = new ConditionTypesResource(mockDebug, mockRegistry);
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
        Promise<ResourceResponse, ResourceException> result
                = testResource.readInstance(mockServerContext, "invalidCondition", mockRequest);

        //then
        result.getOrThrowUninterruptibly();
    }

    @Test
    public void testSuccessfulJsonificationAndQuery() throws JsonMappingException {
        //given
        SubjectContext mockSubjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);
        QueryResourceHandler mockHandler = mock(QueryResourceHandler.class);

        Subject mockSubject = new Subject();
        given(mockSubjectContext.getCallerSubject()).willReturn(mockSubject);

        QueryRequest mockRequest = mock(QueryRequest.class);
        JsonSchema mockSchema = mock(JsonSchema.class);

        given(mockRequest.getPageSize()).willReturn(2);
        given(mockHandler.handleResource(any(ResourceResponse.class))).willReturn(true);
        given(mockMapper.generateJsonSchema((Class<?>) any(Class.class))).willReturn(mockSchema);

        //when
        testResource.queryCollection(mockServerContext, mockRequest, mockHandler);

        //then
        verify(mockHandler, times(2)).handleResource(any(ResourceResponse.class));
    }

    @Test
    public void testSuccessfulJsonificationAndReadAndNamePropertyRemoved()
            throws JsonMappingException, ExecutionException, InterruptedException {
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
                testResource.readInstance(mockServerContext, TEST_CONDITION_WITH_NAME, mockRequest);

        //then

        Map resultMap = result.get().getContent().asMap();
        assertThat(resultMap.containsKey("title")).isTrue();
        assertThat(resultMap.containsKey("config")).isTrue();
        assertThat(resultMap.containsKey("logical")).isTrue();
        assertThat(resultMap.get("title")).isEqualTo(TEST_CONDITION_WITH_NAME);
        assertThat(resultMap.get("logical")).isEqualTo(false);
        assertThat(resultMap.get("config")).isInstanceOf(JsonSchema.class);
        JsonSchema resultSchema = (JsonSchema) resultMap.get("config");
        assertThat(resultSchema.toString().equals("{\"type\":\"object\",\"properties\":{}}")).isTrue();
    }

    @Test
    public void testSuccessfulJsonificationAndLogicalIsCorrect()
            throws JsonMappingException, ExecutionException, InterruptedException {
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
                testResource.readInstance(mockServerContext, TEST_LOGICAL_CONDITION, mockRequest);

        //then

        Map resultMap = result.get().getContent().asMap();
        assertThat(resultMap.containsKey("logical")).isTrue();
        assertThat(resultMap.get("logical")).isEqualTo(true);
    }

    /**
     * Test condition type:
     *
     * IS logical.
     *
     * JSON Schema without removal of 'name' attribute: {"type":"object","properties":{"name":{"type":"string"}}}
     * JSON Schema with removal of 'name' attribute: "{"type":"object","properties":{}}"
     */

    private class TestLogicalConditionTypeWithName extends LogicalCondition {

        private String name;

        @Override
        public void setDisplayType(String displayType) {
            return;
        }

        @Override
        public String getDisplayType() {
            return null;
        }

        @Override
        public void init(Map<String, Set<String>> parameters) {
        }

        @Override
        public void setState(String state) {
        }

        @Override
        public String getState() {
            return null;
        }

        @Override
        public ConditionDecision evaluate(String realm, Subject subject, String resourceName,
                                          Map<String, Set<String>> environment) throws EntitlementException {
            return null;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Test condition type:
     *
     * NOT logical.
     *
     * JSON Schema without removal of 'name' attribute: {"type":"object","properties":{"name":{"type":"string"}}}
     * JSON Schema with removal of 'name' attribute: "{"type":"object","properties":{}}"
     */

    private class TestConditionTypeWithName implements EntitlementCondition {

        private String name;

        @Override
        public void setDisplayType(String displayType) {
            return;
        }

        @Override
        public String getDisplayType() {
            return null;
        }

        @Override
        public void init(Map<String, Set<String>> parameters) {
        }

        @Override
        public void setState(String state) {
        }

        @Override
        public String getState() {
            return null;
        }

        @Override
        public ConditionDecision evaluate(String realm, Subject subject, String resourceName,
                                          Map<String, Set<String>> environment) throws EntitlementException {
            return null;
        }

        public String getName() {
            return name;
        }

        @Override
        public void validate() throws EntitlementException {
            // Nothing
        }
    }
}

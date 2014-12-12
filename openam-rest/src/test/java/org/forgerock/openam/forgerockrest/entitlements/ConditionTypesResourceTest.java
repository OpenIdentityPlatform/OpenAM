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

import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.LogicalCondition;
import com.sun.identity.shared.debug.Debug;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.schema.JsonSchema;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.entitlement.EntitlementRegistry;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.rest.resource.SubjectContext;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import java.util.Map;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

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

        given(mockRequest.getPageSize()).willReturn(2);
        given(mockHandler.handleResource(any(Resource.class))).willReturn(true);
        given(mockMapper.generateJsonSchema((Class<?>) any(Class.class))).willReturn(mockSchema);

        //when
        testResource.queryCollection(mockServerContext, mockRequest, mockHandler);

        //then
        verify(mockHandler, times(2)).handleResource(any(Resource.class));
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
        testResource.readInstance(mockServerContext, TEST_CONDITION_WITH_NAME, mockRequest, mockHandler);

        //then
        ArgumentCaptor<Resource> captor = ArgumentCaptor.forClass(Resource.class);
        verify(mockHandler, times(1)).handleResult(captor.capture());

        Map result = captor.getValue().getContent().asMap();
        assertThat(result.containsKey("title")).isTrue();
        assertThat(result.containsKey("config")).isTrue();
        assertThat(result.containsKey("logical")).isTrue();
        assertThat(result.get("title")).isEqualTo(TEST_CONDITION_WITH_NAME);
        assertThat(result.get("logical")).isEqualTo(false);
        assertThat(result.get("config")).isInstanceOf(JsonSchema.class);
        JsonSchema resultSchema = (JsonSchema) result.get("config");
        assertThat(resultSchema.toString().equals("{\"type\":\"object\",\"properties\":{}}")).isTrue();
    }

    @Test
    public void testSuccessfulJsonificationAndLogicalIsCorrect() throws JsonMappingException {
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
        testResource.readInstance(mockServerContext, TEST_LOGICAL_CONDITION, mockRequest, mockHandler);

        //then
        ArgumentCaptor<Resource> captor = ArgumentCaptor.forClass(Resource.class);
        verify(mockHandler, times(1)).handleResult(captor.capture());

        Map result = captor.getValue().getContent().asMap();
        assertThat(result.containsKey("logical")).isTrue();
        assertThat(result.get("logical")).isEqualTo(true);
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

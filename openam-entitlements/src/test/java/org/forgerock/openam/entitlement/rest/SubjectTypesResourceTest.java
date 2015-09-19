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

import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.json.resource.test.assertj.AssertJQueryResponseAssert.assertThat;
import static org.forgerock.json.resource.test.assertj.AssertJResourceResponseAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import javax.security.auth.Subject;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsonschema.JsonSchema;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.LogicalSubject;
import com.sun.identity.entitlement.SubjectAttributesManager;
import com.sun.identity.entitlement.SubjectDecision;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.ClientContext;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.entitlement.EntitlementRegistry;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SubjectTypesResourceTest {

    SubjectTypesResource testResource;
    ObjectMapper mockMapper = mock(ObjectMapper.class);
    EntitlementRegistry mockRegistry = new EntitlementRegistry();
    Debug mockDebug = mock(Debug.class);

    private final String TEST_CONDITION_WITH_NAME = "testConditionWithName";
    private final String TEST_LOGICAL_CONDITION = "testLogicalCondition";


    @BeforeMethod
    public void setUp() {

        mockRegistry.registerSubjectType(TEST_CONDITION_WITH_NAME, TestSubjectTypeWithName.class);
        mockRegistry.registerSubjectType(TEST_LOGICAL_CONDITION, TestLogicalSubjectTypeWithName.class);

        testResource = new SubjectTypesResource(mockDebug, mockRegistry);
    }

    @Test
    public void shouldThrowErrorWthInvalidCondition() throws JsonMappingException {
        //given
        SSOTokenContext mockSubjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);

        Subject mockSubject = new Subject();
        given(mockSubjectContext.getCallerSubject()).willReturn(mockSubject);

        ReadRequest mockRequest = mock(ReadRequest.class);
        JsonSchema mockSchema = mock(JsonSchema.class);

        given(mockMapper.generateJsonSchema((Class<?>) any(Class.class))).willReturn(mockSchema);

        //when
        Promise<ResourceResponse, ResourceException> promise =
                testResource.readInstance(mockServerContext, "invalidCondition", mockRequest);

        //then
        assertThat(promise).failedWithException().isInstanceOf(NotFoundException.class);
    }

    @Test
    public void testSuccessfulJsonificationAndQuery() throws Exception {
        //given
        SSOTokenContext mockSubjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);

        Subject mockSubject = new Subject();
        given(mockSubjectContext.getCallerSubject()).willReturn(mockSubject);

        QueryRequest mockRequest = mock(QueryRequest.class);
        JsonSchema mockSchema = mock(JsonSchema.class);
        QueryResourceHandler mockHandler = mock(QueryResourceHandler.class);

        given(mockRequest.getPageSize()).willReturn(2);
        given(mockHandler.handleResource(any(ResourceResponse.class))).willReturn(true);
        given(mockMapper.generateJsonSchema((Class<?>) any(Class.class))).willReturn(mockSchema);

        //when
        Promise<QueryResponse, ResourceException> promise =
                testResource.queryCollection(mockServerContext, mockRequest, mockHandler);

        //then
        assertThat(promise).succeeded();
        verify(mockHandler, times(2)).handleResource(any(ResourceResponse.class));
    }

    @Test
    public void testSuccessfulJsonificationAndReadAndSubjectNamePropertyRemoved() throws Exception {
        //given
        SSOTokenContext mockSubjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);

        Subject mockSubject = new Subject();
        given(mockSubjectContext.getCallerSubject()).willReturn(mockSubject);

        ReadRequest mockRequest = mock(ReadRequest.class);
        JsonSchema mockSchema = mock(JsonSchema.class);

        given(mockMapper.generateJsonSchema((Class<?>) any(Class.class))).willReturn(mockSchema);

        //when
        Promise<ResourceResponse, ResourceException> promise =
                testResource.readInstance(mockServerContext, TEST_CONDITION_WITH_NAME, mockRequest);

        //then
        assertThat(promise).succeeded().withContent().hasString("title");
        assertThat(promise).succeeded().withContent().stringAt("title").isEqualTo(TEST_CONDITION_WITH_NAME);
        assertThat(promise).succeeded().withContent().hasBoolean("logical");
        assertThat(promise).succeeded().withContent().booleanAt("logical").isFalse();
        assertThat(promise.get().getContent().get("config").getObject().toString()).
                isEqualTo("{\"type\":\"object\",\"properties\":{}}");
    }

    @Test
    public void testSuccessfulJsonificationAndLogicalIsCorrect() throws JsonMappingException {
        //given
        SSOTokenContext mockSubjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);

        Subject mockSubject = new Subject();
        given(mockSubjectContext.getCallerSubject()).willReturn(mockSubject);

        ReadRequest mockRequest = mock(ReadRequest.class);
        QueryResourceHandler mockHandler = mock(QueryResourceHandler.class);
        JsonSchema mockSchema = mock(JsonSchema.class);

        given(mockMapper.generateJsonSchema((Class<?>) any(Class.class))).willReturn(mockSchema);

        //when
        Promise<ResourceResponse, ResourceException> promise =
                testResource.readInstance(mockServerContext, TEST_LOGICAL_CONDITION, mockRequest);

        //then
        assertThat(promise).succeeded().withContent().hasBoolean("logical");
        assertThat(promise).succeeded().withContent().booleanAt("logical").isTrue();
    }

    /**
     * Test subject type:
     *
     * IS logical.
     */

    private class TestLogicalSubjectTypeWithName extends LogicalSubject {

        @Override
        public SubjectDecision evaluate(String realm, SubjectAttributesManager mgr,
                                        Subject subject, String resourceName,
                                        Map<String, Set<String>> environment) throws EntitlementException {
            return null;
        }
    }

    /**
     * Test subject type:
     *
     * NOT logical.
     *
     * JSON Schema without removal of 'subjectName' attribute:
     *      {"type":"object","properties":{"subjectName":{"type":"string"}}}
     *
     * JSON Schema with removal of 'subjectName' attribute:
     *      "{"type":"object","properties":{}}"
     */
    private class TestSubjectTypeWithName implements EntitlementSubject {

        private String subjectName;

        @Override
        public void setState(String state) {
            return;
        }

        @Override
        public String getState() {
            return null;
        }

        @Override
        public Map<String, Set<String>> getSearchIndexAttributes() {
            return null;
        }

        @Override
        public Set<String> getRequiredAttributeNames() {
            return null;
        }

        @Override
        public SubjectDecision evaluate(String realm, SubjectAttributesManager mgr, Subject subject,
                                        String resourceName, Map<String, Set<String>> environment)
                throws EntitlementException {
            return null;
        }

        @Override
        public boolean isIdentity() {
            return false;
        }
    }
}

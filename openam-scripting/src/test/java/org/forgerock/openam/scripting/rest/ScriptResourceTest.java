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
package org.forgerock.openam.scripting.rest;

import static org.assertj.core.api.Assertions.*;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.scripting.ScriptConstants.*;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptContext.*;
import static org.forgerock.openam.scripting.SupportedScriptingLanguage.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.*;
import com.sun.identity.shared.encode.Base64;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.openam.errors.ExceptionMappingHandler;
import org.forgerock.openam.scripting.ScriptException;
import org.forgerock.openam.scripting.StandardScriptEngineManager;
import org.forgerock.openam.scripting.StandardScriptValidator;
import org.forgerock.openam.scripting.rest.ScriptExceptionMappingHandler;
import org.forgerock.openam.scripting.rest.ScriptResource;
import org.forgerock.openam.scripting.service.ScriptConfiguration;
import org.forgerock.openam.scripting.service.ScriptingService;
import org.forgerock.openam.scripting.service.ScriptingServiceFactory;
import org.forgerock.util.promise.Promise;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScriptResourceTest {

    private final String script = "var a = 123;var b = 456;";
    private final String encodeScript = Base64.encode(script.getBytes());

    private ScriptResource scriptResource;
    private Context context;
    private Map<String, ScriptConfiguration> scriptConfigSet = new LinkedHashMap<>();

    private class MockScriptingService implements ScriptingService {

        @Override
        public ScriptConfiguration create(ScriptConfiguration config) throws ScriptException {
            return config;
        }

        @Override
        public void delete(String uuid) throws ScriptException {
        }

        @Override
        public Set<ScriptConfiguration> getAll() throws ScriptException {
            return new LinkedHashSet<>(scriptConfigSet.values());
        }

        @Override
        public Set<ScriptConfiguration> get(org.forgerock.util.query.QueryFilter<String> queryFilter)
                throws ScriptException {
            return new LinkedHashSet<>(scriptConfigSet.values());
        }

        @Override
        public ScriptConfiguration get(String uuid) throws ScriptException {
            return scriptConfigSet.get(uuid);
        }

        @Override
        public ScriptConfiguration update(ScriptConfiguration config) throws ScriptException {
            return config;
        }
    }


    @BeforeMethod
    public void setUp() throws ResourceException {
        Logger logger = mock(Logger.class);
        ScriptingService scriptingService = new MockScriptingService();
        ScriptingServiceFactory serviceFactory = mock(ScriptingServiceFactory.class);
        when(serviceFactory.create(any(Subject.class), anyString())).thenReturn(scriptingService);
        ExceptionMappingHandler<ScriptException, ResourceException> errorHandler = new ScriptExceptionMappingHandler();
        scriptResource = new ScriptResource(logger, serviceFactory, errorHandler,
                new StandardScriptValidator(new StandardScriptEngineManager()));

        context = mock(Context.class);
        given(context.asContext(HttpContext.class))
                .willReturn(new HttpContext(json(object(field("headers", Collections.emptyMap()),
                        field("parameters", Collections.emptyMap()))), null));
    }

    @Test
    public void shouldCreateScriptConfigurationWithoutError() throws Exception {
        // given
        JsonValue requestJson = json(object(
                field(SCRIPT_NAME, "MyJavaScript"),
                field(SCRIPT_DESCRIPTION, "A test script configuration"),
                field(SCRIPT_TEXT, encodeScript),
                field(SCRIPT_LANGUAGE, "JAVASCRIPT"),
                field(SCRIPT_CONTEXT, "POLICY_CONDITION")));
        CreateRequest createRequest = mock(CreateRequest.class);
        when(createRequest.getContent()).thenReturn(requestJson);

        // when
        ResourceResponse response = scriptResource.createInstance(context, createRequest).getOrThrowUninterruptibly();

        // then
        JsonValue responseJson = response.getContent();
        assertEquals(responseJson.get(SCRIPT_NAME).asString(), "MyJavaScript");
        assertEquals(responseJson.get(SCRIPT_DESCRIPTION).asString(), "A test script configuration");
        assertEquals(responseJson.get(SCRIPT_TEXT).asString(), encodeScript);
        assertEquals(getLanguageFromString(responseJson.get(SCRIPT_LANGUAGE).asString()), JAVASCRIPT);
        assertEquals(getContextFromString(responseJson.get(SCRIPT_CONTEXT).asString()), POLICY_CONDITION);
    }

    @Test
    public void shouldDeleteScriptConfigurationWithoutError() throws Exception {
        // given
        String resourceId = "1234567890";
        DeleteRequest deleteRequest = mock(DeleteRequest.class);

        // when
        ResourceResponse response =
                scriptResource.deleteInstance(context, resourceId, deleteRequest).getOrThrowUninterruptibly();

        // then
        assertNotNull(response);
        assertEquals(response.getId(), resourceId);
    }

    @Test
    public void shouldQueryScriptConfigurationWithoutError() throws Exception {
        // given
        scriptConfigSet.clear();
        scriptConfigSet.put("1234567890", ScriptConfiguration.builder()
                .setId("1234567890")
                .setName("MyJavaScript")
                .setDescription("A test JavaScript configuration")
                .setScript(script)
                .setLanguage(JAVASCRIPT)
                .setContext(POLICY_CONDITION).build());

        scriptConfigSet.put("0987654321", ScriptConfiguration.builder()
                .setId("0987654321")
                .setName("MyGroovyScript")
                .setDescription("A test Groovy script configuration")
                .setScript(script)
                .setLanguage(GROOVY)
                .setContext(POLICY_CONDITION).build());

        QueryRequest queryRequest = mock(QueryRequest.class);
        QueryResourceHandler mockHandler = mock(QueryResourceHandler.class);
        given(mockHandler.handleResource(any(ResourceResponse.class))).willReturn(true);

        // when
        Promise<QueryResponse, ResourceException> promise =
                scriptResource.queryCollection(context, queryRequest, mockHandler);
        QueryResponse response = promise.getOrThrowUninterruptibly();

        // then
        assertNotNull(response);
        ArgumentCaptor<ResourceResponse> responses = ArgumentCaptor.forClass(ResourceResponse.class);
        verify(mockHandler, times(2)).handleResource(responses.capture());

        Iterator<ResourceResponse> iterator = responses.getAllValues().iterator();
        JsonValue responseJsonOne = iterator.next().getContent();
        JsonValue responseJsonTwo = iterator.next().getContent();

        assertEquals(responseJsonOne.get(JSON_UUID).asString(), "1234567890");
        assertEquals(responseJsonOne.get(SCRIPT_NAME).asString(), "MyJavaScript");
        assertEquals(responseJsonOne.get(SCRIPT_DESCRIPTION).asString(), "A test JavaScript configuration");
        assertEquals(responseJsonOne.get(SCRIPT_TEXT).asString(), encodeScript);
        assertEquals(getLanguageFromString(responseJsonOne.get(SCRIPT_LANGUAGE).asString()), JAVASCRIPT);
        assertEquals(getContextFromString(responseJsonOne.get(SCRIPT_CONTEXT).asString()), POLICY_CONDITION);

        assertEquals(responseJsonTwo.get(JSON_UUID).asString(), "0987654321");
        assertEquals(responseJsonTwo.get(SCRIPT_NAME).asString(), "MyGroovyScript");
        assertEquals(responseJsonTwo.get(SCRIPT_DESCRIPTION).asString(), "A test Groovy script configuration");
        assertEquals(responseJsonTwo.get(SCRIPT_TEXT).asString(), encodeScript);
        assertEquals(getLanguageFromString(responseJsonTwo.get(SCRIPT_LANGUAGE).asString()), GROOVY);
        assertEquals(getContextFromString(responseJsonTwo.get(SCRIPT_CONTEXT).asString()), POLICY_CONDITION);

    }

    @Test
    public void shouldReadScriptConfigurationWithoutError() throws Exception {
        // given
        String resourceId = "1234567890";
        scriptConfigSet.clear();
        scriptConfigSet.put(resourceId, ScriptConfiguration.builder()
                .setId(resourceId)
                .setName("MyJavaScript")
                .setDescription("A test JavaScript configuration")
                .setScript(script)
                .setLanguage(JAVASCRIPT)
                .setContext(POLICY_CONDITION).build());

        ReadRequest readRequest = mock(ReadRequest.class);

        // when
        Promise<ResourceResponse, ResourceException> promise = scriptResource.readInstance(context, resourceId,
                readRequest);
        ResourceResponse response = promise.getOrThrowUninterruptibly();

        // then
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(response.getId(), resourceId);
        JsonValue responseJson = response.getContent();
        assertEquals(responseJson.get(JSON_UUID).asString(), resourceId);
        assertEquals(responseJson.get(SCRIPT_NAME).asString(), "MyJavaScript");
        assertEquals(responseJson.get(SCRIPT_DESCRIPTION).asString(), "A test JavaScript configuration");
        assertEquals(responseJson.get(SCRIPT_TEXT).asString(), encodeScript);
        assertEquals(getLanguageFromString(responseJson.get(SCRIPT_LANGUAGE).asString()), JAVASCRIPT);
        assertEquals(getContextFromString(responseJson.get(SCRIPT_CONTEXT).asString()), POLICY_CONDITION);
    }

    @Test
    public void shouldUpdateScriptConfigurationWithoutError() throws Exception {
        // given
        String resourceId = "1234567890";
        JsonValue requestJson = json(object(
                field(SCRIPT_NAME, "MyJavaScript"),
                field(SCRIPT_DESCRIPTION, "A test script configuration"),
                field(SCRIPT_TEXT, encodeScript),
                field(SCRIPT_LANGUAGE, "JAVASCRIPT"),
                field(SCRIPT_CONTEXT, "POLICY_CONDITION")));
        UpdateRequest updateRequest = mock(UpdateRequest.class);
        when(updateRequest.getContent()).thenReturn(requestJson);

        // when
        ResourceResponse response =
                scriptResource.updateInstance(context, resourceId, updateRequest).getOrThrowUninterruptibly();

        // then
        assertNotNull(response);
        assertNotNull(response.getId());
        JsonValue responseJson = response.getContent();
        assertEquals(responseJson.get(SCRIPT_NAME).asString(), "MyJavaScript");
        assertEquals(responseJson.get(SCRIPT_DESCRIPTION).asString(), "A test script configuration");
        assertEquals(responseJson.get(SCRIPT_TEXT).asString(), encodeScript);
        assertEquals(getLanguageFromString(responseJson.get(SCRIPT_LANGUAGE).asString()), JAVASCRIPT);
        assertEquals(getContextFromString(responseJson.get(SCRIPT_CONTEXT).asString()), POLICY_CONDITION);
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldFailIfNoNameIsSuppliedOnCreate() throws ResourceException {
        // given
        JsonValue requestJson = json(object(
                field(SCRIPT_TEXT, encodeScript),
                field(SCRIPT_LANGUAGE, "JAVASCRIPT"),
                field(SCRIPT_CONTEXT, "POLICY_CONDITION")));
        CreateRequest createRequest = mock(CreateRequest.class);
        when(createRequest.getContent()).thenReturn(requestJson);

        // when
        scriptResource.createInstance(context, createRequest).getOrThrowUninterruptibly();

        // then - exception
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldFailIfNoScriptIsSuppliedOnCreate() throws ResourceException {
        // given
        JsonValue requestJson = json(object(
                field(SCRIPT_NAME, "MyJavaScript"),
                field(SCRIPT_LANGUAGE, "JAVASCRIPT"),
                field(SCRIPT_CONTEXT, "POLICY_CONDITION")));
        CreateRequest createRequest = mock(CreateRequest.class);
        when(createRequest.getContent()).thenReturn(requestJson);

        // when
        scriptResource.createInstance(context, createRequest).getOrThrowUninterruptibly();

        // then - exception
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldFailIfNoLanguageIsSuppliedOnCreate() throws ResourceException {
        // given
        JsonValue requestJson = json(object(
                field(SCRIPT_NAME, "MyJavaScript"),
                field(SCRIPT_TEXT, encodeScript),
                field(SCRIPT_CONTEXT, "POLICY_CONDITION")));
        CreateRequest createRequest = mock(CreateRequest.class);
        when(createRequest.getContent()).thenReturn(requestJson);

        // when
        scriptResource.createInstance(context, createRequest).getOrThrowUninterruptibly();

        // then - exception
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldFailIfNoContextIsSuppliedOnCreate() throws ResourceException {
        // given
        JsonValue requestJson = json(object(
                field(SCRIPT_NAME, "MyJavaScript"),
                field(SCRIPT_TEXT, encodeScript),
                field(SCRIPT_LANGUAGE, "JAVASCRIPT")));
        CreateRequest createRequest = mock(CreateRequest.class);
        when(createRequest.getContent()).thenReturn(requestJson);

        // when
        scriptResource.createInstance(context, createRequest).getOrThrowUninterruptibly();

        // then - exception
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldFailIfNoNameIsSuppliedOnUpdate() throws ResourceException {
        // given
        String resourceId = "1234567890";
        JsonValue requestJson = json(object(
                field(SCRIPT_TEXT, encodeScript),
                field(SCRIPT_LANGUAGE, "JAVASCRIPT"),
                field(SCRIPT_CONTEXT, "POLICY_CONDITION")));
        UpdateRequest updateRequest = mock(UpdateRequest.class);
        when(updateRequest.getContent()).thenReturn(requestJson);

        // when
        scriptResource.updateInstance(context, resourceId, updateRequest).getOrThrowUninterruptibly();

        // then - exception
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldFailIfNoScriptIsSuppliedOnUpdate() throws ResourceException {
        // given
        String resourceId = "1234567890";
        JsonValue requestJson = json(object(
                field(SCRIPT_NAME, "MyJavaScript"),
                field(SCRIPT_LANGUAGE, "JAVASCRIPT"),
                field(SCRIPT_CONTEXT, "POLICY_CONDITION")));
        UpdateRequest updateRequest = mock(UpdateRequest.class);
        when(updateRequest.getContent()).thenReturn(requestJson);

        // when
        scriptResource.updateInstance(context, resourceId, updateRequest).getOrThrowUninterruptibly();

        // then - exception
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldFailIfNoLanguageIsSuppliedOnUpdate() throws ResourceException {
        // given
        String resourceId = "1234567890";
        JsonValue requestJson = json(object(
                field(SCRIPT_NAME, "MyJavaScript"),
                field(SCRIPT_TEXT, encodeScript),
                field(SCRIPT_CONTEXT, "POLICY_CONDITION")));
        UpdateRequest updateRequest = mock(UpdateRequest.class);
        when(updateRequest.getContent()).thenReturn(requestJson);

        // when
        scriptResource.updateInstance(context, resourceId, updateRequest).getOrThrowUninterruptibly();

        // then - exception
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldFailIfNoContextIsSuppliedOnUpdate() throws ResourceException {
        // given
        String resourceId = "1234567890";
        JsonValue requestJson = json(object(
                field(SCRIPT_NAME, "MyJavaScript"),
                field(SCRIPT_TEXT, "var a = 123;var b = 456;"),
                field(SCRIPT_LANGUAGE, "JAVASCRIPT")));
        UpdateRequest updateRequest = mock(UpdateRequest.class);
        when(updateRequest.getContent()).thenReturn(requestJson);

        // when
        scriptResource.updateInstance(context, resourceId, updateRequest).getOrThrowUninterruptibly();

        // then - exception
    }

    @Test
    public void shouldQueryScriptConfigurationWithPaging() throws ScriptException, ResourceException {
        // given
        scriptConfigSet.clear();
        for (int i = 0; i < 9; i++) {
            ScriptConfiguration sc = ScriptConfiguration.builder()
                    .generateId()
                    .setName("MyJavaScript"+i)
                    .setScript(script)
                    .setLanguage(JAVASCRIPT)
                    .setContext(POLICY_CONDITION).build();
            scriptConfigSet.put(sc.getId(), sc);
        }

        QueryResourceHandler resultHandler = mock(QueryResourceHandler.class);
        given(resultHandler.handleResource(any(ResourceResponse.class))).willReturn(true);
        QueryRequest queryRequest = mock(QueryRequest.class);
        when(queryRequest.getPageSize()).thenReturn(5);

        // when
        when(queryRequest.getPagedResultsOffset()).thenReturn(0);
        scriptResource.queryCollection(context, queryRequest, resultHandler).getOrThrowUninterruptibly();

        // then
        ArgumentCaptor<ResourceResponse> resources = ArgumentCaptor.forClass(ResourceResponse.class);
        verify(resultHandler, times(5)).handleResource(resources.capture());

        List<ResourceResponse> responses = resources.getAllValues();

        assertThat(responses).isNotNull().hasSize(5);
        int count = 0;
        for (ResourceResponse resource : responses) {
            assertThat(resource.getContent().get(SCRIPT_NAME).asString()).endsWith(String.valueOf(count++));
        }

        // when
        Mockito.reset(resultHandler);
        given(resultHandler.handleResource(any(ResourceResponse.class))).willReturn(true);
        resources = ArgumentCaptor.forClass(ResourceResponse.class);
        when(queryRequest.getPagedResultsOffset()).thenReturn(5);
        scriptResource.queryCollection(context, queryRequest, resultHandler).getOrThrowUninterruptibly();
        verify(resultHandler, times(4)).handleResource(resources.capture());

        // then
        responses = resources.getAllValues();
        assertThat(responses).isNotNull().hasSize(4);
        for (ResourceResponse resource : responses) {
            assertThat(resource.getContent().get(SCRIPT_NAME).asString()).endsWith(String.valueOf(count++));
        }
    }

    private ActionRequest prepareActionRequestForValidate(String script, String language, String action) {
        String encodeScript = Base64.encode(script.getBytes());
        JsonValue requestJson = json(object(
                field(SCRIPT_TEXT, encodeScript),
                field(SCRIPT_LANGUAGE, language)));
        ActionRequest actionRequest = mock(ActionRequest.class);
        when(actionRequest.getContent()).thenReturn(requestJson);
        when(actionRequest.getAction()).thenReturn(action);
        return actionRequest;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldPassScriptValidation() throws ResourceException {
        // given
        ActionRequest request = prepareActionRequestForValidate("var a = 123;var b = 456;", "JAVASCRIPT", "validate");

        // when
        ActionResponse response = scriptResource.actionCollection(context, request).getOrThrowUninterruptibly();

        // then
        assertThat(response.getJsonContent().get("success").asBoolean()).isEqualTo(true);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldFailScriptValidationWithOneError() throws ResourceException {
        // given
        ActionRequest request = prepareActionRequestForValidate(
                "var a = 123;var b = 456; =VALIDATION SHOULD FAIL=", "JAVASCRIPT", "validate");

        // when
        ActionResponse response = scriptResource.actionCollection(context, request).getOrThrowUninterruptibly();

        // then
        assertThat(response.getJsonContent().get("success").asBoolean()).isEqualTo(false);
        assertThat(response.getJsonContent().get("errors").get("line")).isNotNull();
        assertThat(response.getJsonContent().get("errors").get("column")).isNotNull();
        assertThat(response.getJsonContent().get("errors").get("message")).isNotNull();
    }

    @Test(expectedExceptions = BadRequestException.class)
    @SuppressWarnings("unchecked")
    public void shouldReturnErrorWhenLanguageNotRecognised() throws ResourceException {
        // given
        ActionRequest request = prepareActionRequestForValidate("var a = 123;var b = 456;", "INVALID_LANG", "validate");

        // when
        scriptResource.actionCollection(context, request).getOrThrowUninterruptibly();

        // then - exception
    }

    @Test(expectedExceptions = NotSupportedException.class)
    @SuppressWarnings("unchecked")
    public void shouldReturnErrorWhenActionNotRecognised() throws ResourceException {
        // given
        ActionRequest request = prepareActionRequestForValidate("var a = 123;var b = 456;", "JAVASCRIPT", "invalid_action");

        // when
        scriptResource.actionCollection(context, request).getOrThrowUninterruptibly();

        // then - exception
    }

    @Test(expectedExceptions = BadRequestException.class)
    @SuppressWarnings("unchecked")
    public void shouldReturnErrorWhenNoLanguageSpecified() throws ResourceException {
        // given
        String encodeScript = Base64.encode("var a = 123;var b = 456;".getBytes());
        JsonValue requestJson = json(object(field(SCRIPT_TEXT, encodeScript)));
        ActionRequest request = mock(ActionRequest.class);
        when(request.getContent()).thenReturn(requestJson);
        when(request.getAction()).thenReturn("validate");

        // when
        scriptResource.actionCollection(context, request).getOrThrowUninterruptibly();

        // then - exception
    }

    @Test(expectedExceptions = BadRequestException.class)
    @SuppressWarnings("unchecked")
    public void shouldReturnErrorWhenNoScriptSpecified() throws ResourceException {
        // given
        JsonValue requestJson = json(object(field(SCRIPT_LANGUAGE, "JAVASCRIPT")));
        ActionRequest request = mock(ActionRequest.class);
        when(request.getContent()).thenReturn(requestJson);
        when(request.getAction()).thenReturn("validate");

        // when
        scriptResource.actionCollection(context, request).getOrThrowUninterruptibly();

        // then - exception
    }
}

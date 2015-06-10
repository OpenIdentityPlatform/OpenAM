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
package org.forgerock.openam.rest.scripting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.fluent.JsonValue.*;
import static org.forgerock.openam.scripting.ScriptConstants.*;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptContext.POLICY_CONDITION;
import static org.forgerock.openam.scripting.SupportedScriptingLanguage.GROOVY;
import static org.forgerock.openam.scripting.SupportedScriptingLanguage.JAVASCRIPT;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.testng.AssertJUnit.*;

import com.sun.identity.shared.encode.Base64;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.errors.ExceptionMappingHandler;
import org.forgerock.openam.scripting.ScriptException;
import org.forgerock.openam.scripting.StandardScriptEngineManager;
import org.forgerock.openam.scripting.StandardScriptValidator;
import org.forgerock.openam.scripting.service.ScriptConfiguration;
import org.forgerock.openam.scripting.service.ScriptingService;
import org.forgerock.openam.scripting.service.ScriptingServiceFactory;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ScriptResourceTest {

    private final String script = "var a = 123;var b = 456;";
    private final String encodeScript = Base64.encode(script.getBytes());

    private ScriptResource scriptResource;
    private ServerContext serverContext;
    private Map<String, ScriptConfiguration> scriptConfigSet = new LinkedHashMap<String, ScriptConfiguration>();

    private class MockScriptingService implements ScriptingService<ScriptConfiguration> {

        @Override
        public ScriptConfiguration create(ScriptConfiguration config) throws ScriptException {
            return config;
        }

        @Override
        public void delete(String uuid) throws ScriptException {
        }

        @Override
        public Set<ScriptConfiguration> getAll() throws ScriptException {
            return new LinkedHashSet<ScriptConfiguration>(scriptConfigSet.values());
        }

        @Override
        public Set<ScriptConfiguration> get(org.forgerock.util.query.QueryFilter<String> queryFilter)
                throws ScriptException {
            return new LinkedHashSet<ScriptConfiguration>(scriptConfigSet.values());
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

    private class MockResultHandler<T> implements ResultHandler<T> {
        private T result;
        private ResourceException error;

        @Override
        public void handleError(ResourceException error) {
            this.error = error;
        }

        @Override
        public void handleResult(T result) {
            this.result = result;
        }

        public T getResult() {
            return result;
        }

        public ResourceException getError() {
            return error;
        }
    };

    private class MockQueryResultHandler extends MockResultHandler<QueryResult> implements QueryResultHandler {
        private Set<Resource> resources = new LinkedHashSet<Resource>();

        @Override
        public boolean handleResource(Resource resource) {
            resources.add(resource);
            return true;
        }

        public Set<Resource> getResources() {
            return resources;
        }
    };

    @BeforeMethod
    public void setUp() throws ResourceException {
        Logger logger = mock(Logger.class);
        ScriptingService<ScriptConfiguration> scriptingService = new MockScriptingService();
        ScriptingServiceFactory<ScriptConfiguration> serviceFactory = mock(ScriptingServiceFactory.class);
        when(serviceFactory.create(any(Subject.class), anyString())).thenReturn(scriptingService);
        ExceptionMappingHandler<ScriptException, ResourceException> errorHandler = new ScriptExceptionMappingHandler();
        scriptResource = new ScriptResource(logger, serviceFactory, errorHandler,
                new StandardScriptValidator(new StandardScriptEngineManager()));

        serverContext = mock(ServerContext.class);
    }

    @Test
    public void shouldCreateScriptConfigurationWithoutError() throws ScriptException {
        // given
        MockResultHandler<Resource> resultHandler = new MockResultHandler<Resource>();
        JsonValue requestJson = json(object(field(SCRIPT_NAME, "MyJavaScript"), field(SCRIPT_DESCRIPTION,
                "A test script configuration"), field(SCRIPT_TEXT, encodeScript), field(SCRIPT_LANGUAGE,
                "JAVASCRIPT"), field(SCRIPT_CONTEXT, "POLICY_CONDITION")));
        CreateRequest createRequest = mock(CreateRequest.class);
        when(createRequest.getContent()).thenReturn(requestJson);

        // when
        scriptResource.createInstance(serverContext, createRequest, resultHandler);

        // then
        assertNull(resultHandler.getError());
        assertNotNull(resultHandler.getResult());
        assertNotNull(resultHandler.getResult().getId());
        JsonValue responseJson = resultHandler.getResult().getContent();
        assertEquals(responseJson.get(SCRIPT_NAME).asString(), "MyJavaScript");
        assertEquals(responseJson.get(SCRIPT_DESCRIPTION).asString(), "A test script configuration");
        assertEquals(responseJson.get(SCRIPT_TEXT).asString(), encodeScript);
        assertEquals(getLanguageFromString(responseJson.get(SCRIPT_LANGUAGE).asString()), JAVASCRIPT);
        assertEquals(getContextFromString(responseJson.get(SCRIPT_CONTEXT).asString()), POLICY_CONDITION);
    }

    @Test
    public void shouldDeleteScriptConfigurationWithoutError() throws ScriptException {
        // given
        String resourceId = "1234567890";
        DeleteRequest deleteRequest = mock(DeleteRequest.class);
        MockResultHandler<Resource> resultHandler = new MockResultHandler<Resource>();

        // when
        scriptResource.deleteInstance(serverContext, resourceId, deleteRequest, resultHandler);

        // then
        assertNull(resultHandler.getError());
        assertNotNull(resultHandler.getResult());
        assertEquals(resultHandler.getResult().getId(), resourceId);
    }

    @Test
    public void shouldQueryScriptConfigurationWithoutError() throws ScriptException {
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

        MockQueryResultHandler resultHandler = new MockQueryResultHandler();
        QueryRequest queryRequest = mock(QueryRequest.class);

        // when
        scriptResource.queryCollection(serverContext, queryRequest, resultHandler);

        // then
        assertNull(resultHandler.getError());
        Iterator<Resource> iterator = resultHandler.getResources().iterator();
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
    public void shouldReadScriptConfigurationWithoutError() throws ScriptException {
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

        MockResultHandler<Resource> resultHandler = new MockResultHandler<Resource>();
        ReadRequest readRequest = mock(ReadRequest.class);

        // when
        scriptResource.readInstance(serverContext, resourceId, readRequest, resultHandler);

        // then
        assertNull(resultHandler.getError());
        assertNotNull(resultHandler.getResult());
        assertNotNull(resultHandler.getResult().getId());
        assertEquals(resultHandler.getResult().getId(), resourceId);
        JsonValue responseJson = resultHandler.getResult().getContent();
        assertEquals(responseJson.get(JSON_UUID).asString(), resourceId);
        assertEquals(responseJson.get(SCRIPT_NAME).asString(), "MyJavaScript");
        assertEquals(responseJson.get(SCRIPT_DESCRIPTION).asString(), "A test JavaScript configuration");
        assertEquals(responseJson.get(SCRIPT_TEXT).asString(), encodeScript);
        assertEquals(getLanguageFromString(responseJson.get(SCRIPT_LANGUAGE).asString()), JAVASCRIPT);
        assertEquals(getContextFromString(responseJson.get(SCRIPT_CONTEXT).asString()), POLICY_CONDITION);
    }

    @Test
    public void shouldUpdateScriptConfigurationWithoutError() throws ScriptException {
        // given
        String resourceId = "1234567890";
        MockResultHandler<Resource> resultHandler = new MockResultHandler<Resource>();
        JsonValue requestJson = json(object(field(SCRIPT_NAME, "MyJavaScript"), field(SCRIPT_DESCRIPTION,
                "A test script configuration"), field(SCRIPT_TEXT, encodeScript),
                field(SCRIPT_LANGUAGE, "JAVASCRIPT"), field(SCRIPT_CONTEXT, "POLICY_CONDITION")));
        UpdateRequest updateRequest = mock(UpdateRequest.class);
        when(updateRequest.getContent()).thenReturn(requestJson);

        // when
        scriptResource.updateInstance(serverContext, resourceId, updateRequest, resultHandler);

        // then
        assertNull(resultHandler.getError());
        assertNotNull(resultHandler.getResult());
        assertNotNull(resultHandler.getResult().getId());
        JsonValue responseJson = resultHandler.getResult().getContent();
        assertEquals(responseJson.get(SCRIPT_NAME).asString(), "MyJavaScript");
        assertEquals(responseJson.get(SCRIPT_DESCRIPTION).asString(), "A test script configuration");
        assertEquals(responseJson.get(SCRIPT_TEXT).asString(), encodeScript);
        assertEquals(getLanguageFromString(responseJson.get(SCRIPT_LANGUAGE).asString()), JAVASCRIPT);
        assertEquals(getContextFromString(responseJson.get(SCRIPT_CONTEXT).asString()), POLICY_CONDITION);
    }

    @Test
    public void shouldFailIfNoNameIsSuppliedOnCreate() {
        // given
        MockResultHandler<Resource> resultHandler = new MockResultHandler<Resource>();
        JsonValue requestJson = json(object(field(SCRIPT_TEXT, encodeScript), field(SCRIPT_LANGUAGE,
                "JAVASCRIPT"), field(SCRIPT_CONTEXT, "POLICY_CONDITION")));
        CreateRequest createRequest = mock(CreateRequest.class);
        when(createRequest.getContent()).thenReturn(requestJson);

        // when
        scriptResource.createInstance(serverContext, createRequest, resultHandler);

        // then
        assertNull(resultHandler.getResult());
        assertNotNull(resultHandler.getError());
        assertNotNull(resultHandler.getError().getMessage());
        assertEquals(resultHandler.getError().getCode(), ResourceException.BAD_REQUEST);
    }

    @Test
    public void shouldFailIfNoScriptIsSuppliedOnCreate() {
        // given
        MockResultHandler<Resource> resultHandler = new MockResultHandler<Resource>();
        JsonValue requestJson = json(object(
                field(SCRIPT_NAME, "MyJavaScript"),
                field(SCRIPT_LANGUAGE, "JAVASCRIPT"),
                field(SCRIPT_CONTEXT, "POLICY_CONDITION")));
        CreateRequest createRequest = mock(CreateRequest.class);
        when(createRequest.getContent()).thenReturn(requestJson);

        // when
        scriptResource.createInstance(serverContext, createRequest, resultHandler);

        // then
        assertNull(resultHandler.getResult());
        assertNotNull(resultHandler.getError());
        assertNotNull(resultHandler.getError().getMessage());
        assertEquals(resultHandler.getError().getCode(), ResourceException.BAD_REQUEST);
    }

    @Test
    public void shouldFailIfNoLanguageIsSuppliedOnCreate() {
        // given
        MockResultHandler<Resource> resultHandler = new MockResultHandler<Resource>();
        JsonValue requestJson = json(object(
                field(SCRIPT_NAME, "MyJavaScript"),
                field(SCRIPT_TEXT, encodeScript),
                field(SCRIPT_CONTEXT, "POLICY_CONDITION")));
        CreateRequest createRequest = mock(CreateRequest.class);
        when(createRequest.getContent()).thenReturn(requestJson);

        // when
        scriptResource.createInstance(serverContext, createRequest, resultHandler);

        // then
        assertNull(resultHandler.getResult());
        assertNotNull(resultHandler.getError());
        assertNotNull(resultHandler.getError().getMessage());
        assertEquals(resultHandler.getError().getCode(), ResourceException.BAD_REQUEST);
    }

    @Test
    public void shouldFailIfNoContextIsSuppliedOnCreate() {
        // given
        MockResultHandler<Resource> resultHandler = new MockResultHandler<Resource>();
        JsonValue requestJson = json(object(
                field(SCRIPT_NAME, "MyJavaScript"),
                field(SCRIPT_TEXT, encodeScript),
                field(SCRIPT_LANGUAGE, "JAVASCRIPT")));
        CreateRequest createRequest = mock(CreateRequest.class);
        when(createRequest.getContent()).thenReturn(requestJson);

        // when
        scriptResource.createInstance(serverContext, createRequest, resultHandler);

        // then
        assertNull(resultHandler.getResult());
        assertNotNull(resultHandler.getError());
        assertNotNull(resultHandler.getError().getMessage());
        assertEquals(resultHandler.getError().getCode(), ResourceException.BAD_REQUEST);
    }

    @Test
    public void shouldFailIfNoNameIsSuppliedOnUpdate() {
        // given
        String resourceId = "1234567890";
        MockResultHandler<Resource> resultHandler = new MockResultHandler<Resource>();
        JsonValue requestJson = json(object(
                field(SCRIPT_TEXT, encodeScript),
                field(SCRIPT_LANGUAGE, "JAVASCRIPT"),
                field(SCRIPT_CONTEXT, "POLICY_CONDITION")));
        UpdateRequest updateRequest = mock(UpdateRequest.class);
        when(updateRequest.getContent()).thenReturn(requestJson);

        // when
        scriptResource.updateInstance(serverContext, resourceId, updateRequest, resultHandler);

        // then
        assertNull(resultHandler.getResult());
        assertNotNull(resultHandler.getError());
        assertNotNull(resultHandler.getError().getMessage());
        assertEquals(resultHandler.getError().getCode(), ResourceException.BAD_REQUEST);
    }

    @Test
    public void shouldFailIfNoScriptIsSuppliedOnUpdate() {
        // given
        String resourceId = "1234567890";
        MockResultHandler<Resource> resultHandler = new MockResultHandler<Resource>();
        JsonValue requestJson = json(object(
                field(SCRIPT_NAME, "MyJavaScript"),
                field(SCRIPT_LANGUAGE, "JAVASCRIPT"),
                field(SCRIPT_CONTEXT, "POLICY_CONDITION")));
        UpdateRequest updateRequest = mock(UpdateRequest.class);
        when(updateRequest.getContent()).thenReturn(requestJson);

        // when
        scriptResource.updateInstance(serverContext, resourceId, updateRequest, resultHandler);

        // then
        assertNull(resultHandler.getResult());
        assertNotNull(resultHandler.getError());
        assertNotNull(resultHandler.getError().getMessage());
        assertEquals(resultHandler.getError().getCode(), ResourceException.BAD_REQUEST);
    }

    @Test
    public void shouldFailIfNoLanguageIsSuppliedOnUpdate() {
        // given
        String resourceId = "1234567890";
        MockResultHandler<Resource> resultHandler = new MockResultHandler<Resource>();
        JsonValue requestJson = json(object(
                field(SCRIPT_NAME, "MyJavaScript"),
                field(SCRIPT_TEXT, encodeScript),
                field(SCRIPT_CONTEXT, "POLICY_CONDITION")));
        UpdateRequest updateRequest = mock(UpdateRequest.class);
        when(updateRequest.getContent()).thenReturn(requestJson);

        // when
        scriptResource.updateInstance(serverContext, resourceId, updateRequest, resultHandler);

        // then
        assertNull(resultHandler.getResult());
        assertNotNull(resultHandler.getError());
        assertNotNull(resultHandler.getError().getMessage());
        assertEquals(resultHandler.getError().getCode(), ResourceException.BAD_REQUEST);
    }

    @Test
    public void shouldFailIfNoContextIsSuppliedOnUpdate() {
        // given
        String resourceId = "1234567890";
        MockResultHandler<Resource> resultHandler = new MockResultHandler<Resource>();
        JsonValue requestJson = json(object(field(SCRIPT_NAME, "MyJavaScript"), field(SCRIPT_TEXT, "var a = 123;var b" +
                " = 456;"), field(SCRIPT_LANGUAGE, "JAVASCRIPT")));
        UpdateRequest updateRequest = mock(UpdateRequest.class);
        when(updateRequest.getContent()).thenReturn(requestJson);

        // when
        scriptResource.updateInstance(serverContext, resourceId, updateRequest, resultHandler);

        // then
        assertNull(resultHandler.getResult());
        assertNotNull(resultHandler.getError());
        assertNotNull(resultHandler.getError().getMessage());
        assertEquals(resultHandler.getError().getCode(), ResourceException.BAD_REQUEST);
    }

    @Test
    public void shouldQueryScriptConfigurationWithPaging() throws ScriptException {
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

        MockQueryResultHandler resultHandler = new MockQueryResultHandler();
        QueryRequest queryRequest = mock(QueryRequest.class);
        when(queryRequest.getPageSize()).thenReturn(5);

        // when
        when(queryRequest.getPagedResultsOffset()).thenReturn(0);
        scriptResource.queryCollection(serverContext, queryRequest, resultHandler);

        // then
        assertThat(resultHandler.getError()).isNull();
        assertThat(resultHandler.getResources()).isNotNull();
        assertThat(resultHandler.getResources().size()).isEqualTo(5);
        Set<Resource> resources = resultHandler.getResources();
        int count = 0;
        for (Resource resource : resources) {
            assertThat(resource.getContent().get(SCRIPT_NAME).asString()).endsWith(String.valueOf(count++));
        }

        // when
        resultHandler.getResources().clear();
        when(queryRequest.getPagedResultsOffset()).thenReturn(5);
        scriptResource.queryCollection(serverContext, queryRequest, resultHandler);

        // then
        assertThat(resultHandler.getError()).isNull();
        assertThat(resultHandler.getResources()).isNotNull();
        assertThat(resultHandler.getResources().size()).isEqualTo(4);
        resources = resultHandler.getResources();
        for (Resource resource : resources) {
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
    public void shouldPassScriptValidation() {
        // given
        ResultHandler<JsonValue> resultHandler = mock(ResultHandler.class);
        ActionRequest request = prepareActionRequestForValidate("var a = 123;var b = 456;", "JAVASCRIPT", "validate");

        // when
        scriptResource.actionCollection(serverContext, request, resultHandler);

        // then
        verify(resultHandler, times(0)).handleError(any(ResourceException.class));

        ArgumentCaptor<JsonValue> resultCaptor = ArgumentCaptor.forClass(JsonValue.class);
        verify(resultHandler, times(1)).handleResult(resultCaptor.capture());
        assertThat(resultCaptor.getValue().get("success").asBoolean()).isEqualTo(true);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldFailScriptValidationWithOneError() {
        // given
        ResultHandler<JsonValue> resultHandler = mock(ResultHandler.class);
        ActionRequest request = prepareActionRequestForValidate(
                "var a = 123;var b = 456; =VALIDATION SHOULD FAIL=", "JAVASCRIPT", "validate");

        // when
        scriptResource.actionCollection(serverContext, request, resultHandler);

        // then
        verify(resultHandler, times(0)).handleError(any(ResourceException.class));

        ArgumentCaptor<JsonValue> resultCaptor = ArgumentCaptor.forClass(JsonValue.class);
        verify(resultHandler, times(1)).handleResult(resultCaptor.capture());
        assertThat(resultCaptor.getValue().get("success").asBoolean()).isEqualTo(false);
        assertThat(resultCaptor.getValue().get("errors").get("line")).isNotNull();
        assertThat(resultCaptor.getValue().get("errors").get("column")).isNotNull();
        assertThat(resultCaptor.getValue().get("errors").get("message")).isNotNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldReturnErrorWhenLanguageNotRecognised() {
        // given
        ResultHandler<JsonValue> resultHandler = mock(ResultHandler.class);
        ActionRequest request = prepareActionRequestForValidate("var a = 123;var b = 456;", "INVALID_LANG", "validate");

        // when
        scriptResource.actionCollection(serverContext, request, resultHandler);

        // then
        verify(resultHandler, times(0)).handleResult(any(JsonValue.class));

        ArgumentCaptor<ResourceException> resultCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(resultHandler, times(1)).handleError(resultCaptor.capture());
        assertThat(resultCaptor.getValue().getCode()).isEqualTo(ResourceException.BAD_REQUEST);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldReturnErrorWhenActionNotRecognised() {
        // given
        ResultHandler<JsonValue> resultHandler = mock(ResultHandler.class);
        ActionRequest request = prepareActionRequestForValidate("var a = 123;var b = 456;", "JAVASCRIPT", "invalid_action");

        // when
        scriptResource.actionCollection(serverContext, request, resultHandler);

        // then
        verify(resultHandler, times(0)).handleResult(any(JsonValue.class));

        ArgumentCaptor<ResourceException> resultCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(resultHandler, times(1)).handleError(resultCaptor.capture());
        assertThat(resultCaptor.getValue().getCode()).isEqualTo(ResourceException.NOT_SUPPORTED);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldReturnErrorWhenNoLanguageSpecified() {
        // given
        ResultHandler<JsonValue> resultHandler = mock(ResultHandler.class);
        String encodeScript = Base64.encode("var a = 123;var b = 456;".getBytes());
        JsonValue requestJson = json(object(field(SCRIPT_TEXT, encodeScript)));
        ActionRequest request = mock(ActionRequest.class);
        when(request.getContent()).thenReturn(requestJson);
        when(request.getAction()).thenReturn("validate");

        // when
        scriptResource.actionCollection(serverContext, request, resultHandler);

        // then
        verify(resultHandler, times(0)).handleResult(any(JsonValue.class));

        ArgumentCaptor<ResourceException> resultCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(resultHandler, times(1)).handleError(resultCaptor.capture());
        assertThat(resultCaptor.getValue().getCode()).isEqualTo(ResourceException.BAD_REQUEST);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldReturnErrorWhenNoScriptSpecified() {
        // given
        ResultHandler<JsonValue> resultHandler = mock(ResultHandler.class);
        JsonValue requestJson = json(object(field(SCRIPT_LANGUAGE, "JAVASCRIPT")));
        ActionRequest request = mock(ActionRequest.class);
        when(request.getContent()).thenReturn(requestJson);
        when(request.getAction()).thenReturn("validate");

        // when
        scriptResource.actionCollection(serverContext, request, resultHandler);

        // then
        verify(resultHandler, times(0)).handleResult(any(JsonValue.class));

        ArgumentCaptor<ResourceException> resultCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(resultHandler, times(1)).handleError(resultCaptor.capture());
        assertThat(resultCaptor.getValue().getCode()).isEqualTo(ResourceException.BAD_REQUEST);
    }
}

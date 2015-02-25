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

package org.forgerock.oauth2.restlet.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.forgerock.json.fluent.JsonValue.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.forgerock.oauth2.resources.ResourceSetStore;
import org.forgerock.openam.cts.api.fields.ResourceSetTokenField;
import org.forgerock.util.query.BaseQueryFilterVisitor;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Conditions;
import org.restlet.data.Status;
import org.restlet.data.Tag;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ResourceSetRegistrationEndpointTest {

    private static final JsonValue RESOURCE_SET_DESCRIPTION_CONTENT = json(object(field("name", "NAME"),
            field("uri", "URI"), field("type", "TYPE"), field("scopes", array("SCOPE")),
            field("icon_uri", "ICON_URI")));
    private static final JsonValue RESOURCE_SET_DESCRIPTION_UPDATED_CONTENT = json(object(field("name", "NEW_NAME"),
            field("uri", "NEW_URI"), field("type", "NEW_TYPE"), field("scopes", array("NEW_SCOPE")),
            field("icon_uri", "NEW_ICON_URI")));

    private ResourceSetRegistrationEndpoint endpoint;

    private ResourceSetStore store;
    private ResourceSetDescriptionValidator validator;
    private ResourceSetRegistrationListener listener;

    private Response response;

    @BeforeMethod
    @SuppressWarnings("unchecked")
    public void setup() throws ServerException, InvalidGrantException, NotFoundException {

        store = mock(ResourceSetStore.class);
        validator = mock(ResourceSetDescriptionValidator.class);
        OAuth2RequestFactory<Request> requestFactory = mock(OAuth2RequestFactory.class);
        Set<ResourceSetRegistrationListener> listeners = new HashSet<ResourceSetRegistrationListener>();
        listener = mock(ResourceSetRegistrationListener.class);
        listeners.add(listener);

        OAuth2ProviderSettingsFactory providerSettingsFactory = mock(OAuth2ProviderSettingsFactory.class);
        OAuth2ProviderSettings providerSettings = mock(OAuth2ProviderSettings.class);
        given(providerSettingsFactory.get(Matchers.<OAuth2Request>anyObject())).willReturn(providerSettings);
        given(providerSettings.getResourceSetStore()).willReturn(store);

        endpoint = spy(new ResourceSetRegistrationEndpoint(providerSettingsFactory, validator, requestFactory,
                listeners));

        Request request = mock(Request.class);
        ChallengeResponse challengeResponse = new ChallengeResponse(ChallengeScheme.HTTP_BASIC);
        challengeResponse.setRawValue("PAT");
        given(request.getChallengeResponse()).willReturn(challengeResponse);
        given(endpoint.getRequest()).willReturn(request);

        AccessToken accessToken = mock(AccessToken.class);
        given(accessToken.getClientId()).willReturn("CLIENT_ID");

        response = mock(Response.class);
        given(endpoint.getResponse()).willReturn(response);

        OAuth2Request oAuth2Request = mock(OAuth2Request.class);
        given(requestFactory.create(Matchers.<Request>anyObject())).willReturn(oAuth2Request);
        given(oAuth2Request.getToken(AccessToken.class)).willReturn(accessToken);
    }

    private void setUriResourceSetId() {
        Map<String, Object> requestAttributes = new ConcurrentHashMap<String, Object>();
        requestAttributes.put("rsid", "RESOURCE_SET_ID");
        given(endpoint.getRequestAttributes()).willReturn(requestAttributes);
    }

    private void noUriResourceSetId() {
        Map<String, Object> requestAttributes = new ConcurrentHashMap<String, Object>();
        given(endpoint.getRequestAttributes()).willReturn(requestAttributes);
    }

    private void addCondition() {
        Conditions conditions = new Conditions();
        conditions.setMatch(Collections.singletonList(new Tag()));
        given(endpoint.getConditions()).willReturn(conditions);
    }

    private void noConditions() {
        Conditions conditions = new Conditions();
        conditions.setMatch(Collections.<Tag>emptyList());
        given(endpoint.getConditions()).willReturn(conditions);
    }

    private JsonRepresentation createCreateRequestRepresentation() throws JSONException,
            JsonProcessingException, BadRequestException {
        JsonRepresentation entity = mock(JsonRepresentation.class);
        JSONObject jsonObject = mock(JSONObject.class);
        String jsonString = new ObjectMapper().writeValueAsString(RESOURCE_SET_DESCRIPTION_CONTENT.asMap());

        given(entity.getJsonObject()).willReturn(jsonObject);
        given(jsonObject.toString()).willReturn(jsonString);
        given(validator.validate(anyMapOf(String.class, Object.class)))
                .willReturn(RESOURCE_SET_DESCRIPTION_CONTENT.asMap());

        return entity;
    }

    private JsonRepresentation createUpdateRequestRepresentation() throws JSONException,
            JsonProcessingException, BadRequestException {
        JsonRepresentation entity = mock(JsonRepresentation.class);
        JSONObject jsonObject = mock(JSONObject.class);
        String jsonString = new ObjectMapper().writeValueAsString(RESOURCE_SET_DESCRIPTION_UPDATED_CONTENT.asMap());

        given(entity.getJsonObject()).willReturn(jsonObject);
        given(jsonObject.toString()).willReturn(jsonString);
        given(validator.validate(anyMapOf(String.class, Object.class)))
                .willReturn(RESOURCE_SET_DESCRIPTION_UPDATED_CONTENT.asMap());

        return entity;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldCreateResourceSetDescription() throws Exception {

        //Given
        JsonRepresentation entity = createCreateRequestRepresentation();

        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ResourceSetDescription resourceSetDescription = (ResourceSetDescription) invocation.getArguments()[1];
                resourceSetDescription.setId("123");
                return null;
            }
        }).when(store).create(any(OAuth2Request.class), any(ResourceSetDescription.class));

        setUriResourceSetId();
        noConditions();

        //When
        Representation response = endpoint.createOrUpdateResourceSet(entity);

        //Then
        ArgumentCaptor<ResourceSetDescription> resourceSetCaptor =
                ArgumentCaptor.forClass(ResourceSetDescription.class);
        verify(store).create(Matchers.<OAuth2Request>anyObject(), resourceSetCaptor.capture());
        assertThat(resourceSetCaptor.getValue().getId()).isNotNull().isNotEmpty();
        assertThat(resourceSetCaptor.getValue().getResourceSetId()).isEqualTo("RESOURCE_SET_ID");
        assertThat(resourceSetCaptor.getValue().getClientId()).isEqualTo("CLIENT_ID");
        assertThat(resourceSetCaptor.getValue().getName()).isEqualTo("NAME");
        assertThat(resourceSetCaptor.getValue().getUri()).isEqualTo(URI.create("URI"));
        assertThat(resourceSetCaptor.getValue().getType()).isEqualTo("TYPE");
        assertThat(resourceSetCaptor.getValue().getScopes()).containsExactly("SCOPE");
        assertThat(resourceSetCaptor.getValue().getIconUri()).isEqualTo(URI.create("ICON_URI"));

        Map<String, Object> responseBody = (Map<String, Object>) new ObjectMapper()
                .readValue(response.getText(), Map.class);
        assertThat(responseBody).containsKey("_id");
        verify(listener).resourceSetCreated(anyString(), Matchers.<ResourceSetDescription>anyObject());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldReadResourceSetDescription() throws Exception {

        //Given
        ResourceSetDescription resourceSetDescription = new ResourceSetDescription("ID", "RESOURCE_SET_ID", "CLIENT_ID",
                "RESOURCE_OWNER_ID", RESOURCE_SET_DESCRIPTION_CONTENT.asMap());

        setUriResourceSetId();
        given(store.read("RESOURCE_SET_ID", "CLIENT_ID")).willReturn(resourceSetDescription);

        //When
        Representation responseRep = endpoint.readOrListResourceSet();

        //Then
        Map<String, Object> responseBody = (Map<String, Object>) new ObjectMapper()
                .readValue(responseRep.getText(), Map.class);
        assertThat(responseBody).containsKey("_id");
        assertThat(responseBody).contains(entry("name", "NAME"), entry("uri", "URI"), entry("type", "TYPE"),
                entry("scopes", Collections.singletonList("SCOPE")), entry("icon_uri", "ICON_URI"));
    }

    @Test
    public void shouldUpdateResourceSetDescription() throws Exception {

        //Given
        JsonRepresentation entity = createUpdateRequestRepresentation();
        ResourceSetDescription resourceSetDescription = new ResourceSetDescription("ID", "RESOURCE_SET_ID", "CLIENT_ID",
                "RESOURCE_OWNER_ID", RESOURCE_SET_DESCRIPTION_CONTENT.asMap());

        setUriResourceSetId();
        addCondition();
        given(store.read("RESOURCE_SET_ID", "CLIENT_ID")).willReturn(resourceSetDescription);

        //When
        Representation responseRep = endpoint.createOrUpdateResourceSet(entity);

        //Then
        ArgumentCaptor<ResourceSetDescription> resourceSetCaptor =
                ArgumentCaptor.forClass(ResourceSetDescription.class);
        verify(store).update(resourceSetCaptor.capture());
        assertThat(resourceSetCaptor.getValue().getId()).isNotNull().isNotEmpty();
        assertThat(resourceSetCaptor.getValue().getResourceSetId()).isEqualTo("RESOURCE_SET_ID");
        assertThat(resourceSetCaptor.getValue().getClientId()).isEqualTo("CLIENT_ID");
        assertThat(resourceSetCaptor.getValue().getName()).isEqualTo("NEW_NAME");
        assertThat(resourceSetCaptor.getValue().getUri()).isEqualTo(URI.create("NEW_URI"));
        assertThat(resourceSetCaptor.getValue().getType()).isEqualTo("NEW_TYPE");
        assertThat(resourceSetCaptor.getValue().getScopes()).containsExactly("NEW_SCOPE");
        assertThat(resourceSetCaptor.getValue().getIconUri()).isEqualTo(URI.create("NEW_ICON_URI"));

        assertThat(responseRep.getText()).isNull();
        ArgumentCaptor<Status> responseStatusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(response).setStatus(responseStatusCaptor.capture());
        assertThat(responseStatusCaptor.getValue().getCode()).isEqualTo(204);
    }

    @Test
    public void shouldDeleteResourceSetDescription() throws Exception {

        //Given
        setUriResourceSetId();
        addCondition();

        //When
        Representation responseRep = endpoint.deleteResourceSet();

        //Then
        verify(store).delete("RESOURCE_SET_ID", "CLIENT_ID");
        assertThat(responseRep.getText()).isNull();
        ArgumentCaptor<Status> responseStatusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(response).setStatus(responseStatusCaptor.capture());
        assertThat(responseStatusCaptor.getValue().getCode()).isEqualTo(204);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldListResourceSetDescriptions() throws Exception {

        //Given
        Set<ResourceSetDescription> resourceSetDescriptions = new HashSet<ResourceSetDescription>();
        ResourceSetDescription resourceSetDescription = new ResourceSetDescription("ID", "RESOURCE_SET_ID", "CLIENT_ID",
                "RESOURCE_OWNER_ID", RESOURCE_SET_DESCRIPTION_CONTENT.asMap());
        ResourceSetDescription resourceSetDescription2 = new ResourceSetDescription("ID_2", "RESOURCE_SET_ID_2",
                "CLIENT_ID",
                "RESOURCE_OWNER_ID", RESOURCE_SET_DESCRIPTION_UPDATED_CONTENT.asMap());
        resourceSetDescriptions.add(resourceSetDescription);
        resourceSetDescriptions.add(resourceSetDescription2);

        noUriResourceSetId();
        noConditions();
        given(store.query(any(QueryFilter.class)))
                .willReturn(resourceSetDescriptions);

        //When
        Representation responseRep = endpoint.readOrListResourceSet();

        //Then
        ArgumentCaptor<QueryFilter> queryParametersCaptor =
                ArgumentCaptor.forClass(QueryFilter.class);
        verify(store).query(queryParametersCaptor.capture());
        QueryFilter<String> query = queryParametersCaptor.getValue();
        Map<String, String> params = query.accept(QUERY_PARAMS_EXTRACTOR, new HashMap<String, String>());
        assertThat(params).containsExactly(
                entry(ResourceSetTokenField.CLIENT_ID, "CLIENT_ID"));

        List<String> responseBody = (List<String>) new ObjectMapper()
                .readValue(responseRep.getText(), List.class);
        assertThat(responseBody).contains("RESOURCE_SET_ID", "RESOURCE_SET_ID_2");
    }

    private static final QueryFilterVisitor<Map<String, String>, Map<String, String>, String> QUERY_PARAMS_EXTRACTOR =
            new BaseQueryFilterVisitor<Map<String, String>, Map<String, String>, String>() {
                public Map<String, String> visitEqualsFilter(Map<String, String> map, String field, Object value) {
                    map.put(field, value.toString());
                    return map;
                }
            };
}

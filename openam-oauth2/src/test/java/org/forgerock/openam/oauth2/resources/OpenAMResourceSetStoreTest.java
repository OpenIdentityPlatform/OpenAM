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

package org.forgerock.openam.oauth2.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.forgerock.openam.cts.api.fields.ResourceSetTokenField;
import org.forgerock.openam.cts.api.tokens.TokenIdGenerator;
import org.forgerock.openam.sm.datalayer.store.TokenDataStore;
import org.forgerock.util.query.QueryFilter;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class OpenAMResourceSetStoreTest {

    private OpenAMResourceSetStore store;

    private TokenDataStore<ResourceSetDescription> dataStore;

    @BeforeMethod
    @SuppressWarnings("unchecked")
    public void setup() throws NotFoundException {

        dataStore = mock(TokenDataStore.class);
        OAuth2ProviderSettingsFactory providerSettingsFactory = mock(OAuth2ProviderSettingsFactory.class);
        OAuth2ProviderSettings providerSettings = mock(OAuth2ProviderSettings.class);
        TokenIdGenerator idGenerator = mock(TokenIdGenerator.class);

        store = new OpenAMResourceSetStore("REALM", providerSettingsFactory, idGenerator, dataStore);

        given(providerSettingsFactory.get(Matchers.<OAuth2Request>anyObject())).willReturn(providerSettings);
        given(providerSettings.getResourceSetRegistrationPolicyEndpoint(anyString())).willReturn("POLICY_URI");
    }

    @Test(enabled = false, expectedExceptions = BadRequestException.class)
    public void shouldNotCreateDuplicateResourceSetWithSameId() throws Exception {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        ResourceSetDescription resourceSetDescription =
                new ResourceSetDescription("RESOURCE_SET_ID", "CLIENT_ID", "RESOURCE_OWNER_ID",
                        Collections.<String, Object>singletonMap("name", "RESOURCE_SET_NAME"));

        resourceSetDescription.setRealm("REALM");
        given(dataStore.query(Matchers.<QueryFilter<String>>anyObject()))
                .willReturn(Collections.singleton(resourceSetDescription));

        //When
        try {
            store.create(request, resourceSetDescription);
        } catch (BadRequestException e) {
            //Then
            assertThat(resourceSetDescription.getPolicyUri()).isNull();
            verify(dataStore, never()).create(any(ResourceSetDescription.class));
            throw e;
        }
    }

    @Test
    public void shouldCreateResourceSetToken() throws Exception {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        ResourceSetDescription resourceSetDescription =
                new ResourceSetDescription("RESOURCE_SET_ID", "CLIENT_ID", "RESOURCE_OWNER_ID",
                        Collections.<String, Object>singletonMap("name", "RESOURCE_SET_NAME"));

        given(dataStore.query(Matchers.<QueryFilter<String>>anyObject()))
                .willReturn(Collections.<ResourceSetDescription>emptySet());

        //When
        store.create(request, resourceSetDescription);

        //Then
        assertThat(resourceSetDescription.getPolicyUri()).isEqualTo("POLICY_URI");
        verify(dataStore).create(resourceSetDescription);
    }

    @Test
    public void shouldReadResourceSetToken() throws Exception {

        //Given
        ResourceSetDescription resourceSetDescription =
                new ResourceSetDescription("RESOURCE_SET_ID", "CLIENT_ID", "RESOURCE_OWNER_ID",
                        Collections.<String, Object>emptyMap());

        given(dataStore.query(
                QueryFilter.and(
                        QueryFilter.and(
                                QueryFilter.equalTo(ResourceSetTokenField.RESOURCE_SET_ID, "RESOURCE_SET_ID"),
                                QueryFilter.equalTo(ResourceSetTokenField.RESOURCE_OWNER_ID, "RESOURCE_OWNER_ID")),
                        QueryFilter.equalTo(ResourceSetTokenField.REALM, "REALM"))))
                .willReturn(Collections.singleton(resourceSetDescription));

        //When
        ResourceSetDescription readResourceSetDescription = store.read("RESOURCE_SET_ID", "RESOURCE_OWNER_ID");

        //Then
        assertThat(readResourceSetDescription).isEqualTo(readResourceSetDescription);
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void readWithResourceSetUidShouldThrowNotFoundExceptionWhenMultipleResourceSetsFound() throws Exception {

        //Given
        given(dataStore.read("123")).willThrow(new org.forgerock.openam.sm.datalayer.store.NotFoundException("not found"));

        //When
        store.read("123", "RESOURCE_OWNER_ID");

        //Then
        //Excepted NotFoundException
    }

    @Test
    public void shouldUpdateResourceSetToken() throws Exception {

        //Given
        ResourceSetDescription resourceSetDescription =
                new ResourceSetDescription("RESOURCE_SET_ID", "CLIENT_ID", "RESOURCE_OWNER_ID",
                        Collections.<String, Object>emptyMap());

        resourceSetDescription.setRealm("REALM");
        given(dataStore.query(
                QueryFilter.and(
                        QueryFilter.and(
                                QueryFilter.equalTo(ResourceSetTokenField.RESOURCE_SET_ID, "RESOURCE_SET_ID"),
                                QueryFilter.equalTo(ResourceSetTokenField.RESOURCE_OWNER_ID, "RESOURCE_OWNER_ID")),
                        QueryFilter.equalTo(ResourceSetTokenField.REALM, "REALM"))))
                .willReturn(Collections.singleton(resourceSetDescription));

        //When
        store.update(resourceSetDescription);

        //Then
        verify(dataStore).update(resourceSetDescription);
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void shouldNotDeleteResourceSetTokenIfResourceSetNotFound() throws Exception {

        //Given

        //When
        store.delete("RESOURCE_SET_ID", "RESOURCE_OWNER_ID");

        //Then
        //Excepted NotFoundException
    }

    @Test
    public void shouldDeleteResourceSetToken() throws Exception {

        //Given
        ResourceSetDescription resourceSetDescription = new ResourceSetDescription();

        resourceSetDescription.setId("RESOURCE_SET_ID");
        resourceSetDescription.setResourceOwnerId("RESOURCE_OWNER_ID");
        resourceSetDescription.setRealm("REALM");
        given(dataStore.query(
                QueryFilter.and(
                        QueryFilter.and(
                                QueryFilter.equalTo(ResourceSetTokenField.RESOURCE_SET_ID, "RESOURCE_SET_ID"),
                                QueryFilter.equalTo(ResourceSetTokenField.RESOURCE_OWNER_ID, "RESOURCE_OWNER_ID")),
                        QueryFilter.equalTo(ResourceSetTokenField.REALM, "REALM"))))
                .willReturn(Collections.singleton(resourceSetDescription));

        //When
        store.delete("RESOURCE_SET_ID", "RESOURCE_OWNER_ID");

        //Then
        verify(dataStore).delete("RESOURCE_SET_ID");
    }

    @Test
    public void shouldQueryResourceSetToken() throws Exception {

        //Given
        Map<String, Object> queryParameters = new HashMap<String, Object>();
        queryParameters.put(ResourceSetTokenField.CLIENT_ID, "CLIENT_ID");
        ResourceSetDescription resourceSet1 =
                new ResourceSetDescription("123", "CLIENT_ID", "RESOURCE_OWNER_ID",
                        Collections.<String, Object>emptyMap());
        ResourceSetDescription resourceSet2 =
                new ResourceSetDescription("456", "CLIENT_ID", "RESOURCE_OWNER_ID",
                        Collections.<String, Object>emptyMap());

        given(dataStore.query(Matchers.<QueryFilter<String>>anyObject()))
                .willReturn(asSet(resourceSet1, resourceSet2));
        resourceSet1.setRealm("REALM");
        resourceSet2.setRealm("REALM");

        //When
        QueryFilter<String> query = QueryFilter.alwaysTrue();
        Set<ResourceSetDescription> resourceSetDescriptions = store.query(query);

        //Then
        assertThat(resourceSetDescriptions).contains(resourceSet1, resourceSet2);
        ArgumentCaptor<QueryFilter> tokenFilterCaptor = ArgumentCaptor.forClass(QueryFilter.class);
        verify(dataStore).query(tokenFilterCaptor.capture());
        assertThat(tokenFilterCaptor.getValue()).isEqualTo(QueryFilter.and(query,
                QueryFilter.equalTo(ResourceSetTokenField.REALM, "REALM")));
    }
}

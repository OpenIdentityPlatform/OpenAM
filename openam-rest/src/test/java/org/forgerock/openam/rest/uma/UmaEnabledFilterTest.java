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

package org.forgerock.openam.rest.uma;

import static org.assertj.core.api.Assertions.*;
import static org.forgerock.json.JsonValue.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import javax.servlet.http.HttpServletRequest;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalContext;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.http.context.RootContext;
import org.forgerock.http.Context;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.openam.forgerockrest.utils.RequestHolder;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.uma.UmaProviderSettings;
import org.forgerock.openam.uma.UmaProviderSettingsFactory;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class UmaEnabledFilterTest {

    private static UmaProviderSettingsFactory notYetConfiguredFactory;
    private static UmaProviderSettingsFactory notEnabledFactory;
    private static UmaProviderSettingsFactory enabledFactory;
    private Context context;
    private RequestHandler requestHandler;

    @BeforeClass
    public static void setupFactories() throws Exception {
        notYetConfiguredFactory = mock(UmaProviderSettingsFactory.class);
        given(notYetConfiguredFactory.get(any(HttpServletRequest.class), anyString()))
                .willThrow(NotFoundException.class);
        UmaProviderSettings notEnabled = mock(UmaProviderSettings.class);
        given(notEnabled.isEnabled()).willReturn(false);
        notEnabledFactory = mock(UmaProviderSettingsFactory.class);
        given(notEnabledFactory.get(any(HttpServletRequest.class), anyString())).willReturn(notEnabled);
        UmaProviderSettings enabled = mock(UmaProviderSettings.class);
        given(enabled.isEnabled()).willReturn(true);
        enabledFactory = mock(UmaProviderSettingsFactory.class);
        given(enabledFactory.get(any(HttpServletRequest.class), anyString())).willReturn(enabled);
    }

    @BeforeMethod
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        context = new InternalContext(new RealmContext(new RootContext()));
        requestHandler = mock(RequestHandler.class);
        RequestHolder.set(mock(HttpServletRequest.class));
    }

    @DataProvider
    public Object[][] factories() {
        return new Object[][] {
                new Object[] { notYetConfiguredFactory, true },
                new Object[] { notEnabledFactory, true },
                new Object[] { enabledFactory, false }
        };
    }

    @Test(dataProvider = "factories")
    public void testFilterAction(UmaProviderSettingsFactory factory, boolean expectFailure) throws Exception {
        // Given
        UmaEnabledFilter filter = new UmaEnabledFilter(factory);
        ResultHandler<JsonValue> resultHandler = mock(ResultHandler.class);
        ActionRequest request = Requests.newActionRequest("test", "test", "test");

        // When
        filter.filterAction(context, request, resultHandler, requestHandler);

        // Then
        checkResult(expectFailure, resultHandler);
        if (expectFailure) {
            verifyNoMoreInteractions(requestHandler);
        } else {
            verify(requestHandler).handleAction(context, request, resultHandler);
        }
    }

    private void checkResult(boolean expectFailure, ResultHandler<?> resultHandler) {
        if (expectFailure) {
            ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
            verify(resultHandler).handleError(captor.capture());
            assertThat(captor.getValue()).isInstanceOf(NotSupportedException.class);
        } else {
            verifyNoMoreInteractions(resultHandler);
        }
    }

    @Test(dataProvider = "factories")
    public void testFilterCreate(UmaProviderSettingsFactory factory, boolean expectFailure) throws Exception {
        // Given
        UmaEnabledFilter filter = new UmaEnabledFilter(factory);
        ResultHandler<Resource> resultHandler = mock(ResultHandler.class);
        CreateRequest request = Requests.newCreateRequest("test", json(object()));

        // When
        filter.filterCreate(context, request, resultHandler, requestHandler);

        // Then
        checkResult(expectFailure, resultHandler);
        if (expectFailure) {
            verifyNoMoreInteractions(requestHandler);
        } else {
            verify(requestHandler).handleCreate(context, request, resultHandler);
        }
    }

    @Test(dataProvider = "factories")
    public void testFilterDelete(UmaProviderSettingsFactory factory, boolean expectFailure) throws Exception {
        // Given
        UmaEnabledFilter filter = new UmaEnabledFilter(factory);
        ResultHandler<Resource> resultHandler = mock(ResultHandler.class);
        DeleteRequest request = Requests.newDeleteRequest("test");

        // When
        filter.filterDelete(context, request, resultHandler, requestHandler);

        // Then
        checkResult(expectFailure, resultHandler);
        if (expectFailure) {
            verifyNoMoreInteractions(requestHandler);
        } else {
            verify(requestHandler).handleDelete(context, request, resultHandler);
        }
    }

    @Test(dataProvider = "factories")
    public void testFilterPatch(UmaProviderSettingsFactory factory, boolean expectFailure) throws Exception {
        // Given
        UmaEnabledFilter filter = new UmaEnabledFilter(factory);
        ResultHandler<Resource> resultHandler = mock(ResultHandler.class);
        PatchRequest request = Requests.newPatchRequest("test", "test");

        // When
        filter.filterPatch(context, request, resultHandler, requestHandler);

        // Then
        checkResult(expectFailure, resultHandler);
        if (expectFailure) {
            verifyNoMoreInteractions(requestHandler);
        } else {
            verify(requestHandler).handlePatch(context, request, resultHandler);
        }
    }

    @Test(dataProvider = "factories")
    public void testFilterQuery(UmaProviderSettingsFactory factory, boolean expectFailure) throws Exception {
        // Given
        UmaEnabledFilter filter = new UmaEnabledFilter(factory);
        QueryResultHandler resultHandler = mock(QueryResultHandler.class);
        QueryRequest request = Requests.newQueryRequest("test");

        // When
        filter.filterQuery(context, request, resultHandler, requestHandler);

        // Then
        checkResult(expectFailure, resultHandler);
        if (expectFailure) {
            verifyNoMoreInteractions(requestHandler);
        } else {
            verify(requestHandler).handleQuery(context, request, resultHandler);
        }
    }

    @Test(dataProvider = "factories")
    public void testFilterRead(UmaProviderSettingsFactory factory, boolean expectFailure) throws Exception {
        // Given
        UmaEnabledFilter filter = new UmaEnabledFilter(factory);
        ResultHandler<Resource> resultHandler = mock(ResultHandler.class);
        ReadRequest request = Requests.newReadRequest("test");

        // When
        filter.filterRead(context, request, resultHandler, requestHandler);

        // Then
        checkResult(expectFailure, resultHandler);
        if (expectFailure) {
            verifyNoMoreInteractions(requestHandler);
        } else {
            verify(requestHandler).handleRead(context, request, resultHandler);
        }
    }

    @Test(dataProvider = "factories")
    public void testFilterUpdate(UmaProviderSettingsFactory factory, boolean expectFailure) throws Exception {
        // Given
        UmaEnabledFilter filter = new UmaEnabledFilter(factory);
        ResultHandler<Resource> resultHandler = mock(ResultHandler.class);
        UpdateRequest request = Requests.newUpdateRequest("test", json(object()));

        // When
        filter.filterUpdate(context, request, resultHandler, requestHandler);

        // Then
        checkResult(expectFailure, resultHandler);
        if (expectFailure) {
            verifyNoMoreInteractions(requestHandler);
        } else {
            verify(requestHandler).handleUpdate(context, request, resultHandler);
        }
    }
}
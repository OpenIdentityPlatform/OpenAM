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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.core.rest.sms;


import static org.forgerock.json.resource.test.assertj.AssertJResourceResponseAssert.assertThat;
import static org.forgerock.openam.core.rest.sms.SmsRealmProvider.*;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;

import org.assertj.core.api.Assertions;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.utils.RealmNormaliser;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Created by ken.stubbings on 30/11/2015.
 */
public class SmsRealmProviderTest {

    @Mock
    private UpdateRequest mockUpdateRequest;
    @Mock
    private JsonValue updateContent;
    @Mock
    private JsonValue pathAttributeJasonValue;
    @Mock
    private JsonValue realmNameAttributeJasonValue;
    @Mock
    private RealmNormaliser mockRealmNormaliser;
    @Mock
    private Context context;
    @Mock
    private JsonValue createContent;
    @Mock
    private JsonValue realmName;
    @Mock
    private JsonValue parentPath;
    @Mock
    private JsonValue active;
    @Mock
    private JsonValue aliases;
    @Mock
    private CreateRequest createRequest;

    private SmsRealmProvider provider;

    @BeforeMethod
    public void configureMockValues() throws NoSuchFieldException, IllegalAccessException, NotFoundException {
        initMocks(this);
        provider = new SmsRealmProvider(null, null, mockRealmNormaliser);
        when(mockRealmNormaliser.normalise(Mockito.anyString())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocationOnMock) throws Throwable {
                return (String) invocationOnMock.getArguments()[0];
            }
        });

        when(updateContent.get(same(PATH_ATTRIBUTE_NAME))).thenReturn(pathAttributeJasonValue);
        when(updateContent.get(same(REALM_NAME_ATTRIBUTE_NAME))).thenReturn(realmNameAttributeJasonValue);
        when(mockUpdateRequest.getContent()).thenReturn(updateContent);

        when(realmName.isNotNull()).thenReturn(true);
        when(realmName.asString()).thenReturn("realm");

        when(parentPath.isNotNull()).thenReturn(true);
        when(parentPath.asString()).thenReturn("parent");

        when(active.isNotNull()).thenReturn(true);
        when(active.toString()).thenReturn("notnull");
        when(active.isBoolean()).thenReturn(true);

        when(aliases.isNotNull()).thenReturn(true);
        when(aliases.toString()).thenReturn("notnull");
        when(aliases.isCollection()).thenReturn(true);
        when(aliases.asList(String.class)).thenReturn(Arrays.asList(new String[] {"alias1", "alias2"}));

        when(createContent.get(REALM_NAME_ATTRIBUTE_NAME)).thenReturn(realmName);
        when(createContent.get(PATH_ATTRIBUTE_NAME)).thenReturn(parentPath);
        when(createContent.get(ACTIVE_ATTRIBUTE_NAME)).thenReturn(active);
        when(createContent.get(ALIASES_ATTRIBUTE_NAME)).thenReturn(aliases);

        when(createRequest.getContent()).thenReturn(createContent);
    }

    @Test
    public void testGetExpectedPathFromRequestContext() throws NotFoundException {
        doExpectedPathCheck("path/", "realmName", "path/realmName");
        doExpectedPathCheck("path/", "/realmName", "path/realmName");
        doExpectedPathCheck("path", "/realmName", "path/realmName");
        doExpectedPathCheck("path", "realmName", "path/realmName");
        doExpectedPathCheck("/path", "realmName/", "/path/realmName/");
        doExpectedPathCheck("/path", "/", "/path/");
        doExpectedPathCheck("/path/", "/", "/path/");
        doExpectedPathCheck("/path/", "", "/path/");
        doExpectedPathCheck("/path", "", "/path/");
        doExpectedPathCheck("/path", "parentName/ChildName", "/path/parentName/ChildName");
        doExpectedPathCheck("path", "/parentName/ChildName", "path/parentName/ChildName");
        doExpectedPathCheck("path/", "parentName/ChildName", "path/parentName/ChildName");
        doExpectedPathCheck("path/", "/parentName/ChildName", "path/parentName/ChildName");
    }

    @Test
    public void invalidCharsInReamNameShouldThrowBadRequest() {
        //given
        when(realmName.asString()).thenReturn("/xyz");

        //when
        Promise<ResourceResponse, ResourceException> result = provider.handleCreate(context, createRequest);

        //then
        assertThat(result).failedWithException().isInstanceOf(BadRequestException.class);
    }

    @Test
    public void nullValueForRealmShouldThrowBadRequest() {
        //given
        when(realmName.isNotNull()).thenReturn(false);

        //when
        Promise<ResourceResponse, ResourceException> result = provider.handleCreate(context, createRequest);

        //then
        assertThat(result).failedWithException().isInstanceOf(BadRequestException.class);
    }

    @Test
    public void emptyRealmShouldThrowBadRequest() {
        //given
        when(realmName.asString()).thenReturn("");

        //when
        Promise<ResourceResponse, ResourceException> result = provider.handleCreate(context, createRequest);

        //then
        assertThat(result).failedWithException().isInstanceOf(BadRequestException.class);
    }

    @Test
    public void blankRealmShouldThrowBadRequest() {
        //given
        when(realmName.asString()).thenReturn("  ");

        //when
        Promise<ResourceResponse, ResourceException> result = provider.handleCreate(context, createRequest);

        //then
        assertThat(result).failedWithException().isInstanceOf(BadRequestException.class);
    }

    @Test
    public void whenAliasIsNotACollectionShouldThrowBadRequest() {
        //given
        when(aliases.isCollection()).thenReturn(false);

        //when
        Promise<ResourceResponse, ResourceException> result = provider.handleCreate(context, createRequest);

        //then
        assertThat(result).failedWithException().isInstanceOf(BadRequestException.class);
    }

    @Test
    public void invalidCharsInAliasShouldThrowBadRequest() {
        //given
        when(aliases.asList(String.class)).thenReturn(Arrays.asList(new String[] {"#*?%$#"}));

        //when
        Promise<ResourceResponse, ResourceException> result = provider.handleCreate(context, createRequest);

        //then
        assertThat(result).failedWithException().isInstanceOf(BadRequestException.class);
    }

    private void doExpectedPathCheck(String path, String realmName, String expectedPath) throws NotFoundException {
        // Given
        when(pathAttributeJasonValue.asString()).thenReturn(path);
        when(realmNameAttributeJasonValue.asString()).thenReturn(realmName);
        // When
        String calculatedExpectedPath = provider.getExpectedPathFromRequestContext(mockUpdateRequest);
        // Then
        Assertions.assertThat(calculatedExpectedPath).isEqualTo(expectedPath);
    }

}


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

package org.forgerock.openam.core.rest.sms;

import org.assertj.core.api.Assertions;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.openam.utils.RealmNormaliser;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Created by ken.stubbings on 30/11/2015.
 */
public class SmsRealmProviderTest {

    @Mock
    UpdateRequest mockUpdateRequest;
    @Mock
    JsonValue context;
    @Mock
    JsonValue pathAttributeJasonValue;
    @Mock
    JsonValue realmNameAttributeJasonValue;
    @Mock
    RealmNormaliser mockRealmNormaliser;

    SmsRealmProvider provider;

    @BeforeMethod
    public void configureMockValues() throws NoSuchFieldException, IllegalAccessException, NotFoundException {
        MockitoAnnotations.initMocks(this);
        provider = new SmsRealmProvider(null, null, mockRealmNormaliser);
        Mockito.when(mockRealmNormaliser.normalise(Mockito.anyString())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocationOnMock) throws Throwable {
                return (String) invocationOnMock.getArguments()[0];
            }
        });

        Mockito.when(context.get(Mockito.same(SmsRealmProvider.PATH_ATTRIBUTE_NAME))).thenReturn(pathAttributeJasonValue);
        Mockito.when(context.get(Mockito.same(SmsRealmProvider.REALM_NAME_ATTRIBUTE_NAME))).thenReturn(realmNameAttributeJasonValue);
        Mockito.when(mockUpdateRequest.getContent()).thenReturn(context);
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

    private void doExpectedPathCheck(String path, String realmName, String expectedPath) throws NotFoundException {
        // Given
        Mockito.when(pathAttributeJasonValue.asString()).thenReturn(path);
        Mockito.when(realmNameAttributeJasonValue.asString()).thenReturn(realmName);
        // When
        String calculatedExpectedPath = provider.getExpectedPathFromRequestContext(mockUpdateRequest);
        // Then
        Assertions.assertThat(calculatedExpectedPath).isEqualTo(expectedPath);
    }
}


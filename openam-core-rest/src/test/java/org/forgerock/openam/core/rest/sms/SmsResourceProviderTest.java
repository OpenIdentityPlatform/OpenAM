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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.core.rest.sms;

import static org.forgerock.json.JsonValue.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.json.test.assertj.AssertJJsonValueAssert;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.LocaleContext;
import org.forgerock.services.context.Context;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;


public class SmsResourceProviderTest {

    /* This class currently only tests a small number of methods in the SmsResourceProvider Class
     * Do not presume that the current set of mock objects are suitably configured to test additional methods
     */
    @Mock
    private ServiceSchema serviceSchema;
    @Mock
    private SchemaType schemaType;
    @Mock
    private List<ServiceSchema> subSchemaPath;
    private String uriPath = "";
    @Mock
    private SmsJsonConverter jsonConverter;
    @Mock
    private Debug debug;
    @Mock
    private Context theContext;
    @Mock
    private RealmContext realmContext;
    @Mock
    private LocaleContext localeContext;

    private Locale local = new Locale("Test");

    MySmsResourceProvider resourceProvider;
    private LocaleContext context = new LocaleContext(new HttpContext(
            json(object(field("headers", object()), field("parameters", object()))),
            this.getClass().getClassLoader())) {
        @Override
        public Locale getLocale() {
            return Locale.UK;
        }
    };


    @BeforeTest
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        resourceProvider = new MySmsResourceProvider(serviceSchema, schemaType, subSchemaPath, uriPath, true,
                jsonConverter, debug);
        when(localeContext.getLocale()).thenReturn(local);
        when(theContext.asContext(LocaleContext.class)).thenReturn(localeContext);
    }

    @Test
    public void verifyResourceProviderIsNotNull() {
        Assertions.assertThat(resourceProvider).isNotNull();
    }

    @Test
    public void verifyRelmForReturnsRealmWhenItIsContinaedWithinTheContext() {

        // Given
        String mockReturn = "ToReturn";

        when(theContext.containsContext(RealmContext.class)).thenReturn(true);
        when(realmContext.getResolvedRealm()).thenReturn(mockReturn);
        when(theContext.asContext(RealmContext.class)).thenReturn(realmContext);

        // When
        String returnedRealm = resourceProvider.realmFor(theContext);

        // Then
        Assertions.assertThat(returnedRealm).isEqualTo(mockReturn);
    }

    @Test
    public void verifyExpectedJsonValueIsReturnedByCreateAllSubSchema() throws Exception{

        // Given
        Set<String> preDefinedSubSchemas = new HashSet<>(Arrays.asList("subOne", "subTwo", "subThree"));
        Object jvSubOne = object(
                JsonValue.field("_id", "one"),
                JsonValue.field("name", "Sub Schema One"));
        Object jvSubTwo = object(
                JsonValue.field("_id", "two"),
                JsonValue.field("name", "subTwo"));
        Object jvSubThree = object(
                JsonValue.field("_id", "three"),
                JsonValue.field("name", "subThree"));

        when(serviceSchema.getSubSchemaNames()).thenReturn(preDefinedSubSchemas);
        ServiceSchema subSchemaOne = mock(ServiceSchema.class);
        ServiceSchema subSchemaTwo = mock(ServiceSchema.class);
        ServiceSchema subSchemaThree = mock(ServiceSchema.class);
        when(serviceSchema.getSubSchema("subOne")).thenReturn(subSchemaOne);
        when(serviceSchema.getSubSchema("subTwo")).thenReturn(subSchemaTwo);
        when(serviceSchema.getSubSchema("subThree")).thenReturn(subSchemaThree);

        when(subSchemaOne.getI18NFileName()).thenReturn("org/forgerock/openam/core/rest/sms/SmsResourceProviderTest");
        when(subSchemaTwo.getI18NFileName()).thenReturn("org/forgerock/openam/core/rest/sms/SmsResourceProviderTest");
        when(subSchemaThree.getI18NFileName()).thenReturn("org/forgerock/openam/core/rest/sms/SmsResourceProviderTest");

        when(subSchemaOne.getResourceName()).thenReturn("one");
        when(subSchemaTwo.getResourceName()).thenReturn("two");
        when(subSchemaThree.getResourceName()).thenReturn("three");

        when(subSchemaOne.getI18NKey()).thenReturn("subOne");

        // When
        JsonValue returnedJV = resourceProvider.getAllTypes(context, null);

        // Then
        AssertJJsonValueAssert.assertThat(returnedJV)
                .isObject()
                .containsField("result");

        AssertJJsonValueAssert.assertThat(returnedJV.get("result"))
                .isArray()
                .hasSize(3)
                .containsOnly(jvSubOne, jvSubTwo, jvSubThree);
    }

    /**
     * A Local implementation of the abstract class will allow us to test the protected methods.
     */
    private class MySmsResourceProvider extends SmsResourceProvider {

        MySmsResourceProvider(ServiceSchema schema, SchemaType type, List<ServiceSchema> subSchemaPath, String uriPath,
                              boolean serviceHasInstanceName, SmsJsonConverter converter, Debug debug) {
            super(schema, type, subSchemaPath, uriPath, serviceHasInstanceName, converter, debug);
        }
    }
}
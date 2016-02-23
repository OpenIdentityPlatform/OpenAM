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

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import org.assertj.core.api.Assertions;
import org.forgerock.json.JsonValue;
import org.forgerock.json.test.assertj.AssertJJsonValueAssert;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.LocaleContext;
import org.forgerock.services.context.Context;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


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



    @BeforeTest
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        resourceProvider = new MySmsResourceProvider(serviceSchema, schemaType, subSchemaPath, uriPath, true,
                jsonConverter, debug);
        Mockito.when(localeContext.getLocale()).thenReturn(local);
        Mockito.when(theContext.asContext(LocaleContext.class)).thenReturn(localeContext);
    }

    @Test
    public void verifyResourceProviderIsNotNull() {
        Assertions.assertThat(resourceProvider).isNotNull();
    }

    @Test
    public void verifyRelmForReturnsRealmWhenItIsContinaedWithinTheContext() {

        // Given
        String mockReturn = "ToReturn";

        Mockito.when(theContext.containsContext(RealmContext.class)).thenReturn(true);
        Mockito.when(realmContext.getResolvedRealm()).thenReturn(mockReturn);
        Mockito.when(theContext.asContext(RealmContext.class)).thenReturn(realmContext);

        // When
        String returnedRealm = resourceProvider.realmFor(theContext);

        // Then
        Assertions.assertThat(returnedRealm).isEqualTo(mockReturn);
    }

    @Test
    public void verifyExpectedJsonValueIsReturnedByCreateAllSubSchema() throws Exception{

        // Given
        Set<Object> preDefinedSubSchemas = new HashSet<Object>(Arrays.asList("subOne", "subTwo", "subThree"));
        Object jvSubOne = JsonValue.object(
                JsonValue.field("_id", "subOne"),
                JsonValue.field("name", "subOne"));
        Object jvSubTwo = JsonValue.object(
                JsonValue.field("_id", "subTwo"),
                JsonValue.field("name", "subTwo"));
        Object jvSubThree = JsonValue.object(
                JsonValue.field("_id", "subThree"),
                JsonValue.field("name", "subThree"));

        Mockito.when(serviceSchema.getSubSchemaNames()).thenReturn(preDefinedSubSchemas);

        // When
        JsonValue returnedJV = resourceProvider.getAllTypes(null, null);

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
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
package org.forgerock.openam.entitlement.rest;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.entitlement.rest.ApplicationV1Filter.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.testng.AssertJUnit.*;

import com.sun.identity.entitlement.EntitlementException;
import javax.security.auth.Subject;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.entitlement.ResourceType;
import org.forgerock.openam.entitlement.rest.ApplicationV1FilterTransformer;
import org.forgerock.openam.entitlement.service.ResourceTypeService;
import org.forgerock.openam.errors.ExceptionMappingHandler;
import org.forgerock.openam.rest.resource.ContextHelper;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class ApplicationV1FilterTransformerTest {

    private ApplicationV1FilterTransformer transformer;

    private ContextHelper mockContextHelper;
    private ResourceTypeService mockResourceTypeService;
    private ExceptionMappingHandler<EntitlementException, ResourceException> mockResourceErrorHandler;


    @BeforeTest
    public void theSetUp() { //you need this

        mockContextHelper = mock(ContextHelper.class);
        mockResourceTypeService = mock(ResourceTypeService.class);
        mockResourceErrorHandler = mock(ExceptionMappingHandler.class);

        transformer = new ApplicationV1FilterTransformer(mockContextHelper, mockResourceTypeService,
                mockResourceErrorHandler);

    }

    @Test (expectedExceptions = JsonValueException.class)
    public void failWithNoUUIDs() throws EntitlementException {

        //given
        JsonValue jsonValue = json(object(field("noUUID", "no, really")));
        Subject subject = new Subject();

        //when
        transformer.transformJson(jsonValue, subject, "realm");

        //then -- caught by exception

    }

    @Test (expectedExceptions = EntitlementException.class)
    public void failWithNoResourceType() throws EntitlementException {

        //given
        JsonValue jsonValue = json(object(field(RESOURCE_TYPE_UUIDS, array("abc-def-ghi", "def-ghj-kli"))));
        Subject subject = new Subject();
        given(mockResourceTypeService.getResourceType(eq(subject), eq("realm"), eq("abc-def-ghi")))
                .willReturn(null);

        //when
        transformer.transformJson(jsonValue, subject, "realm");

        //then -- caught by exception

    }

    @Test
    public void testSuccessfulTransformation() throws EntitlementException {

        //given
        JsonValue jsonValue = json(object(field(RESOURCE_TYPE_UUIDS, array("abc-def-ghi", "def-ghj-kli"))));
        Subject subject = new Subject();

        ResourceType resourceType = ResourceType.builder().setName("name").setUUID("uuid").addAction("action", true)
                .addPattern("pattern").build();
        ResourceType resourceType2 = ResourceType.builder().setName("name2").setUUID("uuid2").addAction("action2", true)
                .addPattern("pattern2").build();
        given(mockResourceTypeService.getResourceType(eq(subject), eq("realm"), eq("abc-def-ghi")))
                .willReturn(resourceType);
        given(mockResourceTypeService.getResourceType(eq(subject), eq("realm"), eq("def-ghj-kli")))
                .willReturn(resourceType2);

        //when
        transformer.transformJson(jsonValue, subject, "realm");

        //then
        assertTrue(jsonValue.get(RESOURCE_TYPE_UUIDS).isNull());
        assertTrue(jsonValue.get("actions").get("action").asBoolean());
        assertTrue(jsonValue.get("actions").get("action2").asBoolean());
        assertTrue(jsonValue.get("resources").contains("pattern"));
        assertTrue(jsonValue.get("resources").contains("pattern2"));

    }

}

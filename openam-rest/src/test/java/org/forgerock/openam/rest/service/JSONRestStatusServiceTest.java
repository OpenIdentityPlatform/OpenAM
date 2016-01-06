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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.openam.rest.service;

import java.io.IOException;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import org.forgerock.guice.core.GuiceModules;
import org.forgerock.guice.core.GuiceTestCase;
import org.forgerock.json.resource.ResourceException;
import static org.mockito.Mockito.mock;

import org.forgerock.openam.audit.AuditCoreGuiceModule;
import org.forgerock.openam.audit.configuration.AuditConfigurationGuiceModule;
import org.forgerock.openam.core.guice.CoreGuiceModule;
import org.forgerock.openam.core.guice.DataLayerGuiceModule;
import org.forgerock.openam.rest.RestGuiceModule;
import org.forgerock.openam.rest.representations.JacksonRepresentationFactory;
import org.forgerock.openam.shared.guice.SharedGuiceModule;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.inject.AbstractModule;

@GuiceModules({JSONRestStatusServiceTest.TestGuiceModule.class, SharedGuiceModule.class, CoreGuiceModule.class,
        RestGuiceModule.class, AuditCoreGuiceModule.class, AuditConfigurationGuiceModule.class,
        DataLayerGuiceModule.class})
public class JSONRestStatusServiceTest extends GuiceTestCase {

    private RestStatusService restStatusService;

    @BeforeMethod
    public void setUp() {
        restStatusService = new JSONRestStatusService();
    }

    @Test
    public void shouldGetJsonResourceException() throws IOException {

        //Given
        Status status = Status.CLIENT_ERROR_BAD_REQUEST;
        Request request = mock(Request.class);
        Response response = mock(Response.class);

        //When
        Representation representation = restStatusService.toRepresentation(status, request, response);

        //Then
        assertTrue(representation.getText().contains("\"code\":400"));
    }

    @Test
    public void shouldReturnThrowableJsonValueIfResourceException() throws IOException {

        //Given
        Request request = mock(Request.class);
        Response response = mock(Response.class);
        ResourceException exception = ResourceException.newResourceException(401);
        exception.setDetail(json(object(field("bing", "bong"))));
        Status status = new Status(exception.getCode(), exception);

        //When
        Representation representation = restStatusService.toRepresentation(status, request, response);

        //Then
        assertTrue(representation.getText().contains("\"bing\":\"bong\""));

    }

    public static class TestGuiceModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(JacksonRepresentationFactory.class);
        }
    }
}

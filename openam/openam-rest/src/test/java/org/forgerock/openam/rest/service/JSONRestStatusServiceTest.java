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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.rest.service;

import java.io.IOException;
import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import org.forgerock.json.resource.ResourceException;
import static org.mockito.Mockito.mock;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class JSONRestStatusServiceTest {

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
        Representation representation = restStatusService.getRepresentation(status, request, response);

        //Then
        assertTrue(representation.getText().contains("\"code\":400"));
    }

    @Test
    public void shouldReturnThrowableJsonValueIfResourceException() throws IOException {

        //Given
        Request request = mock(Request.class);
        Response response = mock(Response.class);
        ResourceException exception = ResourceException.getException(401);
        exception.setDetail(json(object(field("bing", "bong"))));
        Status status = new Status(exception.getCode(), exception);

        //When
        Representation representation = restStatusService.getRepresentation(status, request, response);

        //Then
        assertTrue(representation.getText().contains("\"bing\":\"bong\""));

    }
}

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

package org.forgerock.openam.forgerockrest.authn.restlet;

import junit.framework.Assert;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.forgerockrest.authn.RestAuthenticationHandler;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthResponseException;
import org.forgerock.openam.rest.service.JSONRestStatusService;
import org.forgerock.openam.rest.service.RestStatusService;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.json.JSONException;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collections;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthenticationServiceV2Test {

    private AuthenticationServiceV2 authServiceV2;

    @BeforeMethod
    public void setUp() throws Exception {
        authServiceV2 = new AuthenticationServiceV2(mock(RestAuthenticationHandler.class));
        Request request = new Request();
        authServiceV2.init(mock(Context.class), request, new Response(request));
    }

    @Test
    public void shouldFailAuthenticationWithUnsupportedMediaTypeMessage() throws IOException, JSONException {
        // given
        JsonRepresentation rep = mock(JsonRepresentation.class);
        when(rep.getMediaType()).thenReturn(MediaType.APPLICATION_XML); // only supports JSON

        // when
        ResourceException exception = null;
        try {
            authServiceV2.authenticate(rep);
            fail();
        } catch (ResourceException e) {
            exception = e;
        }

        // then
        JsonRepresentation jsonRep = new JsonRepresentation(
                new JSONRestStatusService().getRepresentation(exception.getStatus(), null, null));
        Assert.assertEquals(jsonRep.getJsonObject().has("code"), true);
        Assert.assertEquals(jsonRep.getJsonObject().get("code"), 415);
        Assert.assertEquals(jsonRep.getJsonObject().has("reason"), true);
        Assert.assertEquals(jsonRep.getJsonObject().get("reason"), "Unsupported Media Type");
        Assert.assertEquals(jsonRep.getJsonObject().has("message"), true);
        Assert.assertEquals(jsonRep.getJsonObject().get("message"), "Unsupported Media Type");
    }

    @Test
    public void shouldReturnUnauthorizedCodeWithJsonFailureMessage() throws IOException, JSONException {
        // given
        JsonValue jsonValue = JsonValueBuilder.jsonValue()
                .put("failure", true)
                .put("reason", "http-auth-failed")
                .put("authId", "12345")
                .build();
        RestAuthResponseException exception = new RestAuthResponseException(RestAuthException.UNAUTHORIZED,
                Collections.<String, String>emptyMap(), jsonValue);

        // when
        Representation rep = authServiceV2.handleErrorResponse(new Status(exception.getStatusCode()), exception);

        // then
        assertEquals(authServiceV2.getResponse().getStatus().getCode(), Status.CLIENT_ERROR_UNAUTHORIZED.getCode());
        JsonRepresentation jsonRep = new JsonRepresentation(rep);
        Assert.assertEquals(jsonRep.getJsonObject().has("failure"), true);
        Assert.assertEquals(jsonRep.getJsonObject().get("failure"), true);
        Assert.assertEquals(jsonRep.getJsonObject().has("reason"), true);
        Assert.assertEquals(jsonRep.getJsonObject().get("reason"), "http-auth-failed");
        Assert.assertEquals(jsonRep.getJsonObject().has("authId"), true);
        Assert.assertEquals(jsonRep.getJsonObject().get("authId"), "12345");
    }

    @Test
    public void shouldThrowResourceExceptionContainingUnauthorizedCodeWithJsonErrorMessage()
            throws IOException, JSONException {

        // given
        RestAuthException testException = new RestAuthException(401, "Invalid Password!!");
        testException.setFailureUrl("http://localhost:8080");

        // when
        ResourceException exception = null;
        try {
            authServiceV2.handleErrorResponse(Status.CLIENT_ERROR_UNAUTHORIZED, testException);
            fail();
        } catch (ResourceException e) {
            exception = e;
        }

        // then
        JsonRepresentation jsonRep = new JsonRepresentation(
                new JSONRestStatusService().getRepresentation(exception.getStatus(), null, null));
        Assert.assertEquals(jsonRep.getJsonObject().has("code"), true);
        Assert.assertEquals(jsonRep.getJsonObject().get("code"), 401);
        Assert.assertEquals(jsonRep.getJsonObject().has("reason"), true);
        Assert.assertEquals(jsonRep.getJsonObject().get("reason"), "Unauthorized");
        Assert.assertEquals(jsonRep.getJsonObject().has("message"), true);
        Assert.assertEquals(jsonRep.getJsonObject().get("message"), "Invalid Password!!");
        Assert.assertEquals(jsonRep.getJsonObject().has("detail"), true);
        Assert.assertEquals(jsonRep.getJsonObject().getJSONObject("detail").has("failureUrl"), true);
        Assert.assertEquals(jsonRep.getJsonObject().getJSONObject("detail").get("failureUrl"), "http://localhost:8080");
    }
}

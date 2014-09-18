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
import org.forgerock.openam.utils.JsonValueBuilder;
import org.json.JSONException;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collections;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthenticationServiceV1Test {

    private AuthenticationServiceV1 authServiceV1;

    @BeforeMethod
    public void setUp() throws Exception {
        authServiceV1 = new AuthenticationServiceV1(mock(RestAuthenticationHandler.class));
        Request request = new Request();
        authServiceV1.init(mock(Context.class), request, new Response(request));
    }

    @Test
    public void shouldFailAuthenticationWithUnsupportedMediaTypeMessage() throws IOException, JSONException {
        // given
        JsonRepresentation rep = mock(JsonRepresentation.class);
        when(rep.getMediaType()).thenReturn(MediaType.APPLICATION_XML); // only supports JSON

        // when
        Representation responseRep = authServiceV1.authenticate(rep);

        // then
        assertEquals(authServiceV1.getResponse().getStatus().getCode(),
                Status.CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE.getCode());
        JsonRepresentation jsonRep = new JsonRepresentation(responseRep);
        Assert.assertEquals(jsonRep.getJsonObject().has("errorMessage"), true);
        Assert.assertEquals(jsonRep.getJsonObject().get("errorMessage"), "Unsupported Media Type");
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
        Representation rep = authServiceV1.handleErrorResponse(new Status(exception.getStatusCode()), exception);

        // then
        assertEquals(authServiceV1.getResponse().getStatus().getCode(), Status.CLIENT_ERROR_UNAUTHORIZED.getCode());
        JsonRepresentation jsonRep = new JsonRepresentation(rep);
        Assert.assertEquals(jsonRep.getJsonObject().has("failure"), true);
        Assert.assertEquals(jsonRep.getJsonObject().get("failure"), true);
        Assert.assertEquals(jsonRep.getJsonObject().has("reason"), true);
        Assert.assertEquals(jsonRep.getJsonObject().get("reason"), "http-auth-failed");
        Assert.assertEquals(jsonRep.getJsonObject().has("authId"), true);
        Assert.assertEquals(jsonRep.getJsonObject().get("authId"), "12345");
    }

    @Test
    public void shouldReturnUnauthorizedCodeWithJsonErrorMessage() throws IOException, JSONException {
        // given
        RestAuthException exception = new RestAuthException(401, "Invalid Password!!");
        exception.setFailureUrl("http://localhost:8080");

        // when
        Representation rep = authServiceV1.handleErrorResponse(Status.CLIENT_ERROR_UNAUTHORIZED, exception);

        // then
        assertEquals(authServiceV1.getResponse().getStatus().getCode(), Status.CLIENT_ERROR_UNAUTHORIZED.getCode());
        JsonRepresentation jsonRep = new JsonRepresentation(rep);
        Assert.assertEquals(jsonRep.getJsonObject().has("errorMessage"), true);
        Assert.assertEquals(jsonRep.getJsonObject().get("errorMessage"), "Invalid Password!!");
        Assert.assertEquals(jsonRep.getJsonObject().has("failureUrl"), true);
        Assert.assertEquals(jsonRep.getJsonObject().get("failureUrl"), "http://localhost:8080");
    }
}

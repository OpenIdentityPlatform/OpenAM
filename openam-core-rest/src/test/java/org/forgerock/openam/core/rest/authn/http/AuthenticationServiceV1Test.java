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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.core.rest.authn.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Collections;

import com.sun.identity.authentication.spi.AuthLoginException;
import org.forgerock.http.session.Session;
import org.forgerock.services.context.AttributesContext;
import org.forgerock.services.context.RootContext;
import org.forgerock.http.session.SessionContext;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.rest.authn.RestAuthenticationHandler;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthException;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthResponseException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AuthenticationServiceV1Test {

    private AuthenticationServiceV1 authServiceV1;

    @BeforeMethod
    public void setUp() throws Exception {
        authServiceV1 = new AuthenticationServiceV1(mock(RestAuthenticationHandler.class));
    }

    @Test
    public void shouldFailAuthenticationWithUnsupportedMediaTypeMessage() throws IOException {
        // given
        AttributesContext context = new AttributesContext(new SessionContext(new RootContext(), mock(Session.class)));
        Request httpRequest = new Request();
        httpRequest.setEntity("<xml></xml>");
        httpRequest.getHeaders().put(ContentTypeHeader.NAME, "application/xml");

        // when
        Response response = authServiceV1.authenticate(context, httpRequest);

        // then
        assertThat(response.getStatus()).isEqualTo(Status.UNSUPPORTED_MEDIA_TYPE);
        assertThat(json(response.getEntity().getJson())).stringAt("errorMessage").isEqualTo("Unsupported Media Type");
    }

    @Test
    public void shouldReturnUnauthorizedCodeWithJsonFailureMessage() throws IOException {
        // given
        Request httpRequest = new Request();
        JsonValue jsonValue = json(object(
                field("failure", true),
                field("reason", "http-auth-failed"),
                field("authId", "12345")));
        RestAuthResponseException exception = new RestAuthResponseException(RestAuthException.UNAUTHORIZED,
                Collections.<String, String>emptyMap(), jsonValue);

        // when
        Response response = authServiceV1.handleErrorResponse(httpRequest, Status.valueOf(exception.getStatusCode()), exception);

        // then
        assertThat(response.getStatus()).isEqualToComparingFieldByField(Status.UNAUTHORIZED);
        JsonValue responseBody = json(response.getEntity().getJson());
        assertThat(responseBody).booleanAt("failure").isTrue();
        assertThat(responseBody).stringAt("reason").isEqualTo("http-auth-failed");
        assertThat(responseBody).stringAt("authId").isEqualTo("12345");
    }

    @Test
    public void shouldReturnUnauthorizedCodeWithJsonErrorMessage() throws IOException {
        // given
        Request httpRequest = new Request();
        RestAuthException exception = new RestAuthException(401, "Invalid Password!!");
        exception.setFailureUrl("http://localhost:8080");

        // when
        Response response = authServiceV1.handleErrorResponse(httpRequest, Status.valueOf(401), exception);

        // then
        assertThat(response.getStatus()).isEqualTo(Status.UNAUTHORIZED);
        JsonValue responseBody = json(response.getEntity().getJson());
        assertThat(responseBody).stringAt("errorMessage").isEqualTo("Invalid Password!!");
        assertThat(responseBody).stringAt("failureUrl").isEqualTo("http://localhost:8080");
    }

    @Test
    public void shouldReturnFrenchErrorMessageFromException() throws IOException {
        // given
        Request httpRequest = new Request();
        AuthLoginException exception = new AuthLoginException("amAuth", "120", null);
        httpRequest.getHeaders().put("Accept-Language", "fr-fr");

        // when
        String message = authServiceV1.getLocalizedMessage(httpRequest, exception);

        // then
        assertThat(message).isEqualTo("L\u2019authentification sur module n\u2019est pas autoris\u00e9e.");
    }

    @Test
    public void shouldReturnFrenchErrorMessageFromCause() throws IOException {
        // given
        Request httpRequest = new Request();
        AuthLoginException ale = new AuthLoginException("amAuth", "120", null);
        RestAuthException exception = new RestAuthException(401, ale);
        httpRequest.getHeaders().put("Accept-Language", "fr-fr");

        // when
        String message = authServiceV1.getLocalizedMessage(httpRequest, exception);

        // then
        assertThat(message).isEqualTo("L\u2019authentification sur module n\u2019est pas autoris\u00e9e.");
    }

    @Test
    public void shouldReturnErrorMessageWithoutTemplate() throws IOException {
        // given
        Request httpRequest = new Request();
        AuthLoginException ale = new AuthLoginException("amAuth", "119", null);
        RestAuthException exception = new RestAuthException(401, ale);

        // when
        String message = authServiceV1.getLocalizedMessage(httpRequest, exception);

        // then
        assertThat(message).isEqualTo("Invalid Auth Level.");
    }
}

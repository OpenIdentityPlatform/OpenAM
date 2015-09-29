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

public class AuthenticationServiceV2Test {

    private AuthenticationServiceV2 authServiceV2;

    @BeforeMethod
    public void setUp() throws Exception {
        authServiceV2 = new AuthenticationServiceV2(mock(RestAuthenticationHandler.class));
    }

    @Test
    public void shouldFailAuthenticationWithUnsupportedMediaTypeMessage() throws IOException {
        // given
        AttributesContext context = new AttributesContext(new SessionContext(new RootContext(), mock(Session.class)));
        Request httpRequest = new Request();
        httpRequest.setEntity("<xml></xml>");
        httpRequest.getHeaders().put(ContentTypeHeader.NAME, "application/xml");

        // when
        Response response = authServiceV2.authenticate(context, httpRequest);

        // then
        assertThat(response.getStatus()).isEqualTo(Status.UNSUPPORTED_MEDIA_TYPE);
        JsonValue responseBody = json(response.getEntity().getJson());
        assertThat(responseBody).integerAt("code").isEqualTo(415);
        assertThat(responseBody).stringAt("reason").isEqualTo("Unsupported Media Type");
        assertThat(responseBody).stringAt("message").isEqualTo("Unsupported Media Type");
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
        Response response = authServiceV2.handleErrorResponse(httpRequest, Status.valueOf(exception.getStatusCode()), exception);

        // then
        assertThat(response.getStatus()).isEqualToComparingFieldByField(Status.UNAUTHORIZED);
        JsonValue responseBody = json(response.getEntity().getJson());
        assertThat(responseBody).booleanAt("failure").isTrue();
        assertThat(responseBody).stringAt("reason").isEqualTo("http-auth-failed");
        assertThat(responseBody).stringAt("authId").isEqualTo("12345");
    }

    @Test
    public void shouldReturnResponseContainingUnauthorizedCodeWithJsonErrorMessage() throws IOException {

        // given
        Request httpRequest = new Request();
        RestAuthException testException = new RestAuthException(401, "Invalid Password!!");
        testException.setFailureUrl("http://localhost:8080");

        // when
        Response response = authServiceV2.handleErrorResponse(httpRequest, Status.valueOf(401), testException);

        // then
        assertThat(response.getStatus()).isEqualToComparingFieldByField(Status.UNAUTHORIZED);
        JsonValue responseBody = json(response.getEntity().getJson());
        assertThat(responseBody).integerAt("code").isEqualTo(401);
        assertThat(responseBody).stringAt("reason").isEqualTo("Unauthorized");
        assertThat(responseBody).stringAt("message").isEqualTo("Invalid Password!!");
        assertThat(responseBody).stringAt("detail/failureUrl").isEqualTo("http://localhost:8080");
    }
}

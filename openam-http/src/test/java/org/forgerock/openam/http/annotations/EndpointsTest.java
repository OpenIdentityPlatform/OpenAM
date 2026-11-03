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
 * Copyright 2026 3A Systems, LLC.
 */

package org.forgerock.openam.http.annotations;

import static org.assertj.core.api.Assertions.assertThat;

import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.services.context.RootContext;
import org.testng.annotations.Test;

public class EndpointsTest {

    public static final class GetPostEndpoint {
        @Get
        public Response read(@Contextual Request request) {
            return new Response(Status.OK).setEntity("read");
        }

        @Post
        public Response create(@Contextual Request request) {
            return new Response(Status.CREATED);
        }
    }

    public static final class PostOnlyEndpoint {
        @Post
        public Response create(@Contextual Request request) {
            return new Response(Status.CREATED);
        }
    }

    /** Matched by findMethod's second pass: the name, not an annotation. */
    public static final class NamedGetEndpoint {
        public Response get(@Contextual Request request) {
            return new Response(Status.OK);
        }
    }

    public static final class OwnAllowEndpoint {
        @Get
        public Response read(@Contextual Request request) {
            Response response = new Response(Status.METHOD_NOT_ALLOWED);
            response.getHeaders().put("Allow", "GET, OPTIONS");
            return response;
        }
    }

    private static Response call(Object endpoint, String method, String override) throws Exception {
        Request request = new Request().setMethod(method).setUri("http://localhost/x");
        if (override != null) {
            request.getHeaders().put("X-HTTP-Method-Override", override);
        }
        return Endpoints.from(endpoint).handle(new RootContext(), request).getOrThrowUninterruptibly();
    }

    @Test
    public void putOnGetPostEndpointAnswers405WithAllow() throws Exception {
        Response response = call(new GetPostEndpoint(), "PUT", null);
        assertThat(response.getStatus()).isEqualTo(Status.METHOD_NOT_ALLOWED);
        assertThat(response.getHeaders().getFirst("Allow")).isEqualTo("GET, POST");
    }

    @Test
    public void unmappedVerbAnswers405WithAllow() throws Exception {
        Response response = call(new GetPostEndpoint(), "PATCH", null);
        assertThat(response.getStatus()).isEqualTo(Status.METHOD_NOT_ALLOWED);
        assertThat(response.getHeaders().getFirst("Allow")).isEqualTo("GET, POST");
    }

    @Test
    public void methodOverrideToPutAnswers405WithAllow() throws Exception {
        Response response = call(new GetPostEndpoint(), "POST", "PUT");
        assertThat(response.getStatus()).isEqualTo(Status.METHOD_NOT_ALLOWED);
        assertThat(response.getHeaders().getFirst("Allow")).isEqualTo("GET, POST");
    }

    @Test
    public void headIsServedByGet() throws Exception {
        Response response = call(new GetPostEndpoint(), "HEAD", null);
        assertThat(response.getStatus()).isEqualTo(Status.OK);
        assertThat(response.getEntity().getString()).isEqualTo("read");
    }

    @Test
    public void headWithoutGetAnswers405AndHeadIsNotAdvertised() throws Exception {
        Response response = call(new PostOnlyEndpoint(), "HEAD", null);
        assertThat(response.getStatus()).isEqualTo(Status.METHOD_NOT_ALLOWED);
        assertThat(response.getHeaders().getFirst("Allow")).isEqualTo("POST");
    }

    @Test
    public void overrideToHeadIsNotServedByGet() throws Exception {
        Response response = call(new GetPostEndpoint(), "POST", "HEAD");
        assertThat(response.getStatus()).isEqualTo(Status.METHOD_NOT_ALLOWED);
        assertThat(response.getHeaders().getFirst("Allow")).isEqualTo("GET, POST");
    }

    @Test
    public void allowListsMethodsMatchedByName() throws Exception {
        Response response = call(new NamedGetEndpoint(), "PUT", null);
        assertThat(response.getHeaders().getFirst("Allow")).isEqualTo("GET");
    }

    @Test
    public void allowSetByTheHandlerIsKept() throws Exception {
        Response response = call(new OwnAllowEndpoint(), "GET", null);
        assertThat(response.getHeaders().getFirst("Allow")).isEqualTo("GET, OPTIONS");
    }
}

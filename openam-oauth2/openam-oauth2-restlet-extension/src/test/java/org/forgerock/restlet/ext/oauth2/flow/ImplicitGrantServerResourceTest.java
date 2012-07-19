/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package org.forgerock.restlet.ext.oauth2.flow;

import static org.fest.assertions.Assertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Map;

import org.fest.assertions.Condition;
import org.fest.assertions.MapAssert;
import org.forgerock.restlet.ext.oauth2.OAuth2;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.testng.annotations.Test;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class ImplicitGrantServerResourceTest extends AbstractFlowTest {
    @Test
    public void testValidRequest() throws Exception {
        Reference reference = new Reference("riap://component/test/oauth2/authorize");
        reference.addQueryParameter(OAuth2.Params.RESPONSE_TYPE, OAuth2.AuthorizationEndpoint.TOKEN);
        reference.addQueryParameter(OAuth2.Params.CLIENT_ID, "cid");
        reference.addQueryParameter(OAuth2.Params.REDIRECT_URI, "");
        reference.addQueryParameter(OAuth2.Params.SCOPE, "read write");
        reference.addQueryParameter(OAuth2.Params.STATE, "random");

        ChallengeResponse cr = new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "admin", "admin");
        Request request = new Request(Method.GET, reference);
        request.setChallengeResponse(cr);
        Response response = new Response(request);

        // handle
        getClient().handle(request, response);
        assertEquals(response.getStatus(), Status.REDIRECTION_FOUND);
        Form fragment = new Form(response.getLocationRef().getFragment());

        // assert
        assertThat(fragment.getValuesMap()).includes(
                MapAssert.entry(OAuth2.Params.TOKEN_TYPE, OAuth2.Bearer.BEARER),
                MapAssert.entry(OAuth2.Params.EXPIRES_IN, "3600")).is(new Condition<Map<?, ?>>() {
            @Override
            public boolean matches(Map<?, ?> value) {
                return value.containsKey(OAuth2.Params.ACCESS_TOKEN)
                        && value.containsKey(OAuth2.Params.STATE);
            }
        });

        // Increase the scope
        reference = new Reference("riap://component/test/oauth2/authorize");
        reference.addQueryParameter(OAuth2.Params.RESPONSE_TYPE, OAuth2.AuthorizationEndpoint.TOKEN);
        reference.addQueryParameter(OAuth2.Params.CLIENT_ID, "cid");
        reference.addQueryParameter(OAuth2.Params.REDIRECT_URI, "");
        reference.addQueryParameter(OAuth2.Params.SCOPE, "read write execute");
        reference.addQueryParameter(OAuth2.Params.STATE, "random");
        request = new Request(Method.GET, reference);
        request.setChallengeResponse(cr);
        response = new Response(request);

        // handle
        getClient().handle(request, response);

        // Redirect Error message - Unknown Client
        assertEquals(response.getStatus(), Status.REDIRECTION_FOUND);
        fragment = new Form(response.getLocationRef().getFragment());

        // assert
        assertThat(fragment.getValuesMap()).includes(
                MapAssert.entry(OAuth2.Params.TOKEN_TYPE, OAuth2.Bearer.BEARER),
                MapAssert.entry(OAuth2.Params.SCOPE, "read write"),
                MapAssert.entry(OAuth2.Params.EXPIRES_IN, "3600")).is(new Condition<Map<?, ?>>() {
            @Override
            public boolean matches(Map<?, ?> value) {
                return value.containsKey(OAuth2.Params.ACCESS_TOKEN)
                        && value.containsKey(OAuth2.Params.STATE);
            }
        });
    }

    @Test
    public void testInvalidRequest() throws Exception {
        Reference reference = new Reference("riap://component/test/oauth2/authorize");
        ChallengeResponse cr = new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "admin", "admin");

        Request request = new Request(Method.GET, reference);
        request.setChallengeResponse(cr);
        Response response = new Response(request);
        reference.addQueryParameter(OAuth2.Params.RESPONSE_TYPE, OAuth2.AuthorizationEndpoint.TOKEN);
        reference.addQueryParameter(OAuth2.Params.CLIENT_ID, "invalid_cid");
        reference.addQueryParameter(OAuth2.Params.STATE, "random");

        // handle
        getClient().handle(request, response);

        // Show error Page - Unknown Client - No Redirect URI
        assertTrue(response.getEntity() instanceof TemplateRepresentation);
        assertTrue(MediaType.TEXT_HTML.equals(response.getEntity().getMediaType()));

        reference.addQueryParameter(OAuth2.Params.REDIRECT_URI, "random_redirect_uri");
        request.getAttributes().clear();
        response = new Response(request);

        // handle
        getClient().handle(request, response);

        // Redirect Error message - Unknown Client
        assertEquals(response.getStatus(), Status.REDIRECTION_FOUND);
        Form fragment = new Form(response.getLocationRef().getFragment());

        // assert
        assertThat(fragment.getValuesMap()).includes(
                MapAssert.entry(OAuth2.Params.ERROR, OAuth2.Error.INVALID_CLIENT),
                MapAssert.entry(OAuth2.Params.STATE, "random")).is(new Condition<Map<?, ?>>() {
            @Override
            public boolean matches(Map<?, ?> value) {
                return value.containsKey(OAuth2.Params.ERROR_DESCRIPTION)
                        && value.containsKey(OAuth2.Params.STATE);
            }
        });

    }
    /*
    The authorization server MUST support the use of the HTTP "GET"
    method [RFC2616] for the authorization endpoint, and MAY support the
    use of the "POST" method as well.
    */
    @Test
    public void testImplicitPostRequest() throws Exception {
        Reference reference = new Reference("riap://component/test/oauth2/authorize");
        reference.addQueryParameter(OAuth2.Params.RESPONSE_TYPE, OAuth2.AuthorizationEndpoint.TOKEN);
        reference.addQueryParameter(OAuth2.Params.CLIENT_ID, "cid");
        reference.addQueryParameter(OAuth2.Params.REDIRECT_URI, "");
        reference.addQueryParameter(OAuth2.Params.SCOPE, "read write");
        reference.addQueryParameter(OAuth2.Params.STATE, "random");

        ChallengeResponse cr = new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "admin", "admin");
        Request request = new Request(Method.POST, reference);
        request.setChallengeResponse(cr);
        Response response = new Response(request);

        // handle
        getClient().handle(request, response);
        assertEquals(response.getStatus(), Status.REDIRECTION_FOUND);
        Form fragment = new Form(response.getLocationRef().getFragment());

        // assert
        assertThat(fragment.getValuesMap()).includes(
                MapAssert.entry(OAuth2.Params.TOKEN_TYPE, OAuth2.Bearer.BEARER),
                MapAssert.entry(OAuth2.Params.EXPIRES_IN, "3600")).is(new Condition<Map<?, ?>>() {
            @Override
            public boolean matches(Map<?, ?> value) {
                return value.containsKey(OAuth2.Params.ACCESS_TOKEN)
                        && value.containsKey(OAuth2.Params.STATE);
            }
        });

        // Increase the scope
        reference = new Reference("riap://component/test/oauth2/authorize");
        reference.addQueryParameter(OAuth2.Params.RESPONSE_TYPE, OAuth2.AuthorizationEndpoint.TOKEN);
        reference.addQueryParameter(OAuth2.Params.CLIENT_ID, "cid");
        reference.addQueryParameter(OAuth2.Params.REDIRECT_URI, "");
        reference.addQueryParameter(OAuth2.Params.SCOPE, "read write execute");
        reference.addQueryParameter(OAuth2.Params.STATE, "random");
        request = new Request(Method.POST, reference);
        request.setChallengeResponse(cr);
        response = new Response(request);

        // handle
        getClient().handle(request, response);

        // Redirect Error message - Unknown Client
        assertEquals(response.getStatus(), Status.REDIRECTION_FOUND);
        fragment = new Form(response.getLocationRef().getFragment());

        // assert
        assertThat(fragment.getValuesMap()).includes(
                MapAssert.entry(OAuth2.Params.TOKEN_TYPE, OAuth2.Bearer.BEARER),
                MapAssert.entry(OAuth2.Params.SCOPE, "read write"),
                MapAssert.entry(OAuth2.Params.EXPIRES_IN, "3600")).is(new Condition<Map<?, ?>>() {
            @Override
            public boolean matches(Map<?, ?> value) {
                return value.containsKey(OAuth2.Params.ACCESS_TOKEN)
                        && value.containsKey(OAuth2.Params.STATE);
            }
        });
    }

    /*
    Parameters sent without a value MUST be treated as if they were
    omitted from the request.
    */
    @Test
    public void testImplicitParametersWithoutValue() throws Exception {
        Reference reference = new Reference("riap://component/test/oauth2/authorize");
        reference.addQueryParameter(OAuth2.Params.RESPONSE_TYPE, OAuth2.AuthorizationEndpoint.TOKEN);
        //leave client_ID blank
        reference.addQueryParameter(OAuth2.Params.CLIENT_ID, "");
        reference.addQueryParameter(OAuth2.Params.REDIRECT_URI, "");
        reference.addQueryParameter(OAuth2.Params.SCOPE, "read write");
        reference.addQueryParameter(OAuth2.Params.STATE, "random");

        ChallengeResponse cr = new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "admin", "admin");
        Request request = new Request(Method.GET, reference);
        request.setChallengeResponse(cr);
        Response response = new Response(request);

        // handle
        getClient().handle(request, response);

        // Redirect Error message - Unknown Client
        assertEquals(response.getStatus(), Status.REDIRECTION_FOUND);
        Form fragment = new Form(response.getLocationRef().getFragment());

        // assert
        assertThat(fragment.getValuesMap()).includes(
                MapAssert.entry(OAuth2.Params.ERROR, OAuth2.Error.INVALID_CLIENT),
                MapAssert.entry(OAuth2.Params.STATE, "random")).is(new Condition<Map<?, ?>>() {
            @Override
            public boolean matches(Map<?, ?> value) {
                return value.containsKey(OAuth2.Params.ERROR_DESCRIPTION)
                        && value.containsKey(OAuth2.Params.STATE);
            }
        });
    }

    /*
    The authorization server MUST ignore
    unrecognized request parameters.
     */
    @Test
    public void testUnrecognizedParametersInRequest() throws Exception {
        Reference reference = new Reference("riap://component/test/oauth2/authorize");
        reference.addQueryParameter(OAuth2.Params.RESPONSE_TYPE, OAuth2.AuthorizationEndpoint.TOKEN);
        reference.addQueryParameter(OAuth2.Params.CLIENT_ID, "cid");
        reference.addQueryParameter(OAuth2.Params.REDIRECT_URI, "");
        reference.addQueryParameter(OAuth2.Params.SCOPE, "read write");
        reference.addQueryParameter(OAuth2.Params.STATE, "random");
        //add an unrecognized parameter
        reference.addQueryParameter("UNRECOGNIZED_PARAM", "VALUE");

        ChallengeResponse cr = new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "admin", "admin");
        Request request = new Request(Method.GET, reference);
        request.setChallengeResponse(cr);
        Response response = new Response(request);

        // handle
        getClient().handle(request, response);
        assertEquals(response.getStatus(), Status.REDIRECTION_FOUND);
        Form fragment = new Form(response.getLocationRef().getFragment());

        // assert
        assertThat(fragment.getValuesMap()).includes(
                MapAssert.entry(OAuth2.Params.TOKEN_TYPE, OAuth2.Bearer.BEARER),
                MapAssert.entry(OAuth2.Params.EXPIRES_IN, "3600")).is(new Condition<Map<?, ?>>() {
            @Override
            public boolean matches(Map<?, ?> value) {
                return value.containsKey(OAuth2.Params.ACCESS_TOKEN)
                        && value.containsKey(OAuth2.Params.STATE);
            }
        });

        // Increase the scope
        reference = new Reference("riap://component/test/oauth2/authorize");
        reference.addQueryParameter(OAuth2.Params.RESPONSE_TYPE, OAuth2.AuthorizationEndpoint.TOKEN);
        reference.addQueryParameter(OAuth2.Params.CLIENT_ID, "cid");
        reference.addQueryParameter(OAuth2.Params.REDIRECT_URI, "");
        reference.addQueryParameter(OAuth2.Params.SCOPE, "read write execute");
        reference.addQueryParameter(OAuth2.Params.STATE, "random");
        //add an unrecognized parameter
        reference.addQueryParameter("UNRECOGNIZED_PARAM", "VALUE");

        request = new Request(Method.GET, reference);
        request.setChallengeResponse(cr);
        response = new Response(request);

        // handle
        getClient().handle(request, response);

        // Redirect Error message - Unknown Client
        assertEquals(response.getStatus(), Status.REDIRECTION_FOUND);
        fragment = new Form(response.getLocationRef().getFragment());

        // assert
        assertThat(fragment.getValuesMap()).includes(
                MapAssert.entry(OAuth2.Params.TOKEN_TYPE, OAuth2.Bearer.BEARER),
                MapAssert.entry(OAuth2.Params.SCOPE, "read write"),
                MapAssert.entry(OAuth2.Params.EXPIRES_IN, "3600")).is(new Condition<Map<?, ?>>() {
            @Override
            public boolean matches(Map<?, ?> value) {
                return value.containsKey(OAuth2.Params.ACCESS_TOKEN)
                        && value.containsKey(OAuth2.Params.STATE);
            }
        });
    }

    /*
    Request and response parameters
    MUST NOT be included more than once.
     */
    @Test
    public void testMultipleParametersWithSameNameInRequest() throws Exception {
        Reference reference = new Reference("riap://component/test/oauth2/authorize");
        reference.addQueryParameter(OAuth2.Params.RESPONSE_TYPE, OAuth2.AuthorizationEndpoint.TOKEN);
        //add multiple client ids (the server will use the first id)
        reference.addQueryParameter(OAuth2.Params.CLIENT_ID, "");
        reference.addQueryParameter(OAuth2.Params.CLIENT_ID, "cid");
        reference.addQueryParameter(OAuth2.Params.CLIENT_ID, "cid2");
        reference.addQueryParameter(OAuth2.Params.REDIRECT_URI, "");
        reference.addQueryParameter(OAuth2.Params.SCOPE, "read write");
        reference.addQueryParameter(OAuth2.Params.STATE, "random");

        ChallengeResponse cr = new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "admin", "admin");
        Request request = new Request(Method.GET, reference);
        request.setChallengeResponse(cr);
        Response response = new Response(request);

        // handle
        getClient().handle(request, response);

        // Redirect Error message - Unknown Client
        assertEquals(response.getStatus(), Status.REDIRECTION_FOUND);
        Form fragment = new Form(response.getLocationRef().getFragment());

        // assert
        assertThat(fragment.getValuesMap()).includes(
                MapAssert.entry(OAuth2.Params.ERROR, OAuth2.Error.INVALID_CLIENT),
                MapAssert.entry(OAuth2.Params.STATE, "random")).is(new Condition<Map<?, ?>>() {
            @Override
            public boolean matches(Map<?, ?> value) {
                return value.containsKey(OAuth2.Params.ERROR_DESCRIPTION)
                        && value.containsKey(OAuth2.Params.STATE);
            }
        });

        // Increase the scope
        reference = new Reference("riap://component/test/oauth2/authorize");
        reference.addQueryParameter(OAuth2.Params.RESPONSE_TYPE, OAuth2.AuthorizationEndpoint.TOKEN);
        //add multiple client ids (the server will use the first id)
        reference.addQueryParameter(OAuth2.Params.CLIENT_ID, "");
        reference.addQueryParameter(OAuth2.Params.CLIENT_ID, "cid");
        reference.addQueryParameter(OAuth2.Params.CLIENT_ID, "cid2");
        reference.addQueryParameter(OAuth2.Params.REDIRECT_URI, "");
        reference.addQueryParameter(OAuth2.Params.SCOPE, "read write execute");
        reference.addQueryParameter(OAuth2.Params.STATE, "random");
        request = new Request(Method.GET, reference);
        request.setChallengeResponse(cr);
        response = new Response(request);

        // handle
        getClient().handle(request, response);

        // Redirect Error message - Unknown Client
        assertEquals(response.getStatus(), Status.REDIRECTION_FOUND);
        fragment = new Form(response.getLocationRef().getFragment());

        // assert
        assertThat(fragment.getValuesMap()).includes(
                MapAssert.entry(OAuth2.Params.ERROR, OAuth2.Error.INVALID_CLIENT),
                MapAssert.entry(OAuth2.Params.STATE, "random")).is(new Condition<Map<?, ?>>() {
            @Override
            public boolean matches(Map<?, ?> value) {
                return value.containsKey(OAuth2.Params.ERROR_DESCRIPTION)
                        && value.containsKey(OAuth2.Params.STATE);
            }
        });
    }

    /*
    If an authorization request is missing the "response_type" parameter,
    or if the response type is not understood, the authorization server
    MUST return an error response as described in Section 4.1.2.1.
     */
    @Test
    public void testMissingResponseTypeParameterInRequest() throws Exception {
        Reference reference = new Reference("riap://component/test/oauth2/authorize");
        //reference.addQueryParameter(OAuth2.Params.RESPONSE_TYPE, OAuth2.AuthorizationEndpoint.TOKEN);
        reference.addQueryParameter(OAuth2.Params.CLIENT_ID, "cid");
        reference.addQueryParameter(OAuth2.Params.REDIRECT_URI, "");
        reference.addQueryParameter(OAuth2.Params.SCOPE, "read write");
        reference.addQueryParameter(OAuth2.Params.STATE, "random");

        ChallengeResponse cr = new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "admin", "admin");
        Request request = new Request(Method.GET, reference);
        request.setChallengeResponse(cr);
        Response response = new Response(request);

        // handle
        getClient().handle(request, response);
        assertEquals(response.getStatus(), Status.CLIENT_ERROR_NOT_FOUND);

        // Increase the scope
        reference = new Reference("riap://component/test/oauth2/authorize");
        //reference.addQueryParameter(OAuth2.Params.RESPONSE_TYPE, OAuth2.AuthorizationEndpoint.TOKEN);
        reference.addQueryParameter(OAuth2.Params.CLIENT_ID, "cid");
        reference.addQueryParameter(OAuth2.Params.REDIRECT_URI, "");
        reference.addQueryParameter(OAuth2.Params.SCOPE, "read write execute");
        reference.addQueryParameter(OAuth2.Params.STATE, "random");
        request = new Request(Method.GET, reference);
        request.setChallengeResponse(cr);
        response = new Response(request);

        // handle
        getClient().handle(request, response);

        // Redirect Error message - Unknown Client
        assertEquals(response.getStatus(), Status.CLIENT_ERROR_NOT_FOUND);
    }

    /*
    if the response type is not understood, the authorization server
    MUST return an error response as described in Section 4.1.2.1
     */
    @Test
    public void testMisunderstoodResponseTypeParameterInRequest() throws Exception {
        Reference reference = new Reference("riap://component/test/oauth2/authorize");
        reference.addQueryParameter(OAuth2.Params.RESPONSE_TYPE, "Misunderstood");
        reference.addQueryParameter(OAuth2.Params.CLIENT_ID, "cid");
        reference.addQueryParameter(OAuth2.Params.REDIRECT_URI, "");
        reference.addQueryParameter(OAuth2.Params.SCOPE, "read write");
        reference.addQueryParameter(OAuth2.Params.STATE, "random");

        ChallengeResponse cr = new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "admin", "admin");
        Request request = new Request(Method.GET, reference);
        request.setChallengeResponse(cr);
        Response response = new Response(request);

        // handle
        getClient().handle(request, response);
        assertEquals(response.getStatus(), Status.CLIENT_ERROR_UNAUTHORIZED);

        // Increase the scope
        reference = new Reference("riap://component/test/oauth2/authorize");
        reference.addQueryParameter(OAuth2.Params.RESPONSE_TYPE, "Misunderstood");
        reference.addQueryParameter(OAuth2.Params.CLIENT_ID, "cid");
        reference.addQueryParameter(OAuth2.Params.REDIRECT_URI, "");
        reference.addQueryParameter(OAuth2.Params.SCOPE, "read write execute");
        reference.addQueryParameter(OAuth2.Params.STATE, "random");
        request = new Request(Method.GET, reference);
        request.setChallengeResponse(cr);
        response = new Response(request);

        // handle
        getClient().handle(request, response);

        // Redirect Error message - Unknown Client
        assertEquals(response.getStatus(), Status.CLIENT_ERROR_UNAUTHORIZED);
    }
}

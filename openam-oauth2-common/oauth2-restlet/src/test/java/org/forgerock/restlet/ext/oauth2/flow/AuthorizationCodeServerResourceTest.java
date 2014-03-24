/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock Inc. All rights reserved.
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
import static org.testng.Assert.*;

import java.util.Map;

import org.fest.assertions.Condition;
import org.fest.assertions.MapAssert;
import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.restlet.ext.oauth2.consumer.BearerOAuth2Proxy;
import org.forgerock.openam.oauth2.model.BearerToken;
import org.forgerock.restlet.ext.oauth2.consumer.RequestFactory.AuthorizationCodeRequest;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.testng.annotations.Test;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class AuthorizationCodeServerResourceTest extends AbstractFlowTest {
    @Test
    public void testValidRequest() throws Exception {

        BearerOAuth2Proxy auth2Proxy = BearerOAuth2Proxy.popOAuth2Proxy(component.getContext());
        assertNotNull(auth2Proxy);

        AuthorizationCodeRequest factory =
                auth2Proxy.getAuthorizationCodeRequest().setClientId("cid").setRedirectUri(
                        auth2Proxy.getRedirectionEndpoint().toString()).setState("random");
        factory.getScope().add("read");
        factory.getScope().add("write");

        Request request = factory.buildRequest();
        ChallengeResponse resource_owner =
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "admin", "admin");
        request.setChallengeResponse(resource_owner);
        Response response = new Response(request);

        // handle
        getClient().handle(request, response);
        assertTrue(response.getStatus().isSuccess());
        assertTrue(response.getEntity() instanceof TemplateRepresentation);
        assertTrue(MediaType.TEXT_HTML.equals(response.getEntity().getMediaType()));

        request = new Request(Method.POST, auth2Proxy.getAuthorizationEndpoint());
        request.setChallengeResponse(resource_owner);
        response = new Response(request);

        Form parameters = new Form();
        parameters.add(OAuth2Constants.Params.RESPONSE_TYPE, OAuth2Constants.AuthorizationEndpoint.CODE);
        parameters.add(OAuth2Constants.Params.CLIENT_ID, auth2Proxy.getClientId());
        parameters.add(OAuth2Constants.Params.REDIRECT_URI, auth2Proxy.getRedirectionEndpoint().toString());
        parameters.add(OAuth2Constants.Params.SCOPE, "read write");
        parameters.add(OAuth2Constants.Params.STATE, "random");
        parameters.add(OAuth2Constants.Custom.DECISION, OAuth2Constants.Custom.ALLOW);
        request.setEntity(parameters.getWebRepresentation());

        // handle
        getClient().handle(request, response);
        assertEquals(response.getStatus(), Status.REDIRECTION_FOUND);
        Form fragment = response.getLocationRef().getQueryAsForm();

        // assert
        assertThat(fragment.getValuesMap())
                .includes(MapAssert.entry(OAuth2Constants.Params.STATE, "random")).is(
                        new Condition<Map<?, ?>>() {
                            @Override
                            public boolean matches(Map<?, ?> value) {
                                return value.containsKey(OAuth2Constants.Params.CODE);
                            }
                        });

        BearerToken token =
                auth2Proxy.flowAuthorizationToken(fragment.getFirstValue(OAuth2Constants.Params.CODE));
        assertNotNull(token);

    }

    /*
    The authorization server MUST support the use of the HTTP "GET"
    method [RFC2616] for the authorization endpoint, and MAY support the
    use of the "POST" method as well.
     */
    @Test
    public void testAuthorizationAllowsGETRequest() throws Exception {

        BearerOAuth2Proxy auth2Proxy = BearerOAuth2Proxy.popOAuth2Proxy(component.getContext());
        assertNotNull(auth2Proxy);

        AuthorizationCodeRequest factory =
                auth2Proxy.getAuthorizationCodeRequest().setClientId("cid").setRedirectUri(
                        auth2Proxy.getRedirectionEndpoint().toString()).setState("random");
        factory.getScope().add("read");
        factory.getScope().add("write");

        Request request = factory.buildRequest();
        ChallengeResponse resource_owner =
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "admin", "admin");
        request.setChallengeResponse(resource_owner);
        Response response = new Response(request);

        // handle
        getClient().handle(request, response);
        assertTrue(response.getStatus().isSuccess());
        assertTrue(response.getEntity() instanceof TemplateRepresentation);
        assertTrue(MediaType.TEXT_HTML.equals(response.getEntity().getMediaType()));

        //do GET instead of post
        request = new Request(Method.GET, auth2Proxy.getAuthorizationEndpoint());
        request.setChallengeResponse(resource_owner);
        response = new Response(request);

        Form parameters = new Form();
        parameters.add(OAuth2Constants.Params.RESPONSE_TYPE, OAuth2Constants.AuthorizationEndpoint.CODE);
        parameters.add(OAuth2Constants.Params.CLIENT_ID, auth2Proxy.getClientId());
        parameters.add(OAuth2Constants.Params.REDIRECT_URI, auth2Proxy.getRedirectionEndpoint().toString());
        parameters.add(OAuth2Constants.Params.SCOPE, "read write");
        parameters.add(OAuth2Constants.Params.STATE, "random");
        parameters.add(OAuth2Constants.Custom.DECISION, OAuth2Constants.Custom.ALLOW);
        request.setEntity(parameters.getWebRepresentation());

        // handle
        getClient().handle(request, response);
        assertEquals(response.getStatus(), Status.REDIRECTION_FOUND);
        Form fragment = response.getLocationRef().getQueryAsForm();

        // assert
        assertThat(fragment.getValuesMap())
                .includes(MapAssert.entry(OAuth2Constants.Params.STATE, "random")).is(
                new Condition<Map<?, ?>>() {
                    @Override
                    public boolean matches(Map<?, ?> value) {
                        return value.containsKey(OAuth2Constants.Params.CODE);
                    }
                });

        BearerToken token =
                auth2Proxy.flowAuthorizationToken(fragment.getFirstValue(OAuth2Constants.Params.CODE));
        assertNotNull(token);

    }

    /*
    Parameters sent without a value MUST be treated as if they were
    omitted from the request.
     */
    @Test
    public void testAuthorizationEmptyParameterValue() throws Exception {

        BearerOAuth2Proxy auth2Proxy = BearerOAuth2Proxy.popOAuth2Proxy(component.getContext());
        assertNotNull(auth2Proxy);

        AuthorizationCodeRequest factory =
                auth2Proxy.getAuthorizationCodeRequest().setClientId("cid").setRedirectUri(
                        auth2Proxy.getRedirectionEndpoint().toString()).setState("random");
        factory.getScope().add("read");
        factory.getScope().add("write");

        Request request = factory.buildRequest();
        ChallengeResponse resource_owner =
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "admin", "admin");
        request.setChallengeResponse(resource_owner);
        Response response = new Response(request);

        // handle
        getClient().handle(request, response);
        assertTrue(response.getStatus().isSuccess());
        assertTrue(response.getEntity() instanceof TemplateRepresentation);
        assertTrue(MediaType.TEXT_HTML.equals(response.getEntity().getMediaType()));

        request = new Request(Method.POST, auth2Proxy.getAuthorizationEndpoint());
        request.setChallengeResponse(resource_owner);
        response = new Response(request);

        Form parameters = new Form();
        parameters.add(OAuth2Constants.Params.RESPONSE_TYPE, OAuth2Constants.AuthorizationEndpoint.CODE);
        parameters.add(OAuth2Constants.Params.CLIENT_ID, auth2Proxy.getClientId());
        //send empty redirect uri
        parameters.add(OAuth2Constants.Params.REDIRECT_URI, "");
        parameters.add(OAuth2Constants.Params.SCOPE, "read write");
        parameters.add(OAuth2Constants.Params.STATE, "random");
        parameters.add(OAuth2Constants.Custom.DECISION, OAuth2Constants.Custom.ALLOW);
        request.setEntity(parameters.getWebRepresentation());

        // handle
        getClient().handle(request, response);
        assertEquals(response.getStatus(), Status.REDIRECTION_FOUND);
        Form fragment = response.getLocationRef().getQueryAsForm();

        // assert
        assertThat(fragment.getValuesMap())
                .includes(MapAssert.entry(OAuth2Constants.Params.STATE, "random")).is(
                new Condition<Map<?, ?>>() {
                    @Override
                    public boolean matches(Map<?, ?> value) {
                        return value.containsKey(OAuth2Constants.Params.CODE);
                    }
                });

        assertThat(response.getLocationRef().toString().contains("http://localhost:8080/oauth2/cb?"));


        BearerToken token =
                auth2Proxy.flowAuthorizationToken(fragment.getFirstValue(OAuth2Constants.Params.CODE));
        assertNotNull(token);

    }

    /*
    The authorization server MUST ignore
    unrecognized request parameters.
     */
    @Test
    public void testAuthorizationIgnoreUnrecognizedRequestParameter() throws Exception {

        BearerOAuth2Proxy auth2Proxy = BearerOAuth2Proxy.popOAuth2Proxy(component.getContext());
        assertNotNull(auth2Proxy);

        AuthorizationCodeRequest factory =
                auth2Proxy.getAuthorizationCodeRequest().setClientId("cid").setRedirectUri(
                        auth2Proxy.getRedirectionEndpoint().toString()).setState("random");
        factory.getScope().add("read");
        factory.getScope().add("write");

        Request request = factory.buildRequest();
        ChallengeResponse resource_owner =
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "admin", "admin");
        request.setChallengeResponse(resource_owner);
        Response response = new Response(request);

        // handle
        getClient().handle(request, response);
        assertTrue(response.getStatus().isSuccess());
        assertTrue(response.getEntity() instanceof TemplateRepresentation);
        assertTrue(MediaType.TEXT_HTML.equals(response.getEntity().getMediaType()));

        request = new Request(Method.GET, auth2Proxy.getAuthorizationEndpoint());
        request.setChallengeResponse(resource_owner);
        response = new Response(request);

        Form parameters = new Form();
        parameters.add(OAuth2Constants.Params.RESPONSE_TYPE, OAuth2Constants.AuthorizationEndpoint.CODE);
        parameters.add(OAuth2Constants.Params.CLIENT_ID, auth2Proxy.getClientId());
        parameters.add(OAuth2Constants.Params.REDIRECT_URI, auth2Proxy.getRedirectionEndpoint().toString());
        parameters.add(OAuth2Constants.Params.SCOPE, "read write");
        parameters.add(OAuth2Constants.Params.STATE, "random");
        parameters.add(OAuth2Constants.Custom.DECISION, OAuth2Constants.Custom.ALLOW);
        parameters.add("UNRECOGNIZED PARAM", "Value");
        request.setEntity(parameters.getWebRepresentation());

        // handle
        getClient().handle(request, response);
        assertEquals(response.getStatus(), Status.REDIRECTION_FOUND);
        Form fragment = response.getLocationRef().getQueryAsForm();

        // assert
        assertThat(fragment.getValuesMap())
                .includes(MapAssert.entry(OAuth2Constants.Params.STATE, "random")).is(
                new Condition<Map<?, ?>>() {
                    @Override
                    public boolean matches(Map<?, ?> value) {
                        return value.containsKey(OAuth2Constants.Params.CODE);
                    }
                });
        BearerToken token =
                auth2Proxy.flowAuthorizationToken(fragment.getFirstValue(OAuth2Constants.Params.CODE));
        assertNotNull(token);

    }

    /*
    Request and response parameters
    MUST NOT be included more than once
     */
    @Test
    public void testAuthorizationMultipleSameRequestParameters() throws Exception {

        BearerOAuth2Proxy auth2Proxy = BearerOAuth2Proxy.popOAuth2Proxy(component.getContext());
        assertNotNull(auth2Proxy);

        AuthorizationCodeRequest factory =
                auth2Proxy.getAuthorizationCodeRequest().setClientId("cid").setRedirectUri(
                        auth2Proxy.getRedirectionEndpoint().toString()).setState("random");
        factory.getScope().add("read");
        factory.getScope().add("write");

        Request request = factory.buildRequest();
        ChallengeResponse resource_owner =
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "admin", "admin");
        request.setChallengeResponse(resource_owner);
        Response response = new Response(request);

        // handle
        getClient().handle(request, response);
        assertTrue(response.getStatus().isSuccess());
        assertTrue(response.getEntity() instanceof TemplateRepresentation);
        assertTrue(MediaType.TEXT_HTML.equals(response.getEntity().getMediaType()));

        request = new Request(Method.POST, auth2Proxy.getAuthorizationEndpoint());
        request.setChallengeResponse(resource_owner);
        response = new Response(request);

        Form parameters = new Form();
        parameters.add(OAuth2Constants.Params.RESPONSE_TYPE, OAuth2Constants.AuthorizationEndpoint.CODE);
        parameters.add(OAuth2Constants.Params.CLIENT_ID, auth2Proxy.getClientId());
        parameters.add(OAuth2Constants.Params.REDIRECT_URI, auth2Proxy.getRedirectionEndpoint().toString());
        parameters.add(OAuth2Constants.Params.REDIRECT_URI, "http://localhost:8080/a");
        parameters.add(OAuth2Constants.Params.REDIRECT_URI, "http://localhost:8080/b");
        parameters.add(OAuth2Constants.Params.REDIRECT_URI, "http://localhost:8080/c");
        parameters.add(OAuth2Constants.Params.STATE, "random");
        parameters.add(OAuth2Constants.Params.SCOPE, "read write");
        parameters.add(OAuth2Constants.Custom.DECISION, OAuth2Constants.Custom.ALLOW);
        request.setEntity(parameters.getWebRepresentation());

        // handle
        getClient().handle(request, response);
        assertEquals(response.getStatus(), Status.REDIRECTION_FOUND);
        Form fragment = response.getLocationRef().getQueryAsForm();

        // assert
        assertThat(fragment.getValuesMap())
                .includes(MapAssert.entry(OAuth2Constants.Params.STATE, "random")).is(
                new Condition<Map<?, ?>>() {
                    @Override
                    public boolean matches(Map<?, ?> value) {
                        return value.containsKey(OAuth2Constants.Params.CODE);
                    }
                });

        assertThat(response.getLocationRef().toString().contains("http://localhost:8080/oauth2/cb?"));

        BearerToken token =
                auth2Proxy.flowAuthorizationToken(fragment.getFirstValue(OAuth2Constants.Params.CODE));
        assertNotNull(token);
    }

    /*
    If an authorization request is missing the "response_type" parameter,
    or if the response type is not understood, the authorization server
    MUST return an error response as described in Section 4.1.2.1.
     */
    @Test
    public void testAuthorizationForMisUnderstoodResponseType() throws Exception {

        BearerOAuth2Proxy auth2Proxy = BearerOAuth2Proxy.popOAuth2Proxy(component.getContext());
        assertNotNull(auth2Proxy);

        AuthorizationCodeRequest factory =
                auth2Proxy.getAuthorizationCodeRequest().setClientId("cid").setRedirectUri(
                        auth2Proxy.getRedirectionEndpoint().toString()).setState("random");
        factory.getScope().add("read");
        factory.getScope().add("write");

        Request request = factory.buildRequest();
        ChallengeResponse resource_owner =
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "admin", "admin");
        request.setChallengeResponse(resource_owner);
        Response response = new Response(request);

        // handle
        getClient().handle(request, response);
        assertTrue(response.getStatus().isSuccess());
        assertTrue(response.getEntity() instanceof TemplateRepresentation);
        assertTrue(MediaType.TEXT_HTML.equals(response.getEntity().getMediaType()));

        request = new Request(Method.POST, auth2Proxy.getAuthorizationEndpoint());
        request.setChallengeResponse(resource_owner);
        response = new Response(request);

        Form parameters = new Form();
        //miss understood response type
        /* TODO: add the test for missing response type */
        parameters.add(OAuth2Constants.Params.RESPONSE_TYPE, "NOT_UNDERSTOOD");
        parameters.add(OAuth2Constants.Params.CLIENT_ID, auth2Proxy.getClientId());
        parameters.add(OAuth2Constants.Params.REDIRECT_URI, auth2Proxy.getRedirectionEndpoint().toString());
        parameters.add(OAuth2Constants.Params.STATE, "random");
        parameters.add(OAuth2Constants.Params.SCOPE, "read write");
        parameters.add(OAuth2Constants.Custom.DECISION, OAuth2Constants.Custom.ALLOW);
        request.setEntity(parameters.getWebRepresentation());

        // handle
        getClient().handle(request, response);
        assertEquals(response.getStatus(), Status.CLIENT_ERROR_UNAUTHORIZED);
    }

    /*
    If an authorization request fails validation due to a missing,
    invalid, or mismatching redirection URI, the authorization server
    SHOULD inform the resource owner of the error, and MUST NOT
    automatically redirect the user-agent to the invalid redirection URI.
     */
    @Test
    public void testAuthorizationInvalidRedirectURIInRequestParameters() throws Exception {

        BearerOAuth2Proxy auth2Proxy = BearerOAuth2Proxy.popOAuth2Proxy(component.getContext());
        assertNotNull(auth2Proxy);

        AuthorizationCodeRequest factory =
                auth2Proxy.getAuthorizationCodeRequest().setClientId("cid").setRedirectUri(
                        auth2Proxy.getRedirectionEndpoint().toString()).setState("random");
        factory.getScope().add("read");
        factory.getScope().add("write");

        Request request = factory.buildRequest();
        ChallengeResponse resource_owner =
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "admin", "admin");
        request.setChallengeResponse(resource_owner);
        Response response = new Response(request);

        // handle
        getClient().handle(request, response);
        assertTrue(response.getStatus().isSuccess());
        assertTrue(response.getEntity() instanceof TemplateRepresentation);
        assertTrue(MediaType.TEXT_HTML.equals(response.getEntity().getMediaType()));

        request = new Request(Method.POST, auth2Proxy.getAuthorizationEndpoint());
        request.setChallengeResponse(resource_owner);
        response = new Response(request);

        Form parameters = new Form();
        parameters.add(OAuth2Constants.Params.RESPONSE_TYPE, OAuth2Constants.AuthorizationEndpoint.CODE);
        parameters.add(OAuth2Constants.Params.CLIENT_ID, auth2Proxy.getClientId());
        parameters.add(OAuth2Constants.Params.REDIRECT_URI, "http://localhost:8080/a");
        parameters.add(OAuth2Constants.Params.STATE, "random");
        parameters.add(OAuth2Constants.Params.SCOPE, "read write");
        parameters.add(OAuth2Constants.Custom.DECISION, OAuth2Constants.Custom.ALLOW);
        request.setEntity(parameters.getWebRepresentation());

        // handle
        getClient().handle(request, response);
        assertEquals(response.getStatus(), Status.CLIENT_ERROR_UNAUTHORIZED);
        Form fragment = response.getLocationRef().getQueryAsForm();

        // assert
        assertThat(fragment.getValuesMap().get(OAuth2Constants.Params.ERROR).equalsIgnoreCase(OAuth2Constants.Error.REDIRECT_URI_MISMATCH));
    }

    /* TODO:
    A public client that was not issued a client password MUST use the
    "client_id" request parameter to identify itself when sending requests
    to the token endpoint.
    */

    @Test
    public void testValidTokenRequest() throws Exception {

        BearerOAuth2Proxy auth2Proxy = BearerOAuth2Proxy.popOAuth2Proxy(component.getContext());
        assertNotNull(auth2Proxy);

        AuthorizationCodeRequest factory =
                auth2Proxy.getAuthorizationCodeRequest().setClientId("cid").setRedirectUri(
                        auth2Proxy.getRedirectionEndpoint().toString()).setState("random");
        factory.getScope().add("read");
        factory.getScope().add("write");

        Request request = factory.buildRequest();
        ChallengeResponse resource_owner =
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "admin", "admin");
        request.setChallengeResponse(resource_owner);
        Response response = new Response(request);

        // handle
        getClient().handle(request, response);
        assertTrue(response.getStatus().isSuccess());
        assertTrue(response.getEntity() instanceof TemplateRepresentation);
        assertTrue(MediaType.TEXT_HTML.equals(response.getEntity().getMediaType()));

        request = new Request(Method.POST, auth2Proxy.getAuthorizationEndpoint());
        request.setChallengeResponse(resource_owner);
        response = new Response(request);

        Form parameters = new Form();
        parameters.add(OAuth2Constants.Params.RESPONSE_TYPE, OAuth2Constants.AuthorizationEndpoint.CODE);
        parameters.add(OAuth2Constants.Params.CLIENT_ID, auth2Proxy.getClientId());
        parameters.add(OAuth2Constants.Params.REDIRECT_URI, auth2Proxy.getRedirectionEndpoint().toString());
        parameters.add(OAuth2Constants.Params.SCOPE, "read write");
        parameters.add(OAuth2Constants.Params.STATE, "random");
        parameters.add(OAuth2Constants.Custom.DECISION, OAuth2Constants.Custom.ALLOW);
        request.setEntity(parameters.getWebRepresentation());

        // handle
        getClient().handle(request, response);
        assertEquals(response.getStatus(), Status.REDIRECTION_FOUND);
        Form fragment = response.getLocationRef().getQueryAsForm();

        // assert
        assertThat(fragment.getValuesMap())
                .includes(MapAssert.entry(OAuth2Constants.Params.STATE, "random")).is(
                new Condition<Map<?, ?>>() {
                    @Override
                    public boolean matches(Map<?, ?> value) {
                        return value.containsKey(OAuth2Constants.Params.CODE);
                    }
                });

        /*BearerToken token =
                auth2Proxy.flowAuthorizationToken(fragment.getFirstValue(OAuth2.Params.CODE));
        assertNotNull(token);*/

        request = new Request(Method.POST, auth2Proxy.getTokenEndpoint());
        request.setChallengeResponse(resource_owner);
        response = new Response(request);

        parameters = new Form();
        parameters.add(OAuth2Constants.Params.GRANT_TYPE, OAuth2Constants.TokeEndpoint.AUTHORIZATION_CODE);
        parameters.add(OAuth2Constants.Params.CLIENT_ID, auth2Proxy.getClientId());
        parameters.add(OAuth2Constants.Params.REDIRECT_URI, auth2Proxy.getRedirectionEndpoint().toString());
        parameters.add(OAuth2Constants.Params.STATE, "random");
        parameters.add(OAuth2Constants.Custom.DECISION, OAuth2Constants.Custom.ALLOW);
        parameters.add(OAuth2Constants.Params.CODE, fragment.getValuesMap().get(OAuth2Constants.Params.CODE));
        request.setEntity(parameters.getWebRepresentation());
        getClient().handle(request, response);
        assertEquals(response.getStatus(), Status.SUCCESS_ACCEPTED);
        assertTrue(MediaType.APPLICATION_JSON.equals(response.getEntity().getMediaType()));
        JacksonRepresentation<Map> representation =
                new JacksonRepresentation<Map>(response.getEntity(), Map.class);

        // assert
        assertThat(representation.getObject()).includes(
                MapAssert.entry(OAuth2Constants.Params.TOKEN_TYPE, OAuth2Constants.Bearer.BEARER),
                MapAssert.entry(OAuth2Constants.Params.EXPIRES_IN, 3600)).is(new Condition<Map<?, ?>>() {
            @Override
            public boolean matches(Map<?, ?> value) {
                return value.containsKey(OAuth2Constants.Params.ACCESS_TOKEN);
            }
        });
    }

    /*
    The client MUST use the HTTP "POST" method when making access token
    requests.
    */
    @Test
    public void testTokenGETRequest() throws Exception {

        BearerOAuth2Proxy auth2Proxy = BearerOAuth2Proxy.popOAuth2Proxy(component.getContext());
        assertNotNull(auth2Proxy);

        AuthorizationCodeRequest factory =
                auth2Proxy.getAuthorizationCodeRequest().setClientId("cid").setRedirectUri(
                        auth2Proxy.getRedirectionEndpoint().toString()).setState("random");
        factory.getScope().add("read");
        factory.getScope().add("write");

        Request request = factory.buildRequest();
        ChallengeResponse resource_owner =
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "admin", "admin");
        request.setChallengeResponse(resource_owner);
        Response response = new Response(request);

        // handle
        getClient().handle(request, response);
        assertTrue(response.getStatus().isSuccess());
        assertTrue(response.getEntity() instanceof TemplateRepresentation);
        assertTrue(MediaType.TEXT_HTML.equals(response.getEntity().getMediaType()));

        request = new Request(Method.POST, auth2Proxy.getAuthorizationEndpoint());
        request.setChallengeResponse(resource_owner);
        response = new Response(request);

        Form parameters = new Form();
        parameters.add(OAuth2Constants.Params.RESPONSE_TYPE, OAuth2Constants.AuthorizationEndpoint.CODE);
        parameters.add(OAuth2Constants.Params.CLIENT_ID, auth2Proxy.getClientId());
        parameters.add(OAuth2Constants.Params.REDIRECT_URI, auth2Proxy.getRedirectionEndpoint().toString());
        parameters.add(OAuth2Constants.Params.SCOPE, "read write");
        parameters.add(OAuth2Constants.Params.STATE, "random");
        parameters.add(OAuth2Constants.Custom.DECISION, OAuth2Constants.Custom.ALLOW);
        request.setEntity(parameters.getWebRepresentation());

        // handle
        getClient().handle(request, response);
        assertEquals(response.getStatus(), Status.REDIRECTION_FOUND);
        Form fragment = response.getLocationRef().getQueryAsForm();

        // assert
        assertThat(fragment.getValuesMap())
                .includes(MapAssert.entry(OAuth2Constants.Params.STATE, "random")).is(
                new Condition<Map<?, ?>>() {
                    @Override
                    public boolean matches(Map<?, ?> value) {
                        return value.containsKey(OAuth2Constants.Params.CODE);
                    }
                });

        /*BearerToken token =
                auth2Proxy.flowAuthorizationToken(fragment.getFirstValue(OAuth2.Params.CODE));
        assertNotNull(token);*/

        request = new Request(Method.GET, auth2Proxy.getTokenEndpoint());
        request.setChallengeResponse(resource_owner);
        response = new Response(request);

        parameters = new Form();
        parameters.add(OAuth2Constants.Params.GRANT_TYPE, OAuth2Constants.TokeEndpoint.AUTHORIZATION_CODE);
        parameters.add(OAuth2Constants.Params.CLIENT_ID, auth2Proxy.getClientId());
        parameters.add(OAuth2Constants.Params.REDIRECT_URI, auth2Proxy.getRedirectionEndpoint().toString());
        parameters.add(OAuth2Constants.Params.STATE, "random");
        parameters.add(OAuth2Constants.Custom.DECISION, OAuth2Constants.Custom.ALLOW);
        parameters.add(OAuth2Constants.Params.CODE, fragment.getValuesMap().get(OAuth2Constants.Params.CODE));
        request.setEntity(parameters.getWebRepresentation());
        getClient().handle(request, response);
        assertEquals(response.getStatus(), Status.CLIENT_ERROR_UNAUTHORIZED);
    }

    /*
   If an authorization request fails validation due to a missing,
   invalid, or mismatching redirection URI, the authorization server
   SHOULD inform the resource owner of the error, and MUST NOT
   automatically redirect the user-agent to the invalid redirection URI.
    */
    @Test
    public void testTokenInvalidRedirectURIRequest() throws Exception {

        BearerOAuth2Proxy auth2Proxy = BearerOAuth2Proxy.popOAuth2Proxy(component.getContext());
        assertNotNull(auth2Proxy);

        AuthorizationCodeRequest factory =
                auth2Proxy.getAuthorizationCodeRequest().setClientId("cid").setRedirectUri(
                        auth2Proxy.getRedirectionEndpoint().toString()).setState("random");
        factory.getScope().add("read");
        factory.getScope().add("write");

        Request request = factory.buildRequest();
        ChallengeResponse resource_owner =
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "admin", "admin");
        request.setChallengeResponse(resource_owner);
        Response response = new Response(request);

        // handle
        getClient().handle(request, response);
        assertTrue(response.getStatus().isSuccess());
        assertTrue(response.getEntity() instanceof TemplateRepresentation);
        assertTrue(MediaType.TEXT_HTML.equals(response.getEntity().getMediaType()));

        request = new Request(Method.POST, auth2Proxy.getAuthorizationEndpoint());
        request.setChallengeResponse(resource_owner);
        response = new Response(request);

        Form parameters = new Form();
        parameters.add(OAuth2Constants.Params.RESPONSE_TYPE, OAuth2Constants.AuthorizationEndpoint.CODE);
        parameters.add(OAuth2Constants.Params.CLIENT_ID, auth2Proxy.getClientId());
        parameters.add(OAuth2Constants.Params.REDIRECT_URI, auth2Proxy.getRedirectionEndpoint().toString());
        parameters.add(OAuth2Constants.Params.SCOPE, "read write");
        parameters.add(OAuth2Constants.Params.STATE, "random");
        parameters.add(OAuth2Constants.Custom.DECISION, OAuth2Constants.Custom.ALLOW);
        request.setEntity(parameters.getWebRepresentation());

        // handle
        getClient().handle(request, response);
        assertEquals(response.getStatus(), Status.REDIRECTION_FOUND);
        Form fragment = response.getLocationRef().getQueryAsForm();

        // assert
        assertThat(fragment.getValuesMap())
                .includes(MapAssert.entry(OAuth2Constants.Params.STATE, "random")).is(
                new Condition<Map<?, ?>>() {
                    @Override
                    public boolean matches(Map<?, ?> value) {
                        return value.containsKey(OAuth2Constants.Params.CODE);
                    }
                });

        /*BearerToken token =
                auth2Proxy.flowAuthorizationToken(fragment.getFirstValue(OAuth2.Params.CODE));
        assertNotNull(token);*/

        request = new Request(Method.GET, auth2Proxy.getTokenEndpoint());
        request.setChallengeResponse(resource_owner);
        response = new Response(request);

        parameters = new Form();
        parameters.add(OAuth2Constants.Params.GRANT_TYPE, OAuth2Constants.TokeEndpoint.AUTHORIZATION_CODE);
        parameters.add(OAuth2Constants.Params.CLIENT_ID, auth2Proxy.getClientId());
        parameters.add(OAuth2Constants.Params.REDIRECT_URI, "http://localhost:8080/a");
        parameters.add(OAuth2Constants.Params.STATE, "random");
        parameters.add(OAuth2Constants.Custom.DECISION, OAuth2Constants.Custom.ALLOW);
        parameters.add(OAuth2Constants.Params.CODE, fragment.getValuesMap().get(OAuth2Constants.Params.CODE));
        request.setEntity(parameters.getWebRepresentation());
        getClient().handle(request, response);
        assertEquals(response.getStatus(), Status.CLIENT_ERROR_UNAUTHORIZED);
        fragment = response.getLocationRef().getQueryAsForm();

        // assert
        assertThat(fragment.getValuesMap().get(OAuth2Constants.Params.ERROR).equalsIgnoreCase(OAuth2Constants.Error.REDIRECT_URI_MISMATCH));
    }

}

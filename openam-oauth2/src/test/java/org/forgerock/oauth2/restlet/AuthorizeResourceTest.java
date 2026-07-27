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
 * Portions Copyrighted 2026 3A Systems LLC.
 */

package org.forgerock.oauth2.restlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.forgerock.oauth2.core.AuthorizationService;
import org.forgerock.oauth2.core.AuthorizationToken;
import org.forgerock.oauth2.core.CsrfProtection;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.RedirectUriResolver;
import org.forgerock.oauth2.core.ResourceOwnerSessionValidator;
import org.forgerock.oauth2.core.UserInfoClaims;
import org.forgerock.oauth2.core.exceptions.ResourceOwnerAuthenticationRequired;
import org.forgerock.oauth2.core.exceptions.ResourceOwnerConsentRequired;
import org.forgerock.openam.rest.representations.JacksonRepresentationFactory;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.xui.XUIState;
import org.mockito.ArgumentCaptor;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ClientInfo;
import org.restlet.data.MediaType;
import org.restlet.data.Preference;
import org.restlet.data.Reference;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.routing.Router;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AuthorizeResourceTest {

    private AuthorizeResource resource;
    private OAuth2Request o2request;
    private Request request;
    private Response response;
    private AuthorizationService service;
    private AuthorizeRequestHook hook;
    private AuthorizationToken authToken = new AuthorizationToken(Collections.singletonMap("fred", "fred"), false);
    private XUIState xuiState;
    private RedirectUriResolver redirectUriResolver;
    private OAuth2Representation representation;
    private CsrfProtection csrfProtection;
    private ExceptionHandler exceptionHandler;
    private JacksonRepresentationFactory jacksonRepresentationFactory;

    @BeforeMethod
    public void setup() throws Exception {
        representation = mock(OAuth2Representation.class);
        jacksonRepresentationFactory = mock(JacksonRepresentationFactory.class);
        OAuth2RequestFactory oauth2RequestFactory = mock(OAuth2RequestFactory.class);
        o2request = mock(OAuth2Request.class);
        request = mock(Request.class);
        response = mock(Response.class);
        hook = mock(AuthorizeRequestHook.class);
        service = mock(AuthorizationService.class);
        xuiState = mock(XUIState.class);
        redirectUriResolver = mock(RedirectUriResolver.class);
        ResourceOwnerSessionValidator resourceOwnerSessionValidator = mock(ResourceOwnerSessionValidator.class);
        csrfProtection = mock(CsrfProtection.class);

        when(oauth2RequestFactory.create(request)).thenReturn(o2request);

        exceptionHandler = mock(ExceptionHandler.class);

        resource = new AuthorizeResource(oauth2RequestFactory, service, exceptionHandler, representation,
                CollectionUtils.asSet(hook), xuiState, mock(Router.class), null, redirectUriResolver,
                resourceOwnerSessionValidator, csrfProtection, jacksonRepresentationFactory);
        resource = spy(resource);
        doReturn(request).when(resource).getRequest();
        doReturn(response).when(resource).getResponse();
    }

    @Test
    public void shouldCallHooksInGet() throws Exception {
        //given
        when(service.authorize(o2request)).thenReturn(authToken);

        //when
        resource.authorize();

        //then
        verify(hook).beforeAuthorizeHandling(o2request, request, response);
        verify(hook).afterAuthorizeSuccess(o2request, request, response);
    }

    @Test
    public void shouldCallHooksInPost() throws Exception {
        //given
        when(service.authorize(o2request)).thenReturn(authToken);

        //when
        resource.authorize(new EmptyRepresentation());

        //then
        verify(hook).beforeAuthorizeHandling(o2request, request, response);
        verify(hook).afterAuthorizeSuccess(o2request, request, response);
    }

    private static Preference<MediaType> pref(MediaType type, float quality) {
        return new Preference<>(type, quality);
    }

    @SafeVarargs
    private static List<Preference<MediaType>> accepts(Preference<MediaType>... preferences) {
        return Arrays.asList(preferences);
    }

    @DataProvider(name = "acceptedMediaTypes")
    public Object[][] acceptedMediaTypes() {
        return new Object[][]{
                // A bare */* is curl's default. Treating it as a JSON request would silently switch the
                // response format for every existing script that scrapes the consent page.
                {"*/*", accepts(pref(MediaType.ALL, 1f)), false},
                {"no Accept header", accepts(), false},
                // application/* includes JSON without naming it - same trap as */*.
                {"application/*", accepts(pref(MediaType.valueOf("application/*"), 1f)), false},
                {"text/html", accepts(pref(MediaType.TEXT_HTML, 1f)), false},
                {"text/*", accepts(pref(MediaType.valueOf("text/*"), 1f)), false},
                {"browser default", accepts(pref(MediaType.TEXT_HTML, 1f), pref(MediaType.ALL, 0.8f)), false},
                {"application/json", accepts(pref(MediaType.APPLICATION_JSON, 1f)), true},
                {"json wins on q", accepts(pref(MediaType.APPLICATION_JSON, 0.9f),
                        pref(MediaType.TEXT_HTML, 0.8f)), true},
                {"html wins on q", accepts(pref(MediaType.TEXT_HTML, 0.9f),
                        pref(MediaType.APPLICATION_JSON, 0.8f)), false},
                // A tie expresses no preference, so the existing behaviour wins.
                {"explicit json ties with */*", accepts(pref(MediaType.APPLICATION_JSON, 1f),
                        pref(MediaType.ALL, 1f)), false},
                // The stock Accept of axios and similar libraries: JSON is named, but only alongside an
                // equally weighted wildcard. Scripts that scrape the consent page must keep getting it.
                {"axios default", accepts(pref(MediaType.APPLICATION_JSON, 1f),
                        pref(MediaType.TEXT_PLAIN, 1f), pref(MediaType.ALL, 1f)), false},
                // jQuery's $.getJSON deprioritises the wildcard, which is a real preference for JSON.
                {"jQuery getJSON", accepts(pref(MediaType.APPLICATION_JSON, 1f),
                        pref(MediaType.valueOf("text/javascript"), 1f), pref(MediaType.ALL, 0.01f)), true},
                // Spring's RestTemplate names no HTML-capable type at all.
                {"RestTemplate default", accepts(pref(MediaType.APPLICATION_JSON, 1f),
                        pref(MediaType.valueOf("application/*+json"), 1f)), true},
                {"json explicitly unacceptable", accepts(pref(MediaType.APPLICATION_JSON, 0f)), false},
                {"json with charset parameter", accepts(pref(
                        MediaType.valueOf("application/json;charset=UTF-8"), 1f)), true},
        };
    }

    @Test(dataProvider = "acceptedMediaTypes")
    public void shouldServeJsonOnlyWhenExplicitlyPreferred(String label, List<Preference<MediaType>> accepted,
            boolean expected) {
        assertThat(AuthorizeResource.prefersJson(accepted)).as(label).isEqualTo(expected);
    }

    @DataProvider(name = "origins")
    public Object[][] origins() {
        return new Object[][]{
                {"identical", "https://openam.example.com:8443", "https://openam.example.com:8443/openam", true},
                {"implied https port", "https://openam.example.com", "https://openam.example.com:443/openam", true},
                {"implied http port", "http://openam.example.com:80", "http://openam.example.com/openam", true},
                {"host case differs", "https://OpenAM.Example.COM", "https://openam.example.com/openam", true},
                {"different host", "https://evil.invalid", "https://openam.example.com/openam", false},
                {"different scheme", "http://openam.example.com", "https://openam.example.com/openam", false},
                {"different port", "https://openam.example.com:9443", "https://openam.example.com:8443/openam",
                        false},
                // Sandboxed iframes and some redirects send the literal string "null".
                {"opaque origin", "null", "https://openam.example.com/openam", false},
                {"unparseable", "not a url", "https://openam.example.com/openam", false},
                {"sibling subdomain", "https://evil.example.com", "https://openam.example.com/openam", false},
        };
    }

    @Test(dataProvider = "origins")
    public void shouldRecogniseOnlyItsOwnOrigin(String label, String origin, String rootUrl, boolean expected) {
        assertThat(AuthorizeResource.sameOrigin(origin, rootUrl)).as(label).isEqualTo(expected);
    }

    private void givenConsentIsRequired() throws Exception {
        when(service.authorize(o2request)).thenThrow(new ResourceOwnerConsentRequired("client", "description",
                Collections.<String, String>emptyMap(), Collections.<String, String>emptyMap(),
                new UserInfoClaims(Collections.<String, Object>emptyMap(),
                        Collections.<String, List<String>>emptyMap()), "Demo User", true));
        when(request.getResourceRef())
                .thenReturn(new Reference("https://openam.example.com/openam/oauth2/authorize?client_id=x"));
    }

    private void givenClientAccepts(MediaType mediaType) {
        ClientInfo clientInfo = new ClientInfo();
        clientInfo.getAcceptedMediaTypes().add(new Preference<>(mediaType, 1f));
        when(request.getClientInfo()).thenReturn(clientInfo);
    }

    @Test
    public void shouldServeJsonConsentModelWhenRequested() throws Exception {
        //given
        givenConsentIsRequired();
        givenClientAccepts(MediaType.APPLICATION_JSON);
        doReturn(false).when(resource).isForeignOrigin(o2request);
        when(csrfProtection.createCsrfToken(o2request)).thenReturn("the-token");

        //when
        resource.authorize();

        //then
        Map<String, Object> model = captureConsentModel();
        assertThat(model).containsEntry("csrf", "the-token")
                .containsEntry("target", "/openam/oauth2/authorize?client_id=x")
                .containsEntry("client_name", "client")
                .containsEntry("save_consent_enabled", true);
        verify(representation, never()).getRepresentation(any(), any(OAuth2Request.class), anyString(), anyMap());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> captureConsentModel() {
        ArgumentCaptor<Map> model = ArgumentCaptor.forClass(Map.class);
        verify(jacksonRepresentationFactory).create(model.capture());
        return model.getValue();
    }

    @Test
    public void shouldSerialiseNestedScopesAndClaims() throws Exception {
        //given a scope carrying a claim, and a claim belonging to no scope
        when(service.authorize(o2request)).thenThrow(new ResourceOwnerConsentRequired("client", "description",
                Collections.singletonMap("profile", "Your personal information"),
                Collections.singletonMap("given_name", "First name"),
                new UserInfoClaims(Collections.<String, Object>singletonMap("given_name", "Demo"),
                        Collections.singletonMap("profile", Collections.singletonList("given_name"))),
                "Demo User", true));
        when(request.getResourceRef()).thenReturn(new Reference("https://openam.example.com/openam/oauth2/authorize"));
        givenClientAccepts(MediaType.APPLICATION_JSON);
        doReturn(false).when(resource).isForeignOrigin(o2request);

        //when
        resource.authorize();

        //then the model handed to Jackson nests three levels deep
        Map<String, Object> model = captureConsentModel();
        List<Map<String, Object>> scopes = (List<Map<String, Object>>) model.get("scopes");
        assertThat(scopes).hasSize(1);
        assertThat(scopes.get(0)).containsEntry("name", "profile")
                .containsEntry("description", "Your personal information");
        List<Map<String, Object>> nested = (List<Map<String, Object>>) scopes.get(0).get("claims");
        assertThat(nested).hasSize(1);
        assertThat(nested.get(0)).containsEntry("name", "given_name").containsEntry("value", "Demo");
        assertThat((List<?>) model.get("claims")).isEmpty();
    }

    @Test
    public void shouldCarryAbsentFieldsAsNull() throws Exception {
        //given no device flow, so there is no user code
        givenConsentIsRequired();
        givenClientAccepts(MediaType.APPLICATION_JSON);
        doReturn(false).when(resource).isForeignOrigin(o2request);

        //when
        resource.authorize();

        //then the key is present with a null value rather than dropped
        Map<String, Object> model = captureConsentModel();
        assertThat(model).containsKey("user_code");
        assertThat(model.get("user_code")).isNull();
    }

    @Test
    public void shouldRefuseJsonConsentModelToForeignOriginWithoutMintingAToken() throws Exception {
        //given
        givenConsentIsRequired();
        givenClientAccepts(MediaType.APPLICATION_JSON);
        doReturn(true).when(resource).isForeignOrigin(o2request);

        //when
        try {
            resource.authorize();
            failBecauseExceptionWasNotThrown(OAuth2RestletException.class);
        } catch (OAuth2RestletException e) {
            //then
            assertThat(e.getStatus().getCode()).isEqualTo(403);
        }
        // The guard must run before the token is minted, otherwise it would clobber a legitimate one.
        verify(csrfProtection, never()).createCsrfToken(any(OAuth2Request.class));
        verify(jacksonRepresentationFactory, never()).create(any());
    }

    @Test
    public void shouldReportAuthenticationRequiredAsLoginRequiredForJsonClients() throws Exception {
        //given
        givenClientAccepts(MediaType.APPLICATION_JSON);
        when(service.authorize(o2request)).thenThrow(new ResourceOwnerAuthenticationRequired(
                new URI("https://openam.example.com/openam/XUI/#login")));

        //when
        try {
            resource.authorize();
            failBecauseExceptionWasNotThrown(OAuth2RestletException.class);
        } catch (OAuth2RestletException e) {
            //then a redirect to the login page is useless to a non-browser client
            assertThat(e.getStatus().getCode()).isEqualTo(401);
            assertThat(e.getError()).isEqualTo("login_required");
            assertThat(e.getRedirectUri()).isNull();
        }
    }

    @Test
    public void shouldStillRedirectToLoginForBrowsers() throws Exception {
        //given
        givenClientAccepts(MediaType.TEXT_HTML);
        when(service.authorize(o2request)).thenThrow(new ResourceOwnerAuthenticationRequired(
                new URI("https://openam.example.com/openam/XUI/#login")));

        //when
        try {
            resource.authorize();
            failBecauseExceptionWasNotThrown(OAuth2RestletException.class);
        } catch (OAuth2RestletException e) {
            //then
            assertThat(e.getRedirectUri()).isEqualTo("https://openam.example.com/openam/XUI/#login");
        }
    }

    @Test
    public void shouldRenderErrorsAsJsonForJsonClients() {
        //given
        givenClientAccepts(MediaType.APPLICATION_JSON);
        Throwable failure = new OAuth2RestletException(400, "invalid_scope", "Unknown scope", null);

        //when
        resource.doCatch(failure);

        //then
        verify(exceptionHandler).handle(failure, response);
        verify(exceptionHandler, never()).handle(any(Throwable.class), any(), any(Request.class),
                any(Response.class));
    }

    @Test
    public void shouldRenderErrorsAsHtmlForBrowsers() {
        //given
        givenClientAccepts(MediaType.TEXT_HTML);
        Throwable failure = new OAuth2RestletException(400, "invalid_scope", "Unknown scope", null);

        //when
        resource.doCatch(failure);

        //then
        verify(exceptionHandler).handle(eq(failure), any(), eq(request), eq(response));
        verify(exceptionHandler, never()).handle(any(Throwable.class), any(Response.class));
    }

    @Test
    public void shouldKeepRedirectingErrorsThatCarryARedirectUri() {
        //given RFC 6749 4.1.2.1: errors with a usable redirect_uri are reported by redirecting
        givenClientAccepts(MediaType.APPLICATION_JSON);
        Throwable failure = new OAuth2RestletException(400, "invalid_scope", "Unknown scope",
                "https://client.example.com/cb", null);

        //when
        resource.doCatch(failure);

        //then
        verify(exceptionHandler).handle(eq(failure), any(), eq(request), eq(response));
        verify(exceptionHandler, never()).handle(any(Throwable.class), any(Response.class));
    }

    @Test
    public void shouldLookThroughRestletWrappingWhenErrorCarriesARedirectUri() {
        //given Restlet hands doCatch the exception wrapped, which is why ExceptionHandler inspects getCause()
        givenClientAccepts(MediaType.APPLICATION_JSON);
        Throwable wrapped = new RuntimeException(new OAuth2RestletException(400, "invalid_scope", "Unknown scope",
                "https://client.example.com/cb", null));

        //when
        resource.doCatch(wrapped);

        //then
        verify(exceptionHandler).handle(eq(wrapped), any(), eq(request), eq(response));
    }

    @Test
    public void shouldLookThroughRestletWrappingWhenErrorHasNoRedirectUri() {
        //given
        givenClientAccepts(MediaType.APPLICATION_JSON);
        Throwable wrapped = new RuntimeException(
                new OAuth2RestletException(400, "invalid_scope", "Unknown scope", null));

        //when
        resource.doCatch(wrapped);

        //then
        verify(exceptionHandler).handle(wrapped, response);
    }

    @Test
    public void shouldStillRenderConsentPageWhenJsonNotRequested() throws Exception {
        //given
        givenConsentIsRequired();
        givenClientAccepts(MediaType.ALL);
        doReturn(Collections.<String, Object>emptyMap()).when(resource)
                .getDataModel(any(ResourceOwnerConsentRequired.class), any(OAuth2Request.class));

        //when
        resource.authorize();

        //then
        verify(representation).getRepresentation(any(), eq(o2request), eq("authorize.ftl"), anyMap());
        verify(jacksonRepresentationFactory, never()).create(any());
        verify(resource, never()).isForeignOrigin(any(OAuth2Request.class));
    }

}
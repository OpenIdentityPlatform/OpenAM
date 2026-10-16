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

package org.forgerock.oauth2.restlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.openam.rest.representations.JacksonRepresentationFactory;
import org.forgerock.openam.services.baseurl.BaseURLProvider;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.mockito.ArgumentCaptor;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * The OAuth2 error page ({@code page/error.ftl}) hands the error fields to the XUI, which renders them into the DOM
 * with unescaped Handlebars output ({@code {{{error.description}}}}). The template's {@code ?js_string} only makes
 * the values safe inside the JavaScript string literal, so the values must reach the template already HTML-encoded.
 */
public class ExceptionHandlerTest {

    private static final String XSS_PAYLOAD = "<img src=x onerror=alert(document.domain)>";
    private static final String DUPLICATE_PARAMETER_MESSAGE =
            "Invalid Request, duplicate request parameter found : " + XSS_PAYLOAD;

    private ExceptionHandler exceptionHandler;
    private OAuth2Representation representation;
    private Context context;
    private Request request;
    private Response response;

    @BeforeMethod
    public void setUp() {
        representation = mock(OAuth2Representation.class);
        when(representation.toForm(anyMap())).thenCallRealMethod();

        OAuth2RequestFactory requestFactory = mock(OAuth2RequestFactory.class);
        OAuth2Request oauth2Request = mock(OAuth2Request.class);
        when(requestFactory.create(any(Request.class))).thenReturn(oauth2Request);
        when(oauth2Request.getParameter("realm")).thenReturn("/");

        BaseURLProviderFactory baseURLProviderFactory = mock(BaseURLProviderFactory.class);
        BaseURLProvider baseURLProvider = mock(BaseURLProvider.class);
        when(baseURLProviderFactory.get("/")).thenReturn(baseURLProvider);

        exceptionHandler = new ExceptionHandler(representation, baseURLProviderFactory, requestFactory,
                mock(JacksonRepresentationFactory.class));

        context = new Context();
        request = new Request(Method.GET, "http://openam.example.com/openam/oauth2/authorize");
        response = new Response(request);
    }

    @Test
    public void shouldHtmlEncodeErrorDescriptionOnErrorPage() {
        // given: no redirect_uri, so the error is rendered on the OAuth2 error page
        OAuth2RestletException exception =
                new OAuth2RestletException(400, "invalid_request", DUPLICATE_PARAMETER_MESSAGE, "state");

        // when
        exceptionHandler.handle(new ResourceException(exception), context, request, response);

        // then
        Map<String, String> data = errorPageData();
        assertThat(response.getStatus()).isEqualTo(Status.CLIENT_ERROR_BAD_REQUEST);
        assertThat(data.get("error_description"))
                .startsWith("Invalid Request, duplicate request parameter found")
                .contains("&lt;img")
                .doesNotContain("<")
                .doesNotContain(">");
    }

    @Test
    public void shouldHtmlEncodeErrorCodeAndErrorUriOnErrorPage() {
        // given
        OAuth2RestletException exception =
                new OAuth2RestletException(400, "invalid_request" + XSS_PAYLOAD, "description", (String) null);
        exception.setErrorUri("https://example.com/\" onmouseover=\"alert(1)");

        // when
        exceptionHandler.handle(new ResourceException(exception), context, request, response);

        // then
        Map<String, String> data = errorPageData();
        assertThat(data.get("error")).startsWith("invalid_request&lt;img").doesNotContain("<").doesNotContain(">");
        assertThat(data.get("error_uri")).doesNotContain("\"");
    }

    @Test
    public void shouldNotHtmlEncodeErrorWhenRedirectingToClient() {
        // given: a redirect_uri, so the error goes back to the client as query parameters
        OAuth2RestletException exception = new OAuth2RestletException(400, "invalid_request",
                DUPLICATE_PARAMETER_MESSAGE, "https://client.example.com/callback", "state");

        // when
        exceptionHandler.handle(new ResourceException(exception), context, request, response);

        // then
        assertThat(response.getStatus()).isEqualTo(Status.REDIRECTION_FOUND);
        assertThat(response.getLocationRef().getQueryAsForm().getFirstValue("error_description"))
                .isEqualTo(DUPLICATE_PARAMETER_MESSAGE);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> errorPageData() {
        ArgumentCaptor<Map<String, ?>> captor = ArgumentCaptor.forClass(Map.class);
        verify(representation).getRepresentation(eq(context), eq("page"), eq("error.ftl"), captor.capture());
        return (Map<String, String>) captor.getValue();
    }
}

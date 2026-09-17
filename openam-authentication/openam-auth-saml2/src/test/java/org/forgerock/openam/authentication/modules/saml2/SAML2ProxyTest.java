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
* Copyright 2015-2016 ForgeRock AS.
* Portions copyright 2025-2026 3A Systems LLC.
*/
package org.forgerock.openam.authentication.modules.saml2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.authentication.modules.saml2.Constants.AM_LOCATION_COOKIE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.xml.bind.StringInputStream;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.xml.xpath.XPathFactory;

import org.forgerock.util.xml.XMLUtils;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

public class SAML2ProxyTest {

    private final static String KEY = "key";
    private Cookie[] validCookies;
    private final static String COOKIE_LOCATION = "/openam/XUI/#login";
    private final static String ACS_URL = "https://openam.example.com/openam/AuthConsumer/metaAlias/sp";
    private final static String ARTIFACT_QUERY = "SAMLart=AAQAAM0Nc0d";
    private final static String COOKIE_HASH_REDIRECT =
            "com.sun.identity.federation.cookieHashRedirectEnabled";

    @BeforeTest
    void theSetUp() { //you need this
        validCookies = new Cookie[1];
        validCookies[0] = new Cookie(AM_LOCATION_COOKIE, COOKIE_LOCATION);
    }

    @Test
    public void shouldCreateValidUrlFromDataViaPOST() {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getCookies()).willReturn(validCookies);
        given(mockRequest.getMethod()).willReturn("POST");

        //when
        String result = SAML2Proxy.getUrlWithKey(mockRequest, KEY);

        //then
        assertThat(result).contains(COOKIE_LOCATION);
        assertThat(result).contains(SAML2Proxy.RESPONSE_KEY + "=" + KEY);
        assertThat(result).contains(SAML2Proxy.ERROR_PARAM_KEY + "=" + "false");
    }

    @Test
    public void shouldCreateValidUrlFromDataViaGET() {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getCookies()).willReturn(validCookies);
        given(mockRequest.getMethod()).willReturn("GET");

        //when
        String result = SAML2Proxy.getUrlWithKey(mockRequest, KEY);

        //then
        assertThat(result).contains(COOKIE_LOCATION);
        assertThat(result).contains(SAML2Proxy.RESPONSE_KEY + "=" + KEY);
        assertThat(result).contains(SAML2Proxy.ERROR_PARAM_KEY + "=" + "false");
    }

    @Test
    public void shouldEscapeMaliciousHtmlInputOnForm() {
        //given
        String malicious = "\"><script>alert('Bad thing);</script><form ";

        //when
        final String formHtml = SAML2Proxy.getAutoSubmittingFormHtml(URLEncDec.encode(malicious));

        //then
        // The malicious code should not be present
        assertThat(formHtml).doesNotContain(malicious);
        // However, it should be preserved when parsed again
        assertThat(URLEncDec.decode(getFormAction(formHtml))).contains(malicious);
    }

    private String getFormAction(String html) {
        try {
            final Document doc = XMLUtils.getSafeDocumentBuilder(false).parse(new StringInputStream(html));
            return XPathFactory.newInstance().newXPath().evaluate("string(//form/@action)", doc);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void shouldErrorDueToEmptyAuthenticationStepCookiePOST() {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getMethod()).willReturn("POST");

        //when
        SAML2Proxy.getUrlWithKey(mockRequest, KEY);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void shouldErrorDueToEmptyAuthenticationStepCookieGET() {

        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getMethod()).willReturn("GET");

        //when
        SAML2Proxy.getUrlWithKey(mockRequest, KEY);
    }

    @Test
    public void shouldCreateDefaultErrorHTMLPostFromDataViaPOST() {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getCookies()).willReturn(validCookies);
        given(mockRequest.getMethod()).willReturn("POST");
        String errorType = "200";

        //when
        String response = SAML2Proxy.getUrlWithError(mockRequest, errorType);

        //then
        assertThat(response).contains(URLEncDec.encode(SAML2Proxy.DEFAULT_ERROR_MESSAGE));
    }

    @Test
    public void shouldCreateDefaultErrorHTMLPostFromDataViaGET() {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getCookies()).willReturn(validCookies);
        given(mockRequest.getMethod()).willReturn("POST");
        String errorType = "200";

        //when
        String url = SAML2Proxy.getUrlWithError(mockRequest, errorType);

        //then
        assertThat(url).contains(URLEncDec.encode(SAML2Proxy.DEFAULT_ERROR_MESSAGE));
    }

    @Test
    public void shouldCreateErrorHTMLPostFromDataViaPOST() {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getCookies()).willReturn(validCookies);
        given(mockRequest.getMethod()).willReturn("POST");
        String errorType = "200";
        given(mockRequest.getParameter(SAML2Constants.SAML_RESPONSE)).willReturn("SAMLResponse");

        //when
        String result = SAML2Proxy.getUrlWithError(mockRequest, errorType, "messageDetail");

        //then
        assertThat(result).contains(COOKIE_LOCATION);
        assertThat(result).contains("&" + SAML2Proxy.ERROR_PARAM_KEY + "=" + true);
        assertThat(result).contains("&" + SAML2Proxy.ERROR_CODE_PARAM_KEY + "=" + errorType);
        assertThat(result).contains("&" + SAML2Proxy.ERROR_MESSAGE_PARAM_KEY + "=" + "messageDetail");
    }

    @Test
    public void shouldCreateErrorHTMLPostFromDataViaGET() {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getCookies()).willReturn(validCookies);
        given(mockRequest.getMethod()).willReturn("GET");
        String errorType = "200";

        //when
        String result = SAML2Proxy.getUrlWithError(mockRequest, errorType, "MyMessage");

        //then
        assertThat(result).contains(COOKIE_LOCATION);
        assertThat(result).contains("&" + SAML2Proxy.ERROR_PARAM_KEY + "=" + true);
        assertThat(result).contains("&" + SAML2Proxy.ERROR_CODE_PARAM_KEY + "=" + errorType);
        assertThat(result).contains("&" + SAML2Proxy.ERROR_MESSAGE_PARAM_KEY + "=" + "MyMessage");
    }

    @Test
    public void shouldCreateErrorHTMLPostFromDataWithMessage() {

        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getCookies()).willReturn(validCookies);
        given(mockRequest.getMethod()).willReturn("POST");
        String errorType = "200";

        //when
        String result = SAML2Proxy.getUrlWithError(mockRequest, errorType, "MyMessage");

        //then
        assertThat(result).contains(COOKIE_LOCATION);
        assertThat(result).contains("&" + SAML2Proxy.ERROR_PARAM_KEY + "=" + true);
        assertThat(result).contains("&" + SAML2Proxy.ERROR_CODE_PARAM_KEY + "=" + errorType);
        assertThat(result).contains("&" + SAML2Proxy.ERROR_MESSAGE_PARAM_KEY + "=" + "MyMessage");
    }

    /**
     * processSamlResponse builds a URL and then writes it, and by then the response may no longer be
     * this class's to write to. A sendRedirect or a form write on a committed response is an
     * IllegalStateException over whatever the client has already received, so it has to stop.
     * <p>
     * This pins the second of the two guards, the one that catches a response committed by something
     * other than the bounce. The bounce itself signals through a null from getUrl and is pinned by
     * {@link #shouldStopOnceTheLoadBalancerBounceHasAnsweredTheRequest}.
     * <p>
     * Note the failure signal: drop the guard and this fails on the XUIState lookup rather than on
     * the assertions below, because returning before that lookup is part of what the guard does.
     */
    @Test
    public void shouldNotWriteOnTopOfACommittedResponse() throws Exception {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        given(mockRequest.getCookies()).willReturn(validCookies);
        given(mockRequest.getMethod()).willReturn("POST");
        given(mockRequest.getRequestURL()).willReturn(
                new StringBuffer("https://openam.example.com/openam/AuthConsumer/metaAlias/sp"));
        given(mockResponse.isCommitted()).willReturn(true);
        StringWriter written = new StringWriter();

        //when
        SAML2Proxy.processSamlResponse(mockRequest, mockResponse, new PrintWriter(written, true));

        //then
        // any(), not anyString(): Mockito 2's anyString() does not match null, so a regression that
        // redirected to a null URL would slip past a never()/anyString() pair.
        verify(mockResponse, never()).sendRedirect(any());
        assertThat(written.toString()).isEmpty();
    }

    /**
     * The bounce itself: an HTTP-Artifact arrival on a node of a multi server site that carries no
     * load balancer cookie, with cookieHashRedirectEnabled on. FSUtils answers the request with a
     * redirect to ?redirected=1, and this class must not write anything on top of it.
     * <p>
     * The signal is a null out of getUrl and cannot be inferred from response.isCommitted():
     * sendRedirect sets the status and Location and then suspends, and Tomcat flushes only when the
     * context is configured to send a redirect body, which it is not by default. Before this was
     * threaded through, getUrl turned the bounce into a MISSING_COOKIE error URL and the redirect to
     * it silently replaced the bounce, so an artifact login on such a site could not complete.
     * <p>
     * Note the failure signal: remove the null check and this fails on the metadata lookup that
     * follows it, because returning before that lookup is what the check does.
     */
    @Test
    public void shouldStopOnceTheLoadBalancerBounceHasAnsweredTheRequest() throws Exception {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        // validCookies carries the authentication step cookie only - no amlbcookie, which is the
        // shape the bounce acts on. The second server of the site comes from
        // StubPlatformConfiguration, registered in this module's test FederationConfig.properties.
        given(mockRequest.getCookies()).willReturn(validCookies);
        given(mockRequest.getScheme()).willReturn("https");
        given(mockRequest.getServerName()).willReturn("openam.example.com");
        given(mockRequest.getServerPort()).willReturn(443);
        given(mockRequest.getMethod()).willReturn("GET");
        given(mockRequest.getRequestURL()).willReturn(new StringBuffer(ACS_URL));
        given(mockRequest.getQueryString()).willReturn(ARTIFACT_QUERY);
        StringWriter written = new StringWriter();

        //when
        // initializeProperties can only add keys, so the flag goes back as "false", which
        // FSUtils.requireRedirect treats exactly as it treats an unset flag.
        SystemPropertiesManager.initializeProperties(COOKIE_HASH_REDIRECT, "true");
        try {
            SAML2Proxy.processSamlResponse(mockRequest, mockResponse, new PrintWriter(written, true));
        } finally {
            SystemPropertiesManager.initializeProperties(COOKIE_HASH_REDIRECT, "false");
        }

        //then
        ArgumentCaptor<String> redirect = ArgumentCaptor.forClass(String.class);
        // Exactly one redirect, the bounce's own: a second one would be the error URL going out on
        // top of it.
        verify(mockResponse, times(1)).sendRedirect(redirect.capture());
        assertThat(redirect.getValue()).isEqualTo(ACS_URL + "?redirected=1&" + ARTIFACT_QUERY);
        assertThat(written.toString()).isEmpty();
    }
}

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
*/
package org.forgerock.openam.authentication.modules.saml2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.authentication.modules.saml2.Constants.AM_LOCATION_COOKIE;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.xml.bind.StringInputStream;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.xml.xpath.XPathFactory;

import org.forgerock.util.xml.XMLUtils;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

public class SAML2ProxyTest {

    private final static String KEY = "key";
    private Cookie[] validCookies;
    private final static String COOKIE_LOCATION = "/openam/XUI/#login";

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
}

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
* Copyright 2013 ForgeRock Inc.
*/

package org.forgerock.openam.forgerockrest.authn.callbackhandlers;

import com.sun.identity.authentication.spi.X509CertificateCallback;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.forgerockrest.authn.core.HttpMethod;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.mockito.Matchers;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import java.security.cert.X509Certificate;

import static junit.framework.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class RestAuthX509CallbackHandlerTest {

    private RestAuthCallbackHandler<X509CertificateCallback> restAuthX509CallbackHandler;

    @BeforeClass
    public void setUp() {
        restAuthX509CallbackHandler = new RestAuthX509CallbackHandler();
    }

    @Test
    public void shouldGetCallbackClassName() {

        //Given

        //When
        String callbackClassName = restAuthX509CallbackHandler.getCallbackClassName();

        //Then
        assertEquals(X509CertificateCallback.class.getSimpleName(), callbackClassName);
    }

    @Test
    public void shouldUpdateCallbackFromRequest() throws RestAuthCallbackHandlerResponseException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonValue jsonPostBody = mock(JsonValue.class);
        X509CertificateCallback x509CertificateCallback = mock(X509CertificateCallback.class);
        X509Certificate x509Certificate = mock(X509Certificate.class);
        X509Certificate[] x509Certificates = new X509Certificate[]{x509Certificate};

        given(request.getAttribute("javax.servlet.request.X509Certificate")).willReturn(x509Certificates);

        //When
        boolean updated = restAuthX509CallbackHandler.updateCallbackFromRequest(headers, request, response,
                jsonPostBody, x509CertificateCallback, HttpMethod.POST);

        //Then
        verify(x509CertificateCallback).setCertificate(x509Certificate);
        assertTrue(updated);
    }

    @Test
    public void shouldUpdateCallbackFromRequestWithMultipleX509Certificates()
            throws RestAuthCallbackHandlerResponseException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonValue jsonPostBody = mock(JsonValue.class);
        X509CertificateCallback x509CertificateCallback = mock(X509CertificateCallback.class);
        X509Certificate x509Certificate = mock(X509Certificate.class);
        X509Certificate x509Certificate2 = mock(X509Certificate.class);
        X509Certificate[] x509Certificates = new X509Certificate[]{x509Certificate, x509Certificate2};

        given(request.getAttribute("javax.servlet.request.X509Certificate")).willReturn(x509Certificates);

        //When
        boolean updated = restAuthX509CallbackHandler.updateCallbackFromRequest(headers, request, response,
                jsonPostBody, x509CertificateCallback, HttpMethod.POST);

        //Then
        verify(x509CertificateCallback).setCertificate(x509Certificate);
        assertTrue(updated);
    }

    @Test
    public void shouldNotUpdateCallbackFromRequestWithNoX509Certificate()
            throws RestAuthCallbackHandlerResponseException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonValue jsonPostBody = mock(JsonValue.class);
        X509CertificateCallback x509CertificateCallback = mock(X509CertificateCallback.class);
        X509Certificate[] x509Certificates = new X509Certificate[]{};

        given(request.getAttribute("javax.servlet.request.X509Certificate")).willReturn(x509Certificates);

        //When
        boolean updated = restAuthX509CallbackHandler.updateCallbackFromRequest(headers, request, response,
                jsonPostBody, x509CertificateCallback, HttpMethod.POST);

        //Then
        verify(x509CertificateCallback, never()).setCertificate(Matchers.<X509Certificate>anyObject());
        assertTrue(updated);
    }

    @Test
    public void shouldHandleCallback() {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonValue jsonPostBody = mock(JsonValue.class);
        X509CertificateCallback originalX509CertificateCallback = mock(X509CertificateCallback.class);

        //When
        X509CertificateCallback x509CertificateCallback = restAuthX509CallbackHandler.handle(headers, request, response,
                jsonPostBody, originalX509CertificateCallback);

        //Then
        assertEquals(originalX509CertificateCallback, x509CertificateCallback);
    }

    @Test (expectedExceptions = RestAuthException.class)
    public void shouldFailConvertToJson() {

        //Given

        //When
        restAuthX509CallbackHandler.convertToJson(null, 1);

        //Then
        fail();
    }

    @Test (expectedExceptions = RestAuthException.class)
    public void shouldFailToConvertFromJson() {

        //Given

        //When
        restAuthX509CallbackHandler.convertFromJson(null, null);

        //Then
        fail();
    }
}

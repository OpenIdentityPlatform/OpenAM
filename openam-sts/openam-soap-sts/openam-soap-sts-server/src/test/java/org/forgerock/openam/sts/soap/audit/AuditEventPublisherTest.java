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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openam.sts.soap.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.net.ProtocolException;
import java.net.URL;
import java.util.Map;

import org.forgerock.audit.events.AuditEvent;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.HttpURLConnectionWrapper;
import org.forgerock.openam.sts.HttpURLConnectionWrapperFactory;
import org.forgerock.openam.sts.soap.bootstrap.SoapSTSAccessTokenProvider;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @since 13.0.0
 */
public class AuditEventPublisherTest {

    private SoapSTSAccessTokenProvider soapSTSAccessTokenProvider;
    private HttpURLConnectionWrapperFactory httpURLConnectionWrapperFactory;
    private SoapSTSAuditEventPublisher auditEventPublisher;

    @BeforeMethod
    protected void setUp() {
        soapSTSAccessTokenProvider = mock(SoapSTSAccessTokenProvider.class);
        httpURLConnectionWrapperFactory = mock(HttpURLConnectionWrapperFactory.class);

        auditEventPublisher = new SoapSTSAuditEventPublisher(
                httpURLConnectionWrapperFactory,
                "http://openam.example.com:8080/openam/json/audit/access/?_action=create",
                "iPlanetDirectoryPro",
                "protocol=1.0, resource=1.0",
                soapSTSAccessTokenProvider,
                mock(Logger.class));
    }

    @Test
    public void publishesAuditEventsToAMAuditServiceEndpoint() throws Exception {
        ArgumentCaptor<URL> urlCaptor = ArgumentCaptor.forClass(URL.class);
        ArgumentCaptor<Map> headersCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<String> methodCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

        // Given
        AuditEvent auditEvent = mockAuditEvent("event-as-json");
        HttpURLConnectionWrapper httpURLConnectionWrapper = mockHttpURLConnectionWrapper(headersCaptor, methodCaptor, payloadCaptor);
        given(soapSTSAccessTokenProvider.getAccessToken()).willReturn("ssoTokenId");
        given(httpURLConnectionWrapperFactory.httpURLConnectionWrapper(urlCaptor.capture())).willReturn(httpURLConnectionWrapper);

        // When
        auditEventPublisher.tryPublish(AuditConstants.ACCESS_TOPIC, auditEvent);

        // Then
        verify(soapSTSAccessTokenProvider, times(1)).getAccessToken();
        assertThat(urlCaptor.getValue()).isEqualTo(new URL("http://openam.example.com:8080/openam/json/audit/access/?_action=create"));
        assertThat(headersCaptor.getValue().get(AMSTSConstants.CONTENT_TYPE)).isEqualTo(AMSTSConstants.APPLICATION_JSON);
        assertThat(headersCaptor.getValue().get(AMSTSConstants.CREST_VERSION_HEADER_KEY)).isEqualTo("protocol=1.0, resource=1.0");
        assertThat(headersCaptor.getValue().get(AMSTSConstants.COOKIE)).isEqualTo("iPlanetDirectoryPro=ssoTokenId");
        assertThat(methodCaptor.getValue()).isEqualToIgnoringCase(AMSTSConstants.GET);
        assertThat(payloadCaptor.getValue()).isEqualToIgnoringCase("event-as-json");
        verify(soapSTSAccessTokenProvider, times(1)).invalidateAccessToken("ssoTokenId");
    }

    @Test
    public void ensuresCreatedSoapSTSAgentSessionIsLoggedOut() throws Exception {
        // Given
        given(soapSTSAccessTokenProvider.getAccessToken()).willReturn("ssoTokenId");
        given(httpURLConnectionWrapperFactory.httpURLConnectionWrapper(any(URL.class))).willThrow(new RuntimeException());

        // When
        auditEventPublisher.tryPublish(AuditConstants.ACCESS_TOPIC, mock(AuditEvent.class));

        // Then
        verify(soapSTSAccessTokenProvider, times(1)).getAccessToken();
        verify(soapSTSAccessTokenProvider, times(1)).invalidateAccessToken("ssoTokenId");

    }

    private AuditEvent mockAuditEvent(String content) {
        AuditEvent auditEvent = mock(AuditEvent.class);
        JsonValue jsonValue = mock(JsonValue.class);
        given(auditEvent.getValue()).willReturn(jsonValue);
        given(jsonValue.toString()).willReturn(content);
        return auditEvent;
    }

    @SuppressWarnings("unchecked")
    private HttpURLConnectionWrapper mockHttpURLConnectionWrapper(
            final ArgumentCaptor<Map> headersCaptor,
            final ArgumentCaptor<String> methodCaptor,
            final ArgumentCaptor<String> payloadCaptor
    ) throws ProtocolException {

        HttpURLConnectionWrapper httpURLConnectionWrapper = mock(HttpURLConnectionWrapper.class);
        given(httpURLConnectionWrapper.withoutAuditTransactionIdHeader()).willReturn(httpURLConnectionWrapper);
        given(httpURLConnectionWrapper.setRequestHeaders(headersCaptor.capture())).willReturn(httpURLConnectionWrapper);
        given(httpURLConnectionWrapper.setRequestMethod(methodCaptor.capture())).willReturn(httpURLConnectionWrapper);
        given(httpURLConnectionWrapper.setRequestPayload(payloadCaptor.capture())).willReturn(httpURLConnectionWrapper);
        return httpURLConnectionWrapper;
    }

}

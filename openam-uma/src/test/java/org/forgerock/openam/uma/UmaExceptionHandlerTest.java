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

package org.forgerock.openam.uma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.forgerock.json.JsonValue.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Map;

import org.forgerock.openam.rest.representations.JacksonRepresentationFactory;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class UmaExceptionHandlerTest {

    private UmaExceptionHandler exceptionFilter;
    private JacksonRepresentationFactory jacksonRepresentationFactory =
            new JacksonRepresentationFactory(new ObjectMapper());

    @BeforeMethod
    public void setup() {
        exceptionFilter = new UmaExceptionHandler(jacksonRepresentationFactory);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldSet500ExceptionResponse() throws IOException {

        //Given
        Response response = mock(Response.class);
        Exception exception = new Exception("MESSAGE");
        Status status = new Status(444, exception);

        given(response.getStatus()).willReturn(status);

        //When
        exceptionFilter.handleException(response, exception);

        //Then
        ArgumentCaptor<JacksonRepresentation> exceptionResponseCaptor =
                ArgumentCaptor.forClass(JacksonRepresentation.class);
        verify(response).setEntity(exceptionResponseCaptor.capture());
        Map<String, String> responseBody = (Map<String, String>) exceptionResponseCaptor.getValue().getObject();
        assertThat(responseBody).containsOnly(entry("error", "server_error"), entry("error_description", "MESSAGE"));

        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(response).setStatus(statusCaptor.capture());
        assertThat(statusCaptor.getValue().getCode()).isEqualTo(444);
        assertThat(statusCaptor.getValue().getThrowable()).isEqualTo(exception);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldSetUmaExceptionResponse() throws IOException {

        //Given
        Response response = mock(Response.class);
        Throwable throwable = mock(Throwable.class);
        Exception exception = new UmaException(444, "ERROR", "DESCRIPTION");
        given(throwable.getCause()).willReturn(exception);
        Status status = new Status(444, exception);

        given(response.getStatus()).willReturn(status);

        //When
        exceptionFilter.handleException(response, throwable);

        //Then
        ArgumentCaptor<JacksonRepresentation> exceptionResponseCaptor =
                ArgumentCaptor.forClass(JacksonRepresentation.class);
        verify(response).setEntity(exceptionResponseCaptor.capture());
        Map<String, String> responseBody = (Map<String, String>) exceptionResponseCaptor.getValue().getObject();
        assertThat(responseBody).containsOnly(entry("error", "ERROR"), entry("error_description", "DESCRIPTION"));

        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(response).setStatus(statusCaptor.capture());
        assertThat(statusCaptor.getValue().getCode()).isEqualTo(444);
        assertThat(statusCaptor.getValue().getThrowable()).isEqualTo(exception);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldSetUmaExceptionResponseWithDetail() throws IOException {

        //Given
        Response response = mock(Response.class);
        Throwable throwable = mock(Throwable.class);
        Exception exception = new UmaException(444, "ERROR", "DESCRIPTION")
                .setDetail(json(object(field("DETAIL", "VALUE"))));
        given(throwable.getCause()).willReturn(exception);
        Status status = new Status(444, exception);

        given(response.getStatus()).willReturn(status);

        //When
        exceptionFilter.handleException(response, throwable);

        //Then
        ArgumentCaptor<JacksonRepresentation> exceptionResponseCaptor =
                ArgumentCaptor.forClass(JacksonRepresentation.class);
        verify(response).setEntity(exceptionResponseCaptor.capture());
        Map<String, String> responseBody = (Map<String, String>) exceptionResponseCaptor.getValue().getObject();
        assertThat(responseBody).containsOnly(entry("error", "ERROR"), entry("error_description", "DESCRIPTION"),
                entry("DETAIL", "VALUE"));

        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(response).setStatus(statusCaptor.capture());
        assertThat(statusCaptor.getValue().getCode()).isEqualTo(444);
        assertThat(statusCaptor.getValue().getThrowable()).isEqualTo(exception);
    }
}

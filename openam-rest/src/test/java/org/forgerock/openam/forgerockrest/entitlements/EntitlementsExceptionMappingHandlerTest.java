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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.forgerockrest.entitlements;

import com.sun.identity.entitlement.EntitlementException;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestType;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.servlet.HttpContext;
import org.forgerock.openam.forgerockrest.utils.ServerContextUtils;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.forgerock.json.fluent.JsonValue.*;

public class EntitlementsExceptionMappingHandlerTest {
    private static final int ERROR_CODE = 123;
    private static final String ERROR_MESSAGE = "a test message";

    @Test
    public void shouldMapKnownErrorsAsConfigured() {
        // Given
        EntitlementsExceptionMappingHandler errorHandler = new EntitlementsExceptionMappingHandler(
                Collections.singletonMap(ERROR_CODE, ResourceException.NOT_FOUND));
        EntitlementException error = exception(ERROR_CODE, ERROR_MESSAGE);

        // When
        ResourceException result = errorHandler.handleError(null, null, error);

        // Then
        assertThat(result).isInstanceOf(NotFoundException.class)
            .hasMessage(ERROR_MESSAGE);
    }

    @Test
    public void shouldMapUnknownErrorsAsServerErrors() {
        // Given
        EntitlementsExceptionMappingHandler errorHandler = new EntitlementsExceptionMappingHandler(
                Collections.<Integer, Integer>emptyMap());
        EntitlementException error = exception(ERROR_CODE, ERROR_MESSAGE);

        // When
        ResourceException result = errorHandler.handleError(null, null, error);

        // Then
        assertThat(result).isInstanceOf(InternalServerErrorException.class)
                .hasMessage(ERROR_MESSAGE);

    }

    @Test
    public void shouldApplyRequestTypeOverrides() {
        // Given
        Map<RequestType, Map<Integer, Integer>> overrides = new HashMap<RequestType, Map<Integer, Integer>>();
        RequestType requestType = RequestType.CREATE;
        overrides.put(requestType, Collections.singletonMap(ResourceException.NOT_FOUND, ResourceException.BAD_REQUEST));
        EntitlementsExceptionMappingHandler errorHandler = new EntitlementsExceptionMappingHandler(
                Collections.singletonMap(ERROR_CODE, ResourceException.NOT_FOUND),
                overrides
        );
        EntitlementException error = exception(ERROR_CODE, ERROR_MESSAGE);
        Request request = mock(Request.class);
        given(request.getRequestType()).willReturn(requestType);

        // When
        ResourceException result = errorHandler.handleError(null, request, error);

        // Then
        assertThat(result).isInstanceOf(BadRequestException.class)
                          .hasMessage(ERROR_MESSAGE);
    }

    @Test
    public void shouldGetExceptionMessageAsEnglish() throws Exception {
        // Given
        EntitlementsExceptionMappingHandler errorHandler = new EntitlementsExceptionMappingHandler(Collections.singletonMap(EntitlementException.EMPTY_PRIVILEGE_NAME, ResourceException.BAD_REQUEST));
        EntitlementException error = new EntitlementException(EntitlementException.EMPTY_PRIVILEGE_NAME);

        // When
        ResourceException result = errorHandler.handleError(getHttpServerContext("en"), null, error);

        // Then
        assertThat(result.getMessage()).isEqualTo("Policy name cannot be empty.");
    }

    @Test
    public void shouldGetExceptionMessageAsFrench() throws Exception {
        // Given
        EntitlementsExceptionMappingHandler errorHandler = new EntitlementsExceptionMappingHandler(
        Collections.singletonMap(EntitlementException.SUBJECT_REQUIRED, ResourceException.BAD_REQUEST));
        EntitlementException error = new EntitlementException(EntitlementException.SUBJECT_REQUIRED);

        // When
        ResourceException result = errorHandler.handleError(getHttpServerContext("fr"), null, error);

        // Then
        assertThat(result.getMessage()).isEqualTo("Les objets sont obligatoires.");
    }


    private EntitlementException exception(int code, String message) {
        // Use a mock to avoid loading error messages from the resource bundle
        EntitlementException error = mock(EntitlementException.class);
        given(error.getErrorCode()).willReturn(code);
        given(error.getMessage()).willReturn(message);
        return error;
    }

    private ServerContext getHttpServerContext(String ...language) throws Exception {
        final HttpContext httpContext = new HttpContext(json(object(field(HttpContext.ATTR_HEADERS,
                Collections.singletonMap(ServerContextUtils.ACCEPT_LANGUAGE, Arrays.asList(language))),
                field(HttpContext.ATTR_PARAMETERS, Collections.emptyMap()))), null);
        return new ServerContext(httpContext);
    }
}

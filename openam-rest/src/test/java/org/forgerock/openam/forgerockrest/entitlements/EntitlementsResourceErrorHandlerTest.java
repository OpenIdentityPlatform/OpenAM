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
 * Copyright 2014 ForgeRock, AS.
 */

package org.forgerock.openam.forgerockrest.entitlements;

import com.sun.identity.entitlement.EntitlementException;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestType;
import org.forgerock.json.resource.ResourceException;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class EntitlementsResourceErrorHandlerTest {
    private static final int ERROR_CODE = 123;
    private static final String ERROR_MESSAGE = "a test message";

    @Test
    public void shouldMapKnownErrorsAsConfigured() {
        // Given
        EntitlementsResourceErrorHandler errorHandler = new EntitlementsResourceErrorHandler(
                Collections.singletonMap(ERROR_CODE, ResourceException.NOT_FOUND));
        EntitlementException error = exception(ERROR_CODE, ERROR_MESSAGE);

        // When
        ResourceException result = errorHandler.handleError(null, error);

        // Then
        assertThat(result).isInstanceOf(NotFoundException.class)
            .hasMessage(ERROR_MESSAGE);
    }

    @Test
    public void shouldMapUnknownErrorsAsServerErrors() {
        // Given
        EntitlementsResourceErrorHandler errorHandler = new EntitlementsResourceErrorHandler(
                Collections.<Integer, Integer>emptyMap());
        EntitlementException error = exception(ERROR_CODE, ERROR_MESSAGE);

        // When
        ResourceException result = errorHandler.handleError(null, error);

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
        EntitlementsResourceErrorHandler errorHandler = new EntitlementsResourceErrorHandler(
                Collections.singletonMap(ERROR_CODE, ResourceException.NOT_FOUND),
                overrides
        );
        EntitlementException error = exception(ERROR_CODE, ERROR_MESSAGE);
        Request request = mock(Request.class);
        given(request.getRequestType()).willReturn(requestType);

        // When
        ResourceException result = errorHandler.handleError(request, error);

        // Then
        assertThat(result).isInstanceOf(BadRequestException.class)
                          .hasMessage(ERROR_MESSAGE);
    }

    private EntitlementException exception(int code, String message) {
        // Use a mock to avoid loading error messages from the resource bundle
        EntitlementException error = mock(EntitlementException.class);
        given(error.getErrorCode()).willReturn(code);
        given(error.getMessage()).willReturn(message);
        return error;
    }
}

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
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.openam.rest.service;

import org.restlet.Request;
import org.testng.annotations.Test;

import java.util.concurrent.ConcurrentHashMap;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

public class RestletRealmRouterTest {
    @Test
    public void shouldExtractRealmFromRequest() {
        // Given
        String key = "badger";
        Request mockRequest = generateRequest();
        mockRequest.getAttributes().put(RestletRealmRouter.REALM, key);

        // When
        String result = RestletRealmRouter.getRealmFromRequest(mockRequest);

        // Then
        assertThat(result).isEqualTo(key);
    }

    @Test
    public void shouldReturnNullWhenMissing() {
        // Given
        Request mockRequest = generateRequest();

        // When
        String result = RestletRealmRouter.getRealmFromRequest(mockRequest);

        // Then
        assertThat(result).isNull();
    }

    private static Request generateRequest() {
        Request r = mock(Request.class);
        ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<String, Object>();
        given(r.getAttributes()).willReturn(map);
        return r;
    }
}
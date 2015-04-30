/*
 * Copyright 2015 Nomura Research Institute, Ltd.
 *
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
 * Portions Copyrighted 2015 ForgeRock, AS.
 */

package org.forgerock.openam.utils;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

import javax.servlet.http.HttpServletRequest;

import org.testng.annotations.Test;

import com.sun.identity.shared.configuration.SystemPropertiesManager;

public class ClientUtilsTest {

    @Test
    public void shouldReturnXffFirstIpAddress() {

        // Given
        String originalIpAddress = "192.168.1.10";
        String xffKey = "X-Forwarded-For";
        String xffValue = "192.168.1.10, 192.168.1.21, 192.168.1.22";
        SystemPropertiesManager.initializeProperties("com.sun.identity.authentication.client.ipAddressHeader", xffKey);
        HttpServletRequest request = mock(HttpServletRequest.class);
        given(request.getHeader(xffKey)).willReturn(xffValue);

        // When
        String clientIPAddress = ClientUtils.getClientIPAddress(request);

        // Then
        assertEquals(clientIPAddress, originalIpAddress);
    }
}

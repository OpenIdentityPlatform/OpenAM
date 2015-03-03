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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.utils;

import static org.fest.assertions.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.forgerock.openam.services.baseurl.ForwardedHeaderBaseURLProvider;
import org.forgerock.util.Pair;
import org.testng.annotations.Test;

public class ForwardedHeaderTest {

    @Test
    public void testParse() {
        // Given
        String header1 = "for=192.0.2.43, for=198.51.100.17;proto=http;by=203.0.113.43;host=\"abc.123\\\";=fred\"";
        String header2 = "for=192.52.1.35";

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeaders("Forwarded")).thenReturn(Collections.enumeration(Arrays.asList(header1, header2)));

        // When
        ForwardedHeader header = ForwardedHeader.parse(request);

        // Then
        assertThat(header.getForValues()).containsExactly("192.0.2.43", "198.51.100.17", "192.52.1.35");
        assertThat(header.getProtoValues()).containsExactly("http");
        assertThat(header.getByValues()).containsExactly("203.0.113.43");
        assertThat(header.getHostValues()).containsExactly("abc.123\";=fred");
    }

}
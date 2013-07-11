/**
 * Copyright 2013 ForgeRock, Inc.
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
 */
package org.forgerock.openam.auth.shared;

import org.testng.annotations.Test;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.AssertJUnit.assertNull;

/**
 * @author robert.wapshott@forgerock.com
 */
public class AuthnRequestUtilsTest {
    @Test
    public void shouldReturnNullIfNoCookies() {
        // Given
        String key = "badger";
        HttpServletRequest request = mock(HttpServletRequest.class);
        AuthnRequestUtils utils = new AuthnRequestUtils(key);

        // When
        String response = utils.getTokenId(request);

        // Then
        assertNull(response);
    }

    @Test
    public void shouldUseCookiesToFindTokenId() {
        // Given
        String key = "badger";
        HttpServletRequest request = mock(HttpServletRequest.class);
        given(request.getCookies()).willReturn(new Cookie[]{});

        AuthnRequestUtils utils = new AuthnRequestUtils(key);

        // When
        utils.getTokenId(request);

        // Then
        verify(request).getCookies();
    }

    @Test
    public void shouldUseCookieNameToSelectCookie() {
        // Given
        String key = "badger";
        HttpServletRequest request = mock(HttpServletRequest.class);

        Cookie one = mock(Cookie.class);
        given(one.getName()).willReturn(key);
        given(request.getCookies()).willReturn(new Cookie[]{one});

        AuthnRequestUtils utils = new AuthnRequestUtils(key);

        // When
        utils.getTokenId(request);

        // Then
        verify(one).getValue();
    }
}

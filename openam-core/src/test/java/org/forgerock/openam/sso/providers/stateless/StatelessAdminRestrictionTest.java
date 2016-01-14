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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openam.sso.providers.stateless;


import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;

import java.security.Principal;

import org.forgerock.openam.sso.providers.stateless.StatelessAdminRestriction.SuperUserDelegate;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;

public class StatelessAdminRestrictionTest {

    private SuperUserDelegate mockDelegate;
    private StatelessSessionFactory mockFactory;
    private StatelessAdminRestriction restriction;

    @BeforeMethod
    public void setup() {
        mockDelegate = mock(SuperUserDelegate.class);
        mockFactory = mock(StatelessSessionFactory.class);
        restriction = new StatelessAdminRestriction(mockDelegate, mockFactory);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldNotAllowNullSSOToken() throws SessionException {
        restriction.isRestricted((SSOToken)null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldNotAllowNullUserDN() throws SessionException {
        restriction.isRestricted((String)null);
    }

    @Test (expectedExceptions = SessionException.class)
    public void shouldErrorForNonStatelessSession() throws SessionException {
        // Given
        given(mockFactory.containsJwt(anyString())).willReturn(false);
        SSOToken token = mock(SSOToken.class);
        given(token.toString()).willReturn("badger");

        // When
        restriction.isRestricted(token);
    }

    @Test (expectedExceptions = SessionException.class)
    public void shouldHandleErrorProcessingSSOToken() throws SessionException, SSOException {
        // Given
        given(mockFactory.containsJwt(anyString())).willReturn(true);
        SSOToken token = mock(SSOToken.class);
        given(token.toString()).willReturn("badger");
        given(token.getPrincipal()).willThrow(new SSOException("TEST"));

        // When
        restriction.isRestricted(token);
    }

    @Test
    public void shouldIndicateTrueForAdminToken() throws SSOException, SessionException {
        given(mockFactory.containsJwt(anyString())).willReturn(true);
        SSOToken token = mock(SSOToken.class);
        given(token.toString()).willReturn("badger");
        Principal mockPrincipal = mock(Principal.class);
        given(token.getPrincipal()).willReturn(mockPrincipal);
        given(mockPrincipal.getName()).willReturn("badger");

        given(mockDelegate.isSuperUser(anyString())).willReturn(true);

        // When
        boolean result = restriction.isRestricted(token);

        // Then
        assertThat(result).isTrue();
    }


}
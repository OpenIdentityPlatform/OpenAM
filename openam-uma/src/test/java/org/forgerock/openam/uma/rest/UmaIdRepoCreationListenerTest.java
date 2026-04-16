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

package org.forgerock.openam.uma.rest;

import static org.mockito.AdditionalMatchers.or;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.forgerock.openam.core.CoreWrapper;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.idm.AMIdentityRepository;

public class UmaIdRepoCreationListenerTest {

    private UmaIdRepoCreationListener listener;
    @Mock
    private UmaPolicyApplicationListener umaPolicyApplicationListener;
    @Mock
    private CoreWrapper coreWrapper;

    @BeforeMethod
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        listener = new UmaIdRepoCreationListener(umaPolicyApplicationListener, coreWrapper);
    }

    @Test
    public void testNotify() throws Exception {
        // Given
        AMIdentityRepository repo = mock(AMIdentityRepository.class);
        given(coreWrapper.convertRealmNameToOrgName("/test")).willReturn("ou=test");

        // When
        listener.notify(repo, "/test");

        // Then
        verify(repo).addEventListener(umaPolicyApplicationListener);
    }

    @Test
    public void testNotifyMixedCase() throws Exception {
        // Given
        AMIdentityRepository repo = mock(AMIdentityRepository.class);
        given(coreWrapper.convertRealmNameToOrgName(or(eq("/test"), eq("/TEST")))).willReturn("ou=test");

        // When
        listener.notify(repo, "/test");
        listener.notify(repo, "/TEST");

        // Then
        verify(repo, times(1)).addEventListener(umaPolicyApplicationListener);
    }

}
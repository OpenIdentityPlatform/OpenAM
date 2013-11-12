/**
 * Copyright 2013 ForgeRock AS.
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
package org.forgerock.openam.cts;

import org.forgerock.openam.cts.impl.CTSConnectionFactory;
import org.forgerock.openam.cts.impl.LDAPConfig;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.BDDMockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author robert.wapshott@forgerock.com
 */
public class CoreTokenConfigListenerTest {

    private CoreTokenConfigListener listener;
    private LDAPConfig mockLDAPConfig;
    private ExternalTokenConfig mockExternalConfig;
    private CTSConnectionFactory mockFactory;

    @BeforeMethod
    public void setUp() throws Exception {
        mockLDAPConfig = mock(LDAPConfig.class);
        mockExternalConfig = mock(ExternalTokenConfig.class);
        mockFactory = mock(CTSConnectionFactory.class);
        listener = new CoreTokenConfigListener(mockLDAPConfig, mockExternalConfig, mockFactory);
    }

    @Test
    public void shouldCallBothConfigurations() {
        // Given
        // When
        listener.notifyChanges();
        // Then
        verify(mockLDAPConfig).update();
        verify(mockExternalConfig).update();
    }

    @Test
    public void shouldCallConnectionFactory() {
        // Given
        // When
        listener.notifyChanges();
        // Then
        verify(mockFactory).updateConnection();
    }
}

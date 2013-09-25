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

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.times;

/**
 * @author robert.wapshott@forgerock.com
 */
@PrepareForTest(SystemProperties.class)
public class ExternalTokenConfigTest extends PowerMockTestCase {
    @Test
    public void shouldUseSystemPropertiesWrapperForNotifyChanges() {
        // Given
        PowerMockito.mockStatic(SystemProperties.class);
        ExternalTokenConfig config = new ExternalTokenConfig();
        // When
        config.update();
        // Then
        PowerMockito.verifyStatic(times(6));
        SystemProperties.get(anyString());

        PowerMockito.verifyStatic();
        SystemProperties.getAsBoolean(anyString(), anyBoolean());
    }

    @Test
    public void shouldIndicateHasChanged() {
        PowerMockito.mockStatic(SystemProperties.class);
        given(SystemProperties.get(eq(Constants.CTS_STORE_HOSTNAME))).willReturn("badger");

        ExternalTokenConfig config = new ExternalTokenConfig();
        // When
        config.update();
        // Then
        assertThat(config.hasChanged()).isTrue();
    }
}

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

package org.forgerock.openam.session.stateless;

import com.iplanet.am.util.SystemPropertiesWrapper;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;

public class StatelessConfigTest {

    private StatelessConfig config;
    private SystemPropertiesWrapper wrapper;

    @BeforeMethod
    public void setUp() {
        wrapper = mock(SystemPropertiesWrapper.class);
        config = new StatelessConfig(wrapper);
    }

    @Test
    public void shouldUseWrapperForJWTCacheSize() {
        int value = 123;
        given(wrapper.getAsInt(anyString(), anyInt())).willReturn(value);
        assertThat(config.getJWTCacheSize()).isEqualTo(value);
    }
}
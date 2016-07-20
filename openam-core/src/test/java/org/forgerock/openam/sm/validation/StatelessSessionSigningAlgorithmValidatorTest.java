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

package org.forgerock.openam.sm.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Collections;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.shared.configuration.ISystemProperties;

public class StatelessSessionSigningAlgorithmValidatorTest {

    @Mock
    private ISystemProperties mockSystemProperties;

    private StatelessSessionSigningAlgorithmValidator testValidator;

    @BeforeMethod
    public void createValidator() {
        MockitoAnnotations.initMocks(this);
        testValidator = new StatelessSessionSigningAlgorithmValidator(mockSystemProperties);
    }

    @Test
    public void shouldRejectNull() {
        assertThat(testValidator.validate(null)).isFalse();
    }

    @Test
    public void shouldRejectEmpty() {
        assertThat(testValidator.validate(Collections.<String>emptySet())).isFalse();
    }

    @Test
    public void shouldRejectNoneByDefault() {
        assertThat(testValidator.validate(Collections.singleton("NONE"))).isFalse();
    }

    @Test
    public void shouldAllowNoneIfConfigured() {
        given(mockSystemProperties.getOrDefault("org.forgerock.openam.session.stateless.signing.allownone", "false"))
                .willReturn("true");
        assertThat(testValidator.validate(Collections.singleton("NONE"))).isTrue();
    }
}
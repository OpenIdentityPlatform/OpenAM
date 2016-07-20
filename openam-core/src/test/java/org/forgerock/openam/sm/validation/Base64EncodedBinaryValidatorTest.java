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

import java.util.Collections;

import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.util.encode.Base64;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class Base64EncodedBinaryValidatorTest {

    private Base64EncodedBinaryValidator testValidator;

    @BeforeMethod
    public void createValidator() {
        testValidator = new Base64EncodedBinaryValidator();
    }

    @Test
    public void shouldRejectNullValues() {
        assertThat(testValidator.validate(null)).isFalse();
    }

    @Test
    public void shouldRejectEmptyValues() {
        assertThat(testValidator.validate(Collections.<String>emptySet())).isFalse();
    }

    @Test
    public void shouldRejectTooManyValues() {
        assertThat(testValidator.validate(CollectionUtils.asSet("a", "b"))).isFalse();
    }

    @Test
    public void shouldRejectNonBase64Values() {
        assertThat(testValidator.validate(Collections.singleton("*&(*£&(&$£$£(**!%£"))).isFalse();
    }

    @Test
    public void shouldRejectTooSmallValue() {
        assertThat(testValidator.validate(Collections.singleton(Base64.encode(new byte[15])))).isFalse();
    }

    @Test
    public void shouldAllowCorrectValues() {
        assertThat(testValidator.validate(Collections.singleton(Base64.encode(new byte[16])))).isTrue();
    }

    @Test
    public void shouldAllowLargerValues() {
        assertThat(testValidator.validate(Collections.singleton(Base64.encode(new byte[17])))).isTrue();
    }
}
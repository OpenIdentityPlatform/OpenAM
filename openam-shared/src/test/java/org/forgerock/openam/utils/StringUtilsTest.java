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
 * Copyright 2014 ForgeRock AS.
 */
/*
 * Portions Copyrighted 2014 Nomura Research Institute, Ltd.
 */

package org.forgerock.openam.utils;

import static org.fest.assertions.Assertions.assertThat;

import java.io.UnsupportedEncodingException;

import org.testng.annotations.Test;

/**
 * Unit tests for {@link StringUtils}.
 *
 * @since 12.0.0
 */
public class StringUtilsTest {

    @Test
    public void shouldRespectInitialValue() {
        // When...
        String value = StringUtils.ifNullOrEmpty("abc", "def");
        // Then...
        assertThat(value).isEqualTo("abc");
    }

    @Test
    public void shouldUseDefaultWhenNull() {
        // When...
        String value = StringUtils.ifNullOrEmpty(null, "def");
        // Then...
        assertThat(value).isEqualTo("def");
    }

    @Test
    public void shouldUseDefaultWhenEmpty() {
        // When...
        String value = StringUtils.ifNullOrEmpty("", "def");
        // Then...
        assertThat(value).isEqualTo("def");
    }

    @Test
    public void shouldEncodeSpaceUsingPercentEncoding() throws UnsupportedEncodingException {
        // When...
        String value = StringUtils.encodeURIComponent("Hello !'()~ World", "UTF-8");
        // Then...
        assertThat(value).isEqualTo("Hello%20!'()~%20World");
    }

    @Test
    private void shouldBeEmpty() {
        assertThat(StringUtils.isEmpty(null)).isTrue();
        assertThat(StringUtils.isEmpty("")).isTrue();

        assertThat(StringUtils.isEmpty("blah")).isFalse();
        assertThat(StringUtils.isEmpty("    ")).isFalse();
    }

    @Test
    public void shouldBeBlank() {
        assertThat(StringUtils.isBlank(null)).isTrue();
        assertThat(StringUtils.isBlank("")).isTrue();
        assertThat(StringUtils.isBlank("             ")).isTrue();

        assertThat(StringUtils.isBlank("abc")).isFalse();
        assertThat(StringUtils.isBlank("   abc   ")).isFalse();
    }

    @Test
    public void shouldNotBeEmpty() {
        assertThat(StringUtils.isNotEmpty("abc")).isTrue();
        assertThat(StringUtils.isNotEmpty("   ")).isTrue();

        assertThat(StringUtils.isNotEmpty(null)).isFalse();
        assertThat(StringUtils.isNotEmpty("")).isFalse();
    }

    @Test
    public void shouldNotBeBlank() {
        assertThat(StringUtils.isNotBlank("abc")).isTrue();
        assertThat(StringUtils.isNotBlank("     abc     ")).isTrue();

        assertThat(StringUtils.isNotBlank(null)).isFalse();
        assertThat(StringUtils.isNotBlank("")).isFalse();
        assertThat(StringUtils.isNotBlank("             ")).isFalse();
    }

    @Test
    public void shouldReturnSingleValue() {
        assertThat(StringUtils.getParameter("a=1,b=4,c=5", ",", "b").size() == 1);
    }

    @Test
    public void shouldReturnMulitpleValues() {
        assertThat(StringUtils.getParameter("a=1,b=4,c=5,b=6", ",", "b").size() == 2);
    }

    @Test
    public void shouldReturnAnEmptyList() {
        assertThat(StringUtils.getParameter("a=1,b=,c=5", ",", "b").size() == 0);
        assertThat(StringUtils.getParameter("a=1,b,c=5", ",", "b").size() == 0);
    }

}

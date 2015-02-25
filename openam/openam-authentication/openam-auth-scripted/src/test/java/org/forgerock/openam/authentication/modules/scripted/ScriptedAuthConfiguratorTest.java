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

package org.forgerock.openam.authentication.modules.scripted;

import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.fest.assertions.Assertions.assertThat;

public class ScriptedAuthConfiguratorTest {

    @Test
    public void shouldCompileEmptyPatternList() {
        // Given
        Set<String> patterns = Collections.emptySet();

        // When
        List<Pattern> result = ScriptedAuthConfigurator.compilePatternList(patterns);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void shouldStripEmptyPatterns() {
        // Given
        Set<String> patterns = Collections.singleton("");

        // When
        List<Pattern> result = ScriptedAuthConfigurator.compilePatternList(patterns);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void shouldCompileStarToWildcard() {
        // Given
        Set<String> patterns = Collections.singleton("java.lang.*");

        // When
        List<Pattern> result = ScriptedAuthConfigurator.compilePatternList(patterns);

        // Then
        assertThat(result).hasSize(patterns.size());
        assertThat(result.get(0).matcher("java.lang.Foo").matches()).isTrue();
        assertThat(result.get(0).matcher("java.util.Foo").matches()).isFalse();
    }

    @Test
    public void shouldSupportMultipleWildcards() {
        // Given
        Set<String> patterns = Collections.singleton("java.*.Foo*");

        // When
        List<Pattern> result = ScriptedAuthConfigurator.compilePatternList(patterns);

        // Then
        assertThat(result.get(0).matcher("java.lang.FooBar").matches()).isTrue();
        assertThat(result.get(0).matcher("java.foo.FooBar").matches()).isTrue();
        assertThat(result.get(0).matcher("java.FooBar").matches()).isFalse();
        assertThat(result.get(0).matcher("java.foo.BarFoo").matches()).isFalse();
    }

    @Test
    public void shouldQuoteDotCharacters() {
        // Given
        Set<String> patterns = Collections.singleton("java.lang.*");

        // When
        List<Pattern> result = ScriptedAuthConfigurator.compilePatternList(patterns);

        // Then
        assertThat(result.get(0).matcher("javaXlangXFoo").matches()).isFalse();
        assertThat(result.get(0).matcher("java.lang.Foo").matches()).isTrue();
    }

    @Test
    public void shouldQuoteOtherSpecialChars() {
        // Given
        Set<String> patterns = Collections.singleton("foo[1-9]");

        // When
        List<Pattern> result = ScriptedAuthConfigurator.compilePatternList(patterns);

        // Then
        assertThat(result.get(0).matcher("foo6").matches()).isFalse();
        assertThat(result.get(0).matcher("foo[1-9]").matches()).isTrue();
    }
}

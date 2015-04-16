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

package org.forgerock.openam.scripting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.scripting.ScriptConstants.ENGINE_CONFIGURATION;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptContext;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptContext.AUTHENTICATION_SERVER_SIDE;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptContext.AUTHORIZATION_ENTITLEMENT_CONDITION;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class ScriptEngineConfiguratorTest {

    private ScriptEngineConfigurator engineConfigurator;
    private ScriptEngineConfigurator mockEngineConfigurator;
    private Logger logger;

    @BeforeMethod
    public void setup() {
        logger = mock(Logger.class);
        mockEngineConfigurator = mock(ScriptEngineConfigurator.class);
        engineConfigurator = new ScriptEngineConfigurator(logger);
    }

    @Test
    public void shouldCompileEmptyPatternList() {
        // Given
        Set<String> patterns = Collections.emptySet();

        // When
        List<Pattern> result = engineConfigurator.compilePatternList(patterns);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void shouldStripEmptyPatterns() {
        // Given
        Set<String> patterns = Collections.singleton("");

        // When
        List<Pattern> result = engineConfigurator.compilePatternList(patterns);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void shouldCompileStarToWildcard() {
        // Given
        Set<String> patterns = Collections.singleton("java.lang.*");

        // When
        List<Pattern> result = engineConfigurator.compilePatternList(patterns);

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
        List<Pattern> result = engineConfigurator.compilePatternList(patterns);

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
        List<Pattern> result = engineConfigurator.compilePatternList(patterns);

        // Then
        assertThat(result.get(0).matcher("javaXlangXFoo").matches()).isFalse();
        assertThat(result.get(0).matcher("java.lang.Foo").matches()).isTrue();
    }

    @Test
    public void shouldQuoteOtherSpecialChars() {
        // Given
        Set<String> patterns = Collections.singleton("foo[1-9]");

        // When
        List<Pattern> result = engineConfigurator.compilePatternList(patterns);

        // Then
        assertThat(result.get(0).matcher("foo6").matches()).isFalse();
        assertThat(result.get(0).matcher("foo[1-9]").matches()).isTrue();
    }

    @Test
    public void shouldUpdateEngineWithAuthenticationServerSideConfig() {
        // given
        ScriptContext context = AUTHENTICATION_SERVER_SIDE;
        String serviceComponent = "/" + context.name() + "/" + ENGINE_CONFIGURATION;
        doCallRealMethod().when(mockEngineConfigurator)
                .globalConfigChanged(anyString(), anyString(), anyString(), anyString(), anyInt());

        // when
        mockEngineConfigurator.globalConfigChanged(ScriptConstants.SERVICE_NAME, "", "", serviceComponent, 0);

        // then
        ArgumentCaptor<ScriptContext> resourceCaptor = ArgumentCaptor.forClass(ScriptContext.class);
        verify(mockEngineConfigurator, times(1)).updateConfig(resourceCaptor.capture());
        assertThat(resourceCaptor.getValue()).isEqualTo(context);

    }

    @Test
    public void shouldUpdateEngineWithAuthorisationEntitlementConditionConfig() {
        // given
        ScriptContext context = AUTHORIZATION_ENTITLEMENT_CONDITION;
        String serviceComponent = "/" + context.name() + "/" + ENGINE_CONFIGURATION;
        doCallRealMethod().when(mockEngineConfigurator)
                .globalConfigChanged(anyString(), anyString(), anyString(), anyString(), anyInt());

        // when
        mockEngineConfigurator.globalConfigChanged(ScriptConstants.SERVICE_NAME, "", "", serviceComponent, 0);

        // then
        ArgumentCaptor<ScriptContext> contextCaptor = ArgumentCaptor.forClass(ScriptContext.class);
        verify(mockEngineConfigurator, times(1)).updateConfig(contextCaptor.capture());
        assertThat(contextCaptor.getValue()).isEqualTo(context);

    }

    @Test
    public void shouldNotUpdateEngineWhenEngineConfigDidNotChange() {
        // given
        ScriptContext context = AUTHORIZATION_ENTITLEMENT_CONDITION;
        String serviceComponent = "/" + context.name() + "/SomeOtherConfig";
        doCallRealMethod().when(mockEngineConfigurator)
                .globalConfigChanged(anyString(), anyString(), anyString(), anyString(), anyInt());

        // when
        mockEngineConfigurator.globalConfigChanged(ScriptConstants.SERVICE_NAME, "", "", serviceComponent, 0);

        // then
        verify(mockEngineConfigurator, times(0)).updateConfig(any(ScriptContext.class));

    }

    @Test
    public void shouldLogErrorWhenContextNotFound() {
        // given
        String serviceComponent = "/NO_SUCH_CONTEXT/" + ENGINE_CONFIGURATION;

        // when
        engineConfigurator.globalConfigChanged(ScriptConstants.SERVICE_NAME, "", "", serviceComponent, 0);

        // then
        ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger, times(1)).error(stringCaptor.capture(), any(IllegalArgumentException.class));
        assertThat(stringCaptor.getValue()).isEqualTo("Script Context does not exist: NO_SUCH_CONTEXT");

    }
}

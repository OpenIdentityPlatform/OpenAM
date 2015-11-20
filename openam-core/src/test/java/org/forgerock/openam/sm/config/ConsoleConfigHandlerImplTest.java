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

package org.forgerock.openam.sm.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.core.guice.CoreGuiceModule.DNWrapper;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.inject.Injector;
import com.sun.identity.sm.ServiceListener;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Unit test for {@link ConsoleConfigHandlerImpl}.
 *
 * @since 13.0.0
 */
public final class ConsoleConfigHandlerImplTest {

    @Mock
    private SMSConfigProvider configProvider;
    @Mock
    private DNWrapper dnUtils;
    @Mock
    private Injector injector;

    @Captor
    private ArgumentCaptor<ServiceListener> serviceListenerCaptor;

    private Map<String, Set<String>> basicAttributes;
    private Map<String, Set<String>> localeAttributes;

    private ConsoleConfigHandler consoleConfigHandler;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        consoleConfigHandler = new ConsoleConfigHandlerImpl(configProvider, dnUtils, injector);

        basicAttributes = new HashMap<>();
        basicAttributes.put("messageKey", Collections.singleton("Some message"));
        basicAttributes.put("enabledKey", Collections.singleton("true"));
        basicAttributes.put("countKey", Collections.singleton("1234"));
        basicAttributes.put("timeKey", Collections.singleton("9876543210"));
        basicAttributes.put("currencyKey", Collections.singleton("123.456"));
        basicAttributes.put("valuesKey", Collections.singleton("Single value"));

        localeAttributes = new HashMap<>();
        localeAttributes.put("localeStringsKey", asSet("en|English message", "fr|French message"));
        localeAttributes.put("additionalLocaleStringsKey", Collections.<String>emptySet());
    }

    @Test
    public void beanSuccessfullyPopulatedFromDifferentSources() {
        // Given
        given(injector.getInstance(MockBeanBuilder.class)).willReturn(new MockBeanBuilder());
        given(configProvider.getAttributes("MockSource", "/")).willReturn(basicAttributes);
        given(configProvider.getAttributes("MockLocaleSource", "/")).willReturn(localeAttributes);
        given(injector.getInstance(DefaultConfigTransformer.class)).willReturn(new DefaultConfigTransformer());
        given(injector.getInstance(LocaleStringsTransformer.class)).willReturn(new LocaleStringsTransformer());

        // When
        MockBean bean = consoleConfigHandler.getConfig("/", MockBeanBuilder.class);

        // Then
        assertThat(bean.message).isEqualTo("Some message");
        assertThat(bean.enabled).isTrue();
        assertThat(bean.count).isEqualTo(1234);
        assertThat(bean.time).isEqualTo(9876543210L);
        assertThat(bean.currency).isEqualTo(123.456D);
        assertThat(bean.values).containsExactly("Single value");
        assertThat(bean.localeStrings).containsEntry("en", "English message");
        assertThat(bean.localeStrings).containsEntry("fr", "French message");
        assertThat(bean.additionalLocaleStrings).containsEntry("en_US", "American message");
        assertThat(bean.additionalLocaleStrings).containsEntry("de", "German message");
    }

    @Test
    public void listenerGetsNotifiedOnChange() {
        // Given
        ConsoleConfigListener listener = mock(ConsoleConfigListener.class);
        given(dnUtils.orgNameToRealmName("ou=abc,ou=def")).willReturn("/");

        // When
        consoleConfigHandler.registerListener(listener, MockListenerBeanBuilder.class);

        // Then
        verify(configProvider).registerListener(eq("MockListenerSource"), serviceListenerCaptor.capture());
        ServiceListener serviceListener = serviceListenerCaptor.getValue();
        serviceListener.organizationConfigChanged("MockListenerSource", "1.0", "ou=abc,ou=def", "", "", 0);

        verify(listener).configUpdate("MockListenerSource", "/");
    }

    private static final class MockBean {

        private final String message;
        private final boolean enabled;
        private final int count;
        private final long time;
        private final double currency;
        private final Set<String> values;
        private final Map<String, String> localeStrings;
        private final Map<String, String> additionalLocaleStrings;

        private MockBean(MockBeanBuilder builder) {
            message = builder.message;
            enabled = builder.enabled;
            count = builder.count;
            time = builder.time;
            currency = builder.currency;
            values = builder.values;
            localeStrings = builder.localeStrings;
            additionalLocaleStrings = builder.additionalLocaleStrings;
        }

    }

    @ConfigSource({"MockSource", "MockLocaleSource"})
    private static final class MockBeanBuilder implements ConsoleConfigBuilder<MockBean> {

        private String message;
        private boolean enabled;
        private int count;
        private long time;
        private double currency;
        private Set<String> values;
        private Map<String, String> localeStrings;
        private Map<String, String> additionalLocaleStrings;

        @ConfigAttribute("messageKey")
        public void setMessage(String message) {
            this.message = message;
        }

        @ConfigAttribute("enabledKey")
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @ConfigAttribute("countKey")
        public void setCount(int count) {
            this.count = count;
        }

        @ConfigAttribute("timeKey")
        public void setTime(long time) {
            this.time = time;
        }

        @ConfigAttribute("currencyKey")
        public void setCurrency(double currency) {
            this.currency = currency;
        }

        @ConfigAttribute("valuesKey")
        public void setValues(Set<String> values) {
            this.values = values;
        }

        @ConfigAttribute(value = "localeStringsKey", transformer = LocaleStringsTransformer.class)
        public void setLocaleStrings(Map<String, String> localeStrings) {
            this.localeStrings = localeStrings;
        }

        @ConfigAttribute(value = "additionalLocaleStringsKey", transformer = LocaleStringsTransformer.class,
                required = false, defaultValues = {"de|German message", "en_US|American message"})
        public void setAdditionalLocaleStrings(Map<String, String> additionalLocaleStrings) {
            this.additionalLocaleStrings = additionalLocaleStrings;
        }

        @Override
        public MockBean build(Map<String, Set<String>> attributes) {
            return new MockBean(this);
        }

    }

    @ConfigSource("MockListenerSource")
    private static final class MockListenerBeanBuilder implements ConsoleConfigBuilder<String> {

        @Override
        public String build(Map<String, Set<String>> attributes) {
            return "test";
        }

    }

    private static final class LocaleStringsTransformer implements ConfigTransformer<Map<String, String>> {

        @Override
        public Map<String, String> transform(Set<String> values, Class<?> parameterType) {
            Map<String, String> localeStrings = new HashMap<>();

            for (String value : values) {
                String[] parts = value.split("\\|");
                localeStrings.put(parts[0], parts[1]);
            }

            return localeStrings;
        }

    }

}
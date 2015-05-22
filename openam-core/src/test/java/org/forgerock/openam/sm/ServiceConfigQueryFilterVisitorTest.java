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
package org.forgerock.openam.sm;

import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.util.query.QueryFilter.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.identity.sm.ServiceConfig;
import org.forgerock.util.query.QueryFilter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ServiceConfigQueryFilterVisitorTest {

    private ServiceConfig serviceConfig;
    private ServiceConfigQueryFilterVisitor filterVisitor;

    @BeforeMethod
    public void setup() {
        serviceConfig = mock(ServiceConfig.class);
        Map<String, Set<String>> attributeMap = new HashMap<>();
        for (int i = 1; i < 5; i++) {
            attributeMap.put("param" + i, Collections.singleton("value" + i));
        }
        when(serviceConfig.getAttributesForRead()).thenReturn(attributeMap);

        filterVisitor = new ServiceConfigQueryFilterVisitor();
    }

    @Test
    public void shouldMatchEqualsSearch() {
        // given
        QueryFilter<String> queryFilter = equalTo("param1", "value1");

        // when
        boolean result = queryFilter.accept(filterVisitor, serviceConfig);

        // then
        assertThat(result).isTrue();
    }

    @Test
    public void shouldMatchContainsSearch() {
        // given
        QueryFilter<String> queryFilter = contains("param2", "2");

        // when
        boolean result = queryFilter.accept(filterVisitor, serviceConfig);

        // then
        assertThat(result).isTrue();
    }

    @Test
    public void shouldMatchStartsWithSearch() {
        // given
        QueryFilter<String> queryFilter = startsWith("param3", "val");

        // when
        boolean result = queryFilter.accept(filterVisitor, serviceConfig);

        // then
        assertThat(result).isTrue();
    }

    @Test
    public void shouldMatchAndSearch() {
        // given
        QueryFilter<String> queryFilter = and(
                equalTo("param1", "value1"),
                contains("param2", "2"),
                startsWith("param3", "val"));

        // when
        boolean result = queryFilter.accept(filterVisitor, serviceConfig);

        // then
        assertThat(result).isTrue();
    }

    @Test
    public void shouldMatchOrSearch() {
        // given
        QueryFilter<String> queryFilter = or(
                equalTo("param1", "value2"),
                contains("param2", "3"),
                startsWith("param3", "val"));

        // when
        boolean result = queryFilter.accept(filterVisitor, serviceConfig);

        // then
        assertThat(result).isTrue();
    }

    @Test
    public void shouldMatchAndOrComboSearch() {
        // given
        QueryFilter<String> queryFilter = and(
                or(equalTo("param1", "value1"), contains("param2", "3")),
                or(equalTo("param3", "1234"), contains("param4", "4")));

        // when
        boolean result = queryFilter.accept(filterVisitor, serviceConfig);

        // then
        assertThat(result).isTrue();
    }

    @Test
    public void shouldNotMatchEqualsSearch() {
        // given
        QueryFilter<String> queryFilter = equalTo("param1", "value2");

        // when
        boolean result = queryFilter.accept(filterVisitor, serviceConfig);

        // then
        assertThat(result).isFalse();
    }

    @Test
    public void shouldNotMatchContainsSearch() {
        // given
        QueryFilter<String> queryFilter = contains("param2", "4");

        // when
        boolean result = queryFilter.accept(filterVisitor, serviceConfig);

        // then
        assertThat(result).isFalse();
    }

    @Test
    public void shouldNotMatchStartsWithSearch() {
        // given
        QueryFilter<String> queryFilter = startsWith("param3", "test");

        // when
        boolean result = queryFilter.accept(filterVisitor, serviceConfig);

        // then
        assertThat(result).isFalse();
    }

    @Test
    public void shouldNotMatchAndSearch() {
        // given
        QueryFilter<String> queryFilter = and(
                equalTo("param1", "value1"),
                contains("param2", "4"),
                startsWith("param3", "val"));

        // when
        boolean result = queryFilter.accept(filterVisitor, serviceConfig);

        // then
        assertThat(result).isFalse();
    }

    @Test
    public void shouldNotMatchOrSearch() {
        // given
        QueryFilter<String> queryFilter = or(
                equalTo("param1", "value2"),
                contains("param2", "3"),
                startsWith("param3", "test"));

        // when
        boolean result = queryFilter.accept(filterVisitor, serviceConfig);

        // then
        assertThat(result).isFalse();
    }

    @Test
    public void shouldNotMatchAndOrComboSearch() {
        // given
        QueryFilter<String> queryFilter = and(
                or(equalTo("param1", "value1"), contains("param2", "3")),
                or(equalTo("param3", "1234"), contains("param4", "567")));

        // when
        boolean result = queryFilter.accept(filterVisitor, serviceConfig);

        // then
        assertThat(result).isFalse();
    }
}

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
package org.forgerock.openam.entitlement.service;

import static com.sun.identity.entitlement.Application.*;
import static org.forgerock.util.query.QueryFilter.*;
import static org.mockito.Mockito.*;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.forgerock.util.query.QueryFilter;
import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.sm.ServiceConfig;

/**
 * @since 13.1.0
 */
public class ApplicationQueryFilterVisitorTest {

    private static final String APPLICATION_NAME = "TestApplication";
    private static final String CREATED_BY = "TestUser";
    private static final String CREATION_DATE = "1456308148033";
    private static final String LAST_MODIFIED_BY = "AnotherTestUser";
    private static final String LAST_MODIFIED_DATE = "1456308148088";

    private ApplicationQueryFilterVisitor applicationQueryFilterVisitor;
    private ServiceConfig serviceConfig;
    private Map<String, Set<String>> appData;

    @BeforeMethod
    public void setUp() {
        Set<String> metaData = new HashSet<>();
        metaData.add(CREATED_BY_ATTRIBUTE + "=" + CREATED_BY);
        metaData.add(CREATION_DATE_ATTRIBUTE + "=" + CREATION_DATE);
        metaData.add(LAST_MODIFIED_BY_ATTRIBUTE + "=" + LAST_MODIFIED_BY);
        metaData.add(LAST_MODIFIED_DATE_ATTRIBUTE + "=" + LAST_MODIFIED_DATE);
        appData = new HashMap<>();
        appData.put("meta", metaData);

        applicationQueryFilterVisitor = new ApplicationQueryFilterVisitor(APPLICATION_NAME);
        serviceConfig = mock(ServiceConfig.class);
        when(serviceConfig.getAttributes()).thenReturn(appData);
    }

    @Test
    public void shouldModifyConfigData() {
        // given

        // when
        Map<String, Set<String>> configData = applicationQueryFilterVisitor.getConfigData(serviceConfig);

        // then
        assertThat(configData.get("name")).contains(APPLICATION_NAME);
        assertThat(configData.get("createdBy")).contains(CREATED_BY);
        assertThat(configData.get("creationDate")).contains(CREATION_DATE);
        assertThat(configData.get("lastModifiedBy")).contains(LAST_MODIFIED_BY);
        assertThat(configData.get("lastModifiedDate")).contains(LAST_MODIFIED_DATE);
    }

    @Test
    public void shouldFindApplicationName() {
        // given
        QueryFilter<String> queryFilter = equalTo("name" , APPLICATION_NAME);

        // when
        boolean result = queryFilter.accept(applicationQueryFilterVisitor, serviceConfig);

        // then
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void shouldFindCreatedBy() {
        // given
        QueryFilter<String> queryFilter = equalTo("createdBy" , CREATED_BY);

        // when
        boolean result = queryFilter.accept(applicationQueryFilterVisitor, serviceConfig);

        // then
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void shouldFindCreationDate() {
        // given
        QueryFilter<String> queryFilter = lessThanOrEqualTo("creationDate" , 1456308148033l);

        // when
        boolean result = queryFilter.accept(applicationQueryFilterVisitor, serviceConfig);

        // then
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void shouldFindLastModifiedBy() {
        // given
        QueryFilter<String> queryFilter = equalTo("lastModifiedBy" , LAST_MODIFIED_BY);

        // when
        boolean result = queryFilter.accept(applicationQueryFilterVisitor, serviceConfig);

        // then
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void shouldFindLastModifiedDate() {
        // given
        QueryFilter<String> queryFilter = greaterThanOrEqualTo("lastModifiedDate", 1456308148088l);

        // when
        boolean result = queryFilter.accept(applicationQueryFilterVisitor, serviceConfig);

        // then
        Assertions.assertThat(result).isTrue();
    }
}

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

package org.forgerock.openam.core.rest.sms;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.eq;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;

import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.forgerock.openam.sm.ServiceSchemaManagerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class SmsConsoleServiceNameFilterTest {

    SmsConsoleServiceNameFilter testNameFilter;

    Debug mockDebug;
    SmsConsoleServiceConfig mockServiceConfig;
    Set<String> removeServices;
    Set<SchemaType> supportedSchemaTypes;
    Set<String> authenticationServices;
    ServiceSchemaManagerFactory mockServiceSchemaManagerFactory;
    ServiceSchemaManager mockServiceSchemaManager;
    ServiceSchema mockServiceSchema;
    AttributeSchema mockAttributeSchema;

    Set<String> servicesToFilter;

    @BeforeTest
    public void theSetUp() throws SMSException, SSOException { //you need this

        mockDebug = mock(Debug.class);
        mockServiceConfig = mock(SmsConsoleServiceConfig.class);
        removeServices = new HashSet<>();
        supportedSchemaTypes = new HashSet<>();
        authenticationServices = new HashSet<>();
        mockServiceSchemaManagerFactory = mock(ServiceSchemaManagerFactory.class);
        mockServiceSchemaManager = mock(ServiceSchemaManager.class);
        mockServiceSchema = mock(ServiceSchema.class);
        mockAttributeSchema = mock(AttributeSchema.class);

        given(mockServiceSchemaManagerFactory.build(anyString())).willReturn(mockServiceSchemaManager);
        supportedSchemaTypes.add(SchemaType.DYNAMIC);
        given(mockServiceSchemaManager.getSchema(eq(SchemaType.DYNAMIC))).willReturn(mockServiceSchema);
        given(mockServiceSchemaManager.getSchemaTypes()).willReturn(supportedSchemaTypes);
        given(mockServiceSchema.getAttributeSchemas()).willReturn(Collections.singleton(mockAttributeSchema));
        given(mockAttributeSchema.getI18NKey()).willReturn("not_blank");
    }

    @BeforeMethod
    public void partTwo() {
        servicesToFilter = new HashSet<>();
        servicesToFilter.add("service1");
        servicesToFilter.add("service2");
        servicesToFilter.add("service3");
        servicesToFilter.add("service4");
        servicesToFilter.add("service5");

        authenticationServices = new HashSet<>();
        removeServices = new HashSet<>();
    }

    @Test
    public void testFilterRemovesAuthenticationServices() throws SMSException, SSOException, AMConfigurationException {
        //given
        authenticationServices.add("service3");
        authenticationServices.add("service4");
        authenticationServices.add("service5");

        given(mockServiceSchemaManager.getPropertiesViewBeanURL()).willReturn(null);

        testNameFilter = new SmsConsoleServiceNameFilter(mockDebug, mockServiceConfig, removeServices,
        supportedSchemaTypes, authenticationServices, mockServiceSchemaManagerFactory);

        //when
        testNameFilter.filter(servicesToFilter);

        //then
        assertThat(servicesToFilter).contains("service1", "service2");
    }

    @Test
    public void testFilterRemovesRemoveServices() throws SMSException, SSOException, AMConfigurationException {
        //given
        removeServices.add("service1");
        removeServices.add("service2");
        removeServices.add("service3");

        given(mockServiceSchemaManager.getPropertiesViewBeanURL()).willReturn(null);

        testNameFilter = new SmsConsoleServiceNameFilter(mockDebug, mockServiceConfig, removeServices,
                supportedSchemaTypes, authenticationServices, mockServiceSchemaManagerFactory);

        //when
        testNameFilter.filter(servicesToFilter);

        //then
        assertThat(servicesToFilter).contains("service4", "service5");
    }

    @Test
    public void testFilterRemovesUndisplayableServices() throws SMSException, SSOException, AMConfigurationException {
        //given

        given(mockServiceSchemaManager.getPropertiesViewBeanURL()).willReturn(null);
        given(mockAttributeSchema.getI18NKey()).willReturn(null); //overrides theSetUp

        testNameFilter = new SmsConsoleServiceNameFilter(mockDebug, mockServiceConfig, removeServices,
                supportedSchemaTypes, authenticationServices, mockServiceSchemaManagerFactory);

        //when
        testNameFilter.filter(servicesToFilter);

        //then
        assertThat(servicesToFilter).isEmpty();
    }

    @Test
    public void testMapNameRemovesInvisibleServices() throws SMSException, SSOException, AMConfigurationException {
        //given
        given(mockServiceConfig.isServiceVisible("service1")).willReturn(false);
        given(mockServiceConfig.isServiceVisible("service2")).willReturn(false);
        given(mockServiceConfig.isServiceVisible("service3")).willReturn(false);
        given(mockServiceConfig.isServiceVisible("service4")).willReturn(false);
        given(mockServiceConfig.isServiceVisible("service5")).willReturn(false);

        given(mockServiceSchemaManager.getPropertiesViewBeanURL()).willReturn("");
        given(mockServiceSchemaManager.getI18NFileName()).willReturn("filename");

        given(mockServiceSchema.getI18NKey()).willReturn("Service Name");

        testNameFilter = new SmsConsoleServiceNameFilter(mockDebug, mockServiceConfig, removeServices,
                supportedSchemaTypes, authenticationServices, mockServiceSchemaManagerFactory);

        //when
        Map<String, String> results = testNameFilter.mapNameToDisplayName(servicesToFilter);

        //then
        assertThat(results.keySet()).isEmpty();
    }

    @Test
    public void testMapNameRemovesNoResourceNameServices() throws SMSException, SSOException, AMConfigurationException {
        //given
        given(mockServiceConfig.isServiceVisible("service1")).willReturn(true);
        given(mockServiceConfig.isServiceVisible("service2")).willReturn(true);
        given(mockServiceConfig.isServiceVisible("service3")).willReturn(true);
        given(mockServiceConfig.isServiceVisible("service4")).willReturn(true);
        given(mockServiceConfig.isServiceVisible("service5")).willReturn(true);

        given(mockServiceSchemaManager.getPropertiesViewBeanURL()).willReturn("");
        given(mockServiceSchemaManager.getI18NFileName()).willReturn("filename");
        given(mockServiceSchemaManager.getResourceName()).willReturn(null);

        given(mockServiceSchema.getI18NKey()).willReturn("Service Name");

        testNameFilter = new SmsConsoleServiceNameFilter(mockDebug, mockServiceConfig, removeServices,
                supportedSchemaTypes, authenticationServices, mockServiceSchemaManagerFactory);

        //when
        Map<String, String> results = testNameFilter.mapNameToDisplayName(servicesToFilter);

        //then
        assertThat(results.keySet()).isEmpty();
    }

    @Test
    public void testMapNameRemovesIdNameEqualsDisplayNameServices() throws SMSException, SSOException, AMConfigurationException {
        //given
        given(mockServiceSchemaManager.getPropertiesViewBeanURL()).willReturn("");
        given(mockServiceConfig.isServiceVisible("service1")).willReturn(true);
        given(mockServiceConfig.isServiceVisible("service2")).willReturn(true);
        given(mockServiceConfig.isServiceVisible("service3")).willReturn(true);
        given(mockServiceConfig.isServiceVisible("service4")).willReturn(true);
        given(mockServiceConfig.isServiceVisible("service5")).willReturn(true);

        given(mockServiceSchemaManager.getI18NFileName()).willReturn("filename");
        when(mockServiceSchema.getI18NKey()).thenReturn("service1", "serviceName2", "serviceName3", "serviceName4",
                "serviceName5");
        when(mockServiceSchemaManager.getResourceName()).thenReturn("resourceName1", "resourceName2", "resourceName3",
                "resourceName4", "resourceName5");

        testNameFilter = new SmsConsoleServiceNameFilter(mockDebug, mockServiceConfig, removeServices,
                supportedSchemaTypes, authenticationServices, mockServiceSchemaManagerFactory);

        //when
        Map<String, String> results = testNameFilter.mapNameToDisplayName(servicesToFilter);

        //then
        assertThat(results.keySet()).contains("resourceName2", "resourceName3", "resourceName4", "resourceName5");
        assertThat(results.keySet()).doesNotContain("service1");
        assertThat(results.get("resourceName2")).isEqualTo("serviceName2");
        assertThat(results.get("resourceName3")).isEqualTo("serviceName3");
        assertThat(results.get("resourceName4")).isEqualTo("serviceName4");
        assertThat(results.get("resourceName5")).isEqualTo("serviceName5");
    }

}

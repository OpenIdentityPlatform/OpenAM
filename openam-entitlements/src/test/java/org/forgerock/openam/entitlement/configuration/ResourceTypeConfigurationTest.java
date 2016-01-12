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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.entitlement.configuration;

import com.iplanet.sso.SSOException;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import org.forgerock.guava.common.collect.Sets;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;

import java.util.*;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

public class ResourceTypeConfigurationTest {

    private ResourceTypeServiceConfig resourceTypeServiceConfig;

    private ResourceTypeConfigurationImpl impl;

    private ServiceConfig serviceConfig = mock(ServiceConfig.class);

    private ServiceConfig subServiceConfig = mock(ServiceConfig.class);

    private ServiceConfig subSubServiceConfig = mock(ServiceConfig.class);

    @BeforeMethod
    public void setup() throws SMSException, SSOException {
        resourceTypeServiceConfig = mock(ResourceTypeServiceConfig.class);
        impl = new ResourceTypeConfigurationImpl(null, resourceTypeServiceConfig);
        when(serviceConfig.getSubConfig(anyString())).thenReturn(subServiceConfig);
    }

    @Test
    public void containsNameWorks() throws SMSException, SSOException, EntitlementException {
        setupServiceConfigMock(Sets.newHashSet("SameName"));
        assertTrue(impl.containsName(null, "realm", "SameName"));
    }

    @Test
    public void containsNameFails() throws SMSException, SSOException, EntitlementException {
        setupServiceConfigMock(Sets.newHashSet("DifferentName"));
        assertFalse(impl.containsName(null, "realm", "SameName"));
    }

    @Test
    public void containsNameIsCaseInsensitive() throws SMSException, SSOException, EntitlementException {
        setupServiceConfigMock(Sets.newHashSet("case"));
        assertTrue(impl.containsName(null, "realm", "CASE"));
    }

    private void setupServiceConfigMock(Set<String> names) throws SMSException, SSOException, EntitlementException {
        when(subServiceConfig.getSubConfigNames()).thenReturn(names);
        when(subServiceConfig.getSubConfig(anyString())).thenReturn(subSubServiceConfig);
        Map<String, Set<String>> attributes = new HashMap<>();
        attributes.put("name", names);
        when(subSubServiceConfig.getAttributes()).thenReturn(attributes);
        when(resourceTypeServiceConfig.getOrgConfig(any(Subject.class), anyString())).thenReturn(serviceConfig);
    }

}
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

package org.forgerock.openam.authentication.service;

import static org.mockito.Mockito.*;

import java.lang.reflect.Constructor;
import java.security.Principal;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginException;

import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sun.identity.authentication.spi.AMLoginModule;

public class JAASModuleDetectorTest {

    private static final String CONFIG_NAME = "config_name";
    private static final String MOCK_JAAS_NAME = MockJAASModule.class.getName();
    private static final String MOCK_NON_JAAS_MODULE_NAME = MockNonJAASModule.class.getName();

    private Configuration configuration;

    private JAASModuleDetector toTest;

    @BeforeMethod
    public void setUpDetector() throws Exception {
        Constructor<JAASModuleDetector> constructor = JAASModuleDetector.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        toTest = constructor.newInstance();
    }

    public void buildTestClasses(final AppConfigurationEntry[] entries) {
        configuration = mock(Configuration.class);
        when(configuration.getAppConfigurationEntry(CONFIG_NAME)).thenReturn(entries);

        Configuration.setConfiguration(configuration); // Override configuration set by AMLoginContext

    }

    @Test(dataProvider = "jaasDetectionDataProvider")
    public void testDetectJAASModules(AppConfigurationEntry[] entries, Boolean shouldBePureJaas) throws Exception {
        buildTestClasses(entries);

        boolean pureJAASModulePresent = toTest.isPureJAASModulePresent(CONFIG_NAME, configuration);

        Assertions.assertThat(pureJAASModulePresent).isEqualTo(shouldBePureJaas);
    }

    @DataProvider
    public Object[][] jaasDetectionDataProvider() {
        return new Object[][] {
                new Object[] { getAppConfigurationEntries(false), Boolean.FALSE },
                new Object[] { getAppConfigurationEntries(true), Boolean.TRUE },
                new Object[] { getAppConfigurationEntries(false, false), Boolean.FALSE },
                new Object[] { getAppConfigurationEntries(false, true), Boolean.TRUE },
                new Object[] { getAppConfigurationEntries(true, true), Boolean.TRUE }
        };
    }

    private AppConfigurationEntry[] getAppConfigurationEntries(boolean... entryJaasTypes) {
        AppConfigurationEntry[] appConfigurationEntries = new AppConfigurationEntry[entryJaasTypes.length];
        for (int i = 0; i < entryJaasTypes.length; i++) {
            appConfigurationEntries[i] = makeMockAppConfigEntry(entryJaasTypes[i]);
        }
        return appConfigurationEntries;
    }

    private AppConfigurationEntry makeMockAppConfigEntry(boolean isJAAS) {
        AppConfigurationEntry mockEntry = mock(AppConfigurationEntry.class);
        when(mockEntry.getLoginModuleName()).thenReturn(isJAAS ? MOCK_JAAS_NAME : MOCK_NON_JAAS_MODULE_NAME);
        return mockEntry;
    }

    public static class MockJAASModule {}

    public static class MockNonJAASModule extends AMLoginModule {
        @Override
        public void init(Subject subject, Map sharedState, Map options) {
            // do nothing
        }

        @Override
        public int process(Callback[] callbacks, int state) throws LoginException {
            return 0; // do nothing
        }

        @Override
        public Principal getPrincipal() {
            return null; // do nothing
        }
    }
}

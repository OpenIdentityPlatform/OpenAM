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

package org.forgerock.openam.upgrade.steps;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.forgerock.openam.upgrade.UpgradeUtils.ATTR_AUTH_POST_CLASS;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.security.PrivilegedAction;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.upgrade.UpgradeException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class PostAuthenticationPluginUpgradeStepTest {

    private UpgradeStep upgradeStep;

    @Mock
    private PrivilegedAction<SSOToken> adminTokenAction;
    @Mock
    private ConnectionFactory connectionFactory;
    private Set<String> realmNames;
    @Mock
    private ServiceConfig authSettingsServiceConfig;
    @Mock
    private ServiceConfig authChainsParentServiceConfig;
    @Mock
    private ServiceConfig authChainsServiceConfig;
    @Mock
    private ServiceConfig authChainServiceConfig;

    @BeforeMethod
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        realmNames = Collections.singleton("/");

        upgradeStep = new PostAuthenticationPluginUpgradeStep(adminTokenAction, connectionFactory) {

            @Override
            protected Set<String> getRealmNames() throws UpgradeException {
                return realmNames;
            }

            @Override
            ServiceConfig getAuthSettingsServiceConfig(String realm) throws SSOException, SMSException {
                return authSettingsServiceConfig;
            }

            @Override
            ServiceConfig getAuthChainServiceConfig(String realm) throws SSOException, SMSException {
                return authChainsParentServiceConfig;
            }
        };

        given(authChainsParentServiceConfig.getSubConfig("Configurations")).willReturn(authChainsServiceConfig);
    }

    @Test
    public void shouldNotUpdateAnyPapClassesIfNoneAreSet() throws Exception {

        //Given
        given(authSettingsServiceConfig.getAttributesWithoutDefaults()).willReturn(Collections.emptyMap());
        given(authChainsServiceConfig.getSubConfigNames()).willReturn(Collections.<String>emptySet());

        //When
        upgradeStep.initialize();

        //Then
        assertThat(upgradeStep.isApplicable()).isFalse();
    }

    @Test
    public void shouldNotUpdateAnyPapClassesIfNoneRequireUpdating() throws Exception {

        //Given
        given(authSettingsServiceConfig.getAttributesWithoutDefaults()).willReturn(Collections.singletonMap(ATTR_AUTH_POST_CLASS, Collections.singleton("SOME_PAP_CLASS")));
        given(authChainsServiceConfig.getSubConfigNames()).willReturn(Collections.<String>emptySet());

        //When
        upgradeStep.initialize();

        //Then
        assertThat(upgradeStep.isApplicable()).isFalse();
    }

    @DataProvider
    private Object[][] papClassesRequiringUpdate() {
        return new Object[][]{
            {"org.forgerock.openam.authentication.modules.adaptive.Adaptive",
                    "org.forgerock.openam.authentication.modules.adaptive.AdaptivePostAuthenticationPlugin"},
            {"org.forgerock.openam.authentication.modules.persistentcookie.PersistentCookieAuthModule",
                    "org.forgerock.openam.authentication.modules.persistentcookie.PersistentCookieAuthModulePostAuthenticationPlugin"}
        };
    }

    @Test(dataProvider = "papClassesRequiringUpdate")
    public void shouldUpdateAuthSettingPapClasses(String originalPapClass, String newPapClass) throws Exception {

        //Given
        given(authSettingsServiceConfig.getAttributesWithoutDefaults()).willReturn(Collections.singletonMap(ATTR_AUTH_POST_CLASS, Collections.singleton(originalPapClass)));
        given(authChainsServiceConfig.getSubConfigNames()).willReturn(Collections.<String>emptySet());

        //When
        upgradeStep.initialize();
        upgradeStep.perform();

        //Then
        ArgumentCaptor<Map> attributesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(authSettingsServiceConfig).setAttributes(attributesCaptor.capture());
        assertThat(attributesCaptor.getValue()).containsExactly(new AbstractMap.SimpleEntry<>(ATTR_AUTH_POST_CLASS, Collections.singleton(newPapClass)));
    }

    @Test(dataProvider = "papClassesRequiringUpdate")
    public void shouldUpdateAuthChainPapClasses(String originalPapClass, String newPapClass) throws Exception {

        //Given
        given(authSettingsServiceConfig.getAttributesWithoutDefaults()).willReturn(Collections.emptyMap());
        given(authChainsServiceConfig.getSubConfigNames()).willReturn(Collections.singleton("AUTH_CHAIN_NAME"));
        given(authChainsServiceConfig.getSubConfig("AUTH_CHAIN_NAME")).willReturn(authChainServiceConfig);
        given(authChainServiceConfig.getAttributesWithoutDefaults()).willReturn(Collections.singletonMap(ATTR_AUTH_POST_CLASS, Collections.singleton(originalPapClass)));

        //When
        upgradeStep.initialize();
        upgradeStep.perform();

        //Then
        ArgumentCaptor<Map> attributesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(authChainServiceConfig).setAttributes(attributesCaptor.capture());
        assertThat(attributesCaptor.getValue()).containsExactly(new AbstractMap.SimpleEntry<>(ATTR_AUTH_POST_CLASS, Collections.singleton(newPapClass)));
    }

    @Test(dataProvider = "papClassesRequiringUpdate")
    public void shouldUpdateOnlySpecificAuthChainPapClasses(String originalPapClass, String newPapClass) throws Exception {

        //Given
        given(authSettingsServiceConfig.getAttributesWithoutDefaults()).willReturn(Collections.emptyMap());
        given(authChainsServiceConfig.getSubConfigNames()).willReturn(Collections.singleton("AUTH_CHAIN_NAME"));
        given(authChainsServiceConfig.getSubConfig("AUTH_CHAIN_NAME")).willReturn(authChainServiceConfig);
        Set<String> papClasses = new HashSet<>();
        papClasses.add("OTHER_PAP_CLASS");
        papClasses.add(originalPapClass);
        papClasses.add("SOME_OTHER_PAP_CLASS");
        given(authChainServiceConfig.getAttributesWithoutDefaults()).willReturn(Collections.singletonMap(ATTR_AUTH_POST_CLASS, papClasses));

        //When
        upgradeStep.initialize();
        upgradeStep.perform();

        //Then
        Set<String> updatedPapClasses = new HashSet<>();
        updatedPapClasses.add("OTHER_PAP_CLASS");
        updatedPapClasses.add(newPapClass);
        updatedPapClasses.add("SOME_OTHER_PAP_CLASS");
        ArgumentCaptor<Map> attributesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(authChainServiceConfig).setAttributes(attributesCaptor.capture());
        assertThat(attributesCaptor.getValue()).containsExactly(new AbstractMap.SimpleEntry<>(ATTR_AUTH_POST_CLASS, updatedPapClasses));
    }
}

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
 * Copyright 2026 3A Systems, LLC.
 */

package com.sun.identity.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.Properties;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.setup.AMSetupServlet;
import com.sun.identity.shared.Constants;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.core.realms.RealmDnsAliases;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@PrepareForTest({ SystemProperties.class, InjectorHolder.class, AMSetupServlet.class })
@PowerMockIgnore({ "jdk.internal.reflect.*", "jakarta.servlet.*" })
public class FqdnValidatorTest extends PowerMockTestCase {

    private RealmDnsAliases realmDnsAliases;

    @BeforeMethod
    public void setup() {
        PowerMockito.mockStatic(SystemProperties.class);
        given(SystemProperties.get(Constants.AM_SERVER_HOST)).willReturn("openam.example.com");
        Properties properties = new Properties();
        properties.setProperty(Constants.AM_FQDN_MAP + "[openam]", "openam.example.com");
        properties.setProperty(Constants.AM_FQDN_MAP + "[lb.example.com]", "lb.example.com");
        given(SystemProperties.getAll()).willReturn(properties);
        for (String key : properties.stringPropertyNames()) {
            given(SystemProperties.get(key)).willReturn(properties.getProperty(key));
        }

        realmDnsAliases = mock(RealmDnsAliases.class);
        PowerMockito.mockStatic(InjectorHolder.class);
        given(InjectorHolder.getInstance(RealmDnsAliases.class)).willReturn(realmDnsAliases);

        PowerMockito.mockStatic(AMSetupServlet.class);
        given(AMSetupServlet.isCurrentConfigurationValid()).willReturn(true);

        FqdnValidator.getInstance().initialize();
    }

    @Test
    public void shouldAcceptRealmDnsAliasMissingFromFqdnMap() {
        given(realmDnsAliases.contains("acme.example.com")).willReturn(true);

        assertThat(FqdnValidator.getInstance().isHostnameValid("acme.example.com")).isTrue();
    }

    @Test
    public void shouldNotRedirectRealmDnsAliasMissingFromFqdnMap() {
        given(realmDnsAliases.contains("acme.example.com")).willReturn(true);

        assertThat(FqdnValidator.getInstance().getFullyQualifiedHostName("acme.example.com")).isNull();
    }

    @Test
    public void shouldRedirectFqdnMapKeyThatIsAlsoARealmDnsAlias() {
        given(realmDnsAliases.contains("openam")).willReturn(true);

        assertThat(FqdnValidator.getInstance().getFullyQualifiedHostName("openam")).isEqualTo("openam.example.com");
    }

    @Test
    public void shouldRedirectHostThatIsNeitherInFqdnMapNorARealmDnsAlias() {
        assertThat(FqdnValidator.getInstance().isHostnameValid("attacker.example.org")).isFalse();
        assertThat(FqdnValidator.getInstance().getFullyQualifiedHostName("attacker.example.org"))
                .isEqualTo("openam.example.com");
    }

    @Test
    public void shouldNotReadRealmDnsAliasesForHostInFqdnMap() {
        assertThat(FqdnValidator.getInstance().isHostnameValid("lb.example.com")).isTrue();
        assertThat(FqdnValidator.getInstance().isHostnameValid("openam.example.com")).isTrue();
        assertThat(FqdnValidator.getInstance().isHostnameValid("openam")).isFalse();

        verifyZeroInteractions(realmDnsAliases);
    }

    @Test
    public void shouldNotReadRealmDnsAliasesBeforeOpenAMIsConfigured() {
        given(AMSetupServlet.isCurrentConfigurationValid()).willReturn(false);
        given(realmDnsAliases.contains("acme.example.com")).willReturn(true);

        assertThat(FqdnValidator.getInstance().isHostnameValid("acme.example.com")).isFalse();
        verifyZeroInteractions(realmDnsAliases);
    }

    @Test
    public void shouldRejectNullHost() {
        assertThat(FqdnValidator.getInstance().isHostnameValid(null)).isFalse();
        verifyZeroInteractions(realmDnsAliases);
    }
}

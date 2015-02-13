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
 * Copyright 2013-2015 ForgeRock AS.
 */
package org.forgerock.openam.sm.datalayer.impl.ldap;

import static org.fest.assertions.Assertions.*;
import static org.forgerock.openam.utils.CollectionUtils.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;

import java.util.Set;

import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.impl.CTSDataLayerConfiguration;
import org.forgerock.openam.ldap.LDAPURL;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.naming.WebtopNaming;
import com.sun.identity.shared.debug.Debug;

@PrepareForTest({ SystemProperties.class, WebtopNaming.class })
public class ExternalLdapConfigTest extends PowerMockTestCase {

    private Debug debug;
    private LdapDataLayerConfiguration dataLayerConfiguration;

    @BeforeMethod
    public void setup() {
        this.debug = mock(Debug.class);
        this.dataLayerConfiguration = spy(new CTSDataLayerConfiguration("ou=root-dn"));
    }

    @Test
    public void shouldUseSystemPropertiesWrapperForNotifyChanges() throws Exception {
        // Given
        PowerMockito.mockStatic(SystemProperties.class);
        ExternalLdapConfig config = new ExternalLdapConfig(debug);
        // When
        config.update(dataLayerConfiguration);
        // Then
        PowerMockito.verifyStatic(times(4));
        SystemProperties.get(anyString());

        PowerMockito.verifyStatic();
        SystemProperties.getAsBoolean(anyString(), anyBoolean());

        PowerMockito.verifyStatic();
        SystemProperties.getAsInt(anyString(), eq(-1));
    }

    @Test
    public void shouldIndicateHasChanged() {
        PowerMockito.mockStatic(SystemProperties.class);
        given(SystemProperties.get(eq(CoreTokenConstants.CTS_STORE_HOSTNAME))).willReturn("badger");

        ExternalLdapConfig config = new ExternalLdapConfig(debug);
        // When
        config.update(dataLayerConfiguration);
        // Then
        assertThat(config.hasChanged()).isTrue();
    }

    @Test
    public void shouldBeNullForNullPassword() {
        PowerMockito.mockStatic(SystemProperties.class);
        given(SystemProperties.get(eq(CoreTokenConstants.CTS_STORE_PASSWORD))).willReturn(null);

        ExternalLdapConfig config = new ExternalLdapConfig(debug);
        config.update(dataLayerConfiguration);

        // When
        char[] result = config.getBindPassword();
        // Then
        assertThat(result).isNull();
    }

    @Test
    public void shouldPrioritizeServerList() throws Exception {
        // Given
        PowerMockito.mockStatic(SystemProperties.class);
        given(SystemProperties.get(eq(CoreTokenConstants.CTS_STORE_HOSTNAME)))
                .willReturn("test1.com:389|03,test2.com|02,test3.com|01");
        given(SystemProperties.getAsBoolean(CoreTokenConstants.CTS_STORE_SSL_ENABLED, false)).willReturn(true);
        PowerMockito.mockStatic(WebtopNaming.class);
        given(WebtopNaming.getAMServerID()).willReturn("01");
        given(WebtopNaming.getSiteID("01")).willReturn("02");

        ExternalLdapConfig config = new ExternalLdapConfig(debug);
        config.update(dataLayerConfiguration);

        //When
        Set<LDAPURL> urls = config.getLDAPURLs();

        //Then
        assertThat(urls).isEqualTo(asOrderedSet(valueOf("test3.com"), valueOf("test1.com"), valueOf("test2.com")));
    }

    private LDAPURL valueOf(String host) {
        return LDAPURL.valueOf(host, 389, true);
    }
}

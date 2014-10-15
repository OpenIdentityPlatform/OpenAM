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
 * Copyright 2013-2014 ForgeRock AS.
 */
package org.forgerock.openam.cts;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.naming.WebtopNaming;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.ldap.LDAPURL;
import org.forgerock.openam.sm.ExternalCTSConfig;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.openam.utils.CollectionUtils.asOrderedSet;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

@PrepareForTest({ SystemProperties.class, WebtopNaming.class })
public class ExternalCTSConfigTest extends PowerMockTestCase {

    private Debug debug;

    @BeforeMethod
    public void setup() {
        this.debug = mock(Debug.class);
    }

    @Test
    public void shouldUseSystemPropertiesWrapperForNotifyChanges() throws Exception {
        // Given
        PowerMockito.mockStatic(SystemProperties.class);
        ExternalCTSConfig config = new ExternalCTSConfig(debug);
        // When
        config.update();
        // Then
        PowerMockito.verifyStatic(times(5));
        SystemProperties.get(anyString());

        PowerMockito.verifyStatic();
        SystemProperties.getAsBoolean(anyString(), anyBoolean());
    }

    @Test
    public void shouldIndicateHasChanged() {
        PowerMockito.mockStatic(SystemProperties.class);
        given(SystemProperties.get(eq(CoreTokenConstants.CTS_STORE_HOSTNAME))).willReturn("badger");

        ExternalCTSConfig config = new ExternalCTSConfig(debug);
        // When
        config.update();
        // Then
        assertThat(config.hasChanged()).isTrue();
    }

    @Test
    public void shouldBeNegetiveHeartbeatForNullInput() {
        PowerMockito.mockStatic(SystemProperties.class);
        given(SystemProperties.get(eq(Constants.LDAP_HEARTBEAT))).willReturn(null);

        ExternalCTSConfig config = new ExternalCTSConfig(debug);
        // When
        config.update();
        // Then
        assertThat(config.getLdapHeartbeat()).isEqualTo(-1);
    }

    @Test
    public void shouldBeNegetiveHeartbeatForEmptyInput() {
        PowerMockito.mockStatic(SystemProperties.class);
        given(SystemProperties.get(eq(Constants.LDAP_HEARTBEAT))).willReturn("");

        ExternalCTSConfig config = new ExternalCTSConfig(debug);
        // When
        config.update();
        // Then
        assertThat(config.getLdapHeartbeat()).isEqualTo(ExternalCTSConfig.INVALID);
    }

    @Test
    public void shouldBeNegetiveHeartbeatForNonNumberInput() {
        PowerMockito.mockStatic(SystemProperties.class);
        given(SystemProperties.get(eq(Constants.LDAP_HEARTBEAT))).willReturn("String");

        ExternalCTSConfig config = new ExternalCTSConfig(debug);
        // When
        config.update();
        // Then
        assertThat(config.getLdapHeartbeat()).isEqualTo(ExternalCTSConfig.INVALID);
    }

    @Test
    public void shouldBeNullForNullPassword() {
        PowerMockito.mockStatic(SystemProperties.class);
        given(SystemProperties.get(eq(CoreTokenConstants.CTS_STORE_PASSWORD))).willReturn(null);

        ExternalCTSConfig config = new ExternalCTSConfig(debug);
        config.update();

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

        ExternalCTSConfig config = new ExternalCTSConfig(debug);
        config.update();

        //When
        Set<LDAPURL> urls = config.getLDAPURLs();

        //Then
        assertThat(urls).isEqualTo(asOrderedSet(valueOf("test3.com"), valueOf("test1.com"), valueOf("test2.com")));
    }

    private LDAPURL valueOf(String host) {
        return LDAPURL.valueOf(host, 389, true);
    }
}

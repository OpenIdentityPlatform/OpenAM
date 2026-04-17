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
 * Portions copyright 2026 3A Systems, LLC.
 */
package com.iplanet.dpro.session.service;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.naming.WebtopNaming;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.session.SessionServiceURLService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

@PrepareForTest({ SystemProperties.class, WebtopNaming.class })
@PowerMockIgnore("jdk.internal.reflect.*")
public class SessionServerConfigTest extends PowerMockTestCase {

    @Test
    public void localServerIsPrimaryServerIfNoSiteSetup() throws Exception {
        // Given
        PowerMockito.mockStatic(SystemProperties.class);
        given(SystemProperties.get(Constants.AM_SERVER_PROTOCOL)).willReturn("http");
        given(SystemProperties.get(Constants.AM_SERVER_HOST)).willReturn("openam.example.com");
        given(SystemProperties.get(Constants.AM_SERVER_PORT)).willReturn("8080");
        given(SystemProperties.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR)).willReturn("/openam");

        String primary = "01";
        PowerMockito.mockStatic(WebtopNaming.class);
        given(WebtopNaming.getServerID("http", "openam.example.com", "8080", "/openam")).willReturn(primary);
        given(WebtopNaming.getAMServerID()).willReturn("01");
        given(WebtopNaming.getLocalServer()).willReturn("http://openam.example.com:8080/openam");

        // When
        SessionServerConfig config = new SessionServerConfig(mock(Debug.class), mock(SessionServiceURLService.class));

        // Then
        assertThat(config.getPrimaryServerID()).isEqualTo(primary);
    }

    @Test
    public void localServerAndPrimaryServerDifferIfSiteSetup() throws Exception {
        // Given
        PowerMockito.mockStatic(SystemProperties.class);
        given(SystemProperties.get(Constants.AM_SERVER_PROTOCOL)).willReturn("http");
        given(SystemProperties.get(Constants.AM_SERVER_HOST)).willReturn("openam.example.com");
        given(SystemProperties.get(Constants.AM_SERVER_PORT)).willReturn("8080");
        given(SystemProperties.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR)).willReturn("/openam");

        String primary = "01";
        PowerMockito.mockStatic(WebtopNaming.class);
        given(WebtopNaming.getServerID("http", "openam.example.com", "8080", "/openam")).willReturn(primary);
        given(WebtopNaming.isSiteEnabled(anyString())).willReturn(true); // enable site
        given(WebtopNaming.getSiteID(anyString())).willReturn("02");
        given(WebtopNaming.getAMServerID()).willReturn("01");
        given(WebtopNaming.getLocalServer()).willReturn("http://openam.example.com:8080/openam");

        // When
        SessionServerConfig config = new SessionServerConfig(mock(Debug.class), mock(SessionServiceURLService.class));

        // Then
        assertThat(config.getPrimaryServerID()).isNotEqualTo(primary);
    }

}

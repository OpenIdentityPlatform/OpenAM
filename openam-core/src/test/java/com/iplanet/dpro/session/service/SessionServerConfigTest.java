package com.iplanet.dpro.session.service;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.naming.WebtopNaming;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.session.SessionServiceURLService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;

@PrepareForTest({ SystemProperties.class, WebtopNaming.class })
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

        // When
        SessionServerConfig config = new SessionServerConfig(mock(Debug.class), mock(SessionServiceURLService.class));

        // Then
        assertThat(config.getPrimaryServerID()).isNotEqualTo(primary);
    }

}

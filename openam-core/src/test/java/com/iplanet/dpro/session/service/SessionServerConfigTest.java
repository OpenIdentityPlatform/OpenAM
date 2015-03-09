package com.iplanet.dpro.session.service;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.naming.WebtopNaming;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.session.SessionMeta;
import org.forgerock.openam.session.SessionServiceURLService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.Collections;

@PrepareForTest({ SystemProperties.class, WebtopNaming.class })
public class SessionServerConfigTest extends PowerMockTestCase {

    @Test
    public void localServerIsPrimaryServerIfNoSiteSetup() throws Exception {

        // Given
        SessionServiceURLService sessionServiceURLService = mock(SessionServiceURLService.class);

        PowerMockito.mockStatic(SystemProperties.class);
        given(SystemProperties.get(Constants.AM_SERVER_PROTOCOL)).willReturn("http");
        given(SystemProperties.get(Constants.AM_SERVER_HOST)).willReturn("openam.example.com");
        given(SystemProperties.get(Constants.AM_SERVER_PORT)).willReturn("8080");
        given(SystemProperties.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR)).willReturn("/openam");

        PowerMockito.mockStatic(WebtopNaming.class);
        given(WebtopNaming.getServerID("http", "openam.example.com", "8080", "/openam")).willReturn("01");
        given(WebtopNaming.isSiteEnabled("http", "openam.example.com", "8080", "/openam")).willReturn(false);
        given(WebtopNaming.getServerFromID("01")).willReturn("http://openam.example.com:8080/openam");

        PowerMockito.mockStatic(SessionMeta.class);
        given(sessionServiceURLService.getSessionServiceURL("http", "openam.example.com", "8080", "/openam"))
                .willReturn(new URL("http://openam.example.com:8080/openam/sessionservice"));

        // When

        SessionServerConfig config = new SessionServerConfig(mock(Debug.class), sessionServiceURLService);

        // Then

        assertThat(config.getPrimaryServerID()).isEqualTo("01");
        assertThat(config.getPrimaryServerURL()).isEqualTo(new URL("http://openam.example.com:8080/openam"));

        assertThat(config.getLocalServerID()).isEqualTo("01");
        assertThat(config.getLocalServerURL().toString()).isEqualTo("http://openam.example.com:8080/openam");

        assertThat(config.isSiteEnabled()).isEqualTo(false);
        assertThat(config.getSecondarySiteIDs()).isEqualTo(Collections.emptySet());
        assertThat(config.getLocalServerSessionServiceURL())
                .isEqualTo(new URL("http://openam.example.com:8080/openam/sessionservice"));
    }

    @Test
    public void localServerAndPrimaryServerDifferIfSiteSetup() throws Exception {

        // Given
        SessionServiceURLService sessionServiceURLService = mock(SessionServiceURLService.class);

        PowerMockito.mockStatic(SystemProperties.class);
        given(SystemProperties.get(Constants.AM_SERVER_PROTOCOL)).willReturn("http");
        given(SystemProperties.get(Constants.AM_SERVER_HOST)).willReturn("openam2.example.com");
        given(SystemProperties.get(Constants.AM_SERVER_PORT)).willReturn("28080");
        given(SystemProperties.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR)).willReturn("/openam");

        PowerMockito.mockStatic(WebtopNaming.class);
        given(WebtopNaming.getServerID("http", "openam2.example.com", "28080", "/openam")).willReturn("02");
        given(WebtopNaming.isSiteEnabled("http", "openam2.example.com", "28080", "/openam")).willReturn(true);
        given(WebtopNaming.getSiteID("http", "openam2.example.com", "28080", "/openam")).willReturn("03");
        given(WebtopNaming.getSecondarySites("http", "openam2.example.com", "28080", "/openam")).willReturn(null);
        given(WebtopNaming.getServerFromID("03")).willReturn("https://openam.example.com:8080/am");

        PowerMockito.mockStatic(SessionMeta.class);
        given(sessionServiceURLService.getSessionServiceURL("http", "openam2.example.com", "28080", "/openam"))
                .willReturn(new URL("https://openam.example.com:8080/am/sessionservice"));

        // When

        SessionServerConfig sessionServerConfig = new SessionServerConfig(mock(Debug.class), sessionServiceURLService);

        // Then

        assertThat(sessionServerConfig.getPrimaryServerID()).isEqualTo("03");
        assertThat(sessionServerConfig.getPrimaryServerURL()).isEqualTo(new URL("https://openam.example.com:8080/am"));

        assertThat(sessionServerConfig.getLocalServerID()).isEqualTo("02");
        assertThat(sessionServerConfig.getLocalServerURL().toString()).isEqualTo("http://openam2.example.com:28080/openam");

        assertThat(sessionServerConfig.isSiteEnabled()).isEqualTo(true);
        assertThat(sessionServerConfig.getSecondarySiteIDs()).isEqualTo(Collections.emptySet()); // TODO: Why is this empty?
        assertThat(sessionServerConfig.getLocalServerSessionServiceURL())
                .isEqualTo(new URL("https://openam.example.com:8080/am/sessionservice"));
    }

}

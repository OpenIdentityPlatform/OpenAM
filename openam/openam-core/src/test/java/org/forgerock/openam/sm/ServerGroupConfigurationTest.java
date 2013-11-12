/**
 * Copyright 2013 ForgeRock AS.
 *
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
 */
package org.forgerock.openam.sm;

import com.iplanet.services.ldap.Server;
import com.iplanet.services.ldap.ServerGroup;
import com.iplanet.services.ldap.ServerInstance;
import java.util.Arrays;
import java.util.Set;
import static org.fest.assertions.Assertions.*;
import org.forgerock.openam.ldap.LDAPURL;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Mockito.verify;
import org.testng.annotations.Test;

/**
 * @author robert.wapshott@forgerock.com
 */
public class ServerGroupConfigurationTest {

    @Test
    public void shouldReturnCorrectLDAPURLforSimpleConnections() {
        // Given
        String hostName = "localhost";
        int port = 389;
        Server one = mock(Server.class);
        given(one.getServerName()).willReturn(hostName);
        given(one.getPort()).willReturn(port);
        given(one.getConnectionType()).willReturn(Server.Type.CONN_SIMPLE);

        ServerInstance mockInstance = mock(ServerInstance.class);
        ServerGroup mockGroup = mock(ServerGroup.class);
        given(mockGroup.getServersList()).willReturn(Arrays.asList(one));

        ServerGroupConfiguration config = new ServerGroupConfiguration(mockGroup, mockInstance);
        // When
        Set<LDAPURL> result = config.getLDAPURLs();

        // Then
        assertThat(result).hasSize(1);
        LDAPURL url = result.iterator().next();
        assertThat(url.getHost()).isEqualTo(hostName);
        assertThat(url.getPort()).isEqualTo(port);
        assertThat(url.isSSL()).isFalse();
    }

    @Test
    public void shouldReturnCorrectLDAPURLforSSLConnections() {
        // Given
        String hostName = "localhost";
        int port = 389;
        Server one = mock(Server.class);
        given(one.getServerName()).willReturn(hostName);
        given(one.getPort()).willReturn(port);
        given(one.getConnectionType()).willReturn(Server.Type.CONN_SSL);

        ServerInstance mockInstance = mock(ServerInstance.class);
        ServerGroup mockGroup = mock(ServerGroup.class);
        given(mockGroup.getServersList()).willReturn(Arrays.asList(one));

        ServerGroupConfiguration config = new ServerGroupConfiguration(mockGroup, mockInstance);
        // When
        Set<LDAPURL> result = config.getLDAPURLs();

        // Then
        assertThat(result).hasSize(1);
        LDAPURL url = result.iterator().next();
        assertThat(url.getHost()).isEqualTo(hostName);
        assertThat(url.getPort()).isEqualTo(port);
        assertThat(url.isSSL()).isTrue();
    }

    @Test
    public void shouldReturnBindDNFromInstance() {
        // Given
        ServerInstance mockInstance = mock(ServerInstance.class);
        ServerGroup mockGroup = mock(ServerGroup.class);
        ServerGroupConfiguration config = new ServerGroupConfiguration(mockGroup, mockInstance);

        // When
        config.getBindDN();

        // Then
        verify(mockInstance).getAuthID();
    }

    @Test
    public void shouldReturnPasswordFromInstance() {
        // Given
        ServerInstance mockInstance = mock(ServerInstance.class);
        ServerGroup mockGroup = mock(ServerGroup.class);
        ServerGroupConfiguration config = new ServerGroupConfiguration(mockGroup, mockInstance);
        given(mockInstance.getPasswd()).willReturn("");

        // When
        config.getBindPassword();

        // Then
        verify(mockInstance).getPasswd();
    }

    @Test
    public void shouldReturnMaxConnectionsFromInstance() {
        // Given
        ServerInstance mockInstance = mock(ServerInstance.class);
        ServerGroup mockGroup = mock(ServerGroup.class);
        ServerGroupConfiguration config = new ServerGroupConfiguration(mockGroup, mockInstance);

        // When
        config.getMaxConnections();

        // Then
        verify(mockInstance).getMaxConnections();
    }
}

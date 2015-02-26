/**
 * Copyright 2013 ForgeRock, Inc.
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

import com.iplanet.services.ldap.DSConfigMgr;
import com.iplanet.services.ldap.LDAPUser;
import com.iplanet.services.ldap.ServerGroup;
import com.iplanet.services.ldap.ServerInstance;
import org.forgerock.openam.sm.ServerConfigurationFactory;
import org.forgerock.openam.sm.exceptions.ConnectionCredentialsNotFound;
import org.forgerock.openam.sm.exceptions.ServerConfigurationNotFound;
import org.testng.annotations.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;

/**
 * @author robert.wapshott@forgerock.com
 */
public class ServerConfigurationFactoryTest {
    @Test (expectedExceptions = ServerConfigurationNotFound.class)
    public void shouldIndicateInvalidIfServerGroupIsNull() throws ConnectionCredentialsNotFound, ServerConfigurationNotFound {
        // Given
        ServerInstance mockInstance = mock(ServerInstance.class);

        DSConfigMgr mockConfig = mock(DSConfigMgr.class);
        given(mockConfig.getServerGroup(anyString())).willReturn(null);
        given(mockConfig.getServerInstance(
                anyString(), any(LDAPUser.Type.class))).willReturn(mockInstance);

        ServerConfigurationFactory parser = new ServerConfigurationFactory(mockConfig);

        // When / Then
        parser.getServerConfiguration("", LDAPUser.Type.AUTH_ADMIN);
    }

    @Test (expectedExceptions = ConnectionCredentialsNotFound.class)
    public void shouldIndicateInvalidIfServerInstanceIsNull() throws ConnectionCredentialsNotFound, ServerConfigurationNotFound {
        // Given
        ServerGroup mockGroup = mock(ServerGroup.class);

        DSConfigMgr mockConfig = mock(DSConfigMgr.class);
        given(mockConfig.getServerGroup(anyString())).willReturn(mockGroup);
        given(mockConfig.getServerInstance(
                anyString(), any(LDAPUser.Type.class))).willReturn(null);

        ServerConfigurationFactory parser = new ServerConfigurationFactory(mockConfig);

        // When / Then
        parser.getServerConfiguration("", LDAPUser.Type.AUTH_ADMIN);
    }

    @Test
    public void shouldReturnRequestedServerGroup() throws ConnectionCredentialsNotFound, ServerConfigurationNotFound {
        // Given
        String test = "badger";
        ServerGroup mockGroup = mock(ServerGroup.class);
        ServerInstance mockInstance = mock(ServerInstance.class);

        DSConfigMgr mockConfig = mock(DSConfigMgr.class);
        given(mockConfig.getServerGroup(test)).willReturn(mockGroup);
        given(mockConfig.getServerInstance(
                anyString(), any(LDAPUser.Type.class))).willReturn(mockInstance);

        ServerConfigurationFactory parser = new ServerConfigurationFactory(mockConfig);

        // When
        parser.getServerConfiguration(test, LDAPUser.Type.AUTH_ADMIN);

        // Then
        verify(mockConfig).getServerGroup(test);
    }

    @Test
    public void shouldReturnInstanceBindDN() throws ConnectionCredentialsNotFound, ServerConfigurationNotFound {
        // Given
        DSConfigMgr configMgr = mock(DSConfigMgr.class);

        ServerGroup serverGroup = mock(ServerGroup.class);
        given(configMgr.getServerGroup(anyString())).willReturn(serverGroup);

        ServerInstance mockInstance = mock(ServerInstance.class);
        given(configMgr.getServerInstance(
                anyString(), any(LDAPUser.Type.class))).willReturn(mockInstance);
        given(mockInstance.getAuthID()).willReturn("");

        ServerConfigurationFactory parser = new ServerConfigurationFactory(configMgr);

        // When
        String dn = parser.getServerConfiguration("", LDAPUser.Type.AUTH_ADMIN).getBindDN();

        // Then
        verify(mockInstance).getAuthID();
    }
}
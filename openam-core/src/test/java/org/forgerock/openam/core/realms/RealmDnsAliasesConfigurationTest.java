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

package org.forgerock.openam.core.realms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Collections;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.OrganizationConfigManagerFactory;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.Test;

/**
 * Reads the DNS aliases through {@link OrganizationConfigManager}, which {@link RealmDnsAliasesTest} replaces.
 */
// The static initialiser of OrganizationConfigManager needs the configuration store.
@SuppressStaticInitializationFor("com.sun.identity.sm.OrganizationConfigManager")
@PowerMockIgnore({ "jdk.internal.reflect.*" })
public class RealmDnsAliasesConfigurationTest extends PowerMockTestCase {

    @Test
    public void shouldReadDnsAliasesOfNestedRealmsFromTheConfiguration() throws Exception {
        SSOToken adminToken = mock(SSOToken.class);
        OrganizationConfigManagerFactory organizationConfigManagerFactory =
                mock(OrganizationConfigManagerFactory.class);
        ServiceConfigManager idRepoService = mock(ServiceConfigManager.class);
        given(idRepoService.addListener(any(ServiceListener.class))).willReturn("LISTENER_ID");
        OrganizationConfigManager root = mock(OrganizationConfigManager.class);
        OrganizationConfigManager sub = mock(OrganizationConfigManager.class);
        given(organizationConfigManagerFactory.create(adminToken, "/")).willReturn(root);
        given(organizationConfigManagerFactory.create(adminToken, "/acme/sub")).willReturn(sub);
        given(root.getSubOrganizationNames("*", true)).willReturn(asSet("acme/sub"));
        given(sub.getAttributes(IdConstants.REPO_SERVICE)).willReturn(
                Collections.singletonMap(IdConstants.ORGANIZATION_ALIAS_ATTR, asSet("sub.acme.example.com")));
        RealmDnsAliases aliases = new RealmDnsAliases(() -> adminToken, organizationConfigManagerFactory,
                () -> idRepoService);

        assertThat(aliases.contains("sub.acme.example.com")).isTrue();
    }
}

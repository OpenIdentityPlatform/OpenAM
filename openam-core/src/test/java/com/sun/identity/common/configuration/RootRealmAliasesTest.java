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

package com.sun.identity.common.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceManager;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Checks that the DNS aliases of the top-level realm follow the host names of the servers and sites.
 */
// The static initialiser of OrganizationConfigManager needs the configuration store.
@SuppressStaticInitializationFor("com.sun.identity.sm.OrganizationConfigManager")
@PrepareForTest(ConfigurationBase.class)
@PowerMockIgnore({ "jdk.internal.reflect.*" })
public class RootRealmAliasesTest extends PowerMockTestCase {

    private SSOToken token;
    private Map<String, Map<String, Set<String>>> servers;
    private Map<String, String> siteIds;
    private Map<String, String> sitePrimaryUrls;
    private Map<String, Set<String>> siteSecondaryUrls;
    private Set<String> sitesWithoutAccessPoint;
    private Set<String> rootRealmAliases;
    private OrganizationConfigManager rootRealm;

    @BeforeMethod
    public void setUp() throws Exception {
        token = mock(SSOToken.class);
        servers = new LinkedHashMap<>();
        siteIds = new LinkedHashMap<>();
        sitePrimaryUrls = new HashMap<>();
        siteSecondaryUrls = new HashMap<>();
        sitesWithoutAccessPoint = new HashSet<>();
        addServer(ServerConfiguration.DEFAULT_SERVER_CONFIG, ServerConfiguration.DEFAULT_SERVER_ID, null);

        ServiceConfigManager platformService = mock(ServiceConfigManager.class);
        whenNew(ServiceConfigManager.class).withAnyArguments().thenReturn(platformService);
        ServiceConfig globalConfig = validConfig();
        given(platformService.getGlobalConfig(null)).willReturn(globalConfig);
        ServiceConfig serversConfig = serversConfig();
        given(globalConfig.getSubConfig(ConfigurationBase.CONFIG_SERVERS)).willReturn(serversConfig);
        ServiceConfig sitesConfig = sitesConfig();
        given(globalConfig.getSubConfig(ConfigurationBase.CONFIG_SITES)).willReturn(sitesConfig);

        rootRealm = mock(OrganizationConfigManager.class);
        whenNew(OrganizationConfigManager.class).withArguments(token, "/").thenReturn(rootRealm);
        given(rootRealm.getAttributes(ServiceManager.REALM_SERVICE)).willAnswer(invocation -> {
            Map<String, Set<String>> attributes = new HashMap<>();
            attributes.put("sunOrganizationStatus", asSet("Active"));
            attributes.put(OrganizationConfigManager.SUNORG_ALIAS, new HashSet<>(rootRealmAliases));
            return attributes;
        });
        willAnswer(invocation -> {
            Map<String, Set<String>> attributes = invocation.getArgument(1);
            rootRealmAliases = new HashSet<>(attributes.get(OrganizationConfigManager.SUNORG_ALIAS));
            return null;
        }).given(rootRealm).setAttributes(eq(ServiceManager.REALM_SERVICE), anyMap());
    }

    @Test
    public void shouldRemoveHostOfDeletedServerFromRootRealmAliases() throws Exception {
        addServer("http://openam.example.com:8080/openam", "01", null);
        addServer("http://old.example.com:8080/openam", "02", null);
        rootRealmAliases = asSet("openam", "openam.example.com", "old.example.com");

        ServerConfiguration.deleteServerInstance(token, "http://old.example.com:8080/openam");

        assertThat(rootRealmAliases).containsOnly("openam", "openam.example.com");
    }

    @Test
    public void shouldKeepHostOfDeletedServerThatAnotherServerHas() throws Exception {
        addServer("http://openam.example.com:8080/openam", "01", null);
        addServer("http://openam.example.com:9090/openam", "02", null);
        rootRealmAliases = asSet("openam", "openam.example.com");

        ServerConfiguration.deleteServerInstance(token, "http://openam.example.com:9090/openam");

        assertThat(rootRealmAliases).containsOnly("openam", "openam.example.com");
    }

    @Test
    public void shouldKeepHostOfDeletedServerThatAnotherServerHasInOtherCase() throws Exception {
        addServer("http://OPENAM.EXAMPLE.COM:8080/openam", "01", null);
        addServer("http://openam.example.com:9090/openam", "02", null);
        rootRealmAliases = asSet("openam", "openam.example.com");

        ServerConfiguration.deleteServerInstance(token, "http://openam.example.com:9090/openam");

        assertThat(rootRealmAliases).containsOnly("openam", "openam.example.com");
    }

    @Test
    public void shouldRemoveHostOfLastDeletedServerThatTheAliasesHaveInOtherCase() throws Exception {
        addServer("http://A.example.com:8080/openam", "01", null);
        addServer("http://a.example.com:9090/openam", "02", null);
        rootRealmAliases = asSet("openam", "A.example.com");

        ServerConfiguration.deleteServerInstance(token, "http://A.example.com:8080/openam");
        ServerConfiguration.deleteServerInstance(token, "http://a.example.com:9090/openam");

        assertThat(rootRealmAliases).containsOnly("openam");
    }

    @Test
    public void shouldKeepHostOfDeletedServerThatTheSitePrimaryUrlHas() throws Exception {
        addSite("site1", "03", "https://lb.example.com:443/openam");
        addServer("http://openam.example.com:8080/openam", "01", "site1");
        addServer("http://lb.example.com:8080/openam", "02", null);
        rootRealmAliases = asSet("openam", "openam.example.com", "lb.example.com");

        ServerConfiguration.deleteServerInstance(token, "http://lb.example.com:8080/openam");

        assertThat(rootRealmAliases).containsOnly("openam", "openam.example.com", "lb.example.com");
    }

    @Test
    public void shouldKeepHostOfDeletedServerThatASiteSecondaryUrlHas() throws Exception {
        addSite("site1", "03", "https://lb.example.com:443/openam", "https://lb2.example.com:443/openam");
        addServer("http://openam.example.com:8080/openam", "01", "site1");
        addServer("http://lb2.example.com:8080/openam", "02", null);
        rootRealmAliases = asSet("openam", "openam.example.com", "lb.example.com", "lb2.example.com");

        ServerConfiguration.deleteServerInstance(token, "http://lb2.example.com:8080/openam");

        assertThat(rootRealmAliases).containsOnly("openam", "openam.example.com", "lb.example.com",
                "lb2.example.com");
    }

    @Test
    public void shouldRemoveHostOfDeletedSiteFromRootRealmAliases() throws Exception {
        addSite("site1", "03", "https://lb.example.com:443/openam");
        addServer("http://openam.example.com:8080/openam", "01", "site1");
        rootRealmAliases = asSet("openam", "openam.example.com", "lb.example.com");

        SiteConfiguration.deleteSite(token, "site1");

        assertThat(rootRealmAliases).containsOnly("openam", "openam.example.com");
    }

    @Test
    public void shouldKeepHostOfDeletedSiteThatAServerHas() throws Exception {
        addSite("site1", "03", "https://openam.example.com:443/openam");
        addServer("http://openam.example.com:8080/openam", "01", "site1");
        rootRealmAliases = asSet("openam", "openam.example.com");

        SiteConfiguration.deleteSite(token, "site1");

        assertThat(rootRealmAliases).containsOnly("openam", "openam.example.com");
    }

    @Test
    public void shouldRemoveHostOfReplacedSitePrimaryUrlFromRootRealmAliases() throws Exception {
        addSite("site1", "03", "https://old-lb.example.com:443/openam");
        addServer("http://openam.example.com:8080/openam", "01", "site1");
        rootRealmAliases = asSet("openam", "openam.example.com", "old-lb.example.com");

        SiteConfiguration.setSitePrimaryURL(token, "site1", "https://lb.example.com:443/openam");

        assertThat(rootRealmAliases).containsOnly("openam", "openam.example.com");
    }

    @Test
    public void shouldKeepHostOfReplacedSitePrimaryUrlThatTheNewUrlHas() throws Exception {
        addSite("site1", "03", "https://lb.example.com:443/openam");
        addServer("http://openam.example.com:8080/openam", "01", "site1");
        rootRealmAliases = asSet("openam", "openam.example.com", "lb.example.com");

        SiteConfiguration.setSitePrimaryURL(token, "site1", "https://lb.example.com:8443/openam");

        assertThat(rootRealmAliases).containsOnly("openam", "openam.example.com", "lb.example.com");
    }

    @Test
    public void shouldDeleteServerWhenRootRealmAliasesCannotBeUpdated() throws Exception {
        addServer("http://openam.example.com:8080/openam", "01", null);
        addServer("http://old.example.com:8080/openam", "02", null);
        rootRealmAliases = asSet("openam", "openam.example.com", "old.example.com");
        willThrow(new SMSException("Unable to update the realm"))
                .given(rootRealm).setAttributes(eq(ServiceManager.REALM_SERVICE), anyMap());

        boolean deleted = ServerConfiguration.deleteServerInstance(token, "http://old.example.com:8080/openam");

        assertThat(deleted).isTrue();
        assertThat(servers).doesNotContainKey("http://old.example.com:8080/openam");
        assertThat(rootRealmAliases).contains("old.example.com");
    }

    @Test
    public void shouldDeleteSiteWhenAnotherSiteHasNoAccessPoint() throws Exception {
        addSite("site1", "03", "https://lb.example.com:443/openam");
        addSiteWithoutAccessPoint("site2", "04");
        addServer("http://openam.example.com:8080/openam", "01", null);
        rootRealmAliases = asSet("openam", "openam.example.com", "lb.example.com");

        boolean deleted = SiteConfiguration.deleteSite(token, "site1");

        assertThat(deleted).isTrue();
        assertThat(siteIds).doesNotContainKey("site1");
        assertThat(rootRealmAliases).contains("lb.example.com");
    }

    @Test
    public void shouldReplaceSitePrimaryUrlWhenRootRealmAliasesCannotBeRead() throws Exception {
        addSite("site1", "03", "https://old-lb.example.com:443/openam");
        addServer("http://openam.example.com:8080/openam", "01", "site1");
        rootRealmAliases = asSet("openam", "openam.example.com", "old-lb.example.com");
        given(rootRealm.getAttributes(ServiceManager.REALM_SERVICE))
                .willThrow(new SMSException("Unable to read the realm"));

        SiteConfiguration.setSitePrimaryURL(token, "site1", "https://lb.example.com:443/openam");

        assertThat(sitePrimaryUrls).containsEntry("site1", "https://lb.example.com:443/openam");
        assertThat(rootRealmAliases).contains("old-lb.example.com");
    }

    @Test(expectedExceptions = ConfigurationException.class)
    public void shouldRejectDeletingSiteWithoutAccessPointAsInvalidSite() throws Exception {
        addSiteWithoutAccessPoint("site1", "03");

        SiteConfiguration.deleteSite(token, "site1");
    }

    @Test
    public void shouldDeleteSiteWhoseAccessPointHasNoPrimaryUrl() throws Exception {
        addSiteWithoutPrimaryUrl("site1", "03");
        addServer("http://openam.example.com:8080/openam", "01", "site1");
        rootRealmAliases = asSet("openam", "openam.example.com");

        boolean deleted = SiteConfiguration.deleteSite(token, "site1");

        assertThat(deleted).isTrue();
        assertThat(siteIds).doesNotContainKey("site1");
        assertThat(rootRealmAliases).containsOnly("openam", "openam.example.com");
    }

    private void addServer(String name, String serverId, String siteName) {
        Map<String, Set<String>> attributes = new HashMap<>();
        attributes.put(ConfigurationBase.ATTR_SERVER_ID, asSet(serverId));
        attributes.put("parentsiteid", siteName == null ? Collections.<String>emptySet() : asSet(siteName));
        servers.put(name, attributes);
    }

    private void addSite(String siteName, String siteId, String primaryUrl, String... secondaryUrls) {
        siteIds.put(siteName, siteId);
        sitePrimaryUrls.put(siteName, primaryUrl);
        siteSecondaryUrls.put(siteName, asSet(secondaryUrls));
    }

    /** A site whose creation stopped before its access point was written. */
    private void addSiteWithoutAccessPoint(String siteName, String siteId) {
        siteIds.put(siteName, siteId);
        sitesWithoutAccessPoint.add(siteName);
    }

    /** A site whose primary URL was removed from its access point by hand, as {@code ssoadm set-sub-cfg} can do. */
    private void addSiteWithoutPrimaryUrl(String siteName, String siteId) {
        addSite(siteName, siteId, null);
    }

    private ServiceConfig validConfig() {
        ServiceConfig config = mock(ServiceConfig.class);
        given(config.isValid()).willReturn(true);
        return config;
    }

    private ServiceConfig serversConfig() throws Exception {
        ServiceConfig config = validConfig();
        given(config.getSubConfigNames("*")).willAnswer(invocation -> new HashSet<>(servers.keySet()));
        given(config.getSubConfig(anyString())).willAnswer(invocation -> serverConfig(invocation.getArgument(0)));
        willAnswer(invocation -> servers.remove(invocation.<String>getArgument(0)))
                .given(config).removeSubConfig(anyString());
        return config;
    }

    private ServiceConfig serverConfig(String name) throws Exception {
        if (!servers.containsKey(name)) {
            return null;
        }
        ServiceConfig config = validConfig();
        given(config.getAttributes()).willAnswer(invocation -> new HashMap<>(servers.get(name)));
        willAnswer(invocation -> {
            servers.get(name).putAll(invocation.getArgument(0));
            return null;
        }).given(config).setAttributes(anyMap());
        return config;
    }

    private ServiceConfig sitesConfig() throws Exception {
        ServiceConfig config = validConfig();
        given(config.getSubConfigNames("*")).willAnswer(invocation -> new HashSet<>(siteIds.keySet()));
        given(config.getSubConfig(anyString())).willAnswer(invocation -> siteConfig(invocation.getArgument(0)));
        willAnswer(invocation -> {
            String siteName = invocation.getArgument(0);
            siteIds.remove(siteName);
            sitePrimaryUrls.remove(siteName);
            siteSecondaryUrls.remove(siteName);
            return null;
        }).given(config).removeSubConfig(anyString());
        return config;
    }

    private ServiceConfig siteConfig(String siteName) throws Exception {
        if (!siteIds.containsKey(siteName)) {
            return null;
        }
        if (sitesWithoutAccessPoint.contains(siteName)) {
            return validConfig();
        }
        String siteId = siteIds.get(siteName);
        ServiceConfig accessPoint = validConfig();
        given(accessPoint.getAttributes()).willAnswer(invocation -> {
            Map<String, Set<String>> attributes = new HashMap<>();
            attributes.put(ConfigurationBase.ATTR_PRIMARY_SITE_ID, asSet(siteId));
            if (sitePrimaryUrls.get(siteName) != null) {
                attributes.put(ConfigurationBase.ATTR_PRIMARY_SITE_URL, asSet(sitePrimaryUrls.get(siteName)));
            }
            return attributes;
        });
        willAnswer(invocation -> {
            Map<String, Set<String>> attributes = invocation.getArgument(0);
            sitePrimaryUrls.put(siteName, attributes.get(ConfigurationBase.ATTR_PRIMARY_SITE_URL).iterator().next());
            return null;
        }).given(accessPoint).setAttributes(anyMap());
        given(accessPoint.getSubConfigNames("*"))
                .willAnswer(invocation -> new HashSet<>(siteSecondaryUrls.get(siteName)));
        given(accessPoint.getSubConfig(anyString())).willAnswer(invocation -> {
            ServiceConfig secondaryUrl = validConfig();
            given(secondaryUrl.getAttributes()).willReturn(
                    Collections.singletonMap(ConfigurationBase.ATTR_SEC_ID, asSet(siteId)));
            return secondaryUrl;
        });
        ServiceConfig site = validConfig();
        given(site.getSubConfig(ConfigurationBase.SUBCONFIG_ACCESS_URL)).willReturn(accessPoint);
        return site;
    }
}

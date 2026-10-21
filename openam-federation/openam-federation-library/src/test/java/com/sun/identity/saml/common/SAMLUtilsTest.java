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
package com.sun.identity.saml.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sun.identity.saml.common.SAMLServiceManager.SiteEntry;

public class SAMLUtilsTest {

    /**
     * postYN() only checks host, port and path of the target, so the query string
     * is attacker-influenced and must not be able to close the ACTION attribute.
     */
    @Test
    public void postToTargetEscapesTargetUrlInFormAction() throws Exception {
        String target = "https://sp.example.com/acs?next=\"><script>alert(1)</script><a href=\"";
        StringWriter html = new StringWriter();

        SAMLUtils.postToTarget(null, new PrintWriter(html), Collections.emptyList(), target,
                Collections.emptyMap());

        String out = html.toString();
        assertThat(out).doesNotContain("<script>");
        assertThat(out).contains("ACTION=\"https://sp.example.com/acs?next=&quot;&gt;&lt;script&gt;");
    }

    @DataProvider
    public Object[][] hostsInDomain() {
        return new Object[][] {
            {"sp.example.com", "sp.example.com"},
            {"www.sp.example.com", "sp.example.com"},
            {"sp.example.com", "example.com"},
            {"SP.Example.COM", "sp.example.com"},
            {"sp.example.com", " SP.EXAMPLE.COM "},
            {"sp.example.com.", "sp.example.com"},
            {"sp.example.com", ".example.com"},
        };
    }

    @Test(dataProvider = "hostsInDomain")
    public void hostInPartnerDomainMatches(String host, String domain) {
        assertThat(SAMLUtils.isHostInDomain(host, domain)).isTrue();
    }

    @DataProvider
    public Object[][] hostsOutsideDomain() {
        return new Object[][] {
            {"sp.example.com.attacker.com", "sp.example.com"},
            {"evilsp.example.com", "sp.example.com"},
            {"attacker-sp.example.com.evil", "sp.example.com"},
            {"attacker.com", "sp.example.com"},
            {"example.com", "sp.example.com"},
            {"sp.example.com", ""},
            {"sp.example.com", "."},
            {"sp.example.com", null},
            {null, "sp.example.com"},
            {"", ""},
            {"", "."},
            {null, null},
            // java.net.URL before JDK 20 leaves these characters in the host
            {"attacker.com\\.sp.example.com", "sp.example.com"},
            {"attacker.com%5c.sp.example.com", "sp.example.com"},
            // IPv6 literals are not supported as partner targets
            {"[::1]", "[::1]"},
        };
    }

    @Test(dataProvider = "hostsOutsideDomain")
    public void hostThatOnlyContainsPartnerDomainDoesNotMatch(String host, String domain) {
        assertThat(SAMLUtils.isHostInDomain(host, domain)).isFalse();
    }

    @Test
    public void trustedSiteRejectsTargetThatOnlyContainsPartnerHost() {
        Set<SiteEntry> sites = sites(site("sp.example.com", -1));

        assertThat(SAMLUtils.getTrustedSite(sites, "sp.example.com.attacker.com", -1)).isNull();
        assertThat(SAMLUtils.getTrustedSite(sites, "evilsp.example.com", 443)).isNull();
    }

    @Test
    public void trustedSiteWithoutPortAcceptsAnyPort() {
        SiteEntry sp = site("sp.example.com", -1);

        assertThat(SAMLUtils.getTrustedSite(sites(sp), "sp.example.com", -1)).isSameAs(sp);
        assertThat(SAMLUtils.getTrustedSite(sites(sp), "app.sp.example.com", 8443)).isSameAs(sp);
    }

    @Test
    public void trustedSiteWithPortRequiresThatPort() {
        SiteEntry sp = site("sp.example.com", 8443);

        assertThat(SAMLUtils.getTrustedSite(sites(sp), "sp.example.com", 8443)).isSameAs(sp);
        assertThat(SAMLUtils.getTrustedSite(sites(sp), "sp.example.com", 443)).isNull();
        assertThat(SAMLUtils.getTrustedSite(sites(sp), "sp.example.com", -1)).isNull();
    }

    @Test
    public void mostSpecificTrustedSiteWins() {
        SiteEntry domain = site("example.com", -1);
        SiteEntry host = site("sp.example.com", -1);
        SiteEntry hostAndPort = site("sp.example.com", 8443);
        Set<SiteEntry> sites = sites(domain, host, hostAndPort);

        assertThat(SAMLUtils.getTrustedSite(sites, "other.example.com", 443)).isSameAs(domain);
        assertThat(SAMLUtils.getTrustedSite(sites, "sp.example.com", 443)).isSameAs(host);
        assertThat(SAMLUtils.getTrustedSite(sites, "sp.example.com", 8443)).isSameAs(hostAndPort);

        Set<SiteEntry> reversed = sites(hostAndPort, host, domain);

        assertThat(SAMLUtils.getTrustedSite(reversed, "other.example.com", 443)).isSameAs(domain);
        assertThat(SAMLUtils.getTrustedSite(reversed, "sp.example.com", 443)).isSameAs(host);
        assertThat(SAMLUtils.getTrustedSite(reversed, "sp.example.com", 8443)).isSameAs(hostAndPort);
    }

    @Test
    public void specificityIgnoresLeadingAndTrailingDots() {
        SiteEntry dottedDomain = site(".sp.example.com", -1);
        SiteEntry hostAndPort = site("sp.example.com", 8443);
        SiteEntry dottedParent = site(".example.com.", 443);
        SiteEntry subdomain = site("a.example.com", -1);

        assertThat(SAMLUtils.getTrustedSite(sites(dottedDomain, hostAndPort), "sp.example.com", 8443))
                .isSameAs(hostAndPort);
        assertThat(SAMLUtils.getTrustedSite(sites(dottedParent, subdomain), "a.example.com", 443))
                .isSameAs(subdomain);
    }

    @Test
    public void trustedSiteForTargetUrl() throws Exception {
        SiteEntry sp = site("sp.example.com", -1);

        assertThat(SAMLUtils.getTrustedSite(sites(sp), "https://sp.example.com.attacker.com/x")).isNull();
        assertThat(SAMLUtils.getTrustedSite(sites(sp), "https://evilsp.example.com/x")).isNull();
        assertThat(SAMLUtils.getTrustedSite(sites(sp), "https://app.sp.example.com:8443/x")).isSameAs(sp);
        assertThat(SAMLUtils.getTrustedSite(sites(sp), "https://sp.example.com/x?next=a\\b")).isSameAs(sp);
    }

    @Test
    public void targetUrlWithUserInfoIsRejected() throws Exception {
        SiteEntry sp = site("sp.example.com", -1);

        assertThat(SAMLUtils.getTrustedSite(sites(sp), "http://user@sp.example.com/")).isNull();
    }

    /**
     * Browsers read a backslash in an http(s) authority as a slash, so these
     * TARGETs lead to attacker.com. java.net.URL rejects them from JDK 20 on;
     * before that it parses them, and the host must not match.
     */
    @Test
    public void targetUrlWithBackslashInAuthorityIsRejected() {
        SiteEntry sp = site("sp.example.com", -1);

        assertNoTrustedSite(sites(sp), "http://attacker.com\\@sp.example.com/x");
        assertNoTrustedSite(sites(sp), "http://attacker.com\\.sp.example.com/x");
    }

    private static void assertNoTrustedSite(Set<SiteEntry> sites, String target) {
        try {
            assertThat(SAMLUtils.getTrustedSite(sites, target)).isNull();
        } catch (MalformedURLException expected) {
            // JDK 20+
        }
    }

    @Test
    public void noTrustedSitesMeansNoMatch() {
        assertThat(SAMLUtils.getTrustedSite(null, "sp.example.com", -1)).isNull();
        assertThat(SAMLUtils.getTrustedSite(sites(), "sp.example.com", -1)).isNull();
    }

    private static SiteEntry site(String host, int port) {
        return new SiteEntry(host, port, "sourceID", "https://" + host + "/saml", null, "1.1");
    }

    private static Set<SiteEntry> sites(SiteEntry... entries) {
        return new LinkedHashSet<>(Arrays.asList(entries));
    }

    /**
     * Attribute names and values come verbatim from the partner IdP's assertion, so they are
     * escaped in full: markup must stay inert and an '&' must reach the target unchanged.
     */
    @Test
    public void postToTargetEscapesAttributeNamesAndValues() throws Exception {
        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("x\"<y", "<script>alert(1)</script>");
        attrs.put("amp", "a&amp;b");
        StringWriter html = new StringWriter();

        SAMLUtils.postToTarget(null, new PrintWriter(html), Collections.emptyList(),
                "https://sp.example.com/acs", attrs);

        String out = html.toString();
        assertThat(out).doesNotContain("<script>");
        assertThat(out).contains(
                "NAME=\"x&quot;&lt;y\" VALUE=\"&lt;script&gt;alert(1)&lt;/script&gt;\"");
        assertThat(out).contains("NAME=\"amp\" VALUE=\"a&amp;amp;b\"");
        assertThat(out).contains("NAME=\"" + SAMLConstants.POST_ATTR_NAMES
                + "\" VALUE=\"x&quot;&lt;y:amp\"");
    }
}

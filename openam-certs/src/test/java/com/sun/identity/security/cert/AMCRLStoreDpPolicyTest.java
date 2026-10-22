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

package com.sun.identity.security.cert;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/**
 * Verifies the address policy applied to CRL Distribution Point URLs taken from a certificate
 * before any outbound connection is made (GHSA-g5vw-v384-gw9v).
 */
public class AMCRLStoreDpPolicyTest extends CertTestBase {

    @Test
    public void cloudMetadataAddressIsRejected() {
        // The address named in the advisory: a link-local instance-metadata endpoint.
        assertThat(AMCRLStore.isCrlDpUrlAllowed(
                "http://169.254.169.254/latest/meta-data/iam/security-credentials/")).isFalse();
    }

    @Test
    public void loopbackAddressIsRejected() {
        assertThat(AMCRLStore.isCrlDpUrlAllowed("http://127.0.0.1:8080/crl")).isFalse();
    }

    @Test
    public void loopbackHostNameIsRejected() {
        assertThat(AMCRLStore.isCrlDpUrlAllowed("http://localhost:8080/crl")).isFalse();
    }

    @Test
    public void ipv6LoopbackAddressIsRejected() {
        assertThat(AMCRLStore.isCrlDpUrlAllowed("http://[::1]:8080/crl")).isFalse();
    }

    @Test
    public void wildcardAddressIsRejected() {
        assertThat(AMCRLStore.isCrlDpUrlAllowed("http://0.0.0.0/crl")).isFalse();
    }

    @Test
    public void multicastAddressIsRejected() {
        assertThat(AMCRLStore.isCrlDpUrlAllowed("http://224.0.0.1/crl")).isFalse();
    }

    @Test
    public void nonHttpSchemeIsRejected() {
        assertThat(AMCRLStore.isCrlDpUrlAllowed("file:///etc/passwd")).isFalse();
        assertThat(AMCRLStore.isCrlDpUrlAllowed("gopher://198.51.100.7/crl")).isFalse();
    }

    @Test
    public void malformedUrlIsRejected() {
        assertThat(AMCRLStore.isCrlDpUrlAllowed("not a url")).isFalse();
        assertThat(AMCRLStore.isCrlDpUrlAllowed(null)).isFalse();
        assertThat(AMCRLStore.isCrlDpUrlAllowed("")).isFalse();
    }

    @Test
    public void unresolvableHostIsRejected() {
        // .invalid is reserved by RFC 6761 and must never resolve — but a resolver that answers
        // NXDOMAIN with a captive-portal or ad-server address (corporate and ISP DNS both do,
        // and so do some CI runners) hands back a public address for it, and the test then fails
        // on the resolver rather than on the code. The first label is 64 octets, one past the 63
        // RFC 1035 allows, so the name cannot be encoded into a query at all and no resolver —
        // wildcarding or not — has anything to answer.
        String overlongLabel = new String(new char[64]).replace('\0', 'a');
        assertThat(AMCRLStore.isCrlDpUrlAllowed(
                "http://" + overlongLabel + ".example.invalid/crl")).isFalse();
    }

    @Test
    public void publicAddressIsAccepted() {
        // A literal address, so the check does not depend on DNS being reachable.
        assertThat(AMCRLStore.isCrlDpUrlAllowed("http://198.51.100.7/crl")).isTrue();
        assertThat(AMCRLStore.isCrlDpUrlAllowed("https://198.51.100.7/crl")).isTrue();
    }

    @Test
    public void privateAddressIsAcceptedByDefault() {
        // An internal PKI legitimately publishes its CRLs on RFC1918 addresses; blocking them
        // by default would break working deployments on upgrade.
        assertThat(AMCRLStore.isCrlDpUrlAllowed("http://10.1.2.3/crl")).isTrue();
        assertThat(AMCRLStore.isCrlDpUrlAllowed("http://192.168.1.5/crl")).isTrue();
    }

    @Test
    public void privateAddressIsRejectedInStrictMode() throws Exception {
        withSystemProperty(AMCRLStore.CRL_DP_STRICT, "true", () -> {
            assertThat(AMCRLStore.isCrlDpUrlAllowed("http://10.1.2.3/crl")).isFalse();
            assertThat(AMCRLStore.isCrlDpUrlAllowed("http://192.168.1.5/crl")).isFalse();
            // A public address stays allowed in strict mode.
            assertThat(AMCRLStore.isCrlDpUrlAllowed("http://198.51.100.7/crl")).isTrue();
        });
    }

    @Test
    public void blockedAddressIsAcceptedWhenEscapeHatchIsSet() throws Exception {
        withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () ->
                assertThat(AMCRLStore.isCrlDpUrlAllowed("http://169.254.169.254/crl")).isTrue());
    }

    @Test
    public void nonHttpSchemeIsRejectedEvenWithEscapeHatch() throws Exception {
        // The escape hatch relaxes the address policy, not the scheme allow-list: a CRL is
        // always fetched over http(s) or ldap(s), never over file:// or gopher://.
        withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () ->
                assertThat(AMCRLStore.isCrlDpUrlAllowed("file:///etc/passwd")).isFalse());
    }

    @Test
    public void ldapHostPolicyMatchesHttpPolicy() throws Exception {
        assertThat(AMCRLStore.isCrlDpHostAllowed("127.0.0.1")).isFalse();
        assertThat(AMCRLStore.isCrlDpHostAllowed("169.254.169.254")).isFalse();
        assertThat(AMCRLStore.isCrlDpHostAllowed("198.51.100.7")).isTrue();
        assertThat(AMCRLStore.isCrlDpHostAllowed("10.1.2.3")).isTrue();
        assertThat(AMCRLStore.isCrlDpHostAllowed(null)).isFalse();

        withSystemProperty(AMCRLStore.CRL_DP_STRICT, "true", () ->
                assertThat(AMCRLStore.isCrlDpHostAllowed("10.1.2.3")).isFalse());
        withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () ->
                assertThat(AMCRLStore.isCrlDpHostAllowed("127.0.0.1")).isTrue());
    }

    @Test
    public void strictModeAlsoRejectsWhatTheDefaultPolicyRejects() throws Exception {
        // Strict mode has to be a superset of the default: turning it on must never make an
        // address acceptable that was blocked before. Both modes are now the same
        // SsrfUrlValidator policy with one clause switched, which is what makes that hold; when
        // the two were separate implementations, a clause present in only one of them — the
        // broadcast address, say — broke it.
        withSystemProperty(AMCRLStore.CRL_DP_STRICT, "true", () -> {
            assertThat(AMCRLStore.isCrlDpUrlAllowed("http://255.255.255.255/crl")).isFalse();
            assertThat(AMCRLStore.isCrlDpUrlAllowed("http://127.0.0.1/crl")).isFalse();
            assertThat(AMCRLStore.isCrlDpUrlAllowed("http://169.254.169.254/crl")).isFalse();
            assertThat(AMCRLStore.isCrlDpUrlAllowed("http://198.51.100.7/crl")).isTrue();
        });
    }

    @Test
    public void ipv6TransitionAddressesToABlockedIpv4AddressAreRejected() {
        // These three carry an IPv4 address that a packet sent to them actually reaches, so each
        // is a spelling of the metadata endpoint the advisory names. 64:ff9b::/96 is not exotic:
        // it is what an IPv6-only host behind NAT64 uses to reach the IPv4 internet.
        assertThat(AMCRLStore.isCrlDpUrlAllowed("http://[64:ff9b::169.254.169.254]/crl"))
                .as("NAT64 well-known prefix, RFC 6052").isFalse();
        assertThat(AMCRLStore.isCrlDpUrlAllowed("http://[2002:a9fe:a9fe::]/crl"))
                .as("6to4, RFC 3056").isFalse();
        assertThat(AMCRLStore.isCrlDpUrlAllowed("http://[::169.254.169.254]/crl"))
                .as("IPv4-compatible, RFC 4291").isFalse();
        assertThat(AMCRLStore.isCrlDpUrlAllowed("http://[64:ff9b::127.0.0.1]/crl")).isFalse();
    }

    @Test
    public void ipv6TransitionAddressToAPublicIpv4AddressIsAccepted() {
        // The embedded address is what decides, not the transition prefix: a CRL published behind
        // NAT64 must still be reachable.
        assertThat(AMCRLStore.isCrlDpUrlAllowed("http://[64:ff9b::198.51.100.7]/crl")).isTrue();
        assertThat(AMCRLStore.isCrlDpUrlAllowed("http://[2002:c633:6407::]/crl")).isTrue();
    }

    @Test
    public void ipv4LiteralsThatSpellABlockedAddressAnotherWayAreRejected() {
        // Regression locks. All three already behave correctly because InetAddress normalises
        // them, but nothing pinned that, and each is a standard SSRF filter bypass.
        assertThat(AMCRLStore.isCrlDpUrlAllowed("http://[::ffff:169.254.169.254]/crl")).isFalse();
        assertThat(AMCRLStore.isCrlDpUrlAllowed("http://2852039166/crl")).isFalse();
        assertThat(AMCRLStore.isCrlDpUrlAllowed("http://user@169.254.169.254/crl")).isFalse();
        assertThat(AMCRLStore.isCrlDpUrlAllowed("http://127.1/crl")).isFalse();
    }

    @Test
    public void escapeHatchDoesNotAdmitAMissingHost() throws Exception {
        // The escape hatch short-circuits the address policy; it must not also skip the check
        // that there is a host to connect to at all, or a hostless URL reaches the LDAP fetcher.
        withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () -> {
            assertThat(AMCRLStore.isCrlDpHostAllowed(null)).isFalse();
            assertThat(AMCRLStore.isCrlDpHostAllowed("")).isFalse();
        });
    }

    @Test
    public void privateAddressBehindATransitionAddressFollowsThePrivateAddressPolicy()
            throws Exception {
        // 6to4 onto 10.0.0.5: the default mode admits it as it admits 10.0.0.5 itself, and strict
        // mode refuses it for the same reason.
        assertThat(AMCRLStore.isCrlDpUrlAllowed("http://[2002:a00:5::]/crl")).isTrue();
        withSystemProperty(AMCRLStore.CRL_DP_STRICT, "true", () ->
                assertThat(AMCRLStore.isCrlDpUrlAllowed("http://[2002:a00:5::]/crl")).isFalse());
    }

    @Test
    public void sharedAddressSpaceIsRejectedByDefault() {
        // 100.64.0.0/10 holds a cloud metadata endpoint. It is not private address space, so the
        // default mode's allowance for an internal PKI does not reach it.
        assertThat(AMCRLStore.isCrlDpUrlAllowed("http://100.100.100.200/crl")).isFalse();
        assertThat(AMCRLStore.isCrlDpUrlAllowed("http://[2002:6464:64c8::]/crl")).isFalse();
    }

    @Test
    public void ipv6CloudMetadataAddressIsRejectedByDefault() {
        // AWS's IPv6 instance-metadata endpoint sits inside fc00::/7, which the default mode
        // admits for an internal PKI. The endpoint itself stays refused.
        assertThat(AMCRLStore.isCrlDpUrlAllowed(
                "http://[fd00:ec2::254]/latest/meta-data/iam/security-credentials/")).isFalse();
    }
}

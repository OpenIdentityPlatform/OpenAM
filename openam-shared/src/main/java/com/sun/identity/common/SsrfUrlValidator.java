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
 * Copyright 2026 3A Systems LLC.
 */
package com.sun.identity.common;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;

/**
 * Validates that a (client-supplied) URL is safe for the server to fetch, to prevent
 * server-side request forgery (SSRF).
 *
 * <p>A URL is considered safe only when it uses the {@code http} or {@code https} scheme (which
 * rules out {@code file://}, {@code ftp://}, {@code gopher://} and similar variants) and none of
 * the host's resolved addresses point at a blocked target. Blocked targets are:
 *
 * <ul>
 *   <li>loopback ({@code 127.0.0.0/8}, {@code ::1}), wildcard ({@code 0.0.0.0}, {@code ::}),
 *       link-local (incl. cloud metadata {@code 169.254.0.0/16} and {@code fe80::/10}),
 *       private/site-local ({@code 10/8}, {@code 172.16/12}, {@code 192.168/16}) and multicast
 *       addresses, as reported by {@link java.net.InetAddress};</li>
 *   <li>IPv6 unique-local addresses ({@code fc00::/7}), not reported by
 *       {@code isSiteLocalAddress()};</li>
 *   <li>IPv4 special-purpose ranges that {@code InetAddress.is*Address()} does not flag:
 *       {@code 0.0.0.0/8} (RFC 1122), {@code 100.64.0.0/10} shared address space (RFC 6598, incl.
 *       the {@code 100.100.100.200} cloud metadata endpoint), {@code 192.0.0.0/24} (RFC 6890),
 *       {@code 192.88.99.0/24} (RFC 7526), {@code 198.18.0.0/15} (RFC 2544) and {@code 240.0.0.0/4}
 *       reserved incl. the {@code 255.255.255.255} broadcast;</li>
 *   <li>the whole NAT64 local-use prefix {@code 64:ff9b:1::/48} (RFC 8215). Unlike the Well-Known
 *       Prefix, which RFC 6052 §3.1 forbids using for non-global IPv4, this prefix exists
 *       precisely to translate non-global IPv4, and the operator picks the sub-prefix inside the
 *       {@code /48} — so the embedding offset is not knowable from the literal. It is
 *       translation-only space with no legitimate fetch target, so all of it is blocked rather
 *       than only one sub-prefix;</li>
 *   <li>the IPv4 address embedded in an IPv6 transition host literal — NAT64 Well-Known Prefix
 *       ({@code 64:ff9b::/96}, RFC 6052 §2.1), 6to4 ({@code 2002::/16}), Teredo
 *       ({@code 2001:0000::/32}), ISATAP (interface identifier {@code 0000:5efe} /
 *       {@code 0200:5efe}, RFC 5214) and IPv4-mapped/compatible ({@code ::ffff:0:0/96},
 *       {@code ::/96}) — which is extracted and re-checked against every rule above, so an
 *       internal target cannot be smuggled in through a transition address. A literal may embed
 *       more than one candidate (e.g. a 6to4 prefix with an ISATAP interface identifier); every
 *       candidate is checked.</li>
 *   <li>the IPv4 address embedded under an operator-configured NAT64 Network-Specific Prefix, see
 *       {@link #NAT64_PREFIXES_PROPERTY}.</li>
 * </ul>
 *
 * <p>NAT64 Network-Specific Prefixes (RFC 6052 §2.2) are chosen by the operator out of the
 * operator's own address space and are bit-for-bit indistinguishable from an ordinary global
 * address, so they cannot be recognised from the literal alone. Deployments that run a NAT64 with
 * an NSP must declare it through {@link #NAT64_PREFIXES_PROPERTY}; nothing is inferred.
 *
 * <p>Note that the check is applied to the addresses the host resolves to at validation time. A
 * name that resolves to a different address when the URL is later fetched (DNS rebinding) is out
 * of scope here and has to be handled by the fetching code.
 *
 * <p>Apart from {@link #NAT64_PREFIXES_PROPERTY}, which can only ever add blocked ranges, this
 * class carries no configuration of its own: callers that need a runtime "allow any URL" escape
 * hatch read their own system property and short-circuit before calling
 * {@link #isSafeRemoteUrl(String)}.
 */
public final class SsrfUrlValidator {

    /**
     * Comma-separated list of NAT64 Network-Specific Prefixes (RFC 6052 §2.2) in use by this
     * deployment, each written as {@code <ipv6-prefix>/<length>} with a length of 32, 40, 48, 56,
     * 64 or 96 bits — for example
     * {@code org.openidentityplatform.ssrf.nat64-prefixes=2001:db8:122:344::/64,2001:db8:1::/48}.
     *
     * <p>The IPv4 address embedded under each declared prefix is extracted per RFC 6052 §2.2 and
     * re-checked, which closes the NAT64 bypass for deployments that do not use the Well-Known
     * Prefix. An NSP cannot be inferred from an address literal, so without this property such
     * prefixes are simply not recognised — the property can only ever block more, never less.
     *
     * <p>Unparsable entries are skipped with a warning in the {@code amSSRF} debug log; the
     * remaining entries and all built-in rules still apply.
     */
    public static final String NAT64_PREFIXES_PROPERTY =
            "org.openidentityplatform.ssrf.nat64-prefixes";

    private static final String DEBUG_NAME = "amSSRF";

    /** NAT64 Well-Known Prefix 64:ff9b::/96 (RFC 6052 §2.1). */
    private static final Nat64Prefix WELL_KNOWN_PREFIX = new Nat64Prefix(
            new byte[]{0x00, 0x64, (byte) 0xff, (byte) 0x9b, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, 96);

    private static final Nat64Prefix[] NO_PREFIXES = new Nat64Prefix[0];

    /** Last seen raw value of {@link #NAT64_PREFIXES_PROPERTY} together with its parsed form. */
    private static volatile ParsedPrefixes parsedPrefixes = new ParsedPrefixes(null, NO_PREFIXES);

    private SsrfUrlValidator() {
    }

    /**
     * Equivalent to {@link #isSafeRemoteUrl(String, boolean) isSafeRemoteUrl(url, false)}:
     * both {@code http} and {@code https} URLs are accepted.
     *
     * @param url the URL to check.
     * @return {@code true} if the URL is safe for the server to fetch, {@code false} otherwise.
     */
    public static boolean isSafeRemoteUrl(String url) {
        return isSafeRemoteUrl(url, false);
    }

    /**
     * @param url the URL to check.
     * @param requireHttps when {@code true} only the {@code https} scheme is accepted; when
     *                     {@code false} both {@code http} and {@code https} are accepted.
     * @return {@code true} if the URL is safe for the server to fetch, {@code false} otherwise.
     */
    public static boolean isSafeRemoteUrl(String url, boolean requireHttps) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        final URL parsed;
        try {
            parsed = new URL(url);
        } catch (MalformedURLException e) {
            return false;
        }
        final String protocol = parsed.getProtocol();
        final boolean allowedScheme = "https".equalsIgnoreCase(protocol)
                || (!requireHttps && "http".equalsIgnoreCase(protocol));
        if (!allowedScheme) {
            return false;
        }
        final String host = parsed.getHost();
        if (host == null || host.isEmpty()) {
            return false;
        }
        try {
            final InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses.length == 0) {
                return false;
            }
            for (InetAddress address : addresses) {
                if (isBlockedAddress(address)) {
                    return false;
                }
            }
        } catch (UnknownHostException e) {
            return false;
        }
        return true;
    }

    private static boolean isBlockedAddress(InetAddress address) {
        if (address.isLoopbackAddress()          // 127.0.0.0/8, ::1
                || address.isAnyLocalAddress()   // 0.0.0.0, ::
                || address.isLinkLocalAddress()  // 169.254.0.0/16, fe80::/10
                || address.isSiteLocalAddress()  // 10/8, 172.16/12, 192.168/16, fec0::/10
                || address.isMulticastAddress()) {
            return true;
        }
        final byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            return isBlockedIpv4(bytes);
        }
        if (bytes.length != 16) {
            return false;
        }
        // IPv6 Unique Local Addresses (fc00::/7) are not reported by isSiteLocalAddress().
        if ((bytes[0] & 0xfe) == 0xfc) {
            return true;
        }
        // NAT64 local-use prefix 64:ff9b:1::/48 (RFC 8215) is translation-only space, and the
        // operator picks the sub-prefix inside it, so the embedding offset is not knowable.
        // Nothing in that /48 is a legitimate fetch target, so block it outright rather than only
        // the addresses whose embedded IPv4 we happen to be able to locate.
        if (bytes[0] == 0x00 && bytes[1] == 0x64
                && bytes[2] == (byte) 0xff && bytes[3] == (byte) 0x9b
                && bytes[4] == 0x00 && bytes[5] == 0x01) {
            return true;
        }
        // IPv6 transition addresses embed an IPv4 address that InetAddress.is*Address() ignores,
        // so an internal IPv4 target (private, loopback, link-local, ...) can be reached through a
        // NAT64, 6to4, Teredo, ISATAP or IPv4-mapped/compatible host literal. Re-check every
        // embedded IPv4 the literal can be read as.
        for (byte[] embedded : embeddedIpv4Addresses(bytes)) {
            try {
                if (isBlockedAddress(InetAddress.getByAddress(embedded))) {
                    return true;
                }
            } catch (UnknownHostException e) {
                return true; // fail closed; a 4-byte address is always accepted by getByAddress().
            }
        }
        return false;
    }

    /**
     * Blocks the IPv4 special-purpose ranges (IANA IPv4 Special-Purpose Address Registry) that no
     * {@link InetAddress} {@code is*Address()} predicate reports but which still resolve to hosts
     * that must not be reachable. This is reached both directly (a plain IPv4 literal) and via
     * {@link #embeddedIpv4Addresses(byte[])} for transition addresses, so a gap here would re-open
     * the transition-address bypass as well.
     *
     * @param bytes a 4-byte IPv4 address.
     */
    private static boolean isBlockedIpv4(byte[] bytes) {
        final int b0 = bytes[0] & 0xff;
        final int b1 = bytes[1] & 0xff;
        final int b2 = bytes[2] & 0xff;
        return b0 == 0                                 // 0.0.0.0/8 this host on this network (RFC 1122)
                || (b0 == 100 && (b1 & 0xc0) == 0x40)  // 100.64.0.0/10 shared address space (RFC 6598)
                || (b0 == 192 && b1 == 0 && b2 == 0)   // 192.0.0.0/24 IETF protocol assignments (RFC 6890)
                || (b0 == 192 && b1 == 88 && b2 == 99) // 192.88.99.0/24 deprecated 6to4 relay anycast (RFC 7526)
                || (b0 == 198 && (b1 & 0xfe) == 18)    // 198.18.0.0/15 benchmarking (RFC 2544)
                || (b0 & 0xf0) == 0xf0;                // 240.0.0.0/4 reserved + 255.255.255.255 broadcast
    }

    /**
     * Collects every IPv4 address the given 16-byte IPv6 address can be read as under a known
     * transition scheme. A literal can match more than one scheme at a time — a 6to4 prefix
     * carries its IPv4 in the prefix while the interface identifier may independently carry an
     * ISATAP one — and either of them is a reachable target, so all candidates are returned
     * rather than only the first match.
     *
     * @return the embedded IPv4 addresses, empty when the literal carries none.
     */
    private static List<byte[]> embeddedIpv4Addresses(byte[] bytes) {
        List<byte[]> candidates = null;

        // NAT64 Well-Known Prefix 64:ff9b::/96 (RFC 6052 §2.1), plus any operator-declared
        // Network-Specific Prefix, which may use any of the RFC 6052 §2.2 embedding lengths.
        if (matchesPrefix(bytes, WELL_KNOWN_PREFIX)) {
            candidates = add(candidates, extractRfc6052(bytes, WELL_KNOWN_PREFIX.lengthBits));
        }
        for (Nat64Prefix prefix : configuredNat64Prefixes()) {
            if (matchesPrefix(bytes, prefix)) {
                candidates = add(candidates, extractRfc6052(bytes, prefix.lengthBits));
            }
        }
        // 6to4 2002::/16 (RFC 3056): IPv4 in bytes 2..5.
        if (bytes[0] == 0x20 && bytes[1] == 0x02) {
            candidates = add(candidates, new byte[]{bytes[2], bytes[3], bytes[4], bytes[5]});
        }
        // Teredo 2001:0000::/32 (RFC 4380): client IPv4 in the low 32 bits, stored one's-complemented.
        if (bytes[0] == 0x20 && bytes[1] == 0x01 && bytes[2] == 0x00 && bytes[3] == 0x00) {
            candidates = add(candidates, new byte[]{(byte) ~bytes[12], (byte) ~bytes[13],
                    (byte) ~bytes[14], (byte) ~bytes[15]});
        }
        // ISATAP (RFC 5214 §6.1): interface identifier 0000:5efe (private IPv4) or 0200:5efe
        // (global IPv4) followed by the IPv4 address. The marker sits in the interface identifier,
        // so unlike a NAT64 NSP this is recognisable under any /64 prefix.
        if (bytes[9] == 0x00 && bytes[10] == 0x5e && bytes[11] == (byte) 0xfe
                && (bytes[8] == 0x00 || bytes[8] == 0x02)) {
            candidates = add(candidates, new byte[]{bytes[12], bytes[13], bytes[14], bytes[15]});
        }
        // IPv4-mapped ::ffff:0:0/96 and (deprecated) IPv4-compatible ::/96: IPv4 in the low 32 bits.
        if (allZero(bytes, 0, 10)
                && ((bytes[10] == (byte) 0xff && bytes[11] == (byte) 0xff)
                    || (bytes[10] == 0x00 && bytes[11] == 0x00))) {
            candidates = add(candidates, new byte[]{bytes[12], bytes[13], bytes[14], bytes[15]});
        }
        return (candidates == null) ? Collections.<byte[]>emptyList() : candidates;
    }

    /** Appends to a candidate list that is only allocated once there is something to put in it. */
    private static List<byte[]> add(List<byte[]> candidates, byte[] address) {
        final List<byte[]> result = (candidates == null) ? new ArrayList<byte[]>(2) : candidates;
        result.add(address);
        return result;
    }

    /**
     * Extracts the embedded IPv4 address for the RFC 6052 §2.2 embedding of the given prefix
     * length. Note the reserved u-octet at bytes[8], which is skipped by every length below 96.
     * The u-octet is not required to be zero: a translator that ignores it would still forward the
     * request, so reading the address is the safer choice.
     *
     * @param prefixLengthBits one of the lengths accepted by {@link #isEmbeddingLength(int)}.
     */
    private static byte[] extractRfc6052(byte[] bytes, int prefixLengthBits) {
        switch (prefixLengthBits) {
        case 32:
            return new byte[]{bytes[4], bytes[5], bytes[6], bytes[7]};
        case 40:
            return new byte[]{bytes[5], bytes[6], bytes[7], bytes[9]};
        case 48:
            return new byte[]{bytes[6], bytes[7], bytes[9], bytes[10]};
        case 56:
            return new byte[]{bytes[7], bytes[9], bytes[10], bytes[11]};
        case 64:
            return new byte[]{bytes[9], bytes[10], bytes[11], bytes[12]};
        case 96:
            return new byte[]{bytes[12], bytes[13], bytes[14], bytes[15]};
        default:
            throw new IllegalArgumentException("no RFC 6052 embedding for /" + prefixLengthBits);
        }
    }

    /** The prefix lengths for which RFC 6052 §2.2 defines an IPv4 embedding. */
    private static boolean isEmbeddingLength(int prefixLengthBits) {
        return prefixLengthBits == 32 || prefixLengthBits == 40 || prefixLengthBits == 48
                || prefixLengthBits == 56 || prefixLengthBits == 64 || prefixLengthBits == 96;
    }

    private static boolean matchesPrefix(byte[] address, Nat64Prefix prefix) {
        final int fullBytes = prefix.lengthBits / 8;
        for (int i = 0; i < fullBytes; i++) {
            if (address[i] != prefix.prefix[i]) {
                return false;
            }
        }
        final int remainingBits = prefix.lengthBits % 8;
        if (remainingBits != 0) {
            final int mask = 0xff << (8 - remainingBits);
            return ((address[fullBytes] ^ prefix.prefix[fullBytes]) & mask) == 0;
        }
        return true;
    }

    /**
     * Returns the NAT64 Network-Specific Prefixes declared through {@link #NAT64_PREFIXES_PROPERTY},
     * parsing the property value only when it has changed since the previous call so that a
     * malformed entry is reported once rather than on every validation.
     */
    private static Nat64Prefix[] configuredNat64Prefixes() {
        String raw = SystemPropertiesManager.get(NAT64_PREFIXES_PROPERTY);
        if (raw == null) {
            // No ISystemProperties provider on the classpath (openam-shared used standalone):
            // fall back to the JVM properties so that -D still works.
            raw = System.getProperty(NAT64_PREFIXES_PROPERTY);
        }
        final ParsedPrefixes cached = parsedPrefixes;
        if (cached.matches(raw)) {
            return cached.prefixes;
        }
        final ParsedPrefixes fresh = new ParsedPrefixes(raw, parseNat64Prefixes(raw));
        parsedPrefixes = fresh;
        return fresh.prefixes;
    }

    private static Nat64Prefix[] parseNat64Prefixes(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return NO_PREFIXES;
        }
        final List<Nat64Prefix> prefixes = new ArrayList<Nat64Prefix>();
        for (String entry : raw.split(",")) {
            final String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            final Nat64Prefix prefix = parseNat64Prefix(trimmed);
            if (prefix == null) {
                Debug.getInstance(DEBUG_NAME).warning("SsrfUrlValidator: ignoring invalid "
                        + NAT64_PREFIXES_PROPERTY + " entry '" + trimmed + "'; expected an IPv6 "
                        + "prefix with a length of 32, 40, 48, 56, 64 or 96 bits, e.g. "
                        + "2001:db8:122:344::/64");
            } else {
                prefixes.add(prefix);
            }
        }
        return prefixes.toArray(new Nat64Prefix[prefixes.size()]);
    }

    /**
     * Parses a single {@code <ipv6-prefix>/<length>} entry, or returns {@code null} when it is not
     * a well-formed IPv6 prefix with an RFC 6052 §2.2 embedding length.
     */
    private static Nat64Prefix parseNat64Prefix(String entry) {
        final int slash = entry.lastIndexOf('/');
        if (slash <= 0 || slash == entry.length() - 1) {
            return null;
        }
        final int lengthBits;
        try {
            lengthBits = Integer.parseInt(entry.substring(slash + 1).trim());
        } catch (NumberFormatException e) {
            return null;
        }
        if (!isEmbeddingLength(lengthBits)) {
            return null;
        }
        final byte[] prefix;
        try {
            // Bracketed form so that a malformed value fails as an invalid IPv6 literal instead of
            // being looked up in DNS.
            prefix = InetAddress.getByName("[" + entry.substring(0, slash).trim() + "]").getAddress();
        } catch (UnknownHostException e) {
            return null;
        }
        return (prefix.length == 16) ? new Nat64Prefix(prefix, lengthBits) : null;
    }

    private static boolean allZero(byte[] bytes, int fromInclusive, int toExclusive) {
        for (int i = fromInclusive; i < toExclusive; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }
        return true;
    }

    /** An IPv6 prefix under which an IPv4 address is embedded per RFC 6052 §2.2. */
    private static final class Nat64Prefix {

        private final byte[] prefix;
        private final int lengthBits;

        Nat64Prefix(byte[] prefix, int lengthBits) {
            this.prefix = prefix;
            this.lengthBits = lengthBits;
        }
    }

    /** The parsed form of a raw {@link #NAT64_PREFIXES_PROPERTY} value. */
    private static final class ParsedPrefixes {

        private final String raw;
        private final Nat64Prefix[] prefixes;

        ParsedPrefixes(String raw, Nat64Prefix[] prefixes) {
            this.raw = raw;
            this.prefixes = prefixes;
        }

        boolean matches(String candidate) {
            return (raw == null) ? (candidate == null) : raw.equals(candidate);
        }
    }
}

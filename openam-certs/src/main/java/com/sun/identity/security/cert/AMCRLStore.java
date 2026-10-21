/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: AMCRLStore.java,v 1.7 2009/01/28 05:35:12 ww203982 Exp $
 *
 * Portions Copyrighted 2013-2016 ForgeRock AS.
 * Portions Copyrighted 2023-2026 3A Systems, LLC.
 */
package com.sun.identity.security.cert;

import static org.forgerock.openam.utils.Time.*;
import static org.forgerock.util.time.Duration.duration;

import com.iplanet.security.x509.CertUtils;
import com.iplanet.security.x509.IssuingDistributionPointExtension;
import com.sun.identity.common.HttpURLConnectionManager;
import com.sun.identity.common.SsrfUrlValidator;
import com.sun.identity.security.keystore.AMX509TrustManager;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.encode.URLEncDec;
import sun.security.util.DerValue;
import sun.security.x509.CRLDistributionPointsExtension;
import sun.security.x509.DistributionPoint;
import sun.security.x509.GeneralName;
import sun.security.x509.GeneralNameInterface;
import sun.security.x509.GeneralNames;
import sun.security.x509.PKIXExtensions;
import sun.security.x509.URIName;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.security.auth.x500.X500Principal;

import org.apache.commons.lang3.ArrayUtils;
import org.forgerock.i18n.LocalizedIllegalArgumentException;
import org.forgerock.openam.ldap.LDAPRequests;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.LDAPConnectionFactory;
import org.forgerock.opendj.ldap.LDAPUrl;
import org.forgerock.opendj.ldap.ModificationType;
import org.forgerock.opendj.ldap.SSLContextBuilder;
import org.forgerock.opendj.ldap.requests.ModifyRequest;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldif.ConnectionEntryReader;
import org.forgerock.util.Options;
import org.forgerock.util.time.Duration;

/**
 * The class is used to manage crl store in LDAP server
 * This class does get crl and update crl with CRLDistribution
 * PointsExtension in client certificate or IssuingDistribution
 * PointExtension in CRL. This class should be used
 * in order to manage CRL store in LDAP
 * <pre>
 * id-ce-cRLDistributionPoints OBJECT IDENTIFIER ::=  { id-ce 31 }
 *
 * RLDistributionPoints ::= SEQUENCE SIZE (1..MAX) OF DistributionPoint
 *
 * DistributionPoint ::= SEQUENCE {
 *        distributionPoint       [0]     DistributionPointName OPTIONAL,
 *        reasons                 [1]     ReasonFlags OPTIONAL,
 *        cRLIssuer               [2]     GeneralNames OPTIONAL }
 *
 * DistributionPointName ::= CHOICE {
 *        fullName                [0]     GeneralNames,
 *        nameRelativeToCRLIssuer [1]     RelativeDistinguishedName }
 *
 * ReasonFlags ::= BIT STRING {
 *        unused                  (0),
 *        keyCompromise           (1),
 *        cACompromise            (2),
 *        affiliationChanged      (3),
 *        superseded              (4),
 *        cessationOfOperation    (5),
 *        certificateHold         (6),
 *        privilegeWithdrawn      (7),
 *        aACompromise            (8) }
 * </pre>
 *
 * <h2>Fetching CRLs named by an untrusted certificate</h2>
 *
 * The distribution point comes out of the certificate being validated, so this class will make
 * an outbound request to a location someone else chose (GHSA-g5vw-v384-gw9v). Callers are
 * expected to have established that the certificate chains to a configured trust anchor first —
 * {@code Cert.doJCERevocationValidation} and {@code CRLValidator} both do.
 *
 * <p>That check is deliberately <em>not</em> repeated here. Callers validate a whole chain once
 * and then ask for the CRL of each certificate in it; re-validating each certificate on its own
 * would be a stricter test that rejects the intermediates and the root of a chain that has
 * already passed, and revocation would then fail closed. The protection that does live here
 * applies to every caller, in-tree or not:
 *
 * <ul>
 *   <li>the address policy ({@link #isCrlDpUrlAllowed}) on both the HTTP and the LDAP sink,
 *       applied again to every redirect target and never following one out of {@code https};</li>
 *   <li>a {@link FetchBudget} — a size cap, a wall-clock budget and a cap on how many locations
 *       are tried, all belonging to the certificate rather than to a location or a hop;</li>
 *   <li>{@link #CRL_DP_PARAM_HOSTS}, so the deployment's own HTTP parameters are not posted to a
 *       location the certificate chose;</li>
 *   <li>{@link #validateFetchedCrl}, so nothing unverified reaches the shared CRL stores.</li>
 * </ul>
 *
 * <p>So what an out-of-tree caller can still provoke by passing an untrusted certificate is a
 * bounded, address-filtered request to a public address. To also get a CRL <em>stored</em>, it
 * would have to supply a chain in which the CRL's issuer traces to a configured trust anchor
 * through certificates that were each authorised to issue the next — see
 * {@link #findIssuerPublicKeys(X509Certificate, X509Certificate[])}. That walk is not a full PKIX
 * path validation: it does not check validity dates, name constraints or whether the
 * intermediates are themselves revoked. Pass a chain you have validated.
 */

public class AMCRLStore extends AMCertStore {

    public static final String CERTIFICATE_REVOCATION_LIST = "certificaterevocationlist";
    public static final String CERTIFICATE_REVOCATION_LIST_BINARY = CERTIFICATE_REVOCATION_LIST + ";binary";
    /** Maximum number of CRLs held in the JVM-wide in-memory CRL cache. */
    static final String CRL_CACHE_MAX_SIZE = "org.openidentityplatform.openam.crl.cache.max-size";

    private static final int DEFAULT_CRL_CACHE_MAX_SIZE = 1000;

    // In memory CRL cache
    private static final Map<String, X509CRL> cachedcrls = createCrlCache();

    /**
     * Trust store lookups by subject name, see {@link #trustedIssuersOf(X500Principal)}.
     *
     * <p>Bounded and least-recently-used for the same reason {@link #createCrlCache()} is: the
     * names come from certificates presented to the server, including every hop of a chain a
     * caller supplied, so an unbounded map would grow with the issuers a long-running server
     * happens to be shown. The bound is fixed rather than configurable — this holds a handful of
     * public keys per entry and exists only to keep a hot loop off the keystore.
     */
    private static final int TRUSTED_ISSUER_CACHE_MAX_SIZE = 256;

    /**
     * The memoised lookups and the trust store they were read from, held together so that
     * replacing one replaces the other. Two fields would have to be cleared and reassigned in
     * turn, and a thread preempted between the two steps would repopulate the map it had already
     * emptied with entries derived from the keystore that was just swapped out — a stale answer
     * that then survives for the life of the process.
     */
    private static final class TrustedIssuerCache {

        private final KeyStore trustStore;
        private final Map<X500Principal, List<X509Certificate>> issuers =
                Collections.synchronizedMap(
                        new LinkedHashMap<X500Principal, List<X509Certificate>>(16, 0.75f, true) {
                            @Override
                            protected boolean removeEldestEntry(
                                    Map.Entry<X500Principal, List<X509Certificate>> eldest) {
                                return size() > TRUSTED_ISSUER_CACHE_MAX_SIZE;
                            }
                        });

        TrustedIssuerCache(KeyStore trustStore) {
            this.trustStore = trustStore;
        }
    }

    /** The trust store lookups, keyed on the keystore they came from, compared by identity. */
    private static volatile TrustedIssuerCache trustedIssuers;

    /**
     * Creates the in-memory CRL cache: least-recently-used and bounded by
     * {@link #CRL_CACHE_MAX_SIZE}. The cache is keyed on issuer DN and shared JVM-wide across
     * realms, so certificates from unrelated issuers would otherwise grow it without limit.
     */
    static Map<String, X509CRL> createCrlCache() {
        return Collections.synchronizedMap(new LinkedHashMap<String, X509CRL>(16, 0.75f, true) {
            /**
             * Read on each insertion until the configuration answers, then remembered: this runs
             * inside the map's monitor on the authentication path, and the limit does not change
             * once a deployment's own configuration is readable. Not read at construction — the
             * JVM-wide cache is built during class initialisation, before the OpenAM
             * configuration is — and, for the same reason, the fallback is not what gets
             * remembered: a first certificate authentication that happens before the
             * configuration provider is installed (bootstrap, upgrade, an embedded container)
             * would otherwise freeze the default and never read crl.cache.max-size again.
             */
            private long maxSize;

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, X509CRL> eldest) {
                if (maxSize == 0) {
                    maxSize = configuredCrlCacheMaxSize();
                }
                return size() > ((maxSize == 0) ? DEFAULT_CRL_CACHE_MAX_SIZE : maxSize);
            }
        });
    }

    /**
     * @return the configured bound on the CRL cache, or 0 when nothing has been configured for it
     *     yet — which is not the same answer as the default, and must not be remembered as one.
     */
    static long configuredCrlCacheMaxSize() {
        return (getProperty(CRL_CACHE_MAX_SIZE) == null) ? 0
                : getLongProperty(CRL_CACHE_MAX_SIZE, DEFAULT_CRL_CACHE_MAX_SIZE);
    }

    /**
     * When {@code true}, CRL Distribution Point URLs are not address-checked at all, restoring
     * the pre-fix behaviour. Escape hatch for deployments whose CRL DPs sit on an address the
     * default policy blocks. Default {@code false}.
     */
    static final String CRL_DP_ALLOW_ANY_URL = "org.openidentityplatform.openam.crl.dp.allow-any-url";

    /**
     * When {@code true}, private/site-local and IPv6 unique-local addresses are blocked as CRL
     * Distribution Points too, i.e. the full {@link SsrfUrlValidator} policy. Default
     * {@code false}, because an internal PKI legitimately publishes its CRLs on RFC1918
     * addresses and blocking them on upgrade would break working deployments.
     */
    static final String CRL_DP_STRICT = "org.openidentityplatform.openam.crl.dp.strict";

    /**
     * When {@code true} (the default), a CRL fetched from a distribution point is only stored
     * once its signature has been verified against the issuer.
     *
     * <p>Set to {@code false} for a PKI whose CRLs are signed under a rotated CA key: there the
     * signing key is not the one this code can reach, so a perfectly good CRL looks exactly like a
     * forged one and would be discarded, leaving the deployment on a stale CRL until it expires
     * and then failing closed. Only the signature check is relaxed — the issuer and CRL-number
     * checks still apply, and PKIX still verifies any CRL it is handed. An indirect CRL from a
     * delegated CRL signer (RFC 5280 section 5.2.5) is never stored, whatever this is set to.
     */
    static final String CRL_REQUIRE_VERIFIED_SIGNATURE =
            "org.openidentityplatform.openam.crl.require-verified-signature";

    /**
     * Comma-separated list of host names the HTTP parameters configured in
     * {@code iplanet-am-auth-cert-param-get-crl} may be sent to. Empty by default, which means
     * they are sent to none.
     *
     * <p>Those parameters are the deployment's own configuration and routinely carry a
     * credential for its CRL endpoint, while the URL they are posted to comes out of the
     * certificate being validated. Without a list naming the deployment's own distribution
     * points, a certificate that names someone else's URL has the server post that credential
     * there (GHSA-g5vw-v384-gw9v). Matching is on the host alone, case-insensitively; a leading
     * {@code .} matches any host in the domain below it, so {@code .example.com} covers
     * {@code pki.example.com}.
     */
    static final String CRL_DP_PARAM_HOSTS = "org.openidentityplatform.openam.crl.dp.param-hosts";

    /** Largest CRL, in bytes, that will be read from a distribution point. */
    static final String CRL_DP_MAX_SIZE = "org.openidentityplatform.openam.crl.dp.max-size";

    /**
     * Most distribution point locations that will be tried for one certificate. Each one costs a
     * connect and a download budget, and the search runs once per certificate in the chain, so
     * the list a certificate carries has to be bounded somewhere.
     */
    static final String CRL_DP_MAX_LOCATIONS =
            "org.openidentityplatform.openam.crl.dp.max-locations";

    /** Wall-clock budget, in milliseconds, for downloading a CRL from a distribution point. */
    static final String CRL_DP_TIMEOUT = "org.openidentityplatform.openam.crl.dp.timeout";

    /**
     * How many redirects a distribution point may send the fetcher through. Not configurable:
     * a legitimate CDN or http-to-https front end needs one or two, and every extra hop is
     * another request an unauthenticated caller can make the server issue.
     */
    private static final int MAX_CRL_DP_REDIRECTS = 3;

    private static final long DEFAULT_CRL_DP_MAX_SIZE = 10L * 1024 * 1024;
    // This runs on the authentication path, so the budget is what a user will wait, not what a
    // batch job will.
    private static final long DEFAULT_CRL_DP_TIMEOUT = 10000L;
    private static final long DEFAULT_CRL_DP_MAX_LOCATIONS = 5L;

    /** Bit positions in {@link X509Certificate#getKeyUsage()}, RFC 5280 4.2.1.3. */
    private static final int KEY_USAGE_KEY_CERT_SIGN = 5;
    private static final int KEY_USAGE_CRL_SIGN = 6;

    /** OID of the {@code CRLNumber} extension, RFC 5280 section 5.2.3. */
    private static final String CRL_NUMBER_OID = "2.5.29.20";

    private String mCrlAttrName = null;

    /**
     * Supplies the certificate chain a fetched CRL may be verified against.
     *
     * <p>An interface rather than the chain itself so that a caller which has to go to a directory
     * to assemble it is only made to do so when a CRL was actually fetched.
     */
    interface ChainResolver {
        /** @return the chain the certificate was presented with, possibly null. */
        X509Certificate[] chain();
    }

    /** What may be done with a CRL that was just fetched from a distribution point. */
    enum CrlUpdateDecision {
        /** Verified against the issuer and no older than the stored CRL: use it and store it. */
        STORE,
        /** Cannot be verified here: use it for this validation only, never store it. */
        USE_ONLY,
        /** Not usable: keep whatever is already stored. */
        DISCARD
    }

    /**
     * Decides what may be done with a CRL just fetched from a distribution point.
     *
     * <p>A fetched CRL is unauthenticated data from a location named by the certificate under
     * validation. PKIX re-verifies any CRL it is handed, so an invalid one cannot fake a
     * revocation — but until this check, both the JVM-wide CRL cache and the LDAP CRL entry were
     * written from it unconditionally, keyed on issuer DN. That let one request leave a CRL
     * behind that locks out every principal under that issuer (PKIX drops it, revocation status
     * becomes undetermined, validation fails closed) and, because a superseded CRL was accepted
     * as an update, it also let a genuine but older CRL un-revoke a certificate.
     *
     * @param certificate the certificate whose revocation status is being checked.
     * @param fetched the CRL just retrieved from the distribution point, possibly null.
     * @param current the CRL already held for this issuer, possibly null.
     * @param issuerKeys every public key the issuer may be holding, empty if none could be
     *     determined. More than one is normal while a CA is rolling its key over.
     */
    static CrlUpdateDecision validateFetchedCrl(X509Certificate certificate, X509CRL fetched,
            X509CRL current, Collection<PublicKey> issuerKeys) {
        return validateFetchedCrl(certificate, fetched, current, () -> issuerKeys,
                getCRLDPExt(certificate));
    }

    /**
     * As {@link #validateFetchedCrl(X509Certificate, X509CRL, X509CRL, Collection)}, for the
     * caller that already holds the certificate's distribution point extension and can only
     * establish the issuer's keys at a price.
     *
     * @param issuerKeys asked for once, and only by the branch that reads a key. Answering it
     *     means resolving the chain — a whole-subtree LDAP search, on the unauthenticated SAML2
     *     signature path — and then walking it to an anchor, while the two tests above it reject
     *     a CRL without ever looking at a key.
     * @param crlDPExt the certificate's {@code cRLDistributionPoints}, already parsed, or null
     *     when it has none. Only the indirect-CRL branch reads it, but parsing it again here
     *     would mean decoding the whole certificate a second time.
     */
    private static CrlUpdateDecision validateFetchedCrl(X509Certificate certificate,
            X509CRL fetched, X509CRL current, Supplier<Collection<PublicKey>> issuerKeys,
            CRLDistributionPointsExtension crlDPExt) {
        if (fetched == null) {
            return CrlUpdateDecision.DISCARD;
        }
        if (!fetched.getIssuerX500Principal().equals(certificate.getIssuerX500Principal())) {
            // An indirect CRL (RFC 5280 5.2.5) legitimately carries its delegated signer's name
            // here rather than the certificate's issuer, and is marked as such by the indirectCRL
            // flag of its IssuingDistributionPoint. It must not be stored — nothing reachable
            // from here establishes that this deployment's CA really delegated to that signer,
            // and the shared stores are keyed on issuer DN, so accepting it would let whoever
            // answers a distribution point plant a CRL for an issuer they have nothing to do
            // with. Handing it to the caller is a different matter: PKIX accepts an indirect CRL
            // only when it is signed by the cRLIssuer the certificate's distribution point names,
            // so a substituted one is rejected there, while a genuine one is what keeps
            // revocation working for a PKI with a delegated CRL signer.
            //
            // That same delegation is required here rather than left to PKIX alone. This is the
            // one branch that can neither check a signature — the delegated signer's key is not
            // one of the issuer's — nor compare CRL numbers, which belong to a different series;
            // without it, setting one bit in a CRL is enough for whoever answers a distribution
            // point to have any CRL at all handed back to the caller.
            if (isIndirectCrl(fetched)) {
                if (!namesCrlIssuer(crlDPExt, fetched.getIssuerX500Principal())) {
                    debug.error("AMCRLStore.validateFetchedCrl: CRL declares itself an indirect "
                            + "CRL issued by " + fetched.getIssuerX500Principal()
                            + ", which the certificate does not name as a cRLIssuer");
                    return CrlUpdateDecision.DISCARD;
                }
                debug.warning("AMCRLStore.validateFetchedCrl: CRL is an indirect CRL issued by "
                        + fetched.getIssuerX500Principal() + "; it will be used for this "
                        + "validation but not stored");
                return CrlUpdateDecision.USE_ONLY;
            }
            debug.error("AMCRLStore.validateFetchedCrl: CRL was issued by "
                    + fetched.getIssuerX500Principal() + ", expected "
                    + certificate.getIssuerX500Principal());
            return CrlUpdateDecision.DISCARD;
        }
        // Comparing CRL numbers needs no key, so this holds whether or not the signature can be
        // checked below.
        if (current != null && !isAtLeastAsRecentAs(fetched, current)) {
            debug.error("AMCRLStore.validateFetchedCrl: CRL is older than the one already held");
            return CrlUpdateDecision.DISCARD;
        }
        boolean signatureRequired = getBooleanProperty(CRL_REQUIRE_VERIFIED_SIGNATURE, true);
        Collection<PublicKey> keys = issuerKeys.get();
        if (keys == null || keys.isEmpty()) {
            if (!signatureRequired) {
                debug.warning("AMCRLStore.validateFetchedCrl: no issuer key available, storing the "
                        + "CRL anyway because " + CRL_REQUIRE_VERIFIED_SIGNATURE + " is false");
                return CrlUpdateDecision.STORE;
            }
            // Usable now — PKIX checks it again — but not stored, so it is fetched afresh every
            // time. That is the price of not knowing who wrote it.
            debug.warning("AMCRLStore.validateFetchedCrl: no issuer key available; the CRL will "
                    + "be used for this validation but not stored");
            return CrlUpdateDecision.USE_ONLY;
        }
        // Any one of the issuer's keys will do: during a key rollover the CRL is signed with the
        // new one while the retired certificate is still trusted, and either order of discovery
        // has to reach the same answer.
        GeneralSecurityException lastFailure = null;
        for (PublicKey issuerKey : keys) {
            try {
                fetched.verify(issuerKey);
                return CrlUpdateDecision.STORE;
            } catch (GeneralSecurityException e) {
                lastFailure = e;
            }
        }
        if (!signatureRequired) {
            debug.warning("AMCRLStore.validateFetchedCrl: CRL is not signed by the issuer, "
                    + "accepting it because " + CRL_REQUIRE_VERIFIED_SIGNATURE + " is false");
            return CrlUpdateDecision.STORE;
        }
        debug.error("AMCRLStore.validateFetchedCrl: CRL is not signed by the issuer", lastFailure);
        return CrlUpdateDecision.DISCARD;
    }

    /**
     * @return {@code false} if {@code fetched} is demonstrably superseded by {@code current}, or
     *     if {@code current} is numbered and {@code fetched} is not — dropping the extension must
     *     not be a way around the check.
     */
    private static boolean isAtLeastAsRecentAs(X509CRL fetched, X509CRL current) {
        BigInteger currentNumber = getCrlNumber(current);
        if (currentNumber != null) {
            BigInteger fetchedNumber = getCrlNumber(fetched);
            return fetchedNumber != null && fetchedNumber.compareTo(currentNumber) >= 0;
        }
        // RFC 5280 requires CRLNumber, but CAs that omit it exist, and for those the comparison
        // above has nothing to compare. thisUpdate never goes backwards for a legitimate issuer,
        // so it is what is left to recognise a replayed CRL.
        Date fetchedDate = fetched.getThisUpdate();
        Date currentDate = current.getThisUpdate();
        return fetchedDate == null || currentDate == null || !fetchedDate.before(currentDate);
    }

    /**
     * @return the value of the CRL's {@code CRLNumber} extension, or null if it has none or it
     *     cannot be read.
     */
    static BigInteger getCrlNumber(X509CRL crl) {
        byte[] extension = crl.getExtensionValue(CRL_NUMBER_OID);
        if (extension == null) {
            return null;
        }
        try {
            // getExtensionValue() returns the DER OCTET STRING wrapping the extension's own DER
            // encoding, which for CRLNumber is an INTEGER. DerValue checks the tags and the
            // lengths, so a hostile encoding is its problem rather than another hand-rolled
            // parser's; the module already compiles and runs against sun.security.
            return new DerValue(new DerValue(extension).getOctetString()).getBigInteger();
        } catch (IOException | RuntimeException e) {
            debug.warning("AMCRLStore.getCrlNumber: cannot read the CRLNumber extension: "
                    + e.getMessage());
            return null;
        }
    }

    /**
     * Reports whether {@code certificate} delegates its CRLs to {@code crlIssuer}, i.e. names it
     * in the {@code cRLIssuer} field of one of its {@code cRLDistributionPoints}.
     *
     * <p>This is PKIX's own precondition for accepting an indirect CRL (RFC 5280 section 6.3.3):
     * a CRL issued by anyone other than the certificate's issuer counts only for a certificate
     * that named that issuer. The overwhelming majority of certificates delegate to nobody, and
     * for those there is now nothing a distribution point can answer with but a CRL from the
     * issuer itself.
     *
     * @param crlDPExt the certificate's {@code cRLDistributionPoints}, or null when it has none.
     * @param crlIssuer the name the fetched CRL was issued under.
     */
    private static boolean namesCrlIssuer(CRLDistributionPointsExtension crlDPExt,
            X500Principal crlIssuer) {
        for (DistributionPoint dp : distributionPointsOf(crlDPExt)) {
            GeneralNames names = dp.getCRLIssuer();
            if (names == null) {
                continue;
            }
            for (int idx = 0; idx < names.size(); idx++) {
                GeneralName name = names.get(idx);
                // Only a directoryName can be compared with a CRL's issuer; the other forms a
                // cRLIssuer may legally take name nothing an X509CRL reports.
                if (name != null && name.getType() == GeneralNameInterface.NAME_DIRECTORY
                        && crlIssuer.equals(((X500Name) name.getName()).asX500Principal())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return {@code true} if the CRL declares itself an indirect CRL, i.e. one that carries
     *     revocations for issuers other than the one that signed it (RFC 5280 section 5.2.5).
     *     A CRL with no {@code IssuingDistributionPoint} extension, or one that cannot be read,
     *     is not one.
     */
    private static boolean isIndirectCrl(X509CRL crl) {
        IssuingDistributionPointExtension idp = issuingDistributionPointOf(crl);
        return idp != null && idp.getIndirectCRL();
    }

    /**
     * Reads a CRL's own {@code IssuingDistributionPoint} extension — where the CRL says it is
     * published, and whether it is an indirect CRL.
     *
     * <p>{@link X509CRL#getExtensionValue} hands back the DER OCTET STRING that <em>wraps</em>
     * the extension, while {@link IssuingDistributionPointExtension} wants what is inside it.
     * Handing the wrapper straight over threw {@code Invalid encoding} for every CRL, so the
     * extension looked absent whether or not it was there, and the search for a replacement at
     * the CRL's own distribution point never ran at all.
     *
     * @return null when the CRL has no such extension, or it cannot be read.
     */
    static IssuingDistributionPointExtension issuingDistributionPointOf(X509CRL crl) {
        if (crl == null) {
            return null;
        }
        byte[] extension = crl.getExtensionValue(
                PKIXExtensions.IssuingDistributionPoint_Id.toString());
        if (extension == null) {
            return null;
        }
        try {
            return new IssuingDistributionPointExtension(
                    new DerValue(extension).getOctetString());
        } catch (Exception e) {
            debug.warning("AMCRLStore.issuingDistributionPointOf: cannot read the "
                    + "IssuingDistributionPoint extension of the CRL issued by "
                    + crl.getIssuerX500Principal() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Returns the public key that should have signed this certificate's CRLs, taken from the
     * configured trust store or, failing that, from the presented certificate chain.
     *
     * <p>Most PKIs put an intermediate CA between the trust anchor and the end-entity
     * certificate, so the issuer's key is usually not in the trust store at all. Falling back to
     * the chain is what lets those deployments store a verified CRL instead of refetching it on
     * every authentication.
     *
     * <p>A chain supplied by the caller is not trusted on its face: a candidate issuer is only
     * used once it has been traced back to a trust anchor through certificates that were each
     * authorised to issue the next. That matters because this is published API — a caller that
     * hands over an attacker-supplied chain must not thereby authorise an attacker-signed CRL
     * into the shared stores.
     *
     * @param chain the certificate chain the certificate was presented with, possibly null.
     * @return null when no issuer key can be established, in which case the caller must not store
     *     the CRL.
     */
    static List<PublicKey> findIssuerPublicKeys(X509Certificate certificate,
            X509Certificate[] chain) {
        X500Principal issuer = certificate.getIssuerX500Principal();
        // crlSigningOnly, unlike the single-argument overload: this answer decides whether a CRL
        // may be written to the shared stores, and a CA whose keyUsage withholds cRLSign may not
        // have signed one (RFC 5280 6.3.3). Storing a CRL on such a key would guarantee a
        // revocation check that PKIX then fails closed, for every principal under that issuer —
        // the same reason the chain branch below applies mayHaveSignedACrl.
        List<PublicKey> keys = keysOf(trustedIssuersOf(issuer), true);
        if (chain == null) {
            return keys;
        }
        for (X509Certificate candidate : chain) {
            if (candidate != null && issuer.equals(candidate.getSubjectX500Principal())
                    && mayHaveSignedACrl(candidate) && isAnchored(candidate, chain)) {
                keys.add(candidate.getPublicKey());
            }
        }
        return keys;
    }

    /**
     * Walks up the chain from {@code certificate} and reports whether the walk reaches a
     * certificate signed by a trust anchor, checking at each hop both that the signature verifies
     * and that the signer was a CA authorised to issue certificates.
     *
     * <p>This is deliberately not a PKIX path validation: it establishes only that the anchor
     * vouches for the key, which is all that is needed to decide whether a CRL may be stored.
     * Applying the full path check here would reject intermediate and root certificates that
     * legitimately belong to a chain the caller has already validated.
     *
     * <p>The CA check is what separates an anchor's <em>delegates</em> from its <em>subjects</em>.
     * A signature-only walk would accept a chain topped by any ordinary certificate issued under
     * the anchor — with the JDK {@code cacerts} as the default trust store, one free public
     * certificate — letting its holder mint an intermediate around a key of their choosing.
     */
    private static boolean isAnchored(X509Certificate certificate, X509Certificate[] chain) {
        X509Certificate current = certificate;
        // Bounded by the chain length so a cross-certified or hostile chain cannot loop forever.
        for (int hops = 0; hops <= chain.length; hops++) {
            if (!isCertificateIssuer(current)) {
                return false;
            }
            if (isSignedByAnyOf(current, findIssuerPublicKeys(current))) {
                return true;
            }
            X509Certificate parent = findIssuerOf(current, chain);
            if (parent == null) {
                return false;
            }
            current = parent;
        }
        return false;
    }

    /**
     * Reports whether {@code certificate} was authorised to issue the certificate below it in the
     * chain: {@code basicConstraints} says CA, and {@code keyUsage} — when present at all — grants
     * {@code keyCertSign}. RFC 5280 leaves a certificate with no {@code keyUsage} unrestricted,
     * and older private CAs do omit it, so its absence is not treated as a refusal.
     */
    private static boolean isCertificateIssuer(X509Certificate certificate) {
        if (certificate.getBasicConstraints() == -1) {
            return false;
        }
        boolean[] keyUsage = certificate.getKeyUsage();
        return keyUsage == null || (keyUsage.length > KEY_USAGE_KEY_CERT_SIGN
                && keyUsage[KEY_USAGE_KEY_CERT_SIGN]);
    }

    /**
     * Reports whether {@code certificate} could have signed the CRL that is about to be stored.
     * A CA whose {@code keyUsage} withholds {@code cRLSign} may not sign CRLs (RFC 5280 6.3.3),
     * so PKIX will reject anything it did sign — storing that would guarantee a revocation check
     * that fails closed for every principal under the issuer.
     */
    private static boolean mayHaveSignedACrl(X509Certificate certificate) {
        boolean[] keyUsage = certificate.getKeyUsage();
        return keyUsage == null || (keyUsage.length > KEY_USAGE_CRL_SIGN
                && keyUsage[KEY_USAGE_CRL_SIGN]);
    }

    /**
     * Returns the chain member that both carries {@code certificate}'s issuer name and actually
     * signed it. Matching the name alone is not enough: a CA rolling its key over has two
     * certificates under one name for the overlap, and stopping the walk at whichever is listed
     * first would judge a properly anchored chain unanchored half the time.
     *
     * @return null when no chain member signed {@code certificate}. A self-signed certificate is
     *     never its own parent, so a chain that does not reach an anchor ends the walk here.
     */
    private static X509Certificate findIssuerOf(X509Certificate certificate,
            X509Certificate[] chain) {
        X500Principal issuer = certificate.getIssuerX500Principal();
        for (X509Certificate candidate : chain) {
            if (candidate != null && !candidate.equals(certificate)
                    && issuer.equals(candidate.getSubjectX500Principal())
                    && isSignedBy(certificate, candidate.getPublicKey())) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean isSignedByAnyOf(X509Certificate certificate,
            Collection<PublicKey> keys) {
        for (PublicKey key : keys) {
            if (isSignedBy(certificate, key)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSignedBy(X509Certificate certificate, PublicKey key) {
        try {
            certificate.verify(key);
            return true;
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    /**
     * Returns every public key in the configured trust store that could have signed this
     * certificate's CRLs.
     *
     * <p>All of them, not the first: a CA rolling its key over publishes a second certificate
     * under the same subject name, and both are trusted for the overlap. Returning whichever the
     * keystore happens to enumerate first would make CRL verification a coin toss, and a CRL that
     * fails to verify is not stored — so the held one expires and every principal under that CA
     * fails closed.
     *
     * @return an empty, modifiable list when the issuer is not a trust anchor — an intermediate
     *     CA, typically. The caller must then not store the CRL on this evidence alone, since it
     *     cannot check who wrote it.
     */
    static List<PublicKey> findIssuerPublicKeys(X509Certificate certificate) {
        return keysOf(trustedIssuersOf(certificate.getIssuerX500Principal()), false);
    }

    /**
     * @param crlSigningOnly {@code true} to drop a certificate whose {@code keyUsage} withholds
     *     {@code cRLSign}, for the callers deciding whether a CRL may be stored.
     * @return a modifiable list: the caller adds the keys it finds in the presented chain to it.
     */
    private static List<PublicKey> keysOf(List<X509Certificate> certificates,
            boolean crlSigningOnly) {
        List<PublicKey> keys = new ArrayList<>(certificates.size());
        for (X509Certificate certificate : certificates) {
            if (!crlSigningOnly || mayHaveSignedACrl(certificate)) {
                keys.add(certificate.getPublicKey());
            }
        }
        return keys;
    }

    /**
     * The trust store scan behind {@link #findIssuerPublicKeys(X509Certificate)}, memoised per
     * subject name.
     *
     * <p>Scanning is not cheap — every alias is decoded into a certificate — and it runs once per
     * candidate issuer per hop of {@link #isAnchored}, for every certificate in the chain, on the
     * authentication path. {@code AMX509TrustManager} loads its keystore once and holds it in a
     * static, so a hit stays correct for the life of the process; the keystore that produced the
     * entries is held alongside them so that one swapped in later invalidates them instead of
     * being ignored.
     *
     * <p>Certificates rather than keys, because whether a key may be used depends on the
     * certificate that carries it and the two callers want different answers: {@link #isAnchored}
     * only asks who signed what, while a CRL may only be stored on the word of an issuer allowed
     * to sign CRLs.
     *
     * @return an unmodifiable list, empty when nothing in the trust store carries that name.
     */
    private static List<X509Certificate> trustedIssuersOf(X500Principal subject) {
        KeyStore trustStore;
        try {
            trustStore = new AMX509TrustManager().getKeyStore();
        } catch (Exception e) {
            debug.error("AMCRLStore.findIssuerPublicKeys: cannot read the trust store", e);
            return Collections.emptyList();
        }
        if (trustStore == null) {
            return Collections.emptyList();
        }
        // Read once into a local: every get and put below then goes to the cache belonging to the
        // keystore this lookup was made against, whatever another thread does to the field
        // meanwhile. A cache displaced by a keystore swap is simply dropped, entries and all.
        TrustedIssuerCache cache = trustedIssuers;
        if (cache == null || cache.trustStore != trustStore) {
            cache = new TrustedIssuerCache(trustStore);
            trustedIssuers = cache;
        }
        List<X509Certificate> cached = cache.issuers.get(subject);
        if (cached != null) {
            return cached;
        }
        List<X509Certificate> issuers = new ArrayList<>(2);
        try {
            Enumeration<String> aliases = trustStore.aliases();
            while (aliases.hasMoreElements()) {
                Certificate candidate = trustStore.getCertificate(aliases.nextElement());
                if (candidate instanceof X509Certificate && subject.equals(
                        ((X509Certificate) candidate).getSubjectX500Principal())) {
                    issuers.add((X509Certificate) candidate);
                }
            }
        } catch (Exception e) {
            debug.error("AMCRLStore.findIssuerPublicKeys: cannot read the trust store", e);
            // Not cached: a keystore that failed half way through would otherwise answer every
            // later lookup with whatever was read before it failed.
            return Collections.unmodifiableList(issuers);
        }
        List<X509Certificate> result = Collections.unmodifiableList(issuers);
        cache.issuers.put(subject, result);
        return result;
    }

    /**
     * Returns {@code true} if a CRL Distribution Point URL taken from a certificate may be
     * fetched by the server.
     *
     * <p>The URL comes from the {@code cRLDistributionPoints}/{@code IssuingDistributionPoint}
     * extension of the certificate being validated, so without this check the server can be
     * made to issue arbitrary requests to addresses it can reach but the attacker cannot —
     * server-side request forgery (GHSA-g5vw-v384-gw9v).
     *
     * <p>Note the residual TOCTOU: the host is resolved here and resolved again when the
     * connection is opened, so a short-TTL DNS rebind can still defeat the check. Pinning the
     * validated address would need a custom connection factory; this is accepted and matches
     * the equivalent check in {@code PrivilegeChangeNotifier}.
     *
     * @param url the candidate distribution point URL.
     * @return {@code true} if the URL may be fetched.
     */
    static boolean isCrlDpUrlAllowed(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        final URL parsed;
        try {
            parsed = new URL(url);
        } catch (MalformedURLException e) {
            return false;
        }
        // The scheme allow-list is not negotiable: a CRL is always fetched over http(s) (the
        // ldap(s) schemes are handled by getCRLByLdapURI), never over file://, gopher:// etc.
        final String protocol = parsed.getProtocol();
        if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
            return false;
        }
        return isCrlDpHostAllowed(parsed.getHost());
    }

    /**
     * Returns {@code true} if the server may connect to {@code host} to fetch a CRL. Shared by
     * the HTTP and the LDAP distribution point fetchers so both sinks apply one policy.
     *
     * @param host the host name or literal address of the distribution point.
     * @return {@code true} if the host may be contacted.
     */
    static boolean isCrlDpHostAllowed(String host) {
        // Checked before the escape hatch: "allow any address" still needs an address.
        if (host == null || host.isEmpty()) {
            return false;
        }
        if (getBooleanProperty(CRL_DP_ALLOW_ANY_URL)) {
            return true;
        }
        try {
            // Resolved exactly once. The policy itself is SsrfUrlValidator's, which resolves the
            // host when handed a URL — doing that as well would be a second blocking lookup and
            // a second chance for a short-TTL rebind to change the answer under the check.
            //
            // Not covered by CRL_DP_TIMEOUT either way: the JDK offers no timeout on name
            // resolution, and bounding it would mean running every lookup on a thread pool. A
            // distribution point under an unresponsive nameserver therefore stalls for the OS
            // resolver timeout — as it did before this check existed, since connecting resolves
            // the name too. CRL_DP_MAX_LOCATIONS bounds how often that happens per certificate.
            //
            // Strict mode is the full SsrfUrlValidator policy; the default keeps every clause of
            // it but the private/site-local one, because an internal PKI legitimately publishes
            // its CRLs on an RFC1918 address and blocking that on upgrade breaks a working
            // deployment.
            return SsrfUrlValidator.isSafeRemoteAddresses(InetAddress.getAllByName(host),
                    !getBooleanProperty(CRL_DP_STRICT));
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /**
     * Names the two properties that decide the address policy, appended to every refusal.
     *
     * <p>A refused distribution point means the CRL cannot be refreshed, and once the CRL already
     * held expires every certificate under that issuer fails closed. That is a hard failure to
     * trace from the outside, so the log says which setting is in force and which one relaxes it.
     */
    private static String refusalHint() {
        return getBooleanProperty(CRL_DP_STRICT)
                ? " (" + CRL_DP_STRICT + " is on, so private and site-local addresses are"
                        + " blocked too; " + CRL_DP_ALLOW_ANY_URL + " turns the check off"
                        + " altogether)"
                : " (address blocked by the CRL distribution point policy; "
                        + CRL_DP_ALLOW_ANY_URL + " turns the check off altogether)";
    }

    /**
     * Reads a setting, falling back to the plain JVM system property when no OpenAM
     * system-configuration provider is installed. In the server the provider itself falls back
     * to {@code System.getProperty}, so a {@code -D} flag behaves the same either way.
     */
    private static String getProperty(String key) {
        String value = SystemPropertiesManager.get(key);
        return (value != null) ? value : System.getProperty(key);
    }

    private static boolean getBooleanProperty(String key) {
        return getBooleanProperty(key, false);
    }

    /** Reads a switch; a missing value, or one that is neither true nor false, falls back. */
    private static boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        if ("true".equalsIgnoreCase(value.trim())) {
            return true;
        }
        if ("false".equalsIgnoreCase(value.trim())) {
            return false;
        }
        // Not Boolean.parseBoolean, which reads "yes", "1" or "on" as false: for a switch that
        // defaults to on, such as CRL_REQUIRE_VERIFIED_SIGNATURE, that turns it off silently.
        debug.warning("AMCRLStore: " + key + " must be true or false, ignoring " + value);
        return defaultValue;
    }

    /** Reads a positive setting; a missing, unparseable or non-positive value falls back. */
    private static long getLongProperty(String key, long defaultValue) {
        String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            long parsed = Long.parseLong(value.trim());
            if (parsed <= 0) {
                // A zero or negative size or timeout would silently block every fetch, which is
                // a hard failure to trace back to a typo in a configuration file.
                debug.warning("AMCRLStore: " + key + " must be positive, ignoring " + value);
                return defaultValue;
            }
            return parsed;
        } catch (NumberFormatException e) {
            debug.warning("AMCRLStore: " + key + " is not a number: " + value);
            return defaultValue;
        }
    }

    /**
     * Class AMCRLStore is special cased CRL store for LDAP.
     * A AMCRLStore instance has to have all the information for ldap
     * and all the access information for CRLDistributionPointExtension and
     * CRLIssuingDistributionPoint Extension
     *
     * @param param the LDAP certificate store parameters used to configure this CRL store. The
     *     store keeps a copy: changing them afterwards does not affect it.
     */
    public AMCRLStore(AMLDAPCertStoreParameters param) {
        // A copy, because the store searches the CRL directory twice and the second search names
        // the entry updateCRL REPLACEs. Between the two the chain resolver runs, and CRLValidator
        // hands out one shared parameters object that its issuer lookup, and every other thread,
        // sets filters on — reading the caller's object then would write the CRL wherever the
        // last of those searches pointed.
        super((param == null) ? null : new AMLDAPCertStoreParameters(param));
    }

    /**
     * Checks certificate and returns corresponding stored CRL in ldap store
     *
     * @param certificate the certificate whose CRL should be retrieved
     * @return the stored CRL for the certificate's issuer, or null if none was found. See
     *     {@link #getCRL(X509Certificate, X509Certificate[])} for what the caller still has to
     *     check about it.
     * @throws IOException if an error occurs while accessing the CRL store
     */
    public X509CRL getCRL(X509Certificate certificate) throws IOException {
        return getCRL(certificate, null);
    }

    /**
     * Checks certificate and returns corresponding stored CRL in ldap store.
     *
     * <p>Same as {@link #getCRL(X509Certificate)}, but the certificate chain lets a CRL issued by
     * an intermediate CA be verified and therefore stored. Without it such a CRL can only be used
     * for the current validation and is refetched next time.
     *
     * @param certificate the certificate whose CRL should be retrieved
     * @param chain the chain the certificate was presented with. The caller must have validated
     *     it; a key taken from it decides whether a fetched CRL may enter the shared CRL stores.
     *     May be null.
     * @return the CRL for the certificate's issuer, or null if none was found.
     *     <p>Two things about it are the caller's to check. It is not necessarily one that could
     *     be verified locally — see {@link CrlUpdateDecision#USE_ONLY} — so a caller that does not
     *     hand it to PKIX must verify it itself. And it may be <em>past its {@code nextUpdate}</em>:
     *     when the refresh from the distribution point fails or is rejected, the CRL already held
     *     is returned rather than nothing, since a superseded revocation list is still better
     *     evidence than none. PKIX drops an expired CRL by itself, but a caller that asks it
     *     {@link X509CRL#isRevoked} directly has to check {@link X509CRL#getNextUpdate} first.
     * @throws IOException if an error occurs while accessing the CRL store
     */
    public X509CRL getCRL(X509Certificate certificate, X509Certificate[] chain) throws IOException {
        return getCRLWithLazyChain(certificate, () -> chain);
    }

    /**
     * As {@link #getCRL(X509Certificate, X509Certificate[])}, but resolving the chain only if a
     * CRL is actually fetched from a distribution point.
     *
     * <p>The chain is read for one purpose — deciding whether a fetched CRL may be stored — and a
     * CRL still fresh in the store or the cache is fetched from nowhere. A caller that has to go
     * to a directory to assemble the chain should not pay for it on every validation.
     *
     * <p>Deliberately not another {@code getCRL} overload: {@code getCRL(cert, null)} would then
     * have two applicable signatures to choose between.
     */
    X509CRL getCRLWithLazyChain(X509Certificate certificate, ChainResolver chain)
            throws IOException {
        SearchResultEntry crlEntry = null;
        X509CRL crl = null;
        // A fetched CRL that could not be verified here: returned to the caller, never stored.
        X509CRL unverified = null;

        if (storeParam.isDoCRLCaching()) {
            if (debug.messageEnabled()) {
                debug.message("AMCRLStore.getCRL: Trying to get CRL from cache");
            }
            crl = getCRLFromCache(certificate);
        }

        try (Connection ldc = getConnection()) {
            if (ldc == null) {
                // The CRL directory is unreachable. Returning null here would throw away a
                // perfectly good cached CRL and leave the caller with no revocation evidence at
                // all, so PKIX reports the status undetermined and every certificate login under
                // this issuer fails closed for the length of the outage — precisely when the
                // cache should be earning its keep. getConnection() reports a refused factory by
                // returning null; when it throws instead, the catch below has always fallen
                // through to the CRL already held, so this only makes the two paths agree.
                debug.error("AMCRLStore.getCRL: no connection to the CRL store"
                        + (crl == null ? "" : ", using the CRL already held"));
                return crl;
            }
            if (crl == null) {
                if (debug.messageEnabled()) {
                    debug.message("AMCRLStore.getCRL: crl is null");
                }
                crlEntry = getCrlEntry(ldc);
                crl = getCRLFromEntry(crlEntry);
            }

            if (storeParam.isDoUpdateCRLs() && needCRLUpdate(crl)) {
                if (debug.messageEnabled()) {
                    debug.message("AMCRLStore.getCRL: need CRL update");
                }

                X509CRL tmpcrl = null;
                IssuingDistributionPointExtension crlIDPExt = issuingDistributionPointOf(crl);

                CRLDistributionPointsExtension crlDPExt = null;
                try {
                    crlDPExt = getCRLDPExt(certificate);
                } catch (Exception e) {
                    debug.message("AMCRLStore.getCRL: crlDPExt is null");
                }

                // One budget for this certificate, spent across both searches below. Allocating
                // one per search would silently double the configured caps.
                FetchBudget budget = FetchBudget.forOneCertificate();

                if ((tmpcrl == null) && (crlIDPExt != null)) {
                    tmpcrl = getUpdateCRLFromCrlIDP(crlIDPExt, budget);
                }

                if ((tmpcrl == null) && (crlDPExt != null)) {
                    tmpcrl = getUpdateCRLFromCrlDP(crlDPExt, budget);
                }

                if (tmpcrl != null) {
                    // The fetched CRL is unauthenticated data from a location the certificate
                    // named. Check who issued it before it goes anywhere shared; a failed or
                    // rejected fetch must leave the CRL already held untouched.
                    switch (validateFetchedCrl(certificate, tmpcrl, crl,
                            () -> findIssuerPublicKeys(certificate, chain.chain()), crlDPExt)) {
                    case STORE: {
                        // What the anti-rollback comparison in validateFetchedCrl was made
                        // against. That is the CRL this node held, which is not necessarily the
                        // one the shared entry holds.
                        X509CRL held = crl;
                        if (crlEntry == null) {
                            crlEntry = getCrlEntry(ldc);
                            // The stale CRL came from the in-memory cache, so the shared entry
                            // has not been read on this pass. updateCRL REPLACEs it
                            // unconditionally, and another node may have written a newer CRL
                            // there since this one was cached — without comparing against what
                            // the entry actually holds, a node running on a stale cache rolls the
                            // cluster's CRL back and un-revokes whatever the newer one revoked.
                            X509CRL stored = getCRLFromEntry(crlEntry);
                            if (stored != null) {
                                held = stored;
                            }
                        }

                        if (held != null && !isAtLeastAsRecentAs(tmpcrl, held)) {
                            debug.warning("AMCRLStore.getCRL: the CRL store already holds a CRL "
                                    + "newer than the one fetched for "
                                    + certificate.getIssuerX500Principal()
                                    + "; keeping the stored one");
                            // And returning it rather than the fetched one: rolling the answer
                            // back for this validation alone would un-revoke just as effectively.
                            // Caching it below also repairs the stale entry this node started
                            // from, so the next authentication does not refetch.
                            crl = held;
                            break;
                        }

                        if (debug.messageEnabled()) {
                            debug.message("AMCRLStore.getCRL: new crl = " + tmpcrl);
                        }

                        if (crlEntry != null) {
                            updateCRL(ldc, crlEntry.getName().toString(),
                                    crlAttributeName(crlEntry), tmpcrl.getEncoded());
                        }
                        crl = tmpcrl;
                        break;
                    }
                    case USE_ONLY:
                        // Usable for this validation, where PKIX verifies it again, but it must
                        // not reach the CRL cache or the LDAP CRL entry.
                        unverified = tmpcrl;
                        break;
                    default:
                        debug.error("AMCRLStore.getCRL: rejected the fetched CRL, keeping the "
                                + "one already held");
                        break;
                    }
                }
            }

            if (storeParam.isDoCRLCaching()) {
                if (debug.messageEnabled()) {
                    debug.message("AMCRLStore.getCRL: Updating CRL cache");
                }
                updateCRLCache(certificate, crl);
            }

        } catch (Exception e) {
            debug.error("AMCRLStore.getCRL: Error in getting CRL : ", e);
        }

        return (unverified != null) ? unverified : crl;
    }

    /**
     * Checks certificate and returns corresponding stored CRL in cached CRL store.
     *
     * @param certificate the certificate whose issuer is used to look up the cached CRL
     * @return Cached CRL information about the certificate.
     */
    public X509CRL getCRLFromCache(X509Certificate certificate) {
        return cachedcrls.get(CertUtils.getIssuerName(certificate));
    }

    /**
     * Checks certificate and update CRL in cached CRL store.
     *
     * @param certificate the certificate whose issuer identifies the cache entry
     * @param crl the CRL to store in the cache, or null to remove the cached entry
     */
    public void updateCRLCache(X509Certificate certificate, X509CRL crl) {
        String issuer = CertUtils.getIssuerName(certificate);

        if (crl == null) {
            cachedcrls.remove(issuer);
        } else {
            cachedcrls.put(issuer, crl);
        }
    }

    /**
     * Reads the CRL entry, asking for its CRL and nothing else: a CA entry routinely carries
     * certificates and cross-certificates as well, none of which this class uses.
     */
    private SearchResultEntry getCrlEntry(Connection ldc) {
        return (mCrlAttrName == null)
                ? getLdapEntry(ldc, CERTIFICATE_REVOCATION_LIST, CERTIFICATE_REVOCATION_LIST_BINARY)
                : getLdapEntry(ldc, mCrlAttrName);
    }

    private X509CRL getCRLFromEntry(SearchResultEntry entry)
            throws Exception {

        if (debug.messageEnabled()) {
            debug.message("AMCRLStore.getCRLFromEntry:");
        }

        if (entry == null) {
            return null;
        }

        Attribute crlAttribute = null;
        X509CRL crl = null;

        try {
            /*
             * Retrieve the certificate revocation list if available.
             */

            if (mCrlAttrName == null) {
                crlAttribute = entry.getAttribute(CERTIFICATE_REVOCATION_LIST);
                if (crlAttribute == null) {
                    crlAttribute = entry.getAttribute(CERTIFICATE_REVOCATION_LIST_BINARY);
                    if (crlAttribute == null) {
                        debug.error("No CRL Cache is configured");
                        return null;
                    }
                }

                mCrlAttrName = crlAttribute.getAttributeDescriptionAsString();
            } else {
                crlAttribute = entry.getAttribute(mCrlAttrName);
            }

            if (crlAttribute.size() > 1) {
                debug.error("More than one CRL entries are configured");
                return null;
            }
        } catch (Exception e) {
            debug.error("Error in getting Cached CRL");
            return null;
        }

        try {
            byte[] bytes = crlAttribute.firstValue().toByteArray();
            if (debug.messageEnabled()) {
                debug.message("AMCRLStore.getCRLFromEntry: crl size = " +
                        bytes.length);
            }
            crl = parseCrl(bytes);
        } catch (Exception e) {
            debug.error("Certificate: CertRevoked = ", e);
        }

        return crl;
    }

    /**
     * Parses a CRL with a factory of its own. {@code AMCertStore.cf} is shared by every thread in
     * the server, and {@link CertificateFactory} does not promise to be safe for concurrent use.
     */
    private static X509CRL parseCrl(byte[] der) throws GeneralSecurityException {
        return (X509CRL) CertificateFactory.getInstance("X.509")
                .generateCRL(new ByteArrayInputStream(der));
    }

    /**
     * It checks whether the certificate has CRLDistributionPointsExtension
     * or not. If there is, it returns the extension.
     *
     * @param certificate
     */
    static CRLDistributionPointsExtension getCRLDPExt(X509Certificate certificate) {
        CRLDistributionPointsExtension dpExt = null;

        try {
            X509CertImpl certImpl = new X509CertImpl(certificate.getEncoded());
            dpExt = certImpl.getCRLDistributionPointsExtension();
        } catch (Exception e) {
            debug.error("Error finding CRL distribution Point configured: ", e);
        }

        return dpExt;
    }


    /**
     * It updates CRL under the dn in the directory server.
     * It retrieves CRL distribution points from the parameter
     * CRLDistributionPointsExtension dpExt.
     *
     * @param dpExt
     */
    synchronized X509CRL getUpdateCRLFromCrlDP(CRLDistributionPointsExtension dpExt) {
        return getUpdateCRLFromCrlDP(dpExt, FetchBudget.forOneCertificate());
    }

    /**
     * The distribution points of a {@code cRLDistributionPoints} extension.
     *
     * <p>Reflectively, because the accessor was renamed: JDK 21 has
     * {@code getDistributionPoints()} where earlier releases have {@code get("points")}.
     *
     * @return an empty list when the extension is absent or cannot be read, never null.
     */
    @SuppressWarnings("unchecked")
    private static List<DistributionPoint> distributionPointsOf(
            CRLDistributionPointsExtension dpExt) {
        if (dpExt == null) {
            return Collections.emptyList();
        }
        List<DistributionPoint> dps = null;
        try {
            Method accessor = distributionPointsAccessor(dpExt.getClass());
            dps = (List<DistributionPoint>) ((accessor.getParameterCount() == 0)
                    ? accessor.invoke(dpExt) : accessor.invoke(dpExt, "points"));
        } catch (Throwable ioex) {
            if (debug.warningEnabled()) {
                debug.warning("AMCRLStore.distributionPointsOf: ", ioex);
            }
        }
        return (dps == null) ? Collections.<DistributionPoint>emptyList() : dps;
    }

    /** Last accessor resolved by {@link #distributionPointsAccessor(Class)}. */
    private static volatile Method distributionPointsAccessor;

    /**
     * Resolves the accessor once rather than on every call. Two callers now read the distribution
     * points of every certificate in every chain on the authentication path, and on the releases
     * that have only the older accessor the lookup for the newer one filled in a
     * {@code NoSuchMethodException} — stack trace and all — for each of those calls.
     *
     * <p>Only a missing method falls through to the older spelling. The two never coexist on one
     * JDK, so an exception thrown by the accessor that does exist is a real failure and is
     * reported as one, rather than being retried under a name this release does not have.
     */
    private static Method distributionPointsAccessor(Class<?> extension)
            throws NoSuchMethodException {
        Method accessor = distributionPointsAccessor;
        // Re-resolved when the extension is not the class it was resolved against, so a subclass
        // carrying its own accessor is never read through another class's Method.
        if (accessor != null && accessor.getDeclaringClass() == extension) {
            return accessor;
        }
        try { // jdk21
            accessor = extension.getDeclaredMethod("getDistributionPoints");
        } catch (NoSuchMethodException e) {
            accessor = extension.getDeclaredMethod("get", String.class);
        }
        distributionPointsAccessor = accessor;
        return accessor;
    }

	synchronized X509CRL
    getUpdateCRLFromCrlDP(CRLDistributionPointsExtension dpExt, FetchBudget budget) {
        // Get CRL Distribution points
        List<DistributionPoint> dps = distributionPointsOf(dpExt);

        if (dps.isEmpty()) {
            return null;
        }

        // The budget is shared by every distribution point in the extension, and by the caller's
        // other search — it belongs to the certificate, not to any one of them.
        for (DistributionPoint dp : dps) {
            if (!budget.hasLocationsLeft() || budget.isExpired()) {
                // Every further distribution point would only reach the same exhausted budget and
                // log the same warning again, once per point per certificate in the chain.
                break;
            }
            // Null for a distribution point that names a nameRelativeToCRLIssuer or nothing but
            // a cRLIssuer, both of which RFC 5280 allows and neither of which is a location.
            GeneralNames gName = dp.getFullName();
            if (debug.messageEnabled()) {
                debug.message("AMCRLStore.getUpdateCRLFromCrlDP: DP = " +
                        gName);
            }

            byte[] Crls = getCRLsFromGeneralNames(gName, budget);
            if (Crls != null && Crls.length > 0) {
                try {
                    return parseCrl(Crls);
                } catch (Exception ex) {
                    if (debug.warningEnabled()) {
                        debug.warning("AMCRLStore.getUpdateCRLFromCrlDP: " +
                                "Error in generating X509CRL", ex);
                    }
                }
            }
        }

        return null;
    }

    /**
     * Fetches a replacement CRL from the locations the expired CRL's own
     * {@code IssuingDistributionPoint} extension names.
     *
     * @param idpExt the extension, from {@link #issuingDistributionPointOf(X509CRL)}.
     */
    synchronized X509CRL getUpdateCRLFromCrlIDP(IssuingDistributionPointExtension idpExt) {
        return getUpdateCRLFromCrlIDP(idpExt, FetchBudget.forOneCertificate());
    }

    synchronized X509CRL getUpdateCRLFromCrlIDP(IssuingDistributionPointExtension idpExt,
            FetchBudget budget) {
        if (idpExt == null) {
            return null;
        }

        GeneralNames gName = idpExt.getFullName();
        if (gName == null) {
            return null;
        }

        if (debug.messageEnabled()) {
            debug.message("AMCRLStore.getUpdateCRLFromCrlIDP: gName = " + gName);
        }
        byte[] Crls = getCRLsFromGeneralNames(gName, budget);

        X509CRL crl = null;
        if (Crls != null) {
            try {
                crl = parseCrl(Crls);
            } catch (Exception e) {
                debug.error("Error in generating X509CRL" + e.toString());
            }
        }

        return crl;
    }


    /**
     * What one certificate's search for a replacement CRL may spend: how many distribution point
     * locations may still be contacted, and by when the whole search has to be over.
     *
     * <p>Both belong to the certificate, not to a location and not to a redirect hop.
     * {@link #getCRL(X509Certificate, X509Certificate[])} runs two searches for one certificate —
     * the expired CRL's own {@code IssuingDistributionPoint} and the certificate's
     * {@code cRLDistributionPoints} — each over a list the certificate itself supplies, and
     * {@code Cert.doJCERevocationValidation} repeats that for every certificate in the chain. A
     * budget held any further down is multiplied by all of those before a user waiting to log in
     * sees it.
     */
    static final class FetchBudget {

        private int locations;
        /**
         * Read from {@link System#nanoTime()}, not from the wall clock: this measures how long a
         * user has been waiting, and an NTP step backwards would otherwise extend the budget by
         * the size of the correction — with the whole of it handed to a stalling distribution
         * point as a socket timeout — while a step forwards would abort a refresh that was
         * progressing normally.
         */
        private final long deadlineNanos;

        private FetchBudget(int locations, long timeoutMillis) {
            this.locations = locations;
            // Clamped before the conversion. MILLISECONDS.toNanos saturates at Long.MAX_VALUE
            // for anything past ~292 years, and adding a saturated value to System.nanoTime()
            // wraps the deadline negative. Every read of it below is a subtraction, which stays
            // exact across that wrap, so nothing here misbehaves today — but the field would
            // hold a value no ordinary comparison could be added against later, and a budget
            // that large is no bound at all, which is the opposite of what CRL_DP_TIMEOUT is
            // for. Integer.MAX_VALUE ms is where openCrlDpConnection's own clamp already stops,
            // so nothing downstream sees a narrower number than it did before.
            this.deadlineNanos = System.nanoTime()
                    + TimeUnit.MILLISECONDS.toNanos(Math.min(timeoutMillis, Integer.MAX_VALUE));
        }

        /** The budget one certificate gets, from the configured limits. */
        static FetchBudget forOneCertificate() {
            return withLocations((int) Math.min(getLongProperty(CRL_DP_MAX_LOCATIONS,
                    DEFAULT_CRL_DP_MAX_LOCATIONS), Integer.MAX_VALUE));
        }

        /** As {@link #forOneCertificate()}, but with the location count given explicitly. */
        static FetchBudget withLocations(int locations) {
            return new FetchBudget(locations,
                    getLongProperty(CRL_DP_TIMEOUT, DEFAULT_CRL_DP_TIMEOUT));
        }

        boolean hasLocationsLeft() {
            return locations > 0;
        }

        /**
         * Charges one location. Called once a connection to it is actually going to be opened,
         * never before: a certificate that lists a handful of unreachable forms of its CRL — as
         * Active Directory Certificate Services does, with several {@code ldap:///} spellings —
         * must not exhaust the budget before the location that works has been reached.
         */
        void spendLocation() {
            locations--;
        }

        /** @return how much of the time budget is left; zero or less once it is spent. */
        long remainingMillis() {
            return TimeUnit.NANOSECONDS.toMillis(deadlineNanos - System.nanoTime());
        }

        boolean isExpired() {
            // Against the nanosecond deadline rather than remainingMillis(), which truncates
            // the last sub-millisecond sliver of the budget to zero and would call it spent.
            return deadlineNanos - System.nanoTime() <= 0;
        }
    }

    /**
     * @param gName the locations named by one distribution point, or null when it names none —
     *     a {@code DistributionPoint} may legally carry a {@code nameRelativeToCRLIssuer} or
     *     nothing but a {@code cRLIssuer}, and neither is somewhere to connect to.
     * @param budget what the whole search for this certificate may still spend.
     */
    private byte[] getCRLsFromGeneralNames(GeneralNames gName, FetchBudget budget) {
        if (gName == null) {
            return null;
        }
        if (debug.messageEnabled()) {
            debug.message("AMCRLStore.getCRLsFromGeneralNames: gNames.size = " +
                    gName.size());
        }
        for (int idx = 0; idx < gName.size(); idx++) {
            String uri = getDistributionPointUri(gName.get(idx));
            if (uri == null) {
                continue;
            }
            if (!budget.hasLocationsLeft()) {
                debug.warning("AMCRLStore.getCRLsFromGeneralNames: reached the limit of "
                        + CRL_DP_MAX_LOCATIONS + " distribution point locations, not trying "
                        + "any more for this certificate");
                return null;
            }
            // Checked here and not only where a connection is opened: a location the address
            // policy refuses costs no location from the budget, but resolving its name may
            // still have cost a resolver timeout, so the wall-clock budget is what bounds how
            // many refused locations one certificate can make the server look at.
            if (budget.isExpired()) {
                debug.warning("AMCRLStore.getCRLsFromGeneralNames: the " + CRL_DP_TIMEOUT
                        + " budget for this certificate is spent, not trying " + uri);
                return null;
            }
            if (debug.messageEnabled()) {
                debug.message("DP Name : " + uri);
            }
            byte[] crls = getCRLByURI(uri, budget);
            if (crls != null && crls.length > 0) {
                return crls;
            }
        }

        return null;
    }

    /**
     * Returns the location named by {@code generalName} as a URI the server may try to fetch,
     * or {@code null} if this general name does not name one.
     *
     * <p>Only a {@code uniformResourceIdentifier} names a location to connect to, and the URI
     * has to <em>start</em> with a supported scheme. Searching the whole text for {@code "http"}
     * — as this used to — let an {@code rfc822Name} such as {@code user@http://internal:8080/}
     * or a {@code directoryName} such as {@code CN=http://internal:8080/} reach the HTTP fetcher.
     */
    private static String getDistributionPointUri(GeneralName generalName) {
        if (generalName == null || generalName.getType() != GeneralNameInterface.NAME_URI) {
            return null;
        }
        // NAME_URI is GeneralName's own report of what it holds, and only a URIName reports it,
        // so the cast is the type check. Reading the location out of GeneralName.toString()
        // instead — which is how this used to find it — is what let a non-URI name reach the
        // fetcher in the first place.
        String uri = ((URIName) generalName.getName()).getName().trim();

        String protocol = uri.toLowerCase();
        if (protocol.startsWith("http://") || protocol.startsWith("https://")
                || protocol.startsWith("ldap://") || protocol.startsWith("ldaps://")) {
            return uri;
        }
        return null;
    }

    /**
     * It replaces attribute value under the DN.
     * It is used to replace old CRL with new one.
     *
     * @param ldc
     * @param dn
     * @param crls
     */
    private void updateCRL(Connection ldc, String dn, String attributeName, byte[] crls) {
        try {
            ModifyRequest modifyRequest = LDAPRequests.newModifyRequest(dn)
                    .addModification(ModificationType.REPLACE, attributeName, crls);
            ldc.modify(modifyRequest);
        } catch (Exception e) {
            // Deliberately wider than LdapException: an unusable attribute description fails
            // before the request is ever sent. Letting that escape would land in getCRL's outer
            // catch and abandon a CRL that had already been verified.
            debug.error("Error updating CRL Cache : ", e);
        }
    }

    /**
     * Names the attribute the CRL is written back to.
     *
     * <p>{@code mCrlAttrName} is only set when this store has read the entry, and it does not read
     * it when the stale CRL came from the cache — the common case, since the cache is consulted
     * first and only an expired entry gets this far. So fall back to whichever of the two
     * attributes the entry actually carries, and finally to the binary form for an entry that
     * holds no CRL yet.
     */
    private String crlAttributeName(SearchResultEntry entry) {
        if (mCrlAttrName != null) {
            return mCrlAttrName;
        }
        if (entry != null) {
            Attribute existing = entry.getAttribute(CERTIFICATE_REVOCATION_LIST);
            if (existing == null) {
                existing = entry.getAttribute(CERTIFICATE_REVOCATION_LIST_BINARY);
            }
            if (existing != null) {
                return existing.getAttributeDescriptionAsString();
            }
        }
        return CERTIFICATE_REVOCATION_LIST_BINARY;
    }

    /**
     * It is checking uri's protocol.
     * Protocol has to be http(s) or ldap.
     * Based on checked protocol, it gets new CRL by invking
     * getCRLByLdapURI() or getCRLByHttpURI()
     *
     * @param uri
     */
    private byte[] getCRLByURI(String uri, FetchBudget budget) {
        if (debug.messageEnabled()) {
            debug.message("AMCRLStore.getCRLByURI : uri = " + uri);
        }
        if (uri == null) {
            return null;
        }

        // The "https"/"ldaps" arms this used to carry were unreachable — "https://…" already
        // starts with "http". getDistributionPointUri() has since checked the full scheme, and
        // getCRLByHttpURI() checks it again against the parsed URL.
        String protocol = uri.trim().toLowerCase();
        if (protocol.startsWith("http")) {
            return getCRLByHttpURI(uri, budget);
        } else if (protocol.startsWith("ldap")) {
            return getCRLByLdapURI(uri, budget);
        }

        return null;
    }

    /**
     * It gets the new CRL from ldap server.
     * If it is ldap URI, the URI has to be a dn that can be accessed
     * with ldap anonymous bind.
     * (example : ldap://server:port/uid=ca,o=company.com)
     * This dn entry has to have CRL in attribute certificaterevocationlist
     * or certificaterevocationlist;binary.
     * Only that entry is read: any scope, filter or attribute list the URI
     * carries is ignored.
     *
     * @param uri
     */
    byte[] getCRLByLdapURI(String uri) {
        return getCRLByLdapURI(uri, FetchBudget.forOneCertificate());
    }

    /**
     * As {@link #getCRLByLdapURI(String)}, but sharing one certificate's {@link FetchBudget}
     * rather than starting a fresh one.
     */
    byte[] getCRLByLdapURI(String uri, FetchBudget budget) {

        if (debug.messageEnabled()) {
            debug.message("AMCRLStore.getCRLByLdapURI: uri = " + uri);
        }

        LDAPUrl url;
        byte[] crl = null;

        try {
            url = LDAPUrl.valueOf(uri);
        } catch (LocalizedIllegalArgumentException e) {
            debug.error("AMCRLStore.getCRLByLdapURI(): Could not parse uri: {}", uri, e);
            return null;
        }
        debug.message("AMCRLStore.getCRLByLdapURI: url.dn = {}", url.getName());

        // Same address policy as the HTTP fetcher: this host also comes straight out of the
        // certificate being validated, and is just as usable for probing internal services.
        // Note that LDAPUrl defaults a missing host to localhost, so the host-less "ldap:///dn"
        // form Active Directory Certificate Services emits is refused here as a loopback address.
        // That is deliberate — the alternative is connecting to whatever runs on the OpenAM host
        // because a certificate asked for it — and dp.allow-any-url is the way back.
        if (!isCrlDpHostAllowed(url.getHost())) {
            debug.error("AMCRLStore.getCRLByLdapURI: refusing to contact CRL distribution point "
                    + uri + refusalHint());
            return null;
        }
        // OpenDJ's default REQUEST_TIMEOUT is unlimited, so without a budget here a distribution
        // point that completes the handshake and then never answers holds the authentication
        // thread indefinitely — the same denial of service the HTTP fetcher is bounded against.
        // Read once and tested here rather than through isExpired(): what is left of the budget
        // can cross zero between the two calls — the DNS lookup above is not covered by it — and
        // OpenDJ reads a zero Duration as "no limit", the very thing this is here to prevent,
        // while a negative one throws out of Duration's constructor.
        long remainingMillis = budget.remainingMillis();
        if (remainingMillis <= 0) {
            debug.error("AMCRLStore.getCRLByLdapURI: the " + CRL_DP_TIMEOUT
                    + " budget for this certificate was spent before " + uri);
            return null;
        }
        Duration remaining = duration(remainingMillis, TimeUnit.MILLISECONDS);
        Options options = Options.defaultOptions()
                .set(LDAPConnectionFactory.CONNECT_TIMEOUT, remaining)
                .set(LDAPConnectionFactory.REQUEST_TIMEOUT, remaining);

        // Check ldap over SSL
        if (url.isSecure()) {
            try {
                options = options.set(LDAPConnectionFactory.SSL_CONTEXT,
                        new SSLContextBuilder().getSSLContext());
            } catch (GeneralSecurityException e) {
                debug.error("AMCRLStore.getCRLByLdapURI: Error getting SSL Context", e);
                return null;
            }
        }
        // Charged here rather than by the caller, and only once nothing above can still refuse
        // this location: neither one the address policy blocked, nor one the time budget ran out
        // before, nor an ldaps:// one whose SSL context could not be built was ever contacted,
        // and none of them must cost the certificate one of the few locations it may try.
        budget.spendLocation();
        // The factory is closeable and holds a reference-counted transport. This path runs once
        // per distribution point location, up to the configured limit, for every certificate in
        // the chain — leaking one per call adds up.
        try (LDAPConnectionFactory factory = new LDAPConnectionFactory(url.getHost(), url.getPort(),
                     options);
             Connection ldc = factory.getConnection()) {
            // The entry the URL names and its CRL, nothing else. Scope, filter and attribute list
            // come out of the certificate just as the host does, and the address check above says
            // nothing about them: taken verbatim, "??sub?(objectClass=*)" is an anonymous,
            // unindexed whole-subtree search of whichever directory sits at an allowed address —
            // site-local ones included by default. RFC 5280 4.2.1.13 has an LDAP distribution
            // point name the entry that holds the CRL, so a base read loses nothing real.
            ConnectionEntryReader results = ldc.search(LDAPRequests.newSingleEntrySearchRequest(
                    url.getName(), CERTIFICATE_REVOCATION_LIST, CERTIFICATE_REVOCATION_LIST_BINARY));

            if (!results.hasNext()) {
                debug.error("verifyCertificate - No CRL distribution Point configured");
                return null;
            }

            if (results.isReference()) {
                debug.warning("Getting CRL but got LDAP reference: {}", results.readReference());
                return null;
            }

            SearchResultEntry entry = results.readEntry();

            /* 
            * Retrieve the certificate revocation list if available.
            */
            Attribute crlAttribute = entry.getAttribute(CERTIFICATE_REVOCATION_LIST);
            if (crlAttribute == null) {
                crlAttribute = entry.getAttribute(CERTIFICATE_REVOCATION_LIST_BINARY);
                if (crlAttribute == null) {
                    debug.error("verifyCertificate - No CRL distribution Point configured");
                    return null;
                }
            }

            crl = crlBytesFrom(crlAttribute);

        } catch (Exception e) {
            debug.error("getCRLByLdapURI : Error in getting CRL", e);
        }

        return crl;
    }

    /**
     * Reads the CRL out of an attribute served by an LDAP distribution point, refusing anything
     * over {@link #CRL_DP_MAX_SIZE}. The server is one the certificate named, so its answer is as
     * untrusted as an HTTP response body and gets the same cap.
     *
     * @return null if the value is too large to accept.
     */
    static byte[] crlBytesFrom(Attribute crlAttribute) {
        ByteString value = crlAttribute.firstValue();
        long maxSize = getLongProperty(CRL_DP_MAX_SIZE, DEFAULT_CRL_DP_MAX_SIZE);
        if (value.length() > maxSize) {
            debug.error("AMCRLStore.crlBytesFrom: CRL of " + value.length()
                    + " bytes exceeds the maximum of " + maxSize);
            return null;
        }
        return value.toByteArray();
    }

    /**
     * Fetches a CRL over HTTP, following at most {@link #MAX_CRL_DP_REDIRECTS} redirects.
     *
     * <p>Every location is put through {@link #isCrlDpUrlAllowed(String)} first, the one the
     * certificate named and every {@code Location} after it alike: a 3xx off the legitimate
     * distribution point is a URL nothing has checked, and following it blindly — as the JDK
     * would — hands the address check straight back to whoever answers. Refusing 3xx outright was
     * the first fix here and it broke the distribution points that legitimately sit behind a CDN
     * or an http-to-https redirector, with no property to turn it back on.
     *
     * <p>A redirect is never followed from {@code https} down to {@code http}: a 302 must not be a
     * way to move a CRL fetch onto a transport where the answer can be replaced wholesale. The
     * query parameters from {@code storeParam.getURIParams()} are OpenAM's own configuration and
     * go only to a host {@link #CRL_DP_PARAM_HOSTS} names. A 301 or 302 carries them on only to
     * the same URL on another scheme or port; every other redirect, and everything after a 303,
     * is followed as a plain GET.
     *
     * <p>{@link #CRL_DP_TIMEOUT} is the budget for the whole search, not for a hop and not even
     * for this location: the {@link FetchBudget} passed in is the certificate's, and what is left
     * of it is re-checked before each hop and after each phase of it. A socket operation already
     * in flight when the budget runs out is still only noticed when it returns, so one location
     * can overshoot by the timeout of a single operation — bounded, and not the
     * ({@link #MAX_CRL_DP_REDIRECTS} + 1) x locations x chain-length multiple it would be if each
     * hop started a budget of its own.
     */
    byte[] getCRLByHttpURI(String url) {
        return getCRLByHttpURI(url, FetchBudget.forOneCertificate());
    }

    /**
     * As {@link #getCRLByHttpURI(String)}, but sharing one certificate's {@link FetchBudget}
     * rather than starting a fresh one.
     */
    byte[] getCRLByHttpURI(String url, FetchBudget budget) {
        String location = url;
        String uriParams = null;

        for (int hop = 0; ; hop++) {
            if (budget.isExpired()) {
                debug.error("AMCRLStore.getCRLByHttpURI: the " + CRL_DP_TIMEOUT
                        + " budget for " + url + " was spent before " + location);
                return null;
            }
            if (!isCrlDpUrlAllowed(location)) {
                debug.error("AMCRLStore.getCRLByHttpURI: refusing to fetch CRL distribution point "
                        + location + refusalHint());
                return null;
            }
            if (hop == 0) {
                // Charged here rather than by the caller: a location refused above was never
                // contacted, and must not cost the certificate one of the few locations it is
                // allowed to try. Redirects are free — they are bounded by MAX_CRL_DP_REDIRECTS.
                budget.spendLocation();
                uriParams = paramsFor(location);
            }

            HttpURLConnection con = null;
            try {
                con = openCrlDpConnection(location, uriParams, budget.remainingMillis());

                // Between phases, because neither connecting nor reading the status line can be
                // interrupted once started: without these a stalling location costs the connect
                // timeout, then the read timeout, then another read timeout in the body loop.
                if (budget.isExpired()) {
                    debug.error("AMCRLStore.getCRLByHttpURI: the " + CRL_DP_TIMEOUT
                            + " budget for " + url + " was spent connecting to " + location);
                    return null;
                }
                int responseCode = con.getResponseCode();
                if (budget.isExpired()) {
                    debug.error("AMCRLStore.getCRLByHttpURI: the " + CRL_DP_TIMEOUT
                            + " budget for " + url + " was spent waiting for " + location);
                    return null;
                }
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    byte[] crl = readCrlBody(con, budget);
                    if (debug.messageEnabled() && crl != null) {
                        debug.message("AMCRLStore.getCRLByHttpURI: crl.length = " + crl.length);
                    }
                    return crl;
                }

                String next = redirectTarget(con, responseCode, location);
                if (next == null) {
                    // An error page, a login form or a redirect this code will not follow.
                    debug.error("AMCRLStore.getCRLByHttpURI: response code " + responseCode
                            + " from " + location);
                    return null;
                }
                if (hop >= MAX_CRL_DP_REDIRECTS) {
                    debug.error("AMCRLStore.getCRLByHttpURI: more than " + MAX_CRL_DP_REDIRECTS
                            + " redirects from " + url + ", giving up");
                    return null;
                }
                // Carried on only to the same URL on another scheme or port — an http-to-https
                // redirector in front of the deployment's own endpoint. That keeps the host, and
                // so the list's answer for it, and cannot be a Post/Redirect/Get, which points
                // elsewhere. Any other redirect, and anything after a 303, is followed as a GET,
                // as the JDK itself follows a redirected POST: the Location is whatever the last
                // host answered with, and it can name anyone, listed hosts included.
                if (responseCode == HttpURLConnection.HTTP_SEE_OTHER
                        || !onlySchemeOrPortDiffers(location, next)) {
                    uriParams = null;
                }
                location = next;
            } catch (Exception e) {
                debug.error("getCRLByHttpURI : Error in getting CRL", e);
                return null;
            } finally {
                // Every exit above has already read the body it wanted, and the rejection paths
                // deliberately read none of it. Without this a certificate naming several failing
                // distribution points pins a socket per location until the collector runs.
                if (con != null) {
                    con.disconnect();
                }
            }
        }
    }

    /**
     * Opens a connection to {@code url}, bounded by {@code remainingBudget}, posting
     * {@code uriParams} if there are any. Redirects are not followed by the connection itself —
     * {@link #getCRLByHttpURI(String)} follows them so it can re-check each location. The flag is
     * set per connection, not in the shared {@link HttpURLConnectionManager}, which other callers
     * rely on for redirects.
     *
     * <p>Package-private so a test can read back the timeouts this puts on the connection.
     */
    HttpURLConnection openCrlDpConnection(String url, String uriParams, long remainingBudget)
            throws IOException {
        HttpURLConnection con = newCrlDpConnection(new URL(url));
        // Owned here until it is returned: writing the parameter body below opens the socket, and
        // a peer that accepts the connection and then resets during the write throws out of this
        // method before the caller's own variable has been assigned — so the caller's finally
        // block sees null and disconnects nothing, leaving one socket per location per
        // certificate to the collector on the authentication path.
        boolean handedOver = false;
        try {
            configureAndPost(con, uriParams, remainingBudget);
            handedOver = true;
            return con;
        } finally {
            if (!handedOver) {
                con.disconnect();
            }
        }
    }

    /**
     * The connection {@link #openCrlDpConnection} configures. Package-private and overridable so
     * a test can supply one whose failure mode it controls; nothing else should override it.
     */
    HttpURLConnection newCrlDpConnection(URL url) throws IOException {
        return HttpURLConnectionManager.getConnection(url);
    }

    /** The half of {@link #openCrlDpConnection} that can throw with the socket already open. */
    private static void configureAndPost(HttpURLConnection con, String uriParams,
            long remainingBudget) throws IOException {
        con.setInstanceFollowRedirects(false);

        // The download budget is only tested between reads, so a peer that stops sending mid-body
        // is caught by the socket timeouts, not by the budget. Those come from
        // com.sun.identity.url.readTimeout and org.forgerock.openam.url.connectTimeout, which are
        // tuned for unrelated callers and may legitimately be 0 — meaning no timeout at all, i.e.
        // an authentication thread parked on a host that drips bytes out or blackholes SYNs.
        // Bound both by the budget, but never relax whatever the shared settings impose.
        int budgetTimeout = (int) Math.min(remainingBudget, Integer.MAX_VALUE);
        con.setConnectTimeout(boundedByBudget(con.getConnectTimeout(), budgetTimeout));
        con.setReadTimeout(boundedByBudget(con.getReadTimeout(), budgetTimeout));

        con.setDoInput(true);
        con.setUseCaches(false);

        byte[] paramsBytes = encodeUriParams(uriParams);
        if (paramsBytes.length > 0) {
            con.setDoOutput(true);
            con.setRequestProperty("Content-Length", Integer.toString(paramsBytes.length));
            try (BufferedOutputStream out = new BufferedOutputStream(con.getOutputStream())) {
                out.write(paramsBytes, 0, paramsBytes.length);
                out.flush();
            }
        }
    }

    /**
     * The timeout to put on one socket operation: whichever of the shared setting and what is
     * left of the fetch budget expires first, with 0 read as "no limit" rather than as the
     * tightest of the two.
     *
     * @return at least 1, since 0 would mean no timeout at all — the caller has already refused
     *     to open a connection once the budget is spent, so this only rounds up the last
     *     sub-millisecond sliver of it.
     */
    private static int boundedByBudget(int sharedTimeout, int budgetTimeout) {
        return Math.max(1, (sharedTimeout > 0)
                ? Math.min(sharedTimeout, budgetTimeout) : budgetTimeout);
    }

    /**
     * Encodes the configured {@code iplanet-am-auth-cert-param-get-crl} parameters as a form
     * body. Package-private for the test that pins the separator placement.
     */
    static byte[] encodeUriParams(String uriParamsCRL) throws IOException {
        if (uriParamsCRL == null) {
            return new byte[0];
        }
        StringBuilder params = new StringBuilder();
        StringTokenizer st1 = new StringTokenizer(uriParamsCRL, ",");
        while (st1.hasMoreTokens()) {
            String token = st1.nextToken();
            StringTokenizer st2 = new StringTokenizer(token, "=");
            if (st2.countTokens() != 2) {
                continue;
            }
            String param = st2.nextToken();
            String value = st2.nextToken();
            // Separator before each pair but the first, rather than after each pair that is
            // followed by another token: a trailing token that turns out to be malformed leaves
            // the separator appended for it behind, and the body ends in a stray "&".
            if (params.length() > 0) {
                params.append("&");
            }
            params.append(URLEncDec.encode(param)).append("=").append(URLEncDec.encode(value));
        }
        return params.toString().trim().getBytes("UTF-8");
    }

    /**
     * Resolves the {@code Location} of a redirect response against the URL it came from.
     *
     * <p>307 and 308 are deliberately absent: they require the original request to be repeated
     * unchanged, which for a distribution point that was POSTed to would mean replaying the body
     * to a location the certificate did not name. {@link HttpURLConnection} does not follow them
     * either.
     *
     * @return null if this is not a redirect this code follows, or if the location is unusable.
     */
    private static String redirectTarget(HttpURLConnection con, int responseCode, String from) {
        if (responseCode != HttpURLConnection.HTTP_MOVED_PERM
                && responseCode != HttpURLConnection.HTTP_MOVED_TEMP
                && responseCode != HttpURLConnection.HTTP_SEE_OTHER) {
            return null;
        }
        String location = con.getHeaderField("Location");
        if (location == null || location.isEmpty()) {
            return null;
        }
        final URL resolved;
        try {
            // Relative locations are legal, and resolving them here is what lets the result go
            // through the address check as an absolute URL.
            resolved = new URL(new URL(from), location);
        } catch (MalformedURLException e) {
            debug.error("AMCRLStore.redirectTarget: unusable Location " + location + " from "
                    + from, e);
            return null;
        }
        if (isSchemeDowngrade(from, resolved.getProtocol())) {
            debug.error("AMCRLStore.redirectTarget: refusing the redirect from " + from + " to "
                    + resolved + ", which would drop out of https");
            return null;
        }
        return resolved.toExternalForm();
    }

    /**
     * Whether {@code to} is {@code from} on another scheme or port and nothing else: same host,
     * same user info, same path and query. Any other difference, or a URL that does not parse,
     * is {@code false}.
     */
    static boolean onlySchemeOrPortDiffers(String from, String to) {
        try {
            URL source = new URL(from);
            URL target = new URL(to);
            return source.getHost().equalsIgnoreCase(target.getHost())
                    && Objects.equals(source.getUserInfo(), target.getUserInfo())
                    && source.getFile().equals(target.getFile());
        } catch (MalformedURLException e) {
            return false;
        }
    }

    /**
     * Whether following a redirect from {@code from} to a location with scheme {@code toScheme}
     * would take the fetch off TLS.
     *
     * <p>Whoever answers over plaintext can substitute the CRL wholesale, and
     * {@link #validateFetchedCrl} catches that only when an issuer key is reachable — in the
     * {@link CrlUpdateDecision#USE_ONLY} case there is none. Browsers refuse the same downgrade
     * and no legitimate distribution point needs it. The other direction, http to https, is a
     * perfectly ordinary front end and stays allowed.
     */
    static boolean isSchemeDowngrade(String from, String toScheme) {
        return from != null && from.regionMatches(true, 0, "https:", 0, "https:".length())
                && !"https".equalsIgnoreCase(toScheme);
    }

    /**
     * How many refused parameter hosts {@link #warnedParamHosts} remembers: it is bounded because
     * the hosts come out of the certificates being validated, and an unbounded set of them would
     * be one more thing a certificate can grow in the server.
     */
    private static final int WARNED_PARAM_HOSTS_MAX_SIZE = 100;

    /** Hosts {@link #paramsFor} has already refused to send the configured parameters to. */
    private static final Set<String> warnedParamHosts = Collections.newSetFromMap(
            Collections.synchronizedMap(new LinkedHashMap<String, Boolean>(16, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > WARNED_PARAM_HOSTS_MAX_SIZE;
                }
            }));

    /**
     * The configured HTTP parameters, if {@code url}'s host is one they may be sent to.
     *
     * <p>They are the deployment's own configuration and routinely carry a credential for its
     * CRL endpoint, while the URL is one the certificate under validation named. Posting them to
     * a host {@link #CRL_DP_PARAM_HOSTS} does not list would hand that credential to whoever the
     * certificate chose (GHSA-g5vw-v384-gw9v).
     *
     * @return null when nothing is configured, or when this host may not be sent it.
     */
    String paramsFor(String url) {
        String uriParams = storeParam.getURIParams();
        if (uriParams == null || uriParams.trim().isEmpty()) {
            return null;
        }
        final String host;
        try {
            host = new URL(url).getHost();
        } catch (MalformedURLException e) {
            return null;
        }
        String allowed = getProperty(CRL_DP_PARAM_HOSTS);
        if (allowed != null && host != null) {
            for (StringTokenizer hosts = new StringTokenizer(allowed, ","); hosts.hasMoreTokens();) {
                String candidate = hosts.nextToken().trim();
                if (candidate.isEmpty()) {
                    continue;
                }
                // A leading dot is the domain form: ".example.com" covers "pki.example.com" but
                // not "notexample.com", and not "example.com" itself.
                boolean matches = candidate.startsWith(".")
                        ? host.regionMatches(true, host.length() - candidate.length(),
                                candidate, 0, candidate.length())
                        : candidate.equalsIgnoreCase(host);
                if (matches) {
                    return uriParams;
                }
            }
        }
        // Warned once per host, not once per location per certificate per authentication: this
        // is the default state of every deployment that has iplanet-am-auth-cert-param-get-crl
        // configured and has not set CRL_DP_PARAM_HOSTS yet — the ordinary upgrade case — where
        // warning on each fetch is a log-volume regression rather than a signal.
        boolean firstRefusal = warnedParamHosts.add(String.valueOf(host));
        if (firstRefusal || debug.messageEnabled()) {
            String refusal = "AMCRLStore.getCRLByHttpURI: not sending the configured CRL "
                    + "parameters to " + host + ", which " + CRL_DP_PARAM_HOSTS + " does not "
                    + "list. Add the distribution point hosts of this deployment's own PKI to "
                    + "that property if the CRL endpoint needs them.";
            if (firstRefusal) {
                debug.warning(refusal);
            } else {
                debug.message(refusal);
            }
        }
        return null;
    }

    private static byte[] readCrlBody(HttpURLConnection con, FetchBudget budget)
            throws IOException {
        long maxSize = getLongProperty(CRL_DP_MAX_SIZE, DEFAULT_CRL_DP_MAX_SIZE);
        if (con.getContentLengthLong() > maxSize) {
            debug.error("AMCRLStore.getCRLByHttpURI: declared CRL size "
                    + con.getContentLengthLong() + " exceeds the maximum of " + maxSize);
            return null;
        }
        try (InputStream in = con.getInputStream()) {
            return readBounded(in, maxSize, budget);
        }
    }

    /**
     * Reads at most {@code maxBytes} from {@code in}, giving up once {@code budget} is spent.
     *
     * <p>A connection's read timeout only bounds a single {@code read()}, so without a total
     * size and a wall-clock budget a host that drips bytes out slowly can hold an
     * authentication thread indefinitely and grow the buffer without limit.
     *
     * @throws IOException if either bound is exceeded, so the caller discards the response.
     */
    private static byte[] readBounded(InputStream in, long maxBytes, FetchBudget budget)
            throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int len;
        while ((len = in.read(buf, 0, buf.length)) != -1) {
            if (bos.size() + (long) len > maxBytes) {
                throw new IOException("CRL exceeds the maximum of " + maxBytes + " bytes");
            }
            bos.write(buf, 0, len);
            if (budget.isExpired()) {
                throw new IOException("CRL download exceeded its time budget");
            }
        }
        return bos.toByteArray();
    }

    // It returns NextCRLUpdate for current cached CRL
    // It gets CRL from crlAttribue member variable
    private boolean needCRLUpdate(X509CRL crl) {
        Date nextCRLUpdate = null;
        if (crl == null) {
            return true;
        }
        // Check CRLNextUpdate in CRL
        nextCRLUpdate = crl.getNextUpdate();
        if (debug.messageEnabled()) {
            debug.message("AMCRLStore.needCRLUpdate: nextCrlUpdate = " +
                    nextCRLUpdate);
        }

        return ((nextCRLUpdate != null) && nextCRLUpdate.before(newDate()));
    }

    /**
     * It gets the new CRL from ldap server.
     * If it is ldap URI, the URI has to be a dn that can be accessed
     * with ldap anonymous bind.
     * (example : ldap://server:port/uid=ca,o=company.com)
     * This dn entry has to have CRL in attribute certificaterevocationlist
     * or certificaterevocationlist;binary.
     * <p>
     * if attrNames does only contain one value the ldap search filter will be
     * (attrName=Value_of_the_corresponding_Attribute_from_SubjectDN)
     * e.g. SubjectDN of issuer cert 'C=BE, CN=Citizen CA, serialNumber=201007'
     * attrNames is 'CN', search filter used will be (CN=Citizen CA)
     * <p>
     * if attrNames does contain serveral values the ldap search filter value will be
     * a comma separated list of name attribute values, the search attribute will be 'cn'
     * (cn="attrNames[0]=Value_of_the_corresponding_Attribute_from_SubjectDN,
     * attrNames[1]=Value_of_the_corresponding_Attribute_from_SubjectDN")
     * <p>
     * e.g. SubjectDN of issuer cert 'C=BE, CN=Citizen CA, serialNumber=201007'
     * attrNames is {"CN","serialNumber"}, search filter used will be
     * (cn=CN=Citizen CA,serialNumber=201007)
     * <p>
     * The order of the values of attrNames matter as they must match the value of the
     * 'cn' attribute of a crlDistributionPoint entry in the directory server
     *
     * @param ldapParam the LDAP certificate store parameters used to connect to the store
     * @param cert the certificate whose issuer CRL should be retrieved
     * @param attrNames the attribute names from the subjectDN of the issuer cert used to build the search filter
     * @return the CRL retrieved for the certificate's issuer, or null if none was found
     */
    public static X509CRL getCRL(AMLDAPCertStoreParameters ldapParam, X509Certificate cert, String... attrNames) {
        return getCRL(ldapParam, cert, null, attrNames);
    }

    /**
     * As {@link #getCRL(AMLDAPCertStoreParameters, X509Certificate, String...)}, but passes the
     * presented certificate chain on to {@link #getCRL(X509Certificate, X509Certificate[])} so a
     * CRL issued by an intermediate CA can be verified and stored rather than refetched every
     * time.
     *
     * @param chain the chain {@code cert} was presented with, which the caller must already have
     *     validated. May be null.
     */
    public static X509CRL getCRL(AMLDAPCertStoreParameters ldapParam, X509Certificate cert,
            X509Certificate[] chain, String... attrNames) {
        return getCRLWithLazyChain(ldapParam, cert, () -> chain, attrNames);
    }

    /**
     * As {@link #getCRL(AMLDAPCertStoreParameters, X509Certificate, X509Certificate[], String...)},
     * but resolving the chain only if a CRL is actually fetched from a distribution point.
     *
     * <p>Deliberately not another {@code getCRL} overload: a caller spelling "no chain" as a null
     * literal would then have two applicable four-argument signatures to choose between.
     *
     * @param chain resolves the chain {@code cert} was presented with, which the caller must
     *     already have validated. Called at most once, and not at all unless a fetched CRL has to
     *     be verified.
     */
    static X509CRL getCRLWithLazyChain(AMLDAPCertStoreParameters ldapParam, X509Certificate cert,
            ChainResolver chain, String... attrNames) {
        X509CRL crl = null;

        try {
            if (!ArrayUtils.isEmpty(attrNames)) {
                X500Principal issuerPrincipal = cert.getIssuerX500Principal();
                String searchFilter;


                if (attrNames.length < 2) {
                    /*
                     * Get the CN of the input certificate
                     */
                    String attrValue = null;

                    // Retrieve attribute value of the attribute name
                    attrValue = CertUtils.getAttributeValue(issuerPrincipal, attrNames[0]);

                    if (null == attrValue) {
                        return crl;
                    }

                    searchFilter = setSearchFilter(attrNames[0], attrValue);


                } else {
                    String searchFilterValue = buildSearchFilterValue(attrNames, issuerPrincipal);

                    if (searchFilterValue.isEmpty()) {
                        return crl;
                    }
                    searchFilter = setSearchFilter("cn", searchFilterValue);
                }

                if (debug.messageEnabled()) {
                    debug.message("AMCRLStore:getCRL using searchFilter " + searchFilter);
                }

                /*
                 * Lookup the certificate in the LDAP certificate directory
                 */

                // The store gets parameters of its own. It searches twice, and the second search
                // — the one that names the entry updateCRL then REPLACEs — runs after the chain
                // resolver, which looks the issuer up through AMCertStore with these same
                // parameters; CRLValidator shares them with every other thread as well. A filter
                // set on the caller's object would move the write onto whatever entry the last
                // of those searches named.
                AMLDAPCertStoreParameters storeParam = new AMLDAPCertStoreParameters(ldapParam);
                storeParam.setSearchFilter(searchFilter);

                AMCRLStore store = new AMCRLStore(storeParam);
                crl = store.getCRLWithLazyChain(cert, chain);
            }

        } catch (Exception e) {
            debug.error("AMCRLStore:getCRL ", e);
        }

        return crl;
    }

    private static String buildSearchFilterValue(String[] attrNames, X500Principal principal) {
        StringBuilder searchFilterBuilder = new StringBuilder();
        for (int i = 0; i < attrNames.length; i++) {
            String attrName = attrNames[i];
            String attrValue = CertUtils.getAttributeValue(principal, attrName);

            if (null != attrValue) {
                searchFilterBuilder.append(attrName);
                searchFilterBuilder.append("=");
                searchFilterBuilder.append(attrValue);
                if (i < attrNames.length - 1) {
                    searchFilterBuilder.append(",");
                }
            }
        }
        return searchFilterBuilder.toString();
    }
}

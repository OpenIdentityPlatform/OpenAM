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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.shared.security.crypto;

/**
 * Supported KeyStore types.
 *
 * @since 13.0.0
 */
public enum KeyStoreType {
    /**
     * The Sun proprietary Java Key Store format. May not be supported on non-Oracle JREs.
     *
     * @see <a
     * href="http://docs.oracle.com/javase/7/docs/technotes/guides/security/crypto/CryptoSpec.html#
     * KeystoreImplementation">Keystore Implementation</a>
     */
    JKS,
    /**
     * Alternative Sun proprietary Key Store format. Provides stronger cryptographic protections than {@link #JKS}
     * format. May not be supported on all platforms.
     *
     * @see <a
     * href="http://docs.oracle.com/javase/7/docs/technotes/guides/security/crypto/CryptoSpec.html#
     * KeystoreImplementation">Keystore Implementation</a>
     */
    JCEKS,
    /**
     * PKCS#11 cryptographic storage format for hardware security tokens/devices. Relies on platform-specific native
     * support.
     *
     * @see <a href="https://docs.oracle.com/javase/7/docs/technotes/guides/security/p11guide.html">Java PKCS#11
     * Reference Guide</a>
     */
    PKCS11,
    /**
     * PKCS#12 key store format. Supported on all platforms.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7292">RFC 7292: PKCS #12</a>
     */
    PKCS12
}

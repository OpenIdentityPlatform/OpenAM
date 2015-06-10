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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.tokengeneration.oidc.crypto;

import org.forgerock.openam.sts.TokenCreationException;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Defines the concerns related to the signing of OpenIdConnect tokens
 */
public interface OpenIdConnectTokenPKIProvider {
    /**
     * Get the OpenIdConnect Provider's PrivateKey. Used to sign the OpenIdConnect token.
     * @param keyAlias alias identifying the KeyStore PrivateKeyEntry
     * @param keyPassword password for the PrivateKeyEntry
     * @return  the non-null PrivateKey corresponding to this entry.
     * @throws TokenCreationException if a PrivateKey entry could not be found
     */
    PrivateKey getProviderPrivateKey(String keyAlias, String keyPassword) throws TokenCreationException;

    /**
     * Get the OpenIdConnect Provider's X509Certificate[] corresponding to their PrivateKeyEntry. Used to create the
     * reference to the X509Certificate state corresponding to the OP's private key, as documented here:
     * https://tools.ietf.org/html/draft-ietf-jose-json-web-signature-41#section-4.1
     * @param keyAlias alias identifying the PrivateKeyEntry
     * @return  the non-null X509Certificate[] corresponding to this entry, as returned from the underlying KeyStore.
     * The leaf cert will be the first entry.
     * @throws TokenCreationException if an entry could not be found
     */
    X509Certificate[] getProviderCertificateChain(String keyAlias) throws TokenCreationException;
}

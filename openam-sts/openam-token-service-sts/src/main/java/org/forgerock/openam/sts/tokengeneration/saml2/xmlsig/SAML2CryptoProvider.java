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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.tokengeneration.saml2.xmlsig;

import org.forgerock.openam.sts.TokenCreationException;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Encapsulates the concerns of obtaining the Crypto context necessary to sign and encrypt SAML2 assertions.
 */
public interface SAML2CryptoProvider {
    /**
     * Returns the IDP's X509Certificate. Is the X509Certificate associated with the PrivateKeyEntry used to sign
     * SAML2 assertions (note this should really return a X509Certificate[], but the OpenAM Assertion class, which
     * encapsulates assertion signing, only allows for a single X509Certificate).
     * @param certAlias the alias corresponding to the SAML2 IdentityProvider's PrivateKeyEntry
     * @return the X509Certificate - null will not be returned.
     * @throws TokenCreationException if no entry corresponds to the specified alias
     */
    X509Certificate getIDPX509Certificate(String certAlias) throws TokenCreationException;

    /**
     * Returns the SP's X509Certificate. Is the X509Certificate with TrustedCertificateEntry corresponding to the SAML2
     * ServiceProvider corresponding to the published STS instance. Used to encrypt the generated symmetric key used to
     * encrypt assertion state. In other words, SAML2 assertion encryption involves the generation of a symmetric key, which
     * is used to encrypt the assertion, which includes the generated symmetric key, which is encrypted with the SP's
     * public key.
     * @param certAlias the alias corresponding to the SAML2 ServiceProviders TrustedCertificateEntry
     * @return the X509Certificate - null will not be returned.
     * @throws TokenCreationException if no entry corresponds to the specified alias
     */
    X509Certificate getSPX509Certificate(String certAlias) throws TokenCreationException;

    /**
     * Returns the PrivateKey corresponding to the PrivateKeyEntry containing the SAML2 Identity Provider's private key.
     * @param keyAlias the alias referencing the PrivateKeyEntry
     * @param keyPassword the password for the PrivateKeyEntry
     * @return the PrivateKey - null will not be returned
     * @throws TokenCreationException if no entry could be returned corresponding to the alias-password combination
     */
    PrivateKey getIDPPrivateKey(String keyAlias, String keyPassword) throws TokenCreationException;
}

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
import org.forgerock.openam.sts.config.user.OpenIdConnectTokenConfig;
import org.forgerock.openam.sts.tokengeneration.STSCryptoProviderBase;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * @see OpenIdConnectTokenPKIProvider
 */
public class OpenIdConnectTokenPKIProviderImpl extends STSCryptoProviderBase implements OpenIdConnectTokenPKIProvider {

    public OpenIdConnectTokenPKIProviderImpl(final OpenIdConnectTokenConfig tokenConfiguration) throws TokenCreationException{
        //TODO: might want to make the keystore type configurable at some point - see AME-646
        super(tokenConfiguration.getKeystoreLocation(), tokenConfiguration.getKeystorePassword(), JKS_KEYSTORE);
    }

    @Override
    public PrivateKey getProviderPrivateKey(String keyAlias, String keyPassword) throws TokenCreationException {
        return getPrivateKey(keyAlias, keyPassword);
    }

    @Override
    public X509Certificate[] getProviderCertificateChain(String keyAlias) throws TokenCreationException {
        /*
        From the KeyStore javadocs:
        for getCertificateChain:
        The certificate chain must have been associated with the alias by a call to setKeyEntry, or by a call to setEntry with a PrivateKeyEntry.
        for getCertificate:
        If the given alias name identifies an entry created by a call to setCertificateEntry, or created by a call to
        setEntry with a TrustedCertificateEntry, then the trusted certificate contained in that entry is returned.
        If the given alias name identifies an entry created by a call to setKeyEntry, or created by a call to setEntry with a
        PrivateKeyEntry, then the first element of the certificate chain in that entry is returned.

        Thus getCertificate will get X509Certificate state for all cases handled by getCertificateChain, but the X509Certificate
        state which corresponds to a given PrivateKey is most correctly represented as a X509Certificate[]. However, it seems
        that the X509Certificate state corresponding to a PrivateKeyEntry is often entered as a TrustedCertificateEntry - e.g.
        the default keystore.jks bundled with OpenAM is created in that fashion. So this implementation must handle both
        cases.
         */
        try {
            return getX509CertificateChain(keyAlias);
        } catch (TokenCreationException e) {
            return new X509Certificate[] { getX509Certificate(keyAlias) };
        }
    }
}

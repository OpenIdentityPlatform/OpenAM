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
import org.forgerock.openam.sts.config.user.SAML2Config;
import org.forgerock.openam.sts.tokengeneration.STSCryptoProviderBase;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * @see SAML2CryptoProvider
 */
public class SAML2CryptoProviderImpl extends STSCryptoProviderBase implements SAML2CryptoProvider {
    /*
    ctor not guice-injected as instance created by STSInstanceStateImpl
     */
    public SAML2CryptoProviderImpl(final SAML2Config saml2Configuration) throws TokenCreationException {
        //TODO: might want to make the keystore type configurable at some point - see AME-646
        super(saml2Configuration.getKeystoreFileName(), saml2Configuration.getKeystorePassword(), JKS_KEYSTORE);
    }

    @Override
    public X509Certificate getIDPX509Certificate(String certAlias) throws TokenCreationException {
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
        Note also that this method should really return a X509Certificate[], but because the OpenAM certificate signing
        functionality only takes a single X509Certificate instance (see com.sun.identity.saml2.assertion.Assertion#sign for details),
        only a single X509Certificate will be returned.
         */
        try {
            return getX509CertificateChain(certAlias)[0];
        } catch (TokenCreationException e) {
            return getX509Certificate(certAlias);
        }
    }

    @Override
    public X509Certificate getSPX509Certificate(String certAlias) throws TokenCreationException {
        return getX509Certificate(certAlias);
    }

    @Override
    public PrivateKey getIDPPrivateKey(String keyAlias, String keyPassword) throws TokenCreationException {
        return getPrivateKey(keyAlias, keyPassword);
    }
}

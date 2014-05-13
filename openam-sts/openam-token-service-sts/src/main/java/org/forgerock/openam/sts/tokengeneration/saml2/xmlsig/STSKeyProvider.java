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
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.tokengeneration.saml2.xmlsig;

import org.forgerock.openam.sts.TokenCreationException;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * This interface expresses the concerns related to obtaining the STS-instance-specific X509Certificate and PrivateKey
 * instances necessary to sign SAML2 assertions. Unlike the existing com.sun.identity.saml.xmlsig.KeyProvider interface,
 * and its primary implementation, the AMKeyProvider, this KeyProvider cannot be initialized by fileystem-resident
 * configuration files, but via STS KeystoreConfig instances. Also its PrivateKeys will not be 'protected' by the Crypt
 * class' encryption scheme, as these key passwords will be stored in LDAP.
 */
public interface STSKeyProvider {
    X509Certificate getX509Certificate(String certAlias) throws TokenCreationException;
    PrivateKey getPrivateKey(String keyAlias, String keyPassword) throws TokenCreationException;
}

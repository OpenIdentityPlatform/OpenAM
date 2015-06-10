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

import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.TokenCreationException;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * All of the rest-sts-instance-specific state necessary for the TokenGenerationService to generate instance-specific
 * assertions is cached in the RestSTSInstanceStateProvider. The RestSTSInstanceState instances which it provides
 * encapsulate access to the SAML2CryptoProvider, which is an interface defining the concerns for providing the crypto context
 * necessary to sign and/or encrypt assertions. I want all of this state to be initialized upon construction, so that
 * the RestSTSInstanceState instances cached by the RestSTSInstanceStateProvider are immutable. However, it is possible
 * that a rest-sts instance does not require assertion signing or encryption, and thus the keystore configuration is
 * not necessary. The SAML2CryptoProviderImpl will throw an exception when missing keystore state prevents successful
 * initialization, which had been forcing the user to input this state. This class is the solution to this problem -
 * a SAML2CryptoProvider implementation which does not require keystore configuration, and can be created by the
 * SAML2CryptoProviderFactoryImpl when the SAML2Config corresponding to the rest-sts instance does not require signing or
 * encryption support.
 *
 * An exception will be thrown if any of these methods are ever invoked. They are only invoked in the context of
 * signing or encrypting generated assertions. Note that these methods should never be invoked because the SAML2CryptoProvider
 * is encapsulated in a RestSTSInstanceState instance cached in the RestSTSInstanceStateProvider. A new RestSTSInstanceState
 * instance is created and cached the first time a given rest-sts instance consumes the TokenGenerationService - see
 * RestSTSInstanceStateFactoryImpl and the SAML2CryptoProviderFactoryImpl. The bottom line is that an instance of the
 * FauxSAML2CryptoProvider will be set as the SAML2CryptoProvider in the RestSTSInstanceState ONLY if the SAML2Config corresponding
 * to the rest-sts instance indicates no assertion encryption or signing support. If this is the case, the SAML2CryptoProvider
 * will never be referenced.
 */
public class FauxSAML2CryptoProvider implements SAML2CryptoProvider {

    @Override
    public X509Certificate getIDPX509Certificate(String certAlias) throws TokenCreationException {
        throw new TokenCreationException(ResourceException.INTERNAL_ERROR, "Exception in SAML2CryptoProvider caching logic: " +
                "getIDPX509Certificate called on an instance of the FauxSAML2CryptoProvider. This should not happen, as " +
                "instances of this class should be instantiated only for a STS instance which either does not issue " +
                "SAML2 assertions, or issues SAML2 assertions which do not require signing or encryption support. Cause is " +
                "likely a failure in the caching support in the sts token service. Key alias: " + certAlias);
    }

    @Override
    public X509Certificate getSPX509Certificate(String certAlias) throws TokenCreationException {
        throw new TokenCreationException(ResourceException.INTERNAL_ERROR, "Exception in SAML2CryptoProvider caching logic: " +
                "getSPX509Certificate called on an instance of the FauxSAML2CryptoProvider. This should not happen, as " +
                "instances of this class should be instantiated only for a STS instance which either does not issue " +
                "SAML2 assertions, or issues SAML2 assertions which do not require signing or encryption support. Cause is " +
                "likely a failure in the caching support in the sts token service. Key alias: " + certAlias);
    }

    @Override
    public PrivateKey getIDPPrivateKey(String keyAlias, String keyPassword) throws TokenCreationException {
        throw new TokenCreationException(ResourceException.INTERNAL_ERROR, "Exception in SAML2CryptoProvider caching logic: " +
                "getIDPPrivateKey called on an instance of the FauxSAML2CryptoProvider. This should not happen, as " +
                "instances of this class should be instantiated only for a STS instance which either does not issue " +
                "SAML2 assertions, or issues SAML2 assertions which do not require signing or encryption support. Cause is " +
                "likely a failure in the caching support in the sts token service. Key alias: " + keyAlias);
    }
}

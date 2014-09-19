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
 * encapsulate access to the STSKeyProvider, which is an interface defining the concerns for providing the crypto context
 * necessary to sign and/or encrypt assertions. I want all of this state to be initialized upon construction, so that
 * the RestSTSInstanceState instances cached by the RestSTSInstanceStateProvider are immutable. However, it is possible
 * that a rest-sts instance does not require assertion signing or encryption, and thus the keystore configuration is
 * not necessary. The STSKeyProviderImpl will throw an exception when missing keystore state prevents successful
 * initialization, which had been forcing the user to input this state. This class is the solution to this problem -
 * a STSKeyProvider implementation which does not require keystore configuration, and can be created by the
 * STSKeyProviderFactoryImpl when the SAML2Config corresponding to the rest-sts instance does not require signing or
 * encryption support.
 *
 * An exception will be thrown if any of these methods are ever invoked. They are only invoked in the context of
 * signing or encrypting generated assertions. Note that these methods should never be invoked because the STSKeyProvider
 * is encapsulated in a RestSTSInstanceState instance cached in the RestSTSInstanceStateProvider. A new RestSTSInstanceState
 * instance is created and cached the first time a given rest-sts instance consumes the TokenGenerationService - see
 * RestSTSInstanceStateFactoryImpl and the STSKeyProviderFactoryImpl. The bottom line is that an instance of the
 * FauxSTSKeyProvider will be set as the STSKeyProvider in the RestSTSInstanceState ONLY if the SAML2Config corresponding
 * to the rest-sts instance indicates no assertion encryption or signing support. If this is the case, the STSKeyProvider
 * will never be referenced.
 */
public class FauxSTSKeyProvider implements STSKeyProvider {
    @Override
    public X509Certificate getX509Certificate(String certAlias) throws TokenCreationException {
        throw new TokenCreationException(ResourceException.INTERNAL_ERROR, "Exception in STSKeyProvider caching logic: " +
                "getX509Certificate called on an instance of the FauxSTSKeyProvider. This should not happen, as " +
                "instances of this class should be instantiated only for a rest-sts instance which does not require " +
                "signing or encryption support.");
    }

    @Override
    public PrivateKey getPrivateKey(String keyAlias, String keyPassword) throws TokenCreationException {
        throw new TokenCreationException(ResourceException.INTERNAL_ERROR, "Exception in STSKeyProvider caching logic: " +
                "getPrivateKey called on an instance of the FauxSTSKeyProvider. This should not happen, as " +
                "instances of this class should be instantiated only for a rest-sts instance which does not require " +
                "signing or encryption support.");
    }
}

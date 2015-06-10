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

/**
 * Defines concern related to obtaining an instance of the SAML2CryptoProvider class.
 */
public interface SAML2CryptoProviderFactory {
    /**
     * Return the SAML2CryptoProvider encapsulating the crypto context corresponding to the SAML2Config state
     * @param config the SAML2Config instance corresponding to the STS instance consuming the token service.
     * @return a non-null SAML2CryptoProvider implementation
     * @throws TokenCreationException if the crypto provider could not be instantiated - usually due to incorrect KeyStore
     * configuration state.
     */
    SAML2CryptoProvider createSAML2CryptoProvider(SAML2Config config) throws TokenCreationException;
}

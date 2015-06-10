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

package org.forgerock.openam.sts.tokengeneration;

import org.forgerock.openam.sts.TokenCreationException;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;

public class STSCryptoProviderBaseTest {
    @Test
    public void testKeystoreLoadAndKeyRetrieval() throws TokenCreationException {
        STSCryptoProviderBase cryptoProvider = new STSCryptoProviderBase("keystore.jks", "changeit".getBytes(), "JKS") {};
        assertNotNull(cryptoProvider.getX509Certificate("test"));
        assertNotNull(cryptoProvider.getPrivateKey("test", "changeit"));
    }
}

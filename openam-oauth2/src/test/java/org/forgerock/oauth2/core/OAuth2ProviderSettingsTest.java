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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.math.BigInteger;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;
import java.util.Map;

import com.sun.identity.shared.encode.Hash;
import org.forgerock.json.jose.jwk.KeyUse;
import org.forgerock.json.jose.jws.SupportedEllipticCurve;
import org.forgerock.util.encode.Base64url;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class OAuth2ProviderSettingsTest {

    @Test(dataProvider = "ellipticCurves")
    public void shouldGenerateCorrectEllipticCurveJWKs(SupportedEllipticCurve curve) throws Exception {
        // Given
        String alias = "myKeyAlias";
        ECPublicKey key = mock(ECPublicKey.class);
        BigInteger x = BigInteger.ONE;
        BigInteger y = BigInteger.TEN;

        given(key.getW()).willReturn(new ECPoint(x, y));
        given(key.getParams()).willReturn(curve.getParameters());

        // When
        final Map<String, Object> jwk = OAuth2ProviderSettings.createECJWK(alias, key, KeyUse.SIG);

        // Then
        assertThat(jwk).containsEntry("kty", "EC")
                .containsEntry("kid", Hash.hash(alias + ":" + curve.getStandardName() + ":" + x.toString() + ":" + y
                        .toString()))
                .containsEntry("use", "sig")
                .containsEntry("alg", curve.getJwsAlgorithm().name())
                .containsEntry("x", Base64url.encode(x.toByteArray()))
                .containsEntry("y", Base64url.encode(y.toByteArray()))
                .containsEntry("crv", curve.getStandardName());
    }

    @DataProvider
    public static Object[][] ellipticCurves() {
        return new Object[][] {
                { SupportedEllipticCurve.P256 },
                { SupportedEllipticCurve.P384 },
                { SupportedEllipticCurve.P521 }
        };
    }
}
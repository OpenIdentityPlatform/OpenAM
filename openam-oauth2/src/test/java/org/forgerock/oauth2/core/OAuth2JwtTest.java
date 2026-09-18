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
 * Copyright 2026 3A Systems, LLC.
 */

package org.forgerock.oauth2.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.util.encode.Base64url;
import org.testng.annotations.Test;

public class OAuth2JwtTest {

    @Test
    public void getSigningAlgorithmReturnsHeaderAlgorithm() {
        String jwt = new JwtBuilderFactory()
                .jws(new SigningManager().newHmacSigningHandler("secret".getBytes(StandardCharsets.UTF_8)))
                .headers().alg(JwsAlgorithm.HS256).done()
                .claims(new JwtBuilderFactory().claims().sub("s").build())
                .build();

        assertThat(OAuth2Jwt.create(jwt).getSigningAlgorithm()).isEqualTo(JwsAlgorithm.HS256);
    }

    /**
     * {@code JwsHeader.getAlgorithm()} is {@code JwsAlgorithm.valueOf(alg)} and throws for the RFC
     * spelling {@code "none"}, for lower case and for anything it does not know; that is "no usable
     * algorithm", not a server error.
     */
    @Test
    public void getSigningAlgorithmReturnsNullForAlgorithmOutsideTheEnum() {
        assertThat(OAuth2Jwt.create(rawJwt("none")).getSigningAlgorithm()).isNull();
        assertThat(OAuth2Jwt.create(rawJwt("hs256")).getSigningAlgorithm()).isNull();
        assertThat(OAuth2Jwt.create(rawJwt("PS256")).getSigningAlgorithm()).isNull();
    }

    private static String rawJwt(String alg) {
        String header = Base64url.encode(("{\"typ\":\"JWT\",\"alg\":\"" + alg + "\"}").getBytes(StandardCharsets.UTF_8));
        String payload = Base64url.encode("{\"sub\":\"s\"}".getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".";
    }
}

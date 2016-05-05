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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.openam.openidconnect;

import static org.forgerock.oauth2.core.Utils.*;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.List;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openidconnect.OpenIdConnectToken;

/**
 * Models an OpenAM OpenId Connect Token.
 *
 * @since 12.0.0
 */
public class OpenAMOpenIdConnectToken extends OpenIdConnectToken {

    /**
     * Constructs a new OpenAMOpenIdConnectToken.
     *
     * @param signingKeyId The signing key id.
     * @param encryptionKeyId The encryption key id.
     * @param clientSecret The client's secret.
     * @param signingAlgorithm The signing algorithm.
     * @param encryptionAlgorithm The encryption algorithm.
     * @param encryptionMethod The encryption method.
     * @param signingKeyPair The token's signing key pair.
     * @param encryptionPublicKey The token's encryption public key.
     * @param isIDTokenEncryptionEnabled {@code true} if ID token encryption is enabled.
     * @param iss The issuer.
     * @param sub The subject.
     * @param aud The audience.
     * @param azp The authorized party.
     * @param exp The expiry time.
     * @param iat The issued at time.
     * @param authTime The authenticated time.
     * @param nonce The nonce.
     * @param ops The ops.
     * @param atHash The at_hash.
     * @param cHash The c_hash.
     * @param acr The acr.
     * @param amr The amr.
     * @param realm The realm.
     */
    public OpenAMOpenIdConnectToken(String signingKeyId, String encryptionKeyId, byte[] clientSecret,
            KeyPair signingKeyPair, PublicKey encryptionPublicKey, String signingAlgorithm, String encryptionAlgorithm,
            String encryptionMethod, boolean isIDTokenEncryptionEnabled, String iss, String sub, String aud, String azp,
            long exp, long iat, long authTime, String nonce, String ops, String atHash, String cHash, String acr,
            List<String> amr, String realm) {
        super(signingKeyId, encryptionKeyId, clientSecret, signingKeyPair, encryptionPublicKey, signingAlgorithm, encryptionAlgorithm,
                encryptionMethod, isIDTokenEncryptionEnabled, iss, sub, aud, azp, exp, iat, authTime, nonce, ops, atHash, cHash, acr, amr);
        setRealm(realm);
    }

    /**
     * Sets the realm.
     *
     * @param realm The realm.
     */
    private void setRealm(final String realm) {
        if (!isEmpty(realm)) {
            put(OAuth2Constants.CoreTokenParams.REALM, realm);
        }
    }
}

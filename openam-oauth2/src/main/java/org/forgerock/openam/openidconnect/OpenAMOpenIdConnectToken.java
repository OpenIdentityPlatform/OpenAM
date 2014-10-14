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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.openidconnect;

import org.forgerock.oauth2.core.OAuth2Constants;
import static org.forgerock.oauth2.core.Utils.isEmpty;
import org.forgerock.openidconnect.OpenIdConnectToken;

import java.util.List;

/**
 * Models an OpenAM OpenId Connect Token.
 *
 * @since 12.0.0
 */
public class OpenAMOpenIdConnectToken extends OpenIdConnectToken {

    /**
     * Constructs a new OpenAMOpenIdConnectToken.
     *
     * @param clientSecret The client's secret.
     * @param algorithm The algorithm.
     * @param iss The issuer.
     * @param sub The subject.
     * @param aud The audience.
     * @param azp The authorized party.
     * @param exp The expiry time.
     * @param iat The issued at time.
     * @param ath The authenticated time.
     * @param nonce The nonce.
     * @param ops The ops.
     * @param realm The realm.
     */
    public OpenAMOpenIdConnectToken(byte[] clientSecret, String algorithm, String iss, String sub,
            String aud, String azp, long exp, long iat, long ath, String nonce, String ops,
            String atHash, String acr, List<String> amr, String realm) {
        super(clientSecret, algorithm, iss, sub, aud, azp, exp, iat, ath, nonce, ops, atHash, acr, amr);
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

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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.sts.tokengeneration.oidc;

import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.jwt.JwtClaimsSetKey;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents an OpenIdConnect token, as produced by the STS. It is simply a JsonValue, with some convenience
 * methods to set claims.
 *
 * Note that it should be possible to set multiple audience members, but until OPENAM-5990 is addressed, only a single
 * audience member will be specified from the OpenIdConnectTokenConfig associated with a published sts instance.
 *
 * Note that this class originally subclassed the org.forgerock.openidconnect.OpenIdConnectToken, but I decided against
 * this approach because the OpenIdConnectToken#sign method does not support the various means by which the public key
 * corresponding to the signing key can be identified as specified in
 * https://tools.ietf.org/html/draft-ietf-jose-json-web-signature-41#section-4.
 * The bottom line is that the OpenIdConnectToken#sign method identifies the public key only via kid, and thus relies
 * upon on the jwk url exposed by the OpenAM OP. The STS cannot do the same, and thus must support a variety of public-key-
 * identification schemes. The OpenIdConnectToken also combines signing state and claim state - I would prefer to keep
 * this state separate. There are also some claims in the OpenIdConnectToken class (c_hash, at_hash - authorization
 * code hash and access-token hash, respectively) which don't apply in an STS context, as it
 * is not an OAuth2 provider. Thus this class must duplicate the claims-setting functionality in the OpenIdConnectToken class,
 * but is simply re-using constants and jsonValue setter methods.
 */
class STSOpenIdConnectToken extends JsonValue {
    STSOpenIdConnectToken() {
        super(new HashMap<String, Object>());
    }

    /**
     * Sets a value on the OpenId Connect token if the value is not null or an empty String.
     *
     * @param key The key. Must not be {@code null}.
     * @param value The value.
     */
    private void set(String key, String value) {
        if (!StringUtils.isEmpty(value)) {
            put(key, value);
        } else {
            throw new IllegalArgumentException("In STSOpenIdConnectToken, the " + key +
                    " claim was set with an empty value.");
        }
    }

    /**
     * Sets the issuer.
     *
     * @param iss The issuer.
     */
    STSOpenIdConnectToken setIss(String iss) {
        set(OAuth2Constants.JWTTokenParams.ISS, iss);
        return this;
    }

    /**
     * Sets the subject.
     *
     * @param sub The subject.
     */
    STSOpenIdConnectToken setSub(String sub) {
        set(OAuth2Constants.JWTTokenParams.SUB, sub);
        return this;
    }

    /**
     * Sets the audience.
     *
     * @param aud The audience.
     */
    STSOpenIdConnectToken setAud(List<String> aud) {
        if (CollectionUtils.isEmpty(aud)) {
            throw new IllegalArgumentException("Non-null, non-empty audience list must be specified.");
        }
        put(OAuth2Constants.JWTTokenParams.AUD, aud);
        return this;
    }

    /**
     * Sets the authorized party.
     *
     * @param azp The authorized party.
     */
    STSOpenIdConnectToken setAzp(String azp) {
        set(OAuth2Constants.JWTTokenParams.AZP, azp);
        return this;
    }

    /**
     * Sets the expiry time in seconds.
     *
     * @param exp The expiry time.
     */
    STSOpenIdConnectToken setExp(long exp) {
        put(OAuth2Constants.JWTTokenParams.EXP, exp);
        return this;
    }

    /**
     * Sets the issued at time in seconds.
     *
     * @param iat The issued at time.
     */
    STSOpenIdConnectToken setIat(long iat) {
        put(OAuth2Constants.JWTTokenParams.IAT, iat);
        return this;
    }

    /**
     * Sets the authenticated time in seconds.
     *
     * @param authTime The authenticated time.
     */
    STSOpenIdConnectToken setAuthTime(long authTime) {
        put(OAuth2Constants.JWTTokenParams.AUTH_TIME, authTime);
        return this;
    }

    /**
     * Sets the nonce.
     *
     * @param nonce The nonce.
     */
    STSOpenIdConnectToken setNonce(String nonce) {
        set(OAuth2Constants.JWTTokenParams.NONCE, nonce);
        return this;
    }

    /**
     * Sets the ops.
     *
     * @param id The jwt id.
     */
    STSOpenIdConnectToken setId(String id) {
        set(JwtClaimsSetKey.JTI.name(), id);
        return this;
    }

    /**
     * Sets the acr.
     *
     * @param acr The acr.
     */
    STSOpenIdConnectToken setAcr(String acr) {
        set(OAuth2Constants.JWTTokenParams.ACR, acr);
        return this;
    }

    /**
     * Sets the amr.
     *
     * @param amr The amr.
     */
    STSOpenIdConnectToken setAmr(List<String> amr) {
        put(OAuth2Constants.JWTTokenParams.AMR, amr);
        return this;
    }

    void setClaims(Map<String, String> privateClaims) {
        for (Map.Entry<String, String> claim : privateClaims.entrySet()) {
            put(claim.getKey(), claim.getValue());
        }
    }
}

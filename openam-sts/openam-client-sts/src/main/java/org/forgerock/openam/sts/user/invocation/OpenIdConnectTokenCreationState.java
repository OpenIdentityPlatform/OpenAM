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

package org.forgerock.openam.sts.user.invocation;

import org.forgerock.guava.common.base.Objects;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenType;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * This class encapsulates client-specified state necessary for the creation of an OpenIdConnect token. The state is
 * currently limited to a nonce (see the nonce claim: http://openid.net/specs/openid-connect-core-1_0.html#IDToken),
 * and potentially a representation of the user's authorization consent. Not sure about the authorization consent -
 * does the authorization consent apply to both the RO's resource-server-resident resource, AND their identity, or just
 * the RO's resource-server-resident resource? The two are very much entangled in the OAuth2 flows, and a representation
 * of the RO's identity is not provided unless resource-server-resident resource access is granted, so it might make sense
 * to explicitly represent granted access here...
 */
public class OpenIdConnectTokenCreationState {
    private static final int HASH_CONSTANT_ALLOW_ACCESS = 42;
    private static final int HASH_CONSTANT_DISALLOW_ACCESS = 43;
    public static class OpenIdConnectTokenCreationStateBuilder {
        private String nonce;
        // a value of true indicates that access to the RO's identity has been granted. It is necessary to cause an
        // OIDC token to be issued.
        private boolean allowAccess;

        private OpenIdConnectTokenCreationStateBuilder() {}

        public OpenIdConnectTokenCreationStateBuilder nonce(String nonce) {
            this.nonce = nonce;
            return this;
        }

        public OpenIdConnectTokenCreationStateBuilder allowAccess(boolean allowAccess) {
            this.allowAccess = allowAccess;
            return this;
        }

        public OpenIdConnectTokenCreationState build() {
            return new OpenIdConnectTokenCreationState(this);
        }
    }

    private static final String NONCE = "nonce";
    private static final String ALLOW_ACCESS = "allow_access";

    private final String nonce;
    private final boolean allowAccess;

    private OpenIdConnectTokenCreationState(OpenIdConnectTokenCreationStateBuilder builder) {
        this.nonce = builder.nonce;
        this.allowAccess = builder.allowAccess;
    }

    public static OpenIdConnectTokenCreationStateBuilder builder() {
        return new OpenIdConnectTokenCreationStateBuilder();
    }

    public String getNonce() {
        return nonce;
    }

    public boolean getAllowAccess() {
        return allowAccess;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof OpenIdConnectTokenCreationState) {
            OpenIdConnectTokenCreationState otherState = (OpenIdConnectTokenCreationState)other;
            return Objects.equal(nonce, otherState.nonce) && (allowAccess == otherState.allowAccess);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (nonce != null) {
            return nonce.hashCode();
        } else if (allowAccess) {
            return HASH_CONSTANT_ALLOW_ACCESS;
        } else {
            return HASH_CONSTANT_DISALLOW_ACCESS;
        }
    }

    public JsonValue toJson() {
        return json(object(
                field(AMSTSConstants.TOKEN_TYPE_KEY, TokenType.OPENIDCONNECT.name()),
                field(NONCE, nonce),
                field(ALLOW_ACCESS, allowAccess)));
    }

    public static OpenIdConnectTokenCreationState fromJson(JsonValue jsonValue) {
        return OpenIdConnectTokenCreationState.builder()
                .nonce(jsonValue.isDefined(NONCE) ? jsonValue.get(NONCE).asString() : null)
                .allowAccess(jsonValue.isDefined(ALLOW_ACCESS) ? jsonValue.get(ALLOW_ACCESS).asBoolean() : false)
                .build();
    }
}

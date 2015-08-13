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
import org.forgerock.json.JsonValue;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

/**
 * This class encapsulates state pertaining to a token issued by the STS.
 */
public class STSIssuedTokenState {
    /*
    The following two strings aid in creating the _queryFilter param to search for STS issued tokens when performing
    a resource-less GET on the TokenService endpoint.
     */
    public static final String STS_ID_QUERY_ATTRIBUTE = "sts_id";
    public static final String STS_TOKEN_PRINCIPAL_QUERY_ATTRIBUTE = "token_principal";
    public static class STSIssuedTokenStateBuilder {
        private String tokenId;
        private String stsId;
        private String principalName;
        private String tokenType;
        private long expirationTimeInSecondsFromEpoch;

        public STSIssuedTokenStateBuilder tokenId(String tokenId) {
            this.tokenId = tokenId;
            return this;
        }

        public STSIssuedTokenStateBuilder stsId(String stsId) {
            this.stsId = stsId;
            return this;
        }

        public STSIssuedTokenStateBuilder principalName(String principalName) {
            this.principalName = principalName;
            return this;
        }

        public STSIssuedTokenStateBuilder tokenType(String tokenType) {
            this.tokenType = tokenType;
            return this;
        }

        public STSIssuedTokenStateBuilder expirationTimeInSecondsFromEpoch(long expirationTimeInSecondsFromEpoch) {
            this.expirationTimeInSecondsFromEpoch = expirationTimeInSecondsFromEpoch;
            return this;
        }

        public STSIssuedTokenState build() {
            return new STSIssuedTokenState(this);
        }
    }
    private static final String TOKEN_ID = "token_id";
    private static final String STS_ID = "sts_id";
    private static final String PRINCIPAL_NAME = "principal_name";
    private static final String TOKEN_TYPE = "token_type";
    private static final String EXPIRATION_TIME = "expiration_time";

    private final String tokenId;
    private final String stsId;
    private final String principalName;
    private final String tokenType;
    private final long expirationTimeInSecondsFromEpoch;

    private STSIssuedTokenState(STSIssuedTokenStateBuilder builder) {
        tokenId = builder.tokenId;
        stsId = builder.stsId;
        principalName = builder.principalName;
        tokenType = builder.tokenType;
        this.expirationTimeInSecondsFromEpoch = builder.expirationTimeInSecondsFromEpoch;
    }

    public String getTokenId() {
        return tokenId;
    }

    public String getStsId() {
        return stsId;
    }

    public String getPrincipalName() {
        return principalName;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpirationTimeInSecondsFromEpoch() {
        return expirationTimeInSecondsFromEpoch;
    }

    public static STSIssuedTokenStateBuilder builder() {
        return  new STSIssuedTokenStateBuilder();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof STSIssuedTokenState) {
            STSIssuedTokenState otherState = (STSIssuedTokenState)other;
            return this.expirationTimeInSecondsFromEpoch == otherState.expirationTimeInSecondsFromEpoch &&
                    Objects.equal(this.principalName, otherState.principalName) &&
                    Objects.equal(this.stsId, otherState.stsId) &&
                    Objects.equal(this.tokenId, otherState.tokenId) &&
                    Objects.equal(this.tokenType, otherState.tokenType);
        }
        return false;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    public JsonValue toJson() {
        return json(object(
                field(TOKEN_ID, tokenId),
                field(STS_ID, stsId),
                field(PRINCIPAL_NAME, principalName),
                field(TOKEN_TYPE, tokenType),
                field(EXPIRATION_TIME, expirationTimeInSecondsFromEpoch)));

    }

    public static STSIssuedTokenState fromJson(JsonValue jsonValue) {
        return STSIssuedTokenState.builder()
                .tokenId(jsonValue.get(TOKEN_ID).asString())
                .stsId(jsonValue.get(STS_ID).asString())
                .principalName(jsonValue.get(PRINCIPAL_NAME).asString())
                .tokenType(jsonValue.get(TOKEN_TYPE).asString())
                .expirationTimeInSecondsFromEpoch(jsonValue.get(EXPIRATION_TIME).asLong())
                .build();
    }
}

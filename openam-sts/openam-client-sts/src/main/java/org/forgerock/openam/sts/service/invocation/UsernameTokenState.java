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

package org.forgerock.openam.sts.service.invocation;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.token.model.RestUsernameToken;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * Contains state corresponding to a UsernameToken and will emit json which includes state necessary to act as the
 * input_token_state in the RestSTSServiceInvocationState - i.e. the username and password state, and a
 * AMSTSConstants.TOKEN_TYPE_KEY field corresponding to TokenType.USERNAME.name().
 */
public class UsernameTokenState {
    public static class UsernameTokenStateBuilder {
        private byte[] username;
        private byte[] password;

        private UsernameTokenStateBuilder() {}

        public UsernameTokenStateBuilder username(byte[] username) {
            this.username = username;
            return this;
        }

        public UsernameTokenStateBuilder password(byte[] password) {
            this.password = password;
            return this;
        }

        public UsernameTokenState build() throws TokenMarshalException  {
            return new UsernameTokenState(this);
        }
    }
    private final RestUsernameToken restUsernameToken;

    private UsernameTokenState(UsernameTokenStateBuilder builder) throws TokenMarshalException {
        if (builder.username == null) {
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, "Username must be specified.");
        }
        if (builder.password == null) {
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, "Password must be specified.");
        }
        restUsernameToken = new RestUsernameToken(builder.username, builder.password);
    }

    public static UsernameTokenStateBuilder builder() {
        return new UsernameTokenStateBuilder();
    }

    public byte[] getUsername() {
        return restUsernameToken.getUsername();
    }

    public byte[] getPassword() {
        return restUsernameToken.getPassword();
    }

    public JsonValue toJson() throws TokenMarshalException {
        try {
            return json(object(
                    field(AMSTSConstants.TOKEN_TYPE_KEY, TokenType.USERNAME.name()),
                    field(AMSTSConstants.USERNAME_TOKEN_USERNAME, new String(restUsernameToken.getUsername(), AMSTSConstants.UTF_8_CHARSET_ID)),
                    field(AMSTSConstants.USERNAME_TOKEN_PASSWORD, new String(restUsernameToken.getPassword(), AMSTSConstants.UTF_8_CHARSET_ID))));
        } catch (UnsupportedEncodingException e) {
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, "Unsupported charset marshalling toJson: " + e);
        }
    }

    public static UsernameTokenState fromJson(JsonValue jsonValue) throws TokenMarshalException{
        try {
            return UsernameTokenState.builder()
                    .password(jsonValue.get(AMSTSConstants.USERNAME_TOKEN_PASSWORD).asString().getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                    .username(jsonValue.get(AMSTSConstants.USERNAME_TOKEN_USERNAME).asString().getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                    .build();
        } catch (UnsupportedEncodingException e) {
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, "Unsupported charset marshalling fromJson: " + e);
        }
    }

    @Override
    public String toString() {
        try {
            return toJson().toString();
        } catch (TokenMarshalException e) {
            return "Exception caught marshalling toJson: " + e;
        }
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof UsernameTokenState) {
            UsernameTokenState otherState = (UsernameTokenState)other;
            return Arrays.equals(restUsernameToken.getUsername(), otherState.getUsername()) &&
                    Arrays.equals(restUsernameToken.getPassword(), otherState.getPassword());
        }
        return false;
    }
}

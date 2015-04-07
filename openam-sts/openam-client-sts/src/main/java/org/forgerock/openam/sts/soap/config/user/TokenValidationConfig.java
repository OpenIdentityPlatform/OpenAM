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
 * Copyright 2015 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.soap.config.user;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.util.Reject;

import java.util.StringTokenizer;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * This class is used to configure the TokenValidation for soap-sts instances. Token validation is invoked in multiple
 * contexts in the soap-sts:
 * 1. As part of validating the SupportingTokens specified in SecurityPolicy bindings
 * 2. As part of validating the delegated tokens specified in the ActAs/OnBehalfOf elements in a RequestSecurityToken
 * 3. As part of the validate operation
 * Each soap-sts instance must deploy TokenValidators to handle each of these contexts, and each must be configured with
 * state which indicates whether the OpenAM session, generated as part of token validation, should be invalidated after
 * an output token is generated. This class provides the configuration state which allows these determinations to be made.
 * See: https://wikis.forgerock.org/confluence/display/AMPLAN/SOAP+STS%3A+Token+Validation+Context for more details.
 */
public class TokenValidationConfig {
    /*
    Must be same as the delimiter used in the supported-token-transforms AttributeSchema defined in soapSTS.xml.
     */
    private static final String DELIMETER = AMSTSConstants.PIPE;
    private static final String VALIDATED_TOKEN_TYPE = "validatedTokenType";
    private static final String INVALIDATE_INTERIM_OPENAM_SESSION = "invalidateInterimOpenAMSession";

    private final TokenType validatedTokenType;
    private final boolean invalidateInterimOpenAMSession;

    public TokenValidationConfig(TokenType validatedTokenType, boolean invalidateInterimOpenAMSession) {
        this.validatedTokenType = validatedTokenType;
        this.invalidateInterimOpenAMSession = invalidateInterimOpenAMSession;
        Reject.ifNull(validatedTokenType, "Input TokenType cannot be null");
    }


    public boolean invalidateInterimOpenAMSession() {
        return invalidateInterimOpenAMSession;
    }

    public TokenType getValidatedTokenType() {
        return validatedTokenType;
    }

    /*
    Don't include the invalidateInterimOpenAMSession in the equals comparison. We want the set of supported transforms
    to be specific only to the specified token types, not to whether interim tokens are invalidated.
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof TokenValidationConfig) {
            TokenValidationConfig otherConfig = (TokenValidationConfig)other;
            return validatedTokenType.equals(otherConfig.getValidatedTokenType()) &&
                    (invalidateInterimOpenAMSession == otherConfig.invalidateInterimOpenAMSession());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (validatedTokenType.name() + Boolean.toString(invalidateInterimOpenAMSession)).hashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder("validatedTokenType: ")
                .append(validatedTokenType.name())
                .append("; invalidateInterimimOpenAMSession: ")
                .append(invalidateInterimOpenAMSession)
                .toString();
    }
    /*
    Because the Map<String, Object> wrapped by the JsonValue returned by toJson will be used as the basis for the
    Map<String, Set<String>> to persist state to the SMS, all values must ultimately be stored as strings.
     */
    public JsonValue toJson() {
        return json(object(field(VALIDATED_TOKEN_TYPE, validatedTokenType.name()),
                field(INVALIDATE_INTERIM_OPENAM_SESSION, invalidateInterimOpenAMSession)));
    }

    public static TokenValidationConfig fromJson(JsonValue json) {
        return new TokenValidationConfig(
                Enum.valueOf(TokenType.class, json.get(VALIDATED_TOKEN_TYPE).asString()),
                json.get(INVALIDATE_INTERIM_OPENAM_SESSION).asBoolean());
    }

    /*
    A set of TokenValidationConfig instances is encapsulated in the SoapSTSInstanceConfig. When marshalling to a
    Map<String, Set<String>>, each of the TokenValidationConfig instances must be marshaled to a String, so that all of
    the supported TokenValidationConfig can have a representation in the Set<String>>.
     */
    public String toSMSString() {
        return validatedTokenType + DELIMETER + invalidateInterimOpenAMSession;
    }

    /*
    A set of TokenValidationConfig instances is encapsulated in the SoapSTSInstanceConfig. When marshaling back from the
    Set<String> representation of the set of supported token transforms, each TokenValidationConfig must be re-constituted
    from the representation ultimately generated by the toSMSString method. This method achieves that purpose.
     */
    public static TokenValidationConfig fromSMSString(String stringRepresentation) {
        StringTokenizer tokenizer = new StringTokenizer(stringRepresentation, DELIMETER);
        return new TokenValidationConfig(TokenType.valueOf(tokenizer.nextToken()), Boolean.valueOf(tokenizer.nextToken()));
    }
}

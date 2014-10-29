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
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.rest.config.user;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.util.Reject;

import java.util.StringTokenizer;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * This class defines the support token transformation operations supported by a REST STS deployment. The
 * invalidateInterimOpenAMSession boolean indicates whether any OpenAM session, generated via the validation of the
 * inputTokenType, should be invalidated following the generation of the outputTokenType.
 */
public class TokenTransformConfig {
    /*
    Must be same as the delimiter used in the supported-token-transforms AttributeSchema defined in restSTS.xml.
     */
    private static final String DELIMETER = AMSTSConstants.PIPE;
    private final TokenType inputTokenType;
    private final TokenType outputTokenType;
    private final boolean invalidateInterimOpenAMSession;
    public TokenTransformConfig(TokenType inputTokenType, TokenType outputTokenType, boolean invalidateInterimOpenAMSession) {
        this.inputTokenType = inputTokenType;
        this.outputTokenType = outputTokenType;
        this.invalidateInterimOpenAMSession = invalidateInterimOpenAMSession;
        Reject.ifNull(inputTokenType, "Input TokenType cannot be null");
        Reject.ifNull(outputTokenType, "Output TokenType cannot be null");
        if (!TokenType.SAML2.equals(outputTokenType)) {
            throw new IllegalArgumentException("Only output token types of SAML2 are currently supported. " +
                    "Invalid output token type: " + outputTokenType);
        }
        if (TokenType.SAML2.equals(inputTokenType)) {
            throw new IllegalArgumentException("SAML2 tokens are not supported as inputs to a token transformation.");
        }

        if (inputTokenType.equals(outputTokenType)) {
            throw new IllegalArgumentException("Input and output token types in transformation cannot be identical.");
        }
    }

    public TokenType getOutputTokenType() {
        return outputTokenType;
    }

    public boolean isInvalidateInterimOpenAMSession() {
        return invalidateInterimOpenAMSession;
    }

    public TokenType getInputTokenType() {
        return inputTokenType;
    }

    /*
    Don't include the invalidateInterimOpenAMSession in the equals comparison. We want the set of supported transforms
    to be specific only to the specified token types, not to whether interim tokens are invalidated.
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof TokenTransformConfig) {
            TokenTransformConfig otherConfig = (TokenTransformConfig)other;
            return inputTokenType.equals(otherConfig.getInputTokenType()) &&
                outputTokenType.equals(otherConfig.getOutputTokenType());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (inputTokenType.name() + outputTokenType.name()).hashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder("inputTokenType: ")
                .append(inputTokenType.name())
                .append("; outputTokenType: ")
                .append(outputTokenType.name())
                .append("; invalidateInterimimOpenAMSession: ")
                .append(invalidateInterimOpenAMSession)
                .toString();
    }
    /*
    Because the Map<String, Object> wrapped by the JsonValue returned by toJson will be used as the basis for the
    Map<String, Set<String>> to persist state to the SMS, all values must ultimately be stored as strings.
     */
    public JsonValue toJson() {
        return json(object(field("inputTokenType", inputTokenType.name()), field("outputTokenType", outputTokenType.name()),
                field("invalidateInterimOpenAMSession", invalidateInterimOpenAMSession)));
    }

    public static TokenTransformConfig fromJson(JsonValue json) {
        return new TokenTransformConfig(
                Enum.valueOf(TokenType.class, json.get("inputTokenType").asString()),
                Enum.valueOf(TokenType.class, json.get("outputTokenType").asString()),
                json.get("invalidateInterimOpenAMSession").asBoolean());
    }

    /*
    A set of TokenTransformConfig instances is encapsulated in the RestSTSInstanceConfig. When marshalling to a
    Map<String, Set<String>>, each of the TokenTransformConfig instances must be marshaled to a String, so that all of
    the supported TokenTransformConfig can have a representation in the Set<String>>.
     */
    public String toSMSString() {
        return inputTokenType + DELIMETER + outputTokenType + DELIMETER + invalidateInterimOpenAMSession;
    }

    /*
    A set of TokenTransformConfig instances is encapsulated in the RestSTSInstanceConfig. When marshaling back from the
    Set<String> representation of the set of supported token transforms, each TokenTransformConfig must be re-constituted
    from the representation ultimately generated by the toSMSString method. This method achieves that purpose.
     */
    public static TokenTransformConfig fromSMSString(String stringRepresentation) {
        StringTokenizer tokenizer = new StringTokenizer(stringRepresentation, DELIMETER);
        return new TokenTransformConfig(TokenType.valueOf(tokenizer.nextToken()),
                TokenType.valueOf(tokenizer.nextToken()), Boolean.valueOf(tokenizer.nextToken()));
    }
}

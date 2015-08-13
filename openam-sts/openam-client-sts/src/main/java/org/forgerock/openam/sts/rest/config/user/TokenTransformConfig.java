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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.rest.config.user;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenTypeId;
import org.forgerock.util.Reject;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

/**
 * This class defines the support token transformation operations supported by a REST STS deployment. The
 * invalidateInterimOpenAMSession boolean indicates whether any OpenAM session, generated via the validation of the
 * inputTokenType, should be invalidated following the generation of the outputTokenType.
 *
 * Internally, the input and output token types are stored as strings in order to support user-defined token
 * transformation operations.
 */
public class TokenTransformConfig {
    /*
    Must be same as the delimiter used in the supported-token-transforms AttributeSchema defined in restSTS.xml.
     */
    private static final String DELIMETER = AMSTSConstants.PIPE;
    private static final String REGEX_PIPE = "\\|";
    private static final String INPUT_TOKEN_TYPE = "inputTokenType";
    private static final String OUTPUT_TOKEN_TYPE = "outputTokenType";
    private static final String INVALIDATE_INTERIM_OPENAM_SESSION = "invalidateInterimOpenAMSession";
    private static final String SEMI_COLON = ";";
    private static final String COLON = ":";

    private final String inputTokenType;
    private final String outputTokenType;
    private final boolean invalidateInterimOpenAMSession;

    public TokenTransformConfig(TokenTypeId inputTokenType, TokenTypeId outputTokenType, boolean invalidateInterimOpenAMSession) {
        this.inputTokenType = inputTokenType.getId();
        this.outputTokenType = outputTokenType.getId();
        this.invalidateInterimOpenAMSession = invalidateInterimOpenAMSession;
        Reject.ifNull(inputTokenType, INPUT_TOKEN_TYPE + " cannot be null");
        Reject.ifNull(outputTokenType, OUTPUT_TOKEN_TYPE + " cannot be null");
    }

    /*
    Ctor to aid in json marshalling, and to allow for the creation of token transformations involving custom token types.
     */
    public TokenTransformConfig(String inputTokenType, String outputTokenType, boolean invalidateInterimOpenAMSession) {
        this.inputTokenType = inputTokenType;
        this.outputTokenType = outputTokenType;
        this.invalidateInterimOpenAMSession = invalidateInterimOpenAMSession;
        Reject.ifNull(inputTokenType, INPUT_TOKEN_TYPE + " cannot be null");
        Reject.ifNull(outputTokenType, OUTPUT_TOKEN_TYPE + " cannot be null");
    }

    public TokenTypeId getOutputTokenType() {
        return new TokenTypeId() {
            @Override
            public String getId() {
                return outputTokenType;
            }
        };
    }

    public boolean invalidateInterimOpenAMSession() {
        return invalidateInterimOpenAMSession;
    }

    public TokenTypeId getInputTokenType() {
        return new TokenTypeId() {
            @Override
            public String getId() {
                return inputTokenType;
            }
        };
    }

    /*
    Don't include the invalidateInterimOpenAMSession in the equals comparison. We want the set of supported transforms
    to be specific only to the specified token types, not to whether interim tokens are invalidated.
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof TokenTransformConfig) {
            TokenTransformConfig otherConfig = (TokenTransformConfig)other;
            return inputTokenType.equals(otherConfig.getInputTokenType().getId()) &&
                outputTokenType.equals(otherConfig.getOutputTokenType().getId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (inputTokenType + outputTokenType).hashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder(INPUT_TOKEN_TYPE)
                .append(COLON)
                .append(inputTokenType)
                .append(SEMI_COLON)
                .append(OUTPUT_TOKEN_TYPE)
                .append(COLON)
                .append(outputTokenType)
                .append(SEMI_COLON)
                .append(INVALIDATE_INTERIM_OPENAM_SESSION)
                .append(COLON)
                .append(invalidateInterimOpenAMSession)
                .toString();
    }
    /*
    Because the Map<String, Object> wrapped by the JsonValue returned by toJson will be used as the basis for the
    Map<String, Set<String>> to persist state to the SMS, all values must ultimately be stored as strings.
     */
    public JsonValue toJson() {
        return json(object(field(INPUT_TOKEN_TYPE, inputTokenType), field(OUTPUT_TOKEN_TYPE, outputTokenType),
                field(INVALIDATE_INTERIM_OPENAM_SESSION, invalidateInterimOpenAMSession)));
    }

    public static TokenTransformConfig fromJson(JsonValue json) {
        return new TokenTransformConfig(
                json.get(INPUT_TOKEN_TYPE).asString(),
                json.get(OUTPUT_TOKEN_TYPE).asString(),
                json.get(INVALIDATE_INTERIM_OPENAM_SESSION).asBoolean());
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
        String[] tokens = stringRepresentation.split(REGEX_PIPE);
        if (tokens.length != 3) {
            throw new IllegalArgumentException("The SMS String representation of the TokenTransformConfig must be of format: " +
                    "input_token_type|output_token_type|true_or_false. The passed-in string: " + stringRepresentation);
        }
        return new TokenTransformConfig(tokens[0], tokens[1], Boolean.valueOf(tokens[2]));
    }
}

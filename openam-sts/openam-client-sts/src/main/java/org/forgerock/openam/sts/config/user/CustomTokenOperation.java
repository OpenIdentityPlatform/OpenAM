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

package org.forgerock.openam.sts.config.user;

import org.forgerock.json.JsonValue;
import org.forgerock.util.Reject;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

/**
 * This class encapsulates state necessary to configure a token validator or a token provider for a custom token type.
 * Rest and soap sts instances will support token validators and providers of user-specified token types. The basis
 * for custom token operations is the string name of the custom token, and the class name of the implementation of either
 * the rest/soap token validator or token provider. This class encapsulates these two strings, and the parsing to marshal
 * the AdminUI representation of this information into a CustomTokenOperation instance.
 */
public class CustomTokenOperation {
    private static final String PIPE = "|";
    private static final String REGEX_PIPE = "\\|";
    private static final String CUSTOM_TOKEN_NAME = "customTokenName";
    private static final String CUSTOM_OPERATION_CLASS_NAME = "customOperationClassName";
    private static final String SEMI_COLON = ";";
    private static final String COLON = ":";

    private final String customTokenName;
    private final String customOperationClassName;

    public CustomTokenOperation(String customTokenName, String customOperationClassName) {
        this.customTokenName = customTokenName;
        this.customOperationClassName = customOperationClassName;
        Reject.ifNull(customTokenName, CUSTOM_TOKEN_NAME + " cannot be null");
        Reject.ifNull(customOperationClassName, CUSTOM_OPERATION_CLASS_NAME + " cannot be null");
    }

    public String getCustomTokenName() {
        return customTokenName;
    }

    public String getCustomOperationClassName() {
        return customOperationClassName;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof CustomTokenOperation) {
            CustomTokenOperation otherOperation = (CustomTokenOperation)other;
            return customTokenName.equals(otherOperation.customTokenName)
                    && customOperationClassName.equals(otherOperation.customOperationClassName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (customTokenName + customOperationClassName).hashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append(CUSTOM_TOKEN_NAME).append(COLON).append(customTokenName).append(SEMI_COLON)
                .append(CUSTOM_OPERATION_CLASS_NAME).append(COLON).append(customOperationClassName)
                .toString();
    }

    /**
     *
     * @return the json representation of this class instance
     */
    public JsonValue toJson() {
        return json(object(field(CUSTOM_TOKEN_NAME, customTokenName), field(CUSTOM_OPERATION_CLASS_NAME, customOperationClassName)));
    }

    /**
     * Called to marshal a CustomTokenOperation published to sts-publish/rest
     * @param json the json representation of a CustomTokenOperation instance
     * @return a CustomTokenOperation instance parsed from the json string
     */
    public static CustomTokenOperation fromJson(JsonValue json) {
        return new CustomTokenOperation(json.get(CUSTOM_TOKEN_NAME).asString(), json.get(CUSTOM_OPERATION_CLASS_NAME).asString());
    }

    /**
     *
     * @return the SMS representation of CustomTokenOperation state
     */
    public String toSMSString() {
        return customTokenName + PIPE + customOperationClassName;
    }

    /**
     * Called to marshal back to an instance of the CustomTokenOperation from the format supported by the AdminUI
     * @param smsString
     * @throws IllegalArgumentException if the smsString is not of the format token_name'|'implementation_class.
     * @return a CustomTokenOperation instance constituted from the parsed string
     */
    public static CustomTokenOperation fromSMSString(String smsString) {
        String[] tokens = smsString.split(REGEX_PIPE);
        if (tokens.length != 2) {
            throw new IllegalArgumentException("The sms string representation of the CustomTokenOperation must be of " +
                    "format: custom_token_name|impl_class_mame. The sms string argument: " + smsString);

        }
        return new CustomTokenOperation(tokens[0], tokens[1]);
    }
}

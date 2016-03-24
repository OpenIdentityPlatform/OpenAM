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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openam.utils;

/**
 * A utility class to simplify dealing with attributes flagged as static or binary.
 */
public final class AttributeUtils {

    /**
     * Used when defining a static attribute, the attribute should be enclosed by this flag.
     */
    public static final String STATIC_QUOTE = "\"";

    /**
     *  Used when defining a binary attribute, this flag should be appended to the attribute.
     */
    public static final String BINARY_FLAG = ";binary";

    private AttributeUtils() {
    }

    /**
     * For the given attributeName, return true if it is flagged as a static attribute.
     * @param attributeName The attributeName to check for the static flag
     * @return true if the attributeName is flagged as a static attribute
     */
    public static boolean isStaticAttribute(String attributeName) {
        return attributeName != null && attributeName.startsWith(STATIC_QUOTE) && attributeName.endsWith(STATIC_QUOTE);
    }

    /**
     * Return the attributeName without the static flag if it is included.
     * @param attributeName The attribute name with the static flag included
     * @return The attributeName with the static flag removed
     */
    public static String removeStaticAttributeFlag(String attributeName) {
        if (isStaticAttribute(attributeName)) {
            return attributeName.substring(STATIC_QUOTE.length(), attributeName.length() - STATIC_QUOTE.length());
        } else {
            return attributeName;
        }
    }

    /**
     * For the given attributeName, return true if it is flagged as a binary attribute.
     * @param attributeName The attributeName to check for the binary flag
     * @return true if the attributeName is flagged as a binary attribute
     */
    public static boolean isBinaryAttribute(String attributeName) {
        return attributeName != null && attributeName.endsWith(BINARY_FLAG);
    }

    /**
     * Return the attributeName without the binary flag if it is included.
     * @param attributeName The attribute name with the binary flag included
     * @return The attributeName with the binary flag removed
     */
    public static String removeBinaryAttributeFlag(String attributeName) {
        if (isBinaryAttribute(attributeName)) {
            return attributeName.substring(0, attributeName.lastIndexOf(BINARY_FLAG));
        } else {
            return attributeName;
        }
    }
}

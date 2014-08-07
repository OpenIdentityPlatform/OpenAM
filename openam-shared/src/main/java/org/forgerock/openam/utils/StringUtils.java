/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
/*
 * Portions Copyrighted 2014 Nomura Research Institute, Ltd.
 */

package org.forgerock.openam.utils;

import org.forgerock.util.Reject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Utility class for handling Strings
 *
 * @author Peter Major
 */
public final class StringUtils {

    /**
     *
     * @param content The String content to be replaced
     * @param tagSwapMap A map containing the replacable tokens with their new
     * values
     * @return the tagswapped String content
     */
    public static String tagSwap(String content, Map<String, String> tagSwapMap) {
        for (Map.Entry<String, String> entry : tagSwapMap.entrySet()) {
            content = content.replace(entry.getKey(), entry.getValue());
        }
        return content;
    }

    public static String insertContent(String original, int position, String content) {
        return original.substring(0, position) + content + original.substring(position);
    }

    /**
     * Checks that the original string is not null nor empty, if so it returns the non-null default string instead.
     *
     * @param original
     *         the original string
     * @param defaultString
     *         the non-null default string
     *
     * @return the original string if not null and not empty, else the default string
     */
    public static String ifNullOrEmpty(final String original, final String defaultString) {
        Reject.ifNull(defaultString, "Default string must not be null");
        return (original == null || original.isEmpty()) ? defaultString : original;
    }

    /**
     * Encodes the passed String using an algorithm that's compatible
     * with JavaScript's <code>encodeURIComponent</code> function. Returns
     * <code>null</code> if the String is <code>null</code>.
     * 
     * @param component String to be encoded.
     * @param encoding The name of character encoding.
     * @return the same value as JavaScript encodeURIComponent function
     * @exception UnsupportedEncodingException
     *            If the named encoding is not supported
     */
    public static String encodeURIComponent(String component, String encoding) throws UnsupportedEncodingException {
        if (component == null) {
            return null;
        }
        String result = URLEncoder.encode(component, encoding)
                .replaceAll("\\%28", "(")
                .replaceAll("\\%29", ")")
                .replaceAll("\\+", "%20")
                .replaceAll("\\%27", "'")
                .replaceAll("\\%21", "!")
                .replaceAll("\\%7E", "~");
        return result;
    }
}

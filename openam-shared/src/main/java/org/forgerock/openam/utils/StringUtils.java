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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * Takes a list as a String (where each element is of the form name=value) and returns the value
     * specified by parameterName (if it exists).
     * @param list The list e.g. a=1,b=2,c=5
     * @param delimiter The delimiter used to separate the elements (e.g. ",")
     * @param parameterName The name of the parameter to return
     * @return The value(s) specified by parameterName
     */
    public static List<String> getParameter(String list, String delimiter, String parameterName) {
        String[] parameters = null;
        if (list != null) {
            parameters = list.split(delimiter);
        }

        List<String> result = new ArrayList<String>();
        if (parameters != null) {
            for (String parameter : parameters) {
                String[] valueParameterPair = parameter.split("=");
                final String currentParameterName = valueParameterPair[0];
                if (currentParameterName != null) {
                    if (parameterName.equals(currentParameterName.trim())) {
                        if (valueParameterPair.length == 2) {
                            result.add(valueParameterPair[1]);
                        }
                    }
                }
            }
        }
        return result;
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

    /**
     * @param s string to test
     * @return true if the specified string is null or zero length.
     */
    public static boolean isEmpty(final String s) {
        return (s == null || s.length() == 0);
    }

    /**
     * @param s string to test
     * @return true if the specified string is null or when trimmed is empty (i.e. when trimmed it has zero length)
     */
    public static boolean isBlank(final String s) {
        return (s == null || s.trim().length() == 0);
    }

    /**
     * @param s string to test
     * @return test if the specified string is not null and not empty (i.e. is greater than zero length).
     */
    public static boolean isNotEmpty(final String s) {
        return (s != null && s.length() > 0);
    }

    /**
     * @param s string to test
     * @return true if the specified string is not null and when trimmed has greater than zero length.
     */
    public static boolean isNotBlank(final String s) {
        return (s != null && s.trim().length() > 0);
    }

    /**
     * Tests whether any string in the given set is blank.
     *
     * @param xs the set of strings.
     * @return {@code true} if the set is null or empty or if any member of the set is blank.
     * @see #isBlank(String)
     */
    public static boolean isAnyBlank(final Set<String> xs) {
        if (xs == null || xs.isEmpty()) {
            return true;
        }
        for (String x : xs) {
            if (isBlank(x)) {
                return true;
            }
        }
        return false;
    }
}

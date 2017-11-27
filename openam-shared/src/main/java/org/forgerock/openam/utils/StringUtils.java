/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
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
package org.forgerock.openam.utils;

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
     * Compares two strings in a case insensitive manner, that also allows for
     * either of the strings to be null, without issue.
     *
     * @param s1 the first string to be compared.
     * @param s2 the second string to tbe compared.
     * @return true if the parameter values are the same, false if different.
     */
    public static boolean compareCaseInsensitiveString(String s1, String s2) {
        return s1 == null ? s2 == null : s1.equalsIgnoreCase(s2);
    }

}

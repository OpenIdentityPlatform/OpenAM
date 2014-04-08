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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Utils class containing common utility methods used by various OAuth2 response and grant type handlers.
 *
 * @since 12.0.0
 */
public final class Utils {

    /**
     * Private constructor.
     */
    private Utils() { }

    /**
     * Splits the response type string on ' ' character and returns a {@code Set<String>} of the contents.
     *
     * @param responseType The response type string.
     * @return A {@code Set<String>} of the response types.
     */
    static Set<String> splitResponseType(final String responseType) {
        if (responseType == null) {
            return new HashSet<String>();
        }
        return new HashSet<String>(Arrays.asList(responseType.split(" ")));
    }

    /**
     * Splits the scope string on ' ' character and returns a {@code Set<String>} of the contents.
     *
     * @param scope The response type string.
     * @return A {@code Set<String>} of the scope.
     */
    public static Set<String> splitScope(final String scope) {
        if (scope == null) {
            return new HashSet<String>();
        }
        return new HashSet<String>(Arrays.asList(scope.split(" ")));
    }

    /**
     * Splits the string on ' ' character and returns a {@code Set<String>} of the contents.
     *
     * @param string The string.
     * @return A {@code Set<String>}.
     */
    static Set<String> stringToSet(String string) {
        if (string == null || string.isEmpty()) {
            return Collections.emptySet();
        }
        String[] values = string.split(" ");
        Set<String> set = new HashSet<String>(Arrays.asList(values));
        return set;
    }

    /**
     * Joins together a {@code Set<String>} of scope into a String using the ' ' character to separate the individual
     * scopes.
     *
     * @param scope The scope.
     * @return A concatenated String of the scope.
     */
    public static String joinScope(final Set<String> scope) {

        if (scope == null) {
            return "";
        }

        final Iterator<String> iterator = scope.iterator();

        final StringBuilder sb = new StringBuilder();
        if (iterator.hasNext()) {
            sb.append(iterator.next());
        }
        while (iterator.hasNext()) {
            sb.append(" ").append(iterator.next());
        }
        return sb.toString();
    }

    /**
     * Determines if the specified String is {@code null} or empty.
     *
     * @param s The String.
     * @return {@code true} if the String is {@code null} or empty.
     */
    static boolean isEmpty(final String s) {
        return s == null ? true : s.trim().isEmpty();
    }
}

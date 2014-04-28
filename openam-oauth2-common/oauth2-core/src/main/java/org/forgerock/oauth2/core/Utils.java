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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Utility class containing common utility functions.
 *
 * @since 12.0.0
 */
public final class Utils {

    /**
     * Determines whether the specified String is {@code null} or empty.
     *
     * @param s The String to check.
     * @return {@code true} if the String is {@code null} or empty.
     */
    public static boolean isEmpty(final String s) {
        return s == null || s.isEmpty();
    }

    /**
     * Determines whether the specified Collection is {@code null} or empty.
     *
     * @param c The Collection to check.
     * @return {@code true} if the Collection is {@code null} or empty.
     */
    public static boolean isEmpty(final Collection<?> c) {
        return c == null || c.isEmpty();
    }

    /**
     * Determines whether the specified Map is {@code null} or empty.
     *
     * @param m The Map to check.
     * @return {@code true} if the Map is {@code null} or empty.
     */
    public static boolean isEmpty(final Map<?, ?> m) {
        return m == null || m.isEmpty();
    }

    /**
     * Splits the specified String of response types into a {@code Set} of response types.
     * <br/>
     * If the String of response types is {@code null} an empty {@code Set} is returned.
     *
     * @param responseType The String of response types.
     * @return A {@code Set} of response types.
     */
    public static Set<String> splitResponseType(final String responseType) {
        return stringToSet(responseType);
    }

    /**
     * Splits the specified String of scopes into a {@code Set} of scopes.
     * <br/>
     * If the String of scopes is {@code null} an empty {@code Set} is returned.
     *
     * @param scope The String of scopes.
     * @return A {@code Set} of scopes.
     */
    public static Set<String> splitScope(final String scope) {
        return stringToSet(scope);
    }

    /**
     * Joins the specified {@code Set} of scopes into a space delimited String.
     * <br/>
     * If the specified {@code Set} of scopes is null, an empty String is returned.
     *
     * @param scope The scopes to join.
     * @return A String of the joined scopes.
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
     * Splits the string on ' ' character and returns a {@code Set<String>} of the contents.
     *
     * @param string The string.
     * @return A {@code Set<String>}.
     */
    public static Set<String> stringToSet(String string) {
        if (string == null || string.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<String>(Arrays.asList(string.split(" ")));
    }

}

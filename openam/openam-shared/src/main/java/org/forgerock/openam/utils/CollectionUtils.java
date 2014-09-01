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
 * Copyright 2013 ForgeRock Inc.
 */
package org.forgerock.openam.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A simple utility class to simplify interactions with collections.
 *
 * @author Peter Major
 */
public class CollectionUtils {

    private CollectionUtils() {
    }

    /**
     * Converts the passed in objects to a HashSet.
     *
     * @param <T> The type of the passed in objects.
     * @param values An unbounded amount of objects that needs to be converted to Set.
     * @return either an empty set (if there was no value passed in), or a Set that holds all the values passed in.
     */
    public static <T> Set<T> asSet(T... values) {
        if (values == null) {
            return new HashSet<T>(0);
        } else {
            return new HashSet<T>(Arrays.asList(values));
        }
    }

    /**
     * Converts the passed in objects to a LinkedHashSet.
     * @param <T> The type of the passed in objects.
     * @param values An unbounded amount of objects that needs to be converted to Set.
     * @return either an empty set (if there was no value passed in), or a Set that holds all the values passed in.
     */
    public static <T> Set<T> asOrderedSet(T... values) {
        if (values == null) {
            return new LinkedHashSet<T>(0);
        } else {
            return new LinkedHashSet<T>(Arrays.asList(values));
        }
    }
}

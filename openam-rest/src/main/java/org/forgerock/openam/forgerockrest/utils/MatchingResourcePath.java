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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.forgerockrest.utils;

import java.util.Iterator;

import org.forgerock.json.resource.ResourcePath;

/**
 * A wrapper around {@link ResourcePath} that will be equal to another path if their paths match.
 * <p>
 * Paths will be matched by comparing each path element, based on the following rules:
 * <ul>
 *     <li>
 *         If the path element contains a parameter expression, e.g. {@code {param}}, the name of the parameter
 *         will not be matched, but the other path will be expected to also contain a parameter in the same element.
 *     </li>
 *     <li>
 *         If the path element is a {@code *}, then any value in that element of the other will match.
 *     </li>
 *     <li>
 *         Otherwise, normal string equality to be used.
 *     </li>
 * </ul>
 */
public class MatchingResourcePath {

    private final ResourcePath resourcePath;

    private MatchingResourcePath(ResourcePath resourcePath) {
        this.resourcePath = resourcePath;
    }

    /**
     * Create a new MatchingResourcePath from the provided ResourcePath.
     * @param path The path.
     * @return The matching path.
     */
    public static MatchingResourcePath match(ResourcePath path) {
        return new MatchingResourcePath(path);
    }

    /**
     * Create a MatchingResourcePath from the provided String representation.
     * @param path The path.
     * @return The matching path.
     */
    public static MatchingResourcePath resourcePath(String path) {
        return new MatchingResourcePath(ResourcePath.resourcePath(path));
    }

    @Override
    public int hashCode() {
        return resourcePath.size();
    }

    @Override
    public boolean equals(Object obj) {
        ResourcePath comparePath;
        if (obj instanceof MatchingResourcePath) {
            comparePath = ((MatchingResourcePath) obj).resourcePath;
        } else {
            return false;
        }

        Iterator<String> compareElements = comparePath.iterator();

        for (String element : resourcePath) {
            if (!compareElements.hasNext()) {
                return false;
            }
            String compare = compareElements.next();
            if (element.startsWith("{") && element.endsWith("}")) {
                if (!compare.startsWith("{") || !compare.endsWith("}")) {
                    return false;
                }
            } else if (!"*".equals(element) && !"*".equals(compare)) {
                if (!element.equals(compare)) {
                    return false;
                }
            }
        }
        return !compareElements.hasNext();
    }
}

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

package org.forgerock.openam.utils.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A prefix set that simply iterates through a list of prefix candidates and tests each in turn. Suitable when the
 * number of prefixes is relatively small.
 */
public final class SimplePrefixSet implements PrefixSet {
    private final List<String> prefixes;

    /**
     * Constructs the prefix set with the given collection of prefix strings to test.
     *
     * @param prefixes the prefixes.
     */
    public SimplePrefixSet(Collection<String> prefixes) {
        this.prefixes = new ArrayList<>(prefixes);
    }

    /**
     * Constructs a prefix set with the given set of prefixes to test.
     *
     * @param prefixes the prefixes.
     * @return the prefix set.
     */
    public static SimplePrefixSet of(String... prefixes) {
        return new SimplePrefixSet(Arrays.asList(prefixes));
    }

    @Override
    public boolean containsPrefixOf(final String toMatch) {
        for (String prefix : prefixes) {
            if (toMatch.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}

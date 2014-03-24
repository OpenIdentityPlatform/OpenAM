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
 * <br/>
 * TODO Ensure that this class does not become a dumping ground for "utility methods"
 *
 * @since 12.0.0
 */
public final class Utils {

    private Utils() {}

    static Set<String> splitScope(final String scope) {
        if (scope == null) {
            return new HashSet<String>();
        }
        return new HashSet<String>(Arrays.asList(scope.split(" ")));
    }

    static Set<String> stringToSet(String string){
        if (string == null || string.isEmpty()){
            return Collections.emptySet();
        }
        String[] values = string.split(" ");
        Set<String> set = new HashSet<String>(Arrays.asList(values));
        return set;
    }

    public static String join(final Set<String> scope) {

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
}

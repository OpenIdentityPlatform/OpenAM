/*
 * Copyright 2013 ForgeRock AS.
 *
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
 */

package org.forgerock.openam.utils;

public class Enums {

    /**
     * Retrieves the appropriate Enum from the list of avaliable
     * enums that matches on the ordinal index.
     *
     * @param clazz a class of type Enum
     * @param ordinalIndex the ordinal index to look up
     * @return the Enum this ordinal value represents, null otherwise
     */
    public static <E extends Enum<E>> E getEnumFromOrdinal(Class<E> clazz, int ordinalIndex) {

        if (ordinalIndex < 0 || ordinalIndex > clazz.getEnumConstants().length) {
            return null;
        }

        return clazz.getEnumConstants()[ordinalIndex];
    }

}

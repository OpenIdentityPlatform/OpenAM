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

/**
 * Represents a set of prefix strings. Any string that has one of the strings in this set as a prefix will be
 * considered to be a member of this set.
 */
public interface PrefixSet {

    /**
     * Indicates whether this set contains a prefix of the given string.
     *
     * @param toMatch the string to test for a prefix of.
     * @return {@code true} if any of the strings in this set is a prefix of the given string.
     */
    boolean containsPrefixOf(String toMatch);
}

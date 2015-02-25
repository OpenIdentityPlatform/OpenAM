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
 * Copyright 2014 ForgeRock, AS.
 */

package org.forgerock.openam.entitlement;

/**
 * Indicates that the given short name of a condition, subject or attribute has already been registered and so a
 * different name should be chosen.
 *
 * @since 12.0.0
 */
public class NameAlreadyRegisteredException extends RuntimeException {
    private final String shortName;

    public NameAlreadyRegisteredException(final String name) {
        super("Short name '" + name + "' has already been registered");
        this.shortName = name;
    }

    /**
     * Returns the short name that was already registered.
     * @return the short name that has already been registered.
     */
    public String getShortName() {
        return shortName;
    }
}

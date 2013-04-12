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
package org.forgerock.openam.entitlement.utils.indextree.nodecontext;

/**
 * Context key is used to reference the data in the search context.
 *
 * @author andrew.forrest@forgerock.com
 */
public abstract class ContextKey<T> {

    /**
     * Key used by the single level wildcard tree node to determine whether a new URL level has been reached.
     */
    public static final ContextKey<Boolean> LEVEL_REACHED = new ContextKey<Boolean>() {

        @Override
        public Class<Boolean> getType() {
            return Boolean.class;
        }

    };

    protected ContextKey() {
        // Can only be instantiated by sub-typing.
    }

    /**
     * @return The class of the value type.
     */
    public abstract Class<T> getType();

    // Made final to ensure consistency.
    @Override
    public final boolean equals(Object object) {
        return super.equals(object);
    }

    // Made final to ensure consistency.
    @Override
    public final int hashCode() {
        return super.hashCode();
    }

}

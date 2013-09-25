/**
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
 */
package org.forgerock.openam.utils;

/**
 * Responsible for tracking the current and previous values of a property
 * and indicating when they are differ.
 *
 * This is a simple implementation that will store two values and perform
 * an equals on them when requested.
 *
 * @author robert.wapshott@forgerock.com
 */
public class ModifiedProperty<T> {
    private T previous;
    private T current;

    /**
     * @param t The property to store. Maybe null.
     */
    public void set(T t) {
        previous = current;
        current = t;
    }

    /**
     * @return The property if it has been set. Otherwise null.
     */
    public T get() {
        return current;
    }

    /**
     * @return True if the property has been changed to a value that is different from
     * the previous value. Handles nulls.
     */
    public boolean hasChanged() {
        return (current == null ? previous != null : !current.equals(previous));
    }
}

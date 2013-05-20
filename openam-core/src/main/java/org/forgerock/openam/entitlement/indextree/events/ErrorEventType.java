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
package org.forgerock.openam.entitlement.indextree.events;

import java.util.EnumSet;

/**
 * Represents errors that occur during the life cycle of listening for index changes.
 *
 * @author andrew.forrest@forgerock.com
 */
public enum ErrorEventType implements EventType {

    SEARCH_FAILURE, DATA_LOSS;

    private static final EnumSet<ErrorEventType> TYPES = EnumSet.allOf(ErrorEventType.class);

    /**
     * Whether the passed type is an error event type.
     *
     * @param type
     *         The type in question.
     * @return Whether the type is an error event type.
     */
    public static boolean contains(EventType type) {
        return TYPES.contains(type);
    }

    /**
     * @return An event representing the selected type.
     */
    public IndexChangeEvent createEvent() {
        return new IndexChangeEvent(this);
    }

}

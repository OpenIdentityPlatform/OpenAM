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

/**
 * Represents a modification event.
 *
 * @author andrew.forrest@forgerock.com
 */
public class ModificationEvent extends IndexChangeEvent {

    private final String pathIndex;
    private final String realm;

    protected ModificationEvent(String pathIndex, String realm, ModificationEventType type) {
        super(type);
        this.realm = realm;
        this.pathIndex = pathIndex;
    }

    /**
     * @return The path index.
     */
    public String getPathIndex() {
        return pathIndex;
    }

    /**
     * @return The realm for which the path index is associated.
     */
    public String getRealm() {
        return realm;
    }

}

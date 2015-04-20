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
 * Implemented by any class wanting to be notified of any index change events.
 * <p/>
 * It is essential that any implementing class is registered with the
 * {@link IndexChangeObservable#registerObserver(IndexChangeObserver)} in order to be included within any notifications.
 *
 * @author andrew.forrest@forgerock.com
 */
public interface IndexChangeObserver {

    /**
     * Update the observer of an index change event.
     *
     * @param event
     *         The index change event.
     */
    public void update(IndexChangeEvent event);

}

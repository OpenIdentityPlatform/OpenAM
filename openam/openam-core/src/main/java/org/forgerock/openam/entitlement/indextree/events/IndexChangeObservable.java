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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This observable is used to notify its registered observers of index change events.
 *
 * @author andrew.forrest@forgerock.com
 */
public class IndexChangeObservable {

    private final List<IndexChangeObserver> observers;

    public IndexChangeObservable() {
        // This list implementation removes the need for synchronisation and
        // is optimised to give fast reads, which is ideal for this scenario.
        observers = new CopyOnWriteArrayList<IndexChangeObserver>();
    }

    /**
     * Registers an observer to receive index change notifications.
     *
     * @param observer
     *         The observer to be registered.
     */
    public void registerObserver(IndexChangeObserver observer) {
        observers.add(observer);
    }

    /**
     * Removes a registered observer.
     *
     * @param observer
     *         The observer to be unregistered.
     */
    public void removeObserver(IndexChangeObserver observer) {
        observers.remove(observer);
    }

    /**
     * Notify all registered observers of the index change event.
     *
     * @param event
     *         The index change event.
     */
    public void notifyObservers(IndexChangeEvent event) {
        for (IndexChangeObserver observer : observers) {
            observer.update(event);
        }
    }

}

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
package org.forgerock.openam.entitlement.indextree;

import org.forgerock.openam.entitlement.indextree.events.IndexChangeObserver;
import org.forgerock.openam.entitlement.indextree.events.IndexChangeObservable;

/**
 * Responsible for ensuring all index changes are propagated to interested parties therefore keeping any index models
 * up to date. The manager makes use of a {@link IndexChangeObservable} to notify interested parties of index changes.
 * Any interested party needs to abide to the {@link IndexChangeObserver} contract and register with the manager.
 *
 * @author andrew.forrest@forgerock.com
 */
public interface IndexChangeManager {

    /**
     * Initialise the manager.
     */
    public void init();

    /**
     * @see IndexChangeObservable#registerObserver(IndexChangeObserver)
     */
    public void registerObserver(IndexChangeObserver observer);

    /**
     * @see IndexChangeObservable#removeObserver(IndexChangeObserver)
     */
    public void removeObserver(IndexChangeObserver observer);

    /**
     * Informs the manager to shutdown.
     */
    public void shutdown();

}

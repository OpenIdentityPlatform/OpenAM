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
* Copyright 2016 ForgeRock AS.
*/
package org.forgerock.openam.cts.continuous;

import org.forgerock.openam.sm.datalayer.api.DataLayerException;

/**
 * Interface for ensuring that continuous queries can be controlled once configured.
 *
 * A continuous query is expected to, once configured, run against a connection which will continually return
 * data through the connection. A {@link ContinuousQueryListener} should be configured to react to events as they see
 * them, though it is up to the underlying implementation to decide whether the listener should use either
 * {@code objectChanged} or {@code objectsChanged}.
 *
 * @see ContinuousQueryListener
 */
public interface ContinuousQuery {

    /**
     * Begins the continuous query on the datastore.
     *
     * @throws DataLayerException if there were issues creating the connection and starting the query.
     */
    void startQuery() throws DataLayerException;

    /**
     * Ends the continuous query on the datastore, removing any outstanding listeners.
     */
    void stopQuery();

    /**
     * Adds a generic listener to the set of objects that will be informed of changes according to the
     * queried filter.
     *
     * @param listener ContinuousQueryListener that will respond to changes.
     * @return This instance of the ContinuousQuery.
     */
    ContinuousQuery addContinuousQueryListener(ContinuousQueryListener listener);

    /**
     * Removes a generic listener from the set of objects that will be informed of changes according to the
     * queried filter.
     *
     * @param listener ContinuousQueryListener that will respond to changes.
     * @return This instance of the ContinuousQuery.
     */
    ContinuousQuery removeContinuousQueryListener(ContinuousQueryListener listener);

}

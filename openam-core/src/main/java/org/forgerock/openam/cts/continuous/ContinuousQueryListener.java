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

import java.util.Map;
import java.util.Set;

import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;

/**
 * Interface for an object that listens to changes resulting from a continuous query.
 *
 * Continuous Queries will activate immediately when they are added to the {@link CTSPersistentStore} via the
 * {@code addContinuousQueryListener} method.
 *
 * Once configured via the CTS a continuous query is intended to be persistent. Though it is possible to stop
 * and remove ContinuousQueries from the connection they are being executed on - whether through the query
 * having completed or otherwise, the main intention is to relay messages to the listener from the point of
 * server startup through to shutdown.
 *
 * Any number of listeners may be registered against a given {@link ContinuousQuery}, but all must be removed
 * should the query be stopped.
 *
 * In the event of a connection failure to the underlying datastore, the {@link ContinuousQuery} should call
 * {@code connectionLost} to ensure that the caller can respond appropriately (clearing caches, etc).
 *
 * @param <T> Type of returned attribute from the datastore.
 */
public interface ContinuousQueryListener<T> {

    /**
     * Notify the listener that the following token has been altered.
     *
     * @param tokenId the identifier of the token which has changed.
     * @param changeSet the set of changes to the token from the query result.
     * @param changeType the type of change made to the token.
     */
    void objectChanged(String tokenId, Map<String, T> changeSet, ChangeType changeType);

    /**
     * Notify the listener that the following tokens have been altered.
     *
     * @param tokenIds the identifiers of the tokens which have changed.
     */
    void objectsChanged(Set<String> tokenIds);

    /**
     * Notify the listener that an error has occurred, and results may not be trustworthy for a period - e.g. to wipe
     * a dirty cache in the case of connection issues.
     */
    void connectionLost();

    /**
     * If an error occurred initiating the task from the DataLayer (e.g. failing to achieve a connection to the
     * underlying data store), this method should be called with the error passed in.
     *
     * @param error Details of the issue.
     */
    void processError(DataLayerException error);

}

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
package org.forgerock.openam.cts.worker;

import java.util.Collection;

import org.forgerock.openam.cts.impl.query.worker.CTSWorkerQuery;

/**
 * Interface defining how to filter a set of IDs, which have been returned from the data layer in response to
 * a query by a {@link CTSWorkerQuery}.
 */
public interface CTSWorkerFilter {

    /**
     * Filters the set of ids passed in.
     *
     * @param ids Set of ids to filter.
     * @return Set of filtered ids.
     */
    Collection<String> filter(Collection<String> ids);

}

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
* Copyright 2015 ForgeRock AS.
*/
package org.forgerock.openam.rest.batch.helpers;

import java.util.ArrayList;
import java.util.List;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.utils.JsonArray;
import org.forgerock.openam.utils.JsonValueBuilder;

/**
 * ResultHandler for SDK-based query operations. Utilised by the {@link org.forgerock.openam.rest.batch.helpers.Requester}
 * during execution.
 */
class SDKServerQueryResultHandler implements QueryResultHandler {

    private ResourceException exception;
    private final List<Resource> resources = new ArrayList<>();

    @Override
    public void handleError(ResourceException exception) {
        this.exception = exception;
    }

    @Override
    public boolean handleResource(Resource resource) {
        return resources.add(resource);
    }

    @Override
    public void handleResult(QueryResult queryResult) {
        //This space intentionally left blank.
    }

    /**
     * Retrieves the resources returned from the query operation in a single {@link org.forgerock.json.JsonValue}.
     * If there were any errors while executing the query, the appropriate
     * {@link org.forgerock.json.resource.ResourceException} is thrown instead.
     *
     * @return A {@link org.forgerock.json.JsonValue} containing all results of the query.
     * @throws ResourceException If any exception was thrown when accessing the endpoint, it is returned here.
     */
    public JsonValue getResource() throws ResourceException {

        if (exception != null) {
            throw exception;
        }

        final JsonArray responses = JsonValueBuilder.jsonValue().array("results");

        for (Resource resource : resources) {
            responses.add(resource.getContent());
        }

        return responses.build().build();
    }

}

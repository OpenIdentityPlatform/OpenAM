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

import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;

/**
 * ResultHandler for SDK-based operations. Utilised by the {@link org.forgerock.openam.rest.batch.helpers.Requester}
 * during execution. Generic for the purposes of {@link org.forgerock.json.fluent.JsonValue} and
 * {@link org.forgerock.json.resource.Resource} types.
 */
class SDKServerResultHandler<T> implements ResultHandler<T> {

    public T resource;
    public ResourceException exception;

    @Override
    public void handleError(ResourceException exception) {
        this.exception = exception;
    }

    @Override
    public void handleResult(T resource) {
        this.resource = resource;
    }

    /**
     * Retrieves the output of the operation performed in the appropriate form.
     *
     * @return The output of the performed operation.
     * @throws ResourceException If any exception was thrown when accessing the endpoint, it is returned here.
     */
    public T getResource() throws ResourceException {
        if (exception != null) {
            throw exception;
        }

        return resource;
    }
}

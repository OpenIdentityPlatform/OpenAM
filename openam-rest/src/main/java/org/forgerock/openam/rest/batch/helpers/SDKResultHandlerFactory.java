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

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Resource;

/**
 * Factory for producing the appropriate {@link org.forgerock.json.resource.ResultHandler} for internal
 * SDK routed calls.
 */
public class SDKResultHandlerFactory {

    /**
     * Returns a result handler appropriate for query operations.
     */
    public SDKServerQueryResultHandler getQueryResultHandler() {
        return new SDKServerQueryResultHandler();
    }

    /**
     * Returns a result handler appropriate for {@link org.forgerock.json.JsonValue} resulting operations,
     * e.g. Action
     */
    public SDKServerResultHandler<JsonValue> getJsonValueResultHandler() {
        return new SDKServerResultHandler<>();
    }

    /**
     * Returns a result handler appropriate for {@link org.forgerock.json.resource.Resource} resulting operations,
     * e.g. Create
     */
    public SDKServerResultHandler<Resource> getResourceResultHandler() {
        return new SDKServerResultHandler<>();
    }
}

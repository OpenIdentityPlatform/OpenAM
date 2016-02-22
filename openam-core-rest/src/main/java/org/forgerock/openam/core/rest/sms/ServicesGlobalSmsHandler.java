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
* Copyright 2015-2016 ForgeRock AS.
*/
package org.forgerock.openam.core.rest.sms;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.services.context.Context;

/**
 * Dummy class awaiting implementation as the global-config/services
 * endpoint. Seems likely to support getAllTypes as per AME-9708.
 */
public class ServicesGlobalSmsHandler extends DefaultSmsHandler {

    @Override
    public JsonValue getAllTypes(Context context, ActionRequest request) throws NotSupportedException, InternalServerErrorException {
        throw new NotSupportedException("AME-9708");
    }

    @Override
    public JsonValue getCreatableTypes(Context context, ActionRequest request) throws NotSupportedException, InternalServerErrorException {
        throw new NotSupportedException("Operation not supported.");
    }

    @Override
    public JsonValue getSchema(Context context, ActionRequest request) throws NotSupportedException, InternalServerErrorException {
        throw new NotSupportedException("Operation not supported.");
    }

    @Override
    public JsonValue getTemplate(Context context, ActionRequest request) throws NotSupportedException, InternalServerErrorException {
        throw new NotSupportedException("Operation not supported.");
    }

}

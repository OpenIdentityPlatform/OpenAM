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
package org.forgerock.openam.core.rest.sms;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import java.util.List;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.services.context.Context;

/**
 * Special instance of the SmsResourceProvider necessary to support only
 * getSchema and getTemplate for the always-constructed service endpoints:
 *
 * global-config/authentication
 * realm-config/authentication
 * global-config/services
 *
 * In the case of the SMS, 'resourceless' indicates that there's no
 * subtypes which can be created directly as a result of this endpoint, and
 * hence getAllTypes and getCreatableTypes are not supported.
 *
 * The schema of the service and its template may still be read however.
 *
 * @since 14.0.0
 */
public class ResourcelessSmsHandler extends SmsResourceProvider {

    /**
     * Should be called with a valid schema and appropriate schemaType, but other
     * values are essentially not used as this handler does not deal with instances of
     * schema or subschema and only allows for the reading of the schema and the template.
     */
    ResourcelessSmsHandler(ServiceSchema schema, SchemaType type, List<ServiceSchema> subSchemaPath,
                           String uriPath, boolean serviceHasInstanceName, SmsJsonConverter converter, Debug debug) {
        super(schema, type, subSchemaPath, uriPath, serviceHasInstanceName, converter, debug);
    }

    @Override
    public JsonValue getAllTypes(Context context, ActionRequest request)
            throws NotSupportedException, InternalServerErrorException
    {
        throw new NotSupportedException();
    }

    @Override
    public JsonValue getCreatableTypes(Context context, ActionRequest request)
            throws NotSupportedException, InternalServerErrorException {
        throw new NotSupportedException();
    }

}

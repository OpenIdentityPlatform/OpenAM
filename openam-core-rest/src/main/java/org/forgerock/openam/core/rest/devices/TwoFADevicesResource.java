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
package org.forgerock.openam.core.rest.devices;

import static org.forgerock.json.resource.Responses.newResourceResponse;

import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdUtils;
import java.text.ParseException;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.rest.resource.ContextHelper;
import org.forgerock.services.context.Context;
import org.forgerock.util.annotations.VisibleForTesting;

/**
 * Abstract sub-implementation of the UserDevicesResource.
 *
 * A TwoFADevice must have the ability to skip the module, if desired by the user and
 * the configuration of that server allows it.
 *
 * @param <T>
 */
public abstract class TwoFADevicesResource<T extends UserDevicesDao> extends UserDevicesResource<T> {

    protected final static String SKIP = "skip";
    protected final static String CHECK = "check";

    protected final static String VALUE = "value";
    protected final static String RESULT = "result";
    protected final static String RESET = "reset";

    /**
     * Constructs a new UserDevicesResource.
     *
     * @param userDevicesDao An instance of the {@code UserDevicesDao}.
     * @param helper An instance of the {@code ContextHelper}.
     */
    public TwoFADevicesResource(T userDevicesDao, ContextHelper helper) {
        super(userDevicesDao, helper);
    }

    @Override
    protected ResourceResponse convertValue(JsonValue queryResult) throws ParseException {
        return newResourceResponse(queryResult.get(UUID_KEY).asString(), Integer.toString(queryResult.hashCode()),
                queryResult);
    }

    /**
     * Retrieving the user id in this fashion ensures that an admin can utilise these functions.
     */
    @VisibleForTesting
    protected AMIdentity getUserIdFromUri(Context context) throws InternalServerErrorException {
        return IdUtils.getIdentity(contextHelper.getUserId(context), contextHelper.getRealm(context));
    }

}

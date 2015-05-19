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
package org.forgerock.openam.rest.dashboard;

import java.text.ParseException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Resource;

/**
 * Abstract subimplementation of the UserDevicesResource - used to ensure that
 * rest security's second factor is enabled before allowing requests through to the
 * individual resource dao's for processing.
 *
 * A TwoFADevice must have a "uuid" field in it's stored attribute profile.
 *
 * @param <T>
 */
public abstract class TwoFADevicesResource<T extends UserDevicesDao> extends UserDevicesResource<T> {

    /**
     * Constructs a new UserDevicesResource.
     *
     * @param userDevicesDao An instance of the {@code UserDevicesDao}.
     */
    public TwoFADevicesResource(T userDevicesDao) {
        super(userDevicesDao);
    }

    @Override
    protected Resource convertValue(JsonValue queryResult) throws ParseException {
        return new Resource(queryResult.get(UUID_KEY).asString(), Integer.toString(queryResult.hashCode()), queryResult);
    }

}

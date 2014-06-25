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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.rest.marshal;

import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.MapMarshaller;
import org.forgerock.openam.sts.STSPublishException;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;

import java.util.Map;
import java.util.Set;

/**
 * @see org.forgerock.openam.sts.MapMarshaller
 *
 * All of the functionality to marshal to and from an attribute map is ultimately encapsulated in the RestSTSInstanceConfig
 * and encapsulated classes. I don't want to call the static method RestSTSInstanceConfig.marshalFromAttribute map directly
 * from my main-line code however, and thus wrap the consumption of the map-marshalling functionality implemented in
 * RestSTSInstanceConfig in this class.
 */
public class RestSTSInstanceConfigMapMarshaller implements MapMarshaller<RestSTSInstanceConfig> {

    public Map<String, Set<String>> marshallAttributesToMap(RestSTSInstanceConfig instance) throws STSPublishException {
        try {
            return instance.marshalToAttributeMap();
        } catch (RuntimeException e) {
            /*
            The marshalToAttributeMap method may throw a IllegalStateException or IllegalArgument exception if marshalling
            functionality fails.
             */
            throw new STSPublishException(ResourceException.INTERNAL_ERROR,
                    "Exception caught marshalling RestSTSInstanceConfig to map: " + e, e);
        }
    }

    public RestSTSInstanceConfig marshallFromMapAttributes(Map<String, Set<String>> attributes) throws STSPublishException {
        try {
            return RestSTSInstanceConfig.marshalFromAttributeMap(attributes);
        } catch (RuntimeException e) {
            /*
            The marshalToAttributeMap method may throw a IllegalStateException or IllegalArgument exception if marshalling
            functionality fails.
             */
            throw new STSPublishException(ResourceException.INTERNAL_ERROR,
                    "Exception caught marshalling RestSTSInstanceConfig from map: " + e, e);
        }
    }
}

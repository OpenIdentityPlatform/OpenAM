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

package org.forgerock.openam.sts;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.soap.config.user.SoapSTSInstanceConfig;

import java.util.Map;
import java.util.Set;

/**
 * @see InstanceConfigMarshaller
 *
 * All of the functionality to marshal to and from an attribute map is ultimately encapsulated in the SoapSTSInstanceConfig
 * and encapsulated classes. I don't want to call the static method SoapSTSInstanceConfig.marshalFromAttribute map directly
 * from my main-line code however, and thus wrap the consumption of the map-marshalling functionality implemented in
 * SoapSTSInstanceConfig in this class.
 */
public class SoapSTSInstanceConfigMarshaller implements InstanceConfigMarshaller<SoapSTSInstanceConfig> {

    public Map<String, Set<String>> toMap(SoapSTSInstanceConfig instance) throws STSPublishException {
        try {
            return instance.marshalToAttributeMap();
        } catch (RuntimeException e) {
            /*
            The marshalToAttributeMap method may throw a IllegalStateException or IllegalArgument exception if marshalling
            functionality fails.
             */
            throw new STSPublishException(ResourceException.INTERNAL_ERROR,
                    "Exception caught marshalling SoapSTSInstanceConfig to map: " + e, e);
        }
    }

    public SoapSTSInstanceConfig fromMapAttributes(Map<String, Set<String>> attributes) throws STSPublishException {
        try {
            return SoapSTSInstanceConfig.marshalFromAttributeMap(attributes);
        } catch (RuntimeException e) {
            /*
            The marshalToAttributeMap method may throw a IllegalStateException or IllegalArgument exception if marshalling
            functionality fails.
             */
            throw new STSPublishException(ResourceException.INTERNAL_ERROR,
                    "Exception caught marshalling SoapSTSInstanceConfig from map: " + e, e);
        }
    }

    @Override
    public SoapSTSInstanceConfig fromJsonAttributeMap(JsonValue jsonMapAttributes) throws STSPublishException {
        try {
            return SoapSTSInstanceConfig.marshalFromJsonAttributeMap(jsonMapAttributes);
        } catch (RuntimeException e) {
            /*
            The marshalToAttributeMap method may throw a IllegalStateException or IllegalArgument exception if marshalling
            functionality fails.
             */
            throw new STSPublishException(ResourceException.INTERNAL_ERROR,
                    "Exception caught marshalling SoapSTSInstanceConfig from json-wrapped attribute map: " + e, e);
        }
    }

    @Override
    public SoapSTSInstanceConfig fromJson(JsonValue json) throws STSPublishException {
        try {
            return SoapSTSInstanceConfig.fromJson(json);
        } catch (RuntimeException e) {
            /*
            The marshalToAttributeMap method may throw a IllegalStateException or IllegalArgument exception if marshalling
            functionality fails.
             */
            throw new STSPublishException(ResourceException.INTERNAL_ERROR,
                    "Exception caught marshalling SoapSTSInstanceConfig from json: " + e, e);
        }
    }
}

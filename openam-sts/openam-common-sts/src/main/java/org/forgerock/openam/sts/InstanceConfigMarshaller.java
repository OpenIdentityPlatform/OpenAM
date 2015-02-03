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
import org.forgerock.openam.sts.config.user.STSInstanceConfig;

import java.util.Map;
import java.util.Set;

/**
 * Interface to represent the concerns of marshalling to RestSTSInstanceConfig and SoapSTSInstanceConfig instances to/from either
 * the Map<String, Set<String>> SMS representation or the json representation. Simply wraps the static methods in the
 * RestSTSInstanceConfig and SoapSTSInstanceConfig classes, so that they can be easily mocked.
 */
public interface InstanceConfigMarshaller<T extends STSInstanceConfig> {
    /**
     * Called to marshal an STSInstanceConfig subclass instance to the Map<String, Set<String>> required for SMS persistence.
     * @param instance the to-be-marshaled instance
     * @return the SMS-persistence-ready representation of STSInstanceConfig state.
     * @throws STSPublishException if an exception occurs during the marshalling
     */
    Map<String, Set<String>> toMap(T instance) throws STSPublishException;

    /**
     * Marshal STSInstanceConfig subclass instance from the SMS representation
     * @param attributes the attributes retrieved from the SMS
     * @return a STSInstanceConfig subclass
     * @throws STSPublishException if an exception occurs during the marshalling
     */
    T fromMapAttributes(Map<String, Set<String>> attributes) throws STSPublishException;

    /**
     * Marshal STSInstanceConfig subclass instance from the SMS representation, wrapped in json. The ViewBean classes
     * which allow AdminConsole users to configure Soap and Rest STS instances ultimately call the sts publish service
     * to publish the Rest or Soap STS instance. The invocation payload is the SMS Map<String, Set<String>> representation
     * native to the ViewBean context, wrapped in a JsonValue, so that it may be posted to CREST. This method defines the
     * contract to marshal this representation back to a STSInstanceConfig subclass.
     * @param jsonMapAttributes the Map<String, Set<String>> native to the ViewBean context and to SMS persistence, wrapped
     *                          in a JsonValue
     * @return a STSInstanceConfig subclass
     * @throws STSPublishException if an exception occurs during the marshalling
     */
    T fromJsonAttributeMap(JsonValue jsonMapAttributes) throws STSPublishException;

    /**
     * @param json The json representation of the STSInstanceConfig subclass
     * @return a STSInstanceConfig subclass instance
     * @throws STSPublishException if an exception occurs during the marshalling
     */
    T fromJson(JsonValue json) throws STSPublishException;
}

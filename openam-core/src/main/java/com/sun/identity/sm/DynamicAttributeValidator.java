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
 * Copyright 2014 ForgeRock AS.
 */

package com.sun.identity.sm;

import java.util.Map;
import java.util.Set;

/**
 * This can be implemented by services if dynamic validation is required. If an AttributeSchema is associated
 * with a validator class that implements <code>DynamicAttributeValidator</code>, a validation button will automatically
 * be added for the attribute. The button's associated event handler will call the validate method and the UI will
 * report the validation message upon failure.
 *
 * @since 12.0.0
 */
public interface DynamicAttributeValidator {

    /**
     * Allow the implementer to validate the value of a specific attribute.
     * @param instanceName The name of the service instance.
     * @param attributeName The name of the attribute for which validation was requested.
     * @param attributeMap The map that holds all service attribute values, keyed on attribute names.
     * @return The outcome of the validation. If false is returned the validation message will be displayed.
     */
    public boolean validate(String instanceName, String attributeName, Map<String, Set<String>> attributeMap);

    /**
     * Retrieve the validation message. This will only be called when validation failed.
     * @return A message that describes the failure.
     */
    public String getValidationMessage();
}

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
package org.forgerock.openam.sm.validation;

import java.util.Set;

import com.sun.identity.sm.ServiceAttributeValidator;

/**
 * Service Manager validator for ports.
 *
 * @since 13.0.0
 */
public class PortValidator implements ServiceAttributeValidator {

    /**
     * Validates the {@link Set} of values to ensure all of them are valid ports, and that no other set members
     * exist. An empty set will result in a failure also, as this indicates no values were entered.
     *
     * @param values the {@link Set} of attribute values to validate
     * @return true if the {@link Set} has one or more members, and all of those members are valid port numbers,
     * false otherwise.
     */
    @Override
    public boolean validate(Set values) {
        if (values.isEmpty()) {
            return false;
        }

        for (String value : (Set<String>) values) {
            int intValue;
            try {
                intValue = Integer.parseInt(value);
            } catch (NumberFormatException nfe) {
                return false;
            }
            if (intValue <= 0 || intValue > 65535) {
                return false;
            }
        }
        return true;
    }
}

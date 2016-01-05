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
package org.forgerock.openam.audit.validation;

import com.sun.identity.sm.ServiceAttributeValidator;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service Manager validator for hostnames.
 *
 * @since 13.0.0
 */
public class HostnameValidator implements ServiceAttributeValidator {

    private static final Pattern VALID_HOSTNAME_CHARACTERS = Pattern.compile("^[a-zA-Z0-9.]*$");

    /**
     * Validates the {@link Set} of values to ensure that all of them conform to the regular
     * expression "^[a-zA-Z0-9.]*$" - which indicates the permitted structure of a hostname - and that none
     * of them start with a ".". If the set is an empty set this indicates that no values were entered
     * and classes as a validation failure.
     *
     * @param values the {@link Set} of attribute values to validate
     * @return true if the {@link Set} has one or more members, and all of those members are valid hostnames,
     * false otherwise.
     */
    @Override
    public boolean validate(Set values) {
        if (values.isEmpty()) {
            return false;
        }

        for (String value : (Set<String>) values) {
            Matcher matcher = VALID_HOSTNAME_CHARACTERS.matcher(value);
            if (!matcher.matches()) {
                return false;
            }
            if (value.startsWith(".")) {
                return false;
            }
        }
        return true;
    }
}

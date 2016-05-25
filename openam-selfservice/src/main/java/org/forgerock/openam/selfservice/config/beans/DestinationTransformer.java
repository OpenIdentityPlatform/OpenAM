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
package org.forgerock.openam.selfservice.config.beans;

import java.util.Set;

import org.forgerock.openam.sm.config.ConfigTransformer;

/**
 * Transforms the destination string into the corresponding enum value.
 *
 * @since 13.5.0
 */
final class DestinationTransformer implements ConfigTransformer<RegistrationDestination> {

    @Override
    public RegistrationDestination transform(Set<String> values, Class<?> parameterType) {
        if (!RegistrationDestination.class.isAssignableFrom(parameterType)) {
            throw new ClassCastException("Expected parameter type to be of Destination");
        }

        String destinationString = values.iterator().next();
        String normalisedDestinationString = destinationString
                .replaceAll("-", "_")
                .toUpperCase();

        return RegistrationDestination.valueOf(normalisedDestinationString);
    }

}

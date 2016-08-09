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
package org.forgerock.openam.oauth2;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.restlet.Request;

import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.debug.Debug;

/**
 * Utility class containing common utility functions.
 */
final class Utils {

    /**
     * Private constructor.
     */
    private Utils() {
    }

    /**
     * Returns the identity attribute value as a {@code Set<URI>}.
     *
     * @param identity      The identity with the attribute.
     * @param attributeName The name of the attribute.
     * @param logger        The logger used to log eventual exceptions.
     * @return The attribute value as a {@code Set<URI>}.
     */
    static Set<URI> getAttributeValuesAsUris(AMIdentity identity, String attributeName, Debug logger) {
        try {
            Set<URI> urisSet = new HashSet<>();
            Set<String> uris = stripAttributeNameFromValue(identity.getAttribute(attributeName));
            for (String uri : uris) {
                urisSet.add(URI.create(uri));
            }
            return urisSet;
        } catch (Exception e) {
            throw createException(attributeName, e, logger);
        }
    }

    /**
     * Returns the identity attribute value as a {@code Set<String>}.
     *
     * @param identity      The identity with the attribute.
     * @param attributeName The name of the attribute.
     * @param logger        The logger used to log eventual exceptions.
     * @return The attribute value as a {@code Set<String>}.
     */
    static Set<String> getAttributeValuesAsSet(AMIdentity identity, String attributeName, Debug logger) {
        try {
            Set<String> valuesSet = new HashSet<>();
            Set<String> values = stripAttributeNameFromValue(identity.getAttribute(attributeName));
            for (String value : values) {
                valuesSet.add(value);
            }
            return valuesSet;
        } catch (Exception e) {
            throw createException(attributeName, e, logger);
        }
    }

    /**
     * Remove the attribute name from each attribute value of the form attribute_name=attribute_value.
     *
     * @param attributeValues The attribute values.
     * @return The attribute values stripped of the attribute name.
     */
    static Set<String> stripAttributeNameFromValue(Set<String> attributeValues) {
        Set<String> values = new HashSet<>();
        for (String attributeValue : attributeValues) {
            int index = attributeValue.indexOf('=');
            if (index != -1) {
                String trimmedValue = attributeValue.substring(index + 1).trim();
                if (!trimmedValue.isEmpty()) {
                    values.add(trimmedValue);
                }
            }
        }
        return values;
    }

    /**
     * Returns the identity attribute value.
     *
     * @param identity      The identity with the attribute.
     * @param attributeName The name of the attribute.
     * @param logger        The logger used to log eventual exceptions.
     * @return The attribute value.
     */
    static String getAttributeValueFromSet(AMIdentity identity, String attributeName, Debug logger) {
        Set<String> values;
        try {
            values = identity.getAttribute(attributeName);
        } catch (Exception e) {
            throw createException(attributeName, e, logger);
        }
        return values.iterator().next();
    }

    /**
     * Creates and logs an {@code OAuthProblemException},
     * if an exception occurs trying to get an attribute value.
     *
     * @param attributeName The name of the attribute.
     * @param e             The occurred exception.
     * @param logger        The logger used to log the exception.
     * @return An instance of {@code OAuthProblemException}.
     */
    static OAuthProblemException createException(String attributeName, Exception e, Debug logger) {
        logException(attributeName, e, logger);
        return OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                "Unable to get " + attributeName + " from repository");
    }

    private static void logException(String attributeName, Exception e, Debug logger) {
        if (logger != null) {
            logger.error("Unable to get {} from repository", attributeName, e);
        }
    }
}

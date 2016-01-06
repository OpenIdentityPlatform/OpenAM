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

package org.forgerock.openam.rest.representations;


import javax.inject.Inject;

import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A factory class for Restlet's {@code JacksonRepresentation}
 */
public class JacksonRepresentationFactory {

    private final ObjectMapper objectMapper;

    /**
     * A constructor to provide the factory with the instance of Jackson's {@code ObjectMapper} to use in the
     * created representations.
     *
     * @param objectMapper The mapper.
     */
    @Inject
    public JacksonRepresentationFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Create a {@code JacksonRepresentation} using the factory-provided object mapper.
     *
     * @param object The object to be represented.
     * @param <T> The type of the object being represented.
     * @return The representation wrapper.
     */
    public <T> JacksonRepresentation<T> create(T object) {
        JacksonRepresentation<T> representation = new JacksonRepresentation<>(object);
        representation.setObjectMapper(objectMapper);
        return representation;
    }

    /**
     * Create a {@code JacksonRepresentation} using the factory-provided object mapper.
     *
     * @param representation The source representation.
     * @param type The type to read the representation as.
     * @param <T> The type of the object being represented.
     * @return The representation wrapper.
     */
    public <T> JacksonRepresentation<T> create(Representation representation, Class<T> type) {
        JacksonRepresentation<T> jacksonRepresentation = new JacksonRepresentation<>(representation, type);
        jacksonRepresentation.setObjectMapper(objectMapper);
        return jacksonRepresentation;
    }
}

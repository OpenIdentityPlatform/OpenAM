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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.openam.rest.service;

import java.util.Map;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.rest.representations.JacksonRepresentationFactory;
import org.forgerock.util.annotations.VisibleForTesting;
import org.restlet.representation.Representation;

import com.google.inject.Inject;

/**
 * An implementation of {@code RestStatusService} that returns a {@code JacksonRepresentation}.
 */
public class JSONRestStatusService extends RestStatusService {

    private final JacksonRepresentationFactory jacksonRepresentationFactory;

    /**
     * Default constructor - will initialise Guice objects from the {@code InjectorHolder}.
     */
    public JSONRestStatusService() {
        jacksonRepresentationFactory = InjectorHolder.getInstance(JacksonRepresentationFactory.class);
    }

    /**
     * Dependency constructor used for testing. Restlet Framework will call the default
     * constructor {@link #JSONRestStatusService()}.
     *
     * @param factory non null.
     */
    @VisibleForTesting
    @Inject
    public JSONRestStatusService(JacksonRepresentationFactory factory) {
        jacksonRepresentationFactory = factory;
    }

    @Override
    protected Representation representMap(Map<String, Object> map) {
        return jacksonRepresentationFactory.create(map);
    }
}

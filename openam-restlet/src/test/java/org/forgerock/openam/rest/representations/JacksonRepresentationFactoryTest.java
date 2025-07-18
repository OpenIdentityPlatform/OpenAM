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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;

import org.restlet.ext.jackson.JacksonRepresentation;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonRepresentationFactoryTest {

    @Test
    public void shouldUseTheSameObjectMapperEveryTime() throws Exception {
        // Given
        ObjectMapper mapper = new ObjectMapper();
        JacksonRepresentationFactory factory = new JacksonRepresentationFactory(mapper);
        HashMap<Object, Object> body = new HashMap<>();

        // When
        JacksonRepresentation<HashMap<Object, Object>> representation = factory.create(body);

        // Then
        assertThat(representation.getObjectMapper()).isSameAs(mapper);
        assertThat(representation.getObject()).isSameAs(body);
    }

}
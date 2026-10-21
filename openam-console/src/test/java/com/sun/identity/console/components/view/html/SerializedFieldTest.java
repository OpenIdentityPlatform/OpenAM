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
 * Copyright 2026 3A Systems, LLC.
 */
package com.sun.identity.console.components.view.html;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.iplanet.jato.model.DefaultModel;
import com.iplanet.jato.view.View;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.testng.annotations.Test;

/**
 * Pins that {@link SerializedField} reads its value back with the console collection depth
 * ({@code IOUtils.CONSOLE_STATE_MAX_COLLECTION_DEPTH}): console table/model state nests
 * map -&gt; attribute-value map -&gt; value set, which the default {@code maxcollectiondepth} of 2 rejects.
 */
public class SerializedFieldTest {

    @Test
    public void shouldRoundTripConsoleStateShape() {
        // Given: the console page-state shape, one collection level past the default limit.
        Map<String, Object> state = new HashMap<>();
        Map<String, Object> attributeValues = new HashMap<>();
        Set<String> values = new HashSet<>();
        values.add("value");
        attributeValues.put("attribute", values);
        state.put("propertyAttributes", attributeValues);

        SerializedField field = new SerializedField(mock(View.class), new DefaultModel(), "szCache", null);
        field.setValue(state);

        // When
        Map<String, Object> restored = field.getSerializedObj();

        // Then: with the default depth the rejection would surface here as null.
        assertThat(restored).isEqualTo(state);
    }
}

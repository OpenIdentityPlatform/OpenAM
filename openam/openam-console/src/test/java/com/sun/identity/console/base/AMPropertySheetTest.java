/*
 * Copyright 2014 ForgeRock, AS.
 *
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
 */

package com.sun.identity.console.base;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.ContainerView;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Basic unit tests for {@link AMPropertySheet}.
 *
 * @since 12.0.0
 */
public class AMPropertySheetTest {
    private static final String NAME = "testName";
    private AMPropertySheet propertySheet;

    private ContainerView mockContainerView;
    private AMPropertySheetModel mockModel;

    @BeforeMethod
    public void setupMocks() {
        mockContainerView = mock(ContainerView.class);
        mockModel = mock(AMPropertySheetModel.class);

        propertySheet = new AMPropertySheet(mockContainerView, mockModel, NAME);
    }

    /**
     * OPENAM-3579: Test that the property sheet completely removes any properties that are set to the empty string.
     * This ensures that optional attributes can be removed by blanking out the associated field.
     */
    @Test
    public void shouldRemoveEmptyStringPropertyValues() throws ModelControlException, AMConsoleException {
        // Given
        String key = "phonenumber";
        String oldValue = "555123456789";
        String newValue = "";
        Map<String, Set<String>> oldValues = Collections.singletonMap(key, Collections.singleton(oldValue));
        AMModel amModel = mock(AMModel.class);
        given(mockModel.isChildSupported(key)).willReturn(true);
        given(mockModel.getValues(key)).willReturn(new Object[] { newValue });

        // When
        @SuppressWarnings("unchecked")
        Map<String, Set<String>> result = propertySheet.getAttributeValues(oldValues, true, amModel);

        // Then
        assertThat(result).isEqualTo(Collections.singletonMap(key, Collections.emptySet()));
    }
}

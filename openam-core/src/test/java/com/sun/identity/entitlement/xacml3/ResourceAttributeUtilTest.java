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
package com.sun.identity.entitlement.xacml3;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.ResourceAttribute;
import org.codehaus.jackson.map.ObjectMapper;
import org.fest.util.Collections;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.testng.AssertJUnit.fail;

public class ResourceAttributeUtilTest {

    private ResourceAttributeUtil util;

    @Test
    public void shouldSerialiseMockResourceAttribute() throws EntitlementException {
        // Given
        util = new ResourceAttributeUtil();
        ResourceAttribute attribute = new MockResourceAttribute();

        // When
        String result = util.toJSON(attribute);

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    public void shouldCatchFailureIfDeserialisationFails() throws IOException {
        // Given
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        given(mockMapper.readValue(anyString(), any(Class.class))).willThrow(new IOException());
        util = new ResourceAttributeUtil(mockMapper);

        // When / Then
        try {
            util.fromJSON("");
            fail();
        } catch (EntitlementException e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    public void shouldDeserialiseMockResourceAttribute() throws EntitlementException {
        // Given
        util = new ResourceAttributeUtil();
        ResourceAttribute attribute = new MockResourceAttribute();

        // When
        ResourceAttribute result = util.fromJSON(util.toJSON(attribute));

        // Then
        assertThat(result.getPropertyName()).isEqualTo(attribute.getPropertyName());
    }

    /**
     * A non mock object is required for this test because the serialisation code uses reflection.
     */
    private static class MockResourceAttribute implements ResourceAttribute {
        @Override
        public void setPropertyName(String name) {}

        @Override
        public String getPropertyName() {
            return "Badger";
        }

        @Override
        public Set<String> getPropertyValues() {
            return Collections.set("Weasel", "Ferret");
        }

        @Override
        public Map<String, Set<String>> evaluate(Subject adminSubject, String realm, Subject subject, String resourceName, Map<String, Set<String>> environment) throws EntitlementException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setPResponseProviderName(String pResponseProviderName) {}

        @Override
        public String getPResponseProviderName() {
            return "Forrest";
        }

        @Override
        public String getState() {
            return "Sleeping";
        }

        @Override
        public void setState(String s) {}
    }
}
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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.uma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.fluent.JsonValue.*;
import static org.mockito.Mockito.*;
import static org.forgerock.openam.uma.UmaPolicyTest.*;

import java.util.Collections;
import java.util.Set;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class UmaPolicyConverterTest {

    private ResourceSetDescription resourceSet;
    private UmaResourceTypeFactory resourceTypeFactory;
    private UmaPolicyConverter converter;

    @BeforeMethod
    public void setup() {
        resourceSet = new ResourceSetDescription("RESOURCE_SET_ID", "CLIENT_ID",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        resourceSet.setDescription(json(object(field("name", "NAME"))));
        resourceSet.setRealm("/REALM");
        resourceTypeFactory = mock(UmaResourceTypeFactory.class);
        converter = new UmaPolicyConverter(resourceTypeFactory);
    }

    @Test
    public void testAsUnderlyingPolicies() throws Exception {

        //Given
        UmaPolicy umaPolicy = UmaPolicy.valueOf(resourceSet, createUmaPolicyJson());
        when(resourceTypeFactory.getResourceTypeId("/REALM")).thenReturn("RESOURCE_TYPE_UUID");

        //When
        Set<JsonValue> underlyingPolicies = converter.asUnderlyingPolicies(umaPolicy);

        //Then
        boolean foundScopeAPolicy = false;
        boolean foundScopeBPolicy = false;
        for (JsonValue policy : underlyingPolicies) {
            if (policy.contains("NAME - RESOURCE_SET_ID-" + "SCOPE_A".hashCode())) {
                assertThat(policy.asMap()).isEqualTo(createUnderlyingScopeAPolicyJson("RESOURCE_SET_ID").asMap());
                foundScopeAPolicy = true;
            } else if (policy.contains("NAME - RESOURCE_SET_ID-"+ "SCOPE_B".hashCode())) {
                assertThat(policy.asMap()).isEqualTo(createUnderlyingScopeBPolicyJson("RESOURCE_SET_ID").asMap());
                foundScopeBPolicy = true;
            }
        }
        assertThat(foundScopeAPolicy).isTrue();
        assertThat(foundScopeBPolicy).isTrue();

    }
}
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

package org.forgerock.openam.oauth2.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.eq;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.forgerock.json.resource.NotFoundException;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.forgerock.openam.oauth2.resources.labels.LabelType;
import org.forgerock.openam.oauth2.resources.labels.ResourceSetLabel;
import org.forgerock.openam.oauth2.resources.labels.UmaLabelsStore;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ResourceSetLabelRegistrationTest {

    private ResourceSetLabelRegistration labelRegistration;

    @Mock
    private UmaLabelsStore labelsStore;

    @BeforeMethod
    public void setup() throws Exception {
        initMocks(this);
        labelRegistration = new ResourceSetLabelRegistration(labelsStore);
    }

    @Test
    public void shouldUpdateLabelsForNewResourceSet() throws Exception {

        //Given
        ResourceSetDescription resourceSet = newResourceSet("LABEL_ONE");
        givenLabelsDoesNotExist("LABEL_ONE");

        //When
        labelRegistration.updateLabelsForNewResourceSet(resourceSet);

        //Then
        ArgumentCaptor<ResourceSetLabel> labelCaptor = ArgumentCaptor.forClass(ResourceSetLabel.class);
        verify(labelsStore).create(eq("REALM"), eq("RESOURCE_OWNER_ID"), labelCaptor.capture());
        assertThat(labelCaptor.getValue().getId()).isEqualTo("CLIENT_ID/LABEL_ONE");
        assertThat(labelCaptor.getValue().getName()).isEqualTo("LABEL_ONE");
        assertThat(labelCaptor.getValue().getType()).isEqualTo(LabelType.SYSTEM);
        assertThat(labelCaptor.getValue().getResourceSetIds()).containsOnly("RESOURCE_SET_ID");
    }

    @Test
    public void shouldNotUpdateLabelsForNewResourceSetWithNoLabels() throws Exception {

        //Given
        ResourceSetDescription resourceSet = newResourceSet();

        //When
        labelRegistration.updateLabelsForNewResourceSet(resourceSet);

        //Then
        verify(labelsStore, never()).create(eq("REALM"), eq("RESOURCE_OWNER_ID"), any(ResourceSetLabel.class));
    }

    @Test
    public void shouldUpdateLabelsForExistingResourceSet() throws Exception {

        //Given
        givenLabelsForResourceSet("LABEL_ONE", "LABEL_TWO");
        ResourceSetDescription resourceSet = newResourceSet("LABEL_ONE", "LABEL_THREE", "LABEL_FOUR");
        givenLabelsExist("LABEL_ONE", "LABEL_TWO", "LABEL_THREE");
        givenLabelsDoesNotExist("LABEL_FOUR");

        //When
        labelRegistration.updateLabelsForExistingResourceSet(resourceSet);

        //Then
        ArgumentCaptor<ResourceSetLabel> labelCaptor = ArgumentCaptor.forClass(ResourceSetLabel.class);
        verify(labelsStore, times(2)).update(eq("REALM"), eq("RESOURCE_OWNER_ID"), labelCaptor.capture());
        verify(labelsStore).create(eq("REALM"), eq("RESOURCE_OWNER_ID"), labelCaptor.capture());

        List<ResourceSetLabel> labels = labelCaptor.getAllValues();
        for (ResourceSetLabel label : labels) {
            if (label.getId().contains("LABEL_TWO")) {
                assertThat(label.getResourceSetIds()).isEmpty();
            } else if (label.getId().contains("LABEL_THREE")) {
                assertThat(label.getResourceSetIds()).containsOnly("RESOURCE_SET_ID");
            } else if (label.getId().contains("LABEL_FOUR")) {
                assertThat(label.getResourceSetIds()).containsOnly("RESOURCE_SET_ID");
            }
        }
    }

    @Test
    public void shouldUpdateLabelsForExistingResourceSetWithAllLabelsRemoved() throws Exception {

        //Given
        givenLabelsForResourceSet("LABEL_ONE", "LABEL_TWO");
        ResourceSetDescription resourceSet = newResourceSet();
        givenLabelsExist("LABEL_ONE", "LABEL_TWO");

        //When
        labelRegistration.updateLabelsForExistingResourceSet(resourceSet);

        //Then
        ArgumentCaptor<ResourceSetLabel> labelCaptor = ArgumentCaptor.forClass(ResourceSetLabel.class);
        verify(labelsStore, times(2)).update(eq("REALM"), eq("RESOURCE_OWNER_ID"), labelCaptor.capture());

        List<ResourceSetLabel> labels = labelCaptor.getAllValues();
        for (ResourceSetLabel label : labels) {
            assertThat(label.getResourceSetIds()).isEmpty();
        }
    }

    @Test
    public void shouldUpdateLabelsForDeletedResourceSet() throws Exception {

        //Given
        ResourceSetDescription resourceSet = newResourceSet("LABEL_ONE", "LABEL_TWO");
        givenLabelsExist("LABEL_ONE", "LABEL_TWO");

        givenLabelsAreNotIsUse("LABEL_ONE");

        //When
        labelRegistration.updateLabelsForDeletedResourceSet(resourceSet);

        //Then
        ArgumentCaptor<ResourceSetLabel> labelCaptor = ArgumentCaptor.forClass(ResourceSetLabel.class);
        verify(labelsStore, times(2)).update(eq("REALM"), eq("RESOURCE_OWNER_ID"), labelCaptor.capture());
        ArgumentCaptor<String> deletedLabelsCaptor = ArgumentCaptor.forClass(String.class);
        verify(labelsStore, times(2)).delete(eq("REALM"), eq("RESOURCE_OWNER_ID"), deletedLabelsCaptor.capture());

        List<ResourceSetLabel> labels = labelCaptor.getAllValues();
        for (ResourceSetLabel label : labels) {
            assertThat(label.getResourceSetIds()).isEmpty();
        }
        deletedLabelsCaptor.getAllValues().containsAll(Arrays.asList("CLIENT_ID/LABEL_ONE", "CLIENT_ID/LABEL_TWO"));
    }

    @Test
    public void shouldUpdateLabelUsingClientIdIfClientDisplayNameIsNull() throws Exception {

        //Given
        ResourceSetDescription resourceSet = newResourceSet("LABEL_ONE");
        givenLabelsDoesNotExist("LABEL_ONE");

        //When
        labelRegistration.updateLabelsForNewResourceSet(resourceSet);

        //Then
        ArgumentCaptor<ResourceSetLabel> labelCaptor = ArgumentCaptor.forClass(ResourceSetLabel.class);
        verify(labelsStore).create(eq("REALM"), eq("RESOURCE_OWNER_ID"), labelCaptor.capture());
        assertThat(labelCaptor.getValue().getId()).isEqualTo("CLIENT_ID/LABEL_ONE");
        assertThat(labelCaptor.getValue().getName()).isEqualTo("LABEL_ONE");
    }

    private ResourceSetDescription newResourceSet(String... labels) {
        ResourceSetDescription resourceSet = new ResourceSetDescription();
        resourceSet.setId("RESOURCE_SET_ID");
        resourceSet.setRealm("REALM");
        resourceSet.setResourceOwnerId("RESOURCE_OWNER_ID");
        resourceSet.setClientId("CLIENT_ID");
        resourceSet.setDescription(json(object(field("labels", Arrays.asList(labels)))));
        return resourceSet;
    }

    private void givenLabelsDoesNotExist(String... labels) throws Exception {
        for (String label : labels) {
            doThrow(NotFoundException.class).when(labelsStore).read("REALM", "RESOURCE_OWNER_ID", "CLIENT_ID/" + label);
        }
    }

    private void givenLabelsExist(String... labels) throws Exception {
        for (String label : labels) {
            given(labelsStore.read("REALM", "RESOURCE_OWNER_ID", "CLIENT_ID/" + label)).willReturn(newLabel(label));
        }
    }

    private void givenLabelsForResourceSet(String... labels) throws Exception {
        Set<ResourceSetLabel> resourceSetLabels = new HashSet<>();
        for (String label : labels) {
            resourceSetLabels.add(newLabel(label));
        }
        given(labelsStore.forResourceSet("REALM", "RESOURCE_OWNER_ID", "RESOURCE_SET_ID", true))
                .willReturn(resourceSetLabels);
    }

    private ResourceSetLabel newLabel(String label) {
        Set<String> resourceSetIds = new HashSet<>();
        resourceSetIds.add("RESOURCE_SET_ID");
        return new ResourceSetLabel("CLIENT_ID/" + label, label, LabelType.SYSTEM, resourceSetIds);
    }

    private void givenLabelsAreNotIsUse(String... labels) throws Exception {
        for (String label : labels) {
            given(labelsStore.isLabelInUse("REALM", "RESOURCE_OWNER_ID", "CLIENT_ID/" + label)).willReturn(false);
        }
    }
}

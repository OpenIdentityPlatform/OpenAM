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

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.json;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.forgerock.openam.oauth2.resources.labels.LabelType;
import org.forgerock.openam.oauth2.resources.labels.ResourceSetLabel;
import org.forgerock.openam.oauth2.resources.labels.UmaLabelsStore;

/**
 * Updates Resource Set labels on Resource Set registration, update and deletion.
 *
 * @since 13.0.0
 */
public class ResourceSetLabelRegistration {

    private final Debug logger = Debug.getInstance("OAuth2Provider");
    private final UmaLabelsStore labelsStore;

    /**
     * Constructs an instance of a {@code ResourceSetLabelRegistration}.
     *
     * @param labelsStore An instance of the {@code UmaLabelsStore}.
     */
    @Inject
    public ResourceSetLabelRegistration(UmaLabelsStore labelsStore) {
        this.labelsStore = labelsStore;
    }

    /**
     * Adds labels to the new resource set, creating the label if required.
     *
     * @param resourceSet The new resource set.
     */
    void updateLabelsForNewResourceSet(ResourceSetDescription resourceSet) {
        JsonValue labels = resourceSet.getDescription().get(OAuth2Constants.ResourceSets.LABELS);
        if (!labels.isNull() && labels.size() > 0) {
            updateLabels(resourceSet, labels.asSet(String.class), Collections.<String>emptySet());
        }
    }

    /**
     * Adds and removes labels on the updated resource set, creating the label
     * if required and deleting labels which are no longer used.
     *
     * @param resourceSet The updated resource set.
     */
    void updateLabelsForExistingResourceSet(ResourceSetDescription resourceSet) {
        JsonValue newLabels = resourceSet.getDescription().get(OAuth2Constants.ResourceSets.LABELS);
        if (newLabels.isNull()) {
            newLabels = json(array());
        }
        Collection<String> addedLabels = new HashSet<>(newLabels.asSet(String.class));
        try {
            Set<ResourceSetLabel> labels = labelsStore.forResourceSet(resourceSet.getRealm(),
                    resourceSet.getResourceOwnerId(), resourceSet.getId(), true);
            Collection<String> removedLabels = new HashSet<>();
            for (ResourceSetLabel label : labels) {
                String labelName = label.getName().substring(label.getName().lastIndexOf("/") + 1);
                if (!addedLabels.remove(labelName)) {
                    removedLabels.add(labelName);
                }
            }

            updateLabels(resourceSet, addedLabels, removedLabels);
        } catch (ResourceException e) {
            logger.error("Failed to find current labels on resource set: {}", resourceSet.getId(), e);
        }
    }

    /**
     * Removes labels from the deleted resource set, deleting labels which are no longer used.
     *
     * @param resourceSet The deleted resource set.
     */
    void updateLabelsForDeletedResourceSet(ResourceSetDescription resourceSet) {
        JsonValue labels = resourceSet.getDescription().get(OAuth2Constants.ResourceSets.LABELS);
        if (!labels.isNull() && labels.size() > 0) {
            updateLabels(resourceSet, Collections.<String>emptySet(), labels.asSet(String.class));
        }
    }

    private void updateLabels(ResourceSetDescription resourceSet, Collection<String> addedLabels,
        Collection<String> removedLabels) {
        Collection<String> updatedLabels = new HashSet<>(addedLabels);
        updatedLabels.addAll(removedLabels);
        for (String label : updatedLabels) {
            try {
                String labelId = getLabelId(resourceSet.getClientId(), label);
                try {
                    ResourceSetLabel resourceSetLabel = labelsStore.read(resourceSet.getRealm(),
                            resourceSet.getResourceOwnerId(), labelId);
                    if (addedLabels.contains(label)) {
                        resourceSetLabel.addResourceSetId(resourceSet.getId());
                    } else if (removedLabels.contains(label)) {
                        resourceSetLabel.removeResourceSetId(resourceSet.getId());
                    }
                    labelsStore.update(resourceSet.getRealm(), resourceSet.getResourceOwnerId(), resourceSetLabel);
                    if (removedLabels.contains(label)) {
                        if (!labelsStore.isLabelInUse(resourceSet.getRealm(), resourceSet.getResourceOwnerId(),
                                labelId)) {
                            labelsStore.delete(resourceSet.getRealm(), resourceSet.getResourceOwnerId(),
                                    getLabelId(resourceSet.getClientId(), label));
                        }
                    }
                } catch (org.forgerock.json.resource.NotFoundException e) {
                    if (addedLabels.contains(label)) {
                        labelsStore.create(resourceSet.getRealm(), resourceSet.getResourceOwnerId(),
                                new ResourceSetLabel(labelId,
                                        label, LabelType.SYSTEM, Collections.singleton(resourceSet.getId())));
                    }
                }
            } catch (ResourceException e) {
                logger.error("Failed to update label, {}, on resource set: {}",
                        getLabelId(resourceSet.getClientId(), label), resourceSet.getId(), e);
            }
        }
    }

    private String getLabelId(String clientId, String label) {
        return clientId + "/" + label;
    }
}

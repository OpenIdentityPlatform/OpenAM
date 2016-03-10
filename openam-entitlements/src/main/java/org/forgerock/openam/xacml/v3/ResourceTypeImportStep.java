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

package org.forgerock.openam.xacml.v3;

import javax.security.auth.Subject;

import org.forgerock.openam.entitlement.ResourceType;
import org.forgerock.openam.entitlement.service.ResourceTypeService;

import com.sun.identity.entitlement.EntitlementException;

/**
 * Describes how a Resource Type data read from XACML Policy will be imported into OpenAM.
 *
 * @since 13.5.0
 */
class ResourceTypeImportStep implements PersistableImportStep<ResourceType> {

    public static final String TYPE = "ResourceType";

    private final DiffStatus diffStatus;
    private ResourceType resourceType;
    private ResourceTypeService resourceTypeService;
    private String realm;
    private Subject subject;

    /**
     * Constructs ResourceTypeImportStep instance.
     *
     * @param diffStatus
     *         status of the import step.
     * @param resourceType
     *         resource type being imported.
     * @param resourceTypeService
     *         service class for saving resource type.
     * @param realm
     *         to which the resource type belongs.
     * @param subject
     *         admin subject.
     */
    ResourceTypeImportStep(DiffStatus diffStatus, ResourceType resourceType,
            ResourceTypeService resourceTypeService, String realm, Subject subject) {
        this.diffStatus = diffStatus;
        this.resourceType = resourceType;
        this.resourceTypeService = resourceTypeService;
        this.realm = realm;
        this.subject = subject;
    }

    @Override
    public DiffStatus getDiffStatus() {
        return diffStatus;
    }

    @Override
    public String getName() {
        return resourceType.getName();
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void apply() throws EntitlementException {
        switch (diffStatus) {
        case ADD:
            resourceTypeService.saveResourceType(subject, realm, resourceType);
            break;
        case UPDATE:
            resourceTypeService.updateResourceType(subject, realm, resourceType);
            break;
        }
    }

    @Override
    public ResourceType get() {
        return resourceType;
    }
}

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

package org.forgerock.openam.rest.uma;

import javax.inject.Inject;
import javax.security.auth.Subject;

import java.util.Collections;

import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.forgerock.oauth2.restlet.resources.ResourceSetRegistrationListener;
import org.forgerock.openam.entitlement.ResourceType;
import org.forgerock.openam.entitlement.service.ResourceTypeService;
import org.forgerock.openam.forgerockrest.entitlements.wrappers.ApplicationManagerWrapper;
import org.forgerock.openam.uma.UmaConstants;

/**
 * Listener implementation for creating a ResourceType for each Resource Set registration.
 *
 * @since 13.0.0
 */
public class UmaResourceSetRegistrationListener implements ResourceSetRegistrationListener {

    private final Debug logger = Debug.getInstance("UmaProvider");
    private final ResourceTypeService resourceTypeService;
    private final ApplicationManagerWrapper applicationManager;

    /**
     * Creates a new UmaResourceSetRegistrationListener instance.
     *
     * @param resourceTypeService An instance of the {@code ResourceTypeService}.
     * @param applicationManager An instance of the {@code ApplicationManagerWrapper}.
     */
    @Inject
    public UmaResourceSetRegistrationListener(ResourceTypeService resourceTypeService,
            ApplicationManagerWrapper applicationManager) {
        this.resourceTypeService = resourceTypeService;
        this.applicationManager = applicationManager;
    }

    /**
     * Creates a ResourceType for the Resource Set and adds it to the Resource Server's policy Application.
     *
     * @param realm {@inheritDoc}
     * @param resourceSet {@inheritDoc}
     */
    @Override
    public void resourceSetCreated(String realm, ResourceSetDescription resourceSet) {
        ResourceType resourceType = ResourceType.builder(resourceSet.getName() + " - " + resourceSet.getId(), realm)
                .setUUID(resourceSet.getId())
                .addPattern(UmaConstants.UMA_POLICY_SCHEME_PATTERN).build();
        Subject adminSubject = SubjectUtils.createSuperAdminSubject();
        try {
            resourceTypeService.saveResourceType(adminSubject, resourceType);
        } catch (EntitlementException e) {
            if (logger.errorEnabled()) {
                logger.error("Failed to create resource type for resource set, " + resourceSet, e);
            }
        }
        try {
            Application application = applicationManager.getApplication(adminSubject, realm,
                    resourceSet.getClientId().toLowerCase());
            application.addAllResourceTypeUuids(Collections.singleton(resourceType.getUUID()));
            applicationManager.saveApplication(adminSubject, application);
        } catch (EntitlementException e) {
            if (logger.errorEnabled()) {
                logger.error("Failed to add Resource Type, " + resourceType.getUUID() + " to application, "
                        + resourceSet.getClientId(), e);
            }
        }
    }
}

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
 * Copyright 2015-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.uma.rest;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;
import javax.security.auth.Subject;

import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.oauth2.ResourceSetDescription;
import org.forgerock.oauth2.restlet.resources.ResourceSetRegistrationHook;
import org.forgerock.openam.entitlement.ResourceType;
import org.forgerock.openam.entitlement.service.ApplicationService;
import org.forgerock.openam.entitlement.service.ApplicationServiceFactory;
import org.forgerock.openam.entitlement.service.ResourceTypeService;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.AdminSubjectContext;
import org.forgerock.openam.rest.resource.SubjectContext;
import org.forgerock.openam.session.SessionCache;
import org.forgerock.openam.uma.UmaConstants;
import org.forgerock.openam.uma.UmaPolicyService;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;

import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.shared.debug.Debug;

/**
 * Hook implementation for creating a ResourceType for each Resource Set registration.
 *
 * @since 13.0.0
 */
public class UmaResourceSetRegistrationHook implements ResourceSetRegistrationHook {

    private final Debug logger = Debug.getInstance("UmaProvider");
    private final ResourceTypeService resourceTypeService;
    private final ApplicationServiceFactory applicationServiceFactory;
    private final UmaPolicyService policyService;
    private final SessionCache sessionCache;

    /**
     * Creates a new UmaResourceSetRegistrationHook instance.
     * @param resourceTypeService An instance of the {@code ResourceTypeService}.
     * @param applicationServiceFactory An instance of the {@code ApplicationServiceFactory}.
     * @param policyService An instance of the {@code UmaPolicyService}.
     * @param sessionCache An instance of the {@code SessionCache}.
     */
    @Inject
    public UmaResourceSetRegistrationHook(ResourceTypeService resourceTypeService,
            ApplicationServiceFactory applicationServiceFactory, UmaPolicyService policyService,
            SessionCache sessionCache) throws EntitlementException{
        this.resourceTypeService = resourceTypeService;
        this.applicationServiceFactory = applicationServiceFactory;
        this.policyService = policyService;
        this.sessionCache = sessionCache;
    }

    /**
     * Creates a ResourceType for the Resource Set and adds it to the Resource Server's policy Application.
     *
     * @param realm {@inheritDoc}
     * @param resourceSet {@inheritDoc}
     */
    @Override
    public void resourceSetCreated(String realm, ResourceSetDescription resourceSet)
            throws ServerException
    {
        Map<String, Boolean> resourceTypeActions = new HashMap<String, Boolean>();
        for (String umaScope : resourceSet.getScopes()) {
            resourceTypeActions.put(umaScope, Boolean.TRUE);
        }
        ResourceType resourceType = ResourceType.builder()
                .setName(resourceSet.getName() + " - " + resourceSet.getId())
                .setUUID(resourceSet.getId())
                .setDescription("Dynamically created resource type for the UMA resource set. " +
                        "Used to find all Policy Engine Policies that make up an UMA Policy")
                .setActions(resourceTypeActions)
                .addPattern(UmaConstants.UMA_POLICY_SCHEME_PATTERN).build();
        Subject adminSubject = SubjectUtils.createSuperAdminSubject();
        try {
            resourceTypeService.saveResourceType(adminSubject, realm, resourceType);
        } catch (EntitlementException e) {
            logger.error("Failed to create resource type for resource set, {}", resourceSet, e);
            throw new ServerException(e);
        }
        try {
            ApplicationService appService = applicationServiceFactory.create(adminSubject, realm);
            Application application = appService.getApplication(resourceSet.getClientId().toLowerCase());
            application.addResourceTypeUuid(resourceType.getUUID());
            appService.saveApplication(application);
        } catch (EntitlementException e) {
            logger.error("Failed to add Resource Type, " + resourceType.getUUID() + " to application, "
                    + resourceSet.getClientId(), e);
            throw new ServerException(e);
        }
    }

    /**
     * Removes the ResourceType from the Resource Server's policy application, deletes all related policies,
     * then deletes the ResourceSet.
     *
     * @param realm {@inheritDoc}
     * @param resourceSet {@inheritDoc}
     */
    @Override
    public void resourceSetDeleted(String realm, ResourceSetDescription resourceSet) throws ServerException {
        Subject adminSubject = SubjectUtils.createSuperAdminSubject();
        String resourceTypeUUID = resourceSet.getId();
        try {
            ApplicationService appService = applicationServiceFactory.create(adminSubject, realm);
            Application application = appService.getApplication(resourceSet.getClientId().toLowerCase());
            application.removeResourceTypeUuid(resourceTypeUUID);
            appService.saveApplication(application);
        } catch (EntitlementException e) {
            logger.error("Failed to remove Resource Type, " + resourceTypeUUID + " from application, "
                    + resourceSet.getClientId(), e);
            throw new ServerException(e);
        }

        try {
            policyService.deletePolicy(createAdminContext(realm, resourceSet.getResourceOwnerId()), resourceSet.getId());

            resourceTypeService.deleteResourceType(adminSubject, realm, resourceTypeUUID);
        } catch (EntitlementException e) {
            logger.error("Failed to delete Resource Type " + resourceTypeUUID, e);
            throw new ServerException(e);
        } catch (RealmLookupException e) {
            //Should never happen as realm would have been validated already
            logger.error("Failed to delete Resource Type " + resourceTypeUUID, e);
            throw new ServerException(e);
        }
    }

    /**
     * Used to create a context for deleting policies. If this is being called, we know that the user has the right
     * to delete the policies.
     * @param realm The realm to delete the policies in.
     * @param resourceOwnerId The owner of the ResourceSet that the policies are for.
     * @return The generated context.
     */
    private Context createAdminContext(String realm, String resourceOwnerId) throws RealmLookupException {
        RealmContext realmContext = new RealmContext(new RootContext(), Realm.of(realm));
        SubjectContext subjectContext = new AdminSubjectContext(logger, sessionCache,realmContext);

        Map<String, String> templateVariables = new HashMap<>();
        templateVariables.put("user", resourceOwnerId);
        return new UriRouterContext(subjectContext, "", "", templateVariables);
    }
}

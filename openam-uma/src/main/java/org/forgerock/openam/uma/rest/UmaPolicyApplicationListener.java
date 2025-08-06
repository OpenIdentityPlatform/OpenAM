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

import static org.forgerock.openam.uma.UmaConstants.UMA_BACKEND_POLICY_RESOURCE_HANDLER;
import static org.forgerock.openam.utils.CollectionUtils.asSet;

import java.security.AccessController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import javax.security.auth.Subject;

import com.sun.identity.common.configuration.AgentConfiguration;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.entitlement.EntitlementRegistry;
import org.forgerock.openam.oauth2.ResourceSetDescription;
import org.forgerock.oauth2.resources.ResourceSetStore;
import org.forgerock.openam.cts.api.fields.ResourceSetTokenField;
import org.forgerock.openam.entitlement.rest.wrappers.ApplicationTypeManagerWrapper;
import org.forgerock.openam.entitlement.service.ApplicationService;
import org.forgerock.openam.entitlement.service.ApplicationServiceFactory;
import org.forgerock.openam.identity.idm.AMIdentityRepositoryFactory;
import org.forgerock.openam.oauth2.resources.ResourceSetStoreFactory;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.AdminSubjectContext;
import org.forgerock.openam.session.SessionCache;
import org.forgerock.openam.uma.UmaConstants;
import org.forgerock.openam.uma.UmaUtils;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.OpenAMSettings;
import org.forgerock.openam.utils.OpenAMSettingsImpl;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.promise.ExceptionHandler;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.forgerock.util.query.QueryFilter;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.entitlement.DenyOverride;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.JwtClaimSubject;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdEventListener;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.SMSException;

/**
 * Listens for changes to UMA Resource Server (OAuth2 Agent) to create or delete its policy
 * application.
 *
 * @since 13.0.0
 */
public class UmaPolicyApplicationListener implements IdEventListener {

    private final Debug logger = Debug.getInstance("UmaProvider");

    private final AMIdentityRepositoryFactory idRepoFactory;
    private final ApplicationServiceFactory applicationServiceFactory;
    private final ApplicationTypeManagerWrapper applicationTypeManagerWrapper;
    private final RequestHandler policyResource;
    private final ResourceSetStoreFactory resourceSetStoreFactory;
    private final SessionCache sessionCache;


    private static final int NO_ACTION =  1;
    private static final int CREATE_UMA_APPLICATION = 2;
    private static final int REMOVE_UMA_APPLICATION = 3;

    /**
     * Creates an instance of the {@code UmaPolicyApplicationListener}.
     * @param idRepoFactory An instance of the {@code AMIdentityRepositoryFactory}.
     * @param applicationServiceFactory An instance of the {@code ApplicationServiceFactory}.
     * @param applicationTypeManagerWrapper An instance of the {@code ApplicationTypeManagerWrapper}.
     * @param policyResource An instance of the policy backend {@code PromisedRequestHandler}.
     * @param resourceSetStoreFactory An instance of the {@code ResourceSetStoreFactory}.
     * @param sessionCache The cache of session instances.
     */
    @Inject
    public UmaPolicyApplicationListener(final AMIdentityRepositoryFactory idRepoFactory,
            ApplicationServiceFactory applicationServiceFactory,
            ApplicationTypeManagerWrapper applicationTypeManagerWrapper,
            @Named(UMA_BACKEND_POLICY_RESOURCE_HANDLER) RequestHandler policyResource,
            ResourceSetStoreFactory resourceSetStoreFactory, SessionCache sessionCache) {
        this.idRepoFactory = idRepoFactory;
        this.applicationServiceFactory = applicationServiceFactory;
        this.applicationTypeManagerWrapper = applicationTypeManagerWrapper;
        this.policyResource = policyResource;
        this.resourceSetStoreFactory = resourceSetStoreFactory;
        this.sessionCache = sessionCache;
    }

    /**
     * Ensures that if the identity is a UMA resource server then a policy application exists for
     * it, otherwise (based on configuration) deletes the resource servers policy application,
     * policies and resource sets.
     *
     * @param universalId {@inheritDoc}
     */
    @Override
    public void identityChanged(String universalId) {
        try {
            AMIdentity identity = getIdentity(universalId);
            if (!isAgentIdentity(identity)) {
                return;
            }

            switch (getIdentityAction(identity)) {

            case NO_ACTION:
                logger.message("Identity is not an OAuth agent no action needed");
                break;
            case CREATE_UMA_APPLICATION:
                createApplication(identity.getRealm(), identity.getName());
                break;
            case REMOVE_UMA_APPLICATION:
                removeApplication(identity.getRealm(), identity.getName());
                break;

            default:
                logger.error("Failed to handle identity action");
            }
        } catch (IdRepoException e) {
            logger.error("Failed to get identity", e);
        } catch (SSOException e) {
            logger.error("Failed to get identity", e);
        } catch (NotFoundException e) {
            logger.error("Failed to get UMA Provider settings", e);
        } catch (ServerException e) {
            logger.error("Failed to get UMA Provider settings", e);
        }
    }

    /**
     * Not required.
     *
     * @param universalId {@inheritDoc}
     */
    @Override
    public void identityRenamed(String universalId) {
        //OAuth2 agents cannot be renamed
    }

    /**
     * Deletes, (based on configuration), the resource servers policy application, policies and
     * resource sets.
     *
     * @param universalId {@inheritDoc}
     */
    @Override
    public void identityDeleted(String universalId) {
        try {
            AMIdentity identity = getIdentity(universalId);
            if (!isAgentIdentity(identity)) {
                return;
            }
            removeApplication(identity.getRealm(), identity.getName());

        } catch (IdRepoException e) {
            logger.error("Failed to get identity", e);
        } catch (NotFoundException e) {
            logger.error("Failed to get UMA Provider settings", e);
        } catch (ServerException e) {
            logger.error("Failed to get UMA Provider settings", e);
        }
    }

    /**
     * Not required.
     */
    @Override
    public void allIdentitiesChanged() {
    }

    private AMIdentity getIdentity(String universalId) throws IdRepoException {
        return new AMIdentity(null, universalId);
    }

    private boolean isAgentIdentity(AMIdentity identity) {
        return IdType.AGENT.equals(identity.getType());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Set<String>> getIdentityAttributes(AMIdentity identity) throws IdRepoException, SSOException {
        IdSearchControl searchControl = new IdSearchControl();
        searchControl.setReturnAttributes(CollectionUtils.asSet(IdConstants.AGENT_TYPE, OAuth2Constants.OAuth2Client.SCOPES));
        searchControl.setMaxResults(0);
        SSOToken adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
        IdSearchResults searchResults = idRepoFactory.create(identity.getRealm(), adminToken)
                .searchIdentities(IdType.AGENT, identity.getName(), searchControl);
        if (searchResults.getSearchResults().size() != 1) {
            throw new IdRepoException("UmaPolicyApplicationListener.getIdentityAttributes : More than one agent found");
        }
        return new HashMap<String, Set<String>>((Map) searchResults.getResultAttributes().values().iterator().next());
    }

    private void createApplication(String realm, String resourceServerId) {
        Subject adminSubject = SubjectUtils.createSuperAdminSubject();
        try {
            ApplicationService appService = applicationServiceFactory.create(adminSubject, realm);
            Application application = appService.getApplication(resourceServerId);
            if (application == null) {
                ApplicationType applicationType = applicationTypeManagerWrapper.getApplicationType(adminSubject,
                        UmaConstants.UMA_POLICY_APPLICATION_TYPE);
                application = new Application(resourceServerId, applicationType);
                application.setEntitlementCombiner(DenyOverride.class);
                application.setSubjects(asSet(EntitlementRegistry.getSubjectTypeName(JwtClaimSubject.class)));
                appService.saveApplication(application);
            }
        } catch (EntitlementException e) {
            logger.error("Failed to create policy application", e);
        }
    }

    private void removeApplication(String realm, String resourceServerId) throws NotFoundException, ServerException {
        OpenAMSettingsImpl umaSettings = new OpenAMSettingsImpl(UmaConstants.SERVICE_NAME, UmaConstants.SERVICE_VERSION);
        if (onDeleteResourceServerDeletePolicies(umaSettings, realm)) {
            try {
                deletePolicies(realm, resourceServerId);
                Subject adminSubject = SubjectUtils.createSuperAdminSubject();
                ApplicationService appService = applicationServiceFactory.create(adminSubject, realm);
                if (appService.getApplication(resourceServerId) != null) {
                    appService.deleteApplication(resourceServerId);
                }
            } catch (EntitlementException e) {
                logger.error("Failed to remove policy application", e);
            } catch (RealmLookupException e) {
                //Should never happen as realm would have already been validated
                logger.error("Failed to remove policy application", e);
            }
        }
        if (onDeleteResourceServerDeleteResourceSets(umaSettings, realm)) {
            deleteResourceSets(realm, resourceServerId);
        }
    }

    private boolean onDeleteResourceServerDeletePolicies(OpenAMSettings umaSettings, String realm) throws ServerException {
        try {
            return umaSettings.getBooleanSetting(realm, UmaConstants.DELETE_POLICIES_ON_RESOURCE_SERVER_DELETION);
        } catch (SMSException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        } catch (SSOException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        }
    }

    private boolean onDeleteResourceServerDeleteResourceSets(OpenAMSettings umaSettings, String realm) throws ServerException {
        try {
            return umaSettings.getBooleanSetting(realm, UmaConstants.DELETE_RESOURCE_SETS_ON_RESOURCE_SERVER_DELETION);
        } catch (SMSException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        } catch (SSOException e) {
            logger.error(e.getMessage());
            throw new ServerException(e);
        }
    }

    private void deletePolicies(String realm, String resourceServerId) throws RealmLookupException {
        RealmContext realmContext = new RealmContext(new RootContext(), Realm.of(realm));
        final Context context = new AdminSubjectContext(logger, sessionCache, realmContext);
        QueryRequest request = Requests.newQueryRequest("")
                .setQueryFilter(QueryFilter.equalTo(new JsonPointer("applicationName"), resourceServerId));
        final List<ResourceResponse> resources = new ArrayList<>();
        policyResource.handleQuery(context, request, new QueryResourceHandler() {
            @Override
            public boolean handleResource(ResourceResponse resource) {
                resources.add(resource);
                return true;
            }
        })
                .thenAsync(new AsyncFunction<QueryResponse, List<ResourceResponse>, ResourceException>() {
                    @Override
                    public Promise<List<ResourceResponse>, ResourceException> apply(QueryResponse response) {
                        List<Promise<ResourceResponse, ResourceException>> promises = new ArrayList<>();
                        for (ResourceResponse policy : resources) {
                            DeleteRequest deleteRequest = Requests.newDeleteRequest("", policy.getId());
                            promises.add(policyResource.handleDelete(context, deleteRequest));
                        }
                        Promise<List<ResourceResponse>, ResourceException> when = Promises.when(promises);
                        return when;
                    }
                })
                .thenOnException(new ExceptionHandler<ResourceException>() {
                    @Override
                    public void handleException(ResourceException error) {
                        logger.error(error.getReason());
                    }
                });
    }

    private void deleteResourceSets(String realm, String resourceServerId) throws NotFoundException, ServerException {
        ResourceSetStore resourceSetStore = resourceSetStoreFactory.create(DNMapper.orgNameToRealmName(realm));
        QueryFilter<String> queryFilter
                = QueryFilter.equalTo(ResourceSetTokenField.CLIENT_ID, resourceServerId);
        Set<ResourceSetDescription> results = resourceSetStore.query(queryFilter);
        for (ResourceSetDescription resourceSet : results) {
            resourceSetStore.delete(resourceSet.getId(), resourceSet.getResourceOwnerId());
        }
    }

    private int getIdentityAction(AMIdentity identity) throws IdRepoException, SSOException {
        Map<String, Set<String>> attrValues = getIdentityAttributes(identity);
        String agentType =  CollectionHelper.getMapAttr(attrValues, IdConstants.AGENT_TYPE, "NO_TYPE");
        if (!AgentConfiguration.AGENT_TYPE_OAUTH2.equalsIgnoreCase(agentType)) {
            return NO_ACTION;
        } else if (UmaUtils.isUmaResourceServerAgent(attrValues)) {
            return CREATE_UMA_APPLICATION;
        } else {
            return REMOVE_UMA_APPLICATION;
        }
    }

}

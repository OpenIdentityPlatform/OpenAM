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

import javax.inject.Inject;

import java.util.Set;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2Uris;
import org.forgerock.oauth2.core.OAuth2UrisFactory;
import org.forgerock.oauth2.core.ResourceSetFilter;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.forgerock.oauth2.resources.ResourceSetStore;
import org.forgerock.openam.core.RealmInfo;
import org.forgerock.openam.cts.api.fields.ResourceSetTokenField;
import org.forgerock.openam.cts.api.tokens.TokenIdGenerator;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.sm.datalayer.store.TokenDataStore;
import org.forgerock.util.query.QueryFilter;

/**
 * Stores {@code ResourceSetDescription} objects in the CTS.
 *
 * @since 13.0.0
 */
public class OpenAMResourceSetStore implements ResourceSetStore {

    private final Debug logger = Debug.getInstance("OAuth2Provider");
    private final String realm;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final OAuth2UrisFactory<RealmInfo> oauth2UrisFactory;
    private final TokenDataStore<ResourceSetDescription> delegate;
    private final TokenIdGenerator idGenerator;

    /**
     * Constructs a new OpenAMResourceSetStore instance.
     *
     * @param realm The realm this ResourceSetStore is in.
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     * @param oauth2UrisFactory An instance of the OAuth2UrisFactory.
     */
    @Inject
    public OpenAMResourceSetStore(@Assisted String realm, OAuth2ProviderSettingsFactory providerSettingsFactory,
            OAuth2UrisFactory<RealmInfo> oauth2UrisFactory, TokenIdGenerator idGenerator,
            @DataLayer(ConnectionType.RESOURCE_SETS) TokenDataStore delegate) {
        this.realm = realm;
        this.providerSettingsFactory = providerSettingsFactory;
        this.oauth2UrisFactory = oauth2UrisFactory;
        this.delegate = delegate;
        this.idGenerator = idGenerator;
    }

    @Override
    public void create(OAuth2Request request, ResourceSetDescription resourceSetDescription) throws ServerException,
            BadRequestException, NotFoundException {
        resourceSetDescription.setId(idGenerator.generateTokenId(null));
        String policyEndpoint = oauth2UrisFactory.get(request)
                .getResourceSetRegistrationPolicyEndpoint(resourceSetDescription.getId());
        resourceSetDescription.setPolicyUri(policyEndpoint);
        resourceSetDescription.setRealm(realm);
        try {
            delegate.create(resourceSetDescription);
        } catch (org.forgerock.openam.sm.datalayer.store.ServerException e) {
            throw new ServerException(e);
        }
    }

    @Override
    public ResourceSetDescription read(String resourceSetId, ResourceSetFilter filter) throws NotFoundException,
            ServerException {
        Set<ResourceSetDescription> resourceSets =
                query(QueryFilter.equalTo(ResourceSetTokenField.RESOURCE_SET_ID, resourceSetId));

        if(filter != null) {
            resourceSets = filter.filter(resourceSets);
        }
        if(!resourceSets.isEmpty()) {
            return resourceSets.iterator().next();
        }

        throw new NotFoundException("Resource set does not exist with id " + resourceSetId);
    }

    @Override
    public ResourceSetDescription read(String resourceSetId, String resourceOwnerId) throws NotFoundException, ServerException {
        return read(resourceSetId, new ResourceSetOwnerFilter(resourceOwnerId));
    }

    @Override
    public void update(ResourceSetDescription resourceSetDescription) throws NotFoundException, ServerException {
        try {
            if (!realm.equals(resourceSetDescription.getRealm())) {
                throw new ServerException("Could not read token with id, " + resourceSetDescription.getId()
                        + ", in realm, " + realm);
            }
            read(resourceSetDescription.getId(), resourceSetDescription.getResourceOwnerId());
            delegate.update(resourceSetDescription);
        } catch (org.forgerock.openam.sm.datalayer.store.NotFoundException e) {
            throw new NotFoundException("Resource set does not exist with id " + resourceSetDescription.getId());
        } catch (org.forgerock.openam.sm.datalayer.store.ServerException e) {
            throw new ServerException(e);
        }
    }

    @Override
    public void delete(String resourceSetId, String resourceOwnerId) throws NotFoundException, ServerException {
        try {
            ResourceSetDescription token = read(resourceSetId, resourceOwnerId);
            delegate.delete(token.getId());
        } catch (org.forgerock.openam.sm.datalayer.store.NotFoundException e) {
            throw new NotFoundException("Could not find resource set");
        } catch (org.forgerock.openam.sm.datalayer.store.ServerException e) {
            throw new ServerException(e);
        }
    }

    @Override
    public Set<ResourceSetDescription> query(QueryFilter<String> query) throws ServerException {
        Set<ResourceSetDescription> results;
        try {
            results = delegate.query(QueryFilter.and(query,
                    QueryFilter.equalTo(ResourceSetTokenField.REALM, realm)));
        } catch (org.forgerock.openam.sm.datalayer.store.ServerException e) {
            throw new ServerException(e);
        }
        return results;
    }

}

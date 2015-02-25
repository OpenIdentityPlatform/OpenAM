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

import static org.forgerock.util.query.QueryFilter.and;
import static org.forgerock.util.query.QueryFilter.equalTo;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.Set;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.forgerock.oauth2.resources.ResourceSetStore;
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
    private final TokenDataStore<ResourceSetDescription> delegate;
    private final TokenIdGenerator idGenerator;

    /**
     * Constructs a new OpenAMResourceSetStore instance.
     *
     * @param realm The realm this ResourceSetStore is in.
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     */
    @Inject
    public OpenAMResourceSetStore(@Assisted String realm, OAuth2ProviderSettingsFactory providerSettingsFactory,
            TokenIdGenerator idGenerator, @DataLayer(ConnectionType.RESOURCE_SETS) TokenDataStore delegate) {
        this.realm = realm;
        this.providerSettingsFactory = providerSettingsFactory;
        this.delegate = delegate;
        this.idGenerator = idGenerator;
    }

    @Override
    public void create(OAuth2Request request, ResourceSetDescription resourceSetDescription) throws ServerException,
            BadRequestException, NotFoundException {
        resourceSetDescription.setId(idGenerator.generateTokenId(null));
        String policyEndpoint = providerSettingsFactory.get(request)
                .getResourceSetRegistrationPolicyEndpoint(resourceSetDescription.getId());
        resourceSetDescription.setPolicyUri(policyEndpoint);
        resourceSetDescription.setRealm(realm);
        try {
            delegate.create(resourceSetDescription);
        } catch (org.forgerock.openam.sm.datalayer.store.ServerException e) {
            throw new ServerException(e);
        }
    }

    private ResourceSetDescription readResourceSet(String resourceSetId, String clientId) throws ServerException {
        Set<ResourceSetDescription> results;
        try {
            results = delegate.query(and(
                    equalTo(ResourceSetTokenField.RESOURCE_SET_ID, resourceSetId),
                    equalTo(ResourceSetTokenField.CLIENT_ID, clientId)));
        } catch (org.forgerock.openam.sm.datalayer.store.ServerException e) {
            throw new ServerException(e);
        }
        if (results.isEmpty() || results.size() > 1) {
            return null;
        } else {
            ResourceSetDescription resourceSet = results.iterator().next();
            if (!realm.equals(resourceSet.getRealm())) {
                return null;
            }
            return resourceSet;
        }
    }

    @Override
    public ResourceSetDescription read(String resourceSetId, String clientId) throws NotFoundException, ServerException {
        ResourceSetDescription token = readResourceSet(resourceSetId, clientId);
        if (token == null) {
            if (logger.warningEnabled()) {
                logger.warning("Resource set corresponding to id: " + resourceSetId + " not found");
            }
            throw new NotFoundException("Resource set corresponding to id: " + resourceSetId + " not found");
        }
        return token;
    }

    @Override
    public ResourceSetDescription read(String resourceSetUID) throws NotFoundException, ServerException {
        try {
            return delegate.read(resourceSetUID);
        } catch (org.forgerock.openam.sm.datalayer.store.NotFoundException e) {
            throw new NotFoundException("Resource set does not exist with id " + resourceSetUID);
        } catch (org.forgerock.openam.sm.datalayer.store.ServerException e) {
            throw new ServerException(e);
        }
    }

    @Override
    public void update(ResourceSetDescription resourceSetDescription) throws NotFoundException, ServerException {
        try {
            if (!realm.equals(resourceSetDescription.getRealm())) {
                throw new ServerException("Could not read token with id, " + resourceSetDescription.getId()
                        + ", in realm, " + realm);
            }
            delegate.update(resourceSetDescription);
        } catch (org.forgerock.openam.sm.datalayer.store.NotFoundException e) {
            throw new NotFoundException("Resource set does not exist with id " + resourceSetDescription.getId());
        } catch (org.forgerock.openam.sm.datalayer.store.ServerException e) {
            throw new ServerException(e);
        }
    }

    @Override
    public void delete(String resourceSetId, String clientId) throws NotFoundException, ServerException {
        try {
            ResourceSetDescription token = readResourceSet(resourceSetId, clientId);
            if (token == null) {
                if (logger.errorEnabled()) {
                    logger.error("Resource set corresponding to id: " + resourceSetId + " not found");
                }
                throw new NotFoundException("Resource set corresponding to id: " + resourceSetId + " not found");
            }
            delegate.delete(token.getId());
        } catch (org.forgerock.openam.sm.datalayer.store.NotFoundException e) {
            throw new NotFoundException("Could not find resource set");
        } catch (org.forgerock.openam.sm.datalayer.store.ServerException e) {
            throw new ServerException(e);
        }
    }

    @Override
    public Set<ResourceSetDescription> query(QueryFilter<String> query)
            throws ServerException {
        Set<ResourceSetDescription> results;
        try {
            results = delegate.query(query);
        } catch (org.forgerock.openam.sm.datalayer.store.ServerException e) {
            throw new ServerException(e);
        }
        //Ignore tokens in a different realm
        Iterator<ResourceSetDescription> resourceSets = results.iterator();
        while(resourceSets.hasNext()) {
            if (!realm.equals(resourceSets.next().getRealm())) {
                resourceSets.remove();
            }
        }
        return results;
    }

}

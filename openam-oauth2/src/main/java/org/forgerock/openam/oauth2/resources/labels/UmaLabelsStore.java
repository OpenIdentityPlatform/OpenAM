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
 */

package org.forgerock.openam.oauth2.resources.labels;

import static org.forgerock.openam.oauth2.resources.labels.LabelsConstants.*;
import static org.forgerock.opendj.ldap.Filter.*;
import static org.forgerock.opendj.ldap.ModificationType.REPLACE;

import com.google.inject.Inject;
import com.sun.identity.shared.debug.Debug;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.cts.api.tokens.TokenIdGenerator;
import org.forgerock.openam.ldap.LDAPRequests;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.impl.ldap.LdapDataLayerConfiguration;
import org.forgerock.openam.sm.datalayer.providers.LdapConnectionFactoryProvider;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.SearchResultReferenceIOException;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.requests.AddRequest;
import org.forgerock.opendj.ldap.responses.Result;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldif.ConnectionEntryReader;

/**
 * This class stores and gives access to UMA Resource Set labels. The underlying data
 * is accessed using the DJ LDAP SDK.
 */
public class UmaLabelsStore {

    private final Debug debug = Debug.getInstance("UmaProvider");
    private final ConnectionFactory<Connection> connectionFactory;
    private final LdapDataLayerConfiguration ldapConfiguration;
    private final TokenIdGenerator tokenIdGenerator;

    /**
     * Guice constructor for the store.
     * @param connectionFactoryProvider Used to access DJ LDAP SDK {@code Connection} instances.
     * @param ldapConfiguration Provides the LDAP top level DN in which the data has been stored.
     * @param tokenIdGenerator Generates IDs for the label instances.
     */
    @Inject
    public UmaLabelsStore(LdapConnectionFactoryProvider connectionFactoryProvider,
            LdapDataLayerConfiguration ldapConfiguration, TokenIdGenerator tokenIdGenerator) {
        this.tokenIdGenerator = tokenIdGenerator;
        this.connectionFactory = connectionFactoryProvider.createFactory();
        this.ldapConfiguration = ldapConfiguration;
    }

    /**
     * Creates the provided {@link ResourceSetLabel} in the database, and returns an instance
     * with the {@link ResourceSetLabel#id} field populated with the value used.
     * @param realm The current realm.
     * @param username The user that owns the label.
     * @param label The label instance. The {@code id} field should be null.
     * @return A label instance with the {@code id} field populated.
     * @throws ResourceException Thrown if the label cannot be created.
     */
    public ResourceSetLabel create(String realm, String username, ResourceSetLabel label) throws ResourceException {
        String id = tokenIdGenerator.generateTokenId(label.getId());
        try (Connection connection = getConnection()) {
            return createLabel(realm, username, label, id, connection);
        } catch (LdapException e) {
            if (e.getResult().getResultCode().equals(ResultCode.NO_SUCH_OBJECT)) {
                DN userDn = getUserDn(realm, username);
                DN realmDn = userDn.parent();
                try (Connection connection = getConnection()) {
                    try {
                        connection.add(LDAPRequests.newAddRequest(realmDn)
                                .addAttribute("ou", LDAPUtils.rdnValueFromDn(realmDn))
                                .addAttribute("objectClass", "top", ORG_UNIT_OBJECT_CLASS));
                    } catch (LdapException ex) {
                        if (!ex.getResult().getResultCode().equals(ResultCode.ENTRY_ALREADY_EXISTS)) {
                            throw new InternalServerErrorException("Could not create realm entry " + realmDn, ex);
                        }
                    }
                    try {
                        connection.add(LDAPRequests.newAddRequest(userDn)
                                .addAttribute("ou", LDAPUtils.rdnValueFromDn(userDn))
                                .addAttribute("objectClass", "top", ORG_UNIT_OBJECT_CLASS));
                    } catch (LdapException ex) {
                        throw new InternalServerErrorException("Could not create user entry " + userDn, ex);
                    }
                    return createLabel(realm, username, label, id, connection);
                } catch (LdapException e1) {
                    e = e1;
                }
            }
            if (e.getResult().getResultCode().equals(ResultCode.ENTRY_ALREADY_EXISTS)) {
                throw new ConflictException();
            }
            throw new InternalServerErrorException("Could not create", e);
        }
    }

    private ResourceSetLabel createLabel(String realm, String username, ResourceSetLabel label, String id,
                                         Connection connection) throws LdapException, InternalServerErrorException {
        final AddRequest addRequest = LDAPRequests.newAddRequest(getLabelDn(realm, username, id))
                .addAttribute("objectClass", "top", OBJECT_CLASS)
                .addAttribute(ID_ATTR, id)
                .addAttribute(NAME_ATTR, label.getName())
                .addAttribute(TYPE_ATTR, label.getType().name());
        if (CollectionUtils.isNotEmpty(label.getResourceSetIds())) {
            addRequest.addAttribute(RESOURCE_SET_ATTR, label.getResourceSetIds().toArray());
        }
        Result result = connection.add(addRequest);
        if (!result.isSuccess()) {
            throw new InternalServerErrorException("Unknown unsuccessful request");
        }
        return new ResourceSetLabel(id, label.getName(), label.getType(), label.getResourceSetIds());
    }

    /**
     * Reads a label from the underlying database.
     * @param realm The current realm.
     * @param username The user that owns the label.
     * @param id The id of the label.
     * @return The retrieved label details.
     * @throws ResourceException Thrown if the label cannot be read.
     */
    public ResourceSetLabel read(String realm, String username, String id) throws ResourceException {
        try (Connection connection = getConnection()) {
            SearchResultEntry entry = connection.searchSingleEntry(
                    LDAPRequests.newSingleEntrySearchRequest(getLabelDn(realm, username, id)));
            Set<String> resourceSets = new HashSet<>();
            final Attribute resourceSetAttribute = entry.getAttribute(RESOURCE_SET_ATTR);
            if (resourceSetAttribute != null) {
                for (ByteString resourceSetId : resourceSetAttribute) {
                    resourceSets.add(resourceSetId.toString());
                }
            }
            return getResourceSetLabel(entry, resourceSets);
        } catch (LdapException e) {
            final ResultCode resultCode = e.getResult().getResultCode();
            if (resultCode.equals(ResultCode.NO_SUCH_OBJECT)) {
                throw new NotFoundException();
            }
            throw new InternalServerErrorException("Could not read", e);
        }
    }

    /**
     * Updates the provided {@link ResourceSetLabel} in the database.
     * @param realm The current realm.
     * @param username The user that owns the label.
     * @param label The label instance.
     * @throws ResourceException Thrown if the label cannot be updated.
     */
    public void update(String realm, String username, ResourceSetLabel label) throws ResourceException {
        try (Connection connection = getConnection()) {
            Result result = connection.modify(
                    LDAPRequests.newModifyRequest(getLabelDn(realm, username, label.getId()))
                            .addModification(REPLACE, NAME_ATTR, label.getName())
                            .addModification(REPLACE, RESOURCE_SET_ATTR, label.getResourceSetIds().toArray()));
            if (!result.isSuccess()) {
                throw new InternalServerErrorException("Unknown unsuccessful request");
            }
        } catch (LdapException e) {
            final ResultCode resultCode = e.getResult().getResultCode();
            if (resultCode.equals(ResultCode.NO_SUCH_OBJECT)) {
                throw new NotFoundException();
            }
            throw new InternalServerErrorException("Could not update", e);
        }
    }

    /**
     * Deletes the referenced {@link ResourceSetLabel} from the database.
     * @param realm The current realm.
     * @param username The user that owns the label.
     * @param labelId The id of the label to delete.
     * @throws ResourceException Thrown if the label cannot be updated.
     */
    public void delete(String realm, String username, String labelId) throws ResourceException {
        try (Connection connection = getConnection()) {
            Result result = connection.delete(LDAPRequests.newDeleteRequest(getLabelDn(realm, username, labelId)));
            if (!result.isSuccess()) {
                throw new InternalServerErrorException("Unknown unsuccessful request");
            }
        } catch (LdapException e) {
            throw new InternalServerErrorException(e); // TODO
        }
    }

    /**
     * Obtain a list of all labels used by a user from a particular realm.
     * @param realm The current realm.
     * @param username The user in question.
     * @return A list of resource set label objects.
     * @throws ResourceException If the list cannot be loaded.
     */
    public Set<ResourceSetLabel> list(String realm, String username) throws ResourceException {
        return query(realm, username, equality("objectClass", OBJECT_CLASS), false);
    }

    /**
     * Obtain a list of all labels used by a user from a particular realm on a specific resource set.
     * @param realm The current realm.
     * @param username The user in question.
     * @param resourceSetId  The resource set ID.
     * @return A list of resource set label objects.
     * @throws ResourceException If the list cannot be loaded.
     */
    public Set<ResourceSetLabel> forResourceSet(String realm, String username, String resourceSetId, boolean includeResourceSets)
            throws ResourceException {
        return query(realm, username, and(equality("objectClass", OBJECT_CLASS), equality(RESOURCE_SET_ATTR, resourceSetId)), includeResourceSets);
    }

    /**
     * Determines if the label is present on any resource set.
     *
     * @param realm The current realm.
     * @param username The user in question.
     * @param labelId The ID of the label.
     * @return {@code true} if the label is present on a resource set, {@code false} if it is not.
     * @throws ResourceException If it cannot be determined if the label is in use.
     */
    public boolean isLabelInUse(String realm, String username, String labelId)
            throws ResourceException {
        return !query(realm, username, and(equality("objectClass", OBJECT_CLASS), equality(ID_ATTR, labelId),
                present(RESOURCE_SET_ATTR)), false).isEmpty();
    }

    private Set<ResourceSetLabel> query(String realm, String username, Filter filter, boolean includeResourceSets) throws ResourceException {
        try (Connection connection = getConnection()) {
            Set<ResourceSetLabel> result = new HashSet<>();
            String[] attrs;

            if (includeResourceSets) {
                attrs = new String[]{ID_ATTR, NAME_ATTR, TYPE_ATTR, RESOURCE_SET_ATTR};
            } else {
                attrs = new String[]{ID_ATTR, NAME_ATTR, TYPE_ATTR};
            }
            ConnectionEntryReader searchResult = connection.search(
                    LDAPRequests.newSearchRequest(getUserDn(realm, username), SearchScope.SUBORDINATES, filter, attrs));
            while (searchResult.hasNext()) {
                if (searchResult.isReference()) {
                    debug.warning("Encountered reference {} searching for resource set labels for user {} in realm {}",
                            searchResult.readReference(), username, realm);
                } else {
                    final SearchResultEntry entry = searchResult.readEntry();
                    result.add(getResourceSetLabel(entry, getResourceSetIds(entry)));
                }
            }
            return result;
        } catch (LdapException e) {
            if (e.getResult().getResultCode().equals(ResultCode.NO_SUCH_OBJECT)) {
                return Collections.emptySet();
            }
            throw new InternalServerErrorException("Could not complete search", e);
        } catch (SearchResultReferenceIOException e) {
            throw new InternalServerErrorException("Shouldn't get a reference as these have been handled", e);
        }
    }

    private Set<String> getResourceSetIds(SearchResultEntry searchResult) throws SearchResultReferenceIOException, LdapException {
        final Attribute attribute = searchResult.getAttribute(RESOURCE_SET_ATTR);
        if (attribute != null) {
            final Iterator<ByteString> resourceSets = attribute.iterator();
            Set<String> resourceSetIds = new HashSet<>();
            while (resourceSets.hasNext()) {
                resourceSetIds.add(resourceSets.next().toString());
            }
            return resourceSetIds;
        } else {
            return new HashSet<>();
        }
    }

    private Connection getConnection() throws InternalServerErrorException {
        try {
            return connectionFactory.create();
        } catch (DataLayerException e) {
            throw new InternalServerErrorException("Could not get connection", e);
        }
    }

    private DN getLabelDn(String realm, String username, String id) {
        return ldapConfiguration.getTokenStoreRootSuffix()
                .child("ou", realm)
                .child("ou", username)
                .child(ID_ATTR, id);
    }

    private DN getUserDn(String realm, String username) {
        return ldapConfiguration.getTokenStoreRootSuffix()
                .child("ou", realm)
                .child("ou", username);
    }

    private ResourceSetLabel getResourceSetLabel(SearchResultEntry entry, Set<String> resourceSets) {
        return new ResourceSetLabel(entry.getAttribute(ID_ATTR).firstValueAsString(),
                entry.getAttribute(NAME_ATTR).firstValueAsString(),
                LabelType.valueOf(entry.getAttribute(TYPE_ATTR).firstValueAsString()),
                resourceSets);
    }

}

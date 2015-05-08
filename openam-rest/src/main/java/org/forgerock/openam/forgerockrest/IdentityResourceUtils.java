/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: IdentityServicesImpl.java,v 1.20 2010/01/06 19:11:17 veiming Exp $
 *
 * Portions Copyrighted 2010-2015 ForgeRock AS.
 */

package org.forgerock.openam.forgerockrest;

import javax.inject.Singleton;
import java.net.MalformedURLException;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.AgentConfiguration;
import com.sun.identity.common.configuration.ConfigurationException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdOperation;
import com.sun.identity.idm.IdRepoBundle;
import com.sun.identity.idm.IdRepoDuplicateObjectException;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchOpModifier;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.sm.SMSException;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.errors.ExceptionMappingHandler;
import org.forgerock.openam.errors.IdentityServicesExceptionMappingHandler;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;

/**
 * Provides utility methods for the {@link IdentityResourceV1} and
 * {@link IdentityResourceV2} classes for creating, reading, updating, deleting
 * and searching {@link AMIdentity}s.
 *
 * @since 13.0.0
 */
@Singleton
public final class IdentityResourceUtils {

    private static Debug debug = Debug.getInstance("amIdentityServices");

    private final ExceptionMappingHandler<IdRepoException, ResourceException> idServicesErrorHandler;

    IdentityResourceUtils() {
        idServicesErrorHandler = InjectorHolder.getInstance(IdentityServicesExceptionMappingHandler.class);
    }

    /**
     * Creates a new {@code AMIdentity} in the identity repository with the
     * details specified in {@code identity}.
     *
     * @param identity The identity details.
     * @param admin The admin token.
     * @throws ResourceException If a problem occurs.
     */
    void create(IdentityDetails identity, SSOToken admin) throws ResourceException {
        Reject.ifNull(identity, admin);

        // Obtain identity details & verify
        String idName = identity.getName();
        String idType = identity.getType();
        String realm = identity.getRealm();
        if (StringUtils.isEmpty(idName)) {
            // TODO: add a message to the exception
            throw new BadRequestException("Identity name not provided");
        }
        if (StringUtils.isEmpty(idType)) {
            idType = "user";
        }
        if (realm == null) {
            realm = "/";
        }

        try {
            // Obtain IdRepo to create validate IdType & operations
            IdType objectIdType = getIdType(idType);
            AMIdentityRepository repo = getRepo(admin, realm);

            if (!isOperationSupported(repo, objectIdType, IdOperation.CREATE)) {
                // TODO: add message to exception
                throw new UnsupportedOperationException("Unsupported: Type: " + idType + " Operation: CREATE");
            }

            // Obtain creation attributes
            Map<String, Set<String>> idAttrs = identity.getAttributes();

            // Create the identity, special case of Agents to merge
            // and validate the attributes
            AMIdentity amIdentity;
            if (isTypeAgent(objectIdType)) {
                createAgent(idAttrs, objectIdType, idType, idName, realm, admin);
            } else {
                // Create other identites like User, Group, Role, etc.
                amIdentity = repo.createIdentity(objectIdType, idName, idAttrs);

                // Process roles, groups & memberships
                if (IdType.USER.equals(objectIdType)) {

                    Set<String> roles = identity.getRoleList();
                    if (roles != null && !roles.isEmpty()) {
                        if (!isOperationSupported(repo, IdType.ROLE, IdOperation.EDIT)) {
                            // TODO: localize message
                            throw new UnsupportedOperationException("Unsupported: Type: " + IdType.ROLE
                                    + " Operation: EDIT");
                        }
                        for (String roleName : roles) {
                            AMIdentity role = fetchAMIdentity(repo, IdType.ROLE, roleName, false);
                            if (role != null) {
                                role.addMember(amIdentity);
                                role.store();
                            }
                        }
                    }

                    Set<String> groups = identity.getGroupList();
                    if (groups != null && !groups.isEmpty()) {
                        if (!isOperationSupported(repo, IdType.GROUP, IdOperation.EDIT)) {
                            // TODO: localize message
                            throw new UnsupportedOperationException("Unsupported: Type: " + IdType.GROUP
                                    + " Operation: EDIT");
                        }
                        for (String groupName : groups) {
                            AMIdentity group = fetchAMIdentity(repo, IdType.GROUP, groupName, false);
                            if (group != null) {
                                group.addMember(amIdentity);
                                group.store();
                            }
                        }
                    }
                }

                if (IdType.GROUP.equals(objectIdType) || IdType.ROLE.equals(objectIdType)) {

                    Set<String> members = identity.getMemberList();
                    if (members != null) {
                        if (IdType.GROUP.equals(objectIdType)
                                && !isOperationSupported(repo, IdType.GROUP, IdOperation.EDIT)) {
                            throw new ForbiddenException("Token is not authorized");
                        }
                        if (IdType.ROLE.equals(objectIdType) &&
                                !isOperationSupported(repo, IdType.ROLE, IdOperation.EDIT)) {
                            throw new ForbiddenException("Token is not authorized");
                        }
                        for (String memberName : members) {
                            AMIdentity user = fetchAMIdentity(repo, IdType.USER, memberName, false);
                            if (user != null) {
                                amIdentity.addMember(user);
                            }
                        }
                        amIdentity.store();
                    }
                }
            }
        } catch (IdRepoDuplicateObjectException ex) {
            throw new ConflictException("Resource already exists", ex);
        } catch (IdRepoException e) {
            debug.error("IdentityServicesImpl:create", e);
            if (IdRepoBundle.ACCESS_DENIED.equals(e.getErrorCode())) {
                throw new ForbiddenException(e.getMessage());
            } else {
                throw new NotFoundException(e.getMessage());
            }
        } catch (SSOException | SMSException | ConfigurationException | MalformedURLException
                | UnsupportedOperationException e) {
            debug.error("IdentityServicesImpl:create", e);
            throw new NotFoundException(e.getMessage());
        }
    }

    /**
     * Reads an {@code AMIdentity} from the identity repository with the given
     * {@code name}, populating the requested attributes.
     *
     * @param name The name of the identity.
     * @param attributes The attributes to retrieve.
     * @param admin The admin token.
     * @return The identity details.
     * @throws ResourceException If a problem occurs.
     */
    IdentityDetails read(String name, Map<String, Set<String>> attributes, SSOToken admin) throws ResourceException {
        IdentityDetails rv;
        String realm = null;
        String repoRealm;
        String identityType = null;
        List<String> attrsToGet = null;

        if (attributes != null) {
            for (Map.Entry<String, Set<String>> attribute : attributes.entrySet()) {
                String attrName = attribute.getKey();

                if ("realm".equalsIgnoreCase(attrName)) {
                    Set<String> values = attribute.getValue();

                    if (values != null && !values.isEmpty()) {
                        realm = values.iterator().next();
                    }
                } else if ("objecttype".equalsIgnoreCase(attrName)) {
                    Set<String> values = attribute.getValue();

                    if (values != null && !values.isEmpty()) {
                        identityType = values.iterator().next();
                    }
                } else {
                    if (attrsToGet == null) {
                        attrsToGet = new ArrayList<>();
                    }

                    attrsToGet.add(attrName);
                }
            }
        }

        if (StringUtils.isEmpty(realm)) {
            repoRealm = "/";
        } else {
            repoRealm = realm;
        }

        if ((identityType == null) || (identityType.length() < 1)) {
            identityType = "User";
        }

        try {
            AMIdentity amIdentity = getAMIdentity(admin, identityType, name, repoRealm);

            if (amIdentity == null) {
                debug.error("IdentityServicesImpl:read identity not found");
                throw new NotFoundException(name);
            }

            if (isSpecialUser(amIdentity)) {
                throw new NotFoundException("Cannot retrieve attributes for this user.");
            }

            rv = convertToIdentityDetails(amIdentity, attrsToGet);

            if ((realm != null) && (realm.length()> 0)) {
                // use the realm specified by the request
                rv.setRealm(realm);
            }
        } catch (IdRepoException e) {
            debug.error("IdentityServicesImpl:read", e);
            throw idServicesErrorHandler.handleError(e);
        } catch (SSOException e) {
            debug.error("IdentityServicesImpl:read", e);
            throw new NotFoundException(e.getMessage());
        }

        return rv;
    }

    /**
     * Updates an {@code AMIdentity} in the identity repository with the
     * details specified in {@code identity}.
     *
     * @param identity The updated identity details.
     * @param admin The admin token.
     * @throws ResourceException If a problem occurs.
     */
    void update(IdentityDetails identity, SSOToken admin) throws ResourceException {
        String idName = identity.getName();
        String idType = identity.getType();
        String realm = identity.getRealm();

        if (StringUtils.isEmpty(idName)) {
            // TODO: add a message to the exception
            throw new BadRequestException("");
        }

        if (StringUtils.isEmpty(idType)) {
            idType="user";
        }

        if (realm == null) {
            realm = "";
        }

        try {
            IdType objectIdType = getIdType(idType);
            AMIdentityRepository repo = getRepo(admin, realm);

            if (!isOperationSupported(repo, objectIdType, IdOperation.EDIT)) {
                // TODO: add message to exception
                throw new ForbiddenException("");
            }

            AMIdentity amIdentity = getAMIdentity(admin, repo, idType, idName);

            if (amIdentity == null) {
                String msg = "Object \'" + idName + "\' of type \'" + idType + "\' not found.'";
                throw new NotFoundException(msg);
            }

            if (isSpecialUser(amIdentity)) {
                throw new ForbiddenException("Cannot update attributes for this user.");
            }

            Map<String, Set<String>> attrs = identity.getAttributes();

            if (attrs != null && !attrs.isEmpty()) {
                Map<String, Set<String>> idAttrs = new HashMap<>();
                Set<String> removeAttrs = new HashSet<>();

                for (Map.Entry<String, Set<String>> entry : attrs.entrySet()) {
                    String attrName = entry.getKey();
                    Set<String> attrValues = entry.getValue();

                    if (attrValues != null && !attrValues.isEmpty()) {
                        // attribute to add or modify
                        idAttrs.put(attrName, attrValues);
                    } else {
                        // attribute to remove
                        removeAttrs.add(attrName);
                    }
                }

                boolean storeNeeded = false;
                if (!idAttrs.isEmpty()) {
                    amIdentity.setAttributes(idAttrs);
                    storeNeeded = true;
                }

                if (!removeAttrs.isEmpty()) {
                    amIdentity.removeAttributes(removeAttrs);
                    storeNeeded = true;
                }

                if (storeNeeded) {
                    amIdentity.store();
                }
            }

            if (IdType.USER.equals(objectIdType)) {
                Set<String> roles = identity.getRoleList();

                if (roles != null) {
                    setMemberships(repo, amIdentity, roles, IdType.ROLE);
                }

                Set<String> groups = identity.getGroupList();

                if (groups != null) {
                    setMemberships(repo, amIdentity, groups, IdType.GROUP);
                }
            }

            if (IdType.GROUP.equals(objectIdType) || IdType.ROLE.equals(objectIdType)) {
                Set<String> members = identity.getMemberList();
                if (members != null) {
                    setMembers(repo, amIdentity, members, IdType.USER);
                }
            }
        } catch (IdRepoException ex) {
            debug.error("IdentityServicesImpl:update", ex);
            throw idServicesErrorHandler.handleError(ex);
        } catch (SSOException ex) {
            debug.error("IdentityServicesImpl:update", ex);
            throw new BadRequestException(ex.getMessage());
        }
    }

    /**
     * Deletes an {@code AMIdentity} from the identity repository that match
     * the details specified in {@code identity}.
     *
     * @param identity The identity to delete.
     * @param admin The admin token.
     * @throws ResourceException If a problem occurs.
     */
    void delete(IdentityDetails identity, SSOToken admin) throws ResourceException {
        if (identity == null) {
            throw new BadRequestException("delete failed: identity object not specified.");
        }

        String name = identity.getName();
        String identityType = identity.getType();
        String realm = identity.getRealm();

        if (name == null) {
            throw new NotFoundException("delete failed: null object name.");
        }

        if (realm == null) {
            realm = "/";
        }

        try {
            AMIdentity amIdentity = getAMIdentity(admin, identityType, name, realm);

            if (amIdentity != null) {
                if (isSpecialUser(amIdentity)) {
                    throw new ForbiddenException("Cannot delete user.");
                }

                AMIdentityRepository repo = getRepo(admin, realm);
                IdType idType = amIdentity.getType();

                if (IdType.GROUP.equals(idType) || IdType.ROLE.equals(idType)) {
                    // First remove users from memberships
                    Set<AMIdentity> members = getMembers(amIdentity, IdType.USER);
                    for (AMIdentity member : members) {
                        try {
                            removeMember(repo, amIdentity, member);
                        } catch (IdRepoException ex) {
                            //ignore this, member maybe already removed.
                        }
                    }
                }

                deleteAMIdentity(repo, amIdentity);
            } else {
                String msg = "Object \'" + name + "\' of type \'" + identityType + "\' was not found.";
                throw new NotFoundException(msg);
            }
        } catch (IdRepoException ex) {
            debug.error("IdentityServicesImpl:delete", ex);
            throw idServicesErrorHandler.handleError(ex);
        } catch (SSOException ex) {
            debug.error("IdentityServicesImpl:delete", ex);
            throw new BadRequestException(ex.getMessage());
        }
    }

    /**
     * Searches the identity repository to find all {@code AMIdentity}s that
     * match the provided search criteria.
     *
     * @param filter The search filter.
     * @param attributes The search attributes.
     * @param admin The admin token.
     * @return A {@code List} of identity names.
     * @throws ResourceException If a problem occurs.
     */
    List<String> search(String filter, Map<String, Set<String>> attributes, SSOToken admin) throws ResourceException {
        List<String> rv = new ArrayList<>();

        try {
            String realm = "/";
            String objectType = "User";
            Map<String, Set<String>> searchModifiers = attributes;
            if (searchModifiers != null) {
                realm = attractValues("realm", searchModifiers, "/");
                objectType = attractValues("objecttype", searchModifiers, "User");
            }

            AMIdentityRepository repo = getRepo(admin, realm);
            IdType idType = getIdType(objectType);

            if (idType != null) {
                if (StringUtils.isEmpty(filter)) {
                    filter = "*";
                }

                List<AMIdentity> objList = fetchAMIdentities(idType, filter, false, repo, searchModifiers);
                if (objList != null && !objList.isEmpty()) {
                    List<String> names = getNames(realm, idType, objList);
                    if (!names.isEmpty()) {
                        for (String name : names) {
                            rv.add(name);
                        }
                    }
                }
            } else {
                debug.error("IdentityServicesImpl:search unsupported IdType" + objectType);
                throw new BadRequestException("search unsupported IdType: " + objectType);
            }
        } catch (IdRepoException e) {
            debug.error("IdentityServicesImpl:search", e);
            throw new InternalServerErrorException(e.getMessage());
        } catch (SSOException e) {
            debug.error("IdentityServicesImpl:search", e);
            throw new InternalServerErrorException(e.getMessage());
        }

        return rv;
    }

    private boolean isSpecialUser(AMIdentity identity) {
        Set<AMIdentity> specialUsers = getSpecialUsers(identity.getRealm());
        return specialUsers != null && specialUsers.contains(identity);
    }

    private boolean isTypeAgent(IdType objectIdType) {
        return objectIdType.equals(IdType.AGENT) || objectIdType.equals(IdType.AGENTONLY)
                || objectIdType.equals(IdType.AGENTGROUP);
    }

    /**
     * To be backward compatible, look for 'AgentType' attribute
     * in the attribute map which is passed as a parameter and if
     * not present/sent, check if the IdType.AGENTONLY or AGENT
     * and then assume that it is '2.2_Agent' type to create
     * that agent under the 2.2_Agent node.
     **/
    private void createAgent(Map<String, Set<String>> idAttrs, IdType objectIdType, String idType, String idName,
            String realm, SSOToken adminToken) throws SMSException, SSOException, ConfigurationException,
            IdRepoException, MalformedURLException {

        String agentType;
        String serverUrl = null;
        String agentUrl = null;

        final String AGENT_TYPE = "agenttype";
        final String SERVER_URL = "serverurl";
        final String AGENT_URL = "agenturl";
        final String DEFAULT_AGENT_TYPE = "2.2_Agent";

        Set<String> set = idAttrs.remove(AGENT_TYPE);
        if (set != null && !set.isEmpty()) {
            agentType = set.iterator().next();
        } else if (objectIdType.equals(IdType.AGENTONLY) || objectIdType.equals(IdType.AGENT)) {
            agentType = DEFAULT_AGENT_TYPE;
        } else {
            throw new UnsupportedOperationException("Unsupported: Agent Type required for " + idType);
        }

        set = idAttrs.remove(SERVER_URL);
        if (set != null && !set.isEmpty()) {
            serverUrl = set.iterator().next();
        }

        set = idAttrs.remove(AGENT_URL);
        if (set != null && !set.isEmpty()) {
            agentUrl = set.iterator().next();
        }

        if (agentType.equals(AgentConfiguration.AGENT_TYPE_WEB) || agentType.equals(AgentConfiguration.AGENT_TYPE_J2EE)) {
            if (StringUtils.isBlank(agentUrl)) {
                throw new MalformedURLException("Agent type requires agenturl to be configured.");
            } else if (StringUtils.isBlank(serverUrl)) {
                throw new MalformedURLException("Agent type requires serverurl to be configured.");
            }
        }

        if (objectIdType.equals(IdType.AGENT) || objectIdType.equals(IdType.AGENTONLY)) {
            if (StringUtils.isBlank(serverUrl) || StringUtils.isBlank(agentUrl)) {
                AgentConfiguration.createAgent(adminToken, realm, idName, agentType, idAttrs);
            } else {
                AgentConfiguration.createAgent(adminToken, realm, idName, agentType, idAttrs, serverUrl, agentUrl);
            }
        } else {
            if (StringUtils.isBlank(serverUrl) || StringUtils.isBlank(agentUrl)) {
                AgentConfiguration.createAgentGroup(adminToken, realm, idName, agentType, idAttrs);
            } else {
                AgentConfiguration.createAgentGroup(adminToken, realm, idName, agentType, idAttrs, serverUrl, agentUrl);
            }
        }
    }

    private Set<AMIdentity> getSpecialUsers(String realmName) {
        SSOToken adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
        try {
            AMIdentityRepository repo = new AMIdentityRepository(realmName, adminToken);
            IdSearchResults results = repo.getSpecialIdentities(IdType.USER);
            return results.getSearchResults();
        } catch (IdRepoException | SSOException e) {
            debug.warning("AMModelBase.getSpecialUsers", e);
        }
        return Collections.emptySet();
    }

    private Set<String> getSpecialUserNames(String realmName) {
        Set<String> identities = new HashSet<String>();
        Set<AMIdentity> set = getSpecialUsers(realmName);
        if (set != null) {
            for (AMIdentity amid : set) {
                identities.add(amid.getName());
            }
        }
        return identities;
    }

    private void deleteAMIdentities(AMIdentityRepository repo, IdType type, Set<AMIdentity> identities)
            throws IdRepoException, SSOException, ForbiddenException {
        if (isOperationSupported(repo, type, IdOperation.DELETE)) {
            repo.deleteIdentities(identities);
        } else {
            // TODO: add message to exception
            throw new ForbiddenException("");
        }
    }

    private void deleteAMIdentity(AMIdentityRepository repo, AMIdentity amIdentity) throws IdRepoException,
            SSOException, ForbiddenException {
        Set<AMIdentity> identities = new HashSet<>();
        identities.add(amIdentity);
        deleteAMIdentities(repo, amIdentity.getType(), identities);
    }

    private void setMemberships(AMIdentityRepository repo, AMIdentity amIdentity, Set<String> memberships,
            IdType idType) throws IdRepoException, SSOException, NotFoundException, ForbiddenException {
        Set<String> membershipsToAdd = memberships;
        Set<String> membershipsToRemove = null;
        Set<String> currentMemberships = getMembershipNames(amIdentity, idType);

        if (currentMemberships != null && !currentMemberships.isEmpty()) {
            membershipsToRemove = removeAllIgnoreCase(currentMemberships, memberships);
            membershipsToAdd = removeAllIgnoreCase(memberships, currentMemberships);
        }

        if (membershipsToRemove != null) {
            for (String idName : membershipsToRemove) {
                AMIdentity container = fetchAMIdentity(repo, idType, idName, false);
                removeMember(repo, container, amIdentity);
            }
        }

        if (membershipsToAdd != null) {
            for (String idName : membershipsToAdd) {
                AMIdentity container = fetchAMIdentity(repo, idType, idName, false);
                addMember(repo, container, amIdentity);
            }
        }
    }

    private void addMember(AMIdentityRepository repo, AMIdentity amIdentity, AMIdentity member)
            throws IdRepoException, SSOException, ForbiddenException {
        if (!member.isMember(amIdentity)) {
            if (isOperationSupported(repo, amIdentity.getType(), IdOperation.EDIT)) {
                amIdentity.addMember(member);
            } else {
                // TODO: Add message to exception
                throw new ForbiddenException("");
            }
        }
    }

    private Set<AMIdentity> getMembers(AMIdentity amIdentity, IdType type) throws IdRepoException, SSOException {
        return amIdentity.getMembers(type);
    }

    private Set<String> getMemberNames(AMIdentity amIdentity, IdType type) throws IdRepoException, SSOException {
        Set<String> rv = null;
        Set<AMIdentity> members = getMembers(amIdentity, type);
        if (members != null) {
            rv = new HashSet<>();
            for (AMIdentity member : members) {
                rv.add(member.getName());
            }
        }
        return rv;
    }

    private void removeMember(AMIdentityRepository repo, AMIdentity amIdentity, AMIdentity member)
            throws IdRepoException, SSOException, ForbiddenException {
        if (member.isMember(amIdentity)) {
            IdType type = amIdentity.getType();
            if (isOperationSupported(repo, type, IdOperation.EDIT)) {
                amIdentity.removeMember(member);
            } else {
                // TODO: Add message to exception
                throw new ForbiddenException("");
            }
        }
    }

    private Set<String> removeAllIgnoreCase(Set<String> src, Set<String> removeSet) {
        if (src == null || src.isEmpty()) {
            return new HashSet<>();
        } else {
            Set<String> result = new HashSet<>(src);
            if (removeSet != null && !removeSet.isEmpty()) {
                Map<String, String> upcaseSrc = new HashMap<>(src.size());
                for (String s : src) {
                    upcaseSrc.put(s.toUpperCase(), s);
                }

                for (String s : removeSet) {
                    s = upcaseSrc.get(s.toUpperCase());
                    if (s != null) {
                        result.remove(s);
                    }
                }
            }
            return result;
        }
    }

    private void setMembers(AMIdentityRepository repo, AMIdentity amIdentity, Set<String> members, IdType idType)
            throws IdRepoException, SSOException, NotFoundException, ForbiddenException {
        Set<String> membershipsToAdd = members;
        Set<String> membershipsToRemove = null;
        Set<String> currentMembers = getMemberNames(amIdentity, idType);

        if ((currentMembers != null) && (currentMembers.size() > 0)) {
            membershipsToRemove = removeAllIgnoreCase(currentMembers, members);
            membershipsToAdd = removeAllIgnoreCase(members, currentMembers);
        }

        if (membershipsToRemove != null) {
            for (String memberName : membershipsToRemove) {
                AMIdentity identity = fetchAMIdentity(repo, idType, memberName, false);
                if (identity != null) {
                    removeMember(repo, amIdentity, identity);
                }
            }
        }

        if (membershipsToAdd != null) {
            for (String memberName : membershipsToAdd) {
                AMIdentity identity = fetchAMIdentity(repo, idType, memberName, false);
                if (identity != null) {
                    addMember(repo, amIdentity, identity);
                }
            }
        }
    }

    private Set<AMIdentity> getMemberships(AMIdentity amIdentity, IdType idType) throws SSOException {
        try {
            return amIdentity.getMemberships(idType);
        } catch (IdRepoException ex) {
            // This can be thrown if the identity is not a member
            // in any object of idType.
            return new HashSet<>(0);
        }
    }

    private Set<String> getMembershipNames(AMIdentity amIdentity, IdType idType) throws SSOException {
        Set<AMIdentity> memberships = getMemberships(amIdentity, idType);
        Set<String> rv = new HashSet<>(memberships.size());
        for (AMIdentity membership : memberships) {
            rv.add(membership.getName());
        }
        return rv;
    }

    private List<String> getNames(String realm, IdType idType, List<AMIdentity> objList) throws SSOException,
            IdRepoException {
        List<String> names = new ArrayList<String>();

        if (objList != null && !objList.isEmpty()) {
            for (AMIdentity identity : objList) {
                if (identity != null) {
                    names.add(identity.getName());
                }
            }
        }

        if (idType.equals(IdType.USER)) {
            Set<String> specialUserNames = getSpecialUserNames(realm);
            names.removeAll(specialUserNames);
        }
        return names;
    }

    private boolean isOperationSupported(AMIdentityRepository repo, IdType idType, IdOperation operation) {
        try {
            Set ops = repo.getAllowedIdOperations(idType);
            if (ops != null) {
                return ops.contains(operation);
            }
        } catch (IdRepoException | SSOException ex) {
            // Ignore
        }
        return false;
    }

    private AMIdentity fetchAMIdentity(AMIdentityRepository repo, IdType type, String identity, boolean fetchAllAttrs)
            throws IdRepoException, NotFoundException, SSOException {
        AMIdentity rv = null;
        List<AMIdentity> identities = fetchAMIdentities(type, identity, fetchAllAttrs, repo, null);
        if (identities != null && !identities.isEmpty()) {
            rv = identities.get(0);
        }
        return rv;
    }

    private List<AMIdentity> fetchAMIdentities(IdType type, String identity, boolean fetchAllAttrs,
            AMIdentityRepository repo, Map searchModifiers) throws IdRepoException, NotFoundException, SSOException {
        IdSearchControl searchControl = new IdSearchControl();
        IdSearchResults searchResults;
        List<AMIdentity> identities;

        if (isOperationSupported(repo, type, IdOperation.READ)) {
            Set<AMIdentity> resultSet;

            if (fetchAllAttrs) {
                searchControl.setAllReturnAttributes(true);
            } else {
                searchControl.setAllReturnAttributes(false);
            }

            if (searchModifiers != null) {
                searchControl.setSearchModifiers(IdSearchOpModifier.AND,
                        searchModifiers);
            }

            searchResults = repo.searchIdentities(type, identity, searchControl);
            resultSet = searchResults.getSearchResults();
            identities = new ArrayList<>(resultSet);
        } else {
            // A list is expected back
        	/*
        	 * TODO: throw an exception instead of returning an empty list
        	 */
            identities = new ArrayList<>();
        }

        return identities;
    }

    private String attractValues(String name, Map<String, Set<String>> map, String defaultValue) {
        Set<String> set = map.get(name);
        if (set != null && !set.isEmpty()) {
            String value = set.iterator().next().trim();
            map.remove(name);
            return value.isEmpty() ? defaultValue : value;
        } else {
            return defaultValue;
        }
    }

    private AMIdentityRepository getRepo(SSOToken token, String realm) throws IdRepoException {
        if (StringUtils.isEmpty(realm)) {
            realm = "/";
        }
        return new AMIdentityRepository(realm, token);
    }

    private IdType getIdType(String objectType) {
        try {
            return IdUtils.getType(objectType);
        } catch (IdRepoException ioe) {
            // Ignore exception
        }
        return null;
    }

    private AMIdentity getAMIdentity(SSOToken ssoToken, String objectType, String id, String realm)
            throws IdRepoException, SSOException, NotFoundException {
        AMIdentityRepository repo = getRepo(ssoToken, realm);
        return getAMIdentity(ssoToken, repo, objectType, id);
    }

    private AMIdentity getAMIdentity(SSOToken ssoToken, AMIdentityRepository repo, String objectType, String id)
            throws IdRepoException, SSOException, NotFoundException {
        AMIdentity rv = null;
        IdType idType = null;

        if (objectType != null) {
            idType = getIdType(objectType);
        }

        if (idType != null) {
            // First assume id is a universal id
            rv = getAMIdentity(ssoToken, repo, id, idType);

            if (rv == null) {
                // Not found through id lookup, try name lookup
                rv = fetchAMIdentity(repo, idType, id, true);
            }
        }

        return rv;
    }

    private AMIdentity getAMIdentity(SSOToken ssoToken, AMIdentityRepository repo, String guid, IdType idType)
            throws IdRepoException, SSOException {
        if (isOperationSupported(repo, idType, IdOperation.READ)) {
            try {
                if (DN.isDN(guid)) {
                    return new AMIdentity(ssoToken, guid);
                } else {
                    return new AMIdentity(ssoToken, guid, idType, repo.getRealmIdentity().getRealm(), null);
                }
            } catch (IdRepoException ex) {
                String errCode = ex.getErrorCode();

                // If it is error code 215, ignore the error as this indicates
                // an invalid uid.
                if (!"215".equals(errCode)) {
                   throw ex;
                }
            }
        }
        return null;
    }

    private IdentityDetails convertToIdentityDetails(AMIdentity amIdentity, List<String> attrList) throws IdRepoException,
            SSOException {
        IdentityDetails rv = null;

        if (amIdentity != null) {
            IdType idType = amIdentity.getType();
            Map<String, Set<String>> attrs;
            boolean addUniversalId = false;

            rv = new IdentityDetails();
            rv.setName(amIdentity.getName());
            rv.setType(amIdentity.getType().getName());
            rv.setRealm(amIdentity.getRealm());

            if (IdType.USER.equals(idType)) {
                Set<AMIdentity> roles = amIdentity.getMemberships(IdType.ROLE);

                if (roles != null && !roles.isEmpty()) {
                    AMIdentity[] rolesFound = new AMIdentity[roles.size()];
                    String[] roleNames = new String[rolesFound.length];

                    roles.toArray(rolesFound);

                    for (int i = 0; i < rolesFound.length; i++) {
                        roleNames[i] = rolesFound[i].getName();
                    }

                    rv.setRoleList(new HashSet<>(Arrays.asList(roleNames)));
                }

                Set<AMIdentity> groups = amIdentity.getMemberships(IdType.GROUP);

                if ((groups != null) && (groups.size() > 0)) {
                    AMIdentity[] groupsFound = new AMIdentity[groups.size()];
                    String[] groupNames = new String[groupsFound.length];

                    groups.toArray(groupsFound);

                    for (int i = 0; i < groupsFound.length; i++) {
                        groupNames[i] = groupsFound[i].getName();
                    }

                    rv.setGroupList(new HashSet<>(Arrays.asList(groupNames)));
                }
            }

            if (IdType.GROUP.equals(idType) || IdType.ROLE.equals(idType)) {
                Set<AMIdentity> members = amIdentity.getMembers(IdType.USER);

                if ((members != null) && (members.size() > 0)) {
                    AMIdentity[] membersFound = new AMIdentity[members.size()];
                    String[] memberNames = new String[membersFound.length];

                    members.toArray(membersFound);

                    for (int i = 0; i < membersFound.length; i++) {
                        memberNames[i] = membersFound[i].getName();
                    }

                    rv.setMemberList(new HashSet<>(Arrays.asList(memberNames)));
                }
            }

            if (attrList != null) {
                Set<String> attrNames = new HashSet<>();
                for (String attrName : attrList) {
                    if ("universalid".equalsIgnoreCase(attrName)) {
                        addUniversalId = true;
                    } else {
                        if (!attrNames.contains(attrName)) {
                            attrNames.add(attrName);
                        }
                    }
                }
                attrs = amIdentity.getAttributes(attrNames);
            } else {
                attrs = amIdentity.getAttributes();
                addUniversalId = true;
            }

            if (addUniversalId) {
                if (attrs == null) {
                    attrs = new HashMap<>();
                }

                Set<String> uidValue = new HashSet<>();

                uidValue.add(amIdentity.getUniversalId());
                attrs.put("universalid", uidValue);
            }

            if (attrs != null) {
                rv.setAttributes(attrs);
            }
        }
        return rv;
    }
}

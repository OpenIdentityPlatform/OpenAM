/**
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
 * $Id: EntitiesModel.java,v 1.7 2008/12/05 20:00:50 farble1670 Exp $
 *
 */

package com.sun.identity.console.idm.model;

import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import java.util.Map;
import java.util.Set;

/* - NEED NOT LOG - */

public interface EntitiesModel
    extends AMModel
{
    String TF_NAME = "tfName";
    String TF_TYPE = "tfType";
    String AGENT_ROOT_URL = "agentRootURL=";
    
    /**
     * Agent profile, device key value attribute name.
     */
    String ATTR_NAME_DEVICE_KEY_VALUE = "sunIdentityServerDeviceKeyValue";

    /**
     * Returns entity names.
     *
     * @param realmName Name of Realm.
     * @param strType Entity Type.
     * @param pattern Search Pattern.
     */
    IdSearchResults getEntityNames(
        String realmName,
        String strType,
        String pattern
    ) throws AMConsoleException;

    /**
     * Returns property sheet XML for Entity Profile.
     *
     * @param realmName Name of Realm.
     * @param idType Type of Entity.
     * @param agentType mainly for agent type.
     * @param bCreate <code>true</code> for creation operation.
     * @param viewbeanClassName Class Name of View Bean.
     * @return property sheet XML for Entity Profile.
     * @throws AMConsoleException if XML cannot be obtained.
     */
    String getPropertyXMLString(
        String realmName,
        String idType,
        String agentType,
        boolean bCreate,
        String viewbeanClassName
    ) throws AMConsoleException;

    /**
     * Returns defauls values for an Entity Type.
     *
     * @param idType Type of Entity.
     * @param agentType mainly for agent type.
     * @param bCreate true for Creation page.
     * @throws AMConsoleException if default values cannot be obtained.
     */
    Map getDefaultAttributeValues(
        String idType,
        String agentType,
        boolean bCreate)
        throws AMConsoleException;

    /**
     * Returns attribute values of an entity object.
     *
     * @param universalId Universal ID of the entity.
     * @param bCreate true for creation page.
     * @return attribute values of an entity object.
     * @throws AMConsoleException if object cannot located.
     */
    Map getAttributeValues(String universalId, boolean bCreate)
        throws AMConsoleException;

    /**
     * Creates an entity.
     *
     * @param realmName Name of Realm.
     * @param entityName Name of Entity.
     * @param idType Type of Entity.
     * @param values Map of attribute name to Set of attribute values.
     * @throws AMConsoleException if entity cannot be created.
     */
    void createEntity(
        String realmName,
        String entityName,
        String idType,
        Map values
    ) throws AMConsoleException;

    /**
     * Modifies profile of entity.
     *
     * @param realmName Name of Realm.
     * @param universalId Universal ID of the entity.
     * @param values Map of attribute name to set of attribute values.
     * @throws AMConsoleException if entity cannot be located or modified.
     */
    void modifyEntity(String realmName, String universalId, Map values) 
        throws AMConsoleException;

    /**
     * Deletes entities.
     *
     * @param realmName Name of Realm.
     * @param names Name of Entities to be deleted.
     * @throws AMConsoleException if entity cannot be deleted.
     */
    void deleteEntities(String realmName, Set names) 
        throws AMConsoleException;

    /**
     * Returns true if services can be assigned to this entity type.
     *
     * @param realmName Name of Realm.
     * @param idType Type of Entity.
     * @return true if services can be assigned to this entity type.
     */
    boolean canAssignService(String realmName, String idType);

    /**
     * Returns a set of entity types of which a given type can have member of.
     *
     * @param realmName Name of Realm.
     * @param idType Type of Entity.
     * @return a set of entity types of which a given type can have member of.
     * @throws AMConsoleException if <code>idType</code> is not supported.
     */
    Set getIdTypeMemberOf(String realmName, String idType)
        throws AMConsoleException;

    /**
     * Returns a set of entity types that can be member of a given type.
     *
     * @param realmName Name of Realm.
     * @param idType Type of Entity.
     * @return a set of entity types that can be member of a given type.
     * @throws AMConsoleException if <code>idType</code> is not supported.
     */
    Set getIdTypeBeMemberOf(String realmName, String idType)
        throws AMConsoleException;

    /**
     * Returns true of members can be added to a type.
     *
     * @param realmName Name of Realm.
     * @param idType Type of Entity.
     * @param containerIDType Type of Entity of Container.
     * @return true of members can be added to a type.
     */
    boolean canAddMember(
        String realmName,
        String idType,
        String containerIDType
    ) throws AMConsoleException;

    /**
     * Returns membership of an entity.
     *
     * @param realmName Name of Realm.
     * @param universalId Universal ID of the entity.
     * @param type Type of membership.
     * @return membership of an entity.
     * @throws AMConsoleException if members cannot be returned.
     */
    Set getMembership(String realmName, String universalId, String type) 
        throws AMConsoleException;

    /**
     * Returns members of an entity.
     *
     * @param realmName Name of Realm.
     * @param universalId Universal ID of the entity.
     * @param type Type of membership.
     * @return members of an entity.
     * @throws AMConsoleException if members cannot be returned.
     */
    Set getMembers(String realmName, String universalId, String type) 
        throws AMConsoleException;

    /**
     * Adds an entity to a set of membership.
     *
     * @param universalId Universal ID of the entity.
     * @param membership Set of Universal ID of membership.
     * @throws AMConsoleException if membership addition fails.
     */
    void addMemberships(String universalId, Set membership)
        throws AMConsoleException;

    /**
     * Adds an entities to a membership.
     *
     * @param universalId Universal ID of the membership.
     * @param names Set of Universal ID of entities.
     * @throws AMConsoleException if membership addition fails.
     */
    void addMembers(String universalId, Set names)
        throws AMConsoleException;

    /**
     * Removes an entity from a set of memberships.
     *
     * @param universalId Universal ID of the entity.
     * @param membership Set of Universal ID of membership.
     * @throws AMConsoleException if membership removal fails.
     */
    void removeMemberships(String universalId, Set membership)
        throws AMConsoleException;

    /**
     * Removes a set of entities from a membership.
     *
     * @param universalId Universal ID of the membership.
     * @param names Set of Universal ID of entities.
     * @throws AMConsoleException if membership removal fails.
     */
    void removeMembers(String universalId, Set names)
        throws AMConsoleException;

    /**
     * Returns assigned memberships.
     *
     * @param universalId Universal ID of the entity.
     * @param memberships Set of assignable memberships.
     * @throws AMConsoleException if memberships information cannot be
     * determined.
     */
    Set getAssignedMemberships(String universalId, Set memberships)
        throws AMConsoleException;

    /**
     * Returns assigned members.
     *
     * @param universalId Universal ID of the entity.
     * @param members Set of assignable members.
     * @throws AMConsoleException if members information cannot be
     * determined.
     */
    Set getAssignedMembers(String universalId, Set members)
        throws AMConsoleException;

    /**
     * Returns assigned services. Map of service name to its display name.
     *
     * @param universalId Universal ID of the entity.
     * @return assigned services.
     * @throws AMConsoleException if service information cannot be determined.
     */
    Map getAssignedServiceNames(String universalId)
        throws AMConsoleException;

    /**
     * Returns assignable services. Map of service name to its display name.
     *
     * @param universalId Universal ID of the entity.
     * @return assignable services.
     * @throws AMConsoleException if service information cannot be determined.
     */
    Map getAssignableServiceNames(String universalId)
        throws AMConsoleException;

    /**
     * Returns the XML for property sheet view component.
     *
     * @param realmName Name of Realm.
     * @param serviceName Name of service.
     * @param idType type of Identity.
     * @param bCreate true if the property sheet is for identity creation.
     * @param viewbeanClassName Class Name of View Bean.
     * @return the XML for property sheet view component.
     * @throws AMConsoleException if XML cannot be created.
     */
    String getServicePropertySheetXML(
        String realmName,
        String serviceName,
        IdType idType,
        boolean bCreate,
        String viewbeanClassName
    ) throws AMConsoleException;

    /**
     * Returns defauls values for an Entity Type.
     *
     * @param idType ID Type;
     * @param serviceName Name of service name.
     * @throws AMConsoleException if default values cannot be obtained.
     */
    Map getDefaultValues(String serviceName, String idType)
        throws AMConsoleException;

    /**
     * Assigns service to an entity.
     *
     * @param universalId Universal ID of the entity.
     * @param serviceName Service names.
     * @param values Attribute Values of the service.
     * @throws AMConsoleException if service cannot be assigned.
     */
    void assignService(String universalId, String serviceName, Map values)
        throws AMConsoleException;

    /**
     * Unassigns services from an entity.
     *
     * @param universalId Universal ID of the entity.
     * @param serviceNames Set of service names to be unassigned.
     * @throws AMConsoleException if services cannot be unassigned.
     */
    void unassignServices(String universalId, Set serviceNames)
        throws AMConsoleException;

    /**
     * Returns properties view bean URL for an attribute schema.
     *
     * @param name Name of attribute schema.
     * @return properties view bean URL for an attribute schema.
     */
    String getPropertiesViewBean(String name);

    /**
     * Returns service attribute values of an entity.
     *
     * @param universalId Universal ID of the entity.
     * @param serviceName Name of service name.
     * @return service attribute values of entity.
     * @throws AMConsoleException if values cannot be returned.
     */
    Map getServiceAttributeValues(String universalId, String serviceName)
        throws AMConsoleException;

    /**
     * Set service attribute values to an entity.
     *
     * @param universalId Universal ID of the entity.
     * @param serviceName Name of service name.
     * @param values Attribute values.
     * @throws AMConsoleException if values cannot be set.
     */
    void setServiceAttributeValues(
        String universalId,
        String serviceName,
        Map values
    ) throws AMConsoleException;

    /**
     * Returns true if service has user attribute schema.
     *
     * @param serviceName Name of service.
     * @return true if service has user attribute schema.
     */
    boolean hasUserAttributeSchema(String serviceName);

    /**
     * Returns true if service has displayable user or dynamic attributes.
     *
     * @param serviceName Name of service.
     * @return true if service has displayable attributes.
     */
    boolean hasDisplayableAttributes(String serviceName);

    /**
     * Returns service name of a given ID type.
     *
     * @param idType ID Type.
     * @param agentType Agent Type.
     * @return service name of a given ID type.
     */
    String getServiceNameForIdType(String idType, String agentType);

    /**
     * Set end user flag.
     *
     * @param endUser end user flag.
     */
    void setEndUser(boolean endUser);

    /**
     * Returns the type of <code>entity</code> object for which the model
     * was constructed. 
     *
     * @return type of <code>entity</code> object being used.
     */
    String getEntityType();

    /**
     * Returns all the authentication chains in a realm.
     *
     * @param realm Name of realm.
     * @return all the authentication chains in a realm.
     * @throws AMConsoleException if authentication chains cannot be returned.
     */
    Set getAuthenticationChains(String realm)
        throws AMConsoleException;

    /**
     * Returns <code>true</code> if services is supported for the identity.
     * 
     * @return <code>true</code> if services is supported for the identity.
     */
    boolean isServicesSupported();

    public boolean repoExists(String realmName);

}

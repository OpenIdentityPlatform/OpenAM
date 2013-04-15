/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: IdRepo.java,v 1.8 2009/07/02 20:33:30 hengming Exp $
 *
 */

/**
 * Portions Copyrighted 2013 ForgeRock, Inc.
 */
package com.sun.identity.idm;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.callback.Callback;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SchemaType;

/**
 * 
 * This interface defines the methods which need to be implemented by plugins.
 * Two plugins are supported, <code> ldap </code> and <code> remote </code>.
 *
 * @supported.all.api
 */
public abstract class IdRepo {

    /**
     * The constants used to define membership operations.
     */
    public static final int ADDMEMBER = 1;

    public static final int REMOVEMEMBER = 2;

    public Map configMap = Collections.EMPTY_MAP;

    public static final int NO_MOD = -1;

    public static final int OR_MOD = 0;

    public static final int AND_MOD = 1;

    /**
     * Initialization paramters as configred for a given plugin.
     * 
     * @param configParams
     * @throws IdRepoException 
     */
    public void initialize(Map configParams) throws IdRepoException {
        configMap = Collections.unmodifiableMap(configParams);
    }

    /**
     * This method is invoked just before the plugin is removed from the IdRepo
     * cache of plugins. This helps the plugin clean up after itself
     * (connections, persistent searches etc.). This method should be overridden
     * by plugins that need to do this.
     * 
     */
    public void shutdown() {
        // do nothing
    }

    /**
     * Return supported operations for a given IdType
     * 
     * @param type
     *     Identity type
     * @return set of IdOperation supported for this IdType.
     */
    public Set getSupportedOperations(IdType type) {
        Set set = new HashSet();
        set.add(IdOperation.READ);
        return set;
    }

    /**
     * @return Returns a Set of IdTypes supported by this plugin.
     * Returns the supported types of identities for this
     * plugin. If a plugin does not override this method, it
     * returns an empty set.
     *
     * @return a Set of IdTypes supported by this plugin.
     */
    public Set getSupportedTypes() {
        return Collections.EMPTY_SET;
    }

    /**
     * Returns true if the <code> name </code> object exists in the data store.
    *
     * @param token
     *     Single sign on token of identity performing the task.
     * @param type
     *     Identity type of this object.
     * @param name
     *     Name of the object  of interest.
     * @return
     *     <code>true</code> if name object is in data store
     *     else <code>false</code>
     * @throws IdRepoException If there are repository related error conditions.
     * @throws SSOException If identity's single sign on token is invalid.
     */
    public abstract boolean isExists(SSOToken token, IdType type, String name)
            throws IdRepoException, SSOException;

    /**
     * Returns true if the <code> name </code> object is active.
     *
     * @return
     *     <code>true</code> if name object is in active
     *     else <code>false</code>
     * @param token
     *     Single sign on token of identity performing the task.
     * @param type
     *     Identity type of this object.
     * @param name
     *     Name of the object of interest.
     * @throws IdRepoException If there are repository related error conditions.
     * @throws SSOException If identity's single sign on token is invalid.
     */
    public boolean isActive(SSOToken token, IdType type, String name)
            throws IdRepoException, SSOException {
        return false;
    }

    /**
     * Sets the object's status to <code>active</code>.
     *
     * @param token
     *     Single sign on token of identity performing the task.
     * @param type
     *     Identity type of this object.
     * @param name
     *     Name of the object of interest.
     * @param active
     *     true if setting to active; false otherwise.
     * @throws IdRepoException If there are repository related error conditions.
     * @throws SSOException If identity's single sign on token is invalid.
     */
    public abstract void setActiveStatus(SSOToken token, IdType type,
        String name,  boolean active)
        throws IdRepoException, SSOException;

    /**
     * Returns all attributes and values of name object
     *
     * @param token
     *     Single sign on token of identity performing the task.
     * @param type
     *     Identity type of this object.
     * @param name
     *     Name of the object of interest.
     * @return
     *     Map of attribute-values
     * @throws IdRepoException If there are repository related error conditions.
     * @throws SSOException If identity's single sign on token is invalid.
     */
    public abstract Map getAttributes(SSOToken token, IdType type, String name)
            throws IdRepoException, SSOException;

    /**
     * Returns requested attributes and values of name object.
     *
     * @param token
     *     Single sign on token of identity performing the task.
     * @param type
     *     Identity type of this object.
     * @param name
     *     Name of the object of interest.
     * @param attrNames
     *     Set of attribute names to be read
     * @return
     *     Map of attribute-values
     * @throws IdRepoException If there are repository related error conditions.
     * @throws SSOException If identity's single sign on token is invalid.
     */
    public abstract Map getAttributes(SSOToken token, IdType type, String name,
            Set attrNames) throws IdRepoException, SSOException;

    /**
     * Returns requested binary attributes as an array of bytes.
     *
     * @param token
     *     Single sign on token of identity performing the task.
     * @param type
     *     Identity type of this object.
     * @param name
     *     Name of the object of interest.
     * @param attrNames
     *     Set of attribute names to be read
     * @return
     *     Map of attribute-values
     * @throws IdRepoException If there are repository related error conditions.
     * @throws SSOException If identity's single sign on token is invalid.
     */
    public abstract Map getBinaryAttributes(SSOToken token, IdType type,
            String name, Set attrNames) throws IdRepoException, SSOException;

    /**
     * Creates an identity.
     *
     * @param token
     *     Single sign on token of identity performing the task.
     * @param type
     *     Identity type of this object.
     * @param name
     *     Name of the object of interest.
     * @param attrMap
     *     Map of attribute-values assoicated with this object.
     * @throws IdRepoException If there are repository related error conditions.
     * @throws SSOException If identity's single sign on token is invalid.
     */
    public abstract String create(SSOToken token, IdType type, String name,
            Map attrMap) throws IdRepoException, SSOException;

    /**
     * Deletes an identity.
     *
     * @param token
     *     Single sign on token of identity performing the task.
     * @param type
     *     Identity type of this object.
     * @param name
     *     Name of the object of interest.
     * @throws IdRepoException If there are repository related error conditions.
     * @throws SSOException If identity's single sign on token is invalid.
     */
    public abstract void delete(SSOToken token, IdType type, String name)
            throws IdRepoException, SSOException;

    /**
     * Set the values of attributes of the identity.
     *
     * @param token
     *     Single sign on token of identity performing the task.
     * @param type
     *     Identity type of this object.
     * @param name
     *     Name of the object of interest.
     * @param attributes
     *     Map of attribute-values to set or add.
     * @param isAdd
     *     if <code>true</code> add the attribute-values; otherwise
     *     replaces the attribute-values.
     * @throws IdRepoException If there are repository related error conditions.
     * @throws SSOException If identity's single sign on token is invalid.
     */
    public abstract void setAttributes(SSOToken token, IdType type,
            String name, Map attributes, boolean isAdd) throws IdRepoException,
            SSOException;

    /**
     *
     * Set the values of binary attributes the identity.
     *
     * @param token
     *     Single sign on token of identity performing the task.
     * @param type
     *     Identity type of this object.
     * @param name
     *     Name of the object of interest.
     * @param attributes
     *     Map of binary attribute-values to set or add.
     * @param isAdd
     *     if <code>true</code> add the attribute-values; otherwise
     *     replaces the attribute-values.
     * @throws IdRepoException If there are repository related error conditions.
     * @throws SSOException If identity's single sign on token is invalid.
     */
    public abstract void setBinaryAttributes(SSOToken token, IdType type,
            String name, Map attributes, boolean isAdd) throws IdRepoException,
            SSOException;

    /**
     *
     * Changes password of identity.
     *
     * @param token Single sign on token of identity performing the task.
     * @param type identity type of this object.
     * @param name name of the object of interest.
     * @param attrName password attribute name
     * @param oldPassword old password
     * @param newPassword new password
     * @throws IdRepoException If there are repository related error conditions.
     * @throws SSOException If identity's single sign on token is invalid.
     */
    public void changePassword(SSOToken token, IdType type,
            String name, String attrName, String oldPassword,
            String newPassword) throws IdRepoException, SSOException {

            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "228", args);
    }

    /**
     * Removes the attributes from the identity.
     *
     * @param token
     *     Single sign on token of identity performing the task.
     * @param type
     *     Identity type of this object.
     * @param name
     *     Name of the object of interest.
     * @param attrNames
     *     Set of attribute names to remove.
     * @throws IdRepoException If there are repository related error conditions.
     * @throws SSOException If identity's single sign on token is invalid.
     */
    public abstract void removeAttributes(SSOToken token, IdType type,
            String name, Set attrNames) throws IdRepoException, SSOException;

    /**
     * Search for specific type of identities.
     *
     * @param token
     *     Single sign on token of identity performing the task.
     * @param type
     *     Identity type of this object.
     * @param pattern
     *     pattern to search for.
     * @param maxTime
     *     maximum wait time for search.
     * @param maxResults
     *     maximum records to return.
     * @param returnAttrs
     *     Set of attribute names to return.
     * @param returnAllAttrs
     *     return all attributes
     * @param filterOp
     *     filter condition.
     * @param avPairs
     *     additional search conditions.
     * @return RepoSearchResults
     * @throws IdRepoException If there are repository related error conditions.
     * @throws SSOException If identity's single sign on token is invalid.
     */
    public abstract RepoSearchResults search(SSOToken token, IdType type,
            String pattern, int maxTime, int maxResults, Set returnAttrs,
            boolean returnAllAttrs, int filterOp, Map avPairs, 
            boolean recursive) throws IdRepoException, SSOException;

    /**
     * Modify membership of the identity. Set of members is
     * a set of unique identifiers of other identities.
     *
     * @param token
     *     Single sign on token of identity performing the task.
     * @param type
     *     Identity type of this object.
     * @param name
     *     Name of the object of interest.
     * @param members
     *     Set of names to be added as members of name
     * @param membersType
     *     IdType of members.
     * @param operation
     *     operations to perform on members ADDMEMBER or REMOVEMEMBER.
     * @throws IdRepoException If there are repository related error conditions.
     * @throws SSOException If identity's single sign on token is invalid.
     */
    public abstract void modifyMemberShip(SSOToken token, IdType type,
            String name, Set members, IdType membersType, int operation)
            throws IdRepoException, SSOException;

    /**
     * Returns the memberships of an identity. For example, returns the groups or roles that a user belongs to. The
     * list retrieved here for a user MUST be consistent with member queries against the corresponding groups.
     *
     * @param token
     *     Single sign on token of identity performing the task.
     * @param type
     *     Identity type of this object.
     * @param name
     *     Name of the object of interest.
     * @param membersType
     *     IdType of members of name object.
     * @return
     *     Set of of members belongs to <code>name</code>
     * @throws IdRepoException If there are repository related error conditions.
     * @throws SSOException If identity's single sign on token is invalid.
     */
    public abstract Set getMembers(SSOToken token, IdType type, String name,
            IdType membersType) throws IdRepoException, SSOException;

    /**
     * Returns the memberships of an identity. For example, returns the
     * groups or roles that a user belongs to.
     *
     * @param token
     *     Single sign on token of identity performing the task.
     * @param type
     *     Identity type of this object.
     * @param name
     *     Name of the object of interest.
     * @param membershipType
     *     IdType of memberships to return.
     * @return
     *     Set of objects that <code>name</code> is a member of.
     * @throws IdRepoException If there are repository related error conditions.
     * @throws SSOException If identity's single sign on token is invalid.
     */
    public abstract Set getMemberships(SSOToken token, IdType type,
            String name, IdType membershipType) throws IdRepoException,
            SSOException;

    /**
     * This method is used to assign a service to the given identity.
     * The behavior of this method will be different, depending on
     * how each plugin will implement the services model. The map
     * of attribute-values has already been validated and default
     * values have already been inherited by the framework.
     * The plugin has to verify if the service is assigned (in which
     * case it should throw an exception), and assign the service
     * and the attributes to the identity (if supported).
     *
     *
     * @param token
     *     Single sign on token of identity performing the task.
     * @param type
     *     Identity type of this object.
     * @param name
     *     Name of the object of interest.
     * @param serviceName
     *     service to assign
     * @param stype
     * @param attrMap
     *     Map of attribute-values.
     * @throws IdRepoException If there are repository related error conditions.
     * @throws SSOException If identity's single sign on token is invalid.
     */
    public abstract void assignService(SSOToken token, IdType type,
            String name, String serviceName, SchemaType stype, Map attrMap)
            throws IdRepoException, SSOException;

    /**
     * Returns the set of services assigned to this identity.
     * The framework has to check if the values are objectclasses,
     * then map it to service names. Or if they are servicenames, then
     * there is no mapping needed.
     *
     * @param token
     *     Single sign on token of identity performing the task.
     * @param type
     *     Identity type of this object.
     * @param name
     *     Name of the object of interest.
     * @param mapOfServicesAndOCs
     * @return
     *     Set of name of services assigned to <code>name</code>
     * @throws IdRepoException If there are repository related error conditions.
     * @throws SSOException If identity's single sign on token is invalid.
     */
    public abstract Set getAssignedServices(SSOToken token, IdType type,
            String name, Map mapOfServicesAndOCs) throws IdRepoException,
            SSOException;

    /**
     * If the service is already assigned to the identity then
     * this method unassigns the service and removes the related
     * attributes from the entry.
     *
     * @param token
     *     Single sign on token of identity performing the task.
     * @param type
     *     Identity type of this object.
     * @param name
     *     Name of the object of interest.
     * @param serviceName
     *     Service name to remove.
     * @param attrMap
     *     Map of attribute-values to remove
     * @throws IdRepoException If there are repository related error conditions.
     * @throws SSOException If identity's single sign on token is invalid.
     */
    public abstract void unassignService(SSOToken token, IdType type,
            String name, String serviceName, Map attrMap)
            throws IdRepoException, SSOException;

    /**
     * Returns the attribute values of the service attributes.
     *
     * @param token
     *     Single sign on token of identity performing the task.
     * @param type
     *     Identity type of this object.
     * @param name
     *     Name of the object of interest.
     * @param serviceName
     *     Name of service.
     * @param attrNames
     *     Set of attribute names.
     * @return
     *     Map of attribute-values.
     * @throws IdRepoException If there are repository related error conditions.
     * @throws SSOException If identity's single sign on token is invalid.
     */
    public abstract Map getServiceAttributes(SSOToken token, IdType type,
            String name, String serviceName, Set attrNames)
            throws IdRepoException, SSOException;

    /**
     * Returns the requested binary attribute values of the service attributes
     * as an array of bytes.
     *
     * @param token
     *     Single sign on token of identity performing the task.
     * @param type
     *     Identity type of this object.
     * @param name
     *     Name of the object of interest.
     * @param serviceName
     *     Name of service.
     * @param attrNames
     *     Set of attribute names.
     * @return
     *     Map of attribute-values.
     * @throws IdRepoException If there are repository related error conditions.
     * @throws SSOException If identity's single sign on token is invalid.
     */
    public abstract Map getBinaryServiceAttributes(SSOToken token, IdType type,
            String name, String serviceName, Set attrNames)
            throws   IdRepoException, SSOException;

    /**
     * Modifies the attribute values of the service attributes.
     *
     * @param token
     *     Single sign on token of identity performing the task.
     * @param type
     *     Identity type of this object.
     * @param name
     *     Name of the object of interest.
     * @param serviceName
     *     Name of service.
     * @param sType
     * @param attrMap
     *     map of attribute-values.
     * @throws IdRepoException If there are repository related error conditions.
     * @throws SSOException If identity's single sign on token is invalid.
     */
    public abstract void modifyService(SSOToken token, IdType type,
            String name, String serviceName, SchemaType sType, Map attrMap)
            throws IdRepoException, SSOException;

    /**
     * Adds a listener for changes in the repository
     *
     * @param token
     *     Single sign on token of identity performing the task.
     * @param listener
     * @return status code
     * @throws IdRepoException If there are repository related error conditions.
     * @throws SSOException If identity's single sign on token is invalid.
     */
    public abstract int addListener(SSOToken token, IdRepoListener listener)
            throws IdRepoException, SSOException;

    /**
     * Removes the listener added using <code> addListener </code> method. This
     * is called by the IdRepo framework when the plugin is being shutdown due
     * to configuration change, so that a new instance can be created with the
     * new configuration map.
     * 
     */
    public abstract void removeListener();

    /**
     * Return the configuration map
     * 
     * @return configuration map
     */
    public Map getConfiguration() {
        return configMap;
    }

    /**
     * Returns the fully qualified name for the identity. It is expected that
     * the fully qualified name would be unique, hence it is recommended to
     * prefix the name with the data store name or protocol. Used by IdRepo
     * framework to check for equality of two identities
     * 
     * @param token
     *            administrator SSOToken that can be used by the datastore to
     *            determine the fully qualified name
     * @param type
     *            type of the identity
     * @param name
     *            name of the identity
     * 
     * @return fully qualified name for the identity within the data store
     * @throws IdRepoException If there are repository related error conditions.
     * @throws SSOException If identity's single sign on token is invalid.
     */
    public String getFullyQualifiedName(SSOToken token, IdType type, 
            String name) throws IdRepoException, SSOException {
        return ("default://" + type.toString() + "/" + name);
    }

    /**
     * Returns <code>true</code> if the data store supports authentication of
     * identities. Used by IdRepo framework to authenticate identities.
     * 
     * @return <code>true</code> if data store supports authentication of of
     *         identities; else <code>false</code>
     */
    public boolean supportsAuthentication() {
        return (false);
    }

    /**
     * Returns <code>true</code> if the data store successfully authenticates
     * the identity with the provided credentials. In case the data store
     * requires additional credentials, the list would be returned via the
     * <code>IdRepoException</code> exception.
     * 
     * @param credentials
     *            Array of callback objects containing information such as
     *            username and password.
     * 
     * @return <code>true</code> if data store authenticates the identity;
     *         else <code>false</code>
     */
    public boolean authenticate(Callback[] credentials) throws IdRepoException,
            com.sun.identity.authentication.spi.AuthLoginException {
        return (false);
    }
}

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
 * $Id: AMObject.java,v 1.4 2008/06/25 05:41:20 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;


/**
 * The <code>AMObject</code> interface provides methods to manage various Sun
 * Java System Access Manager objects and their attributes.
 *
 * @deprecated As of Sun Java System Access Manager 7.1.
 * @supported.all.api
 */
public interface AMObject extends AMConstants {

    // Object Types
    /**
     * Represents a User object type
     */
    public static final int USER = 1;

    /**
     * Represents an Organization object type
     */
    public static final int ORGANIZATION = 2;

    /**
     * Represents a Organizational Unit object type
     */
    public static final int ORGANIZATIONAL_UNIT = 3;

    /**
     * Represents a group container object type
     */
    public static final int GROUP_CONTAINER = 4;

    /**
     * Represents a People Container object type
     */
    public static final int PEOPLE_CONTAINER = 5;

    /**
     * Represents a Role object type
     */
    public static final int ROLE = 6;

    /**
     * Represents a Managed Role object type
     */
    public static final int MANAGED_ROLE = 7;

    /**
     * Represents a Filtered Role object type
     */
    public static final int FILTERED_ROLE = 8;

    /**
     * Represents a Group object type
     */
    public static final int GROUP = 9;

    /**
     * Represents a Static Group object type
     */
    public static final int STATIC_GROUP = 10;

    /**
     * Represents a Dynamic Group object type
     */
    public static final int DYNAMIC_GROUP = 11;

    /**
     * Represents a Dynamic Group object type
     */
    public static final int ASSIGNABLE_DYNAMIC_GROUP = 12;

    /**
     * Represents a Template object type
     */
    public static final int TEMPLATE = 13;

    /**
     * Represents Policy Information
     */
    public static final int POLICY = 14;

    /**
     * Represents Service Information
     */
    public static final int SERVICE = 15;

    /**
     * Represents role profile Information
     */
    public static final int ROLE_PROFILE = 16;

    /**
     * Represents group profile Information
     */
    public static final int GROUP_PROFILE = 17;

    /**
     * Represents resource object type
     */
    public static final int RESOURCE = 21;

    /**
     * Represents the status of an object as active
     */
    public static final int ACTIVE = 18;

    /**
     * String representation of the "active" state
     */
    public static final String ACTIVE_VALUE = "active";

    /**
     * Represents the status of an object as inactive
     */
    public static final int INACTIVE = 19;

    /**
     * String representation of the "inactive" state
     */
    public static final String INACTIVE_VALUE = "inactive";

    /**
     * Represents the status of an object as "deleted"
     */
    public static final int DELETED = 20;

    /**
     * String representation of the "deleted" state
     */
    public static final String DELETED_VALUE = "deleted";

    /**
     * Represents an object type that cannot be identified
     */
    public static final int UNKNOWN_OBJECT_TYPE = -1;

    /**
     * Used to indicate the object type has not yet determined. Mainly used in
     * caching. Local to this package
     */
    static final int UNDETERMINED_OBJECT_TYPE = -9999;

    /**
     * Returns the DN of the entry.
     * 
     * @return String DN
     */
    public String getDN();

    /**
     * Returns the parent DN of the entry.
     * 
     * @return String DN
     */
    public String getParentDN();

    /**
     * Stores the change to directory server. This method should be called after
     * doing <code>setAttributes</code> so that the changes that are made can
     * be permanently committed to the LDAP data store.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void store() throws AMException, SSOException;

    /**
     * Stores the change to directory server. This method should be called after
     * doing <code>setAttributes</code> or any other <code>set methods </code>
     * provided. so that the changes that are made can be permanently committed
     * to the LDAP data store.
     * 
     * @param addValues
     *            If <code>addValues</code> is true, then the attribute values
     *            as set in the <code>setAttributes</code> method are added to
     *            any existing values for the same attribute in the directory.
     *            Otherwise, the attribute values replace existing values in the
     *            data store.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void store(boolean addValues) throws AMException, SSOException;

    /**
     * Checks if the entry exists in the directory or not. First a syntax check
     * is done on the DN string corresponding to the entry. If the DN syntax is
     * valid, a directory call will be made to check for the existence of the
     * entry.
     * <p>
     * 
     * <B>NOTE:</B> This method internally invokes a call to the directory to
     * verify the existence of the entry. There could be a performance overhead.
     * Hence, please use your discretion while using this method.
     * 
     * @return false if the entry does not have a valid DN syntax or if the
     *         entry does not exists in the Directory. False otherwise.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public boolean isExists() throws SSOException;

    /**
     * Returns Map of all attributes. Map key is the attribute name and value is
     * the attribute value.
     * 
     * @return Map of all attributes.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Map getAttributes() throws AMException, SSOException;

    /**
     * Returns Map of all attributes directly from data store. Map key is the
     * attribute name and value is the attribute value.
     * 
     * @return Map of all attributes.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Map getAttributesFromDataStore() throws AMException, SSOException;

    /**
     * Returns Map of all attributes. Map key is the attribute name and value is
     * the attribute value in byte[][] format.
     * 
     * @return Map of all attributes.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Map getAttributesByteArray() throws AMException, SSOException;

    /**
     * Returns Map of specified attributes. Map key is the attribute name and
     * value is the attribute value.
     * 
     * @param attributeNames
     *            The Set of attribute names.
     * @return Map of specified attributes.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Map getAttributes(Set attributeNames) throws AMException,
            SSOException;

    /**
     * Returns Map of specified attributes directly from data store. Map key is
     * the attribute name and value is the attribute value.
     * 
     * @param attributeNames
     *            The Set of attribute names.
     * @return Map of specified attributes.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Map getAttributesFromDataStore(Set attributeNames)
            throws AMException, SSOException;

    /**
     * Returns Map of specified attributes. Map key is the attribute name and
     * value is the attribute value in byte[][] format.
     * 
     * @param attributeNames
     *            The Set of attribute names.
     * @return Map of specified attributes. The key of the map is the attribute
     *         name and the values in byte[][] format.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Map getAttributesByteArray(Set attributeNames) throws AMException,
            SSOException;

    /**
     * Returns attribute value.
     * 
     * @param attributeName
     *            Attribute name
     * @return Set of attribute values.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set getAttribute(String attributeName) throws AMException,
            SSOException;

    /**
     * Returns attribute value.
     * 
     * @param attributeName
     *            Attribute name
     * @return attribute values in byte[][] format or null if the attribute does
     *         not exist.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public byte[][] getAttributeByteArray(String attributeName)
            throws AMException, SSOException;

    /**
     * Returns string type attribute value.
     * 
     * @param attributeName
     *            Attribute name
     * @return String value of attribute
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public String getStringAttribute(String attributeName) throws AMException,
            SSOException;

    /**
     * Returns Map of all attributes of specified service. Map key is the
     * attribute name and value is the attribute value.
     * 
     * @param serviceName
     *            Service name
     * @return Map of all attributes of specified service, an empty Map will be
     *         returned if no service attribute is defined in the specified
     *         service.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     * @deprecated This method has been deprecated. Please use service template
     *             objects to obtain service attributes.
     * @see AMOrganization#getTemplate
     */
    public Map getServiceAttributes(String serviceName) throws AMException,
            SSOException;

    /**
     * Creates a Template with no priority for the given service associated with
     * this <code>AMObject</code>.
     * 
     * @param templateType
     *           the template type. Can be one of the following:
     *           <ul>
     *           <li>
     *           {@link AMTemplate#DYNAMIC_TEMPLATE AMTemplate.DYNAMIC_TEMPLATE}
     *           <li> {@link AMTemplate#ORGANIZATION_TEMPLATE  
     *           AMTemplate.ORGANIZATION_TEMPLATE}
     *           </ul>
     * 
     * @param serviceName
     *            service name
     * @param attributes
     *            Map of attributes name-value pairs. if it is null default
     *            values will be used.
     * @return <code>AMTemplate</code> the service template for this
     *         <code>AMObject</code>.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     * @throws UnsupportedOperationException
     *             if the class implementing this interface does not support
     *             this method.
     */
    public AMTemplate createTemplate(int templateType, String serviceName,
            Map attributes) throws UnsupportedOperationException, AMException,
            SSOException;

    /**
     * Creates a Template with a priority for the given service associated with
     * this <code>AMObject</code>.
     * 
     * @param templateType
     *           the template type. Can be one of the following:
     *           <ul>
     *           <li>
     *           {@link AMTemplate#DYNAMIC_TEMPLATE AMTemplate.DYNAMIC_TEMPLATE}
     *           <li> {@link AMTemplate#ORGANIZATION_TEMPLATE  
     *           AMTemplate.ORGANIZATION_TEMPLATE}
     *           </ul>
     * @param serviceName
     *            service name.
     * @param attributes
     *            Map of attributes name-value pairs
     * @param priority
     *            template priority (0 is the highest priority)
     * @return <code>AMTemplate</code> the service template for this
     *         <code>AMObject</code>.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     * @throws UnsupportedOperationException
     *             if the class implementing this interface does not support
     *             this method
     */
    public AMTemplate createTemplate(int templateType, String serviceName,
            Map attributes, int priority) throws UnsupportedOperationException,
            AMException, SSOException;

    /**
     * Returns the Template for the given service associated with this
     * <code>AMObject</code>.
     * 
     * @param serviceName
     *            service name.
     * @param templateType
     *           the template type. Can be one of the following:
     *           <ul>
     *           <li>
     *           {@link AMTemplate#DYNAMIC_TEMPLATE AMTemplate.DYNAMIC_TEMPLATE}
     *           <li> {@link AMTemplate#ORGANIZATION_TEMPLATE  
     *           AMTemplate.ORGANIZATION_TEMPLATE}
     *           </ul>
     * @return <code>AMTemplate</code> the service template for this
     *         <code>AMObject</code>.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     * @throws UnsupportedOperationException
     *             if the class implementing this interface does not support
     *             this method
     */
    public AMTemplate getTemplate(String serviceName, int templateType)
            throws UnsupportedOperationException, AMException, SSOException;

    /**
     * Assigns the given policies to this object.
     * 
     * @param serviceName
     *            service name.
     * @param policyDNs
     *            Set of policy DN string
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     * @deprecated This method has been deprecated. Please use:
     *             <code>com.sun.identity.policy</code> package for creating
     *             and managing policies.
     */
    public void assignPolicies(String serviceName, Set policyDNs)
            throws AMException, SSOException;

    /**
     * Unassigns the given policies from this object.
     * 
     * @param serviceName
     *            service name.
     * @param policyDNs
     *            Set of policy DN string
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     * @deprecated This method has been deprecated. Please use:
     *             <code>com.sun.identity.policy</code> package for creating
     *             and managing policies.
     */
    public void unassignPolicies(String serviceName, Set policyDNs)
            throws AMException, SSOException;

    /**
     * Sets byte attribute values in this <code>AMObject</code>. Note that
     * this method sets or replaces the attribute value with the new value
     * supplied. Also, the attributes changed by this method are not committed
     * to the LDAP data store unless the method {@link AMObject#store store()}
     * is called explicitly.
     * 
     * @param attrName
     *            the attribute name
     * @param byteValues
     *            attribute values in byte[][] format
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void setAttributeByteArray(String attrName, byte[][] byteValues)
            throws AMException, SSOException;

    /**
     * Sets byte attribute values in this <code>AMObject</code>. Note that
     * this method sets or replaces the attribute value with the new value
     * supplied. Also, the attributes changed by this method are not committed
     * to the LDAP data store unless the method {@link AMObject#store store()}
     * is called explicitly.
     * 
     * @param attributes
     *            Map where key is the attribute name and values are in byte[][]
     *            format.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */

    public void setAttributesByteArray(Map attributes) throws AMException,
            SSOException;

    /**
     * Sets attribute values in this <code>AMObject</code>. Note that this
     * method sets or replaces the attribute value with the new value supplied.
     * Also, the attributes changed by this method are not committed to the LDAP
     * data store unless the method {@link AMObject#store store()} is called
     * explicitly.
     * 
     * @param attributes
     *            Map where key is the attribute name and value is a Set of
     *            attribute values. Each of the attribute value must be a string
     *            value.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void setAttributes(Map attributes) throws AMException, SSOException;

    /**
     * Removes attributes in this <code>AMObject</code>. The attributes are
     * removed from the LDAP data store
     * 
     * @param attributes
     *            The Set of attribute names
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void removeAttributes(Set attributes) throws AMException,
            SSOException;

    /**
     * Sets string type attribute value.
     * 
     * @param attributeName
     *            attribute name
     * @param value
     *            value to be set for the attribute names.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void setStringAttribute(String attributeName, String value)
            throws AMException, SSOException;

    /**
     * Deletes the object.
     * 
     * @see #delete(boolean)
     * @see #purge(boolean, int)
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void delete() throws AMException, SSOException;

    /**
     * Deletes object(s). This method takes a boolean parameter, if its value is
     * true, will remove the object and any objects under it, otherwise, will
     * try to remove the object only. Two notes on recursive delete. First, be
     * aware of the PERFORMANCE hit when large amount of child objects present.
     * In the soft-delete mode, this method will mark the following objects for
     * deletion: <code> Organization, Group, User </code>
     * <code>purge()</code>
     * should be used to physically delete this object.
     * 
     * @see #purge(boolean, int)
     * 
     * @param recursive
     *            if true delete the object and any objects under it, otherwise,
     *            delete the object only.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void delete(boolean recursive) throws AMException, SSOException;

    /**
     * Search objects based on specified level and filter.
     * 
     * @param level
     *            The search level starting from the object
     * @param filter
     *            The search filter
     * @return Set of object DN's matching the search
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set search(int level, String filter) throws AMException,
            SSOException;

    /**
     * Registers a event listener that needs to be invoked when a relevant event
     * occurs. If the listener was already registered, then it is registered
     * only once; no duplicate registration is allowed.
     * <p>
     * {@link Object#equals Object.equals()} method on the listener object is
     * used to determine duplicates.
     * 
     * @param listener
     *            listener object that will be called upon when an event occurs.
     * @throws SSOException
     *             if errors were encountered in adding a new
     *             <code>SSOTokenListener</code> instance
     */
    public void addEventListener(AMEventListener listener) throws SSOException;

    /**
     * Unregisters a previously registered event listener. If the
     * <code>listener</code> was not registered previously, the method simply
     * returns without doing anything.
     * 
     * @param listener
     *            listener object that will be removed or unregistered.
     */
    public void removeEventListener(AMEventListener listener);

    /**
     * Creates a Policy Template with no priority for the given service
     * associated with this <code>AMObject</code>. This is a convenience
     * method and is equivalent to <code>createTemplate(
     * AMTemplate.POLICY_TEMPLATE, serviceName, attributes)</code>.
     * 
     * @param serviceName
     *            service name.
     * @param attributes
     *            Map of attributes name-value pairs.
     * @return <code>AMTemplate</code> the service template for this
     *         <code>AMObject</code>.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     * @throws UnsupportedOperationException
     *             if the class implementing this interface does not support
     *             this method.
     * @deprecated use <code>com.sun.identity.policy</code> package for
     *             creating and managing policies.
     */
    public AMTemplate createPolicyTemplate(String serviceName, Map attributes)
            throws UnsupportedOperationException, AMException, SSOException;

    /**
     * Creates a Policy Template with a priority for the given service
     * associated with this <code>AMObject</code>. This is a convenience
     * method and is equivalent to
     * <code>createTemplate(AMTemplate.POLICY_TEMPLATE, serviceName,
     * attributes, priority)</code>.
     * 
     * @param serviceName
     *            service name.
     * @param attributes
     *            Map of attributes name-value pairs.
     * @param priority
     *            template priority (0 is the highest priority).
     * @return <code>AMTemplate</code> the service template for this
     *         <code>AMObject</code>.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     * @throws UnsupportedOperationException
     *             if the class implementing this interface does not support
     *             this method.
     * @deprecated use <code>com.sun.identity.policy</code> package for
     *             creating and managing policies.
     */
    public AMTemplate createPolicyTemplate(String serviceName, Map attributes,
            int priority) throws UnsupportedOperationException, AMException,
            SSOException;

    /**
     * Returns the policy template for a service defined for this object
     * ignoring any inheritance. This is a convenience method and is equivalent
     * to <code>getTemplate(serviceName, AMTemplate.POLICY_TEMPLATE)</code>.
     * <code>AMUser</code> object will throw
     * <code>UnsupportedOperationException</code> because this method is not
     * relevant for <code>AMUser</code>.
     * 
     * @param serviceName
     *            service name.
     * @return <code>AMTemplate</code> the policy template of the service for
     *         this object.
     * @throws UnsupportedOperationException
     *             if the class implementing this interface does not support
     *             this method.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid
     * @deprecated This method has been deprecated. Please use:
     *             <code>com.sun.identity.policy</code> package for creating
     *             and managing policies.
     */
    public AMTemplate getPolicyTemplate(String serviceName)
            throws UnsupportedOperationException, AMException, SSOException;

    /**
     * Returns the effective service policy defined at this object after
     * considering any inheritance from any policy templates.
     * 
     * @param serviceName
     *            service name.
     * @return Map the effective service policy for the object after
     *         inheritance; key is the attribute name and value is attribute
     *         value. An empty Map will be returned if no policy attribute is
     *         defined in the specified service.
     * 
     * @throws UnsupportedOperationException
     *             if the class implementing this interface does not support
     *             this method
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     * @deprecated This method has been deprecated. Please use:
     *             <code>com.sun.identity.policy</code> package for creating
     *             and managing policies.
     */
    public Map getPolicy(String serviceName)
            throws UnsupportedOperationException, AMException, SSOException;

    /**
     * Returns the object's organization. NOTE: Obtaining an organization DN
     * involves considerable overhead. Hence after obtaining the organization
     * DN, each object saves this information. Consecutives method calls on this
     * object fetch the value stored in the object. Creating a new
     * <code>AMObject</code> instance every time to obtain the organization DN
     * is not recommended.
     * 
     * @return The object's organization DN.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store or the object does not have
     *             organization DN.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public String getOrganizationDN() throws AMException, SSOException;

    /**
     * Assign services to the entity (user/group/organization/organization
     * unit). Also sets the attributes as provided in the map
     * <code>serviceNameAndAttrs</code>. <code>serviceNameAndAttrs</code>
     * is a map of keys which are service names and values which are
     * attribute-value maps. Attribute values are validated against the
     * respective service schemas before being set. Any required attributes (as
     * defined in the service schema) not provided in the attribute Map, will be
     * included and set to default values (picked up from the service schema).
     * Only services which have been registered with the parent organization of
     * the entity (the organization itself, in case the entity is an
     * organization) will be assigned to the entity. So before assigning a
     * service to an entity, <code>registerService()</code> should be used on
     * the parent organization.
     * 
     * @see AMOrganization#registerService
     * @param serviceNameAndAttrs
     *            Map of Service name with Map of Attribute-Value pairs
     * @throws AMException
     *             if an error is encounters when trying to access/retrieve data
     *             from the data store
     * @throws SSOException
     *             if the token is no longer valid
     */
    public void assignServices(Map serviceNameAndAttrs) throws AMException,
            SSOException;

    /**
     * Assign services to the entity (user/group/organization/organization
     * unit). Also sets the attributes as provided in the map
     * <code>serviceNameAndAttrs</code>. <code>serviceNameAndAttrs</code>
     * is a map of keys which are service names and values which are
     * attribute-value maps. Attribute values are validated against the
     * respective service schemas before being set. Any required attributes (as
     * defined in the service schema) not provided in the attribute Map, will be
     * included and set to default values (picked up from the service schema).
     * Only services which have been registered with the parent organization of
     * the entity (the organization itself, in case the entity is an
     * organization) will be assigned to the entity. So before assigning a
     * service to an entity, <code>registerService()</code> should be used on
     * the parent organization.
     * 
     * @see AMOrganization#registerService
     * @param serviceNameAndAttrs
     *            Map of Service name with Map of Attribute-Value pairs
     * @param store
     *            If true, then the service attributes are saved in the
     *            directory entry, otherwise, they are saved in the object and
     *            the store method has to be called, to save these attributes
     *            persistently in the data store.
     * @throws AMException
     *             if an error is encounters when trying to access/retrieve data
     *             from the data store
     * @throws SSOException
     *             if the token is no longer valid
     */
    /*
     * public void assignServices(Map serviceNameAndAttrs, boolean store) throws
     * AMException, SSOException;
     */

    /**
     * Modify the service attributes of a service assigned to a entity
     * (user/group/organization/organizational unit). It replaces existing
     * service attribute values with the ones provided. If new attribute values
     * are provided, those are set too. Values of the attribute are validated
     * against the service schema. Before the values are replaced in the entry,
     * the <code>ServiceCallback</code> classes, if any, are instantiated and
     * the <code>validateAttribute()</code> method is called.
     * 
     * @param serviceName
     *            Name of the service which has to be modified
     * @param attrMap
     *            Map of attribute names and set of attribute values.
     * @throws AMException
     *             if an error occurs during validation or storing
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void modifyService(String serviceName, Map attrMap)
            throws AMException, SSOException;

    /**
     * Set the status of the service for this entity (User/Organization
     * /Group/Organizational Unit). The valid values for the status attribute
     * are defined in the SMS DTD. Before the status attribute is set, it is
     * verified to see if the service is first assigned to the user or not, and
     * that the value for the status is valid per the DTD.
     * 
     * @param serviceName
     *            Name of service whose status attribute has to be changed
     * @param status
     *            One of the status values as defined in the service schema
     * @throws AMException
     *             if a data store exception is encountered.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void setServiceStatus(String serviceName, String status)
            throws AMException, SSOException;

    /**
     * Get the status of the service for this entity (user/group/organization/
     * organizational unit). Returns null, if service is not assigned to the
     * entity.
     * 
     * @return status Value of the status attribute
     * @param serviceName
     *            Name of service
     * @throws AMException
     *             if a data store exception is encountered
     * @throws SSOException
     *             if single sign on token is no longer valid.
     */
    public String getServiceStatus(String serviceName) throws AMException,
            SSOException;

    /**
     * Unassigns services from the user/group/organization/ organizational unit.
     * 
     * @param serviceNames
     *            Set of service names
     * @throws AMException
     *             if there is an internal error in the access management data
     *             store.
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public void unassignServices(Set serviceNames) throws AMException,
            SSOException;

    /**
     * Returns all service names that are assigned to the
     * user/group/organization/organizational unit.
     * 
     * @return The Set of service names that are assigned to the user.
     * 
     * @throws AMException
     *             if there is an internal error in the access management data
     *             store.
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set getAssignedServices() throws AMException, SSOException;

    /**
     * This method will physically delete the entry from the data store. This
     * method will override the soft-delete option, which the method
     * <code> delete()</code> will not. There is a big PERFORMANCE hit if this
     * method is used to delete a large Organization in the recursive mode.
     * 
     * @see #delete()
     * @param recursive
     *            If true, then recursively delete the whole subtree.
     * @param graceperiod
     *            If set to an integer greater than -1, it will verify if the
     *            object was last modified at least that many days ago before
     *            physically deleting it. Pre/Post <code>Callback</code>
     *            plugins as registered in the Administration Service, will be
     *            called upon object deletion. If any of the
     *            <code>pre-callback</code> classes throw an exception, then
     *            the operation is aborted.
     * @throws AMException
     *             if there is an internal error in the access management data
     *             store.
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public void purge(boolean recursive, int graceperiod) throws AMException,
            SSOException;
}

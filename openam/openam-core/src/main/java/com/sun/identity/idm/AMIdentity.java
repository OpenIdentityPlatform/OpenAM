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
 * $Id: AMIdentity.java,v 1.37 2009/11/20 23:52:54 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted 2011-2013 ForgeRock, Inc.
 */
package com.sun.identity.idm;

import com.iplanet.am.sdk.AMCommonUtils;
import com.iplanet.am.sdk.AMCrypt;
import com.iplanet.am.sdk.AMHashMap;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.common.CaseInsensitiveHashSet;
import com.sun.identity.idm.common.IdRepoUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.ServiceNotFoundException;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.shared.ldap.util.RDN;

/**
 * This class represents an Identity which needs to be managed by Access
 * Manager. This identity could exist in multiple repositories, which are
 * configured for a given realm or organization. When any operation is performed
 * from this class, it executes all plugins that are configured for performing
 * that operation. For eg: getAttributes. The application gets access to
 * constructing <code> AMIdentity </code> objects by using
 * <code> AMIdentityRepository
 * </code> interfaces. For example:
 * <p>
 *
 * <PRE>
 *
 * AMIdentityRepository idrepo = new AMIdentityRepository(token, org);
 * AMIdentity id = idrepo.getRealmIdentity();
 *
 * </PRE>
 *
 * The <code>id</code> returned above is the AMIdentity object of the user's
 * single sign-on token passed above. The results obtained from search performed
 * using AMIdentityRepository also return AMIdentity objects. The type of an
 * object can be determined by doing the following:
 * <p>
 *
 * <PRE>
 *
 * IdType type = identity.getType();
 *
 * </PRE>
 *
 * The name of an object can be determined by:
 * <p>
 *
 * <PRE>
 *
 * String name = identity.getName();
 *
 * </PRE>
 *
 * @supported.api
 */

public final class AMIdentity {

    private String univIdWithoutDN;

    private final SSOToken token;

    private final String name;

    private final IdType type;

    private final String orgName;

    private Set fullyQualifiedNames;

    private final AMHashMap modMap = new AMHashMap(false);

    private final AMHashMap binaryModMap = new AMHashMap(true);

    protected String univDN = null;

    /**
     * @supported.api
     *
     * Constructor for the <code>AMIdentity</code> object.
     *
     * @param ssotoken
     *            Single sign on token of the user
     * @throws SSOException
     *             if user's single sign on token is invalid.
     * @throws IdRepoException
     *            if the single sign on token does not have a
     *            a valid universal identifier
     */
    public AMIdentity(SSOToken ssotoken) throws SSOException, IdRepoException {
        this(ssotoken, ssotoken.getProperty(Constants.UNIVERSAL_IDENTIFIER));
    }

    /**
     * @supported.api
     *
     * Constructor for the <code>AMIdentity</code> object.
     *
     * @param ssotoken
     *            Single sign on token to construct the identity
     *            object. Access permission to Identity object
     *            would be based on this user
     * @param universalId
     *            Universal Identifier of the identity.
     *
     * @throws IdRepoException
     *            if the universal identifier is invalid
     *
     */
    public AMIdentity(SSOToken ssotoken, String universalId)
        throws IdRepoException {
        this.token = ssotoken;
        // Validate Universal ID
        DN dnObject = new DN(universalId);
        String[] array = null;
        if ((universalId != null) &&
            universalId.toLowerCase().startsWith("id=") &&
            dnObject.isDN()) {
            array = dnObject.explodeDN(true);
        }
        if ((array == null) || (array.length <3)) {
            // Not a valid UUID since it should have the
            // name, type and realm components
            Object args[] = { universalId };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "215", args);
        }

        // Valid UUID, construct rest of the parameters
        univIdWithoutDN = dnObject.toRFCString();

        // Check for AMSDK DN
        int index;
        if ((index = univIdWithoutDN.toLowerCase().indexOf(
            ",amsdkdn=")) != -1) {
            // obtain DN and univIdWithoutDN
            univDN = univIdWithoutDN.substring(index + 9);
            univIdWithoutDN = univIdWithoutDN.substring(0, index);
            dnObject = new DN(univIdWithoutDN);

        }
        name = LDAPDN.unEscapeValue( array[0] );
        type = new IdType(array[1]);
        orgName = dnObject.getParent().getParent().toRFCString();
    }

    public AMIdentity(DN universalId, SSOToken ssotoken)
        throws IdRepoException {
        this.token = ssotoken;
        // Validate Universal ID
        DN dnObject = universalId;
        String[] array = null;
        if ((dnObject != null) &&
            ((RDN) dnObject.getRDNs().get(0)).getType().toLowerCase()
                .equals("id") &&
            dnObject.isDN()) {
            array = dnObject.explodeDN(true);
        }
        if ((array == null) || (array.length <3)) {
            // Not a valid UUID since it should have the
            // name, type and realm components
            Object args[] = { dnObject };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "215", args);
        }

        // Valid UUID, construct rest of the parameters
        univIdWithoutDN = dnObject.toRFCString();
        // Check for AMSDK DN
        int index;
        if ((index = univIdWithoutDN.toLowerCase().indexOf(
            ",amsdkdn=")) != -1) {
            // obtain DN and univIdWithoutDN
            univDN = univIdWithoutDN.substring(index + 9);
            univIdWithoutDN = univIdWithoutDN.substring(0, index);
            dnObject = new DN(univIdWithoutDN);
        }
        name = array[0];
        type = new IdType(array[1]);
        orgName = dnObject.getParent().getParent().toRFCString();
    }

    /**
     * Constructor for the <code>AMIdentity</code> object.
     *
     * @param token
     *            Single sign on token to construct the identity
     *            object. Access permission to Identity object
     *            would be based on this user
     * @param name
     *            the name associated with this identity.
     * @param type
     *            the <code>IdType</code> of this identity.
     * @param orgName
     *            the organizaton name this identity belongs to.
     * @param amsdkdn
     *            the amsdk name assoicated with this identity if any.
     */
    public AMIdentity(SSOToken token, String name, IdType type, String orgName,
            String amsdkdn) {
        this.name = name;
        this.type = type;
        this.orgName = com.sun.identity.sm.DNMapper.orgNameToDN(orgName);
        this.token = token;
        if ((amsdkdn != null) && (amsdkdn.length() > 0)) {
            this.univDN = (new DN(amsdkdn)).toRFCString();
        }
        StringBuilder sb = new StringBuilder(100);
        if ((name != null) && (name.indexOf(',') != -1)) {
            DN nameDN = new DN(name);
            if (nameDN.isDN()) {
                name = LDAPDN.explodeDN(nameDN, true)[0];
            }
        }
        sb.append("id=").append(name).append(",ou=").append(type.getName())
            .append(",").append(this.orgName);

        univIdWithoutDN = sb.toString();
    }

    public AMIdentity(DN amsdkdn, SSOToken token, String name, IdType type,
            String orgName) {
        this.name = name;
        this.type = type;
        this.orgName = com.sun.identity.sm.DNMapper.orgNameToDN(orgName);
        this.token = token;
        if (amsdkdn != null) {
            this.univDN = amsdkdn.toRFCString();
        }
        StringBuilder sb = new StringBuilder(100);
        if ((name != null) && (name.indexOf(',') != -1)) {
            DN nameDN = new DN(name);
            if (nameDN.isDN()) {
                name = LDAPDN.explodeDN(nameDN, true)[0];
            }
        }
        sb.append("id=").append(LDAPDN.escapeValue( name ) )
            .append(",ou=").append(type.getName())
            .append(",").append(this.orgName);

        univIdWithoutDN = sb.toString();
    }

    // General APIs
    /**
     *
     * Returns the name of the identity.
     *
     * @return Name of the identity
     * @supported.api
     */
    public String getName() {
        String sname = name;
        if (type.equals(IdType.REALM)) {
            // Since '0'th location currently has ContainerDefaultTemplate
            // the 2nd location would have the realm name
            String[] array = (new DN(univIdWithoutDN)).explodeDN(true);
            sname = array[2];
        }
        return sname;
    }

    /**
     * Returns the Type of the Identity.
     *
     * @return <code>IdType</code> representing the type of this object.
     * @supported.api
     */
    public IdType getType() {
        return type;
    }

    /**
     * Returns the realm for this identity.
     *
     * @return String representing realm name.
     * @supported.api
     */
    public String getRealm() {
        return orgName;
    }

    /**
     * If there is a status attribute configured, then verifies if the identity
     * is active and returns true. This method is only valid for AMIdentity
     * objects of type User and Agent.
     *
     * @return true if the identity is active or if it is not configured for a
     *         status attribute, false otherwise.
     * @throws IdRepoException
     *             If there are repository related error conditions.
     * @throws SSOException
     *             If user's single sign on token is invalid.
     * @supported.api
     */
    public boolean isActive() throws IdRepoException, SSOException {
        IdServices idServices = IdServicesFactory.getDataStoreServices();
        return idServices.isActive(token, type, name, orgName, univDN);
    }

    /**
     * If there is a status attribute configured, then set its status to
     * true or activated state if the parameter active is true.
     * This method is only valid for AMIdentity objects of type User and Agent.
     *
     * @param active The state value to assign to status attribute. The actual
     * value assigned to the status attribute will depend on what is configured
     * for that particular plugin.  If active is true, the status will be
     * assigned the value corresponding to activated.
     * @throws IdRepoException If there are repository related error conditions.
     * @throws SSOException If user's single sign on token is invalid.
     * @supported.api
     */
    public void setActiveStatus(boolean active)
        throws IdRepoException, SSOException {
        IdServices idServices =
            IdServicesFactory.getDataStoreServices();
        idServices.setActiveStatus(token, type, name, orgName, univDN, active);
    }

    /**
     * Returns all attributes and values of this identity. This method is only
     * valid for AMIdentity objects of type User, Agent, Group, and Role.
     *
     * @return Map of attribute-values
     * @throws IdRepoException
     *             If there are repository related error conditions.
     * @throws SSOException
     *             If user's single sign on token is invalid.
     * @supported.api
     */
    public Map getAttributes() throws IdRepoException, SSOException {

        IdServices idServices = IdServicesFactory.getDataStoreServices();
        Map attrs = idServices
                .getAttributes(token, type, name, orgName, univDN);
        if (debug.messageEnabled()) {
            debug.message("AMIdentity.getAttributes all: attrs=" +
                IdRepoUtils.getAttrMapWithoutPasswordAttrs(attrs, null));
        }
        return attrs;
    }

    /**
     * Returns requested attributes and values of this object.
     *
     * This method is only valid for AMIdentity object of type User, Agent,
     * Group, and Role.
     *
     * @param attrNames
     *            Set of attribute names to be read
     * @return Map of attribute-values.
     * @throws IdRepoException
     *             If there are repository related error conditions.
     * @throws SSOException
     *             If user's single sign on token is invalid.
     * @supported.api
     */
    public Map getAttributes(Set attrNames) throws IdRepoException,
            SSOException {

        IdServices idServices = IdServicesFactory.getDataStoreServices();
        Map attrs = idServices.getAttributes(token, type, name, attrNames,
                orgName, univDN, true);
        CaseInsensitiveHashMap caseAttrs = new CaseInsensitiveHashMap(attrs);
        CaseInsensitiveHashMap resultMap = new CaseInsensitiveHashMap();
        Iterator it = attrNames.iterator();
        while (it.hasNext()) {
            String attrName = (String) it.next();
            if (caseAttrs.containsKey(attrName)) {
                resultMap.put(attrName, caseAttrs.get(attrName));
            }
        }

        if (debug.messageEnabled()) {
            debug.message("AMIdentity.getAttributes 6: attrNames=" + attrNames
                    + ";  resultMap=" + resultMap + "; attrs=" + attrs);
        }
        return resultMap;
    }

    /**
     * Returns requested attributes and values of this object.
     *
     * This method is only valid for AMIdentity objects of type User, Agent,
     * Group, and Role.
     *
     * @param attrNames
     *            Set of attribute names to be read
     * @return Map of attribute-values.
     * @throws IdRepoException
     *             If there are repository related error conditions.
     * @throws SSOException
     *             If user's single sign on token is invalid.
     * @supported.api
     */
    public Map getBinaryAttributes(Set attrNames) throws IdRepoException,
            SSOException {

        IdServices idServices = IdServicesFactory.getDataStoreServices();
        return idServices.getAttributes(token, type, name, attrNames, orgName,
                univDN, false);
    }

    /**
     * Returns the values of the requested attribute. Returns an empty set, if
     * the attribute is not set in the object.
     *
     * This method is only valid for AMIdentity objects of type User, Agent,
     * Group, and Role.
     *
     * @param attrName
     *            Name of attribute
     * @return Set of attribute values.
     * @throws IdRepoException
     *             if there are repository related error conditions.
     * @throws SSOException
     *             If user's single sign on token is invalid.
     * @supported.api
     */
    public Set getAttribute(String attrName) throws IdRepoException,
            SSOException {

        Set attrNames = new HashSet();
        attrNames.add(attrName);
        IdServices idServices = IdServicesFactory.getDataStoreServices();
        Map valMap = idServices.getAttributes(token, type, name, attrNames,
                orgName, univDN, true);
        return ((Set) valMap.get(attrName));
    }

    /**
     * Sets the values of attributes. This method should be followed by the
     * method "store" to commit the changes to the Repository.
     * This method is only valid for <code>AMIdentity</code> objects of
     * type User and Agent.
     *
     * @param attrMap is a map of attribute name
     *        <code>(String)</code>
     *        to a <code>Set</code> of attribute values <code>(String)</code>.
     *        It is arranged as:
     *        Map::attrMap -->
     *        Key: String::AttributeName
     *        Value: Set::AttributeValues (Set of String)
     * @throws IdRepoException
     *             If there are repository related error conditions.
     * @throws SSOException
     *             If user's single sign on token is invalid.
     * @supported.api
     */
    public void setAttributes(Map attrMap) throws IdRepoException, SSOException
    {
        modMap.copy(attrMap);
    }

    /**
     * Changes password for the identity.
     *
     * @param oldPassword old password
     * @param newPassword new password
     * @throws IdRepoException If there are repository related error conditions.
     * @throws SSOException If user's single sign on token is invalid.
     * @supported.api
     */
    public void changePassword(String oldPassword, String newPassword)
        throws IdRepoException, SSOException {

        IdServices idServices = IdServicesFactory.getDataStoreServices();
        idServices.changePassword(token, type, name, oldPassword,
            newPassword, orgName, getDN());
    }

    /**
     * Set the values of binary attributes. This method should be followed by
     * the method "store" to commit the changes to the Repository
     *
     * This method is only valid for AMIdentity objects of type User and Agent.
     *
     * @param attrMap
     *            Map of attribute-values to be set in the repository or
     *            repositories (if multiple plugins are configured for "edit").
     * @throws IdRepoException
     *             If there are repository related error conditions.
     * @throws SSOException
     *             If user's single sign on token is invalid.
     * @supported.api
     */
    public void setBinaryAttributes(Map attrMap) throws IdRepoException,
            SSOException {
        binaryModMap.copy(attrMap);
    }

    /**
     * Removes the attributes from the identity entry. This method should be
     * followed by a "store" to commit the changes to the Repository.
     *
     * This method is only valid for AMIdentity objects of type User and Agent.
     *
     * @param attrNames
     *            Set of attribute names to be removed
     * @throws IdRepoException
     *             If there are repository related error conditions.
     * @throws SSOException
     *             If the user's single sign on token is invalid
     * @supported.api
     */
    public void removeAttributes(Set attrNames) throws IdRepoException,
            SSOException {
        if (attrNames == null || attrNames.isEmpty()) {
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "201", null);
        }

        boolean agentflg = getType().equals(IdType.AGENTONLY);
        if (agentflg) {
            IdServices idServices = IdServicesFactory.getDataStoreServices();
            idServices.removeAttributes(token, type, name, attrNames,
                orgName, null);
            Iterator it = attrNames.iterator();
            while (it.hasNext()) {
                String attr = (String) it.next();
                modMap.remove(attr);
            }
        } else {
            Iterator it = attrNames.iterator();
            while (it.hasNext()) {
                String attr = (String) it.next();
                modMap.put(attr, Collections.EMPTY_SET);
            }
        }
    }

    /**
     * Stores the attributes of the object.
     *
     * This method is only valid for AMIdentity objects of type User and Agent.
     *
     * @throws IdRepoException
     *             If there are repository related error conditions.
     * @throws SSOException
     *             If user's single sign on token is invalid.
     * @supported.api
     */
    public void store() throws IdRepoException, SSOException {
        IdServices idServices = IdServicesFactory.getDataStoreServices();
        if (modMap != null && !modMap.isEmpty()) {
            idServices.setAttributes(token, type, name, modMap, false, orgName,
                    univDN, true);
            modMap.clear();
        }
        if (binaryModMap != null && !binaryModMap.isEmpty()) {
            idServices.setAttributes(token, type, name, binaryModMap, false,
                    orgName, univDN, false);
            binaryModMap.clear();
        }
    }

    // SERVICE RELATED APIS

    /**
     * Returns the set of services already assigned to this identity.
     *
     * This method is only valid for AMIdentity object of type User.
     *
     * @return Set of serviceNames
     * @throws IdRepoException
     *             If there are repository related error conditions.
     * @throws SSOException
     *             If user's single sign on token is invalid.
     * @supported.api
     */
    public Set getAssignedServices() throws IdRepoException, SSOException {
        // Get all service names for the type from SMS
        ServiceManager sm;
        try {
            sm = new ServiceManager(token);
        } catch (SMSException smse) {
            debug.error("Error while creating Service manager:", smse);
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "106", null);
        }
        Map sMap = sm.getServiceNamesAndOCs(type.getName());

        // Get the list of assigned services
        IdServices idServices = IdServicesFactory.getDataStoreServices();
        Set assigned = Collections.EMPTY_SET;
        try {
            assigned = idServices.getAssignedServices(token, type, name, sMap,
                    orgName, univDN);
        } catch (IdRepoException ide) {
            // Check if this is permission denied exception
            if (!ide.getErrorCode().equals("402")) {
                throw (ide);
            }
        }
        return (assigned);
    }

    /**
     * Returns all services which can be assigned to this entity.
     *
     * This method is only valid for AMIdentity object of type User.
     *
     * @return Set of service names
     * @throws IdRepoException
     *             if there are repository related error conditions.
     * @throws SSOException
     *             If user's single sign on token is invalid.
     * @supported.api
     */
    public Set getAssignableServices() throws IdRepoException, SSOException {
        // Get all service names for the type from SMS
        ServiceManager sm;
        try {
            sm = new ServiceManager(token);
        } catch (SMSException smse) {
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "106", null);
        }
        Map sMap = sm.getServiceNamesAndOCs(type.getName());

        // Get the list of assigned services
        IdServices idServices = IdServicesFactory.getDataStoreServices();
        Set assigned = Collections.EMPTY_SET;
        try {
            assigned = idServices.getAssignedServices(token, type, name, sMap,
                    orgName, univDN);
        } catch (IdRepoException ide) {
            // Check if this is permission denied exception
            if (!ide.getErrorCode().equals("402")) {
                throw (ide);
            } else {
                // Return the empty set
                return (assigned);
            }
        }

        // Return the difference
        Set keys = sMap.keySet();
        keys.removeAll(assigned);
        return (keys);

    }

    /**
     * Assigns the service and service related attributes to the identity.
     *
     * This method is only valid for AMIdentity object of type User.
     *
     * @param serviceName
     *            Name of service to be assigned.
     * @param attributes
     *            Map of attribute-values
     * @throws IdRepoException
     *             If there are repository related error conditions.
     * @throws SSOException
     *             If user's single sign on token is invalid.
     * @supported.api
     */
    public void assignService(String serviceName, Map attributes)
            throws IdRepoException, SSOException {

        IdServices idServices = IdServicesFactory.getDataStoreServices();
        Set OCs = getServiceOCs(token, serviceName);
        SchemaType stype;
        Map tMap = new HashMap();
        tMap.put(serviceName, OCs);
        Set assignedServices = idServices.getAssignedServices(token, type,
                name, tMap, orgName, univDN);

        if (assignedServices.contains(serviceName)) {
            Object args[] = { serviceName, type.getName() };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "105", args);
        }

        // Validate the service attributes
        try {
            ServiceSchemaManager ssm = new ServiceSchemaManager(serviceName,
                    token);
            ServiceSchema ss = ssm.getSchema(type.getName());

            if (ss != null) {
                // Check if attrMap has cos priority attribute
                // If present, remove it for validating the attributes
                Set cosPriority = (attributes != null) ?
                    (Set)attributes.remove(COS_PRIORITY) : null;
                attributes = ss.validateAndInheritDefaults(attributes, orgName,
                        true);
                if (cosPriority != null) {
                    attributes.put(COS_PRIORITY, cosPriority);
                }
                attributes = AMCommonUtils.removeEmptyValues(attributes);
                stype = ss.getServiceType();
            } else {
                ss = ssm.getSchema(SchemaType.DYNAMIC);
                if (ss == null) {
                    Object args[] = { serviceName };
                    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "102",
                            args);
                }
                if (attributes == null) {
                    try {
                        attributes = getServiceConfig(token, serviceName,
                                SchemaType.DYNAMIC);
                    } catch (SMSException smsex) {
                        Object args[] = { serviceName, type.getName() };
                        throw new IdRepoException(IdRepoBundle.BUNDLE_NAME,
                                "451", args);
                    }
                } else {
                    attributes = ss.validateAndInheritDefaults(attributes,
                            orgName, true);
                }
                attributes = AMCommonUtils.removeEmptyValues(attributes);
                stype = SchemaType.DYNAMIC;
            }

            // TODO: Remove this dependency of AMCrypt
            attributes = AMCrypt.encryptPasswords(attributes, ss);
        } catch (SMSException smse) {
            // debug.error here
            Object[] args = { serviceName };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "101", args);
        }

        attributes.put("objectclass", OCs);
        // The protocol for params is to pass the
        // name of the service, and attribute Map containing the
        // OCs to be set and validated attribute map
        idServices.assignService(token, type, name, serviceName, stype,
                attributes, orgName, univDN);
    }

    /**
     * Removes a service from the identity.
     *
     * This method is only valid for AMIdentity object of type User.
     *
     * @param serviceName
     *            Name of service to be removed.
     * @throws IdRepoException
     *             If there are repository related error conditions.
     * @throws SSOException
     *             If user's single sign on token is invalid.
     * @supported.api
     */
    public void unassignService(String serviceName) throws IdRepoException,
            SSOException {
        IdServices idServices = IdServicesFactory.getDataStoreServices();
        Set OCs = getServiceOCs(token, serviceName);

        Map tMap = new HashMap();
        tMap.put(serviceName, OCs);
        Set assignedServices = idServices.getAssignedServices(token, type,
                name, tMap, orgName, univDN);

        if (!assignedServices.contains(serviceName)) {
            Object args[] = { serviceName };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "101", args);
        }

        Map attrMap = new HashMap();
        Set objectclasses = getAttribute("objectclass");
        if (objectclasses != null && !objectclasses.isEmpty()) {
            Set removeOCs = AMCommonUtils.updateAndGetRemovableOCs(
                    objectclasses, OCs);

            try {
                // Get attribute names for USER type only, so plugin knows
                // what attributes to remove.
                Set attrNames = new HashSet();
                ServiceSchemaManager ssm = new ServiceSchemaManager(
                        serviceName, token);
                ServiceSchema uss = ssm.getSchema(type.getName());

                if (uss != null) {
                    attrNames = uss.getAttributeSchemaNames();
                }

                Iterator it = attrNames.iterator();
                while (it.hasNext()) {
                    String a = (String) it.next();
                    attrMap.put(a, Collections.EMPTY_SET);
                }
            } catch (SMSException smse) {
                /*
                 * debug.error( "AMIdentity.unassignService: Caught SM
                 * exception", smse); do nothing
                 */
            }

            attrMap.put("objectclass", removeOCs);
            // The protocol is to pass service Name and Map of objectclasses
            // to be removed from entry.
        }

        idServices.unassignService(token, type, name, serviceName, attrMap,
                orgName, univDN);
    }

    /**
     * Returns attributes related to a service, if the service is assigned to
     * the identity.
     *
     * This method is only valid for AMIdentity object of type User.
     *
     * @param serviceName
     *            Name of the service.
     * @return Map of attribute-values.
     * @throws IdRepoException
     *             if there are repository related error conditions.
     * @throws SSOException
     *             If user's single sign on token is invalid.
     * @supported.api
     */
    public Map getServiceAttributes(String serviceName)
        throws IdRepoException, SSOException {
        Set attrNames = getServiceAttributesName(serviceName);

        IdServices idServices =
            IdServicesFactory.getDataStoreServices();
        if (debug.messageEnabled()) {
            debug.message("AMIdentity.getServiceAttributes: attrNames="
                + attrNames + ";  orgName=" + orgName + ";  univDN=" + univDN);
       }
        return idServices.getServiceAttributes(token, type, name, serviceName,
            attrNames, orgName, univDN);
    }


    /**
     * Returns attributes related to a service, if the service is assigned
     * to the identity.
     *
     * This method is only valid for AMIdentity object of type User.
     *
     * @param serviceName Name of the service.
     * @return Map of attribute-values in array of byte.
     * @throws IdRepoException if there are repository related error conditions.
     * @throws SSOException If user's single sign on token is invalid.
     * iPlanet-PUBLIC-METHOD
     */
    public Map getBinaryServiceAttributes(String serviceName)
        throws IdRepoException, SSOException {
        Set attrNames = getServiceAttributesName(serviceName);

        IdServices idServices =
            IdServicesFactory.getDataStoreServices();
        if (debug.messageEnabled()) {
            debug.message("AMIdentity.getBinaryServiceAttributes: attrNames="
                + attrNames + ";  orgName=" + orgName + ";  univDN=" + univDN);
        }
        return idServices.getBinaryServiceAttributes(token, type, name,
            serviceName, attrNames, orgName, univDN);
    }


    /**
     * Returns attributes related to a service, if the service is assigned
     * to the identity.
     *
     * This method is only valid for AMIdentity object of type User.
     *
     * @param serviceName Name of the service.
     * @return Map of attribute-values.
     * @throws IdRepoException if there are repository related error conditions.
     * @throws SSOException If user's single sign on token is invalid.
     * @supported.api
     */
    public Map getServiceAttributesAscending(String serviceName)
        throws IdRepoException, SSOException {
        Set attrNames = getServiceAttributesName(serviceName);

        IdServices idServices =
            IdServicesFactory.getDataStoreServices();
        if (debug.messageEnabled()) {
            debug.message("AMIdentity.getServiceAttributesAscending: "
                + "attrNames=" + attrNames + ";  orgName=" + orgName
                + ";  univDN=" + univDN);
        }
        return idServices.getServiceAttributesAscending(token, type, name,
            serviceName, attrNames, orgName, univDN);
    }


    /**
     * Set attributes related to a specific service. The assumption is that the
     * service is already assigned to the identity. The attributes for the
     * service are validated against the service schema.
     *
     * This method is only valid for AMIdentity object of type User.
     *
     * @param serviceName
     *            Name of the service.
     * @param attrMap
     *            Map of attribute-values.
     * @throws IdRepoException
     *             If there are repository related error conditions.
     * @throws SSOException
     *             If user's single sign on token is invalid.
     * @supported.api
     */
    public void modifyService(String serviceName, Map attrMap)
            throws IdRepoException, SSOException {
        IdServices idServices = IdServicesFactory.getDataStoreServices();
        Set OCs = getServiceOCs(token, serviceName);
        SchemaType stype;
        Map tMap = new HashMap();
        tMap.put(serviceName, OCs);
        Set assignedServices = idServices.getAssignedServices(token, type,
                name, tMap, orgName, univDN);
        if (!assignedServices.contains(serviceName)) {
            Object args[] = { serviceName };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "101", args);
        }

        // Check if attrMap has cos priority attribute
        // If present, remove it for validating the attributes
        boolean hasCosPriority = (new CaseInsensitiveHashSet(
            attrMap.keySet()).contains(COS_PRIORITY));
        Object values = null;
        if (hasCosPriority) {
             attrMap = new CaseInsensitiveHashMap(attrMap);
             values = attrMap.remove(COS_PRIORITY);
        }

        // Validate the attributes
        try {
            ServiceSchemaManager ssm = new ServiceSchemaManager(serviceName,
                    token);
            ServiceSchema ss = ssm.getSchema(type.getName());
            if (ss != null) {
                attrMap = ss.validateAndInheritDefaults(attrMap, false);
                stype = ss.getServiceType();
            } else if ((ss = ssm.getSchema(SchemaType.DYNAMIC)) != null) {
                 attrMap = ss.validateAndInheritDefaults(attrMap, false);
                 stype = SchemaType.DYNAMIC;
            } else {
                 Object args[] = { serviceName };
                 throw new IdRepoException(IdRepoBundle.BUNDLE_NAME,
                     "102", args);
            }
        } catch (SMSException smse) {
            // debug.error
            Object args[] = { serviceName };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "103", args);
        }

        // Add COS priority if present
        if (hasCosPriority) {
            attrMap.put(COS_PRIORITY, values);
        }

        // modify service attrs
        if (debug.messageEnabled()) {
            debug.message("AMIdentity.modifyService befre idService " +
                "serviceName=" + serviceName + ";  attrMap=" + attrMap);
        }
        idServices.modifyService(token, type, name, serviceName, stype,
            attrMap, orgName, univDN);
    }


    /**

     * Removes attributes value related to a specific service by
     * setting it to empty.
     * The assumption is that the service is already assigned to
     * the identity. The attributes for the service are validated
     * against the service schema.
     *
     * This method is only valid for <AMIdentity> object of type User.
     *
     * @param serviceName Name of the service.
     * @param attrNames Set of attributes name.
     * @throws IdRepoException If there are repository related error conditions.
     * @throws SSOException If user's single sign on token is invalid.
     * @supported.api
     */
    public void removeServiceAttributes(String serviceName, Set attrNames)
        throws IdRepoException, SSOException {
        Map attrMap = new HashMap(attrNames.size() *2);
        Iterator it = attrNames.iterator();
        while (it.hasNext()) {
            String attrName = (String) it.next();
            attrMap.put(attrName, Collections.EMPTY_SET);
        }
        modifyService(serviceName, attrMap);
    }


    // MEMBERSHIP RELATED APIS
    /**
     * Verifies if this identity is a member of the identity being passed.
     *
     * This method is only valid for AMIdentity objects of type Role, Group and
     * User.
     *
     * @param identity
     *            <code>AMIdentity</code> to check membership with
     * @return true if this Identity is a member of the given Identity
     * @throws IdRepoException
     *             if there are repository related error conditions.
     * @throws SSOException
     *             if user's single sign on token is invalid.
     * @supported.api
     */
    public boolean isMember(AMIdentity identity) throws IdRepoException,
            SSOException {
        boolean ismember = false;
        IdRepoException idException = null;
        IdServices idServices = IdServicesFactory.getDataStoreServices();
        try {
            //This method should always retrieve all the membership information a user could possibly have (either
            //through the user when memberOf attribute is defined, or through the group using uniquemember attribute),
            //hence there is no need to try to look up the group and query its members to see if this given identity
            //is in that list.
            //Generally speaking, this should be the case for every IdRepo implementation -> when we ask for the user
            //memberships, we should always get all of them for the sake of consistency.
            Set members = idServices.getMemberships(token, getType(),
                    getName(), identity.getType(), orgName, getDN());
            if (members != null && members.contains(identity)) {
                ismember = true;
            } else if (members != null) {
                // Check for fully qualified names or
                // if AM SDK DNs for these identities match
                String dn = identity.getDN();
                Iterator it = members.iterator();
                while (it.hasNext()) {
                    AMIdentity id = (AMIdentity) it.next();
                    if (identity.equals(id)) {
                        ismember = true;
                        break;
                    } else if (dn != null) {
                        String mdn = id.getDN();
                        if ((mdn != null) && mdn.equalsIgnoreCase(dn)) {
                            ismember = true;
                            break;
                        }
                    }
                }
            }

            // If membership is still false, check only the UUID
            // without the amsdkdn
            if (!ismember && members != null && !members.isEmpty()) {
                // Get UUID without amsdkdn for "membership" identity
                String identityDN = identity.getUniversalId();
                String amsdkdn = identity.getDN();
                if ((amsdkdn != null) &&
                    (identityDN.toLowerCase().indexOf(",amsdkdn=") != -1)) {
                    identityDN = identityDN.substring(0, identityDN
                            .indexOf(amsdkdn) - 9);
                }
                // Get UUID without amsdkdn for users memberships
                Iterator it = members.iterator();
                while (it.hasNext()) {
                    AMIdentity id = (AMIdentity) it.next();
                    String idDN = id.getUniversalId();
                    String mdn = id.getDN();
                    if (mdn != null) {
                        int endIdx = idDN.indexOf(mdn) - 9;
                        if (endIdx >= 0) {
                            idDN = idDN.substring(0, endIdx);
                        }
                    }
                    if (idDN.equalsIgnoreCase(identityDN)) {
                        ismember = true;
                        break;
                    }
                }
            }

        } catch (IdRepoException ide) {
            // Save the exception to be used later
            idException = ide;
        }

        if (idException != null) {
            throw (idException);
        }
        return ismember;
    }

    /**
     * @supported.api
     *
     * If membership is supported then add the new identity as a member.
     *
     * @param identity
     *            AMIdentity to be added
     * @throws IdRepoException
     *             if there are repository related error conditions.
     * @throws SSOException
     *             if user's single sign on token is invalid. non-public methods
     */
    public void addMember(AMIdentity identity) throws IdRepoException,
            SSOException {
        IdServices idServices = IdServicesFactory.getDataStoreServices();
        Set members = new HashSet();
        members.add(identity.getName());
        idServices.modifyMemberShip(token, type, name, members, identity
                .getType(), IdRepo.ADDMEMBER, orgName);
    }

    /**
     * @supported.api
     *
     * Removes the identity from this identity's membership.
     *
     * @param identity
     *            AMIdentity to be removed from membership.
     * @throws IdRepoException
     *             if there are repository related error conditions.
     * @throws SSOException
     *             if user's single sign on token is invalid. non-public methods
     */
    public void removeMember(AMIdentity identity) throws IdRepoException,
            SSOException {
        IdServices idServices = IdServicesFactory.getDataStoreServices();
        Set members = new HashSet();
        members.add(identity.getName());
        idServices.modifyMemberShip(token, type, name, members, identity
                .getType(), IdRepo.REMOVEMEMBER, orgName);
    }

    /**
     * @supported.api
     *
     * Removes the identities from this identity's membership.
     *
     * @param identityObjects
     *            Set of AMIdentity objects
     * @throws IdRepoException
     *             if there are repository related error conditions.
     * @throws SSOException
     *             if user's single sign on token is invalid. non-public methods
     */
    public void removeMembers(Set identityObjects) throws IdRepoException,
            SSOException {
        IdServices idServices = IdServicesFactory.getDataStoreServices();
        Set members = new HashSet();
        Iterator it = identityObjects.iterator();

        while (it.hasNext()) {
            AMIdentity identity = (AMIdentity) it.next();
            members.add(identity.getName());
            idServices.modifyMemberShip(token, type, name, members, identity
                    .getType(), IdRepo.REMOVEMEMBER, orgName);
            members = new HashSet();
        }
    }

    /**
     * Return all members of a given identity type of this identity as a Set of
     * AMIdentity objects.
     *
     * This method is only valid for AMIdentity objects of type Group and User.
     *
     * @param mtype
     *            Type of identity objects
     * @return Set of AMIdentity objects that are members of this object.
     * @throws IdRepoException
     *             if there are repository related error conditions.
     * @throws SSOException
     *             if user's single sign on token is invalid.
     * @supported.api
     */
    public Set getMembers(IdType mtype) throws IdRepoException, SSOException {
        IdServices idServices = IdServicesFactory.getDataStoreServices();
        return idServices
                .getMembers(token, type, name, orgName, mtype, getDN());
    }

    /**
     * Returns the set of identities that this identity belongs to.
     *
     * This method is only valid for AMIdentity objects of type User and Role.
     *
     * @param mtype
     *            Type of member identity.
     * @return Set of AMIdentity objects of the given type that this identity
     *         belongs to.
     * @throws IdRepoException
     *             if there are repository related error conditions.
     * @throws SSOException
     *             if user's single sign on token is invalid.
     * @supported.api
     */
    public Set getMemberships(IdType mtype) throws IdRepoException,
            SSOException {
        IdServices idServices = IdServicesFactory.getDataStoreServices();
        return idServices.getMemberships(token, type, name, mtype, orgName,
                getDN());
    }

    /**
     * This method determines if the identity exists and returns true or false.
     *
     * This method is only valid for AMIdentity objects of type User and Agent.
     *
     * @return true if the identity exists or false otherwise.
     * @throws IdRepoException
     *             If there are repository related error conditions.
     * @throws SSOException
     *             If user's single sign on token is invalid.
     * @supported.api
     */
    public boolean isExists() throws IdRepoException, SSOException {
        IdServices idServices = IdServicesFactory.getDataStoreServices();
        return idServices.isExists(token, type, name, orgName);
    }

    /**
     * Returns <code>true</code> if the given object is equal to this object.
     *
     * @param o Object for comparison.
     * @return <code>true</code> if the given object is equal to this object.
     * @supported.api
     */
    @Override
    public boolean equals(Object o) {
        boolean isEqual = false;
        if (o instanceof AMIdentity) {
            AMIdentity compareTo = (AMIdentity) o;
            if (univIdWithoutDN.equalsIgnoreCase(
                compareTo.univIdWithoutDN)) {
                isEqual = true;
            } else if (univDN != null) {
                // check if the amsdkdn match
                String dn = compareTo.getDN();
                if (dn != null && dn.equalsIgnoreCase(univDN)) {
                    isEqual = true;
                }
            }

            if (!isEqual && !type.equals(IdType.REALM) &&
                type.equals(compareTo.getType())) {
                // Check fully qualified names
                Set sfqn = getFullyQualifiedNames();
                Set cfqn = compareTo.getFullyQualifiedNames();
                if ((sfqn != null) && (cfqn != null) &&
                    !sfqn.isEmpty() && !cfqn.isEmpty()) {
                    for (Iterator items = sfqn.iterator();
                        items.hasNext();) {
                        String next = (String)items.next();
                        if (next != null && cfqn.contains(next)) {
                            isEqual = true;
                            break;
                        }
                    }
                }
            }
        }
        return (isEqual);
    }

    /**
     * Non-javadoc, non-public methods
     */
    @Override
    public int hashCode() {
        return (univIdWithoutDN.toLowerCase().hashCode());
    }

    /**
     * Nonjavadoc, non-public methods
     *
     */
    public void setDN(String dn) {
        univDN = dn;
    }

    /**
     * Returns universal distinguished name of this object.
     *
     * @return universal distinguished name of this object.
     */
    public String getDN() {
        return univDN;
    }

    /**
     * Returns the universal identifier of this object.
     *
     * @return String representing the universal identifier of this object.
     * @supported.api
     */
    public String getUniversalId() {
        return univIdWithoutDN;
    }

    /**
     * Returns String representation of the <code>AMIdentity</code>
     * object. It returns universal identifier, orgname, type, etc.
     *
     * @return String representation of the <code>ServiceConfig</code> object.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(100);
        sb.append("AMIdentity object: ").append(univIdWithoutDN);
        if (univDN != null) {
            sb.append("AMSDKDN=").append(univDN);
        }
        return (sb.toString());
    }

    // Returns a set of fully qulified names, as returned by DataStores
    protected Set getFullyQualifiedNames() {
        if (fullyQualifiedNames == null) {
            try {
                IdServices idServices =
                    IdServicesFactory.getDataStoreServices();
                fullyQualifiedNames = idServices.getFullyQualifiedNames(
                    token, type, name, orgName);
            } catch (IdRepoException ire) {
                if (debug.messageEnabled()) {
                    debug.message("AMIdentity:getFullyQualifiedNames: " +
                        "got exception: ", ire);
                }
            } catch (SSOException ssoe) {
                if (debug.messageEnabled()) {
                    debug.message("AMIdentity:getFullyQualifiedNames: " +
                        "got exception: ", ssoe);
                }
            }
        }
        return (fullyQualifiedNames);
    }

    private Set getServiceOCs(SSOToken token, String serviceName)
            throws SSOException {
        Set result = new HashSet();
        try {
            if (serviceHasSubSchema(token, serviceName, SchemaType.GLOBAL)) {
                Map attrs = getServiceConfig(token, serviceName,
                        SchemaType.GLOBAL);
                Set vals = (Set) attrs.get("serviceObjectClasses");

                if (vals != null) {
                    result.addAll(vals);
                }
            }
        } catch (SMSException smsex) {
        }

        return result;
    }

    /**
     * Get service default config from SMS
     *
     * @param token
     *            SSOToken a valid SSOToken
     * @param serviceName
     *            the service name
     * @param schemaType
     *            service schema type (Dynamic, Policy etc)
     * @return returns a Map of Default Configuration values for the specified
     *         service.
     */
    private Map getServiceConfig(SSOToken token, String serviceName,
            SchemaType type) throws SMSException, SSOException {
        Map attrMap = null; // Map of attribute/value pairs
        if (type != SchemaType.POLICY) {
            ServiceSchemaManager scm = new ServiceSchemaManager(serviceName,
                    token);
            ServiceSchema gsc = scm.getSchema(type);
            attrMap = gsc.getAttributeDefaults();
        }
        return attrMap;
    }

    /**
     * Returns true if the service has the subSchema. False otherwise.
     *
     * @param token
     *            SSOToken a valid SSOToken
     * @param serviceName
     *            the service name
     * @param schemaType
     *            service schema type (Dynamic, Policy etc)
     * @return true if the service has the subSchema.
     */
    private boolean serviceHasSubSchema(SSOToken token, String serviceName,
            SchemaType schemaType) throws SMSException, SSOException {
        boolean schemaTypeFlg = false;
        try {
            ServiceSchemaManager ssm = new ServiceSchemaManager(serviceName,
                    token);
            Set types = ssm.getSchemaTypes();
            if (debug.messageEnabled()) {
                debug.message("AMServiceUtils.serviceHasSubSchema() "
                        + "SchemaTypes types for " + serviceName + " are: "
                        + types);
            }
            schemaTypeFlg = types.contains(schemaType);
        } catch (ServiceNotFoundException ex) {
            if (debug.warningEnabled()) {
                debug.warning("AMServiceUtils.serviceHasSubSchema() "
                        + "Service does not exist : " + serviceName);
            }
        }
        return (schemaTypeFlg);
    }

    private Set getServiceAttributesName(String serviceName)
        throws IdRepoException, SSOException {
        Set attrNames = Collections.EMPTY_SET;

        try {
            // Get attribute names for USER type only, so plugin knows
            // what attributes to remove.
            attrNames = new HashSet();
            ServiceSchemaManager ssm = new ServiceSchemaManager(
                serviceName, token);
            ServiceSchema uss = ssm.getSchema(type.getName());

            if (uss != null) {
                attrNames = uss.getAttributeSchemaNames();
            }
            // If the identity type is not of role, filteredrole or
            // realm, need to add dynamic attributes also
            if (!(type.equals(IdType.ROLE) || type.equals(IdType.REALM) ||
                type.equals(IdType.FILTEREDROLE))) {
                uss = ssm.getDynamicSchema();
                if (uss != null) {
                    if (attrNames == Collections.EMPTY_SET) {
                        attrNames = uss.getAttributeSchemaNames();
                    } else {
                        attrNames.addAll(uss.getAttributeSchemaNames());
                    }
                }
            } else {
                // Add COS priority attribute
                attrNames.add(COS_PRIORITY);
            }
        } catch (SMSException smse) {
            if (debug.messageEnabled()) {
                debug.message(
                    "AMIdentity.getServiceAttributes: Caught SM exception",
                    smse);
            }
            // just returned whatever we find or empty set
            // if services is not found.
        }

        return attrNames;
    }

    private static Debug debug = Debug.getInstance("amIdm");

    public static String COS_PRIORITY = "cospriority";
}

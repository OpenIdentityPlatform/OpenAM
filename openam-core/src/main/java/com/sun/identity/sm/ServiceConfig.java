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
 * $Id: ServiceConfig.java,v 1.18 2009/01/28 05:35:03 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted 2011-2013 ForgeRock Inc
 * Portions Copyrighted 2012 Open Source Solution Technology Corporation
 */
package com.sun.identity.sm;

import com.iplanet.services.util.AMEncryption;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.ums.IUMSConstants;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import com.sun.identity.shared.ldap.util.DN;

/**
 * The class <code>ServiceConfig</code> provides interfaces to manage the
 * configuration information of a service configuration. It provides methods to
 * get and set configuration parameters for this service configuration.
 *
 * @supported.all.api
 */
public class ServiceConfig {
    // Instance variables
    private SSOToken token;

    private ServiceConfigImpl sc;

    private ServiceSchemaImpl ss;

    private ServiceConfigManager scm;

    /**
     * Default constructor. Makes it private so that it can not be instantiated.
     */
    private ServiceConfig() {
        // hence can not be instantiated
    }

    /**
     * Protected constructor
     */
    protected ServiceConfig(ServiceConfigManager scm, ServiceConfigImpl sc)
            throws SMSException, SSOException {
        this.scm = scm;
        token = scm.getSSOToken();
        this.sc = sc;
        this.ss = sc.getServiceSchemaImpl();
    }

    /**
     * Returns the name of this service configuration.
     * 
     * @return the name of this service configuration
     */
    public String getServiceName() {
        return (scm.getName());
    }

    /**
     * Returns the service version
     * 
     * @return service version
     */
    public String getVersion() {
        return (scm.getVersion());
    }

    /**
     * Returns the service component name. It is "/" separated and the root
     * component name is "/".
     * 
     * @return service component name
     */
    public String getComponentName() {
        validate();
        return (sc.getComponentName());
    }

    /**
     * Returns the service component's schema ID. For global and organization's
     * root configurations it returns an empty string.
     * 
     * @return service component's schema ID
     */
    public String getSchemaID() {
        validate();
        return (sc.getSchemaID());
    }

    /**
     * Returns the priority assigned to the service configuration.
     * 
     * @return the priority assigned to the service configuration
     */
    public int getPriority() {
        validate();
        return (sc.getPriority());
    }

    /**
     * Sets the priority to the service configuration.
     * 
     * @param priority
     *            the priority to be assigned to the configuration
     * @throws SMSException
     *             if there is an error occurred while performing the operation
     * @throws SSOException
     *             if the user's single sign-on is invalid or expired
     */
    public void setPriority(int priority) throws SSOException, SMSException {
        validateServiceConfigImpl();
        StringBuilder sb = new StringBuilder(8);
        String[] priorities = { sb.append(priority).toString() };
        SMSEntry e = sc.getSMSEntry();
        e.setAttribute(SMSEntry.ATTR_PRIORITY, priorities);
        saveSMSEntry(e);
    }

    /**
     * Returns the labeled uri assigned to the service configuration.
     * 
     * @return the labeled uri assigned to the service configuration
     */
    public String getLabeledUri() {
        validate();
        return (sc.getLabeledUri());
    }

    /**
     * Sets the labeled uri to the service configuration.
     * 
     * @param luri the labeled uri to be assigned to the configuration
     * @throws SMSException
     *             if there is an error occurred while performing the operation
     * @throws SSOException
     *             if the user's single sign-on is invalid or expired
     */
    public void setLabeledUri(String luri) throws SSOException, SMSException {
        validateServiceConfigImpl();
        StringBuilder sb = new StringBuilder(8);
        String[] lUris = { sb.append(luri).toString() };
        SMSEntry e = sc.getSMSEntry();
        e.setAttribute(SMSEntry.ATTR_LABELED_URI, lUris);
        saveSMSEntry(e);
    }

    /**
     * delete the labeled uri to the service configuration.
     * 
     * @param luri the labeled uri to be assigned to the configuration
     * @throws SMSException
     *             if there is an error occurred while performing the operation
     * @throws SSOException
     *             if the user's single sign-on is invalid or expired
     */
    public void deleteLabeledUri(String luri) throws SSOException, SMSException {
        validateServiceConfigImpl();
        SMSEntry e = sc.getSMSEntry();
        sc.setLabeledUri(null);
        e.removeAttribute(SMSEntry.ATTR_LABELED_URI, luri);
        saveSMSEntry(e);
    }

    /**
     * Returns the names of all service's sub-configurations.
     * 
     * @return set of names of all service's sub-configurations
     * @throws SMSException
     *             if there is an error accessing the data store
     */
    public Set getSubConfigNames() throws SMSException {
        validateServiceConfigImpl();
        try {
            return (sc.getSubConfigNames(token));
        } catch (SSOException s) {
            SMSEntry.debug.error("ServiceConfig: Unable to "
                    + "get subConfig Names", s);
        }
        return (Collections.EMPTY_SET);
    }

    /**
     * Method to get names of service's sub-configurations that match the given
     * pattern.
     * 
     * @param pattern
     *            pattern to match for sub-configuration names
     * @return names of the service sub-configuration
     * @throws SMSException
     *             if an error occurred while performing the operation.
     */
    public Set getSubConfigNames(String pattern) throws SMSException {
        validateServiceConfigImpl();
        try {
            return (sc.getSubConfigNames(token, pattern));
        } catch (SSOException s) {
            SMSEntry.debug.error("ServiceConfigManager: Unable to "
                    + "get subConfig Names for filter: " + pattern, s);
        }
        return (Collections.EMPTY_SET);

    }

    /**
     * Method to get names of service's sub-configurations that match the given
     * pattern and belongs to the specified service schema name.
     * 
     * @param pattern
     *            pattern to match for other entities.
     * @param schemaName
     *            service schema name.
     * @return names of the service sub-configuration
     * @throws SMSException
     *             if an error occurred while performing the operation.
     */
    public Set getSubConfigNames(String pattern, String schemaName)
            throws SMSException {
        validateServiceConfigImpl();
        try {
            return (sc.getSubConfigNames(token, pattern, schemaName));
        } catch (SSOException s) {
            SMSEntry.debug.error("ServiceConfigManager: Unable to "
                    + "get subConfig Names for filters: " + pattern + "AND"
                    + schemaName, s);
        }
        return (Collections.EMPTY_SET);

    }

    /**
     * Returns a set of exported fully qualified sub-configuration names that
     * can be imported used locally as service configuration
     * 
     * @param serviceId
     *            service schema identifier
     * @return names of fully qualified applicable service sub-configurations
     * @throws SMSException
     *             if an error occurred while performing the operation.
     */
    public Set getExportedSubConfigNames(String serviceId) throws SMSException {
        return (null);
    }

    /**
     * Returns the service's sub-configuration given the service's
     * sub-configuration name.
     * 
     * @param subConfigName
     *            The name of the service's sub-configuration to retrieve.
     * @return The <code>ServiceConfig</code> object corresponding to the
     *         specified name of the service's sub-configuration.
     * @throws SMSException
     *             if there is an error occurred while performing the operation
     * @throws SSOException
     *             if the user's single sign-on is invalid or expired
     */
    public ServiceConfig getSubConfig(String subConfigName)
            throws SSOException, SMSException {
        validateServiceConfigImpl();
        ServiceConfigImpl sci = sc.getSubConfig(token, subConfigName);
        return ((sci == null) ? null : new ServiceConfig(scm, sci));
    }

    /**
     * Adds a service sub-configuration with configuration parameters.
     * 
     * @param subConfigName
     *            the name of service sub-configuration to add
     * @param subConfigId
     *            type of service sub-configuration
     * @param priority
     *            the priority of the configuration
     * @param attrs
     *            configuration parameters for the sub-configuration
     * @throws SMSException
     *             if there is an error occurred while performing the operation
     * @throws SSOException
     *             if the user's single sign-on is invalid or expired
     */
    public void addSubConfig(String subConfigName, String subConfigId,
            int priority, Map attrs) throws SMSException, SSOException {
        validateServiceConfigImpl();
        // Check if this entry exists
        if (sc.isNewEntry()) {
            // Ideally these nodes should have been created, since they
            // are not present we need to create them
            scm.createOrganizationConfig(sc.getOrganizationName(), null);
            // Check if rest of the component names are present
            checkAndCreateComponents(sc.getDN());
        }

        // Get service schemas
        ServiceSchemaImpl nss = null;
        if (subConfigId != null) {
            nss = ss.getSubSchema(subConfigId);
        } else {
            nss = ss.getSubSchema(subConfigName);
        }
        if (nss == null) {
            String[] args = { subConfigName };
            throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    "sms-invalid-add-sub-config", args));
        }

        if (!nss.supportsMultipleConfigurations()
                && !getSubConfigNames().isEmpty()) {
            String[] args = { subConfigName };
            throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    "sms-invalid-add-sub-config", args));
        }

        // Convert priority to string
        StringBuilder sb = new StringBuilder(8);
        sb.append(priority);

        // Create the entry
        CreateServiceConfig.createSubConfigEntry(token, ("ou=" + subConfigName
                + "," + sc.getDN()), nss, subConfigId, sb.toString(),
                SMSUtils.copyAttributes(attrs), sc.getOrganizationName());
    }

    /**
     * Removes the service sub-configuration.
     * 
     * @param subConfigName
     *            name of service sub-configuration to remove
     * @throws SMSException
     *             if there is an error occurred while performing the operation
     * @throws SSOException
     *             if the user's single sign-on is invalid or expired
     */
    public void removeSubConfig(String subConfigName) throws SMSException,
            SSOException {
        validateServiceConfigImpl();
        // Obtain the SMSEntry for the subconfig and delete it
        // unescape in case users provide such a subConfigName for deletion.
        // "http:&amp;#47;&amp;#47;abc.east.sun.com:58080"

        subConfigName = SMSSchema.unescapeName(subConfigName);

        // First remove the entry from ServiceConfigImpl Cache.

        // Construct subconfig DN
        String sdn = "ou=" + subConfigName + "," + sc.getDN();

        // Construct ServiceConfigManagerImpl
        ServiceConfigManagerImpl scmImpl = ServiceConfigManagerImpl.
            getInstance(token, getServiceName(), getVersion());

        // Construct ServiceConfigImpl of the removed subconfig.
        ServiceConfigImpl sConfigImpl =
            sc.getSubConfig(token, subConfigName);
        
        // Call ServiceConfigImpl's deleteInstance() to remove from cache.
        if (sConfigImpl != null) {
            ServiceConfigImpl.deleteInstance(token, scmImpl, null, sdn, "/", 
                sConfigImpl.getGroupName(), (getComponentName() + "/" 
                + SMSSchema.escapeSpecialCharacters(subConfigName)), false, 
                ss);
        }
        // Remove this entry from smsentry.
        CachedSMSEntry cEntry = CachedSMSEntry.getInstance(token, sdn);
        if (cEntry.isDirty()) {
            cEntry.refresh();
        }
        SMSEntry entry = cEntry.getClonedSMSEntry();
        entry.delete(token);
        cEntry.refresh(entry);

        // Remove the entry from CachedSubEntries
        CachedSubEntries cse = CachedSubEntries.getInstance(token, sc.getDN());
        cse.remove(subConfigName);
    }

    /**
     * Imports a service sub-configuration to the list of localy defined
     * sub-configuration. The imported sub-configuration name must be fully
     * qualified, as obtained from <code>getExportedSubConfigNames</code>.
     * 
     * @param subConfigName
     *            the name of service sub-configuration to add locally
     * @param exportedSubConfigName
     *            the fully qualified name of the exported sub-configuration
     *            name
     * @throws SMSException
     *             if there is an error occurred while performing the operation
     * @throws SSOException
     *             if the user's single sign-on is invalid or expired
     */
    public void importSubConfig(String subConfigName,
            String exportedSubConfigName) throws SMSException, SSOException {
    }

    /**
     * Returns the service configuration parameters. The keys in the
     * <code>Map</code> contains the attribute names and their corresponding
     * values in the <code>Map</code> is a <code>Set</code> that contains
     * the values for the attribute. This method picks up the default values for
     * any attributes not defined in the <code>ServiceConfig</code>. The
     * default values for these attributes are picked up from the Service
     * Schema. If there is no default value defined, then this method will still
     * return the attribute-value pair, except that the Set will be a
     * Collections.EMPTY_SET. This is distinct from an empty Set with no entries
     * in it. AN empty set represents an attribute whose value has been set to
     * an empty value by the application using the <code>setAttributes()</code>
     * method.
     * 
     * @return the <code>Map</code> where key is the attribute name and value
     *         is the <code>Set</code> of attribute values
     */
    public Map getAttributes() {
        validate();
        return (sc.getAttributes());
    }

        /**
     * Returns the service configuration parameters for read only.
     * The keys in the <code>Map</code> contains the attribute names and
     * their corresponding values in the <code>Map</code> is a
     * <code>Set</code> that contains the values for the attribute.
     */

    /**
     * Returns the service configuration parameters without inheriting the
     * default values from service's schema. The keys in the <code>Map</code>
     * contains the attribute names and their corresponding values in the
     * <code>Map</code> is a <code>Set</code> that contains the values for
     * the attribute.
     */
    public Map getAttributesWithoutDefaults() {
        validate();
        return (sc.getAttributesWithoutDefaults());
    }
    
    /**
     * Returns the service configuration parameters for read only,
     * modification cannot be performed on the return <code>Map</code>.
     * The keys in the
     * <code>Map</code> contains the attribute names and their
     * corresponding values in the <code>Map</code> is a
     * <code>Set</code> that contains the values for the attribute.
     * This method picks up the default values for any attributes
     * not defined in the <code>ServiceConfig</code>. The default values for
     * these attributes are picked up from the Service Schema.
     * If there is no default value defined, then this method
     * will still return the attribute-value pair, except that
     * the Set will be a Collections.EMPTY_SET.
     * This is distinct from an empty Set with no entries in it.
     * AN empty set represents an attribute whose value has
     * been set to an empty value by the application using
     * the <code>setAttributes()</code> method.
     * 
     * @return the <code>Map</code> where key is the attribute name
     *	 and value is the <code>Set</code> of attribute values
     */
    public Map getAttributesForRead() {
        validate();
	return (sc.getAttributesForRead());
    }

    /**
     * Returns the service configuration parameters for read only without
     * inheriting the default values from service's schema. The keys
     * in the  <code>Map</code> contains the attribute names and their
     * corresponding values in the <code>Map</code> is a
     * <code>Set</code> that contains the values for the attribute.
     */
    public Map getAttributesWithoutDefaultsForRead() {
        validate();
        return (sc.getAttributesWithoutDefaultsForRead());
    }

    /**
     * Sets the service configuration parameters. The keys in the
     * <code>Map</code> contains the attribute names and their corresponding
     * values in the <code>Map</code> is a <code>Set</code> that contains
     * the values for the attribute. This method will replace the existing
     * attribute values with the given one. For attributes that are not
     * specified in <code>attrs</code>, it will not be modified.
     * 
     * @param attrs
     *            the <code>Map</code> where key is the attribute name and
     *            value is the <code>Set</code> of attribute values
     * @throws SMSException
     *             if there is an error occurred while performing the operation
     * @throws SSOException
     *             if the user's single sign-on is invalid or expired
     */
    public void setAttributes(Map attrs) throws SMSException, SSOException {
        validateServiceConfigImpl();
        Map oldAttrs = sc.getAttributesWithoutDefaults();
        Iterator it = oldAttrs.keySet().iterator();
        Map newAttrs = SMSUtils.copyAttributes(attrs);
        while (it.hasNext()) {
            String s = (String) it.next();
            if (!newAttrs.containsKey(s)) {
                newAttrs.put(s, oldAttrs.get(s));
            }
        }
        /*
         * For validation using ChoiceValues plugin we need to pass in
         * OrganizationName, since the plugins use organization names to compute
         * the choice values
         */
        ss.validateAttributes(token, newAttrs, true, sc.getOrganizationName());
        SMSEntry e = sc.getSMSEntry();
        SMSUtils.setAttributeValuePairs(e, newAttrs, ss
                .getSearchableAttributeNames());
        saveSMSEntry(e);
    }

    /**
     * Adds a configuration parameter to the service configuration.
     * 
     * @param attrName
     *            the name of the attribute to add
     * @param values
     *            the set of values to add
     * @throws SMSException
     *             if there is an error occurred while performing the operation
     * @throws SSOException
     *             if the user's single sign-on is invalid or expired
     */
    public void addAttribute(String attrName, Set values) throws SMSException,
            SSOException {
        validateServiceConfigImpl();
        // Get current attributes
        Map attributes = getAttributes();
        // Validate attribute values
        Set newVals = values;
        Set oldVals = (Set) attributes.get(attrName);
        if (oldVals != null) {
            newVals = new HashSet();
            newVals.addAll(values);
            newVals.addAll(oldVals);
        }
        ss
                .validateAttrValues(token, attrName, newVals, true, sc
                        .getOrganizationName());
        // Store the entry
        SMSEntry e = sc.getSMSEntry();
        SMSUtils.addAttribute(e, attrName, values, ss
                .getSearchableAttributeNames());
        saveSMSEntry(e);
    }

    /**
     * Removes a configuration parameter from the service configuration.
     * 
     * @param attrName
     *            the name of the attribute to remove
     * @throws SMSException
     *             if there is an error occurred while performing the operation
     * @throws SSOException
     *             if the user's single sign-on is invalid or expired
     */
    public void removeAttribute(String attrName) throws SMSException,
            SSOException {
        validateServiceConfigImpl();
        SMSEntry e = sc.getSMSEntry();
        SMSUtils.removeAttribute(e, attrName);
        saveSMSEntry(e);
    }

    /**
     * Removes a configuration parameters from the service configuration.
     * 
     * @param attrNames
     *            <code>Set</code> of attribute names to remove
     * @throws SMSException
     *             if there is an error occurred while performing the operation
     * @throws SSOException
     *             if the user's single sign-on is invalid or expired
     */
    public void removeAttributes(Set attrNames) throws SMSException,
            SSOException {
        validateServiceConfigImpl();
        SMSEntry e = sc.getSMSEntry();
        if (attrNames != null && !attrNames.isEmpty()) {
            for (Iterator items = attrNames.iterator(); items.hasNext();) {
                SMSUtils.removeAttribute(e, (String) items.next());
            }
            saveSMSEntry(e);
        }
    }

    /**
     * Removes the specific values for the given configuration parameter.
     * 
     * @param attrName
     *            the name of the attribute
     * @param values
     *            set of attribute values to remove from the given attribute
     * @throws SMSException
     *             if there is an error occurred while performing the operation
     * @throws SSOException
     *             if the user's single sign-on is invalid or expired
     */
    public void removeAttributeValues(String attrName, Set values)
            throws SMSException, SSOException {
        validateServiceConfigImpl();
        SMSEntry e = sc.getSMSEntry();
        SMSUtils.removeAttributeValues(e, attrName, values, ss
                .getSearchableAttributeNames());
        saveSMSEntry(e);
    }

    /**
     * Replaces old value of the configuration parameter with new value.
     * 
     * @param attrName
     *            the name of the attribute
     * @param oldValue
     *            the old value to remove from the attribute
     * @param newValue
     *            the new value to add to the attribute
     * @throws SMSException
     *             if there is an error occurred while performing the operation
     * @throws SSOException
     *             if the user's single sign-on is invalid or expired
     */
    public void replaceAttributeValue(String attrName, String oldValue,
            String newValue) throws SMSException, SSOException {
        validateServiceConfigImpl();
        // Get current attributes
        Map attributes = getAttributes();
        // Validate values

        Set currentValues = (Set) attributes.get(attrName);
        if (currentValues != null && !currentValues.contains(oldValue)) {
            throw (new SMSException("Current value doesn't match supplied value",
                    "sms-INVALID_PARAMETERS"));
        }

        Set newVals = new HashSet();
        Set oldVals = (Set) attributes.get(attrName);
        if (oldVals != null) {
            newVals.addAll(oldVals);
            newVals.remove(oldValue);
        }
        newVals.add(newValue);
        ss
                .validateAttrValues(token, attrName, newVals, true, sc
                        .getOrganizationName());
        // Store the entry
        SMSEntry e = sc.getSMSEntry();
        SMSUtils.replaceAttributeValue(e, attrName, oldValue, newValue, ss
                .getSearchableAttributeNames());
        saveSMSEntry(e);
    }

    /**
     * Replaces the old values of the configuration parameter with the new
     * values.
     * 
     * @param attrName
     *            the name of the attribute
     * @param oldValues
     *            the set of old values to remove from the attribute
     * @param newValues
     *            the set of new values to add to the attribute
     * @throws SMSException
     *             if there is an error occurred while performing the operation
     * @throws SSOException
     *             if the user's single sign-on is invalid or expired
     */
    public void replaceAttributeValues(String attrName, Set oldValues,
            Set newValues) throws SMSException, SSOException {
        validateServiceConfigImpl();
        // Get current attributes
        Map attributes = getAttributes();
        // Validate values
        Set newVals = new HashSet();
        Set oldVals = (Set) attributes.get(attrName);
        if (oldVals != null) {
            newVals.addAll(oldVals);
            newVals.removeAll(oldValues);
        }
        newVals.addAll(newValues);
        ss.validateAttrValues(token, attrName, newVals, true,
            sc.getOrganizationName());
        // Store the entry
        SMSEntry e = sc.getSMSEntry();
        SMSUtils.replaceAttributeValues(e, attrName, oldValues, newValues, ss
                .getSearchableAttributeNames());
        saveSMSEntry(e);
    }

    /**
     * Returns the LDAP DN represented by this <code>ServiceConfig</code>
     * object.
     * 
     * @return the LDAP DN represented by this <code>ServiceConfig</code>
     *         object.
     */
    public String getDN() {
        validate();
        return (sc.getDN());
    }

    /**
     * Returns the last modified time stamp of this configuration This method is
     * expensive because it does not cache the modified time stamp but goes
     * directly to the data store to obtain the value of this entry
     * 
     * @return The last modified time stamp as a string with the format of
     *         <code> yyyyMMddhhmmss </code>
     * @throws SMSException
     *             if there is an error trying to read from the data store
     * @throws SSOException
     *             if the single sign-on token of the user is invalid.
     */

    public String getLastModifiedTime() throws SMSException, SSOException {
        validateServiceConfigImpl();
        SMSEntry e = sc.getSMSEntry();
        String vals[] = e.getAttributeValues(SMSEntry.ATTR_MODIFY_TIMESTAMP,
                true);
        String mTS = null;
        if (vals != null) {
            mTS = vals[0];
        }
        return mTS;
    }

    /**
     * Returns the organization names to which the service configuration is
     * being exported. The organization names would be fully qualified starting
     * with a forward slash "/". To specify an entire sub-tree that can use the
     * service configuration, a "*" would have to be appended after the final
     * forward slash. For example "/a/b/c/*" would imply all sub-organization
     * under "/a/b/c" can use this service configuration. Exporting implies
     * privileges to read the service configuration data, but not to modify or
     * delete.
     * 
     * @return names of organizations to which service configuration
     *         configuration is exported
     */
    public Set getExportedOrganizationNames() {
        return (null);
    }

    /**
     * Sets the organization names that can import the service configuration.
     * The organization names must be fully qualified, starting with a forward
     * slash "/". To specify an entire sub-tree that can use the service
     * configuration, a "*" would have to be appended after the final forward
     * slash. For example "/a/b/c/*" would imply all sub-organization under
     * "/a/b/c" can use this service configuration. Exporting implies privileges
     * to read the service configuration data and not to modify or delete.
     * 
     * @param names
     *            names of the organizations that can import the service
     *            configuration
     */
    public void setExportedOrganizationNames(Set names) throws SMSException,
            SSOException {
    }

    /**
     * Adds the organization names to the list of organization names that can
     * import this service configutation. If one does not exist it will be
     * created. The organization names must be fully qualified, starting with a
     * forward slash "/". To specify an entire sub-tree that can use the service
     * configuration, a "*" would have to be appended after the final forward
     * slash. For example "/a/b/c/*" would imply all sub-organization under
     * "/a/b/c" can use this service configuration. Exporting implies privileges
     * to read the service configuration data and not to modify or delete.
     * 
     * @param names
     *            names of the organizations that can import the service
     *            configuration
     */
    public void addExportedOrganizationNames(Set names) throws SMSException,
            SSOException {
    }

    /**
     * Removes the organization names from the list of organization names that
     * can import the service configuration. If the organization has already
     * imported the service configutation, it would have to be undone before the
     * organization name can be removed from the list. The organization names
     * must be fully qualified, starting with a forward slash "/". To specify an
     * entire sub-tree that can use the service configuration, a "*" would have
     * to be appended after the final forward slash. For example "/a/b/c/*"
     * would imply all sub-organization under "/a/b/c" can use this service
     * configuration.
     * 
     * @param names
     *            names of the organizations that will be removed from the list
     *            of organization names that can import the service
     *            configutation
     */
    public void removeSharedOrganizationNames(Set names) throws SMSException,
            SSOException {
    }

    /**
     * Returns String representation of the <code>ServiceConfig</code> object.
     * It returns attributes defined and sub configurations.
     * 
     * @return String representation of the <code>ServiceConfig</code> object.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // Print the attributes
        sb.append("Service Component name: " + getComponentName());
        sb.append("\n\tAttributes: " + getAttributes()).append("\n");

        // Try sub-configs
        try {
            Iterator subConfigNames = getSubConfigNames().iterator();
            while (subConfigNames.hasNext()) {
                ServiceConfig ssc = getSubConfig((String)subConfigNames.next());
                sb.append(ssc);
            }
        } catch (Exception e) {
            sb.append(e.getMessage());
        }
        return (sb.toString());
    }

    // Protected methods
    void saveSMSEntry(SMSEntry e) throws SMSException, SSOException {
        if (e.isNewEntry()) {
            // Check if base nodes exists
            CreateServiceConfig.checkBaseNodesForOrg(token, DNMapper
                    .orgNameToDN(sc.getOrganizationName()), getServiceName(),
                    getVersion());
            // Check if parent DN is present
            String parentDN = (new DN(e.getDN())).getParent().toString();
            checkAndCreateComponents(parentDN);
            // Add object classses to this entry
            e.addAttribute(SMSEntry.ATTR_OBJECTCLASS, SMSEntry.OC_TOP);
            e.addAttribute(SMSEntry.ATTR_OBJECTCLASS, SMSEntry.OC_SERVICE_COMP);
        }
        e.save(token);
        sc.refresh(e);
    }

    public void checkAndCreateGroup(String dn, String groupName) 
        throws SMSException, SSOException {

        CachedSMSEntry entry = CachedSMSEntry.getInstance(token, dn);
        if (entry.isDirty()) {
            entry.refresh();
        }
        if (entry.isNewEntry()) {
            // Check if parent exisits
            String pDN = (new DN(dn)).getParent().toString();
            CachedSMSEntry pEntry = CachedSMSEntry.getInstance(token, pDN);
            if (pEntry.isDirty()) {
                pEntry.refresh();
            }
            if (pEntry.isNewEntry()) {
                checkAndCreateComponents(pDN);
            }
            // Create this entry
            SMSEntry e = entry.getClonedSMSEntry();
            e.addAttribute(SMSEntry.ATTR_OBJECTCLASS, SMSEntry.OC_TOP);
            e.addAttribute(SMSEntry.ATTR_OBJECTCLASS,SMSEntry.OC_SERVICE_COMP);
            e.addAttribute(SMSEntry.ATTR_SERVICE_ID, groupName);
            e.save(token);
            entry.refresh(e);
        }
    }

    void checkAndCreateComponents(String dn) throws SMSException, SSOException {
        CachedSMSEntry entry = CachedSMSEntry.getInstance(token, dn);
        if (entry.isDirty()) {
            entry.refresh();
        }
        if (entry.isNewEntry()) {
            // Check if parent exisits
            String pDN = (new DN(dn)).getParent().toString();
            CachedSMSEntry pEntry = CachedSMSEntry.getInstance(token, pDN);
            if (pEntry.isDirty()) {
                pEntry.refresh();
            }
            if (pEntry.isNewEntry()) {
                checkAndCreateComponents(pDN);
            }
            // Create this entry
            SMSEntry e = entry.getClonedSMSEntry();
            e.addAttribute(SMSEntry.ATTR_OBJECTCLASS, SMSEntry.OC_TOP);
            e.addAttribute(SMSEntry.ATTR_OBJECTCLASS, SMSEntry.OC_SERVICE_COMP);
            e.save(token);
            entry.refresh(e);
        }
    }
    
    private void validate() {
        try {
            validateServiceConfigImpl();
        } catch (SMSException e) {
            // Ignore the exception
        }
    }
    
    private void validateServiceConfigImpl() throws SMSException {
        if (!sc.isValid()) {
            throw (new SMSException("service-config: " + sc.getDN() +
                " No loger valid. Cache has been cleared. Recreate from" +
                "ServiceConfigManager"));
        }
    }
    
    /**
     * Returns the status of this Service Configuration Object.
     * Must be used by classes that cache ServiceConfig.
     * 
     * @return <code>true</code> if this object is still valid.
     */
    public boolean isValid() {
        return (sc.isValid());
    }
    
    /**
     * Returns <code>true</code> if the entry exist
     */
    public boolean exists() {
    	return (!sc.isNewEntry());
    }
    
    public String toXML(String NodeTag, AMEncryption encryptObj)
        throws SMSException, SSOException {
        validateServiceConfigImpl();
        return sc.toXML(token, NodeTag, encryptObj);
    }

    public String toXML(String NodeTag, AMEncryption encryptObj, String orgName)
        throws SMSException, SSOException {
        validateServiceConfigImpl();
        return sc.toXML(token, NodeTag, encryptObj, orgName);
    }
}

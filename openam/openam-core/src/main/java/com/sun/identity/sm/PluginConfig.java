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
 * $Id: PluginConfig.java,v 1.5 2009/01/28 05:35:03 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.sm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.sun.identity.shared.ldap.util.DN;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;

/**
 * The class <code>PluginConfig</code> provides interfaces to manage the
 * plugin configuration information of a service. It provides methods to get and
 * set plugin configuration parameters for this service plugins.
 */
public class PluginConfig {
    // Instance variables
    private String name;

    private SSOToken token;

    private PluginConfigImpl pc;

    private PluginSchemaImpl ps;

    private ServiceConfigManager scm;

    /**
     * Default constructor. Makes it private so that it cannot be instantiated.
     */
    private PluginConfig() {
        // hence cannont be instantiated
    }

    /**
     * Protected constructor
     */
    protected PluginConfig(String name, ServiceConfigManager scm,
            PluginConfigImpl pc) throws SMSException, SSOException {
        this.scm = scm;
        token = scm.getSSOToken();
        this.pc = pc;
        this.ps = pc.getPluginSchemaImpl();
        this.name = name;
    }

    /**
     * Returns the name of this service plugin.
     * 
     * @return the name of this service plugin
     */
    public String getName() {
        return (name);
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
     * Returns the priority assigned to the service plugin.
     * 
     * @return the priority assigned to the service plugin
     */
    public int getPriority() {
        validate();
        return (pc.getPriority());
    }

    /**
     * Sets the priority to the service plugin.
     * 
     * @param priority
     *            the priority to be assigned to the plugin
     */
    public void setPriority(int priority) throws SSOException, SMSException {
        validatePluginConfig();
        StringBuilder sb = new StringBuilder(8);
        String[] priorities = { sb.append(priority).toString() };
        SMSEntry e = pc.getSMSEntry();
        e.setAttribute(SMSEntry.ATTR_PRIORITY, priorities);
        saveSMSEntry(e);
    }

    /**
     * Returns the service plugin parameters. The keys in the <code>Map</code>
     * contains the attribute names and their corresponding values in the
     * <code>Map</code> is a <code>Set</code> that contains the values for
     * the attribute.
     * 
     * @return the <code>Map</code> where key is the attribute name and value
     *         is the <code>Set</code> of attribute values
     */
    public Map getAttributes() {
        validate();
        return (pc.getAttributes());
    }

    /**
     * Sets the service plugin parameters. The keys in the <code>Map</code>
     * contains the attribute names and their corresponding values in the
     * <code>Map</code> is a <code>Set</code> that contains the values for
     * the attribute.
     * 
     * @param attrs
     *            the <code>Map</code> where key is the attribute name and
     *            value is the <code>Set</code> of attribute values
     * @throws SMSException
     */
    public void setAttributes(Map attrs) throws SMSException, SSOException {
        validatePluginConfig();
        Map newAttrs = new HashMap(attrs);
        Map oldAttrs = getAttributes();
        Iterator it = oldAttrs.keySet().iterator();
        while (it.hasNext()) {
            String s = (String) it.next();
            if (!newAttrs.containsKey(s))
                newAttrs.put(s, oldAttrs.get(s));
        }
        ps.validateAttributes(newAttrs, true);
        SMSEntry e = pc.getSMSEntry();
        SMSUtils.setAttributeValuePairs(e, newAttrs, ps
                .getSearchableAttributeNames());
        saveSMSEntry(e);
    }

    /**
     * Adds a configuration parameter to the service plugin.
     * 
     * @param attrName
     *            the name of the attribute to add
     * @param values
     *            the set of values to add
     * @throws SMSException
     */
    public void addAttribute(String attrName, Set values) throws SMSException,
            SSOException {
        validatePluginConfig();
        // Get current attributes
        Map attributes = getAttributes();
        // Validate attribute values
        Set newVals = values;
        Set oldVals = (Set) attributes.get(attrName);
        if (oldVals != null) {
            newVals = new HashSet();
            newVals.addAll(oldVals);
            newVals.addAll(values);
        }
        ps
                .validateAttrValues(token, attrName, newVals, true, pc
                        .getOrganizationName());
        // Store the entry
        SMSEntry e = pc.getSMSEntry();
        SMSUtils.addAttribute(e, attrName, values, ps
                .getSearchableAttributeNames());
        saveSMSEntry(e);
    }

    /**
     * Removes a configuration parameter from the service plugin.
     * 
     * @param attrName
     *            the name of the attribute to remove
     * @throws SMSException
     */
    public void removeAttribute(String attrName) throws SMSException,
            SSOException {
        validatePluginConfig();
        SMSEntry e = pc.getSMSEntry();
        SMSUtils.removeAttribute(e, attrName);
        saveSMSEntry(e);
    }

    /**
     * Removes the specific values for the given configuration plugin.
     * 
     * @param attrName
     *            the name of the attribute
     * @param values
     *            set of attribute values to remove from the given attribute
     * @throws SMSException
     */
    public void removeAttributeValues(String attrName, Set values)
            throws SMSException, SSOException {
        validatePluginConfig();
        SMSEntry e = pc.getSMSEntry();
        SMSUtils.removeAttributeValues(e, attrName, values, ps
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
     */
    public void replaceAttributeValue(String attrName, String oldValue,
            String newValue) throws SMSException, SSOException {
        validatePluginConfig();
        // Get current attributes
        Map attributes = getAttributes();
        // Validate values
        Set newVals = new HashSet();
        Set oldVals = (Set) attributes.get(attrName);
        if (oldVals != null) {
            newVals.addAll(oldVals);
            newVals.remove(oldValue);
        }
        newVals.add(newValue);
        ps
                .validateAttrValues(token, attrName, newVals, true, pc
                        .getOrganizationName());
        // Store the entry
        SMSEntry e = pc.getSMSEntry();
        SMSUtils.replaceAttributeValue(e, attrName, oldValue, newValue, ps
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
     */
    public void replaceAttributeValues(String attrName, Set oldValues,
            Set newValues) throws SMSException, SSOException {
        validatePluginConfig();
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
        ps
                .validateAttrValues(token, attrName, newVals, true, pc
                        .getOrganizationName());
        // Store the entry
        SMSEntry e = pc.getSMSEntry();
        SMSUtils.replaceAttributeValues(e, attrName, oldValues, newValues, ps
                .getSearchableAttributeNames());
        saveSMSEntry(e);
    }

    /**
     * Returns the LDAP DN represented by this plugin object.
     */
    public String getDN() {
        return (pc.getDN());
    }

    /**
     * Returns String representation of the plugin object. It returns attributes
     * defined and sub configurations.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // Print the attributes
        sb.append("Plugin name: ").append(getName());
        sb.append("\n\tAttributes: ").append(getAttributes()).append("\n");
        return (sb.toString());
    }

    // Protected methods
    void saveSMSEntry(SMSEntry e) throws SMSException, SSOException {
        if (e.isNewEntry()) {
            // Check if base nodes exists
            CreateServiceConfig.checkBaseNodesForOrg(token, DNMapper
                    .orgNameToDN(pc.getOrganizationName()), getServiceName(),
                    getVersion());
            // Add object classses
            e.addAttribute(SMSEntry.ATTR_OBJECTCLASS, SMSEntry.OC_TOP);
            e.addAttribute(SMSEntry.ATTR_OBJECTCLASS, SMSEntry.OC_SERVICE_COMP);
        }
        e.save(token);
        pc.refresh(e);
    }

    // Delete the plugin config
    void delete() throws SMSException, SSOException {
        SMSEntry e = pc.getSMSEntry();
        // First remove the entry from parent
        DN dn = new DN(e.getDN());
        CachedSubEntries cse = CachedSubEntries.getInstance(token, dn
                .getParent().toString());
        cse.remove(getName());
        // Remove this entry
        e.delete(token);
        pc.refresh(e);
    }
    
    // Validate PluginConfigImpl
    protected void validate() {
        try {
            validatePluginConfig();
        } catch (SMSException ex) {
            SMSEntry.debug.error("PluginSchema:validate exception", ex);
        }
    }
    
    protected void validatePluginConfig() throws SMSException {
        if (!pc.isValid()) {
            throw (new SMSException("plugin-config: " + name +
                " No loger valid. Cache has been cleared. Recreate from" +
                "ServiceSchemaManager"));
        }
    }
}

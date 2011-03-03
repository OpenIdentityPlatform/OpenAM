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
 * $Id: ServiceInstance.java,v 1.5 2008/07/11 01:46:21 arviranga Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.sm;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.ums.IUMSConstants;

/**
 * The class <code>ServiceInstance</code> provides methods to manage service's
 * instance variables.
 *
 * @supported.all.api
 */
public class ServiceInstance {
    // InstanceVariables
    private ServiceConfigManager scm;

    private SSOToken token;

    private ServiceInstanceImpl instance;

    // Protected constructor
    ServiceInstance(ServiceConfigManager scm, ServiceInstanceImpl i) {
        this.scm = scm;
        this.instance = i;
        this.token = scm.getSSOToken();
    }

    /**
     * Returns the instance name.
     * 
     * @return the instance name.
     */
    public String getName() {
        return (instance.getName());
    }

    /**
     * Returns the service name.
     * 
     * @return the service name.
     */
    public String getServiceName() {
        return (scm.getName());
    }

    /**
     * Returns the service version.
     * 
     * @return the service version.
     */
    public String getVersion() {
        return (scm.getVersion());
    }

    /**
     * Returns the group name from which the configuration parameters for the
     * instance must be obtained.
     * 
     * @return the group name from which the configuration parameters for the
     *         instance must be obtained.
     */
    public String getGroup() {
        return (instance.getGroup());
    }

    /**
     * Sets the group name for this instance.
     * 
     * @param groupName
     *            name of group.
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     * @throws SMSException
     *             if an error occurred while performing the operation
     */
    public void setGroup(String groupName) throws SSOException, SMSException {
        validateServiceInstance();
        if (!scm.containsGroup(groupName)) {
            String[] args = { groupName };
            throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    "sms-invalid-group-name", args));
        }
        String[] groups = { groupName };
        SMSEntry entry = instance.getSMSEntry();
        entry.setAttribute(SMSEntry.ATTR_SERVICE_ID, groups);
        entry.save(token);
        instance.refresh(entry);
    }

    /**
     * Returns the URL of the service. Will be <code>null</code> if the
     * service does not have an URI.
     * 
     * @return the URL of the service. Will be <code>null</code> if the
     *         service does not have an URI.
     */
    public String getURI() {
        validate();
        return (instance.getURI());
    }

    /**
     * Sets the URI for the service instance.
     * 
     * @param uri
     *            URI of the service instance.
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     * @throws SMSException
     *             if an error occurred while performing the operation
     */
    public void setURI(String uri) throws SSOException, SMSException {
        validateServiceInstance();
        String[] uris = { uri };
        SMSEntry entry = instance.getSMSEntry();
        entry.setAttribute(SMSEntry.ATTR_LABELED_URI, uris);
        entry.save(token);
        instance.refresh(entry);
    }

    /**
     * Returns the String representation of the <code>ServiceInstance</code>
     * object.
     * 
     * @return the String representation of the <code>ServiceInstance</code>
     *         object.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder(100);
        sb.append("\nService Instance: ").append(getName()).append(
                "\n\tGroup: ").append(getGroup()).append("\n\tURI: ").append(
                getURI()).append("\n\tAttributes: ").append(getAttributes());
        return (sb.toString());
    }

    /**
     * Returns the attributes that are associated with the service's instances.
     * 
     * @return the attributes that are associated with the service's instances.
     */
    public Map getAttributes() {
        validate();
        return (instance.getAttributes());
    }

    /**
     * Sets the attributes that are specific to the service instance. It is up
     * to the service developer to define the set of attributes and values
     * 
     * @param attrs
     *            map of attribute name to values.
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     * @throws SMSException
     *             if an error occurred while performing the operation
     */
    public void setAttributes(Map attrs) throws SSOException, SMSException {
        validateServiceInstance();
        SMSEntry e = instance.getSMSEntry();
        SMSUtils.setAttributeValuePairs(e, attrs, Collections.EMPTY_SET);
        e.save(token);
        instance.refresh(e);
    }

    /**
     * Adds the given attribute name and values to the attribute set.
     * 
     * @param attrName
     *            name of attribute.
     * @param values
     *            values to be added.
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     * @throws SMSException
     *             if an error occurred while performing the operation
     */
    public void addAttribute(String attrName, Set values) throws SSOException,
            SMSException {
        validateServiceInstance();
        SMSEntry e = instance.getSMSEntry();
        SMSUtils.addAttribute(e, attrName, values, Collections.EMPTY_SET);
        e.save(token);
        instance.refresh(e);
    }

    /**
     * Removes the specified attribute name and its values from the attribute
     * set.
     * 
     * @param attrName
     *            name of attribute.
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     * @throws SMSException
     *             if an error occurred while performing the operation
     */
    public void removeAttribute(String attrName) throws SSOException,
            SMSException {
        validateServiceInstance();
        SMSEntry e = instance.getSMSEntry();
        SMSUtils.removeAttribute(e, attrName);
        e.save(token);
        instance.refresh(e);
    }

    /**
     * Removes the specified attribute's values.
     * 
     * @param attrName
     *            name of attribute.
     * @param values
     *            values to be removed.
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     * @throws SMSException
     *             if an error occurred while performing the operation
     */
    public void removeAttributeValues(String attrName, Set values)
            throws SSOException, SMSException {
        validateServiceInstance();
        SMSEntry e = instance.getSMSEntry();
        SMSUtils.removeAttributeValues(e, attrName, values,
                Collections.EMPTY_SET);
        e.save(token);
        instance.refresh(e);
    }

    /**
     * Replaces the attribute's old value with the new value.
     * 
     * @param attrName
     *            name of attribute.
     * @param oldValue
     *            old value.
     * @param newValue
     *            new value.
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     * @throws SMSException
     *             if an error occurred while performing the operation
     */
    public void replaceAttributeValue(String attrName, String oldValue,
            String newValue) throws SSOException, SMSException {
        validateServiceInstance();
        SMSEntry e = instance.getSMSEntry();
        SMSUtils.replaceAttributeValue(e, attrName, oldValue, newValue,
                Collections.EMPTY_SET);
        e.save(token);
        instance.refresh(e);
    }

    /**
     * Replaces the attribute's old values with the new values
     * 
     * @param attrName
     *            name of attribute.
     * @param oldValues
     *            old values.
     * @param newValues
     *            new values.
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     * @throws SMSException
     *             if an error occurred while performing the operation
     */
    public void replaceAttributeValues(String attrName, Set oldValues,
            Set newValues) throws SSOException, SMSException {
        validateServiceInstance();
        SMSEntry e = instance.getSMSEntry();
        SMSUtils.replaceAttributeValues(e, attrName, oldValues, newValues,
                Collections.EMPTY_SET);
        e.save(token);
        instance.refresh(e);
    }
    
    public String toXML() {
        return instance.toXML();
    }
    
    // ----------------------------------------------------------
    // Protected methods
    // ----------------------------------------------------------
    void delete() throws SMSException, SSOException {
        validateServiceInstance();
        SMSEntry entry = instance.getSMSEntry();
        entry.delete();
        instance.refresh(entry);
    }
    
    // Validate PluginConfigImpl
    protected void validate() {
        try {
            validateServiceInstance();
        } catch (SMSException ex) {
            SMSEntry.debug.error("ServiceInstance:validate exception", ex);
        }
    }
    
    protected void validateServiceInstance() throws SMSException {
        if (!instance.isValid()) {
            throw (new SMSException("Serviceinstance:validate " + getName() +
                " No loger valid. Cache has been cleared. Recreate from" +
                "ServiceConfigManager"));
        }
    }
}

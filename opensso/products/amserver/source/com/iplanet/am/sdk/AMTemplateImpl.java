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
 * $Id: AMTemplateImpl.java,v 1.3 2008/06/25 05:41:23 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

/*
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
*/
class AMTemplateImpl extends AMObjectImpl implements AMTemplate {
    private String serviceName;

    private int type;

    private ServiceSchema serviceSchema = null;

    static String cospriorityAN = "cospriority";

    public AMTemplateImpl(SSOToken ssoToken, String DN, String serviceName,
            int type) throws AMException, SSOException {
        super(ssoToken, DN, TEMPLATE);
        this.serviceName = serviceName;
        this.type = type;
        this.profileType = type;
        try {
            ServiceSchemaManager ssm = new ServiceSchemaManager(serviceName,
                    ssoToken);
            if (type == POLICY_TEMPLATE) {
                serviceSchema = ssm.getPolicySchema();
            } else {
                serviceSchema = ssm.getDynamicSchema();
            }

            if (serviceSchema == null) {
                if (debug.warningEnabled()) {
                    debug.warning("AMTemplateImpl:"
                            + AMSDKBundle.getString("484"));
                }
                throw new AMException(AMSDKBundle.getString("484"), "484");
            }
        } catch (SMSException smsex) {
            if (debug.warningEnabled()) {
                debug.warning("AMTemplateImpl:", smsex);
            }
            throw new AMException(AMSDKBundle.getString("484"), "484");
        }
    }

    /**
     * Gets the name of the service to which this template belongs. This method
     * can be used in conjunction with SMS APIs to get the
     * AttributeSchema/ServiceSchema for the service.
     * 
     * @return service name.
     */
    public String getServiceName() {
        return this.serviceName;
    }

    /**
     * Returns a set of Attribute Schemas that defines the schema (metadata) of
     * this template.
     * 
     * @return Set of <code>com.sun.identity.sm.AttributeSchema</code> for
     *         this template
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid.
     */
    public Set getAttributeSchemas() throws AMException, SSOException {
        return serviceSchema.getAttributeSchemas();
    }

    /**
     * Gets the priority of this template in the DIT.
     * 
     * @return int priority
     */
    public int getPriority() throws SSOException {
        try {
            return getIntegerAttribute(cospriorityAN);
        } catch (AMException e) {
            debug.message("AMTemplateImpl.getPriority", e);
            return UNDEFINED_PRIORITY;
        }
    }

    /**
     * Sets the priority of this template in the DIT
     * 
     * @param priority
     *            priority
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public void setPriority(int priority) throws AMException, SSOException {
        setIntegerAttribute(cospriorityAN, priority);
        store();
    }

    /**
     * Gets the type of the template.
     * 
     * @return Returns one of the following possible values:
     *         <ul>
     *         <li> <code>AMTemplate.POLICY_TEMPLATE</code>
     *         <li> <code>AMTemplate.OTHER_TEMPLATE</code>
     */
    public int getType() {
        return this.type;
    }

    /**
     * Get the policy name. Returns a policyDN if this AMTemplate is a named
     * policy template, otherwise returns null
     * 
     * @return Set DNs of the named policies
     */
    public Set getPolicyNames() throws AMException, SSOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets attribute values in this AMObject. Note that this method sets or
     * replaces the attribute value with the new value supplied.
     * 
     * @param attributes
     *            Map where key is the attribute name and value is the attribute
     *            value
     * @throws AMException
     *             if there is an internal problem with AM Store.
     * @throws SSOException
     *             if the sign-on is no longer valid.
     */
    public void setAttributes(Map attributes) throws AMException, SSOException {
        attributes = AMCrypt.encryptPasswords(attributes, serviceSchema);
        try {
            serviceSchema.validateAttributes(attributes);
        } catch (SMSException smsex) {
            debug.error("AMTemplateImpl.setAttributes", smsex);
            throw new AMException(AMSDKBundle.getString("334"), "334");
        }
        super.setAttributes(attributes);
    }

    /**
     * Gets attribute value.
     * 
     * @param attributeName
     *            Attribute name
     * @return Set of attribute values.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the sign-on is no longer valid.
     */
    public Set getAttribute(String attributeName) throws AMException,
            SSOException {
        Set values = super.getAttribute(attributeName);
        return AMCrypt.decryptPasswords(values, attributeName, serviceSchema);
    }

    /**
     * Gets Map of specified attributes. Map key is the attribute name and value
     * is the attribute value.
     * 
     * @param attributeNames
     *            The Set of attribute names.
     * @return Map of specified attributes.
     * @throws AMException
     *             if there is an internal problem with AM Store.
     * @throws SSOException
     *             if the sign-on is no longer valid.
     */
    public Map getAttributes(Set attributeNames) throws AMException,
            SSOException {
        Map attributes = super.getAttributes(attributeNames);
        attributes = AMCrypt.decryptPasswords(attributes, serviceSchema);
        return attributes;
    }

}

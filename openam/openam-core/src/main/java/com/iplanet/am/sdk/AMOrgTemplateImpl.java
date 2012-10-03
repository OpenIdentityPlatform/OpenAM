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
 * $Id: AMOrgTemplateImpl.java,v 1.3 2008/06/25 05:41:21 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.sm.InvalidAttributeValueException;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceNotFoundException;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

/*
 * @deprecated  As of Sun Java System Access Manager 7.1.
*/
class AMOrgTemplateImpl extends AMObjectImpl implements AMTemplate {
    String serviceName;

    ServiceSchema serviceSchema = null;

    ServiceConfig serviceConfig = null;

    String orgDN;

    Map modAttributes = new HashMap();

    public AMOrgTemplateImpl(SSOToken ssoToken, String DN, String serviceName,
            ServiceConfig sc, String orgDN) throws AMException, SSOException {
        super(ssoToken, DN, TEMPLATE);
        this.serviceName = serviceName;
        serviceConfig = sc;
        this.orgDN = orgDN;
        if (serviceConfig == null) {
            throw new AMException(AMSDKBundle.getString("485"), "485");
        }

        try {
            ServiceSchemaManager ssm = new ServiceSchemaManager(serviceName,
                    ssoToken);
            serviceSchema = ssm.getSchema(SchemaType.ORGANIZATION);

            if (serviceSchema == null) {
                debug.error("AMOrgTemplateImpl:AMOrgTemplateImpl: "
                        + AMSDKBundle.getString("484"));
                throw new AMException(AMSDKBundle.getString("484"), "484");
            }
        } catch (SMSException smsex) {
            debug.error("AMTemplateImpl:AMTemplateImpl", smsex);
            throw new AMException(AMSDKBundle.getString("484"), "484");
        }
    }

    /**
     * Returns the name of the service to which this template belongs. This
     * method can be used in conjunction with SMS APIs to get the
     * AttributeSchema/ServiceSchema for the service.
     * 
     * @return service name.
     */
    public String getServiceName() {
        return this.serviceName;
    }

    /**
     * Gets the Attribute Schemas that defines the schema (metadata) of this
     * template.
     * 
     * @return Set Set of com.iplanet.services.AttributeSchema for this template
     * 
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
    public int getPriority() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the priority of this template in the DIT
     * 
     * @param priority
     *            priority
     * @throws UnsupportedOperationException
     */
    public void setPriority(int priority) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the type of the template.
     * 
     * @return Returns AMTemplate.ORGANIZATION_TEMPLATE
     */
    public int getType() {
        return ORGANIZATION_TEMPLATE;
    }

    /**
     * throws UnsupportedOperationException
     * 
     * @return Set DNs of the named policies
     */
    public Set getPolicyNames() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets attribute values in this AMObject. Note that this method sets or
     * replaces the attribute value with the new value supplied.
     * 
     * @param attributes
     *            Map where key is the attribute name and value is the attribute
     *            value
     */
    public void setAttributes(Map attributes) {
        modAttributes.putAll(attributes);
    }

    /**
     * Stores the change to directory server.
     * 
     * @throws AMException
     *             if there is an internal problem with AM Store.
     * @throws SSOException
     *             if the sign-on is no longer valid.
     */
    public void store() throws AMException, SSOException {
        Map attributes = modAttributes;
        modAttributes = new HashMap();

        try {
            serviceConfig.setAttributes(attributes);
        } catch (ServiceNotFoundException ex) {
            Object args[] = { serviceName };
            throw new AMException(AMSDKBundle.getString("481", args), "481",
                    args);
        } catch (InvalidAttributeValueException ex) {
            Object args[] = ex.getMessageArgs();
            debug.error("Store exception from SMS: " + ex, ex);
            throw new AMException(AMSDKBundle.getString("975", args), "975",
                    args);
        } catch (SMSException ex) {
            Object args[] = { serviceName };
            debug.error("Store exception from SMS: " + ex, ex);
            throw new AMException(AMSDKBundle.getString("486", args), "486",
                    args);
        }

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
        SSOTokenManager.getInstance().validateToken(token);

        Map attributes = serviceConfig.getAttributes();
        if (attributeName != null && attributes != null) {
            return (Set) attributes.get(attributeName);
        }

        return new HashSet();
    }

    /**
     * Gets string type attribute value.
     * 
     * @param attributeName
     *            Attribute name
     * @return String value of attribute
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the sign-on is no longer valid.
     */
    public String getStringAttribute(String attributeName) throws AMException,
            SSOException {
        Set values = getAttribute(attributeName);

        if (values != null && values.size() == 1) {
            return (String) values.iterator().next();
        } else if (values == null || values.isEmpty()) {
            return "";
        } else {
            throw new AMException(AMSDKBundle.getString("150"), "150");
        }
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
        Map attributes = serviceConfig.getAttributes();

        if (attributeNames != null && attributes != null) {
            Map resultMap = new HashMap();

            Iterator iter = attributeNames.iterator();
            while (iter.hasNext()) {
                String attrName = (String) iter.next();
                Object attrVal = attributes.get(attrName);
                if (attrVal != null) {
                    resultMap.put(attrName, attrVal);
                }
            }
            return resultMap;
        } else {
            return attributes;
        }
    }

    /**
     * Deletes this tempate.
     * 
     * @param recursive
     *            is not used.
     * @throws AMException
     *             if there is an internal problem with AM Store.
     * @throws SSOException
     *             if the sign-on is no longer valid.
     */
    public void delete(boolean recursive) throws AMException, SSOException {
        modAttributes = new HashMap();

        try {
            ServiceConfigManager scm = new ServiceConfigManager(serviceName,
                    token);
            scm.removeOrganizationConfiguration(orgDN, null, false);
        } catch (ServiceNotFoundException ex) {
            Object args[] = { serviceName };
            throw new AMException(AMSDKBundle.getString("481", args), "481",
                    args);
        } catch (SMSException ex) {
            Object args[] = { serviceName };
            throw new AMException(AMSDKBundle.getString("913", args), "913",
                    args);
        }
    }

    /**
     * Always return true
     */
    public boolean isExists() throws SSOException {
        return (true);
    }

}

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
 * $Id: AMAuthenticationInstance.java,v 1.2 2008/06/25 05:41:51 qcheng Exp $
 *
 */


package com.sun.identity.authentication.config;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.SMSException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This class provides implementation of an individual instance
 * of a module type. A module instance has a name, type and
 * its own configuration data, which can be accessed through
 * <code>ServiceConfig</code> interface. 
 */ 
public class AMAuthenticationInstance {
    private String moduleName;
    private String moduleType;
    private ServiceConfig serviceConfig = null;
    private ServiceSchema globalSchema = null;

    /**
     * Constructs a module instance object. This constructor can
     * only be called through <code>ModuleInstanceManager</code>.
     *
     * @param name The name of the module instance.
     * @param type The type this module belongs to. e.g. LDAP, JDBC, etc.
     * @param config Service configuration for the module instance.
    */
    protected AMAuthenticationInstance(
        String name,
        String type, 
        ServiceConfig config,
        ServiceSchema global) {
        moduleName = name;
        moduleType = type;
        serviceConfig = config;
        globalSchema = global;
    }

    /**
     * Returns the name of the module instance.
     *
     * @return Name of the module instance.
     */
    public String getName() {
        return moduleName;
    }

    /**
     * Returns the type of the module instance.
     *
     * @return Type of the module instance.
     */
    public String getType() {
        return moduleType;
    }

    /**
     * Returns a <code>ServiceConfig</code> instance which can be used to access
     * and make changes to the module instance configuration data.
     * @return A <code>ServiceConfig</code> object for this module instance.
     */
    public ServiceConfig getServiceConfig() {
        return serviceConfig;
    }

    /**
     * Returns the configuration attributes of the module instance.
     *
     * @return Map of attribute name to a set of attribute values.
     */
    public Map getAttributeValues(){ 
        Map org = null;
        if (serviceConfig != null) {
            org = serviceConfig.getAttributes();
        }
        Map global = null;
        if (globalSchema != null) {
            global = globalSchema.getAttributeDefaults();
        }
        HashMap totalAttrs = new HashMap();
        if (global != null) {
            totalAttrs.putAll(global);
        }
        if (org != null) {
            totalAttrs.putAll(org);
        }
        return totalAttrs;
    }

    /**
     * Returns the configuration attribute values for the specified attributes.
     *
     * @param names Set of specified attributes.
     * @return Map of attribute name to a set of attribute values.
     */
    public Map getAttributeValues(Set names) {
        Map allAttrs = getAttributeValues();
        Map attrs = new HashMap();
        for (Iterator it = names.iterator(); it.hasNext(); ){
            Object key = it.next();
            if (allAttrs.containsKey(key)) {
                attrs.put(key, allAttrs.get(key));
            }
        }
        return attrs;
    }

    /**
     * Sets the configuration parameters. This method will replace the existing
     * attribute values with the given ones. For attributes that are not in
     * <code>values</code>, they will not be modified.
     *
     * @param values the <code>Map</code> in which keys are the attribute names
     *        and values are the <code>Set</code> of attribute values.
     * @throws SMSException if there is an error occurred while performing the
     *         operation.
     * @throws SSOException if the user's SSO token is invalid or expired.
     */
    public void setAttributeValues(Map values)
            throws SMSException, SSOException {
        if (serviceConfig != null) {
            serviceConfig.setAttributes(values);
        }
    }

    /**
     * Sets a configuration parameter with the specified value 
     * <code>values</code>.
     *
     * @param name the name of the attribute to be set.
     * @param values the <code>Set</code> of values for that attribute.
     * @throws SMSException if there is an error occurred while performing the
     *         operation.
     * @throws SSOException if the user's SSO token is invalid or expired.
     */
    public void setAttribute(String name, Set values)
            throws SMSException, SSOException {
        if (serviceConfig != null) {
            serviceConfig.addAttribute(name, values);
        }
    }

}

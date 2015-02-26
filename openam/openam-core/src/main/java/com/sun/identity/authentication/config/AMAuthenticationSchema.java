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
 * $Id: AMAuthenticationSchema.java,v 1.3 2008/06/25 05:41:52 qcheng Exp $
 *
 */


package com.sun.identity.authentication.config;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SchemaException;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.SMSException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This class implements a subset of the service schema operations
 * for the <code>AMModule</code> instance.
 */
public class AMAuthenticationSchema
{
    private ServiceSchema serviceSchema;

    protected AMAuthenticationSchema(ServiceSchema schema) {
        serviceSchema = schema;
    }

    /**
     * Returns the name of this schema.
     *
     * @return a String value of the schema name.
     */
    public String getName() {
        return serviceSchema.getName();
    }

    /**
     * Returns the name of the service schema.
     *
     * @return the name of the service schema.
     */
    public String getServiceName() {
        return serviceSchema.getServiceName();
    }

   /**
    * Returns a set of required attribute names.
    *
    * @return a <code>Set</code> of the required attribute names of 
    *         the subject schema.
    */
    public Set getRequiredAttributeNames() {
        Set names = new HashSet();
        for (Iterator it = getAttributeNames().iterator(); it.hasNext(); ){
            String attr = (String)it.next();
            AttributeSchema as = getAttributeSchema(attr);
            String anyValue = as.getAny();
            if (anyValue != null && (anyValue.indexOf("required") != -1)) {
                names.add(attr);
            }
        }
        return names;
    }

    /**
     * Returns a set of all the attribute names.
     *
     * @return a <code>Set</code> of all the attribute names.
     */
    public Set getAttributeNames(){
        return serviceSchema.getAttributeSchemaNames();
    }

    /**
     * Returns an <code>AttributeSchema</code> of the specified attribute name.
     *
     * @param attr the specified attribute name.
     * @return the <code>AttributeSchema</code> of the attribute.
     */
    public AttributeSchema getAttributeSchema(String attr) {
        return serviceSchema.getAttributeSchema(attr);
    }

    /**
     * Returns a Set which contains all the <code>AttributeSchemas</code>.
     *
     * @return Set of <code>AttributeSchema</code>.
     */
    public Set getAttributeSchemas() {
        return serviceSchema.getAttributeSchemas();
    }

    /**
     * Returns the default values for all the attributes.
     *
     * @return a <code>Map</code> that contains all the default attributes
     *         and their values.
     */
    public Map getAttributeValues(){
        return serviceSchema.getAttributeDefaults();
    }

    /**
     * Returns the default values for the specified attributes.
     *
     * @param names a <code>Set</code> of attribute names in String values.
     * @return a <code>Map</code> that contains all the default attributes
     *         and their values for the specified attributes.
     */
    public Map getAttributeValues(Set names) {
        Map allAttrs = getAttributeValues();
        Map attrs = new HashMap();
        for (Iterator it = names.iterator(); it.hasNext(); ) {
            Object key = it.next();
            if (allAttrs.containsKey(key)) {
                attrs.put(key, allAttrs.get(key));
            }
        }
        return attrs;
    }

    /**
     * Sets the default attribute values. 
     *
     * @param values A map of the names of <code>AttributeSchema</code> to
     *        modify, and a Set of Values which should replace the default
     *        values of the current schema.
     * @throws SchemaException
     * @throws SMSException if an error occurred while performing the operation
     * @throws SSOException if the single sign on token is invalid or expired
     */
    public void setAttributeValues(Map values) 
        throws SchemaException, SMSException, SSOException {
        serviceSchema.setAttributeDefaults(values);
    }

    /**
     * Sets default value for a specific attribute.
     *
     * @param attrName Name of the attribute for which defaults
     *      values need to be replaced.
     * @param values Set of new values to replace the old ones.
     * @throws SchemaException if an error occurred while parsing the XML
     * @throws SMSException if an error occurred while performing the operation
     * @throws SSOException if the single sign on token is invalid or expired
     */
    public void setAttribute(String attrName, Set values) 
            throws SchemaException, SMSException, SSOException {
        serviceSchema.setAttributeDefaults(attrName, values);
    }
}

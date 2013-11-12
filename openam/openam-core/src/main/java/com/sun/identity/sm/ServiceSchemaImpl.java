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
 * $Id: ServiceSchemaImpl.java,v 1.7 2008/06/25 05:44:05 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2012-2013 ForgeRock Inc
 */
package com.sun.identity.sm;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.common.CaseInsensitiveHashSet;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.iplanet.sso.SSOToken;
import com.iplanet.ums.IUMSConstants;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The class <code>ServiceSchema</code> provides interfaces to manage the
 * schema information of a service. The schema for a service can be one of the
 * following types: GLOBAL, ORGANIZATION, DYNAMIC, USER, and POLICY.
 */
class ServiceSchemaImpl {
    // ServiceSchema XML node
    Node schemaNode;

    // Instance variables
    String name;

    String i18nKey;

    String statusAttribute;

    String validate;

    String inheritance;

    // Attribute & sub-schema variables
    Set serviceAttributes;

    Set searchableAttributeNames;

    Map<String, AttributeSchemaImpl> attrSchemas;

    Map attrValidators;

    Map attrDefaults;

    Map attrReadOnlyDefaults;

    Map subSchemas;

    ServiceSchemaImpl orgAttrSchema;

    ServiceSchemaManagerImpl ssm;

    boolean isOrgAttrSchema;

    String serviceName;

    // Debug class
    static Debug debug = SMSEntry.debug;

    ServiceSchemaImpl() {
        // do nothing
    }

    // Protected Constructor
    protected ServiceSchemaImpl(Node node) {
        this(null, node);
    }

    // Protected constructor
    protected ServiceSchemaImpl(ServiceSchemaManagerImpl ss, Node node) {
        ssm = ss;
        update(node);
    }

    /**
     * Returns the name of the schema.
     */
    String getName() {
        return (name);
    }

    /**
     * Returns the I18N key that points to the description of the service.
     */
    String getI18NKey() {
        return ((i18nKey == null) && (ssm != null)) ? ssm.getI18NKey()
                : i18nKey;
    }

    /**
     * Returns statusAttribute name
     * 
     */
    String getStatusAttribute() {
        return (statusAttribute);
    }

    /**
     * Returns the value of validate attribute for this schema/subschema
     */
    String getValidate() {
        return (validate);
    }

    /**
     * Returns <code>true</code> if service schema supports multiple
     * configurations; <code>false</code> otherwise.
     */
    boolean supportsMultipleConfigurations() {
        return (inheritance != null)
                && inheritance.equalsIgnoreCase("multiple");
    }

    /**
     * Returns the names of the schema attributes defined for the service. It
     * does not return the schema attributes defined for the sub-schema.
     */
    Set<String> getAttributeSchemaNames() {
        return new HashSet<String>(attrSchemas.keySet());
    }

    /**
     * Returns the names of the schema attributes defined for the service which
     * are searchable. It does not return the schema attributes defined for the
     * sub-schema.
     */
    protected Set getSearchableAttributeNames() {
        Set tmpSet = new HashSet();
        if (searchableAttributeNames != null
                && !searchableAttributeNames.isEmpty()) {
            Iterator itr = searchableAttributeNames.iterator();
            while (itr.hasNext()) {
                String str = (String) itr.next();
                if ((isOrgAttrSchema)
                        && (!str.toLowerCase().startsWith(
                                serviceName.toLowerCase()))) {
                    tmpSet.add((serviceName + "-" + str).toLowerCase());
                } else {
                    tmpSet.add(str);
                }
            }
        }
        searchableAttributeNames = tmpSet;
        return (searchableAttributeNames);
    }

    Set getServiceAttributeNames() {
        return (new HashSet(serviceAttributes));
    }

    /**
     * Returns the schema for an attribute given the name of the attribute,
     * defined for this service. It returns only the attribute schema defined at
     * the top level for the service and not from the sub-schema.
     */
    AttributeSchemaImpl getAttributeSchema(String attributeName) {
        return attrSchemas.get(attributeName);
    }

    Set<AttributeSchemaImpl> getAttributeSchemas() {
        return new HashSet<AttributeSchemaImpl>(attrSchemas.values());
    }

    /**
     * Get a map of all the attribute and their default values in this schema
     */
    Map getAttributeDefaults() {
        return (SMSUtils.copyAttributes(attrDefaults));
    }

    /**
     * Get a read only map (Unmodifiable map) of all the attribute and their
     * default values in this schema
     */
    Map getReadOnlyAttributeDefaults() {
        return attrReadOnlyDefaults;
    }

    /**
     * Returns the names of sub-schemas for the service.
     */
    Set getSubSchemaNames() {
        return (new HashSet(subSchemas.keySet()));
    }

    /**
     * Returns <code>ServiceSchema</code> object given the name of the
     * service's sub-schema.
     */
    ServiceSchemaImpl getSubSchema(String subSchemaName) throws SMSException {
        return ((ServiceSchemaImpl) subSchemas.get(subSchemaName));
    }

    /**
     * Returns <code>ServiceSchema</code> for creating Organizations if
     * present; else <code>null</code>.
     */
    ServiceSchemaImpl getOrgAttrSchema() {
        return (orgAttrSchema);
    }

    /**
     * Determines whether each attribute in the attribute set is valid. Iterates
     * though the set checking each element to see if there is a validator that
     * needs to execute.
     */
    boolean validateAttributes(Map attributeSet, boolean encodePassword)
            throws SMSException {
        return (validateAttributes(attributeSet, encodePassword, null));
    }

    /**
     * Determines whether each attribute in the attribute set is valid. This
     * method additionally takes the organization name that would be passed to
     * the validation. Iterates though the set checking each element to see if
     * there is a validator that needs to execute.
     */
    boolean validateAttributes(Map attributeSet, boolean encodePassword, 
        String orgName) throws SMSException {
        return (validateAttributes(null, attributeSet, encodePassword, orgName));
    }

    /**
     * Determines whether each attribute in the attribute set is valid. This
     * method additionally takes the organization name and SSOToken that would 
     * be passed to the validation. Iterates though the set checking each element 
     * to see if there is a validator that needs to execute.
     */
    boolean validateAttributes(SSOToken ssoToken, Map attributeSet, boolean 
        encodePassword, String orgName) throws SMSException {
        if (validate != null && validate.equalsIgnoreCase("no")) {
            // Do not validate attributes in this subschema
            return (true);
        }

        // to check for duplicates (case insensitive)
        CaseInsensitiveHashSet asNames = new CaseInsensitiveHashSet();
        // For each attribute, validate its values
        for (Iterator items = attributeSet.keySet().iterator(); 
                                                    items.hasNext();) 
        {
            String attrName = (String) items.next();
            if (asNames.contains(attrName)) {
                Object[] args = { attrName };
                throw new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                        "sms-attributeschema-duplicates", args);
            } else {
                asNames.add(attrName);
            }
            if (!attrName.equalsIgnoreCase(SMSUtils.COSPRIORITY)) {
                Set vals = (Set) attributeSet.get(attrName);
                validateAttrValues(ssoToken, attrName, vals, 
                    encodePassword, orgName);
            }
        }
        return (true);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(100);
        if (getName() != null) {
            sb.append("Schema name: ").append(getName()).append("\n");
        }
        // Attributes
        if (attrSchemas.size() > 0) {
            sb.append("Attribute Schemas:\n");
            Iterator<String> items = attrSchemas.keySet().iterator();
            while (items.hasNext()) {
                sb.append(attrSchemas.get(items.next()).toString());
            }
        }
        // Sub-schemas
        if (subSchemas.size() > 0) {
            sb.append("Sub-Schemas:\n");
            Iterator items = subSchemas.keySet().iterator();
            while (items.hasNext()) {
                sb.append(subSchemas.get(items.next()).toString());
            }
        }
        return (sb.toString());
    }

    Node getSchemaNode() {
        return (schemaNode);
    }

    // -----------------------------------------------------------
    // Protected methods
    // -----------------------------------------------------------
    synchronized void update(Node sNode) {
        schemaNode = sNode;
        if (schemaNode == null) {
            if (debug.warningEnabled()) {
                debug.warning("ServiceSchemaImpl::update schema node is NULL");
            }
            name = "";
            i18nKey = null;
            statusAttribute = null;
            serviceAttributes = new HashSet();
            searchableAttributeNames = new HashSet();
            attrSchemas = attrValidators = attrDefaults = new HashMap();
            subSchemas = new CaseInsensitiveHashMap();
            attrReadOnlyDefaults = Collections.unmodifiableMap(new HashMap());
        }

        // Get the name and i18nKey
        name = XMLUtils.getNodeAttributeValue(schemaNode, SMSUtils.NAME);
        i18nKey = XMLUtils.getNodeAttributeValue(schemaNode, SMSUtils.I18N_KEY);
        statusAttribute = XMLUtils.getNodeAttributeValue(schemaNode,
                SMSUtils.STATUS_ATTRIBUTE);
        inheritance = XMLUtils.getNodeAttributeValue(schemaNode,
                SMSUtils.INHERITANCE);
        validate = XMLUtils
                .getNodeAttributeValue(schemaNode, SMSUtils.VALIDATE);

        // Update sub-schema's, organization schema and attributes
        Set newServiceAttributes = new HashSet();
        Set newSearchableAttributeNames = new HashSet();
        Map<String, AttributeSchemaImpl> newAttrSchemas = new HashMap<String, AttributeSchemaImpl>();
        Map newAttrValidators = new HashMap();
        Map newAttrDefaults = new HashMap();
        Map newSubSchemas = new CaseInsensitiveHashMap();
        Map tempUnmodifiableDefaults = new HashMap();
        NodeList children = schemaNode.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            String name = XMLUtils.getNodeAttributeValue(node, SMSUtils.NAME);
            if (node.getNodeName().equals(SMSUtils.SCHEMA_ATTRIBUTE)) {
                AttributeSchemaImpl asi = null;
                if (attrSchemas != null) {
                    asi = attrSchemas.get(name);
                }
                if (asi == null) {
                    asi = new AttributeSchemaImpl(node);
                } else {
                    // Get the instance of attribute schema and update it
                    asi.update(node);
                }
                newAttrSchemas.put(name, asi);
                if (!asi.isStatusAttribute() && !asi.isServiceIdentifier()) {
                    newServiceAttributes.add(name);
                }
                if (asi.isSearchable()) {
                    newSearchableAttributeNames.add(name);
                }
                newAttrValidators.put(name, new AttributeValidator(asi));
                newAttrDefaults.put(name, asi.getDefaultValues());
                tempUnmodifiableDefaults.put(name, Collections
                        .unmodifiableSet(asi.getDefaultValues()));
            } else if (node.getNodeName().equals(SMSUtils.SUB_SCHEMA)) {
                ServiceSchemaImpl ssi = null;
                if (subSchemas != null) {
                    ssi = (ServiceSchemaImpl) subSchemas.get(name);
                }
                if (ssi == null) {
                    newSubSchemas.put(name, new ServiceSchemaImpl(ssm, node));
                } else {
                    ssi.update(node);
                    newSubSchemas.put(name, ssi);
                }
            } 
            else if (node.getNodeName().equals(SMSUtils.ORG_ATTRIBUTE_SCHEMA))
            {
                orgAttrSchema = new ServiceSchemaImpl(ssm, node);
            }
        }
        serviceAttributes = newServiceAttributes;
        attrSchemas = newAttrSchemas;
        searchableAttributeNames = newSearchableAttributeNames;
        attrValidators = newAttrValidators;
        attrDefaults = newAttrDefaults;
        attrReadOnlyDefaults = Collections
                .unmodifiableMap(tempUnmodifiableDefaults);
        subSchemas = newSubSchemas;
    }
    
    synchronized void clear() {
        // org attr-schema
        if (orgAttrSchema != null) {
        	orgAttrSchema.clear();
        	orgAttrSchema = null;
        }
    	
    	// Sub-schemas
        if (subSchemas.size() > 0) {
            Iterator items = subSchemas.keySet().iterator();
            while (items.hasNext()) {
            	ServiceSchemaImpl ssi = (ServiceSchemaImpl)subSchemas.get(items.next());
            	ssi.clear();
            }
        }
        
        //important to clean all reference to ServiceSchemaManagerImpl
        ssm = null;
        
        //finally clear all subschemas
        subSchemas.clear();
    }

    AttributeValidator getAttributeValidator(String attrName) {
        AttributeValidator av = (AttributeValidator) attrValidators
                .get(attrName);
        if (av == null) {
            AttributeSchemaImpl as = getAttributeSchema(attrName);
            if (as == null) {
                return null;
            }
            av = new AttributeValidator(as);
            attrValidators.put(attrName, av);
        }
        return (av);
    }

    // Validates the values for the attribute, for the
    // given organization name
    void validateAttrValues(SSOToken ssoToken, String attrName, Set values,
            boolean encodePassword, String orgName) throws SMSException {
        if (validate != null && validate.equalsIgnoreCase("no")) {
            // Do not validate attributes in this subschema
            return;
        }
        AttributeValidator av = getAttributeValidator(attrName);
        if (av == null) {
            // Invalid attribute name
            if (debug.messageEnabled()) {
                debug.message("ServiceSchemaImpl::validateAttrValues "
                        + "Invalid Attribute: " + attrName + " in service: "
                        + ssm.getName() + " sub-schema: " + name);
            }
            String[] args = { attrName };
            throw (new InvalidAttributeNameException(
                    IUMSConstants.UMS_BUNDLE_NAME,
                    "sms-validation_failed_invalid_name", args));
        }

        // Check if required attributes have values
        // %%% Due to comms issue, where they define AttributeSchema
        // %%% without default values, this check should not be done
        // %%% if orgName is null
        // OrgName should be null only during loading of schema
        // and when ServiceSchema.validateAttributes(...) is called
        // with no orgName param or if orgName == null
        if (orgName != null) {
            AttributeSchemaImpl as = getAttributeSchema(attrName);
            String anyValue = as.getAny();
            if (anyValue != null && anyValue.indexOf("required") > -1
                    && (values == null || values.isEmpty())) {
                Object args[] = { attrName };
                SMSException smse = new SMSException(
                        IUMSConstants.UMS_BUNDLE_NAME,
                        "sms-required-attribute-delete", args);
                debug.error(
                        "ServiceSchemaImpl.validateAttrValues: " + attrName
                                + " is a required attribute and cannot"
                                + " be deleted", smse);
                throw smse;
            }
        }

        // If orgName is not null, pass it as an environment map
        if (orgName != null) {
            HashMap env = new HashMap();
            env.put(Constants.ORGANIZATION_NAME, orgName);
            if (ssoToken != null) {
                env.put(Constants.SSO_TOKEN, ssoToken); 
            }
            String i18nFileName = (ssm != null) ? ssm.getI18NFileName() : null;
            av.validate(values, i18nFileName, encodePassword, env);
        } else {
            String i18nFileName = (ssm != null) ? ssm.getI18NFileName() : null;
            av.validate(values, i18nFileName, encodePassword);
        }

        validatePlugin(ssoToken, attrName, values);
    }

    /**
     * Validates the attribute with the validation plugin if a plugin has been
     * registered for this attribute in the service schema.
     * 
     * @param token 
     *            Single Sign On token.
     * @param attrName
     *            the name of the attribute to validate
     * @param values
     *            the <code>Set</code> of string values to validate
     * @return true if the values are valid or there is no validation plugin
     *         registered to the attribute; false otherwise
     * @throws SMSException
     *             error during instantiating the Java class
     * @throws InvalidAttributeNameException
     *             the attribute does not appear in the schema
     */
    boolean validatePlugin(SSOToken token, String attrName, Set values
    ) throws SMSException, InvalidAttributeNameException {

        AttributeSchemaImpl as = getAttributeSchema(attrName);
        if (as == null) {
            String[] args = { attrName };
            throw new InvalidAttributeNameException(
                    IUMSConstants.UMS_BUNDLE_NAME,
                    "sms-validation_failed_invalid_name", args);
        }

        String validatorName = as.getValidator();
        if (validatorName == null) {
            // no validator registered for this service attribute
            return true;
        }

        AttributeSchemaImpl validatorAttrSchema = 
           getAttributeSchema(validatorName);
        if (validatorAttrSchema != null) {
            boolean isServerMode = SystemProperties.isServerMode();
            Set javaClasses = validatorAttrSchema.getDefaultValues();
            for (Iterator it = javaClasses.iterator(); it.hasNext();) {
                String javaClass = (String) it.next();
                try {
                    serverEndAttrValidation(as, attrName, values, javaClass);
                } catch (SMSException e) {
                    if (!isServerMode) {
                        clientEndAttrValidation(
                            token, as, attrName, values, javaClass);

                    } else {
                        throw e;
                    }
                }
            }
        }

        return true;
    }

    private void clientEndAttrValidation(
        SSOToken token,
        AttributeSchemaImpl as,
        String attrName,
        Set values,
        String javaClass
    ) throws SMSException {
        if (!RemoteServiceAttributeValidator.validate(
            token, javaClass, values)
        ) {
            throwInvalidAttributeValuesException(
                javaClass.equals("com.sun.identity.sm.RequiredValueValidator"),
                attrName, as);
        }
    }
    
    private void serverEndAttrValidation(
        AttributeSchemaImpl as,
        String attrName,
        Set values,
        String javaClass
    ) throws SMSException {
        try {
            Class clazz = Class.forName(javaClass);
            ServiceAttributeValidator validator = (ServiceAttributeValidator)
                clazz.newInstance();
            validatePlugin(validator, as, attrName, values);
        } catch (InstantiationException ex) {
            debug.error("ServiceSchemaImpl.serverEndAttrValidation", ex);
            String args[] = {javaClass};
            throw new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                IUMSConstants.SMS_VALIDATOR_CANNOT_INSTANTIATE_CLASS,
                args);
        } catch (IllegalAccessException ex) {
            debug.error("ServiceSchemaImpl.serverEndAttrValidation", ex);
            String args[] = {javaClass};
            throw new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                IUMSConstants.SMS_VALIDATOR_CANNOT_INSTANTIATE_CLASS,
                args);
        } catch (ClassNotFoundException ex) {
            debug.error("ServiceSchemaImpl.serverEndAttrValidation", ex);
            String args[] = {javaClass};
            throw new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                IUMSConstants.SMS_VALIDATOR_CANNOT_INSTANTIATE_CLASS,
                args);
        }
    }

    private void validatePlugin(
        ServiceAttributeValidator validator,
        AttributeSchemaImpl as, 
        String attrName, 
        Set values
    ) throws InvalidAttributeValueException {
        if (!validator.validate(values)) {
            throwInvalidAttributeValuesException(
                (validator instanceof RequiredValueValidator),
                attrName, as);
        }
    }
    
    private void throwInvalidAttributeValuesException(
        boolean isRequiredValue,
        String attrName,
        AttributeSchemaImpl as)
        throws InvalidAttributeValueException {
        String i18nFileName = (ssm != null) ? ssm.getI18NFileName() : null;

        String message = (isRequiredValue)
            ? "sms-attribute-values-missing"
            : "sms-attribute-values-does-not-match-schema";

        if (i18nFileName != null) {
            String[] args = {attrName, i18nFileName, as.getI18NKey()};
            throw new InvalidAttributeValueException(
                IUMSConstants.UMS_BUNDLE_NAME, message, args);
        } else {
            String[] args = {attrName};
            throw new InvalidAttributeValueException(
                IUMSConstants.UMS_BUNDLE_NAME, message, args);
        }
    }
}

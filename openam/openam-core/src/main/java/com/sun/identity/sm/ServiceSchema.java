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
 * $Id: ServiceSchema.java,v 1.12 2008/08/30 16:46:47 goodearth Exp $
 *
 */

/*
 * Portions Copyrighted 2011-2013 ForgeRock AS
 */
package com.sun.identity.sm;

import com.iplanet.sso.SSOException;
import com.iplanet.ums.IUMSConstants;
import com.sun.identity.shared.Constants;
import com.sun.identity.common.CaseInsensitiveHashSet;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * The class <code>ServiceSchema</code> provides interfaces to manage the
 * schema information of a service. The schema for a service can be one of the
 * following types: GLOBAL, ORGANIZATION, DYNAMIC, USER, and POLICY.
 *
 * @supported.all.api
 */
public class ServiceSchema {

    // Pointer to service's schema manager & type
    ServiceSchemaImpl ss;

    String componentName;

    SchemaType type;

    ServiceSchemaManager ssm;

    boolean orgAttrSchema;

    /**
     * Default constructor (private).
     */
    private ServiceSchema() {
        // do nothing
    }

    protected ServiceSchema(ServiceSchemaImpl ssi, String compName,
            SchemaType type, ServiceSchemaManager ssm) {
        this(ssi, compName, type, ssm, false);
    }

    protected ServiceSchema(ServiceSchemaImpl ssi, String compName,
            SchemaType type, ServiceSchemaManager ssm, boolean isOrgAttrSchema) 
    {
        this.ss = ssi;
        this.componentName = compName;
        this.type = type;
        this.ssm = ssm;
        this.orgAttrSchema = isOrgAttrSchema;
        this.ss.isOrgAttrSchema = isOrgAttrSchema;
        this.ss.serviceName = ssm.getName();
    }

    /**
     * Returns the name of the service.
     * 
     * @return the name of the schema
     */
    public String getServiceName() {
        return (ssm.getName());
    }

    /**
     * Returns the version of the service.
     * 
     * @return version of the service schema
     */
    public String getVersion() {
        return (ssm.getVersion());
    }

    /**
     * Returns the name of the schema.
     * 
     * @return the name of the schema
     */
    public String getName() {
        return (ss.getName());
    }

    /**
     * Returns the schema type.
     * 
     * @return the schema type.
     */
    public SchemaType getServiceType() {
        return (type);
    }

    /**
     * Returns the I18N key that points to the description of the service.
     * 
     * @return the I18N key that points to the description of the service
     */
    public String getI18NKey() {
        return (ss.getI18NKey());
    }

    /**
     * Returns <code>true</code> if service schema supports multiple
     * configurations; <code>false</code> otherwise
     * 
     * @return <code>true</code> if service schema supports multiple
     *         configurations; <code>false</code> otherwise
     */
    public boolean supportsMultipleConfigurations() {
        return (ss.supportsMultipleConfigurations());
    }

    /**
     * Sets the value of the I18N key in the service schema.
     * 
     * @param key
     *            Value to be set for the I18N key of the service schema.
     * @throws SMSException
     *             if there is a problem setting the value in the data store.
     * @throws SSOException
     *             If the user has an invalid SSO token.
     */
    public void setI18Nkey(String key) throws SMSException, SSOException {
        SMSEntry.validateToken(ssm.getSSOToken());
        Node sNode = ss.getSchemaNode();
        ((Element) sNode).setAttribute(SMSUtils.I18N_KEY, key);
        ssm
                .replaceSchema((ServiceSchemaManagerImpl.getInstance(ssm
                        .getSSOToken(), ssm.getName(), ssm.getVersion()))
                        .getDocument());
        ss.i18nKey = key;
    }

    /**
     * Set the value of inheritance attribute in service schema.
     * 
     * @param value
     *            New value of inheritance attribute.
     * @throws SMSException
     *             if there is a problem setting the value in the data store.
     * @throws SSOException
     *             if the user has an invalid single sign on token.
     */
    public void setInheritance(String value) throws SMSException, SSOException {
        if (!value.equals("single") && !value.equals("multiple")) {
            String[] arg = { value };
            throw new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    "sms-invalid-inheritance-value", arg);
        }

        SMSEntry.validateToken(ssm.getSSOToken());
        Node sNode = ss.getSchemaNode();
        ((Element) sNode).setAttribute(SMSUtils.INHERITANCE, value);
        ssm
                .replaceSchema((ServiceSchemaManagerImpl.getInstance(ssm
                        .getSSOToken(), ssm.getName(), ssm.getVersion()))
                        .getDocument());
        ss.inheritance = value;
    }

    /**
     * Returns the view bean URL for this service
     * 
     * @return file name that contains I18N messages
     */
    public String getPropertiesViewBeanURL() {
        return (ssm.getPropertiesViewBeanURL());
    }

    /**
     * Returns the name of the status attribute, as defined in the Service
     * schema
     * 
     * @return String name of status attribute
     */
    public String getStatusAttribute() {
        return (ss.getStatusAttribute());
    }

    /**
     * Returns <code>true</code> if the service configuration created can be
     * exported to other organizations.
     * 
     * @return <code>true</code> if service configurations for this schema can
     *         be exported to other organizations; <code>false</code>
     *         otherwise.
     */
    public boolean isExportable() {
        return (false);
    }

    /**
     * Sets the exportable nature of the service configurations created for this
     * schema. Setting it to <code>true</code> allows the configurations to be
     * exported to other organizations and a value of <code>false</code>
     * disables exporting of configuration data.
     * 
     * @param exportable
     *            <code>true</code> if service configurations for this schema
     *            can be exported to other organizations; <code>false</code>
     *            otherwise.
     */
    public void setExportable(boolean exportable) {
    }

    /**
     * Returns the I18N properties file name for the service schema.
     * 
     * @return the I18N properties file name for the service schema
     */
    public String getI18NFileName() {
        return (ssm.getI18NFileName());
    }

    /**
     * Sets the I18N properties file name for the service schema
     * 
     * @param url
     *            properties file name
     * @throws SMSException
     *             if an error occurred while trying to perform the operation
     * @throws SSOException
     *             if the single sign on token is invalid or expired
     */
    public void setI18NFileName(String url) throws SMSException, SSOException {
        ssm.setI18NFileName(url);
    }

    /**
     * Returns the names of the schema attributes defined for the service. It
     * does not return the schema attributes defined for the sub-schema.
     * 
     * @return the names of schema attributes defined for the service
     */
    public Set getAttributeSchemaNames() {
        return (ss.getAttributeSchemaNames());
    }

    /**
     * Returns the names of the schema attributes defined for the service which
     * are searchable. It does not return the schema attributes defined for the
     * sub-schema.
     * 
     * @return the names of schema attributes defined for the service which are
     *         searchable attributes.
     */
    protected Set getSearchableAttributeNames() {
        return (ss.getSearchableAttributeNames());
    }

    /**
     * Returns the schema for an attribute given the name of the attribute,
     * defined for this service. It returns only the attribute schema defined at
     * the top level for the service and not from the sub-schema.
     * 
     * @param attributeName
     *            the name of the schema attribute
     * @return the schema for the attribute
     */
    public AttributeSchema getAttributeSchema(String attributeName) {
        AttributeSchemaImpl as = ss.getAttributeSchema(attributeName);
        return ((as == null) ? null : new AttributeSchema(as, ssm, this));
    }

    /**
     * Returns the attribute schemas defined for the service. It does not return
     * the schema attributes defined for the sub-schema.
     * 
     * @return attribute schemas defined for the service
     */
    public Set getAttributeSchemas() {
        Set answer = new HashSet();
        for (Iterator items = getAttributeSchemaNames().iterator(); items
                .hasNext();) {
            String attrName = (String) items.next();
            answer.add(getAttributeSchema(attrName));
        }
        return (answer);
    }

    /**
     * Returns the attribute schemas defined for the service that is not a
     * status attribute and is not a service attribute. It does not return the
     * schema attributes defined for the sub-schema.
     * 
     * @return attribute schemas defined for the service
     */
    public Set getServiceAttributeNames() {
        return (ss.getServiceAttributeNames());
    }

    /**
     * Validates the <code>attrMap</code> against the attributes defined in
     * this schema of the service. It will throw an exception if the map
     * contains any attribute not listed in the schema. It will also pick up
     * default values for any attributes not in the map but which are listed in
     * the schema, if the boolean <Code>inherit</Code> is set to true.
     * 
     * @param attrMap
     *            map of attributes
     * @param inherit
     *            if true, then inherit the default values
     * @return Map of validated attributes with default values
     * @throws SMSException
     *             if invalid attribute names are present in the
     *             <code>attrMap</code>.
     */
    public Map validateAndInheritDefaults(Map attrMap, boolean inherit)
            throws SMSException {
        return (validateAndInheritDefaults(attrMap, null, inherit));
    }

    /**
     * Validates the <code>attrMap</code> against the attributes defined in
     * this schema of the service for the given organization. It will throw an
     * exception if the map contains any attribute not listed in the schema. It
     * will also pick up default values for any attributes not in the map but
     * which are listed in the schema, if the boolean <Code>inherit</Code> is
     * set to true.
     * 
     * @param attrMap
     *            map of attributes
     * @param inherit
     *            if true, then inherit the default values
     * @return Map of validated attributes with default values
     * @throws SMSException
     *             if invalid attribute names are present in the
     *             <code>attrMap</code>.
     */
    public Map validateAndInheritDefaults(Map attrMap, String orgName,
            boolean inherit) throws SMSException {
        SMSEntry.validateToken(ssm.getSSOToken());

        if (attrMap == null || attrMap.isEmpty()) {
            attrMap = new HashMap();
        }
        Iterator keys = attrMap.keySet().iterator();
        Set defAttrSet = ss.getAttributeSchemaNames();
        while (keys.hasNext()) {
            String attr = (String) keys.next();
            if (!defAttrSet.contains(attr)) {
                // This attribute is not listed in the service.
                debug.error("ServiceSchema.validateAndInheritDefaults: " + attr
                        + " is not listed in the service " + getServiceName());
                throw new InvalidAttributeNameException(
                        IUMSConstants.UMS_BUNDLE_NAME,
                        "services_validator_invalid_attr_name", null);
            }
        }

        // If orgName is not null, populate the envMap
        Map envMap = Collections.EMPTY_MAP;
        if (orgName != null) {
            envMap = new HashMap();
            envMap.put(Constants.ORGANIZATION_NAME, orgName);
            envMap.put(Constants.SSO_TOKEN, ssm.getSSOToken());
        }
        Iterator ass = ss.getAttributeSchemaNames().iterator();
        while (ass.hasNext()) {
            String attr = (String) ass.next();
            AttributeSchemaImpl as = ss.getAttributeSchema(attr);
            AttributeValidator av = ss.getAttributeValidator(attr);
            String anyValue = as.getAny();
            if (inherit && (anyValue != null) &&
                (anyValue.indexOf("required") > -1)) {
                // Inherit default values of this attribute, if
                // required
                attrMap = av.inheritDefaults(attrMap);
                Set attrVals = (Set) attrMap.get(attr);
                if (attrVals == null || attrVals.isEmpty()) {
                    // A required attribute is being deleted
                    // throw an exception.
                    debug.error("ServiceSchema.validateAndInheritDefaults: "
                            + attr + " is a required attribute and cannot"
                            + " be deleted");
                    Object args[] = { attr };
                    throw new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                            "sms-required-attribute-delete", args);
                }
            } else if (inherit) {
                attrMap = av.inheritDefaults(attrMap);
            }
            // validate the attribute against Schema
            Set valSet = (Set) attrMap.get(attr);
            if (valSet != null) {
                String i18nFileName = (ssm != null) ? ssm.getI18NFileName()
                        : null;
                av.validate(valSet, i18nFileName, false, envMap);
            }
        }
        if (debug.messageEnabled()) {
            debug.error("ServiceSchema.validate&InheritDef: "
                    + " returning attrMap: " + attrMap.toString());
        }
        return attrMap;
    }

    /**
     * Adds the attribute schema to this service. The schema is defined in XML
     * input stream that follows the SMS DTD.
     * 
     * @param xmlAttrSchema
     *            the XML format of the attribute schema
     * @throws SMSException
     *             if an error occurred while performing the operation
     * @throws SSOException
     *             if the single sign on token is invalid or expired
     */
    public void addAttributeSchema(InputStream xmlAttrSchema)
            throws SSOException, SMSException {
        SMSEntry.validateToken(ssm.getSSOToken());
        // Check if attribute exists
        Document doc = SMSSchema.getXMLDocument(xmlAttrSchema, false);
        NodeList nl = doc.getElementsByTagName(SMSUtils.SCHEMA_ATTRIBUTE);
        CaseInsensitiveHashSet asNames =
            new CaseInsensitiveHashSet(ss.getAttributeSchemaNames());
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            AttributeSchemaImpl as = new AttributeSchemaImpl(node);
            if (asNames.contains(as.getName())) {
                Object[] args = { as.getName() };
                throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                        "sms-attributeschema-already-exists", args));
            }
        }
        appendAttributeSchema(nl);
    }

    /**
     * Removes the attribute schema from this service.
     * 
     * @param attrName
     *            the name of the attribute schema
     * @throws SMSException
     *             if an error occurred while performing the operation
     * @throws SSOException
     *             if the single sign on token is invalid or expired
     */
    public void removeAttributeSchema(String attrName) throws SSOException,
            SMSException {
        removeChildNode(SMSUtils.SCHEMA_ATTRIBUTE, attrName);
    }

    /**
     * Returns a map of all the attribute and their default values in this
     * schema.
     * 
     * @return Map of Attribute Names and Sets of their default values as
     *         defined in the Schema
     */
    public Map getAttributeDefaults() {
        return (ss.getAttributeDefaults());
    }

    /**
     * Returns an unmodifiable map of all the attribute and their default values
     * in this schema.
     * 
     * @return Map of Attribute Names and Sets of their default values as
     *         defined in the Schema
     */
    public Map getReadOnlyAttributeDefaults() {
        return (ss.getReadOnlyAttributeDefaults());
    }

    /**
     * Method to change the default values of attributes in the schema.
     * 
     * @param attrs
     *            A map of the names of <code>AttributeSchema</code> to
     *            modify, and a Set of Values which should replace the default
     *            values of the current schema.
     * @throws SMSException
     *             if an error occurred while performing the operation
     * @throws SSOException
     *             if the single sign on token is invalid or expired
     */
    public void setAttributeDefaults(Map attrs) throws SSOException,
            SMSException {
        SMSEntry.validateToken(ssm.getSSOToken());
        // Get a copy of the XML document
        Document document = ssm.getDocumentCopy();
        Iterator items = attrs.keySet().iterator();
        while (items.hasNext()) {
            String attrName = (String) items.next();
            AttributeSchema as = getAttributeSchema(attrName);
            if (as == null) {
                Object[] args = { attrName };
                throw (new SchemaException(IUMSConstants.UMS_BUNDLE_NAME,
                    "sms-invalid-attr-name", args)); 
            }
            as.setDefaultValues((Set) attrs.get(attrName), document);
        }
        // Save the document
        ssm.replaceSchema(document);
    }

    /**
     * Method to change default value for a specific attribute.
     * 
     * @param attrName
     *            Name of the attribute for which defaults values need to be
     *            replaced.
     * @param values
     *            Set of new values to replace the old ones.
     * @throws SchemaException
     *             if an error occurred while parsing the XML
     * @throws SMSException
     *             if an error occurred while performing the operation
     * @throws SSOException
     *             if the single sign on token is invalid or expired
     */
    public void setAttributeDefaults(String attrName, Set values)
            throws SchemaException, SMSException, SSOException {
        SMSEntry.validateToken(ssm.getSSOToken());
        AttributeSchema as = getAttributeSchema(attrName);
        if (as == null) {
            Object[] args = { attrName };
            throw (new SchemaException(IUMSConstants.UMS_BUNDLE_NAME,
                    "sms-invalid-attr-name", args));
        }
        as.setDefaultValues(values);
    }

    /**
     * Removes the default values of attributes in the schema.
     * 
     * @param attrs
     *            A set of the names of <code>AttributeSchema</code>.
     * @throws SMSException
     *             if an error occurred while performing the operation
     * @throws SSOException
     *             if the single sign on token is invalid or expired
     */
    public void removeAttributeDefaults(Set attrs) throws SMSException,
            SSOException {
        SMSEntry.validateToken(ssm.getSSOToken());
        Iterator it = attrs.iterator();
        while (it.hasNext()) {
            String asName = (String) it.next();
            AttributeSchema as = getAttributeSchema(asName);
            if (as == null) {
                throw (new InvalidAttributeNameException(
                        IUMSConstants.UMS_BUNDLE_NAME,
                        IUMSConstants.services_validator_invalid_attr_name,
                        null));
            }
            as.removeDefaultValues();
        }
    }

    /**
     * Returns the names of sub-schemas for the service.
     * 
     * @return the names of service's sub-schemas
     */
    public Set getSubSchemaNames() {
        return (ss.getSubSchemaNames());
    }

    /**
     * Returns <code>ServiceSchema</code> object given the name of the
     * service's sub-schema.
     * 
     * @param subSchemaName
     *            the name of the service's sub-schema
     * @return <code>ServiceSchema</code> object
     * @throws SMSException
     *             if an error occurred while performing the operation
     * 
     */
    public ServiceSchema getSubSchema(String subSchemaName) throws SMSException 
    {
        SMSEntry.validateToken(ssm.getSSOToken());
        ServiceSchema answer = null;
        ServiceSchemaImpl ssi = ss.getSubSchema(subSchemaName);
        if (ssi != null) {
            answer = new ServiceSchema(ssi,
                    componentName + "/" + subSchemaName, type, ssm);
        }
        return (answer);
    }

    /**
     * Adds the service's sub-schema given the XML input stream that follows the
     * SMS DTD.
     * 
     * @param xmlSubSchema
     *            the XML format of the sub-schema
     * @throws SMSException
     *             if an error occurred while performing the operation
     * @throws SSOException
     *             if the single sign on token is invalid or expired
     */
    public void addSubSchema(InputStream xmlSubSchema) throws SSOException,
            SMSException {
        SMSEntry.validateToken(ssm.getSSOToken());
        // Check if attribute exists
        Document doc = SMSSchema.getXMLDocument(xmlSubSchema, false);
        NodeList nl = doc.getElementsByTagName(SMSUtils.SUB_SCHEMA);
        Set asNames = ss.getSubSchemaNames();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            String nodeName = XMLUtils.getNodeAttributeValue(node,
                    SMSUtils.NAME);
            if (asNames.contains(nodeName)) {
                Object[] args = { nodeName };
                throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                        "sms-subschema-already-exists", args));
            }
        }

        appendSubSchema(doc);
    }

    /**
     * Removes the service's sub-schema from the service.
     * 
     * @param subSchemaName
     *            the name of the service's sub-schema
     * @throws SMSException
     *             if an error occurred while performing the operation
     * @throws SSOException
     *             if the single sign on token is invalid or expired
     */
    public void removeSubSchema(String subSchemaName) throws SSOException,
            SMSException {
        SMSEntry.validateToken(ssm.getSSOToken());
        removeChildNode(SMSUtils.SUB_SCHEMA, subSchemaName);
    }

    /**
     * Determines whether each attribute in the attribute set is valid. Iterates
     * though the set checking each element to see if there is a validator that
     * needs to execute.
     * 
     * @param attributeSet
     *            the <code>Map</code> where key is the attribute name and
     *            value is the <code>Set</code> of attribute values
     * @return true if all attributes are valid
     * @throws SMSException
     *             if an error occurred while performing the operation
     */
    public boolean validateAttributes(Map attributeSet) throws SMSException {
        SMSEntry.validateToken(ssm.getSSOToken());
        return (ss.validateAttributes(ssm.getSSOToken(), attributeSet, false,
            null));
    }

    /**
     * Determines whether each attribute in the attribute set is valid for the
     * given organization. Iterates though the set checking each element to see
     * if there is a validator that needs to execute.
     * 
     * @param attributeSet
     *            the <code>Map</code> where key is the attribute name and
     *            value is the <code>Set</code> of attribute values
     * @param orgName
     *            organization name
     * @return true if all attributes are valid
     * @throws SMSException
     *             if an error occurred while performing the operation
     */
    public boolean validateAttributes(Map attributeSet, String orgName)
            throws SMSException {
        SMSEntry.validateToken(ssm.getSSOToken());
        return (ss.validateAttributes(ssm.getSSOToken(), attributeSet, false, 
            orgName));
    }

    /**
     * Returns string representation of the schema.
     * 
     * @return string representation of the schema.
     */
    public String toString() {
        return (ss.toString());
    }

    /**
     * Returns the Node of this schema element. Used by Policy component's
     * <code>ServiceType</code> to get <code>ActionSchema</code>.
     * 
     * @return the Node of this schema element. Used by Policy component's
     *         <code>ServiceType</code> to get <code>ActionSchema</code>.
     */
    public Node getSchemaNode() {
        Node node = null;
        try {
            node = getSchemaNode(ssm.getDocumentCopy());
        } catch (SMSException ssme) {
            debug.error("ServiceSchema::getSchemaNode: invalid schema");
        }
        return (node);
    }

    private void appendAttributeSchema(NodeList nodes) throws SSOException, SMSException {
        if (nodes == null || nodes.getLength() == 0) {
            throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    IUMSConstants.SMS_SMSSchema_no_schema_element, null));
        }
        Document schemaDoc = ssm.getDocumentCopy();
        try {
            Node schemaNode = getSchemaNode(schemaDoc);
            NodeList childrens = schemaNode.getChildNodes();
            Node nextSibling = null;
            for (int i = 0; i < childrens.getLength(); i++) {
                Node child = childrens.item(i);
                if (Node.ELEMENT_NODE == child.getNodeType()
                        && !SMSUtils.SCHEMA_ATTRIBUTE.equals(child.getNodeName())) {
                    nextSibling = child;
                    break;
                }
            }
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                Node iNode = schemaDoc.importNode(node, true);
                schemaNode.insertBefore(iNode, nextSibling);
            }
        } catch (Exception e) {
            throw (new SMSException(e.getMessage(), e, "sms-cannot_append_NODE"));
        }
        ssm.replaceSchema(schemaDoc);
    }

    private void appendSubSchema(Document doc) throws SSOException, SMSException {
        if (doc == null) {
            throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    IUMSConstants.SMS_SMSSchema_no_schema_element, null));
        }
        
        Document schemaDoc = ssm.getDocumentCopy();
        
        try {
            Node schemaNode = getSchemaNode(schemaDoc);
            Node node = XMLUtils.getRootNode(doc, SMSUtils.SUB_SCHEMA);
            NodeList childrens = schemaNode.getChildNodes();
            Node nextSibling = null;
            for (int i = 0; i < childrens.getLength(); i++) {
                Node child = childrens.item(i);
                if (Node.ELEMENT_NODE == child.getNodeType()
                        && !SMSUtils.SCHEMA_ATTRIBUTE.equals(child.getNodeName())) {
                    //In this case we've found an entry that is not an AttributeSchema, so this is definitely an
                    //element before which we can place a SubSchema. (i.e. the found element was a SubSchema or
                    //an OrganizationAttributeSchema). Note that this will change the order of the SubSchema
                    //elements in the service description, however the order should not matter for SMS.
                    nextSibling = child;
                    break;
                }
            }
            Node iNode = schemaDoc.importNode(node, true);
            schemaNode.insertBefore(iNode, nextSibling);
        } catch (Exception ex) {
            throw (new SMSException(ex.getMessage(), ex, "sms-cannot_append_NODE"));
        }
       
        ssm.replaceSchema(schemaDoc);
    }

    // -----------------------------------------------------------
    // Protected methods
    // -----------------------------------------------------------
    void removeChildNode(String nodeType, String nodeName) throws SSOException,
            SMSException {
        if (debug.messageEnabled()) {
            debug.message("ServiceSchema::appendChildNode called for: "
                    + getServiceName() + "(" + ssm.getVersion() + ") "
                    + componentName);
        }
        Document schemaDoc = ssm.getDocumentCopy();
        Node schemaNode = getSchemaNode(schemaDoc);
        if (schemaNode != null) {
            Node node = XMLUtils.getNamedChildNode(schemaNode, nodeType,
                    SMSUtils.NAME, nodeName);
            if (node != null) {
                schemaNode.removeChild(node);
                ssm.replaceSchema(schemaDoc);
            }
        }
    }

    // -----------------------------------------------------------
    // Method to obtain schema node
    // -----------------------------------------------------------
    Node getSchemaNode(Document document) throws SMSException {
        NodeList nodes = document.getElementsByTagName(SMSUtils.SCHEMA);
        if ((nodes == null) || (nodes.getLength() == 0)) {
            throwInvalidSchemaException();
        }
        Node rNode = nodes.item(0);

        // Get the schema type node
        String schemaType = SMSUtils.GLOBAL_SCHEMA;
        if (type.equals(SchemaType.ORGANIZATION)) {
            schemaType = SMSUtils.ORG_SCHEMA;
        } else if (type.equals(SchemaType.DYNAMIC)) {
            schemaType = SMSUtils.DYNAMIC_SCHEMA;
        } else if (type.equals(SchemaType.USER)) {
            schemaType = SMSUtils.USER_SCHEMA;
        } else if (type.equals(SchemaType.POLICY)) {
            schemaType = SMSUtils.POLICY_SCHEMA;
        } else if (type.equals(SchemaType.GROUP)) {
            schemaType = SMSUtils.GROUP_SCHEMA;
        } else if (type.equals(SchemaType.DOMAIN)) {
            schemaType = SMSUtils.DOMAIN_SCHEMA;
        }

        Node stNode = XMLUtils.getChildNode(rNode, schemaType);
        if (stNode == null) {
            throwInvalidSchemaException();
        }

        // Walk the component name
        if ((componentName == null) || (componentName.length() == 0)) {
            return (stNode);
        } else if (orgAttrSchema) {
            // OrganizationAttributeSchema
            return (XMLUtils
                    .getChildNode(stNode, SMSUtils.ORG_ATTRIBUTE_SCHEMA));
        }

        StringTokenizer st = new StringTokenizer(componentName, "/");
        while (st.hasMoreTokens()) {
            String tokenName = st.nextToken();
            if ((tokenName == null) || (tokenName.length() == 0)) {
                continue;
            }
            if ((stNode = XMLUtils.getNamedChildNode(stNode,
                    SMSUtils.SUB_SCHEMA, SMSUtils.NAME, tokenName)) == null) {
                throwInvalidSchemaException();
            }
        }
        return (stNode);
    }

    // -----------------------------------------------------------
    // Method to obtain organizationattributeschema node
    // -----------------------------------------------------------
    Node getOrgAttrSchemaNode(Document doc) throws SMSException {
        NodeList nodes = doc.getElementsByTagName(
            SMSUtils.ORG_ATTRIBUTE_SCHEMA);
        if (nodes == null || (nodes.getLength() == 0)) {
            // Throw an exception
            throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                "sms-invalid-orgattr-schema-document", null));
        }
        Node rNode = nodes.item(0);
        if (rNode == null) {
            throwInvalidSchemaException();
        }
        return (rNode);
    }

    void throwInvalidSchemaException() throws SMSException {
        SMSEntry.debug.error("ServiceSchema::getSchemaNode: "
                + "Invalid service schema XML: " + getServiceName() + "("
                + ssm.getVersion() + ")");
        throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                IUMSConstants.SMS_SMSSchema_no_service_element, null));
    }

    static Debug debug = SMSEntry.debug;
}

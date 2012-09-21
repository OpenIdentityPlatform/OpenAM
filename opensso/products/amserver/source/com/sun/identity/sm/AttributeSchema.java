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
 * $Id: AttributeSchema.java,v 1.13 2009/01/13 06:56:08 mahesh_prasad_r Exp $
 *
 */

package com.sun.identity.sm;

import java.security.AccessController;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.iplanet.sso.SSOException;
import com.sun.identity.security.EncodeAction;

/**
 * The class <code>AttributeSchema</code> provides methods to access the
 * schema of a configuration parameter. Also, it provides methods to set default
 * and choice values.
 *
 * @supported.all.api
 */
public class AttributeSchema {
    // Debug
    private static Debug debug = SMSEntry.debug;

    // Instance variable
    ServiceSchemaManager ssm;

    ServiceSchema ss;

    PluginSchema ps;

    AttributeSchemaImpl as;

    /**
     * Constructor. Makes it private so that it cannot be instantiated.
     */
    private AttributeSchema() {
    }

    /**
     * Constructor used by ServiceSchema to instantiate
     * <code>AttributeSchema</code> objects.
     */
    protected AttributeSchema(AttributeSchemaImpl as, ServiceSchemaManager ssm,
            ServiceSchema ss) {
        this.ssm = ssm;
        this.ss = ss;
        this.as = as;
        if (as == null) {
            debug.error("AttributeSchema:: IMPL is NULL");
        }
    }

    protected AttributeSchema(AttributeSchemaImpl as, PluginSchema ps) {
        this.as = as;
        this.ps = ps;
    }

    /**
     * Returns the name of the attribute.
     * 
     * @return the name of the attribute
     */
    public String getName() {
        return (as.getName());
    }

    /**
     * Returns the type of the attribute.
     * 
     * @return the type of the attribute
     */
    public AttributeSchema.Type getType() {
        return (as.getType());
    }

    /**
     * Returns Service Schema.
     * 
     * @return Service Schema.
     */
    public ServiceSchema getServiceSchema() {
        return ss;
    }

    /**
     * Sets the type.
     * 
     * @param type
     *            to be changed to
     * @throws SMSException
     *             if an error is encountered when trying to set
     * @throws SSOException
     *             if the single sign on token is invalid or expired
     */
    public void setType(String type) throws SMSException, SSOException {
        updateXMLDocument(SMSUtils.ATTRIBUTE_TYPE, type);
    }

    /**
     * Returns the UI type of the attribute.
     * 
     * @return the UI type of the attribute; or null if the UI Type is not
     *         defined
     */
    public AttributeSchema.UIType getUIType() {
        return (as.getUIType());
    }

    /**
     * Sets the <code>UIType</code> attribute.
     * 
     * @param uiType
     *            user interface type.
     * @throws SMSException
     *             if an error is encountered when trying to set
     *             <code>UIType</code> to the attribute schema.
     * @throws SSOException
     *             if the single sign on token is invalid or expired
     */
    public void setUIType(String uiType) throws SMSException, SSOException {
        updateXMLDocument(SMSUtils.ATTRIBUTE_UITYPE, uiType);
    }

    /**
     * Returns the syntax of the attribute.
     * 
     * @return the syntax of the attribute
     */
    public AttributeSchema.Syntax getSyntax() {
        return (as.getSyntax());
    }

    /**
     * Sets the Syntax attribute.
     * 
     * @param synt
     *            syntax
     * @throws SMSException
     *             if an error is encountered when trying to set the attribute
     *             syntax
     * @throws SSOException
     *             if the single sign on token is invalid or expired
     */
    public void setSyntax(String synt) throws SMSException, SSOException {
        updateXMLDocument(SMSUtils.ATTRIBUTE_SYNTAX, synt);
    }

    /**
     * Returns the I18N key to describe the configuration attribute.
     * 
     * @return the I18N key to describe the configuration attribute
     */
    public String getI18NKey() {
        return (as.getI18NKey());
    }

    /**
     * Sets the I18N key to describe the configuration attribute.
     * 
     * @param i18nKey
     *            the I18N key to describe the attribute
     * @throws SMSException
     *             if an error is encountered when trying to set I18N key to the
     *             attribute schema
     * @throws SSOException
     *             if the single sign on token is invalid or expired
     */
    public void setI18NKey(String i18nKey) throws SMSException, SSOException {
        updateXMLDocument(SMSUtils.I18N_KEY, i18nKey);
    }

    /**
     * Returns the value of the <code>cosQualifier</code> for this attribute
     * that is <code>default, override, operational or merge-cos</code>.
     * 
     * @return the value of the <code>cosQualifier</code>.
     */
    public String getCosQualifier() {
        return (as.getCosQualifier());
    }

    /**
     * Sets the <code>cosQualifier</code> attribute
     * 
     * @param cosq
     *            value of <code>cosQualifier</code>.
     * @throws SMSException
     *             if an error is encountered when trying to set.
     * @throws SSOException
     *             if the single sign on token is invalid or expired
     */
    public void setCosQualifier(String cosq) throws SMSException, SSOException {
        updateXMLDocument(SMSUtils.ATTRIBUTE_COS_QUALIFIER, cosq);
    }

    /**
     * Returns the default values of the attribute. If there are no default
     * values defined for this attribute in the schema then this method returns
     * a Collections.EMPTY_SET
     * 
     * @return set of default values of the attribute
     */
    public Set getDefaultValues() {
        return (as.getDefaultValues());
    }

    /**
     * Returns the default values of the attribute for the given environment
     * parameters. If there are no default values defined for this attribute in
     * the schema then this method returns a Collections.EMPTY_SET
     * 
     * @param envParams
     *            Map of environment parameter to a set of values
     * @return set of default values of the attribute
     */
    public Set getDefaultValues(Map envParams) {
        return (as.getDefaultValues(envParams));
    }

    /**
     * Sets the default values of the attribute.
     * 
     * @param values
     *            the set of default values
     * @throws SMSException
     *             if an error is encountered when trying to set.
     * @throws SSOException
     *             if the single sign on token is invalid or expired
     */
    public void setDefaultValues(Set values) throws SMSException, SSOException {
        updateDefaultValues(values);
    }

    /**
     * Protected method to set the default values in the given XML document.
     * 
     * @throws SMSException
     *             if an error is encountered when trying to set.
     * @throws SSOException
     *             if the single sign on token is invalid or expired
     */
    void setDefaultValues(Set values, Document document) throws SMSException,
            SSOException {
        updateDefaultValues(values, document);
    }

    /**
     * Adds a default value to the existing set of default values.
     * 
     * @param value
     *            the default value to add
     * @throws SMSException
     *             if an error is encountered when trying to set.
     * @throws SSOException
     *             if the single sign on token is invalid or expired
     */
    public void addDefaultValue(String value) throws SMSException, SSOException
    {
        Set defaultValues = getDefaultValues();
        if (defaultValues != Collections.EMPTY_SET) {
            defaultValues.add(value);
        } else {
            defaultValues = new HashSet();
            defaultValues.add(value);
        }
        updateDefaultValues(defaultValues);
    }

    /**
     * Removes the all the default values for the attribute.
     * 
     * @throws SMSException
     *             if an error is encountered when trying to set.
     * @throws SSOException
     *             if the single sign on token is invalid or expired
     * 
     */
    public void removeDefaultValues() throws SMSException, SSOException {
        updateDefaultValues(new HashSet());
    }

    /**
     * Removes the given value from the set of default values.
     * 
     * @param value
     *            the default value to remove
     * @throws SMSException
     *             if an error is encountered when trying to set.
     * @throws SSOException
     *             if the single sign on token is invalid or expired
     */
    public void removeDefaultValue(String value) throws SMSException,
            SSOException {
        Set defaultValues = getDefaultValues();
        if (defaultValues != Collections.EMPTY_SET) {
            defaultValues.remove(value);
            updateDefaultValues(defaultValues);
        }
    }

    /**
     * Returns the possible choice values for the attribute if the attribute
     * type is either <code>SINGLE_CHOICE</code> or
     * <code>MULTIPLE_CHOICE</code>.
     * 
     * @return set of possible choice values
     */
    public String[] getChoiceValues() {
        return (as.getChoiceValues());
    }

    /**
     * Returns the possible choice values for the attribute if the attribute
     * type is either <code>SINGLE_CHOICE</code> or
     * <code>MULTIPLE_CHOICE</code>, for the given environment parameters.
     * 
     * @param envParams
     *            Map of environment parameter to a set of values
     * @return set of possible choice values
     */
    public String[] getChoiceValues(Map envParams) {
        return (as.getChoiceValues(envParams));
    }

    /**
     * Returns the I18N key for the given choice value.
     * 
     * @param cValue
     *            choice value.
     * @return the I18N key for the given choice value
     */
    public String getChoiceValueI18NKey(String cValue) {
        return (as.getChoiceValueI18NKey(cValue));
    }

    /**
     * Adds a choice value and its i18n key to the existing set of choice
     * values.
     * 
     * @param value
     *            the choice value to add
     * @param i18nKey
     *            the I18N key for the choice value
     * @throws SMSException
     *             if an error is encountered when trying to set.
     * @throws SSOException
     *             if the single sign on token is invalid or expired
     */
    public void addChoiceValue(String value, String i18nKey)
            throws SMSException, SSOException {
        Map choiceValues = as.getChoiceValuesMap();
        choiceValues.put(value, i18nKey);
        updateChoiceValues(choiceValues);
    }

    /**
     * Removes the given value from the set of choice values.
     * 
     * @param value
     *            the choice value to remove
     * @throws SMSException
     *             if an error is encountered when trying to set.
     * @throws SSOException
     *             if the single sign on token is invalid or expired
     */
    public void removeChoiceValue(String value) throws SMSException,
            SSOException {
        Map choiceValues = as.getChoiceValuesMap();
        if (choiceValues.remove(value) != null) {
            updateChoiceValues(choiceValues);
        }
    }

    /**
     * Returns the start range if the attribute syntax is either
     * <code>NUMBER_RANGE</code> or <code>DECIMAL_RANGE</code>.
     * 
     * @return the start range for the attribute value
     */
    public String getStartRange() {
        return (as.getStartRange());
    }

    /**
     * Sets the start range attribute.
     * 
     * @param stRange
     *            start range.
     * @throws SMSException
     *             if an error is encountered when trying to set
     * @throws SSOException
     *             if the single sign on token is invalid or expired
     */
    public void setStartRange(String stRange) throws SMSException, SSOException
    {
        updateXMLDocument(SMSUtils.ATTRIBUTE_RANGE_START, stRange);
    }

    /**
     * Returns the end range if the attribute syntax is either
     * <code>NUMBER_RANGE</code> or <code>DECIMAL_RANGE</code>.
     * 
     * @return the end range for the attribute value
     */
    public String getEndRange() {
        return (as.getEndRange());
    }

    /**
     * Sets the end range Attribute.
     * 
     * @param edRange
     *            end range.
     * @throws SMSException
     *             if an error is encountered when trying to set
     * @throws SSOException
     *             if the single sign on token is invalid or expired
     */
    public void setEndRange(String edRange) throws SMSException, SSOException {
        updateXMLDocument(SMSUtils.ATTRIBUTE_RANGE_END, edRange);
    }

    /**
     * Method to get the validator name for using to validate this service
     * attribute
     * 
     * @return the validator name
     */
    public String getValidator() {
        return (as.getValidator());
    }

    /**
     * Sets the Validator attribute
     * 
     * @param valid
     *            validator
     * @throws SMSException
     *             if an error is encountered when trying to set
     * @throws SSOException
     *             if the single sign on token is invalid or expired
     */
    public void setValidator(String valid) throws SMSException, SSOException {
        updateXMLDocument(SMSUtils.ATTRIBUTE_VALIDATOR, valid);
    }

    /**
     * Returns the minimum number of values for the attribute if the attribute
     * is of type <code>MULTIPLE_CHOICE</code>.
     * 
     * @return the minimum number of values
     */
    public int getMinValue() {
        return (as.getMinValue());
    }

    /**
     * Sets the minimum value attribute.
     * 
     * @param minV
     *            minimum value.
     * @throws SMSException
     *             if an error is encountered when trying to set
     * @throws SSOException
     *             if the single sign on token is invalid or expired
     */
    public void setMinValue(String minV) throws SMSException, SSOException {
        updateXMLDocument(SMSUtils.ATTRIBUTE_MIN_VALUE, minV);
    }

    /**
     * Returns the maximum number of values for the attribute if the attribute
     * is of type <code>MULTIPLE_CHOICE</code>.
     * 
     * @return the maximum number of values
     */
    public int getMaxValue() {
        return (as.getMaxValue());
    }

    /**
     * Sets the maximum value attribute.
     * 
     * @param maxV
     *            maximum value.
     * @throws SMSException
     *             if an error is encountered when trying to set
     * @throws SSOException
     *             if the single sign on token is invalid or expired
     */
    public void setMaxValue(String maxV) throws SMSException, SSOException {
        updateXMLDocument(SMSUtils.ATTRIBUTE_MAX_VALUE, maxV);
    }

    /**
     * Sets the boolean values of the attribute.
     *
     * @param trueValue string value for <code>BooleanTrueValue</code>.
     * @param trueValueI18nKey <code>I18N</code> key for
     *        <code>BooleanTrueValue</code>.
     * @param falseValue string value for <code>BooleanFalseValue</code>.
     * @param falseValueI18nKey <code>I18N</code> Key for
     *        <code>BooleanFalseValue</code>.
     * @throws SMSException if an error is encountered when trying to  set.
     * @throws SSOException if the single sign on token is invalid or expired
     */
    public void setBooleanValues(
        String trueValue,
        String trueValueI18nKey,
        String falseValue,
        String falseValueI18nKey
    ) throws SSOException, SMSException {
      updateBooleanValues(trueValue, trueValueI18nKey,
            falseValue, falseValueI18nKey, null);
    }


    /**
     * Returns the string value for <code>BooleanTrueValue</code>.
     * 
     * @return the string value for <code>BooleanTrueValue</code>.
     */
    public String getTrueValue() {
        return (as.getTrueValue());
    }

    /**
     * Returns the <code>I18N</code> key for <code>BooleanTrueValue</code>.
     * 
     * @return the <code>I18N</code> key for <code>BooleanTrueValue</code>.
     */
    public String getTrueValueI18NKey() {
        return (as.getTrueValueI18NKey());
    }

    /**
     * Returns the string value for <code>BooleanFalseValue</code>.
     * 
     * @return the string value for <code>BooleanFalseValue</code>.
     */
    public String getFalseValue() {
        return (as.getFalseValue());
    }

    /**
     * Returns the <code>I18N</code> Key for <code>BooleanFalseValue</code>.
     * 
     * @return the <code>I18N</code> Key for <code>BooleanFalseValue</code>.
     */
    public String getFalseValueI18NKey() {
        return (as.getFalseValueI18NKey());
    }

    /**
     * Returns true if the attribute is an optional attribute.
     * 
     * @return true if the attribute is an optional attribute.
     */
    public boolean isOptional() {
        return (as.isOptional());
    }

    /**
     * Returns true if the attribute is a service identifier (i.e., in the case
     * of LDAP it would be the COS Specifier attribute).
     * 
     * @return true if the attribute is service identifier attribute.
     */
    public boolean isServiceIdentifier() {
        return (as.isServiceIdentifier());
    }

    /**
     * Checks if the attribute allows to have resource name.
     * 
     * @return true if the attribute allows to have resource name; false
     *         otherwise
     */
    public boolean isResourceNameAllowed() {
        return (as.isResourceNameAllowed());
    }

    /**
     * Returns true if the attribute is a service's status attribute.
     * 
     * @return true if the attribute is a status attribute.
     */
    public boolean isStatusAttribute() {
        return (as.isStatusAttribute());
    }

    /**
     * Method to get service specific attributes. It return the value of the
     * "any" attribute, if set in the XML schema for the service
     * 
     * @return value of "any" attribute
     */
    public String getAny() {
        return (as.getAny());
    }

    /**
     * Sets the any attribute.
     * 
     * @param a
     *            value for any attribute.
     * @throws SMSException
     *             if an error is encountered when trying to set.
     * @throws SSOException
     *             if the single sign on token is invalid or expired.
     */
    public void setAny(String a) throws SMSException, SSOException {
        updateXMLDocument(SMSUtils.ATTRIBUTE_ANY, a);
    }

    /**
     * Returns URL of the view bean for the attribute.
     * 
     * @return URL for view bean
     */
    public String getPropertiesViewBeanURL() {
        return (as.getPropertiesViewBeanURL());
    }

    /**
     * Sets the URL of the view bean for the attribute.
     * 
     * @param prop
     *            properties view bean URL.
     * @throws SMSException
     *             if an error is encountered when trying to set.
     * @throws SSOException
     *             if the single sign on token is invalid or expired.
     */
    public void setPropertiesViewBeanUR(String prop) throws SMSException,
            SSOException {
        updateXMLDocument(SMSUtils.ATTRIBUTE_VIEW_BEAN_URL, prop);
    }

    /**
     * Returns <code>true</code> if the attribute is searchable;
     * <code>false</code> otherwise
     * 
     * @return <code>true</code> if the attribute is an optional attribute;
     *         <code>false</code> otherwise
     */
    public boolean isSearchable() {
        return (as.isSearchable());
    }

    /**
     * Sets the attribute isSearchable, if value is set to <code>true
     * </code>,
     * or <code>false</code>.
     * 
     * @param value
     *            if set to <code>true</code> the attribute will be
     *            searchable; else searches cannot be performed on this
     *            attribute.
     * @throws SMSException
     *             if an error is encountered when trying to set
     * @throws SSOException
     *             if the single sign on token is invalid or expired
     */
    public void setSearchable(String value) throws SMSException, SSOException {
        if ((!(value.toLowerCase()).equals("yes"))
                && (!(value.toLowerCase()).equals("no"))) {
            String[] arg = { value };
            debug.error("AttributeSchema: Invalid isSearchable value");
            throw new SMSException(SMSEntry.bundle
                    .getString("sms-invalid-searchable-value")
                    + ":" + arg, "sms-invalid-searchable-value");
        }
        updateXMLDocument(SMSUtils.ISSEARCHABLE, value);
    }

    /**
     * Returns a string representation of this <code> AttributeSchema </code>
     * object.
     * 
     * @return String representation of this object
     */
    public String toString() {
        return (as.toString());
    }

    /**
     * Method for modifying default values
     */
    protected void updateDefaultValues(Set defaultValues) throws SMSException,
            SSOException {
        updateDefaultValues(defaultValues, null);
    }

    /**
     * Method for modifying default values given the XML document
     */
    protected void updateDefaultValues(Set defaultValues, Document doc)
            throws SMSException, SSOException {
        // Check if the values are valid
        if (ss != null) {
            Map tempattrs = new HashMap(1);
            tempattrs.put(getName(), defaultValues);
            ss.validateAttributes(tempattrs);
        }

        // Check if the attributes have to be encoded
        boolean encode = false;
        if (getSyntax().equals(Syntax.PASSWORD)
                || getSyntax().equals(Syntax.ENCRYPTED_PASSWORD)) {
            encode = true;
        }

        // Construct DefaultValues node
        StringBuffer sb = new StringBuffer(100);
        sb.append(XML_PREFIX).append(DEFAULT_VALUES_BEGIN);
        Iterator items = defaultValues.iterator();
        while (items.hasNext()) {
            sb.append(VALUE_BEGIN);
            if (encode) {
                String encString = (String) items.next();
                try {
                    encString = (String) AccessController
                            .doPrivileged(new EncodeAction(encString));
                } catch (Throwable e) {
                    debug.error("AttributeSchema: Unable to encode", e);
                }
                sb.append(encString);
            } else {
                sb.append(SMSSchema.escapeSpecialCharacters((String) items
                        .next()));
            }
            sb.append(VALUE_END);
        }
        sb.append(DEFAULT_VALUES_END);
        updateXMLDocument(sb, SMSUtils.ATTRIBUTE_DEFAULT_ELEMENT, doc);
    }

    protected void updateChoiceValues(Map choiceValues) throws SMSException,
            SSOException {
        updateChoiceValues(choiceValues, null);
    }

     protected void updateChoiceValues(Map choiceValues, Document doc)
            throws SMSException, SSOException {
        // Construct ChoiceValues
        StringBuffer sb = new StringBuffer(100);
        sb.append(XML_PREFIX).append(CHOICE_VALUES_BEGIN);
        Iterator items = choiceValues.keySet().iterator();
        while (items.hasNext()) {
            String[] vals = new String[2];
            String value = SMSSchema.escapeSpecialCharacters((String) items
                    .next());
            String i18nKey = (String) choiceValues.get(value);
            if (i18nKey == null) {
                vals[0] = value;
                sb.append(MessageFormat.format(CHOICE_VALUE, (Object[])vals));
            } else {
                vals[0] = i18nKey;
                vals[1] = value;
                sb.append(MessageFormat.format(
                    CHOICE_VALUE_KEY, (Object[])vals));
            }
        }
        sb.append(CHOICE_VALUES_END);
        updateXMLDocument(sb, SMSUtils.ATTRIBUTE_CHOICE_VALUES_ELEMENT, doc);
    }

    protected void updateBooleanValues(
        String trueValue,
        String trueValueI18nKey,
        String falseValue,
        String falseValueI18nKey,
        Document doc
    ) throws SMSException, SSOException {
      // Construct BooleanValues
      StringBuffer sb = new StringBuffer(100);
      sb.append(XML_PREFIX).append(BOOLEAN_VALUES_BEGIN);

      String[] trueVals = new String[2];
        if ((trueValueI18nKey != null) && (trueValue != null)) {
            trueVals[0] = trueValueI18nKey;
            trueVals[1] = SMSSchema.escapeSpecialCharacters(trueValue);
        } else {
            trueVals[0] = getTrueValueI18NKey();
            trueVals[1] = getTrueValue();
        }
        sb.append(MessageFormat.format(TRUE_BOOLEAN_KEY, (Object[])trueVals));

      String[] falseVals = new String[2];
      if ((falseValueI18nKey != null) && (falseValue != null)) {
          falseVals[0] = falseValueI18nKey;
          falseVals[1] = SMSSchema.escapeSpecialCharacters(falseValue);
      } else {
            falseVals[0] = getFalseValueI18NKey();
            falseVals[1] = getFalseValue();
      }
      sb.append(MessageFormat.format(FALSE_BOOLEAN_KEY, (Object[])falseVals));

      sb.append(BOOLEAN_VALUES_END);
      updateXMLDocument(sb, SMSUtils.ATTRIBUTE_BOOLEAN_VALUES_ELEMENT, doc);
    }

    protected void updateXMLDocument(StringBuffer sb, String elementName,
            Document updateDoc) throws SMSException, SSOException {
        // Update the default element in XML
        try {
            // Construct the XML document
            Document doc = SMSSchema.getXMLDocument(sb.toString(), false);
            Node node = XMLUtils.getRootNode(doc, elementName);

            // Convert to Schema's document
            Document schemaDoc = null;
            if (updateDoc != null) {
                schemaDoc = updateDoc;
            } else if (ssm != null) {
                schemaDoc = ssm.getDocumentCopy();
            } else {
                schemaDoc = ps.getDocumentCopy();
            }
            Node nNode = schemaDoc.importNode(node, true);

            // Traverse the document to get this attribute element
            Node schemaNode = null;
            if (ss != null) {
                schemaNode = ss.getSchemaNode(schemaDoc);
            } else {
                schemaNode = ps.getPluginSchemaNode(schemaDoc);
            }
            Node attrSchemaNode = XMLUtils.getNamedChildNode(schemaNode,
                    SMSUtils.SCHEMA_ATTRIBUTE, SMSUtils.NAME, getName());

            // Try getting OrganizationAttributeSchema if AttributeSchema
            // node is not there within Organization node.
            // This will be a special case for idrepo service.
            if (attrSchemaNode == null) {
                schemaNode = ss.getOrgAttrSchemaNode(schemaDoc);
                attrSchemaNode = XMLUtils.getNamedChildNode(schemaNode,
                    SMSUtils.SCHEMA_ATTRIBUTE, SMSUtils.NAME, getName());
            }
            Node oNode = XMLUtils.getChildNode(attrSchemaNode, elementName);
            if (oNode != null) {
                attrSchemaNode.replaceChild(nNode, oNode);
            } else {
                attrSchemaNode.appendChild(nNode);
            }
            // Update the schema in the directory
            if (updateDoc != null) {
                // do nothing
            } else if (ssm != null) {
                ssm.replaceSchema(schemaDoc);
            } else {
                ps.replacePluginSchema(schemaDoc);
            }
        } catch (Exception e) {
            throw (new SMSException(e.getMessage(), e,
                    "sms-cannot-update-xml-document"));
        }
    }

    /**
     * update attribute value in attribute schema element
     */
    protected void updateXMLDocument(String attrName, String attrValue)
            throws SMSException, SSOException {
        // Update the default element in XML
        try {
            // Construct the XML document
            Document schemaDoc = null;
            if (ssm != null) {
                schemaDoc = ssm.getDocumentCopy();
            } else {
                schemaDoc = ps.getDocumentCopy();
            }

            // Traverse the document to get this attribute element
            Node schemaNode = null;
            if (ss != null) {
                schemaNode = ss.getSchemaNode(schemaDoc);
            } else {
                schemaNode = ps.getPluginSchemaNode(schemaDoc);
            }

            Node attrSchemaNode = XMLUtils.getNamedChildNode(schemaNode,
                    SMSUtils.SCHEMA_ATTRIBUTE, SMSUtils.NAME, getName());
            ((Element) attrSchemaNode).setAttribute(attrName, attrValue);

            // Update the schema in the directory
            if (ssm != null) {
                ssm.replaceSchema(schemaDoc);
            } else {
                ps.replacePluginSchema(schemaDoc);
            }
        } catch (Exception e) {
            throw (new SMSException(e.getMessage(), e,
                    "sms-cannot-update-xml-document"));
        }
    }

    /**
     * The class <code>Type</code> defines the types of schema attributes and
     * provides static constants for these types. This could also be viewed as a
     * higher level structured data types like Set, List, etc. The primitive
     * data types are defined by <code>Syntax</code>. Currently defined
     * schema attribute types are <code>SINGLE</code>, <code>LIST</code>,
     * <code>SINGLE_CHOICE</code>, <code>MULTIPLE_CHOICE</code>,
     * <code>SIGNATURE</code> and <code>VALIDATOR</code>.
     */
    public static class Type extends Object {

        /**
         * The <code>SINGLE</code> attribute type specifies that the attribute
         * can have only a single value.
         */
        public static final Type SINGLE = new Type("single");

        /**
         * The <code>LIST</code> attribute type specifies that the attribute
         * can have multiple values, i.e., multi-valued attribute.
         */
        public static final Type LIST = new Type("list");

        /**
         * The <code>SINGLE_CHOICE</code> attribute type specifies that the
         * attribute can have value defined by the <code>getChoiceValues</code>
         * method of <code>AttributeSchema</code>.
         */
        public static final Type SINGLE_CHOICE = new Type("single_choice");

        /**
         * The <code>MULTIPLE_CHOICE</code> attribute type specifies that the
         * attribute can have multiple values defined by the
         * <code>getChoiceValues</code> method of <code>AttributeSchema</code>.
         */
        public static final Type MULTIPLE_CHOICE = new Type("multiple_choice");

        /**
         * The <code>SIGNATURE</code> attribute type specifies that the
         * attribute is a signing attribute.
         */
        public static final Type SIGNATURE = new Type("signature");

        /**
         * The <code>VALIDATOR</code> attribute type specifies that the
         * attribute defines a attribute validator plugin.
         */
        public static final Type VALIDATOR = new Type("validator");

        private String attrType;

        private Type() {
        }

        private Type(String type) {
            attrType = type;
        }

        /**
         * The method returns the string representation of the schema attribute
         * type.
         * 
         * @return String string representation of schema attribute type
         */
        public String toString() {
            return attrType;
        }

        /**
         * Method to check if two schema attribute types are equal.
         * 
         * @param schemaAttrType
         *            the reference object with which to compare
         * 
         * @return <code>true</code> if the objects are same; <code>
         * false</code>
         *         otherwise
         */
        public boolean equals(Object schemaAttrType) {
            if (schemaAttrType instanceof Type) {
                Type s = (Type) schemaAttrType;
                return (s.attrType.equals(attrType));
            }
            return (false);
        }

        /**
         * Returns a hash code value for the object.
         * 
         * @return a hash code value for the object
         */
        public int hashCode() {
            return attrType.hashCode();
        }
    }

    /**
     * The class <code>UIType</code> defines the UI types of schema attributes
     * and provides static constants for these types. These types mainly will be
     * used by the GUI to determine how to display the schema attributes.
     * Currently defined schema attribute UI types are <code>RADIO</code>,
     * <code>LINK</code>, <code>BUTTON</code> and
     * <code>NAME_VALUE_LIST</code>
     */
    public static class UIType extends Object {

        /**
         * The <code>RADIO</code> attribute type specifies that the attribute
         * should be display as radio button.
         */
        public static final UIType RADIO = new UIType("radio");

        /**
         * The <code>LINK</code> attribute type specifies that the attribute
         * should be display as a link.
         */
        public static final UIType LINK = new UIType("link");

        /**
         * The <code>BUTTON</code> attribute type specifies that the attribute
         * should be display as a button.
         */
        public static final UIType BUTTON = new UIType("button");

        /**
         * The <code>NAME_VALUE_LIST</code> attribute type specifies that the
         * attribute should be display as a name value list widget.
         */
        public static final UIType NAME_VALUE_LIST = new UIType(
            "name_value_list");

        /**
         * The <code>UNORDERED_LIST</code> attribute type specifies that the
         * attribute should be display as an unordered list widget.
         */
        public static final UIType UNORDEREDLIST = new UIType("unorderedlist");

        /**
         * The <code>ORDERED_LIST</code> attribute type specifies that the
         * attribute should be display as an ordered list widget.
         */
        public static final UIType ORDEREDLIST = new UIType("orderedlist");

        /**
         * The <code>MAP_LIST</code> attribute type specifies that the
         * attribute should be display as an map list widget.
         */
        public static final UIType MAPLIST = new UIType("maplist");

        /**
         * The <code>GLOBALMAP_LIST</code> attribute type specifies that the
         * attribute should be display as a global map list widget.
         */
        public static final UIType GLOBALMAPLIST = new UIType("globalmaplist");
        
        /**
         * The <code>ADDREMOVELIST</code> attribute type specifies that the
         * multiple choice attribute should be display as add remove list
         * widget.
         */
        public static final UIType ADDREMOVELIST = new UIType("addremovelist");

        private String attrType;

        private UIType() {
        }

        private UIType(String type) {
            attrType = type;
        }

        /**
         * The method returns the string representation of the schema attribute
         * UI type.
         * 
         * @return String string representation of schema attribute UI type
         */
        public String toString() {
            return attrType;
        }

        /**
         * Method to check if two schema attribute UI types are equal.
         * 
         * @param schemaAttrType
         *            the reference object with which to compare
         * 
         * @return <code>true</code> if the objects are same; <code>
         * false</code>
         *         otherwise
         */
        public boolean equals(Object schemaAttrType) {
            if (schemaAttrType instanceof UIType) {
                UIType s = (UIType) schemaAttrType;
                return (s.attrType.equals(attrType));
            }
            return (false);
        }

        /**
         * Returns a hash code value for the object.
         * 
         * @return a hash code value for the object
         */
        public int hashCode() {
            return attrType.hashCode();
        }
    }

    /**
     * The class <code>Syntax</code> defines the syntax of the schema
     * attributes and provides static constants for these types. In other words,
     * this class defines the primitive data types for the schema attributes.
     */
    public static class Syntax {

        /**
         * The <code>BOOLEAN</code> attribute syntax specifies that the
         * attribute is of boolean type, i.e., can have a value of either
         * <code>true</code> or <code>
         * false</code>
         */
        public static final Syntax BOOLEAN = new Syntax("boolean");

        /**
         * The <code>EMAIL</code> attribute syntax specifies that the
         * attribute is a email address.
         */
        public static final Syntax EMAIL = new Syntax("email");

        /**
         * The <code>URL</code> attribute syntax specifies that the attribute
         * is a URL.
         */
        public static final Syntax URL = new Syntax("url");

        /**
         * The <code>STRING</code> attribute syntax specifies that the
         * attribute is of text type, i.e., can have any unicode characters.
         */
        public static final Syntax STRING = new Syntax("string");

        /**
         * The <code>PARAGRAPH</code> attribute syntax specifies that the
         * attribute is of multi-lined text type.
         */
        public static final Syntax PARAGRAPH = new Syntax("paragraph");

        /**
         * The <code>XML</code> attribute syntax specifies that the attribute
         * is of XML type, i.e., can have any unicode characters.
         */
        public static final Syntax XML = new Syntax("xml");

        /**
         * The <code>PASSWORD</code> attribute syntax specifies that the
         * attribute is of password type, will be used by UI to mask the
         * password typed.
         */
        public static final Syntax PASSWORD = new Syntax("password");

        /**
         * The <code>ENCRYPTED PASSWORD</code> attribute syntax specifies that
         * the attribute is of password type, will be used by UI to mask the
         * password typed.
         */
        public static final Syntax ENCRYPTED_PASSWORD = new Syntax(
                "encrypted_password");

        /**
         * The <code>DATE</code> attribute syntax specifies that the attribute
         * is of date type.
         */
        public static final Syntax DATE = new Syntax("date");

        /**
         * The <code>NUMERIC</code> attribute syntax specifies that the
         * attribute is numeric, i.e., can have numbers only.
         */
        public static final Syntax NUMERIC = new Syntax("numeric");

        /**
         * The <code>NUMBER</code> attribute syntax specifies that the
         * attribute is a number.
         */
        public static final Syntax NUMBER = new Syntax("number");

        /**
         * The <code>DECIMAL</code> attribute syntax specifies that the
         * attribute is a decimal value.
         */
        public static final Syntax DECIMAL = new Syntax("decimal");

        /**
         * The <code>PERCENT</code> attribute syntax specifies that the
         * attribute is a percentage.
         */
        public static final Syntax PERCENT = new Syntax("percent");

        /**
         * The <code>NUMBER_RANGE</code> attribute syntax specifies that the
         * attribute is a number within a range.
         */
        public static final Syntax NUMBER_RANGE = new Syntax("number_range");

        /**
         * The <code>DECIMAL_RANGE</code> attribute syntax specifies that the
         * attribute is a decimal number within a range.
         */
        public static final Syntax DECIMAL_RANGE = new Syntax("decimal_range");

        /**
         * The <code>DECIMAL_NUMBER</code> attribute syntax specifies that the
         * attribute is a floating point number, e.g., 1.5, 3.56, etc.
         */
        public static final Syntax DECIMAL_NUMBER = 
            new Syntax("decimal_number");

        /**
         * The <code>DN</code> attribute syntax specifies that the attribute
         * should be an LDAP distinguished name (DN).
         */
        public static final Syntax DN = new Syntax("dn");

        private String attrSyntax;

        private Syntax() {
        }

        private Syntax(String syntax) {
            attrSyntax = syntax;
        }

        /**
         * The method returns the string representation of the schema attribute
         * syntax.
         * 
         * @return String string representation of schema attribute syntax
         */
        public String toString() {
            return (attrSyntax);
        }

        /**
         * Method to check if two schema attribute syntax are equal.
         * 
         * @param schemaAttrSyntax
         *            the reference object with which to compare
         * 
         * @return <code>true</code> if the objects are same; <code>
         * false</code>
         *         otherwise
         */
        public boolean equals(Object schemaAttrSyntax) {
            if (schemaAttrSyntax instanceof Syntax) {
                Syntax s = (Syntax) schemaAttrSyntax;
                return (s.attrSyntax.equals(attrSyntax));
            }
            return (false);
        }

        /**
         * Returns a hash code value for the object.
         * 
         * @return a hash code value for the object
         */
        public int hashCode() {
            return attrSyntax.hashCode();
        }
    }

    private static final String XML_PREFIX = 
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

    private static final String DEFAULT_VALUES_BEGIN = "<"
            + SMSUtils.ATTRIBUTE_DEFAULT_ELEMENT + ">";

    private static final String DEFAULT_VALUES_END = "</"
            + SMSUtils.ATTRIBUTE_DEFAULT_ELEMENT + ">";

    static final String VALUE_BEGIN = "<" + SMSUtils.ATTRIBUTE_VALUE + ">";

    static final String VALUE_END = "</" + SMSUtils.ATTRIBUTE_VALUE + ">";

    private static final String CHOICE_VALUES_BEGIN = "<"
            + SMSUtils.ATTRIBUTE_CHOICE_VALUES_ELEMENT + ">";

    private static final String CHOICE_VALUES_END = "</"
            + SMSUtils.ATTRIBUTE_CHOICE_VALUES_ELEMENT + ">";

    private static final String CHOICE_VALUE_KEY = "<"
            + SMSUtils.ATTRIBUTE_CHOICE_VALUE_ELEMENT + " " + SMSUtils.I18N_KEY
            + "=\"{0}\">{1}</" + SMSUtils.ATTRIBUTE_CHOICE_VALUE_ELEMENT + ">";

    private static final String CHOICE_VALUE = "<"
            + SMSUtils.ATTRIBUTE_CHOICE_VALUE_ELEMENT + ">{0}</"
            + SMSUtils.ATTRIBUTE_CHOICE_VALUE_ELEMENT + ">";

    private static final String BOOLEAN_VALUES_BEGIN =
      "<" + SMSUtils.ATTRIBUTE_BOOLEAN_VALUES_ELEMENT + ">";

    private static final String BOOLEAN_VALUES_END =
      "</" + SMSUtils.ATTRIBUTE_BOOLEAN_VALUES_ELEMENT + ">";

    private static final String TRUE_BOOLEAN_KEY =
      "<" + SMSUtils.ATTRIBUTE_TRUE_BOOLEAN_ELEMENT +
      " " + SMSUtils.I18N_KEY + "=\"{0}\">{1}</" +
      SMSUtils.ATTRIBUTE_TRUE_BOOLEAN_ELEMENT + ">";

    private static final String FALSE_BOOLEAN_KEY =
      "<" + SMSUtils.ATTRIBUTE_FALSE_BOOLEAN_ELEMENT +
      " " + SMSUtils.I18N_KEY + "=\"{0}\">{1}</" +
      SMSUtils.ATTRIBUTE_FALSE_BOOLEAN_ELEMENT + ">";
}

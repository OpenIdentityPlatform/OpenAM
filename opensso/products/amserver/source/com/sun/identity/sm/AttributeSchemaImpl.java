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
 * $Id: AttributeSchemaImpl.java,v 1.3 2008/06/25 05:44:03 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */

package com.sun.identity.sm;

import com.sun.identity.security.DecodeAction;
import com.sun.identity.shared.xml.XMLUtils;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * The class <code>AttributeSchemaImpl</code> provides methods to access the
 * schema of a configuration parameter.
 */
public class AttributeSchemaImpl {

    // Input variables
    private Node attrSchemaNode;

    // Variables derived from the Node
    private String name;

    private String key;

    private AttributeSchema.Type type;

    private AttributeSchema.UIType uitype;

    private AttributeSchema.Syntax syntax;

    private Set defaultValues = null;

    private DefaultValues defaultsObject = null;

    private Map choiceValues;

    private ChoiceValues choiceObject = null;

    private String trueBooleanValue;

    private String trueValueKey;

    private String falseBooleanValue;

    private String falseValueKey;

    private String cosQualifier;

    private String rangeStart;

    private String rangeEnd;

    private int minValue;

    private int maxValue;

    private String validator;

    private boolean isOptional;

    private boolean isServiceIdentifier;

    private boolean isResourceNameAllowed;

    private boolean isStatusAttribute;

    private String any;

    // Attribute display properties
    private String attributeViewBeanURL;

    boolean isSearchable;

    /**
     * Constructor used by ServiceSchema to instantiate AttributeSchema objects.
     */
    protected AttributeSchemaImpl(Node node) {
        update(node);
    }

    /**
     * Retunrs the name of the attribute.
     */
    public String getName() {
        return (name);
    }

    /**
     * Returns the type of the attribute.
     */
    public AttributeSchema.Type getType() {
        return (type);
    }

    /**
     * Returns the UI type of the attribute; or null if UI type is not defined.
     */
    public AttributeSchema.UIType getUIType() {
        return (uitype);
    }

    /**
     * Returns the syntax of the attribute.
     */
    public AttributeSchema.Syntax getSyntax() {
        return (syntax);
    }

    /**
     * Returns the I18N key to describe the configuration attribute.
     */
    public String getI18NKey() {
        return (key);
    }

    /**
     * Returns the value of the cosQualifier for this attribute. Either default,
     * overrid, operational or merge-cos.
     * 
     * @return the value of the cosQualifier for this attribute.
     */
    public String getCosQualifier() {
        return (cosQualifier);
    }

    /**
     * Returns the default values of the attribute.
     */
    public Set getDefaultValues() {
        if (defaultsObject != null) {
            defaultValues = defaultsObject.getDefaultValues();
        }
        if ((defaultValues != null) && (!defaultValues.isEmpty())) {
            HashSet answer = new HashSet();
            answer.addAll(defaultValues);
            return (answer);
        }
        return (Collections.EMPTY_SET);
        // return (new HashSet());
    }

    /**
     * Returns the default values of the attribute.
     * 
     * @param envParams
     *            Map of environment parameter to a set of values
     * @return default values for the attribute
     */
    public Set getDefaultValues(Map envParams) {
        if (defaultsObject != null) {
            defaultValues = defaultsObject.getDefaultValues(envParams);
        }
        if ((defaultValues != null) && (!defaultValues.isEmpty())) {
            HashSet answer = new HashSet();
            answer.addAll(defaultValues);
            return (answer);
        }
        return (Collections.EMPTY_SET);
        // return (new HashSet());
    }

    /**
     * Returns the possible choice values for the attribute if the attribute
     * type is either <code>SINGLE_CHOICE</code> or
     * <code>MULTIPLE_CHOICE</code>.
     * 
     * @return choice values for the attribute
     */
    public String[] getChoiceValues() {
        if (choiceObject != null) {
            choiceValues = choiceObject.getChoiceValues();
        }
        if (choiceValues != null) {
            Set ret = choiceValues.keySet();
            return ((String[]) ret.toArray(new String[ret.size()]));
        }
        return (null);
    }

    /**
     * Returns the possible choice values for the attribute if the attribute
     * type is either <code>SINGLE_CHOICE</code> or
     * <code>MULTIPLE_CHOICE</code>.
     * 
     * @param envParams
     *            Map of environment parameter to a set of values
     * @return choice values for the attribute
     */
    public String[] getChoiceValues(Map envParams) {
        if (choiceObject != null) {
            choiceValues = choiceObject.getChoiceValues(envParams);
        }
        if (choiceValues != null) {
            Set ret = choiceValues.keySet();
            return ((String[]) ret.toArray(new String[ret.size()]));
        }
        return (null);
    }

    /**
     * Returns the choice values as a Map.
     * 
     * @return choice values for the attribute and its i18n key
     */
    protected Map getChoiceValuesMap() {
        if (choiceObject != null) {
            choiceValues = choiceObject.getChoiceValues();
        }
        if (choiceValues != null) {
            return (choiceValues);
        }
        return (new HashMap());
    }

    /**
     * Returns the choice values as a set.
     * 
     * @return choice values for the attribute
     */
    protected Set getChoiceValuesSet() {
        if (choiceObject != null) {
            choiceValues = choiceObject.getChoiceValues();
        }
        if (choiceValues != null) {
            return (choiceValues.keySet());
        }
        return (new HashSet());
    }

    /**
     * Returns the choice values as a Map.
     * 
     * @param envParams
     *            Map of environment parameter to a set of values
     * @return choice values for the attribute
     */
    protected Map getChoiceValuesMap(Map envParams) {
        if (choiceObject != null) {
            choiceValues = choiceObject.getChoiceValues(envParams);
        }
        if (choiceValues != null) {
            return (choiceValues);
        }
        return (new HashMap());
    }

    /**
     * Returns the choice values as a set.
     * 
     * @param envParams
     *            Map of environment parameter to a set of values
     * @return choice values for the attribute
     */
    protected Set getChoiceValuesSet(Map envParams) {
        if (choiceObject != null) {
            choiceValues = choiceObject.getChoiceValues(envParams);
        }
        if (choiceValues != null) {
            return (choiceValues.keySet());
        }
        return (new HashSet());
    }

    /**
     * Returns the I18N key for the given choice value.
     * 
     * @return the I18N key for the given choice value
     */
    public String getChoiceValueI18NKey(String cValue) {
        if (choiceObject != null) {
            choiceValues = choiceObject.getChoiceValues();
        }
        return ((String) choiceValues.get(cValue));
    }

    /**
     * Returns the start range if the attribute syntax is either
     * <code>NUMBER_RANGE</code> or <code>DECIMAL_RANGE</code>.
     */
    public String getStartRange() {
        return (rangeStart);
    }

    /**
     * Returns the end range if the attribute syntax is either
     * <code>NUMBER_RANGE</code> or <code>DECIMAL_RANGE</code>.
     */
    public String getEndRange() {
        return (rangeEnd);
    }

    /**
     * Method to get the validator name for using to validate this service
     * attribute
     * 
     * @return the validator name
     */
    public String getValidator() {
        return (validator);
    }

    /**
     * Returns the minimum number of values for the attribute if the attribute
     * is of type <code>MULTIPLE_CHOICE</code>.
     */
    public int getMinValue() {
        return (minValue);
    }

    /**
     * Returns the maximum number of values for the attribute if the attribute
     * is of type <code>MULTIPLE_CHOICE</code>.
     */
    public int getMaxValue() {
        return (maxValue);
    }

    /**
     * Returns the string value for BooleanTrueValue.
     */
    public String getTrueValue() {
        return (trueBooleanValue);
    }

    /**
     * Retruns the I18N Key for BooleanTrueValue.
     */
    public String getTrueValueI18NKey() {
        return (trueValueKey);
    }

    /**
     * Returns the string value for BooleanFalseValue.
     */
    public String getFalseValue() {
        return (falseBooleanValue);
    }

    /**
     * Returns the I18N Key for BooleanFalseValue.
     */
    public String getFalseValueI18NKey() {
        return (falseValueKey);
    }

    /**
     * Checks if the attribute is an optional attribute.
     */
    public boolean isOptional() {
        return (isOptional);
    }

    /**
     * Chekcs if the attribute is a service identifier (i.e., in the case of
     * LDAP it would be the COS Specifier attribute).
     */
    public boolean isServiceIdentifier() {
        return (isServiceIdentifier);
    }

    /**
     * Checks if the attribute allows to have resource name.
     */
    public boolean isResourceNameAllowed() {
        return (isResourceNameAllowed);
    }

    /**
     * Checkds if the attribute is a service's status attribute.
     */
    public boolean isStatusAttribute() {
        return (isStatusAttribute);
    }

    /**
     * Method to get service specific attributes. It return the value of the
     * "any" attribute, if set in the XML schema for the service
     */
    public String getAny() {
        return (any);
    }

    /**
     * Returns URL of the view bean for the attribute.
     * 
     * @return URL for view bean
     */
    public String getPropertiesViewBeanURL() {
        return (attributeViewBeanURL);
    }

    /**
     * String represenation of the AttributeSchema
     */
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("Attr Name=").append(name);
        buf.append("\n\tType=").append(type);
        buf.append("\n\tUIType=").append(uitype);
        buf.append("\n\tSyntax=").append(syntax);
        buf.append("\n\tI18n Key=").append(key);
        buf.append("\n\tDefault values=").append(defaultValues);
        buf.append("\n\tChoice Values=").append(choiceValues);
        buf.append("\n\tRangeStart=").append(rangeStart);
        buf.append("\n\tRangeEnd=").append(rangeEnd);
        buf.append("\n\tMinValue=").append(minValue);
        buf.append("\n\tMaxValue=").append(maxValue);
        buf.append("\n\tCoS Qualifier=").append(cosQualifier);
        buf.append("\n\tAny=").append(any);
        buf.append("\n\tView Bean URL=").append(attributeViewBeanURL);
        buf.append("\n\tisOptional=").append(isOptional);
        buf.append("\n\tisServiceIdentifier=").append(isServiceIdentifier);
        buf.append("\n\tisResourceNameAllowed=").append(isResourceNameAllowed);
        buf.append("\n\tisStatusAttribute=").append(isStatusAttribute);
        buf.append("\n\tisSearchable=").append(isSearchable);
        buf.append("\n");
        return buf.toString();
    }
    
    public Node getAttributeSchemaNode() {
        return attrSchemaNode;
    }

    /**
     * Updates the attribute schema object based on information in the XML node
     */
    void update(Node n) {
        Node node;

        // Copy the XML node
        attrSchemaNode = n;

        // Get attribute name
        name = XMLUtils.getNodeAttributeValue(n, SMSUtils.NAME);

        // Get I18N key
        key = XMLUtils.getNodeAttributeValue(n, SMSUtils.I18N_KEY);

        // Get Attribute type
        String attrType = XMLUtils.getNodeAttributeValue(n,
                SMSUtils.ATTRIBUTE_TYPE);
        type = AttributeSchema.Type.LIST;
        if (attrType != null) {
            try {
                Class attrClass = (AttributeSchema.Type.LIST).getClass();
                type = (AttributeSchema.Type) (attrClass.getField(attrType
                        .toUpperCase()).get(AttributeSchema.Type.LIST));
            } catch (Exception e) {
                // do nothing, use the default
            }
        }

        // Get attribute UI type
        String attrUIType = XMLUtils.getNodeAttributeValue(n,
                SMSUtils.ATTRIBUTE_UITYPE);
        uitype = null;
        if (attrUIType != null) {
            try {
                Class attrClass = (AttributeSchema.UIType.LINK).getClass();
                uitype = (AttributeSchema.UIType) (attrClass
                        .getField(attrUIType.toUpperCase())
                        .get(AttributeSchema.UIType.LINK));
            } catch (Exception e) {
                // do nothing, use the default
            }
        }

        // Get attribute syntax
        String attrSyntax = XMLUtils.getNodeAttributeValue(n,
                SMSUtils.ATTRIBUTE_SYNTAX);
        syntax = AttributeSchema.Syntax.STRING;
        if (attrSyntax != null) {
            try {
                Class syntaxClass = (AttributeSchema.Syntax.STRING).getClass();
                syntax = (AttributeSchema.Syntax) (syntaxClass
                        .getField(attrSyntax.toUpperCase())
                        .get(AttributeSchema.Syntax.STRING));
            } catch (Exception e) {
                // do nothing, use the default setting
            }
        }

        // If syntax is boolean, get the "true" & "false" values
        Node booleanValue;
        if ((syntax.equals(AttributeSchema.Syntax.BOOLEAN))
                && ((booleanValue = XMLUtils.getChildNode(n,
                        SMSUtils.ATTRIBUTE_BOOLEAN_VALUES_ELEMENT)) != null)) {
            // Get the True value
            if ((node = XMLUtils.getChildNode(booleanValue,
                    SMSUtils.ATTRIBUTE_TRUE_BOOLEAN_ELEMENT)) != null) {
                trueBooleanValue = XMLUtils.getValueOfValueNode(node);
                trueValueKey = XMLUtils.getNodeAttributeValue(node,
                        SMSUtils.I18N_KEY);
            } else {
                trueBooleanValue = "true";
            }
            // Get the false value
            if ((node = XMLUtils.getChildNode(booleanValue,
                    SMSUtils.ATTRIBUTE_FALSE_BOOLEAN_ELEMENT)) != null) {
                falseBooleanValue = XMLUtils.getValueOfValueNode(node);
                falseValueKey = XMLUtils.getNodeAttributeValue(node,
                        SMSUtils.I18N_KEY);
            } else {
                falseBooleanValue = "false";
            }
        } else {
            trueBooleanValue = "true";
            falseBooleanValue = "false";
        }

        // Get choice values, if applicable
        if (type.equals(AttributeSchema.Type.SINGLE_CHOICE)
                || type.equals(AttributeSchema.Type.MULTIPLE_CHOICE)) {
            Node choiceValueNode = XMLUtils.getChildNode(n,
                    SMSUtils.ATTRIBUTE_CHOICE_VALUES_ELEMENT);
            if (choiceValueNode != null) {
                // If the sub-element ChoiceValuesClassName, use it
                Node cvClassName = XMLUtils.getChildNode(choiceValueNode,
                        SMSUtils.ATTRIBUTE_CHOICE_CLASS);
                if (cvClassName != null) {
                    String className = XMLUtils.getNodeAttributeValue(
                            cvClassName, SMSUtils.CLASS_NAME);
                    try {
                        Class c = Class.forName(className);
                        choiceObject = (ChoiceValues) c.newInstance();
                        choiceObject.setAttributeSchema(this);
                        choiceObject.setKeyValues(cvClassName);
                        choiceObject.setParentNode(n);
                    } catch (Exception e) {
                        SMSEntry.debug.error("SMS AttributeSchema: "
                                + "Unable to load class: " + className, e);
                        choiceObject = null;
                    }
                }
                // If choice class not present, use ChoiceValues element
                if (choiceObject == null) {
                    // Choice object was not configured or error in obtaining it
                    choiceValues = new HashMap();
                    Iterator cit = XMLUtils.getChildNodes(choiceValueNode,
                            SMSUtils.ATTRIBUTE_CHOICE_VALUE_ELEMENT).iterator();
                    while (cit.hasNext()) {
                        Node cnode = (Node) cit.next();
                        String choiceValue = XMLUtils
                                .getValueOfValueNode(cnode);
                        String i18nKey = XMLUtils.getNodeAttributeValue(cnode,
                                SMSUtils.I18N_KEY);
                        choiceValues.put(choiceValue, i18nKey);
                    }
                }
            }
        }

        // Get default values
        if ((node = XMLUtils
                .getChildNode(n, SMSUtils.ATTRIBUTE_DEFAULT_ELEMENT)) != null) {
            // If the sub-element DefaultValuesClassName, use it
            Node dvClassName = XMLUtils.getChildNode(node,
                    SMSUtils.ATTRIBUTE_DEFAULT_CLASS);
            if (dvClassName != null) {
                String className = XMLUtils.getNodeAttributeValue(dvClassName,
                        SMSUtils.CLASS_NAME);
                try {
                    Class c = Class.forName(className);
                    defaultsObject = (DefaultValues) c.newInstance();
                    defaultsObject.setAttributeSchema(this);
                    defaultsObject.setKeyValues(dvClassName);
                    defaultsObject.setParentNode(n);
                } catch (Exception e) {
                    SMSEntry.debug.error("SMS AttributeSchema: "
                            + "Unable to load class: " + className, e);
                    // use default approach
                    defaultValues = getValues(node);
                }
            } else {
                defaultValues = getValues(node);
            }
        }

        // If syntax is password, decrypt the attribute values
        if ((syntax.equals(AttributeSchema.Syntax.PASSWORD) || syntax
                .equals(AttributeSchema.Syntax.ENCRYPTED_PASSWORD))
                && (defaultValues != null)) {
            Iterator iter = defaultValues.iterator();
            defaultValues = new HashSet();
            while (iter.hasNext()) {
                String value = (String) iter.next();
                if (value != null) {
                    try {
                        value = (String) AccessController
                                .doPrivileged(new DecodeAction(value));
                    } catch (Throwable e) {
                        SMSEntry.debug.error(
                                "AttributeSchemaImpl: Unable to decode", e);
                    }
                }
                defaultValues.add(value);
            }
        }

        // Set the cosQualifier
        if ((cosQualifier = XMLUtils.getNodeAttributeValue(n,
                SMSUtils.ATTRIBUTE_COS_QUALIFIER)) == null) {
            cosQualifier = "default";
        }

        // Get range start
        rangeStart = XMLUtils.getNodeAttributeValue(n,
                SMSUtils.ATTRIBUTE_RANGE_START);

        // Get range end
        rangeEnd = XMLUtils.getNodeAttributeValue(n,
                SMSUtils.ATTRIBUTE_RANGE_END);

        // Get minimum number of values
        try {
            minValue = Integer.parseInt(XMLUtils.getNodeAttributeValue(n,
                    SMSUtils.ATTRIBUTE_MIN_VALUE));
        } catch (NumberFormatException e) {
            minValue = -1;
        }

        // Get maximum number of values
        try {
            maxValue = Integer.parseInt(XMLUtils.getNodeAttributeValue(n,
                    SMSUtils.ATTRIBUTE_MAX_VALUE));
        } catch (NumberFormatException e) {
            maxValue = -1;
        }

        // get validator
        validator = XMLUtils.getNodeAttributeValue(n,
                SMSUtils.ATTRIBUTE_VALIDATOR);

        // Check if the variable is optional
        if (XMLUtils.getChildNode(n, SMSUtils.ATTRIBUTE_OPTIONAL) != null) {
            isOptional = true;
        }

        // COS identifer
        isServiceIdentifier = false;
        if (XMLUtils.getChildNode(n, SMSUtils.ATTRIBUTE_SERVICE_ID) != null) {
            isServiceIdentifier = true;
        }

        // Resource name allowed
        isResourceNameAllowed = false;
        if (XMLUtils.getChildNode(n, SMSUtils.ATTRIBUTE_RESOURCE_NAME) != null){
            isResourceNameAllowed = true;
        }

        // Service Status attribute
        isStatusAttribute = false;
        if (XMLUtils.getChildNode(n, SMSUtils.ATTRIBUTE_STATUS_ATTR) != null) {
            isStatusAttribute = true;
        }

        // Any attribute
        any = XMLUtils.getNodeAttributeValue(n, SMSUtils.ATTRIBUTE_ANY);

        // Get view bean url
        attributeViewBeanURL = XMLUtils.getNodeAttributeValue(n,
                SMSUtils.ATTRIBUTE_VIEW_BEAN_URL);

        isSearchable = false;
        String srch = XMLUtils.getNodeAttributeValue(n, SMSUtils.ISSEARCHABLE);
        if ((srch != null) && (srch.equalsIgnoreCase("yes"))) {
            isSearchable = true;
        }
    }

    protected static Set getValues(Node node) {
        Set retVal = new HashSet();
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeName().equals(SMSUtils.ATTRIBUTE_VALUE)) {
                retVal.add(XMLUtils.getValueOfValueNode(n));
            }
        }
        return (retVal);
    }

    /**
     * Returns <code>true</code> if the attribute is searchable;
     * <code>false</code> otherwise
     * 
     * @return <code>true</code> if the attribute is an optional attribute;
     *         <code>false</code> otherwise
     */
    public boolean isSearchable() {
        return (isSearchable);
    }
}

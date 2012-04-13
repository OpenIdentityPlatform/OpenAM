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
 * $Id: CreationTemplate.java,v 1.4 2008/06/25 05:41:44 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.ums;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

import com.iplanet.services.ldap.Attr;
import com.iplanet.services.ldap.AttrSet;
import com.iplanet.services.util.I18n;
import com.iplanet.ums.validation.DataConstraintException;
import com.iplanet.ums.validation.Validation;
import com.iplanet.ums.validation.ValidationElement;

/**
 * Represents templates for creating objects in UMS. CreationTemplate is used to
 * aid in creating objects in which it serves as reusable guidelines to
 * instantiate UMS objects properly at runtime. The guidelines are used to
 * instantiate objects in memory correctly so that subsequent storage in
 * persistent storage can be done successfully. It is the intention that
 * CreationTemplate allows applications to determine what is correct so that
 * some control is given in the application for early detection of problems of
 * UMS object creations. Reusability and flexibility are two desired goals of
 * CreationTemplate.
 * 
 * @see Template
 * @see SearchTemplate
 *
 * @supported.api
 */
public class CreationTemplate extends Template {

    private static I18n i18n = I18n.getInstance(IUMSConstants.UMS_PKG);

    /**
     * Default constructor for deserialization
     * 
     */
    public CreationTemplate() {
        super();
    }

    /**
     * Creates a template with required and optional attributes.
     * 
     * @param name
     *            Template name
     * @param required
     *            Set of required attributes
     * @param optional
     *            Set of optional attributes
     * @param classes
     *            Array of classes that this CreationTemplate is associated with
     *            for object creation
     */
    public CreationTemplate(String name, AttrSet required, AttrSet optional,
            ArrayList classes) {
        super(name);
        setRequiredAttributeSet(required);
        setOptionalAttributeSet(optional);
        setCreationClasses(classes);
    }

    /**
     * Creates a template with required and optional attributes.
     * 
     * @param name
     *            Template name
     * @param required
     *            Set of required attributes
     * @param optional
     *            Set of optional attributes
     * @param cls
     *            Class that this CreationTemplate is associated with for object
     *            creation
     */
    public CreationTemplate(String name, AttrSet required, AttrSet optional,
            Class cls) {
        this(name, required, optional);
        ArrayList classes = new ArrayList();
        classes.add(cls);
        setCreationClasses(classes);
    }

    /**
     * Creates a template with required and optional attributes.
     * 
     * @param name
     *            Template name
     * @param required
     *            Set of required attributes
     * @param optional
     *            Set of optional attributes
     */
    public CreationTemplate(String name, AttrSet required, AttrSet optional) {
        // No definition for the class
        //
        // this( name, required, optional, null );
        super(name);
        setRequiredAttributeSet(required);
        setOptionalAttributeSet(optional);
    }

    /**
     * Gets the value of a given attribute in the template.
     * 
     * @param attributeName
     *            Name of attribute for which to return values
     * @return The attribute with the specified name, or <CODE>null</CODE> if
     *         attributeName is <CODE>null</CODE>, or the attribute is not
     *         found.
     * 
     * @supported.api
     */
    public Attr getAttribute(String attributeName) {
        if (attributeName == null) {
            return null;
        }
        Attr attr = null;
        if (m_required != null) {
            attr = m_required.getAttribute(attributeName);
        }
        if ((attr == null) && (m_optional != null)) {
            attr = m_optional.getAttribute(attributeName);
        }
        return attr;
    }

    /**
     * Gets a list of required attribute names defined in the object.
     * 
     * @return Names of all required attributes defined
     * 
     * @supported.api
     */
    public String[] getRequiredAttributeNames() {
        return (m_required == null) ? new String[0] : m_required
                .getAttributeNames();
    }

    /**
     * Gets a list of optional attribute names defined in the object.
     * 
     * @return Names of all optional attributes defined
     * 
     * @supported.api
     */
    public String[] getOptionalAttributeNames() {
        return (m_optional == null) ? new String[0] : m_optional
                .getAttributeNames();
    }

    /**
     * Gets the required attributes for object creation.
     * 
     * @return set of required attributes
     * 
     * @supported.api
     */
    public AttrSet getRequiredAttributeSet() {
        return m_required;
    }

    /**
     * Gets the optional attributes for object creation.
     * 
     * @return set of optional attributes
     * 
     * @supported.api
     */
    public AttrSet getOptionalAttributeSet() {
        return m_optional;
    }

    /**
     * Get the classes that the CreationTemplate is associated with.
     * 
     * @return classes associated with this template
     * 
     * @supported.api
     */
    public ArrayList getCreationClasses() {
        return m_classes;
    }

    /**
     * Gets enumeration of ValidationElement for the attribute name
     * 
     * @param attrName
     *            Attribute name
     * @return Enumeration of ValidationElement
     * 
     * @supported.api
     */
    public Enumeration getValidation(String attrName) {
        Vector v = new Vector();
        if (attrName != null && m_validated != null) {
            Attr attr = m_validated.getAttribute(attrName);
            if (attr != null) {
                String[] validationStrings = attr.getStringValues();
                for (int i = 0; i < validationStrings.length; i++) {
                    v.add(decodeValidationString(validationStrings[i]));
                }
            }
        }
        return v.elements();
    }

    AttrSet getValidation() {
        return m_validated;
    }

    /**
     * Gets a list of attribute names registered for validation.
     * 
     * @return a list of attribute names registered for validation
     * 
     * @supported.api
     */
    public String[] getValidatedAttributeNames() {
        return (m_validated == null) ? new String[0] : m_validated
                .getAttributeNames();
    }

    /**
     * Gets enumeration of attributes for object creation.
     * 
     * @return enumeration of required and optional attributes
     */
    public Enumeration getAttributes() {
        Vector v = new Vector();
        if (m_required != null) {
            for (int i = 0; i < m_required.size(); i++) {
                v.add(m_required.elementAt(i));
            }
        }

        if (m_optional != null) {
            for (int i = 0; i < m_optional.size(); i++) {
                v.add(m_optional.elementAt(i));
            }
        }

        return v.elements();
    }

    /**
     * Sets the required attributes.
     * 
     * @param attrSet
     *            set of required attributes
     */
    public void setRequiredAttributeSet(AttrSet attrSet) {
        // ??? Should we clone attrSet instead of keeping a reference?
        m_required = attrSet;
    }

    /**
     * Sets the optional attributes.
     * 
     * @param attrSet
     *            set of optional attributes
     */
    public void setOptionalAttributeSet(AttrSet attrSet) {
        // ??? Should we clone attrSet instead of keeping a reference?
        m_optional = attrSet;
    }

    /**
     * Set the class that the CreationTemplate is associated with.
     * 
     * @param classes
     *            Classes associated with this template
     */
    public void setCreationClasses(ArrayList classes) {
        m_classes = classes;
    }

    /**
     * Adds the attribute to the required attributes.
     * 
     * @param attr
     *            The attribute to be added
     */
    public void addRequiredAttribute(Attr attr) {
        if (m_required == null) {
            m_required = new AttrSet();
        }
        m_required.add(attr);
    }

    /**
     * Adds the attribute to the optional attributes.
     * 
     * @param attr
     *            The attribute to be added
     */
    public void addOptionalAttribute(Attr attr) {
        if (m_optional == null) {
            m_optional = new AttrSet();
        }
        m_optional.add(attr);
    }

    /**
     * Sets the validation table
     * 
     * @param attrSet
     *            validation table in attribute set format
     */
    void setValidation(AttrSet attrSet) {
        // ??? Should we clone attrSet instead of keeping a reference?
        m_validated = attrSet;
    }

    /**
     * Adds the validator and the rule for the attribute name.
     * 
     * @param attrName Attribute name to validate.
     * @param validatorClass Validator class name used for validation.
     * @param rule The optional rule used by the validator.
     */
    public void addValidation(
        String attrName,
        String validatorClass,
        String rule
    ) {
        if (validatorClass != null && attrName != null) {
            String validationString = encodeValidationString(validatorClass,
                    rule);
            if (validationString != null) {
                if (m_validated == null) {
                    m_validated = new AttrSet();
                }
                if (!m_validated.contains(attrName, validationString)) {
                    m_validated.add(new Attr(attrName, validationString));
                }
            }
        }
    }

    /**
     * Removes all validations from the attribute.
     * 
     * @param attrName
     *            attribute name of the validations to be removed
     */
    public void removeValidation(String attrName) {
        if (m_validated != null) {
            m_validated.remove(attrName);
        }
    }

    /**
     * Sets the naming attribute.
     * 
     * @param namingAttribute
     *            naming attribute
     */
    void setNamingAttribute(String namingAttribute) {
        m_namingAttribute = namingAttribute;
    }

    /**
     * Gets the naming attribute.
     * 
     * @return the naming attribute
     * 
     * @supported.api
     */
    public String getNamingAttribute() {
        return m_namingAttribute;
    }

    /**
     * Returns a copy of the template.
     * 
     * @return a copy of the template
     * 
     * @supported.api
     */
    public Object clone() {
        CreationTemplate t = (CreationTemplate) super.clone();
        if (m_required != null) {
            t.setRequiredAttributeSet((AttrSet) m_required.clone());
        }
        if (m_optional != null) {
            t.setOptionalAttributeSet((AttrSet) m_optional.clone());
        }
        if (m_validated != null) {
            t.setValidation((AttrSet) m_validated.clone());
        }
        return t;
    }

    /**
     * Encode an attrSet in a single attribute with multiple values using the
     * given attribute name and the values (tag,value) found in the given
     * attribute set. For example:
     * 
     * <pre>
     *       required: objectclass=top
     *       required: objectclass=groupofuniquenames
     *       required: cn
     *       required: sn
     * </pre>
     * 
     * @param attrName
     *            Name of the encoded attribute
     * @param attrSet
     *            Attribute Set to be encoded in a single attribute
     * @param delimiter
     *            String token used as delimiter for the encoding
     * @return Encoded attribute or null object if attrSet is empty
     */
    static Attr encodeAttrSet(String attrName, AttrSet attrSet, 
            String delimiter) 
    {
        if (attrSet == null || attrSet.size() == 0) {
            return null;
        }

        Enumeration attrEnum = attrSet.getAttributes();
        Attr encodedAttr = new Attr(attrName);

        while (attrEnum.hasMoreElements()) {
            Attr a = (Attr) attrEnum.nextElement();
            String[] values = a.getStringValues();
            String[] encodedValues = new String[values.length];

            if (values.length == 0) {
                encodedAttr.addValue(a.getName());
            } else {
                for (int i = 0; i < values.length; i++) {
                    encodedValues[i] = a.getName() + delimiter + values[i];
                }
                encodedAttr.addValues(encodedValues);
            }
        }

        return encodedAttr;
    }

    private static String encodeValidationString(String className, String rule)
    {
        if (rule == null) {
            return className;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(className);
        sb.append("(");
        sb.append(rule);
        sb.append(")");
        return sb.toString();
    }

    private static ValidationElement decodeValidationString(String value) {
        int index = value.indexOf('(');
        if (index < 0) {
            return (new ValidationElement(value, null));
        }
        String className = value.substring(0, index);
        String rule = value.substring(index + 1, value.length() - 1);
        return (new ValidationElement(className, rule));
    }

    /**
     * Render the object.
     * 
     * @return The object in printable form
     * 
     * @supported.api
     */
    public String toString() {
        return "CreationTemplate: " + getName() + " { Required " + m_required
                + " Optional " + m_optional + " Validation " + m_validated
                + " Naming Attribute " + m_namingAttribute + " }";
    }

    /**
     * Validate attrSet according to the definition of required and optional
     * attributes defined in the template.
     * 
     * @param attrSet
     *            Attribute set to be validated
     * @return true if the given attrSet conforms to the template
     * @throws UMSException
     *             if attrSet doesn't conform to the template
     */
    boolean validateAttrSet(AttrSet attrSet) throws UMSException {
        AttrSet reqAttrs = getRequiredAttributeSet();
        AttrSet optionalAttrs = getOptionalAttributeSet();
        if (reqAttrs == null && optionalAttrs == null) {
            throw new UMSException(i18n
                    .getString(IUMSConstants.TEMPLATE_NO_ATTR));
        }
        String[] attrNames = attrSet.getAttributeNames();
        int attrSetSize = (attrNames != null) ? attrNames.length : -1;
        String attrName = null;

        // Loop on attributes in the template comparing with the argument
        // attSet and ensure all required attributtes are supplied
        // or have a default
        if (reqAttrs != null) {
            Enumeration attrEnum = reqAttrs.getAttributes();
            while (attrEnum.hasMoreElements()) {
                Attr anAttr = (Attr) attrEnum.nextElement();
                // if ( !attrSet.contains(anAttr.getName().toLowerCase())) {
                if (!attrSet.contains(anAttr.getName())) {
                    // A required attribute which was not supplied
                    if (anAttr.size() > 0) {
                        // There is a default value
                        attrSet.add((Attr) anAttr.clone());
                    } else {
                        // No default value. This is an error! A value
                        // should have been supplied in attrSet.
                        attrName = anAttr.getName();
                        String args[] = new String[1];
                        args[0] = attrName;
                        String msg = i18n.getString(IUMSConstants.NO_VALUE,
                                args);
                        throw new UMSException(msg);
                    }
                }
            }
        }

        // If the optional attributes set is set to "*", which means allowing
        // all attributes, then no need to ensure the given attributes are in
        // either the required attributes set or the optional attributes set
        if (optionalAttrs != null && optionalAttrs.contains("*")) {
            return true;
        }

        // Loop on attributes in the argument attrSet comparing with the
        // template and ensure all the attributes are allowed
        //
        boolean attrAllowed = false;
        for (int i = 0; i < attrSetSize; i++) {
            attrAllowed = false;
            attrName = attrNames[i];
            if (reqAttrs != null && reqAttrs.contains(attrName)) {
                attrAllowed = true;
            } else if (optionalAttrs != null
                    && optionalAttrs.contains(attrName)) {
                attrAllowed = true;
            }
            if (!attrAllowed) {
                String args[] = new String[1];
                args[0] = attrName;
                String msg = i18n.getString(IUMSConstants.ATTR_NOT_ALLOWED,
                        args);
                // TODO: need to define new and meaningful exception for
                // unknown attribute not conforming to the
                // given creation template
                //
                throw new UMSException(msg);
            }
        }
        return true;
    }

    /**
     * Validate attrSet according to the definition of validated attributes
     * defined in the template.
     * 
     * @param attrSet
     *            Attribute set to be validated
     * @return true if the given attrSet conforms to the template
     * @throws DataConstraintException
     *             if attrSet doesn't conform to the template
     * @throws UMSException
     *             failure
     */
    boolean validateAttributes(AttrSet attrSet) throws UMSException,
            DataConstraintException {

        Enumeration en1 = attrSet.getAttributes();
        while (en1.hasMoreElements()) {
            Attr attr = (Attr) en1.nextElement();
            Enumeration en2 = getValidation(attr.getName());
            while (en2.hasMoreElements()) {
                ValidationElement vElement = (ValidationElement) en2
                        .nextElement();
                // calls method in Validation to validate each values
                // of the attribute
                Validation.validateAttribute(attr, vElement.getValidator(),
                        vElement.getRule());
            }
        }
        return true;
    }

    private AttrSet m_required = null;

    private AttrSet m_optional = null;

    private ArrayList m_classes = null;

    private AttrSet m_validated = null;

    private String m_namingAttribute = null;
}

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
 * $Id: AMAttributeSchema.java,v 1.3 2008/06/25 05:41:19 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import java.util.Set;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SMSException;

/**
 * The class <code>AMAttributeSchema</code> provides the interfaces to obtain
 * meta information about service configuration variable.
 * 
 * @deprecated This class has been deprecated. Please use
 *             <code>com.sun.identity.sm.AttributeSchema</code>.
 * @supported.all.api
 */
public class AMAttributeSchema extends Object {

    /**
     * The <code>TYPE_SINGLE</code> attribute type specifies that the
     * attribute can have only a single value
     */
    public static final int TYPE_SINGLE = 1;

    /**
     * The <code>TYPE_LIST</code> attribute type specifies that the attribute
     * can have multiple values, i.e., multi-valued attribute
     */
    public static final int TYPE_LIST = 2;

    /**
     * The <code>TYPE_SINGLE_CHOICE</code> attribute type specifies that the
     * attribute can have value defined by the <code>getChoiceValues</code>
     * method of <code>AMAttributeSchema</code>.
     */
    public static final int TYPE_SINGLE_CHOICE = 3;

    /**
     * The <code>TYPE_MULTIPLE_CHOICE</code> attribute type specifies that the
     * attribute can have multiple values defined by the
     * <code>getChoiceValues</code> method of <code>AMAttributeSchema</code>.
     */
    public static final int TYPE_MULTIPLE_CHOICE = 4;

    /**
     * The <code>TYPE_SIGNATURE</code> attribute type specifies that the
     * attribute is a signing attribute.
     */
    public static final int TYPE_SIGNATURE = 20;

    /**
     * The <code>VALIDATOR</code> attribute type specifies that the attribute
     * defines a attribute validator plugin.
     */
    public static final int TYPE_VALIDATOR = 21;

    /**
     * The <code>UITYPE_RADIO</code> attribute type specifies that the
     * attribute should be display as a radio button.
     */
    public static final int UITYPE_RADIO = 22;

    /**
     * The <code>UITYPE_LINK</code> attribute type specifies that the
     * attribute should be display as a link.
     */
    public static final int UITYPE_LINK = 23;

    /**
     * The <code>UITYPE_BUTTON</code> attribute type specifies that the
     * attribute should be display as a button.
     */
    public static final int UITYPE_BUTTON = 24;

    /**
     * The <code>UITYPE_UNDEFINED</code> attribute type specifies that the UI
     * type is not defined.
     */
    public static final int UITYPE_UNDEFINED = 25;

    /**
     * The <code>SYNTAX_BOOLEAN</code> attribute syntax specifies that the
     * attribute is of boolean type, i.e., can have a value of either
     * <code>true</code> or <code>
     * false</code>
     */
    public static final int SYNTAX_BOOLEAN = 5;

    /**
     * The <code>SYNTAX_EMAIL</code> attribute syntax specifies that the
     * attribute is a email address.
     */
    public static final int SYNTAX_EMAIL = 6;

    /**
     * The <code>SYNTAX_URL</code> attribute syntax specifies that the
     * attribute is a URL.
     */
    public static final int SYNTAX_URL = 7;

    /**
     * The <code>SYNTAX_STRING</code> attribute syntax specifies that the
     * attribute is of text type, i.e., can have any unicode characters
     */
    public static final int SYNTAX_STRING = 8;

    /**
     * The <code>SYNTAX_PASSWORD</code> attribute syntax specifies that the
     * attribute is of password type, will be used by UI to mask the password
     * typed.
     */
    public static final int SYNTAX_PASSWORD = 9;

    /**
     * The <code>SYNTAX_NUMERIC</code> attribute syntax specifies that the
     * attribute is numeric, i.e., can have numbers only.
     */
    public static final int SYNTAX_NUMERIC = 10;

    /**
     * The <code>SYNTAX_NUMBER</code> attribute syntax specifies that the
     * attribute is a number.
     */
    public static final int SYNTAX_NUMBER = 11;

    /**
     * The <code>SYNTAX_PERCENT</code> attribute syntax specifies that the
     * attribute is a percentage.
     */
    public static final int SYNTAX_PERCENT = 12;

    /**
     * The <code>SYNTAX_NUMBER_RANGE</code> attribute syntax specifies that
     * the attribute is a number within a range.
     */
    public static final int SYNTAX_NUMBER_RANGE = 13;

    /**
     * The <code>SYNTAX_DECIMAL_RANGE</code> attribute syntax specifies that
     * the attribute is a decimal number within a range.
     */
    public static final int SYNTAX_DECIMAL_RANGE = 14;

    /**
     * The <code>SYNTAX_DECIMAL_NUMBER</code> attribute syntax specifies that
     * the attribute is a floating point number, e.g., 1.5, 3.56, etc.
     */
    public static final int SYNTAX_DECIMAL_NUMBER = 15;

    /**
     * The <code>SYNTAX_DN</code> attribute syntax specifies that the
     * attribute should be an LDAP distinguished name (DN).
     */
    public static final int SYNTAX_DN = 16;

    /**
     * The <code>SYNTAX_PARAGRAPH</code> attribute syntax specifies that the
     * attribute should be a paragraph
     */
    public static final int SYNTAX_PARAGRAPH = 17;

    /**
     * The <code>SYNTAX_DATE</code> attribute syntax specifies that the
     * attribute should be a date
     */
    public static final int SYNTAX_DATE = 18;

    /**
     * The <code>SYNTAX_XML</code> attribute syntax specifies that the
     * attribute should be a XML blob
     */
    public static final int SYNTAX_XML = 19;

    /**
     * The <code>SYNTAX_ENCRYPTED_PASSWORD</code> attribute syntax specifies
     * that the attribute is of password type, will be used by UI to mask the
     * password typed.
     */
    public static final int SYNTAX_ENCRYPTED_PASSWORD = 20;

    private AttributeSchema attrSchema;

    protected AMAttributeSchema(AttributeSchema as) {
        attrSchema = as;
    }

    /**
     * The method returns the name of the attribute
     * 
     * @return name of the attribute
     */
    public String getName() {
        return attrSchema.getName();
    }

    /**
     * The method returns the type of the attribute i.e., single, list or choice
     * 
     * @return the type of the attribute.
     */
    public int getType() {
        String type = attrSchema.getType().toString();

        // Map the SMS attribute type to public attribute type
        if (type.equals(AttributeSchema.Type.SINGLE.toString()))
            return TYPE_SINGLE;
        else if (type.equals(AttributeSchema.Type.LIST.toString()))
            return TYPE_LIST;
        else if (type.equals(AttributeSchema.Type.SINGLE_CHOICE.toString()))
            return TYPE_SINGLE_CHOICE;
        else if (type.equals(AttributeSchema.Type.MULTIPLE_CHOICE.toString()))
            return TYPE_MULTIPLE_CHOICE;
        else if (type.equals(AttributeSchema.Type.SIGNATURE.toString()))
            return TYPE_SIGNATURE;
        else if (type.equals(AttributeSchema.Type.VALIDATOR.toString()))
            return TYPE_VALIDATOR;

        return -1; // Should'nt occur
    }

    /**
     * The method returns the UI type of the attribute i.e., link, button, ...
     * 
     * @return the UI type of the attribute; <code>UITYPE_UNDEFINED</code> if
     *         the UI type is not defined for the attribute
     */
    public int getUIType() {
        AttributeSchema.UIType uitype = attrSchema.getUIType();

        // Map the SMS attribute UI type to public attribute type
        if (uitype == null) {
            // UI type not defined
            return UITYPE_UNDEFINED;
        } else if (uitype.equals(AttributeSchema.UIType.RADIO)) {
            return UITYPE_RADIO;
        } else if (uitype.equals(AttributeSchema.UIType.LINK)) {
            return UITYPE_LINK;
        } else if (uitype.equals(AttributeSchema.UIType.BUTTON)) {
            return UITYPE_BUTTON;
        }

        return UITYPE_UNDEFINED;
    }

    /**
     * The method returns the syntax of the attribute i.e., string, boolean,
     * distinguished name (String), numeric.
     * 
     * @return syntax of the attribute
     */
    public int getSyntax() {
        String syntax = attrSchema.getSyntax().toString();

        // Map the SMS attribute type to public attribute type
        if (syntax.equals(AttributeSchema.Syntax.BOOLEAN.toString()))
            return SYNTAX_BOOLEAN;
        else if (syntax.equals(AttributeSchema.Syntax.EMAIL.toString()))
            return SYNTAX_EMAIL;
        else if (syntax.equals(AttributeSchema.Syntax.URL.toString()))
            return SYNTAX_URL;
        else if (syntax.equals(AttributeSchema.Syntax.STRING.toString()))
            return SYNTAX_STRING;
        else if (syntax.equals(AttributeSchema.Syntax.ENCRYPTED_PASSWORD
                .toString()))
            return SYNTAX_ENCRYPTED_PASSWORD;
        else if (syntax.equals(AttributeSchema.Syntax.PASSWORD.toString()))
            return SYNTAX_PASSWORD;
        else if (syntax.equals(AttributeSchema.Syntax.NUMERIC.toString()))
            return SYNTAX_NUMERIC;
        else if (syntax.equals(AttributeSchema.Syntax.NUMBER.toString()))
            return SYNTAX_NUMBER;
        else if (syntax.equals(AttributeSchema.Syntax.PERCENT.toString()))
            return SYNTAX_PERCENT;
        else if (syntax.equals(AttributeSchema.Syntax.NUMBER_RANGE.toString()))
            return SYNTAX_NUMBER_RANGE;
        else if (syntax.equals(AttributeSchema.Syntax.DECIMAL_RANGE.toString()))
            return SYNTAX_DECIMAL_RANGE;
        else if (syntax
                .equals(AttributeSchema.Syntax.DECIMAL_NUMBER.toString()))
            return SYNTAX_DECIMAL_NUMBER;
        else if (syntax.equals(AttributeSchema.Syntax.DN.toString()))
            return SYNTAX_DN;
        else if (syntax.equals(AttributeSchema.Syntax.PARAGRAPH.toString()))
            return SYNTAX_PARAGRAPH;
        else if (syntax.equals(AttributeSchema.Syntax.XML.toString()))
            return SYNTAX_XML;
        else if (syntax.equals(AttributeSchema.Syntax.DATE.toString()))
            return SYNTAX_DATE;

        return -1;
    }

    /**
     * Returns the value of the <code>cosQualifier</code> for this attribute
     * that is default or merge-schemes.
     * 
     * @return String value of <code>cosQualifier</code>.
     */
    public String getCosQualifier() {
        return attrSchema.getCosQualifier();
    }

    /**
     * The method returns the default value of the attribute;
     * 
     * @return Set containing the default values of the attribute or
     *         <code>Collections.EMPTY_SET<code> otherwise
     */
    public Set getDefaultValues() {
        return attrSchema.getDefaultValues();
    }

    /**
     * If the attribute is of choice type, this method returns the possible
     * values for the attribute; <code>null</code> otherwise
     * 
     * @return String array of possible choice values; null otherwise
     */
    public String[] getChoiceValues() {
        return attrSchema.getChoiceValues();
    }

    /**
     * Given a choice value, this method returns the i18nKey for that choice
     * value
     * 
     * @param cValue
     *            the choice value
     * @return the i18N key corresponding to the choice value
     */
    public String getChoiceValueI18NKey(String cValue) {
        return attrSchema.getChoiceValueI18NKey(cValue);
    }

    /**
     * Returns I18N key to describe the configuration attribute.
     * 
     * @return i18n index key to the resource bundle
     */
    public String getI18NKey() {
        return attrSchema.getI18NKey();
    }

    /**
     * Set I18N key to describe the configuration attribute
     * 
     * @param i18nKey
     *            value of <code>i18nKey</code>.
     * @throws AMException
     */
    public void setI18NKey(String i18nKey) throws AMException {
        try {
            attrSchema.setI18NKey(i18nKey);
        } catch (SMSException se) {
            AMCommonUtils.debug.message(
                    "AMAttributeSchema.setAttributeDefaults(Map): ", se);
            throw new AMException(AMSDKBundle.getString("916"), "916");
        } catch (SSOException so) {
            AMCommonUtils.debug.message(
                    "AMAttributeSchema.setAttributeDefaults(Map): ", so);
            throw new AMException(AMSDKBundle.getString("916"), "916");
        }
    }

    /**
     * Method to get starting range for possible values of the attribute
     * 
     * @return starting range for the attribute value
     */
    public String getStartRange() {
        return attrSchema.getStartRange();
    }

    /**
     * Method to get ending range for possible values of the attribute
     * 
     * @return ending range for the attribute value
     */
    public String getEndRange() {
        return attrSchema.getEndRange();
    }

    /**
     * Method to get service specific attributes. It return the value of the
     * "any" attribute, if set in the XML schema for the service
     * 
     * @return value of "any" attribute
     */
    public String getAny() {
        return attrSchema.getAny();
    }

    /**
     * Returns URL of the view bean for the attribute.
     * 
     * @return URL for view bean
     */
    public String getPropertiesViewBeanURL() {
        return attrSchema.getPropertiesViewBeanURL();
    }

    /**
     * Method to get the string value for "TRUE"
     * 
     * @return String for TRUE value
     */
    public String getTrueValue() {
        return attrSchema.getTrueValue();
    }

    /**
     * Method to get the i18n Key for <code>BooleanTrueValue</code>.
     * 
     * @return String i18nKey for TRUE value
     */
    public String getTrueValueI18NKey() {
        return attrSchema.getTrueValueI18NKey();
    }

    /**
     * Method to get string value for "FALSE"
     * 
     * @return String for FALSE value
     */
    public String getFalseValue() {
        return attrSchema.getFalseValue();
    }

    /**
     * Method to get the i18n Key for <code>BooleanFalseValue</code>.
     * 
     * @return String i18nKey for FALSE value.
     */
    public String getFalseValueI18NKey() {
        return attrSchema.getFalseValueI18NKey();
    }

    /**
     * Method that returns the String representation of the
     * <code>AMAttributeSchema</code>.
     * 
     * @return String representation of the <code>AMAttributeSchema</code>/
     */
    public String toString() {
        return attrSchema.toString();
    }
}

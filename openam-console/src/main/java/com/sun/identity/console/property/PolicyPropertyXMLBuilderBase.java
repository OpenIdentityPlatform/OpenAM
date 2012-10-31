/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: PolicyPropertyXMLBuilderBase.java,v 1.5 2008/10/02 16:31:29 veiming Exp $
 *
 */

package com.sun.identity.console.property;

import com.sun.identity.console.base.model.AMDisplayType;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.Syntax;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.TreeSet;

public abstract class PolicyPropertyXMLBuilderBase
    extends PropertyXMLBuilderBase
{
   
    
    protected AMModel model;

    /**
     * Returns a XML for displaying attribute in property sheet.
     *
     * @param prefix Prefix XML String.
     * @return XML for displaying attribute in property sheet.
     */
    public String getXML(String prefix) {
        StringBuffer xml = new StringBuffer(1000);
        xml.append(getXMLDefinitionHeader())
            .append(START_TAG)
            .append(prefix);
        List propertyNames = getPropertyNames();

        if ((propertyNames != null) && !propertyNames.isEmpty()) {
            Object[] params = {"values",
                model.getLocalizedString(getSectionLabel())};
            xml.append(MessageFormat.format(SECTION_START_TAG, params));

            for (Iterator iter = propertyNames.iterator(); iter.hasNext(); ) {
                buildXML((String)iter.next(), xml);
            }

            xml.append(SECTION_END_TAG);
        }

        xml.append(END_TAG);
        return xml.toString();
    }

    private void buildXML(String name, StringBuffer xml) {
        Syntax syntax = getPropertySyntax(name);
        String tagClassName = getTagClassName(syntax);
        
        if (tagClassName != null) {
            xml.append(PROPERTY_START_TAG);
            Object[] params = new String[3];
            params[0] = name;
            params[2] = name;
           
            try {
                params[1] = getDisplayName(name, model.getUserLocale());
            } catch (PolicyException e) {
                debug.warning("PropertyXMLBuilderBase.buildXML", e);
                params[1] = name;
            }

            xml.append(MessageFormat.format(LABEL_TAG, params));

            if (allAttributesReadonly) {
                Object[] param = {name, STATIC_TEXT_TAG_NAME};
                xml.append(MessageFormat.format(COMPONENT_START_TAG, param));
            } else {
                Object[] param = {name, tagClassName};
                xml.append(MessageFormat.format(COMPONENT_START_TAG, param));
                
                // Textarea, textfield, and editable list components will
                // localize the data unless this attribute is set to false
                if (tagClassName.equals(TAGNAME_TEXTAREA)) {
                    xml.append(NON_LOCALIZED_FIELD);
                } else if (tagClassName.equals(TAGNAME_TEXTFIELD)) {
                    // set the size of the text field based on its syntax
                    Object[] pSize = { getStringFieldSize(syntax) };
                    xml.append(MessageFormat.format(TEXTBOX_SIZE_TAG, pSize));
                    xml.append(NON_LOCALIZED_FIELD);
                } else if (tagClassName.equals(TAGNAME_MULTIPLE_CHOICE)) {
                    xml.append(MULTIPLE_ATTRIBUTE_TAG)
                        .append(LIST_SIZE_TAG);
                }
                
                appendChoiceValues(name, syntax, xml);
            }

            xml.append(COMPONENT_END_TAG);
            xml.append(PROPERTY_END_TAG);
        }
    }

    /**
     * Used to determine how large a textfield should be displayed. The
     * size will be determined based on the syntax of the attribute.
     * A mapping in amProperty.properties will set the value of the
     * field size. If no mapping is found the default size of 50 will
     * be used.
     */
    private String getStringFieldSize(Syntax syntax) {
        String size = DEFAULT_TEXTFIELD_SIZE;

        try {
            int type = AMDisplayType.getDisplaySyntax(syntax);
            if (type == AMDisplayType.SYNTAX_TEXTFIELD) {
                size = properties.getString("policy.textfield");
            } 
        } catch (MissingResourceException e) {
            //ignore: use default size
        }
        return size;
    }

    private void appendChoiceValues(
        String name,
        Syntax syntax,
        StringBuffer xml
    ) {
        int syn = AMDisplayType.getDisplaySyntax(syntax);

        switch (syn) {
        case AMDisplayType.SYNTAX_SINGLE_CHOICE:
        case AMDisplayType.SYNTAX_MULTIPLE_CHOICE:
            try {
                Set validValues = getValidValues(name);
                if ((validValues != null) && !validValues.isEmpty()) {
                    Set sorted = new TreeSet(validValues);
    
                    for (Iterator iter = sorted.iterator(); iter.hasNext(); ) {
                        String val = (String)iter.next();
                        Object[] params = {val, val};
                        xml.append(MessageFormat.format(OPTION_TAG, params));
                    }
                }
            } catch (PolicyException e) {
                debug.warning(
                    "PolicyPropertyXMLBuilderBase.appendChoiceValues", e);
            }

            break;
        }
    }

    private static String getTagClassName(Syntax syntax) {
        String tagClassName = TAGNAME_TEXTFIELD;
        int syn = AMDisplayType.getDisplaySyntax(syntax);

        switch (syn) {
        case AMDisplayType.SYNTAX_TEXTFIELD:
            tagClassName = TAGNAME_TEXTFIELD;
            break;
        case AMDisplayType.SYNTAX_SINGLE_CHOICE:
            tagClassName = TAGNAME_DROPDOWN_MENU;
            break;
        case AMDisplayType.SYNTAX_MULTIPLE_CHOICE:
            tagClassName = TAGNAME_MULTIPLE_CHOICE;
            break;
        case AMDisplayType.SYNTAX_LIST:
            tagClassName = TAGNAME_EDITABLE_LIST;
            break;
        } 

        return tagClassName;
    }

    protected abstract List getPropertyNames();
    protected abstract String getSectionLabel();
    protected abstract Syntax getPropertySyntax(String propertyName);
    protected abstract String getDisplayName(String propertyName, Locale locale)
        throws PolicyException;
    protected abstract Set getValidValues(String propertyName)
        throws PolicyException;
}

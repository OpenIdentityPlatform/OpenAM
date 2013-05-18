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
 * $Id: PropertyXMLBuilderBase.java,v 1.20 2009/12/11 23:23:55 veiming Exp $
 *
 */

/*
 * Portions Copyrighted 2011-2013 ForgeRock, Inc.
 */

package com.sun.identity.console.property;

import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;      
import java.util.TreeSet;
import org.forgerock.openam.console.ui.taglib.propertysheet.CCPropertySheetTag;

public abstract class PropertyXMLBuilderBase
    implements PropertyTemplate
{
    public static Debug debug = Debug.getInstance(
        AMAdminConstants.CONSOLE_DEBUG_FILENAME);
    
    protected static ResourceBundle properties =
        ResourceBundle.getBundle("amProperty");

    protected static Map mapSchemaTypeToName = new HashMap(8);
    protected static List orderDisplaySchemaType = new ArrayList(4);
    protected static Map mapUITypeToName = new HashMap(8);
    protected static Map mapTypeToName = new HashMap(8);
    protected static Map mapSyntaxToName = new HashMap(20);

    static {
        mapSchemaTypeToName.put(SchemaType.GLOBAL, "schemaType.global");
        mapSchemaTypeToName.put(SchemaType.ORGANIZATION,
            "schemaType.organization");
        mapSchemaTypeToName.put(SchemaType.USER, "schemaType.user");
        mapSchemaTypeToName.put(SchemaType.DYNAMIC, "schemaType.dynamic");

        orderDisplaySchemaType.add(SchemaType.GLOBAL);
        orderDisplaySchemaType.add(SchemaType.ORGANIZATION);
        orderDisplaySchemaType.add(SchemaType.USER);
        orderDisplaySchemaType.add(SchemaType.DYNAMIC);

        mapUITypeToName.put(AttributeSchema.UIType.BUTTON, "button");
        mapUITypeToName.put(AttributeSchema.UIType.LINK, "link");
        mapUITypeToName.put(AttributeSchema.UIType.ADDREMOVELIST, "addremove");
        mapUITypeToName.put(AttributeSchema.UIType.NAME_VALUE_LIST,
            "namevaluelist");
        mapUITypeToName.put(AttributeSchema.UIType.RADIO, "radio");
        mapUITypeToName.put(AttributeSchema.UIType.UNORDEREDLIST, 
            "unorderedlist");
        mapUITypeToName.put(AttributeSchema.UIType.ORDEREDLIST,
            "orderedlist");
        mapUITypeToName.put(AttributeSchema.UIType.MAPLIST,  "maplist");
        mapUITypeToName.put(AttributeSchema.UIType.GLOBALMAPLIST,  
            "globalmaplist");

        mapTypeToName.put(AttributeSchema.Type.LIST, "list");
        mapTypeToName.put(AttributeSchema.Type.MULTIPLE_CHOICE,
            "multiple_choice");
        mapTypeToName.put(AttributeSchema.Type.SINGLE_CHOICE, "single_choice");
        mapTypeToName.put(AttributeSchema.Type.SINGLE, "single");

        mapSyntaxToName.put(AttributeSchema.Syntax.BOOLEAN, "boolean");
        mapSyntaxToName.put(AttributeSchema.Syntax.PASSWORD, "password");
        mapSyntaxToName.put(AttributeSchema.Syntax.ENCRYPTED_PASSWORD,
            "password");
        mapSyntaxToName.put(AttributeSchema.Syntax.PARAGRAPH, "paragraph");
        mapSyntaxToName.put(AttributeSchema.Syntax.XML, "xml");
        mapSyntaxToName.put(AttributeSchema.Syntax.STRING, "string");
        mapSyntaxToName.put(AttributeSchema.Syntax.NUMERIC, "numeric");
        mapSyntaxToName.put(AttributeSchema.Syntax.NUMBER_RANGE,"number_range");
        mapSyntaxToName.put(AttributeSchema.Syntax.NUMBER, "number");
        mapSyntaxToName.put(AttributeSchema.Syntax.DN, "dn");
        mapSyntaxToName.put(AttributeSchema.Syntax.URL, "url");
    }

    protected ServiceSchemaManager svcSchemaManager;
    protected AMModel model;
    protected boolean supportSubConfig;
    protected String viewBeanName;
    protected ResourceBundle serviceBundle;
    protected Map mapTypeToAttributeSchema;
    protected String serviceName;
    protected boolean allAttributesReadonly;
    protected String currentRealm;

    static String getTagClassName(AttributeSchema as) {
        String tagClassName = null;
        AttributeSchema.UIType uitype = as.getUIType();

        if (uitype != null) {
            String val = (String)mapUITypeToName.get(uitype);
                try {
                tagClassName = properties.getString(val);
            } catch (MissingResourceException e) {
                // do nothing
            }
        }

        AttributeSchema.Type type = as.getType();
        String valType = (String)mapTypeToName.get(type);

        if ((tagClassName == null) && (valType != null)) {
            try {
                tagClassName = properties.getString(valType);
            } catch (MissingResourceException e) {
                // do nothing
            }
        }

        if (tagClassName == null) {
            AttributeSchema.Syntax syntax = as.getSyntax();
            String valSyntax = (String)mapSyntaxToName.get(syntax);

            if (valSyntax != null) {
                try {
                    tagClassName = properties.getString(
                        valType + "." + valSyntax);
                } catch (MissingResourceException e) {
                    // do nothing
                }
            }
        }

        // last try
        if (tagClassName == null) {
            try {
                tagClassName = properties.getString(valType + ".whatever");
            } catch (MissingResourceException e) {
                // do nothing
            }
        }

        if ((tagClassName != null) && (tagClassName.trim().length() == 0)) {
            tagClassName = null;
        }
        return tagClassName;
    }

    /**
     * Removes hidden attribute schemas.
     *
     * @param attributeSchemas Set of Attribute Schemas.
     */
    public static void filterHiddenAttributes(Set attributeSchemas) {
        removeAttributeSchemaWithoutI18nKey(attributeSchemas);
        for (Iterator iter = attributeSchemas.iterator(); iter.hasNext(); ) {
            AttributeSchema as = (AttributeSchema)iter.next();
            String any = as.getAny();
                                                                                
            if (!hasAnyAttribute(any, ANY_REQUIRED) &&
                !hasAnyAttribute(any, ANY_DISPLAY) &&
                !hasAnyAttribute(any, ANY_ADMIN_DISPLAY) &&
                !hasAnyAttribute(any, ANY_DISPLAYRO) &&
                !hasAnyAttribute(any, ANY_ADMIN_DISPLAYRO)
            ) {
                iter.remove();
            } else if (getTagClassName(as) == null) {
                iter.remove();
            }
        }
    }

    /**
     * Removes attribute schema that does not have <code>i18nKey</code>.
     *
     * @param attributeSchemas Set of Attribute Schemas.
     */
    public static void removeAttributeSchemaWithoutI18nKey(Set attributeSchemas)
    {
        for (Iterator iter = attributeSchemas.iterator(); iter.hasNext(); ) {
            AttributeSchema as = (AttributeSchema)iter.next();
            String i18n = as.getI18NKey();
            if ((i18n == null) || (i18n.trim().length() == 0)) {
                iter.remove();
            }
        }
    }

    /**
     * Removes the attributes that do not match a specified set of any
     * display strings.
     *   
     * @param attributeSchemas Set of Attribute Schemas.
     */  
    public static void filterAttributes(Set attributeSchemas, String a[]) {
        removeAttributeSchemaWithoutI18nKey(attributeSchemas);
        int size = a.length;

        for (Iterator iter = attributeSchemas.iterator(); iter.hasNext(); ) {
            AttributeSchema as = (AttributeSchema)iter.next();
            String any = as.getAny();

            boolean found = false;
            for (int x = 0; x < size && !found; x++) {
                found = hasAnyAttribute(any, a[x]);
            }
            if (!found || (getTagClassName(as) == null)) {
                iter.remove();
            }
        }
    }

    public static boolean hasAnyAttribute(String any, String attribute) {
        boolean has = false;
        if ((any != null) && (any.trim().length() > 0)) {
            StringTokenizer st = new StringTokenizer(any, "|");
                                                                                
            while (st.hasMoreTokens() && !has) {
                String token = st.nextToken();
                has = token.equals(attribute);
            }
        }
        return has;
    }

    /**
     * Returns a replaced XML. This method replaces a property element.
     *
     * @param xml XML to operate on.
     * @param propertyName Name of the property (attribute) that is going to be
     *              replaced.
     * @param propertyXML XML to replace.
     * @return a replaced XML.
     */
    public static String swapXMLProperty(
        String xml,
        String propertyName,
        String propertyXML
    ) {
        String ccHead = "<cc name=\"" + propertyName + "\"";
        int start = xml.indexOf(ccHead);
        if (start != -1) {
            start = xml.lastIndexOf(
                PropertyTemplate.PROPERTY_START_OPEN_TAG, start);
            int end = xml.indexOf("</property>", start);
                                                                                
            if (end != -1) {
                xml = xml.substring(0, start) + propertyXML +
                    xml.substring(end + 11);
            }
        }
        return xml;
    }

    /**
     * Inserts XML to a property XML.
     *
     * @param xml Original property XML.
     * @param insertXML XML to be inserted to <code>xml</code>.
     * @param marker A marker where <code>insertXML</code> will be inserted.
     * @param afterMarker <code>true</code> to insert XML after marker.
     * @return the modified XML.
     */
    public static String insertXML(
        String xml,
        String insertXML,
        String marker,
        boolean afterMarker
    ) {
        int idx = xml.indexOf(marker);
        if (idx != -1) {
            int start = (afterMarker) ? idx + marker.length() : idx;
            xml = xml.substring(0, start) + insertXML +
                xml.substring(start);
        }
        return xml;
    }
    
    /**
     * Inserts XML to a property XML.
     *
     * @param xml Original property XML.
     * @param insertXML XML to be inserted to <code>xml</code>.
     * @param marker A marker where <code>insertXML</code> will be inserted.
     * @return the modified XML.
     */
    public static String insertXML(
        String xml,
        String insertXML,
        String marker
    ) {
        int idx = xml.indexOf(marker);
        if (idx != -1) {
            xml = xml.substring(0, idx) + insertXML +
                xml.substring(idx + marker.length()+1);
        }
        return xml;
    }

    /**
     * Returns true if sub configuration is supported.
     *
     * @return true if sub configuration is supported.
     */
    public boolean isSubConfigSupported() {
        return supportSubConfig;
    }

    /**
     * Set support sub configuration state.
     *
     * @param state <code>true</code> to support sub configuration.
     */
    public void setSupportSubConfig(boolean state) {
        supportSubConfig = state;
    }

    /**
     * Set name of view bean.
     *
     * @param name Name of view bean.
     */
    public void setViewBeanName(String name) {
        viewBeanName = name;
    }

    /**
     * Returns name of view bean.
     *
     * @return Name of view bean.
     */
    public String getViewBeanName() {
        return viewBeanName;
    }

    protected void getServiceResourceBundle() {
        String rbName = svcSchemaManager.getI18NFileName();
        try {
            serviceBundle = ResourceBundle.getBundle(rbName,
                model.getUserLocale());
        } catch (MissingResourceException e) {
            debug.warning(
                "PropertyXMLBuilderBase.getServiceResourceBundle " +
                e.getMessage());
        }
    }

    /**
     * Returns a set of attributeSchemas that are displayed.
     *
     * @return Set of attribute names that are displayed.
     */
    public Set getAttributeSchemas() {
        Collection values = mapTypeToAttributeSchema.values();
        Set set = new HashSet();
        for (Iterator iter = values.iterator(); iter.hasNext(); ) {
            set.addAll((Set)iter.next());
        }
        return set;
    }

    protected void getServiceResourceBundle(ServiceSchema ss) {
        String rbName = ss.getI18NFileName();
        try {
            serviceBundle = ResourceBundle.getBundle(rbName,
                model.getUserLocale());
        } catch (MissingResourceException e) {
            debug.warning(
                "PropertyXMLBuilderBase.getServiceResourceBundle", e);
        }
    }

    protected void buildReadonlyXML(
        AttributeSchema as,
        StringBuffer xml,
        AMModel model,
        ResourceBundle serviceBundle
    ) {
        String name = getAttributeNameForPropertyXML(as); 
        xml.append(PROPERTY_START_TAG);
        addLabel(as, xml, serviceBundle);
        Object[] param = {name};
        xml.append(MessageFormat.format(READONLY_START_TAG, param));
        xml.append(COMPONENT_END_TAG); 

        String tagClassName = getTagClassName(as);
        if (tagClassName.equals(TAGNAME_PASSWORD)) {
            param[0] = name + PropertyTemplate.PASSWORD_VALUE_TAG;
            xml.append(MessageFormat.format(READONLY_START_TAG, param));
            xml.append(COMPONENT_END_TAG); 
        }

        getInlineHelp(as, xml, serviceBundle);
        xml.append(PROPERTY_END_TAG);
    }    
    
    protected void buildAttributeSchemaTypeXML(
        AttributeSchema as,
        StringBuffer xml,
        AMModel model,
        ResourceBundle serviceBundle,
        boolean addSubSection
    ) {
        String tagClassName = getTagClassName(as);

        if (tagClassName != null) {
            String name = getAttributeNameForPropertyXML(as);

            boolean editableList = tagClassName.equals(TAGNAME_EDITABLE_LIST);
            boolean orderedList = tagClassName.equals(TAGNAME_ORDERED_LIST);
            boolean unorderedList = tagClassName.equals(TAGNAME_UNORDERED_LIST);
            boolean mapList = tagClassName.equals(TAGNAME_MAP_LIST);
            boolean globalMapList = tagClassName.equals(
                TAGNAME_GLOBAL_MAP_LIST);
            
            AttributeSchema.Type type = as.getType();
            AttributeSchema.UIType uitype = as.getUIType();
            boolean addremovelist = 
                type.equals(AttributeSchema.Type.MULTIPLE_CHOICE) &&
                (uitype != null) &&
                uitype.equals(AttributeSchema.UIType.ADDREMOVELIST);
            boolean listTyped = editableList || orderedList || unorderedList ||
                mapList || globalMapList || addremovelist;
            
            if (listTyped && addSubSection) {
                /*
                * create a subsection without a title to hold the 
                * editable list component.
                */
                xml.append(SUBSECTION_DUMMY_START_TAG);
            }

            if (needRequiredTag(as)) {
                xml.append(PROPERTY_REQUIRED_START_TAG);
            } else {
                xml.append(PROPERTY_START_TAG);
            }

            addLabel(as, xml, serviceBundle);


            if (addremovelist) {
                xml.append(GROUP_START_TAG).append(PROPERTY_START_TAG);
                appendAddRemoveListComponent(as, xml, serviceBundle);
                addremovelist = true;
            } else {
                //need to handle link specially.
                if (tagClassName.equals(TAGNAME_BUTTON)) {
                    String editString = model.getLocalizedString("label.edit");
                    Object[] pLink = {"'" + name + "'", editString};
                    xml.append(MessageFormat.format(
                        COMPONENT_BUTTON_START_TAG, pLink));
                } else if (tagClassName.equals(TAGNAME_HREF)) {
                    String editString = model.getLocalizedString("label.edit");
                    Object[] pLink = {
                        PropertyTemplate.PARAM_ATTR_NAME + "=" + name +
                        "&amp;" + 
                        PropertyTemplate.PARAM_PROPERTIES_VIEW_BEAN_URL +
                        "=" + as.getPropertiesViewBeanURL(),
                        "linkLabel" + name,
                        editString
                    };
                    xml.append(MessageFormat.format(
                        COMPONENT_LINK_START_TAG, pLink));
                } else if (tagClassName.equals(TAGNAME_CHECKBOX)) {
                    Object[] param = {name, tagClassName,
                        model.getLocalizedString("label.Enable")
                    };
                    xml.append(MessageFormat.format(
                        COMPONENT_BOOLEAN_START_TAG, param));
                } else if (listTyped) {
                    /* 
                     * putting the editable list component wihin a group tag
                     * to help isolate the list box and text box together on the
                     * page. Otherwise the list component blends in with other
                     * components on the page.
                     */
                    Object[] param = {
                        name,
                        model.getLocalizedString("label.current.value"),
                        model.getLocalizedString("label.new.value")
                    };

                    xml.append(GROUP_START_TAG).append(PROPERTY_START_TAG);

                    if (editableList) {
                        xml.append(MessageFormat.format(
                            COMPONENT_EDITABLE_LIST_START_TAG, param));
                    } else if (orderedList) {
                        xml.append(MessageFormat.format(
                            COMPONENT_ORDERED_LIST_START_TAG, param));
                    } else if (unorderedList) {
                        xml.append(MessageFormat.format(
                            COMPONENT_UNORDERED_LIST_START_TAG, param));
                    } else if (mapList) {
                        xml.append(MessageFormat.format(
                            COMPONENT_MAP_LIST_START_TAG, param));
                    } else if (globalMapList) {
                        xml.append(MessageFormat.format(
                            COMPONENT_GLOBAL_MAP_LIST_START_TAG, param));
                    }
                } else {
                    Object[] param = {name, tagClassName};
                    xml.append(MessageFormat.format(COMPONENT_START_TAG,param));

                    /*
                     * if its a textarea component add the no localize attribute
                     * set the size of the text field based on its syntax
                     */
                    if (tagClassName.equals(TAGNAME_TEXTFIELD)) {
                        Object[] pSize = {getStringFieldSize(as)};
                        xml.append(MessageFormat.format(TEXTBOX_SIZE_TAG, 
                            pSize));
                        xml.append(NON_LOCALIZED_FIELD);
                        xml.append(NO_AUTO_SUBMIT);
                    } else if (tagClassName.equals(TAGNAME_TEXTAREA)) {
                        xml.append(NON_LOCALIZED_FIELD);
                    } else if (tagClassName.equals(TAGNAME_PASSWORD)) {
                        xml.append(NO_AUTO_SUBMIT);
                    }

                    appendChoiceValues(as, xml, model, serviceBundle);
                }

                xml.append(COMPONENT_END_TAG);
            }

            if (type.equals(AttributeSchema.Type.MULTIPLE_CHOICE)) {
                if ((uitype == null) ||
                    !uitype.equals(AttributeSchema.UIType.ADDREMOVELIST)) {
                    appendMultipleChoiceCheckboxes(as, xml, serviceBundle);
                }
            }
            
            appendDateMarker(as, xml);
            getInlineHelp(as, xml, serviceBundle);

            xml.append(PROPERTY_END_TAG);

            // close off the group and subsection tags for the editable list
            if (listTyped) {
                xml.append(GROUP_END_TAG)
                    .append("&lt;p>")
                    .append(PROPERTY_END_TAG);
                if (addSubSection) {
                    xml.append(SUBSECTION_END_TAG);
                }
            }
        }
    }

    /**
     * This will get the syntax of the attribute, and compute an appropriate
     * size for the length of the text field. This will help the user 
     * understand what the expected value is for the field. The field sizes
     * are set in amProperty.properties so the customer can change the
     * defaults as necessary.
     */
    private String getStringFieldSize(AttributeSchema as) {
        String size = "";
        AttributeSchema.Syntax syntax = as.getSyntax();
        String valSyntax = (String)mapSyntaxToName.get(syntax);
        if (valSyntax != null) {
            try {
                if (valSyntax.equals("number_range")) {
                    String tmp = as.getEndRange();
                    size = Integer.toString(tmp.length());
                } else {
                    size = properties.getString("textfield." + valSyntax);
                }
            } catch (MissingResourceException e) {
                size = properties.getString("textfield.default");
            }
        }
        return size;
    }

    private void appendDateMarker(
        AttributeSchema as,
        StringBuffer xml
    ) {
        AttributeSchema.Syntax syntax = as.getSyntax();
        if (syntax.equals(AttributeSchema.Syntax.DATE)) {
            Object[] arg = {as.getName()};
            xml.append(MessageFormat.format(PropertyTemplate.DATE_MARKER, arg));
        }
    }

    protected void buildConfirmPasswordXML(
        AttributeSchema as,
        StringBuffer xml,
        AMModel model,
        ResourceBundle serviceBundle 
    ) {

        String name = getAttributeNameForPropertyXML(as);

        if (needRequiredTag(as)) {
            xml.append(PROPERTY_REQUIRED_START_TAG);
        } else {
            xml.append(PROPERTY_START_TAG);
        }

        addPasswordConfirmLabel(as, xml, serviceBundle, model);
        Object[] param = {name};
        xml.append(MessageFormat.format(COMPONENT_PWD_START_TAG, param));
        xml.append(COMPONENT_END_TAG);
        xml.append(PROPERTY_END_TAG);
    }

    private void addLabel(
        AttributeSchema as,
        StringBuffer xml,
        ResourceBundle serviceBundle
    ) {
        String name = getAttributeNameForPropertyXML(as);
        Object[] params = {name,
            escapeSpecialChars(com.sun.identity.shared.locale.Locale.getString(
                serviceBundle, as.getI18NKey(), debug)),
            name};
        xml.append(MessageFormat.format(LABEL_TAG, params));
    }

    private void addPasswordConfirmLabel(
        AttributeSchema as,
        StringBuffer xml,
        ResourceBundle serviceBundle,
        AMModel model
    ) {
        String name = as.getName();
        Object[] labelParam = {
            escapeSpecialChars(com.sun.identity.shared.locale.Locale.getString(
                serviceBundle, as.getI18NKey(), debug))
        };
        String label = MessageFormat.format(model.getLocalizedString(
            "password.confirm.label"), labelParam);

        Object[] params = {name, label, name};
        xml.append(MessageFormat.format(LABEL_TAG, params));
    }

    private void getInlineHelp(
        AttributeSchema as,
        StringBuffer xml,
        ResourceBundle serviceBundle
    ) {
        String i18nKey = as.getI18NKey();
        if ((i18nKey == null) || (i18nKey.length() < 1)) {
            return;
        }
        
        try {
            String helpString = CCPropertySheetTag.getDynamicHelp(serviceBundle, i18nKey + ".help");
            if (helpString == null) {
                return;
            }
            Object[] params = {as.getName(), escapeSpecialChars(helpString)};
            xml.append(MessageFormat.format(HELP_TAG, params));
        } catch (MissingResourceException e) {
            // no-op, assumption here is help is not defined for this attr.
        }
    }

    public String escapeSpecialChars(String text) {
        String escaped = null;

        if ((text != null) && (text.trim().length() > 0)) {
            StringBuilder sb = new StringBuilder(text.length());
            int len = text.length();

            for (int i = 0; i < len; i++) {
                char c = text.charAt(i);

                switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                default:
                    sb.append(c);
                    break;
                }
            }

            escaped = sb.toString();
        } else {
            escaped = text;
        }

        return escaped;
    }

    private void appendChoiceValues(
        AttributeSchema as,
        StringBuffer xml,
        AMModel model,
        ResourceBundle serviceBundle
    ) {
        AttributeSchema.Type type = as.getType();
                                                                                
        if (type.equals(AttributeSchema.Type.SINGLE)) {
            AttributeSchema.UIType uitype = as.getUIType();
                                                                                
            if ((uitype != null) && uitype.equals(AttributeSchema.UIType.RADIO))
            {
                Object[] p1 = {
                    model.getLocalizedString("label.Yes"), "true"};
                xml.append(MessageFormat.format(OPTION_TAG, p1));
                Object[] p2 = {model.getLocalizedString("label.No"), "false"};
                xml.append(MessageFormat.format(OPTION_TAG, p2));
            }
        } else if (type.equals(AttributeSchema.Type.SINGLE_CHOICE)) {
            appendChoiceValueForSelectableComponent(as, xml, serviceBundle);
        }
    }

    private void appendMultipleChoiceCheckboxes(
        AttributeSchema as,
        StringBuffer xml,
        ResourceBundle serviceBundle
    ) {
        Map map = new HashMap();
        Set sorted = getSortedChoiceValues(as, map, serviceBundle);
        String name = getAttributeNameForPropertyXML(as);
        Object[] nameArg = {name};
        xml.append(MessageFormat.format(DYN_GUI_MULTIPLE_LIST_MARKER_XML,
            nameArg));

        for (Iterator iter = sorted.iterator(); iter.hasNext(); ) {
            String localizedName = (String)iter.next();
            Object[] params = {
                name, (String)map.get(localizedName), localizedName};
            xml.append(MessageFormat.format(DYN_GUI_MULTIPLE_LIST_CHECKBOX_XML,
                params));
        }
    }
    
    private void appendAddRemoveListComponent(
        AttributeSchema as,
        StringBuffer xml,
        ResourceBundle serviceBundle) {
        Map map = new HashMap();
        Set sorted = getSortedChoiceValues(as, map, serviceBundle);
        String name = as.getName();
        Object[] nameArg = {name};
        xml.append(MessageFormat.format(ADD_REMOVE_COMPONENT_XML, nameArg));
        
        for (Iterator iter = sorted.iterator(); iter.hasNext(); ) {
            String localizedName = (String)iter.next();
            Object[] params = {localizedName, (String)map.get(localizedName)};
            xml.append(MessageFormat.format(OPTION_TAG, params));
        }
        xml.append(COMPONENT_END_TAG);
    }

    public static String getMultipleChoiceCheck(
        String attrName,
        Set choices
    ) {
        StringBuilder xml = new StringBuilder();
        Set sorted = new TreeSet();
        if ((choices != null) && !choices.isEmpty()) {
            for (Iterator iter = choices.iterator(); iter.hasNext(); ) {
                sorted.add(iter.next());
            }

            for (Iterator iter = sorted.iterator(); iter.hasNext(); ) {
                String choice = (String)iter.next();
                Object[] params = {attrName, choice, choice};
                xml.append(
                    MessageFormat.format(DYN_GUI_MULTIPLE_LIST_CHECKBOX_XML,
                    params));
            }
        }
        return xml.toString();
    }
    
    /*
     * Sets the value for the current realm name being used to build the 
     * property sheet xml page.
     *
     * @param value the name of the realm 
     */
    protected void setCurrentRealm(String value) {
        currentRealm = value;
    }
    
    /*
     * Returns the current realm name value which can be used for constructing
     * the xml page. If the realm name has not been set the default location
     * which is stored in <code>AMModelBase</code> will be returned. If no
     * location has been set in the model the realm where the user logged in
     * is returned.
     *
     * @return name of the realm used for building the page.
     */
    protected String getCurrentRealm() {
        return (currentRealm != null) ? currentRealm : model.getLocationDN();            
    }
    
    private Set getSortedChoiceValues(
        AttributeSchema as,
        Map values,
        ResourceBundle serviceBundle
    ) {
        Set sorted = new TreeSet();
        Map tmp = new HashMap(2);
        tmp.put(Constants.ORGANIZATION_NAME, getCurrentRealm());
        String[] choices =  as.getChoiceValues(tmp);
        
        if ((choices != null) && (choices.length > 0)) {
            for (int i = 0; i < choices.length; i++) {
                String val = choices[i];
                String i18nKey = as.getChoiceValueI18NKey(val);
                String localizedName = null;

                if ((i18nKey == null) || (i18nKey.trim().length() == 0)) {
                    localizedName = val;
                } else {
                    localizedName =
                        com.sun.identity.shared.locale.Locale.getString(
                            serviceBundle, i18nKey, debug);
                }
                values.put(localizedName, val);
                sorted.add(localizedName);
            }
        }

        return sorted;
    }
        

    private void appendChoiceValueForSelectableComponent(
        AttributeSchema as,
        StringBuffer xml,
        ResourceBundle serviceBundle
    ) {
        Map map = new HashMap();
        boolean defaultValue = false;
        Set sorted = getSortedChoiceValues(as, map, serviceBundle);

        for (Iterator iter = sorted.iterator(); iter.hasNext(); ) {
            String localizedName = (String)iter.next();
            String value = (String)map.get(localizedName);
            Object[] params = {escapeSpecialChars(localizedName),
                escapeSpecialChars(value)};
            if (!defaultValue) {
                xml.append("<attribute name=\"defaultValue\" value=\"")
                    .append(escapeSpecialChars(value))
                    .append("\" />");
                defaultValue = true;
            }
            xml.append(MessageFormat.format(OPTION_TAG, params));
        }
    }

    /*
    * This will remove an entire subsection which surrounds the 
    * specified attribute. The subsection will be replaced with a 
    * <code>&lt;property&gt;</code> tag.
    *
    * @param original xml string to be modified.
    * @param attribute name which is going to be removed.
    * @param insert string with new xml.
    *
    * @return new xml string without subsection.
    */
    public static String removeSubSection(
        String original, 
        String attribute, 
        String insert
    ) {
        int mark = original.indexOf(attribute);
        String start = original.substring(0, mark);
        int index = start.lastIndexOf("<subsection");

        // the attribute is not within a subsection
        if (index == -1) {
            return original;
        }
        start = start.substring(0, index);

        String end = original.substring(mark);
        index = end.indexOf("</subsection>") + 13;

        // the end of the subsection is missing. invalid xml perhaps.
        if (index == -1) {
            return original;
        }
        end = end.substring(index);

        return start + insert + end;
    }

    /**
     * Returns an altered XML. This method prepends a property element.
     *
     * @param xml XML to operate on.
     * @param propertyXML XML to replace.
     * @return the altered XML.
     */
    public static String prependXMLProperty(
        String xml,
        String propertyXML
    ) {
        int idx = xml.indexOf(PropertyTemplate.START_TAG);
        idx += PropertyTemplate.START_TAG.length();
        return xml.substring(0, idx) + propertyXML + xml.substring(idx);
    }

    /**
     * Returns an altered XML. This method appends a property element.
     *
     * @param xml XML to operate on.
     * @param propertyXML XML to replace.
     * @return the altered XML.
     */
    public static String appendXMLProperty(
        String xml,
        String propertyXML
    ) {
        int idx = xml.lastIndexOf(PropertyTemplate.SECTION_END_TAG);
        if (idx != -1) {
            xml = xml.substring(0, idx) + propertyXML + xml.substring(idx);
        }
        return xml;
    }

    /**
     * Returns service schema manager.
     *
     * @return service schema manager.
     */
    public ServiceSchemaManager getServiceSchemaManager() {
        return svcSchemaManager;
    }
    
    protected void buildSchemaTypeXML(
        String schemaTypeName,
        Set attributeSchemas,
        StringBuffer xml,
        AMModel model,
        ResourceBundle serviceBundle,
        Set readonly
    ) {
        buildSchemaTypeXML(schemaTypeName, attributeSchemas, xml, model, serviceBundle, readonly, true, true);
    }

    protected void buildSchemaTypeXML(
        String schemaTypeName,
        Set attributeSchemas,
        StringBuffer xml,
        AMModel model,
        ResourceBundle serviceBundle,
        Set readonly,
        boolean section,
        boolean addSubSection
    ) {
        if (section) {
            String label = "lbl" + schemaTypeName.replace('.', '_');
            Object[] params = { label, schemaTypeName};
            xml.append(MessageFormat.format(SECTION_START_TAG, params));
        }        

        List sorted = new ArrayList(attributeSchemas);
        Collections.sort(sorted, new AttributeSchemaComparator(null));

        for (Iterator iter = sorted.iterator(); iter.hasNext(); ) {
            AttributeSchema as = (AttributeSchema)iter.next();
                                                                                
            if (allAttributesReadonly || readonly.contains(as.getName())) {
                buildReadonlyXML(as, xml, model, serviceBundle);
            } else {
                buildAttributeSchemaTypeXML(as, xml, model, serviceBundle, addSubSection);
                String tagClassName = getTagClassName(as);
                                                                                
                if (tagClassName.equals(TAGNAME_PASSWORD)) {
                    buildConfirmPasswordXML(as, xml, model, serviceBundle);
                }
            }
        }

        xml.append((section ? SECTION_END_TAG : ""));
    }
    
    protected void buildSchemaTypeXML(
        String schemaTypeName,
        Set<AttributeSchema> attributeSchemas,
        StringBuffer xml,
        AMModel model,
        ResourceBundle serviceBundle,
        Set readonly,
        List<String> sectionList
    ) {
        Object[] params = { schemaTypeName, schemaTypeName, "true" };
        Set<AttributeSchema> as = getAttributeSchemaForSection(attributeSchemas, sectionList);
        
        // skip empty sections
        if (as.isEmpty()) {
            return;
        }
        
        xml.append(MessageFormat.format(SUBSECTION_START_TAG, params));
        buildSchemaTypeXML(schemaTypeName, as, xml, model, serviceBundle, readonly, false, false);
        xml.append(SUBSECTION_END_TAG);
    } 
    
    protected Set<AttributeSchema> getAttributeSchemaForSection(Set<AttributeSchema> attributeSchemas, List<String> sectionList) {
        Set<AttributeSchema> result = new HashSet<AttributeSchema>();
        
        for (AttributeSchema attribute : attributeSchemas) {
            if (sectionList.contains(attribute.getName())) {
                result.add(attribute);
            }
        }
        
        return result;
    }

    /**
     * Returns a property XML of a given well formed attributes XML string.
     *
     * @param properties Well formed attributes XML string..
     * @return a property XML of a given well formed attributes XML string.
     */
    public static String formPropertySheetXML(String properties) {
        return formPropertySheetXML(properties, false);
    }

    /**
     * Returns a property XML of a given well formed attributes XML string.
     *
     * @param properties Well formed attributes XML string..
     * @param addSection <code>true</code> to add a dummy section to the XML.
     * @return a property XML of a given well formed attributes XML string.
     */
    public static String formPropertySheetXML(
        String properties,
        boolean addSection
    ) {
        return (addSection)
            ?
                getXMLDefinitionHeader() + PropertyTemplate.START_TAG +
                    PropertyTemplate.SECTION_DUMMY_START_TAG +
                        properties + PropertyTemplate.SECTION_END_TAG +
                            PropertyTemplate.END_TAG
            :
                getXMLDefinitionHeader() + PropertyTemplate.START_TAG +
                        properties + PropertyTemplate.END_TAG;
    }

    /**
     * Set all all attribute values read only.
     *
     * @param flag true if all attribute values are read only.
     */
    public void setAllAttributeReadOnly(boolean flag) {
        allAttributesReadonly = flag;
    }

    protected String getAttributeNameForPropertyXML(AttributeSchema as) {
        return as.getName();
    }

    /**
     * Performs a tag substitution on a string.
     *
     * @param template Template for tag swapping.
     * @param tag Tag.
     * @param value Value of Tag.
     */
    protected String tagSwap(String template, String tag, String value) {
        int valLen = value.length();
        int tagLen = tag.length();
        int idx = template.indexOf(tag);
        while (idx != -1) {
            template = template.substring(0, idx) + value +
                template.substring(idx + tagLen);
            idx = template.indexOf(tag, idx + valLen);
        }
        return template;
    }
    
    private static boolean needRequiredTag(AttributeSchema as) {
        return hasAnyAttribute(as.getAny(), ANY_REQUIRED) ||
            ((as.getValidator() != null) &&
            as.getValidator().equals("RequiredValueValidator"));
    }

    public static String getXMLDefinitionHeader() {
        return PropertyTemplate.DEFINITION;
    }
}

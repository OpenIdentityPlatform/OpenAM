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
 * $Id: PropertyTemplate.java,v 1.5 2008/10/02 16:31:29 veiming Exp $
 *
 */

/*
 * Portions Copyrighted 2011-2013 ForgeRock, Inc.
 */

package com.sun.identity.console.property;

public interface PropertyTemplate {
    String BLANK_VALUE = "blank.header";

    String DYN_GUI_MULTIPLE_LIST_CHECKBOX_PREFIX =
        "__DYN_GUI_MULTIPLE_LIST_CHECKBOX_";
    String DYN_GUI_MULTIPLE_LIST_CHECKBOX_MARKER =
        "__DYN_GUI_MULTIPLE_LIST_CHECKBOX_MARKER_";

    String TAGNAME_HREF =
        "com.sun.web.ui.taglib.html.CCHrefTag";
    String TAGNAME_TEXTFIELD =
        "com.sun.web.ui.taglib.html.CCTextFieldTag";
    String TAGNAME_TEXTAREA =
        "com.sun.web.ui.taglib.html.CCTextAreaTag";
    String TAGNAME_BUTTON =
        "com.sun.web.ui.taglib.html.CCButtonTag";
    String TAGNAME_CHECKBOX =
        "com.sun.web.ui.taglib.html.CCCheckBoxTag";
    String TAGNAME_PASSWORD =
        "com.sun.web.ui.taglib.html.CCPasswordTag";
    String TAGNAME_EDITABLE_LIST =
        "com.sun.web.ui.taglib.editablelist.CCEditableListTag";
    String TAGNAME_ORDERED_LIST =
        "com.sun.identity.console.ui.taglib.CCOrderedListTag";
    String TAGNAME_UNORDERED_LIST =
        "com.sun.identity.console.ui.taglib.CCUnOrderedListTag";
    String TAGNAME_MAP_LIST =
        "com.sun.identity.console.ui.taglib.CCMapListTag";
    String TAGNAME_GLOBAL_MAP_LIST =
        "com.sun.identity.console.ui.taglib.CCGlobalMapListTag";
    String TAGNAME_DROPDOWN_MENU = 
        "com.sun.web.ui.taglib.html.CCDropDownMenuTag";
    String TAGNAME_MULTIPLE_CHOICE =
        "com.sun.web.ui.taglib.html.CCSelectableListTag";

    String NULL_TYPE = "null";
                                                                                
    String ANY_REQUIRED = "required";
    String ANY_DISPLAY = "display";
    String ANY_ADMIN_DISPLAY = "adminDisplay";
    String ANY_DISPLAYRO = "displayRO";
    String ANY_ADMIN_DISPLAYRO = "adminDisplayRO";
    String DATE_MARKER_NAME = "dateMarker";
                                                                                
    String PWD_CONFIRM_SUFFIX = "_confirm";

    String DEFINITION = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
        "<!DOCTYPE propertysheet SYSTEM \"propertysheet.dtd\">\n";
    String START_TAG = "<propertysheet>\n";
    String END_TAG = "</propertysheet>\n";

    String SECTION_DUMMY_START_TAG =
        "<section name=\"dummy\" defaultValue=\"blank.header\" >";
    String SECTION_START_TAG = "<section name=\"{0}\" defaultValue=\"{1}\">\n";
    String SECTION_END_TAG = "</section>\n";
    
    String SUBSECTION_DUMMY_START_TAG = "<subsection name=\"dummy\" defaultValue=\"\">";
    String SUBSECTION_START_TAG = "<subsection name=\"{0}\" defaultValue=\"{1}\" spacer=\"{2}\">";
    String SUBSECTION_END_TAG = "</subsection>";
    
    String GROUP_START_TAG = "<ccgroup>";
    String GROUP_END_TAG = "</ccgroup>";
    
    String PROPERTY_START_TAG = "<property >\n";
    String PROPERTY_START_OPEN_TAG = "<property ";
    String PROPERTY_REQUIRED_START_TAG = "<property required=\"true\">\n";
    String PROPERTY_END_TAG = "</property>\n";

    String LABEL_TAG =
        "<label name=\"lbl{0}\" defaultValue=\"{1}\" labelFor=\"{2}\" />\n";
    String LABEL_PWD_TAG =
        "<label name=\"lbl{0}_confirm\" defaultValue=\"{1}\" labelFor=\"{2}_confirm\" />\n";

    String COMPONENT_START_TAG = "<cc name=\"{0}\" tagclass=\"{1}\">\n";
    String COMPONENT_PWD_START_TAG = "<cc name=\"{0}_confirm\" tagclass=\"com.sun.web.ui.taglib.html.CCPasswordTag\">\n";
    String COMPONENT_BOOLEAN_START_TAG = "<cc name=\"{0}\" tagclass=\"{1}\">\n<attribute name=\"label\" value=\"{2}\"/>\n";
    String READONLY_START_TAG = "<cc name=\"{0}\" tagclass=\"com.sun.web.ui.taglib.html.CCStaticTextFieldTag\">\n";
    String COMPONENT_END_TAG = "</cc>\n";
    
    String COMPONENT_EDITABLE_LIST_START_TAG = "<cc name=\"{0}\" tagclass=\"com.sun.web.ui.taglib.editablelist.CCEditableListTag\"> <attribute name=\"listboxLabel\" value=\"{1}\" /><attribute name=\"textboxLabel\" value=\"{2}\" />";
    String COMPONENT_ORDERED_LIST_START_TAG = "<cc name=\"{0}\" tagclass=\"com.sun.identity.console.ui.taglib.CCOrderedListTag\">";
    String COMPONENT_UNORDERED_LIST_START_TAG = "<cc name=\"{0}\" tagclass=\"com.sun.identity.console.ui.taglib.CCUnOrderedListTag\"> <attribute name=\"listboxLabel\" value=\"{1}\" /><attribute name=\"textboxLabel\" value=\"{2}\" />";
    String COMPONENT_MAP_LIST_START_TAG = "<cc name=\"{0}\" tagclass=\"com.sun.identity.console.ui.taglib.CCMapListTag\"> <attribute name=\"listboxLabel\" value=\"{1}\" /><attribute name=\"textboxLabel\" value=\"{2}\" />";
    String COMPONENT_GLOBAL_MAP_LIST_START_TAG = "<cc name=\"{0}\" tagclass=\"com.sun.identity.console.ui.taglib.CCGlobalMapListTag\"> <attribute name=\"listboxLabel\" value=\"{1}\" /><attribute name=\"textboxLabel\" value=\"{2}\" />";

    String PASSWORD_VALUE_TAG = "__AM_PASSWORD_TAG__";

    String COMPONENT_LINK_START_TAG = "<cc name=\"dynLink\" tagclass=\"com.sun.web.ui.taglib.html.CCHrefTag\">\n<attribute name=\"onClick\" value=\"openNewWindow();\" /><attribute name=\"queryParams\" value=\"{0}\" /><attribute name=\"target\" value=\"newwindow\" /><cc name=\"{1}\" tagclass=\"com.sun.web.ui.taglib.html.CCStaticTextFieldTag\">\n<attribute name=\"defaultValue\" value=\"{2}\" /></cc>\n";
    String COMPONENT_BUTTON_START_TAG = "<cc name=\"dynLink\" tagclass=\"com.sun.web.ui.taglib.html.CCButtonTag\">\n<attribute name=\"onClick\" value=\"openNewWindow(); submitButton(this, {0});\" /><attribute name=\"defaultValue\" value=\"{1}\" />\n";
    String TEXTBOX_SIZE_TAG =
        "<attribute name=\"size\" value=\"{0}\" />\n";
    String NON_LOCALIZED_FIELD = "<attribute name=\"localizeDisplayFieldValue\" value=\"false\" />\n";
    String NO_AUTO_SUBMIT = "<attribute name=\"autoSubmit\" value=\"false\" />\n";

    String MULTIPLE_ATTRIBUTE_TAG =
        "<attribute name=\"multiple\" value=\"true\" />\n";
    String LIST_SIZE_TAG =
        "<attribute name=\"size\" value=\"8\" />\n";
    String OPTION_TAG = "<option label=\"{0}\" value=\"{1}\" />\n";

    String HELP_TAG = " <fieldhelp name=\"help{0}\" defaultValue=\"{1}\" />\n";

    String STATIC_TEXT_TAG_NAME =
        "com.sun.web.ui.taglib.html.CCStaticTextFieldTag";

    String COMPONENT_ADD_REMOVE_DUPLICATION_EDIT_TAG =
        "<property span=\"true\">\n<cc name=\"tbl{0}\" tagclass=\"com.sun.web.ui.taglib.table.CCActionTableTag\" >\n<attribute name=\"title\" value=\"{1}\" />\n<attribute name=\"showPaginationControls\" value=\"false\" />\n<attribute name=\"selectionType\" value=\"multiple\" />\n<attribute name=\"selectionJavascript\" value=\"clickedNameValueList(this)\" />\n<attribute name=\"showAdvancedSortingIcon\" value=\"false\" />\n<attribute name=\"showLowerActions\" value=\"false\" />\n<attribute name=\"showPaginationControls\" value=\"false\" />\n<attribute name=\"showPaginationIcon\" value=\"false\" />\n<attribute name=\"showSelectionIcons\" value=\"false\" />\n<attribute name=\"showSelectionSortIcon\" value=\"false\" />\n<attribute name=\"showSortingRow\" value=\"true\" />\n</cc>\n</property>\n";

    String DATE_MARKER = "<cc name=\"dateMarker{0}\" tagclass=\"com.sun.web.ui.taglib.html.CCHiddenTag\"><attribute name=\"defaultValue\" value=\"true\" /></cc>";

    String PARAM_ATTR_NAME = "attrname";
    String PARAM_PROPERTIES_VIEW_BEAN_URL = "propviewbean";

    String DYN_GUI_MULTIPLE_LIST_MARKER_XML =
        "<cc name=\"__DYN_GUI_MULTIPLE_LIST_CHECKBOX_MARKER_{0}\" tagclass=\"com.sun.web.ui.taglib.html.CCHiddenTag\"><attribute name=\"defaultValue\" value=\"true\" /></cc>";
    String DYN_GUI_MULTIPLE_LIST_CHECKBOX_XML =
        "<cc name=\"__DYN_GUI_MULTIPLE_LIST_CHECKBOX_{0}_{1}\" tagclass=\"com.sun.web.ui.taglib.html.CCCheckBoxTag\"><attribute name=\"label\" value=\"{2}\" /></cc><cc name=\"multiplelistsep\" tagclass=\"com.sun.web.ui.taglib.html.CCStaticTextFieldTag\"><attribute name=\"defaultValue\" value=\"&lt;br>\" /><attribute name=\"escape\" value=\"false\"/></cc>";

    String ADD_REMOVE_COMPONENT_XML = 
        "<cc name=\"{0}\" tagclass=\"com.sun.web.ui.taglib.addremove.CCAddRemoveTag\">" +
        "<attribute name=\"showMoveUpDownButtons\" value=\"false\" /><attribute name=\"labelLocation\" value=\"LABEL_ABOVE\" />";
    String SUB_CONFIG_TABLE_VIEW_BEAN = "<viewBean>";
    String SUB_CONFIG_TABLE_XML =
        "<section name=\"subconfig\" defaultValue=\"subconfig.section.header\"><property span=\"true\"><cc name=\"tblSubConfig\" tagclass=\"com.sun.web.ui.taglib.table.CCActionTableTag\"><attribute name=\"title\" value=\"services.subconfig.table.title\" /><attribute name=\"empty\" value=\"services.subconfig.table.empty.message\" /><attribute name=\"showPaginationControls\" value=\"false\" /><attribute name=\"selectionType\" value=\"multiple\" /><attribute name=\"showAdvancedSortingIcon\" value=\"false\" /><attribute name=\"showLowerActions\" value=\"false\" /><attribute name=\"showPaginationControls\" value=\"false\" /><attribute name=\"showPaginationIcon\" value=\"false\" /><attribute name=\"showSelectionIcons\" value=\"true\" /><attribute name=\"showSelectionSortIcon\" value=\"false\" /><attribute name=\"showSortingRow\" value=\"true\" /><attribute name=\"selectionJavascript\" value=\"toggleTblButtonState('" + SUB_CONFIG_TABLE_VIEW_BEAN + "', '" + SUB_CONFIG_TABLE_VIEW_BEAN + ".tblSubConfig', 'tblButton', '" + SUB_CONFIG_TABLE_VIEW_BEAN + ".tblSubConfigButtonDelete', this)\" /></cc></property></section>";

    String DEFAULT_TEXTFIELD_SIZE = "50";  
}

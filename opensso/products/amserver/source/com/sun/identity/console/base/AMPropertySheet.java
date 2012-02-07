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
 * $Id: AMPropertySheet.java,v 1.11 2009/10/21 00:46:38 asyhuang Exp $
 *
 */

package com.sun.identity.console.base;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.ContainerView;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.ViewBean;
import com.iplanet.jato.view.html.Option;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.property.PropertyTemplate;
import com.sun.identity.console.ui.model.CCMapListModel;
import com.sun.identity.console.ui.model.CCOrderedListModel;
import com.sun.identity.console.ui.model.CCUnOrderedListModel;
import com.sun.identity.console.ui.view.CCMapList;
import com.sun.identity.console.ui.view.CCOrderedList;
import com.sun.identity.console.ui.view.CCUnOrderedList;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.web.ui.model.CCAddRemoveModel;
import com.sun.web.ui.model.CCDateTimeModel;
import com.sun.web.ui.model.CCEditableListModel;
import com.sun.web.ui.model.CCPropertySheetModelInterface;
import com.sun.web.ui.view.addremove.CCAddRemove;
import com.sun.web.ui.view.datetime.CCDateTime;
import com.sun.web.ui.view.editablelist.CCEditableList;
import com.sun.web.ui.view.html.CCCheckBox;
import com.sun.web.ui.view.html.CCRadioButton;
import com.sun.web.ui.view.html.CCSelect;
import com.sun.web.ui.view.propertysheet.CCPropertySheet;
import com.sun.web.ui.view.table.CCActionTable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * This class provides convenient methods to get and set attribute values to
 * property sheet view.
 * IMPORTANT: 
 * The attribute names that is passed into these convenient methods need to
 * correspond to the property sheet xml.
 */
public class AMPropertySheet
    extends CCPropertySheet
{
    private static String PASSWORD_MASK = "*********";
    
    // Copying set of constructors from parent class
    public AMPropertySheet(
        ContainerView parent,
        CCPropertySheetModelInterface model,
        String name
    ) {
        super(parent, model, name);
    }

    public AMPropertySheet(ContainerView parent, String name) {
        super(parent, name);
    }

    /**
     * Set values to model of a <code>CCPropertySheet</code> component.
     *          
     * @param attributeValues Map of attribute name to values.
     */
    public void setAttributeValues(Map attributeValues, AMModel amModel) {
        AMPropertySheetModel model = (AMPropertySheetModel)getModel();
        ViewBean parent = getParentViewBean();

        for (Iterator iter = attributeValues.keySet().iterator();
            iter.hasNext();
        ) {
            String name = (String)iter.next();
            // different attribute types set different types of data
            // get Object from the values and convert as needed.
            Object values = attributeValues.get(name);
            View view = null;

            try {
                view = parent.getChild(name);
            } catch (IllegalArgumentException e) {
                // skip this attribute. its not defined in the property sheet
                continue;
            }

            if (view != null) {
                if (setValuesToMultipleChoices(
                    parent, name, values, amModel, model) ||
                    setValuesToAddRemove(view, name, values, amModel, model) ||
                    setValuesToEditableList(view, name, values, amModel,model)||
                    setValuesToOrderedList(view, name, values, amModel,model) ||
                    setValuesToUnOrderedList(view, name, values, amModel,model)||
                    setValuesToMapList(view, name, values, amModel,model) ||
                    setValuesToDateTime(view, name, values, amModel,model)||
                    setValuesToSelect(view, name, values, amModel, model)
                ) {
                    // do nothing.
                } else {
                    // all other element types should be passing a set
                    if (Set.class.isInstance(values)) {
                        if (model.isChildSupported(
                            name + PropertyTemplate.PWD_CONFIRM_SUFFIX)
                        ) {
                            model.setValues(
                                name + PropertyTemplate.PWD_CONFIRM_SUFFIX,
                                ((Set)values).toArray(), amModel);
                        }

                        if ((values != null) && !((Set)values).isEmpty() &&
                                model.isChildSupported(
                                    name + PropertyTemplate.PASSWORD_VALUE_TAG)
                        ) {
                            model.setValue(name, PASSWORD_MASK);
                        } else {
                            model.setValues(
                                name, ((Set)values).toArray(), amModel);
                        }
                    }
                }
            }
        }
    }

    private boolean setValuesToMultipleChoices(
        ViewBean parent,
        String name,
        Object values,
        AMModel amModel,
        AMPropertySheetModel model
    ) {
        boolean set = false;
        if (Set.class.isInstance(values)) {
            for (Iterator i = ((Set)values).iterator(); i.hasNext(); ) {
                String val = (String)i.next();
                if ((val != null) && (val.indexOf('=') == -1)) {
                    val = escapeSqBracket(val); 
                    try {
                        View view = parent.getChild(
                        PropertyTemplate.DYN_GUI_MULTIPLE_LIST_CHECKBOX_PREFIX +
                        name + "_" + val);
                        if (view instanceof CCCheckBox) {
                            ((CCCheckBox)view).setChecked(true);
                        }
                        set = true;
                    } catch (IllegalArgumentException e) {
                        //ok if it is not found.
                    }
                }
            }
        }
        return set;
    }

    private boolean setValuesToDateTime(
        View view,
        String name,
        Object values,
        AMModel amModel,
        AMPropertySheetModel model
    ) {
        boolean set = false;
        if (CCDateTime.class.isInstance(view)) {
            CCDateTimeModel m = (CCDateTimeModel)model.getModel(name);
            if (Set.class.isInstance(values)) {
                Set v = (Set)values;
                if (values != null) {
                    m.setStartDateTime((Date)v.iterator().next());
                }
            }
            set = true;
        }
        return set;
    }
    private boolean setValuesToUnOrderedList(
        View view,
        String name,
        Object values,
        AMModel amModel,
        AMPropertySheetModel model
    ) {
        boolean set = false;
        if (CCUnOrderedList.class.isInstance(view)) {
            ((CCUnOrderedList)view).resetStateData();
            CCUnOrderedListModel m = (CCUnOrderedListModel)model.getModel(name);
            if (Set.class.isInstance(values)) {
                Set sorted = new TreeSet(new OrderedListComparator());
                sorted.addAll((Set)values);
                if (sorted.size() == 1) {
                    String tmp = (String)sorted.iterator().next();
                    if (tmp.equals("[0]=")) {
                        sorted.clear();
                    }
                }
                List list = new ArrayList();
                
                for (Iterator i = sorted.iterator(); i.hasNext(); ) { 
                    String val = (String)i.next();
                    int idx = val.indexOf(']');
                    idx = val.indexOf('=', idx);
                    list.add(val.substring(idx+1).trim());
                }
                m.setOptionList(AMViewBeanBase.createOptionList(
                    list, amModel.getUserLocale(), false));
            }
            set = true;
        }
        return set;
    }
    
    private boolean setValuesToMapList(
        View view,
        String name,
        Object values,
        AMModel amModel,
        AMPropertySheetModel model
    ) {
        boolean set = false;
        if (CCMapList.class.isInstance(view)) {
            ((CCMapList)view).resetStateData();
            CCMapListModel m = (CCMapListModel)model.getModel(name);
            if (Set.class.isInstance(values)) {
                Set v = new HashSet();
                v.addAll((Set)values);
                v.remove("[]=");
                m.setOptionList(AMViewBeanBase.createOptionList(
                    v, amModel.getUserLocale(), false));
            }
            set = true;
        }
        return set;
    }
    
    private boolean setValuesToOrderedList(
        View view,
        String name,
        Object values,
        AMModel amModel,
        AMPropertySheetModel model
    ) {
        boolean set = false;
        if (CCOrderedList.class.isInstance(view)) {
            ((CCOrderedList)view).resetStateData();
            CCOrderedListModel m = (CCOrderedListModel)
                model.getModel(name);
            if (Set.class.isInstance(values)) {
                Set sorted = new TreeSet(new OrderedListComparator());
                sorted.addAll((Set)values);

                if (sorted.size() == 1) {
                    String tmp = (String)sorted.iterator().next();
                    if (tmp.equals("[0]=")) {
                        sorted.clear();
                    }
                }
                List list = new ArrayList();
                
                for (Iterator i = sorted.iterator(); i.hasNext(); ) { 
                    String val = (String)i.next();
                    int idx = val.indexOf(']');
                    idx = val.indexOf('=', idx);
                    list.add(val.substring(idx+1).trim());
                }
                m.setSelectedOptionList(
                    AMViewBeanBase.createOptionList(
                        list, amModel.getUserLocale(), false));
            }
            set = true;
        }
        return set;
    }

    
    private boolean setValuesToEditableList(
        View view,
        String name,
        Object values,
        AMModel amModel,
        AMPropertySheetModel model
    ) {
        boolean set = false;
        if (CCEditableList.class.isInstance(view) &&
            !CCUnOrderedList.class.isInstance(view) &&
            !CCMapList.class.isInstance(view)
        ) {
            ((CCEditableList)view).resetStateData();
            CCEditableListModel m = (CCEditableListModel)
                model.getModel(name);
            if (Set.class.isInstance(values)) {
                m.setOptionList(
                    AMViewBeanBase.createOptionList(
                        (Set)values, amModel.getUserLocale(), false));
            }
            set = true;
        }
        return set;
    }

    private boolean setValuesToAddRemove(
        View view,
        String name,
        Object values,
        AMModel amModel,
        AMPropertySheetModel model
    ) {
        boolean set = false;
        if (CCAddRemove.class.isInstance(view)) {
            CCAddRemoveModel m = (CCAddRemoveModel)model.getModel(name);
            if (Set.class.isInstance(values)) {
                Set selectedSet = (Set)values;
                OptionList possibleOptions = model.getAddRemoveAvailOptions(name);
                if (possibleOptions != null) {
                    OptionList availOptions = new OptionList();
                    
                    if ((selectedSet != null) && !selectedSet.isEmpty()) {
                        OptionList optList = new OptionList();
                        for (Iterator i = selectedSet.iterator(); i.hasNext();) {
                            String val = (String)i.next();
                            optList.add(possibleOptions.getValueLabel(val), val);
                        }
                        m.setSelectedOptionList(optList);
                    
                        for (int i = 0; i < possibleOptions.size(); i++) {
                            Option opt = possibleOptions.get(i);
                            if (!selectedSet.contains(opt.getValue())) { 
                                availOptions.add(opt);
                            }
                        }
                        m.setAvailableOptionList(availOptions);
                    } else {
                        m.setAvailableOptionList(possibleOptions);
                    }
                } else {
                    m.setSelectedOptionList(AMViewBeanBase.createOptionList(
                        selectedSet, amModel.getUserLocale()));
                }
            }
            set = true;
        }
        return set;
    }

    private boolean setValuesToSelect(
        View view,
        String name,
        Object values,
        AMModel amModel,
        AMPropertySheetModel model
    ) {
        boolean set = false;
        if (CCSelect.class.isInstance(view)) {
            ((CCSelect)view).resetStateData();
            // selectable list elements should pass a Map of choice
            // values and the selected values.
            if (Map.class.isInstance(values)) {
                Map tmp =(Map)values;
                Set choices = (Set)tmp.get(AMAdminConstants.CHOICES);
                values = (Set)tmp.get(AMAdminConstants.VALUES);
                CCSelect sl = (CCSelect)view;
                sl.setOptions(AMViewBeanBase.createOptionList(
                    choices, amModel.getUserLocale()));
                model.setValues(name, ((Set)values).toArray(), amModel);
            } else {
                model.setValues(name, ((Set)values).toArray(), amModel);
            }
            set = true;
        }
        return set;
    }

    /**
     * Returns a map of attribute name to values.
     *
     * @param orig Map of attribute to original values.
     * @param model <code>AMModel</code> object.
     * @return a map of attribute name to values.
     * @throws ModelControlException if cannot get model of property sheet.
     * @throws AMConsoleException if password and confirm password values do
     *         not match.
     */
    public Map getAttributeValues(Map orig, AMModel model)
        throws ModelControlException, AMConsoleException
    {
        return getAttributeValues(orig, true, true, model);
    }

    /**
     * Returns a map of attribute name to values which have different values 
     * from the original values.
     *
     * @param orig Map of attribute to original values.
     * @param modified true to return map of attribute name to values that
     *        have values that is different from values in <code>orig</code>.
     * @param model <code>AMModel</code> object.
     * @return a map of attribute name to values.
     * @throws ModelControlException if cannot get model of property sheet.
     * @throws AMConsoleException if password and confirm password values do
     *         not match.
     */
    public Map getAttributeValues(Map orig, boolean modified, AMModel model)
        throws ModelControlException, AMConsoleException {
        return getAttributeValues(orig, modified, true,  model);
    }

    /**
     * Returns a map of attribute name to values which have different values 
     * from the original values.
     *
     * @param orig Map of attribute to original values.
     * @param modified true to return map of attribute name to values that
     *        have values that is different from values in <code>orig</code>.
     * @param matchPwd true to match password with confirm password value.
     * @param amModel <code>AMModel</code> object.
     * @return a map of attribute name to values.
     * @throws ModelControlException if cannot get model of property sheet.
     * @throws AMConsoleException if password and confirm password values do
     *         not match.
     */
    public Map getAttributeValues(
        Map orig,
        boolean modified,
        boolean matchPwd,
        AMModel amModel
    ) throws ModelControlException, AMConsoleException
    {
        restoreStateData(orig.keySet());
        AMPropertySheetModel model = (AMPropertySheetModel)getModel();
        Map attrValues = new HashMap(orig.size() *2);
        Map multipleChoiceValues = getMultipleChoiceValues(model);

        for (Iterator iter = orig.keySet().iterator(); iter.hasNext();) {
            String name = (String)iter.next();

            if (model.isChildSupported(name)) {
                Object tmpValue = orig.get(name);
                Set origValue = null;

                // values will either be a Set or a Map depending on 
                // the element type used in the display
                if (Set.class.isInstance(tmpValue)) {
                    origValue = (Set)tmpValue;
                } else {
                    // assuming this child is a list
                    Map tmp = (Map)tmpValue;
                    origValue = (Set)tmp.get("values");
                }
                
                Object childModel = model.getModel(name);
                Set values = null;

                if (CCAddRemoveModel.class.isInstance(childModel)) {
                    values = getValues(
                        ((CCAddRemoveModel)childModel).getSelectedOptionList());
                } else if (CCEditableListModel.class.isInstance(childModel)) {
                    values = getValues(
                        ((CCEditableListModel)childModel).getOptionList());
                } else if (model.isChildSupported(
                    name + PropertyTemplate.PWD_CONFIRM_SUFFIX)
                ) {
                    String pwd = (String)model.getValue(name);
                    String confirmPwd = (String)model.getValue(
                        name + PropertyTemplate.PWD_CONFIRM_SUFFIX);
                    if (!matchPwd || pwd.equals(confirmPwd)) {
                        if (!pwd.equals(AMPropertySheetModel.passwordRandom)) {
                            values = new HashSet(2);
                            values.add(pwd);
                        } else {
                            values = (Set)orig.get(name);
                        }
                    } else {
                        throw new AMConsoleException("password-mismatched");
                    }
                } else if (model.isChildSupported(
                    PropertyTemplate.DATE_MARKER_NAME + name)
                ) {
                    String date = (String)model.getValue(name);
                    values = getDateInUserLocale(date, amModel);
                } else {
                    values = (Set)multipleChoiceValues.get(name);
                    if (values == null) {
                        values = AMAdminUtils.toSet(model.getValues(name));
                    }
                }

                if (!modified || !equalsSet(values, origValue)) {
                    attrValues.put(name, values);
                }
            }
        }

        return attrValues;
    }

    private Set getDateInUserLocale(String date, AMModel model)
        throws AMConsoleException
    {
        Set values = null;
        if ((date != null) && (date.trim().length() > 0)) {
            Date dt = com.sun.identity.shared.locale.Locale.parseDateString(
                date, model.getUserLocale());
            String dateString =
                com.sun.identity.shared.locale.Locale.getNormalizedDateString(
                    dt);
            if (dateString != null) {
                values = new HashSet(2);
                values.add(dateString);
            } else {
                String[] arg = {date};
                throw new AMConsoleException(MessageFormat.format(
                    model.getLocalizedString("invalid-date.message"), 
                    (Object[])arg));
            }
        }
        return (values == null) ? Collections.EMPTY_SET : values;
    }

    /*
     * Comparing two set. Collections.EMPTY is equals to set with no value.
     * and equals to set with one entry which is an empty string.
     */
    private boolean equalsSet(Set s1, Set s2) {
        return s1.equals(s2) ||
            (setComparableToCollectionEmptySet(s1) &&
            setComparableToCollectionEmptySet(s2));
    }

    private boolean setComparableToCollectionEmptySet(Set s) {
        boolean compatible = false;
        if (!s.isEmpty()) {
            if (s.size() == 1) {
                String str = (String)s.iterator().next();
                compatible = (str == null) || (str.trim().length() == 0);
            }
        } else {
            compatible = true;
        }
        return compatible;
    }

    /**
     * Returns a map of attribute name to values.
     *
     * @param attrNames Set of attribute names to retrieve.
     * @return map of attribute name to values.
     * @throws ModelControlException if cannot get model of property sheet.
     * @throws AMConsoleException if password and confirm password values do
     *         not match.
     */
    public Map getAttributeValues(Collection attrNames)
        throws ModelControlException, AMConsoleException
    {
        restoreStateData(attrNames);
        AMPropertySheetModel model = (AMPropertySheetModel)getModel();
        Map attrValues = new HashMap(attrNames.size() *2);
        Map multipleChoiceValues = getMultipleChoiceValues(model);

        for (Iterator iter = attrNames.iterator(); iter.hasNext();) {
            String name = (String)iter.next();
            Object childModel = model.getModel(name);
            Set values = null;

            if (CCAddRemoveModel.class.isInstance(childModel)) {
                values = getValues(
                    ((CCAddRemoveModel)childModel).getSelectedOptionList());
            } else if (CCUnOrderedListModel.class.isInstance(childModel)) {
                values = getListValues(
                    ((CCUnOrderedListModel)childModel).getOptionList());
                if ((values == null) || values.isEmpty()) {
                    values = new HashSet(2);
                    values.add("[0]=");
                }
            } else if (CCMapListModel.class.isInstance(childModel)) {
                values = getValues(
                    ((CCMapListModel)childModel).getOptionList());
                if ((values == null) || values.isEmpty()) {
                    values = new HashSet(2);
                    values.add("[]=");
                }
            } else if (CCEditableListModel.class.isInstance(childModel)) {
                values = getValues(
                    ((CCEditableListModel)childModel).getOptionList());
            } else if (CCOrderedListModel.class.isInstance(childModel)) {
                values = getListValues(
                    ((CCOrderedListModel)childModel).getSelectedOptionList());
                if ((values == null) || values.isEmpty()) {
                    values = new HashSet(2);
                    values.add("[0]=");
                }
            } else if (model.isChildSupported(
                name + PropertyTemplate.PWD_CONFIRM_SUFFIX)
            ) {
                String pwd = (String)model.getValue(name);
                String confirmPwd = (String)model.getValue(
                    name + PropertyTemplate.PWD_CONFIRM_SUFFIX);
                if (pwd.equals(confirmPwd)) {
                    if (!pwd.equals(AMPropertySheetModel.passwordRandom)) {
                        values = new HashSet(2);
                        values.add(pwd);
                    }
                } else {
                    throw new AMConsoleException("password-mismatched");
                }
            } else {
                values = (Set)multipleChoiceValues.get(name);
                if (values == null) {
                    values = AMAdminUtils.toSet(model.getValues(name));
                }
            }

            if (values != null) {
                attrValues.put(name, values);
            }
        }

        return attrValues;
    }

    public Map getMultipleChoiceValues(AMPropertySheetModel model) {
        Map values = new HashMap();
        Set childNames = model.getChildrenNames();
        int len =
            PropertyTemplate.DYN_GUI_MULTIPLE_LIST_CHECKBOX_PREFIX.length();
        int markerLen =
            PropertyTemplate.DYN_GUI_MULTIPLE_LIST_CHECKBOX_MARKER.length();

        for (Iterator iter = childNames.iterator(); iter.hasNext();) {
            View view = (View)iter.next();
            String name = view.getName();

            if (name.startsWith(
                PropertyTemplate.DYN_GUI_MULTIPLE_LIST_CHECKBOX_MARKER)
            ) {
                if (model.getValue(name).equals("true")) {
                    name = name.substring(markerLen);
                    Set val = (Set)values.get(name);
                    if (val == null) {
                        val = new HashSet();
                        values.put(name, val);
                    }
                }
            } else if (name.startsWith(
                PropertyTemplate.DYN_GUI_MULTIPLE_LIST_CHECKBOX_PREFIX)
            ) {
                if (model.getValue(name).equals("true")) {
                    name = name.substring(len);
                    int idx = name.lastIndexOf('_');
                    String value = unescapeSqBracket(name.substring(idx+1)); 
                    name = name.substring(0, idx);
                                                                                
                    Set val = (Set)values.get(name);
                    if (val == null) {
                        val = new HashSet();
                        values.put(name, val);
                    }
                    val.add(value);
                }
            }
        }
        return values;
    }


    private static Set getValues(OptionList optList) {
        OrderedSet values = null;

        if ((optList != null) && (optList.size() > 0)) {
            int sz = optList.size();
            values = new OrderedSet();

            for (int i = 0; i < sz; i++) {
                Option opt = optList.get(i);
                values.add(opt.getValue());
            }
        }

        return (values == null) ? Collections.EMPTY_SET : values;
    }
    
    private static Set getListValues(OptionList optList) {
        Set values = null;

        if ((optList != null) && (optList.size() > 0)) {
            int sz = optList.size();
            values = new HashSet(sz *2);

            for (int i = 0; i < sz; i++) {
                Option opt = optList.get(i);
                values.add("[" + i + "]=" + opt.getValue());
            }
        }

        return (values == null) ? Collections.EMPTY_SET : values;
    }

    /**
     * Automates the restoring of store data in some children. This is required
     * for some special children such as <code>CCAddRemvoe</code>.
     *
     * @throws ModelControlException if cannot get model of property sheet.
     */
    public void restoreStateData(Collection participatingChildren)
        throws ModelControlException 
    {
        ViewBean parent = getParentViewBean();
        String[] names = parent.getChildNames();

        if ((names != null) && (names.length > 0)) {
            for (int i = 0; i < names.length; i++) {
                String name = names[i];

                if (participatingChildren.contains(name)) {
                    View child = parent.getChild(name);

                    if (CCAddRemove.class.isInstance(child)) {
                        ((CCAddRemove)child).restoreStateData();
                    } else if (CCUnOrderedList.class.isInstance(child)) {
                        ((CCUnOrderedList)child).restoreStateData();
                    } else if (CCMapList.class.isInstance(child)) {
                        ((CCMapList)child).restoreStateData();
                    } else if (CCEditableList.class.isInstance(child)) {
                        ((CCEditableList)child).restoreStateData();
                    } else if (CCOrderedList.class.isInstance(child)) {
                        ((CCOrderedList)child).restoreStateData();
                    } else if (CCActionTable.class.isInstance(child)) {
                        ((CCActionTable)child).restoreStateData();
                    }
                }
            }
        }
    }

    public void init() {
        ViewBean parent = getParentViewBean();
        AMPropertySheetModel model = (AMPropertySheetModel)getModel();
        Map radioComponents = model.getRadioDefaultValues();
        if ((radioComponents != null) && !radioComponents.isEmpty()) {
            for (Iterator i = radioComponents.keySet().iterator(); i.hasNext();
            ) {
                String name = (String)i.next();
                CCRadioButton rb = (CCRadioButton)parent.getChild(name);
                Object value = rb.getValue();
                if (value == null) {
                    rb.setValue(radioComponents.get(name));
                }
            }
        }
    }

     private String escapeSqBracket(String str) {
         str = str.replaceAll("&", "&amp;");
         str = str.replaceAll("\\[", "&5B");
         str = str.replaceAll("\\]", "&5D");
         return str;
     }

     private String unescapeSqBracket(String str) {
         str = str.replaceAll("&5D", "]");
         str = str.replaceAll("&5B", "[");
         str = str.replaceAll("&amp;", "&");
         return str;
     }
}

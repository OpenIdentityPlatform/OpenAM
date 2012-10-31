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
 * $Id: ActionTiledView.java,v 1.2 2008/06/25 05:43:00 qcheng Exp $
 *
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.view.View;
import com.iplanet.jato.view.html.Option;
import com.iplanet.jato.view.html.OptionList;
import com.iplanet.jato.view.event.ChildDisplayEvent;
import com.sun.identity.console.base.AMTableTiledView;
import com.sun.identity.console.base.model.AMDisplayType;
import com.sun.identity.policy.ActionSchema;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCAddRemoveModel;
import com.sun.web.ui.model.CCEditableListModel;
import com.sun.web.ui.view.editablelist.CCEditableList;
import com.sun.web.ui.view.html.CCCheckBox;
import com.sun.web.ui.view.html.CCDropDownMenu;
import com.sun.web.ui.view.html.CCRadioButton;
import com.sun.web.ui.view.html.CCSelectableList;
import java.util.Set;

public class ActionTiledView
    extends AMTableTiledView {

    public ActionTiledView(View parent, CCActionTableModel model, String name) {
        super(parent, model, name);
        CCAddRemoveModel addRemoveModel = new CCAddRemoveModel();
        addRemoveModel.setShowMoveUpDownButtons("false");
        model.setModel(RuleOpViewBeanBase.TBL_ACTIONS_SELECTABLE_LIST,
            addRemoveModel);
    }

    public boolean beginChildDisplay(ChildDisplayEvent event) {
        super.endDisplay(event);
        boolean display = true;
        int rowIndex = model.getRowIndex();
                                                                                
        if (rowIndex < model.getNumRows()) {
            String childName = event.getChildName();
            
            ActionSchema actionSchema = getActionSchema(rowIndex);
            int type = AMDisplayType.getInstance().getDisplayType(actionSchema);
            int syntax = AMDisplayType.getInstance().getDisplaySyntax(
                actionSchema);

            if (childName.indexOf(RuleOpViewBeanBase.TBL_ACTIONS_RADIO_VALUE)
                != -1) {
                display = displayRadioAction(
                    actionSchema, childName, type, syntax);
            } else if (childName.indexOf(
                RuleOpViewBeanBase.TBL_ACTIONS_CHECKBOX_VALUE) != -1) {
                display = displayBooleanAction(
                    actionSchema, childName, type, syntax);
            } else if (childName.indexOf(
                RuleOpViewBeanBase.TBL_ACTIONS_PASSWORD_VALUE) != -1) {
                display = displayPasswordAction(
                    actionSchema, childName, type, syntax);
            } else if (childName.indexOf(
                RuleOpViewBeanBase.TBL_ACTIONS_TEXTAREA_VALUE) != -1) {
                display = displayTextAreaAction(
                    actionSchema, childName, type, syntax);
            } else if (childName.indexOf(
                RuleOpViewBeanBase.TBL_ACTIONS_TEXT_VALUE) != -1) {
                display = displayTextFieldAction(
                    actionSchema, childName, type, syntax);
            } else if (childName.indexOf(
                RuleOpViewBeanBase.TBL_ACTIONS_DROPDOWN_MENU) != -1) {
                display = displaySingleChoiceAction(
                    actionSchema, childName, type, syntax);
            } else if (childName.indexOf(
                RuleOpViewBeanBase.TBL_ACTIONS_SELECTABLE_LIST) != -1) {
                display = displayMultipleChoiceAction(
                    actionSchema, childName, type, syntax);
            } else if (childName.indexOf(
                RuleOpViewBeanBase.TBL_ACTIONS_EDITABLE_LIST) != -1) {
                display = displayEditableListAction(
                    actionSchema, childName, type, syntax);
            }
        }
                                                                                
        return display;
    }

    private ActionSchema getActionSchema(int i) {
        RuleOpViewBeanBase parentVB = (RuleOpViewBeanBase)getParentViewBean();
        return parentVB.getActionSchema(i);
    }

    private boolean displayRadioAction(
        ActionSchema actionSchema,
        String childName,
        int type,
        int syntax
    ) {
        boolean display = false;
        RuleOpViewBeanBase parentVB = (RuleOpViewBeanBase)getParentViewBean();
        String serviceType = (String)parentVB.propertySheetModel.getValue(
            RuleOpViewBeanBase.SERVICE_TYPE);

        if ((type == AMDisplayType.TYPE_SINGLE) &&
            (syntax == AMDisplayType.SYNTAX_RADIO)
        ) {
            CCRadioButton rb = (CCRadioButton)getChild(childName);
            OptionList optList = parentVB.getChoiceValues(
                serviceType, actionSchema);
            rb.setOptions(optList);

            if ((optList != null) && (optList.size() > 0)) {
                Set set = parentVB.isSubmitCycle() ?
                    parentVB.getCurrentActionValues(actionSchema) :
                    parentVB.getDefaultActionValues(actionSchema);
                String value = ((set == null) || set.isEmpty()) ?
                    optList.getValue(0) : (String)set.iterator().next();
                rb.setValue(value);
                display = true;
            }
        }

        return display;
    }

    private boolean displaySingleChoiceAction(
        ActionSchema actionSchema,
        String childName,
        int type,
        int syntax
    ) {
        boolean display = false;
        RuleOpViewBeanBase parentVB = (RuleOpViewBeanBase)getParentViewBean();
        String serviceType = (String)parentVB.propertySheetModel.getValue(
            RuleOpViewBeanBase.SERVICE_TYPE);

        if (type == AMDisplayType.TYPE_SINGLE_CHOICE) {
            CCDropDownMenu list = (CCDropDownMenu)getChild(childName);
            OptionList optList = parentVB.getChoiceValues(
                serviceType, actionSchema);
            list.setOptions(optList);

            if ((optList != null) && (optList.size() > 0)) {
                if (!parentVB.isSubmitCycle()) {
                    Set set = parentVB.getDefaultActionValues(actionSchema);
                    String value = ((set == null) || set.isEmpty()) ?
                        optList.getValue(0) : (String)set.iterator().next();
                    list.setValue(value);
                }
                display = true;
            }
        }

        return display;
    }

    private boolean displayEditableListAction(
        ActionSchema actionSchema,
        String childName,
        int type,
        int syntax
    ) {
        boolean display = (type == AMDisplayType.TYPE_LIST);
        if (display) {
            CCEditableList child = (CCEditableList)getChild(childName);
            child.resetStateData();
            CCEditableListModel m = (CCEditableListModel)
                model.getModel(childName);
            RuleOpViewBeanBase parentVB =
                (RuleOpViewBeanBase)getParentViewBean();
            Set defaultValues = parentVB.getValues(m.getOptionList());
                
            if ((defaultValues == null) || defaultValues.isEmpty()) {
                defaultValues = parentVB.getDefaultActionValues(actionSchema);
                m.setOptionList(parentVB.createOptionList(defaultValues));
            }
        }
        return display;
    }

    private boolean displayMultipleChoiceAction(
        ActionSchema actionSchema,
        String childName,
        int type,
        int syntax
    ) {
        boolean display = false;
        RuleOpViewBeanBase parentVB = (RuleOpViewBeanBase)getParentViewBean();
        String serviceType = (String)parentVB.propertySheetModel.getValue(
            RuleOpViewBeanBase.SERVICE_TYPE);

        if (type == AMDisplayType.TYPE_MULTIPLE_CHOICE) {
            CCSelectableList child = (CCSelectableList)getChild(childName);
            child.resetStateData();
            CCAddRemoveModel addRemoveModel = (CCAddRemoveModel)model.getModel(
                childName);
            Set defaultValues = parentVB.getValues(
                addRemoveModel.getSelectedOptionList());

            if ((defaultValues == null) || defaultValues.isEmpty()) {
                defaultValues = parentVB.getDefaultActionValues(actionSchema);
            }

            OptionList optList = parentVB.getChoiceValues(
                serviceType, actionSchema);
            int sz = optList.size();
            OptionList availList = new OptionList();

            for (int i = 0; i < sz; i++) {
                Option opt = (Option)optList.get(i);
                if (!defaultValues.contains(opt.getValue())) {
                    availList.add(opt);
                }
            }

            addRemoveModel.setAvailableOptionList(availList);
            addRemoveModel.setSelectedOptionList(parentVB.createOptionList(
                defaultValues));
            display = true;
        }

        return display;
    }

    private boolean displayTextFieldAction(
        ActionSchema actionSchema,
        String childName,
        int type,
        int syntax
    ) {
        boolean display = (type == AMDisplayType.TYPE_SINGLE) &&
            (syntax == AMDisplayType.SYNTAX_TEXTFIELD);
        if (display) {
            setDisplayDefaultValue(actionSchema, childName);
        }
        return display;
    }

    private boolean displayBooleanAction(
        ActionSchema actionSchema,
        String childName,
        int type,
        int syntax
    ) {
        boolean display = (type == AMDisplayType.TYPE_SINGLE) &&
            (syntax == AMDisplayType.SYNTAX_BOOLEAN);

        if (display) {
            RuleOpViewBeanBase parentVB = (RuleOpViewBeanBase)
                getParentViewBean();
            Set set = (!parentVB.isSubmitCycle()) ?
                parentVB.getCurrentActionValues(actionSchema) :
                parentVB.getDefaultActionValues(actionSchema);
            String value = ((set != null) && !set.isEmpty()) ?
                (String)set.iterator().next() : "false";
            CCCheckBox cb = (CCCheckBox)getChild(childName);
            cb.setValue(value.equals("true") ? "true" : "false");
        }

        return display;
    }

    private boolean displayPasswordAction(
        ActionSchema actionSchema,
        String childName,
        int type,
        int syntax
    ) {
        boolean display = (syntax == AMDisplayType.SYNTAX_PASSWORD) ||
            (syntax == AMDisplayType.SYNTAX_ENCRYPTED_PASSWORD);
        display = display && (type == AMDisplayType.TYPE_SINGLE);

        if (display) {
            RuleOpViewBeanBase parentVB = (RuleOpViewBeanBase)
                getParentViewBean();
            setDisplayDefaultValue(actionSchema, childName);
        }
        return display;
    }

    private boolean displayTextAreaAction(
        ActionSchema actionSchema,
        String childName,
        int type,
        int syntax
    ) {
        boolean display = (type == AMDisplayType.TYPE_SINGLE) &&
            (syntax == AMDisplayType.SYNTAX_PARAGRAPH);

        if (display) {
            RuleOpViewBeanBase parentVB = (RuleOpViewBeanBase)
                getParentViewBean();
            setDisplayDefaultValue(actionSchema, childName);
        }
        return display;
    }

    private void setDisplayDefaultValue(
        ActionSchema actionSchema,
        String childName
    ) {
        RuleOpViewBeanBase parentVB = (RuleOpViewBeanBase)getParentViewBean();
        Set set = parentVB.getDefaultActionValues(actionSchema);
        if ((set != null) && !set.isEmpty()) {
            model.setValue(childName, (String)set.iterator().next());
        }
    }
}

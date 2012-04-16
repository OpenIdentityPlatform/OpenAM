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
 * $Id: SessionPropertyOpViewBeanBase.java,v 1.2 2008/06/25 05:43:06 qcheng Exp $
 *
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.policy.model.PolicyModelImpl;
import com.sun.identity.policy.plugins.SessionPropertyCondition;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public abstract class SessionPropertyOpViewBeanBase
    extends AMPrimaryMastHeadViewBean
{
    static final String CALL_VIEW_BEAN = "callingViewBean";

    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    static final String ATTR_NAME = "name";
    static final String ATTR_VALUES = "values";
    private static Set ATTRIBUTE_NAMES = new HashSet(4);

    static {
        ATTRIBUTE_NAMES.add(ATTR_NAME);
        ATTRIBUTE_NAMES.add(ATTR_VALUES);
    }

    protected CCPageTitleModel ptModel;
    private AMPropertySheetModel propertySheetModel;

    /**
     * Creates a realm operation base view bean.
     *
     * @param name Name of view
     */
    public SessionPropertyOpViewBeanBase(String name) {
        super(name);
        createPageTitleModel();
        createPropertyModel();
        registerChildren();
    }

    protected void registerChildren() {
        ptModel.registerChildren(this);
        registerChild(PGTITLE_TWO_BTNS, CCPageTitle.class);
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        propertySheetModel.registerChildren(this);
        super.registerChildren();
    }

    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/twoBtnsPageTitle.xml"));
        ptModel.setValue("button1",
            "policy.table.condition.session.property.button.create");
        ptModel.setValue("button2", "button.cancel");
    }

    private void createPropertyModel() {
        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/propertySessionPropertyEntry.xml"));
        propertySheetModel.clear();
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(PGTITLE_TWO_BTNS)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (name.equals(PROPERTY_ATTRIBUTE)) {
            view = new AMPropertySheet(this, propertySheetModel, name);
        } else if (propertySheetModel.isChildSupported(name)) {
            view = propertySheetModel.createChild(this, name, getModel());
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        return new PolicyModelImpl(req, getPageSessionAttributes());
    }

    protected void setValues(String name, Set values) {
        propertySheetModel.setValue(ATTR_NAME, name);
        propertySheetModel.setValues(ATTR_VALUES, values.toArray(), getModel());
    }

    protected Map getValues()
        throws ModelControlException, AMConsoleException
    {
        AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
        Map values = ps.getAttributeValues(ATTRIBUTE_NAMES);

        String name = null;
        Set setName = (Set)values.get(ATTR_NAME);
        if ((setName != null) && !setName.isEmpty()) {
            name = (String)setName.iterator().next();
            name = name.trim();
            if (name.length() == 0) {
                name = null;
            }
        }
        if (name == null) {
            throw new AMConsoleException(
                "policy.condition.missing.session.property.name.message");
        }
        if (name.equals(SessionPropertyCondition.VALUE_CASE_INSENSITIVE)) {
            throw new AMConsoleException(
                "policy.condition.session.property.reserved.name.message");
        }

        Set propertyValues = (Set)values.get(ATTR_VALUES);
        if ((propertyValues == null) || propertyValues.isEmpty()) {
            throw new AMConsoleException(
                "policy.condition.missing.session.property.message");
        }

        Map map = new HashMap(4);
        map.put(ATTR_NAME, name);
        map.put(ATTR_VALUES, propertyValues);
        return map;
    }

    protected void forwardToCallingViewBean() {
        String vbName = (String)getPageSessionAttribute(CALL_VIEW_BEAN);
        try {
            AMViewBeanBase vb = (AMViewBeanBase)getViewBean(
                Class.forName(vbName));
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (ClassNotFoundException e) {
            forwardTo();
        }
    }
}

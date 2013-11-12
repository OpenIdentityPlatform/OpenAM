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
 * $Id: RMRealmAddViewBean.java,v 1.2 2008/06/25 05:43:11 qcheng Exp $
 *
 */

package com.sun.identity.console.realm;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.html.Option;
import com.iplanet.jato.view.html.OptionList;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.realm.model.RMRealmModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCTextField;
import com.sun.web.ui.view.html.CCSelectableList;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.HashMap;

public class RMRealmAddViewBean
    extends RMRealmOpViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/realm/RMRealmAdd.jsp";

    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";

    private AMPropertySheetModel propertySheetModel;

    /**
     * Creates a realm creation view bean.
     */
    public RMRealmAddViewBean() {
        super("RMRealmAdd");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        createPageTitleModel();
        createPropertyModel();
        registerChildren();
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(PGTITLE_TWO_BTNS, CCPageTitle.class);
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        registerChild(RMRealmModel.TF_NAME, CCTextField.class);
        propertySheetModel.registerChildren(this);
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(PGTITLE_TWO_BTNS)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (name.equals(PROPERTY_ATTRIBUTE)) {
            view = new AMPropertySheet(this, propertySheetModel, name);
        } else if (propertySheetModel.isChildSupported(name)) {
            view = propertySheetModel.createChild(this, name, getModel());
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
        populateRealmParentList();
        setDefaultValues();
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/twoBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.ok");
        ptModel.setValue("button2", "button.cancel");
    }

    private void createPropertyModel() {
        RMRealmModel model = (RMRealmModel)getModel();
        try {
            propertySheetModel = new AMPropertySheetModel(
                model.getCreateRealmPropertyXML());
            propertySheetModel.clear();
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }

    private void populateRealmParentList() {
        RMRealmModel model = (RMRealmModel)getModel();
        Set realms = new HashSet();
        try {
            realms = model.getRealmNames(model.getStartDN(), "*");
        } catch (AMConsoleException a) {
            // do something here
        }
        Map display = new HashMap(realms.size() * 2);
        for (Iterator i=realms.iterator(); i.hasNext();) {
            String tmp = (String)i.next();
            String path = getPath(tmp);
            display.put(tmp, path);
        }

        OptionList optionList = createOptionList(display);
        CCSelectableList parentList = 
            (CCSelectableList)getChild(RMRealmModel.TF_PARENT);
        parentList.setOptions(optionList);

        String value = (String)parentList.getValue();
        if ((value == null) || (value.length() == 0)) {
            Option opt = optionList.get(0);
            parentList.setValue(opt.getValue());
        }
    }

    private void setDefaultValues() {
        RMRealmModel model = (RMRealmModel)getModel();
        Map defaultValues = model.getDefaultValues();
        AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
        ps.setAttributeValues(defaultValues, model);
    }

    /**
     * Handles cancel request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        backTrail();
        RMRealmViewBean vb = (RMRealmViewBean)getViewBean(
            RMRealmViewBean.class);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Handles create realm request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        RMRealmModel model = (RMRealmModel)getModel();
        AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
        String realmName = (String)getDisplayFieldValue(RMRealmModel.TF_NAME);
        realmName = realmName.trim();

        String parent = (String)getDisplayFieldValue(RMRealmModel.TF_PARENT);
        if (parent != null) {
            parent = parent.trim();
        } else {
            parent = model.getStartDN();
        }

        if (realmName.length() > 0) {
            try {
                Map values = ps.getAttributeValues(
                    model.getDataMap(), false, model);
                model.createSubRealm(parent, realmName, values);
                backTrail();
                RMRealmViewBean vb = (RMRealmViewBean)
                    getViewBean(RMRealmViewBean.class);
                passPgSessionMap(vb);
                vb.forwardTo(getRequestContext());
            } catch (AMConsoleException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
                forwardTo();
            }
        } else {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                "realm.missing.realmName");
            forwardTo();
        }
    }

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.addrealm";
    }

    protected boolean startPageTrail() {
        return false;
    }
}

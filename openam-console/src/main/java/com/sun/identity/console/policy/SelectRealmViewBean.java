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
 * $Id: SelectRealmViewBean.java,v 1.2 2008/06/25 05:43:05 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.policy;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.Button;
import com.iplanet.jato.view.html.OptionList;
import com.iplanet.jato.model.ModelControlException;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.policy.model.SelectRealmModel;
import com.sun.identity.console.policy.model.SelectRealmModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCSelect;
import com.sun.web.ui.view.html.CCStaticTextField;
import com.sun.web.ui.view.masthead.CCSecondaryMasthead;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/**
 * This appears in a popup window, user can search for realms; select one of 
 * of the realm; and the realm will be populated to the REALM attribute value
 * in the opener window.
 */
public class SelectRealmViewBean
    extends AMViewBeanBase
{
    /**
     * Default Display URL.
     */
    public static final String DEFAULT_DISPLAY_URL =
            "/console/policy/SelectRealm.jsp";
        
    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    private static final String ATTR_FILTER = "tfSearch";
    private static final String ATTR_REALM_NAMES = "RealmNames";
    private static final String ATTR_SCHEMES = "Schemes";
    private static final String SEC_MH_COMMON = "secMhCommon";
    private static final String BTN_REALM = "btnRealm";

    private static final String TXT_EMPTY_REALM = "emptyRealm";

    private CCPageTitleModel ptModel;
    private AMPropertySheetModel propertySheetModel;
    private boolean bRealmSelect;

    /**
     * Creates an instance of the view bean.
     */
    public SelectRealmViewBean() {
        super("SelectRealm");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        createPageTitleModel();
        createPropertyModel();
        registerChildren();
    }

    protected void registerChildren() {
        ptModel.registerChildren(this);
        propertySheetModel.registerChildren(this);
        registerChild(PGTITLE_TWO_BTNS, CCPageTitle.class);
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        registerChild(SEC_MH_COMMON, CCSecondaryMasthead.class);
        registerChild(BTN_REALM, Button.class);
        registerChild(TXT_EMPTY_REALM, CCStaticTextField.class);
        super.registerChildren();
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(PGTITLE_TWO_BTNS)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (name.equals(BTN_REALM)) {
            view = new Button(this, name, "");
        } else if (name.equals(PROPERTY_ATTRIBUTE)) {
            view = new AMPropertySheet(this, propertySheetModel, name);
        } else if (propertySheetModel.isChildSupported(name)) {
            view = propertySheetModel.createChild(this, name, getModel());
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else if (name.equals(SEC_MH_COMMON)) {
            view = new CCSecondaryMasthead(this, name);
        } else if (name.equals(TXT_EMPTY_REALM)) {
            view = new CCStaticTextField(this, name, "");
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
                getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/pageTitleRealmSelect.xml"));
    }

    private void createPropertyModel() {
        propertySheetModel = new AMPropertySheetModel(
                getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/propertyRMSelectRealm.xml"));
        propertySheetModel.clear();
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
                RequestManager.getRequestContext().getRequest();
        return new SelectRealmModelImpl(req, getPageSessionAttributes());
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
        disableButton("button1", true);
        setDisplayFieldValue(TXT_EMPTY_REALM, getModel().getLocalizedString(
            "policy.condition.null.realm"));
        String filter = (String)getDisplayFieldValue(ATTR_FILTER);

        if ((filter == null) || (filter.trim().length() == 0)) {
            setDisplayFieldValue(AuthToRealmHelper.ATTR_FILTER, "*");
            filter = "*";
        }

        Set realmNames = getRealmNames(filter);

        if ((realmNames != null) && !realmNames.isEmpty()) {
            CCSelect sl = (CCSelect)getChild(ATTR_REALM_NAMES);
            sl.setOptions(createOptionList(getLabelValueMap(realmNames)));
            String realm = null;

            if (!bRealmSelect) {
                realm = "";
                sl.setValue(realm);
            } else {
                realm = (String)sl.getValue();
            }

            setSchemes(realm);
        } else {
            CCSelect sl = (CCSelect)getChild(ATTR_SCHEMES);
            sl.setOptions(new OptionList());
        }
    }

    private Map getLabelValueMap(Set values) {
        Map map = new HashMap(values.size() *2);
        for (Iterator iter = values.iterator(); iter.hasNext(); ) {
            String val = (String)iter.next();
            if (val.length() == 0) {
                map.put("", getModel().getLocalizedString(
                    "policy.condition.null.realm"));
            } else {
                map.put(val, getPath(val));
            }
        }
        return map;
    }

    private Set getRealmNames(String filter) {
        Set realmNames = null;
        SelectRealmModel model = (SelectRealmModel)getModel();

        try {
            realmNames = model.getRealmNames("/", filter);
            if (realmNames.isEmpty()) {
                if (!isInlineAlertMessageSet()) {
                    setInlineAlertMessage(CCAlert.TYPE_INFO,
                            "message.information",
                        "policy.condition.selectrealm.no.search.result.message"
                            );
                }
            } else {
                disableButton("button1", false);
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
        }

        if ((realmNames == null) || realmNames.isEmpty()) {
            realmNames = new HashSet(1);
        }

        realmNames.add("");
        return realmNames;
    }

    private void setSchemes(String realm) {
        if ((realm == null) || (realm.length() == 0)) {
            realm = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
        }
        SelectRealmModel model = (SelectRealmModel)getModel();
        try {
            Set schemes = model.getAuthenticationInstances(realm);
            CCSelect sl = (CCSelect)getChild(ATTR_SCHEMES);
            sl.setOptions(createOptionList(schemes));
        } catch (AMConsoleException e) {
            if (!isInlineAlertMessageSet()) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                        e.getMessage());
            }
        }
    }

    /**
     * Refreshes the view so that search of realm with a new filter can 
     * happens.
     *
     * @param event Request Invocation Event.
     */
    public void handleBtnSearchRequest(RequestInvocationEvent event) {
        forwardTo();
    }


    /**
     * Refreshes the view so that search for services can be done.
     *
     * @param event Request Invocation Event.
     */
    public void handleBtnRealmRequest(RequestInvocationEvent event) {
        bRealmSelect = true;
        forwardTo();
    }
}

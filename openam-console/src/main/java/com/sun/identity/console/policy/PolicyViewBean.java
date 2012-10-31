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
 * $Id: PolicyViewBean.java,v 1.4 2009/10/08 21:56:11 asyhuang Exp $
 *
 */

/*
 * Portions Copyrighted 2012 ForgeRock Inc 
 * Portions Copyrighted 2012 Open Source Solution Technology Corporation
 */
package com.sun.identity.console.policy;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.util.HtmlUtil;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.components.view.html.SerializedField;
import com.sun.identity.console.realm.HasEntitiesTabs;
import com.sun.identity.console.realm.RealmPropertiesBase;
import com.sun.identity.console.policy.model.CachedPolicy;
import com.sun.identity.console.policy.model.PolicyModel;
import com.sun.identity.console.policy.model.PolicyModelImpl;
import com.sun.identity.policy.Policy;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCButton;
import com.sun.web.ui.view.html.CCTextField;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.table.CCActionTable;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCPageTitleModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public class PolicyViewBean
    extends RealmPropertiesBase
    implements HasEntitiesTabs
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/policy/Policy.jsp";

    private static final String TF_FILTER = "tfFilter";
    private static final String BTN_SEARCH = "btnSearch";

    private static final String TBL_SEARCH = "tblSearch";
    private static final String TBL_BUTTON_ADD = "tblButtonAddNormal";
    private static final String TBL_BUTTON_ADD_REFERRAL =
        "tblButtonAddReferral";
    private static final String TBL_BUTTON_DELETE = "tblButtonDelete";

    private static final String TBL_COL_NAME = "tblColName";
    private static final String TBL_DATA_NAME = "tblDataName";
    private static final String TBL_COL_RESOURCES = "tblColResources";
    private static final String TBL_DATA_RESOURCES = "tblDataResources";
    private static final String TBL_COL_ACTIVE = "tblColActive";
    private static final String TBL_DATA_ACTIVE = "tblDataActive";

    private static final String TBL_DATA_ACTION_HREF = "tblDataActionHref";

    private static final String PAGETITLE = "pgtitle";

    private CCActionTableModel tblModel = null;

    /**
     * Creates a policy view bean.
     */
    public PolicyViewBean() {
        super("Policy");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    protected void initialize() {
        if (!initialized) {
            super.initialize();
            createTableModel();
            createPageTitleModel();
            registerChildren();
            initialized = true;
        }
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(TF_FILTER, CCTextField.class);
        registerChild(BTN_SEARCH, CCButton.class);
        registerChild(PAGETITLE, CCPageTitle.class);
        registerChild(TBL_SEARCH, CCActionTable.class);
        ptModel.registerChildren(this);
        tblModel.registerChildren(this);
    }

    protected View createChild(String name) {
        View view = null;

        try {
            if (name.equals(TBL_SEARCH)) {
                SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
                populateTableModel((List)szCache.getSerializedObj());
                view = new CCActionTable(this, tblModel, name);
            } else if (name.equals(PAGETITLE)) {
                view = new CCPageTitle(this, ptModel, name);
            } else if (tblModel.isChildSupported(name)) {
                view = tblModel.createChild(this, name);
            } else if (ptModel.isChildSupported(name)) {
                view = ptModel.createChild(this,name);
            } else {
                view = super.createChild(name);
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
        return view;
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
        resetButtonState(TBL_BUTTON_DELETE);
        getPolicyNames();

        if (!isInlineAlertMessageSet()) {
            String curRealm = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            PolicyModel model = (PolicyModel)getModel();
            String message = model.canCreatePolicy(curRealm);

            if (message != null) {
                CCButton child = (CCButton)getChild(TBL_BUTTON_ADD);
                child.setDisabled(true);
                child = (CCButton)getChild(TBL_BUTTON_ADD_REFERRAL);
                child.setDisabled(true);
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    message);
            }
        }
        setPageTitle(getModel(), "page.title.policy");
    }

    protected AMModel getModelInternal() {
        RequestContext rc = RequestManager.getRequestContext();
        HttpServletRequest req = rc.getRequest();
        return new PolicyModelImpl(req, getPageSessionAttributes());
    }

    private void createTableModel() {
        tblModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/tblPMPolicy.xml"));
        tblModel.setTitleLabel("label.items");
        tblModel.setActionValue(
            TBL_BUTTON_ADD, "table.policy.normal.button.new");
        tblModel.setActionValue(
            TBL_BUTTON_ADD_REFERRAL, "table.policy.referral.button.new");
        tblModel.setActionValue(TBL_BUTTON_DELETE,
            "table.policy.button.delete");
        tblModel.setActionValue(TBL_COL_NAME, "table.policy.column.name");
        tblModel.setActionValue(TBL_COL_RESOURCES,
            "table.policy.column.resources");
        tblModel.setActionValue(TBL_COL_ACTIVE,
            "table.policy.column.active");
    }

    private void getPolicyNames() {
        PolicyModel model = (PolicyModel)getModel();
        String filter = ((String)getDisplayFieldValue(TF_FILTER));

        if ((filter == null) || (filter.length() == 0)) {
            filter = "*";
            setDisplayFieldValue(TF_FILTER, "*");
        } else {
            filter = filter.trim();
        }

        try {
            String curRealm = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            populateTableModel(model.getPolicyNames(curRealm, filter));
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }

    private void populateTableModel(Collection policyNames)
        throws AMConsoleException 
    {
        tblModel.clearAll();
        PolicyModel model = (PolicyModel)getModel();
        tblModel.setMaxRows(model.getPageSize());

        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);

        if ((policyNames != null) && !policyNames.isEmpty()) {
            List list = new ArrayList(policyNames.size());
            boolean firstEntry = true;

            for (Iterator iter = policyNames.iterator(); iter.hasNext(); ) {
                if (firstEntry) {
                    firstEntry = false;
                } else {
                    tblModel.appendRow();
                }

                String name = (String)iter.next();
                tblModel.setValue(TBL_DATA_NAME, name);
                tblModel.setValue(TBL_DATA_ACTION_HREF, stringToHex(name));
                list.add(name);
                
                String realm = (String)getPageSessionAttribute(
                    AMAdminConstants.CURRENT_REALM);
                Set resourceNames =
                    model.getProtectedResourceNames(realm, name);
                boolean isActive = model.isPolicyActive(realm, name);
                if (resourceNames.isEmpty()) {
                    tblModel.setValue(TBL_DATA_RESOURCES,
                        model.getLocalizedString(
                        "policy.resources.empty.message"));
                    tblModel.setValue(TBL_DATA_ACTIVE,
                            model.getLocalizedString(
                        "policy.resources.empty.message"));
                } else {
                    StringBuilder sbResources = new StringBuilder();
                    boolean first = true;
                    for(Iterator i=resourceNames.iterator(); i.hasNext();) {
                        String resourceName = (String)i.next();
                        if (first) {
                            first = false;
                        } else {
                            sbResources.append("<br>");
                        }
                        sbResources.append(HtmlUtil.escape(resourceName));
                    }
                    tblModel.setValue(TBL_DATA_RESOURCES,
                        sbResources.toString());
                    tblModel.setValue(TBL_DATA_ACTIVE,
                        (isActive) ? model.getLocalizedString(
                        "policy.resources.active.true") :
                            model.getLocalizedString(
                        "policy.resources.active.false"));
                }
            }
            szCache.setValue((ArrayList)list);
        } else {
            szCache.setValue(null);
        }
    }
    
    /**
     * Handles search request.
     *
     * @param event Request Invocation Event.
     */
    public void handleBtnSearchRequest(RequestInvocationEvent event) {
        forwardTo();
    }

    /**
     * Forwards request to creation view bean.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblButtonAddNormalRequest(RequestInvocationEvent event) {
        PolicyNormalAddViewBean vb = (PolicyNormalAddViewBean)getViewBean(
            PolicyNormalAddViewBean.class);

        try {
            PolicyModel model = (PolicyModel)getModel();
            String id = model.cachePolicy(
                model.getLocalizedString("policy.create.name"), "", false,
                    true);
            unlockPageTrail();
            setPageSessionAttribute(
                PolicyOpViewBeanBase.PG_SESSION_POLICY_CACHE_ID, id);
        } catch (AMConsoleException e) {
            debug.error("error with forwarding to normal page");
        }

        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Forwards request to creation view bean.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblButtonAddReferralRequest(
        RequestInvocationEvent event) 
    {
        PolicyReferralAddViewBean vb = (PolicyReferralAddViewBean)getViewBean(
            PolicyReferralAddViewBean.class);

        try {
            PolicyModel model = (PolicyModel)getModel();
            String id = model.cachePolicy(
                model.getLocalizedString("policy.create.name"), "", true, true);
            unlockPageTrail();
            setPageSessionAttribute(
                PolicyOpViewBeanBase.PG_SESSION_POLICY_CACHE_ID, id);
        } catch (AMConsoleException e) {
            debug.error("error with forwarding to referral page");
        }

        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }
    
    /**
     * Forwards request to edit policy view bean.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblDataActionHrefRequest(RequestInvocationEvent event) {
        PolicyModel model = (PolicyModel)getModel();
        String policyName = hexToString((String)getDisplayFieldValue(
            TBL_DATA_ACTION_HREF));
        String curRealm = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        try {
            String id = model.cachePolicy(curRealm, policyName);
            setPageSessionAttribute(
                PolicyOpViewBeanBase.PG_SESSION_POLICY_CACHE_ID, id);
            CachedPolicy cachedPolicy = model.getCachedPolicy(id);
            Policy policy = cachedPolicy.getPolicy();
            PolicyOpViewBeanBase vb = (policy.isReferralPolicy())
                ? (PolicyOpViewBeanBase)getViewBean(
                    PolicyReferralEditViewBean.class)
                : (PolicyOpViewBeanBase)getViewBean(
                    PolicyNormalEditViewBean.class);
            unlockPageTrail();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
            forwardTo();
        }
    }

    /**
     * Deletes policies.
     *
     * @param event Request Invocation Event.
     * @throws ModelControlException if table model cannot be restored.
     */
    public void handleTblButtonDeleteRequest(RequestInvocationEvent event)
        throws ModelControlException
    {
        CCActionTable table = (CCActionTable)getChild(TBL_SEARCH);
        table.restoreStateData();

        Integer[] selected = tblModel.getSelectedRows();
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
        List cache = (List)szCache.getSerializedObj();
        Set names = new HashSet(selected.length *2);

        for (int i = 0; i < selected.length; i++) {
            names.add((String)cache.get(selected[i].intValue()));
        }

        try {
            PolicyModel model = (PolicyModel)getModel();
            String curRealm = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            model.deletePolicies(curRealm, names);

            if (selected.length == 1) {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "policy.message.deleted");
            } else {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "policy.message.deleted.pural");
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }

        forwardTo();
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/oneBtnPageTitle.xml"));
        ptModel.setValue("button1", getBackButtonLabel());
    }

    /**
     * Handles reset request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event) {
        backTrail();
        forwardToRealmView(event);
    }
}

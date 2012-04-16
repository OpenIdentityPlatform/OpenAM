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
 * $Id: ResponseProviderOpViewBeanBase.java,v 1.2 2008/06/25 05:43:04 qcheng Exp $
 *
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.delegation.model.DelegationConfig;
import com.sun.identity.console.policy.model.CachedPolicy;
import com.sun.identity.console.policy.model.PolicyCache;
import com.sun.identity.console.policy.model.PolicyModel;
import com.sun.identity.policy.interfaces.ResponseProvider;
import com.sun.web.ui.model.CCAddRemoveModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public abstract class ResponseProviderOpViewBeanBase
    extends ProfileViewBeanBase
{
    protected static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    public static final String CALLING_VIEW_BEAN = 
        "ResponseProviderOpViewBeanBaseCallingVB";
    public static final String PG_SESSION_PROVIDER_NAME = "providerName";
    public static final String PG_SESSION_PROVIDER_TYPE = "providerType";

    protected static final String REALM_NAME = "tfRealmName";
    protected static final String RESPONSEPROVIDER_NAME =
        "tfResponseProviderName";
    protected static final String RESPONSEPROVIDER_TYPE =
        "responseProviderTypeName";
    protected static final String RESPONSEPROVIDER_TYPE_NAME =
        "tfResponseProviderTypeName";
    public static final String VALUES_TEXT_VALUE = "valuesTextValue";
    public static final String VALUES_SINGLE_CHOICE_VALUE =
        "valuesSingleChoiceValue";
    public static final String VALUES_MULTIPLE_CHOICE_VALUE =
        "valuesMultipleChoiceValue";

    protected CCPageTitleModel ptModel;
    private CCAddRemoveModel addRemoveModel;
    protected AMPropertySheetModel propertySheetModel;
    protected boolean canModify;
    protected boolean submitCycle;

    public ResponseProviderOpViewBeanBase(
        String name,
        String defaultDisplayURL
    ) {
        super(name, defaultDisplayURL);
    }

    protected void initialize() {
        if (!initialized) {
            initialized = createPropertyModel();

            if (initialized) {
                super.initialize();
                createPageTitleModel();
                createTableModel();
                registerChildren();
            }
        }
    }

    private boolean createPropertyModel() {
        boolean init = false;
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();

        String pType = (String)getPageSessionAttribute(
            PG_SESSION_PROVIDER_TYPE);
        if ((pType == null) || (pType.trim().length() == 0)) {
            pType = req.getParameter(getName() + "." + RESPONSEPROVIDER_TYPE);
            if ((pType != null) && (pType.trim().length() > 0)) {
                setPageSessionAttribute(PG_SESSION_PROVIDER_TYPE, pType);
            }
        }

        String curRealm = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        if ((curRealm == null) || (curRealm.trim().length() == 0)) {
            curRealm = req.getParameter(getName() + "." + REALM_NAME);

            if ((curRealm != null) && (curRealm.trim().length() > 0)) {
                setPageSessionAttribute(AMAdminConstants.CURRENT_REALM,
                    curRealm);
            }
        }

        init = (curRealm != null) && (curRealm.trim().length() > 0) &&
            (pType != null) && (pType.trim().length() > 0);

        DelegationConfig dConfig = DelegationConfig.getInstance();
        canModify = dConfig.hasPermission(curRealm, null,
            AMAdminConstants.PERMISSION_MODIFY, getModel(),
            getClass().getName());

        if (init) {
            propertySheetModel = new AMPropertySheetModel(
                getResponseProviderXML(curRealm, pType, !canModify));
            propertySheetModel.clear();

            if (canModify) {
                addRemoveModel = new CCAddRemoveModel();
                addRemoveModel.setShowMoveUpDownButtons("false");
                addRemoveModel.clear();
                propertySheetModel.setModel(
                    VALUES_MULTIPLE_CHOICE_VALUE, addRemoveModel);
            }
        }

        return init;
    }


    protected void registerChildren() {
        super.registerChildren();
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        ptModel.registerChildren(this);
        propertySheetModel.registerChildren(this);
    }

    protected View createChild(String name) {
        View view = null;

        if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else if (name.equals(PROPERTY_ATTRIBUTE)) {
            view = new AMPropertySheet(this, propertySheetModel, name);
        } else if (propertySheetModel.isChildSupported(name)) {
            view = propertySheetModel.createChild(this, name, getModel());
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    public AMPropertySheetModel getPropertySheetModel() {
        return propertySheetModel;
    }

    public void setErrorMessage(String msg) {
        setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error", msg);
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);

        if (!submitCycle) {
            PolicyModel model = (PolicyModel)getModel();
            String providerName = (String)getPageSessionAttribute(
                PG_SESSION_PROVIDER_NAME);
            String providerType = (String)getPageSessionAttribute(
                PG_SESSION_PROVIDER_TYPE);
            String realmName = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            propertySheetModel.setValue(RESPONSEPROVIDER_NAME, providerName);
            propertySheetModel.setValue(RESPONSEPROVIDER_TYPE, providerType);
            Map map = model.getActiveResponseProviderTypes(realmName);
            String i18nName = (String)map.get(providerType);
            propertySheetModel.setValue(RESPONSEPROVIDER_TYPE_NAME, i18nName);
            setPropertiesValues(getDefaultValues(), model);
        }
    }

    protected void setPropertiesValues(Map values, PolicyModel model) {
        if ((values != null) && !values.isEmpty()) {
            for (Iterator i = values.keySet().iterator(); i.hasNext(); ) {
                String propName = (String)i.next();
                Set val = (Set)values.get(propName);
                if ((val != null) && !val.isEmpty()) {
                    propertySheetModel.setValues(propName, val.toArray(),model);
                }
            }
        }
    }

    protected ResponseProvider createResponseProvider()
        throws ModelControlException, AMConsoleException {
        ResponseProvider provider = null;
        String providerType = (String)propertySheetModel.getValue(
            RESPONSEPROVIDER_TYPE);
        String providerName = getResponseProviderName();

        if (providerName != null) {
            Map values = getValues(providerType);
            if (values != null) {
                provider = createResponseProvider(providerType, values);
            }
        }

        return provider;
    }

    private ResponseProvider createResponseProvider(
        String providerType,
        Map values
    ) throws AMConsoleException {
        String realmName = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        PolicyModel model = (PolicyModel)getModel();
        return model.createResponseProvider(realmName, providerType, values);
    }

    private String getResponseProviderName() {
        String providerName = (String)propertySheetModel.getValue(
            RESPONSEPROVIDER_NAME);
        providerName = providerName.trim();
        if (providerName.length() == 0) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                "policy.missing.responseProviderName");
            providerName = null;
        }
        return providerName;
    }

    protected Map getValues(String providerType)
        throws ModelControlException, AMConsoleException {
        PolicyModel model = (PolicyModel)getModel();
        String realmName = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        Map values = getResponseProviderValues(model, realmName, providerType);

        if (values.isEmpty()) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                getMissingValuesMessage());
            values = null;
        }

        return values;
    }

    protected Map getResponseProviderValues(
        PolicyModel model,
        String realmName,
        String providerType
    ) throws ModelControlException, AMConsoleException {
        List propertyNames = model.getResponseProviderPropertyNames(
            realmName, providerType);
        AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
        return ps.getAttributeValues(propertyNames);
    }

    protected CachedPolicy getCachedPolicy()
        throws AMConsoleException
    {
        CachedPolicy policy = null;
        String cacheID = (String)getPageSessionAttribute(
            ProfileViewBeanBase.PG_SESSION_POLICY_CACHE_ID);
        if (cacheID != null) {
            PolicyCache cache = PolicyCache.getInstance();
            PolicyModel model = (PolicyModel)getModel();
            policy = model.getCachedPolicy(cacheID);
        }
        return policy;
    }

    protected void forwardToPolicyViewBean() {
        try {
            Class clazz = Class.forName(
                (String)removePageSessionAttribute(CALLING_VIEW_BEAN));
            PolicyOpViewBeanBase vb = (PolicyOpViewBeanBase)getViewBean(clazz);
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (ClassNotFoundException e) {
            forwardTo();
        }
    }

    /**
     * Handles cancel request.
     *
     * @param event Request invocation event
     */
    public void handleButton3Request(RequestInvocationEvent event) {
        backTrail();
        forwardToPolicyViewBean();
    }

    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException {
        SelectResponseProviderTypeViewBean vb =
            (SelectResponseProviderTypeViewBean)
            getViewBean(SelectResponseProviderTypeViewBean.class);
        unlockPageTrailForSwapping();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }


    protected String getResponseProviderXML(
        String curRealm,
        String providerType,
        boolean readonly
    ) {
        PolicyModel model = (PolicyModel)getModel();
        return model.getResponseProviderXML(
            curRealm, providerType, isCreateViewBean(), readonly);
    }

    protected String getMissingValuesMessage() {
        return "policy.missing.responseprovider.value";
    }

    protected void createTableModel() {
        // do nothing.
    }

    protected abstract void createPageTitleModel();
    protected abstract boolean hasValues();
    protected abstract Map getDefaultValues();
    protected abstract boolean isCreateViewBean();
}

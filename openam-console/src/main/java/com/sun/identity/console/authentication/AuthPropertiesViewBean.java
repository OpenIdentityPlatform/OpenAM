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
 * $Id: AuthPropertiesViewBean.java,v 1.5 2008/07/07 20:39:19 veiming Exp $
 *
 */

package com.sun.identity.console.authentication;


import com.iplanet.jato.RequestManager;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;

import com.sun.identity.authentication.config.AMAuthenticationInstance;
import com.sun.identity.console.authentication.model.AuthConfigurationModel;
import com.sun.identity.console.authentication.model.AuthConfigurationModelImpl;
import com.sun.identity.console.authentication.model.AuthPropertiesModel;
import com.sun.identity.console.authentication.model.AuthPropertiesModelImpl;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.delegation.model.DelegationConfig;
import com.sun.identity.console.realm.RealmPropertiesBase;
import com.sun.identity.shared.datastruct.OrderedSet;

import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCDropDownMenu;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.table.CCActionTable;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import com.iplanet.jato.NavigationException;

/**
 * This is the main authentication properties view page which displays the
 * authentication instances, authentication configurations, and a few of 
 * the core authentication properties (with a link to the full set of 
 * properties). From this view the user can create new auth instances, 
 * configurations, and basic property editing. The XML file used to build 
 * the layout is propertyRealmAuth.xml. tblAuthInstance.xml and 
 * tblAuthConfig.xml define the table structures for the instance and config
 * tables respectively.
 */
public  class AuthPropertiesViewBean
    extends RealmPropertiesBase
{
    public static final String DEFAULT_DISPLAY_URL = 
        "/console/authentication/AuthProperties.jsp";

    public static final String INSTANCE_MSG = "missingInstanceMessage";
    public static final String REALM_AUTH = "realm_authentication";

    protected static final String PROPERTY_ATTRIBUTE = "propertyAttributes";

    private boolean tablePopulated = false;
    private boolean initialized = false;
    private static final String PGTITLE_TWO_BTNS = "pageTitle";
    private AMPropertySheetModel propertySheetModel;

    // instance table properties
    private static final String AUTH_INSTANCE_TABLE = 
        "authenticationModuleInstanceTable";
    private static final String CREATE_INSTANCE_BUTTON = "createInstanceButton";
    private static final String DELETE_INSTANCE_BUTTON = "deleteInstanceButton";
    private static final String NAME_COLUMN_LABEL =  "nameColumnLabel";
    private static final String NAME_COLUMN_DATA = "nameColumnData";
    private static final String NAME_COLUMN_DATA_NO_HREF =
        "nameColumnDataNoHref";
    private static final String TYPE_COLUMN_LABEL = "typeColumnLabel";
    private static final String TYPE_COLUMN_DATA = "typeColumnData";
    private static final String ACTION_COLUMN_LABEL ="actionColumnLabel";
    private static final String ACTION_COLUMN_HREF = "actionColumnHREF";
    private static final String ACTION_COLUMN_HREF_LABEL = 
        "actionColumnHREFLabel";

    // configuration table properties
    private static final String AUTH_CONFIG_TABLE = 
        "authenticationConfigurationTable";
    private static final String CREATE_CONFIG_BUTTON = "createConfigButton";
    private static final String DELETE_CONFIG_BUTTON = "deleteConfigButton";
    private static final String CONFIG_NAME_COLUMN_LABEL =  
        "configNameColumnLabel";
    private static final String CONFIG_NAME_COLUMN_DATA = 
        "configNameColumnData";
    private static final String CONFIG_ACTION_COLUMN_LABEL =
        "configActionColumnLabel";
    private static final String CONFIG_ACTION_COLUMN_HREF = 
        "configActionColumnHREF";
    private static final String CONFIG_ACTION_COLUMN_HREF_LABEL = 
        "configActionColumnHREFLabel";

    private static final String AUTH_CONFIG = "iplanet-am-auth-org-config";
    private static final String ADMIN_AUTH_CONFIG = 
        "iplanet-am-auth-admin-auth-module";
    private static final String INSTANCES_REMOVED = "instancesRemoved";

    /**
     * Creates a authentication domains view bean.
     */
    public AuthPropertiesViewBean() {
        super("AuthProperties");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    protected void initialize()  {
        if (!initialized) {
            String realmName = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_PROFILE);

            if (realmName != null) {
                initialized = true;
                super.initialize();
                createPageTitleModel();
                createPropertyModel(realmName);
                createInstanceTable();
                createConfigurationTable();
                registerChildren();
            }
        }
    }

    protected void registerChildren() {
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        propertySheetModel.registerChildren(this);
        ptModel.registerChildren(this);
        super.registerChildren();
    }

    protected View createChild(String name) {
        if (!tablePopulated) {
            populateConfigTable();
            populateInstanceTable();
        }
        View view = null;

        if (name.equals(PROPERTY_ATTRIBUTE)) {
            view = new AMPropertySheet(this, propertySheetModel, name);
        } else if (propertySheetModel.isChildSupported(name)) {
            view = propertySheetModel.createChild(this, name, getModel());
        } else  if (name.equals(PGTITLE_TWO_BTNS)) {
            view =  new CCPageTitle(this, ptModel, name);
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else {
            view = super.createChild(name);
        }
        return view;
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
        resetButtonState(DELETE_CONFIG_BUTTON);
        resetButtonState(DELETE_INSTANCE_BUTTON);

        AuthPropertiesModel model = (AuthPropertiesModel)getModel();
        if (model != null) {
            AMPropertySheet ps = 
                (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
            try {
                ps.setAttributeValues(model.getValues(), model);
                populateConfigTable();
                populateInstanceTable();
                populateConfigMenu();
            } catch (AMConsoleException a) {
                setInlineAlertMessage(CCAlert.TYPE_WARNING, 
                    "message.warning", "noproperties.message");
            }
            setPageTitle(getModel(), "page.title.realms.authentication");
        }
        String msg = (String)removePageSessionAttribute(INSTANCE_MSG);
        if (msg != null) {
            setInlineAlertMessage(
                CCAlert.TYPE_WARNING, "message.warning", msg);
        }
    }

    private void populateConfigTable() {
        tablePopulated = true;
        CCActionTableModel tableModel = (CCActionTableModel)
            propertySheetModel.getModel(AUTH_CONFIG_TABLE);
        tableModel.clearAll();

        // get config names from previous pass. Need to use an ordered set
        // here to assure the same order is retained during the request cycle.
        OrderedSet configSet =
            (OrderedSet)removePageSessionAttribute(AUTH_CONFIG_TABLE);

        // no instances if this is the 1st pass, create it now
        if (configSet == null || configSet.isEmpty()) {
            AuthPropertiesModel model = (AuthPropertiesModel)getModel();
            String realm =  (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            if ((realm == null) || (realm.length() == 0)) {
                realm = AMModelBase.getStartDN(
                    getRequestContext().getRequest());
            }
            configSet = new OrderedSet();
            configSet.addAll(AuthConfigurationModelImpl.getNamedConfigurations(
                model.getUserSSOToken(), realm));
        }

        // add the data to the table
        boolean firstEntry = true;
        for (Iterator i=configSet.iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            
            if (!firstEntry) {
                tableModel.appendRow();
            } else {
                firstEntry = false;
            }
            tableModel.setValue(CONFIG_NAME_COLUMN_DATA, name);
            tableModel.setValue(CONFIG_ACTION_COLUMN_HREF, 
                stringToHex(name));
            tableModel.setValue(CONFIG_ACTION_COLUMN_HREF_LABEL,
                "authentication.module.instances.action.label");
        }

        // set the instances in the page session so when a request comes in
        // we can prepopulate the table model.
        setPageSessionAttribute(AUTH_CONFIG_TABLE, configSet);
    }

    private void populateConfigMenu() {
        String realm =  (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        if ((realm == null) || (realm.length() == 0)) {
            realm = AMModelBase.getStartDN(getRequestContext().getRequest());
        }
        
        AuthPropertiesModel model = (AuthPropertiesModel)getModel();
        OrderedSet configs = new OrderedSet();
        configs.addAll(AuthConfigurationModelImpl.getNamedConfigurations(
            model.getUserSSOToken(), realm));

        OptionList containers = new OptionList();
        for (Iterator i = configs.iterator(); i.hasNext();) {
            String entry = (String)i.next();
            containers.add(entry, entry);
        }
        CCDropDownMenu ac = (CCDropDownMenu)getChild(AUTH_CONFIG);
        ac.setOptions(containers);

        CCDropDownMenu aac = (CCDropDownMenu)getChild(ADMIN_AUTH_CONFIG);
        aac.setOptions(containers);
    }

    private void populateInstanceTable() {   
        tablePopulated = true;
        CCActionTableModel tableModel = (CCActionTableModel)
            propertySheetModel.getModel(AUTH_INSTANCE_TABLE);
        tableModel.clearAll();
        boolean firstEntry = true;

        Map instanceMap = new HashMap();
        AuthPropertiesModel model = (AuthPropertiesModelImpl)getModel();
        Set tmp = model.getAuthInstances();

        /* 
         * These instance were deleted in the previous request. This
         * is needed because the getAuthInstances call may return the 
         * instances that were just deleted.
         */
        Set removedInstances = 
            (Set)removePageSessionAttribute(INSTANCES_REMOVED);

        for (Iterator i=tmp.iterator(); i.hasNext();) {
            AMAuthenticationInstance inst = 
                (AMAuthenticationInstance)i.next();
            String name = inst.getName();
            if ((removedInstances == null) || 
                (!removedInstances.contains(name))) 
            {
                instanceMap.put(name,inst);
            }
        }

        /*
        * get instance names from previous pass. Need to use an ordered set
        * here to assure the same order is retained during the request cycle.
        */
        OrderedSet instanceSet = 
            (OrderedSet)removePageSessionAttribute(AUTH_INSTANCE_TABLE);

        // no instances if this is the 1st pass, create it now
        if (instanceSet == null) {
            instanceSet = new OrderedSet();
            instanceSet.addAll(instanceMap.keySet());
        }

        for (Iterator i=instanceSet.iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            AMAuthenticationInstance instance = 
                (AMAuthenticationInstance)instanceMap.get(name);

            /*
            * check if instance still exists. This can happen if user goes
            * the the advanced core properties page and removes an auth type
            * from the available auth instances list.
            */
            if (instance != null) {
                if (!firstEntry) {
                    tableModel.appendRow();
                } else {
                    firstEntry = false;
                }
                String type = instance.getType();

                if (model.hasAuthAttributes(type)) {
                    tableModel.setValue(NAME_COLUMN_DATA, name);
                    tableModel.setValue(ACTION_COLUMN_HREF, 
                        stringToHex(name));
                    tableModel.setValue(NAME_COLUMN_DATA_NO_HREF, "");
                } else {
                    tableModel.setValue(NAME_COLUMN_DATA, "");
                    tableModel.setValue(ACTION_COLUMN_HREF, 
                        stringToHex(name));
                    tableModel.setValue(NAME_COLUMN_DATA_NO_HREF, name);
                }

                tableModel.setValue(TYPE_COLUMN_DATA, type);
            }
        }

        /*
        * set the instances in the page session so when a request comes in 
        * we can prepopulate the table model. 
        */
        setPageSessionAttribute(AUTH_INSTANCE_TABLE, instanceSet);
    }

    private AMModel getConfigModel() {
        RequestContext rc = RequestManager.getRequestContext();
        HttpServletRequest req = rc.getRequest();
        return new AuthConfigurationModelImpl(req, getPageSessionAttributes());
    }

    protected AMModel getModelInternal() {
        RequestContext rc = RequestManager.getRequestContext();
        HttpServletRequest req = rc.getRequest();
        return new AuthPropertiesModelImpl(req, getPageSessionAttributes());
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", getBackButtonLabel());
    }

    private void createPropertyModel(String realmName) {
        DelegationConfig dConfig = DelegationConfig.getInstance();
        boolean canModify = dConfig.hasPermission(realmName, null,
            AMAdminConstants.PERMISSION_MODIFY, getModel(),
            getClass().getName());

        String xmlFile = (canModify) ?
            "com/sun/identity/console/propertyRealmAuth.xml" :
            "com/sun/identity/console/propertyRealmAuth_Readonly.xml";
        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(xmlFile));
        propertySheetModel.clear();
    }

    /*
    * Responsible for creating the model used for the authentication instances
    */
    private void createInstanceTable() {
        CCActionTableModel tableModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/tblAuthInstance.xml"));

        tableModel.setTitleLabel("label.items");
        tableModel.setActionValue(NAME_COLUMN_LABEL,
            "authentication.instance.table.name.column");
        tableModel.setActionValue(TYPE_COLUMN_LABEL,
            "authentication.instance.table.type.column");
        tableModel.setActionValue(ACTION_COLUMN_LABEL,
            "authentication.instance.table.action.column");
        tableModel.setActionValue(CREATE_INSTANCE_BUTTON,
            "authentication.instance.table.create.button");
        tableModel.setActionValue(DELETE_INSTANCE_BUTTON,
            "authentication.instance.table.delete.button");

        propertySheetModel.setModel(AUTH_INSTANCE_TABLE, tableModel);
    }

    /*
     * Responsible for creating the model used for the authentication instances
     * configurations.
     */
    private void createConfigurationTable() {
        CCActionTableModel tblModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/tblAuthConfiguration.xml"));

        tblModel.setTitleLabel("label.items");
        tblModel.setActionValue(CONFIG_NAME_COLUMN_LABEL,
            "authentication.configuration.table.name.column");
        tblModel.setActionValue(CONFIG_ACTION_COLUMN_LABEL,
            "authentication.configuration.table.action.column");
        tblModel.setActionValue(CREATE_CONFIG_BUTTON,
            "authentication.configuration.table.create.button");
        tblModel.setActionValue(DELETE_CONFIG_BUTTON,
            "authentication.configuration.table.delete.button");

        propertySheetModel.setModel(AUTH_CONFIG_TABLE, tblModel);
    }

    /*****************************************************************
    *
    * Event handlers
    *
    *****************************************************************/

    /**
     * Handles new authentication configuration request.
     *
     * @param event Request Invocation Event.
     */
    public void handleAdvancedOptionButtonRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        CoreAttributesViewBean vb = (CoreAttributesViewBean)
            getViewBean(CoreAttributesViewBean.class);
        unlockPageTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Handles new authentication configuration request.
     *
     * @param event Request Invocation Event.
     */
    public void handleCreateConfigButtonRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        NewAuthConfigViewBean vb = (NewAuthConfigViewBean)
            getViewBean(NewAuthConfigViewBean.class);
        removePageSessionAttribute(AUTH_CONFIG_TABLE); 
        unlockPageTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Handles the delete authentication instance request.
     *
     * @param event Request Invocation Event.
     */
    public void handleDeleteConfigButtonRequest(
        RequestInvocationEvent event
    ) throws ModelControlException
    {
        CCActionTable table = (CCActionTable)getChild(AUTH_CONFIG_TABLE);
        table.restoreStateData();

        CCActionTableModel tableModel = (CCActionTableModel)
            propertySheetModel.getModel(AUTH_CONFIG_TABLE);

        Integer[] selected = tableModel.getSelectedRows();
        Set configurations = new HashSet(selected.length *2);

        for (int i = 0; i < selected.length; i++) {
            tableModel.setRowIndex(selected[i].intValue());
            configurations.add(
                (String)tableModel.getValue(CONFIG_NAME_COLUMN_DATA));
        }

        try {
            AuthConfigurationModel m = (AuthConfigurationModel)getConfigModel();
            String curRealm = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            if ((curRealm == null) || (curRealm.length() == 0)) {
                curRealm = AMModelBase.getStartDN(
                    getRequestContext().getRequest());
            }
            m.deleteAuthConfiguration(curRealm, configurations);

            if (selected.length == 1) {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "authentication.config.deleted");
            } else {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "authentication.config.deleted.multiple");
            }
            removePageSessionAttribute(AUTH_CONFIG_TABLE);
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }

        forwardTo();
    }

    /**
     * Handles the new authentication instance request.
     *
     * @param event Request Invocation Event.
     */
    public void handleCreateInstanceButtonRequest(
        RequestInvocationEvent event
    ) {
        removePageSessionAttribute(AUTH_INSTANCE_TABLE);
        NewAuthInstanceViewBean vb = (NewAuthInstanceViewBean)
            getViewBean(NewAuthInstanceViewBean.class);
        unlockPageTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Handles the delete authentication instance request.
     *
     * @param event Request Invocation Event.
     */
    public void handleDeleteInstanceButtonRequest(
        RequestInvocationEvent event
    ) throws ModelControlException 
    {
        CCActionTable table = (CCActionTable)getChild(AUTH_INSTANCE_TABLE);
        table.restoreStateData();

        CCActionTableModel tableModel = (CCActionTableModel)
            propertySheetModel.getModel(AUTH_INSTANCE_TABLE);

        Integer[] selected = tableModel.getSelectedRows();
        Set instances = new HashSet(selected.length *2);

        for (int i = 0; i < selected.length; i++) {
            tableModel.setRowIndex(selected[i].intValue());
            instances.add((String)tableModel.getValue(NAME_COLUMN_DATA));
        }


        try {
            AuthPropertiesModel model = (AuthPropertiesModel)getModel();
            model.removeAuthInstance(instances);

            /* 
             * There is a timing issue with the backend after an 
             * instance is deleted causing the UI to be out of synch with the 
             * backend. We are storing the instances removed to be used when 
             * the page is redrawn to ensure the deleted instances are not 
             * put back into the instance table.
             */
            setPageSessionAttribute(INSTANCES_REMOVED, (Serializable)instances);
            
            if (selected.length == 1) {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "authentication.instance.deleted");
            } else {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "authentication.instance.deleted.multiple");
            }
            removePageSessionAttribute(AUTH_INSTANCE_TABLE);
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }

        forwardTo();
    }

    /**
     * Handles save button request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        try {
            AuthPropertiesModel model = (AuthPropertiesModel)getModel();
            Map original = model.getValues();
            AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
            model.setValues(ps.getAttributeValues(original, true, model));
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                "authentication.profile.updated");
        } catch (AMConsoleException a) {
            setInlineAlertMessage(
                CCAlert.TYPE_ERROR, "message.error", a.getMessage());
        }
        forwardTo();
    }

    /**
     * Handles reset request.
     *   
     * @param event Request invocation event
     */  
    public void handleButton2Request(RequestInvocationEvent event) {
        forwardTo();
    }

    /**
     * Handles the edit authentication instance request.
     *
     * @param event Request Invocation Event.
     */
    public void handleActionColumnHREFRequest(
        RequestInvocationEvent event
    ) throws ModelControlException 
    {
        String type = hexToString(
            (String)getDisplayFieldValue(ACTION_COLUMN_HREF));

        AuthPropertiesModel model = (AuthPropertiesModel)getModel();
        if (model.getServiceName(type) == null) {
            setPageSessionAttribute(INSTANCE_MSG,
                model.getLocalizedString("no.module.instance"));
            forwardTo();
        } else {            
            setPageSessionAttribute(
                EditAuthTypeViewBean.SERVICE_TYPE, type);
            
            /*
             * EditAuthTypeViewBean displays the properties for the auth 
             * instance selected. We need to set the current profile value
             * to be the name of the current realm. Current Profile is use
             * by AMServiceProfileViewBeanBase.getPropertySheetXML() when 
             * building the page.
             */ 
            setPageSessionAttribute(AMAdminConstants.CURRENT_PROFILE, 
                (String)getPageSessionAttribute(AMAdminConstants.CURRENT_REALM));
            
            EditAuthTypeViewBean vb = (EditAuthTypeViewBean)
                getViewBean(EditAuthTypeViewBean.class);
            unlockPageTrail();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } 
    }

    /**
     * Handles the edit authentication configuration request.
     *
     * @param event Request Invocation Event.
     */
    public void handleConfigActionColumnHREFRequest(
        RequestInvocationEvent event
    ) throws ModelControlException
    { 
        String name = hexToString(
            (String)getDisplayFieldValue(CONFIG_ACTION_COLUMN_HREF));

        AuthConfigViewBean vb = (AuthConfigViewBean)
            getViewBean(AuthConfigViewBean.class);
        setPageSessionAttribute(AuthConfigurationModelImpl.CONFIG_NAME, name);
        unlockPageTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    public void forwardTo(RequestContext reqContext)
        throws NavigationException 
    {
        // set the realm selected in the parentage path in the model.
        String tmp = (String)
            getPageSessionAttribute(AMAdminConstants.CURRENT_REALM);
        AuthPropertiesModel m = (AuthPropertiesModel)getModel();
        m.setCurrentRealm(tmp);

        removePageSessionAttribute(AUTH_INSTANCE_TABLE); 
        removePageSessionAttribute(AUTH_CONFIG_TABLE);
        super.forwardTo(reqContext);
    }
}

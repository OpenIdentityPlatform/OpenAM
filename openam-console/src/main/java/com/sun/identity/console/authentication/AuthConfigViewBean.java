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
 * $Id: AuthConfigViewBean.java,v 1.5 2009/01/12 19:26:08 asyhuang Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.authentication;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.authentication.config.AMAuthConfigUtils;
import com.sun.identity.authentication.config.AMAuthenticationInstance;
import com.sun.identity.authentication.config.AuthConfigurationEntry;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.console.authentication.model.AuthConfigurationModel;
import com.sun.identity.console.authentication.model.AuthConfigurationModelImpl;
import com.sun.identity.console.authentication.model.AuthPropertiesModel;
import com.sun.identity.console.authentication.model.AuthPropertiesModelImpl;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.delegation.model.DelegationConfig;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCButton;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.table.CCActionTable;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;


public class AuthConfigViewBean
    extends AMPrimaryMastHeadViewBean
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/authentication/AuthConfig.jsp";

    private static final String PGTITLE_THREE_BTNS = "pgtitleThreeBtns";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";

    private AuthConfigurationModel acModel = null;
    private CCActionTableModel entryTableModel = null;
    private AMPropertySheetModel propertySheetModel;
    private CCPageTitleModel ptModel;

    public static final String ADD_ENTRY_BUTTON = "addEntryButton";
    public static final String AUTH_ENTRY_TABLE = "authConfigEntryTable";
    public static final String CRITERIA = "criteria";
    public static final String CRITERIA_LABEL = "criteriaLabel";
    public static final String ENTRY_LIST = "authConfigEntryList";
    public static final String MODULE_NAME = "moduleName";
    public static final String MODULE_NAME_LABEL = "moduleNameLabel";
    public static final String OPTION_FIELD = "optionField";
    public static final String OPTION_FIELD_LABEL = "optionFieldLabel";
    public static final String REMOVE_ENTRY_BUTTON = "removeEntryButton";
    public static final String REORDER_ENTRY_BUTTON = "reorderEntryButton";
    public static final String REQUIRED_FLAG = "REQUIRED";
    public static final String ACTION_TILED_VIEW = "actionTiledView";

    private String currentRealm = null;
    private String configName = null;
    private boolean tablePopulated = false;

    /**
     * Creates a realm confguration view bean.
     */
    public AuthConfigViewBean() {
        super("AuthConfig");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    protected void initialize() {
        if (!initialized) {
            String realmName = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_PROFILE);

            if (realmName != null) {
                super.initialize();
                initialized = true;
                createPageTitleModel();
                createPropertyModel(realmName);
                createAuthEntryTable();
                registerChildren();
            }
        }
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(PGTITLE_THREE_BTNS, CCPageTitle.class);
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        registerChild(ACTION_TILED_VIEW, AuthActionTiledView.class);
        registerChild(AUTH_ENTRY_TABLE, CCActionTable.class);
        propertySheetModel.registerChildren(this);
        ptModel.registerChildren(this);
    }

    protected View createChild(String name) {
        View view = null;

        if (!tablePopulated) {
            populateEntryTable();
        }

        if (name.equals(PGTITLE_THREE_BTNS)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (name.equals(PROPERTY_ATTRIBUTE)) {
            view = new AMPropertySheet(this, propertySheetModel, name);
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else if (name.equals(ACTION_TILED_VIEW)) {
            view = new AuthActionTiledView(this, entryTableModel, name);
        } else if (name.equals(AUTH_ENTRY_TABLE)) {
            CCActionTable table = new CCActionTable(this,entryTableModel,name);
            table.setTiledView(
                (AuthActionTiledView)getChild(ACTION_TILED_VIEW));
            view = table;
        } else if (propertySheetModel.isChildSupported(name)) {
            view = propertySheetModel.createChild(this, name, getModel());
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    private AuthPropertiesModel getPropertiesModel() {
        RequestContext rc = RequestManager.getRequestContext();
        HttpServletRequest req = rc.getRequest();
        return new AuthPropertiesModelImpl(req, getPageSessionAttributes());
    }

    /*
     * Iterate through the list of known AuthInstances and get the 
     * instance name. Use those names to create a set, then sort that set
     * of names.
     */
    private List getInstanceNames() {
        AuthPropertiesModel apm = getPropertiesModel();
        Set tmp = apm.getAuthInstances();
        Set instances = new HashSet(tmp.size() * 2);

        for (Iterator i=tmp.iterator(); i.hasNext();) {
            AMAuthenticationInstance inst =
                (AMAuthenticationInstance)i.next();
            instances.add(inst.getName());
        }

        return AMFormatUtils.sortItems(instances, apm.getUserLocale());
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
        resetButtonState(REMOVE_ENTRY_BUTTON);

        acModel = (AuthConfigurationModel)getModel();

        // set the page title to include the auth config name
        String title = acModel.getLocalizedString(
            "page.title.auth.config.edit");
        String[] param = { getConfigName() };
        ptModel.setPageTitleText(MessageFormat.format(title, (Object[])param));

        AuthConfigurationModel model = (AuthConfigurationModel)getModel();
        if (model != null) {
            AMPropertySheet ps = 
                (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
            ps.setAttributeValues(getValues(model), model);
        }
        if (!tablePopulated) {
            populateEntryTable();
        }

        CCButton btnReorder = (CCButton)getChild(REORDER_ENTRY_BUTTON);
        btnReorder.setDisabled(model.getNumberEntries() < 2);

        if (getInstanceNames().isEmpty()) {        
            CCButton btnAdd = (CCButton)getChild(ADD_ENTRY_BUTTON);
            btnAdd.setDisabled(true);

            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                "authentication.instance.list.empty");
        }
        
        showInvalidSet();
    }
    
    public void showInvalidSet() {
        Set invalidSet = new HashSet();
        Set validSet  = new HashSet();
        
        List list = getInstanceNames();
        if ((list != null) && !list.isEmpty()) {
            validSet.addAll(list);
        }
        
		AuthConfigurationModel model = (AuthConfigurationModel)getModel();
        int size = model.getNumberEntries();
        
        for (int i = 0; i < size; i++) {
            String module = model.getModuleName(i);
            if (!validSet.contains(module)) {
                invalidSet.add(module);
            }
        }
        
        if (!invalidSet.isEmpty()) {
            StringBuilder buff = new StringBuilder();
            boolean bFirst = true;
            for (Iterator i = invalidSet.iterator(); i.hasNext(); ) {
                if (bFirst) {
                    bFirst = false;
                } else {
                    buff.append(", ");
                }
                buff.append((String)i.next());
            }
            Object[] params = {buff.toString()};
            String msg = (invalidSet.size() > 1) ? 
                "authentication.instance.invalids" : 
                "authentication.instance.invalid";
            setInlineAlertMessage(CCAlert.TYPE_WARNING, "message.warning",
                MessageFormat.format(model.getLocalizedString(msg), params));            
        }
    }

    private Map getValues(AuthConfigurationModel model) {
        Map values =(Map)getPageSessionAttribute(
            ReorderAuthChainsViewBean.PG_SESSION_TRACKING);

        if (values == null) {
            try {
                values = model.getValues();
            } catch (AMConsoleException a) {
                setInlineAlertMessage(CCAlert.TYPE_WARNING, 
                    "message.warning", "noproperties.message");
            } 
        }
        return (values == null) ? Collections.EMPTY_MAP : values;
    }

    /**
     * Handles save button request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        CCActionTable table = (CCActionTable)getChild(AUTH_ENTRY_TABLE);
        table.restoreStateData();

        acModel = (AuthConfigurationModel)getModel();
        acModel.setEntries(getTableData());

        try {
            Map original = acModel.getValues();
            AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
            Map updated = ps.getAttributeValues(original, true, acModel);
            acModel.setValues(updated);

            acModel.store(getRealmName(), getConfigName());
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                "authentication.save.ok");
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
            tablePopulated = false;
        }

        forwardTo();
    }

    private List getTableData() throws ModelControlException{
        List entryList = new ArrayList();

        CCActionTable table = (CCActionTable)getChild(AUTH_ENTRY_TABLE);
        table.restoreStateData();

        int size = entryTableModel.getNumRows();
        for (int i=0; i < size; i++) {
            entryTableModel.setRowIndex(i);
            String module = (String)entryTableModel.getValue(MODULE_NAME);
            String flag = (String)entryTableModel.getValue(CRITERIA);
            String option = (String)entryTableModel.getValue(OPTION_FIELD);
            try {
                AuthConfigurationEntry ae = new AuthConfigurationEntry(
                    module, flag, option);
                entryList.add(ae);
            } catch (AMConfigurationException e) {
                debug.warning("AuthConfigViewBean.getTableData() " +
                    "Couldn't create the auth configuration entry",e); }
        }

        return entryList;
    }    

    /**
     * Handles reset request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        acModel = (AuthConfigurationModel)getModel();
        acModel.reset(getRealmName(), getConfigName());
        tablePopulated = false;
        removePageSessionAttribute(ENTRY_LIST);
        removePageSessionAttribute(
                ReorderAuthChainsViewBean.PG_SESSION_TRACKING);
        forwardTo();
    }

    /**
     * Handles reset request.
     *
     * @param event Request invocation event
     */
    public void handleButton3Request(RequestInvocationEvent event) {
        backTrail();
        AuthPropertiesViewBean vb = (AuthPropertiesViewBean)
            getViewBean(AuthPropertiesViewBean.class);
        removePageSessionAttribute(ENTRY_LIST);
        removePageSessionAttribute(
                ReorderAuthChainsViewBean.PG_SESSION_TRACKING);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Handles add auth instances request.
     *
     * @param event Request invocation event
     */
    public void handleAddEntryButtonRequest(RequestInvocationEvent event)
        throws ModelControlException
    {
        // get the current values from the table so we don't 
        // overwrite any changes already made by the user.
        List currentEntries = getTableData();

        // create a new entry, add it to the list, and generat the xml
        // use the first entry from the available instances list as default
        Iterator i = getInstanceNames().iterator();
        String moduleName = (String)i.next();
        try {
            AuthConfigurationEntry ace = new AuthConfigurationEntry(
                moduleName, REQUIRED_FLAG, "");
            currentEntries.add(ace);
            acModel.setEntries(currentEntries);
            setPageSessionAttribute(ENTRY_LIST, 
                acModel.getXMLValue(getRealmName(), getConfigName()));
        } catch (AMConfigurationException a) {
            debug.warning("AuthConfigViewBean.handleAddEntryButtonRequest() " +
                "Adding new config entry failed", a);
        }
        cacheValues();
        populateEntryTable();
        forwardTo();
    }

    /**
     * Handles remove auth instance request.
     *
     * @param event Request invocation event
     */
    public void handleRemoveEntryButtonRequest(RequestInvocationEvent event)
        throws ModelControlException
    {
        CCActionTable table = (CCActionTable)getChild(AUTH_ENTRY_TABLE);
        table.restoreStateData();

        // get the entries selected in the table and remove from 
        // the model
        Integer[] selected = entryTableModel.getSelectedRows();
        acModel = (AuthConfigurationModel)getModel();
        acModel.removeAuthEntries(selected);

        String xml = acModel.getXMLValue(getRealmName(), getConfigName());
        setPageSessionAttribute(ENTRY_LIST, xml);

        // set back to false to force the values to be re-read
        cacheValues();
        tablePopulated = false;
        forwardTo();
    }

    /**
     * Handles reorder authentication chains request.
     *
     * @param event Request invocation event
     */
    public void handleReorderEntryButtonRequest(RequestInvocationEvent event)
        throws ModelControlException
    {
        CCActionTable table = (CCActionTable)getChild(AUTH_ENTRY_TABLE);
        table.restoreStateData();

        cacheValues();
        setPageSessionAttribute(ENTRY_LIST,
            AMAuthConfigUtils.authConfigurationEntryToXMLString(
            getTableData()));

        ReorderAuthChainsViewBean vb = (ReorderAuthChainsViewBean)
            getViewBean(ReorderAuthChainsViewBean.class);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    private void cacheValues()
        throws ModelControlException
    {
        AuthConfigurationModel acModel = (AuthConfigurationModel)getModel();
        try {
            Map original = acModel.getValues();
            AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
            Map changedValues = 
                    (Map) ps.getAttributeValues(original, true, acModel);
            original.putAll(changedValues);
            setPageSessionAttribute(
                    ReorderAuthChainsViewBean.PG_SESSION_TRACKING,
                    (HashMap)changedValues);            
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
            forwardTo();
        }
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", getBackButtonLabel("config.auth.label"));
    }

    protected AMModel getModelInternal() {
        RequestContext rc = RequestManager.getRequestContext();
        acModel = new AuthConfigurationModelImpl(
            rc.getRequest(), getPageSessionAttributes());

        acModel.initialize(getRealmName(), getConfigName());
        return acModel;
    }

    private void createPropertyModel(String realmName) {
        DelegationConfig dConfig = DelegationConfig.getInstance();
        boolean canModify = dConfig.hasPermission(realmName, null,
            AMAdminConstants.PERMISSION_MODIFY, getModel(),
            getClass().getName());

        String xmlFile = (canModify) ?
            "com/sun/identity/console/propertyAuthConfig.xml" :
            "com/sun/identity/console/propertyAuthConfig_Readonly.xml";
        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(xmlFile));
        propertySheetModel.clear();
    }

    private void createAuthEntryTable() {
        entryTableModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/tblAuthConfig.xml"));

        entryTableModel.setTitleLabel("label.items");
        entryTableModel.setActionValue(MODULE_NAME_LABEL,
            "authentication.config.entry.module.column");
        entryTableModel.setActionValue(CRITERIA_LABEL,
            "authentication.config.entry.criteria.column");
        entryTableModel.setActionValue(OPTION_FIELD_LABEL,
            "authentication.config.entry.option.column");
        entryTableModel.setActionValue(ADD_ENTRY_BUTTON,
            "authentication.config.entry.add.button");
        entryTableModel.setActionValue(REMOVE_ENTRY_BUTTON,
            "authentication.config.entry.delete.button");
        entryTableModel.setActionValue(REORDER_ENTRY_BUTTON,
            "authentication.config.entry.reorder.button");

        propertySheetModel.setModel(AUTH_ENTRY_TABLE, entryTableModel);
    }

    private String getRealmName() {
        if (currentRealm == null) {
            currentRealm = 
                (String)getPageSessionAttribute(AMAdminConstants.CURRENT_REALM);
        }
        return currentRealm;
    }

    private String getConfigName() {
        if (configName == null) {
            configName = (String)getPageSessionAttribute(
                AuthConfigurationModelImpl.CONFIG_NAME);
        }
        return configName;
    }

    private void populateEntryTable() {
        tablePopulated = true;
        entryTableModel.clearAll();

        acModel = (AuthConfigurationModel)getModel(); 
        
        // see if the xml is stored for the module entries. if it is we
        // need to take it and populate the entry values in the model.
        String entryValue = (String)getPageSessionAttribute(ENTRY_LIST);
        if ((entryValue != null) && (entryValue.length() > 0) ) {
            acModel.setXMLValue(entryValue);
        } else {
            entryValue = acModel.getXMLValue(getRealmName(), getConfigName());
        }

        int size = acModel.getNumberEntries();
        for (int x=0; x < size; x++ ) {
            String module = acModel.getModuleName(x);
            String flag = acModel.getModuleFlag(x);
            String options = acModel.getModuleOptions(x);
            if (x > 0) {
                entryTableModel.appendRow();
            }

            entryTableModel.setValue(MODULE_NAME,module);
            entryTableModel.setValue(CRITERIA,flag);
            entryTableModel.setValue(OPTION_FIELD,options);
        }

        // store for retrieval during request cycle
        setPageSessionAttribute(ENTRY_LIST, entryValue);
    }

    public String getOptionFieldValue(int index) {
        acModel = (AuthConfigurationModelImpl)getModel();
        return acModel.getModuleOptions(index);
    }

    public String getModuleFlag(int index) {
        acModel = (AuthConfigurationModelImpl)getModel();
        return acModel.getModuleFlag(index);
    }

    public String getModuleName(int index) {
        acModel = (AuthConfigurationModelImpl)getModel();
        return acModel.getModuleName(index);
    }
    
    public OptionList getModuleNameChoiceValues() {
        return createOptionList(getInstanceNames());
    }

    public OptionList getCriteriaNameChoiceValues() {
        acModel = (AuthConfigurationModelImpl)getModel();
        return createOptionList(acModel.getCriteriaMap());
    }

    protected String getBreadCrumbDisplayName() {
        String[] arg = {getConfigName()};
        AuthConfigurationModelImpl model = 
            (AuthConfigurationModelImpl)getModel();
        return MessageFormat.format(model.getLocalizedString(
            "breadcrumbs.auth.editConfiguration"), (Object[])arg);
    }

    protected boolean startPageTrail() {
        return false;
    }
}

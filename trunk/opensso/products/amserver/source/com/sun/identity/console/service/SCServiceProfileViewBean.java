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
 * $Id: SCServiceProfileViewBean.java,v 1.7 2009/01/09 22:35:19 asyhuang Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.console.service;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.NavigationException;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.authentication.util.AMAuthUtils;
import com.sun.identity.console.base.AMServiceProfile;
import com.sun.identity.console.base.AMServiceProfileViewBeanBase;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.base.model.AMServiceProfileModel;
import com.sun.identity.console.base.model.SMSubConfig;
import com.sun.identity.console.components.view.html.SerializedField;
import com.sun.identity.console.service.model.SubSchemaModel;
import com.sun.identity.console.service.model.SubSchemaModelImpl;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.table.CCActionTable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import com.sun.identity.console.base.model.AMAdminConstants; 
import com.sun.identity.console.service.model.SubConfigModel;
import com.sun.identity.console.service.model.SubConfigModelImpl;
import com.sun.identity.sm.SMSEntry;
import com.sun.web.ui.view.html.CCRadioButton;

public class SCServiceProfileViewBean extends AMServiceProfileViewBeanBase {
    public static final String DEFAULT_DISPLAY_URL =
        "/console/service/SCServiceProfile.jsp";
    public static final String PAGE_NAME = "SCServiceProfile";

    private boolean populatedSubConfigTable;

    /**
     * Creates a service profile view bean.
     */
    public SCServiceProfileViewBean() {
        super(PAGE_NAME, DEFAULT_DISPLAY_URL, null);
        initService();
    }

    SCServiceProfileViewBean(String name, String defaultURL) {
        super(name, defaultURL, null);
        initService();
    }

    private void initService() {
        String serviceName = (String)getPageSessionAttribute(
            AMServiceProfile.SERVICE_NAME);
        if (serviceName != null) {
            initialize(serviceName);
        }
    }

    protected View createChild(String name) {
        if (!populatedSubConfigTable &&
            name.equals(AMPropertySheetModel.TBL_SUB_CONFIG)
        ) {
            populatedSubConfigTable = true;
            SubSchemaModel model = (SubSchemaModel)getModel();
            if (model.hasGlobalSubSchema()) {
                SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
                populateTableModel((List)szCache.getSerializedObj());
            }
        }
        return super.createChild(name);
    }

    private void populateTableModel(List subconfig) {
        CCActionTable tbl = (CCActionTable)getChild(
            AMPropertySheetModel.TBL_SUB_CONFIG);
        CCActionTableModel tblModel =(CCActionTableModel)tbl.getModel();
        tblModel.clearAll();

        if (subconfig != null) {
            SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
            List cache = new ArrayList(subconfig.size());

            if (!subconfig.isEmpty()) {
                tblModel.clearAll();
                boolean firstEntry = true;

                for (Iterator iter = subconfig.iterator(); iter.hasNext(); ) {
                    if (firstEntry) {
                        firstEntry = false;
                    } else {
                        tblModel.appendRow();
                    }
                    SMSubConfig conf = (SMSubConfig)iter.next();
                    tblModel.setValue(
                        AMPropertySheetModel.TBL_SUB_CONFIG_DATA_NAME,
                        conf.getName());
                    tblModel.setValue(
                        AMPropertySheetModel.TBL_SUB_CONFIG_HREF_NAME,
                        conf.getName());
                    tblModel.setValue(
                        AMPropertySheetModel.TBL_SUB_CONFIG_DATA_TYPE,
                        conf.getType());
                    cache.add(conf);
                }
            }
            szCache.setValue((ArrayList)cache);
        }
    }

    public void forwardTo(RequestContext reqContext)
        throws NavigationException {
        initialize((String)getPageSessionAttribute(
            AMServiceProfile.SERVICE_NAME));
        super.forwardTo(reqContext);
    }

     public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);

        SubSchemaModel model = (SubSchemaModel)getModel();
        if (model.hasGlobalSubSchema()){
            if (!submitCycle) {
                populateTableModel(model.getSubConfigurations());
            }
            resetButtonState(AMPropertySheetModel.TBL_SUB_CONFIG_BUTTON_DELETE);

            Map createable = model.getCreateableSubSchemaNames();
            if (createable.isEmpty()) {
                resetButtonState(
                    AMPropertySheetModel.TBL_SUB_CONFIG_BUTTON_ADD);
            } else {
                SubConfigModel scModel = getSubConfigModel(); 
                boolean canCreate = false;
                for (Iterator i = createable.keySet().iterator(); 
                    i.hasNext() && !canCreate;
                ) {
                    String name = (String)i.next();
                    String plugin = scModel.getSelectableSubConfigNamesPlugin(
                        name);
                    if (plugin == null) {
                        canCreate = true;
                    } else {
                        Set subconfigNames = scModel.getSelectableConfigNames(
                            name);
                        canCreate = (subconfigNames != null) && 
                            !subconfigNames.isEmpty();
                    }
                }
                disableButton(AMPropertySheetModel.TBL_SUB_CONFIG_BUTTON_ADD,
                    !canCreate);              
            }
        }
        if(serviceName.equals("iPlanetAMAuthHTTPBasicService")){
             CCRadioButton radio = (CCRadioButton)getChild(
                            "iplanet-am-auth-http-basic-module-configured");             
            if((radio.getValue()==null) || (radio.getValue().equals(""))){
                String defaultModule = new String();                               
                String realmName = SMSEntry.getRootSuffix();
                if (realmName != null) {
                    List moduleList = 
                        AMAuthUtils.getModuleInstancesForHttpBasic(realmName);                 
                if (!moduleList.isEmpty()){
                    defaultModule =  (String) moduleList.get(0);                   
                    radio.setValue(defaultModule);
                }
                }
             }
        }
    }

    protected String getPropertySheetXML(AMServiceProfileModel model)
        throws AMConsoleException {
        return ((SubSchemaModel)model).getPropertySheetXML(
            "/", PAGE_NAME, getClass().getName());
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        try {
            return new SubSchemaModelImpl(
                req, serviceName, getPageSessionAttributes());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
        return null;
    }
    
    private SubConfigModel getSubConfigModel() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        try {
            return new SubConfigModelImpl(
                req, serviceName, "/", getPageSessionAttributes());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
        return null;
    }

    /**
     * Adds sub configuration.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblSubConfigButtonAddRequest(
        RequestInvocationEvent event
    ) {
        SubSchemaModel model = (SubSchemaModel)getModel();
        Map createable = model.getCreateableSubSchemaNames();

        if (!createable.isEmpty()) {
            if (createable.size() > 1) {
                SubSchemaTypeSelectViewBean vb = (SubSchemaTypeSelectViewBean)
                    getViewBean(SubSchemaTypeSelectViewBean.class);
                addViewBeanClassToPageSession();
                unlockPageTrail();
                passPgSessionMap(vb);
                vb.forwardTo(getRequestContext());
            } else {
                String subSchema =
                    (String)createable.keySet().iterator().next();
                setPageSessionAttribute(
                    AMServiceProfile.PG_SESSION_SUB_SCHEMA_NAME,
                    subSchema);
                SubConfigAddViewBean vb = (SubConfigAddViewBean)getViewBean(
                    SubConfigAddViewBean.class);
                addViewBeanClassToPageSession();
                unlockPageTrail();
                passPgSessionMap(vb);
                vb.forwardTo(getRequestContext());
            }
        } else {
            forwardTo();
        }
    }

    private void addViewBeanClassToPageSession() {
        ArrayList viewBeanClasses = (ArrayList)getPageSessionAttribute(
            AMServiceProfile.PG_SESSION_PROFILE_VIEWBEANS);
        if (viewBeanClasses == null) {
            viewBeanClasses = new ArrayList();
            setPageSessionAttribute(
                AMServiceProfile.PG_SESSION_PROFILE_VIEWBEANS, viewBeanClasses);
        }
        viewBeanClasses.add(0, "../service/SCServiceProfile");
    }

    /**
     * Deletes sub configuration.
     *
     * @param event Request Invocation Event.
     * @throws ModelControlException if table model cannot be restored.
     */
    public void handleTblSubConfigButtonDeleteRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        submitCycle = true;
        CCActionTable tbl = (CCActionTable)getChild(
            AMPropertySheetModel.TBL_SUB_CONFIG);
        tbl.restoreStateData();
        CCActionTableModel tblModel =(CCActionTableModel)tbl.getModel();
        Integer[] selected = tblModel.getSelectedRows();
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
        List list = (List)szCache.getSerializedObj();
        Set names = new HashSet(selected.length *2);

        for (int i = 0; i < selected.length; i++) {
            SMSubConfig sc = (SMSubConfig)list.get(selected[i].intValue());
            names.add(sc.getName());
        }

        try {
            SubSchemaModel model = (SubSchemaModel)getModel();
            model.deleteSubConfigurations(names);

            if (selected.length == 1) {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "subconfig.message.deleted");
            } else {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "subconfig.message.deleted.pural");
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }

        submitCycle = false;
        forwardTo();
    }

    /**
     * Handles edit sub configuration request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblSubConfigHrefNameRequest(RequestInvocationEvent event){
        String configName = (String)getDisplayFieldValue(
            AMPropertySheetModel.TBL_SUB_CONFIG_HREF_NAME);
        SubConfigEditViewBean vb = (SubConfigEditViewBean)getViewBean(
            SubConfigEditViewBean.class);
        ArrayList subConfigNames = (ArrayList)getPageSessionAttribute(
            AMServiceProfile.PG_SESSION_SUB_CONFIG_IDS);
        if (subConfigNames == null) {
            subConfigNames = new ArrayList();
            subConfigNames.add("/");
            setPageSessionAttribute(AMServiceProfile.PG_SESSION_SUB_CONFIG_IDS,
                subConfigNames);
        }
        subConfigNames.add(0, configName);
        addViewBeanClassToPageSession();
        unlockPageTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    protected String getBreadCrumbDisplayName() {
        AMModel model = (AMModel)getModel();
        String serviceName = (String)getPageSessionAttribute(
            AMServiceProfile.SERVICE_NAME);
        Object[] arg = {model.getLocalizedServiceName(serviceName)};
        return MessageFormat.format(model.getLocalizedString(
            "breadcrumbs.services.edit"), arg);
    }

    protected boolean startPageTrail() {
        return false;
    }

    // button1 (Save) request done in AMServiceProfileViewBean
    // button2 (Reset) request done in AMServiceProfileViewBean

    /**
     * Handles reset request.
     *
     * @param event Request invocation event
     */
    public void handleButton3Request(RequestInvocationEvent event) {
        backTrail();
        try {
            String name = (String) getPageSessionAttribute(
                    AMAdminConstants.SAVE_VB_NAME);
            SCConfigViewBean vb = (SCConfigViewBean) getViewBean(
                    Class.forName(name));
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext()); 
        } catch (ClassNotFoundException e) {
            debug.warning("SCServiceProfileViewBean.handleButton3Request:", e);
        }
    }
}

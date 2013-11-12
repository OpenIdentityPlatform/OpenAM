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
 * $Id: SubConfigEditViewBean.java,v 1.2 2008/06/25 05:43:17 qcheng Exp $
 *
 */

package com.sun.identity.console.service;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPostViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMServiceProfile;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.base.model.SMSubConfig;
import com.sun.identity.console.components.view.html.SerializedField;
import com.sun.identity.console.service.model.SubConfigModel;
import com.sun.identity.console.service.model.SubConfigModelImpl;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.table.CCActionTable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public class SubConfigEditViewBean
    extends AMPrimaryMastHeadViewBean
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/service/SubConfigEdit.jsp";
    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    private static final String ATTR_SUBCONFIG_NAME = "tfSubConfigName";

    private CCPageTitleModel ptModel;
    private AMPropertySheetModel propertySheetModel;
    private boolean init;
    private boolean submitCycle;
    private boolean populatedSubConfigTable;

    /**
     * Creates a view to prompt user for services to be added to realm.
     */
    public SubConfigEditViewBean() {
        super("SubConfigEdit");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    protected void initialize() {
        if (!init) {
            String serviceName = (String)getPageSessionAttribute(
                AMServiceProfile.SERVICE_NAME);
            if ((serviceName != null) && (serviceName.trim().length() > 0)) {
                super.initialize();
                createPageTitleModel();
                createPropertyModel();
                registerChildren();
                init = true;
            }
        }
    }

    protected void registerChildren() {
        super.registerChildren();
        ptModel.registerChildren(this);
        propertySheetModel.registerChildren(this);
        registerChild(PGTITLE_TWO_BTNS, CCPageTitle.class);
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        registerChild(SZ_CACHE, SerializedField.class);
    }

    protected View createChild(String name) {
        if (!populatedSubConfigTable &&
            name.equals(AMPropertySheetModel.TBL_SUB_CONFIG)
        ) {
            populatedSubConfigTable = true;
            SubConfigModel model = (SubConfigModel)getModel();
            if (model.hasGlobalSubSchema()) {
                SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
                populateTableModel((List)szCache.getSerializedObj());
            }
        }

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


    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", "button.cancel");
    }

    private void createPropertyModel() {
        try {
            SubConfigModel model = (SubConfigModel)getModel();
            propertySheetModel = new AMPropertySheetModel(
                model.getEditConfigPropertyXML(getClass().getName()));
            propertySheetModel.clear();
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }

    protected AMModel getModelInternal() {
        AMModel model = null;
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        String serviceName = (String)getPageSessionAttribute(
            AMServiceProfile.SERVICE_NAME);
        String parentId = getParentId();

        try {
            model = new SubConfigModelImpl(req, serviceName, parentId,
                getPageSessionAttributes());
        } catch (AMConsoleException e) {
            //
        }

        return model;
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);
        SubConfigModel model = (SubConfigModel)getModel();

        if (!submitCycle) {
            AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
            propertySheetModel.clear();

            try {
                ps.setAttributeValues(
                    model.getSubConfigAttributeValues(), model);
            } catch (AMConsoleException a) {
                setInlineAlertMessage(CCAlert.TYPE_WARNING,
                    "message.warning", "noproperties.message");
            }
        }

        if (model.hasGlobalSubSchema()){
            if (!submitCycle) {
                populateTableModel(model.getSubConfigurations());
            }
            resetButtonState(AMPropertySheetModel.TBL_SUB_CONFIG_BUTTON_DELETE);

            Map createable = model.getCreateableSubSchemaNames();
            if (createable.isEmpty()) {
                resetButtonState(
                    AMPropertySheetModel.TBL_SUB_CONFIG_BUTTON_ADD);
            }
        }

        List parentIds = (List)getPageSessionAttribute(
            AMServiceProfile.PG_SESSION_SUB_CONFIG_IDS);
        String parentId = (String)parentIds.get(0);
        String[] param = {parentId};
        ptModel.setPageTitleText(MessageFormat.format(
            model.getLocalizedString("page.title.services.edit.subconfig"),
            (Object[])param));
    }

    private String getParentId() {
        List parentIds = (List)getPageSessionAttribute(
            AMServiceProfile.PG_SESSION_SUB_CONFIG_IDS);
        List escapeParentIds = AMAdminUtils.replaceString(
            parentIds, "%", "%25");
        escapeParentIds = AMAdminUtils.replaceString(
            escapeParentIds, "/", "%2F");
        return AMAdminUtils.getString(escapeParentIds, "/", true);
    }

    /**
     * Handles cancel request.
     *
     * @param event Request invocation event
     */
    public void handleButton3Request(RequestInvocationEvent event) {
        backToProfileViewBean();
    }

    /**
     * Handles reset request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        forwardTo();
    }

    private void backToProfileViewBean() {
        List urls = (List)getPageSessionAttribute(
            AMServiceProfile.PG_SESSION_PROFILE_VIEWBEANS);
        String url = (String)urls.remove(0);

        List parentIds = (List)getPageSessionAttribute(
            AMServiceProfile.PG_SESSION_SUB_CONFIG_IDS);
        parentIds.remove(0);

        AMPostViewBean vb = (AMPostViewBean)getViewBean(AMPostViewBean.class);
        backTrail();
        passPgSessionMap(vb);
        vb.setTargetViewBeanURL(url);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Handles next button request.
     *
     * @param event Request invocation event.
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException {
        submitCycle = true;
        SubConfigModel model = (SubConfigModel)getModel();
        AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);

        try {
            Map orig = model.getSubConfigAttributeValues();
            Map values = ps.getAttributeValues(orig, true, true, model);
            model.setSubConfigAttributeValues(values);
            backToProfileViewBean();
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
            forwardTo();
        }
    }

    /**
     * Adds sub configuration.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblSubConfigButtonAddRequest(
        RequestInvocationEvent event
    ) {
        SubConfigModel model = (SubConfigModel)getModel();
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
        ArrayList urls = (ArrayList)getPageSessionAttribute(
            AMServiceProfile.PG_SESSION_PROFILE_VIEWBEANS);
        if (urls == null) {
            urls = new ArrayList();
            setPageSessionAttribute(
                AMServiceProfile.PG_SESSION_PROFILE_VIEWBEANS, urls);
        }
        urls.add(0, "../service/SubConfigEdit.jsp");
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
            SubConfigModel model = (SubConfigModel)getModel();
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

        AMPostViewBean vb = (AMPostViewBean)getViewBean(AMPostViewBean.class);
        unlockPageTrail();
        passPgSessionMap(vb);
        vb.setTargetViewBeanURL("../service/SubConfigEdit");
        vb.forwardTo(getRequestContext());
    }

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.services.subconfig.edit";
    }

    protected boolean startPageTrail() {
        return false;
    }

}

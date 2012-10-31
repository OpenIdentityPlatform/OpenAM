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
 * $Id: SubConfigAddViewBean.java,v 1.3 2008/06/25 05:43:17 qcheng Exp $
 *
 */

package com.sun.identity.console.service;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPostViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMServiceProfile;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.service.model.SubConfigModel;
import com.sun.identity.console.service.model.SubConfigModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCDropDownMenu;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public class SubConfigAddViewBean
    extends AMPrimaryMastHeadViewBean
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/service/SubConfigAdd.jsp";
    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    private static final String ATTR_SUBCONFIG_NAME = "tfSubConfigName";

    private CCPageTitleModel ptModel;
    private AMPropertySheetModel propertySheetModel;
    private boolean init;
    private boolean submitCycle;

    /**
     * Creates a view to prompt user for services to be added to realm.
     */
    public SubConfigAddViewBean() {
        super("SubConfigAdd");
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
        ptModel.registerChildren(this);
        propertySheetModel.registerChildren(this);
        registerChild(PGTITLE_TWO_BTNS, CCPageTitle.class);
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        super.registerChildren();
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

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/twoBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.add");
        ptModel.setValue("button2", "button.cancel");
    }

    private void createPropertyModel() {
        try {
            String schemaName = (String)getPageSessionAttribute(
                AMServiceProfile.PG_SESSION_SUB_SCHEMA_NAME);
            SubConfigModel model = (SubConfigModel)getModel();
            propertySheetModel = new AMPropertySheetModel(
                model.getAddConfigPropertyXML(schemaName));
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
        List parentIds = (List)getPageSessionAttribute(
            AMServiceProfile.PG_SESSION_SUB_CONFIG_IDS);
        String parentId = AMAdminUtils.getString(parentIds, "/", true);

        try {
            model = new SubConfigModelImpl(req, serviceName, parentId,
                getPageSessionAttributes());
        } catch (AMConsoleException e) {
            debug.error("SubConfigAddViewBean.getModelInternal", e);
        }

        return model;
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);
        SubConfigModel model = (SubConfigModel)getModel();
        String schemaName = (String)getPageSessionAttribute(
            AMServiceProfile.PG_SESSION_SUB_SCHEMA_NAME);

        if (!submitCycle) {
            AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
            propertySheetModel.clear();

            try {
                ps.setAttributeValues(model.getServiceSchemaDefaultValues(
                    schemaName), model);
            } catch (AMConsoleException a) {
                setInlineAlertMessage(CCAlert.TYPE_WARNING,
                    "message.warning", "noproperties.message");
            }
        }

        Set subconfigNames = model.getSelectableConfigNames(schemaName);
        if ((subconfigNames != null) && !subconfigNames.isEmpty()) {
            CCDropDownMenu menu = (CCDropDownMenu)getChild(ATTR_SUBCONFIG_NAME);
            OptionList optList = this.createOptionList(subconfigNames);
            menu.setOptions(optList);
        }
    }

    /**
     * Handles cancel request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        backToProfileViewBean();
    }

    private void backToProfileViewBean() {
        List urls = (List)getPageSessionAttribute(
            AMServiceProfile.PG_SESSION_PROFILE_VIEWBEANS);
        String url = (String)urls.remove(0);
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
        String subConfigName = (String)getDisplayFieldValue(
            ATTR_SUBCONFIG_NAME);
        SubConfigModel model = (SubConfigModel)getModel();

        try {
            AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
            String schemaName = (String)getPageSessionAttribute(
                AMServiceProfile.PG_SESSION_SUB_SCHEMA_NAME);
            Map values = ps.getAttributeValues(
                model.getAttributeNames(schemaName));
            model.createSubConfig(subConfigName, schemaName, values);
            backToProfileViewBean();
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
            forwardTo();
        }
    }

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.services.subconfig.add";
    }

    protected boolean startPageTrail() {
        return false;
    }

}

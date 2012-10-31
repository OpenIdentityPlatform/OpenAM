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
 * $Id: SubSchemaTypeSelectViewBean.java,v 1.2 2008/06/25 05:43:17 qcheng Exp $
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
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.service.model.SubConfigModel;
import com.sun.identity.console.service.model.SubConfigModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.html.CCRadioButton;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public class SubSchemaTypeSelectViewBean
    extends AMPrimaryMastHeadViewBean
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/service/SubSchemaTypeSelect.jsp";
    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    private static final String RB_SUBCONFIG = "rbSubConfig";

    private CCPageTitleModel ptModel;
    private AMPropertySheetModel propertySheetModel;
    private boolean init;
    private String serviceName;
    private String parentId;

    /**
     * Creates a view to prompt user for services to be added to realm.
     */
    public SubSchemaTypeSelectViewBean() {
        super("SubSchemaTypeSelect");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    protected void initialize() {
        if (!init) {
            serviceName = (String)getPageSessionAttribute(
                AMServiceProfile.SERVICE_NAME);
            if ((serviceName != null) && (serviceName.trim().length() > 0)) {
                super.initialize();
                List parentIds = (List)getPageSessionAttribute(
                    AMServiceProfile.PG_SESSION_SUB_CONFIG_IDS);
                parentId = AMAdminUtils.getString(parentIds, "/", true);

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
        ptModel.setValue("button1", "button.next");
        ptModel.setValue("button2", "button.cancel");
    }

    private void createPropertyModel() {
        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/propertySubConfigSelect.xml"));
        propertySheetModel.clear();
    }

    protected AMModel getModelInternal() {
        AMModel model = null;
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        try {
            model = new SubConfigModelImpl(req, serviceName, parentId,
                getPageSessionAttributes());
        } catch (AMConsoleException e) {
            debug.error("SubSchemaTypeSelectViewBean.getModelInternal", e);
        }
        return model;
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);

        SubConfigModel model = (SubConfigModel)getModel();
        Map createables = model.getCreateableSubSchemaNames();
        CCRadioButton rb = (CCRadioButton)getChild(RB_SUBCONFIG);
        OptionList optList = AMFormatUtils.getSortedOptionList(
            createables, model.getUserLocale());
        rb.setOptions(optList);

        String val = (String)rb.getValue();
        if ((val == null) || (val.length() == 0)) {
            rb.setValue(optList.getValue(0));
        }
    }

    /**
     * Handles cancel request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) {
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
        String subSchema = (String)getDisplayFieldValue(RB_SUBCONFIG);
        SubConfigAddViewBean vb = (SubConfigAddViewBean)getViewBean(
            SubConfigAddViewBean.class);
        unlockPageTrailForSwapping();
        passPgSessionMap(vb);
        vb.setPageSessionAttribute(AMServiceProfile.PG_SESSION_SUB_SCHEMA_NAME,
            subSchema);
        vb.forwardTo(getRequestContext());
    }

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.services.subschema.select";
    }

    protected boolean startPageTrail() {
        return false;
    }
}

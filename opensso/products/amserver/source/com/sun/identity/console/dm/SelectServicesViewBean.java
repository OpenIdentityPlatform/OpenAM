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
 * $Id: SelectServicesViewBean.java,v 1.2 2008/06/25 05:42:55 qcheng Exp $
 *
 */

package com.sun.identity.console.dm;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.dm.model.UMOrganizationModel;
import com.sun.identity.console.dm.model.UMOrganizationModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.model.CCPropertySheetModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCRadioButton;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public class SelectServicesViewBean
    extends AMPrimaryMastHeadViewBean
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/dm/SelectServices.jsp";
    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    private static final String ATTR_SERVICE_LIST = "cbServiceList";

    private CCPageTitleModel ptModel;
    private CCPropertySheetModel propertySheetModel;

    /**
     * Creates a view to prompt user for services to be added to realm.
     */
    public SelectServicesViewBean() {
	super("SelectServices");
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
	super.registerChildren();
    }

    protected View createChild(String name) {
	View view = null;

	if (name.equals(PGTITLE_TWO_BTNS)) {
	    view = new CCPageTitle(this, ptModel, name);
	} else if (name.equals(PROPERTY_ATTRIBUTE)) {
	    view = new AMPropertySheet(this, propertySheetModel, name);
	} else if (propertySheetModel.isChildSupported(name)) {
	    view = propertySheetModel.createChild(this, name);
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
        ptModel.setValue("button1", "button.ok");
        ptModel.setValue("button2", "button.cancel");
    }

    private void createPropertyModel() {
        propertySheetModel = new CCPropertySheetModel(
	    getClass().getClassLoader().getResourceAsStream(
		"com/sun/identity/console/propertyOrgSelectServices.xml"));
        propertySheetModel.clear();
    }

    protected AMModel getModelInternal() {
	HttpServletRequest req =
	    RequestManager.getRequestContext().getRequest();
	return new UMOrganizationModelImpl(req, getPageSessionAttributes());
    }

    public void beginDisplay(DisplayEvent event)
	throws ModelControlException {
	super.beginDisplay(event);

	try {
	    String org = (String)getPageSessionAttribute(
		AMAdminConstants.CURRENT_PROFILE);
	    
	    UMOrganizationModel model = (UMOrganizationModel)getModel();
            Map assignable = model.getAssignableServiceNames(org);
	    if (assignable == null || assignable.isEmpty()) {
	        setInlineAlertMessage(CCAlert.TYPE_INFO, 
		    "message.information", "no.assignable.services");
	    }
	    CCRadioButton rb = (CCRadioButton)getChild(ATTR_SERVICE_LIST);
	    OptionList optList = AMFormatUtils.getSortedOptionList(
		assignable, model.getUserLocale());
	    rb.setOptions(optList);

	    String val = (String)rb.getValue();
	    if ((val == null) || (val.length() == 0)) {
		rb.setValue(optList.getValue(0));
	    }
	} catch (AMConsoleException e) {
	    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
		e.getMessage());
	}
    }

    /**
     * Handles cancel request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        forwardToServiceView(event);
    }

    private void forwardToServiceView(RequestInvocationEvent event) {
        String returnVB = (String)removePageSessionAttribute(
            AMAdminConstants.SAVE_VB_NAME);

        if (returnVB == null) {
            forwardTo();
        } else {
            try {
                String tmp = (String)getPageSessionAttribute(
                    AMAdminConstants.CURRENT_ORG);
                Class clazz = Class.forName(returnVB);
                AMViewBeanBase vb = (AMViewBeanBase)getViewBean(clazz);
		backTrail();
                passPgSessionMap(vb);
                vb.forwardTo(getRequestContext());
            } catch (ClassNotFoundException cnfe) {
                forwardTo();
            }
        }
    }

    /**
     * Handles next button request.
     *
     * @param event Request invocation event.
     */
    public void handleButton1Request(RequestInvocationEvent event)
	throws ModelControlException {
        
	UMOrganizationModel model = (UMOrganizationModel)getModel();
	String serviceName = (String)getDisplayFieldValue(ATTR_SERVICE_LIST);
	if (serviceName == null) {
	    forwardToServiceView(event);
	} else {	
	    serviceName = serviceName.trim();
	    if (serviceName.length() > 0) {
                String organization  = (String)getPageSessionAttribute(
                    AMAdminConstants.CURRENT_PROFILE);

                try {
                    model.registerService(organization, serviceName);
                    forwardToServiceView(event);
                } catch (AMConsoleException e) {
                    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
			    e.getMessage());
                    forwardTo();
                }
	    } else {
	        setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
		    "services.missing.servicename");
	        forwardTo();
	    }
	}
    }

    protected String getBreadCrumbDisplayName() {
	return "breadcrumbs.directorymanager.services.select";
    }

    protected boolean startPageTrail() {
	return false;
    }
}

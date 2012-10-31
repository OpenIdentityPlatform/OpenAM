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
 * $Id: ServicesSelectViewBean.java,v 1.4 2009/03/16 18:28:45 veiming Exp $
 *
 */

package com.sun.identity.console.idm;

import com.sun.identity.shared.locale.Locale;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.idm.model.EntitiesModel;
import com.sun.identity.console.idm.model.EntitiesModelImpl;
import com.sun.identity.console.service.model.SCUtils;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCRadioButton;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServicesSelectViewBean
    extends AMPrimaryMastHeadViewBean
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/idm/ServicesSelect.jsp";
    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    private static final String ATTR_SERVICE_LIST = "cbServiceList";

    private CCPageTitleModel ptModel;
    private AMPropertySheetModel propertySheetModel;
    private boolean submitCycle;

    /**
     * Creates a view to prompt user for services to be added to identity.
     */
    public ServicesSelectViewBean() {
        super("ServicesSelect");
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
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.back");
        ptModel.setValue("button2", "button.next");
        ptModel.setValue("button3", "button.cancel");
    }

    private void createPropertyModel() {
        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/propertyEntityServicesSelect.xml"));
        propertySheetModel.clear();
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        return new EntitiesModelImpl(req, getPageSessionAttributes());
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);
        disableButton("button1", true);
        CCRadioButton rb = (CCRadioButton)getChild(ATTR_SERVICE_LIST);
        setRadioButtons(rb);
    }

    private void setRadioButtons(CCRadioButton rb) {
        try {
            EntitiesModel model = (EntitiesModel)getModel();
            String universalId = (String)getPageSessionAttribute(
                EntityEditViewBean.UNIVERSAL_ID);
            Map assigned = model.getAssignedServiceNames(universalId);
            Map assignables = model.getAssignableServiceNames(universalId);

            for (Iterator i = assigned.keySet().iterator(); i.hasNext(); ) {
                assignables.remove(i.next());
            }

            if (!assignables.isEmpty()) {
                OptionList optList = AMFormatUtils.getSortedOptionList(
                    assignables, model.getUserLocale());
                rb.setOptions(optList);
                if (!submitCycle) {
                    rb.setValue(optList.getValue(0));
                }
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
    public void handleButton3Request(RequestInvocationEvent event) {
        forwardToEntityServiceViewBean();
    }

    private void forwardToEntityServiceViewBean() {
        EntityServicesViewBean vb = (EntityServicesViewBean)getViewBean(
            EntityServicesViewBean.class);
        backTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Handles assigned button request.
     *
     * @param event Request invocation event.
     */
    public void handleButton2Request(RequestInvocationEvent event)
        throws ModelControlException {
        submitCycle = true;
        String serviceName = (String)getDisplayFieldValue(ATTR_SERVICE_LIST);
        String universalId = (String)getPageSessionAttribute(
            EntityEditViewBean.UNIVERSAL_ID);
        EntitiesModel model = (EntitiesModel)getModel();

        if (model.hasUserAttributeSchema(serviceName)) {
            unlockPageTrailForSwapping();
            fowardToAddServiceViewBean(model, universalId, serviceName);
        } else {
            try {
                model.assignService(
                    universalId, serviceName, Collections.EMPTY_MAP);
                forwardToEntityServiceViewBean();
            } catch (AMConsoleException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
                forwardTo();
            }
        }
    }

    private void fowardToAddServiceViewBean(
        EntitiesModel model,
        String universalId,
        String serviceName
    ) {
        SCUtils utils = new SCUtils(serviceName, model);
        String propertiesViewBeanURL = utils.getServiceDisplayURL();
        String type = (String)getPageSessionAttribute(
            EntityEditViewBean.ENTITY_TYPE);
        String realm = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        if (realm == null) {
            realm = "/";
        }
        // Work around for not showing auth config service custom view. 
        // This is not needed in 7.0, but is still used by old 6.3 console.
        if (serviceName.equals(AMAdminConstants.AUTH_CONFIG_SERVICE)) {
            propertiesViewBeanURL = null;
        }

        if ((propertiesViewBeanURL != null) &&
            (propertiesViewBeanURL.trim().length() > 0)
        ) {
            try {
                String charset = getCharset(model);
                propertiesViewBeanURL += "?ServiceName=" + serviceName +
                    "&type=" + Locale.URLEncodeField(type, charset) +
                    "&realm=" + Locale.URLEncodeField(
                        stringToHex(realm), charset) +
                    "&User=" +
                    Locale.URLEncodeField(stringToHex(universalId), charset) +
                    "&Op=" + AMAdminConstants.OPERATION_ADD;
                HttpServletResponse response =
                    getRequestContext().getResponse();
                response.sendRedirect(propertiesViewBeanURL);
            } catch (UnsupportedEncodingException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
                forwardTo();
            } catch (IOException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
                forwardTo();
            }
        } else {
            ServicesAddViewBean vb = (ServicesAddViewBean)getViewBean(
                ServicesAddViewBean.class);
            setPageSessionAttribute(ServicesAddViewBean.SERVICE_NAME,
                serviceName);
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        }
    }

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.editentities.selectservice";
    }

    protected boolean startPageTrail() {
        return false;
    }

}

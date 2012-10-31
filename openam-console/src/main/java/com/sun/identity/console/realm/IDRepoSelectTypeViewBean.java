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
 * $Id: IDRepoSelectTypeViewBean.java,v 1.2 2008/06/25 05:43:11 qcheng Exp $
 *
 */

package com.sun.identity.console.realm;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.realm.model.IDRepoModel;
import com.sun.identity.console.realm.model.IDRepoModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCRadioButton;
import com.sun.web.ui.view.html.CCTextField;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import javax.servlet.http.HttpServletRequest;


public class IDRepoSelectTypeViewBean
    extends AMPrimaryMastHeadViewBean
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/realm/IDRepoSelectType.jsp";
    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    private static final String ATTR_IDREPO_TYPE = "tfIDRepoType";

    private CCPageTitleModel ptModel;
    private AMPropertySheetModel propertySheetModel;

    /**
     * Creates a view to prompt user for ID Repo type before  creation.
     */
    public IDRepoSelectTypeViewBean() {
        super("IDRepoSelectType");
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
        registerChild(IDRepoModel.TF_NAME, CCTextField.class);
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
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.back");
        ptModel.setValue("button2", "button.next");
        ptModel.setValue("button3", "button.cancel");
    }

    private void createPropertyModel() {
        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/propertyRMIDRepoSelectType.xml"));
        propertySheetModel.clear();
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        return new IDRepoModelImpl(req, getPageSessionAttributes());
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);
        disableButton("button1", true);

        try {
            IDRepoModel model = (IDRepoModel)getModel();
            OptionList optList = model.getIDRepoTypes();

            if (optList.size() > 0) {
                CCRadioButton menu = (CCRadioButton)getChild(ATTR_IDREPO_TYPE);
                menu.setOptions(optList);
                String type = (String)menu.getValue();

                if ((type == null) || (type.trim().length() == 0)) {
                    menu.setValue(optList.getValue(0));
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
        IDRepoViewBean vb = (IDRepoViewBean)getViewBean(IDRepoViewBean.class);
        backTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Handles next button request.
     *
     * @param event Request invocation event.
     */
    public void handleButton2Request(RequestInvocationEvent event)
        throws ModelControlException {
        IDRepoModel model = (IDRepoModel)getModel();
        String idRepoName = (String)propertySheetModel.getValue(
            IDRepoModel.TF_NAME);
        idRepoName = idRepoName.trim();
        String idRepoType = (String)propertySheetModel.getValue(
            ATTR_IDREPO_TYPE);

        if (idRepoName.length() > 0) {
            IDRepoAddViewBean vb = (IDRepoAddViewBean)getViewBean(
                IDRepoAddViewBean.class);
            setPageSessionAttribute(IDRepoAddViewBean.IDREPO_NAME, idRepoName);
            setPageSessionAttribute(IDRepoAddViewBean.IDREPO_TYPE, idRepoType);
            unlockPageTrailForSwapping();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } else {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                "idrepo.missing.idRepoName");
            forwardTo();
        }
    }

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.realm.idrepo.selectIdRepo";
    }

    protected boolean startPageTrail() {
        return false;
    }
}

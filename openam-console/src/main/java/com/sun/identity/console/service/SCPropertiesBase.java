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
 * $Id: SCPropertiesBase.java,v 1.2 2008/06/25 05:43:16 qcheng Exp $
 *
 */

package com.sun.identity.console.service;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.service.model.SCModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.model.CCPageTitleModel;
import java.util.Map;

/**
 * This class can be used as the default base for classes which use a 
 * property sheet for their display. The extending class needs to only
 * define the jsp, the name of the property sheet xml file, the name 
 * of the tab currently being displayed, and return the model.
 */
public abstract class SCPropertiesBase
    extends AMPrimaryMastHeadViewBean
{
    private  static final String PGTITLE_TWO_BTNS = "pageTitle";
    private CCPageTitleModel ptModel;

    protected static final String PROPERTY_ATTRIBUTE = "propertyAttributes";

    protected AMPropertySheetModel propertySheetModel;

    /**
     * Creates a authentication domains view bean.
     */
    public SCPropertiesBase(String name, String displayURL) {
        super(name);
        setDefaultDisplayURL(displayURL);
    }

    protected void initialize() {
        if (!initialized) {
            super.initialize();
            initialized = true;
            createPageTitleModel();
            createPropertyModel();
            registerChildren();
        }
    }

    protected View createChild(String name) {
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
        SCModel model = (SCModel)getModel();
        
        if (model  != null) {
            AMPropertySheet ps = 
                (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
            propertySheetModel.clear();
            setAttributeValuesToPropertyModel(ps, model);
        }
    }

    protected void setAttributeValuesToPropertyModel(
        AMPropertySheet ps,
        SCModel model
    ) {
        try {
            ps.setAttributeValues(model.getValues(), model);
        } catch (AMConsoleException a) {
            setInlineAlertMessage(CCAlert.TYPE_WARNING,
                "message.warning", "noproperties.message");
        }
    }

    protected void registerChildren() {
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        propertySheetModel.registerChildren(this);
        ptModel.registerChildren(this);
        super.registerChildren();
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));

        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", getBackButtonLabel("page.title.config"));
    }

    protected void createPropertyModel() {
        propertySheetModel = new AMPropertySheetModel(getPropertySheet());
        propertySheetModel.clear();
    }

    protected abstract String getPropertySheet();

    /**
     * Handles reset request.
     *   
     * @param event Request invocation event
     */  
    public void handleButton2Request(RequestInvocationEvent event) {
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
            SCModel model = (SCModel)getModel();
            Map modifiedValues = getValues();
            onBeforeSaveProfile(modifiedValues);
            model.setValues(modifiedValues);
            setInlineAlertMessage(CCAlert.TYPE_INFO,
                "message.information", "message.updated");
            forwardTo();
        } catch (AMConsoleException a) {
            setInlineAlertMessage(
                CCAlert.TYPE_ERROR, "message.error", a.getMessage());
            forwardTo();
        }
    }

    protected Map getValues()
        throws ModelControlException, AMConsoleException
    {
        SCModel model = (SCModel)getModel();
        Map values = null;
        Map orig = model.getValues();
        AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
        // pass true to get only the values that have been modified
        return ps.getAttributeValues(orig, true, model);
    }

    /**
     * Dervived class can overwrite this method to alter attribute values
     * before are the saved.
     *
     * @return true to proceed with saving attribute values.
     */
    protected boolean onBeforeSaveProfile(Map attrValues) {
        return true;
    }
}

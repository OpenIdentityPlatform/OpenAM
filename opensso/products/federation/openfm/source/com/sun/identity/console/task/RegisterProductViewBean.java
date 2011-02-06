/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: RegisterProductViewBean.java,v 1.2 2008/06/25 05:49:48 qcheng Exp $
 *
 */

package com.sun.identity.console.task;

import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.ChildContentDisplayEvent;
import com.iplanet.jato.view.event.DisplayEvent;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.task.model.TaskModel;
import com.sun.identity.console.task.model.TaskModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.html.CCDropDownMenu;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import javax.servlet.http.HttpServletRequest;

import com.sun.scn.client.comm.RegistrationWrapper;
import java.util.List;

/**
 * Create register product UI.
 */
public class RegisterProductViewBean
    extends AMPrimaryMastHeadViewBean
{
    private static final String TAG_TABLE =
        "<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" title=\"\">";

    public static final String DEFAULT_DISPLAY_URL =
        "/console/task/RegisterProduct.jsp";
    private static final String PAGETITLE = "pgtitle";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    
    private CCPageTitleModel ptModel;
    private AMPropertySheetModel propertySheetModel;

    private RegistrationWrapper regWrapper = null;
    private static final String REGISTRATOR_ID = "FederatedAccessManager";
    
    public RegisterProductViewBean() {
        super("RegisterProduct");
        regWrapper = new RegistrationWrapper(REGISTRATOR_ID);
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        createPageTitleModel();
        createPropertyModel();
        registerChildren();
    }

    protected void registerChildren() {
        ptModel.registerChildren(this);
        propertySheetModel.registerChildren(this);
        registerChild(PAGETITLE, CCPageTitle.class);
        super.registerChildren();
    }
    
    protected View createChild(String name) {
        View view = null;
        
        if (name.equals(PAGETITLE)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else if (name.equals(PROPERTY_ATTRIBUTE)) {
            view = new AMPropertySheet(this, propertySheetModel, name);
        } else if (propertySheetModel.isChildSupported(name)) {
            view = propertySheetModel.createChild(this, name, getModel());
        } else {
            view = super.createChild(name);
        }
        
        return view;
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/twoBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.register");
        ptModel.setValue("button2", "button.cancel");
    }
    
    private void createPropertyModel() {
        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/propertyRegisterProduct.xml"));
        propertySheetModel.clear();
    }
    
    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new TaskModelImpl(req, getPageSessionAttributes());
        
    }

    public void beginDisplay(DisplayEvent e) {
        String value = (String)getDisplayFieldValue("radioAcctOption");
        if ((value == null) || (value.length() == 0)) {
            setDisplayFieldValue("radioAcctOption", "acct");
        }
        List countries = regWrapper.getAvailableCountries();
        CCDropDownMenu cb = (CCDropDownMenu)getChild("tfCountry");
        cb.setOptions(createOptionList(countries));
    }
    
    public String endPropertyAttributesDisplay(
        ChildContentDisplayEvent event
    ) {
        String html = event.getContent();
        int idx = html.indexOf("tfExistUserName");
        idx = html.lastIndexOf("<table", idx);
        html = html.substring(0, idx) + "<div id=\"existAcct\">" +
            html.substring(idx);
        
        idx = html.indexOf("tfDomain");
        idx = html.indexOf("<tr>", idx);
        html = html.substring(0, idx) + 
            "</table></div><div id=\"newAcct\" style=\"display:none\">" +
            TAG_TABLE +
            html.substring(idx);
        
        idx = html.indexOf("tfCountry");
        idx = html.indexOf("</table>", idx);
        html = html.substring(0, idx+8) + "</div>" + html.substring(idx+8);

        idx = html.indexOf("tfDomain");
        idx = html.lastIndexOf("<div ", idx);
        idx = html.lastIndexOf("<div ", idx -5);
        html = html.substring(0, idx+4) + " id=\"domainLabel\"" +
            html.substring(idx+4);
        return html;
    }
}

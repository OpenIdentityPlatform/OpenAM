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
 * $Id: CreateRemoteIDPViewBean.java,v 1.2 2008/06/25 05:49:47 qcheng Exp $
 *
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package com.sun.identity.console.task;

import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.ChildContentDisplayEvent;
import com.iplanet.jato.view.event.DisplayEvent;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.task.model.TaskModel;
import com.sun.identity.console.task.model.TaskModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCDropDownMenu;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.util.Set;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Create remote identity provider UI.
 */
public class CreateRemoteIDPViewBean
    extends AMPrimaryMastHeadViewBean
{
    private static final String TAG_TABLE =
        "<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" title=\"\">";

    public static final String DEFAULT_DISPLAY_URL =
        "/console/task/CreateRemoteIDP.jsp";
    private static final String PAGETITLE = "pgtitle";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    private static final String REALM = "tfRealm";
    private static final String RADIO_META  = "radioMeta";
    private static final String COT = "tfCOT";
    private static final String COT_CHOICE = "choiceCOT";
    private static final String SELECT_COT  = "radioCOT";
    
    private CCPageTitleModel ptModel;
    private AMPropertySheetModel propertySheetModel;
    
    public CreateRemoteIDPViewBean() {
        super("CreateRemoteIDP");
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
        ptModel.setValue("button1", "button.configure");
        ptModel.setValue("button2", "button.cancel");
    }
    
    private void createPropertyModel() {
        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/propertyCreateRemoteIDP.xml"));
        propertySheetModel.clear();
    }
    
    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new TaskModelImpl(req, getPageSessionAttributes());
        
    }

    public void beginDisplay(DisplayEvent e) {
        HttpServletRequest req = getRequestContext().getRequest();
        String cot = req.getParameter("cot");
        if ((cot != null) && (cot.trim().length() > 0)) {
            setDisplayFieldValue(COT, cot);
            setDisplayFieldValue(COT_CHOICE, cot);
        }
        String realm = req.getParameter("realm");
        if ((realm != null) && (realm.trim().length() > 0)) {
            setDisplayFieldValue(REALM, realm);
        }

        String value = (String)getDisplayFieldValue(RADIO_META);
        if ((value == null) || value.equals("")){
            setDisplayFieldValue(RADIO_META, "url");
        }
        
        value = (String)getDisplayFieldValue(SELECT_COT);
        if ((value == null) || value.equals("")){
            setDisplayFieldValue(SELECT_COT, "no");
        }
        
        try {
            TaskModel model = (TaskModel)getModel();
            Set realms = model.getRealms();
            CCDropDownMenu menuRealm = (CCDropDownMenu)getChild(REALM);
            menuRealm.setOptions(createOptionList(realms));
        } catch (AMConsoleException ex) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                ex.getMessage());
        }
    }
    
    public String endPropertyAttributesDisplay(
        ChildContentDisplayEvent event
    ) {
        String html = event.getContent();
        int idx = html.indexOf("tfMetadataFile\"");
        idx = html.lastIndexOf("<input ", idx);
        html = html.substring(0, idx) +
            "<span id=\"metadatafilename\"></span>" +
            html.substring(idx);
        
        idx = html.indexOf("CreateRemoteIDP.tfMetadataFileURL");
        idx = html.indexOf("<div ", idx);
        html = html.substring(0, idx+5) + "id=\"metadataurlhelp\" " +
            html.substring(idx+5);
        
        idx = html.indexOf("tfRealm");
        idx = html.lastIndexOf("<div ", idx);
        html = html.substring(0, idx+5) + "id=\"realmfld\" " +
            html.substring(idx+5);
        idx = html.lastIndexOf("<div ", idx-10);
        html = html.substring(0, idx+5) + "id=\"realmlbl\" " +
            html.substring(idx + 5);
        idx = html.indexOf("radioCOT");
        idx = html.lastIndexOf("<table ", idx);
        idx = html.lastIndexOf("<table ", idx - 5);
        html = html.substring(0, idx) +
            "<div id=\"cotsection\" style=\"display:none\">" +
            html.substring(idx);

        idx = html.indexOf("radioCOT");
        idx = html.lastIndexOf("<tr>", idx);
        html = html.substring(0, idx) + "</table>" +
            "<div id=\"cotq\" style=\"display:none\">" +
            TAG_TABLE + html.substring(idx);

        idx = html.indexOf("choiceCOT");
        idx = html.lastIndexOf("<tr>", idx);
        html = html.substring(0, idx) + "</table></div>" +
            "<div id=\"cotchoice\" style=\"display:none\">" +
            TAG_TABLE + html.substring(idx);

        idx = html.indexOf("tfCOT");
        idx = html.lastIndexOf("<tr>", idx);
        html = html.substring(0, idx) + "</table></div>" +
            "<div id=\"cottf\" style=\"display:none\">" +
            TAG_TABLE + html.substring(idx);
        idx = html.indexOf("</table>", idx);
        idx = html.indexOf("</table>", idx + 4);
        html = html.substring(0, idx + 8) + "</div></div>" +
            html.substring(idx + 8);
        return html;
    }
}

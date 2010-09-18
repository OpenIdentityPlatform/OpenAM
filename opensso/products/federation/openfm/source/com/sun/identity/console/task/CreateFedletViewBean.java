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
 * $Id: CreateFedletViewBean.java,v 1.4 2008/07/24 18:16:55 veiming Exp $
 *
 */

package com.sun.identity.console.task;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.ChildContentDisplayEvent;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.task.model.TaskModel;
import com.sun.identity.console.task.model.TaskModelImpl;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCDropDownMenu;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.servlet.http.HttpServletRequest;

/**
 * Create Fedlet UI.
 */
public class CreateFedletViewBean
    extends AMPrimaryMastHeadViewBean
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/task/CreateFedlet.jsp";
    private static final String PAGETITLE = "pgtitle";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    private static final String REALM = "tfRealm";
    private static final String COT_CHOICE = "choiceCOT";
    
    private CCPageTitleModel ptModel;
    private CCActionTableModel tableModel;
    private AMPropertySheetModel propertySheetModel;
    
    
    public void forwardTo(RequestContext rc) {
        boolean hasIdp = false;

        try {
            TaskModel model = (TaskModel) getModel();
            Set realms = model.getRealms();
            for (Iterator i = realms.iterator(); i.hasNext() && !hasIdp; ) {
                String realm = (String)i.next();
                Set cots = model.getCircleOfTrusts(realm);

                for (Iterator j = cots.iterator(); j.hasNext() && !hasIdp; ) {
                    String cot = (String)j.next();
                    Set idps = model.getHostedIDP(realm, cot);
                    hasIdp = !idps.isEmpty();
                }
            }
        } catch (AMConsoleException ex) {
            hasIdp = false;
        }
        
        if (hasIdp) {
            super.forwardTo(rc);
        } else {
            CreateFedletWarningViewBean vb = (CreateFedletWarningViewBean)
                getViewBean(CreateFedletWarningViewBean.class);
            vb.forwardTo(rc);
        }
    }
    
    public CreateFedletViewBean() {
        super("CreateFedlet");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        createPageTitleModel();
        createPropertyModel();
        createAttrMappingTable();
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
        ptModel.setValue("button1", "button.create");
        ptModel.setValue("button2", "button.cancel");
    }
    
    private void createPropertyModel() {
        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/propertyCreateFedlet.xml"));
        propertySheetModel.clear();
    }

    private void createAttrMappingTable() {
        tableModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/attributesMappingTable.xml"));
        tableModel.setTitleLabel("");
        tableModel.setActionValue("deleteAttrMappingBtn",
            "configure.provider.attributesmapping.delete.button");
        tableModel.setActionValue("NameColumn",
            "configure.provider.attributesmapping.column.name");
        tableModel.setActionValue("AssertionColumn",
            "configure.provider.attributesmapping.column.assertion");
        propertySheetModel.setModel("tblattrmapping", tableModel);
    }

    private void populateTableModel() {
        tableModel.clearAll();
        tableModel.setValue("NameValue", "");
        tableModel.setValue("AssertionValue", "");
    }
    
    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new TaskModelImpl(req, getPageSessionAttributes());
    }

    public void beginDisplay(DisplayEvent e) {
        HttpServletRequest req = getRequestContext().getRequest();

        populateTableModel();

        Set userAttrNames = AMAdminUtils.getUserAttributeNames();
        userAttrNames.remove("iplanet-am-user-account-life");
        CCDropDownMenu menuUserAttribute = (CCDropDownMenu) getChild(
            "menuUserAttributes");
        OptionList optList = createOptionList(userAttrNames);
        optList.add(0, "name.attribute.mapping.select", "");
        menuUserAttribute.setOptions(optList);

        try {
            TaskModel model = (TaskModel)getModel();
            Map map = model.getRealmCotWithHostedIDPs();
            Set realms = new TreeSet();
            realms.addAll(map.keySet());
            CCDropDownMenu menuRealm = (CCDropDownMenu)getChild(REALM);
            menuRealm.setOptions(createOptionList(realms));
            
            String realm = req.getParameter("realm");
            if ((realm != null) && (realm.trim().length() > 0)) {
                setDisplayFieldValue(REALM, realm);
            } else {
                if (!realms.isEmpty()) {
                    realm = (String)realms.iterator().next(); 
                }
            }
            if ((realm != null) && (realm.trim().length() > 0)) {
                Map mapCots = (Map)map.get(realm);
                Set cots = new TreeSet();
                cots.addAll(mapCots.keySet());
                CCDropDownMenu menuCOT = (CCDropDownMenu)getChild("choiceCOT");
                menuCOT.setOptions(createOptionList(cots));
                
                String cot = req.getParameter("cot");
                if ((cot != null) && (cot.trim().length() > 0)) {
                    setDisplayFieldValue("choiceCOT", cot);
                } else {
                    if (!cots.isEmpty()) {
                        cot = (String)cots.iterator().next();
                    }
                }
                
                if ((cot != null) && (cot.trim().length() > 0)) {
                    Set idps = new TreeSet();
                    idps.addAll((Set)mapCots.get(cot));
                    CCDropDownMenu menuIDP = (CCDropDownMenu)getChild(
                        "choiceIDP");
                    menuIDP.setOptions(createOptionList(idps));
                    
                    String idp = req.getParameter("entityId");
                    if ((idp != null) && (idp.trim().length() > 0)) {
                        setDisplayFieldValue("choiceIDP", idp);
                    }
                }
            }
        } catch (AMConsoleException ex) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                ex.getMessage());
        }

        String cot = req.getParameter("cot");
        if ((cot != null) && (cot.trim().length() > 0)) {
            setDisplayFieldValue(COT_CHOICE, cot);
        }
    }
    
    public String endPropertyAttributesDisplay(
        ChildContentDisplayEvent event
    ) {
        String html = event.getContent();
        int idx = html.indexOf("tfRealm");
        idx = html.lastIndexOf("<div ", idx);
        html = html.substring(0, idx+5) + "id=\"realmfld\" " +
            html.substring(idx+5);
        idx = html.lastIndexOf("<div ", idx-10);
        html = html.substring(0, idx+5) + "id=\"realmlbl\" " +
            html.substring(idx + 5);
        idx = html.indexOf("</td>", idx);
        idx = html.indexOf("</td>", idx +1);
        html = html.substring(0, idx) +
            "<div class=\"ConTblCl1Div\" id=\"realmtxt\"></div>" +
            html.substring(idx);
        
        idx = html.indexOf("choiceCOT");
        idx = html.lastIndexOf("<div ", idx);
        html = html.substring(0, idx+5) + "id=\"cotfld\" " +
            html.substring(idx+5);
        idx = html.lastIndexOf("<div ", idx-10);
        html = html.substring(0, idx+5) + "id=\"cotlbl\" " +
            html.substring(idx + 5);
        idx = html.indexOf("</td>", idx);
        idx = html.indexOf("</td>", idx +1);
        html = html.substring(0, idx) +
            "<div class=\"ConTblCl1Div\" id=\"cottxt\"></div>" +
            html.substring(idx);

        idx = html.indexOf("choiceIDP");
        idx = html.lastIndexOf("<div ", idx);
        html = html.substring(0, idx+5) + "id=\"idpfld\" " +
            html.substring(idx+5);
        idx = html.lastIndexOf("<div ", idx-10);
        html = html.substring(0, idx+5) + "id=\"idplbl\" " +
            html.substring(idx + 5);
        idx = html.indexOf("</td>", idx);
        idx = html.indexOf("</td>", idx +1);
        html = html.substring(0, idx) +
            "<div class=\"ConTblCl1Div\" id=\"idptxt\"></div>" +
            html.substring(idx);
        idx = html.indexOf("id=\"realmlbl\"");

        html = removeSortHref(html);
        return html;
    }
    
    static String removeSortHref(String html) {
        int idx = html.indexOf("deleteAttrMappingBtn");
        idx = html.lastIndexOf("</caption>", idx);
        int idx1 = html.lastIndexOf("&#160;", idx);
        html = html.substring(0, idx1) + html.substring(idx);
        
        idx = html.indexOf("SortHref=");        
        while (idx != -1) {
            idx = html.lastIndexOf("<a", idx);
            idx1 = html.indexOf(">", idx);
            int idx2 = html.indexOf("</a>", idx);
            html = html.substring(0, idx) + "<b>" + 
                html.substring(idx1+1, idx2) + "</b>" + html.substring(idx2+4);
            idx = html.indexOf("SortHref=");
        }
        
        idx = html.indexOf("sort_up_sel.gif");
        idx = html.lastIndexOf("<img", idx);
        idx1 = html.indexOf("/>", idx);
        html = html.substring(0, idx) + html.substring(idx1+2);
        
        idx = html.indexOf("tfAttrMappingAssertion");
        idx = html.lastIndexOf("<input", idx);
        html = html.substring(0, idx) + 
            "<table border=0 cellpadding=2 cellspacing=0><tr><td>" +
            html.substring(idx);
        
        idx = html.indexOf("tfAttrMappingName");
        idx = html.lastIndexOf("<input", idx);
        html = html.substring(0, idx) + 
            "</td><td>" +
            html.substring(idx);

        idx = html.indexOf("btnAddAttrMapping");
        idx = html.lastIndexOf("<input", idx);
        html = html.substring(0, idx) + 
            "</td><td>" +
            html.substring(idx);

        idx = html.indexOf("menuUserAttributes");
        idx = html.lastIndexOf("<select ", idx);
        html = html.substring(0, idx) + 
            "</td></tr><tr><td></td><td colspan=2>" +
            html.substring(idx);
        idx = html.indexOf("</div>", idx);
        html = html.substring(0, idx+6) + 
            "</td></tr></table>" +
            html.substring(idx+6);
        
        
        return html;
    }
}

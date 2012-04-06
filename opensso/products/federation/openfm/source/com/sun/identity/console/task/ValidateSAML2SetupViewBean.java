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
 * $Id: ValidateSAML2SetupViewBean.java,v 1.5 2008/08/21 04:37:46 veiming Exp $
 *
 */

package com.sun.identity.console.task;

import com.iplanet.jato.model.ModelControlException;
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
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCDropDownMenu;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import javax.servlet.http.HttpServletRequest;

/**
 * Validate Entity Setup.
 */
public class ValidateSAML2SetupViewBean
    extends AMPrimaryMastHeadViewBean
{
    private static final String TAG_TABLE =
        "<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" title=\"\">";
    public static final String DEFAULT_DISPLAY_URL =
        "/console/task/ValidateSAML2Setup.jsp";
    private static final String PAGETITLE = "pgtitle";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";

    private static final String REALM = "tfRealm";
    
    private CCPageTitleModel ptModel;
    private CCActionTableModel tableModel;
    private AMPropertySheetModel propertySheetModel;
    private boolean bOneRealm;
    private Set setCOTs;
    
    public ValidateSAML2SetupViewBean() {
        super("ValidateSAML2Setup");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        createPageTitleModel();
        createPropertyModel();
        createCOTTable();
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
        ptModel.setValue("button1", "button.validate");
        ptModel.setValue("button2", "button.cancel");
    }
    
    private void createPropertyModel() {
        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/propertyValidateSAML2Setup.xml"));
        propertySheetModel.clear();
    }

    private void createCOTTable() {
        tableModel = new CCActionTableModel (
            getClass ().getClassLoader ().getResourceAsStream (
            "com/sun/identity/console/validateCotTable.xml"));
        tableModel.setTitleLabel ("label.items");
        tableModel.setActionValue ("NameColumn",
            "validate.entities.circle.of.trust.tbl.column.name");
        tableModel.setActionValue ("HostedIDPColumn",
            "validate.entities.circle.of.trust.tbl.column.hostedidp");
        tableModel.setActionValue ("RemoteIDPColumn",
            "validate.entities.circle.of.trust.tbl.column.remoteidp");
        tableModel.setActionValue ("HostedSPColumn",
            "validate.entities.circle.of.trust.tbl.column.hostedsp");
        tableModel.setActionValue ("RemoteSPColumn",
            "validate.entities.circle.of.trust.tbl.column.remotesp");
        propertySheetModel.setModel("tblcots", tableModel);
    }
 
    private void populateTableModel() {
        tableModel.clearAll();
        TaskModel model = (TaskModel) getModel();
        int invalid = 0;

        try {
            String realm = getRequestContext().getRequest().getParameter(
                "realm");
            if ((realm == null) || (realm.length() == 0)) {
                realm = "/";
            }

            CCDropDownMenu menu = (CCDropDownMenu)getChild("tfRealm");
            Set realms = model.getRealms();
            menu.setOptions(createOptionList(realms));
            menu.setValue(realm);
            bOneRealm = (realms.size() == 1);
            
            Set cots = model.getCircleOfTrusts(realm);
            setCOTs = new TreeSet();
            setCOTs.addAll(cots);
            int counter = 0;
            for (Iterator i = setCOTs.iterator(); i.hasNext(); ) {
                String cotName = (String)i.next();
                if (counter > 0) {
                    tableModel.appendRow();
                }
                counter++;
                int nHostedIDP = model.getHostedIDP(realm, cotName).size();
                int nRemoteIDP = model.getRemoteIDP(realm, cotName).size();
                int nHostedSP = model.getHostedSP(realm, cotName).size();
                int nRemoteSP = model.getRemoteSP(realm, cotName).size();
                
                boolean valid = ((nHostedIDP > 0) && (nRemoteSP > 0)) ||
                    ((nRemoteIDP > 0) && (nHostedSP > 0));
                tableModel.setValue("NameValue", cotName);

                tableModel.setSelectionVisible(valid);
                
                if (!valid) {
                    invalid++;
                }
                tableModel.setValue("HostedIDPValue",
                    Integer.toString(nHostedIDP));
                tableModel.setValue("RemoteIDPValue",
                    Integer.toString(nRemoteIDP));
                tableModel.setValue("HostedSPValue",
                    Integer.toString(nHostedSP));
                tableModel.setValue("RemoteSPValue",
                    Integer.toString(nRemoteSP));
            }
        } catch (AMConsoleException ex) {
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                ex.getMessage());
        }

        if ((setCOTs.size() - invalid) == 0) {
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                "no.providers.to.validate");
        }
    }

    public String endPropertyAttributesDisplay(
        ChildContentDisplayEvent event
    ) {
        String html = event.getContent();
        
        if (bOneRealm) {
            int idx = html.indexOf("tfRealm");
            idx = html.lastIndexOf("<tr>", idx);
            idx = html.indexOf("<td ", idx);
            idx = html.indexOf(">", idx);
            html = html.substring(0, idx+1) + 
                "<div id=\"divRealmLabel\" style=\"display:none\">" +
                html.substring(idx+1);
            idx = html.indexOf("</td>", idx);
            html = html.substring(0, idx) + "</div>" + 
                html.substring(idx);

            idx = html.indexOf("<td ", idx);
            idx = html.indexOf(">", idx);
            html = html.substring(0, idx+1) + 
                "<div id=\"divRealm\" style=\"display:none\">" +
                html.substring(idx+1);
            idx = html.indexOf("</td>", idx);
            html = html.substring(0, idx) + "</div>" + 
                html.substring(idx);
        }
        
        int idx = html.indexOf("tfCOT");
        idx = html.lastIndexOf("<tr>", idx);
        idx = html.indexOf("<td ", idx);
        idx = html.indexOf(">", idx);
        html = html.substring(0, idx+1) +
                "<div id=\"divCOTLabel\" style=\"display:none\">" +
            html.substring(idx+1);
        idx = html.indexOf("</td>", idx);
        html = html.substring(0, idx) + "</div>" + 
            html.substring(idx);

        idx = html.indexOf("<td ", idx);
        idx = html.indexOf(">", idx);
        html = html.substring(0, idx + 1) + 
            "<div id=\"divCOT\" style=\"display:none\">" +
            html.substring(idx + 1);
        idx = html.indexOf("</td>", idx);
        html = html.substring(0, idx) + "</div>" + 
            html.substring(idx);

        TaskModel model = (TaskModel)getModel();
        
        idx = html.indexOf("tfCOT");
        idx = html.lastIndexOf("<input ", idx);
        html = html.substring(0, idx) +
            "<span class=\"ConTblCl1Div\" id=\"txtCot\"></span>&#160;" +
            "<a id=\"linkShowCOTTable\" href=\"javascript:showCOTTable()\"" +
            " style=\"display:none\">" +
            model.getLocalizedString("validate.cot.table.show") +
            "</a>" +
            "<a id=\"linkHideCOTTable\" href=\"javascript:hideCOTTable()\"" +
            " style=\"display:none\">" +
            model.getLocalizedString("validate.cot.table.hide") +
            "</a>" +
            html.substring(idx);
        
        idx = html.indexOf("<table ", idx);
        int idx1 = html.indexOf("</table>", idx);
        html = html.substring(0, idx) + html.substring(idx1+9);

        idx = html.indexOf("<div class=\"ConFldSetLgdDiv\">");
        if (idx == -1) {
            idx = html.indexOf("<legend class=\"ConFldSetLgd\"");
            idx = html.lastIndexOf("<fieldset ", idx);
            html = html.substring(0, idx) + "<div id=\"divCOTTable\">" +
                html.substring(idx);
            idx = html.indexOf("</fieldset>", idx);
            html = html.substring(0, idx+11) + "</div>" +
                html.substring(idx+11);
        } else {
            idx = html.lastIndexOf("<div ", idx-3);
            html = html.substring(0, idx+5) + "id=\"divCOTTable\" " +
                html.substring(idx+5);
        }
        
        idx = html.indexOf("tfIDP");
        idx = html.lastIndexOf("<img ", idx);
        idx = html.lastIndexOf("<table ", idx-2);
        html = html.substring(0, idx) + 
            "<div id=\"divEntities\" style=\"display:none\">" + 
            html.substring(idx);
        idx = html.indexOf("</table>", idx);
        idx = html.indexOf("</table>", idx+3);
        html = html.substring(0, idx+8) + "</div>" +
            html.substring(idx+8);
        
        idx = html.indexOf("id=\"divCOTTable\"");
        idx = html.lastIndexOf(">", idx);
        
        
        if ((setCOTs != null) && !setCOTs.isEmpty()) {
            StringBuffer buff = new StringBuffer();
            buff.append("\n<script language=\"Javascript\">\n");
            buff.append("cots = new Array();\n");
            int cnt = 0;
            for (Iterator i = setCOTs.iterator(); i.hasNext(); cnt++) {
                String cotName = (String)i.next();
                buff.append("cots[")
                    .append(Integer.toString(cnt))
                    .append("] = \"")
                    .append(cotName)
                    .append("\";\n");
            }
            buff.append("</script>\n");
            html += buff.toString();
        }

        html = addQuestionImages(html);
        return html;
    }
    
    private String addQuestionImages(String html) {
        int idx = html.indexOf("TblColHdrSel");
        if (idx != -1) {
            idx = html.lastIndexOf("</caption>", idx);
            int idx1 = html.lastIndexOf("&#160;", idx);
            html = html.substring(0, idx1) + html.substring(idx);

            int endIdx = html.indexOf("</table>", idx);
            idx = html.indexOf("<tr>", idx1);
            while (idx < endIdx) {
                idx = html.indexOf("<td ", idx);
                idx1 = html.indexOf("<input ", idx);
                int endTD = html.indexOf("</td>", idx);
                if ((idx1 == -1) || (idx1 > endTD)) {
                    idx = html.indexOf(">", idx);
                    html = html.substring(0, idx) +
                        " valign=\"middle\">" +
                        "<img src=\"../console/images/question.gif\" width=\"16\" " +
                        "height=\"16\" border=\"0\" " +
                        "onmouseover=\"showCannotValidateDiv(event)\" " +
                        "onmouseout=\"hideCannotValidateDiv()\" />" +
                        html.substring(idx + 1);
                }
                idx = html.indexOf("<tr>", idx);
            }
        }
        return html;
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new TaskModelImpl(req, getPageSessionAttributes());
        
    }

    public void beginDisplay(DisplayEvent e) 
        throws ModelControlException {
        super.beginDisplay(e);
        populateTableModel();
        this.disableButton("button1", true);
    }
}

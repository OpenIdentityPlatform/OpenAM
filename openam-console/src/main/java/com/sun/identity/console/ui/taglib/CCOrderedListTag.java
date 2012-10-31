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
 * $Id: CCOrderedListTag.java,v 1.1 2008/07/02 17:21:46 veiming Exp $
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.ui.taglib;

import com.iplanet.jato.view.View;
import com.sun.identity.console.ui.model.CCOrderedListModel;
import com.sun.identity.console.ui.view.CCOrderedList;
import com.sun.web.ui.common.CCJavascript;
import com.sun.web.ui.common.CCStyle;
import com.sun.web.ui.model.CCEditableListModelInterface;
import com.sun.web.ui.model.CCOrderedListModelBaseInterface;
import com.sun.web.ui.taglib.html.CCButtonTag;
import com.sun.web.ui.taglib.html.CCTextFieldTag;
import com.sun.web.ui.taglib.orderablelist.CCOrderableListTag;
import com.sun.web.ui.view.html.CCButton;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;

/**
 * This is the corresponding tag for <code>CCOrderedListView</code>.
 */
public class CCOrderedListTag extends CCOrderableListTag  {
    private CCTextFieldTag textfieldTag;
    private CCButtonTag addButtonTag;
    private CCButtonTag deleteButtonTag;
    
    private View textfield;
    private CCButton addButton;
    private CCButton deleteButton;

    private String addBtnString;
    private String deleteBtnString;
    private String textfieldString;

    public void reset() {
        super.reset();
        addButtonTag = null;
        deleteButtonTag = null;
        textfieldTag = null;
    }
    
    protected String getHTMLStringInternal(
        Tag parent, 
        PageContext pageContext,
        View view
    ) throws JspException {
        CCOrderedList field = (CCOrderedList)
            super.init(parent, pageContext, view, CCOrderedList.class);
        initChildViews(field);
        String strHTML = super.getHTMLStringInternal(parent, pageContext, view);
        includeJavascriptFile(CCJavascript.EDITABLELIST_JS);
        int idx = strHTML.lastIndexOf("class=\"Btn2Dis\"");
        idx = strHTML.indexOf("/>", idx) + 2;
        strHTML = strHTML.substring(0, idx) + 
            "\n<div class=\"" + CCStyle.ADDREMOVE_HORIZONTAL_BETWEEN + "\">\n" +
            deleteBtnString + "\n</div>" +
            strHTML.substring(idx);
        strHTML += getJavascript(view);
        
        idx = strHTML.indexOf("handleSelectedOnChange();");
        int idx2 = strHTML.lastIndexOf("javascript", idx);
        idx = strHTML.indexOf("return", idx);
        
        String js = strHTML.substring(idx2, idx);
        js = js.replaceAll("CCOrderableList_", "Editable_");
        
        strHTML = strHTML.substring(0, idx) + js + strHTML.substring(idx);
        strHTML = swapNameSpace(strHTML, "removeFromList();");
       
        idx = strHTML.lastIndexOf("handleReload();");
        idx2 = strHTML.lastIndexOf("Editable_", idx);
        
        js = strHTML.substring(idx2, idx);
        js += "selectedHiddenText = document.forms['" + getFormName() + 
            "'].elements['" + jsNameQualifier + ".SelectedTextField'];";
        strHTML = strHTML.substring(0, idx+15) + js + strHTML.substring(idx+15); 
        
        textfieldString = textfieldTag.getHTMLString(parent, pageContext,
            textfield);
        idx = strHTML.lastIndexOf("</tr>");
        strHTML = strHTML.substring(0, idx +5) + 
            "<tr><td>&nbsp;</td><td colspan=\"2\">" +
            "<table title=\"\" border=\"0\" cellspacing=\"0\" " +
            "cellpadding=\"0\" class=\"" + CCStyle.EDITABLELIST_ADD_TABLE +
            "\"><tr><td>" + textfieldString + "&nbsp;&nbsp;" +
            addBtnString + "</div></td></tr></table></td></tr>" +
            strHTML.substring(idx +5);
        strHTML = swapNameSpace(strHTML, "addToList();");
        return strHTML;
    }
    
    private String swapNameSpace(String strHTML, String target) {
        int idx = strHTML.indexOf(target);
        idx = strHTML.lastIndexOf("CCOrderableList_", idx);
        strHTML = strHTML.substring(0, idx) + "Editable_" + 
            strHTML.substring(idx + 16);
        idx = strHTML.indexOf(");", idx);
        
        strHTML = strHTML.substring(0, idx +2) +
            "javascript: CCOrderableList_" + jsNameQualifier.replace('.', '_') + 
            ".handleSelectedOnChange();" +
            strHTML.substring(idx +2);
        return strHTML;
    }
    
    private String getJavascript(View view) {
        StringBuilder buffer = new StringBuilder();
        
        String jsName = "Editable_" +
            view.getQualifiedName().replace('.', '_').replace('-', '_');

            buffer.append("\n<script type=\"text/javascript\">var ")
            .append(jsName)
            .append(" = new CCEditableList('")
            .append(jsNameQualifier)
            .append("', ")
            .append(selectedListboxVar)
            .append(", '")
            .append(getFormName())
            .append("', '")
            .append(CCOrderedListModelBaseInterface.SEPARATOR)
            .append("', '")
            .append(CCEditableListModelInterface.DEFAULT_OPTION_VALUE)
            .append("', '")
            .append(model.getDefaultOptionLabel())
            .append("', '1');</script>\n");

        buffer.append("<script type=\"text/javascript\">")
            .append(jsName)
            .append(".handleReload();</script>");
        return buffer.toString();
    }
    
    protected void initButtons() {
        super.initButtons();
        CCOrderedListModel m = (CCOrderedListModel)model;
        addButton.setDisplayLabel(m.getAddButtonLabel());
        deleteButton.setDisplayLabel(m.getDeleteButtonLabel());
    }
    
    protected void initTags() {
        super.initTags();
        textfieldTag = new CCTextFieldTag();
        textfieldTag.setSize("50");
        addButtonTag = new CCButtonTag();
        deleteButtonTag = new CCButtonTag();
        deleteButtonTag.setDynamic("true");
        deleteButtonTag.setDisabled("true");
    }

    protected void initOnEventMethods() {
        super.initOnEventMethods();

        StringBuffer b = new StringBuffer();
        String beginScript = JAVASCRIPT_KEYWORD + jsObjectName + ".";
        String endScript = "; return false;";

        b.append(beginScript).append("removeFromList()").append(endScript);
        deleteButtonTag.setOnClick(b.toString());

        b = new StringBuffer();
        b.append(beginScript).append("addToList()").append(endScript);
        addButtonTag.setOnClick(b.toString());
    }

    protected void initChildViews(CCOrderedList field) {
        // register common ordered list children
        super.initChildViews(field);
        textfield = field.getChild(CCOrderedList.TEXTFIELD);
        addButton = (CCButton)field.getChild(CCOrderedList.ADD_BUTTON);
        deleteButton = (CCButton)field.getChild(CCOrderedList.DELETE_BUTTON);
    }

    protected void initHtmlVars(Tag parent, PageContext pageContext, View view)
        throws JspException {
        super.initHtmlVars(parent, pageContext, view);

        addBtnString = addButtonTag.getHTMLString(
            parent, pageContext, addButton);
        deleteBtnString = deleteButtonTag.getHTMLString(
            parent, pageContext, deleteButton);
    }
}

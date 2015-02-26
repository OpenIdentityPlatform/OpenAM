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
 * $Id: CCMapListTag.java,v 1.2 2008/07/28 23:43:36 veiming Exp $
 */

package com.sun.identity.console.ui.taglib;

import com.iplanet.jato.view.View;
import com.sun.identity.console.ui.model.CCMapListModel;
import com.sun.identity.console.ui.view.CCMapList;
import com.sun.web.ui.taglib.editablelist.CCEditableListTag;
import com.sun.web.ui.taglib.html.CCTextFieldTag;
import com.sun.web.ui.view.editablelist.CCEditableList;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;

/**
 * This is the corresponding tag for <code>CCMapListView</code>.
 */
public class CCMapListTag extends CCEditableListTag  {
    private CCMapListModel model;
    private CCTextFieldTag valueTextfieldTag;
    private View valueTextfield;
    private String valueTextfieldString;
    
    protected View init(
        Tag parent,
        PageContext pageContext,
        View view,
        Class c
    ) throws JspException {
        View baseView = super.init(parent, pageContext, view, c);
        CCMapList field = (CCMapList)view;
        model = (CCMapListModel)field.getModel();
        return baseView;
    }
    
    public void reset() {
        super.reset();
        valueTextfieldTag = null;
    }

    protected String getHTMLStringInternal(
        Tag parent,
        PageContext pageContext,
        View view
    ) throws JspException {
        String strHTML = super.getHTMLStringInternal(parent, pageContext, view);
        int idx = strHTML.lastIndexOf(".textField");
        idx = strHTML.lastIndexOf("<input type=", idx);
        
        int idxBtnStart = strHTML.indexOf("<input ", idx+5);
        int idxBtnEnd = strHTML.indexOf("/>", idxBtnStart) +2;
        
        valueTextfieldString = valueTextfieldTag.getHTMLString(parent,
            pageContext, valueTextfield);
        strHTML = strHTML.substring(0, idx) +
            "<table border=0 cellpadding=0 cellspacing=0>" +
            "<tr><td>" +
            "<table border=0 cellpadding=0 cellspacing=0>" +
            "<tr><td><span class=\"LblLev2Txt\">" + model.getKeyLabel() + 
            "</span></td>" +
            "<td><span class=\"LblLev2Txt\">" + model.getValueLabel() + 
            "</span></td></tr>" + "<tr><td>" +
            strHTML.substring(idx, idxBtnStart) + "</td><td>" +
            valueTextfieldString + "</td></tr></table>" +
            "</td><td>" +
            "<table border=0 cellpadding=0 cellspacing=0>" +
            "<tr><td><span class=\"LblLev2Txt\">&nbsp;</span></td></tr>" +
            "<tr><td>&nbsp;&nbsp;" + 
            strHTML.substring(idxBtnStart, idxBtnEnd) + "</td></tr></table>" +
            "</td></tr></table>" + strHTML.substring(idxBtnEnd);
        
        idx = strHTML.indexOf("new CCEditableList(");
        idx = strHTML.indexOf(");", idx);
        String extraParam = (isGlobal()) ? ", '1', '1'" : ", '1'";
        strHTML = strHTML.substring(0, idx) + extraParam +
            strHTML.substring(idx);
        
        idx = strHTML.indexOf("new CCEditableList(");
        idx = strHTML.indexOf(");", idx) +2;
        strHTML = strHTML.substring(0, idx) + 
            "var msgMapListInvalidEntry=\"" + model.getMsgInvalidEntry() + 
            "\"; " +
            "var msgMapListInvalidKey=\"" + model.getMsgInvalidKey() + "\"; " +
            "var msgMapListInvalidValue=\"" + model.getMsgInvalidValue() + 
                "\"; " +
            "var msgMapListInvalidNoKey=\"" + model.getMsgMapListInvalidNoKey()+
                "\"; " +
            strHTML.substring(idx);
        
        return strHTML;
    }
    
    protected void initTags() {
        super.initTags();
        valueTextfieldTag = new CCTextFieldTag();
    }

    protected void initChildViews(CCEditableList field) {
        // register common ordered list children
        super.initChildViews(field);
        valueTextfield = field.getChild(CCMapList.VALUE_TEXTFIELD);
    }

    /**
     * Returns <code>true</code> for global map.
     *
     * @return <code>true</code> for global map.
     */
    protected boolean isGlobal() {
        return false;
    }
}

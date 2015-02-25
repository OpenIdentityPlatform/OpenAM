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
 * $Id: CCUnOrderedListTag.java,v 1.1 2008/07/02 17:21:46 veiming Exp $
 */

package com.sun.identity.console.ui.taglib;

import com.iplanet.jato.view.View;
import com.sun.web.ui.taglib.editablelist.CCEditableListTag;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;

/**
 * This is the corresponding tag for <code>CCUnOrderedListView</code>.
 */
public class CCUnOrderedListTag extends CCEditableListTag  {
    protected String getHTMLStringInternal(
        Tag parent,
        PageContext pageContext,
        View view
    ) throws JspException {
        String strHTML = super.getHTMLStringInternal(parent, pageContext, view);
        int idx = strHTML.indexOf("new CCEditableList(");
        idx = strHTML.indexOf(");", idx);
        strHTML = strHTML.substring(0, idx) + ", '1'" + strHTML.substring(idx);
        return strHTML;
    }
}

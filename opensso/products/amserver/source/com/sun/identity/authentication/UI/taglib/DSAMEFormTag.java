/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: DSAMEFormTag.java,v 1.2 2008/06/25 05:41:50 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted 2011-2012 ForgeRock AS
 */
package com.sun.identity.authentication.UI.taglib;

import javax.servlet.jsp.JspException;
import com.iplanet.jato.taglib.html.FormTag;
import com.iplanet.jato.util.NonSyncStringBuffer;
import com.sun.identity.authentication.UI.AuthViewBeanBase;
import com.iplanet.jato.view.ViewBean;

import java.util.Map;
import java.util.Iterator;

/**
 * Form tag extends form JATO Form tag.  It adds a content encoding
 * hidden field element.  This element helps web server to figure
 * out the encoding used.
 */
public class DSAMEFormTag  extends FormTag 
{
    /** constructs a form tag */
    public DSAMEFormTag() {
        super();
    }

    /**
     * performs start tag
     *
     * @return EVAL_BODY_INCLUDE always
     * @throws JspException if request context is null
     */
    public int doStartTag()
        throws JspException
    {
        reset();

        Map m = getValueMap();
        
        NonSyncStringBuffer buffer =
            new NonSyncStringBuffer("<form ");
        for (Iterator it=m.keySet().iterator(); it.hasNext();) {
            String key= (String) it.next();
            String val = (String) m.get(key);
            buffer.append ("  "+key+"=\""+val+"\"");
        }
        buffer.append( ">");

        writeOutput(buffer);

        return EVAL_BODY_INCLUDE;
    }

    public int doEndTag () 
        throws JspException {
        ViewBean vb = getParentViewBean();
        NonSyncStringBuffer buffer =new NonSyncStringBuffer();
        buffer.append( "<input type=\"hidden\" name=\"");
        buffer.append(AuthViewBeanBase.PAGE_ENCODING)
            .append("\" value=\"")
            .append(vb.getDisplayFieldValue("gx_charset"))
            .append("\"/>")
            .append("</form>");
        writeOutput(buffer);
        return EVAL_PAGE;
    }

    public void setName(String name) {
        setValue("name",name);        
    }

    public void setAction(String name) {
        setValue("action",name);
        }

    public void setTilePrefix (String name) throws JspException  {
        ViewBean vb = getParentViewBean();
        if (vb instanceof AuthViewBeanBase) {
            AuthViewBeanBase ab = (AuthViewBeanBase) vb;
            setValue("name", name+ab.getTileIndex());
        } else {
            setValue ("name", name);
        }

    }
    
    public void setDefaultCommandChild (String name)   { 
        try {
        ViewBean vb = getParentViewBean();

        String value = (String) vb.getDisplayFieldValue(name);
        setValue ("action", value);
        } catch (JspException ex) {
            setValue ("action",name);
        }
    }
    

}

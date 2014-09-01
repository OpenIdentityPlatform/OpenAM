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
 * $Id: DSAMEValueTag.java,v 1.2 2008/06/25 05:41:50 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.UI.taglib;

import javax.servlet.jsp.JspException;
import com.iplanet.jato.taglib.DisplayFieldTagBase;
import com.iplanet.jato.ViewBeanManager;

import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.BodyContent;

import com.sun.identity.authentication.UI.LoginViewBean;

/**
 * Value tag reimplements JATO Body tag.
 * This tag is used to get the 'value' of any 'key' 
 * (which is a public variable) defined in LoginViewBean.
 */
public class DSAMEValueTag extends DisplayFieldTagBase implements BodyTag {
    private BodyContent bodyContent=null;
    
    /** property declaration for tag attribute: key.
     */    
    private String key = "";    
    
    /** Constructs a value tag */
    public DSAMEValueTag() {
        super();
    }
    
    /**
     * reset tag
     */
    public void reset() {
        super.reset();
        bodyContent = null;
    }
    
    /**
     * Performs start tag
     *
     * @return EVAL_BODY_INCLUDE always
     * @throws JspException if request context is null
     */
    public int doStartTag()
    throws JspException {
        reset();
                
        key = (String) getValue("key");
        String value = "" ;
        try {
            ViewBeanManager viewBeanManager =
                getRequestContext().getViewBeanManager();
            LoginViewBean vb = (LoginViewBean) viewBeanManager.getViewBean(
            com.sun.identity.authentication.UI.LoginViewBean.class);
            //ViewBean viewBean = getParentViewBean();
            //LoginViewBean vb = (LoginViewBean) viewBean;
            value = (String) vb.getDisplayFieldValue(key);
            setValue("key", value);
        } catch (Exception ex) {
            setValue("key", key);
        }
        writeOutput(value);
        return SKIP_BODY;
    }
    
    
    /**
     * does nothing here
     */
    public void doInitBody()
    throws JspException {
    }
    
    /**
     * does nothing here
     */
    public int doAfterBody()
    throws JspException {
        return SKIP_BODY;
    }
    
    /**
     * does nothing here
     *
     *
     */
    public int doEndTag()
    throws JspException {
        return SKIP_BODY;
    }
    
    public BodyContent getBodyContent() {
        return bodyContent;
    }
    
    public void setBodyContent(BodyContent value) {
        bodyContent = value;
    }
            
    public void setKey(String value) {
        setValue("key", value);
    }
    
    public String getKey() {
        return key;
    }
}

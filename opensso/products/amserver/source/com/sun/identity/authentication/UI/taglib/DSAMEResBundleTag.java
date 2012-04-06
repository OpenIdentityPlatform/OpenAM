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
 * $Id: DSAMEResBundleTag.java,v 1.3 2008/06/25 05:41:50 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.UI.taglib;

import com.iplanet.jato.CompleteRequestException;
import com.iplanet.jato.taglib.DisplayFieldTagBase;
import com.iplanet.jato.util.NonSyncStringBuffer;
import com.iplanet.jato.view.ViewBean;
import com.sun.identity.authentication.UI.AuthViewBeanBase;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTag;

/**
 * Href tag reimplements JATO Href tag.  It adds a content encoding
 * query parameter and do not do extra encoding.  This element helps
 * web server to figure out the encoding used.
 */
public class DSAMEResBundleTag
    extends DisplayFieldTagBase
    implements BodyTag
{
    private BodyContent bodyContent=null;
    private NonSyncStringBuffer buffer=null;
    private boolean displayed=false;
    private CompleteRequestException abortedException;


    /** constructs a href tag */
    public DSAMEResBundleTag() {
        super();
    }

    /**
     * reset tag 
     */
    public void reset() {
        super.reset();
        bodyContent = null;
        buffer = null;
        displayed = false;
        abortedException = null;
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
        ViewBean vb = getParentViewBean();
        java.util.Locale locale ;
        if (vb instanceof AuthViewBeanBase) {
            AuthViewBeanBase ab = (AuthViewBeanBase) vb;
            locale = ab.getRequestLocale();
        } else {
            locale = java.util.Locale.getDefault();
        }
        String rbName =(String) getValue ("BundleName");
        String resKey = (String) getValue ("ResourceKey");
        String resValue ;
        ResourceBundle rb = AMResourceBundleCache.getInstance().getResBundle(
            rbName, locale);
        try {
            resValue = rb.getString (resKey);
        } catch (MissingResourceException ex) {
            resValue = resKey;
        }
        writeOutput(resValue);
        return SKIP_BODY;
    }


    /**
     * does nothing here
     */
    public void doInitBody()
        throws JspException
    {
    }

    /**
     * does nothing here
     */
    public int doAfterBody()
        throws JspException
    {
        return SKIP_BODY;
    }

    /**
     * does nothing here
     *
     * 
     */
    public int doEndTag()
        throws JspException
    {
        return SKIP_BODY;
    }

    public BodyContent getBodyContent() {
        return bodyContent;
    }

    public void setBodyContent(BodyContent value) {
        bodyContent = value;
    }

    public void setBundleName(String name) {

        setValue ("BundleName", name);
    }

    public void setResourceKey (String name) {
        setValue ("ResourceKey", name);
    }
}

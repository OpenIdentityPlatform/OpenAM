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
 * $Id: DSAMEHrefTag.java,v 1.3 2008/06/25 05:41:50 qcheng Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 * Portions Copyrighted 2012 Open Source Solution Technology Corporation
 */
package com.sun.identity.authentication.UI.taglib;

import com.iplanet.jato.CompleteRequestException;
import com.iplanet.jato.taglib.DisplayFieldTagBase;
import com.iplanet.jato.util.NonSyncStringBuffer;
import com.iplanet.jato.view.ContainerView;
import com.iplanet.jato.view.DisplayField;
import com.iplanet.jato.view.ViewBean;
import com.iplanet.jato.view.ViewBeanBase;
import com.sun.identity.authentication.UI.AuthViewBeanBase;
import com.sun.identity.shared.debug.Debug;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTag;

import org.owasp.esapi.ESAPI;

/**
 * Href tag reimplements JATO Href tag.  It adds a content encoding
 * query parameter and do not do extra encoding.  This element helps
 * web server to figure out the encoding used.
 */
public class DSAMEHrefTag
extends DisplayFieldTagBase
implements BodyTag {
    private BodyContent bodyContent=null;
    private NonSyncStringBuffer buffer=null;
    private boolean displayed=false;
    private CompleteRequestException abortedException;
    static Debug debug ;
    static {
        debug = Debug.getInstance("amTagLib");
    }
    
    /** constructs a href tag */
    public DSAMEHrefTag() {
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
     * @return EVAL_BODY_BUFFERED always
     * @throws JspException if request context is null
     */
    public int doStartTag()
    throws JspException {
        reset();
        
        try {
            if (fireBeginDisplayEvent()) {
                ViewBean viewBean = getParentViewBean();
                AuthViewBeanBase dsameVB = (AuthViewBeanBase) viewBean;
                String value = (String)viewBean.getDisplayFieldValue(getName());
                buffer = new NonSyncStringBuffer("<a href=\"");
                String pgEncoding = (String) dsameVB.getDisplayFieldValue(AuthViewBeanBase.PAGE_ENCODING);

                NonSyncStringBuffer url = new NonSyncStringBuffer(value);
                url.append(value.contains("?") ? '&' : '?');
                url.append(AuthViewBeanBase.PAGE_ENCODING).append('=').append(pgEncoding);
                // Append the Query String NVP's that might have been added as JSP tag attributes
                appendQueryParams(url);
                if (getAnchor()!=null) {
                    url.append("#").append(getAnchor());
                }
                buffer.append(ESAPI.encoder().encodeForHTMLAttribute(url.toString())).append('\"');
                
                if (getTarget() != null) {
                    buffer.append(" target=\"")
                    .append(getTarget())
                    .append("\"");
                }
                
                if (getTitle() != null) {
                    buffer.append(" title=\"")
                    .append(getTitle())
                    .append("\"");
                }
                
                // Append the additional "standard" attributes
                appendCommonHtmlAttributes(buffer);
                appendJavaScriptAttributes(buffer);
                appendStyleAttributes(buffer);
                
                buffer.append(">");
                displayed = true;
            } else {
                displayed = false;
            }
        } catch (CompleteRequestException e) {
            // CompleteRequestException tunneling workaround:
            // Workaround to allow developers to stop the request
            // from a display event by throwing a CompleteRequestException.
            // The problem is that some containers catch this exception in
            // their JSP rendering subsystem, and so we need to tunnel it
            // through for the developer.
            // Save the exception here to rethrow later (in doEndTag)
            abortedException = e;
            return SKIP_BODY;
        }
        
        if (displayed) {
            return EVAL_BODY_BUFFERED;
        } else {
            return SKIP_BODY;
        }
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
     * does end tag
     *
     * @return SKIP_PAGE if tag is not going to be displayed
     */
    public int doEndTag()
    throws JspException {
        try {
            // If the display was aborted during the beginning of the process by
            // a CompleteRequestException, we retrhow that exception here,
            // because this is the only location we can safely return SKIP_PAGE
            
            if (abortedException != null) {
                throw abortedException;
            }
            
            if (displayed) {
                BodyContent bodyContent = getBodyContent();
                
                if (bodyContent != null) {
                    // Assume that "true" is default for trim
                    if (getTrim() == null || isTrue(getTrim())) {
                        buffer.append(bodyContent.getString().trim());
                    } else {
                        buffer.append(bodyContent.getString());
                    }
                }
                
                buffer.append("</a>");
                writeOutput(fireEndDisplayEvent(buffer.toString()));
            }
        } catch (CompleteRequestException e) {
            // CompleteRequestException tunneling workaround:
            // Workaround to allow developers to stop the request
            // from a display event by throwing a CompleteRequestException.
            // The problem is that some containers catch this exception in
            // their JSP rendering subsystem, and so we need to tunnel it
            // through for the developer.
            
            // Mark the JSP rendering as cancelled.  The calling
            // ViewBean.foward() or ViewBean.include() methods
            // should pick this up and then throw a complete request
            // exception that was properly thrown here.
            getRequestContext().getRequest().setAttribute(
            ViewBeanBase.DISPLAY_EVENT_COMPLETED_REQUEST_ATTRIBUTE_NAME, e);
            return SKIP_PAGE;
        }
        
        return EVAL_PAGE;
    }
    
    public BodyContent getBodyContent() {
        return bodyContent;
    }
    
    public void setBodyContent(BodyContent value) {
        bodyContent = value;
    }
    
    /**
     * Appends the Query String name/value pairs (NVP) that have been supplied
     * via the JSP tag attribute "queryParams".
     * NOTE - this tag assumes that the JSP author has URL encoded the value
     * portions of the name value pairs where needed.
     * This tag also prepends an ampersand '&' before the first NVP, and assumes
     * that the JSP author has provided the '&' delimiters between the remaining
     * NVPs.
     *
     */
    protected void appendQueryParams(NonSyncStringBuffer buffer)
    throws JspException {
        String nvPairs = getQueryParams();
        
        if ((nvPairs != null) && (nvPairs.length() > 0)) {
            buffer.append("&amp;");
            buffer.append(nvPairs);
        }
    }
    
    
    
    
    /**
     * takes an arbitrarily deep namePath (e.g. "Page1.Repeated2.Foo") which
     * describes the containment relationship of a given display field to
     * its container views, and returns a reference to the display field itself.
     *
     * @param namePath - An arbitrarily deep namePath (e.g.
     * "Page1.Repeated2.Foo") which describes the containment relationship of
     * a display field to its container views
     * @return requested DisplayField
     */
    private DisplayField getDisplayField(String[] namePath)
    throws JspException {
        // We can assume that the source class must be the parent view bean !!!
        // The same is not true for the target though.
        ContainerView parentView=getParentViewBean();
        
        /*
         * Descend through view hierarchy until you get to the direct parent
         * field the direct parent may be a tiled view, arbitrarily nested
         * start count at one,
         */
        for (int j = 1; j < namePath.length-1; j++) {
            parentView = (ContainerView)parentView.getChild(namePath[j]);
        }
        
        return (DisplayField) parentView.getChild(namePath[namePath.length-1]);
    }
    
    public String getTarget() {
        return (String)getValue("target");
    }
    
    public void setTarget(String value) {
        setValue("target",value);
    }
    
    public String getTitle() {
        return (String)getValue("title");
    }
    
    public void setTitle(String value) {
        setValue("title",value);
    }
    
    public String getAnchor() {
        return (String)getValue("anchor");
    }
    
    public void setAnchor(String value) {
        setValue("anchor",value);
    }
    
    public String getQueryParams() {
        return (String)getValue("queryParams");
    }
    
    public void setQueryParams(String value) {
        setValue("queryParams",value);
    }
    
    public String getTrim() {
        return (String)getValue("trim");
    }
    
    public void setTrim(String value) {
        setValue("trim",value);
    }
}

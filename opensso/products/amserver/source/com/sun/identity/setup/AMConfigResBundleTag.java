/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AMConfigResBundleTag.java,v 1.3 2008/06/25 05:44:01 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.setup;

import com.iplanet.am.util.Locale;
import java.io.UnsupportedEncodingException;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/*
 * This tag handler class is used to load resource bundle
 * for the jsp for configurator UI based on user's preferred locale
 * and store the object in page scope for use by "message" tag.
 * 
 * Also this tag handler class sets the request and response charset
 * to UTF-8 to make sure non-ascii characters are displayed correctly.
 */
public class AMConfigResBundleTag 
        extends TagSupport {

    private String bundleName = null;
    private java.util.Locale configLocale = null;
    private ResourceBundle rb = null;
    private static String CONFIG_RESBUNDLE = "configurator_resbundle";
    private HttpServletRequest req = null;
    private HttpServletResponse res = null;
    private static String UTF_8 = "UTF-8";
    
    /** constructs a tag */
    public AMConfigResBundleTag() {
	super();
    }

    /**
     * performs start tag
     *
     * @throws JspException if request context is null
     */
    public int doStartTag() throws JspException {
        req = (HttpServletRequest)pageContext.getRequest();
	res = (HttpServletResponse)pageContext.getResponse();

        setLocale(req);
        try {
            req.setCharacterEncoding(UTF_8);
	    res.setContentType("text/html; charset="+UTF_8);
        } catch (UnsupportedEncodingException uee) {
            //Do nothing.
        }
        
        try {
            if (configLocale != null) {        
                rb = ResourceBundle.getBundle(getBundleName(), configLocale);
            } else {
                rb = ResourceBundle.getBundle(getBundleName());
            }
        } catch (MissingResourceException mre) {
            // Do nothing
        }
        //Store the resource bundle object in pageContext for other tags to use.
        pageContext.setAttribute(CONFIG_RESBUNDLE, rb);
        
	return SKIP_BODY;
    }

    /*
     * does nothing here
     * 
     */
    public int doEndTag() throws JspException {
	return SKIP_BODY;
    }

    public void setBundleName(String name) {
	this.bundleName = name;
    }
    
    public String getBundleName () {
        return bundleName;
    }

    private void setLocale (HttpServletRequest request) {      
        if (request != null) {
            String superLocale = request.getParameter("locale");

            if (superLocale != null && superLocale.length() > 0) {
		configLocale = new java.util.Locale(superLocale);
            } else {
		String acceptLangHeader =
                          (String)request.getHeader("Accept-Language");
		if ((acceptLangHeader !=  null) &&
                     (acceptLangHeader.length() > 0)) {
                    String acclocale = 
                        Locale.getLocaleStringFromAcceptLangHeader(
                            acceptLangHeader);
		    configLocale = new java.util.Locale(acclocale);
		}
            }
       }
    }

    public java.util.Locale getConfigLocale () {
        return configLocale;
    }
 
}

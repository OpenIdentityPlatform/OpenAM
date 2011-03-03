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
 * $Id: AMConfigMsgTag.java,v 1.4 2008/06/25 05:44:01 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.setup;

import java.io.IOException;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.util.List;
import java.util.ArrayList;
import java.text.MessageFormat;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;


/*
 * This tag handler class is used to display localized
 * error message according to user's preferred locale. 
 * It handles both simple and complex message formatting.
 *
 * For ex., the following message is a complex one which might need
 * formatting based on the locale.
 *
 * "Please check the server logs : " + basedir + deployuri + "/debug"
 *
 * In some locale, it might be something like this with localized message.
 * "Please check"+ basedir + deployuri + "/debug for server logs."
 * 
 * "message" tag retrieves the localized message using the resource bundle
 * loaded by "resBundle" tag. "param" tag is used to pass the variable parts 
 * of a message to message tag for formatting.
 *
 * In the above case the value (basedir + deployuri + "/debug") is
 * passed to message tag handler class with the index.
 * In amConfigurator.properties the string will be defined as follows:
 * 
 * amConfigurator.properties -> configurator.log=Please check the server
 * logs : {0}
 * amConfigurator_<locale> -> configurator.log=Please check {0} for server
 * logs. 
 */
public class AMConfigMsgTag 
        extends BodyTagSupport {

    private String bundleName = null;
    private String i18nKey = null;
    private String patterntype = null;
    private String pattern = null;
    private static String CONFIG_RESBUNDLE = "configurator_resbundle";
    private static String MESSAGE_PATTERN = "message";
    private List argList = null;
    private ResourceBundle rb = null;

    /* constructs a tag */
    public AMConfigMsgTag() {
	super();
        argList = new ArrayList (5);
    }
    
    /*
     * performs start tag
     *
     * @throws JspException if request context is null
     */
    public int doStartTag() throws JspException {
        rb = (ResourceBundle)pageContext.getAttribute(
            CONFIG_RESBUNDLE);
        
	try {
            if (getPatterntype() != null && 
                getPatterntype().equals(MESSAGE_PATTERN)) 
            {
                MessageFormat msgformat = new MessageFormat("");
                msgformat.setLocale(rb.getLocale());
                try {
                    pattern = rb.getString(getI18nKey());
                    msgformat.applyPattern(pattern);
                } catch (MissingResourceException mre) {
                    pattern = getI18nKey();
                }
            }
	} catch (Exception ex) {
	    //May not need this.
	}
       
	return EVAL_BODY_INCLUDE;
    }

    /*
     * does nothing here
     * 
     */
    public int doEndTag() throws JspException {
        String resValue;
        if (argList.size() > 0) {
            resValue = MessageFormat.format (pattern, argList.toArray());
        } else {
            try {
                resValue = rb.getString (getI18nKey());
            } catch (MissingResourceException mre) {
                resValue = getI18nKey();
            }
        }
        
        try {
            pageContext.getOut().print(resValue);
        } catch (IOException ie) {
            //Do appropriate error handling
            ie.printStackTrace();
        }
	return EVAL_PAGE;
    }

    public void setI18nKey (String name) {
	this.i18nKey = name;
    }
    
    public String getI18nKey () {
        return i18nKey;
    }
    
    public void setPatterntype (String patterntype) {
        this.patterntype = patterntype;
    }
    
    public String getPatterntype () {
        return patterntype;
    }
    
    /* 
     * Builds array with arguments. These arguments will be used to set 
     * the variable part of localizable messages from configurator properties
     * file.
     */
    public void addArgumentList (int index, String arg) {
        argList.add(index, arg);
    }
    
}

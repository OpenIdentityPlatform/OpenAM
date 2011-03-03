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
 * $Id: AMConfigParamTag.java,v 1.3 2008/06/25 05:44:01 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.setup;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/*
 * This is the tag handler class  for the tag "param" which is 
 * used to pass variables in localizable messages as
 * arguments to AMConfigMsgTag, which then 
 * formats the message according to preferred locale.
 * 

 * In the above case the value (basedir + deployuri + "/debug") is
 * passed to message tag handler class with the index.
 * In amConfigurator.properties the string will be defined as follows:
 *
 * Fpr the message "Please check the server logs : " + debugdir, the 
 * properties file will have the following entries. Note that the position
 * of the variable will change based on locale.
 * amConfigurator.properties -> configurator.log=Please check the server
 * logs : {0}
 * amConfigurator_<locale> -> configurator.log=Please check {0} for server
 * logs.

 */
public class AMConfigParamTag 
        extends TagSupport {

    private String index = null;
    private String arg = null;
    
    /* constructs a tag */
    public AMConfigParamTag() {
	super();
    }
    
    /*
     * performs start tag
     *
     * @throws JspException if request context is null
     */
    public int doStartTag() throws JspException {
        AMConfigMsgTag msgTag = (AMConfigMsgTag) findAncestorWithClass(this, 
            AMConfigMsgTag.class);
        if (msgTag == null) {
            //Log the error message and throw JspException
	    throw new JspException(
                "Param tag does not have a parent Message tag");
        }
        msgTag.addArgumentList ((new Integer (getIndex())).intValue(), 
            getArg());

	return SKIP_BODY;
    }

    /*
     * does nothing here
     * 
     */
    public int doEndTag() throws JspException {
    	return SKIP_BODY;
    }

    public void setIndex (String index) {
	this.index = index;
    }
    
    public String getIndex () {
        return index;
    }
    
    public void setArg (String arg) {
        this.arg = arg;
    }
    
    public String getArg () {
        return arg;
    }
    
}

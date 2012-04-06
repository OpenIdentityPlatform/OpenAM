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
 * $Id: PagePropertiesCallback.java,v 1.2 2008/06/25 05:42:06 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2011-2012 ForgeRock AS
 */

package com.sun.identity.authentication.spi;

import java.util.List;

import javax.security.auth.callback.Callback;

/**
 * <code>PagePropertiesCallback</code> class implements
 * <code>Callback</code> and used for exchanging all UI related attributes
 * information such as template name, <code>errorState</code> to indicate
 * whether a template is an error page, page header, image name , page timeout
 * value, name of module.
 *
 * @supported.all.api
 */
public class PagePropertiesCallback implements Callback {
    private String image=null;
    private int timeOut=60;
    private String templateName=null;
    private String moduleName=null;
    private String header=null;
    private boolean error=false;
    private List attribute;
    private List require;
    private List<String> infoText;
    private String page_state=null;
    
    /**
     * Creates a <code>PagePropertiesCallback</code> for a given module
     * name, header string, page image, page time out, JSP template name,
     * error state and page state.
     *
     * @param name Name of the authentication module.
     * @param header Header string for the authentication module display page.
     * @param image Image for the authentication module display page.
     * @param timeOut Time out value for the authentication module display page.
     * @param templateName JSP page name for the authentication module display.
     * @param error Error state for the authentication module.
     * @param page_state State of the authentication module display page.
     */
    public PagePropertiesCallback(
        String name,
        String header,
        String image,
        int timeOut,
        String templateName,
        boolean error,
        String page_state) {
        this.image = image;
        if (timeOut!=0) {
            this.timeOut = timeOut;
        }
        this.templateName = templateName;
        this.moduleName = name;
        this.header = header;
        this.error = error;
        this.page_state = page_state;
    }
    
    /**
     * Returns the authentication module display page image.
     *
     * @return the image for the authentication module display page.
     */
    public String getImage() {
        return image;
    }
    
    /**
     * Returns the authentication module display page time out value.
     *
     * @return the time out value for the authentication module display page.
     */
    public int getTimeOutValue() {
        return timeOut;
    }
    
    /**
     * Returns the authentication module display page state.
     *
     * @return the state for the authentication module display page.
     */
    public String getPageState() {
        return page_state;
    }
    
    /**
     * Returns the authentication module display page template name.
     *
     * @return the JSP page template of the authentication module display.
     */
    public String getTemplateName() {
        return templateName;
    }
    
    /**
     * Returns the authentication module name.
     *
     * @return the name of the authentication module.
     */
    public String getModuleName() {
        return moduleName;
    }
    
    /**
     * Returns the authentication module header string display.
     *
     * @return the header string display for the authentication module page.
     */
    public String getHeader() {
        return header;
    }
    
    /**
     * Returns the authentication module error state.
     *
     * @return the error state for the authentication module page.
     */
    public boolean getErrorState() {
        return error;
    }
    
    /**
     * Returns the list of authentication module data store specific attributes.
     *
     * @return the list of authentication module data store specific attributes.
     */
    public List getAttribute() {
        return attribute;
    }
    
    /**
     * Returns the list of authentication module display attributes which are
     * required to be entered by the end user.
     *
     * @return the list of authentication module display attributes which are
     * required to be entered by the end user.
     */
    public List getRequire() {
        return require;
    }
    
    /**
     * Returns the list of infoText elements to display alongside the authentication
     * module display attributes. 
     * @return the list of infoText elements
     */
    public List<String> getInfoText() {
        return infoText;
    }
    
    /**
     * Sets the authentication module header string display.
     *
     * @param header Header string display for the authentication module page.
     */
    public void setHeader(String header) {
        this.header = header;
    }
    
    /**
     * Sets the list of authentication module data store specific attributes.
     *
     * @param attribute the list of authentication module data store specific
     * attributes.
     */
    public void setAttribute(List attribute) {
        this.attribute = attribute;
    }
    
    /**
     * Sets the list of authentication module display attributes which are
     * required to be entered by the end user.
     *
     * @param require the list of authentication module display attributes which
     * are required to be entered by the end user.
     */
    public void setRequire(List require) {
        this.require = require;
    }
    
    public void setInfoText(List<String> infoText) {
        this.infoText = infoText;
    }
    
    /**
     * Sets the authentication module display page state.
     *
     * @param page_state the state for the authentication module display page.
     */
    public void setPageState(String page_state) {
        this.page_state = page_state;
    }
}

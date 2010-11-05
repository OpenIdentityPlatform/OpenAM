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
 * $Id: PagePropertiesCallback.java,v 1.2 2008/06/25 05:42:05 qcheng Exp $
 *
 */



package com.sun.identity.authentication.service;

import java.util.List;
import javax.security.auth.callback.Callback;

/**
 * This <code>PagePropertiesCallback</code> class implements
 * <code>Callback</code> and used for exchanging all UI related attributes
 * information such as template name, <code>errorState</code> to indicate
 * whether a template is an error page, page header, image name , page timeout
 * value, name of module.
 *
 * @deprecated This class has been deprecated in Access Manager 6.3 and will
 * not be available in the next Access Manager release. Use 
 * <code>com.sun.identity.authentication.spi.PagePropertiesCallback</code>
 * instead of this class.
 */
public class PagePropertiesCallback implements Callback {
    private String image=null;
    private int timeOut=60;
    private String templateName=null; 
    private String moduleName=null;
    private String header=null;
    private String headerTemplate=null;
    private boolean error=false;
    private List attribute;
    private List require;
    private String page_state=null;

    /**
     * Creates <code>PagePropertiesCallback</code> object.
     *
     * @param name
     * @param header
     * @param image
     * @param timeOut
     * @param templateName
     * @param error
     * @param page_state
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
        headerTemplate = header;
        this.error = error;
        this.page_state = page_state;
    }

    /**
     * Returns image
     * @return image
     */
    public String getImage() {
        return image;
    }

    /**
     * Returns timeout value
     * @return timeout value
     */
    public int getTimeOutValue() {
        return timeOut;
    }

    /**
     * Returns page state
     * @return page state
     */
    public String getPageState() {
        return page_state;
    }
    
    /**
     * Returns template name
     * @return template name
     */
    public String getTemplateName() {
        return templateName;
    }

    /**
     * Returns module name
     * @return module name
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * Returns header
     * @return header
     */
    public String getHeader() {
        return header;
    }

    /**
     * Returns header template
     * @return header template
     */
    public String getHeaderTemplate() {
        return headerTemplate;
    }

    /**
     * Returns error state
     * @return error state
     */
    public boolean getErrorState() {
        return error;
    }

    /**
     * Returns attribute
     * @return attribute
     */
    public List getAttribute() {
        return attribute;
    }

    /**
     * Returns list of require
     * @return list of require
     */
    public List getRequire() {
        return require;
    }

    /**
     * Sets header for page
     * @param header to be set
     */
    public void setHeader(String header) {
        this.header = header;
    }

    /**
     * Sets list of attribute
     * @param attribute list of attribute to be set
     */
    public void setAttribute(List attribute) {
        this.attribute = attribute;
    }

    /**
     * Sets list of require
     * @param require list of require to be set
     */
    public void setRequire(List require) {
        this.require = require;
    }
    
    /**
     * Sets page state
     * @param page_state page state to be set
     */
    public void setPageState(String page_state) {
        this.page_state = page_state;
    }
}

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
 * $Id: ServiceRequest.java,v 1.1 2008/11/22 02:19:54 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.core.service;

import java.io.Serializable;

/**
 * This class represents the format of the service request object.
 * This should be passed to service that accepts request and process it.
 * Clients must pass this request while executing the service.
 *
 */

public class ServiceRequest implements Serializable {
    private String command;
    private Object cmdSet = null;
    private Object reqData;
    
    public ServiceRequest() {
    }
    
    /**
     * Creates <code>ServiceRequest</code> with the given command and
     * request data.
     *
     * @param cmd command to be executed.
     * @param data contains input params to process the request.
     */
    public ServiceRequest(String cmd, Object data) {
        command = cmd;
        reqData = data;
    }
    
    /**
     * Returns the command of this request.
     *
     * @return command of this request.
     */
    public String getCommand() {
        return command;
    }
    
    /**
     * Returns the command set of this request.
     *
     * @return command set of this request.
     */
    public Object getCommandSet() {
        return cmdSet;
    }
    
    /**
     * Returns the input data for this request.
     *
     * @return Object containing the input data for this request.
     */
    public Object getData() {
        return reqData;
    }
    
    /**
     * Sets command for this service request.
     *
     * @param cmd command to be set for this request.
     */
    public void setCommand(String cmd) {
        command = cmd;
    }
    
    /**
     * Sets commands for this service request.
     *
     * @param cmdSet commands to be set for this request.
     */
    public void setCommand(Object cmdSet) {
        this.cmdSet = cmdSet;
    }
    
    /**
     * Sets the input data for this request.
     *
     * @param data input data for this request.
     */
    public void setData(Object data) {
        reqData = data;
    }
}

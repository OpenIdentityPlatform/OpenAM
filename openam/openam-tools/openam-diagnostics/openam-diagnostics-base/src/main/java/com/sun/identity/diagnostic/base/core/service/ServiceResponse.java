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
 * $Id: ServiceResponse.java,v 1.2 2009/11/13 21:54:35 ak138937 Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.diagnostic.base.core.service;

import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class represents the format of the service response object.
 * This should be passed to service that accepts request and process it.
 * Clients must pass this response while executing the service.
 * Upon successful execution, services place the result in this object.
 *
 */

public class ServiceResponse implements Serializable {
    private String status;
    private List result = null;
    private List warning = null;
    private List error = null;
    private List<String> msg = null;  
    private String opStr = "";
    private StringWriter strWriter = new StringWriter();
    private StringReader strReader = new StringReader(opStr);
    public static final String PASS = "msg-pass";
    public static final String FAIL = "msg-fail";
    
    /** Creates a new instance of ServiceResponse */
    public ServiceResponse() {
        result = new ArrayList();
        warning = new ArrayList();
        error = new ArrayList();
        msg = new ArrayList<String>();
    }
    
    /**
     * Creates new <code>ServiceResponse</code> with the given
     * status and result.
     *
     * @param status status of the performed service.
     * @param res result of the performed service.
     */
    public ServiceResponse(String status, Object res) {
        this.status = status;
        result = new ArrayList();
        result.add(res.toString());
    }
    
    /**
     * Sets the status of the service execution.
     *
     * @param status status of the executed service.
     */
    public void setStatus(String status) {
        this.status = status;
    }
    
    /**
     * Returns the status the executed service.
     *
     * @return status of the executed service.
     */
    public String getStatus() {
        return getStatusResult((ArrayList)this.msg);
    }
    
    /**
     * Sets the result of the service execution.
     *
     * @param result result of the executed service.
     */
    public void setResult(Object result) {
        this.result.add(result.toString());
    }
    
    /**
     * Returns the result of the executed service.
     *
     * @return result of the executed service.
     */
    public String getResult() {
        return toResponseStr(result);
    }

    /**
     * Returns the specific result of the executed service.
     *
     * @param idx Index of the result string.
     * @return specific result of the executed service.
     */
    public String getResult(int idx) {
        return (String)this.result.get(idx);
    }
    
    /**
     * Sets the message of the service execution.
     *
     * @param msg message of the executed service.
     */
    public void setMessage(Object msg) {
        this.msg.add(msg.toString());
    }
    
    /**
     * Returns the message of the executed service.
     *
     * @return message of the executed service.
     */
    public String getMessage() {
        return toResponseStr(msg);
    }
    
    /**
     * Sets the warning of the service execution.
     *
     * @param warning Warning of the executed service.
     */
    public void setWarning(Object warning) {
        this.warning.add(warning.toString());
    }
    
    /**
     * Returns the warning of the executed service.
     *
     * @return warning of the executed service.
     */
    public String getWarning() {
        return toResponseStr(warning);
    }
    
    /**
     * Sets the error of the service execution.
     *
     * @param error warning of the executed service.
     */
    public void setError(Object error) {
        this.error.add(error.toString());
    }
    
    /**
     * Returns the error of the executed service.
     *
     * @return error of the executed service.
     */
    public String getError() {
        return toResponseStr(error);
    }
    
    private String toResponseStr(List msg) {
        StringBuilder buff = new StringBuilder();
        for (Iterator i = msg.iterator(); i.hasNext(); ) {
            String response = (String)i.next();
            buff.append(response).append("\n");
        }
        return buff.toString();
    }
    
    private String getStatusResult(ArrayList inList){
        String status = PASS;
        for (int h=0; h<inList.size(); h++) {
            if (((String)inList.get(h)).toLowerCase().contains("failed")){
                status = FAIL;
                break;
            }
        }
        return status;
    }
}

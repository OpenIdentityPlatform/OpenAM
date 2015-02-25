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
 * $Id: FSAuthContextResult.java,v 1.2 2008/06/25 05:46:52 qcheng Exp $
 *
 */
package com.sun.identity.federation.services;


/**
 * This class is used to model authentication context result.
 */
public class FSAuthContextResult {
    
    private String loginURL;
    private String authContextRef;
    
    /**
     * Constructs new <code>FSAuthContextResult</code> object.
     */
    public FSAuthContextResult() {
        loginURL = null;
        authContextRef = null;
    }
    
    /**
     * Returns login url.
     * @return login url
     * @see #setLoginURL(String)
     */
    public String getLoginURL(){
        return loginURL;
    }
    
    /**
     * Returns authentication context reference.
     * @return authentication context reference string
     * @see #setAuthContextRef(String)
     */
    public String getAuthContextRef(){
        return authContextRef;
    }
    
    /**
     * Sets login url.
     * @param url login url to be set
     * @see #getLoginURL()
     */
    public void setLoginURL(String url){
        loginURL = url;
    }
    
    /**
     * Sets authentication context reference.
     * @param classRef authentication context reference string to be set
     * @see #getAuthContextRef()
     */
    public void setAuthContextRef(String classRef){
        authContextRef =  classRef;
    }
  
}

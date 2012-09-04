/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: COTConstants.java,v 1.3 2008/06/25 05:46:38 qcheng Exp $
 *
 */

package com.sun.identity.cot;

/**
 * This class defines constants used by circle of trust manager classes.
 */
public interface COTConstants {
    /**
     * Circle Of Trust Service Name.
     */
    String COT_SERVICE = "sunFMCOTConfigService";
    
    /**
     * Circle of Trust Service Version
     */
    String COT_SERVICE_VERSION = "1.0";
   
    /**
     * Circle of Trust Configuration Name.
     */
    static final String COT_CONFIG_NAME="LIBCOT";
    
    /**
     * Circle of Trust Description Attribute name.
     */
    static final String COT_DESC = "sun-fm-cot-description";
    
    /**
     * Circle of Trust Type  Attribute name.
     */
    static final String COT_TYPE = "sun-fm-cot-type";
    
    /**
     * Circle of Trust Status Attribute name.
     */
    static final String COT_STATUS = "sun-fm-cot-status";
    
    /**
     * Circle of Trust Writer Service URL Attribute name.
     */
    static final String COT_WRITER_SERVICE = "sun-fm-writerservice-url";
    
    /**
     * Circle of Trust Reader Service URL Attribute name.
     */
    static final String COT_READER_SERVICE = "sun-fm-readerservice-url";
    
    /**
     * IDFF Circle of Trust Writer Service URL Attribute name.
     */
    String COT_IDFF_WRITER_SERVICE = 
        "sun-fm-idff-writerservice-url";
    
    /**
     * IDFF Circle of Trust Reader Service URL Attribute name.
     */
    String COT_IDFF_READER_SERVICE = 
        "sun-fm-idff-readerservice-url";
    
    /**
     * SAMLv2 Circle of Trust Writer Service URL Attribute name.
     */
    String COT_SAML2_WRITER_SERVICE = 
        "sun-fm-saml2-writerservice-url";
    
    /**
     * SAMLv2 Circle of Trust Reader Service URL Attribute name.
     */
    String COT_SAML2_READER_SERVICE = 
        "sun-fm-saml2-readerservice-url";
    
    /**
     * Trusted Providers in a Circle of Trust Attribute name.
     */
    static final String COT_TRUSTED_PROVIDERS = "sun-fm-trusted-providers";
    
    /**
     * Active Status.
     */
    static final String ACTIVE= "active";
    
    /**
     * Inactive Status.
     */
    static final String INACTIVE = "inactive";
    
    /**
     * List of Circle of Trust Attribute name.
     */
    static final String COT_LIST = "cotlist";
    
    /**
     * ID-FF Protocol
     */
    public String IDFF = "idff";
    
    /**
     * SAML2 protocol
     */
    public String SAML2 = "saml2";
    
    /**
     * WS-Federation protocol
     */
    public String WS_FED = "wsfed";

    /**
     * delimiter 
     */
    public String DELIMITER = "|";
    
    /**
     * Circle of Trust Log Prefix
     */
    static final String COT = "COT";
    
    /**
     * Root Realm 
     */
    static final String ROOT_REALM = "/";

}

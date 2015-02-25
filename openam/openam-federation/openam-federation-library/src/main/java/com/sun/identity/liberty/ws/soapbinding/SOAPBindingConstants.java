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
 * $Id: SOAPBindingConstants.java,v 1.3 2008/06/25 05:47:23 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.soapbinding; 

/**
 * This class contains all the constants used by the
 * Soapbinding classes.
 */

public final class SOAPBindingConstants {
    
    /**
     * XML Namespace
     */
    public static final String NS_XML = "http://www.w3.org/2000/xmlns/";
    
    /**
     * SOAP Envelope URL
     */
    public static final String NS_SOAP =
                    "http://schemas.xmlsoap.org/soap/envelope/";
    
    /** 
     * SOAPBinding Name Space.
     */
    public static final String NS_SOAP_BINDING = "urn:liberty:sb:2003-08";
    
    
    /**
     * SOAPBinding Name Space.
     */
    public static final String NS_SOAP_BINDING_11 = "urn:liberty:sb:2004-04";

    /**
     * Header Tag.
     */
    public static final String TAG_HEADER = "Header";
    
    /**
     * Body Tag.
     */
    public static final String TAG_BODY = "Body";
    
    /**
     * Fault Tag.
     */
    public static final String TAG_FAULT = "Fault";
    
    /*
     * Fault Code Tag.
     */
    public static final String TAG_FAULT_CODE = "faultcode";
    
    /** 
     * Fault String Tag.
     */
    public static final String TAG_FAULT_STRING = "faultstring";
    
    /**
     * Fault Actor Tag.
     */
    public static final String TAG_FAULT_ACTOR = "faultactor";
    
    /**
     * Detail Tag.
     */
    public static final String TAG_DETAIL = "detail";
    
    /**
     * Correlation Tag.
     */
    public static final String TAG_CORRELATION = "Correlation";
    
    /**
     * Consent Tag.
     */
    public static final String TAG_CONSENT = "Consent";
    
    /**
     * Usage Directive Tag.
     */
    public static final String TAG_USAGE_DIRECTIVE = "UsageDirective";
    
    /**
     * Provider Tag.
     */
    public static final String TAG_PROVIDER = "Provider";
    
    /**
     * ProcessingContext Tag.
     */
    public static final String TAG_PROCESSING_CONTEXT = "ProcessingContext";
    
    /** 
     * ServiceInstanceUpdate Tag.
     */
    public static final String TAG_SERVICE_INSTANCE_UPDATE =
                           "ServiceInstanceUpdate";
    
    /**
     * SecurityMechID Tag.
     */
    public static final String TAG_SECURITY_MECH_ID = "SecurityMechID";
    
    /**
     * Credential Tag.
     */
    public static final String TAG_CREDENTIAL = "Credential";
    
    /**
     * Endpoint Tag.
     */
    public static final String TAG_ENDPOINT = "Endpoint";
    
    /**
     * Status Tag.
     */
    public static final String TAG_STATUS = "Status";
    
    /**
     * <code>messageID</code> Attribute.
     */
    public static final String ATTR_MESSAGE_ID = "messageID";
    
    /**
     * <code>refToMessageID</code> Attribute.
     */
    public static final String ATTR_REF_TO_MESSAGE_ID = "refToMessageID";
    
    /**
     * <code>timestamp</code> Attribute.
     */ 
    public static final String ATTR_TIMESTAMP = "timestamp";
    
    /**
     * <code>id</code> Attribute.
     */
    public static final String ATTR_id = "id";
    
    /**
     * <code>mustUnderstand</code> Attribute.
     */
    public static final String ATTR_MUSTUNDERSTAND = "mustUnderstand";
    
    /**
     * <code>actor</code> Attribute.
     */
    public static final String ATTR_ACTOR = "actor";
    
    /**
     * <code>uri</code> Attribute.
     */
    public static final String ATTR_URI = "uri";
    
    /**
     * <code>ref</code> Attribute.
     */
    public static final String ATTR_REF = "ref";
    
    /**
     * <code>providerID</code> Attribute.
     */
    public static final String ATTR_PROVIDER_ID = "providerID";
    
    /**
     * <code>code</code> Attribute.
     */
    public static final String ATTR_CODE = "code";
    
    /**
     * <code>comment</code> Attribute.
     */
    public static final String ATTR_COMMENT = "comment";
    
    /**
     * <code>affiliationID</code> Attribute.
     */
    public static final String ATTR_AFFILIATION_ID = "affiliationID";
    
    /**
     * <code>notOnOrAfter</code> Attribute.
     */
    public static final String ATTR_NOT_ON_OR_AFTER = "notOnOrAfter";
    
    /*
     * Envelope Prefix.
     */
    public static final String PTAG_ENVELOPE = "S:Envelope";
    
    /**
     * Header Prefix.
     */
    public static final String PTAG_HEADER = "S:Header";
    
    /**
     * Body Prefix.
     */
    public static final String PTAG_BODY = "S:Body";
    
    /**
     * SOAP Fault Prefix.
     */
    public static final String PTAG_FAULT = "S:Fault";
    
    /** 
     * Correlation Prefix.
     */
    public static final String PTAG_CORRELATION = "sb:Correlation";
    
    /**
     * Consent Prefix.
     */
    public static final String PTAG_CONSENT = "sb:Consent";
    
    /**
     * Usage Directive Prefix.
     */
    public static final String PTAG_USAGE_DIRECTIVE = "sb:UsageDirective";
    
    /** 
     * Provider Prefix.
     */
    public static final String PTAG_PROVIDER = "sb:Provider";
    
    /**
     * ProcessingContext Prefix.
     */
    public static final String PTAG_PROCESSING_CONTEXT= "sb:ProcessingContext";
    
    /** 
     * Service Instance Update Prefix.
     */
    public static final String PTAG_SERVICE_INSTANCE_UPDATE =
                           "sb-ext:ServiceInstanceUpdate";
    
    /**
     * SecurityMechID Prefix.
     */
    public static final String PTAG_SECURITY_MECH_ID = "sb-ext:SecurityMechID";
    
    /**
     * Credential Prefix.
     */
    public static final String PTAG_CREDENTIAL = "sb-ext:Credential";
    
    /**
     * EndPoint Prefix.
     */
    public static final String PTAG_ENDPOINT = "sb-ext:Endpoint";
    
    /**
     * Status Prefix.
     */
    public static final String PTAG_STATUS = "sb:Status";
    
    /**
     * mustUnderstand Attribute Prefix.
     */
    public static final String PATTR_MUSTUNDERSTAND = "S:mustUnderstand";
    
    /**
     * actor Attribute Prefix.
     */
    public static final String PATTR_ACTOR = "S:actor";
    
    /**
     * Soap NameSpace Prefix.
     */
    public static final String XMLNS_SOAP = "xmlns:S";
    
    /**
     * SOAPBinding Name Space Prefix.
     */ 
    public static final String XMLNS_SOAP_BINDING = "xmlns:sb";
    
    /**
     * SOAPBinding Extended Name Space Prefix.
     */
    public static final String XMLNS_SOAP_BINDING_11 = "xmlns:sb-ext";
    
    /**
     * SOAP Prefix.
     */
    public static final String PREFIX_SOAP = "S";
    
    /** 
     * SOAPBinding Prefix.
     */
    public static final String PREFIX_SOAP_BINDING = "sb";
    
    /**
     * Extended SOAPBinding Prefix.
     */
    public static final String PREFIX_SOAP_BINDING_11 = "sb-ext";
    
    /**
     * Fault Code Prefix.
     */
    public static final String DEFAULT_PREFIX_FAULT_CODE_VALUE = "fc";
    
    /** 
     * Default SOAP Actor Value.
     */
    public static final String DEFAULT_SOAP_ACTOR =
                           "http://schemas.xmlsoap.org/soap/actor/next";

    /**
     * SOAP Action Header.
     */
    static final String SOAP_ACTION_HEADER = "SOAPAction";
    
    /**
     * Default Encoding .
     */
    static final String DEFAULT_ENCODING = "UTF-8";

    /**
     * ID-WSF verstion 1.1.
     */
    public static final String WSF_11_VERSION = "1.1";

    /**
     * ID-WSF verstion 1.0.
     */
    public static final String WSF_10_VERSION = "1.0";

    /**
     * Liberty Request.
     */
    public static final String LIBERTY_REQUEST = "LibertyRequest";

}

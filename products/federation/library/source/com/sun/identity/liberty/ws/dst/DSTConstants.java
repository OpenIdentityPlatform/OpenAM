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
 * $Id: DSTConstants.java,v 1.2 2008/06/25 05:47:13 qcheng Exp $
 *
 */

package com.sun.identity.liberty.ws.dst;

public class DSTConstants {

    public static final String DST_JAXB_PKG = 
            "com.sun.identity.liberty.ws.idpp.jaxb:" +
            "com.sun.identity.liberty.ws.idpp.plugin.jaxb";
    // status values 
    public static final String OK = "OK";
    public static final String NO_RESOURCE = "InvalidResourceID";
    public static final String NO_MULTIPLE_RESOURCE= "NoMultipleResources";
    public static final String NO_MULTIPLE_ALLOWED = "NoMultipleAllowed";
    public static final String NOT_ALLOWED_TO_MODIFY = "NotAllowedToModify";
    public static final String MISSING_SELECT = "MissingSelect";
    public static final String MISSING_RESOURCE = "MissingResource";
    public static final String INVALID_SELECT = "InvalidSelect";
    public static final String EXTENSION_NOT_SUPPORTED = 
                                "ExtensionNotSupported";
    public static final String DATA_NOT_SUPPORTED = "DataNotSupported";
    public static final String CHANGED_SINCE_NOT_SUPPORTED = 
                                "changedSinceNotSupported";
    public static final String FAILED = "Failed";
    public static final String CHANGED_SINCE_RETURNS_ALL = 
                                "changedSinceReturnsAll";
    public static final String UNEXPECTED_ERROR = "UnexpectedError";
    public static final String NOT_AUTHORIZED = "ActionNotAuthorized";
    //Security profiles
    public static final String SAML_TOKEN = "SAMLTokenProfile";
    public static final String X509 = " X509Profile";
    public static final String ANONYMOUS = "AnonymousProfile";

    public static final String SERVICE_TYPE = "servicetype";
    public static final String SERVICE_CLASS = "serviceclass";
    public static final String IDPP_SERVICE_TYPE = 
                               "urn:liberty:id-sis-pp:2003-08";
    public static final String QUERY_ACTION = "QUERY";
    public static final String MODIFY_ACTION = "MODIFY";
  
    public static final String DEFAULT_NS_PREFIX = "sis";
    public static final String NL = "\n"; 

}

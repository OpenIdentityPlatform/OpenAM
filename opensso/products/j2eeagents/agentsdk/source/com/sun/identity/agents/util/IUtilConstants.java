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
 * $Id: IUtilConstants.java,v 1.3 2008/06/25 05:51:59 qcheng Exp $
 *
 */

package com.sun.identity.agents.util;


public interface IUtilConstants {

    /** Field NEW_LINE **/
    public static final String NEW_LINE = System.getProperty("line.separator",
                                              "\n");

    /** Field SEPARATOR **/
    public static final String SEPARATOR =
        "-----------------------------------------------------------";

    /** Field TRANSPORT_CRYPT_PREFIX **/
    public static final String TRANSPORT_CRYPT_PREFIX = "*";

    /** Field REFERER_HEADER **/
    public static final String REFERER_HEADER = "referer";

    /** Field DEFAULT_COOKIE_PATH **/
    public static final String DEFAULT_COOKIE_PATH = "/";
    
    /** Field INT_REQUEST_ID_LENGTH (AuthnRequest ID) **/
    public static final int INT_REQUEST_ID_LENGTH = 20;
    
    /** Field STR_REQUEST_ID_PREFIX (AuthnRequest ID) **/
    public static final String STR_REQUEST_ID_PREFIX = "s";
    
    /** Field STR_UTC_DATE_FORMAT **/
    public static final String STR_UTC_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'"; 

    /** Field COOKIE_RESET_STRING **/
    public static final String COOKIE_RESET_STRING = "reset";
       
    /** Field HTTP_METHOD_GET **/
    public static final String HTTP_METHOD_GET = "GET";
    
    /** Field HTTP_METHOD_POST **/
    public static final String HTTP_METHOD_POST = "POST";
         
     /** Field Default name for the anonymous user **/
    public static final String ANONYMOUS_USER_NAME = "ANONYMOUS";

}


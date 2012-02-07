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
 * $Id: IConfigurationDefaultValueConstants.java,v 1.4 2008/06/25 05:51:36 qcheng Exp $
 *
 */

package com.sun.identity.agents.arch;

/**
 * Defines default values used for various agent related configuration keys.
 */
public interface IConfigurationDefaultValueConstants {

   /**
    * Default locale-language value.
    */
    public static final String DEFAULT_LOCALE_LANG = "en";
    
   /**
    * Default locale-country value.
    */
    public static final String DEFAULT_LOCALE_COUNTRY = "US";
    
   /**
    * Default User Mapping Mode string.
    */
    public static final String DEFAULT_USER_MAPPING_MODE =
        UserMappingMode.STR_MODE_USER_ID;
       
   /**
    * Default User-Id property name.
    */
    public static final String DEFAULT_USER_ID_PROPERTY = "UserToken";
    
   /**
    * Default User Attribute Name value.
    */
    public static final String DEFAULT_USER_ATTRIBUTE_NAME = "employeenumber";
    
   /**
    * Default use-DN flag value.
    */
    public static final String DEFAULT_USE_DN = "false";
    
    /**
     * Deafult organization name is the root realm name
     */
    public static final String DEFAULT_ORG_NAME = "/";
    
}

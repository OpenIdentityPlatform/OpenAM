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
 * $Id: IBaseModuleConstants.java,v 1.3 2008/06/25 05:51:35 qcheng Exp $
 *
 */

package com.sun.identity.agents.arch;

/**
 * Constants used by <code>BaseModule</code> and other <code>Module</code>s.
 */
public interface IBaseModuleConstants {

   //-------------- Common Constants --------------------//
    
   /**
    * Resource offset for uniqueness
    */
    public static final int OFFSET = 0;

   /**
    * Multiplier used for every resource 
    */
    public static final int OFFSET_MULTIPLIER = 10000;

   /**
    * Starting offset for optional modules
    */
    public static final int OPTIONAL_MODULE_OFFSET = 
        OFFSET + 16 * OFFSET_MULTIPLIER;

   //-------------- Base Module Constatns --------------------//
    
   /**
    * Base Module: module code
    */
    public static final byte BASE_MODULE_CODE = 0x0;

   /**
    * Base Module: resource
    */
    public static final String BASE_RESOURCE = "amAgentCore";

   /**
    * Base Module: offset
    */
    public static final int BASE_OFFSET = OFFSET;

   //-------------- AmWebPolicy Module Constatns -------------//    

    /**
    * AmWebPolicy Module: module code
    */
    public static final byte AM_WEB_MODULE_CODE = 0x1;

   /**
    * AmWebPolicy Module: resource
    */
    public static final String AM_WEB_RESOURCE = "amWebPolicy";

   /**
    * AmWebPolicy Module: offset
    */
    public static final int AM_WEB_OFFSET = BASE_OFFSET 
                            + OFFSET_MULTIPLIER;

   //-------------- AmFilter Module Constatns ---------------//    

   /**
    * AmFilter Module: module code
    */
    public static final byte AM_FILTER_MODULE_CODE = 0x2;

   /**
    * AmFilter Module: resource
    */
    public static final String AM_FILTER_RESOURCE = "amFilter";

   /**
    * AmFilter Module: offset
    */
    public static final int AM_FILTER_OFFSET = AM_WEB_OFFSET
                                               + OFFSET_MULTIPLIER;

   //-------------- AmRealm Module Constatns ------------------//    

   /**
    * AmRealm Module: module code
    */
    public static final byte AM_REALM_MODULE_CODE = 0x3;

   /**
    * AmRealm Module: resource
    */
    public static final String AM_REALM_RESOURCE = "amRealm";

   /**
    * AmRealm Module: offset
    */
    public static final int AM_REALM_OFFSET = AM_FILTER_OFFSET
                                              + OFFSET_MULTIPLIER;

   //-------------- AmRealm Module Constatns --------------------//    

   /**
    * AmLog Module: module code
    */
    public static final byte AM_LOG_MODULE_CODE = 0x4;

   /**
    * AmLog Module: resource
    */
    public static final String AM_LOG_RESOURCE = "amAgentLog";

   /**
    * AmLog Module: offset
    */
    public static final int AM_LOG_OFFSET = AM_REALM_OFFSET
                                            + OFFSET_MULTIPLIER;


   //------------------- Base Module Constants -----------------------//
   /**
    * Base Module Locale: resource id
    */
    public static final int STR_BASE_MODULE = BASE_OFFSET + 1;
}

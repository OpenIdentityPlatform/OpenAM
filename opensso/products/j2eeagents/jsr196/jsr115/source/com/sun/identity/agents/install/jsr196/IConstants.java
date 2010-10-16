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
 * $Id: IConstants.java,v 1.1 2009/01/30 12:09:38 kalpanakm Exp $
 *
 */

package com.sun.identity.agents.install.jsr196;

public interface IConstants {
        
   /** Field STR_AS_GROUP **/
   public static String STR_AS_GROUP = "as81Tools";
   
   /** Field STR_FORWARD_SLASH **/
   public static final String STR_FORWARD_SLASH = "/";

   public static final String STR_AGENT_REALM_CLASS_NAME = 
       "com.sun.identity.agents.jsr196.AmASRealm";

   public static final String STR_LOGIN_MODULE_CLASS_NAME = 
       "com.sun.identity.agents.jsr196.AmASLoginModule";

}



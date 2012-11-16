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
 * $Id: AuthOptions.java,v 1.3 2008/06/25 05:42:12 qcheng Exp $
 *
 */

package com.sun.identity.cli.authentication;

public interface AuthOptions {
    /**
     * Name of authentication configuration.
     */
    String AUTH_CONFIG_NAME = "name";
    
    /**
     * Set of authentication configuration names.
     */
    String AUTH_CONFIG_NAMES = "names";
    
    /**
     * Authentication configuration entries.
     */
    String AUTH_CONFIG_ENTRIES = "entries";
    
    /**
     * Authentication configuration module name.
     */
    String AUTH_CONFIG_MODULE_NAME = "modulename";
    
    /**
     * Authentication configuration criteria.
     */
    String AUTH_CONFIG_CRITERIA = "criteria";
    
    /**
     * Authentication configuration options.
     */
    String AUTH_CONFIG_OPTIONS = "options";
    
    /**
     * Authentication configuration position/order.
     */
    String AUTH_CONFIG_POSITION = "position";
    
    /**
     * Type of authentication instance.
     */
    String AUTH_INSTANCE_TYPE = "authtype";
    
    /**
     * Name of authentication instance.
     */
    String AUTH_INSTANCE_NAME = "name";

    /**
     * Set of authentication instance names.
     */
    String AUTH_INSTANCE_NAMES = "names";

    /**
     * Authentication configuration attribute name.
     */
    String AUTH_CONFIG_ATTR = "iplanet-am-auth-configuration";
}

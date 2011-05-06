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
 * $Id: ConfigurationActionEvent.java,v 1.3 2008/06/25 05:47:25 qcheng Exp $
 *
 */

package com.sun.identity.plugin.configuration;

/**
 * The <code>ConfigurationActionEvent</code> class represents 
 * Configuration event.
 *
 * @supported.all.api
 */
public interface ConfigurationActionEvent {
    /**
     * Add new configuration.
     */
    public static final int ADDED = 1;
    /**
     * Delete configuration.
     */
    public static final int DELETED = 2;
    /**
     * Modify configuration.
     */
    public static final int MODIFIED = 3;
        
    /**
     * Returns the type of this event.
     *
     * @return The type of this event. Possible types are :
     * <ul>
     * <li><code>ADDED</code>,
     * <li><code>DELETED</code> and
     * <li><code>MODIFIED</code>
     * </ul>
     */
    public int getType();

    /**
     * Returns configuration name.
     * @return configuration name or null if it is default configuration.
     */
    public String getConfigurationName();

    /**
     * Returns component name.
     * @return component name 
     */
    public String getComponentName();

    /**
     * Returns realm name. 
     * @return realm name or null if it is default configuration.
     */
    public String getRealm();
}

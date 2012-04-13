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
 * $Id: ConfigurationActionEventImpl.java,v 1.2 2008/06/25 05:49:57 qcheng Exp $
 *
 */

package com.sun.identity.plugin.configuration.impl;

import com.sun.identity.plugin.configuration.ConfigurationActionEvent;
import com.sun.identity.plugin.configuration.ConfigurationException;

/**
 * The <code>ConfigurationActionEventImpl</code> class represents 
 * Configuration event.
 */
public class ConfigurationActionEventImpl implements ConfigurationActionEvent {

    private int type;
    private String configName;
    private String componentName;
    private String realm;

    /**
     * This constructor takes type, configuration name, componenet name
     * and realm.
     *
     * @param type the type.
     * @param configName the configuration name.
     * @param componentName the component name.
     * @param realm the realm.
     */
    ConfigurationActionEventImpl(int type, String configName,
        String componentName, String realm) {

        this.type = type;
        this.configName = configName;
        this.componentName = componentName;
        this.realm = realm;
    }

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
    public int getType() {
        return type;
    }

    /**
     * Returns configuration name.
     * @return configuration name  or null if it is default configuration.
     */
    public String getConfigurationName() {
        return configName;
    }

    /**
     * Returns component name.
     * @return component name 
     */
    public String getComponentName() {
        return componentName;
    }

    /**
     * Returns realm name. 
     * @return realm name or null if it is default configuration.
     */
    public String getRealm() {
        return realm;
    }
}

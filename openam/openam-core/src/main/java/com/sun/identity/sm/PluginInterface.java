/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: PluginInterface.java,v 1.4 2008/07/06 05:48:29 arviranga Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.sm;


import com.sun.identity.shared.xml.XMLUtils;
import org.w3c.dom.Node;

/**
 * The class <code>PluginInterface</code> provides interfaces needed to obtain
 * information about a plugin interface defined by the service schema.
 */
public class PluginInterface extends Object {

    private String name;

    private String interfaceClass;

    private String key;

    private PluginInterface() {
        // cannot be instantiated
    }

    protected PluginInterface(Node n) {
        name = XMLUtils.getNodeAttributeValue(n, SMSUtils.NAME);
        interfaceClass = XMLUtils.getNodeAttributeValue(n,
                SMSUtils.PLUGIN_INTERFACE_CLASS);
        key = XMLUtils.getNodeAttributeValue(n, SMSUtils.I18N_KEY);
    }

    protected PluginInterface(String name, String interfaceClass, String key) {
        this.name = name;
        this.interfaceClass = interfaceClass;
        this.key = key;
    }

    /**
     * Returns the plugin interface name.
     * 
     * @return plugin interface name
     */
    public String getName() {
        return (name);
    }

    /**
     * Returns the plugin interface class name.
     * 
     * @return plugin interface class name
     */
    public String getInterfaceClass() {
        return (interfaceClass);
    }

    /**
     * Returns the i18n key that describes the function provided by the plugin
     * interface
     * 
     * @return i18n index key to the resource bundle
     */
    public String getI18NKey() {
        return (key);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(100);
        sb.append("PluginInterface name: ").append(name).
                append("\n\tInterface class: ").append(interfaceClass).
                append("\n\tI18N Key: ").append(key);
        return (sb.toString());
    }
}

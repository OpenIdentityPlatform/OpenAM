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
 * $Id: AuthConfigurationEntry.java,v 1.4 2008/06/25 05:41:52 qcheng Exp $
 *
 */



package com.sun.identity.authentication.config;

import com.sun.identity.shared.debug.Debug;
import java.io.Serializable;
import org.w3c.dom.Node;

/**
 * Represents one Authentication Configuration Entry
 */
public class AuthConfigurationEntry implements Serializable {
    private static Debug debug = Debug.getInstance("amAuthConfig");

    private String module = null;
    private String flag = null;
    private String options = null;

    /**
     * Constructor.
     *
     * @param module Login Module class name example
     *        <code>com.sun.identity.authentication.modules.ldap.LDAP</code>
     * @param flag Control flag, one of 
     *        <code>REQUIRED, OPTIONAL, REQUISITE, SUFFICIENT</code>
     * @param options Options as space separated string.
     * @throws AMConfigurationException if module or flag is null or flag is 
     *         invalid.
     */
    public AuthConfigurationEntry(String module, String flag, String options) 
            throws AMConfigurationException {
        checkModule(module);
        checkFlag(flag);
        this.module = module; 
        this.flag = flag;
        this.options = options;
    }

    /**
     * Constructor.
     */
    protected AuthConfigurationEntry(Node node)
        throws AMConfigurationException {
        if (debug.messageEnabled()) {
            debug.message("AuthConfigEntry, value=" + node.toString());
        }
        String value = node.getFirstChild().getNodeValue();
        if (value == null || value.length() == 0) {
            debug.error("AuthConfigEntry, invalid value=" + value);
            throw new AMConfigurationException(
                AMAuthConfigUtils.bundleName, "invalidConfig"); 
        } 

        value = value.trim();
        int pos = value.indexOf(" ");
        if (pos == -1) {
            debug.error("AuthConfigEntry, invalid value=" + value);
            throw new AMConfigurationException(
                AMAuthConfigUtils.bundleName, "invalidConfig"); 
        }
        // set module
        this.module = value.substring(0, pos);
        value = value.substring(pos + 1).trim(); 
        pos = value.indexOf(" ");
        if (pos == -1) {
            // no options
            this.flag = value;
        } else {
            this.flag = value.substring(0, pos);
            this.options = value.substring(pos + 1).trim();
        }
        checkFlag(this.flag);
    }

    /**
     * Returns string representation of this object.
     *
     * @return string representation of this object.
     */
    public String toString() {
        if (options == null) { 
            return module + " " + flag;
        } else {
            return module + " " + flag + " " + options;
        }
    }

    /**
     * Returns login module name
     * @return login module name
     */
    public String getLoginModuleName() {
        return module;
    }

    /**
     * Sets login module name
     *
     * @param moduleName module name.
     * @throws AMConfigurationException if module is null.
     */
    public void setLoginModuleName(String moduleName) 
            throws AMConfigurationException {
        checkModule(moduleName);
        module = moduleName;
    }

    /**
     * Returns control flag .
     *
     * @return control flag .
     */
    public String getControlFlag() {
        return flag;
    }

    /**
     * Sets control flag.
     *
     * @param flag control flag
     * @throws AMConfigurationException if flag is null or invalid.
     */
    public void setControlFlag(String flag) throws AMConfigurationException {
        checkFlag(flag);
        this.flag = flag;
    }

    /**
     * Returns options.
     *
     * @return options.
     */
    public String getOptions() {
        return options;
    }

    /**
     * Sets options.
     * @param options
     */
    public void setOptions(String options) {
        this.options = options;
    }

    private void checkModule(String module) throws AMConfigurationException {
        if (module == null || module.length() == 0) {
            throw new AMConfigurationException(
                AMAuthConfigUtils.bundleName, "invalidConfig");
        } 
    }

    private void checkFlag(String flag) throws AMConfigurationException{
        if (flag == null || flag.length() == 0) {
            throw new AMConfigurationException(
                AMAuthConfigUtils.bundleName, "invalidConfig");
        }
        if (!flag.equalsIgnoreCase("REQUIRED") &&
            !flag.equalsIgnoreCase("OPTIONAL") &&
            !flag.equalsIgnoreCase("REQUISITE") &&
            !flag.equalsIgnoreCase("SUFFICIENT")) {
            debug.warning("AuthConfigEntry, invalid flag : " + flag);
            throw new AMConfigurationException(
                AMAuthConfigUtils.bundleName, "invalidConfig");
        }
    }
}

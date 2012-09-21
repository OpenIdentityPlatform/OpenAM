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
 * $Id: ValidationInfo.java,v 1.2 2008/06/25 05:51:25 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.util.Map;

public class ValidationInfo {

    public String toString() {
        return "[Name: " + getName() + ", Class: " + getClassName() + ","
                + " Property Map: " + getPropertiesMap() + "]";
    }

    public ValidationInfo(String name, Map props, String className) {
        setName(name);
        setPropertiesMap(props);
        setClassName(className);
    }

    public String getName() {
        return name;
    }

    public Map getPropertiesMap() {
        return props;
    }

    public String getClassName() {
        return className;
    }

    private void setName(String name) {
        this.name = name;
    }

    private void setClassName(String className) {
        this.className = className;
    }

    private void setPropertiesMap(Map map) {
        props = map;
    }

    private String name;

    private String className;

    private Map props;

}

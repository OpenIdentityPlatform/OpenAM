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
 * $Id: DefaultValueFinderInfo.java,v 1.2 2008/06/25 05:51:18 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

/**
 * @author ap74890
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class DefaultValueFinderInfo {

    public String toString() {
        return "[Static: " + getValue() + ", Class: " + getClassName() + "]";
    }

    public DefaultValueFinderInfo(String className) {
        this(className, null);
    }

    public DefaultValueFinderInfo(String className, String value) {
        setClassName(className);
        setValue(value);
    }

    public String getClassName() {
        return className;
    }

    public String getValue() {
        return value;
    }

    private void setClassName(String className) {
        this.className = className;
    }

    private void setValue(String value) {
        this.value = value;
    }

    private String className;

    private String value;
}

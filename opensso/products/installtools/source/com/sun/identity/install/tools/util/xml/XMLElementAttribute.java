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
 * $Id: XMLElementAttribute.java,v 1.2 2008/06/25 05:51:31 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.util.xml;

/**
 * Represents the attribute of an XML element.
 */
public class XMLElementAttribute {

    /**
     * Returns the name of this attribute
     * 
     * @return the name of this attribute
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the value of this attribute
     * 
     * @return the attribute value
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the string representation of this attribute.
     * 
     * @return the string representation of this attribute
     */
    public String toString() {
        return getName() + "=" + getValue();
    }

    /**
     * Constructor
     * 
     * @param name
     * @param value
     */
    XMLElementAttribute(String name, String value) {
        setName(name);
        setValue(value);
    }

    /**
     * Sets the value of this attribute.
     * 
     * @param value
     */
    void setValue(String value) {
        this.value = value;
    }

    /**
     * Returns the name of this attribute.
     * 
     * @param name
     */
    private void setName(String name) {
        this.name = name;
    }

    private String name;

    private String value;
}

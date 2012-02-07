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
 * $Id: NameValuePair.java,v 1.2 2008/06/25 05:51:59 qcheng Exp $
 *
 */

package com.sun.identity.agents.util;

/**
 * An Object contains a name and its corresponding value 
 */
public class NameValuePair {
    /**
     * Constructor NameValuePair
     *
     *
     * @param name the name 
     * @param value the value
     *
     */
    public NameValuePair(String name, String value) {
        setName(name);
        setValue(value);
    }

    /**
     * Sets the name
     *
     * @param name the name 
     *
     */
    private void setName(String name) {
        _name = name;
    }

    /**
     * Sets the value
     *
     * @param value the value
     *
     */
    private void setValue(String value) {
        _value = value;
    }

    /**
     * Returns the name
     *
     * @return the name
     *
     */
    public String getName() {
        return _name;
    }

    /**
     * Returns the value 
     *
     * @return the value 
     *
     */
    public String getValue() {
        return _value;
    }

    /**
     * Returns a string representation of the object
     *
     * @return a string representation of the object
     */
    public String toString() {
        return "NameValuePair" + "[" + getName() + "=" + getValue() + "]";
    }

    private String _name;
    private String _value;
}

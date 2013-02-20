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
 * $Id: Template.java,v 1.3 2008/06/25 05:41:46 qcheng Exp $
 *
 */

package com.iplanet.ums;

import java.io.Serializable;

/**
 * Represents Template objects in UMS. This is the abstract class for
 * CreationTemplate for object creation and SearchTemplate for defining
 * guidelines for searching functionality.
 * <P>
 * 
 * @see CreationTemplate
 * @see SearchTemplate
 * @supported.all.api
 */
public abstract class Template implements Serializable, Cloneable {
    /**
     * Default constructor for deserialization
     */
    public Template() {
    }

    /**
     * Creates a Template with a name; should be called from a derived class.
     * 
     * @param name
     *            Template name
     */
    public Template(String name) {
        _name = name;
    }

    /**
     * Gets the name of the template.
     * 
     * @return name of the Template
     */
    public String getName() {
        return _name;
    }

    /**
     * Sets the name of the template (for deserialization).
     * 
     * @param name
     *            name of the template
     */
    public void setName(String name) {
        _name = name;
    }

    /**
     * Returns a copy of the template.
     * 
     * @return a copy of the template
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException ex) {
            return null;
        }
    }

    private String _name = null;
}

/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ViewAttribute.java,v 1.5 2009/06/24 23:47:02 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import java.io.Serializable;

public abstract class ViewAttribute implements Comparable, Serializable {
    private String name;
    private boolean nameEditable = false;

    public String getName() {
        return name;
    }

    public void setEditable(boolean editable) {
        setNameEditable(editable);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return getName();
    }

    @Override
    public String toString() {
        return getTitle();
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ViewAttribute)) {
            return false;
        }
        ViewAttribute va = (ViewAttribute)o;
        return va.getName().equals(getName());
    }

    public boolean isNameEditable() {
        if (name == null || name.length() == 0) {
            return true;
        }
        return nameEditable;
    }

    public void setNameEditable(boolean nameEditable) {
        this.nameEditable = nameEditable;
    }

    public int compareTo(Object o) {
        ViewAttribute other = (ViewAttribute)o;
        return name.compareTo(other.getName());
    }
}

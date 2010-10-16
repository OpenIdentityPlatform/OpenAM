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
 * $Id: UrlResource.java,v 1.15 2009/08/13 13:27:00 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import java.io.Serializable;
import java.util.List;

public class UrlResource extends Resource implements Serializable {
    private List<Part> parts;

    public UrlResource() {
        super();
    }

    public UrlResource(String name) {
        this();
        setName(name);
    }

    public List<Part> getParts() {
        return parts;
    }

    public static class Part {
        private String string;
        private String value;

        public String getString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

    }

    public boolean isExceptable() {
        return getName().endsWith("*");
    }

    public String getExceptionPrefix() {
        return getName().substring(0, getName().length()-1);
    }

    public boolean isAddable() {
        return getName().contains("*");
    }

    public UrlResource deepClone() {
        UrlResource ur = new UrlResource();
        ur.setName(getName());
        
        return ur;
    }

    public static UrlResource valueOf(String s) {
        UrlResource ur = new UrlResource();
        ur.setName(s);

        return ur;
    }

    public UrlResourceParts getUrlResourceParts() {
        return new UrlResourceParts(this);
    }

    public boolean isBlank() {
        return getName() == null || getName().length() == 0;
    }
}

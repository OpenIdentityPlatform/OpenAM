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
 * $Id: Action.java,v 1.9 2009/06/04 11:49:13 veiming Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.DeepCloneable;
import com.sun.identity.admin.Resources;

public abstract class Action implements DeepCloneable {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract Object getValue();

    @Override
    public abstract boolean equals(Object o);

    @Override
    public int hashCode() {
        return (getName()+getValue()).hashCode();
    }

    @Override
    public abstract String toString();

    public String getTitle() {
        Resources r = new Resources();
        String title = r.getString(this, "title."+getName(), getName());
        if (title == null) {
            title = r.getString(this, "title._none", getName());
        }
        if (title == null) {
            title = getName();
        }
        return title;
    }
}

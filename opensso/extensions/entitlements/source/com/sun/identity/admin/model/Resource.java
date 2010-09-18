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
 * $Id: Resource.java,v 1.10 2009/08/04 18:50:46 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.DeepCloneable;
import com.sun.identity.admin.Resources;
import java.io.Serializable;
import org.apache.commons.collections.comparators.NullComparator;

public abstract class Resource implements Serializable, DeepCloneable {
    private String name;
    private boolean visible = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        Resources r = new Resources();
        String title = r.getString(this, "title."+getName(), getName());
        if (title != null) {
            return title;
        }
        return getName();
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public String toString() {
        return getTitle();
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Resource) {
            Resource other = (Resource)o;
            NullComparator nc = new NullComparator();
            return nc.compare(getName(), other.getName()) == 0;
        }

        return false;
    }
}

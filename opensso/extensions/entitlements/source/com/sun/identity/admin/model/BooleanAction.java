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
 * $Id: BooleanAction.java,v 1.14 2009/06/24 23:47:02 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.DeepCloneable;
import com.sun.identity.admin.Resources;
import com.sun.identity.admin.handler.BooleanActionHandler;
import java.io.Serializable;

public class BooleanAction
        extends Action
        implements Serializable {

    private boolean allow = false;
    private BooleanActionHandler booleanActionHandler;

    public BooleanAction() {
        booleanActionHandler = new BooleanActionHandler();
        booleanActionHandler.setBooleanAction(this);
    }

    public Boolean getValue() {
        return Boolean.valueOf(allow);
    }

    public DeepCloneable deepClone() {
        BooleanAction clone = new BooleanAction();
        clone.setName(getName());
        clone.setAllow(allow);

        return clone;
    }

    public BooleanActionHandler getBooleanActionHandler() {
        return booleanActionHandler;
    }

    public void setBooleanActionHandler(BooleanActionHandler booleanActionHandler) {
        this.booleanActionHandler = booleanActionHandler;
    }

    public boolean isAllow() {
        return allow;
    }

    public void setAllow(boolean allow) {
        this.allow = allow;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BooleanAction)) {
            return false;
        }
        BooleanAction other = (BooleanAction)o;

        if (getName().equals(other.getName())) {
            return true;
        }

        if (getName().equals(other.getName().toLowerCase())) {
            return true;
        }

        return false;
    }

    public String toString() {
        return getTitle() + ": " + getValueTitle();
    }

    private String getValueTitle() {
        Resources r = new Resources();
        String title = r.getString(this, "allow."+allow);
        return title;
    }
}

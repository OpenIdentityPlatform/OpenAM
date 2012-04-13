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
 * $Id: AttributeViewSubject.java,v 1.3 2009/07/31 21:53:48 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.Resources;
import com.sun.identity.entitlement.AttributeSubject;
import com.sun.identity.entitlement.EntitlementSubject;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;

public class AttributeViewSubject extends ViewSubject {
    private String value;
    private boolean valueEditable = true;

    public EntitlementSubject getEntitlementSubject() {
        AttributeSubject as = new AttributeSubject();
        as.setID(getName());
        as.setValue(value);
        return as;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
        valueEditable = false;
    }

    public boolean isValueEditable() {
        if (value == null || value.length() == 0) {
            return true;
        }

        return valueEditable;
    }

    public void valueChanged(ValueChangeEvent event) {
        valueEditable = false;
    }

    public void valueClicked(ActionEvent event) {
        valueEditable = true;
    }

    @Override
    public String toString() {
        Resources r = new Resources();
        String v = (value == null || value.length() == 0) ? r.getString(this, "emptyValue") : value;
        return getSubjectType().getTitle() + ":" + getTitle() + "=" + v;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AttributeViewSubject)) {
            return false;
        }
        AttributeViewSubject avs = (AttributeViewSubject)o;
        return avs.getName().equals(getName());
    }
}

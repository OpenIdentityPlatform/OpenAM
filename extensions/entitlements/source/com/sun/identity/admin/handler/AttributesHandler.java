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
 * $Id: AttributesHandler.java,v 1.4 2009/06/04 11:49:12 veiming Exp $
 */

package com.sun.identity.admin.handler;

import com.sun.identity.admin.model.AttributesBean;
import com.sun.identity.admin.model.ViewAttribute;
import java.io.Serializable;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesEvent;
import javax.faces.event.ValueChangeEvent;

public class AttributesHandler implements Serializable {
    private AttributesBean attributesBean;

    public AttributesHandler(AttributesBean attributesBean) {
        this.attributesBean = attributesBean;
    }

    public AttributesBean getAttributesBean() {
        return attributesBean;
    }

    public void setAttributesBean(AttributesBean attributesBean) {
        this.attributesBean = attributesBean;
    }

    protected ViewAttribute getViewAttribute(FacesEvent event) {
        ViewAttribute va = (ViewAttribute) event.getComponent().getAttributes().get("viewAttribute");
        assert (va != null);

        return va;
    }

    public void removeListener(ActionEvent event) {
        ViewAttribute va = getViewAttribute(event);
        attributesBean.getViewAttributes().remove(va);
    }

    public void addListener(ActionEvent event) {
        ViewAttribute va = attributesBean.newViewAttribute();
        va.setEditable(true);
        attributesBean.getViewAttributes().add(va);
    }

    public void editNameListener(ActionEvent event) {
        ViewAttribute va = (ViewAttribute)getViewAttribute(event);
        va.setNameEditable(true);
    }

    public void nameEditedListener(ValueChangeEvent event) {
        ViewAttribute va = (ViewAttribute)getViewAttribute(event);
        va.setNameEditable(false);
    }
}

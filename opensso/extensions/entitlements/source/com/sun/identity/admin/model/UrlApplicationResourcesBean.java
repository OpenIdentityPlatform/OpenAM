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
 * $Id: UrlApplicationResourcesBean.java,v 1.1 2009/08/04 18:50:46 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import java.io.Serializable;

public class UrlApplicationResourcesBean implements Serializable {

    private boolean addPopupVisible = false;
    private UrlResource addPopupResource;

    public boolean isAddPopupVisible() {
        return addPopupVisible;
    }

    public void setAddPopupVisible(boolean addPopupVisible) {
        this.addPopupVisible = addPopupVisible;
    }

    public String getAddPopupResourceName() {
        if (getAddPopupResource() == null) {
            return null;
        }
        return getAddPopupResource().getName();
    }

    public void setAddPopupResourceName(String name) {
        if (name == null) {
            setAddPopupResource(null);
        } else {
            UrlResource ur = new UrlResource();
            ur.setName(name);
            setAddPopupResource(ur);
        }
    }

    public UrlResource getAddPopupResource() {
        return addPopupResource;
    }

    public void setAddPopupResource(UrlResource addPopupResource) {
        this.addPopupResource = addPopupResource;
    }
}

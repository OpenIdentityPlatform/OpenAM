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
 * $Id: UrlResourcesBean.java,v 1.11 2009/06/04 11:49:18 veiming Exp $
 */

package com.sun.identity.admin.model;

import com.icesoft.faces.context.effects.Effect;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.faces.model.SelectItem;

public class UrlResourcesBean implements Serializable {

    private boolean addPopupVisible = false;
    private String addExceptionPopupName;
    private String searchFilter;
    private UrlResource addPopupResource;
    private UrlResourceParts addPopupUrlResourceParts;
    private boolean addExceptionPopupVisible;
    private UrlResource addExceptionPopupResource;
    private List<Resource> addPopupAvailableResources;
    private Effect resourcesMessageEffect;

    public boolean isAddPopupVisible() {
        return addPopupVisible;
    }

    public void setAddPopupVisible(boolean addPopupVisible) {
        this.addPopupVisible = addPopupVisible;
    }

    public String getSearchFilter() {
        return searchFilter;
    }

    public void setSearchFilter(String searchFilter) {
        this.searchFilter = searchFilter;
    }

    public boolean isAddExceptionPopupVisible() {
        return addExceptionPopupVisible;
    }

    public void setAddExceptionPopupVisible(boolean addExceptionPopupVisible) {
        this.addExceptionPopupVisible = addExceptionPopupVisible;
    }

    public UrlResource getAddExceptionPopupResource() {
        return addExceptionPopupResource;
    }

    public void setAddExceptionPopupResource(UrlResource addExceptionPopupResource) {
        this.addExceptionPopupResource = addExceptionPopupResource;
    }

    public String getAddExceptionPopupName() {
        return addExceptionPopupName;
    }

    public void setAddExceptionPopupName(String addExceptionPopupName) {
        this.addExceptionPopupName = addExceptionPopupName == null ? null : addExceptionPopupName.trim();
    }

    public List<SelectItem> getAddPopupAvailableResourceNameItems() {
        List<SelectItem> items = new ArrayList<SelectItem>();
        if (getAddPopupAvailableResources() == null) {
            return items;
        }

        for (Resource r : getAddPopupAvailableResources()) {
            UrlResource ur = (UrlResource) r;
            SelectItem i = new SelectItem(ur.getName());
            items.add(i);
        }

        return items;
    }

    public void setAddPopupAvailableResources(List<Resource> addPopupAvailableResources) {
        this.addPopupAvailableResources = new ArrayList<Resource>();
        for (Resource r : addPopupAvailableResources) {
            UrlResource ur = (UrlResource) r;
            if (ur.isAddable()) {
                this.getAddPopupAvailableResources().add(ur);
            }
        }
    }

    public Effect getResourcesMessageEffect() {
        return resourcesMessageEffect;
    }

    public void setResourcesMessageEffect(Effect resourcesMessageEffect) {
        this.resourcesMessageEffect = resourcesMessageEffect;
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

    public UrlResourceParts getAddPopupUrlResourceParts() {
        return addPopupUrlResourceParts;
    }

    public void setAddPopupUrlResourceParts(UrlResourceParts addPopupUrlResourceParts) {
        this.addPopupUrlResourceParts = addPopupUrlResourceParts;
    }

    public List<Resource> getAddPopupAvailableResources() {
        return addPopupAvailableResources;
    }
}

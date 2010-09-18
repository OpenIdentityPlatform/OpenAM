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
 * $Id: FilterHolder.java,v 1.2 2009/09/30 22:53:35 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.faces.model.SelectItem;

public class FilterHolder implements Serializable {
    private ViewFilter viewFilter;
    private Map<String,ViewFilterType> viewFilterTypes;
    private ViewFilterType viewFilterType;

    public String getViewFilterTypeName() {
        return getViewFilterType().getName();
    }

    public void setViewFilterTypeName(String name) {
        setViewFilterType(viewFilterTypes.get(name));
    }

    public List<SelectItem> getViewFilterTypeNameItems() {
        List<SelectItem> items = new ArrayList<SelectItem>();
        for (ViewFilterType vft: viewFilterTypes.values()) {
            items.add(new SelectItem(vft.getName(), vft.getTitle()));
        }

        return items;
    }

    public ViewFilter getViewFilter() {
        return viewFilter;
    }

    public void setViewFilter(ViewFilter viewFilter) {
        this.viewFilter = viewFilter;
    }

    public Map<String,ViewFilterType> getViewFilterTypes() {
        return viewFilterTypes;
    }

    public void setViewFilterTypes(Map<String,ViewFilterType> viewFilterTypes) {
        this.viewFilterTypes = viewFilterTypes;
        if (viewFilterTypes != null && viewFilterTypes.size() > 0) {
            setViewFilterType(viewFilterTypes.values().iterator().next());
        }
    }

    public ViewFilterType getViewFilterType() {
        return viewFilterType;
    }

    public void setViewFilterType(ViewFilterType viewFilterType) {
        this.viewFilterType = viewFilterType;
        viewFilter = viewFilterType.newViewFilter();
    }

}

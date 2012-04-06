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
 * $Id: PolicyFilterHolder.java,v 1.2 2009/06/04 11:49:16 veiming Exp $
 */

package com.sun.identity.admin.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.faces.model.SelectItem;

public class PolicyFilterHolder implements Serializable {
    private PolicyFilter policyFilter;
    private Map<String,PolicyFilterType> policyFilterTypes;
    private PolicyFilterType policyFilterType;

    public String getPolicyFilterTypeName() {
        return getPolicyFilterType().getName();
    }

    public void setPolicyFilterTypeName(String name) {
        setPolicyFilterType(getPolicyFilterTypes().get(name));
    }

    public List<SelectItem> getPolicyFilterTypeNameItems() {
        List<SelectItem> items = new ArrayList<SelectItem>();
        for (PolicyFilterType pft: getPolicyFilterTypes().values()) {
            items.add(new SelectItem(pft.getName(), pft.getTitle()));
        }

        return items;
    }

    public PolicyFilter getPolicyFilter() {
        return policyFilter;
    }

    public void setPolicyFilter(PolicyFilter policyFilter) {
        this.policyFilter = policyFilter;
    }

    public Map<String, PolicyFilterType> getPolicyFilterTypes() {
        return policyFilterTypes;
    }

    public void setPolicyFilterTypes(Map<String, PolicyFilterType> policyFilterTypes) {
        this.policyFilterTypes = policyFilterTypes;
        if (policyFilterTypes != null && policyFilterTypes.size() > 0) {
            setPolicyFilterType(policyFilterTypes.values().iterator().next());
        }
    }

    public PolicyFilterType getPolicyFilterType() {
        return policyFilterType;
    }

    public void setPolicyFilterType(PolicyFilterType policyFilterType) {
        this.policyFilterType = policyFilterType;
        policyFilter = policyFilterType.newPolicyFilter();
    }

}

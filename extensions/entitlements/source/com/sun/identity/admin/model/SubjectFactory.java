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
 * $Id: SubjectFactory.java,v 1.7 2009/08/04 19:41:56 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.ManagedBeanResolver;
import com.sun.identity.admin.dao.SubjectDao;
import com.sun.identity.entitlement.EntitlementSubject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.faces.model.SelectItem;

public class SubjectFactory implements Serializable {
    private Map<String,SubjectType> entitlementSubjectToSubjectTypeMap;
    private Map<String,SubjectDao> viewSubjectToSubjectDaoMap;
    private Map<String,SubjectType> viewSubjectToSubjectTypeMap;
    private Map<SubjectType,SubjectContainer> subjectTypeToSubjectContainerMap;

    private SubjectType getSubjectType(EntitlementSubject es) {
        String className = es.getClass().getName();
        return entitlementSubjectToSubjectTypeMap.get(className);
    }

    public List<SelectItem> getSubjectTypeNameItems() {
        List<SelectItem> items = new ArrayList<SelectItem>();
        for (SubjectType st: getSubjectTypes()) {
            SelectItem si = new SelectItem(st.getName(), st.getTitle());
            items.add(si);
        }

        return items;
    }

    public Map<String,SubjectType> getSubjectTypeNameMap() {
        Map<String,SubjectType> m = new HashMap<String,SubjectType>();
        for (SubjectType st: getSubjectTypes()) {
            m.put(st.getName(), st);
        }

        return m;
    }

    public List<SubjectType> getSubjectTypes() {
        List<SubjectType> subjectTypes = new ArrayList<SubjectType>(subjectTypeToSubjectContainerMap.keySet());
        ManagedBeanResolver mbr = new ManagedBeanResolver();
        SubjectType st;

        st = (SubjectType)mbr.resolve("orSubjectType");
        assert(st != null);
        subjectTypes.add(st);

        st = (SubjectType)mbr.resolve("andSubjectType");
        assert(st != null);
        subjectTypes.add(st);

        st = (SubjectType)mbr.resolve("notSubjectType");
        assert(st != null);
        subjectTypes.add(st);

        return subjectTypes;
    }

    public SubjectDao getSubjectDao(ViewSubject vs) {
        String className = vs.getClass().getName();
        return viewSubjectToSubjectDaoMap.get(className);
    }

    public ViewSubject getViewSubject(EntitlementSubject es) {
        if (es == null) {
            return null;
        }
        
        SubjectType st = getSubjectType(es);
        assert(st != null);
        ViewSubject vs = st.newViewSubject(es, this);

        return vs;
    }

    public SubjectType getSubjectType(String className) {
        SubjectType st = viewSubjectToSubjectTypeMap.get(className);
        return st;
    }

    public SubjectContainer getSubjectContainer(SubjectType st) {
        SubjectContainer sc = subjectTypeToSubjectContainerMap.get(st);
        return sc;
    }

    public void setViewSubjectToSubjectDaoMap(Map<String, SubjectDao> viewSubjectToSubjectDaoMap) {
        this.viewSubjectToSubjectDaoMap = viewSubjectToSubjectDaoMap;
    }

    public void setEntitlementSubjectToSubjectTypeMap(Map<String, SubjectType> entitlementSubjectToSubjectTypeMap) {
        this.entitlementSubjectToSubjectTypeMap = entitlementSubjectToSubjectTypeMap;
    }

    public void setViewSubjectToSubjectTypeMap(Map<String, SubjectType> viewSubjectToSubjectTypeMap) {
        this.viewSubjectToSubjectTypeMap = viewSubjectToSubjectTypeMap;
    }

    public void setSubjectTypeToSubjectContainerMap(Map<SubjectType, SubjectContainer> subjectTypeToSubjectContainerMap) {
        this.subjectTypeToSubjectContainerMap = subjectTypeToSubjectContainerMap;
    }

    public static SubjectFactory getInstance() {
        ManagedBeanResolver mbr = new ManagedBeanResolver();
        SubjectFactory sf = (SubjectFactory)mbr.resolve("subjectFactory");
        return sf;
    }
}

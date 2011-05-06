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
 * $Id: ViewApplicationDao.java,v 1.11 2010/01/13 19:36:46 farble1670 Exp $
 */
package com.sun.identity.admin.dao;

import com.iplanet.sso.SSOToken;
import com.sun.identity.admin.ManagedBeanResolver;
import com.sun.identity.admin.Token;
import com.sun.identity.admin.model.FilterHolder;
import com.sun.identity.admin.model.RealmBean;
import com.sun.identity.admin.model.RealmsBean;
import com.sun.identity.admin.model.ViewApplication;
import com.sun.identity.admin.model.ViewApplicationType;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationManager;
import com.sun.identity.entitlement.ApplicationPrivilege.Action;
import com.sun.identity.entitlement.ApplicationPrivilegeManager;
import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.entitlement.ApplicationTypeManager;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.security.auth.Subject;
import java.util.Set;
import java.util.HashSet;
import com.sun.identity.entitlement.util.SearchFilter;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import java.util.Collections;
import java.util.List;

public class ViewApplicationDao implements Serializable {

    private ViewApplicationTypeDao viewApplicationTypeDao;

    public void setViewApplicationTypeDao(ViewApplicationTypeDao viewApplicationTypeDao) {
        this.viewApplicationTypeDao = viewApplicationTypeDao;
    }

    private String getPattern(String filter) {
        String pattern;
        if (filter == null || filter.length() == 0) {
            pattern = "*";
        } else {
            pattern = "*" + filter + "*";
        }

        return pattern;
    }

    private Set<SearchFilter> getSearchFilters(List<FilterHolder> filterHolders) {
        Set<SearchFilter> sfs = new HashSet<SearchFilter>();

        for (FilterHolder fh : filterHolders) {
            List<SearchFilter> l = fh.getViewFilter().getSearchFilters();
            if (l != null) {
                // TODO: list should never be null
                sfs.addAll(l);
            }
        }

        return sfs;
    }

    public Map<String, ViewApplication> getViewApplications() {
        return getViewApplications(null, Collections.EMPTY_LIST);
    }

    public Map<String, ViewApplication> getViewApplications(String filter, List<FilterHolder> filterHolders) {
        Set<SearchFilter> sfs = getSearchFilters(filterHolders);
        String pattern = getPattern(filter);
        sfs.add(new SearchFilter(Application.NAME_ATTRIBUTE, pattern));

        Map<String, ViewApplication> viewApplications = new HashMap<String, ViewApplication>();

        ManagedBeanResolver mbr = new ManagedBeanResolver();
        Map<String, ViewApplicationType> entitlementApplicationTypeToViewApplicationTypeMap = (Map<String, ViewApplicationType>) mbr.resolve("entitlementApplicationTypeToViewApplicationTypeMap");
        Token token = new Token();
        Subject userSubject = token.getSubject();
        RealmBean realmBean = RealmsBean.getInstance().getRealmBean();
        ApplicationPrivilegeManager apm = getApplicationPrivilegeManager();

        Set<String> names;
        try {
            names = ApplicationManager.search(userSubject, realmBean.getName(), sfs);
            for (String name : names) {
                Application a = ApplicationManager.getApplication(userSubject, realmBean.getName(), name);
                if (a.getResources() == null || a.getResources().size() == 0) {
                    // TODO: log
                    continue;
                }

                // application type
                ViewApplicationType vat = entitlementApplicationTypeToViewApplicationTypeMap.get(a.getApplicationType().getName());
                if (vat == null) {
                    // TODO: log
                    continue;
                }

                ViewApplication va = new ViewApplication(a);

                // writable?
                boolean writable = apm.hasPrivilege(a, Action.MODIFY);
                va.setWritable(writable);

                // in use?
                boolean inUse = !a.canBeDeleted();
                va.setInUse(inUse);

                viewApplications.put(va.getName(), va);
            }

            return viewApplications;
        } catch (EntitlementException ee) {
            throw new RuntimeException(ee);
        }
    }

    private ApplicationPrivilegeManager getApplicationPrivilegeManager() {
        SSOToken t = new Token().getSSOToken();
        Subject s = SubjectUtils.createSubject(t);
        RealmBean realmBean = RealmsBean.getInstance().getRealmBean();
        ApplicationPrivilegeManager apm = ApplicationPrivilegeManager.getInstance(realmBean.getName(), s);

        return apm;
    }

    public ViewApplication getViewApplication(String name) {
        ManagedBeanResolver mbr = new ManagedBeanResolver();
        Map<String, ViewApplicationType> entitlementApplicationTypeToViewApplicationTypeMap = (Map<String, ViewApplicationType>) mbr.resolve("entitlementApplicationTypeToViewApplicationTypeMap");

        Token token = new Token();
        Subject adminSubject = token.getSubject();

        RealmBean realmBean = RealmsBean.getInstance().getRealmBean();

        try {
            Application a = ApplicationManager.getApplication(adminSubject, realmBean.getName(), name);
            if (a.getResources() == null || a.getResources().size() == 0) {
                return null;
            }

            // application type
            ViewApplicationType vat = entitlementApplicationTypeToViewApplicationTypeMap.get(a.getApplicationType().getName());
            if (vat == null) {
                return null;
            }

            ViewApplication va = new ViewApplication(a);
            return va;
        } catch (EntitlementException e) {
            throw new RuntimeException(e);
        }
    }

    public Application newApplication(String name, ViewApplicationType vat) {
        String eApplicationTypeName = vat.getEntitlementApplicationType();
        Token token = new Token();
        Subject adminSubject = token.getSubject();
        String realm = RealmsBean.getInstance().getRealmBean().getName();
        ApplicationType at = ApplicationTypeManager.getAppplicationType(adminSubject, eApplicationTypeName);
        Application a;
        try {
            a = ApplicationManager.newApplication(realm, name, at);
        } catch (EntitlementException ee) {
            throw new AssertionError(ee);
        }

        return a;


    }

    public void remove(ViewApplication va) {
        Token token = new Token();
        Subject adminSubject = token.getSubject();

        RealmBean realmBean = RealmsBean.getInstance().getRealmBean();
        try {
            ApplicationManager.deleteApplication(adminSubject, realmBean.getName(), va.getName());
        } catch (EntitlementException ee) {
            throw new AssertionError(ee);
        }
    }

    public boolean exists(ViewApplication va) {
        Token token = new Token();
        Subject adminSubject = token.getSubject();

        RealmBean realmBean = RealmsBean.getInstance().getRealmBean();
        try {
            Application a = ApplicationManager.getApplication(adminSubject, realmBean.getName(), va.getName());
            return (a != null);
        } catch (EntitlementException e) {
            return false;
        }
    }

    public void setViewApplication(ViewApplication va) {
        try {
            Application a = va.toApplication();
            RealmBean realmBean = RealmsBean.getInstance().getRealmBean();
            Subject adminSubject = new Token().getSubject();
            ApplicationManager.saveApplication(adminSubject, realmBean.getName(), a);
        } catch (EntitlementException ee) {
            throw new AssertionError(ee);
        }
    }

    public Application getApplication(ViewApplication va) {
        String name = va.getName();
        Token token = new Token();
        Subject adminSubject = token.getSubject();
        RealmBean realmBean = RealmsBean.getInstance().getRealmBean();

        try {
            return ApplicationManager.getApplication(adminSubject, realmBean.getName(), name);
        } catch (EntitlementException e) {
            throw new RuntimeException(e);
        }
    }

    public static ViewApplicationDao getInstance() {
        ManagedBeanResolver mbr = new ManagedBeanResolver();
        ViewApplicationDao vadao = (ViewApplicationDao) mbr.resolve("viewApplicationDao");
        return vadao;
    }
}

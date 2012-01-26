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
 * $Id: RealmDao.java,v 1.4 2009/06/09 22:40:37 farble1670 Exp $
 */
package com.sun.identity.admin.dao;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.admin.Token;
import com.sun.identity.admin.model.RealmBean;
import com.sun.identity.common.Constants;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RealmDao implements Serializable {

    private static RealmBean rootRealmBean;


    static {
        rootRealmBean = new RealmBean();
        rootRealmBean.setName("/");
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

    public List<RealmBean> getSubRealmBeans(
            RealmBean baseRealmBean, String filter, boolean recurse) {

        String pattern = getPattern(filter);
        if (baseRealmBean == null) {
            baseRealmBean = getBaseRealmBean();
        }
        List<RealmBean> realmBeans = new ArrayList<RealmBean>();

        try {
            SSOToken ssot = new Token().getSSOToken();
            OrganizationConfigManager orgMgr =
                    new OrganizationConfigManager(ssot, baseRealmBean.getName());

            // get sub realms
            Set<String> names = orgMgr.getSubOrganizationNames(pattern, recurse);
            for (String name : names) {
                OrganizationConfigManager subOrgMgr = orgMgr.getSubOrgConfigManager(name);
                name = DNMapper.orgNameToRealmName(subOrgMgr.getOrganizationName());
                RealmBean rb = new RealmBean();
                rb.setName(name);
                realmBeans.add(rb);
            }

            return realmBeans;
        } catch (SMSException smse) {
            throw new RuntimeException(smse);
        }
    }

    public List<RealmBean> getPeerRealmBeans(
            RealmBean baseRealmBean, String filter) {

        String pattern = getPattern(filter);
        if (baseRealmBean == null) {
            baseRealmBean = getBaseRealmBean();
        }
        List<RealmBean> realmBeans = new ArrayList<RealmBean>();

        if (baseRealmBean.equals(rootRealmBean)) {
            return realmBeans;
        }

        try {
            SSOToken ssot = new Token().getSSOToken();
            OrganizationConfigManager orgMgr =
                    new OrganizationConfigManager(ssot, baseRealmBean.getName());

            // get peer realms
            OrganizationConfigManager parentOrgMgr = orgMgr.getParentOrgConfigManager();
            Set<String> names = parentOrgMgr.getSubOrganizationNames(pattern, false);
            names.remove(baseRealmBean.getName());
            for (String name : names) {
                OrganizationConfigManager peerOrgMgr = parentOrgMgr.getSubOrgConfigManager(name);
                name = DNMapper.orgNameToRealmName(peerOrgMgr.getOrganizationName());
                RealmBean rb = new RealmBean();
                rb.setName(name);
                if (!realmBeans.contains(rb)) {
                    realmBeans.add(rb);
                }
            }

            return realmBeans;
        } catch (SMSException smse) {
            throw new RuntimeException(smse);
        }
    }

    public static RealmBean getBaseRealmBean(SSOToken t) {
        RealmBean rb = new RealmBean();
        rb.setName(getBaseRealmName(t));
        return rb;
    }

    public RealmBean getBaseRealmBean() {
        RealmBean rb = new RealmBean();
        rb.setName(getBaseRealmName());
        return rb;
    }

    public static String getBaseRealmName(SSOToken t) {
        try {
            String org = t.getProperty(Constants.ORGANIZATION);
            String name = DNMapper.orgNameToRealmName(org);
            return name;
        } catch (SSOException ssoe) {
            throw new RuntimeException(ssoe);
        }
    }

    public String getBaseRealmName() {
        Token t = new Token();
        return getBaseRealmName(t.getSSOToken());
    }
}

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
 * $Id: RealmsBean.java,v 1.8 2009/06/11 19:20:40 farble1670 Exp $
 */
package com.sun.identity.admin.model;

import com.sun.identity.admin.ManagedBeanResolver;
import com.sun.identity.admin.dao.RealmDao;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.faces.model.SelectItem;
import org.apache.commons.collections.comparators.NullComparator;

public class RealmsBean implements Serializable {

    private List<RealmBean> realmBeans;
    private RealmBean realmBean;
    private RealmDao realmDao;
    private RealmBean baseRealmBean;
    private boolean realmSelectPopupVisible = false;
    private RealmBean realmSelectPopupRealmBean;
    private String realmSelectPopupFilter;
    private boolean realmChange = false;

    public void resetRealmSelectPopup() {
        realmSelectPopupVisible = false;
        realmSelectPopupRealmBean = null;
        realmChange = false;
        realmSelectPopupFilter = null;
        resetRealmBeans();
    }

    private void resetRealmBeans() {
        realmBeans = realmDao.getSubRealmBeans(baseRealmBean, realmSelectPopupFilter, true);
        realmBeans.add(baseRealmBean);
        Collections.sort(realmBeans);
    }

    public void setRealmDao(RealmDao realmDao) {
        this.realmDao = realmDao;
        baseRealmBean = realmDao.getBaseRealmBean();
        realmBean = baseRealmBean;
        resetRealmBeans();
    }

    public List<SelectItem> getRealmBeanItems() {
        List<SelectItem> items = new ArrayList<SelectItem>();
        for (RealmBean rb : realmBeans) {
            if (!rb.equals(realmBean)) {
                items.add(new SelectItem(rb, rb.getTitle()));
            }
        }
        return items;
    }

    public static RealmsBean getInstance() {
        ManagedBeanResolver mbr = new ManagedBeanResolver();
        RealmsBean realmsBean = (RealmsBean) mbr.resolve("realmsBean");
        return realmsBean;
    }

    public List<RealmBean> getRealmBeans() {
        return realmBeans;
    }

    public RealmBean getRealmBean() {
        return realmBean;
    }

    public void setRealmBean(RealmBean realmBean) {
        this.realmBean = realmBean;
    }

    public RealmBean getBaseRealmBean() {
        return baseRealmBean;
    }

    public boolean isRealmSelectPopupVisible() {
        return realmSelectPopupVisible;
    }

    public void setRealmSelectPopupVisible(boolean realmSelectPopupVisible) {
        this.realmSelectPopupVisible = realmSelectPopupVisible;
    }

    public RealmBean getRealmSelectPopupRealmBean() {
        return realmSelectPopupRealmBean;
    }

    public void setRealmSelectPopupRealmBean(RealmBean realmSelectPopupRealmBean) {
        this.realmSelectPopupRealmBean = realmSelectPopupRealmBean;
    }

    public String getRealmSelectPopupFilter() {
        return realmSelectPopupFilter;
    }

    public void setRealmSelectPopupFilter(String realmSelectPopupFilter) {
        if (realmSelectPopupFilter == null) {
            realmSelectPopupFilter = "";
        }
        NullComparator n = new NullComparator();
        if (n.compare(this.realmSelectPopupFilter, realmSelectPopupFilter) != 0) {
            this.realmSelectPopupFilter = realmSelectPopupFilter;
            resetRealmBeans();
        }
    }

    public boolean isRealmChange() {
        return realmChange;
    }

    public void setRealmChange(boolean realmChange) {
        this.realmChange = realmChange;
    }
}

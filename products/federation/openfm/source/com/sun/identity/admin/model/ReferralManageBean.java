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
 * $Id: ReferralManageBean.java,v 1.4 2009/11/17 21:32:20 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import com.icesoft.faces.component.datapaginator.DataPaginator;
import com.sun.identity.admin.ManagedBeanResolver;
import com.sun.identity.admin.dao.ReferralDao;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.comparators.NullComparator;

public class ReferralManageBean implements Serializable {
    private List<ReferralBean> referralBeans;
    private ReferralDao referralDao;
    private String searchFilter = "";
    private List<FilterHolder> filterHolders = new ArrayList<FilterHolder>();
    private Map<String,ViewFilterType> viewFilterTypes;
    private ReferralManageTableBean referralManageTableBean;
    private boolean removePopupVisible = false;
    private boolean viewOptionsPopupVisible = false;
    private List<String> viewOptionsPopupColumnsVisible = new ArrayList<String>();
    private int viewOptionsPopupRows = 10;
    private DataPaginator dataPaginator;

    public List<ReferralBean> getReferralBeans() {
        return referralBeans;
    }

    public void newFilterHolder() {
        FilterHolder fh = new FilterHolder();
        fh.setViewFilterTypes(viewFilterTypes);
        filterHolders.add(fh);
    }

    public void setReferralDao(ReferralDao referralDao) {
        this.referralDao = referralDao;
        reset();
    }

    public void reset() {
        referralBeans = referralDao.getReferralBeans(getSearchFilter(),getFilterHolders());
        getReferralManageTableBean().setReferralBeans(referralBeans);
    }

    public List<FilterHolder> getFilterHolders() {
        return filterHolders;
    }

    public ReferralManageTableBean getReferralManageTableBean() {
        return referralManageTableBean;
    }

    public void setReferralManageTableBean(ReferralManageTableBean referralManageTableBean) {
        this.referralManageTableBean = referralManageTableBean;
    }

    public void setViewFilterTypes(Map<String,ViewFilterType> viewFilterTypes) {
        this.viewFilterTypes = viewFilterTypes;
    }

    public String getSearchFilter() {
        return searchFilter;
    }

    public void setSearchFilter(String searchFilter) {
        if (searchFilter == null) {
            searchFilter = "";
        }
        NullComparator n = new NullComparator();
        if (n.compare(this.searchFilter, searchFilter) != 0) {
            this.searchFilter = searchFilter;
            reset();
        }
    }

    public boolean isRemovePopupVisible() {
        return removePopupVisible;
    }

    public void setRemovePopupVisible(boolean removePopupVisible) {
        this.removePopupVisible = removePopupVisible;
    }

    public int getSizeSelected() {
        int size = 0;
        for (ReferralBean rb: referralBeans) {
            if (rb.isSelected()) {
                size++;
            }
        }

        return size;
    }

    public boolean isViewOptionsPopupVisible() {
        return viewOptionsPopupVisible;
    }

    public void setViewOptionsPopupVisible(boolean viewOptionsPopupVisible) {
        this.viewOptionsPopupVisible = viewOptionsPopupVisible;
    }

    public List<String> getViewOptionsPopupColumnsVisible() {
        return viewOptionsPopupColumnsVisible;
    }

    public void setViewOptionsPopupColumnsVisible(List<String> viewOptionsPopupColumnsVisible) {
        this.viewOptionsPopupColumnsVisible = viewOptionsPopupColumnsVisible;
    }

    public int getViewOptionsPopupRows() {
        return viewOptionsPopupRows;
    }

    public void setViewOptionsPopupRows(int viewOptionsPopupRows) {
        this.viewOptionsPopupRows = viewOptionsPopupRows;
    }

    public static ReferralManageBean getInstance() {
        ManagedBeanResolver mbr = new ManagedBeanResolver();
        ReferralManageBean rmb = (ReferralManageBean)mbr.resolve("referralManageBean");
        return rmb;
    }

    public DataPaginator getDataPaginator() {
        return dataPaginator;
    }

    public void setDataPaginator(DataPaginator dataPaginator) {
        this.dataPaginator = dataPaginator;
    }

}

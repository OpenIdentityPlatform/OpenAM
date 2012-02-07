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
 * $Id: DelegationManageBean.java,v 1.1 2009/11/18 17:14:31 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import com.icesoft.faces.component.datapaginator.DataPaginator;
import com.sun.identity.admin.dao.DelegationDao;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.comparators.NullComparator;

public class DelegationManageBean implements Serializable {
    private List<DelegationBean> delegationBeans;
    private DelegationDao delegationDao;
    private DelegationManageTableBean delegationManageTableBean;
    private boolean viewOptionsPopupVisible = false;
    private List<String> viewOptionsPopupColumnsVisible = new ArrayList<String>();
    private int viewOptionsPopupRows = 10;
    private String searchFilter = "";
    private List<FilterHolder> filterHolders = new ArrayList<FilterHolder>();
    private boolean removePopupVisible = false;
    private Map<String,ViewFilterType> viewFilterTypes;
    private DataPaginator dataPaginator;

    public void newFilterHolder() {
        FilterHolder fh = new FilterHolder();
        fh.setViewFilterTypes(viewFilterTypes);
        filterHolders.add(fh);
    }

    public boolean isLimited() {
        return delegationDao.isLimited();
    }

    public List<DelegationBean> getDelegationBeans() {
        return delegationBeans;
    }

    public void setDelegationDao(DelegationDao delegationDao) {
        this.delegationDao = delegationDao;
        reset();
    }

    public DelegationManageTableBean getDelegationManageTableBean() {
        return delegationManageTableBean;
    }

    public void setDelegationManageTableBean(DelegationManageTableBean delegationManageTableBean) {
        this.delegationManageTableBean = delegationManageTableBean;
    }

    public void reset() {
        delegationBeans = delegationDao.getDelegationBeans(searchFilter, filterHolders);
        delegationManageTableBean.setDelegationBeans(delegationBeans);
        delegationManageTableBean.sort();
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

    public int getSizeSelected() {
        int size = 0;
        for (DelegationBean db: delegationBeans) {
            if (db.isSelected()) {
                size++;
            }
        }

        return size;
    }

    public boolean isRemovePopupVisible() {
        return removePopupVisible;
    }

    public void setRemovePopupVisible(boolean removePopupVisible) {
        this.removePopupVisible = removePopupVisible;
    }

    public Map<String, ViewFilterType> getViewFilterTypes() {
        return viewFilterTypes;
    }

    public void setViewFilterTypes(Map<String, ViewFilterType> viewFilterTypes) {
        this.viewFilterTypes = viewFilterTypes;
    }

    public List<FilterHolder> getFilterHolders() {
        return filterHolders;
    }

    public void setFilterHolders(List<FilterHolder> filterHolders) {
        this.filterHolders = filterHolders;
    }

    public DataPaginator getDataPaginator() {
        return dataPaginator;
    }

    public void setDataPaginator(DataPaginator dataPaginator) {
        this.dataPaginator = dataPaginator;
    }
}

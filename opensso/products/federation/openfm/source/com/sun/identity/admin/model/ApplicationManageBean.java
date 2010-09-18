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
 * $Id: ApplicationManageBean.java,v 1.7 2009/11/17 21:44:54 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import com.icesoft.faces.component.datapaginator.DataPaginator;
import com.sun.identity.admin.dao.ViewApplicationDao;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.comparators.NullComparator;

public class ApplicationManageBean implements Serializable {
    private List<ViewApplication> viewApplications;
    private ViewApplicationDao viewApplicationDao;
    private ApplicationManageTableBean applicationManageTableBean;
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

    public List<ViewApplication> getViewApplications() {
        return viewApplications;
    }

    public void setViewApplicationDao(ViewApplicationDao viewApplicationDao) {
        this.viewApplicationDao = viewApplicationDao;
        reset();
    }

    public ApplicationManageTableBean getApplicationManageTableBean() {
        return applicationManageTableBean;
    }

    public void setApplicationManageTableBean(ApplicationManageTableBean applicationManageTableBean) {
        this.applicationManageTableBean = applicationManageTableBean;
    }

    public void reset() {
        viewApplications = new ArrayList<ViewApplication>(viewApplicationDao.getViewApplications(searchFilter, filterHolders).values());
        applicationManageTableBean.setViewApplications(viewApplications);
        applicationManageTableBean.sort();
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
        for (ViewApplication va: viewApplications) {
            if (va.isSelected()) {
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

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
 * $Id: PolicyManageBean.java,v 1.20 2009/06/04 11:49:16 veiming Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.ManagedBeanResolver;
import com.sun.identity.admin.dao.PolicyDao;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.comparators.NullComparator;

public class PolicyManageBean implements Serializable {

    private List<PrivilegeBean> privilegeBeans;
    private PolicyDao policyDao;
    private PolicyManageTableBean policyManageTableBean = new PolicyManageTableBean();
    private boolean viewOptionsPopupVisible = false;
    private boolean removePopupVisible = false;
    private boolean exportPopupVisible = false;
    private String searchFilter = "";
    private List<PolicyFilterHolder> policyFilterHolders = new ArrayList<PolicyFilterHolder>();
    private Map<String, PolicyFilterType> policyFilterTypes;
    private boolean selectAll;
    private List<String> viewOptionsPopupColumnsVisible = new ArrayList<String>();
    private int viewOptionsPopupRows = 10;

    public List<PrivilegeBean> getPrivilegeBeans() {
        return privilegeBeans;
    }

    public void newPolicyFilterHolder() {
        PolicyFilterHolder pfh = new PolicyFilterHolder();
        pfh.setPolicyFilterTypes(getPolicyFilterTypes());
        policyFilterHolders.add(pfh);
    }

    public void reset() {
        privilegeBeans = policyDao.getPrivilegeBeans(searchFilter, policyFilterHolders);
        policyManageTableBean.setPrivilegeBeans(privilegeBeans);
    }

    public void setPolicyDao(PolicyDao policyDao) {
        this.policyDao = policyDao;
        reset();
    }

    public PolicyManageTableBean getPolicyManageTableBean() {
        return policyManageTableBean;
    }

    public boolean isViewOptionsPopupVisible() {
        return viewOptionsPopupVisible;
    }

    public void setViewOptionsPopupVisible(boolean viewOptionsPopupVisible) {
        this.viewOptionsPopupVisible = viewOptionsPopupVisible;
    }

    public String getSearchFilter() {
        return searchFilter;
    }

    public List<String> getAllPrivilegeNames() {
        return policyDao.getPrivilegeNames();
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

    public List<PolicyFilterHolder> getPolicyFilterHolders() {
        return policyFilterHolders;
    }

    public Map<String, PolicyFilterType> getPolicyFilterTypes() {
        return policyFilterTypes;
    }

    public void setPolicyFilterTypes(Map<String, PolicyFilterType> policyFilterTypes) {
        this.policyFilterTypes = policyFilterTypes;
    }

    public int getSizeSelected() {
        int size = 0;
        for (PrivilegeBean pb: privilegeBeans) {
            if (pb.isSelected()) {
                size++;
            }
        }

        return size;
    }

    public boolean isSelectAll() {
        return selectAll;
    }

    public void setSelectAll(boolean selectAll) {
        this.selectAll = selectAll;
    }

    public boolean isRemovePopupVisible() {
        return removePopupVisible;
    }

    public void setRemovePopupVisible(boolean removePopupVisible) {
        this.removePopupVisible = removePopupVisible;
    }

    public boolean isExportPopupVisible() {
        return exportPopupVisible;
    }

    public void setExportPopupVisible(boolean exportPopupVisible) {
        this.exportPopupVisible = exportPopupVisible;
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

    public static PolicyManageBean getInstance() {
        ManagedBeanResolver mbr = new ManagedBeanResolver();
        PolicyManageBean pmb = (PolicyManageBean)mbr.resolve("policyManageBean");
        return pmb;
    }
}

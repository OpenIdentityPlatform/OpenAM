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
 * $Id: ReferralManageBean.java,v 1.5 2009/06/29 13:48:01 farble1670 Exp $
 */

package com.sun.identity.admin.model;

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
    private List<PolicyFilterHolder> policyFilterHolders = new ArrayList<PolicyFilterHolder>();
    private Map<String, PolicyFilterType> policyFilterTypes;
    private ReferralManageTableBean referralManageTableBean = new ReferralManageTableBean();
    private boolean selectAll;
    private boolean removePopupVisible = false;
    private boolean viewOptionsPopupVisible = false;
    private List<String> viewOptionsPopupColumnsVisible = new ArrayList<String>();
    private int viewOptionsPopupRows = 10;

    public List<ReferralBean> getReferralBeans() {
        return referralBeans;
    }

    public void newPolicyFilterHolder() {
        PolicyFilterHolder pfh = new PolicyFilterHolder();
        pfh.setPolicyFilterTypes(policyFilterTypes);
        policyFilterHolders.add(pfh);
    }

    public void setReferralDao(ReferralDao referralDao) {
        this.referralDao = referralDao;
        reset();
    }

    public void reset() {
        referralBeans = referralDao.getReferralBeans(getSearchFilter(),getPolicyFilterHolders());
        getReferralManageTableBean().setReferralBeans(referralBeans);
    }

    public List<PolicyFilterHolder> getPolicyFilterHolders() {
        return policyFilterHolders;
    }

    public ReferralManageTableBean getReferralManageTableBean() {
        return referralManageTableBean;
    }

    public boolean isSelectAll() {
        return selectAll;
    }

    public void setSelectAll(boolean selectAll) {
        this.selectAll = selectAll;
    }

    public void setPolicyFilterTypes(Map<String, PolicyFilterType> policyFilterTypes) {
        this.policyFilterTypes = policyFilterTypes;
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

}

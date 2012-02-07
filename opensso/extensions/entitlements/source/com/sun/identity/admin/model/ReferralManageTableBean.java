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
 * $Id: ReferralManageTableBean.java,v 1.5 2009/06/22 18:31:57 farble1670 Exp $
 */
package com.sun.identity.admin.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReferralManageTableBean implements Serializable {
    private static Map<TableSortKey, Comparator> comparators = new HashMap<TableSortKey, Comparator>();

    static {
        comparators.put(new TableSortKey("name", true), new ReferralBean.NameComparator(true));
        comparators.put(new TableSortKey("name", false), new ReferralBean.NameComparator(false));
        comparators.put(new TableSortKey("description", true), new ReferralBean.DescriptionComparator(true));
        comparators.put(new TableSortKey("description", false), new ReferralBean.DescriptionComparator(false));
        comparators.put(new TableSortKey("birth", true), new ReferralBean.BirthComparator(true));
        comparators.put(new TableSortKey("birth", false), new ReferralBean.BirthComparator(false));
        comparators.put(new TableSortKey("author", true), new ReferralBean.AuthorComparator(true));
        comparators.put(new TableSortKey("author", false), new ReferralBean.AuthorComparator(false));
        comparators.put(new TableSortKey("modifier", true), new ReferralBean.ModifierComparator(true));
        comparators.put(new TableSortKey("modifier", false), new ReferralBean.ModifierComparator(false));
        comparators.put(new TableSortKey("modified", true), new ReferralBean.ModifiedComparator(true));
        comparators.put(new TableSortKey("modified", false), new ReferralBean.ModifiedComparator(false));
    }
    private List<ReferralBean> referralBeans;
    private int rows = 10;
    private TableSortKey tableSortKey = new TableSortKey("name");
    private List<String> columnsVisible = new ArrayList<String>();

    public ReferralManageTableBean() {
        columnsVisible.add("description");
        columnsVisible.add("resources");
        columnsVisible.add("subjects");
        columnsVisible.add("modified");
    }

    public List<ReferralBean> getReferralBeans() {
        return referralBeans;
    }

    public void setReferralBeans(List<ReferralBean> referralBeans) {
        this.referralBeans = referralBeans;
        sort();
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public TableSortKey getTableSortKey() {
        return tableSortKey;
    }

    public void setTableSortKey(TableSortKey tableSortKey) {
        this.tableSortKey = tableSortKey;
    }

    public void sort() {
        Comparator c = comparators.get(tableSortKey);
        Collections.sort(referralBeans, c);
    }

    public boolean isDescriptionColumnVisible() {
        return getColumnsVisible().contains("description");
    }

    public boolean isResourcesColumnVisible() {
        return getColumnsVisible().contains("resources");
    }

    public boolean isSubjectsColumnVisible() {
        return getColumnsVisible().contains("subjects");
    }

    public boolean isAuthorColumnVisible() {
        return getColumnsVisible().contains("author");
    }

    public boolean isBirthColumnVisible() {
        return getColumnsVisible().contains("birth");
    }

    public boolean isModifiedColumnVisible() {
        return getColumnsVisible().contains("modified");
    }

    public boolean isModifierColumnVisible() {
        return getColumnsVisible().contains("modifier");
    }

    public List<String> getColumnsVisible() {
        return columnsVisible;
    }

    public void setColumnsVisible(List<String> columnsVisible) {
        this.columnsVisible = columnsVisible;
    }
}

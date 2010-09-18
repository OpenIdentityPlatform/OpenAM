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
 * $Id: DelegationManageTableBean.java,v 1.6 2009/11/19 00:07:07 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DelegationManageTableBean implements Serializable {

    private TableSortKey sortKey = new TableSortKey("name");
    private List<DelegationBean> delegationBeans;
    private static Map<TableSortKey,Comparator> comparators = new HashMap<TableSortKey,Comparator>();
    private int cellWidth = 20;
    private List<String> columnsVisible;
    private int rows = 10;

    static {
        comparators.put(new TableSortKey("name", true), new DelegationBean.NameComparator(true));
        comparators.put(new TableSortKey("name", false), new DelegationBean.NameComparator(false));
        comparators.put(new TableSortKey("description", true), new DelegationBean.DescriptionComparator(true));
        comparators.put(new TableSortKey("description", false), new DelegationBean.DescriptionComparator(false));
        comparators.put(new TableSortKey("modifier", true), new DelegationBean.ModifierComparator(true));
        comparators.put(new TableSortKey("modifier", false), new DelegationBean.ModifierComparator(false));
        comparators.put(new TableSortKey("author", true), new DelegationBean.AuthorComparator(true));
        comparators.put(new TableSortKey("author", false), new DelegationBean.AuthorComparator(false));
        comparators.put(new TableSortKey("birth", true), new DelegationBean.BirthComparator(true));
        comparators.put(new TableSortKey("birth", false), new DelegationBean.BirthComparator(false));
        comparators.put(new TableSortKey("modified", true), new DelegationBean.ModifiedComparator(true));
        comparators.put(new TableSortKey("modified", false), new DelegationBean.ModifiedComparator(false));
    }

    public DelegationManageTableBean() {
        // nothing
    }

    public int getCellWidth() {
        return cellWidth;
    }

    public List<String> getColumnsVisible() {
        return columnsVisible;
    }

    public void setColumnsVisible(List<String> columnsVisible) {
        this.columnsVisible = columnsVisible;
    }

    public void setDelegationBeans(List<DelegationBean> delegationBeans) {
        this.delegationBeans = delegationBeans;
        sort();
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public TableSortKey getSortKey() {
        return sortKey;
    }

    public void setSortKey(TableSortKey sortKey) {
        this.sortKey = sortKey;
    }

    public void sort() {
        Comparator c = comparators.get(sortKey);
        Collections.sort(delegationBeans, c);
    }

    public boolean isDescriptionColumnVisible() {
        return columnsVisible.contains("description");
    }

    public boolean isModifierColumnVisible() {
        return columnsVisible.contains("modifier");
    }

    public boolean isModifiedColumnVisible() {
        return columnsVisible.contains("modified");
    }

    public boolean isBirthColumnVisible() {
        return columnsVisible.contains("birth");
    }

    public boolean isAuthorColumnVisible() {
        return columnsVisible.contains("author");
    }

    public boolean isResourcesColumnVisible() {
        return columnsVisible.contains("resources");
    }

    public boolean isSubjectsColumnVisible() {
        return columnsVisible.contains("subjects");
    }

    public boolean isActionsColumnVisible() {
        return columnsVisible.contains("actions");
    }
}

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
 * $Id: PolicyManageTableBean.java,v 1.20 2009/06/22 14:53:20 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PolicyManageTableBean implements Serializable {

    public int getCellWidth() {
        return cellWidth;
    }

    public List<String> getColumnsVisible() {
        return columnsVisible;
    }

    public void setColumnsVisible(List<String> columnsVisible) {
        this.columnsVisible = columnsVisible;
    }

    public void setPrivilegeBeans(List<PrivilegeBean> privilegeBeans) {
        this.privilegeBeans = privilegeBeans;
        sort();
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    private TableSortKey sortKey = new TableSortKey("name");
    private List<PrivilegeBean> privilegeBeans;
    private static Map<TableSortKey,Comparator> comparators = new HashMap<TableSortKey,Comparator>();
    private int cellWidth = 20;
    private List<String> columnsVisible = new ArrayList<String>();
    private int rows = 10;

    static {
        comparators.put(new TableSortKey("name", true), new PrivilegeBean.NameComparator(true));
        comparators.put(new TableSortKey("name", false), new PrivilegeBean.NameComparator(false));
        comparators.put(new TableSortKey("description", true), new PrivilegeBean.DescriptionComparator(true));
        comparators.put(new TableSortKey("description", false), new PrivilegeBean.DescriptionComparator(false));
        comparators.put(new TableSortKey("application", true), new PrivilegeBean.ApplicationComparator(true));
        comparators.put(new TableSortKey("application", false), new PrivilegeBean.ApplicationComparator(false));
        comparators.put(new TableSortKey("birth", true), new PrivilegeBean.BirthComparator(true));
        comparators.put(new TableSortKey("birth", false), new PrivilegeBean.BirthComparator(false));
        comparators.put(new TableSortKey("author", true), new PrivilegeBean.AuthorComparator(true));
        comparators.put(new TableSortKey("author", false), new PrivilegeBean.AuthorComparator(false));
        comparators.put(new TableSortKey("modified", true), new PrivilegeBean.ModifiedComparator(true));
        comparators.put(new TableSortKey("modified", false), new PrivilegeBean.ModifiedComparator(false));
        comparators.put(new TableSortKey("modifier", true), new PrivilegeBean.ModifierComparator(true));
        comparators.put(new TableSortKey("modifier", false), new PrivilegeBean.ModifierComparator(false));
    }

    public PolicyManageTableBean() {
        columnsVisible.add("description");
        columnsVisible.add("resources");
        columnsVisible.add("subject");
        columnsVisible.add("modified");
    }

    public TableSortKey getSortKey() {
        return sortKey;
    }

    public void setSortKey(TableSortKey sortKey) {
        this.sortKey = sortKey;
    }

    public void sort() {
        Comparator c = comparators.get(sortKey);
        Collections.sort(privilegeBeans, c);
    }

    public boolean isResourcesColumnVisible() {
        return getColumnsVisible().contains("resources");
    }

    public boolean isDescriptionColumnVisible() {
        return getColumnsVisible().contains("description");
    }

    public boolean isApplicationColumnVisible() {
        return getColumnsVisible().contains("application");
    }

    public boolean isExceptionsColumnVisible() {
        return getColumnsVisible().contains("exceptions");
    }

    public boolean isSubjectColumnVisible() {
        return getColumnsVisible().contains("subject");
    }

    public boolean isConditionColumnVisible() {
        return getColumnsVisible().contains("condition");
    }

    public boolean isActionColumnVisible() {
        return getColumnsVisible().contains("action");
    }

    public boolean isStaticAttributesColumnVisible() {
        return getColumnsVisible().contains("attributes-static");
    }

    public boolean isUserAttributesColumnVisible() {
        return getColumnsVisible().contains("attributes-user");
    }

    public boolean isBirthColumnVisible() {
        return getColumnsVisible().contains("birth");
    }

    public boolean isModifiedColumnVisible() {
        return getColumnsVisible().contains("modified");
    }

    public boolean isAuthorColumnVisible() {
        return getColumnsVisible().contains("author");
    }

    public boolean isModifierColumnVisible() {
        return getColumnsVisible().contains("modifier");
    }
}

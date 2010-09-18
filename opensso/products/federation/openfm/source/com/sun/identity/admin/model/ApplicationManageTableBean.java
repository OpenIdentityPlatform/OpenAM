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
 * $Id: ApplicationManageTableBean.java,v 1.7 2010/01/11 18:39:41 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplicationManageTableBean implements Serializable {

    public int getCellWidth() {
        return cellWidth;
    }

    public List<String> getColumnsVisible() {
        return columnsVisible;
    }

    public void setColumnsVisible(List<String> columnsVisible) {
        this.columnsVisible = columnsVisible;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    private TableSortKey sortKey = new TableSortKey("name");
    private List<ViewApplication> viewApplications;
    private static Map<TableSortKey,Comparator> comparators = new HashMap<TableSortKey,Comparator>();
    private int cellWidth = 20;
    private List<String> columnsVisible;
    private int rows = 10;

    static {
        comparators.put(new TableSortKey("name", true), new ViewApplication.NameComparator(true));
        comparators.put(new TableSortKey("name", false), new ViewApplication.NameComparator(false));
        comparators.put(new TableSortKey("description", true), new ViewApplication.DescriptionComparator(true));
        comparators.put(new TableSortKey("description", false), new ViewApplication.DescriptionComparator(false));
        comparators.put(new TableSortKey("applicationType", true), new ViewApplication.ApplicationTypeComparator(true));
        comparators.put(new TableSortKey("applicationType", false), new ViewApplication.ApplicationTypeComparator(false));
        comparators.put(new TableSortKey("overrideRule", true), new ViewApplication.OverrideRuleComparator(false));
        comparators.put(new TableSortKey("overrideRule", false), new ViewApplication.OverrideRuleComparator(false));
        comparators.put(new TableSortKey("birth", true), new ViewApplication.BirthComparator(true));
        comparators.put(new TableSortKey("birth", false), new ViewApplication.BirthComparator(false));
        comparators.put(new TableSortKey("author", true), new ViewApplication.AuthorComparator(true));
        comparators.put(new TableSortKey("author", false), new ViewApplication.AuthorComparator(false));
        comparators.put(new TableSortKey("modified", true), new ViewApplication.ModifiedComparator(true));
        comparators.put(new TableSortKey("modified", false), new ViewApplication.ModifiedComparator(false));
        comparators.put(new TableSortKey("modifier", true), new ViewApplication.ModifierComparator(true));
        comparators.put(new TableSortKey("modifier", false), new ViewApplication.ModifierComparator(false));
    }

    public ApplicationManageTableBean() {
        // nothing
    }

    public TableSortKey getSortKey() {
        return sortKey;
    }

    public void setSortKey(TableSortKey sortKey) {
        this.sortKey = sortKey;
    }

    public void sort() {
        Comparator c = comparators.get(sortKey);
        Collections.sort(viewApplications, c);
    }

    public void setViewApplications(List<ViewApplication> viewApplications) {
        this.viewApplications = viewApplications;
        sort();
    }

    public boolean isDescriptionColumnVisible() {
        return columnsVisible.contains("description");
    }

    public boolean isApplicationTypeColumnVisible() {
        return columnsVisible.contains("applicationType");
    }

    public boolean isResourcesColumnVisible() {
        return columnsVisible.contains("resources");
    }

    public boolean isSubjectTypesColumnVisible() {
        return columnsVisible.contains("subjectTypes");
    }

    public boolean isActionsColumnVisible() {
        return columnsVisible.contains("actions");
    }

    public boolean isConditionTypesColumnVisible() {
        return columnsVisible.contains("conditionTypes");
    }

    public boolean isOverrideRuleColumnVisible() {
        return columnsVisible.contains("overrideRule");
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

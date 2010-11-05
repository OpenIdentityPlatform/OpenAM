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
 * $Id: TableSortKey.java,v 1.1 2009/06/22 14:53:20 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import java.io.Serializable;

public class TableSortKey implements Serializable {

    private boolean ascending = false;
    private String column = null;

    public TableSortKey(String column) {
        this.column = column;
    }

    public TableSortKey(String column, boolean ascending) {
        this.column = column;
        this.ascending = ascending;
    }

    public boolean isAscending() {
        return ascending;
    }

    public void setAscending(boolean ascending) {
        this.ascending = ascending;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TableSortKey)) {
            return false;
        }

        TableSortKey other = (TableSortKey) o;
        return other.toString().endsWith(toString());
    }

    @Override
    public String toString() {
        return column + ":" + ascending;
    }
}

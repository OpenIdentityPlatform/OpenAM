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
 * $Id: ContainerViewCondition.java,v 1.7 2009/06/04 11:49:14 veiming Exp $
 */

package com.sun.identity.admin.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class ContainerViewCondition extends ViewCondition implements ContainerTreeNode {

    public ContainerViewCondition() {
        super();
    }
    private List<ViewCondition> viewConditions = new ArrayList<ViewCondition>();

    public void addViewCondition(ViewCondition vc) {
        getViewConditions().add(vc);
    }

    public List<ViewCondition> getViewConditions() {
        return viewConditions;
    }

    public int getViewConditionsSize() {
        return getViewConditions().size();
    }

    public List getTreeNodes() {
        return getViewConditions();
    }

    @Override
    public String getToString() {
        return toString();
    }

    @Override
    public String getToFormattedString() {
        return getToFormattedString(0);
    }

    @Override
    String getToFormattedString(int i) {
        StringBuffer b = new StringBuffer();
        String indent = getIndentString(i);

        b.append(indent);
        b.append(getTitle());
        b.append(" (\n");

        if (getViewConditions().size() > 0) {
            for (Iterator<ViewCondition> iter = getViewConditions().iterator(); iter.hasNext();) {
                b.append(iter.next().getToFormattedString(i + 2));
                if (iter.hasNext()) {
                    b.append(",");
                }
                b.append("\n");
            }
        }
        b.append(indent);
        b.append(")");

        return b.toString();
    }

    @Override
    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append(getTitle());
        b.append(" (");

        if (getViewConditions().size() > 0) {
            for (Iterator<ViewCondition> i = getViewConditions().iterator(); i.hasNext();) {
                b.append(i.next().toString());
                if (i.hasNext()) {
                    b.append(",");
                }
            }
        }
        b.append(")");

        return b.toString();
    }

    public void setViewConditions(List<ViewCondition> viewConditions) {
        this.viewConditions = viewConditions;
    }
}

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
 * $Id: ViewSubject.java,v 1.14 2009/07/31 21:53:48 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import com.icesoft.faces.context.effects.Appear;
import com.icesoft.faces.context.effects.Effect;
import com.sun.identity.entitlement.EntitlementSubject;
import java.io.Serializable;

public abstract class ViewSubject implements MultiPanelBean, TreeNode, Serializable {
    private boolean panelExpanded = true;
    private Effect panelExpandEffect;
    private Effect panelEffect;
    private boolean panelVisible = false;
    private SubjectType subjectType;
    private String name;

    public ViewSubject() {
        panelEffect = new Appear();
        panelEffect.setSubmit(true);
        panelEffect.setTransitory(false);
    }

    public abstract EntitlementSubject getEntitlementSubject();

    public boolean isPanelExpanded() {
        return panelExpanded;
    }

    public void setPanelExpanded(boolean panelExpanded) {
        this.panelExpanded = panelExpanded;
    }

    public Effect getPanelExpandEffect() {
        return panelExpandEffect;
    }

    public void setPanelExpandEffect(Effect panelExpandEffect) {
        this.panelExpandEffect = panelExpandEffect;
    }

    public Effect getPanelEffect() {
        return panelEffect;
    }

    public void setPanelEffect(Effect panelEffect) {
        this.panelEffect = panelEffect;
    }

    public boolean isPanelVisible() {
        return panelVisible;
    }

    public void setPanelVisible(boolean panelVisible) {
        if (!this.panelVisible) {
            this.panelVisible = panelVisible;
        }
    }

    public SubjectType getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(SubjectType subjectType) {
        this.subjectType = subjectType;
    }

    public String getName() {
        return name != null ? name : subjectType.getName();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return getName();
    }

    public String getToString() {
        return toString();
    }

    public String getToFormattedString() {
        return toString();
    }

    String getToFormattedString(int i) {
        return getIndentString(i) + toString();
    }

    public int getSize() {
        Tree t = new Tree(this);
        return t.size();
    }

    public int getSizeLeafs() {
        Tree t = new Tree(this);
        return t.sizeLeafs();
    }


    String getIndentString(int i) {
        String indent = "";
        for (int j = 0; j < i; j++) {
            indent += " ";
        }

        return indent;
    }

    @Override
    public String toString() {
        return subjectType.getTitle() + ":" + getTitle();
    }

    @Override
    public int hashCode() {
        return new String(getClass().getName() + ":" + getName()).hashCode();
    }

    @Override
    public abstract boolean equals(Object o);
}

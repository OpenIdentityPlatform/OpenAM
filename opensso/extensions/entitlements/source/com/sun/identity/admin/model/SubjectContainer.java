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
 * $Id: SubjectContainer.java,v 1.13 2009/06/04 11:49:17 veiming Exp $
 */

package com.sun.identity.admin.model;

import com.icesoft.faces.context.effects.Effect;
import com.sun.identity.admin.dao.SubjectDao;
import java.io.Serializable;
import java.util.List;
import org.apache.commons.collections.comparators.NullComparator;

public class SubjectContainer implements MultiPanelBean, Serializable {

    private SubjectDao subjectDao;
    private SubjectType subjectType;
    private List<ViewSubject> viewSubjects;
    private boolean panelExpanded = false;
    private Effect panelExpandEffect;
    private Effect panelEffect;
    private boolean panelVisible = false;
    private String filter = "";
    private boolean searchVisible = false;

    public boolean isVisible() {
        if (filter != null && filter.length() > 0) {
            return true;
        }
        if (viewSubjects.size() > 0) {
            return true;
        }

        return false;
    }

    public void setSubjectDao(SubjectDao subjectDao) {
        this.subjectDao = subjectDao;
        reset();
    }

    private void reset() {
        viewSubjects = subjectDao.getViewSubjects(filter);
    }

    public void setSubjectType(SubjectType subjectType) {
        this.subjectType = subjectType;
    }

    public List<ViewSubject> getViewSubjects() {
        return viewSubjects;
    }

    public SubjectType getSubjectType() {
        return subjectType;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        if (filter == null) {
            filter = "";
        }
        NullComparator n = new NullComparator();
        if (n.compare(this.filter, filter) != 0) {
            this.filter = filter;
            reset();
        }
    }

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
        this.panelVisible = panelVisible;
    }

    public boolean isSearchVisible() {
        return searchVisible;
    }

    public void setSearchVisible(boolean searchVisible) {
        this.searchVisible = searchVisible;
    }
}

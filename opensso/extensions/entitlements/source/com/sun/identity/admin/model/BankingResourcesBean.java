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
 * $Id: BankingResourcesBean.java,v 1.3 2009/06/04 11:49:14 veiming Exp $
 */

package com.sun.identity.admin.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.faces.model.SelectItem;

public class BankingResourcesBean implements Serializable {

    private boolean addPopupVisible;
    private String addPopupAccountNumber;
    private List<ViewSubject> viewSubjects;
    private ViewSubject addPopupViewSubject;
    private boolean allAccounts = false;

    public BankingResourcesBean() {
        reset();
    }

    public void reset() {
        addPopupVisible = false;
        addPopupAccountNumber = null;
        viewSubjects = Collections.EMPTY_LIST;
        addPopupViewSubject = null;
        allAccounts = false;
    }

    public boolean isAddPopupVisible() {
        return addPopupVisible;
    }

    public void setAddPopupVisible(boolean addPopupVisible) {
        this.addPopupVisible = addPopupVisible;
    }

    public List<SelectItem> getViewSubjectItems() {
        List<SelectItem> items = new ArrayList<SelectItem>();
        for (ViewSubject vs : viewSubjects) {
            IdRepoUserViewSubject idus = (IdRepoUserViewSubject) vs;
            SelectItem item;
            if (idus.getEmployeeNumber() != null && idus.getEmployeeNumber().length() > 0) {
                item = new SelectItem(idus, idus.getEmployeeNumber());
                items.add(item);
            }
        }
        return items;
    }

    public List<ViewSubject> getViewSubjects() {
        return viewSubjects;
    }

    public void setViewSubjects(List<ViewSubject> viewSubjects) {
        this.viewSubjects = viewSubjects;
    }

    public String getAddPopupAccountNumber() {
        return addPopupAccountNumber;
    }

    public void setAddPopupAccountNumber(String addPopupAccountNumber) {
        this.addPopupAccountNumber = addPopupAccountNumber;
    }

    public ViewSubject getAddPopupViewSubject() {
        return addPopupViewSubject;
    }

    public void setAddPopupViewSubject(ViewSubject addPopupViewSubject) {
        this.addPopupViewSubject = addPopupViewSubject;
    }

    public boolean isAllAccounts() {
        return allAccounts;
    }

    public void setAllAccounts(boolean allAccounts) {
        this.allAccounts = allAccounts;
    }
}

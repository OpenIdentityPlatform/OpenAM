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
 * $Id: BooleanActionsBean.java,v 1.8 2009/08/11 15:06:17 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.DeepCloneableArrayList;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BooleanActionsBean implements Serializable {
    private List<Action> actions = new ArrayList<Action>();
    private boolean addPopupVisible = false;
    private String addPopupName;

    public List<Action> getActions() {
        return actions;
    }

    public void setActions(List<Action> actions) {
        this.actions = actions;
    }

    public boolean isAddPopupVisible() {
        return addPopupVisible;
    }

    public void setAddPopupVisible(boolean addPopupVisible) {
        this.addPopupVisible = addPopupVisible;
    }

    public String getAddPopupName() {
        return addPopupName;
    }

    public void setAddPopupName(String addPopupName) {
        this.addPopupName = addPopupName;
    }

    public String getActionsToString() {
        StringBuffer b = new StringBuffer();

        for (Iterator<Action> i = actions.iterator(); i.hasNext();) {
            b.append(i.next());
            if (i.hasNext()) {
                b.append(",");
            }

        }

        return b.toString();

    }

    public String getActionsToFormattedString() {
        StringBuffer b = new StringBuffer();

        for (Iterator<Action> i = actions.iterator(); i.hasNext();) {
            b.append(i.next());
            if (i.hasNext()) {
                b.append("\n");
            }

        }

        return b.toString();

    }

    public void setActions(ViewApplication viewApplication) {
        getActions().clear();
        getActions().addAll(new DeepCloneableArrayList<Action>(viewApplication.getBooleanActionsBean().getActions()).deepClone());
    }

    public Map<String,Boolean> getActionMap() {
        Map<String,Boolean> am = new HashMap<String,Boolean>();
        for (Action a: actions) {
            am.put(a.getName(), (Boolean)a.getValue());
        }

        return am;
    }
}

/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: CCOrderedListModel.java,v 1.1 2008/07/02 17:21:45 veiming Exp $
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.ui.model;

import com.sun.web.ui.model.CCOrderableListModel;

/**
 * This is the model for <code>CCOrderedListView</code>.
 */
public class CCOrderedListModel extends CCOrderableListModel {
    private String addButtonLabel;
    private String deleteButtonLabel;

    /**
     * Returns add button label.
     *
     * @return add button label.
     */
    public String getAddButtonLabel() {
        return addButtonLabel;
    }

    /**
     * Set add button label.
     *
     * @param lbl add button label.
     */
    public void setAddButtonLabel(String lbl) {
        addButtonLabel = lbl;
    }

    /**
     * Returns delete button label.
     *
     * @return delete button label.
     */
    public String getDeleteButtonLabel() {
        return deleteButtonLabel;
    }

    /**
     * Set delete button label.
     *
     * @param lbl delete button label.
     */
    public void setDeleteButtonLabel(String lbl) {
        deleteButtonLabel = lbl;
    }
}

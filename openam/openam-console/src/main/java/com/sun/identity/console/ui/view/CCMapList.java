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
 * $Id: CCMapList.java,v 1.1 2008/07/02 17:21:46 veiming Exp $
 */

/*
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.console.ui.view;

import com.iplanet.jato.view.ContainerView;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.html.OptionList;
import com.iplanet.jato.view.html.TextField;
import com.sun.identity.console.ui.model.CCMapListModel;
import com.sun.web.ui.model.CCEditableListModelInterface;
import com.sun.web.ui.view.editablelist.CCEditableList;
import com.sun.web.ui.view.html.CCSelect;

/**
 * This class is designed to be used use with CCMapListTag.
 * <p>
 * This class maintains the state of selectable list options set
 * via client-side Javascript. Once the state of a selectable list has
 * been set, that state will persist until the state is cleared via
 * the resetStateData() method.
 */
public class CCMapList extends CCEditableList {
    public static final String VALUE_TEXTFIELD = "valueTextField";
    private CCMapListModel model;

    /**
     * Construct a minimal instance using the parent's default model
     * and the field's name as its bound name
     *
     * @param parent The parent view of this object
     * @param model The model to which this instance is bound.
     * @param name This view's name. 
     */
    public CCMapList(
        ContainerView parent,
        CCEditableListModelInterface model,
        String name
    ) {
        super(parent, model, name);
    }

    @Override
    public void setModel(CCEditableListModelInterface model) throws IllegalArgumentException {
        if (!(model instanceof CCMapListModel)) {
            throw new IllegalArgumentException("CCMapList should have CCMapListModel");
        }
        super.setModel(model);
        this.model = (CCMapListModel) model;
    }

    /**
     * Register child views.
     */
    protected void registerChildren() {
        super.registerChildren();
        registerChild(VALUE_TEXTFIELD, model.hasValueOptionList() ? CCSelect.class : TextField.class);
    }

    /**
     * Create child views.
     *
     * @param name The child view name.
     * @return The View object.
     */
    protected View createChild(String name) {
        if (name.equals(VALUE_TEXTFIELD)) {
            if (model.getValueOptionList() == null) {
                return new TextField(this, name, null);
            } else {
                return new CCSelect(this, name, (Object) null, model.getValueOptionList());
            }
        } else {
            return super.createChild(name);
        }
    }
}

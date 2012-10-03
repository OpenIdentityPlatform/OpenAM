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
 * $Id: CCOrderedList.java,v 1.1 2008/07/02 17:21:46 veiming Exp $
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.ui.view;

import com.iplanet.jato.view.ContainerView;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.html.Option;
import com.iplanet.jato.view.html.OptionList;
import com.iplanet.jato.view.html.TextField;
import com.sun.web.ui.model.CCOrderableListModelInterface;
import com.sun.web.ui.model.CCOrderedListModelBase;
import com.sun.web.ui.view.html.CCButton;
import com.sun.web.ui.view.orderablelist.CCOrderableList;

/**
 * This class is designed to be used use with CCOrderedListTag.
 * <p>
 * This class maintains the state of selectable list options set
 * via client-side Javascript. Once the state of a selectable list has
 * been set, that state will persist until the state is cleared via
 * the resetStateData() method.
 */
public class CCOrderedList extends CCOrderableList {

    public static final String ADD_BUTTON = "addButton";

    public static final String DELETE_BUTTON = "deleteButton";

    public static final String TEXTFIELD = "textField";
    
    /**
     * Construct a minimal instance using the parent's default model
     * and the field's name as its bound name
     *
     * @param parent The parent view of this object
     * @param model The model to which this instance is bound.
     * @param name This view's name. 
     */
    public CCOrderedList(
        ContainerView parent,
        CCOrderableListModelInterface model, 
        String name
    ) {
        super(parent, model, name);
    }

    /**
     * Register child views.
     */
    protected void registerChildren() {
        super.registerChildren();
        registerChild(ADD_BUTTON, CCButton.class);
        registerChild(DELETE_BUTTON, CCButton.class);
        registerChild(TEXTFIELD, TextField.class);
    }
    
    /**
     * Create child views.
     *
     * @param name The child view name.
     * @return The View object.
     */
    protected View createChild(String name) {
        if (name.equals(ADD_BUTTON)) {
            CCButton child = new CCButton(this, name, null);
            return child;
        } else if (name.equals(DELETE_BUTTON)) {
            CCButton child = new CCButton(this, name, null);
            // move down btn initially disabled since nothing is selected
            child.setDisabled(true);
            return child;
        } else if (name.equals(TEXTFIELD)) {
            return new TextField(this, name, null);
        } else {
            return super.createChild(name);
        }
    }
    
    protected String getOptions(OptionList optionList) {
        StringBuilder buffer = new StringBuilder();

        if (optionList != null) {
            for (int i = 0; i < optionList.size(); i++) {
                Option option = optionList.get(i);

                if (option != null) {
                    if (buffer.length() > 0) {
                        buffer.append(CCOrderedListModelBase.SEPARATOR);
                    }
                    String label = option.getLabel();
                    if (label.length() == 0) {
                        label = " ";
                    }
                    String value = option.getValue();
                    if (value.length() == 0) {
                        value = " ";
                    }
                    buffer.append(label)
                        .append(CCOrderedListModelBase.SEPARATOR)
                        .append(value);
                }
            }
        }
        return buffer.toString();
    }

}

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
 * $Id: CCUnOrderedList.java,v 1.1 2008/07/02 17:21:46 veiming Exp $
 */

package com.sun.identity.console.ui.view;

import com.iplanet.jato.view.ContainerView;
import com.sun.web.ui.model.CCEditableListModelInterface;
import com.sun.web.ui.view.editablelist.CCEditableList;

/**
 * This class is designed to be used use with CCUnOrderedListTag.
 * <p>
 * This class maintains the state of selectable list options set
 * via client-side Javascript. Once the state of a selectable list has
 * been set, that state will persist until the state is cleared via
 * the resetStateData() method.
 */
public class CCUnOrderedList extends CCEditableList {
    
    /**
     * Construct a minimal instance using the parent's default model
     * and the field's name as its bound name
     *
     * @param parent The parent view of this object
     * @param model The model to which this instance is bound.
     * @param name This view's name. 
     */
    public CCUnOrderedList(
        ContainerView parent,
        CCEditableListModelInterface model,
        String name
    ) {
        super(parent, model, name);
    }
}

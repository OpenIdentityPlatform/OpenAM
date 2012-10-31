/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AMTableTiledView.java,v 1.2 2008/06/25 05:42:47 qcheng Exp $
 *
 */

package com.sun.identity.console.base;

import com.iplanet.jato.view.RequestHandlingTiledViewBase;
import com.iplanet.jato.view.View;
import com.sun.web.ui.model.CCActionTableModel;

public class AMTableTiledView extends RequestHandlingTiledViewBase
{
    protected CCActionTableModel model = null;

    /**
     * Constructs an instance with the specified properties.
     *
     * @param parent The parent view of this object.
     * @param name This view's name.
     */
    public AMTableTiledView(View parent, CCActionTableModel model, String name){
        super(parent, name);
        this.model = model;
        registerChildren();
        setPrimaryModel(model);
    }
                                                                                
    /**
     * Registers each child view.
     */
    protected void registerChildren() {
        model.registerChildren(this);
    }

    /**
     * Instantiates each child view.
     */
    protected View createChild(String name) {
        if (model.isChildSupported(name)) {
            // Create child from action table model.
            return model.createChild(this, name);
        } else {
            throw new IllegalArgumentException(
                "Invalid child name [" + name + "]");
        }
    }
}

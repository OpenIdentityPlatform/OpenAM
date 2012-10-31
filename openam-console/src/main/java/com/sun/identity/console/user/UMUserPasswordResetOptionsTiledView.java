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
 * $Id: UMUserPasswordResetOptionsTiledView.java,v 1.2 2008/06/25 05:43:22 qcheng Exp $
 *
 */

package com.sun.identity.console.user;

import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.ChildDisplayEvent;
import com.sun.identity.console.base.AMTableTiledView;
import com.sun.identity.console.user.model.UMUserPasswordResetOptionsData;
import com.sun.web.ui.model.CCActionTableModel;

public class UMUserPasswordResetOptionsTiledView
    extends AMTableTiledView {

    public UMUserPasswordResetOptionsTiledView(
        View parent,
        CCActionTableModel model,
        String name
    ) {
        super(parent, model, name);
    }

    public boolean beginChildDisplay(ChildDisplayEvent event) {
        super.endDisplay(event);
        boolean display = true;
        int rowIndex = model.getRowIndex();
                                                                                
        if (rowIndex < model.getNumRows()) {
            String childName = event.getChildName();
            
            UMUserPasswordResetOptionsData data =
                getUserPasswordResetOptionsData(rowIndex);
            boolean isPersonalQuestion = data.isPersonalQuestion();

            if (childName.equals(
                UMUserPasswordResetOptionsViewBean.TBL_DATA_PERSONAL_QUESTION)
            ) {
                display = isPersonalQuestion;
            } else if (childName.equals(
                UMUserPasswordResetOptionsViewBean.TBL_DATA_QUESTION)
            ) {
                display = !isPersonalQuestion;
            }
        }
                                                                                
        return display;
    }

    private UMUserPasswordResetOptionsData getUserPasswordResetOptionsData(
        int i
    ) {
        UMUserPasswordResetOptionsViewBean parentVB =
            (UMUserPasswordResetOptionsViewBean)getParentViewBean();
        return parentVB.getUserPasswordResetOptionsData(i);
    }
}

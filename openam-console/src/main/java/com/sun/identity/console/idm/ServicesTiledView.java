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
 * $Id: ServicesTiledView.java,v 1.2 2008/06/25 05:42:59 qcheng Exp $
 *
 */

package com.sun.identity.console.idm;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.View;
import com.sun.identity.console.base.AMTableTiledView;
import com.sun.web.ui.model.CCActionTableModel;

public class ServicesTiledView
    extends AMTableTiledView {

    public ServicesTiledView(
        View parent,
        CCActionTableModel model,
        String name
    ) {
        super(parent, model, name);
    }

    public void handleTblDataActionHrefRequest(RequestInvocationEvent event)
        throws ModelControlException {
        String name = (String)getDisplayFieldValue(
            EntityServicesViewBean.TBL_DATA_ACTION_HREF);
        EntityServicesViewBean parentViewBean =
            (EntityServicesViewBean)getParentViewBean();
        parentViewBean.handleTblDataActionHrefRequest(name);
    }
}

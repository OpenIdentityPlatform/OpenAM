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
 * $Id: RMRealmOpViewBeanBase.java,v 1.2 2008/06/25 05:43:11 qcheng Exp $
 *
 * Portions Copyrighted 2025 3A Systems LLC.
 *
 */

package com.sun.identity.console.realm;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.view.View;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.realm.model.RMRealmModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import jakarta.servlet.http.HttpServletRequest;

public abstract class RMRealmOpViewBeanBase
    extends AMPrimaryMastHeadViewBean
{
    protected CCPageTitleModel ptModel;

    /**
     * Creates a realm operation base view bean.
     *
     * @param name Name of view
     */
    public RMRealmOpViewBeanBase(String name) {
        super(name);
    }

    protected void registerChildren() {
        ptModel.registerChildren(this);
        super.registerChildren();
    }

    protected View createChild(String name) {
        View view = null;

        if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        return new RMRealmModelImpl(req, getPageSessionAttributes());
    }
}

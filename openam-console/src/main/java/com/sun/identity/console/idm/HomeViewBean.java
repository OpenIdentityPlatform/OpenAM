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
 * $Id: HomeViewBean.java,v 1.2 2008/06/25 05:42:59 qcheng Exp $
 *
 */

package com.sun.identity.console.idm;

import com.iplanet.jato.NavigationException;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.view.View;
import com.sun.identity.console.base.AMViewConfig;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.realm.RealmPropertiesBase;
import com.sun.identity.console.idm.model.EntitiesModel;
import com.sun.identity.console.idm.model.EntitiesModelImpl;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.model.CCPageTitleModel;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class HomeViewBean
    extends RealmPropertiesBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/idm/Home.jsp";
    private static final String PAGETITLE = "pgtitle";

    public HomeViewBean() {
        super("Home");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/simplePageTitle.xml"));
        registerChildren();
    }

    protected void registerChildren() {
        super.registerChildren();
        ptModel.registerChildren(this);
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(PAGETITLE)) {
            new CCPageTitle(this, ptModel, name);
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    public void forwardTo(RequestContext reqContext)
        throws NavigationException {
        EntitiesModel model = (EntitiesModel)getModel();
        String curRealm = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        AMViewConfig config = AMViewConfig.getInstance();
        List supported = config.getSupportedEntityTypes(curRealm, model);

        if (supported.isEmpty()) {
            super.forwardTo(reqContext);
        } else {
            EntitiesViewBean vb = (EntitiesViewBean)getViewBean(
                EntitiesViewBean.class);
            setPageSessionAttribute(EntitiesViewBean.PG_SESSION_ENTITY_TYPE,
                (String)supported.iterator().next());
            passPgSessionMap(vb);
            vb.forwardTo(reqContext);
        }
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new EntitiesModelImpl(req, getPageSessionAttributes());
    }
}

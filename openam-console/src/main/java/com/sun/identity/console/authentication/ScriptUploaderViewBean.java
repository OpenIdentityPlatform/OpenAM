/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */

package com.sun.identity.console.authentication;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.view.View;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.masthead.CCSecondaryMasthead;
import com.sun.web.ui.view.pagetitle.CCPageTitle;

import javax.servlet.http.HttpServletRequest;

/**
 * JATO view-bean for uploading scripts. Based on {@link com.sun.identity.console.federation.FileUploaderViewBean} but
 * performs different quoting/escaping of text content.
 *
 * @since 12.0.0
 */
public class ScriptUploaderViewBean extends AMViewBeanBase {

    private static final String DEFAULT_DISPLAY_URL = "/console/authentication/ScriptUploader.jsp";

    private CCPageTitleModel ptModel;

    /**
     * Creates the view bean and initialises the model and view.
     */
    public ScriptUploaderViewBean() {
        super("ScriptUploader");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        createPageTitleModel();
        registerChildren();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void registerChildren() {
        ptModel.registerChildren(this);
        registerChild("secondaryMasthead", CCSecondaryMasthead.class);
        super.registerChildren();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected View createChild(String name) {
        View view = null;

        if (name.equals("pgtitle")) {
            view = new CCPageTitle(this, ptModel, name);
        } else if ((ptModel != null) && ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else if (name.equals("secondaryMasthead")) {
            view = new CCSecondaryMasthead(this, name);
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    /**
     * Re-uses same page title model as the federation file upload to get the same upload/cancel buttons.
     */
    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
                getClass().getClassLoader().getResourceAsStream(
                        "com/sun/identity/console/fileUploaderPageTitle.xml"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected AMModel getModelInternal() {
        RequestContext rc = RequestManager.getRequestContext();
        HttpServletRequest req = rc.getRequest();
        return new AMModelBase(req, getPageSessionAttributes());
    }
}

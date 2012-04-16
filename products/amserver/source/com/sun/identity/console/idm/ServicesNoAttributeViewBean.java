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
 * $Id: ServicesNoAttributeViewBean.java,v 1.2 2008/06/25 05:42:59 qcheng Exp $
 *
 */

package com.sun.identity.console.idm;

import com.iplanet.jato.NavigationException;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.idm.model.EntitiesModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import java.text.MessageFormat;
import java.util.Map;

public class ServicesNoAttributeViewBean
    extends ServiceViewBeanBase {
    public static final String DEFAULT_DISPLAY_URL =
        "/console/idm/ServicesNoAttribute.jsp";

    /**
     * Creates a service profile view bean.
     */
    public ServicesNoAttributeViewBean() {
        super("ServicesNoAttribute", DEFAULT_DISPLAY_URL, null);
        String lserviceName = (String)getPageSessionAttribute(SERVICE_NAME);
        if (lserviceName != null) {
            initialize(lserviceName);
        }
    }

    public void forwardTo(RequestContext reqContext)
        throws NavigationException {
        String serviceName = (String)getPageSessionAttribute(SERVICE_NAME);
        initialize(serviceName);
        super.forwardTo(reqContext);
    }


    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/simplePageTitle.xml"));
    }

    protected void createPropertyModel() {
        // NOOP
    }

    protected Map getValues()
        throws ModelControlException, AMConsoleException
    {
        return null;
    }

    protected Map getAttributeValues()
        throws ModelControlException, AMConsoleException {
        return null;
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
        setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
            "no.properties");
    }

    protected String getPageTitle() {
        EntitiesModel model = (EntitiesModel)getModel();
        String lserviceName = model.getLocalizedServiceName(
            (String)getPageSessionAttribute(SERVICE_NAME));
        String[] param = {lserviceName};
        return MessageFormat.format(
            model.getLocalizedString("page.title.entities.editservice"), (Object[])param);
    }

    protected boolean isCreateViewBean() {
        return false;
    }

    protected String getBreadCrumbDisplayName() {
        EntitiesModel model = (EntitiesModel)getModel();
        String lserviceName = model.getLocalizedServiceName(
            (String)getPageSessionAttribute(SERVICE_NAME));
        String[] param = {lserviceName};
        return MessageFormat.format(model.getLocalizedString(
            "breadcrumbs.editentities.editservice"), (Object[])param);
    }

    protected boolean startPageTrail() {
        return false;
    }
}

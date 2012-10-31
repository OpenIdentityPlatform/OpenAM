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
 * $Id: G11NSupportedCharsetsEditViewBean.java,v 1.3 2008/06/25 05:43:14 qcheng Exp $
 *
 */

package com.sun.identity.console.service;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.delegation.model.DelegationConfig;
import com.sun.identity.console.service.model.SMG11NModelImpl;
import com.sun.identity.console.service.model.LocaleSupportedCharsetsEntry;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.web.ui.model.CCPageTitleModel;
import java.util.Map;

public class G11NSupportedCharsetsEditViewBean
    extends G11NSupportedCharsetsViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/service/G11NSupportedCharsetsEdit.jsp";
    private static final String PGATTR_INDEX = "containerTblIndex";
    private boolean populateValues = false;

    public G11NSupportedCharsetsEditViewBean() {
        super("G11NSupportedCharsetsEdit", DEFAULT_DISPLAY_URL);
    }

    void populateValues(String index) {
        setPageSessionAttribute(PGATTR_INDEX, index);
        populateValues = true;
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);

        if (populateValues) {
            int index = Integer.parseInt((String)
                getPageSessionAttribute(PGATTR_INDEX));

            Map mapAttrs = (Map)getPageSessionAttribute(
                SMG11NViewBean.PROPERTY_ATTRIBUTE);
            OrderedSet set = (OrderedSet)mapAttrs.get(
                SMG11NModelImpl.ATTRIBUTE_NAME_SUPPORTED_CHARSETS);
            setValues((String)set.get(index));
        }
    }

    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setPageTitleText(getPageTitleText());
        ptModel.setValue("button1", "button.ok");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", "button.cancel");
    }

    /**
     * Handles reset request.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        populateValues = true;
        forwardTo();
    }

    /**
     * Handles cancel request.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton3Request(RequestInvocationEvent event) {
        super.handleButton2Request(event);
    }

    protected String getButtonlLabel() {
        return "button.ok";
    }

    protected String getPageTitleText() {
        return "globalization.service.SupportedCharsets.edit.page.title";
    }

    protected void handleButton1Request(Map values) {
        SMG11NViewBean vb = (SMG11NViewBean)getViewBean(SMG11NViewBean.class);

        Map mapAttrs = (Map)getPageSessionAttribute(
            SMG11NViewBean.PROPERTY_ATTRIBUTE);
        OrderedSet containers = (OrderedSet)mapAttrs.get(
            SMG11NModelImpl.ATTRIBUTE_NAME_SUPPORTED_CHARSETS);
        int index = Integer.parseInt((String)
            getPageSessionAttribute(PGATTR_INDEX));

        String val = LocaleSupportedCharsetsEntry.toString(
            (String)values.get(ATTR_LOCALE),
            (String)values.get(ATTR_CHARSETS));
        containers.set(index, val);
        setPageSessionAttribute(SMG11NViewBean.PAGE_MODIFIED, "1");
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    protected void createPropertyModel() {
        DelegationConfig dConfig = DelegationConfig.getInstance();
        boolean canModify = dConfig.hasPermission("/", null,
            AMAdminConstants.PERMISSION_MODIFY,
            getRequestContext().getRequest(), getClass().getName());

        String xmlFile = (canModify) ?
            "com/sun/identity/console/propertyG11NSupportedCharsets.xml" :
         "com/sun/identity/console/propertyG11NSupportedCharsets_Readonly.xml";
        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(xmlFile));
        propertySheetModel.clear();
    }
}

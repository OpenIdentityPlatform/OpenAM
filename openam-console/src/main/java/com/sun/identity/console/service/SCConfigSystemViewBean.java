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
 * $Id: SCConfigSystemViewBean.java,v 1.3 2008/06/25 05:43:15 qcheng Exp $
 *
 */

package com.sun.identity.console.service;

import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.service.model.SCConfigModel;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCPropertySheetModel;
import com.sun.identity.console.base.model.AMAdminConstants; 
import java.util.List;

public class SCConfigSystemViewBean extends SCConfigViewBean {
    public static final String DEFAULT_DISPLAY_URL =
        "/console/service/SCConfigSystem.jsp";
    public static final String DEFAULT_VIEW_BEAN = 
            "com.sun.identity.console.service.SCConfigSystemViewBean";

    private static final String SEC_SYSTEM = SCConfigModel.SEC_SYSTEM;
    private static final String TBL_SYSTEM = "tblSystem";

    private CCActionTableModel tblModelSystem;

    public SCConfigSystemViewBean() {
        super("SCConfigSystem", DEFAULT_DISPLAY_URL);
    }

    protected void createPropertyModel() {
        psModel = new CCPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/propertySCSystemConfig.xml"));
        createTableModels();
        psModel.setModel(TBL_SYSTEM, tblModelSystem);
    }

    protected void createTableModels() {
        SCConfigModel model = (SCConfigModel)getModel();
        tblModelSystem = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/tblSCConfigSystem.xml"));
        List svcNames = model.getServiceNames(SEC_SYSTEM);
        populateTableModel(tblModelSystem, svcNames, SEC_SYSTEM);
    }

    public void handleTblHrefSystemRequest(RequestInvocationEvent event) {
        setPageSessionAttribute(
                AMAdminConstants.SAVE_VB_NAME, DEFAULT_VIEW_BEAN);
        String name = (String)getDisplayFieldValue(
            TBL_HREF_PREFIX + SEC_SYSTEM);
        forwardToProfile(name);
    }
}

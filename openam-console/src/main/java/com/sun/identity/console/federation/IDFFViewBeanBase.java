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
 * $Id: IDFFViewBeanBase.java,v 1.5 2008/06/25 05:49:36 qcheng Exp $
 *
 */

package com.sun.identity.console.federation;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.View;

import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.tabs.CCTabs;

import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.AMViewConfig;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMSystemConfig;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.federation.model.EntityModel;
import com.sun.identity.console.federation.model.IDFFModel;
import com.sun.identity.console.federation.model.IDFFModelImpl;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public abstract class IDFFViewBeanBase
    extends EntityPropertiesBase 
{
    protected static final String TF_NAME = "tfName";
    protected static final String TXT_TYPE = "txtType";
    protected static final String TF_DESCRIPTION = "tfDescription";
        
    protected static List AUTH_CONTEXT_REF_NAMES = new ArrayList();
    
    public static final String MOBILE_CONTRACT =
        "http://www.projectliberty.org/schemas/authctx/classes/MobileContract";
    public static final String MOBILE_DIGITALID =
        "http://www.projectliberty.org/schemas/authctx/classes/MobileDigitalID";
    public static final String MOBILE_UNREGISTERED =
        "http://www.projectliberty.org/schemas/authctx/classes/MobileUnregistered";
    public static final String PASSWORD =
        "http://www.projectliberty.org/schemas/authctx/classes/Password";
    public static final String PASSWORD_PROTECTED_TRANSPORT =
        "http://www.projectliberty.org/schemas/authctx/classes/PasswordProtectedTransport";
    public static final String PREVIOUS_SESSION =
        "http://www.projectliberty.org/schemas/authctx/classes/Previous-Session";
    public static final String SMARTCARD =
        "http://www.projectliberty.org/schemas/authctx/classes/Smartcard";
    public static final String SMARTCARD_PKI =
        "http://www.projectliberty.org/schemas/authctx/classes/Smartcard-PKI";
    public static final String SOFTWARE_PKI =
        "http://www.projectliberty.org/schemas/authctx/classes/Software-PKI";
    public static final String TIME_SYNC_TOKEN =
        "http://www.projectliberty.org/schemas/authctx/classes/Time-Sync-Token";
    
    static {
        AUTH_CONTEXT_REF_NAMES.add(MOBILE_CONTRACT);
        AUTH_CONTEXT_REF_NAMES.add(MOBILE_DIGITALID);
        AUTH_CONTEXT_REF_NAMES.add(MOBILE_UNREGISTERED);
        AUTH_CONTEXT_REF_NAMES.add(PASSWORD);
        AUTH_CONTEXT_REF_NAMES.add(PASSWORD_PROTECTED_TRANSPORT);
        AUTH_CONTEXT_REF_NAMES.add(PREVIOUS_SESSION);
        AUTH_CONTEXT_REF_NAMES.add(SMARTCARD);
        AUTH_CONTEXT_REF_NAMES.add(SMARTCARD_PKI);
        AUTH_CONTEXT_REF_NAMES.add(SOFTWARE_PKI);
        AUTH_CONTEXT_REF_NAMES.add(TIME_SYNC_TOKEN);
    }
    
    public IDFFViewBeanBase(String name) {
        super(name);
    }
    
    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new IDFFModelImpl(req, getPageSessionAttributes());
    }
    
    protected String getProfileName() {
        return EntityModel.IDFF;
    }
    
    protected abstract void createPropertyModel();
}

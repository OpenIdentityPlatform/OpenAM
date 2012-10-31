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
 * $Id: AgentGroupAddViewBean.java,v 1.6 2008/06/25 05:42:44 qcheng Exp $
 *
 */

package com.sun.identity.console.agentconfig;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.common.configuration.AgentConfiguration;
import com.sun.identity.console.agentconfig.model.AgentsModel;
import com.sun.identity.console.agentconfig.model.AgentsModelImpl;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCTextField;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/**
 * View Bean to create agent group.
 */
public class AgentGroupAddViewBean
    extends AMPrimaryMastHeadViewBean
{
    private static final String DEFAULT_DISPLAY_URL =
        "/console/agentconfig/AgentGroupAdd.jsp";
    private static final String TF_NAME = "tfName";
    private static final String TF_SERVER_URL = "tfServerURL";
    private static final String TF_AGENT_URL = "tfAgentURL";
    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";

    private CCPageTitleModel ptModel;
    private AMPropertySheetModel propertySheetModel;

    /**
     * Creates an instance of this view bean.
     */
    public AgentGroupAddViewBean() {
        super("AgentGroupAdd");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    protected void initialize() {
        if (!initialized) {
            String agentType = getAgentType();
            if (agentType != null) {
                createPageTitleModel();
                createPropertyModel();
                registerChildren();
            }
        }
    }

    protected void registerChildren() {
        ptModel.registerChildren(this);
        propertySheetModel.registerChildren(this);
        registerChild(PGTITLE_TWO_BTNS, CCPageTitle.class);
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        registerChild(TF_NAME, CCTextField.class);
        super.registerChildren();
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(PGTITLE_TWO_BTNS)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (name.equals(PROPERTY_ATTRIBUTE)) {
            view = new AMPropertySheet(this, propertySheetModel, name);
        } else if (propertySheetModel.isChildSupported(name)) {
            view = propertySheetModel.createChild(this, name, getModel());
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/twoBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.create");
        ptModel.setValue("button2", "button.cancel");
    }

    private void createPropertyModel() {
        String agentType = getAgentType();
        String xml;

        if (agentType.equals(AgentConfiguration.AGENT_TYPE_J2EE)) {
            xml = "com/sun/identity/console/propertyAgentGroupAddJ2EE.xml";
        } else if (agentType.equals(AgentConfiguration.AGENT_TYPE_WEB)) {
            xml = "com/sun/identity/console/propertyAgentGroupAddWeb.xml";
        } else{
            xml = "com/sun/identity/console/propertyAgentGroupAdd.xml";
        }

        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(xml));
        propertySheetModel.clear();
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        return new AgentsModelImpl(req, getPageSessionAttributes());
    }

    /**
     * Handles create request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event) {
        AgentsModel model = (AgentsModel)getModel();
        String agentType = getAgentType();
        AMPropertySheet prop = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
        String agentGroupName = (String)propertySheetModel.getValue(TF_NAME);
        agentGroupName = agentGroupName.trim();
        String curRealm = (String) getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        try {
            if (agentType.equals(AgentConfiguration.AGENT_TYPE_J2EE) ||
                agentType.equals(AgentConfiguration.AGENT_TYPE_WEB)
            ) {
                String serverURL = (String)propertySheetModel.getValue(
                    TF_SERVER_URL);
                serverURL = serverURL.trim();
                model.createAgentGroup(curRealm, agentGroupName, agentType, 
                    serverURL, null);
            } else {
                model.createAgentGroup(curRealm, agentGroupName, agentType);
            }
            forwardToAgentsViewBean();
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
            forwardTo();
        }
    }
    
    /**
     * Handles cancel request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        forwardToAgentsViewBean();
    }
    
    private void forwardToAgentsViewBean() {
        AgentsViewBean vb = (AgentsViewBean)getViewBean(AgentsViewBean.class);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    protected boolean startPageTrail() {
        return false;
    }
    
    private String getAgentType() {
        return (String)getPageSessionAttribute(
            AgentsViewBean.PG_SESSION_SUPERCEDE_AGENT_TYPE);
    }
}

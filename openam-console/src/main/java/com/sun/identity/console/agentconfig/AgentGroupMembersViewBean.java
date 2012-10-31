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
 * $Id: AgentGroupMembersViewBean.java,v 1.2 2008/06/25 05:42:44 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.agentconfig;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.agentconfig.model.AgentsModel;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.idm.AMIdentity;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * View Bean to displays group members.
 */
public class AgentGroupMembersViewBean
    extends GenericAgentProfileViewBean
{
    private static final String DEFAULT_DISPLAY_URL =
        "/console/agentconfig/AgentGroupMembers.jsp";
    private static final String CHILD_MEMBERS = "txtMembers";
    
    /**
     * Creates an instance of this view bean.
     */
    public AgentGroupMembersViewBean() {
        super("AgentGroupMembers");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }
    
    protected boolean createPropertyModel() {
        return (getPageSessionAttribute(
            AgentsViewBean.PG_SESSION_AGENT_TYPE) != null);
    }

    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/oneBtnPageTitle.xml"));
        ptModel.setValue("button1", getBackButtonLabel());
    }

    protected void setPropertySheetValues() {
        //do nothing
    }

    protected void setAgentTitle() {
        AgentsModel model = (AgentsModel)getModel();
        String universalId = (String)getPageSessionAttribute(
            AgentProfileViewBean.UNIVERSAL_ID);
        
        String title = model.getLocalizedString(
            "edit.agentconfig.group.members.title");
        try {
            String displayName = model.getDisplayName(universalId);
            Object[] param = { displayName };
            ptModel.setPageTitleText(MessageFormat.format(title, param));
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }

    /**
     * Displays servers and sites information.
     *
     * @param event Display Event.
     * @throws ModelControlException if unable to initialize model.
     */
    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);
        AgentsModel model = (AgentsModel)getModel();
        String universalId = (String)getPageSessionAttribute(UNIVERSAL_ID);
        try {
            Set agents = model.getAgentGroupMembers(universalId);
            
            if ((agents == null) || agents.isEmpty()) {
                setDisplayFieldValue(CHILD_MEMBERS, model.getLocalizedString(
                    "agentconfig.group.members.nomembers"));
            } else {
                Set ordered = new TreeSet();
                Map nameToId = new HashMap(agents.size() *2);
                for (Iterator i = agents.iterator(); i.hasNext(); ) {
                    AMIdentity amid = (AMIdentity)i.next();
                    String name = amid.getName();
                    ordered.add(name);
                    nameToId.put(name, amid.getUniversalId());
                }
                StringBuilder buff = new StringBuilder();
                for (Iterator i = ordered.iterator(); i.hasNext(); ) {
                    String name = (String)i.next();
                    buff.append(name)
                    .append(" (")
                    .append((String)nameToId.get(name))
                    .append(")<br />");
                }
                setDisplayFieldValue(CHILD_MEMBERS, buff.toString());
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }

    /**
     * Handles return to agent configuration page request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException {
        AgentsViewBean vb = (AgentsViewBean)getViewBean(
            AgentsViewBean.class);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    protected HashSet getPropertyNames() {
        return new HashSet();
    }

    protected Map getFormValues()
        throws AMConsoleException, ModelControlException {
        return Collections.EMPTY_MAP;
    }

    protected AMPropertySheetModel createPropertySheetModel(String type) {
        return null;
    }

    protected void setDefaultValues(String type)
        throws AMConsoleException {
        //do nothing
    }
}
